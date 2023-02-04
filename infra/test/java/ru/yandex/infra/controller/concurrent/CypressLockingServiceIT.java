package ru.yandex.infra.controller.concurrent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.YtUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class CypressLockingServiceIT {
    private YPath lockPath;
    private YPath epochPath;
    private static String proxyAddress;
    private static Yt ytClient;

    @BeforeAll
    static void beforeAll() {
        proxyAddress = System.getenv("YT_HTTP_PROXY_ADDR");
        YtConfiguration.Builder builder = YtUtils.getDefaultConfigurationBuilder(proxyAddress, "");
        ytClient = Yt.builder(builder.withSimpleCommandsRetries(0).build())
                .http()
                .build();
    }

    @BeforeEach
    void before() {
        YPath lockDir = YPath.simple("//tmp").child(UUID.randomUUID().toString());
        lockPath = lockDir.child("lock");
        epochPath = lockDir.child("epoch");

        ytClient.cypress().create(lockDir, CypressNodeType.MAP);
    }

    @Test
    void acquireLock() {
        CypressLockingService lockingService = createService();
        assertThat(lockingService.lock(), equalTo(1L));
    }

    @Test
    void notAcquireSimultaneousLocks() {
        CypressLockingService service1 = createService();
        service1.lock();

        CypressLockingService service2 = createService();
        CompletableFuture<?> locked = new CompletableFuture<>();
        Thread lockThread = new Thread(() -> {
            service2.lock();
            locked.complete(null);
        });
        lockThread.start();
        // Testing shows that taking lock requires about 500 ms
        Assertions.assertThrows(TimeoutException.class, () -> locked.get(2, TimeUnit.SECONDS));
        lockThread.interrupt();
    }

    @Test
    void acquireLostLock() {
        CypressLockingService service1 = createService();
        long epoch = service1.lock();

        CypressLockingService service2 = createService();
        service1.getInternalLock().unlock();
        assertThat(service2.lock(), equalTo(epoch + 1));
    }

    @Test
    void leaderServiceWorks() {
        CypressLockingService lockingService = createService();
        LeaderService leaderService = new LeaderServiceImpl("test", lockingService, new MetricRegistry());
        leaderService.ensureLeadership();
        assertThat("Lock must be taken", lockingService.getInternalLock().isTaken());
    }

    private CypressLockingService createService() {
        return new CypressLockingService(proxyAddress, "", lockPath, epochPath);
    }
}
