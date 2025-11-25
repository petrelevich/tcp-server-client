package ru.tcp.network;

import java.io.OutputStream;
import java.net.Socket;
import ru.tcp.AppenderException;
import ru.tcp.encoder.MessageEncoder;

public class NetworkClient {
    private final ConnectionParameters connectionParameters;
    private Socket clientSocket;
    private OutputStream outputStream;
    private final MessageEncoder messageEncoder;
    private static final int RETRY_LIMIT = 5;
    private int retryCounter = 0;

    public NetworkClient(ConnectionParameters connectionParameters, MessageEncoder messageEncoder) {
        this.connectionParameters = connectionParameters;
        this.messageEncoder = messageEncoder;
    }

    public void connect() {
        try {
            clientSocket = new Socket(connectionParameters.serverHost(), connectionParameters.serverPort());
            outputStream = clientSocket.getOutputStream();
        } catch (Exception ex) {
            throw new AppenderException("can't connect to server: " + connectionParameters + " :" + ex.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.close();
            }
        } catch (Exception ex) {
            throw new AppenderException("can't connect to server: " + connectionParameters + " :" + ex.getMessage());
        }
    }

    public void send(byte[] message) {
        var serialisedMessage = messageEncoder.encode(message);
        var runAgain = true;
        while (retryCounter < RETRY_LIMIT && runAgain) {
            try {
                if (clientSocket == null) {
                    connect();
                }
                outputStream.write(serialisedMessage);
                outputStream.flush();
                runAgain = false;
            } catch (Exception ex) {
                connect();
                retryCounter++;
                throw new AppenderException("retryCounter:" + retryCounter + " can't send :" + ex.getMessage());
            }
        }
    }
}
