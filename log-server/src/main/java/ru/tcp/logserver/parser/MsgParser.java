package ru.tcp.logserver.parser;

import java.net.SocketAddress;
import java.util.List;

public interface MsgParser {

    byte HEADER = 0x01;
    byte BEGIN_MESSAGE = 0x02;
    byte END_MESSAGE = 0x03;

    int HEADER_INDEX = 0;
    int LENGTH_INDEX = 1;
    int BEGIN_MESSAGE_INDEX = 5;

    List<byte[]> parseMessage(SocketAddress clientAddress, byte[] message);

    void newMessage(SocketAddress clientAddress);

    void cleanMessagesMap(SocketAddress client);
}
