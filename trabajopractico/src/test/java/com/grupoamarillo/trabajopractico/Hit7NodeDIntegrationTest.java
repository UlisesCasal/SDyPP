package com.grupoamarillo.trabajopractico;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para Hit-7: Sistema de inscripciones coordinado por NodeD.
 *
 * NodeD mantiene dos ventanas de tiempo:
 *   - actualNodes : nodos activos en la ventana corriente (visibles por GET_ACTIVE).
 *   - nextNodes   : nodos registrados para la próxima ventana.
 *
 * Protocolo TCP:
 *   REGISTER <puerto>  → el nodo queda anotado en nextNodes (respuesta: "REGISTERED NEXT <epoch_ms>").
 *   GET_ACTIVE         → devuelve los nodos de actualNodes seguidos de "END".
 *   Otro comando       → responde "ERROR".
 *
 * Cada 60 s NodeD rota: nextNodes → actualNodes y limpia nextNodes.
 * En los tests la rotación se dispara manualmente para evitar esperas reales.
 *
 * Las inscripciones se persisten en archivos JSON (nextNodes.json / actualNodes.json).
 */
class Hit7NodeDIntegrationTest {

    // ── Estado compartido del NodeD embebido ─────────────────────────────────

    private final List<String> actualNodes = new ArrayList<>();
    private final List<String> nextNodes   = new ArrayList<>();
    private final Object windowLock        = new Object();
    private long nextWindowStartTime;

    // Archivos JSON temporales creados para cada test
    private File actualJsonFile;
    private File nextJsonFile;

    // Socket del servidor embebido
    private ServerSocket serverSocket;
    private int          serverPort;

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        nextWindowStartTime = System.currentTimeMillis() + 60_000L;

        actualJsonFile = File.createTempFile("actualNodes", ".json");
        nextJsonFile   = File.createTempFile("nextNodes",   ".json");

        // Puerto libre asignado por el SO
        serverSocket = new ServerSocket(0);
        serverPort   = serverSocket.getLocalPort();

