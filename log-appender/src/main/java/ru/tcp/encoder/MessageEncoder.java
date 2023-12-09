package ru.tcp.encoder;

public interface MessageEncoder {
    byte[] encode(byte[] message);
}
