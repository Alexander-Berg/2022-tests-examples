package com.yandex.launcher.loaders;

import android.content.Context;

import com.yandex.launcher.common.loaders.IServerConfiguration;

import java.util.Locale;

public class MockServerConfiguration implements IServerConfiguration {
    private final String host;
    private final int port;

    public MockServerConfiguration(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getAddress(Context context) {
        return String.format(Locale.getDefault(), "http://%s:%d", host, port);
    }

    @Override
    public String getAddress(Context context, String path) {
        return getAddress(context) + path;
    }

    @Override
    public String getSearchAddress(Context context) {
        // TODO
        return null;
    }
}
