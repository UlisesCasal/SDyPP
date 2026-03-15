/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.grupoamarillo.trabajopractico;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author esteban
 */


public class ClientServerC {

    public static void main(String[] args) throws Exception {

        // Controla que se pasen los 2 parametros
        if (args.length != 2) {
            System.out.println("Uso: java ClientServerC IP_ESCUCHA:PUERTO IP_CLIENTE:PUERTO");
            return;
        }

        // Toma el parametro de ESCUCHA y genera la IP y puerto
        String[] listenParts = args[0].split(":");
        String listenIp = listenParts[0];
        int listenPort = Integer.parseInt(listenParts[1]);

        // Toma el parametro del CLIENTE y genera la IP y puerto
        String[] clientParts = args[1].split(":");
        String clientIp = clientParts[0];
        int clientPort = Integer.parseInt(clientParts[1]);


        // Colas para almacenar mensajes para el cliente y para el servidor
        BlockingQueue<String> toServerQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> toClientQueue = new LinkedBlockingQueue<>();

        // Hilo para el servidor
        Thread serverThread = new Thread(() -> runServer(listenIp, listenPort, toServerQueue));
        serverThread.setName("ServerThread");
        serverThread.start();

        // Delay para que se inicie el servidor, en instancias locales, antes de recibir a un cliente
        Thread.sleep(500);
        
        // Hilo para conexion con cliente
        Thread clientThread = new Thread(() -> runClient(clientIp, clientPort, toClientQueue));
        clientThread.setName("ClientThread");
        clientThread.start();

        // Lee stdin y almacena los mensajes en colas (s: envia al servidor que acepto el socket, c: al cliente del socket)
        // Genera un chat para enviar mensajes
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Uso: escribe 's:mensaje' para enviar al cliente que conecte al servidor, 'c:mensaje' para enviar al servidor remoto, o 'exit' para salir.");
        String line;
        while ((line = stdin.readLine()) != null) {
            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Saliendo...");
                System.exit(0);
            }
            if (line.startsWith("s:")) {
                toServerQueue.offer(line.substring(2));
            } else if (line.startsWith("c:")) {
                toClientQueue.offer(line.substring(2));
            } else {
                toServerQueue.offer(line);
                toClientQueue.offer(line);
            }
        }
    }

    private static void runServer(String ip, int port, BlockingQueue<String> outQueue) {
        try (ServerSocket server = new ServerSocket()) {

            server.bind(new InetSocketAddress(ip, port));
            System.out.println("Servidor: escuchando en " + ip + ":" + port);

            while (true) {
                Socket socket = server.accept();
                System.out.println("Servidor: cliente conectado desde " + socket.getRemoteSocketAddress());

                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Hilo para leer del socket y mostrar los mensajes entrantes
                Thread reader = new Thread(() -> {
                    try {
                        String msg;
                        while ((msg = entrada.readLine()) != null) {
                            System.out.println("[desde cliente] " + msg);
                        }
                    } catch (Exception e) {
                        System.out.println("Servidor: conexión cliente terminada: " + e.getMessage());
                    }
                });
                reader.setDaemon(true);
                reader.start();

                // Hilo para enviar mensajes al cliente conectado
                Thread writer = new Thread(() -> {
                    try {
                        while (!socket.isClosed()) {
                            String toSend = outQueue.poll(1, TimeUnit.SECONDS);
                            if (toSend != null) {
                                salida.println(toSend);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        System.out.println("Servidor writer error: " + e.getMessage());
                    }
                });
                writer.setDaemon(true);
                writer.start();

                // Mantiene la conexion viva hasta que el hilo lector concluya
                try {
                    reader.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                try {
                    socket.close();
                } catch (Exception ignore) {}
                System.out.println("Servidor: cliente desconectado, esperando nueva conexión...");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runClient(String ip, int port, BlockingQueue<String> outQueue) {
        while (true) {
            try (Socket socket = new Socket()) {
                // Conecta el socket, esperando 5 segundos para establecer la conexion 
                socket.connect(new InetSocketAddress(ip, port), 5000);
                System.out.println("Cliente: conectado a " + ip + ":" + port);

                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Hilo para leer los mensajes del servidor
                Thread reader = new Thread(() -> {
                    try {
                        String msg;
                        while ((msg = entrada.readLine()) != null) {
                            System.out.println("[desde servidor] " + msg);
                        }
                    } catch (Exception e) {
                        System.out.println("Cliente reader terminado: " + e.getMessage());
                    }
                });
                reader.setDaemon(true);
                reader.start();

                // Hilo para enviar mensajes al servidor
                Thread writer = new Thread(() -> {
                    try {
                        // Envia saludo inicial
                        salida.println("Hola desde cliente");
                        while (!socket.isClosed()) {
                            String toSend = outQueue.poll(1, TimeUnit.SECONDS);
                            if (toSend != null) {
                                salida.println(toSend);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        System.out.println("Cliente writer error: " + e.getMessage());
                    }
                });
                writer.setDaemon(true);
                writer.start();

                // Espera al hilo lector para terminar (i.e., remote closed)
                try {
                    reader.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            } catch (Exception e) {
                System.out.println("Cliente: error de conexión, reintentando en 5s: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}


