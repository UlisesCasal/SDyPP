# Hit #2 - Reconexión del cliente A

## Consigna

- Si B se cae o corta conexión, A debe reconectarse y reenviar saludo.

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit2
javac -d out/hit2 \
  src/main/java/com/grupoamarillo/trabajopractico/Hit2/ServidorB.java \
  src/main/java/com/grupoamarillo/trabajopractico/Hit2/ClienteA.java
```

## Ejecutar

Terminal 1:

```bash
cd trabajopractico
java -cp out/hit2 com.grupoamarillo.trabajopractico.Hit2.ServidorB
```

Terminal 2:

```bash
cd trabajopractico
java -cp out/hit2 com.grupoamarillo.trabajopractico.Hit2.ClienteA
```

## Prueba de reconexión

- Iniciá primero el cliente.
- Apagá y levantá el servidor.
- Verificá en consola que A reintenta y vuelve a saludar.
