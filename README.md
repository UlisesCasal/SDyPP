# SDyPP — Hit #2: Comunicación TCP con Sockets en Java

## Descripción

Implementación de una comunicación básica **cliente-servidor** usando **sockets TCP** en Java. El nodo **A** (cliente) se conecta al nodo **B** (servidor), envía un mensaje de texto plano y recibe una respuesta. El ejercicio demuestra los fundamentos de la programación distribuida con sockets: apertura de conexión, envío/recepción de datos y cierre del canal.

## Estructura del proyecto

```
src/main/java/com/grupoamarillo/trabajopractico/
  ├── ServidorB.java                   ← Servidor TCP: escucha en puerto 5001 y responde al cliente
  ├── ClienteA.java                    ← Cliente TCP: se conecta al servidor, envía mensaje y recibe respuesta
  └── TrabajopracticoApplication.java  ← Entry point Spring Boot

src/test/java/com/grupoamarillo/trabajopractico/
  ├── Servidor/
  │   └── ServidorB.java               ← Versión de test del servidor
  └── Cliente/
      └── ClienteA.java                ← Versión de test del cliente
```

## Flujo de comunicación

```
┌──────────┐                        ┌──────────┐
│ ClienteA │──── "Hola B, soy A" ──▶│ ServidorB│
│  (Nodo A)│◀── "Hola A, te       ──│  (Nodo B)│
│          │     saluda B"          │          │
└──────────┘                        └──────────┘
       Socket TCP — puerto 5001
```

1. **ServidorB** abre un `ServerSocket` en el puerto `5001` y queda en espera con `accept()`.
2. **ClienteA** crea un `Socket` hacia `localhost:5001` y establece la conexión TCP.
3. El cliente envía `"Hola B, soy A"` usando un `PrintWriter`.
4. El servidor recibe el mensaje con un `BufferedReader`, lo imprime y responde `"Hola A, te saluda B"`.
5. El cliente recibe la respuesta, la muestra en consola y cierra el socket.
6. El servidor cierra el socket del cliente y el `ServerSocket`.

> **Nota:** El cliente implementa un mecanismo de **reintentos con espera**: si no logra conectarse al servidor, espera 5 segundos y vuelve a intentar (loop con `Thread.sleep(5000)`), permitiendo iniciar el cliente antes que el servidor sin que falle.

## Cómo compilar y ejecutar

### Requisitos

- Java 17+
- Maven (incluido como wrapper: `./mvnw`)

### Compilar

```bash
./mvnw clean compile
```

### Ejecutar la comunicación entre nodos

Se deben abrir **dos terminales** y ejecutar primero el servidor, luego el cliente.

#### En Bash (Linux / macOS)

```bash
# Terminal 1 — Servidor B (escucha en puerto 5001)
./mvnw -q exec:java \
  -Dexec.mainClass="com.grupoamarillo.trabajopractico.ServidorB"

# Terminal 2 — Cliente A (se conecta a localhost:5001)
./mvnw -q exec:java \
  -Dexec.mainClass="com.grupoamarillo.trabajopractico.ClienteA"
```

#### En Windows (PowerShell / CMD)

```powershell
# Terminal 1 — Servidor B
mvn exec:java "-Dexec.mainClass=com.grupoamarillo.trabajopractico.ServidorB"

# Terminal 2 — Cliente A
mvn exec:java "-Dexec.mainClass=com.grupoamarillo.trabajopractico.ClienteA"
```

> **Nota para usuarios de Windows:** En Windows las terminales tienden a corromper los argumentos pasados por `-D`. Para solucionarlo, debés englobar la estructura de tu argumento `-D` en comillas como se muestra arriba.

#### Alternativa: compilar y ejecutar con `javac` / `java`

```bash
# Compilar
javac -d target/classes src/main/java/com/grupoamarillo/trabajopractico/ServidorB.java \
                        src/main/java/com/grupoamarillo/trabajopractico/ClienteA.java

# Terminal 1 — Servidor B
java -cp target/classes com.grupoamarillo.trabajopractico.ServidorB

# Terminal 2 — Cliente A
java -cp target/classes com.grupoamarillo.trabajopractico.ClienteA
```

---

## Resultado esperado

### Terminal del Servidor B

```
Servidor B esperando conexión...
Cliente conectado
Cliente dice: Hola B, soy A
```

### Terminal del Cliente A

```
Servidor responde: Hola A, te saluda B
```

---

## Conceptos clave utilizados

| Concepto              | Descripción                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------|
| **Socket TCP**        | Canal de comunicación bidireccional entre dos procesos a través de la red                      |
| **ServerSocket**      | Socket especial del servidor que escucha conexiones entrantes en un puerto                     |
| **accept()**          | Método bloqueante que espera y acepta una conexión entrante del cliente                        |
| **PrintWriter**       | Stream de salida para enviar datos de texto a través del socket                                |
| **BufferedReader**    | Stream de entrada para recibir datos de texto a través del socket                              |
| **Reintentos**        | El cliente reintenta la conexión cada 5 segundos si el servidor no está disponible             |

---

## Tecnologías utilizadas

- Java 17
- Spring Boot 4.0.3
- Sockets TCP (`java.net.Socket`, `java.net.ServerSocket`)
- Streams de I/O (`java.io.PrintWriter`, `java.io.BufferedReader`)
