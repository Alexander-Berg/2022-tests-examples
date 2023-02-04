package ru.yandex.solomon.alert.dao;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.solomon.core.db.dao.memory.InMemoryProjectsDao;
import ru.yandex.solomon.core.db.model.Project;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class ProjectHolderImplTest {

    private ManualClock clock;
    private ManualScheduledExecutorService executorService;
    private InMemoryProjectsDao projectsDao;
    private ProjectHolderImpl holder;

    @Rule
    public Timeout timeoutRule = Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, TimeUnit.SECONDS)
        .build();

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
        projectsDao = new InMemoryProjectsDao();
        holder = new ProjectHolderImpl(projectsDao, executorService, executorService);
    }

    @After
    public void tearDown() throws Exception {
        holder.close();
        executorService.shutdownNow();
    }

    @Test
    public void periodicReload() throws InterruptedException {
        {
            addProject("alice");
            while (!holder.getProjects().contains("alice")) {
                awaitSync(actSync());
            }
            assertEquals(Set.of("alice"), holder.getProjects());
        }

        {
            addProject("bob");
            while (!holder.getProjects().contains("bob")) {
                awaitSync(actSync());
            }
            assertEquals(Set.of("alice", "bob"), holder.getProjects());
        }

        {
            projectsDao.deleteOne("alice", true).join();
            while (holder.getProjects().contains("alice")) {
                awaitSync(actSync());
            }
            assertEquals(Set.of("bob"), holder.getProjects());
        }
    }

    @Test
    public void hasProject() {
        holder.awaitNextReload().join();
        assertFalse(holder.hasProject("alice").join());
        assertEquals(Set.of(), holder.getProjects());
        addProject("alice");
        assertTrue(holder.hasProject("alice").join());
        assertEquals(Set.of("alice"), holder.getProjects());
    }

    private CountDownLatch actSync() {
        var sync = new CountDownLatch(1);
        holder.awaitNextReload().whenComplete((ignore, e) -> {
            sync.countDown();
        });
        return sync;
    }

    private void awaitSync(CountDownLatch sync) throws InterruptedException {
        do {
            clock.passedTime(5, TimeUnit.MINUTES);
        } while (!sync.await(5, TimeUnit.MILLISECONDS));
    }

    private void addProject(String name) {
        projectsDao.insert(Project.newBuilder()
                .setId(name)
                .setName(name)
                .setOwner("my")
                .build())
                .join();
    }
}
