# SDyPP — Hit #3: Comunicación TCP con Sockets

## Descripción

Implementación de comunicación punto a punto entre dos nodos (`ClienteA` y `ServidorB`) utilizando sockets TCP puros en Java. Este hit establece las bases de la comunicación en red del proyecto, permitiendo el envío de mensajes de texto y la recepción de respuestas de manera síncrona.

## Estructura del proyecto

```
src/main/java/com/grupoamarillo/trabajopractico/
  ├── ClienteA.java                    ← Nodo Cliente: Inicia conexión y envía saludo
  ├── ServidorB.java                   ← Nodo Servidor: Escucha conexiones y responde
  └── TrabajopracticoApplication.java  ← Clase principal de Spring Boot

src/test/java/com/grupoamarillo/trabajopractico/
  └── TrabajopracticoApplicationTests.java  ← Tests de integración para la comunicación TCP
```

## Cómo compilar y ejecutar

### Requisitos

- Java 17+
- Maven (incluido como wrapper: `./mvnw`)

### Compilar

```bash
./mvnw clean compile
```

### Ejecutar tests

Para verificar que la comunicación funciona correctamente según lo esperado:

```bash
./mvnw test
```

### Ejecutar la comunicación entre nodos

En dos terminales separadas (recomendado para ver el flujo en tiempo real):

**Terminal 1 — Servidor B (Escucha en puerto 5001)**
```bash
./mvnw spring-boot:run -Dspring-boot.run.main-class="com.grupoamarillo.trabajopractico.ServidorB"
```

**Terminal 2 — Cliente A (Conecta a puerto 5001)**
```bash
./mvnw spring-boot:run -Dspring-boot.run.main-class="com.grupoamarillo.trabajopractico.ClienteA"
```

## Resultados de la comunicación

### Interacción exitosa

Cuando ambos nodos están activos, el flujo de consola es el siguiente:

**Consola Servidor B:**
```text
Servidor B esperando conexión...
Cliente conectado
Cliente dice: Hola B, soy A
```

**Consola Cliente A:**
```text
Servidor responde: Hola A, te saluda B
```

### Manejo de errores y reconexión

El `ClienteA` está diseñado para reintentar la conexión cada 5 segundos si el `ServidorB` no está disponible, garantizando robustez ante desconexiones momentáneas.

---

## Tecnologías utilizadas

- **Java 17**: Lenguaje principal de desarrollo.
- **Spring Boot 4.0.3**: Framework para la gestión del ciclo de vida de la aplicación.
- **Java Sockets (java.net)**: API para la comunicación TCP.
- **Maven**: Gestión de dependencias y construcción del proyecto.
