package ru.tcp;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import ru.tcp.encoder.MessageEncoderBase;
import ru.tcp.network.ConnectionParameters;
import ru.tcp.network.NetworkClient;

public class LogAppender extends UnsynchronizedAppenderBase<LoggingEvent> {
    private static final String MESSAGE_TEMPLATE = "[TCP appender] %s";

    private String serverHost;
    private Integer serverPort;

    private final Queue<LoggingEvent> eventsQueue = new ArrayBlockingQueue<>(1000);

    private Encoder<LoggingEvent> encoder;

    private Thread senderThread;

    public void setServerHost(String host) {
        this.serverHost = host;
        addInfo(String.format(MESSAGE_TEMPLATE, "set host:" + host));
    }

    public void setEncoder(Encoder<LoggingEvent> encoder) {
        this.encoder = encoder;
    }

    public void setServerPort(String port) {
        this.serverPort = Integer.parseInt(port);
        addInfo(String.format(MESSAGE_TEMPLATE, "set port:" + port));
    }

    @Override
    public void start() {
        if (serverHost == null) {
            addError(String.format(MESSAGE_TEMPLATE, "serverHost is null"));
            return;
        }

        if (serverPort == null) {
            addError(String.format(MESSAGE_TEMPLATE, "serverPort is null"));
            return;
        }
        var messageEncoder = new MessageEncoderBase();
        var connectionParameters = new ConnectionParameters(serverHost, serverPort);
        var networkClient = new NetworkClient(connectionParameters, messageEncoder);
        try {
            networkClient.connect();
        } catch (Exception ex) {
            addError(ex.getMessage());
        }

        senderThread = Thread.ofVirtual().name("senderThread").start(() -> sendMessages(networkClient));
        super.start();
        addInfo(String.format(MESSAGE_TEMPLATE, "started"));
    }

    @Override
    public void stop() {
        super.stop();
        senderThread.interrupt();
        addInfo(String.format(MESSAGE_TEMPLATE, "stopped"));
    }

    @Override
    public void append(LoggingEvent eventObject) {
        var result = eventsQueue.offer(eventObject);
        if (!result) {
            addWarn(String.format(MESSAGE_TEMPLATE, "eventsQueue is full"));
        }
    }

    private void sendMessages(NetworkClient networkClient) {
        while (!Thread.currentThread().isInterrupted()) {
            var event = eventsQueue.poll();
            if (event != null) {
                try {
                    networkClient.send(encoder.encode(event));
                } catch (Exception ex) {
                    addError(String.format(MESSAGE_TEMPLATE, ex.getMessage()));
                }
            }
        }
        try {
            networkClient.disconnect();
        } catch (Exception ex) {
            addError(ex.getMessage());
        }
    }
}
