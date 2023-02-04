package ru.yandex.qe.hitman.testing.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author ortemij
 * 02/03/16 20:37
 */
public class TestResourceUtil {

    private TestResourceUtil() {
    }

    public static File copyResourceToTmpFile(String resource) {
        try {
            File tempFile = File.createTempFile("file", "");
            tempFile.deleteOnExit();

            try (InputStream in = TestResourceUtil.class.getResourceAsStream(resource)) {
                try (OutputStream out = new FileOutputStream(tempFile)) {
                    IOUtils.copy(in, out);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
