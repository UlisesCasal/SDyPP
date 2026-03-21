package com.grupoamarillo.trabajopractico.Hit7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NodeC {

    public static void main(String[] args) throws Exception {
        // Este nodo necesita 2 argumentos al arrancar:
        // 1) IP de NodeD (registro)
        // 2) Puerto TCP de NodeD
        if (args.length != 2) {
            System.out.println("Uso: java NodeC IP_NODE_D PUERTO_NODE_D");
            return;
        }
        // Dirección de NodeD donde este nodo se va a registrar.
        String registryIP = args[0];
        // Puerto de NodeD.
        int registryPort = Integer.parseInt(args[1]);

        // Abre un ServerSocket en puerto 0:
        // el SO asigna un puerto libre automáticamente.
        ServerSocket server = new ServerSocket(0);
        // Guarda el puerto real asignado para poder anunciarlo a NodeD.
        int listenPort = server.getLocalPort();

        System.out.println("Nodo C escuchando en puerto " + listenPort);

        // Levanta el lado servidor de este nodo en un hilo separado.
        // Así puede aceptar conexiones entrantes mientras hace otras tareas.
        Thread server_thread = new Thread(() -> startServer(server));
        server_thread.start();

        // Espera corta para asegurar que el server ya está escuchando.
        Thread.sleep(1000);

        registerWithNodeD(registryIP, registryPort, listenPort);
        startActivePeerSync(registryIP, registryPort, listenPort);
    }

    private static void startServer(ServerSocket server) {

        try {

            // Bucle principal del servidor local:
            // acepta clientes y delega cada conexión a un hilo.
            while (true) {

                Socket client = server.accept();

                // Un hilo por conexión para no bloquear nuevas aceptaciones.
                new Thread(() -> handleConnection(client)).start();
            }

        } catch (IOException e) {

            System.out.println("Error servidor: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket socket) {

        try (

            // Lee texto línea por línea desde el cliente remoto.
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // Escribe respuesta al cliente remoto con autoflush.
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true)

        ) {

            // Espera un mensaje de una línea.
            String msg = in.readLine();

            if (msg == null) {
                // Si la conexión se cerró sin datos, termina.
                return;
            }

            System.out.println("Mensaje recibido: " + msg);

            // Responde con ACK y repite el contenido recibido.
            out.println("ACK " + msg);

        } catch (IOException e) {

            System.out.println("Cliente desconectado");

        } finally {

            try {

                socket.close();

                // getLocalPort() es el puerto local de este lado del socket.
                System.out.println("Escuchando en Puerto " +
                        socket.getLocalPort() + "...");

            } catch (IOException ignored) {}
        }
    }

    private static void registerWithNodeD(String host, int port, int myPort) {
        try (
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            System.out.println("Registrándose en NodeD " + host + ":" + port);
            out.println("REGISTER " + myPort);
            String ack = in.readLine();
            System.out.println("Respuesta registro: " + ack);
        } catch (IOException e) {
            System.out.println("No se pudo conectar al NodeD");
        }
    }

    private static void startActivePeerSync(String host, int port, int myPort) {
        Thread syncThread = new Thread(() -> {
            while (true) {
                List<String> peers = fetchActivePeers(host, port);
                for (String peer : peers) {
                    String[] parts = peer.split(":");
                    if (parts.length != 2) {
                        continue;
                    }
                    String peerIp = parts[0];
                    int peerPort = Integer.parseInt(parts[1]);
                    if (peerPort == myPort) {
                        continue;
                    }
                    new Thread(() -> startClient(peerIp, peerPort)).start();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        });
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private static List<String> fetchActivePeers(String host, int port) {
        List<String> peers = new ArrayList<>();
        try (
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("GET_ACTIVE");
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) {
                    break;
                }
                peers.add(line);
            }
        } catch (IOException e) {
            System.out.println("No se pudo consultar nodos activos en NodeD");
        }
        return peers;
    }

    private static void startClient(String host, int port) {
        
        // Reintenta hasta lograr conectarse al peer.
        while (true) {

            try (

                // Conexión saliente al nodo peer.
                Socket socket = new Socket(host, port);

                // Stream para enviar un mensaje al peer.
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true);

                // Stream para leer respuesta del peer.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))

            ) {

                System.out.println("Conectado a " + host + ":" + port);

                // Mensaje de prueba; usa puerto local de esta conexión cliente.
                String msg = "Hola desde " + socket.getLocalPort();

                out.println(msg);

                // Espera ACK del peer.
                String response = in.readLine();

                System.out.println("Respuesta: " + response);

                System.out.println("Cliente cerrado");

                // Sale del bucle cuando la comunicación fue exitosa.
                break;

            } catch (IOException e) {

                System.out.println("No se pudo conectar a "
                        + host + ":" + port + " Reintentando...");

                try {
                    // Espera antes de reintentar para evitar ciclo agresivo.
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
