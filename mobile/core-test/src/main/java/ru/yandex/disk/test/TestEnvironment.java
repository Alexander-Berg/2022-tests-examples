package ru.yandex.disk.test;

import android.os.Environment;

import java.io.File;

public class TestEnvironment {

    public static File getTestRootDirectory() {
        File testDirectory = new File(Environment.getExternalStorageDirectory(), "disk-unit-tests");
        testDirectory.mkdirs();
        return testDirectory;
    }

}
