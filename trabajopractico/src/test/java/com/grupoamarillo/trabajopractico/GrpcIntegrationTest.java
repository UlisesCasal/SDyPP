package com.grupoamarillo.trabajopractico;

import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeRequest;
import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeResponse;
import com.grupoamarillo.trabajopractico.grpc.NodoServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrpcIntegrationTest {

    private static Server server;
    private static ManagedChannel channel;
    private static NodoServiceGrpc.NodoServiceBlockingStub stub;

    @BeforeAll
    public static void setup() throws IOException {
        server = ServerBuilder.forPort(0)
                .addService(new NodoServiceImpl())
                .build()
                .start();
        channel = ManagedChannelBuilder.forAddress("localhost", server.getPort())
                .usePlaintext()
                .build();
        stub = NodoServiceGrpc.newBlockingStub(channel);
    }

    @Test
    public void testComunicacionGrpcExitosa() {
        String mensajeDeIda = "Hola desde JUnit!";
        String origenDeIda = "MiTest";
        MensajeRequest request = MensajeRequest.newBuilder()
                .setMsg(mensajeDeIda)
                .setFrom(origenDeIda)
                .build();
        MensajeResponse response = stub.enviar(request);
        assertNotNull(response);
        assertEquals("D", response.getFrom());
        String expectedMessage = "Recibido de MiTest: Hola desde JUnit!";
        assertEquals(expectedMessage, response.getMsg());
    }

    @Test
    public void testProtobufSerializaMenosQueJsonSimple() {
        String mensaje = "Hola D";
        String origen = "C";
        MensajeRequest request = MensajeRequest.newBuilder()
                .setMsg(mensaje)
                .setFrom(origen)
                .build();
        byte[] protobufBytes = request.toByteArray();
        String json = "{\"msg\":\"" + mensaje + "\",\"from\":\"" + origen + "\"}";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        assertTrue(protobufBytes.length < jsonBytes.length);
    }

    @AfterAll
    public static void teardown() throws Exception {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(3, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdown();
            server.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    private static class NodoServiceImpl extends NodoServiceGrpc.NodoServiceImplBase {
        @Override
        public void enviar(MensajeRequest request, StreamObserver<MensajeResponse> responseObserver) {
            MensajeResponse response = MensajeResponse.newBuilder()
                    .setMsg("Recibido de " + request.getFrom() + ": " + request.getMsg())
                    .setFrom("D")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
