package com.grupoamarillo.trabajopractico;

public class ClientServerC {

    public static void main(String[] args) throws Exception {
        // args[0] = IP:PUERTO donde este nodo escucha
        // args[1] = IP:PUERTO del nodo remoto
        // args[2] y args[3] son opcionales para personalizar emisor y mensaje
        if (args.length < 2 || args.length > 4) {
            System.out.println("Uso: java ClientServerC IP:PUERTO_LOCAL IP:PUERTO_REMOTO [ORIGEN] [MENSAJE]");
            return;
        }

        String[] local  = args[0].split(":");
        String[] remote = args[1].split(":");

        int puertoLocal  = Integer.parseInt(local[1]);
        String ipRemota  = remote[0];
        int puertoRemoto = Integer.parseInt(remote[1]);

        // Se levanta primero el servidor gRPC local para poder recibir mensajes.
        Thread serverThread = new Thread(() -> {
            try { ServidorGrpc.iniciar(puertoLocal); }
            catch (Exception e) { e.printStackTrace(); }
        });
        serverThread.start();

        // Pequeña espera para asegurar que el servidor ya quedó listo.
        Thread.sleep(1000);

        String origen = args.length >= 3 ? args[2] : "C";
        String mensaje = args.length == 4 ? args[3] : "Hola desde " + origen;

        // Se envía un mensaje al nodo remoto usando el stub generado por gRPC.
        ClienteGRPC.conectar(ipRemota, puertoRemoto, origen, mensaje);
    }
}
