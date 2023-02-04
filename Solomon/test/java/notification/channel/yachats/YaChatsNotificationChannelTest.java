package ru.yandex.solomon.alert.notification.channel.yachats;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.yachats.YaChatsClient;
import ru.yandex.solomon.config.thread.StubThreadPoolProvider;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomTelegramNotification;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomYaChatsNotification;


/**
 * @author Ivan Tsybulin
 **/
public class YaChatsNotificationChannelTest {

    private static final String URL = "localhost";

    @Rule
    public Timeout globalTimeout = Timeout.builder()
        .withTimeout(500, TimeUnit.SECONDS)
        .withLookingForStuckThread(true)
        .build();

    private ManualScheduledExecutorService executorService;
    private YaChatsClientStub yaChatsClient;
    private StubThreadPoolProvider threadPoolProvider;
    private Event event;
    private YaChatsNotificationChannelFactory factory;

    @Before
    public void setUp() throws Exception {
        MustacheTemplateFactory templateFactory = new MustacheTemplateFactory();

        ManualClock clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
        yaChatsClient = new YaChatsClientStub();

        YaChatsTemplate template = YaChatsTemplate.create(templateFactory, new TemplateVarsFactory(URL, URL));

        threadPoolProvider = new StubThreadPoolProvider();

        YaChatsRawLimits rawLimits = new YaChatsRawLimits(1000, 1000);
        factory = new YaChatsNotificationChannelFactory(yaChatsClient, templateFactory, template, rawLimits, executorService);
        this.event = eval(randomAlert()
                .toBuilder()
                .build(),
            EvaluationStatus.Code.ALARM);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
        threadPoolProvider.close();
    }

    @Test
    public void successNotification() {
        var notification = randomYaChatsNotification(ThreadLocalRandom.current()).toBuilder()
                .setTextTemplate("my template")
                .build();
        NotificationChannel channel = factory.createChannel(notification);

        yaChatsClient.addResponse(notification.getLogin(), notification.getGroupId(), "my template",
                CompletableFuture.completedFuture(new YaChatsClient.Response(200, "OK")));

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        Assert.assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());
    }

    @Test
    public void failedNotification() {
        var notification = randomYaChatsNotification(ThreadLocalRandom.current()).toBuilder()
                .setTextTemplate("my template")
                .build();
        NotificationChannel channel = factory.createChannel(notification);

        yaChatsClient.addResponse(notification.getLogin(), notification.getGroupId(), "my template",
                CompletableFuture.completedFuture(new YaChatsClient.Response(400, "wtf")));

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        Assert.assertEquals(NotificationStatus.Code.INVALID_REQUEST, status.getCode());
        Assert.assertEquals("wtf", status.getDescription());
    }

    @Test
    public void canHandleTelegram() {
        var notification = randomTelegramNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate("my tg template")
                .setLogin("uranix")
                .build();
        NotificationChannel channel = factory.createChannel(notification);

        yaChatsClient.addResponse("uranix", null, "my tg template",
                CompletableFuture.completedFuture(new YaChatsClient.Response(200, "fallback")));

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        Assert.assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());
    }

    @Test
    public void canHandleTelegramGroup() {
        var notification = randomTelegramNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate("my tg template")
                .setLogin("")
                .setChatId(-100500)
                .build();
        NotificationChannel channel = factory.createChannel(notification);

        yaChatsClient.addResponse("", "0/0/tg-100500", "my tg template",
                CompletableFuture.completedFuture(new YaChatsClient.Response(200, "fallback")));

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        Assert.assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());
    }
}
