# Hit #1 - Cliente TCP (A) y Servidor TCP (B)

## Consigna

- Servidor TCP B espera saludo de A y responde.
- Cliente TCP A se conecta a B y envía saludo.

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit1
javac -d out/hit1 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-1/Servidor/ServidorB.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit-1/Cliente/ClienteA.java
```

## Ejecutar

Terminal 1:

```bash
cd trabajopractico
java -cp out/hit1 com.grupoamarillo.trabajopractico.Servidor.ServidorB
```

Terminal 2:

```bash
cd trabajopractico
java -cp out/hit1 com.grupoamarillo.trabajopractico.Cliente.ClienteA
```
