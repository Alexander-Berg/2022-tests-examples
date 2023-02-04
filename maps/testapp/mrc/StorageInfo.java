package com.yandex.maps.testapp.mrc;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class StorageInfo {
    public final File path;
    public final boolean removable;

    protected StorageInfo(@NonNull File path, boolean removable) {
        this.path = path;
        this.removable = removable;
    }

    @NotNull
    @Override
    public String toString() {
        return "StorageInfo{" +
                "path='" + path + '\'' +
                ", removable=" + removable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StorageInfo that = (StorageInfo) o;
        return path.equals(that.path);

    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
