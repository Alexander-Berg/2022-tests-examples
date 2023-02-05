package ru.yandex.disk.trash;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.AppStartSessionProvider;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.fetchfilelist.SyncException;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.RemoteRepoOnNext;
import ru.yandex.disk.remote.RestApiClient;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;

import javax.annotation.NonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

@NonnullByDefault
@RunWith(RobolectricTestRunner.class)
public class FetchTrashItemsCommandTest extends AndroidTestCase2 {

    private FakeRemoteRepo fakeRemoteRepo;
    private EventLogger eventLogger;
    private TrashDatabase trashDatabase;
    private FetchTrashItemsCommand command;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        fakeRemoteRepo = new FakeRemoteRepo();
        eventLogger = new EventLogger();
        TrashDatabaseOpenHelper trashDatabaseOpenHelper =
                new TrashDatabaseOpenHelper(getMockContext(), "index.test.db", 1);
        trashDatabase = TestObjectsFactory.createTrashDatabase(trashDatabaseOpenHelper, eventLogger);

        command = new FetchTrashItemsCommand(fakeRemoteRepo, trashDatabase, eventLogger,
                new CredentialsManagerWithUser("u"));
    }

    @Test
    public void shouldFetchFromRestApiToTrashDatabase() throws Exception {
        fakeRemoteRepo.addTrashItem(new TrashItem.Builder()
                .setPath("trash:/ABC")
                .build());

        command.execute(new FetchTrashItemsCommandRequest());

        assertThat(trashDatabase.queryAll().getCount(), equalTo(1));
        assertThat(eventLogger.getCount(), equalTo(3));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.TrashListChanged.class));
        assertThat(eventLogger.get(1), instanceOf(DiskEvents.FetchTrashItemsSucceeded.class));
        assertThat(eventLogger.get(2), instanceOf(DiskEvents.FetchTrashItemsFinished.class));
    }

    @Test
    public void shouldSendFetchTrashItemsFinished() throws Exception {
        command.execute(new FetchTrashItemsCommandRequest());

        assertThat(eventLogger.findByClass(DiskEvents.FetchTrashItemsFinished.class),
                not(nullValue()));
    }

    @Test
    public void shouldSendFetchTrashItemsFailedOnException() throws Exception {
        fakeRemoteRepo.throwException(new RemoteExecutionException("test"));

        command.execute(new FetchTrashItemsCommandRequest());

        assertThat(eventLogger.findByClass(DiskEvents.FetchTrashItemsFailed.class),
                not(nullValue()));
        assertThat(eventLogger.findByClass(DiskEvents.FetchTrashItemsFinished.class),
                not(nullValue()));
    }

    @Test
    public void shouldNotCommitSyncerOnException() throws Exception {
        trashDatabase.updateOrInsert(new TrashItemRow(new TrashItem.Builder()
                .setPath("trash:/ABC")
                .build()));

        fakeRemoteRepo.throwException(new RemoteExecutionException("test"));

        command.execute(new FetchTrashItemsCommandRequest());

        assertThat(trashDatabase.queryAll().getCount(), equalTo(1));
    }

    private static class FakeRemoteRepo extends RemoteRepo {
        private List<TrashItemInfo> trashItemInfos = new ArrayList<>();

        public FakeRemoteRepo() {
            super(mock(Credentials.class), mock(WebdavClient.Pool.class), mock(RestApiClient.class),
                mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false), mock(AppStartSessionProvider.class));
        }

        @Override
        public void listTrash(
            RemoteRepoOnNext<TrashItem, SyncException> callback
        ) throws RemoteExecutionException, SyncException {
            for (TrashItemInfo info : trashItemInfos) {
                if (info.exception != null) {
                    throw info.exception;
                }
                callback.onNext(info.item);
            }
        }

        public void addTrashItem(TrashItem trashItem) {
            TrashItemInfo info = new TrashItemInfo();
            info.item = trashItem;
            trashItemInfos.add(info);
        }

        public void throwException(RemoteExecutionException e) {
            TrashItemInfo info = new TrashItemInfo();
            info.exception = e;
            trashItemInfos.add(info);
        }

        private static class TrashItemInfo {
            TrashItem item;
            RemoteExecutionException exception;
        }
    }

}
