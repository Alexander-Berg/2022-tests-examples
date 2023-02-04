package ru.yandex.solomon.alert.notification.channel.cloud;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import yandex.cloud.auth.api.AsyncCloudAuthClient;
import yandex.cloud.auth.api.Resource;
import yandex.cloud.auth.api.Subject;
import yandex.cloud.auth.api.credentials.AbstractCredentials;

import ru.yandex.solomon.ut.ManualClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.misc.concurrent.CompletableFutures.join;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class CloudAuthClientTest {
    private ManualClock clock;
    private CloudAuthClientStub cloudAuthClientProxy;
    private AccountingCloudAuthClient accountAuthClient;
    private CloudAuthClient cloudAuthClient;

    private static class AccountingCloudAuthClient implements AsyncCloudAuthClient {
        private final AsyncCloudAuthClient proxy;
        private long authenticateCalls;

        public long getAuthenticateCalls() {
            return authenticateCalls;
        }

        public long getAuthorizeCalls() {
            return authorizeCalls;
        }

        private long authorizeCalls;

        public AccountingCloudAuthClient(AsyncCloudAuthClient proxy) {
            this.proxy = proxy;
            authenticateCalls = 0;
            authorizeCalls = 0;
        }

        @Override
        public CompletableFuture<Subject> authenticate(AbstractCredentials credentials) {
            authenticateCalls++;
            return proxy.authenticate(credentials);
        }

        @Override
        public CompletableFuture<Subject> authorize(AbstractCredentials credentials, String permission, Stream<Resource> path) {
            authorizeCalls++;
            return proxy.authorize(credentials, permission, path);
        }

        @Override
        public CompletableFuture<Subject> authorize(Subject.Id subjectId, String permission, Stream<Resource> path) {
            authorizeCalls++;
            return proxy.authorize(subjectId, permission, path);
        }
    }

    @Before
    public void setUp() {
        clock = new ManualClock();
        cloudAuthClientProxy = new CloudAuthClientStub();
        accountAuthClient = new AccountingCloudAuthClient(cloudAuthClientProxy);
        cloudAuthClient = new CloudAuthClient(accountAuthClient, clock);

        cloudAuthClientProxy.add("uranix", "monitoring.data.read", Resource.folder("f1"), Resource.folder("f2"));
        cloudAuthClientProxy.add("uranix", "monitoring.data.write", Resource.folder("f1"));
        cloudAuthClientProxy.add("vpupkin", "monitoring.data.drop", Resource.folder("all"));
    }

    @Test
    public void cachingWorks() {
        for (int i = 0; i < 5; i++) {
            assertTrue(join(cloudAuthClient.isAuthorizedToReadData("f1", "uranix")));
        }

        for (int i = 0; i < 15; i++) {
            assertFalse(join(cloudAuthClient.isAuthorizedToReadData("f3", "uranix")));
        }

        for (int i = 0; i < 10; i++) {
            assertFalse(join(cloudAuthClient.isAuthorizedToReadData("all", "vpupkin")));
        }

        assertEquals(3, accountAuthClient.getAuthorizeCalls());
    }

    @Test
    public void renewCacheEntry() {
        for (int i = 0; i < 5; i++) {
            assertTrue(join(cloudAuthClient.isAuthorizedToReadData("f1", "uranix")));
        }

        clock.passedTime(1000, TimeUnit.SECONDS);

        for (int i = 0; i < 3; i++) {
            assertTrue(join(cloudAuthClient.isAuthorizedToReadData("f1", "uranix")));
        }

        assertEquals(2, accountAuthClient.getAuthorizeCalls());
    }


    @Test
    public void iamOffline() {
        for (int i = 0; i < 5; i++) {
            assertTrue(join(cloudAuthClient.isAuthorizedToReadData("f1", "uranix")));
        }

        cloudAuthClientProxy.setEnabled(false);
        clock.passedTime(1000, TimeUnit.SECONDS);

        for (int i = 0; i < 3; i++) {
            assertTrue(join(cloudAuthClient.isAuthorizedToReadData("f1", "uranix")));
        }

        cloudAuthClientProxy.setEnabled(true);

        for (int i = 0; i < 10; i++) {
            assertTrue(join(cloudAuthClient.isAuthorizedToReadData("f1", "uranix")));
        }

        assertEquals(1 + 3 + 1, accountAuthClient.getAuthorizeCalls());
    }

    @Test
    public void iamOfflineNotCached() {
        for (int i = 0; i < 5; i++) {
            assertTrue(join(cloudAuthClient.isAuthorizedToReadData("f1", "uranix")));
        }

        cloudAuthClientProxy.setEnabled(false);
        clock.passedTime(1000, TimeUnit.SECONDS);

        int fails = 0;
        for (int i = 0; i < 3; i++) {
            try {
                join(cloudAuthClient.isAuthorizedToReadData("f3", "uranix"));
            } catch (CompletionException e) {
                fails++;
            }
        }

        assertEquals(1 + 3, accountAuthClient.getAuthorizeCalls());
        assertEquals(3, fails);
    }
}
