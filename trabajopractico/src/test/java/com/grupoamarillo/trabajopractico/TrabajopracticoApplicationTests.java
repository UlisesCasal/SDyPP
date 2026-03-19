package com.grupoamarillo.trabajopractico;

import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeRequest;
import com.grupoamarillo.trabajopractico.grpc.MensajeProto.MensajeResponse;
import com.grupoamarillo.trabajopractico.grpc.NodoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TrabajopracticoApplicationTests {
	@Test
	void contextLoads() {
	}

	@Test
	void grpcRoundTripEntreNodos() throws Exception {
		int puertoPrueba = 5090;
		ServidorGrpc.iniciarAsincrono(puertoPrueba);

		// El test hace una llamada RPC real para validar el flujo cliente-servidor.
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", puertoPrueba)
				.usePlaintext()
				.build();
		try {
			NodoServiceGrpc.NodoServiceBlockingStub stub = NodoServiceGrpc.newBlockingStub(channel);
			MensajeRequest request = MensajeRequest.newBuilder()
					.setFrom("C")
					.setMsg("Hola D")
					.build();

			MensajeResponse response = stub.enviar(request);
			assertEquals("D", response.getFrom());
			assertTrue(response.getMsg().contains("Hola D"));
		} finally {
			channel.shutdown();
			channel.awaitTermination(2, TimeUnit.SECONDS);
			ServidorGrpc.detener();
		}
	}
}
