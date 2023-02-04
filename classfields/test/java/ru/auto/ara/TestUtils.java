package ru.auto.ara;

import ru.auto.util.L;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import droidninja.filepicker.utils.Utils;

public class TestUtils {

    public static String readFromFile(String fileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            return new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            L.e("parse file", "parsing error", e);
        } finally {
            Utils.closeSilently(is);
        }

        return "";
    }

}
