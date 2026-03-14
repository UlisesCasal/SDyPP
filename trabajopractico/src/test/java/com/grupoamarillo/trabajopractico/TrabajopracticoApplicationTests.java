package com.grupoamarillo.trabajopractico;

import com.grupoamarillo.trabajopractico.Cliente.ClienteA;
import com.grupoamarillo.trabajopractico.Servidor.ServidorB;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@SpringBootTest
class TrabajopracticoApplicationTests {
	@Test
	void contextLoads() {
	}

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
}
