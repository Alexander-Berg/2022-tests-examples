package com.yandex.mail.fakeserver;

import android.content.Context;

import com.yandex.mail.util.FakeServerHacks;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;
import timber.log.Timber;

import static com.yandex.mail.tools.CommonTestUtils.bypassStrictMode;

public class MockWebServerHelper {

    @NonNull
    private final Context context;

    @Nullable
    private MockWebServer server;

    public MockWebServerHelper(@NonNull Context context) {
        this.context = context;
    }

    @Nullable
    public MockWebServer getServer() {
        return server;
    }

    public void start() {
        bypassStrictMode(
                () -> {
                    System.setProperty("javax.net.ssl.trustStoreType", "JKS");
                    server = new MockWebServer();
                    try {
                        /*
                            It's sligtly easier to start on a specific port, however, doing that sometimes
                            results in "java.net.BindException: Address already in use" on CI

                            Also, if we will run tests in parallel at some point, it will be a problem as well.
                         */
                        server.start();
                        HttpUrl url = server.url("/");
                        Timber.i("Running fake server on url %s", url);
                        server.setDispatcher(FakeServer.getInstance().getDispatcher());
                        FakeServer.getInstance().setBaseUrl(url);
                        FakeServerHacks.setFakeServerUrl(context, url.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void shutdown() {
        bypassStrictMode(
                () -> {
                    FakeServerHacks.setFakeServerUrl(context, null);
                    assert server != null;
                    try {
                        server.shutdown();
                        Timber.i("Stopped fake server");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}
