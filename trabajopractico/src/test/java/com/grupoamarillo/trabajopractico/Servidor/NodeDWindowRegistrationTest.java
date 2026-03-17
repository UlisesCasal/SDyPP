package com.grupoamarillo.trabajopractico.Servidor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class NodeDWindowRegistrationTest {
    @BeforeAll
    static void startNodeD() throws Exception {
        resetNodeDState();
        if (!isPortOpen(9000)) {
            Thread serverThread = new Thread(() -> {
                try {
                    NodeD.main(new String[0]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            waitForPort(9000, Duration.ofSeconds(5));
        }
    }

    @Test
    void registroEntraASiguienteVentanaYPasaAActivaTrasRotacion() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            System.out.println("[TEST] Inicio: registro entra a siguiente ventana");
            resetNodeDState();

            String registerResponse = sendAndReadFirstLine("REGISTER 50111");
            assertNotNull(registerResponse);
            assertTrue(registerResponse.startsWith("REGISTERED "));
            System.out.println("[TEST] Respuesta REGISTER: " + registerResponse);

            List<String> activeBefore = listActive();
            assertTrue(activeBefore.stream().noneMatch(line -> line.endsWith(":50111")));
            System.out.println("[TEST] Activos antes de rotar: " + activeBefore);

            forceWindowRolloverNow();

            List<String> activeAfter = listActive();
            assertTrue(activeAfter.stream().anyMatch(line -> line.endsWith(":50111")));
            System.out.println("[TEST] Activos después de rotar: " + activeAfter);

            File jsonFile = new File("inscripciones-nodeD.json");
            assertTrue(jsonFile.exists());
            String json = Files.readString(jsonFile.toPath());
            assertTrue(json.contains("\"activeNodes\""));
            assertTrue(json.contains("\"nextWindowNodes\""));
            assertTrue(json.contains(":50111"));
            System.out.println("[TEST] JSON de estado:\n" + json);
        });
    }

    @Test
    void dosNodosActivosEnMismaVentanaSeComunican() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            System.out.println("[TEST] Inicio: dos nodos activos se comunican");
            resetNodeDState();
            MiniNode node1 = new MiniNode();
            MiniNode node2 = new MiniNode();
            try {
                node1.start();
                node2.start();
                System.out.println("[TEST] Node1 port=" + node1.port() + ", Node2 port=" + node2.port());

                String response1 = sendAndReadFirstLine("REGISTER " + node1.port());
                String response2 = sendAndReadFirstLine("REGISTER " + node2.port());
                assertNotNull(response1);
                assertNotNull(response2);
                assertTrue(response1.startsWith("REGISTERED "));
                assertTrue(response2.startsWith("REGISTERED "));
                System.out.println("[TEST] REGISTER responses: " + response1 + " | " + response2);

                forceWindowRolloverNow();

                List<String> active = listActive();
                assertTrue(active.stream().anyMatch(line -> line.endsWith(":" + node1.port())));
                assertTrue(active.stream().anyMatch(line -> line.endsWith(":" + node2.port())));
                System.out.println("[TEST] Activos en ventana: " + active);

                node1.connectToPeers(active);
                node2.connectToPeers(active);

                waitUntil(() -> node1.messagesReceived() > 0 && node2.messagesReceived() > 0, Duration.ofSeconds(3));
                System.out.println("[TEST] Mensajes recibidos node1=" + node1.messagesReceived() + ", node2=" + node2.messagesReceived());
            } finally {
                node1.stop();
                node2.stop();
            }
        });
    }

    @Test
    void healthReflejaNodosActivosConectados() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            System.out.println("[TEST] Inicio: health refleja nodos activos");
            resetNodeDState();
            MiniNode node1 = new MiniNode();
            MiniNode node2 = new MiniNode();
            try {
                node1.start();
                node2.start();
                System.out.println("[TEST] Node1 port=" + node1.port() + ", Node2 port=" + node2.port());

                sendAndReadFirstLine("REGISTER " + node1.port());
                sendAndReadFirstLine("REGISTER " + node2.port());
                forceWindowRolloverNow();

                String health = readHealth();
                assertTrue(health.contains("\"active_nodes\": 2"));
                assertTrue(health.contains("\"status\": \"OK\""));
                System.out.println("[TEST] /health response:\n" + health);
            } finally {
                node1.stop();
                node2.stop();
            }
        });
    }

    private static String sendAndReadFirstLine(String command) throws Exception {
        try (Socket socket = new Socket("localhost", 9000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println(command);
            return in.readLine();
        }
    }

    private static List<String> listActive() throws Exception {
        try (Socket socket = new Socket("localhost", 9000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            socket.setSoTimeout(200);
            out.println("LIST_ACTIVE");
            String header = in.readLine();
            assertNotNull(header);
            assertTrue(header.startsWith("WINDOW "));

            List<String> lines = new ArrayList<>();
            while (true) {
                try {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    lines.add(line);
                } catch (SocketTimeoutException ignored) {
                    break;
                }
            }
            return lines;
        }
    }

    private static String readHealth() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/health").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(1000);
        conn.setReadTimeout(1000);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static void resetNodeDState() throws Exception {
        Field activeField = NodeD.class.getDeclaredField("activeNodes");
        Field nextField = NodeD.class.getDeclaredField("nextWindowNodes");
        activeField.setAccessible(true);
        nextField.setAccessible(true);
        activeField.set(null, new ArrayList<String>());
        nextField.set(null, new ArrayList<String>());

        Method initialize = NodeD.class.getDeclaredMethod("initializeWindows");
        initialize.setAccessible(true);
        initialize.invoke(null);

        Method save = NodeD.class.getDeclaredMethod("saveState");
        save.setAccessible(true);
        save.invoke(null);
    }

    private static void forceWindowRolloverNow() throws Exception {
        Field nextWindowField = NodeD.class.getDeclaredField("nextWindowStartMs");
        nextWindowField.setAccessible(true);
        nextWindowField.setLong(null, System.currentTimeMillis() - 1);

        Method advance = NodeD.class.getDeclaredMethod("advanceWindowsToNow");
        advance.setAccessible(true);
        advance.invoke(null);

        Method save = NodeD.class.getDeclaredMethod("saveState");
        save.setAccessible(true);
        save.invoke(null);
    }

    private static boolean isPortOpen(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitForPort(int port, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isPortOpen(port)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("No se pudo iniciar NodeD en puerto " + port);
    }

    private static void waitUntil(Check check, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("No se cumplió la condición dentro del timeout");
    }

    @FunctionalInterface
    private interface Check {
        boolean ok() throws Exception;
    }

    private static class MiniNode {
        private ServerSocket serverSocket;
        private final List<String> messages = new ArrayList<>();

        void start() throws Exception {
            serverSocket = new ServerSocket(0);
            Thread thread = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try (Socket socket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        String msg = in.readLine();
                        if (msg != null) {
                            synchronized (messages) {
                                messages.add(msg);
                            }
                            out.println("ACK " + msg);
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        int messagesReceived() {
            synchronized (messages) {
                return messages.size();
            }
        }

        void connectToPeers(List<String> peers) {
            for (String peer : peers) {
                String[] parts = peer.split(":");
                if (parts.length != 2) {
                    continue;
                }
                int peerPort = Integer.parseInt(parts[1]);
                if (peerPort == port()) {
                    continue;
                }
                try (Socket socket = new Socket(parts[0], peerPort);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    out.println("hola desde " + port());
                    in.readLine();
                } catch (Exception ignored) {
                }
            }
        }

        void stop() throws Exception {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }
}
