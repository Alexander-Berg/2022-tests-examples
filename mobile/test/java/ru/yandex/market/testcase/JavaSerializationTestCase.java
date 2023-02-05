package ru.yandex.market.testcase;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public abstract class JavaSerializationTestCase {

    @Test
    public void testClassProperlySerializable() throws IOException, ClassNotFoundException {
        final Object initialObject = getInstance();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(initialObject);
        objectOutputStream.close();

        final byte[] serializedBytes = byteArrayOutputStream.toByteArray();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedBytes);
        final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        final Object readObject = objectInputStream.readObject();

        assertThat(initialObject, equalTo(readObject));
    }

    @NonNull
    protected abstract Object getInstance();
}
