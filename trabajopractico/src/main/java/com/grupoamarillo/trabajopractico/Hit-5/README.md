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
mkdir -p out/hit5
javac -d out/hit5 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-5/Message.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-5/ClientServerC.java
```

## Ejecutar dos nodos C

Terminal 1:

```bash
cd trabajopractico
java -cp out/hit5 com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6101 127.0.0.1:6102
```

Terminal 2:

```bash
cd trabajopractico
java -cp out/hit5 com.grupoamarillo.trabajopractico.ClientServerC 127.0.0.1:6102 127.0.0.1:6101
```
