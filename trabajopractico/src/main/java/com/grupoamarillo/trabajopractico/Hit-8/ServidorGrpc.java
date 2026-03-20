package com.grupoamarillo.trabajopractico;

import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeRequest;
import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeResponse;
import com.grupoamarillo.trabajopractico.grpc.NodoServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ServidorGrpc {
    private static volatile Server server;

    public static synchronized Server iniciarAsincrono(int puerto) throws IOException {
        // Evita volver a crear el servidor si ya está iniciado.
        if (server != null) {
            return server;
        }
        server = ServerBuilder
                .forPort(puerto)
                .addService(new NodoServiceImpl())
                .build()
                .start();
        // Cierra el servidor cuando la JVM termina para no dejar puertos abiertos.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                detener();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        return server;
    }

    public static void iniciar(int puerto) throws IOException, InterruptedException {
        // Modo bloqueante: útil para nodos que viven esperando requests.
        Server localServer = iniciarAsincrono(puerto);
        localServer.awaitTermination();
    }

    public static synchronized void detener() throws InterruptedException {
        if (server == null) {
            return;
        }
        server.shutdown();
        if (!server.awaitTermination(3, TimeUnit.SECONDS)) {
            server.shutdownNow();
        }
        server = null;
    }

    private static class NodoServiceImpl extends NodoServiceGrpc.NodoServiceImplBase {
        @Override
        public void enviar(MensajeRequest request, StreamObserver<MensajeResponse> responseObserver) {
            // Implementación concreta del RPC "Enviar" definido en el .proto.
            String respuestaTexto = "Recibido de " + request.getFrom() + ": " + request.getMsg();
            MensajeResponse response = MensajeResponse.newBuilder()
                    .setMsg(respuestaTexto)
                    .setFrom("D")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
