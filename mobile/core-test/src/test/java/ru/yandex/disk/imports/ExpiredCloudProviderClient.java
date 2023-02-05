package ru.yandex.disk.imports;

import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.InputStream;

class ExpiredCloudProviderClient extends CloudProviderClient {
    public ExpiredCloudProviderClient() {
        super(null);
    }

    @Override
    public String getFileName(Uri uri) throws FileNotFoundException {
        throw new SecurityException();
    }

    @Override
    public InputStream openStream(Uri uri) throws FileNotFoundException {
        throw new SecurityException();
    }
}
