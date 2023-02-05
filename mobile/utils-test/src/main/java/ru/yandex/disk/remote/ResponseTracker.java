package ru.yandex.disk.remote;

import okio.BasicResponseBody;
import ru.yandex.disk.test.OpenInfo;
import ru.yandex.disk.test.ResourceTracker;

public class ResponseTracker extends ResourceTracker {

    public void track(final BasicResponseBody body) {
        add(new OpenInfo("response body") {
            @Override
            protected boolean isClosed() {
                return body.isClosed();
            }
        });
    }

}
