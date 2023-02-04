package ru.yandex.partnerdata.feedloader.process.mysql;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.feedloader.data.ExecStatistics;
import ru.yandex.feedloader.data.Provider;
import ru.yandex.feedloader.data.Task;
import ru.yandex.feedloader.data.TaskResult;
import ru.yandex.feedloader.depot.TaskManager;
import ru.yandex.feedloader.depot.TaskResultManager;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Specs for [[TaskResultManager]]
 *
 * Created by sunlight on 15.09.15.
 */
@ContextConfiguration(locations = {"classpath:test-mysql.xml"})
@Ignore
public class TaskResultManagerTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private JdbcTemplate mySqlJdbcTemplate;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private TaskResultManager taskResultManager;

    @Before
    public void clean() {
        mySqlJdbcTemplate.update("DELETE FROM task");
        mySqlJdbcTemplate.update("DELETE FROM task_result");
    }

    private final static String FEED_URL = "http://yandex.ru/favicon.ico";

    @Test
    public void create() {
        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);

        final Task task = getTask(1438536);

        final TaskResult taskResult = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                creationTime(new Date()).
                fileName("abc123xyz").
                url(FEED_URL).
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
        final boolean added2 = taskResultManager.add(taskResult);
        assertEquals(added2, true);

        final TaskResult result = taskResultManager.getLast(task.getId());
        assertNotNull(result);
        assertEquals(result.getTaskId(), task.getId());
        assertEquals(result.getStateId(), 1);
        assertEquals(result.getStateInfo(), null);
        assertEquals(result.getFileName(), "abc123xyz");
        assertEquals(result.getUrl(), FEED_URL);
        assertEquals(result.getServiceInfo(), null);
        assertEquals(result.getMd5(), "asf");
        assertEquals(result.getETag(), "dfdas");
        assertEquals(result.getFeedSize(), 100);
        assertEquals(result.getProcessTimeInMillis(), 199);
        assertEquals(result.getHost(), "csbo01ft");

        final ExecStatistics stat = taskResult.getExecStatistics();
        assertEquals(stat.getDownloadFromUrlTimeInMillis(), 1456);
        assertEquals(stat.getDownloadToEllipticstimeInMillis(), 45);
        assertEquals(stat.getXmlValidationTimeInMillis(), 67);
        assertEquals(stat.getXsdValidationTimeInMillis(), 34);

        final TaskResult tt = taskResultManager.get(result.getId());
        assertEquals(tt, result);
    }

    @Test
    public void setServiceResponse() {
        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);

        final Task task = getTask(1438536);

        final TaskResult taskResult = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                creationTime(new Date()).
                fileName("abc123xyz").
                url(FEED_URL).
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
        final boolean added2 = taskResultManager.add(taskResult);
        assertEquals(added2, true);

        final TaskResult result = taskResultManager.getLast(task.getId());
        assertNotNull(result);

        final boolean set = taskResultManager.setServiceResponse(result.getId(), "Everything is cool", 1);
        assertEquals(set, true);

        final TaskResult withResponse = taskResultManager.getLast(task.getId());
        assertNotNull(withResponse);

        assertEquals(withResponse.getServiceInfo(), "Everything is cool");
        assertEquals(withResponse.getServiceResultStateId(), 1);
    }

    @Test
    public void oldTasksResults() {
        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);
        final Task task = getTask(1438536);

        final TaskResult taskResult = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                creationTime(new Date()).
                fileName("abc123xyz").
                url(FEED_URL).
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
        final boolean added2 = taskResultManager.add(taskResult);
        assertEquals(added2, true);

        final List<TaskResult> olds = taskResultManager.getOldTaskResults();
        assertEquals(olds.size(), 0);

        mySqlJdbcTemplate.update("update task_result set creation_time = now() - interval 1 day");
        final List<TaskResult> olds2 = taskResultManager.getOldTaskResults();
        assertEquals(olds2.size(), 0);

        mySqlJdbcTemplate.update("update task_result set creation_time = now() - interval 2 day");
        final List<TaskResult> olds3 = taskResultManager.getOldTaskResults();
        assertEquals(olds3.size(), 0);

        mySqlJdbcTemplate.update("update task_result set creation_time = now() - interval 15 day");
        final List<TaskResult> olds4 = taskResultManager.getOldTaskResults();
        assertEquals(olds4.size(), 1);
    }

    @Test
    public void getLastSuccessful() {
        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);
        final Task task = getTask(1438536);

        final TaskResult first = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz").
                url(FEED_URL).
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

        sleep(1000);

        final TaskResult second = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(2).
                stateInfo(null).
                url(FEED_URL).
                eTag("asfsdaf").
                processTimeInMillis(256).
                host("csbo01f").
                processStartDate(new Date())
                .build();
        final boolean sAdded = taskResultManager.add(second);
        assertEquals(sAdded, true);

        sleep(1000);

        final TaskResult third = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("123xyz").
                url(FEED_URL).
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
        final boolean sThird = taskResultManager.add(third);
        assertEquals(sThird, true);

        final TaskResult last = taskResultManager.getLastSuccessful(task.getId());
        assertNotNull(last);
        assertEquals(third.getMd5(), last.getMd5());


        final TaskResult tr = taskResultManager.getSuccessWithPosInQueue(task.getId(), 1);
        assertNotNull(tr);
        assertEquals(tr.getMd5(), third.getMd5());

        final TaskResult f = taskResultManager.getSuccessWithPosInQueue(task.getId(), 2);
        assertNotNull(f);
        assertEquals(f.getMd5(), first.getMd5());
    }


    @Test
    public void deleteByFileName() {
        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);
        final Task task = getTask(1438536);

        final TaskResult first = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz").
                url(FEED_URL).
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

        sleep(1000);

        final TaskResult second = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(2).
                stateInfo(null).
                url(FEED_URL).
                eTag("asfsdaf").
                processTimeInMillis(256).
                host("csbo01f").
                processStartDate(new Date())
                .build();
        final boolean sAdded = taskResultManager.add(second);
        assertEquals(sAdded, true);

        sleep(1000);

        final TaskResult third = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("123xyz").
                url(FEED_URL).
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
        final boolean tAdded = taskResultManager.add(third);
        assertEquals(tAdded, true);

        sleep(1000);

        final TaskResult forth = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("123xyz").
                url(FEED_URL).
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
        final boolean tforth = taskResultManager.add(forth);
        assertTrue(tforth);

        final int deleted = taskResultManager.deleteByFileName(Cu.list("abc123xyz"));
        assertEquals(deleted, 1);

        final int deleted2 = taskResultManager.deleteByFileName(Cu.list("123xyz"));
        assertEquals(deleted2, 2);
    }

    @Test
    public void getUnmodifiedForLast3Days() {
        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);
        final Task task = getTask(1438536);

        final TaskResult first = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz").
                url(FEED_URL).
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
        assertTrue(fAdded);

        mySqlJdbcTemplate.update("update task_result set creation_time = now() - interval 3 day");
        sleep(1000);

        final TaskResult second = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz").
                url(FEED_URL).
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
        final boolean fSecond = taskResultManager.add(second);
        assertTrue(fSecond);

        List<TaskResult> oldTaskResult = taskResultManager.getOldTaskResults();
        assertEquals(oldTaskResult.size(), 1);

        final List<Long> ids = taskResultManager.getIdsWithObsoleteFeedFiles(oldTaskResult);
        assertEquals(ids.size(), 0);
    }

    @Test
    public void getUnmodifiedForLast3Days2() {
        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);
        final Task task = getTask(1438536);

        final TaskResult first = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz").
                url(FEED_URL).
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

        mySqlJdbcTemplate.update("update task_result set creation_time = now() - interval 3 day");
        sleep(1000);

        final TaskResult second = new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName("abc123xyz1").
                url(FEED_URL).
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
        final boolean fSecond = taskResultManager.add(second);
        assertEquals(fSecond, true);

        List<TaskResult> oldTaskResult = taskResultManager.getOldTaskResults();
        assertEquals(oldTaskResult.size(), 1);


        final List<Long> ids = taskResultManager.getIdsWithObsoleteFeedFiles(oldTaskResult);
        assertEquals(ids.size(), 1);

        final int deleted = taskResultManager.delete(ids);
        assertEquals(deleted, 1);

        List<TaskResult> oldTaskResult2 = taskResultManager.getOldTaskResults();
        assertEquals(oldTaskResult2.size(), 0);
    }

    @Test
    public void testGetMoreRecentIdsWithSameFile() {
        clean();

        long partnerId = 10001;
        long partnerId2 = 10002;
        String fileName = "feed.bin";

        assertTrue(taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(partnerId).
                        serviceId(1).
                        stateId(0).
                        periodInSecond(10).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(new Date()).
                        build()
        ));
        final Task task = getTask(partnerId);

        assertTrue(taskResultManager.add(new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName(fileName).
                url(FEED_URL).
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("host0").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build()
        ));

        final TaskResult taskResult = taskResultManager.getLast(task.getId());
        assertEquals(taskResultManager.getMoreRecentIdsWithSameFile(taskResult.getId()).size(), 0);

        sleep(1000);

        assertTrue(taskResultManager.add(new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName(fileName).
                url(FEED_URL).
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("host0").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build()
        ));
        assertTrue(taskResultManager.add(new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName(fileName + "_").
                url(FEED_URL).
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("host0").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build()
        ));
        assertEquals(taskResultManager.getMoreRecentIdsWithSameFile(taskResult.getId()).size(), 1);

        assertTrue(taskResultManager.add(new TaskResult.Builder(-1).
                taskId(task.getId()).
                stateId(1).
                stateInfo(null).
                fileName(fileName).
                url(FEED_URL).
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("host0").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build()
        ));
        assertEquals(taskResultManager.getMoreRecentIdsWithSameFile(taskResult.getId()).size(), 2);

        assertTrue(taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(partnerId2).
                        serviceId(1).
                        stateId(0).
                        periodInSecond(10).
                        runNow(true).
                        url(FEED_URL).
                        enabled(true).
                        lastFeedChanged(new Date()).
                        build()
        ));
        final Task task2 = getTask(partnerId2);

        assertTrue(taskResultManager.add(new TaskResult.Builder(-1).
                taskId(task2.getId()).
                stateId(1).
                stateInfo(null).
                url(FEED_URL).
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("host0").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build()
        ));

        final TaskResult taskResult2 = taskResultManager.getLast(task2.getId());
        assertEquals(taskResultManager.getMoreRecentIdsWithSameFile(taskResult2.getId()).size(), 0);

        sleep(1000);
        
        assertTrue(taskResultManager.add(new TaskResult.Builder(-1).
                taskId(task2.getId()).
                stateId(1).
                stateInfo(null).
                fileName(fileName).
                url(FEED_URL).
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("host0").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build()
        ));
        assertTrue(taskResultManager.add(new TaskResult.Builder(-1).
                taskId(task2.getId()).
                stateId(1).
                stateInfo(null).
                url(FEED_URL).
                serviceInfo(null).
                md5("asf").
                eTag("dfdas").
                feedSize(100).
                processTimeInMillis(199).
                host("host0").
                processStartDate(new Date()).
                execStatistics(
                        new ExecStatistics.Builder().
                                downloadFromUrlTimeInMillis(1456).
                                downloadToEllipticstimeInMillis(45).
                                xmlValidationTimeInMillis(67).
                                xsdValidationTimeInMillis(34).
                                build()
                ).build()
        ));

        assertEquals(taskResultManager.getMoreRecentIdsWithSameFile(taskResult2.getId()).size(), 0);
    }

    private void sleep(final long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            logger.error("error", e);
        }
    }

    private Task getTask(long partnerId) {
        final List<Task> tasks = taskManager.getTasksForPartners(Cu.list(partnerId));
        final Task t = tasks.get(0);
        return taskManager.get(t.getId());
    }
}
