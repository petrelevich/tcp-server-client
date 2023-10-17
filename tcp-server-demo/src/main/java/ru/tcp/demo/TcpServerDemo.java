package ru.tcp.demo;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tcp.Server;

public class TcpServerDemo {
    private final Logger logger = LoggerFactory.getLogger(TcpServerDemo.class);

    public static void main(String[] args) {
        new TcpServerDemo().run();
    }

    private void run() {
        var server = new Server(8080);

        try (var executor = Executors.newFixedThreadPool(2)) {
            executor.submit(server::start);

            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        handleConnectedClientsEvents(server);
                        handleDisConnectedClientsEvents(server);
                        handleClientMessages(server);
                    } catch (Exception ex) {
                        logger.error("error:{}", ex.getMessage());
                    }
                }
            });
        }
    }

    private void handleConnectedClientsEvents(Server server) {
        var newClient = server.getConnectedClientsEvents().poll();
        if (newClient != null) {
            var res = server.send(newClient, "hello".getBytes(StandardCharsets.UTF_8));
            logger.info("sending result:{}", res);
        }
    }

    private void handleDisConnectedClientsEvents(Server server) {
        var disconnectedClient = server.getDisConnectedClientsEvents().poll();
        if (disconnectedClient != null) {
            logger.info("disconnectedClient:{}", disconnectedClient);
        }
    }

    private void handleClientMessages(Server server) {
        var messageFromClient = server.getMessagesFromClients().poll();
        if (messageFromClient != null) {
            String messageAsString = new String(messageFromClient.message(), StandardCharsets.UTF_8);
            logger.info("from:{}, message:{}", messageFromClient.clientAddress(), messageAsString);
        }
    }
}
