package com.grupoamarillo.trabajopractico.Hit3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteA {

    public static void main(String[] args) throws InterruptedException {

        String host = "localhost";
        int puerto = 5001;
        while (true) {
            try {
                // Crea un nuevo Socket, especificando q es local y en el puerto
                Socket socket = new Socket(host, puerto);

                // Crea un PrintWriter para enviar datos al servidor a traves del socket
                PrintWriter salida = new PrintWriter(
                        socket.getOutputStream(), true);

                // Crea un BufferedReader para recibir datos del servidor a traves del socket
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                // Envía un mensaje al servidor
                salida.println("Hola B, soy A");

                // Recibe la respuesta del servidor
                String respuesta = entrada.readLine();
                System.out.println("Servidor responde: " + respuesta);

                // Cierra el socket
                socket.close();
                break;
            } catch (IOException e) {
                // e.printStackTrace();
                System.out.println("Error al conectar con el servidor: " + e.getMessage());
                Thread.sleep(5000); // Espero 5 segundos y vuelvo a correr el loop
            }
        }
    }
}