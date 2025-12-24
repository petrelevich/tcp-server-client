package ru.tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private static final int PORT = 8080;
    private static final String HOST = "localhost";
    private static final int CLIENT_NUMBER = 15;

    public static void main(String[] args) {
        try (var executor = Executors.newFixedThreadPool(CLIENT_NUMBER)) {
            var counter = new AtomicInteger(0);
            var latch = new CountDownLatch(CLIENT_NUMBER);
            for (var idx = 0; idx < CLIENT_NUMBER; idx++) {
                executor.submit(() -> send("testData_%d".formatted(counter.incrementAndGet()), latch));
            }
        }
    }

    private static void send(String request, CountDownLatch latch) {
        try {
            latch.countDown();
            var waitResult = latch.await(10, TimeUnit.SECONDS);
            if (!waitResult) {
                logger.error("unexpected beginning");
            }
            try (var clientSocket = new Socket(HOST, PORT)) {
                var out = new PrintWriter(clientSocket.getOutputStream(), true);
                var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                for (var idx = 0; idx < 3; idx++) {
                    logger.info("sending to server: {}", idx);
                    out.println(request);
                    var resp = in.readLine();
                    logger.info("server response: {}", resp);
                    responseProcessing();
                }
            }
            logger.info("connection closed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            logger.error("error", ex);
        }
    }

    private static void responseProcessing() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
