# SDyPP — Hit #6: Comunicación gRPC con Protocol Buffers

## Descripción

Refactorización de la comunicación del Hit #5 (mensajes JSON sobre TCP) reemplazándola por **gRPC con Protocol Buffers**. Los nodos C y D se comunican mediante llamadas RPC tipadas, eliminando la serialización/deserialización JSON manual.

## Estructura del proyecto

```
src/main/proto/
  └── message.proto                    ← Define mensajes y servicio RPC

src/main/java/com/grupoamarillo/trabajopractico/
  ├── ClientServerC.java               ← Main: levanta servidor gRPC + envía mensaje al nodo remoto
  ├── ServidorGrpc.java                ← Servidor gRPC (implementación de NodoService)
  ├── ClienteGRPC.java                 ← Cliente gRPC con medición de latencia
  ├── ComparadorJsonVsProtobuf.java    ← Benchmark: tamaño y latencia JSON vs Protobuf
  ├── Message.java                     ← POJO para serialización JSON (comparación)
  └── TrabajopracticoApplication.java  ← Entry point Spring Boot

target/generated-sources/protobuf/
  ├── java/.../MensajeProto.java       ← Clases Protobuf (generadas automáticamente)
  └── grpc-java/.../NodoServiceGrpc.java ← Stubs gRPC (generados automáticamente)
```

## Archivo `.proto`

```protobuf
syntax = "proto3";

package comunicacion;

option java_package = "com.grupoamarillo.trabajopractico.grpc";
option java_outer_classname = "MensajeProto";

service NodoService {
  rpc Enviar (MensajeRequest) returns (MensajeResponse);
}

message MensajeRequest {
  string msg  = 1;
  string from = 2;
}

message MensajeResponse {
  string msg  = 1;
  string from = 2;
}
```

Los stubs de cliente y servidor se generan automáticamente al compilar con Maven (`protobuf-maven-plugin` + `protoc-gen-grpc-java`).

## Cómo compilar y ejecutar

### Requisitos

- Java 17+
- Maven (incluido como wrapper: `./mvnw`)

### Compilar

```bash
./mvnw clean compile
```

Esto genera automáticamente las clases Protobuf y los stubs gRPC en `target/generated-sources/`.

### Ejecutar tests

```bash
./mvnw test
```

Incluye un test de round-trip gRPC real (`grpcRoundTripEntreNodos`).

### Ejecutar la comunicación entre nodos

En dos terminales:

```bash
# Terminal 1 — Nodo D (escucha en 5054, envía a C en 5053)
./mvnw -q exec:java \
  -Dexec.mainClass="com.grupoamarillo.trabajopractico.ClientServerC" \
  -Dexec.arguments="localhost:5054,localhost:5053,D,Hola desde D"

# Terminal 2 — Nodo C (escucha en 5053, envía a D en 5054)
./mvnw -q exec:java \
  -Dexec.mainClass="com.grupoamarillo.trabajopractico.ClientServerC" \
  -Dexec.arguments="localhost:5053,localhost:5054,C,Hola desde C"
```

### Ejecutar el comparador JSON vs Protobuf

```bash
./mvnw -q exec:java \
  -Dexec.mainClass="com.grupoamarillo.trabajopractico.ComparadorJsonVsProtobuf"
```

---

## Resultados de la comparación

### Tamaño de mensajes (payload: `msg="Hola D"`, `from="C"`)

| Formato   | Tamaño (bytes) | Diferencia        |
|-----------|:--------------:|-------------------|
| JSON      | 27             | —                 |
| Protobuf  | 11             | **59% más chico** |

### Latencia gRPC (200 llamadas, localhost)

| Métrica  | Valor     |
|----------|:---------:|
| Promedio | 0,621 ms  |
| Mínima   | 0,373 ms  |
| Máxima   | 2,074 ms  |

> **Nota:** la primera llamada en frío (cold start) tiene una latencia más alta (~130 ms) debido al establecimiento de la conexión HTTP/2, el handshake gRPC y la compilación JIT.

### Comunicación Nodo C → Nodo D

```
Respuesta gRPC -> from=D, msg=Recibido de C: Hola desde C
```

---

## Comparación: JSON sobre TCP vs gRPC con Protocol Buffers

| Aspecto                    | JSON sobre TCP                                    | gRPC con Protocol Buffers                       |
|----------------------------|---------------------------------------------------|--------------------------------------------------|
| **Tamaño del mensaje**     | 27 bytes                                          | 11 bytes (59% menor)                             |
| **Serialización**          | Manual (`String.format` / parseo de JSON)         | Automática (clases generadas por `protoc`)        |
| **Tipado**                 | No — JSON es texto plano, errores en runtime      | Sí — objetos tipados, errores en compilación     |
| **Latencia promedio**      | —                                                 | 0,621 ms (warm), ~130 ms (cold start)           |
| **Protocolo de transporte**| TCP plano                                         | HTTP/2 (multiplexing, compresión de headers)     |
| **Código manual**          | Serialización y deserialización a mano            | Solo definir el `.proto`, el resto se genera     |
| **Mantenimiento**          | Agregar un campo requiere cambiar parser y builder | Agregar un campo al `.proto` y recompilar        |

### Experiencia de desarrollo

- **JSON sobre TCP**: requiere escribir manualmente la serialización (`construirJson()`), deserialización, y manejar el protocolo TCP (sockets, buffers, `flush`). Propenso a errores de tipado y formateo.
- **gRPC con Protocol Buffers**: se define un archivo `.proto` con los mensajes y el servicio, se ejecuta `mvnw compile`, y se obtienen clases Java tipadas y stubs listos para usar. El código de negocio se reduce significativamente y los errores se detectan en tiempo de compilación.

---

## Tecnologías utilizadas

- Java 17
- Spring Boot 4.0.3
- gRPC 1.63.0 (`grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`)
- Protocol Buffers 3.25.1
- Maven con `protobuf-maven-plugin` 0.6.1