# Hit #7 - Sistema de inscripciones por ventanas

## Consigna

- D administra una ventana actual y una siguiente (1 minuto).
- C se registra para la próxima ventana.
- C solo ve nodos activos de la ventana actual.
- Persistencia en JSON para seguimiento (`actualNodes.json`, `nextNodes.json`).

## Precondición

- El puerto `8080` debe estar libre (en este HIT está fijo en código para `/health`).
- Verificación rápida:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit7
javac -d out/hit7 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit7/Servidor/NodeD.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit7/Cliente/NodeC.java
```

## Ejecutar

Terminal 1 (NodeD):

```bash
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Hit7.Servidor.NodeD
```

Terminal 2 (NodeC):

```bash
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Hit7.Cliente.NodeC 127.0.0.1 9000
```

Terminal 3 (otro NodeC):

```bash
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Hit7.Cliente.NodeC 127.0.0.1 9000
```

## Tests del Hit

```bash
cd trabajopractico
./mvnw -q -Dtest=Hit7NodeDIntegrationTest test
```
