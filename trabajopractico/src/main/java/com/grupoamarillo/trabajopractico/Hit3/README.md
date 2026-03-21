# Hit #3 - Servidor B tolerante al cierre de A

## Consigna

- B debe seguir funcionando aunque A cierre abruptamente.

## Compilar

```bash
cd trabajopractico
mkdir -p out/hit3 out/hit2
javac -d out/hit3 src/main/java/com/grupoamarillo/trabajopractico/Hit3/ServidorB.java
javac -d out/hit2 src/main/java/com/grupoamarillo/trabajopractico/Hit3/ClienteA.java
```

## Ejecutar

Terminal 1:

```bash
cd trabajopractico
java -cp out/hit3 com.grupoamarillo.trabajopractico.Hit3.ServidorB
```

Terminal 2:

```bash
cd trabajopractico
java -cp out/hit3 com.grupoamarillo.trabajopractico.Hit3.ClienteA
```

## Verificación

- Cortá el cliente y volvé a conectarlo.
- El servidor debe seguir aceptando conexiones sin reiniciar.
