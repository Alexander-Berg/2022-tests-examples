package com.yandex.maps.testapp.guidance.performance;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;

final class Utilities {

    private static final String FILE_NAME = "com.yandex.maps.testapp.instrumentedtests.GuidancePerformanceTests.pb";

    static String getNetworkRecordFilePath(Context ctx) throws FileNotFoundException {
        File filesDir = ctx.getExternalFilesDir(null);

        if (filesDir == null) {
            throw new FileNotFoundException("No external storage available!");
        }

        return filesDir.getPath() + "/" + FILE_NAME;
    }
}
