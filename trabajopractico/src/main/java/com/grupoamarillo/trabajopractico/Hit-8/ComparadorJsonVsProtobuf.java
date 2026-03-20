package com.grupoamarillo.trabajopractico;

import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeRequest;
import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeResponse;
import com.grupoamarillo.trabajopractico.grpc.NodoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.nio.charset.StandardCharsets;

public class ComparadorJsonVsProtobuf {

    public static void main(String[] args) throws Exception {
        String from = args.length >= 1 ? args[0] : "C";
        String msg = args.length >= 2 ? args[1] : "Hola D";
        int iteraciones = args.length >= 3 ? Integer.parseInt(args[2]) : 200;
        int puertoGrpc = args.length >= 4 ? Integer.parseInt(args[3]) : 5053;

        // Comparación de tamaño del payload serializado.
        byte[] jsonBytes = construirJson(msg, from).getBytes(StandardCharsets.UTF_8);
        byte[] protobufBytes = MensajeRequest.newBuilder().setMsg(msg).setFrom(from).build().toByteArray();

        System.out.println("Tamaño JSON: " + jsonBytes.length + " bytes");
        System.out.println("Tamaño Protobuf: " + protobufBytes.length + " bytes");

        // Benchmark simple de latencia de llamadas gRPC round-trip.
        ServidorGrpc.iniciarAsincrono(puertoGrpc);
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", puertoGrpc)
                .usePlaintext()
                .build();

        try {
            NodoServiceGrpc.NodoServiceBlockingStub stub = NodoServiceGrpc.newBlockingStub(channel);
            MensajeRequest request = MensajeRequest.newBuilder().setMsg(msg).setFrom(from).build();

            for (int i = 0; i < 20; i++) {
                stub.enviar(request);
            }

            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            long total = 0;

            for (int i = 0; i < iteraciones; i++) {
                long inicio = System.nanoTime();
                MensajeResponse response = stub.enviar(request);
                long delta = System.nanoTime() - inicio;

                if (response.getMsg().isEmpty()) {
                    throw new IllegalStateException("Respuesta vacía");
                }

                min = Math.min(min, delta);
                max = Math.max(max, delta);
                total += delta;
            }

            double promedioMs = (total / (double) iteraciones) / 1_000_000.0;
            double minMs = min / 1_000_000.0;
            double maxMs = max / 1_000_000.0;

            System.out.printf("Latencia gRPC promedio (%d llamadas): %.3f ms%n", iteraciones, promedioMs);
            System.out.printf("Latencia gRPC mínima: %.3f ms%n", minMs);
            System.out.printf("Latencia gRPC máxima: %.3f ms%n", maxMs);
        } finally {
            channel.shutdownNow();
            ServidorGrpc.detener();
        }
    }

    private static String construirJson(String msg, String from) {
        String msgEscapado = msg.replace("\\", "\\\\").replace("\"", "\\\"");
        String fromEscapado = from.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"msg\":\"" + msgEscapado + "\",\"from\":\"" + fromEscapado + "\"}";
    }
}
