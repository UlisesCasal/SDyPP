package com.grupoamarillo.trabajopractico.Hit6.Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeC {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Uso: java NodeC IP_NODE_D PUERTO_NODE_D");
            return;
        }

        String registryIP = args[0];
        int registryPort = Integer.parseInt(args[1]);

        // PUERTO ALEATORIO
        ServerSocket server = new ServerSocket(0);
        int listenPort = server.getLocalPort();

        System.out.println("Nodo C escuchando en puerto " + listenPort);

        // HILO SERVIDOR (igual que HIT4)
        Thread server_thread = new Thread(() -> startServer(server));
        server_thread.start();

        Thread.sleep(1000);

        // REGISTRO EN NODE D
        registerWithNodeD(registryIP, registryPort, listenPort);
    }

    private static void startServer(ServerSocket server) {

        try {

            while (true) {

                Socket client = server.accept();

                new Thread(() -> handleConnection(client)).start();
            }

        } catch (IOException e) {

            System.out.println("Error servidor: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket socket) {

        try (

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true)

        ) {

            String msg = in.readLine();

            if (msg == null) {
                return;
            }

            System.out.println("Mensaje recibido: " + msg);

            out.println("ACK " + msg);

        } catch (IOException e) {

            System.out.println("Cliente desconectado");

        } finally {

            try {

                socket.close();

                System.out.println("Escuchando en Puerto " +
                        socket.getLocalPort() + "...");

            } catch (IOException ignored) {}
        }
    }

    private static void registerWithNodeD(String host, int port, int myPort) {

        try (

            Socket socket = new Socket(host, port);

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))

        ) {

            System.out.println("Registrándose en NodeD " + host + ":" + port);

            // ENVIA EL REGISTRO
            out.println("REGISTER " + myPort);

            String line;

            // RECIBE LISTA DE NODOS
            while ((line = in.readLine()) != null) {

                String[] parts = line.split(":");

                String ip = parts[0];
                int peerPort = Integer.parseInt(parts[1]);

                // EVITAR CONECTARSE A SI MISMO
                if (peerPort == myPort) {
                    continue;
                }

                new Thread(() -> startClient(ip, peerPort)).start();
            }

        } catch (IOException e) {

            System.out.println("No se pudo conectar al NodeD");
        }
    }

    private static void startClient(String host, int port) {

        while (true) {

            try (

                Socket socket = new Socket(host, port);

                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))

            ) {

                System.out.println("Conectado a " + host + ":" + port);

                String msg = "Hola desde " + socket.getLocalPort();

                out.println(msg);

                String response = in.readLine();

                System.out.println("Respuesta: " + response);

                System.out.println("Cliente cerrado");

                break;

            } catch (IOException e) {

                System.out.println("No se pudo conectar a "
                        + host + ":" + port + " Reintentando...");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}