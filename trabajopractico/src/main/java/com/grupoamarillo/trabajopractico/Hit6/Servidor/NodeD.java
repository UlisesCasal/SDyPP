package com.grupoamarillo.trabajopractico.Hit6.Servidor;
import java.io.*;
import java.net.*;
import java.util.*;

import com.grupoamarillo.trabajopractico.Hit6.Message;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

public class NodeD {

    private static List<String> nodes = new ArrayList<>();
    private static long startTime = System.currentTimeMillis();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        int tcpPort = 9000;
        int httpPort = 8080;

        startHealthEndpoint(httpPort);

        try (ServerSocket server = new ServerSocket(tcpPort)) {
            System.out.println("NodeD escuchando en puerto " + tcpPort);
            System.out.println("Health en http://localhost:" + httpPort + "/health");

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
            String json = in.readLine();
            if (json == null) return;

            // DESERIALIZAR
            Message request = mapper.readValue(json, Message.class);

            if (!"REGISTER".equals(request.action)) return;

            String clientIP = socket.getInetAddress().getHostAddress();
            int clientPort = request.port;

            String nodeAddress = clientIP + ":" + clientPort;

            System.out.println("Registro recibido: " + nodeAddress);

            synchronized (nodes) {

                nodes.add(nodeAddress);

                // RESPONDER CON JSON
                for (String node : nodes) {
                    String[] parts = node.split(":");

                    Message responseNode = new Message();
                    responseNode.ip = parts[0];
                    responseNode.port = Integer.parseInt(parts[1]);

                    String responseJson = mapper.writeValueAsString(responseNode);
                    out.println(responseJson);
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