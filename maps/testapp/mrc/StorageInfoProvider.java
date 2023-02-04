package com.yandex.maps.testapp.mrc;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.ContextCompat;

import com.yandex.runtime.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageInfoProvider {
    private Context context;
    private List<StorageInfo> storageInfos = new ArrayList<>();

    public StorageInfoProvider(Context context) {
        this.context = context;
        reloadInfo(context);
    }

    public void reloadInfo(Context context) {
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);

        storageInfos.clear();
        for (File file : externalFilesDirs) {
            if (file == null)
                continue;

            try {
                boolean isRemovable = isRemovalbe(file);
                storageInfos.add(new StorageInfo(file, isRemovable));
            } catch (IllegalArgumentException e) {
                Logger.warn("Failed to read storage info: " + file.toString());
            }
        }
    }

    public StorageInfo getBuiltInStorage() {
        for (StorageInfo info : storageInfos) {
            if (!info.removable) {
                return info;
            }
        }
        return new StorageInfo(context.getFilesDir(), false);
    }

    public StorageInfo getRemovableStorage() {
        for (StorageInfo info : storageInfos) {
            if (info.removable) {
                return info;
            }
        }
        return null;
    }

    public static boolean isRemovalbe(File file) {
        return Environment.isExternalStorageRemovable(file);
    }

    private static boolean isRemovalbeStorageHeuristic(File file) {
        String path = file.getAbsolutePath();
        return (!path.contains("emulated") &&
                (path.contains("/mnt") || path.contains("/storage") || path.contains("/sdcard")));
    }
}
