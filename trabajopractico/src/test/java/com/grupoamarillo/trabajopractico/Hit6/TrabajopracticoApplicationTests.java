package com.grupoamarillo.trabajopractico.Hit6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class Hit6TrabajopracticoApplicationTests {

    private final List<String> nodes = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private ServerSocket serverSocket;
    private int serverPort;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0);
        serverPort = serverSocket.getLocalPort();

        Thread serverThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleRegistration(client)).start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        synchronized (nodes) {
            nodes.clear();
        }
    }

    @Test
    void registerDevuelveNodoEnFormatoJson() throws Exception {
        List<Message> response = sendRegister(8101);

        assertFalse(response.isEmpty());
        Message node = response.get(0);
        assertNotNull(node.ip);
        assertEquals(8101, node.port);
    }

    @Test
    void segundoRegistroRecibeListaCompleta() throws Exception {
        sendRegister(8101);
        List<Message> secondResponse = sendRegister(8102);

        assertEquals(2, secondResponse.size());
        assertEquals(8101, secondResponse.get(0).port);
        assertEquals(8102, secondResponse.get(1).port);
    }

    private List<Message> sendRegister(int port) throws Exception {
        try (
            Socket socket = new Socket("127.0.0.1", serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            Message request = new Message();
            request.action = "REGISTER";
            request.port = port;
            out.println(mapper.writeValueAsString(request));

            List<Message> response = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) {
                response.add(mapper.readValue(line, Message.class));
            }
            return response;
        }
    }

    private void handleRegistration(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String json = in.readLine();
            if (json == null) {
                return;
            }
            Message request = mapper.readValue(json, Message.class);
            if (!"REGISTER".equals(request.action)) {
                return;
            }
            String clientIP = socket.getInetAddress().getHostAddress();
            String nodeAddress = clientIP + ":" + request.port;
            synchronized (nodes) {
                nodes.add(nodeAddress);
                for (String node : nodes) {
                    String[] parts = node.split(":");
                    Message responseNode = new Message();
                    responseNode.ip = parts[0];
                    responseNode.port = Integer.parseInt(parts[1]);
                    out.println(mapper.writeValueAsString(responseNode));
                }
            }
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
