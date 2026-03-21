# Hit #6 - Registro de contactos (Nodo D)

## Consigna

- D mantiene en RAM un registro de nodos C activos.
- D expone `GET /health` en JSON con estado del servicio.
- C recibe solo IP/puerto de D, se registra y obtiene peers para saludar.

## Precondición

- El puerto `8080` debe estar libre (en este HIT está fijo en código).
- Verificación rápida:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit6
javac -d out/hit6 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit6/Servidor/NodeD.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit6/Cliente/NodeC.java
```

## Ejecutar

Terminal 1 (NodeD):

```bash
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Hit6.Servidor.NodeD
```

Terminal 2 (NodeC):

```bash
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Hit6.Cliente.NodeC 127.0.0.1 9000
```

Terminal 3 (otro NodeC):

```bash
cd trabajopractico
java -cp out/hit6 com.grupoamarillo.trabajopractico.Hit6.Cliente.NodeC 127.0.0.1 9000
```

## Health check

```bash
curl http://localhost:8080/health
```
