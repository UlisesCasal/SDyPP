# Hit #4 - Programa C bidireccional

## Consigna

- Un único programa C actúa como servidor y cliente.
- Cada instancia recibe `IP:PUERTO_LOCAL` y `IP:PUERTO_REMOTO`.
- Dos instancias se saludan mutuamente.

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit4
javac -d out/hit4 src/main/java/com/grupoamarillo/trabajopractico/Hit4/ClientServerC.java
```

## Ejecutar dos nodos C

Terminal 1:

```bash
cd trabajopractico
java -cp out/hit4 com.grupoamarillo.trabajopractico.Hit4.ClientServerC 127.0.0.1:6001 127.0.0.1:6002
```

Terminal 2:

```bash
cd trabajopractico
java -cp out/hit4 com.grupoamarillo.trabajopractico.Hit4.ClientServerC 127.0.0.1:6002 127.0.0.1:6001
```
