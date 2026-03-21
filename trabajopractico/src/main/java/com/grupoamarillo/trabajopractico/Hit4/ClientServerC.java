package  com.grupoamarillo.trabajopractico.Hit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientServerC {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Uso: java ClientServerC IP_ESCUCHA:PUERTO IP_REMOTO:PUERTO");
            return;
        } 
		// Toma los agumentos para cargar las IP y puertos
        String[] local = args[0].split(":");
        String[] remote = args[1].split(":");

        String listenIP = local[0];
        int listenPort = Integer.parseInt(local[1]);

        String remoteIP = remote[0];
        int remotePort = Integer.parseInt(remote[1]);

		// hilo para el servidor
        Thread server_thread = new Thread(() -> startServer(listenIP, listenPort));
		server_thread.start();

        Thread.sleep(1000);
		
		Thread client_thread = new Thread(() -> startClient(remoteIP, remotePort));
		client_thread.start();

    }

    private static void startServer(String ip, int port) {

        try (ServerSocket server = new ServerSocket()) {

            server.bind(new InetSocketAddress(ip, port));

            System.out.println("Escuchando en " + ip + ":" + port);

            
            while (true) {
                Socket client = server.accept();

                new Thread(() -> handleConnection(client)).start();
            }

        } catch (IOException e) {
            System.out.println("Error servidor: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket socket) {
        //System.out.println(String.format("Cliente conectado por puerto %d",socket.getPort()));

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
                // Cuando concluye la conexión reinforma que esta escuchando
                // Sigue escuchando igual, solo se informa para no dejar la consola "pensando"
                System.out.println(String.format("Escuchando en Puerto %d ...", socket.getLocalPort()));
            } catch (IOException ignored) {}
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

                System.out.println("No se pudo conectar a "+ host + ":" + port +" Reintentando...");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}