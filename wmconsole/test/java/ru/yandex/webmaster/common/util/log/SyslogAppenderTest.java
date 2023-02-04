package ru.yandex.webmaster.common.util.log;

import org.junit.Assert;
import org.junit.Test;

public class SyslogAppenderTest {
    @Test
    public void testByteArrayBuilder_appendUnsignedInt_less0() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        try {
            builder.appendUnsignedInt(-1);
            Assert.fail("Method must throw");
        } catch (Exception e) {}
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_0() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(0);

        Assert.assertEquals(1, builder.getPosition());
        Assert.assertEquals((int)'0', (int)buffer[0]);
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_1() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(1);

        Assert.assertEquals(1, builder.getPosition());
        Assert.assertEquals((int)'1', (int)buffer[0]);
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_9() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(9);

        Assert.assertEquals(1, builder.getPosition());
        Assert.assertEquals((int)'9', (int)buffer[0]);
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_10() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(10);

        Assert.assertEquals(2, builder.getPosition());
        Assert.assertEquals((int)'1', (int)buffer[0]);
        Assert.assertEquals((int)'0', (int)buffer[1]);
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_11() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(11);

        Assert.assertEquals(2, builder.getPosition());
        Assert.assertEquals((int)'1', (int)buffer[0]);
        Assert.assertEquals((int)'1', (int)buffer[1]);
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_99() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(99);

        Assert.assertEquals(2, builder.getPosition());
        Assert.assertEquals((int)'9', (int)buffer[0]);
        Assert.assertEquals((int)'9', (int)buffer[1]);
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_100() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(100);

        Assert.assertEquals(3, builder.getPosition());
        Assert.assertEquals((int)'1', (int)buffer[0]);
        Assert.assertEquals((int)'0', (int)buffer[1]);
        Assert.assertEquals((int)'0', (int)buffer[2]);
    }

    @Test
    public void testByteArrayBuilder_appendUnsignedInt_101() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        builder.appendUnsignedInt(101);

        Assert.assertEquals(3, builder.getPosition());
        Assert.assertEquals((int)'1', (int)buffer[0]);
        Assert.assertEquals((int)'0', (int)buffer[1]);
        Assert.assertEquals((int)'1', (int)buffer[2]);
    }

    @Test
    public void testBsdMessage_appendSmallIntWithPrefix_1() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        SyslogAppender.BsdMessage.addSmallIntWithPrefix(builder, 0, ' ');

        Assert.assertEquals(2, builder.getPosition());
        Assert.assertEquals((int)' ', (int)buffer[0]);
        Assert.assertEquals((int)'0', (int)buffer[1]);
    }

    @Test
    public void testBsdMessage_appendSmallIntWithPrefix_10() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        SyslogAppender.BsdMessage.addSmallIntWithPrefix(builder, 10, ' ');

        Assert.assertEquals(2, builder.getPosition());
        Assert.assertEquals((int)'1', (int)buffer[0]);
        Assert.assertEquals((int)'0', (int)buffer[1]);
    }

    @Test
    public void testBsdMessage_appendSmallIntWithPrefix_01() throws Exception {
        byte[] buffer = new byte[10];
        SyslogAppender.ByteArrayBuilder builder = new SyslogAppender.ByteArrayBuilder(buffer);
        SyslogAppender.BsdMessage.addSmallIntWithPrefix(builder, 1, '0');

        Assert.assertEquals(2, builder.getPosition());
        Assert.assertEquals((int)'0', (int)buffer[0]);
        Assert.assertEquals((int)'1', (int)buffer[1]);
    }
}
