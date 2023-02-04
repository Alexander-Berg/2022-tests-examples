package ru.yandex.solomon.alert.notification.channel;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.unroll.MultiAlertUtils;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.nextEval;

/**
 * @author Vladimir Gordiychuk
 */
public class SubAlertEventBatchCollectorTest {

    private static ManualClock clock;
    private static ManualScheduledExecutorService executorService;
    private static ConcurrentLinkedQueue<List<Event>> sandbox = new ConcurrentLinkedQueue<>();
    private EventBatchCollector collector;

    @BeforeClass
    public static void before() {
        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
    }

    @AfterClass
    public static void after() {
        executorService.shutdownNow();
    }

    @Before
    public void setUp() throws Exception {
        sandbox.clear();
        collector = new SubAlertEventBatchCollector(executorService, (events, future) -> {
            sandbox.add(events);
            future.complete(NotificationStatus.SUCCESS);
        });
    }

    @Test
    public void singleEventIntoBatch() {
        Alert parent = randomAlert()
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        Labels labels = Labels.of("host", "alerting-01");
        SubAlert subAlert = SubAlert.newBuilder()
                .setId(MultiAlertUtils.getAlertId(parent, labels))
                .setParent(parent)
                .setGroupKey(labels)
                .build();

        Event event1 = eval(subAlert, EvaluationStatus.ALARM);
        CompletableFuture future = collector.add(event1);

        clock.passedTime(1, TimeUnit.MINUTES);
        future.join();

        assertThat(sandbox.size(), equalTo(1));
        assertThat(sandbox.poll(), allOf(iterableWithSize(1), hasItem(event1)));

        clock.passedTime(1, TimeUnit.MINUTES);
        assertThat(sandbox.size(), equalTo(0));

        Event event2 = nextEval(event1, EvaluationStatus.OK);
        CompletableFuture future2 = collector.add(event2);

        clock.passedTime(1, TimeUnit.MINUTES);
        future2.join();

        assertThat(sandbox.size(), equalTo(1));
        assertThat(sandbox.poll(), allOf(iterableWithSize(1), hasItem(event2)));
    }

    @Test
    public void multipleEventIntoBatch() {
        Alert parent = randomAlert()
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        SubAlert alice = SubAlert.newBuilder()
                .setId(MultiAlertUtils.getAlertId(parent, Labels.of("host", "alice")))
                .setParent(parent)
                .setGroupKey(Labels.of("host", "alice"))
                .build();

        SubAlert bob = SubAlert.newBuilder()
                .setId(MultiAlertUtils.getAlertId(parent, Labels.of("host", "bob")))
                .setParent(parent)
                .setGroupKey(Labels.of("host", "bob"))
                .build();

        Event aliceEvent = eval(alice, EvaluationStatus.ALARM);
        Event bobEvent = eval(bob, EvaluationStatus.OK);

        CompletableFuture aliceFuture = collector.add(aliceEvent);
        clock.passedTime(10, TimeUnit.SECONDS);
        CompletableFuture bobFuture = collector.add(bobEvent);

        clock.passedTime(1, TimeUnit.MINUTES);
        CompletableFuture.allOf(aliceFuture, bobFuture).join();

        assertThat(sandbox.size(), equalTo(1));
        assertThat(sandbox.poll(), equalTo(Arrays.asList(aliceEvent, bobEvent)));
    }

    @Test
    public void parallelBatching() {
        Alert parent = randomAlert()
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        CompletableFuture future = IntStream.range(0, 100)
                .parallel()
                .mapToObj(index -> {
                    Labels labels = Labels.of("host", "solomon-" + index);
                    SubAlert subAlert = SubAlert.newBuilder()
                            .setId(MultiAlertUtils.getAlertId(parent, labels))
                            .setParent(parent)
                            .setGroupKey(labels)
                            .build();


                    return EventTestSupport.eval(subAlert, ThreadLocalRandom.current().nextBoolean() ? EvaluationStatus.OK : EvaluationStatus.ALARM);
                })
                .map(collector::add)
                .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOfUnit));

        clock.passedTime(1, TimeUnit.MINUTES);
        future.join();

        assertThat(sandbox.size(), equalTo(1));
        assertThat(sandbox.poll(), iterableWithSize(100));
    }

    @Test
    public void batchedIndependent() {
        Alert alice = randomAlert()
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        Alert bob = randomAlert()
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        CompletableFuture future = IntStream.range(0, 100)
                .parallel()
                .mapToObj(index -> {
                    Labels labels = Labels.of("host", "solomon-" + index);
                    Alert parent = ThreadLocalRandom.current().nextBoolean() ? alice : bob;
                    SubAlert subAlert = SubAlert.newBuilder()
                            .setId(MultiAlertUtils.getAlertId(parent, labels))
                            .setParent(parent)
                            .setGroupKey(labels)
                            .build();

                    return EventTestSupport.eval(subAlert, ThreadLocalRandom.current().nextBoolean() ? EvaluationStatus.OK : EvaluationStatus.ALARM);
                })
                .map(collector::add)
                .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOfUnit));

        clock.passedTime(1, TimeUnit.MINUTES);
        future.join();

        assertThat(sandbox.size(), equalTo(2));
    }
}
