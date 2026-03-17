package com.grupoamarillo.trabajopractico.Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NodeC {
    private static final String REGISTRY_HOST = "localhost";
    private static final int REGISTRY_PORT = 9000;

    /**
     * Punto de entrada del programa NodeC.
     *
     * Usa NodeD fijo en localhost:9000.
     * 
     * Flujo:
     * - Crea un ServerSocket en un puerto aleatorio (0) para escuchar conexiones entrantes.
     * - Lanza un hilo servidor que acepta clientes y responde con ACK.
     * - Tras 1 s de margen, se registra en NodeD enviando su puerto.
     * - NodeD le devuelve una lista de pares (IP:puerto) con los que debe conectarse.
     * - Por cada par (excepto él mismo) lanza un hilo cliente que envía un saludo y espera respuesta.
     */
    public static void main(String[] args) throws Exception {
        // Puerto local de escucha de este nodo C.
        ServerSocket server = new ServerSocket(0);
        int listenPort = server.getLocalPort();
        System.out.println("Nodo C escuchando en puerto " + listenPort);

        // Hilo servidor para recibir conexiones de otros C.
        Thread serverThread = new Thread(() -> startServer(server));
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(1000);

        // Registro para la siguiente ventana y luego consulta periódica de activos.
        registerWithNodeD(REGISTRY_HOST, REGISTRY_PORT, listenPort);
        pollActiveNodes(REGISTRY_HOST, REGISTRY_PORT, listenPort);
    }

    /**
     * Bucle infinito que acepta conexiones entrantes.
     * Por cada cliente crea un hilo que llama a handleConnection.
     */
    private static void startServer(ServerSocket server) {
        try {
            while (true) {
                Socket client = server.accept();           // Bloquea hasta nueva conexión
                new Thread(() -> handleConnection(client)).start();
            }
        } catch (IOException e) {
            System.out.println("Error servidor: " + e.getMessage());
        }
    }

    /**
     * Atiende a un cliente conectado:
     * - Lee un mensaje de texto (línea completa).
     * - Imprime el mensaje por consola.
     * - Responde "ACK " + mensaje.
     * - Cierra el socket y avisa que vuelve a escuchar.
     */
    private static void handleConnection(Socket socket) {
        // Try-with-resources: cierra automáticamente in y out
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), true)) {

            String msg = in.readLine();
            if (msg == null) return;          // Cliente cerró conexión prematuramente

            System.out.println("Mensaje recibido: " + msg);
            out.println("ACK " + msg);        // Envía acuse

        } catch (IOException e) {
            System.out.println("Cliente desconectado");
        } finally {
            // Cierra socket y muestra puerto local para confirmar que sigue escuchando
            try {
                socket.close();
                System.out.println("Escuchando en Puerto " +
                        socket.getLocalPort() + "...");
            } catch (IOException ignored) {}
        }
    }

    // Registro del nodo C para la ventana siguiente.
    private static void registerWithNodeD(String host, int port, int myPort) {
        while (true) {
            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("REGISTER " + myPort);
                String response = in.readLine();
                if (response != null && response.startsWith("REGISTERED")) {
                    System.out.println("Registro en D: " + response);
                    return;
                }
                System.out.println("Respuesta inválida de NodeD al registrar");
            } catch (IOException e) {
                System.out.println("Esperando NodeD para registrar...");
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Consulta en loop los nodos activos de la ventana actual.
    private static void pollActiveNodes(String host, int port, int myPort) throws InterruptedException {
        long currentWindow = -1L;
        List<String> alreadyContacted = new ArrayList<>();

        while (true) {
            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("LIST_ACTIVE");
                String header = in.readLine();

                if (header == null || !header.startsWith("WINDOW ")) {
                    Thread.sleep(3000);
                    continue;
                }

                long window = Long.parseLong(header.substring("WINDOW ".length()));

                // Cuando cambia la ventana, reiniciamos la lista de contactos.
                if (window != currentWindow) {
                    currentWindow = window;
                    alreadyContacted.clear();
                    System.out.println("Ventana activa: " + currentWindow);
                }

                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length != 2) {
                        continue;
                    }

                    String ip = parts[0];
                    int peerPort = Integer.parseInt(parts[1]);

                    if (peerPort == myPort) {
                        continue;
                    }

                    // Sin HashSet: evitamos duplicados con List.contains.
                    if (!alreadyContacted.contains(line)) {
                        alreadyContacted.add(line);
                        new Thread(() -> startClient(ip, peerPort)).start();
                    }
                }

            } catch (IOException e) {
                System.out.println("No se pudo consultar activos en NodeD");
            }

            Thread.sleep(3000);
        }
    }

    /**
     * Cliente persistente que intenta conectarse a un par dado:
     * - Envia "Hola desde <puertoLocal>".
     * - Espera respuesta (ACK) y la imprime.
     * - Cierra la conexión y termina el hilo.
     * 
     * Si falla la conexión, espera 2 s y reintenta hasta un máximo de intentos.
     */
    private static void startClient(String host, int port) {
        int attempts = 0;
        int maxAttempts = 5;
        while (attempts < maxAttempts) {
            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(
                         socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Conectado a " + host + ":" + port);

                // Envía saludo con puerto local para identificarse
                String msg = "Hola desde " + socket.getLocalPort();
                out.println(msg);

                // Lee respuesta del servidor
                String response = in.readLine();
                System.out.println("Respuesta: " + response);

                System.out.println("Cliente cerrado");
                return;

            } catch (IOException e) {
                attempts++;
                System.out.println("No se pudo conectar a "
                        + host + ":" + port + " Reintentando...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
        System.out.println("Se cancela conexión a " + host + ":" + port + " tras " + maxAttempts + " intentos");
    }
}
