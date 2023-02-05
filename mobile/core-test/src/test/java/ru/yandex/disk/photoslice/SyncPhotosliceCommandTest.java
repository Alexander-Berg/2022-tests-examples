package ru.yandex.disk.photoslice;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.remote.exceptions.TemporaryException;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.settings.markers.AlbumsSettings;
import ru.yandex.disk.sync.PhotosliceSyncStateManager;
import ru.yandex.disk.test.AndroidTestCase2;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Config(manifest = Config.NONE)
public class SyncPhotosliceCommandTest extends AndroidTestCase2 {

    private EventLogger eventLogger;
    private PhotosliceStructureSyncer mockStructureSyncer;

    private SyncPhotosliceCommand command;
    private PhotosliceStructureSyncer.Callback callback;
    private PhotosliceSyncStateManager mockSyncStateManager;

    public void setUp() throws Exception {
        super.setUp();
        eventLogger = new EventLogger();
        mockSyncStateManager = mock(PhotosliceSyncStateManager.class);
        when(mockSyncStateManager.isSyncAllowed()).thenReturn(true);
        when(mockSyncStateManager.isReadyToShow()).thenReturn(true);
        PhotosliceStructureSyncerFactory factory = mock(PhotosliceStructureSyncerFactory.class);
        mockStructureSyncer = mock(PhotosliceStructureSyncer.class);
        when(factory.create(any(PhotosliceStructureSyncer.Callback.class))).then(invocation -> {
            callback = (PhotosliceStructureSyncer.Callback) invocation.getArguments()[0];
            return mockStructureSyncer;
        });
        doAnswer(invocation -> {
            callback.onMinimumMetaLoaded();
            return null;
        }).when(mockStructureSyncer).syncStructure();
        final CredentialsManagerWithUser credentialsManagerWithUser = new CredentialsManagerWithUser("u");
        command = new SyncPhotosliceCommand(factory, eventLogger, mockSyncStateManager,
                new CommandLogger(), mock(MomentsDatabase.class),
                credentialsManagerWithUser, mock(AlbumsSettings.class),
                credentialsManagerWithUser.getActiveAccountCredentials());
    }

    @Test
    public void shouldSendPhotosliceSyncProgressEvents() throws Exception {
        command.execute(new SyncPhotosliceCommandRequest());

        assertThat(eventLogger.get(0), instanceOf(DiskEvents.SyncPhotosliceSucceeded.class));
        assertThat(eventLogger.get(1), instanceOf(DiskEvents.SyncPhotosliceFinished.class));
    }

    @Test
    public void shouldSendSyncPhotosliceFailedOnSyncException() throws Exception {
        doThrow(new TemporaryException("test")).when(mockStructureSyncer).syncStructure();

        command.execute(new SyncPhotosliceCommandRequest());

        assertThat(eventLogger.get(0), instanceOf(DiskEvents.SyncPhotosliceFailed.class));
        assertThat(eventLogger.get(1), instanceOf(DiskEvents.SyncPhotosliceFinished.class));
    }

    @Test
    public void shouldHaveOneRunningCommand() throws Exception {
        doAnswer(new Answer() {
            boolean firstInvocation = true;

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (firstInvocation) {
                    firstInvocation = false;
                    command.execute(null);
                }
                return null;
            }
        }).when(mockStructureSyncer).syncStructure();

        command.execute(new SyncPhotosliceCommandRequest());

        verify(mockStructureSyncer, times(2)).syncStructure();
    }

    @Test
    public void shouldNotStuckAfterFirstExecution() throws Exception {
        command.execute(new SyncPhotosliceCommandRequest());
        command.execute(new SyncPhotosliceCommandRequest());

        verify(mockStructureSyncer, times(2)).syncStructure();
    }

    @Test
    public void shouldSyncIfValid() throws Exception {
        command.execute(new SyncPhotosliceCommandRequest());

        verify(mockStructureSyncer).syncStructure();
    }

    @Test
    public void shouldMarkValidWhenFinish() throws Exception {

        command.execute(new SyncPhotosliceCommandRequest());

        verify(mockSyncStateManager).markReadyToShow(true);
    }

    @Test
    public void shouldMarkValidInEndIfForced() throws Exception {
        when(mockSyncStateManager.isReadyToShow()).thenReturn(false);

        command.execute(new SyncPhotosliceCommandRequest());

        verify(mockSyncStateManager).markReadyToShow(false);
        verify(mockSyncStateManager).markReadyToShow(true);
    }

}
