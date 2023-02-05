package ru.yandex.disk.trash;

import org.junit.Test;
import ru.yandex.disk.AppStartSessionProvider;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.Mocks;
import ru.yandex.disk.commonactions.CapacityInfoCache;
import ru.yandex.disk.commonactions.CapacityInfoCacheDB;
import ru.yandex.disk.commonactions.FetchCapacityInfoCommand;
import ru.yandex.disk.commonactions.FetchCapacityInfoCommandRequest;
import ru.yandex.disk.event.DiskEvents.RequestCapacityInfoFailed;
import ru.yandex.disk.event.DiskEvents.RequestCapacityInfoSucceeded;
import ru.yandex.disk.event.Event;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.remote.CapacityInfo;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.RestApiClient;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.settings.UserSettings;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static ru.yandex.disk.test.TestObjectsFactory.createCredentials;

public class FetchCapacityInfoCommandTest extends AndroidTestCase2 {

    private FetchCapacityInfoCommand command;
    private SeclusiveContext context;
    private EventLogger eventSender;
    private CapacityInfoCache cache;
    private FakeRemoteRepo fakeRemoteRepo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = new SeclusiveContext(mContext);
        context.setAccountManager(Mocks.mockAccountManager());
        eventSender = new EventLogger();

        fakeRemoteRepo = new FakeRemoteRepo();

        cache = new CapacityInfoCacheDB(getUserSettings());
        command = new FetchCapacityInfoCommand(fakeRemoteRepo, eventSender, cache);
    }

    private UserSettings getUserSettings() {
        ApplicationSettings appSettings = TestObjectsFactory.createApplicationSettings(context);
        return appSettings.getUserSettings(createCredentials());
    }

    @Test
    public void checkTrashSizeReceivedSucceeded() {
        Mocks.addContentProviders(context);
        command.execute(new FetchCapacityInfoCommandRequest());

        Event event = eventSender.get(0);
        assertThat(event, instanceOf(RequestCapacityInfoSucceeded.class));

        assertThat(cache.read().getTrashSize(), equalTo(16L));
    }

    @Test
    public void checkTrashSizeReceivedFailed() throws Exception {
        fakeRemoteRepo.throwException(new RemoteExecutionException("test"));
        command.execute(new FetchCapacityInfoCommandRequest());

        assertThat(eventSender.getAll().size(), equalTo(1));

        Event event = eventSender.get(0);
        assertThat(event, instanceOf(RequestCapacityInfoFailed.class));
    }

    private static class FakeRemoteRepo extends RemoteRepo {
        private RemoteExecutionException e;

        public FakeRemoteRepo() {
            super(mock(Credentials.class), mock(WebdavClient.Pool.class), mock(RestApiClient.class),
                mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false), mock(AppStartSessionProvider.class));
        }

        @Nonnull
        public CapacityInfo getCapacityInfo() throws RemoteExecutionException {
            if (e != null) {
                throw e;
            }
            return new CapacityInfo(100, 30, 16);
        }

        public void throwException(RemoteExecutionException e) {
            this.e = e;
        }

    }
}
