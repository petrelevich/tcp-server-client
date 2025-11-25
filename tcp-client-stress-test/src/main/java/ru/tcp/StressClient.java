package ru.tcp;

import java.net.Socket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StressClient {
    private static final Logger log = LoggerFactory.getLogger(StressClient.class);
    private static final int REQUEST_COUNTER_LIMIT = 100_000;
    private static final int WORK_LIMIT = 30;
    private static final byte[] DATA = new byte[1024];

    public static void main(String[] args) throws InterruptedException {
        var factory = Thread.ofVirtual().name("client-", 0).factory();
        var stressClient = new StressClient();

        var clientParameters = stressClient.parsParameters(args);

        try (var executor = Executors.newThreadPerTaskExecutor(factory)) {
            var begin = Instant.now();
            log.info("begin:{}", begin);
            for (var idx = 0; idx < clientParameters.threadCount(); idx++) {
                executor.submit(() -> stressClient.send(clientParameters.serverHost(), clientParameters.serverPort()));
                log.info("scheduled:{}", idx);
            }
            executor.shutdown();
            var result = executor.awaitTermination(WORK_LIMIT, TimeUnit.MINUTES);
            if (!result) {
                log.error("timeout elapsed before termination");
                executor.shutdownNow();
            }
            var end = Instant.now();
            log.info("end:{}, spend:{}", end, ChronoUnit.SECONDS.between(begin, end));
        }
    }

    private ClientParameters parsParameters(String[] agrs) {
        log.atInfo()
                .setMessage("arguments:{}")
                .addArgument(() -> Arrays.toString(agrs))
                .log();
        String host = null;
        int port = 0;
        int threadCount = 0;
        for (var arg : agrs) {
            var argParts = arg.replace(" ", "").split("=");
            var name = argParts[0];
            var value = argParts[1];

            switch (name) {
                case "-h":
                    host = value;
                    break;
                case "-p":
                    port = Integer.parseInt(value);
                    break;
                case "-t":
                    threadCount = Integer.parseInt(value);
                    break;
                default:
                    log.error("unknown argument:{}", name);
            }
        }
        if (host == null) {
            throw new IllegalArgumentException("Host is not defined");
        }
        if (port == 0) {
            throw new IllegalArgumentException("Port is not defined");
        }
        if (threadCount <= 0) {
            throw new IllegalArgumentException("ThreadCount is not defined");
        }
        return new ClientParameters(host, port, threadCount);
    }

    private void send(String host, int port) {
        try {
            try (var clientSocket = new Socket(host, port);
                    var out = clientSocket.getOutputStream();
                    var in = clientSocket.getInputStream()) {
                for (var idx = 0; idx < REQUEST_COUNTER_LIMIT; idx++) {
                    out.write(DATA);
                    out.flush();
                    if (idx % 1000 == 0) {
                        log.info("send counter:{}", idx);
                    }
                }
                log.info("send done. writes:{}, data:{}", REQUEST_COUNTER_LIMIT, REQUEST_COUNTER_LIMIT * DATA.length);
                out.write("stop".getBytes());
                var buffer = new byte[10];
                var response = new StringBuilder();
                do {
                    var read = in.read(buffer);
                    response.append(new String(Arrays.copyOf(buffer, read)));
                    log.info("response:{}", response);
                } while (!response.toString().contains("ok"));
            }
        } catch (Exception ex) {
            log.error("error", ex);
        }
    }
}
