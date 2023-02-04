package ru.yandex.webmaster3.worker;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import ru.yandex.webmaster3.core.worker.WorkerApi;
import ru.yandex.webmaster3.core.worker.task.PeriodicTaskState;
import ru.yandex.webmaster3.core.worker.task.PeriodicTaskType;
import ru.yandex.webmaster3.storage.abt.AbtService;
import ru.yandex.webmaster3.storage.abt.model.Experiment;
import ru.yandex.webmaster3.storage.logging.TasksLoggingBatchWriter;
import ru.yandex.webmaster3.storage.logging.TasksLoggingCHDao;
import ru.yandex.webmaster3.storage.logging.TasksLoggingService;
import ru.yandex.webmaster3.storage.util.yt.YtCluster;
import ru.yandex.webmaster3.storage.util.yt.YtPath;
import ru.yandex.webmaster3.storage.util.yt.YtService;
import ru.yandex.webmaster3.storage.util.yt.lock.CypressProvider;
import ru.yandex.webmaster3.storage.util.yt.lock.EphemeralNodeStateChecker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ishalaru
 * 24.11.2020
 **/
public class SimpleZkSchedulerTest {

    static {
        ch.qos.logback.classic.Logger parentLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
        parentLogger.setLevel(Level.ERROR);
        ch.qos.logback.classic.Logger log =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ru.yandex.webmaster3.worker.SimpleZkScheduler");
        parentLogger.setLevel(Level.INFO);
    }

    public static String token = "";

    public CypressProvider createYtService() {
        YtService ytService = new YtService();
        YtCluster ytCluster = new YtCluster("locke", URI.create("http://locke.yt.yandex.net"), token, Files.createTempDir());
        ytService.setClusters(List.of(ytCluster));
        ytService.setDefaultCacheFolder(Files.createTempDir());
        ytService.init();
        ytService.start();
        EphemeralNodeStateChecker ephemeralNodeStateChecker = new EphemeralNodeStateChecker(ytService);
        ephemeralNodeStateChecker.setExpirationWaitTimestamp(30000);
        ephemeralNodeStateChecker.init();
        CypressProvider lock = new CypressProvider(ytService, ephemeralNodeStateChecker);
        lock.init();
        lock.setRoot(YtPath.create("locke", "//home/webmaster/test/"));
        return lock;
    }

    @SneakyThrows
    @Test
    @Ignore
    public void test() {
        CypressProvider cypressProvider = createYtService();
        AbtService abtService = mock(AbtService.class);
        when(abtService.isInExperiment(Mockito.any(), Mockito.any(Experiment.class))).thenReturn(true);
        WorkerApi workerApi = mock(WorkerApi.class);
        SimpleZkScheduler simpleZkScheduler = new SimpleZkScheduler(cypressProvider, workerApi);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        final PeriodicTaskTest periodicTaskTest = new PeriodicTaskTest();
        final TasksLoggingService tasksLoggingService = new TasksLoggingService();
        tasksLoggingService.setHostName("test1");
        tasksLoggingService.setTasksLoggingBatchWriter(mock(TasksLoggingBatchWriter.class));
        tasksLoggingService.setMdbPeriodicTasksLoggingCHDao(mock(TasksLoggingCHDao.class));
        periodicTaskTest.setPeriodicTasksLoggingService(tasksLoggingService);
        periodicTaskTest.number = 1;
        when(applicationContext.getBeansOfType(PeriodicTask.class)).thenReturn(Map.of("Test", periodicTaskTest));
        simpleZkScheduler.setApplicationContext(applicationContext);
        simpleZkScheduler.setHostname("localhost1");
        simpleZkScheduler.init();
        simpleZkScheduler.start();


        ApplicationContext applicationContext2 = mock(ApplicationContext.class);
        final PeriodicTaskTest periodicTaskTest2 = new PeriodicTaskTest();
        final TasksLoggingService tasksLoggingService2 = new TasksLoggingService();
        tasksLoggingService2.setHostName("test2");
        tasksLoggingService2.setTasksLoggingBatchWriter(mock(TasksLoggingBatchWriter.class));
        tasksLoggingService2.setMdbPeriodicTasksLoggingCHDao(mock(TasksLoggingCHDao.class));
        periodicTaskTest2.setPeriodicTasksLoggingService(tasksLoggingService2);
        periodicTaskTest2.number = 2;
        when(applicationContext2.getBeansOfType(PeriodicTask.class)).thenReturn(Map.of("Test", periodicTaskTest2));

        SimpleZkScheduler simpleZkScheduler_2 = new SimpleZkScheduler(createYtService(), workerApi);
        simpleZkScheduler_2.setApplicationContext(applicationContext2);
        simpleZkScheduler_2.setHostname("localhost2");
        simpleZkScheduler_2.init();
        simpleZkScheduler_2.start();


        Thread.sleep(6100000);
    }

    private final class PeriodicTaskTest extends PeriodicTask<State> {
        int number;


        @Override
        public Result run(UUID runId) throws Exception {
            System.out.println(DateTime.now() + "Test run start " + number);
            Thread.sleep(180000);
            System.out.println(DateTime.now() + "Test run complete" + number);
            return Result.SUCCESS;
        }

        @Override
        public PeriodicTaskType getType() {
            return PeriodicTaskType.TEST_TASK;
        }

        @Override
        public TaskSchedule getSchedule() {
            return TaskSchedule.startByCron("0 * * * * *");
        }
    }

    private final class State implements PeriodicTaskState {

    }
}
