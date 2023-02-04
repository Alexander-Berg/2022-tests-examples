package ru.yandex.solomon.alert.cluster.balancer;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.AlertPersistStateSupport;
import ru.yandex.solomon.alert.dao.ProjectsHolderStub;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertStatesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertsDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryMutesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryNotificationsDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryShardsDao;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.balancer.ShardsHolder;
import ru.yandex.solomon.locks.ReadOnlyDistributedLockStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.util.host.HostUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.mute.domain.MuteTestSupport.randomMute;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomNotification;
import static ru.yandex.solomon.idempotency.IdempotentOperation.NO_OPERATION;

/**
 * @author Vladimir Gordiychuk
 */
public class ShardsHolderImplTest {

    private ManualClock clock;
    private ReadOnlyDistributedLockStub lock;
    private InMemoryShardsDao shardsDao;
    private InMemoryAlertsDao alertsDao;
    private InMemoryNotificationsDao channelsDao;
    private InMemoryMutesDao mutesDao;
    private InMemoryAlertStatesDao statesDao;
    private ShardsHolder holder;
    private ProjectsHolderStub projectsHolder;

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        lock = new ReadOnlyDistributedLockStub(clock);
        lock.setOwner(HostUtils.getFqdn());
        shardsDao = new InMemoryShardsDao();
        alertsDao = new InMemoryAlertsDao();
        channelsDao = new InMemoryNotificationsDao();
        mutesDao = new InMemoryMutesDao();
        statesDao = new InMemoryAlertStatesDao();
        projectsHolder = new ProjectsHolderStub();
        holder = new ShardsHolderImpl(projectsHolder, shardsDao, alertsDao, channelsDao, mutesDao, statesDao, lock);
    }

    @Test
    public void emptyShards() {
        holder.reload().join();
        assertEquals(Set.of(), holder.getShards());
    }

    @Test
    public void reloadShards() {
        holder.reload().join();
        shardsDao.insert("alice").join();
        projectsHolder.addProject("alice");
        holder.reload().join();
        assertEquals(Set.of("alice"), holder.getShards());

        shardsDao.insert("bob").join();
        projectsHolder.addProject("bob");
        holder.reload().join();
        assertEquals(Set.of("alice", "bob"), holder.getShards());
    }

    @Test
    public void addShard() {
        holder.reload().join();
        add("alice");
        assertEquals(Set.of("alice"), holder.getShards());
        assertEquals(List.of("alice"), shardsDao.findAll().join());
    }

    @Test
    public void deleteShardAlerts() {
        holder.reload().join();
        var alert = randomAlert();
        add(alert.getProjectId());
        alertsDao.insert(alert, NO_OPERATION).join();
        holder.delete(alert.getProjectId()).join();
        assertEquals(Set.of(), holder.getShards());
        assertEquals(List.of(), alertsDao.findAll(alert.getProjectId()).join());
        assertEquals(List.of(), shardsDao.findAll().join());
    }

    @Test
    public void deleteShardChannels() {
        holder.reload().join();
        var channel = randomNotification();
        add(channel.getProjectId());
        channelsDao.insert(channel, NO_OPERATION).join();
        holder.delete(channel.getProjectId()).join();
        assertEquals(Set.of(), holder.getShards());
        assertEquals(List.of(), channelsDao.findAll(channel.getProjectId()).join());
        assertEquals(List.of(), shardsDao.findAll().join());
    }

    @Test
    public void deleteShardState() {
        holder.reload().join();
        add("alice");
        var state = List.of(AlertPersistStateSupport.INSTANCE.randomState());
        statesDao.save("alice", clock.instant(), new AssignmentSeqNo(1, 42), state).join();
        holder.delete("alice").join();

        assertEquals(Set.of(), holder.getShards());
        assertEquals(List.of(), statesDao.findAll("alice").join());
        assertEquals(List.of(), shardsDao.findAll().join());
    }

    @Test
    public void unableDeleteWhenNotLeader() {
        holder.reload().join();
        add("alice");
        projectsHolder.addProject("alice");
        lock.setOwner(null);

        var exception = holder.delete("alice")
                .thenApply(unused -> null)
                .exceptionally(e -> e)
                .join();

        assertNotNull(exception);
    }

    @Test
    public void avoidDeleteShardIfResourceNotDeleted() {
        holder.reload().join();
        var alert = randomAlert();
        add(alert.getProjectId());
        alertsDao.insert(alert, NO_OPERATION).join();
        alertsDao.setError(new RuntimeException("One of DAO unavailable"));

        for (int index = 0; index < 3; index++) {
            var exception = holder.delete(alert.getProjectId())
                    .thenApply(unused -> null)
                    .exceptionally(e -> e)
                    .join();

            assertNotNull(exception);
            assertEquals(Set.of(alert.getProjectId()), Set.copyOf(holder.getShards()));
            assertEquals(List.of(alert.getProjectId()), shardsDao.findAll().join());
        }

        alertsDao.setError(null);
        holder.delete(alert.getProjectId()).join();
        assertEquals(Set.of(), holder.getShards());
        assertEquals(List.of(), alertsDao.findAll(alert.getProjectId()).join());
        assertEquals(List.of(), shardsDao.findAll().join());
    }

    @Test
    public void removeFromDeleteListWhenDeleteInFlight() {
        holder.reload().join();
        add("alice");
        assertEquals(Set.of("alice"), holder.getShards());
        holder.delete("alice"); // don't wait complete
        assertEquals(Set.of(), holder.getShards());
    }

    @Test
    public void addShardsByAlerts() {
        holder.reload().join();
        assertEquals(Set.of(), holder.getShards());

        var alert = randomAlert();
        projectsHolder.addProject(alert.getProjectId());
        alertsDao.insert(alert, NO_OPERATION).join();

        for (int index = 0; index < 3; index++) {
            holder.reload().join();
            assertEquals(Set.of(alert.getProjectId()), holder.getShards());
            assertEquals(List.of(alert.getProjectId()), shardsDao.findAll().join());
        }
    }

    @Test
    public void addShardsByChannel() {
        holder.reload().join();
        assertEquals(Set.of(), holder.getShards());

        var channel = randomNotification();
        projectsHolder.addProject(channel.getProjectId());
        channelsDao.insert(channel, NO_OPERATION).join();

        for (int index = 0; index < 3; index++) {
            holder.reload().join();
            assertEquals(Set.of(channel.getProjectId()), holder.getShards());
            assertEquals(List.of(channel.getProjectId()), shardsDao.findAll().join());
        }
    }

    @Test
    public void addShardsByMute() {
        holder.reload().join();
        assertEquals(Set.of(), holder.getShards());

        var mute = randomMute();
        projectsHolder.addProject(mute.getProjectId());
        mutesDao.insert(mute, NO_OPERATION).join();

        for (int index = 0; index < 3; index++) {
            holder.reload().join();
            assertEquals(Set.of(mute.getProjectId()), holder.getShards());
            assertEquals(List.of(mute.getProjectId()), shardsDao.findAll().join());
        }
    }

    private void add(String shardId) {
        projectsHolder.addProject(shardId);
        holder.add(shardId).join();
    }
}
