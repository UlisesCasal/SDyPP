# Hit #7 - Sistema de inscripciones por ventanas

## Consigna

- D administra una ventana actual y una siguiente (1 minuto).
- C se registra para la próxima ventana.
- C solo ve nodos activos de la ventana actual.
- Persistencia en JSON para seguimiento (`actualNodes.json`, `nextNodes.json`).

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit7
javac -d out/hit7 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-7/Servidor/NodeD.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-7/Cliente/NodeC.java
```

## Ejecutar

Terminal 1 (NodeD):

```bash
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Servidor.NodeD
```

Terminal 2 (NodeC):

```bash
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

Terminal 3 (otro NodeC):

```bash
cd trabajopractico
java -cp out/hit7 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

## Tests del Hit

```bash
cd trabajopractico
mvn -Dtest=Hit7NodeDIntegrationTest test
```
