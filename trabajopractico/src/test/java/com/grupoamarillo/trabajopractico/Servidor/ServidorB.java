package com.grupoamarillo.trabajopractico.Servidor;
import java.io.*;
import java.net.*;

public class ServidorB {

    public static void main(String[] args) {
        int puerto = 5001;

        try {
            //Crea un nuevo ServerSocket, especificando q es local y en el puerto (escuchando)
            ServerSocket servidor = new ServerSocket(puerto);
            System.out.println("Servidor B esperando conexión...");

            //Acepta una conexión entrante del cliente
            Socket cliente = servidor.accept(); // espera cliente
            System.out.println("Cliente conectado");

            //Crea un BufferedReader para recibir datos del cliente a traves del socket
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(cliente.getInputStream()));
                
            //Crea un PrintWriter para enviar datos al cliente a traves del socket
            PrintWriter salida = new PrintWriter(
                    cliente.getOutputStream(), true);
                        
            //Recibe el mensaje del cliente
            String saludo = entrada.readLine();
            System.out.println("Cliente dice: " + saludo);

            //Envía una respuesta al cliente
            salida.println("Hola A, te saluda B");

            //Cierra el socket y el ServerSocket
            cliente.close();
            servidor.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}