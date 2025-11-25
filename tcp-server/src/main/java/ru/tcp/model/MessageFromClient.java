package ru.tcp.model;

import java.net.SocketAddress;
import java.util.Objects;

public record MessageFromClient(SocketAddress clientAddress, byte[] message) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageFromClient that = (MessageFromClient) o;
        return Objects.equals(clientAddress, that.clientAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientAddress);
    }

    @Override
    public String toString() {
        return "MessageForClient{" + "clientAddress=" + clientAddress + '}';
    }
}
