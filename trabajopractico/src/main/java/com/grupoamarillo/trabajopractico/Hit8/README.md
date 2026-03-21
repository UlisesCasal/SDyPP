# Hit #8 - gRPC con Protocol Buffers

## Consigna

- Reemplazar JSON/TCP del Hit-5 por gRPC + Protobuf.
- Definir `.proto`, generar stubs y usar llamadas gRPC.
- Comparar tamaño de mensaje, latencia y experiencia de desarrollo.

## Compilar

```bash
cd trabajopractico
./mvnw -q clean compile
```

## Ejecutar nodo servidor gRPC

En este hit, `ClientServerC` levanta el servidor local y luego envía al remoto.

Terminal 1:

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit8.ClientServerC \
  -Dexec.args="127.0.0.1:6201 127.0.0.1:6202 C1 Hola_desde_C1" \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```

Terminal 2:

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit8.ClientServerC \
  -Dexec.args="127.0.0.1:6202 127.0.0.1:6201 C2 Hola_desde_C2" \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```

## Ejecutar comparador JSON vs Protobuf

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit8.ComparadorJsonVsProtobuf \
  -Dexec.args="C Hola_D 200 5053" \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```

## Tests del Hit

```bash
cd trabajopractico
./mvnw -q -Dtest=GrpcIntegrationTest test
```
