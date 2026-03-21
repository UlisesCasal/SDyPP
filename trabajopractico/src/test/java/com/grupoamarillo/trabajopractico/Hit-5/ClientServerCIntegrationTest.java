package com.grupoamarillo.trabajopractico;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class Hit5ClientServerCIntegrationTest {

    @Test
    void twoInstancesExchangeGreetings() {
        System.out.println("TP1 Hit 4 --- Test");
        assertTimeoutPreemptively(Duration.ofSeconds(12), () -> {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (PrintStream ps = new PrintStream(baos)) {
                System.setOut(ps);

                Thread t1 = new Thread(() -> {
                    try {
                        ClientServerC.main(new String[] {"127.0.0.1:7000", "127.0.0.1:7001"});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                Thread t2 = new Thread(() -> {
                    try {
                        ClientServerC.main(new String[] {"127.0.0.1:7001", "127.0.0.1:7000"});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                t1.setDaemon(true);
                t2.setDaemon(true);
                t1.start();
                t2.start();

                long deadline = System.currentTimeMillis() + Duration.ofSeconds(8).toMillis();
                while (System.currentTimeMillis() < deadline) {
                    String out = baos.toString();
                    long countTcp = out.lines().filter(l -> l.contains("Respuesta: ACK Hola desde")).count();
                    long countGrpc = out.lines().filter(l -> l.contains("Respuesta gRPC ->")).count();
                    if (countTcp >= 2 || countGrpc >= 1) {
                        return;
                    }
                    Thread.sleep(100);
                }

                fail("No mutual greetings detected. Output:\n" + baos.toString());

            } finally {
                System.setOut(originalOut);
            }
        });
    }
}
