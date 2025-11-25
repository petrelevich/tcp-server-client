package ru.tcp.server.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.tcp.logserver.parser.MsgParser.BEGIN_MESSAGE;
import static ru.tcp.logserver.parser.MsgParser.END_MESSAGE;
import static ru.tcp.logserver.parser.MsgParser.HEADER;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import ru.tcp.logserver.parser.MsgParserSm;

class MsgParserSmTest {

    @Test
    void parse_correct_msg() {
        // given
        var parser = new MsgParserSm();
        var clientAddress = new InetSocketAddress("localhost", 9090);

        var content = "test";

        // when
        parser.newMessage(clientAddress);
        var parsedMsgs = parser.parseMessage(clientAddress, makeCorrectMsg(content));

        // then
        assertThat(parsedMsgs).hasSize(1);
        assertThat(new String(parsedMsgs.get(0))).isEqualTo(content);
    }

    @Test
    void parse_correct_partial_msg() {
        // given
        var parser = new MsgParserSm();
        var clientAddress = new InetSocketAddress("localhost", 9090);

        var content = "testOPjz";
        var msg = makeCorrectMsg(content);
        var part1 = new byte[2];
        var part2 = new byte[3];
        var part3 = new byte[msg.length - part1.length - part2.length];

        System.arraycopy(msg, 0, part1, 0, part1.length);
        System.arraycopy(msg, part1.length, part2, 0, part2.length);
        System.arraycopy(msg, part1.length + part2.length, part3, 0, part3.length);

        // when
        parser.newMessage(clientAddress);
        var parsedMsgs1 = parser.parseMessage(clientAddress, part1);
        var parsedMsgs2 = parser.parseMessage(clientAddress, part2);
        var parsedMsgs3 = parser.parseMessage(clientAddress, part3);

        // then
        assertThat(parsedMsgs1).isEmpty();
        assertThat(parsedMsgs2).isEmpty();

        assertThat(parsedMsgs3).hasSize(1);
        assertThat(new String(parsedMsgs3.get(0))).isEqualTo(content);
    }

    @Test
    void parse_several_correct_msg() {
        // given
        var parser = new MsgParserSm();
        var clientAddress = new InetSocketAddress("localhost", 9090);

        var content1 = "test1";
        var content2 = "test2";
        var content3 = "test3";

        var msg1 = makeCorrectMsg(content1);
        var msg2 = makeCorrectMsg(content2);
        var msg3 = makeCorrectMsg(content3);

        var msgAll = new byte[msg1.length + msg2.length + msg3.length];
        System.arraycopy(msg1, 0, msgAll, 0, msg1.length);
        System.arraycopy(msg2, 0, msgAll, msg1.length, msg2.length);
        System.arraycopy(msg3, 0, msgAll, msg1.length + msg2.length, msg3.length);

        // when
        parser.newMessage(clientAddress);
        var parsedMsgs = parser.parseMessage(clientAddress, msgAll);

        // then
        assertThat(parsedMsgs).hasSize(3);
        assertThat(new String(parsedMsgs.get(0))).isEqualTo(content1);
        assertThat(new String(parsedMsgs.get(1))).isEqualTo(content2);
        assertThat(new String(parsedMsgs.get(2))).isEqualTo(content3);
    }

    @Test
    void parse_several_correct_msg_with_trash() {
        // given
        var parser = new MsgParserSm();
        var clientAddress = new InetSocketAddress("localhost", 9090);

        var content1 = "test1";
        var content2 = "test2";
        var content3 = "test3";

        var trash1 = new byte[] {0, 0, 0};
        var msg1 = makeCorrectMsg(content1);
        var trash2 = new byte[] {0, 0, 0};
        var msg2 = makeCorrectMsg(content2);
        var trash3 = new byte[] {0, 0, 0};
        var msg3 = makeCorrectMsg(content3);
        var trash4 = new byte[] {0, 0, 0};

        var msgAll = new byte
                [msg1.length
                        + msg2.length
                        + msg3.length
                        + trash1.length
                        + trash2.length
                        + trash3.length
                        + trash4.length];

        System.arraycopy(trash1, 0, msgAll, 0, trash1.length);
        System.arraycopy(msg1, 0, msgAll, trash1.length, msg1.length);
        System.arraycopy(trash2, 0, msgAll, trash1.length + msg1.length, trash2.length);
        System.arraycopy(msg2, 0, msgAll, trash1.length + msg1.length + trash2.length, msg2.length);
        System.arraycopy(trash3, 0, msgAll, trash1.length + msg1.length + trash2.length + msg2.length, trash3.length);
        System.arraycopy(
                msg3,
                0,
                msgAll,
                trash1.length + msg1.length + trash2.length + msg2.length + trash3.length,
                msg3.length);
        System.arraycopy(
                trash4,
                0,
                msgAll,
                trash1.length + msg1.length + trash2.length + msg2.length + trash3.length + msg3.length,
                trash4.length);

        // when
        parser.newMessage(clientAddress);
        var parsedMsgs = parser.parseMessage(clientAddress, msgAll);

        // then
        assertThat(parsedMsgs).hasSize(3);
        assertThat(new String(parsedMsgs.get(0))).isEqualTo(content1);
        assertThat(new String(parsedMsgs.get(1))).isEqualTo(content2);
        assertThat(new String(parsedMsgs.get(2))).isEqualTo(content3);
    }

    /**
     * message:
     * byte HEADER = 0x01;
     * byte BEGIN_MESSAGE = 0x02;
     * byte END_MESSAGE = 0x03;
     * <p>
     * int HEADER_INDEX = 0;
     * int LENGTH_INDEX = 1;
     * int BEGIN_MESSAGE_INDEX = 5;
     */
    private byte[] makeCorrectMsg(String content) {

        var length = content.getBytes().length;
        var messageLength = 1 + 4 + 1 + length + 1;
        var message = ByteBuffer.allocate(messageLength);
        message.put(HEADER);
        message.put(ByteBuffer.allocate(4).putInt(length).array());
        message.put(BEGIN_MESSAGE);
        message.put(content.getBytes());
        message.put(END_MESSAGE);
        return message.array();
    }
}
