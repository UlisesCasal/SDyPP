package com.grupoamarillo.trabajopractico;


import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class Hit3TrabajopracticoApplicationTests {
	@Test
	void servidorBSigueActivoSiASeDesconecta() {
		assertTimeoutPreemptively(Duration.ofSeconds(12), () -> {
			Thread servidorThread = new Thread(() -> ServidorB.main(new String[0]));
			servidorThread.setDaemon(true);
			servidorThread.start();
			esperarPuertoDisponible(Duration.ofSeconds(5));
			try (Socket conexionAbortada = new Socket("localhost", 5001)) {
			}
			String respuesta = solicitarRespuestaConReintento(Duration.ofSeconds(5));
			assertEquals("Hola A, te saluda B", respuesta);
		});
	}

	private void esperarPuertoDisponible(Duration timeout) throws InterruptedException {
		long limite = System.currentTimeMillis() + timeout.toMillis();
		while (System.currentTimeMillis() < limite) {
			try (Socket ignored = new Socket("localhost", 5001)) {
				return;
			} catch (IOException e) {
				Thread.sleep(100);
			}
		}
		throw new IllegalStateException("Servidor B no levantó en el tiempo esperado");
	}

	private String solicitarRespuestaConReintento(Duration timeout) throws InterruptedException {
		long limite = System.currentTimeMillis() + timeout.toMillis();
		IOException ultimoError = null;
		while (System.currentTimeMillis() < limite) {
			try (Socket socket = new Socket("localhost", 5001);
				 PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
				 BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
				salida.println("Hola B, soy A");
				String respuesta = entrada.readLine();
				if (respuesta != null) {
					return respuesta;
				}
			} catch (IOException e) {
				ultimoError = e;
				Thread.sleep(100);
			}
		}
		throw new IllegalStateException("No se obtuvo respuesta de Servidor B", ultimoError);
	}
}
