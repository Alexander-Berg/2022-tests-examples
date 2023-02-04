package com.yandex.maps.testapp;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {
    public static File createFolder(Context context, String folderName) {
        File folder = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/" + folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    public static boolean clearFile(String filePath) {
        try {
            new FileOutputStream(filePath).close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean writeToFile(Context context, String fileName, String text) {
        try {
            File file = new File(fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(text);
            writer.close();
        } catch (IOException e) {
            Toast.makeText(context, "Can not write to file " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Nullable
    public static File testFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    public static String readFromFile(@NonNull File file) {
        try {
            BufferedReader bf = new BufferedReader(new FileReader(file));
            return bf.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
