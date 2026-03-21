package com.grupoamarillo.trabajopractico.Hit3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorB {

    public static void main(String[] args) {
        int puerto = 5001;

        while (true) {
            try {
                // Crea un nuevo ServerSocket, especificando q es local y en el puerto
                // (escuchando)
                ServerSocket servidor = new ServerSocket(puerto);
                System.out.println("Servidor B esperando conexión...");

                // Acepta una conexión entrante del cliente
                Socket cliente = servidor.accept(); // espera cliente
                System.out.println("Cliente conectado");

                // Crea un BufferedReader para recibir datos del cliente a traves del socket
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(cliente.getInputStream()));

                // Crea un PrintWriter para enviar datos al cliente a traves del socket
                PrintWriter salida = new PrintWriter(
                        cliente.getOutputStream(), true);

                // Recibe el mensaje del cliente
                String saludo = entrada.readLine();
                System.out.println("Cliente dice: " + saludo);

                // Envía una respuesta al cliente
                salida.println("Hola A, te saluda B");

                // Cierra el socket y el ServerSocket
                cliente.close();
                servidor.close();

            } catch (IOException e) {
                // e.printStackTrace();
                System.out.println("Error al conectar con el cliente: " + e.getMessage());

            }
        }
    }
}