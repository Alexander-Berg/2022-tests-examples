package com.yandex.mail;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.RestClient;
import com.yandex.mail.disk.DiskInterface;
import com.yandex.mail.disk.DiskModule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;

public class MockDiskModule extends DiskModule {

    @Override
    @NonNull
    public DiskInterface provideDisk(@NonNull OkHttpClient client) {
        return new DiskInterface() {
            @Override
            @Nullable
            public Boolean supportsDisk(@NonNull Credentials credentials) {
                return false; // no Disk in tests!
            }

            @NonNull
            @Override
            public RestClient getClient(@NonNull Credentials credentials) {
                throw new UnsupportedOperationException(); // no Disk in tests!
            }
        };
    }
}
