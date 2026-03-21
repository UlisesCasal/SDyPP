package com.grupoamarillo.trabajopractico.Hit6;

import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.HttpServer;

public class NodeD {

    // Registro en RAM de nodos C
    private static List<String> nodes = new ArrayList<>();

    // Tiempo de inicio para calcular uptime
    private static long startTime = System.currentTimeMillis();

    public static void main(String[] args) throws Exception {

        int tcpPort = 9000;
        int httpPort = 8080;

        // Iniciar endpoint HTTP /health
        startHealthEndpoint(httpPort);

        try (// Servidor TCP para registros
        ServerSocket server = new ServerSocket(tcpPort)) {
            System.out.println("NodeD escuchando registros en puerto " + tcpPort);
            System.out.println("Health endpoint en http://localhost:" + httpPort + "/health");

            while (true) {

                Socket socket = server.accept();

                new Thread(() -> handleRegistration(socket)).start();
            }
        }
    }

    private static void handleRegistration(Socket socket) {

        try (

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true)

        ) {

            String request = in.readLine();

            if (request == null) {
                return;
            }

            // Esperamos: REGISTER <puerto>
            String[] parts = request.split(" ");

            if (parts.length != 2) {
                return;
            }

            String clientIP = socket.getInetAddress().getHostAddress();
            String clientPort = parts[1];

            String nodeAddress = clientIP + ":" + clientPort;

            System.out.println("Registro recibido: " + nodeAddress);

            synchronized (nodes) {

                nodes.add(nodeAddress);

                // Enviar lista de nodos al cliente
                for (String node : nodes) {

                    out.println(node);
                }
            }

        } catch (IOException e) {

            System.out.println("Error manejando registro");

        } finally {

            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void startHealthEndpoint(int port) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", exchange -> {

            long uptime = (System.currentTimeMillis() - startTime) / 1000;

            String response;

            synchronized (nodes) {

                response =
                        "{\n" +
                        "\"nodes\": " + nodes.size() + ",\n" +
                        "\"uptime_seconds\": " + uptime + ",\n" +
                        "\"status\": \"OK\"\n" +
                        "}";
            }

            exchange.sendResponseHeaders(200, response.length());

            OutputStream os = exchange.getResponseBody();

            os.write(response.getBytes());

            os.close();
        });

        server.start();
    }
}