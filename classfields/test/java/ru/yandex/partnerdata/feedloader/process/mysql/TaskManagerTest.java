package ru.yandex.partnerdata.feedloader.process.mysql;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.feedloader.data.Provider;
import ru.yandex.feedloader.data.Task;
import ru.yandex.feedloader.depot.TaskManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Specs for [[TaskManager]]
 *
 * Created by sunlight on 11.09.15.
 */
@ContextConfiguration(locations = {"classpath:test-mysql.xml"})
@Ignore
public class TaskManagerTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private JdbcTemplate mySqlJdbcTemplate;

    @Autowired
    private TaskManager taskManager;

    private final static String FEED_URL = "http://yandex.ru/favicon.ico";

    @Before
    public void clean() {
        mySqlJdbcTemplate.update("DELETE FROM task");
    }

    @Test
    public void create() {
        //final DateTime lastFetchTime = new DateTime(2015, 11, 10, 12, 15, 10);
        final Date lastFeedChanged = new DateTime(2015, 11, 9, 10, 59, 10).toDate();
        final Task task = new Task.Builder(-1).
                provider(Provider.INTERNAL).
                partnerId(1438536).
                serviceId(32767).
                stateId(0).
                periodInSecond(1800).
                runNow(true).
                url(FEED_URL).
                login("yandex").
                password("pass").
                enabled(true).
                lastFeedChanged(lastFeedChanged).
                build();
        boolean added = taskManager.add(task);
        assertEquals(added, true);

        final List<Task> tasks = taskManager.getTasksForPartners(Cu.list(1438536L));
        assertEquals(tasks.size(), 1);

        Task saved = tasks.get(0);
        assertEquals(saved.getPartnerId(), 1438536);
        assertEquals(saved.getServiceId(), 32767);
        assertEquals(saved.getStateId(), 0);
        assertEquals(saved.getPeriodInSecond(), 1800);
        assertEquals(saved.getUrl(), FEED_URL);
        assertEquals(saved.getLogin(), "yandex");
        assertEquals(saved.getPassword(), "pass");
        assertEquals(saved.isEnabled(), true);

        final Task saved2 = taskManager.get(saved.getId());
        assertEquals(saved2.getPartnerId(), 1438536);
        assertEquals(saved2.getServiceId(), 32767);
        assertEquals(saved2.getStateId(), 0);
        assertEquals(saved2.getPeriodInSecond(), 1800);
        assertEquals(saved2.getUrl(), FEED_URL);
        assertEquals(saved2.getLogin(), "yandex");
        assertEquals(saved2.getPassword(), "pass");
        assertEquals(saved2.isEnabled(), true);
    }

    @Test
    public void lock() {
        final Date lastFeedChanged = new DateTime(2015, 11, 8, 9, 40, 10).toDate();
        final Task task = new Task.Builder(-1).
                provider(Provider.INTERNAL).
                partnerId(143).
                serviceId(1).
                stateId(0).
                periodInSecond(3600).
                runNow(true).
                url(FEED_URL).
                enabled(true).
                lastFeedChanged(lastFeedChanged).
                build();

        final boolean locked = taskManager.lock(task);
        assertEquals(locked, false);

        final boolean added = taskManager.add(task);
        assertEquals(added, true);

        final Task saved = getTask(143L);
        assertEquals(saved.getStateId(), 0L);

        final boolean locked2 = taskManager.lock(saved);
        assertEquals(locked2, true);

        final Task saved2 = taskManager.get(saved.getId());
        assertEquals(saved2.getStateId(), 1L);
    }

    @Test
    public void unlock() {
        final Date lastFeedChanged = new DateTime(2015, 11, 8, 9, 40, 10).toDate();
        final Task task = new Task.Builder(-1).
                provider(Provider.INTERNAL).
                partnerId(143).
                serviceId(1).
                stateId(0).
                periodInSecond(3600).
                runNow(true).
                url(FEED_URL).
                enabled(true).
                lastFeedChanged(lastFeedChanged).
                build();

        final boolean added = taskManager.add(task);
        assertEquals(added, true);

        final Task saved = getTask(143L);
        assertEquals(saved.getStateId(), 0L);

        final boolean isLocked = taskManager.lock(saved);
        assertEquals(isLocked, true);

        final Task locked = taskManager.get(saved.getId());
        assertEquals(locked.getStateId(), 1L);

        final boolean isUnlocked = taskManager.unlock(saved);
        assertEquals(isUnlocked, true);

        final Task unlocked = taskManager.get(saved.getId());
        assertEquals(unlocked.getStateId(), 0L);
    }

    @Test
    public void lockAll() {
        final Date lastFeedChanged1 = new DateTime(2015, 11, 8, 9, 40, 10).toDate();
        final Task task1 = new Task.Builder(-1).
                provider(Provider.INTERNAL).
                partnerId(143).
                serviceId(1).
                stateId(0).
                periodInSecond(3600).
                runNow(true).
                url(FEED_URL).
                enabled(true).
                lastFeedChanged(lastFeedChanged1).
                build();

        final Date lastFeedChanged2 = new DateTime(2015, 10, 7, 8, 46, 10).toDate();
        final Task task2 = new Task.Builder(-1).
                provider(Provider.INTERNAL).
                partnerId(99).
                serviceId(5).
                stateId(0).
                periodInSecond(1800).
                runNow(true).
                url(FEED_URL).
                enabled(true).
                lastFeedChanged(lastFeedChanged2).
                build();

        final boolean a1 = taskManager.add(task1);
        assertEquals(a1, true);

        final boolean a2 = taskManager.add(task2);
        assertEquals(a2, true);

        final List<Long> partnerIds = new ArrayList<>();
        partnerIds.add(143L);
        partnerIds.add(99L);

        final List<Task> saved = taskManager.getTasksForPartners(partnerIds);
        assertEquals(saved.size(), 2);

        final int locked = taskManager.lock(saved);
        assertEquals(locked, 2);

        final List<Task> saved2 = taskManager.getTasksForPartners(partnerIds);
        final Task t1 = taskManager.get(saved2.get(0).getId());
        assertEquals(t1.getStateId(), 1);
        final Task t2 = taskManager.get(saved2.get(1).getId());
        assertEquals(t2.getStateId(), 1);


        final List<Long> unlocked = taskManager.unlockAll();
        assertEquals(unlocked.size(), 0);
    }

    @Test
    public void testLastOkFetchTime() {
        final Date lastFeedChanged = new DateTime(2015, 11, 8, 9, 40, 10).toDate();
        final Task task = new Task.Builder(-1).
                provider(Provider.INTERNAL).
                partnerId(143).
                serviceId(1).
                stateId(0).
                periodInSecond(3600).
                runNow(true).
                url(FEED_URL).
                enabled(true).
                lastFeedChanged(lastFeedChanged).
                build();

        final boolean added = taskManager.add(task);
        assertEquals(added, true);

        final Task saved = getTask(143L);

        final DateTime lastOk = new DateTime(2014, 10, 5, 8, 10, 10);
        final boolean setted = taskManager.setLastFetchTime(saved, lastOk);
        assertEquals(setted, true);

        final Task saved2 = getTask(143L);
        final Date lastFetch = saved2.getLastFetchTime();
    }

    @Test
    public void testLastFeedChange() {
        final Date lastFeedChanged = new DateTime(2015, 11, 8, 9, 40, 10).toDate();
        final Task task = new Task.Builder(-1).
                provider(Provider.INTERNAL).
                partnerId(143).
                serviceId(1).
                stateId(0).
                periodInSecond(3600).
                runNow(true).
                url(FEED_URL).
                enabled(true).
                lastFeedChanged(lastFeedChanged).
                build();

        final boolean added = taskManager.add(task);
        assertEquals(added, true);

        final Task saved = getTask(143L);

        final DateTime lastOk = new DateTime(2014, 10, 5, 8, 10, 10);
        final boolean setted = taskManager.setLastFeedChangeTime(saved, lastOk);
        assertEquals(setted, true);

        final Task saved2 = getTask(143L);
        final Date lastFetch = saved2.getLastFetchTime();
    }

    private Task getTask(long partnerId) {
        final List<Task> tasks = taskManager.getTasksForPartners(Cu.list(partnerId));
        final Task t = tasks.get(0);
        return taskManager.get(t.getId());
    }
}
