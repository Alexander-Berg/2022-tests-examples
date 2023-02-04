package ru.yandex.solomon.alert.notification.channel.telegram;

import java.io.CharArrayWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.mustachejava.util.HtmlEscaper;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.bolts.collection.Try;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.misc.random.Random2;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.charts.exceptions.ChartsEmptyResultException;
import ru.yandex.solomon.alert.charts.exceptions.ChartsUnknownException;
import ru.yandex.solomon.alert.dao.memory.InMemoryTelegramEventsDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.telegram.TelegramNotification;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.MultiAlertUtils;
import ru.yandex.solomon.alert.util.RateLimit;
import ru.yandex.solomon.alert.util.RateLimiters;
import ru.yandex.solomon.config.thread.StubThreadPoolProvider;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomSubAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.notification.channel.telegram.EventFeatures.chooseMaybeOneEventForPhoto;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomTelegramNotification;


/**
 * @author alexlovkov
 **/
@RunWith(Parameterized.class)
public class TelegramNotificationChannelTest {

    private static final String URL = "localhost";
    private static final int MAX_CAPTION_SIZE_FOR_PHOTO = 1024;

    @Rule
    public Timeout globalTimeout = Timeout.builder()
        .withTimeout(500, TimeUnit.SECONDS)
        .withLookingForStuckThread(true)
        .build();

    @Parameterized.Parameter
    public int alertNameLength;
    private InMemoryTelegramEventsDao telegramEventsDao;

    @Parameterized.Parameters(name = "{0}")
    public static List<Integer> namesLength() {
        return Arrays.asList(
            10, 1300
        );
    }

    private StaffClientStub staffClientStub;

    private ManualClock clock;
    private ManualScheduledExecutorService executorService;
    private TelegramClientStub telegramClient;
    private TelegramTemplate template;
    private TelegramNotification notification;
    private TelegramNotificationChannelFactory factory;
    private StubThreadPoolProvider threadPoolProvider;
    private ChatIdResolverStub chatIdResolver;
    private ChartsClientStub chartsClientStub;
    private Event event;
    private FeatureFlagHolderStub flagsHolder;

    @Before
    public void setUp() throws Exception {
        MustacheTemplateFactory templateFactory = new MustacheTemplateFactory();

        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
        telegramClient = new TelegramClientStub();

        template = TelegramTemplate.create(templateFactory, new TemplateVarsFactory(URL, URL));
        notification = randomTelegramNotification(ThreadLocalRandom.current());

        threadPoolProvider = new StubThreadPoolProvider();

        staffClientStub = new StaffClientStub();
        staffClientStub.generateUsers();
        chatIdResolver = new ChatIdResolverStub();
        chartsClientStub = new ChartsClientStub();
        telegramEventsDao = new InMemoryTelegramEventsDao();
        RawTelegramLimits rawLimits = new RawTelegramLimits(1000, 1000, 1e3);
        flagsHolder = new FeatureFlagHolderStub();
        factory =
            new TelegramNotificationChannelFactory(telegramClient, staffClientStub, chartsClientStub, chatIdResolver,
                templateFactory, template, rawLimits, executorService, telegramEventsDao);
        this.event = eval(randomAlert()
                .toBuilder()
                .setName(Random2.R.nextString(this.alertNameLength))
                .build(),
            EvaluationStatus.Code.ALARM);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
        threadPoolProvider.close();
    }

    private TelegramNotificationChannel createChannel() {
        return createChannel(1000, 1e3);
    }

    private TelegramNotificationChannel createChannel(
        int userRateLimit,
        double groupRateLimit)
    {
        return createChannel(RateLimiters.create(1000), userRateLimit, groupRateLimit);
    }

    private TelegramNotificationChannel createChannel(
        RateLimit generalRateLimit,
        int userRateLimit,
        double groupRateLimit)
    {
        RawTelegramLimits rawLimits = new RawTelegramLimits(1000, userRateLimit, groupRateLimit);
        boolean isGroup = StringUtils.isEmpty(notification.getLogin());
        TelegramLimits telegramLimits = TelegramLimits.create(generalRateLimit, rawLimits, isGroup);
        return new TelegramNotificationChannel(notification, telegramClient, staffClientStub,
            chartsClientStub, chatIdResolver, executorService, telegramEventsDao,
            telegramLimits, template);
    }

