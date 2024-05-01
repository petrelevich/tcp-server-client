package ru.tcp;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.tcp.model.MessageForClient;
import ru.tcp.model.MessageFromClient;

@SuppressWarnings("java:S1191")
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static final byte[] EMPTY_ARRAY = new byte[0];
    private static final int TIME_OUT_MS = 100;

    private final sun.misc.Unsafe unsafe;
    private final int port;
    private final InetAddress addr;
    private long messagesFromClientsCounter;
    private long bytesFromClientsCounter;

    private final Map<SocketAddress, SocketChannel> clients = new HashMap<>();
    private final BlockingQueue<SocketAddress> connectedClientsEvents = new LinkedBlockingQueue<>();
    private final BlockingQueue<SocketAddress> disConnectedClientsEvents = new LinkedBlockingQueue<>();
    private final BlockingQueue<MessageForClient> messagesForClients = new ArrayBlockingQueue<>(1000);
    private final BlockingQueue<MessageFromClient> messagesFromClients = new ArrayBlockingQueue<>(1000);

    private static final int RESULT_LIMIT = 102400;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    private final List<ByteBuffer> parts = new ArrayList<>();
    private volatile boolean run = true;

    public Server(int port) {
        this(null, port);
    }

    @SuppressWarnings("java:S3011")
    public Server(InetAddress addr, int port) {
        try {
            Constructor<sun.misc.Unsafe> unsafeConstructor = sun.misc.Unsafe.class.getDeclaredConstructor();
            unsafeConstructor.setAccessible(true);
            unsafe = unsafeConstructor.newInstance();
        } catch (Exception ex) {
            throw new TcpServerException(ex);
        }

        logger.debug("addr:{}, port:{}", addr, port);
        this.addr = addr;
        this.port = port;
    }

    @SuppressWarnings("java:S2139")
    public void start() {
        try {
            try (var serverSocketChannel = ServerSocketChannel.open()) {
                serverSocketChannel.configureBlocking(false);
                var serverSocket = serverSocketChannel.socket();
                serverSocket.bind(new InetSocketAddress(addr, port));
                try (var selector = Selector.open()) {
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    logger.info("server started. addr:{}, port:{}", addr, port);
                    while (run) {
                        handleSelector(selector);
                    }
                    logger.info("server stopped. addr:{}, port:{}", addr, port);
                }
            }
        } catch (Exception ex) {
            logger.error("error. addr:{}, port:{}", addr, port, ex);
            throw new TcpServerException(ex);
        }
    }

    public void stop() {
        logger.info("stop command received. addr:{}, port:{}", addr, port);
        run = false;
    }

    public BlockingQueue<SocketAddress> getConnectedClientsEvents() {
        return connectedClientsEvents;
    }

    public BlockingQueue<SocketAddress> getDisConnectedClientsEvents() {
        return disConnectedClientsEvents;
    }

    public BlockingQueue<MessageFromClient> getMessagesFromClients() {
        return messagesFromClients;
    }

    public boolean send(SocketAddress clientAddress, byte[] data) {
        var result = messagesForClients.offer(new MessageForClient(clientAddress, data));
        logger.debug("Scheduled for sending to the client:{}, result:{}", clientAddress, result);
        return result;
    }

    private void handleSelector(Selector selector) {
        try {
            selector.select(this::performIO, TIME_OUT_MS);
            sendMessagesToClients();
        } catch (ClientCommunicationException ex) {
            var clintAddress = getSocketAddress(ex.getSocketChannel());
            logger.error("error in client communication:{}", clintAddress, ex);
            disconnect(clintAddress);
        } catch (Exception ex) {
            logger.error("unexpected error:{}", ex.getMessage(), ex);
        }
    }

    private void performIO(SelectionKey selectedKey) {
    	if (selectedKey.isAcceptable()) {
            acceptConnection(selectedKey);
        } else if (selectedKey.isReadable()) {
            readFromClient(selectedKey);
        }
    }

    private void acceptConnection(SelectionKey key) {
        var serverSocketChannel = (ServerSocketChannel) key.channel();
        try {
            var clientSocketChannel = serverSocketChannel.accept();
            var selector = key.selector();
            logger.debug(
                    "accept client connection, key:{}, selector:{}, clientSocketChannel:{}",
                    key,
                    selector,
                    clientSocketChannel);

            clientSocketChannel.configureBlocking(false);
            
            if (messagesForClients.isEmpty()) {
            	clientSocketChannel.register(selector, SelectionKey.OP_READ);
            } else {
            	clientSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }

            var remoteAddress = clientSocketChannel.getRemoteAddress();
            clients.put(remoteAddress, clientSocketChannel);
            connectedClientsEvents.add(remoteAddress);
        } catch (Exception ex) {
            logger.error("can't accept new client on:{}", key);
        }
    }

    private void disconnect(SocketAddress clientAddress) {
        var clientChannel = clients.remove(clientAddress);
        if (clientChannel != null) {
            try {
                clientChannel.close();
            } catch (IOException e) {
                logger.error("clientChannel:{}, closing error:{}", clientAddress, e.getMessage(), e);
            }
        }
        logger.debug(
                "messagesFromClientsCounter:{}, bytesFromClientsCounter:{}",
                messagesFromClientsCounter,
                bytesFromClientsCounter);
        disConnectedClientsEvents.add(clientAddress);
    }

    private void readFromClient(SelectionKey selectionKey) {
        var socketChannel = (SocketChannel) selectionKey.channel();
        logger.debug("{}. read from client", socketChannel);

        var data = readRequest(socketChannel);
        bytesFromClientsCounter += data.length;
        if (data.length == 0) {
            disconnect(getSocketAddress(socketChannel));
        } else {
            messagesFromClientsCounter++;
            messagesFromClients.add(new MessageFromClient(getSocketAddress(socketChannel), data));
        }
    }

    private SocketAddress getSocketAddress(SocketChannel socketChannel) {
        try {
            return socketChannel.getRemoteAddress();
        } catch (Exception ex) {
            throw new ClientCommunicationException("get RemoteAddress error", ex, socketChannel);
        }
    }

    private byte[] readRequest(SocketChannel socketChannel) {
        try {
            int usedIdx = 0;
            int readBytesTotal = 0;
            int readBytes;
            while (readBytesTotal < RESULT_LIMIT && (readBytes = socketChannel.read(buffer)) > 0) {
                buffer.flip();
                if (usedIdx >= parts.size()) {
                    parts.add(ByteBuffer.allocateDirect(readBytes));
                }

                if (parts.get(usedIdx).capacity() < readBytes) {
                    unsafe.invokeCleaner(parts.get(usedIdx));
                    parts.add(usedIdx, ByteBuffer.allocateDirect(readBytes));
                }

                parts.get(usedIdx).put(buffer);
                buffer.flip();
                readBytesTotal += readBytes;
                usedIdx++;
            }
            logger.debug("read bytes:{}, usedIdx:{}", readBytesTotal, usedIdx);

            if (readBytesTotal == 0) {
                return EMPTY_ARRAY;
            }
            var result = new byte[readBytesTotal];
            var resultIdx = 0;

            for (var idx = 0; idx < usedIdx; idx++) {
                var part = parts.get(idx);
                part.flip();
                part.get(result, resultIdx, part.limit());
                resultIdx += part.limit();
                part.flip();
            }
            return result;
        } catch (Exception ex) {
            throw new ClientCommunicationException("Reading error", ex, socketChannel);
        }
    }

    private void sendMessagesToClients() {
        MessageForClient msg;
        while ((msg = messagesForClients.poll()) != null) {
            var client = clients.get(msg.clientAddress());
            if (client == null) {
                logger.error("client {} not found", msg.clientAddress());
            } else {
                write(client, msg.message());
            }
        }
    }

    private void write(SocketChannel clientChannel, byte[] data) {
        logger.debug("write to client:{}, data.length:{}", clientChannel, data.length);
        var bufferForWrite = ByteBuffer.allocate(data.length);
        bufferForWrite.put(data);
        bufferForWrite.flip();
        try {
            clientChannel.write(bufferForWrite);
        } catch (Exception ex) {
            throw new ClientCommunicationException("Write to the client error", ex, clientChannel);
        }
    }
}
