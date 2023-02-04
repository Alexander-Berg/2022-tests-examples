package ru.yandex.partnerdata.feedloader.process.mysql;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.feedloader.data.ExecStatistics;
import ru.yandex.feedloader.data.Provider;
import ru.yandex.feedloader.data.Task;
import ru.yandex.feedloader.data.TaskResult;
import ru.yandex.feedloader.data.capa.Feed;
import ru.yandex.feedloader.depot.TaskManager;
import ru.yandex.feedloader.depot.TaskResultManager;
import ru.yandex.feedloader.depot.capa.FeedManager;
import ru.yandex.feedloader.localization.tanker.model.Language;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Specs for [[FeedManager]]
 *
 * Created by sunlight on 16.09.15.
 */
@ContextConfiguration(locations = {"classpath:test-mysql.xml"})
@Ignore
public class FeedManagerTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private FeedManager feedManager;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private TaskResultManager taskResultManager;

    @Autowired
    private JdbcTemplate mySqlJdbcTemplate;

    private Task t1;
    private Task t2;
    private Task t3;

    @Before
    public void init() {
        mySqlJdbcTemplate.update("delete from task");
        mySqlJdbcTemplate.update("delete from task_result");

        final Date lastFeedChanged = new Date();
        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(5).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url("http://auto.ru/feed/new/yandex.xml.gz").
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);

        final Task task = getTask(1438536);
        t1 = task;

        final TaskResult first = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz").
                url("http://auto.ru/feed/new/yandex.xml.gz").
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("csbo01ft").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build();
        final boolean fAdded = taskResultManager.add(first);
        assertEquals(fAdded, true);

        final Date lastFeedChanged2 = new Date();
        final boolean added2 = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(456).
                        serviceId(1).
                        stateId(0).
                        periodInSecond(3600).
                        runNow(true).
                        url("http://nmls.ru/feed/new/yandex.xml.gz").
                        enabled(true).
                        lastFeedChanged(lastFeedChanged2).
                        build()
        );
        assertEquals(added2, true);

        final Task task2 = getTask(456);
        t2 = task2;

        final TaskResult first2 = new TaskResult.Builder(-1).
                taskId(task2.getId()).
                stateId(1).
                stateInfo(null).
                fileName("vbn456edf").
                url("http://nmls.ru/feed/new/yandex.xml.gz").
                serviceInfo(null).
                md5("fgh356").
                eTag("43").
                feedSize(99999).
                processTimeInMillis(199).
                host("csbo01ft").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(45623).
                                downloadToEllipticstimeInMillis(675).
                                xmlValidationTimeInMillis(345).
                                xsdValidationTimeInMillis(67).
                                build()
                ).build();
        final boolean fAdded2 = taskResultManager.add(first2);
        assertEquals(fAdded2, true);

        mySqlJdbcTemplate.update("update task_result set creation_time = now() - interval 3 day");

        sleep(1000);

        final TaskResult second = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz1").
                url("http://auto.ru/feed/new/yandex.xml.gz").
                serviceInfo(null).
                md5("asf1").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("csbo01ft").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build();
        final boolean fSecond = taskResultManager.add(second);
        assertEquals(fSecond, true);

        final TaskResult second2 = new TaskResult.Builder(-1).
                taskId(task2.getId()).
                stateId(2).
                stateInfo(null).
                fileName("vbn456edf").
                url("http://nmls.ru/feed/new/yandex.xml.gz").
                serviceInfo(null).
                md5("fgh356r").
                eTag("43").
                feedSize(99999).
                processTimeInMillis(199).
                host("csbo01ft").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(8902).
                                downloadToEllipticstimeInMillis(4393).
                                xmlValidationTimeInMillis(584).
                                xsdValidationTimeInMillis(104).
                                build()
                ).build();
        final boolean fSecond2 = taskResultManager.add(second2);
        assertEquals(fSecond2, true);

        final Date lastFeedChanged3 = new Date();
        final boolean added3 = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(9999).
                        serviceId(4).
                        stateId(0).
                        periodInSecond(3600).
                        runNow(true).
                        url("http://job.ru/feed/new/yandex.xml.gz").
                        enabled(true).
                        lastFeedChanged(lastFeedChanged3).
                        build()
        );
        assertEquals(added3, true);

        final Task task3 = getTask(9999);
        t3 = task3;

        final TaskResult first3 = new TaskResult.Builder(-1).
                taskId(task3.getId()).
                stateId(4).
                stateInfo(null).
                fileName("ggg").
                url("http://job.ru/feed/new/yandex.xml.gz").
                serviceInfo(null).
                md5("ggg").
                eTag("43").
                feedSize(99999).
                processTimeInMillis(199).
                host("csbo01ft").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(45623).
                                downloadToEllipticstimeInMillis(675).
                                xmlValidationTimeInMillis(345).
                                xsdValidationTimeInMillis(67).
                                build()
                ).build();
        final boolean fAdded3 = taskResultManager.add(first3);
        assertEquals(fAdded3, true);

        sleep(1000);
    }

    @Test
    public void getFeedsTest() {
        List<Feed> feeds = feedManager.getFeeds(t1.getId(), Language.ENGLISH);
        assertEquals(feeds.size(), 2);

        final Feed tr11 = feeds.get(0);
        final Feed tr12 = feeds.get(1);

        assertEquals(tr11.getMd5(), "asf1");
        assertEquals(tr12.getMd5(), "asf");

        List<Feed> feeds2 = feedManager.getFeeds(t2.getId(), Language.ENGLISH);
        assertEquals(feeds2.size(), 2);

        final Feed tr21 = feeds2.get(0);
        final Feed tr22 = feeds2.get(1);

        assertEquals(tr21.getMd5(), "fgh356r");
        assertEquals(tr22.getMd5(), "fgh356");

        List<Feed> feeds3 = feedManager.getFeeds(t3.getId(), Language.ENGLISH);
        assertEquals(feeds3.size(), 1);
        assertEquals(feeds3.get(0).getMd5(), "ggg");
    }

    @Test
    public void getLastFeedsForService() {
        List<Feed> feeds = feedManager.getLastFeedsByService(1, Language.ENGLISH);
        assertEquals(feeds.size(), 1);
        assertEquals(feeds.get(0).getMd5(), "fgh356r");

        List<Feed> feeds2 = feedManager.getLastFeedsByService(5, Language.ENGLISH);
        assertEquals(feeds2.size(), 1);
        assertEquals(feeds2.get(0).getMd5(), "asf1");

        List<Feed> feeds3 = feedManager.getLastFeedsByService(4, Language.ENGLISH);
        assertEquals(feeds3.size(), 1);
        assertEquals(feeds3.get(0).getMd5(), "ggg");
    }

    @Test
    public void getLastValidFeedsForService() {
        List<Feed> feeds = feedManager.getLastValidFeedsByService(1, Language.ENGLISH);
        assertEquals(feeds.size(), 1);
        assertEquals(feeds.get(0).getMd5(), "fgh356");

        List<Feed> feeds2 = feedManager.getLastValidFeedsByService(5, Language.ENGLISH);
        assertEquals(feeds2.size(), 1);
        assertEquals(feeds2.get(0).getMd5(), "asf1");

        List<Feed> feeds3 = feedManager.getLastValidFeedsByService(4, Language.ENGLISH);
        assertEquals(feeds3.size(), 0);
    }

    @Test
    public void getLastFeedsByPartnerIds() {
        List<Feed> feeds = feedManager.getLastFeedsByPartnerIds(
                Cu.list(1438536L, 456L, 9999L),
                Language.RUSSIAN);

        assertEquals(feeds.size(), 3);

        List<String> md5s = Cf.newList();
        for (Feed feed : feeds) {
            md5s.add(feed.getMd5());
        }

        assertEquals(md5s.size(), 3);
        assertEquals(md5s, Cu.list("asf1", "fgh356r", "ggg"));
    }

    @Test
    public void getLastValidFeedsByPartnerIds() {
        List<Feed> feeds = feedManager.getLastValidFeedsByPartnerIds(
                Cu.list(1438536L, 456L, 9999L),
                Language.RUSSIAN);

        assertEquals(feeds.size(), 2);

        List<String> md5s = Cf.newList();
        for (Feed feed : feeds) {
            md5s.add(feed.getMd5());
        }

        assertEquals(md5s.size(), 2);
        assertEquals(md5s, Cu.list("asf1", "fgh356"));
    }

    @Test
    public void getFeedsByTaskResultIds() {
        List<Task> tasks = Cu.list(t1, t2, t3);

        for (Task t : tasks) {
            final List<Feed> feeds = feedManager.getFeeds(t.getId(), Language.ENGLISH);
            final List<Long> ids = getResulIds(feeds);

            final List<Feed> f1 = feedManager.getFeedsByTaskResultIds(ids, Language.ENGLISH);
            assertEquals(f1.size(), ids.size());
            assertEquals(getMd5s(feeds), getMd5s(f1));
        }
    }

    @Test
    public void getFeedsByTaskResultIdsForCAPA() {
        List<Task> tasks = Cu.list(t1, t2, t3);

        for (Task t : tasks) {
            final List<Feed> feeds = feedManager.getFeeds(t.getId(), Language.ENGLISH);
            final List<Long> ids = getResulIds(feeds);

            final List<Feed> f1 = feedManager.getFeedsByTaskResultIdsForCAPA(ids, Language.ENGLISH);
            assertEquals(f1.size(), ids.size());
            assertEquals(getMd5s(feeds), getMd5s(f1));
        }
    }

    @Test
    public void getFeeds() {
        final List<Feed> feed = feedManager.getFeeds(t1.getId(), Language.ENGLISH);
        assertEquals(getMd5List(feed), Cf.list("asf1", "asf"));
        assertEquals(feed.size(), 2);

        final List<Feed> feed2 = feedManager.getFeeds(t2.getId(), Language.ENGLISH);
        assertEquals(getMd5List(feed2), Cf.list("fgh356r", "fgh356"));
        assertEquals(feed2.size(), 2);

        final List<Feed> feed3 = feedManager.getFeeds(t3.getId(), Language.ENGLISH);
        assertEquals(getMd5List(feed3), Cf.list("ggg"));
        assertEquals(feed3.size(), 1);
    }

    @Test
    public void getFeedInPeriod() {
        final Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        final List<Feed> feeds = feedManager.getLastFeeds(
                Cf.list(t1.getId(), t2.getId(), t3.getId()),
                cal.getTime(),
                new Date(),
                Language.ENGLISH);
        assertEquals(feeds.size(), 3);
    }

    private Task getTask(long partnerId) {
        final List<Task> tasks = taskManager.getTasksForPartners(Cu.list(partnerId));
        final Task t = tasks.get(0);
        return taskManager.get(t.getId());
    }

    private Set<String> getMd5s(List<Feed> feeds) {
        Set<String> md5s = Cf.newHashSet();
        for (Feed feed : feeds) {
            md5s.add(feed.getMd5());
        }
        return md5s;
    }

    private List<String> getMd5List(List<Feed> feeds) {
        List<String> md5s = Cf.list();
        for (Feed feed : feeds) {
            md5s.add(feed.getMd5());
        }
        return md5s;
    }

    public List<Long> getResulIds(List<Feed> feeds) {
        List<Long> ids = Cf.newList();
        for (Feed feed : feeds) {
            ids.add(feed.getId());
        }
        return ids;
    }

    private void sleep(final long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            logger.error("error", e);
        }
    }
}
