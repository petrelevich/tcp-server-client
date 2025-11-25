package ru.tcp.logserver.parser;

import static ru.tcp.logserver.parser.ParserState.BEGIN;
import static ru.tcp.logserver.parser.ParserState.LENGTH;
import static ru.tcp.logserver.parser.ParserState.MSG;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgParserSm implements MsgParser {
    private static final Logger logger = LoggerFactory.getLogger(MsgParserSm.class);

    private static final int MAX_MESSAGE_SIZE = 1024 * 10;

    private final Map<SocketAddress, ByteBuffer> messages = new ConcurrentHashMap<>();
    private final Map<SocketAddress, Integer> messagesExpectedLength = new ConcurrentHashMap<>();
    private final Map<SocketAddress, ParserState> messagesMsgState = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings({"java:S3776", "java:S6541"})
    public List<byte[]> parseMessage(SocketAddress clientAddress, byte[] message) {
        var parsedMsgs = new ArrayList<byte[]>();
        byte[] messageForProcess = message;

        var currentMessage = messages.get(clientAddress);
        var msgState = messagesMsgState.get(clientAddress);
        if (msgState == ParserState.HEADER) {
            if (message[HEADER_INDEX] == HEADER) {
                messagesMsgState.put(clientAddress, LENGTH);
            } else {
                var headerIdx = -1;
                for (var idx = 0; idx < message.length && headerIdx < 0; idx++) {
                    if (message[idx] == HEADER) {
                        headerIdx = idx;
                    }
                }
                if (headerIdx >= 0) {
                    messageForProcess = new byte[message.length - headerIdx];
                    System.arraycopy(message, headerIdx, messageForProcess, 0, messageForProcess.length);
                    messagesMsgState.put(clientAddress, LENGTH);
                } else {
                    return parsedMsgs;
                }
            }
        }
        currentMessage.put(messageForProcess);

        msgState = messagesMsgState.get(clientAddress);
        if (msgState == ParserState.LENGTH) {
            if (currentMessage.position() >= LENGTH_INDEX + 4) {
                var length = currentMessage.getInt(LENGTH_INDEX);
                messagesExpectedLength.put(clientAddress, length);
                messagesMsgState.put(clientAddress, BEGIN);
            } else {
                return parsedMsgs;
            }
        }

        msgState = messagesMsgState.get(clientAddress);
        if (msgState == ParserState.BEGIN
                && currentMessage.position() >= BEGIN_MESSAGE_INDEX
                && currentMessage.get(BEGIN_MESSAGE_INDEX) == BEGIN_MESSAGE) {
            messagesMsgState.put(clientAddress, MSG);
        }

        msgState = messagesMsgState.get(clientAddress);
        if (msgState == ParserState.MSG) {
            var expectedLength = messagesExpectedLength.get(clientAddress);
            if (currentMessage.position() > BEGIN_MESSAGE_INDEX + expectedLength) {
                var endMessageIndex = BEGIN_MESSAGE_INDEX + expectedLength + 1;
                if (currentMessage.get(endMessageIndex) == END_MESSAGE) {
                    var msg = new byte[expectedLength];
                    currentMessage.get(BEGIN_MESSAGE_INDEX + 1, msg);
                    parsedMsgs.add(msg);
                    newMessage(clientAddress);
                    var nextMsgIndex = endMessageIndex + 1;
                    var restMsgSize = currentMessage.position() - nextMsgIndex;
                    if (restMsgSize > 0) {
                        var nextMessages = new byte[restMsgSize];
                        currentMessage.get(nextMsgIndex, nextMessages);
                        parsedMsgs.addAll(parseMessage(clientAddress, nextMessages));
                    }
                } else {
                    logger.error("{}: wrong message format: END_MESSAGE not found", clientAddress);
                    newMessage(clientAddress);
                }
            }
        }
        return parsedMsgs;
    }

    @Override
    public void newMessage(SocketAddress clientAddress) {
        messagesMsgState.put(clientAddress, ParserState.HEADER);
        messages.put(clientAddress, ByteBuffer.allocate(MAX_MESSAGE_SIZE));
    }

    @Override
    public void cleanMessagesMap(SocketAddress client) {
        messages.remove(client);
        messagesExpectedLength.remove(client);
    }
}
