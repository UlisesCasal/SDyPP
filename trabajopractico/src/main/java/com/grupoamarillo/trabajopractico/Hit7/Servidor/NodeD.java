package com.grupoamarillo.trabajopractico.Hit7.Servidor;

import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.HttpServer;

public class NodeD {

    private static final List<String> actualNodes = new ArrayList<>();
    private static final List<String> nextNodes = new ArrayList<>();
    private static final Object windowLock = new Object();
    private static final long windowDuration = 60000L;
    private static long windowStartTime = (System.currentTimeMillis() / windowDuration) * windowDuration;
    private static long nextWindowStartTime = windowStartTime + windowDuration;

    // Marca de tiempo inicial para calcular uptime del servicio.
    private static long startTime = System.currentTimeMillis();

    public static void main(String[] args) throws Exception {

        // Puerto TCP donde escucha solicitudes de registro de NodeC.
        int tcpPort = 9000;
        // Puerto HTTP donde expone /health.
        int httpPort = 8080;

        startHealthEndpoint(httpPort);
        startWindowScheduler();

        try (ServerSocket server = new ServerSocket(tcpPort)) {
            System.out.println("NodeD escuchando registros en puerto " + tcpPort);
            System.out.println("Health endpoint en http://localhost:" + httpPort + "/health");

            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        }
    }

    private static void startWindowScheduler() {
        Thread scheduler = new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();
                long sleepMs = Math.max(1L, nextWindowStartTime - now);
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {
                }
                rotateWindow();
            }
        });
        scheduler.setDaemon(true);
        scheduler.start();
    }

    private static void rotateWindow() {
        synchronized (windowLock) {
            actualNodes.clear();
            actualNodes.addAll(nextNodes);
            nextNodes.clear();
            windowStartTime = nextWindowStartTime;
            nextWindowStartTime = windowStartTime + windowDuration;
            writeNodesToJson(actualNodes, "actualNodes.json", "actual", windowStartTime);
            writeNodesToJson(nextNodes, "nextNodes.json", "next", nextWindowStartTime);
        }
    }

    private static void handleClient(Socket socket) {

        try (

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true)

        ) {

            // Lee la primera línea del cliente.
            String request = in.readLine();

            if (request == null) {
                // Conexión cerrada sin datos.
                return;
            }

            String[] parts = request.split(" ");
            String command = parts[0];

            if ("REGISTER".equals(command) && parts.length == 2) {
                String clientIP = socket.getInetAddress().getHostAddress();
                String nodeAddress = clientIP + ":" + parts[1];
                synchronized (windowLock) {
                    if (!nextNodes.contains(nodeAddress)) {
                        nextNodes.add(nodeAddress);
                    }
                    writeNodesToJson(nextNodes, "nextNodes.json", "next", nextWindowStartTime);
                }
                out.println("REGISTERED NEXT " + nextWindowStartTime);
                return;
            }

            if ("GET_ACTIVE".equals(command)) {
                synchronized (windowLock) {
                    for (String node : actualNodes) {
                        out.println(node);
                    }
                }
                out.println("END");
                return;
            }

            out.println("ERROR");

        } catch (IOException e) {

            System.out.println("Error manejando registro");

        } finally {

            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void writeNodesToJson(List<String> nodes, String fileName, String window, long windowStart) {
        try (FileWriter file = new FileWriter(fileName)) {
            long uptime = (System.currentTimeMillis() - startTime) / 1000;
            String response =
                    "{\n" +
                    "\"window\": \"" + window + "\",\n" +
                    "\"window_start_epoch_ms\": " + windowStart + ",\n" +
                    "\"nodes\": " + nodes.size() + ",\n" +
                    "\"uptime_seconds\": " + uptime + ",\n" +
                    "\"status\": \"OK\",\n" +
                    "\"node_list\": [\n" +
                    nodes.stream().map(n -> "\"" + n + "\"")
                            .collect(java.util.stream.Collectors.joining(",\n")) +
                    "\n]\n" +
                    "}";
            file.write(response);
        } catch (IOException e) {
            System.out.println("Error escribiendo en el archivo json");
        }
    }

    private static void startHealthEndpoint(int port) throws IOException {

        // Crea servidor HTTP embebido.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Endpoint GET /health que retorna estado en JSON simple.
        server.createContext("/health", exchange -> {

            // Uptime en segundos.
            long uptime = (System.currentTimeMillis() - startTime) / 1000;

            String response;

            synchronized (windowLock) {
                response =
                        "{\n" +
                        "\"window\": \"actual\",\n" +
                        "\"nodes\": " + actualNodes.size() + ",\n" +
                        "\"uptime_seconds\": " + uptime + ",\n" +
                        "\"status\": \"OK\"\n" +
                        "}";
            }

            // Envía status 200 y longitud del cuerpo.
            exchange.sendResponseHeaders(200, response.length());

            // Escribe el cuerpo de la respuesta.
            OutputStream os = exchange.getResponseBody();

            os.write(response.getBytes());

            // Cierra el stream para finalizar la respuesta.
            os.close();
        });

        // Comienza a aceptar solicitudes HTTP.
        server.start();
    }
}
