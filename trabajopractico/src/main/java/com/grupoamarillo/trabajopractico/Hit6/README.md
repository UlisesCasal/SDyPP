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
./mvnw -q clean compile
```

## Ejecutar

Terminal 1 (NodeD):

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit6.Servidor.NodeD \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```

Terminal 2 (NodeC):

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit6.Cliente.NodeC \
  -Dexec.args="127.0.0.1 9000" \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```

Terminal 3 (otro NodeC):

```bash
cd trabajopractico
./mvnw -q \
  -Dexec.mainClass=com.grupoamarillo.trabajopractico.Hit6.Cliente.NodeC \
  -Dexec.args="127.0.0.1 9000" \
  org.codehaus.mojo:exec-maven-plugin:3.6.1:java
```

## Health check

```bash
curl http://localhost:8080/health
```
