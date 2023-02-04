package ru.yandex.feedloader.storage.impl;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.yandex.feedloader.storage.FileStorageService;

import java.io.File;

/**
 * Test for [[RepeatableFileStorageService]]
 *
 * @author alesavin
 */
@RunWith(JUnit4.class)
public class RepeatableFileStorageServiceTest extends TestCase {

    @Test
    public void testRepeat() {
        FileStorageService delegate = new InMemoryFileStorageService();
        FileStorageService service =
                new RepeatableFileStorageService(delegate, 3, 1000L);

        service.put("test", new File(""));
    }

}
