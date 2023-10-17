package ru.tcp;

import java.nio.channels.SocketChannel;

public class ClientCommunicationException extends RuntimeException {

    private final transient SocketChannel socketChannel;

    public ClientCommunicationException(String message, Throwable cause, SocketChannel socketChannel) {
        super(message, cause);
        this.socketChannel = socketChannel;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
}
