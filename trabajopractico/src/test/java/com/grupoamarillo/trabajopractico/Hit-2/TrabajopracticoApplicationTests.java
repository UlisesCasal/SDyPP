package com.grupoamarillo.trabajopractico;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class Hit2TrabajopracticoApplicationTests {
	@Test
	void clienteASeReconectaYReenviaSaludoSiBCierraConexion() {
		assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
			ByteArrayOutputStream salidaCapturada = new ByteArrayOutputStream();
			PrintStream salidaOriginal = System.out;

			try {
				System.setOut(new PrintStream(salidaCapturada));

				Thread clienteThread = new Thread(() -> {
					try {
						ClienteA.main(new String[0]);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
				clienteThread.start();

				TimeUnit.MILLISECONDS.sleep(500);

				Thread servidorThread = new Thread(() -> ServidorB.main(new String[0]));
				servidorThread.setDaemon(true);
				servidorThread.start();

				clienteThread.join(12000);
				assertTrue(!clienteThread.isAlive());
			} finally {
				System.setOut(salidaOriginal);
			}

			String logs = salidaCapturada.toString();
			assertTrue(logs.contains("Error al conectar con el servidor"));
			assertTrue(logs.contains("Servidor responde: Hola A, te saluda B"));
		});
	}

	@Test
	void comunicacionDirectaHappyPath() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			ByteArrayOutputStream salidaCapturada = new ByteArrayOutputStream();
			PrintStream salidaOriginal = System.out;

			try {
				System.setOut(new PrintStream(salidaCapturada));

				// Servidor arranca primero
				Thread servidorThread = new Thread(() -> ServidorB.main(new String[0]));
				servidorThread.setDaemon(true);
				servidorThread.start();

				// Espera a que el servidor esté escuchando
				TimeUnit.MILLISECONDS.sleep(500);

				// Cliente se conecta directamente (sin reintentos)
				Thread clienteThread = new Thread(() -> {
					try {
						ClienteA.main(new String[0]);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
				clienteThread.start();
				clienteThread.join(8000);

				assertTrue(!clienteThread.isAlive(), "El cliente debería haber terminado");
			} finally {
				System.setOut(salidaOriginal);
			}

			String logs = salidaCapturada.toString();
			// Verifica que NO hubo errores de conexión (happy path)
			assertTrue(!logs.contains("Error al conectar con el servidor"),
					"No debería haber errores de conexión en el happy path");
			// Verifica que la comunicación fue exitosa
			assertTrue(logs.contains("Servidor responde: Hola A, te saluda B"),
					"El cliente debería recibir la respuesta del servidor");
		});
	}

	@Test
	void servidorRecibeMensajeCorrectoDelCliente() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			ByteArrayOutputStream salidaCapturada = new ByteArrayOutputStream();
			PrintStream salidaOriginal = System.out;

			try {
				System.setOut(new PrintStream(salidaCapturada));

				// Servidor arranca primero
				Thread servidorThread = new Thread(() -> ServidorB.main(new String[0]));
				servidorThread.setDaemon(true);
				servidorThread.start();

				TimeUnit.MILLISECONDS.sleep(500);

				// Cliente envía mensaje
				Thread clienteThread = new Thread(() -> {
					try {
						ClienteA.main(new String[0]);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
				clienteThread.start();
				clienteThread.join(8000);
			} finally {
				System.setOut(salidaOriginal);
			}

			String logs = salidaCapturada.toString();
			// Verifica que el servidor recibió el mensaje correcto del cliente
			assertTrue(logs.contains("Cliente dice: Hola B, soy A"),
					"El servidor debería recibir 'Hola B, soy A' del cliente");
			// Verifica que el servidor detectó la conexión
			assertTrue(logs.contains("Cliente conectado"),
					"El servidor debería detectar la conexión del cliente");
		});
	}
}
