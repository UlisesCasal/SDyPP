package com.grupoamarillo.trabajopractico;

import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeRequest;
import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeResponse;
import com.grupoamarillo.trabajopractico.grpc.NodoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GrpcIntegrationTest {

    private static final int TEST_PORT = 5059; // Usaremos un puerto específico para que no choque con la app normal
    private static ManagedChannel channel;
    private static NodoServiceGrpc.NodoServiceBlockingStub stub;

    @BeforeAll
    public static void setup() throws IOException {
        // Levantar el servidor gRPC de tu proyecto
        ServidorGrpc.iniciarAsincrono(TEST_PORT);

        // Crear el canal de comunicación del cliente hacia el servidor
        channel = ManagedChannelBuilder.forAddress("localhost", TEST_PORT)
                .usePlaintext() // Sin TLS para simplificar locales
                .build();
        
        // Crear el stub (objeto que implementa la interfaz de red)
        stub = NodoServiceGrpc.newBlockingStub(channel);
    }

    @Test
    public void testComunicacionGrpcExitosa() {
        // "Arrange" - Preparación del entorno y datos de prueba
        String mensajeDeIda = "Hola desde JUnit!";
        String origenDeIda = "MiTest";
        
        MensajeRequest request = MensajeRequest.newBuilder()
                .setMsg(mensajeDeIda)
                .setFrom(origenDeIda)
                .build();

        // "Act" - Ejecución de la acción (hacemos la llamada gRPC real por la red local)
        MensajeResponse response = stub.enviar(request);

        // "Assert" - Validamos que el resultado sea matemáticamente exacto al código de ServidorGrpc
        assertNotNull(response, "La respuesta del servidor no debe ser nula.");
        
        // El servidor siempre setea "D" de regreso según ServidorGrpc.java
        assertEquals("D", response.getFrom(), "El nodo servidor debería identificarse como 'D'.");
        
        // Según tu código actual, el servidor siempre concatena "Recibido de " + from + ": " + msg
        String expectedMessage = "Recibido de MiTest: Hola desde JUnit!";
        assertEquals(expectedMessage, response.getMsg(), "El servidor no construyó bien el mensaje de confirmación.");
    }

    @AfterAll
    public static void teardown() throws InterruptedException {
        // Limpiamos la conexión y cerramos el servidor para que el puerto quede libre y los tests sigan funcionando la próxima
        if (channel != null) {
            channel.shutdownNow();
        }
        ServidorGrpc.detener();
    }
}