        Thread serverThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error en servidor embebido: " + e.getMessage());
                    }
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        actualJsonFile.delete();
        nextJsonFile.delete();
    }

    // ── Lógica de NodeD embebida (espejo de Hit-7/Servidor/NodeD.java) ───────

    /**
     * Maneja una conexión TCP entrante replicando exactamente el comportamiento de NodeD.
     */
    private void handleClient(Socket socket) {
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String request = in.readLine();
            if (request == null) return;

            String[] parts   = request.split(" ");
            String   command = parts[0];

            // ── REGISTER <puerto> ────────────────────────────────────────────
            if ("REGISTER".equals(command) && parts.length == 2) {
                String addr = socket.getInetAddress().getHostAddress() + ":" + parts[1];
                synchronized (windowLock) {
                    if (!nextNodes.contains(addr)) {
                        nextNodes.add(addr);
                    }
                    writeJson(nextNodes, nextJsonFile, "next", nextWindowStartTime);
                }
                out.println("REGISTERED NEXT " + nextWindowStartTime);
                return;
            }

            // ── GET_ACTIVE ───────────────────────────────────────────────────
            if ("GET_ACTIVE".equals(command)) {
                synchronized (windowLock) {
                    for (String node : actualNodes) {
                        out.println(node);
                    }
                }
                out.println("END");
                return;
            }

            // ── Comando desconocido ──────────────────────────────────────────
            out.println("ERROR");

        } catch (IOException e) {
            // Error de conexión; ignorar
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Simula el cambio de ventana de NodeD (normalmente programado cada 60 s).
     * nextNodes → actualNodes; nextNodes queda vacío; se actualizan los JSON.
     */
    private void rotateWindow() {
        synchronized (windowLock) {
            actualNodes.clear();
            actualNodes.addAll(nextNodes);
            nextNodes.clear();
            long currentWindowStart = nextWindowStartTime;
            nextWindowStartTime += 60_000L;
            writeJson(actualNodes, actualJsonFile, "actual", currentWindowStart);
            writeJson(nextNodes,   nextJsonFile,   "next",   nextWindowStartTime);
        }
    }

    /**
     * Escribe el estado de inscripciones en un archivo JSON.
     * Formato idéntico al empleado por el NodeD real.
     */
    private void writeJson(List<String> nodes, File file, String window, long windowStartEpoch) {
        try (FileWriter fw = new FileWriter(file)) {
            String nodeList = nodes.stream()
                    .map(n -> "\"" + n + "\"")
                    .collect(Collectors.joining(",\n"));
            fw.write("{\n"
                    + "\"window\": \"" + window + "\",\n"
                    + "\"window_start_epoch_ms\": " + windowStartEpoch + ",\n"
                    + "\"nodes\": " + nodes.size() + ",\n"
                    + "\"node_list\": [\n" + nodeList + "\n]\n"
                    + "}");
        } catch (IOException e) {
            fail("No se pudo escribir el archivo JSON: " + e.getMessage());
        }
    }

    // ── Helpers de red ───────────────────────────────────────────────────────

    /** Envía un comando al servidor y devuelve la primera línea de respuesta. */
    private String sendCommand(String command) throws Exception {
        try (
            Socket        s   = new Socket("127.0.0.1", serverPort);
            PrintWriter   out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))
        ) {
            out.println(command);
            return in.readLine();
        }
    }

    /** Envía GET_ACTIVE y recoge todas las líneas hasta "END". */
    private List<String> getActiveNodes() throws Exception {
        List<String> result = new ArrayList<>();
        try (
            Socket        s   = new Socket("127.0.0.1", serverPort);
            PrintWriter   out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))
        ) {
            out.println("GET_ACTIVE");
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                result.add(line);
            }
        }
        return result;
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    /**
     * T1: Al iniciar, la ventana activa debe estar vacía.
     */
    @Test
    void testGetActiveInitiallyEmpty() throws Exception {
        System.out.println("[Hit-7] T1: GET_ACTIVE vacío al inicio");

        List<String> active = getActiveNodes();

        assertTrue(active.isEmpty(),
                "Al iniciar, la ventana activa debe estar vacía");
    }

    /**
     * T2: La respuesta a REGISTER debe comenzar con "REGISTERED NEXT".
     */
    @Test
    void testRegisterRespondsWithRegisteredNext() throws Exception {
        System.out.println("[Hit-7] T2: REGISTER responde con REGISTERED NEXT <epoch>");

        String response = sendCommand("REGISTER 8100");

        assertNotNull(response, "El servidor no debe devolver null");
        assertTrue(response.startsWith("REGISTERED NEXT"),
                "La respuesta al registro debe comenzar con 'REGISTERED NEXT'");
    }

    /**
     * T3: El nodo registrado va a la ventana siguiente, NO a la activa.
     */
    @Test
    void testRegisteredNodeGoesToNextWindowNotActive() throws Exception {
        System.out.println("[Hit-7] T3: nodo registrado va a nextNodes, no a actualNodes");

        sendCommand("REGISTER 8101");

        List<String> active = getActiveNodes();
        assertTrue(active.isEmpty(),
                "El nodo recién registrado no debe ser visible en la ventana activa");

        synchronized (windowLock) {
            assertFalse(nextNodes.isEmpty(),
                    "El nodo debe figurar en nextNodes");
            assertTrue(nextNodes.get(0).endsWith(":8101"),
                    "nextNodes debe contener la dirección registrada con puerto 8101");
        }
    }

    /**
     * T4: Después de rotar la ventana, el nodo pasa de nextNodes a actualNodes.
     */
    @Test
    void testWindowRotationMovesNodesToActive() throws Exception {
        System.out.println("[Hit-7] T4: rotación mueve nextNodes → actualNodes");

        sendCommand("REGISTER 8102");

        assertTrue(getActiveNodes().isEmpty(),
                "Antes de rotar, la ventana activa debe estar vacía");

        rotateWindow();

        List<String> activeAfter = getActiveNodes();
        assertEquals(1, activeAfter.size(),
                "Después de rotar debe haber exactamente 1 nodo activo");
        assertTrue(activeAfter.get(0).endsWith(":8102"),
                "El nodo activo debe ser el que se registró (puerto 8102)");
    }

    /**
     * T5: Registrar el mismo nodo dos veces no genera duplicados.
     */
    @Test
    void testNoDuplicateRegistrations() throws Exception {
        System.out.println("[Hit-7] T5: no se permiten duplicados");

        sendCommand("REGISTER 8103");
        sendCommand("REGISTER 8103");   // mismo nodo

        synchronized (windowLock) {
            assertEquals(1, nextNodes.size(),
                    "El mismo nodo no debe registrarse dos veces");
        }
    }

    /**
     * T6: Varios nodos distintos se registran correctamente y quedan activos tras rotar.
     */
    @Test
    void testMultipleDistinctNodesRegistration() throws Exception {
        System.out.println("[Hit-7] T6: múltiples nodos distintos");

        sendCommand("REGISTER 8104");
        sendCommand("REGISTER 8105");
        sendCommand("REGISTER 8106");

        synchronized (windowLock) {
            assertEquals(3, nextNodes.size(),
                    "Deben registrarse 3 nodos distintos");
        }

        rotateWindow();

        List<String> active = getActiveNodes();
        assertEquals(3, active.size(),
                "Los 3 nodos deben estar activos tras la rotación");
    }

    /**
     * T7: Los nodos de la próxima ventana no son visibles hasta que se rota.
     */
    @Test
    void testRegisteredNodesNotVisibleBeforeRotation() throws Exception {
        System.out.println("[Hit-7] T7: inscripciones de próxima ventana no visibles aún");

        sendCommand("REGISTER 8107");
        sendCommand("REGISTER 8108");

        assertTrue(getActiveNodes().isEmpty(),
                "Los nodos registrados para la próxima ventana no deben verse en la actual");

        rotateWindow();

        assertEquals(2, getActiveNodes().size(),
                "Tras rotar, ambos nodos deben estar activos");
    }

    /**
     * T8: Los nodos que se registran después de la rotación van a la NUEVA ventana siguiente,
     *     no a la que ya está activa.
     */
    @Test
    void testRegistrationsAfterRotationGoToNewNextWindow() throws Exception {
        System.out.println("[Hit-7] T8: registros post-rotación van a la nueva ventana siguiente");

        sendCommand("REGISTER 8109");
        rotateWindow();   // 8109 → activo

        sendCommand("REGISTER 8110");   // 8110 → próxima ventana

        List<String> active = getActiveNodes();
        assertEquals(1, active.size(), "Solo 8109 debe estar activo");
        assertTrue(active.get(0).endsWith(":8109"),
                "El nodo activo debe ser 8109");

        rotateWindow();   // 8110 → activo; 8109 ya no está

        List<String> activeAfterSecond = getActiveNodes();
        assertEquals(1, activeAfterSecond.size(),
                "Solo 8110 debe estar activo en la segunda ventana");
        assertTrue(activeAfterSecond.get(0).endsWith(":8110"),
                "El nodo activo debe ser 8110");
    }

    /**
     * T9: Un comando desconocido debe devolver "ERROR".
     */
    @Test
    void testInvalidCommandReturnsError() throws Exception {
        System.out.println("[Hit-7] T9: comando inválido → ERROR");

        String response = sendCommand("COMANDO_INVALIDO");

        assertEquals("ERROR", response,
                "Un comando desconocido debe retornar ERROR");
    }

    /**
     * T10: Al registrar un nodo se escribe el archivo JSON de nextNodes con contenido válido.
     */
    @Test
    void testJsonFileWrittenOnRegistration() throws Exception {
        System.out.println("[Hit-7] T10: se escribe nextNodes.json al registrar");

        sendCommand("REGISTER 8111");

        // Breve pausa para que el hilo servidor haya escrito el archivo
        Thread.sleep(200);

        assertTrue(nextJsonFile.length() > 0,
                "El archivo nextNodes.json debe tener contenido tras el registro");

        String content = new String(Files.readAllBytes(nextJsonFile.toPath()));
        assertTrue(content.contains("\"window\": \"next\""),
                "El JSON debe indicar la ventana 'next'");
        assertTrue(content.contains("\"nodes\": 1"),
                "El JSON debe reflejar 1 nodo registrado");
        assertTrue(content.contains(":8111"),
                "El JSON debe contener la dirección del nodo registrado");
    }

    /**
     * T11: Tras la rotación, actualNodes.json y nextNodes.json se actualizan correctamente.
     */
    @Test
    void testJsonFilesUpdatedOnRotation() throws Exception {
        System.out.println("[Hit-7] T11: archivos JSON actualizados al rotar ventana");

        sendCommand("REGISTER 8112");
        rotateWindow();

        String actualContent = new String(Files.readAllBytes(actualJsonFile.toPath()));
        String nextContent   = new String(Files.readAllBytes(nextJsonFile.toPath()));

        // actualNodes.json debe reflejar el nodo que pasó a activo
        assertTrue(actualContent.contains("\"window\": \"actual\""),
                "actualNodes.json debe tener window='actual'");
        assertTrue(actualContent.contains(":8112"),
                "actualNodes.json debe contener el nodo que pasó a activo");
        assertTrue(actualContent.contains("\"nodes\": 1"),
                "actualNodes.json debe indicar 1 nodo activo");

        // nextNodes.json debe estar vacío tras la rotación
        assertTrue(nextContent.contains("\"window\": \"next\""),
                "nextNodes.json debe tener window='next'");
        assertTrue(nextContent.contains("\"nodes\": 0"),
                "nextNodes.json debe indicar 0 nodos (ventana siguiente vacía)");
    }

    /**
     * T12: Comunicación P2P entre nodos C usando NodeD como directorio.
     *      Flujo completo: registrar → rotar → GET_ACTIVE → conectarse al par → ACK.
     */
    @Test
    void testNodeCPeerCommunicationViaNodeD() throws Exception {
        System.out.println("[Hit-7] T12: comunicación P2P de NodeC via NodeD");

        // Levantar un mini NodeC-servidor que responde con "ACK <mensaje>"
        ServerSocket peerServer = new ServerSocket(0);
        int          peerPort   = peerServer.getLocalPort();

        Thread peerThread = new Thread(() -> {
            try (
                Socket        peer = peerServer.accept();
                BufferedReader in  = new BufferedReader(new InputStreamReader(peer.getInputStream()));
                PrintWriter    out = new PrintWriter(peer.getOutputStream(), true)
            ) {
                String msg = in.readLine();
                out.println("ACK " + msg);
            } catch (IOException e) {
                // ignorar si el socket ya está cerrado
            }
        });
        peerThread.setDaemon(true);
        peerThread.start();

        // Registrar el peer en NodeD y rotarlo a activo
        sendCommand("REGISTER " + peerPort);
        rotateWindow();

        // Consultar la lista activa
        List<String> active = getActiveNodes();
        assertEquals(1, active.size(),
                "Debe haber exactamente un nodo activo después de la rotación");

        // Parsear IP:puerto del nodo activo
        String[] addrParts   = active.get(0).split(":");
        int      resolvedPort = Integer.parseInt(addrParts[addrParts.length - 1]);

        // Conectarse al peer y verificar el intercambio ACK
        try (
            Socket        s   = new Socket("127.0.0.1", resolvedPort);
            PrintWriter   out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))
        ) {
            out.println("Hola desde test");
            String ack = in.readLine();
            assertEquals("ACK Hola desde test", ack,
                    "El peer (NodeC) debe responder con 'ACK ' + mensaje original");
        } finally {
            peerServer.close();
        }
    }
}
