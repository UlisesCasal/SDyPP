package com.grupoamarillo.trabajopractico.Servidor;

import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NodeD {

    // Lock para proteger estado compartido.
    private static final Object LOCK = new Object();

    // Duración de la ventana (1 minuto).
    private static final long WINDOW_MS = 60_000L;

    // Archivo JSON donde se persisten las inscripciones.
    private static final String STATE_FILE = "inscripciones-nodeD.json";
    private static final int TCP_PORT = 9000;
    private static final int HTTP_PORT = 8080;

    // Nodos activos de la ventana actual.
    private static List<String> activeNodes = new ArrayList<>();

    // Nodos registrados para la siguiente ventana.
    private static List<String> nextWindowNodes = new ArrayList<>();

    // Métrica de uptime para /health.
    private static long startTime = System.currentTimeMillis();

    // Tiempos de ventana.
    private static long currentWindowStartMs;
    private static long nextWindowStartMs;

    /**
     * Punto de entrada principal de NodeD.
     * Configura e inicia dos servicios en paralelo:
     * 1. Un endpoint HTTP /health (puerto 8080) que expone métricas del servidor.
     * 2. Un servidor TCP (puerto 9000) que acepta registros de nodos C.
     * Cada conexión TCP se atiende en un hilo separado.
     */
    public static void main(String[] args) throws Exception {
        // Inicializamos ventana actual y siguiente redondeando al minuto.
        initializeWindows();

        // Guardamos estado inicial y arrancamos tareas auxiliares.
        saveState();
        startWindowRotator();
        try {
            startHealthEndpoint(HTTP_PORT);
        } catch (BindException e) {
            System.out.println("No se pudo iniciar /health en puerto " + HTTP_PORT + ". Ya está en uso.");
            return;
        }

        try (ServerSocket server = new ServerSocket(TCP_PORT)) {
            System.out.println("NodeD escuchando en puerto " + TCP_PORT);
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handleCommand(socket)).start();
            }
        } catch (BindException e) {
            System.out.println("No se pudo iniciar TCP en puerto " + TCP_PORT + ". Ya está en uso.");
        }
    }

    // Calcula inicio de ventana actual y próxima.
    private static void initializeWindows() {
        long now = System.currentTimeMillis();
        long minuteStart = (now / WINDOW_MS) * WINDOW_MS;
        currentWindowStartMs = minuteStart;
        nextWindowStartMs = minuteStart + WINDOW_MS;
    }

    // Hilo que cada minuto mueve nextWindowNodes -> activeNodes.
    private static void startWindowRotator() {
        Thread rotator = new Thread(() -> {
            while (true) {
                try {
                    long wait = nextWindowStartMs - System.currentTimeMillis();
                    if (wait > 0) {
                        Thread.sleep(wait);
                    }
                    synchronized (LOCK) {
                        advanceWindowsToNow();
                        saveState();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        rotator.setDaemon(true);
        rotator.start();
    }

    // Maneja comandos REGISTER y LIST_ACTIVE.
    /**
     * Maneja cada conexión TCP entrante de un nodo C.
     *
     * Protocolo de texto plano (una línea por request/response):
     * 1) REGISTER <puerto>
     *    - Extrae el puerto que el nodo C abrió para recibir conexiones.
     *    - Construye la clave “IP:puerto” usando la IP remota del socket.
     *    - Adquiere el candado (LOCK) para garantizar exclusión mutua.
     *    - Si la clave no existe en nextWindowNodes, la inserta (evita duplicados).
     *    - Persiste el estado en disco vía saveState().
     *    - Responde: “REGISTERED <nextWindowStartMs>” para que el emisor sepa
     *      cuándo comenzará la ventana en la que su registro será efectivo.
     *
     * 2) LIST_ACTIVE
     *    - Adquiere el candado para leer activeNodes de forma consistente.
     *    - Primero envía “WINDOW <currentWindowStartMs>” para indicar a qué
     *      ventana de 1 minuto pertenecen los nodos que siguen.
     *    - Luego escribe una línea por cada nodo activo.
     *    - El cliente cierra la conexión cuando vea EOF o línea vacía.
     *
     * 3) Cualquier otro comando
     *    - Responde “ERROR UNKNOWN_COMMAND”.
     *
     * Gestión de errores:
     * - Si REGISTER llega mal formado (falta puerto o está vacío) devuelve
     *   “ERROR BAD_REQUEST”.
     * - IOException durante la lectura/escritura se loguea y se cierra el socket.
     * - El bloque finally garantiza que el socket se cierre siempre.
     *
     * Nota de concurrencia: solo se adquiere el candado cuando se toca
     * estado compartido (nextWindowNodes o activeNodes); el resto del
     * procesamiento (parseo, construcción de respuestas) se hace sin bloqueo
     * para minimizar la contención.
     */
    private static void handleCommand(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String request = in.readLine();
            if (request == null) {
                return;
            }

            if (request.startsWith("REGISTER ")) {
                String[] parts = request.split(" ", 2);
                if (parts.length != 2 || parts[1].isBlank()) {
                    out.println("ERROR BAD_REQUEST");
                    return;
                }

                String node = socket.getInetAddress().getHostAddress() + ":" + parts[1].trim();
                synchronized (LOCK) {
                    advanceWindowsToNow();
                    if (!nextWindowNodes.contains(node)) {
                        nextWindowNodes.add(node);
                    }
                    saveState();
                    out.println("REGISTERED " + nextWindowStartMs);
                }
                return;
            }

            if ("LIST_ACTIVE".equals(request)) {
                synchronized (LOCK) {
                    advanceWindowsToNow();
                    out.println("WINDOW " + currentWindowStartMs);
                    for (String node : activeNodes) {
                        out.println(node);
                    }
                }
                return;
            }

            out.println("ERROR UNKNOWN_COMMAND");
        } catch (IOException e) {
            System.out.println("Error manejando conexión: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void advanceWindowsToNow() {
        long now = System.currentTimeMillis();
        while (now >= nextWindowStartMs) {
            activeNodes = new ArrayList<>(nextWindowNodes);
            nextWindowNodes.clear();
            currentWindowStartMs = nextWindowStartMs;
            nextWindowStartMs = currentWindowStartMs + WINDOW_MS;
        }
    }

    // Persiste el estado actual en JSON de texto plano.
    private static void saveState() {
        try (FileWriter writer = new FileWriter(STATE_FILE, false)) {
            writer.write(toJson());
        } catch (IOException e) {
            System.out.println("No se pudo guardar JSON: " + e.getMessage());
        }
    }

    private static String toJson() {
        return "{\n" +
                "  \"currentWindowStartMs\": " + currentWindowStartMs + ",\n" +
                "  \"nextWindowStartMs\": " + nextWindowStartMs + ",\n" +
                "  \"activeNodes\": " + toJsonArray(activeNodes) + ",\n" +
                "  \"nextWindowNodes\": " + toJsonArray(nextWindowNodes) + "\n" +
                "}";
    }

    private static String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(list.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void startHealthEndpoint(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> {
            long uptime = (System.currentTimeMillis() - startTime) / 1000;
            String response;

            synchronized (LOCK) {
                response = "{\n" +
                        "\"active_nodes\": " + activeNodes.size() + ",\n" +
                        "\"next_window_nodes\": " + nextWindowNodes.size() + ",\n" +
                        "\"current_window_start_ms\": " + currentWindowStartMs + ",\n" +
                        "\"next_window_start_ms\": " + nextWindowStartMs + ",\n" +
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
