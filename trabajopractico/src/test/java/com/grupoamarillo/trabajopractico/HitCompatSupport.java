package com.grupoamarillo.trabajopractico;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

class ClienteA {
    public static void main(String[] args) throws InterruptedException {
        String host = "localhost";
        int puerto = 5001;
        while (true) {
            try (
                    Socket socket = new Socket(host, puerto);
                    PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                salida.println("Hola B, soy A");
                String respuesta = entrada.readLine();
                System.out.println("Servidor responde: " + respuesta);
                break;
            } catch (IOException e) {
                System.out.println("Error al conectar con el servidor: " + e.getMessage());
                Thread.sleep(200);
            }
        }
    }
}

class ServidorB {
    public static void main(String[] args) {
        int puerto = 5001;
        while (true) {
            try (ServerSocket servidor = new ServerSocket(puerto)) {
                while (true) {
                    System.out.println("Servidor B esperando conexión...");
                    try (
                            Socket cliente = servidor.accept();
                            BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                            PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true)
                    ) {
                        System.out.println("Cliente conectado");
                        String saludo = entrada.readLine();
                        System.out.println("Cliente dice: " + saludo);
                        salida.println("Hola A, te saluda B");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error al conectar con el cliente: " + e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}

class ClientServerC {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            return;
        }
        String[] local = args[0].split(":");
        String[] remote = args[1].split(":");
        String listenIp = local[0];
        int listenPort = Integer.parseInt(local[1]);
        String remoteIp = remote[0];
        int remotePort = Integer.parseInt(remote[1]);
        Thread serverThread = new Thread(() -> startServer(listenIp, listenPort));
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300);
        Thread clientThread = new Thread(() -> startClient(remoteIp, remotePort));
        clientThread.setDaemon(true);
        clientThread.start();
        clientThread.join(3000);
    }

    private static void startServer(String ip, int port) {
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(ip, port));
            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleConnection(client)).start();
            }
        } catch (IOException ignored) {
        }
    }

    private static void handleConnection(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String msg = in.readLine();
            if (msg != null) {
                out.println("ACK " + msg);
            }
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void startClient(String host, int port) {
        long deadline = System.currentTimeMillis() + 2500;
        while (System.currentTimeMillis() < deadline) {
            try (
                    Socket socket = new Socket(host, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                String msg = "Hola desde " + socket.getLocalPort();
                out.println(msg);
                String response = in.readLine();
                System.out.println("Respuesta: " + response);
                return;
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
