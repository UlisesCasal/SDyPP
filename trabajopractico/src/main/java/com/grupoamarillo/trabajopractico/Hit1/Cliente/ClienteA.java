package com.grupoamarillo.trabajopractico.Hit1.Cliente;
import java.io.*;
import java.net.*;

public class ClienteA {

    public static void main(String[] args) {

        String host = "localhost";
        int puerto = 5001;

        try {
            //Crea un nuevo Socket, especificando q es local y en el puerto 
            Socket socket = new Socket(host, puerto);
            
            //Crea un PrintWriter para enviar datos al servidor a traves del socket
            PrintWriter salida = new PrintWriter(
                    socket.getOutputStream(), true);

            //Crea un BufferedReader para recibir datos del servidor a traves del socket
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            
            //Envía un mensaje al servidor
            salida.println("Hola B, soy A");

            //Recibe la respuesta del servidor
            String respuesta = entrada.readLine();
            System.out.println("Servidor responde: " + respuesta);

            //Cierra el socket
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}