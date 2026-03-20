# SDyPP

Guía para ejecutar cada ejercicio de la carpeta `hit` (Hit-1 a Hit-8) del proyecto Java.

## Requisitos

- Java 17 instalado
- Terminal en la raíz del repo
- Para comandos con Maven Wrapper: permisos de ejecución en `trabajopractico/mvnw`

## Estructura de código

Los ejercicios están en:

- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-1`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-2`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-3`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-4`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-5`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-6`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-7`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-8`

## Hit #1 - Cliente TCP (A) y Servidor TCP (B)

Consigna cubierta:
- B espera el saludo de A y responde.
- A se conecta a B y lo saluda.

Compilar:

```bash
cd trabajopractico
mkdir -p out/hit1
javac -d out/hit1 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-1/Servidor/ServidorB.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-1/Cliente/ClienteA.java
```

Ejecutar en dos terminales:

```bash
# Terminal 1
cd trabajopractico
java -cp out/hit1 com.grupoamarillo.trabajopractico.Servidor.ServidorB
```

```bash
# Terminal 2
cd trabajopractico
java -cp out/hit1 com.grupoamarillo.trabajopractico.Cliente.ClienteA
```

## Hit #2 - Reconexión del cliente A

Consigna cubierta:
- A reintenta conexión y vuelve a enviar saludo si B cayó.

Compilar:

```bash
cd trabajopractico
mkdir -p out/hit2
javac -d out/hit2 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-2/ServidorB.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-2/ClienteA.java
```

Ejecutar en dos terminales:

```bash
# Terminal 1
cd trabajopractico
java -cp out/hit2 com.grupoamarillo.trabajopractico.ServidorB
```

```bash
# Terminal 2
cd trabajopractico
java -cp out/hit2 com.grupoamarillo.trabajopractico.ClienteA
```

Para probar reconexión:
- levantá primero el cliente,
- luego iniciá/cortá el servidor,
- verificá que A reintenta cada 5 segundos.

## Hit #3 - Servidor B tolerante a cierre de A

Consigna cubierta:
- B sigue funcionando aunque A cierre abruptamente.

Compilar:

```bash
cd trabajopractico
mkdir -p out/hit3
javac -d out/hit3 src/main/java/com/grupoamarillo/trabajopractico/Hit-3/ServidorB.java
```

Ejecutar:

```bash
# Terminal 1 (servidor B)
cd trabajopractico
java -cp out/hit3 com.grupoamarillo.trabajopractico.ServidorB
```

```bash
# Terminal 2 (cliente de prueba: podés usar el de Hit-2)
cd trabajopractico
java -cp out/hit2 com.grupoamarillo.trabajopractico.ClienteA
```

## Hit #4 - Programa C bidireccional (cliente + servidor)

Consigna cubierta:
- Un único programa C que escucha y también se conecta a otro C.
- Dos instancias se saludan mutuamente.

Compilar:

```bash
cd trabajopractico
mkdir -p out/hit4
javac -d out/hit4 src/main/java/com/grupoamarillo/trabajopractico/Hit-4/ClientServerC.java
```

Ejecutar dos nodos C:

```bash
# Terminal 1 (Nodo C1)
cd trabajopractico
java -cp out/hit4 com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6001 127.0.0.1:6002
```

```bash
# Terminal 2 (Nodo C2)
cd trabajopractico
java -cp out/hit4 com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6002 127.0.0.1:6001
```

## Hit #5 - Serialización JSON sobre TCP

Consigna cubierta:
- C envía/recibe mensajes JSON.
- Serializa/deserializa al enviar/recibir.

Compilar:

```bash
cd trabajopractico
mkdir -p out/hit5
javac -d out/hit5 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-5/Message.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-5/ClientServerC.java
```

Ejecutar dos nodos C:

```bash
# Terminal 1
cd trabajopractico
java -cp out/hit5 com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6101 127.0.0.1:6102
```

```bash
# Terminal 2
cd trabajopractico
java -cp out/hit5 com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6102 127.0.0.1:6101
```

## Hit #6 - Registro de contactos (Nodo D + health endpoint)

Consigna cubierta:
- Nodo D registra nodos C en RAM.
- Endpoint HTTP `/health` con estado JSON.
- C arranca en puerto aleatorio, se registra y obtiene peers para saludar.

Compilar:

```bash
cd trabajopractico
mkdir -p out/hit6
javac -d out/hit6 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-6/Servidor/NodeD.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-6/Cliente/NodeC.java
```

Ejecutar:

```bash
# Terminal 1 (Nodo D)
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Servidor.NodeD
```

```bash
# Terminal 2 (Nodo C1)
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

```bash
# Terminal 3 (Nodo C2)
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

Health check:

```bash
curl http://localhost:8080/health
```

## Hit #7 - Sistema de inscripciones por ventana (1 minuto)

Consigna cubierta:
- D administra ventana actual y próxima ventana.
- C se registra para ventana siguiente.
- C consulta activos de ventana actual.
- Persistencia en archivos JSON (`actualNodes.json` y `nextNodes.json`).

Compilar:

```bash
cd trabajopractico
mkdir -p out/hit7
javac -d out/hit7 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-7/Servidor/NodeD.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-7/Cliente/NodeC.java
```

Ejecutar:

```bash
# Terminal 1 (Nodo D)
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Servidor.NodeD
```

```bash
# Terminal 2 (Nodo C1)
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

```bash
# Terminal 3 (Nodo C2)
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

Health check:

```bash
curl http://localhost:8080/health
```

## Hit #8 - gRPC con Protocol Buffers

Consigna cubierta:
- Comunicación de Hit #5 migrada a gRPC/protobuf.
- `.proto` definido y stubs generados.
- Comparación JSON vs protobuf (tamaño y latencia) con clase comparadora.

Compilar proyecto (incluye generación protobuf):

```bash
cd trabajopractico
./mvnw clean compile
```

Ejecutar dos nodos C gRPC:

```bash
# Terminal 1
cd trabajopractico
./mvnw -q exec:java -Dexec.mainClass=com.grupoamarillo.trabajopractico.ClientServerC -Dexec.args="127.0.0.1:6201 127.0.0.1:6202 C1 Hola_desde_C1"
```

```bash
# Terminal 2
cd trabajopractico
./mvnw -q exec:java -Dexec.mainClass=com.grupoamarillo.trabajopractico.ClientServerC -Dexec.args="127.0.0.1:6202 127.0.0.1:6201 C2 Hola_desde_C2"
```

Ejecutar comparativa JSON vs protobuf:

```bash
cd trabajopractico
./mvnw -q exec:java -Dexec.mainClass=com.grupoamarillo.trabajopractico.ComparadorJsonVsProtobuf -Dexec.args="C Hola_D 200 5053"
```

## Nota rápida

Si `./mvnw` no tiene permisos:

```bash
chmod +x trabajopractico/mvnw
```
