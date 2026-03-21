package com.grupoamarillo.trabajopractico.Hit6.Cliente;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.grupoamarillo.trabajopractico.Hit6.Message;

import tools.jackson.databind.ObjectMapper;

public class NodeC {

    private static final ObjectMapper mapper = new ObjectMapper();

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

        // HILO SERVIDOR
        Thread serverThread = new Thread(() -> startServer(server));
        serverThread.start();

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
            String json = in.readLine();
            if (json == null) return;

            // DESERIALIZAR
            Message message = mapper.readValue(json, Message.class);

            System.out.println("Mensaje recibido: " + message.msg +
                    " desde " + message.from);

            // RESPUESTA
            Message response = new Message(
                    "ACK " + message.msg,
                    String.valueOf(socket.getLocalPort())
            );

            String responseJson = mapper.writeValueAsString(response);
            out.println(responseJson);

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
            System.out.println("RegistrÃ¡ndose en NodeD " + host + ":" + port);

            // MENSAJE JSON DE REGISTRO
            Message registerMsg = new Message();
            registerMsg.action = "REGISTER";
            registerMsg.port = myPort;

            String json = mapper.writeValueAsString(registerMsg);
            out.println(json);

            String line;

            // RECIBIR NODOS
            while ((line = in.readLine()) != null) {

                Message node = mapper.readValue(line, Message.class);

                String ip = node.ip;
                int peerPort = node.port;

                if (peerPort == myPort) continue;

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

                // MENSAJE JSON
                Message message = new Message(
                        "Hola",
                        String.valueOf(socket.getLocalPort())
                );

                String json = mapper.writeValueAsString(message);
                out.println(json);

                String response = in.readLine();

                Message responseMsg = mapper.readValue(response, Message.class);

                System.out.println("Respuesta: " + responseMsg.msg +
                        " desde " + responseMsg.from);

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