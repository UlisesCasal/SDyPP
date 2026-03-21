# Hit #8 - gRPC con Protocol Buffers

## Consigna

- Reemplazar JSON/TCP del Hit-5 por gRPC + Protobuf.
- Definir `.proto`, generar stubs y usar llamadas gRPC.
- Comparar tamaño de mensaje, latencia y experiencia de desarrollo.

## Compilar

```bash
cd trabajopractico
mvn clean compile
```

## Ejecutar nodo servidor gRPC

En este hit, `ClientServerC` levanta el servidor local y luego envía al remoto.

Terminal 1:

```bash
cd trabajopractico
java -cp target/classes com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6201 127.0.0.1:6202 C1 Hola_desde_C1
```

Terminal 2:

```bash
cd trabajopractico
java -cp target/classes com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6202 127.0.0.1:6201 C2 Hola_desde_C2
```

## Ejecutar comparador JSON vs Protobuf

```bash
cd trabajopractico
java -cp target/classes com.grupoamarillo.trabajopractico.ComparadorJsonVsProtobuf C Hola_D 200 5053
```

## Tests del Hit

```bash
cd trabajopractico
mvn -Dtest=GrpcIntegrationTest test
```
