package ru.tcp.demo;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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

        try (var executor = Executors.newFixedThreadPool(3)) {
            executor.submit(server::start);

            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        handleConnectedClientsEvents(server);
                        handleDisConnectedClientsEvents(server);
                    } catch (Exception ex) {
                        logger.error("error:{}", ex.getMessage());
                    }
                }
            });

            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    handleClientMessages(server);
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

    private final Map<SocketAddress, StringBuilder> messages = new HashMap<>();

    private void handleClientMessages(Server server) {
        var messageFromClient = server.getMessagesFromClients().poll();
        if (messageFromClient != null) {
            var clientAddress = messageFromClient.clientAddress();

            var messageAsString = new String(messageFromClient.message(), StandardCharsets.UTF_8);
            logger.info("from:{}, message.length:{}", clientAddress, messageAsString.length());

            if (messageAsString.contains("s")
                    || messageAsString.contains("t")
                    || messageAsString.contains("o")
                    || messageAsString.contains("p")) {
                messages.putIfAbsent(clientAddress, new StringBuilder());
                messages.get(clientAddress).append(messageAsString);

                if (messages.get(clientAddress).toString().contains("stop")) {
                    server.send(clientAddress, "ok".getBytes(StandardCharsets.UTF_8));
                    logger.info("done:{}", clientAddress);
                    messages.remove(clientAddress);
                }
            }
        }
    }
}
