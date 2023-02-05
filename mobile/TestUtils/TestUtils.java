package com.yandex.launcher.testutils;

import com.yandex.google.common.io.CharStreams;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestUtils {
    public static String getFileContent(String fname) throws IOException {
        File file = new File(TestUtils.class.getResource("/" + fname).getFile());
        if (!file.exists()) {
            System.out.println("getFileContent " + fname + " doesn't exist");
            return null;
        }
        return CharStreams.toString(new FileReader(file));
    }
}
