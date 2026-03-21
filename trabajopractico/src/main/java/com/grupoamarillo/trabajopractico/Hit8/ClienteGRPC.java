package com.grupoamarillo.trabajopractico.Hit8;

import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeRequest;
import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeResponse;
import com.grupoamarillo.trabajopractico.grpc.NodoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class ClienteGRPC {
    public static void conectar(String host, int puerto, String origen, String mensaje) throws InterruptedException {
        // Canal HTTP/2 sobre el cual el stub va a hacer las llamadas RPC.
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, puerto)
                .usePlaintext()
                .build();

        try {
            // Stub bloqueante generado automáticamente desde message.proto.
            NodoServiceGrpc.NodoServiceBlockingStub stub = NodoServiceGrpc.newBlockingStub(channel);
            // Mensaje protobuf tipado: evita parseo manual de JSON.
            MensajeRequest request = MensajeRequest.newBuilder()
                    .setMsg(mensaje)
                    .setFrom(origen)
                    .build();

            // Medición de latencia de la llamada RPC.
            long inicio = System.nanoTime();
            MensajeResponse response = stub.enviar(request);
            long duracionNanos = System.nanoTime() - inicio;

            double latenciaMs = duracionNanos / 1_000_000.0;
            System.out.println("Respuesta gRPC -> from=" + response.getFrom() + ", msg=" + response.getMsg());
            System.out.printf("Latencia gRPC: %.3f ms%n", latenciaMs);
        } finally {
            // Cierre prolijo del canal para liberar recursos de red.
            channel.shutdown();
            if (!channel.awaitTermination(3, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        }
    }
}