    @Test
    public void successNotification() {
        telegramClient.addCodeResponse(200);
        telegramClient.addCodeResponse(200);

        notification = notification
            .toBuilder()
            .setSendScreenshot(true)
            .setChatId(ChatIdResolverStub.CHAT_ID).build();
        NotificationChannel channel = createChannel();

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        assertMessages(status);
    }

    @Test
    public void telegramEventSingle() {
        telegramClient.addCodeResponse(200);
        telegramClient.addCodeResponse(200);

        notification = notification
            .toBuilder()
            .setSendScreenshot(true)
            .setChatId(ChatIdResolverStub.CHAT_ID).build();
        NotificationChannel channel = createChannel();

        var event = eval(randomAlert()
                .toBuilder()
                .setId("test")
                .setGroupByLabels(List.of())
                .build(),
            EvaluationStatus.Code.ALARM);

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());

        var record = Iterables.getOnlyElement(telegramEventsDao.getDao().values());
        assertEquals("test", record.getAlertId());
        assertEquals("", record.getSubAlertId());
    }

    @Test
    public void telegramEventMulti() {
        telegramClient.addCodeResponse(200);
        telegramClient.addCodeResponse(200);

        notification = notification
            .toBuilder()
            .setSendScreenshot(true)
            .setChatId(ChatIdResolverStub.CHAT_ID).build();
        NotificationChannel channel = createChannel();

        var subAlert = randomSubAlert(ThreadLocalRandom.current());

        var event = eval(subAlert, EvaluationStatus.Code.ALARM);

        var future = channel.send(Instant.EPOCH, event);
        while (!future.isDone()) {
            clock.passedTime(1, TimeUnit.SECONDS);
        }
        assertEquals(NotificationStatus.Code.SUCCESS, future.join().getCode());

        var record = Iterables.getOnlyElement(telegramEventsDao.getDao().values());
        assertEquals(subAlert.getId(), record.getSubAlertId());
        assertEquals(subAlert.getParent().getId(), record.getAlertId());
    }

    private boolean isLongAlert(Alert alert) {
        return alert.getName().length() > MAX_CAPTION_SIZE_FOR_PHOTO;
    }

    @Test
    public void failedNotificationLongMessage() {
        telegramClient.addCodeResponse(400);

        notification = notification.toBuilder()
            .setChatId(ChatIdResolverStub.CHAT_ID)
            .setSendScreenshot(true)
            .build();
        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.INVALID_REQUEST));
        assertThat(telegramClient.countMessageInSandbox(), equalTo(1));

        TelegramClientStub.Message message = telegramClient.receiveMessage();
        assertEquals(ChatIdResolverStub.CHAT_ID, message.getChatId());
        assertThat(message.getText(), containsString(event.getAlert().getName()));
        assertThat(message.getText(), containsString("ALARM"));
    }

    @Test
    public void resolverDoesnthaveGroup() {
        telegramClient.addCodeResponse(200);

        chatIdResolver.returnNull();
        notification = notification.toBuilder()
            .setLogin("")
            .setChatId(42)
            .build();
        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertEquals(NotificationStatus.Code.INVALID_REQUEST, status.getCode());
        assertEquals(0, telegramClient.countMessageInSandbox());
        assertTrue(status.getDescription().contains("can't resolve"));
    }

    @Test
    public void successNotificationWithChatIdInsideNotification() {
        telegramClient.addCodeResponse(200);
        telegramClient.addCodeResponse(200);

        notification = notification.toBuilder().setChatId(666L).build();
        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertMessages(status);
    }

    @Test
    public void userWithoutTelegram() {
        staffClientStub.generateUsersWithoutTelegram();
        notification = notification.toBuilder().setLogin("alexlovkov").build();
        telegramClient.addCodeResponse(200);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertEquals(NotificationStatus.Code.INVALID_REQUEST, status.getCode());
        assertEquals(0, telegramClient.countMessageInSandbox());
        assertTrue(status.getDescription().contains("doesn't have telegram"));
    }

    @Test
    public void resolverDoesntHaveUser() {
        chatIdResolver.returnNull();
        notification = notification.toBuilder().setLogin("alexlovkov").build();
        telegramClient.addCodeResponse(200);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertEquals(NotificationStatus.Code.NOT_SUBSCRIBED, status.getCode());
        assertEquals(0, telegramClient.countMessageInSandbox());
        assertTrue(status.getDescription().contains("is not known"));
    }

    @Test
    public void sendToDismissedUser() {
        // we don't check dismissing for group
        notification = notification.toBuilder()
            .setLogin("alexlovkov")
            .build();
        staffClientStub.generateDismissedUsers();
        telegramClient.addCodeResponse(200);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.INVALID_REQUEST));
        assertEquals(0, telegramClient.countMessageInSandbox());
        assertTrue(status.getDescription().contains(" dismissed"));
    }

    @Test
    public void staffNotFound() {
        notification = notification.toBuilder()
                .setLogin("nobody")
                .build();
        staffClientStub.doNotGenerateUsers();
        telegramClient.addCodeResponse(200);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.INVALID_REQUEST));
        assertEquals(0, telegramClient.countMessageInSandbox());
        assertTrue(status.getDescription().contains("not found"));
    }

    @Test
    public void exceedUserRateLimit() {
        telegramClient.addCodeResponse(200);
        notification = notification.toBuilder()
            .setChatId(ChatIdResolverStub.CHAT_ID)
            .setSendScreenshot(false)
            .build();
        int userRateLimit = 1000;
        double groupRateLimit = 1e3;
        if (notification.getLogin() == null) {
            groupRateLimit = 1;
        } else {
            userRateLimit = 1;
        }
        NotificationChannel channel = createChannel(userRateLimit, groupRateLimit);
        for (int i = 0; i < 2; i++) {
            channel.send(Instant.EPOCH, event).join();
            channel.send(Instant.EPOCH, event).join();
        }
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        NotificationStatus secondStatus = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(telegramClient.countMessageInSandbox(), equalTo(5));

        TelegramClientStub.Message message = telegramClient.receiveMessage();
        assertEquals(ChatIdResolverStub.CHAT_ID, message.getChatId());
        assertThat(message.getText(), containsString(event.getAlert().getName()));
        assertThat(message.getText(), containsString("ALARM"));

        assertThat(secondStatus.getCode(), equalTo(NotificationStatus.Code.RESOURCE_EXHAUSTED));
        assertThat(secondStatus.getDescription(), Matchers.containsString("chat"));
    }

    @Test
    public void generalRateLimitTest() {
        telegramClient.addCodeResponse(200);
        RateLimit generalRateLimit = RateLimiters.create(1);
        TelegramNotificationChannel firstChannel =
            createChannel(generalRateLimit, 1000, 1e3);
        TelegramNotificationChannel secondChannel =
            createChannel(generalRateLimit, 1000, 1e3);
        NotificationStatus firstStatus = firstChannel.send(Instant.EPOCH, event).join();
        NotificationStatus secondStatus = secondChannel.send(Instant.EPOCH, event).join();

        assertThat(firstStatus.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(secondStatus.getCode(), equalTo(NotificationStatus.Code.RESOURCE_EXHAUSTED));
    }

    @Test
    public void invalidRequest() {
        telegramClient.addCodeResponse(400);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.INVALID_REQUEST));
    }

    @Test
    public void tooManyRequests() {
        telegramClient.addCodeResponse(429);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.RESOURCE_EXHAUSTED));
    }

    @Test
    public void serverProblems500() {
        telegramClient.addCodeResponse(500);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
    }

    @Test
    public void otherErrors() {
        telegramClient.addCodeResponse(666);

        NotificationChannel channel = createChannel();
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.toString(), status.getCode(), equalTo(NotificationStatus.Code.ERROR));
    }

    @Test
    public void defaultTemplate() {
        telegramClient.addCodeResponse(200);
        telegramClient.addCodeResponse(200);
        TelegramNotification notification = randomTelegramNotification(ThreadLocalRandom.current())
            .toBuilder()
            .setLogin("alexlovkov")
            .setTextTemplate("")
            .build();
        this.notification = notification;
        NotificationChannel channel = factory.createChannel(notification);

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        assertMessages(status);
    }

    @Test
    public void customHostTemplate() {
        telegramClient.addCodeResponse(200);
        telegramClient.addCodeResponse(200);
        TelegramNotification notification = randomTelegramNotification(ThreadLocalRandom.current())
            .toBuilder()
            .setLogin("alexlovkov")
            .setTextTemplate("{{alert.name}} in state {{status.code}}")
            .build();
        this.notification = notification;
        NotificationChannel channel = factory.createChannel(notification);

        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        assertMessages(status);
    }

    @Test
    public void defaultMultiAlertTemplate() {
        for (int i = 0; i < 10; i++) {
            telegramClient.addCodeResponse(200);
        }
        TelegramNotification notification = randomTelegramNotification(ThreadLocalRandom.current())
            .toBuilder()
            .setTextTemplate("")
            .setLogin("alexlovkov")
            .build();
        NotificationChannel channel = factory.createChannel(notification);

        Alert parent = randomAlert().toBuilder()
            .setName("My alert")
            .setGroupByLabel("host")
            .build();

        int code = Random2.R.nextInt(EvaluationStatus.Code.values().length);
        EvaluationStatus evalStatus = EvaluationStatus.Code.values()[code].toStatus();

        List<CompletableFuture<NotificationStatus>> futures = IntStream.range(0, 10)
            .parallel()
            .mapToObj(index -> {
                Labels labels = Labels.of("host", "my-host-" + index);
                SubAlert subAlert = SubAlert.newBuilder()
                    .setId(MultiAlertUtils.getAlertId(parent, labels))
                    .setParent(parent)
                    .setGroupKey(labels)
                    .build();

                return eval(subAlert, evalStatus);
            })
            .map(event -> channel.send(Instant.EPOCH, event)
                .thenApply(status -> {
                    if (status.getCode() != NotificationStatus.Code.SUCCESS) {
                        throw new IllegalStateException(status.toString());
                    }
                    return status;
                }))
            .collect(Collectors.toList());

        // await complete batching
        clock.passedTime(1, TimeUnit.MINUTES);
        CompletableFutures.allOfVoid(futures).join();

        assertThat(telegramClient.countMessageInSandbox(), equalTo(1));

        TelegramClientStub.Message message = telegramClient.receiveMessage();
        assertThat(message.getChatId(), equalTo(ChatIdResolverStub.CHAT_ID));
        assertThat(message.getText(), containsString(parent.getName()));
        assertThat(message.getText(), containsString(evalStatus.getCode().toString()));
        EvaluationStatus.Code evalStatusCode = evalStatus.getCode();
        Set<EvaluationStatus.Code> codesWithScreenshot = Set.of(EvaluationStatus.Code.OK, EvaluationStatus.Code.WARN, EvaluationStatus.Code.ALARM);
        if (codesWithScreenshot.contains(evalStatusCode)) {
            assertEquals(notification.isSendScreenshot(), message.isPhoto());
        } else {
            assertFalse(message.isPhoto());
        }
    }

    @Test
    public void eventToPhoto() {
        randomSubAlert(ThreadLocalRandom.current());

        var parent = randomAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setGroupByLabel("host")
            .build();

        var alice = SubAlert.newBuilder()
            .setParent(parent)
            .setId("alice")
            .setGroupKey(Labels.of("host", "alice"))
            .build();

        var bob = SubAlert.newBuilder()
            .setParent(parent)
            .setId("bob")
            .setGroupKey(Labels.of("host", "bob"))
            .build();

        var eva = SubAlert.newBuilder()
            .setParent(parent)
            .setId("eva")
            .setGroupKey(Labels.of("host", "eva"))
            .build();

        {
            var aliceEval = eval(alice, EvaluationStatus.OK);
            var evaEval = eval(eva, EvaluationStatus.OK);
            var bobEval = eval(bob, EvaluationStatus.OK);

            var result = chooseMaybeOneEventForPhoto(true, List.of(aliceEval, evaEval, bobEval));
            assertEquals("events compare by grop labels", aliceEval, result.event());
            assertTrue(result.isPhoto());
        }

        {
            var aliceEval = eval(alice, EvaluationStatus.OK);
            var evaEval = eval(eva, EvaluationStatus.ALARM);
            var bobEval = eval(bob, EvaluationStatus.ALARM);

            var result = chooseMaybeOneEventForPhoto(true, List.of(aliceEval, evaEval, bobEval));
            assertEquals("events compare by status", bobEval, result.event());
            assertTrue(result.isPhoto());
        }

        {
            var aliceEval = eval(alice, EvaluationStatus.OK);
            var evaEval = eval(eva, EvaluationStatus.WARN);
            var bobEval = eval(bob, EvaluationStatus.ALARM);

            var result = chooseMaybeOneEventForPhoto(true, List.of(aliceEval, evaEval, bobEval));
            assertEquals("events compare by status", bobEval, result.event());
            assertTrue(result.isPhoto());
        }

        {
            var aliceEval = eval(alice, EvaluationStatus.NO_DATA);
            var evaEval = eval(eva, EvaluationStatus.NO_DATA);
            var bobEval = eval(bob, EvaluationStatus.ERROR);

            var events = List.of(aliceEval, evaEval, bobEval);
            var result = chooseMaybeOneEventForPhoto(true, events);
            assertTrue(events.contains(result.event()));
            assertFalse(result.isPhoto());
        }

        {
            var aliceEval = eval(alice, EvaluationStatus.OK);
            var evaEval = eval(eva, EvaluationStatus.WARN);
            var bobEval = eval(bob, EvaluationStatus.ALARM);

            var events = List.of(aliceEval, evaEval, bobEval);
            var result = chooseMaybeOneEventForPhoto(false, events);
            assertTrue(events.contains(result.event()));
            assertFalse(result.isPhoto());
        }

        {
            var aliceEval = eval(alice, EvaluationStatus.NO_DATA);
            var evaEval = eval(eva, EvaluationStatus.NO_DATA);
            var bobEval = eval(bob, EvaluationStatus.ERROR);

            var events = List.of(aliceEval, evaEval, bobEval);
            var result = chooseMaybeOneEventForPhoto(false, events);
            assertTrue(events.contains(result.event()));
            assertFalse(result.isPhoto());
        }
    }

    @Test
    public void sendErrorSubAlert() {
        var subAlert = randomSubAlert(ThreadLocalRandom.current());

        var event = eval(subAlert, EvaluationStatus.Code.ERROR);
        var future = createChannel().send(Instant.EPOCH, event);
        clock.passedTime(1, TimeUnit.MINUTES);
        var result = future.join();

        assertEquals(NotificationStatus.Code.SUCCESS, result.getCode());
        assertEquals(1, telegramClient.countMessageInSandbox());
    }

    private static String mustacheEscape(String input) {
        CharArrayWriter buffer = new CharArrayWriter();
        HtmlEscaper.escape(input, buffer);
        return buffer.toString();
    }

    @Test
    public void subalertsWithAbsentLabel() {
        Alert parent = randomAlert()
                .toBuilder()
                .setGroupByLabels(List.of("host"))
                .build();

        Labels labels1 = Labels.of("host", "solomon-01");
        Labels labels2 = Labels.of();

        var subAlert1 = SubAlert.newBuilder()
                    .setId(MultiAlertUtils.getAlertId(parent, labels1))
                    .setParent(parent)
                    .setGroupKey(labels1)
                    .build();
        var subAlert2 = SubAlert.newBuilder()
                .setId(MultiAlertUtils.getAlertId(parent, labels2))
                .setParent(parent)
                .setGroupKey(labels2)
                .build();


        var event1 = eval(subAlert1, EvaluationStatus.Code.ALARM);
        var event2 = eval(subAlert2, EvaluationStatus.Code.ALARM);
        var channel = createChannel();
        var future1 = channel.send(Instant.EPOCH, event1);
        var future2 = channel.send(Instant.EPOCH, event2);
        clock.passedTime(1, TimeUnit.MINUTES);
        var result = CompletableFutures.allOf2(future1, future2).join();

        assertEquals(NotificationStatus.Code.SUCCESS, result.get1().getCode());
        assertEquals(NotificationStatus.Code.SUCCESS, result.get2().getCode());
        assertEquals(1, telegramClient.countMessageInSandbox());
        String message = telegramClient.receiveMessage().getText();
        assertThat(message, containsString(mustacheEscape("{host='solomon-01'}")));
        assertThat(message, containsString(mustacheEscape("{host=-}")));
    }

    @Test
    public void subalertsWithAbsentLabels() {
        Alert parent = randomAlert()
                .toBuilder()
                .setGroupByLabels(List.of("host", "status"))
                .build();

        List<Labels> labelsOptions = List.of(
                Labels.of("host", "solomon-01", "status", "OK"),
                Labels.of("host", "solomon-02"),
                Labels.of()
        );


        var subAlerts = labelsOptions.stream()
                .map(labels -> SubAlert.newBuilder()
                        .setId(MultiAlertUtils.getAlertId(parent, labels))
                        .setParent(parent)
                        .setGroupKey(labels)
                        .build())
                .collect(Collectors.toList());

        var events = subAlerts.stream()
                .map(alert -> eval(alert, EvaluationStatus.Code.ALARM))
                .collect(Collectors.toList());
        var channel = createChannel();

        var future = events.stream()
                .map(event -> channel.send(Instant.EPOCH, event))
                .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOf));
        clock.passedTime(1, TimeUnit.MINUTES);
        var results = future.join();

        for (var result : results) {
            assertEquals(NotificationStatus.Code.SUCCESS, result.getCode());
        }
        assertEquals(1, telegramClient.countMessageInSandbox());
        String message = telegramClient.receiveMessage().getText();

        assertThat(message, containsString(mustacheEscape("{host='solomon-01', status='OK'}")));
        assertThat(message, containsString(mustacheEscape("{host='solomon-02', status=-}")));
        assertThat(message, containsString(mustacheEscape("{host=-, status=-}")));
    }

    @Test
    public void sendLongWithScreenToGroup() {
        notification = notification.toBuilder()
                .setChatId(ChatIdResolverStub.CHAT_ID)
                .setLogin("")
                .setSendScreenshot(true)
                .build();
        var result = createChannel(3, 0.3)
                .send(Instant.EPOCH, event)
                .join();

        assertMessages(result);
    }

    @Test
    public void chartsError() {
        chartsClientStub.predefineResult(Try.failure(new ChartsUnknownException("unavailable")));
        notification = notification.toBuilder()
                .setChatId(ChatIdResolverStub.CHAT_ID)
                .setLogin("")
                .setSendScreenshot(true)
                .build();
        var status = createChannel(3, 0.3)
                .send(Instant.EPOCH, event)
                .join();

        assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());
        assertEquals(1, telegramClient.countMessageInSandbox());
        assertEquals("expected one event in dao", 1, telegramEventsDao.getDao().size());
    }

    @Test
    public void chartsEmpty() {
        chartsClientStub.predefineResult(Try.failure(new ChartsEmptyResultException("NO_DATA", "reqId", "traceId")));
        notification = notification.toBuilder()
                .setChatId(ChatIdResolverStub.CHAT_ID)
                .setLogin("")
                .setSendScreenshot(true)
                .build();
        var status = createChannel(3, 0.3)
                .send(Instant.EPOCH, event)
                .join();

        assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());
        assertEquals(1, telegramClient.countMessageInSandbox());
        assertEquals("expected one event in dao", 1, telegramEventsDao.getDao().size());
    }

    private void assertMessages(NotificationStatus status) {
        assertMessages(status, event.getAlert());
    }

    private void assertMessages(NotificationStatus status, Alert alert) {
        assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());
        int expectedMessages = 1;
        if (isLongAlert(alert) && notification.isSendScreenshot()) {
            expectedMessages = 2;
        }
        assertEquals("Messages in sandbox", expectedMessages, telegramClient.countMessageInSandbox());

        TelegramClientStub.Message message = telegramClient.receiveMessage();
        assertEquals(ChatIdResolverStub.CHAT_ID, message.getChatId());
        assertThat("Message text contains alert name", message.getText(), containsString(event.getAlert().getName()));
        assertThat("Message text contains alert status", message.getText(), containsString(event.getState().getStatus().getCode().name()));

        if (expectedMessages == 2) {
            assertThat("expected one text message", telegramClient.countMessageInSandbox(), equalTo(1));
            TelegramClientStub.Message photoMessage = telegramClient.receiveMessage();
            assertTrue("expected photo message", photoMessage.isPhoto());
        } else {
            assertThat("expected no text messages", telegramClient.countMessageInSandbox(), equalTo(0));
        }
        assertEquals("expected one event in dao", 1, telegramEventsDao.getDao().size());
    }
}
