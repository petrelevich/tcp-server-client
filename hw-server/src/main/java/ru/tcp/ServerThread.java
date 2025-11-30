package ru.tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerThread {
    private static final Logger logger = LoggerFactory.getLogger(ServerThread.class);
    private static final int PORT = 8080;
    private final ExecutorService executor = Executors.newFixedThreadPool(
            10, task -> Thread.ofPlatform().name("server-", 1).unstarted(task));

    public static void main(String[] args) {
        new ServerThread().go();
    }

    private void go() {
        try (var serverSocket = new ServerSocket(PORT, 1000, InetAddress.getLoopbackAddress())) {
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
        try (var out = new PrintWriter(clientSocket.getOutputStream(), true);
                var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String input;
            while ((input = in.readLine()) != null) {
                logger.info("from client: {} ", input);
                out.println("echo:" + input);
            }
            logger.info("client finished");
        } catch (Exception ex) {
            logger.error("error", ex);
        }
    }
}
