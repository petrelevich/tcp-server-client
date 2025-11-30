package ru.tcp;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerNIO {
    private static final Logger logger = LoggerFactory.getLogger(ServerNIO.class);

    private static final int PORT_0 = 8080;

    public static void main(String[] args) throws IOException {
        new ServerNIO().go();
    }

    private void go() throws IOException {
        try (var serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(PORT_0));

            try (var selector = Selector.open()) {
                serverSocketChannel.register(selector, OP_ACCEPT);
                while (!Thread.currentThread().isInterrupted()) {
                    logger.info("waiting for client");
                    selector.select(this::performIO, 10_000);
                }
            }
        }
    }

    private void performIO(SelectionKey selectedKey) {
        try {
            logger.info("event, key:{}", selectedKey);
            if (selectedKey.isAcceptable()) {
                acceptConnection(selectedKey);
            } else if (selectedKey.isReadable()) {
                readWriteClient(selectedKey);
            }
        } catch (Exception ex) {
            throw new NetworkException(ex);
        }
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        Selector selector = key.selector();
        logger.info("accept client connection, key:{}, selector:{}", key, selector);
        // selector=sun.nio.ch.EPollSelectorImpl - Linux epoll based Selector implementation
        var serverSocketChannel = (ServerSocketChannel) key.channel();
        var socketChannel = serverSocketChannel.accept(); // The socket channel for the new connection

        socketChannel.configureBlocking(false);
        socketChannel.register(selector, OP_READ);
        logger.info("socketChannel:{}", socketChannel);
    }

    private void readWriteClient(SelectionKey selectionKey) throws IOException {
        logger.info("read from client");
        var socketChannel = (SocketChannel) selectionKey.channel();

        handleRequest(socketChannel)
                .map(this::processClientRequest)
                .ifPresent(response -> sendResponse(socketChannel, response));
    }

    private Optional<String> handleRequest(SocketChannel socketChannel) throws IOException {
        var buffer = ByteBuffer.allocate(5);
        var inputBuffer = new StringBuilder(100);
        long msgSize = 0;
        int changSize;
        while ((changSize = socketChannel.read(buffer)) > 0) {
            msgSize += changSize;
            buffer.flip();
            var input = StandardCharsets.UTF_8.decode(buffer).toString();
            logger.info("from client: {} ", input);

            buffer.flip();
            inputBuffer.append(input);
        }

        if (msgSize > 0) {
            String requestFromClient = inputBuffer.toString().replace("\n", "").replace("\r", "");
            logger.info("requestFromClient: {} ", requestFromClient);
            return Optional.of(requestFromClient);
        } else {
            closeSocketChannel(socketChannel);
            logger.info("requestFromClient is empty");
            return Optional.empty();
        }
    }

    private void sendResponse(SocketChannel socketChannel, String responseForClient) {
        try {
            var buffer = ByteBuffer.allocate(5);
            var response = responseForClient.getBytes();
            for (byte b : response) {
                buffer.put(b);
                if (buffer.position() == buffer.limit()) {
                    buffer.flip();
                    socketChannel.write(buffer);
                    buffer.flip();
                }
            }
            if (buffer.hasRemaining()) {
                buffer.flip();
                socketChannel.write(buffer);
            }
        } catch (Exception ex) {
            logger.error("error sending response", ex);
            closeSocketChannel(socketChannel);
        }
    }

    private void closeSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (Exception exClose) {
            logger.error("error close socketChannel", exClose);
        }
    }

    private String processClientRequest(String input) {
        if ("wait".equals(input)) {
            logger.info("waiting...");
            sleep();
        }
        return String.format("echo: %s%n", input);
    }

    private void sleep() {
        try {
            Thread.sleep(TimeUnit.MINUTES.toMillis(2));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
