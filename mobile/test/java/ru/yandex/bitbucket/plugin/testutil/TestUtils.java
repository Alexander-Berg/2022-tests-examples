package ru.yandex.bitbucket.plugin.testutil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestUtils {
    public static void assertSerializable(Object testClassInstance) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(testClassInstance);
            objectOutputStream.flush();

            byte[] serialized = byteArrayOutputStream.toByteArray();
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(serialized));
            Object after = objectInputStream.readObject();

            assertEquals(testClassInstance, after);
        } catch (ClassNotFoundException | IOException e) {
            fail(e.toString());
        }
    }
}
