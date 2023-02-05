package ru.yandex.disk.sync;

import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.DeveloperSettings;

import static org.mockito.Mockito.mock;

public class SyncStateManagerStub extends OfflineSyncStateManager {

    public SyncStateManagerStub() {
        super(null, null, mock(CredentialsManager.class), null, null, null, mock(DeveloperSettings.class));
    }

    @Override
    public boolean isSyncAutomatically() {
        return true;
    }

    @Override
    public boolean isSyncAllowed() {
        return true;
    }
}
