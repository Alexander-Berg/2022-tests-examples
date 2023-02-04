package ru.yandex.qe.dispenser.ws.job;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.internal.util.MockUtil;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.yandex.qe.dispenser.domain.dao.DiJdbcTemplate;
import ru.yandex.qe.dispenser.domain.hierarchy.HierarchySupplier;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

public abstract class AbstractJobTest extends AcceptanceTestBase {
    @Autowired(required = false)
    private DiJdbcTemplate jdbcTemplate;

    @Autowired
    protected HierarchySupplier hierarchy;

    protected ClassPathXmlApplicationContext applicationContext;
    protected MockBeanPostProcessor mockBeanPostProcessor;
    private final String config;
    private final String[] profiles;

    protected AbstractJobTest(final String[] profiles) {
        this.config = "classpath:/spring/quartz-jobs-test.xml";
        this.profiles = profiles;
        mockBeanPostProcessor = new MockBeanPostProcessor();
    }

    @BeforeAll
    public void beforeClass() throws SchedulerException {
        Assumptions.assumeFalse(jdbcTemplate == null, "No jdbcTemplate found");

        applicationContext = new ClassPathXmlApplicationContext(new String[]{config}, false, super.applicationContext);

        applicationContext.getEnvironment().setActiveProfiles(profiles);

        applicationContext.addBeanFactoryPostProcessor((bf) -> {
            bf.addBeanPostProcessor(mockBeanPostProcessor);
        });

        applicationContext.refresh();
        hierarchy.update();
    }


    @BeforeEach
    @Override
    public void setUp() {
    }

    protected static class MockBeanPostProcessor implements BeanPostProcessor {

        private final Map<String, Object> mockByName = new HashMap<>();

        private MockBeanPostProcessor() {
        }

        @Override
        public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
            if (MockUtil.isMock(bean)) {
                mockByName.put(beanName, bean);
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
            return bean;
        }

        public <T> T getMockByName(final String beanName) {
            return (T) mockByName.get(beanName);
        }
    }

    protected static class WaitListener implements JobListener {

        private final Semaphore jobExecutedStateLock = new Semaphore(1);

        private final String name;

        private WaitListener(final String name) throws InterruptedException {
            this.name = name;
            jobExecutedStateLock.acquire();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void jobToBeExecuted(final JobExecutionContext context) {

        }

        @Override
        public void jobExecutionVetoed(final JobExecutionContext context) {

        }

        @Override
        public void jobWasExecuted(final JobExecutionContext context, final JobExecutionException jobException) {
            jobExecutedStateLock.release();
        }

        public void waitJobWasExecuted() throws InterruptedException {
            jobExecutedStateLock.acquire();
        }
    }

    protected static void triggerAndWait(final Scheduler clusteredScheduler, final Trigger trigger) throws InterruptedException, SchedulerException, TimeoutException, ExecutionException {
        final JobKey jobKey = trigger.getJobKey();

        final JobDetail jobDetail = clusteredScheduler.getJobDetail(trigger.getJobKey());
        clusteredScheduler.pauseTrigger(trigger.getKey());

        final WaitListener waitListener = new WaitListener(jobKey.getName() + "_listener");
        clusteredScheduler.getListenerManager().addJobListener(waitListener);

        clusteredScheduler.triggerJob(jobDetail.getKey());

            CompletableFuture.runAsync(() -> {
                try {
                    waitListener.waitJobWasExecuted();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
            }).get(1, TimeUnit.SECONDS);

    }

    protected static void waitForTrigger(final Scheduler clusteredScheduler, final Trigger trigger, final int seconds)
            throws InterruptedException, SchedulerException, TimeoutException, ExecutionException {
        final JobKey jobKey = trigger.getJobKey();

        final AbstractJobTest.WaitListener waitListener = new AbstractJobTest.WaitListener(jobKey.getName() + "_listener");
        clusteredScheduler.getListenerManager().addJobListener(waitListener);

        CompletableFuture.runAsync(() -> {
            try {
                waitListener.waitJobWasExecuted();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
        }).get(seconds, TimeUnit.SECONDS);
    }
}
