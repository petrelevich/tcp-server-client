package ru.tcp.demo;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tcp.Server;

public class TcpServerDemo {
    private static final Logger logger = LoggerFactory.getLogger(TcpServerDemo.class);

    public static void main(String[] args) {
        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();

        logger.info("availableProcessors:{}", Runtime.getRuntime().availableProcessors());
        logger.info("TotalMemorySize, mb:{}", os.getTotalMemorySize() / 1024 / 1024);
        logger.info("maxMemory, mb:{}", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        logger.info("freeMemory, mb:{}", Runtime.getRuntime().freeMemory() / 1024 / 1024);

        new TcpServerDemo().run();
    }

    private void run() {
        var server = new Server(8081);

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
            var messageAsString = new String(messageFromClient.message(), StandardCharsets.UTF_8);
            logger.info("from:{}, message.length:{}", messageFromClient.clientAddress(), messageAsString.length());
            if (messageAsString.contains("stop")) {
                server.send(messageFromClient.clientAddress(), "ok".getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
