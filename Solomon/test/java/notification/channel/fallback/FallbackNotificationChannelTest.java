package ru.yandex.solomon.alert.notification.channel.fallback;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.dao.memory.InMemoryTelegramEventsDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.telegram.ChartsClientStub;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.channel.telegram.RawTelegramLimits;
import ru.yandex.solomon.alert.notification.channel.telegram.StaffClientStub;
import ru.yandex.solomon.alert.notification.channel.telegram.TelegramClientStub;
import ru.yandex.solomon.alert.notification.channel.telegram.TelegramNotificationChannelFactory;
import ru.yandex.solomon.alert.notification.channel.telegram.TelegramTemplate;
import ru.yandex.solomon.alert.notification.channel.yachats.YaChatsClientStub;
import ru.yandex.solomon.alert.notification.channel.yachats.YaChatsNotificationChannelFactory;
import ru.yandex.solomon.alert.notification.channel.yachats.YaChatsRawLimits;
import ru.yandex.solomon.alert.notification.channel.yachats.YaChatsTemplate;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.yachats.YaChatsClient;
import ru.yandex.solomon.flags.FeatureFlag;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomTelegramNotification;

public class FallbackNotificationChannelTest {

    private TelegramClientStub tgClient;
    private YaChatsClientStub ymClient;

    private TemplateVarsFactory templateVarsFactory;
    private FallbackNotificationChannelFactory factory;
    private MustacheTemplateFactory templateFactory;
    private ManualClock clock;
    private ScheduledExecutorService executorService;
    private FeatureFlagHolderStub flagsHolder;
    private Alert alert;

    private TelegramNotificationChannelFactory makeTgFactory() throws IOException {
        var template = TelegramTemplate.create(templateFactory, templateVarsFactory);
        var staffClientStub = new StaffClientStub();
        staffClientStub.generateUsers();
        var chatIdResolver = new ChatIdResolverStub();
        var chartsClientStub = new ChartsClientStub();
        var telegramEventsDao = new InMemoryTelegramEventsDao();
        var rawLimits = new RawTelegramLimits(1000, 1000, 1e3);
        return new TelegramNotificationChannelFactory(tgClient, staffClientStub, chartsClientStub, chatIdResolver,
                templateFactory, template, rawLimits, executorService, telegramEventsDao);
    }

    private YaChatsNotificationChannelFactory makeYmFactory() throws IOException {
        var template = YaChatsTemplate.create(templateFactory, templateVarsFactory);
        var rawLimits = new YaChatsRawLimits(1000, 1000);
        return new YaChatsNotificationChannelFactory(ymClient, templateFactory, template, rawLimits, executorService);
    }

    @Before
    public void setUp() throws IOException {
        tgClient = new TelegramClientStub();
        ymClient = new YaChatsClientStub();
        templateFactory = new MustacheTemplateFactory();
        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
        flagsHolder = new FeatureFlagHolderStub();
        templateVarsFactory = new TemplateVarsFactory("localhost", "localhost");

        var tgFactory = makeTgFactory();
        var ymFactory = makeYmFactory();

        factory = new FallbackNotificationChannelFactory(tgFactory, ymFactory, flagsHolder);
        alert = randomAlert().toBuilder()
                .setGroupByLabels(List.of())
                .build();
    }

    @After
    public void tearDown() {
        tgClient.close();
        ymClient.close();
        executorService.shutdown();
    }

    @Test
    public void normalMode() {
        var notification = randomTelegramNotification(ThreadLocalRandom.current());

        var channel = factory.createChannel(notification);
        var result = channel.send(Instant.EPOCH, eval(alert, EvaluationStatus.Code.ALARM)).join();
        Assert.assertEquals(NotificationStatus.Code.SUCCESS, result.getCode());
    }

    @Test
    public void tgIsDown() {
        var notification = randomTelegramNotification(ThreadLocalRandom.current());

        var channel = factory.createChannel(notification);
        tgClient.addCodeResponse(503);
        var result = channel.send(Instant.EPOCH, eval(alert, EvaluationStatus.Code.ALARM)).join();
        Assert.assertEquals(NotificationStatus.Code.ERROR_ABLE_TO_RETRY, result.getCode());
    }

    @Test
    public void fallbackToYmForUser() {
        var notification = randomTelegramNotification(ThreadLocalRandom.current()).toBuilder()
                .setChatId(0)
                .setLogin("uranix")
                .setTextTemplate(">{{status.code}}<")
                .build();

        flagsHolder.setFlag(notification.getProjectId(), FeatureFlag.DUPLICATE_TELEGRAM_TO_MESSENGER, true);

        var channel = factory.createChannel(notification);
        tgClient.addCodeResponse(503);
        var promise = new CompletableFuture<YaChatsClient.Response>();
        ymClient.addResponse(notification.getLogin(), null, ">ALARM<", promise);
        executorService.schedule(() -> promise.complete(new YaChatsClient.Response(200, "YM to the rescue")),
                100, TimeUnit.MILLISECONDS);

        var result = channel.send(Instant.EPOCH, eval(alert, EvaluationStatus.Code.ALARM)).join();
        Assert.assertEquals(NotificationStatus.Code.SUCCESS, result.getCode());
    }

    @Test
    public void bothFail() {
        var notification = randomTelegramNotification(ThreadLocalRandom.current()).toBuilder()
                .setChatId(0)
                .setLogin("uranix")
                .setTextTemplate(">{{status.code}}<")
                .build();

        flagsHolder.setFlag(notification.getProjectId(), FeatureFlag.DUPLICATE_TELEGRAM_TO_MESSENGER, true);

        var channel = factory.createChannel(notification);
        tgClient.addCodeResponse(503);
        var promise = new CompletableFuture<YaChatsClient.Response>();
        ymClient.addResponse(notification.getLogin(), null, ">ALARM<", promise);
        executorService.schedule(() -> promise.complete(new YaChatsClient.Response(504, "YM is broken")),
                100, TimeUnit.MILLISECONDS);

        var result = channel.send(Instant.EPOCH, eval(alert, EvaluationStatus.Code.ALARM)).join();
        Assert.assertEquals(NotificationStatus.Code.ERROR_ABLE_TO_RETRY, result.getCode());
        Assert.assertEquals("YM is broken", result.getDescription());
    }

    @Test
    public void duplicateEvenIfTgIsOk() {
        var notification = randomTelegramNotification(ThreadLocalRandom.current()).toBuilder()
                .setChatId(0)
                .setLogin("uranix")
                .setTextTemplate(">{{status.code}}<")
                .build();

        flagsHolder.setFlag(notification.getProjectId(), FeatureFlag.DUPLICATE_TELEGRAM_TO_MESSENGER, true);

        var channel = factory.createChannel(notification);
        tgClient.addCodeResponse(200);
        var promise = new CompletableFuture<YaChatsClient.Response>();
        ymClient.addResponse(notification.getLogin(), null, ">ALARM<", promise);
        executorService.schedule(() -> promise.complete(new YaChatsClient.Response(200, "YM to the rescue")),
                1000, TimeUnit.SECONDS);

        var result = channel.send(Instant.EPOCH, eval(alert, EvaluationStatus.Code.ALARM)).join();
        Assert.assertEquals(NotificationStatus.Code.SUCCESS, result.getCode());
        Assert.assertEquals(0, ymClient.getLeftResponseCount());
    }
}
