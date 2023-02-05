package com.yandex.frankenstein.io;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class IOReaderTest {

    private static final String UNKNOWN_FILE_PATH = "unknown.txt";
    private static final String TEXT = "Hello, World!";
    private static final byte[] TEXT_BYTES = TEXT.getBytes(Charset.defaultCharset());

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new FileReader(), IOReaderTest.class.getClassLoader().getResource("hello.txt").getPath()},
                {new ResourceReader(), "hello.txt"}
        });
    }

    @Parameter
    public IOReader mIoReader;

    @Parameter(1)
    public String mFileName;

    @Test
    public void testGetFile() throws IOException {
        final File textFile = mIoReader.getFile(mFileName);
        final byte[] actualText = Files.readAllBytes(textFile.toPath());

        assertThat(actualText.length).isEqualTo(TEXT_BYTES.length);
        assertThat(actualText).isEqualTo(TEXT_BYTES);
    }

    @Test
    public void testReadAsString() {
        final String actualText = mIoReader.readAsString(mFileName);

        assertThat(actualText).isEqualTo(TEXT);
    }

    @Test(expected = RuntimeException.class)
    public void testReadUnknownFileAsString() {
        mIoReader.readAsString(UNKNOWN_FILE_PATH);
    }

    @Test
    public void testReadAsByteArray() {
        final byte[] actualText = mIoReader.readAsByteArray(mFileName);

        assertThat(actualText).isEqualTo(TEXT_BYTES);
    }

    @Test(expected = RuntimeException.class)
    public void testReadUnknownFileAsByteArray() {
        mIoReader.readAsByteArray(UNKNOWN_FILE_PATH);
    }

    @Test
    public void testGetInputStream() throws IOException {
        final byte[] actualText = new byte[TEXT_BYTES.length];
        final InputStream inputStream = mIoReader.getInputStream(mFileName);
        final int bytesRead = inputStream.read(actualText);

        assertThat(bytesRead).isEqualTo(TEXT_BYTES.length);
        assertThat(actualText).isEqualTo(TEXT_BYTES);
    }

    @Test(expected = RuntimeException.class)
    public void testReadUnknownResource() {
        mIoReader.getInputStream(UNKNOWN_FILE_PATH);
    }
}
