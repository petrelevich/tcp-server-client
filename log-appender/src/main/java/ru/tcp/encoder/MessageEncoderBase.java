package ru.tcp.encoder;

import java.nio.ByteBuffer;

public class MessageEncoderBase implements MessageEncoder {
    private static final byte HEADER = 0x01;
    private static final byte BEGIN_MESSAGE = 0x02;
    private static final byte END_MESSAGE = 0x03;

    @Override
    public byte[] encode(byte[] content) {
        var length = content.length;
        var messageLength = 1 + 4 + 1 + length + 1;
        var message = ByteBuffer.allocate(messageLength);
        message.put(HEADER);
        message.put(ByteBuffer.allocate(4).putInt(length).array());
        message.put(BEGIN_MESSAGE);
        message.put(content);
        message.put(END_MESSAGE);
        return message.array();
    }
}
