package ru.yandex.qe.dispenser.domain.util;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public final class FileUtilsTest {
    @Disabled
    @Test
    public void directoryInResourcesShouldBeFound() throws Exception {
        final String path = "server/domain/src/test/resources/test";
        final File existingFile = FileUtils.find(path);
        Assertions.assertTrue(existingFile.exists());
        Assertions.assertTrue(existingFile.getPath().endsWith(path));
    }
}
