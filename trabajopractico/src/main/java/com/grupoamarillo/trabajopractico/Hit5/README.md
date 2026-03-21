# Hit #5 - Serialización JSON

## Consigna

- Modificar C para enviar mensajes en JSON.
- Serializar al enviar y deserializar al recibir.

## Código

- `Message.java`: estructura de payload.
- `ClientServerC.java`: envío/recepción de JSON.

## Compilar

```bash
cd trabajopractico
./mvnw -q clean compile
```

## Ejecutar dos nodos C

Terminal 1:

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit5.ClientServerC \
  -Dexec.args="127.0.0.1:6101 127.0.0.1:6102" \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```

Terminal 2:

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit5.ClientServerC \
  -Dexec.args="127.0.0.1:6102 127.0.0.1:6101" \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```
