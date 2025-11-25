package ru.tcp.logserver;

import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.tcp.Server;
import ru.tcp.logserver.parser.MsgParser;

@Component
public class ServerRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ServerRunner.class);

    private final ExecutorService executorForProcessing;
    private final Server server;
    private final MsgParser msgParser;

    public ServerRunner(
            @Qualifier("executorForProcessing") ExecutorService executorForProcessing,
            Server server,
            MsgParser msgParser) {
        this.executorForProcessing = executorForProcessing;
        this.server = server;
        this.msgParser = msgParser;
    }

    @Override
    public void run(String... args) {
        logger.info("Starting server...");
        executorForProcessing.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    handleConnectedClientsEvents(server);
                    handleDisConnectedClientsEvents(server);
                    handleClientMessages(server);
                    if (executorForProcessing.isShutdown()) {
                        logger.info("server handlers are stopping...");
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception ex) {
                    logger.error("error:{}", ex.getMessage());
                }
            }
        });
        server.start();
    }

    private void handleConnectedClientsEvents(Server server) {
        var newClient = server.getConnectedClientsEvents().poll();
        if (newClient != null) {
            msgParser.newMessage(newClient);
            logger.info("connected client:{}", newClient);
        }
    }

    private void handleDisConnectedClientsEvents(Server server) {
        var disconnectedClient = server.getDisConnectedClientsEvents().poll();
        if (disconnectedClient != null) {
            logger.info("disconnected client:{}", disconnectedClient);
            msgParser.cleanMessagesMap(disconnectedClient);
        }
    }

    private void handleClientMessages(Server server) {
        var messageFromClient = server.getMessagesFromClients().poll();
        if (messageFromClient != null) {
            var clientAddress = messageFromClient.clientAddress();
            logger.info("{}:, message.length:{}", clientAddress, messageFromClient.message().length);
            var msgs = msgParser.parseMessage(clientAddress, messageFromClient.message());

            for (var msg : msgs) {
                logger.atInfo()
                        .setMessage("msg:{}")
                        .addArgument(() -> new String(msg))
                        .log();
            }
        }
    }
}
