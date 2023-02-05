package ru.yandex.disk.download;

import android.content.Intent;
import android.os.Bundle;
import org.junit.Test;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.util.SystemClock;

import static org.hamcrest.Matchers.notNullValue;

public class DownloadNotifierTest extends AndroidTestCase2 {

    private SeclusiveContext context;
    private DownloadNotifier notifier;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(getMockContext());
        notifier = new DownloadNotifier(SystemClock.REAL, new EventLogger());
    }

    @Test
    public void shouldSendCorrectReasonCode() throws Exception {
        // TODO
//        notifier.sendTaskFailedBroadcast(0);
//        assertThat(getLastBroadcastExtras().getInt(DiskIntents.EXTRA_REASON), equalTo(0));
//
//        notifier.sendNoSpaceAvailable(0);
//        assertThat(getLastBroadcastExtras().getInt(DiskIntents.EXTRA_REASON), equalTo(1));
    }

    private Bundle getLastBroadcastExtras() {
        Intent intent = context.getSendBroadcastRequests().getLast().getIntent();
        assertThat("intent", intent, notNullValue());
        Bundle extras = intent.getExtras();
        assertThat("extras", extras, notNullValue());
        return extras;
    }
}
