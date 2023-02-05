package ru.yandex.disk.notifications;

import org.junit.Test;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.SubscriptionId;
import ru.yandex.disk.remote.exceptions.TemporaryException;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.settings.UserSettings;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class UbsubscribeToRemoteUpdatesCommandTest extends AndroidTestCase2 {

    private UbsubscribeToRemoteUpdatesCommand command;
    private RemoteRepo mockRemoteRepo;
    private UserSettings userSettings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mockRemoteRepo = mock(RemoteRepo.class);
        final ApplicationSettings applicationSettings = TestObjectsFactory.createApplicationSettings(getMockContext());
        userSettings = applicationSettings.getUserSettings(TestObjectsFactory.createCredentials());

        command = new UbsubscribeToRemoteUpdatesCommand(userSettings, mockRemoteRepo);
    }

    @Test
    public void shouldUnsubscribeToRemoteRepo() throws Exception {
        final SubscriptionId subscriptionId = createTestSubscriptionId();
        userSettings.setSubscriptionId(subscriptionId);

        command.execute(new UbsubscribeToRemoteUpdatesCommandRequest());

        verify(mockRemoteRepo).unsubscribe(subscriptionId);
        assertThat(userSettings.getSubscriptionId(), is(nullValue()));
    }

    @Test
    public void shouldResetEvenIfException() throws Exception {
        final SubscriptionId subscriptionId = createTestSubscriptionId();
        userSettings.setSubscriptionId(subscriptionId);

        doThrow(new TemporaryException("test")).when(mockRemoteRepo).unsubscribe(subscriptionId);

        command.execute(new UbsubscribeToRemoteUpdatesCommandRequest());

        verify(mockRemoteRepo).unsubscribe(subscriptionId);
        assertThat(userSettings.getSubscriptionId(), is(nullValue()));
    }

    @Test
    public void shouldNotUnsubscribeIfNoId() throws Exception {
        final SubscriptionId subscriptionId = createTestSubscriptionId();
        userSettings.setSubscriptionId(subscriptionId);

        command.execute(new UbsubscribeToRemoteUpdatesCommandRequest());

        verify(mockRemoteRepo).unsubscribe(subscriptionId);
    }

    private static SubscriptionId createTestSubscriptionId() {
        return new SubscriptionId("test", "test", "test");
    }
}
