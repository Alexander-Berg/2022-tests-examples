package ru.yandex.disk.upload;

import android.content.Context;
import android.os.Environment;
import org.mockito.Mockito;
import org.robolectric.shadows.ShadowEnvironment;
import ru.yandex.disk.autoupload.observer.BaseStorageListProvider;
import ru.yandex.disk.util.FileSystem;
import ru.yandex.disk.util.System;
import ru.yandex.disk.util.TestSystem;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageListProviderStub extends BaseStorageListProvider {

    public StorageListProviderStub() {
        super(getMockContext(), getMockSystem(), getMockFileSystem());
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    }

    @Override
    public File getPrimaryFilesDir() {
        return Environment.getExternalStorageDirectory();
    }

    @Override
    public List<File> getFilesDirs() {
        return Collections.singletonList(getPrimaryFilesDir());
    }

    @Nonnull
    @Override
    public List<StorageInfo> getSecondaryStorages() {
        return new ArrayList<>();
    }

    private static Context getMockContext() {
        return Mockito.mock(Context.class);
    }

    private static System getMockSystem() {
        return new TestSystem();
    }

    private static FileSystem getMockFileSystem() {
        return FileSystem.getInstance();
    }
}
