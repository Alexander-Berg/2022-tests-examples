package ru.yandex.disk.cache;

import android.content.Context;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.upload.StorageListProviderStub;
import ru.yandex.disk.util.Diagnostics;

import java.io.File;

import static org.mockito.Mockito.mock;

public class StorageStub extends ApplicationStorage {

    private boolean currentPartitionPrimary;
    private File storageDir;

    public StorageStub(Context context,
                       ApplicationSettings applicationSettings, CredentialsManager cm,
                       Diagnostics diagnostics,
                       DeveloperSettings developerSettings) {

        super(context, applicationSettings, cm, mock(CommandStarter.class), new StorageListProviderStub(),
            diagnostics, developerSettings);
    }

    @Override
    public CachePartition getCurrentCachePartition() {
        return new CachePartition(currentPartitionPrimary, "");
    }

    public void setCurrentPartitionAsPrimary(boolean primary) {
        this.currentPartitionPrimary = primary;
    }


    @Override
    public File getStorage() {
        return storageDir;
    }

    public void setStorageDir(File storageDir) {
        this.storageDir = storageDir;
    }
}
