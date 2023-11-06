package ru.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerBlocked {
    private static final Logger logger = LoggerFactory.getLogger(ServerBlocked.class);
    private static final int PORT = 8080;
    private static final byte[] EMPTY_ARRAY = new byte[0];
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void main(String[] args) {
        new ServerBlocked().go();
    }

    private void go() {
        try (var serverSocket = new ServerSocket(PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                logger.info("waiting for client connection");
                var clientSocket = serverSocket.accept();
                executor.submit(() -> clientHandler(clientSocket));
            }
        } catch (Exception ex) {
            logger.error("error", ex);
        }
        executor.shutdown();
    }

    private void clientHandler(Socket clientSocket) {
        logger.info("client connected:{}", clientSocket.getRemoteSocketAddress());
        try (var out = clientSocket.getOutputStream();
                var in = clientSocket.getInputStream()) {
            while (!Thread.currentThread().isInterrupted()) {
                var dataFromClient = getDataFromClient(in);
                if (dataFromClient.length == 0) {
                    logger.info("client disconnected:{}", clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                    return;
                }
                logger.atInfo()
                        .setMessage("from client as string: {} ")
                        .addArgument(() -> new String(dataFromClient))
                        .log();
                out.write(dataFromClient);
            }
        } catch (Exception ex) {
            logger.error("error", ex);
        }
    }

    private byte[] getDataFromClient(InputStream in) throws IOException {
        List<byte[]> parts = new ArrayList<>();
        var buffer = new byte[2];
        int readBytesTotal = 0;
        int readBytes;
        var finished = false;
        while (!finished && (readBytes = in.read(buffer)) > 0) {
            parts.add(new byte[readBytes]);
            System.arraycopy(buffer, 0, parts.getLast(), 0, readBytes);
            readBytesTotal += readBytes;

            if (in.available() == 0) {
                finished = true;
                logger.debug("message finished");
            }
        }
        logger.debug("read bytes:{}", readBytesTotal);

        if (readBytesTotal == 0) {
            return EMPTY_ARRAY;
        }
        var result = new byte[readBytesTotal];
        var resultIdx = 0;

        for (var part : parts) {
            System.arraycopy(part, 0, result, resultIdx, part.length);
            resultIdx += part.length;
        }
        return result;
    }
}
