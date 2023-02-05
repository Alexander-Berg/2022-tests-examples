package ru.yandex.market;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceHelper {

    @NonNull
    public static String getResponse(@NonNull final String resourcePath) {
        final StringBuilder res = new StringBuilder();
        try {
            final InputStream inputStream =
                    ResourceHelper.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String str = br.readLine();
                while (str != null) {
                    res.append(str);
                    str = br.readLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (res.length() == 0) {
            throw new IllegalStateException("Response not found " + resourcePath);
        }
        return res.toString();
    }
}
