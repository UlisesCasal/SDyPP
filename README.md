# SDyPP

Guía general del TP1 (Hit-1 a Hit-8), con ejecución del proyecto y tests.

## Requisitos

- Java 17 o superior
- Maven 3.9+ o Maven Wrapper
- Terminal en la raíz del repo

## Levantar proyecto

```bash
cd trabajopractico
mvn clean compile
```

Si preferís wrapper:

```bash
cd trabajopractico
./mvnw clean compile
```

## Ejecutar tests

Para correr toda la suite:

```bash
cd trabajopractico
mvn clean test
```

También funciona con wrapper:

```bash
cd trabajopractico
./mvnw clean test
```

## Dónde está cada Hit

- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-1`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-2`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-3`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-4`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-5`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-6`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-7`
- `trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit-8`

## README por Hit

- [Hit-1 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit1/README.md)
- [Hit-2 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit2/README.md)
- [Hit-3 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit3/README.md)
- [Hit-4 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit4/README.md)
- [Hit-5 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit5/README.md)
- [Hit-6 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit6/README.md)
- [Hit-7 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit7/README.md)
- [Hit-8 README](trabajopractico/src/main/java/com/grupoamarillo/trabajopractico/Hit8/README.md)

## Nota útil

Si `./mvnw` no tiene permisos:

```bash
chmod +x trabajopractico/mvnw
```
