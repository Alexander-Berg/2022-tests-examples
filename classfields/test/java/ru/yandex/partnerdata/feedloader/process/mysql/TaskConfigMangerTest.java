package ru.yandex.partnerdata.feedloader.process.mysql;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.common.util.collections.Triple;
import ru.yandex.feedloader.data.Provider;
import ru.yandex.feedloader.data.Task;
import ru.yandex.feedloader.data.TaskConfig;
import ru.yandex.feedloader.depot.TaskConfigManager;
import ru.yandex.feedloader.depot.TaskManager;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Specs for [[TaskConfigManager]]
 *
 * Created by sunlight on 21.09.15.
 */
@ContextConfiguration(locations = {"classpath:test-mysql.xml"})
@Ignore
public class TaskConfigMangerTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private JdbcTemplate mySqlJdbcTemplate;

    @Autowired
    private TaskConfigManager taskConfigManger;

    @Autowired
    private TaskManager taskManager;

    @Before
    public void clean() {
        mySqlJdbcTemplate.update("DELETE FROM task");
        mySqlJdbcTemplate.update("DELETE FROM task_config");

        final Date lastFeedChanged = new Date();

        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(1438536).
                        serviceId(32767).
                        stateId(0).
                        periodInSecond(1800).
                        runNow(true).
                        url("http://auto.ru/feed/new/yandex.xml.gz").
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);
    }

    @Test
    public void createConfig() {
        final Task task = getTask(1438536);
        assertNotNull(task);
        taskConfigManger.addTaskConfig(task.getId(), "fastcheck", "true");

        final TaskConfig taskConfig = taskConfigManger.getConfig(task.getId());
        assertEquals(taskConfig.isFastCheck(), true);
        assertEquals(taskConfig.getForceUpdatePeriodInMillis(), 0);
        assertEquals(taskConfig.getXsdSchema(), null);
        assertEquals(taskConfig.isTolerantXsdCheck(), false);
    }

    @Test
    public void createConfig2() {
        final Task task = getTask(1438536);
        assertNotNull(task);
        taskConfigManger.addTaskConfig(task.getId(), "fastcheck", "true");
        taskConfigManger.addTaskConfig(task.getId(), "force_update_in_minutes", "10");
        taskConfigManger.addTaskConfig(task.getId(), "tolerant_xsd_check", "true");
        taskConfigManger.addTaskConfig(task.getId(), "xsd_schema", "auto.xsd");

        final TaskConfig taskConfig = taskConfigManger.getConfig(task.getId());
        assertEquals(taskConfig.isFastCheck(), true);
        assertEquals(taskConfig.getForceUpdatePeriodInMillis(), 600000);
        assertEquals(taskConfig.getXsdSchema(), "auto.xsd");
        assertEquals(taskConfig.isTolerantXsdCheck(), true);
    }

    @Test
    public void getTaskTest() {
        final Date lastFeedChanged = new Date();
        final boolean added = taskManager.add(
                new Task.Builder(-1).
                        provider(Provider.INTERNAL).
                        partnerId(13455).
                        serviceId(1).
                        stateId(0).
                        periodInSecond(3600).
                        runNow(true).
                        url("http://job.ru/feed/new/yandex.xml.gz").
                        enabled(true).
                        lastFeedChanged(lastFeedChanged).
                        build()
        );
        assertEquals(added, true);

        final Task task2 = getTask(13455);
        assertNotNull(task2);

        final Task task = getTask(1438536);
        assertNotNull(task);

        taskConfigManger.addTaskConfig(task2.getId(), "fastcheck", "false");
        taskConfigManger.addTaskConfig(task2.getId(), "tolerant_xsd_check", "true");

        taskConfigManger.addTaskConfig(task.getId(), "tolerant_xsd_check", "false");
        taskConfigManger.addTaskConfig(task.getId(), "xsd_schema", "realty.xsd");
        taskConfigManger.addTaskConfig(task.getId(), "fastcheck", "true");
        taskConfigManger.addTaskConfig(task.getId(), "force_update_in_minutes", "60");

        final Map<Task, TaskConfig> configs = taskConfigManger.getConfigs(Cu.list(task, task2));
        assertEquals(configs.size(), 2);

        final TaskConfig config = configs.get(task);
        assertEquals(config.getForceUpdatePeriodInMillis(), 3600000L);
        assertEquals(config.getXsdSchema(), "realty.xsd");
        assertEquals(config.isFastCheck(), true);
        assertEquals(config.isTolerantXsdCheck(), false);


        final TaskConfig config2 = configs.get(task2);
        assertEquals(config2.getForceUpdatePeriodInMillis(), 0L);
        assertEquals(config2.getXsdSchema(), null);
        assertEquals(config2.isFastCheck(), false);
        assertEquals(config2.isTolerantXsdCheck(), true);
    }

    @Test
    public void updateTaskConfig() {

        final Task task = getTask(1438536);
        assertNotNull(task);

        taskConfigManger.addTaskConfig(task.getId(), "tolerant_xsd_check", "false");
        taskConfigManger.addTaskConfig(task.getId(), "fastcheck", "true");
        taskConfigManger.addTaskConfig(task.getId(), "force_update_in_minutes", "60");

        final TaskConfig config = taskConfigManger.getConfig(task.getId());
        assertEquals(config.getForceUpdatePeriodInMillis(), 3600000L);
        assertEquals(config.getXsdSchema(), null);
        assertEquals(config.isFastCheck(), true);
        assertEquals(config.isTolerantXsdCheck(), false);

        taskConfigManger.addConfigsOrUpdate(
                Cu.list(
                        Triple.<Object,Object,Object>of(task.getId(), "tolerant_xsd_check", "true"),
                        Triple.<Object,Object,Object>of(task.getId(), "xsd_schema", "auto.xsd"),
                        Triple.<Object,Object,Object>of(task.getId(), "force_update_in_minutes", "30")
                )
        );

        final TaskConfig config2 = taskConfigManger.getConfig(task.getId());
        assertEquals(config2.getForceUpdatePeriodInMillis(), 1800000L);
        assertEquals(config2.getXsdSchema(), "auto.xsd");
        assertEquals(config2.isFastCheck(), true);
        assertEquals(config2.isTolerantXsdCheck(), true);
    }

    private Task getTask(long partnerId) {
        final List<Task> tasks = taskManager.getTasksForPartners(Cu.list(partnerId));
        final Task t = tasks.get(0);
        return taskManager.get(t.getId());
    }
}
