# Hit #6 - Registro de contactos (Nodo D)

## Consigna

- D mantiene en RAM un registro de nodos C activos.
- D expone `GET /health` en JSON con estado del servicio.
- C recibe solo IP/puerto de D, se registra y obtiene peers para saludar.

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit6
javac -d out/hit6 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-6/Servidor/NodeD.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-6/Cliente/NodeC.java
```

## Ejecutar

Terminal 1 (NodeD):

```bash
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Servidor.NodeD
```

Terminal 2 (NodeC):

```bash
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

Terminal 3 (otro NodeC):

```bash
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Cliente.NodeC 127.0.0.1 9000
```

## Health check

```bash
curl http://localhost:8080/health
```
