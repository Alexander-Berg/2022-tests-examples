package ru.yandex.disk.notifications;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import ru.yandex.disk.fetchfilelist.PathLock;
import ru.yandex.disk.provider.ContentChangeNotifier;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.remote.MessagingCloud;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.SubscriptionId;
import ru.yandex.disk.remote.SubscriptionRequest;
import ru.yandex.disk.service.CommandsMap;
import ru.yandex.disk.settings.UserSettings;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.stats.AnalyticEventKeys;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.Diagnostics;
import ru.yandex.disk.util.Installation;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@Config(manifest = Config.NONE)
public class SubscribeToRemoteUpdatesCommandTest extends AndroidTestCase2 {

    private static final String TEST_REG_ID = "REG/ID";
    private static final String TEST_INST_ID = "INST_ID";
    private SubscribeToRemoteUpdatesCommand command;
    private SubscribeToRemoteUpdatesCommandRequest request;
    private RemoteRepo mockRemoteRepo;
    private UserSettings userSettings;
    private Diagnostics diagnostics;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        request = new SubscribeToRemoteUpdatesCommandRequest(TEST_REG_ID, MessagingCloud.GCM);
        final ContentChangeNotifier notifier = mock(ContentChangeNotifier.class);
        final DiskDatabase diskDatabase =
                TestObjectsFactory.createDiskDatabase(new SQLiteOpenHelper2(getMockContext(),
                        "test", 1), notifier, new PathLock());

        mockRemoteRepo = mock(RemoteRepo.class);
        final SubscriptionId subscriptionId = new SubscriptionId("id", "id", "test");
        when(mockRemoteRepo.subscribe(Mockito.any(SubscriptionRequest.class)))
                .thenReturn(subscriptionId);
        final Installation mockInstallation = mock(Installation.class);
        when(mockInstallation.id()).thenReturn(TEST_INST_ID);
        userSettings = TestObjectsFactory.createApplicationSettings(getMockContext())
            .getUserSettings(TestObjectsFactory.createCredentials());
        final ListenableFuture commandsMap = mock(ListenableFuture.class);
        when(commandsMap.get()).thenReturn(mock(CommandsMap.class));
        diagnostics = mock(Diagnostics.class);
        command = new SubscribeToRemoteUpdatesCommand(
                diskDatabase,
                mockRemoteRepo,
                mockInstallation,
                userSettings, diagnostics, Collections.emptyList());
    }

    @Test
    public void shouldBuildCorrectSubscriptionId() throws Exception {
        command.execute(request);

        final ArgumentCaptor<SubscriptionRequest> requestArgumentCaptor =
                ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(mockRemoteRepo).subscribe(requestArgumentCaptor.capture());
        final SubscriptionRequest subscriptionRequest = requestArgumentCaptor.getValue();

        final String expectedToken =
                "{\"p\":\"a\",\"c\":\"ru.yandex.disk\",\"d\":\"INST_ID\",\"t\":\"REG/ID\"}";
        assertThat(subscriptionRequest.getWebdavToken(), equalTo(expectedToken));
        assertThat(subscriptionRequest.getRegId(), equalTo(TEST_REG_ID));
        //TODO assertThat(subscriptionRequest.getFileList(), equalTo());
        assertThat(subscriptionRequest.getInstanceId(), equalTo(TEST_INST_ID));
    }

    @Test
    public void shouldSaveSubscriptionId() throws Exception {
        command.execute(request);

        assertThat(userSettings.getSubscriptionId(), is(notNullValue()));
    }

    @Test
    public void shouldSendAnalyticsOnNullSubscriptionId() {
        command.execute(new SubscribeToRemoteUpdatesCommandRequest());

        verify(diagnostics).send(AnalyticEventKeys.NULL_REGISTRATION_ID);
    }
}
