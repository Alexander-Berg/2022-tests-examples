/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.util;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public final class ResourcesUtil {

    @NonNull
    public static String getTextFromFile(@NonNull final String fileName) throws IOException {
        return readFile(findResource(fileName));
    }

    @NonNull
    private static File findResource(@NonNull final String resourceName) {
        final URL fileURL = ResourcesUtil.class.getClassLoader().getResource(resourceName);
        return new File(fileURL.getFile());
    }

    @NonNull
    private static String readFile(@NonNull final File file) throws IOException {

        final StringBuilder fileContents = new StringBuilder((int) file.length());
        final Scanner scanner = new Scanner(file);

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine());
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }
}
