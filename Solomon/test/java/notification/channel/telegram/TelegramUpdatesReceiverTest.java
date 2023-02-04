package ru.yandex.solomon.alert.notification.channel.telegram;


import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.io.Resources;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.MuteConverter;
import ru.yandex.solomon.alert.charts.ChartsClient;
import ru.yandex.solomon.alert.client.MuteApi;
import ru.yandex.solomon.alert.cluster.broker.mute.ProjectMuteService;
import ru.yandex.solomon.alert.cluster.broker.mute.search.MuteSearch;
import ru.yandex.solomon.alert.dao.TelegramEventContext;
import ru.yandex.solomon.alert.dao.TelegramEventRecord;
import ru.yandex.solomon.alert.dao.TelegramEventsDao;
import ru.yandex.solomon.alert.dao.TelegramRecord;
import ru.yandex.solomon.alert.dao.memory.InMemoryMutesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryTelegramDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryTelegramEventsDao;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.telegram.DefaultTelegramClient;
import ru.yandex.solomon.alert.telegram.dto.TelegramUpdate;
import ru.yandex.solomon.config.protobuf.Time;
import ru.yandex.solomon.config.protobuf.TimeUnit;
import ru.yandex.solomon.config.protobuf.alert.TelegramChannelConfig;
import ru.yandex.solomon.locks.ReadOnlyDistributedLockStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.util.host.HostUtils;
import ru.yandex.staff.UserInfo;


/**
 * @author alexlovkov
 **/
public class TelegramUpdatesReceiverTest {

    private TelegramClientStub client;
    private InMemoryTelegramDao dao;
    private TelegramChannelConfig.Builder config;
    private TelegramUpdatesReceiver telegramUpdatesReceiver;
    private StaffClientStub staffClient;
    private ScheduledExecutorService schedulerExecutorService;
    private ReadOnlyDistributedLockStub distributedLock;
    private ChatIdResolverImpl chatIdResolver;
    private TelegramEventsDao telegramEventsDao;
    private ChartsClient chartsClient;
    private MuteApi muteApi;

    @Rule
    public Timeout timeoutRule = Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    @Before
    public void setUp() {
        client = new TelegramClientStub();
        config = TelegramChannelConfig.newBuilder();
        config.setBotName("Solomon-super-bot");
        config.setToken("MySecretToken");
        config.setRateLimitEventsPerSecond(30);
        config.setRateLimitEventsPerChat(1);
        config.setRateLimitEventsPerSecondForGroup(0.3);
        Time period = Time.newBuilder().setValue(30).setUnit(TimeUnit.MILLISECONDS).build();
        config.setFetchPeriod(period);
        staffClient = new StaffClientStub();
        schedulerExecutorService = Executors.newScheduledThreadPool(4);
        distributedLock = new ReadOnlyDistributedLockStub(new ManualClock());
        distributedLock.setOwner(HostUtils.getFqdn());
        telegramEventsDao = new InMemoryTelegramEventsDao();
        chartsClient = new ChartsClientStub();
        var mutesDao = new InMemoryMutesDao();
        var mutesSearch = new MuteSearch(MuteConverter.INSTANCE);
        var projectMuteService = new ProjectMuteService(
                "junk",
                Clock.systemUTC(),
                mutesDao,
                mutesSearch,
                MuteConverter.INSTANCE
        );
        projectMuteService.run().join();
        muteApi = new MuteApiStub(projectMuteService);
    }

    @After
    public void shutDown() {
        schedulerExecutorService.shutdownNow();
        client.close();
    }

    private void runFetch(int rows) {
        dao = new InMemoryTelegramDao(rows);
        chatIdResolver = new ChatIdResolverImpl(dao);
        var templateVarsFactory = new TemplateVarsFactory("solomon", "solomon.cloud");
        telegramUpdatesReceiver =
            new TelegramUpdatesReceiver(client, schedulerExecutorService,
                schedulerExecutorService, config.build(), dao, staffClient, distributedLock,
                chatIdResolver, telegramEventsDao, chartsClient, templateVarsFactory, muteApi);
    }

    @Test
    public void testStartMessage() throws InterruptedException, IOException {
        staffClient.generateUsersByTelegramLogin();
        client.addUpdate(buildStartMessage(1, "alex2", 3, "/start"));
        client.addUpdate(buildStartMessage(4, "alEx5", 6, "/start"));
        client.addUpdate(buildStartMessage(7, "ALEX8", 9, "/start "));
        client.addUpdate(buildStartMessage(10, "alex11", 12, " /start"));
        client.addUpdate(buildStartMessage(13, "alex14", 15, "/whatever"));
        client.addUpdate(buildStartMessage(13, "alex14", 15, ""));
        for (int i = 0; i < 3; i++) {
            client.addCodeResponse(200);
        }
        runFetch(2);
        dao.sync();

        TelegramRecord record = dao.get(3).join().get();
        Assert.assertEquals(3, record.getChatId());
        Assert.assertEquals("alex2", record.getName());
        Assert.assertFalse(record.isGroup());

        record = dao.get(6).join().get();
        Assert.assertEquals(6, record.getChatId());
        Assert.assertEquals("alEx5", record.getName());
        Assert.assertFalse(record.isGroup());

        List<String> groups = chatIdResolver.listGroups();
        Assert.assertTrue(groups.isEmpty());

        Assert.assertNotEquals(0, chatIdResolver.getChatIdByTelegramLogin("alex5"));
        Assert.assertNotEquals(0, chatIdResolver.getChatIdByTelegramLogin("ALEX5"));
        Assert.assertEquals(chatIdResolver.getChatIdByTelegramLogin("alex8"), chatIdResolver.getChatIdByTelegramLogin("ALEX8"));
    }

    // TODO: fix race here
    @Test
    public void increaseUpdateIdIfStaffException() throws IOException, InterruptedException {
        client.addUpdate(buildStartMessage(1, "alex2", 3, "/start"));
        client.addCodeResponse(200);
        client.setCountDownLatch(1);
        runFetch(0);
        client.sync();
        // we send message before increasing lastSeenUpdateId, so we have a race here
        while (telegramUpdatesReceiver.getLastSeenUpdateId() == 0) {
            ThreadUtils.sleep(10);
        }
        Assert.assertEquals(1L, telegramUpdatesReceiver.getLastSeenUpdateId());
    }

    @Test
    public void notLeaderTest() throws IOException {
        distributedLock.setOwner("not me");
        staffClient.generateUsersByTelegramLogin();
        // don't take updates because I am not a leader
        client.addUpdate(buildStartMessage(1, "alex2", 3, "/start"));
        client.addUpdate(buildStartMessage(4, "alex5", 6, "/start"));
        for (int i = 0; i < 2; i++) {
            client.addCodeResponse(200);
        }
        runFetch(0);
        Assert.assertEquals(0, dao.findAll().join().size());
    }

    @Test
    public void skipWithUpdateIdMoreThanPrevious() throws InterruptedException, IOException {
        staffClient.generateUsersByTelegramLogin();
        client.addUpdate(buildStartMessage(1, "telegram1", 3, "/start"));
        client.addUpdate(buildStartMessage(4, "telegram5", 6, "/start"));
        client.addCodeResponse(200);
        client.addCodeResponse(200);
        runFetch(2);
        dao.sync();
        // updateId less then previous, skip it
        client.clearUpdates();
        client.addUpdate(buildStartMessage(0, "telegram8", 666, "/start"));
        dao.setCountDownLatch(1);

        TelegramRecord record = dao.get(3).join().get();
        Assert.assertEquals(3, record.getChatId());
        Assert.assertEquals("telegram1", record.getName());
        Assert.assertFalse(record.isGroup());

        record = dao.get(6).join().get();
        Assert.assertEquals(6, record.getChatId());
        Assert.assertEquals("telegram5", record.getName());
        Assert.assertFalse(record.isGroup());

        Assert.assertEquals(2, dao.getMap().size());
    }

    @Test
    public void addBotToGroupChat() throws InterruptedException, IOException {
        client.addUpdate(
            buildNewChatMember(1, true, config.getBotName(), 4, "Charm666",
                "supergroup", "chatName123"));

        client.addUpdate(
            buildNewChatMember(5, true, config.getBotName(), 7, "Charm666", "group",
                "chatName456"));

        // was added not me because of title
        client.addUpdate(
            buildNewChatMember(3, true, config.getBotName() + "FAKE", 6, "Charm666", "supergroup",
                "chatName123"));
//         was added not me because he is not bot
        client.addUpdate(
            buildNewChatMember(4, false, config.getBotName(), 4, "Charm666", "supergroup",
                "chatName123"));

        client.addCodeResponse(200);
        client.addCodeResponse(200);

        UserInfo userInfo = new UserInfo(123,
            "alexlovkov",
            "+76663451122",
            false,
            List.of("Charm666", "anotherTelegramAccount"),
            "",
            "");
        staffClient.add("Charm666", userInfo);
        runFetch(2);
        dao.sync();

        TelegramRecord record = dao.get(4).join().get();
        Assert.assertEquals(4, record.getChatId());
        Assert.assertEquals("chatName123", record.getName());
        Assert.assertTrue(record.isGroup());

        record = dao.get(7).join().get();
        Assert.assertEquals(7, record.getChatId());
        Assert.assertEquals("chatName456", record.getName());
        Assert.assertTrue(record.isGroup());

        Assert.assertEquals(2, dao.getMap().size());
    }

    @Test
    public void addBotToGroupChatWhichTitleAlreadyInDB() throws IOException, InterruptedException {
        client.setCountDownLatch(1);
        client.addUpdate(buildNewChatMember(1, true, config.getBotName(), 3, "Charm666", "supergroup", "chatName123"));
        client.addCodeResponse(200);
        UserInfo userInfo = new UserInfo(123,
            "alexlovkov",
            "+76663451122",
            false,
            List.of("Charm666", "anotherTelegramAccount"),
            "",
            "");
        staffClient.add("Charm666", userInfo);
        dao = new InMemoryTelegramDao(1);
        dao.upsert(new TelegramRecord(4, "chatName123", true));
        dao.sync();
        var templateVarsFactory = new TemplateVarsFactory("solomon", "solomon.cloud");
        telegramUpdatesReceiver = new TelegramUpdatesReceiver(client, schedulerExecutorService,
            schedulerExecutorService, config.build(), dao, staffClient, distributedLock, new ChatIdResolverImpl(dao),
            telegramEventsDao, chartsClient, templateVarsFactory, muteApi);
        client.sync();
        Assert.assertTrue(client.receiveMessage().getText().contains("already exist"));
        Assert.assertEquals(1, dao.findAll().join().size());
    }

    @Test
    public void manyChatsWithSameName() throws IOException, InterruptedException {
        staffClient.generateUsersByTelegramLogin();

        client.addUpdate(
            buildNewChatMember(1, true, config.getBotName(), 1, "Charm666" + "0", "supergroup",
                "oldChatTitle"));
        client.addCodeResponse(200);
        runFetch(1);
        dao.sync();

        int n = 1000;

        client.setCountDownLatch(n - 1);
        for (int i = 1; i < n; i++) {
            client.addUpdate(
                buildNewChatMember(i + 1, true, config.getBotName(), i + 1, "Charm666" + i, "supergroup",
                    "oldChatTitle"));
            client.addCodeResponse(200);
        }
        client.sync();

        dao.setCountDownLatch(1);
        client.addUpdate(buildRenameChatUpdate(n + 1, 1, "newChatTitle"));
        client.addCodeResponse(200);
        dao.sync();

        client.setCountDownLatch(n - 1);
        for (int i = 1; i < n; i++) {
            client.addUpdate(buildRenameChatUpdate(i + n + 1, i + 1, "newChatTitle"));
            client.addCodeResponse(200);
        }
        client.sync();
        List<TelegramRecord> records = dao.findAll().join();
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("newChatTitle", records.get(0).getName());
        var groups = chatIdResolver.listGroups();
        Assert.assertEquals(List.of("newChatTitle"), groups);
    }

    @Test
    public void renameGroupToAlreadyExistedTitle() throws IOException, InterruptedException {
        staffClient.generateUsersByTelegramLogin();

        client.setCountDownLatch(2);
        client.addUpdate(
            buildNewChatMember(1, true, config.getBotName(), 1, "Charm666" + "0", "supergroup",
                "title1"));

        client.addUpdate(
            buildNewChatMember(2, true, config.getBotName(), 2, "Charm777" + "0", "supergroup",
                "title2"));
        client.addCodeResponse(200);
        client.addCodeResponse(200);
        runFetch(2);
        dao.sync();
        client.sync();

        client.clearUpdates();
        client.setCountDownLatch(1);
        client.addCodeResponse(200);
        client.addUpdate(buildRenameChatUpdate(3, 1, "title2"));
        client.sync();

        List<TelegramRecord> records = dao.findAll().join();
        Assert.assertEquals(2, records.size());

        // miss 2 "success" messages
        client.receiveMessage();
        client.receiveMessage();
        TelegramClientStub.Message message = client.receiveMessage();
        Assert.assertTrue(message.getText().contains("title2"));
        Assert.assertTrue(message.getText().contains("already exist"));


        TelegramRecord r1 = dao.get(1).join().get();
        Assert.assertEquals("title1", r1.getName());
        TelegramRecord r2 = dao.get(2).join().get();
        Assert.assertEquals("title2", r2.getName());

        var groups = chatIdResolver.listGroups();

        Assert.assertEquals(2, groups.size());
        Assert.assertTrue(groups.containsAll(List.of("title1", "title2")));
    }

    /**
     *  This test checks that chat wasn't in db, but was renamed (it means bot was in chat) then we added it to db
     *  It means, that user added bot to the chat, but it wasn't success (i.e this chat-title not unique)
     *  then he renamed it. So we fix that you don't need remove bot and add it again by this test
     */
    @Test
    public void chatWasRenamedNotIbDB() throws InterruptedException, IOException {
        staffClient.generateUsersByTelegramLogin();
        runFetch(1);
        client.addUpdate(buildRenameChatUpdate(1, 1, "newChatTitle"));
        client.addCodeResponse(200);
        dao.sync();

        List<TelegramRecord> records = dao.findAll().join();
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("newChatTitle", records.get(0).getName());
        var groups = chatIdResolver.listGroups();

        Assert.assertEquals(List.of("newChatTitle"), groups);
    }

    @Test
    public void userLeftChat() throws IOException, InterruptedException {
        client.addUpdate(
            buildNewChatMember(1, true, config.getBotName(), 4, "Charm666", "supergroup",
                "chatName123"));
        client.addCodeResponse(200);
        UserInfo userInfo = new UserInfo(123,
            "alexlovkov",
            "+76663451122",
            false,
            List.of("Charm666", "anotherTelegramAccount"),
            "",
            "");
        staffClient.add("Charm666", userInfo);
        runFetch(1);
        dao.sync();
        Assert.assertEquals(1, dao.getMap().size());

        dao.setCountDownLatch(1);
        client.clearUpdates();
        TelegramUpdate botLeftChat =
            buildBotLeftChatUpdate(true, config.getBotName(), "chatName123", 4);
        client.addUpdate(botLeftChat);
        dao.sync();
        Assert.assertEquals(0, dao.getMap().size());
    }

    @Test
    public void testPrivateMessageUserWithoutStaff() throws IOException, InterruptedException {
        client.addUpdate(
            buildNewChatMember(1, true, config.getBotName(), 4, "Charm666", "supergroup",
                "chatName123"));
        client.addCodeResponse(200);
        client.setCountDownLatch(1);
        runFetch(0);
        client.sync();
        var message = client.receiveMessage();
        Assert.assertTrue(message.getText().contains("wasn't found"));
    }

    @Test
    public void testAddToGroupWithoutStaff() throws IOException, InterruptedException {
        client.clearUpdates();
        client.setCountDownLatch(1);
        client.addUpdate(
            buildNewChatMember(1, true, config.getBotName(), 4, "Charm666", "supergroup",
                "chatName123"));
        client.addCodeResponse(200);
        runFetch(0);
        client.sync();
        var message = client.receiveMessage();
        Assert.assertTrue(message.getText().contains("wasn't found"));
    }

    @Test
    public void testRenameChat() throws IOException, InterruptedException {
        client.addUpdate(
            buildNewChatMember(1, true, config.getBotName(), 4, "Charmik1", "supergroup",
                "chatName123"));
        client.addCodeResponse(200);
        UserInfo userInfo = new UserInfo(123,
            "alexlovkov",
            "+76663451122",
            false,
            List.of("Charmik1", "anotherTelegramAccount"),
            "",
            "");
        staffClient.add("Charmik1", userInfo);
        runFetch(1);
        dao.sync();
        Assert.assertEquals(1, dao.getMap().size());
        TelegramRecord record = dao.get(4).join().get();
        Assert.assertTrue("chatName123", record.isGroup());

        // delete + upsert
        dao.setCountDownLatch(2);
        client.clearUpdates();
        TelegramUpdate botLeftChat = buildRenameChatUpdate(2, 4, "newChatName");
        client.addUpdate(botLeftChat);
        dao.sync();
        record = dao.get(4).join().get();
        Assert.assertEquals("newChatName", record.getName());
        Assert.assertTrue(record.isGroup());
        Assert.assertEquals(1, dao.getMap().size());
    }

    @Test
    public void upgradeToSuperGroup() throws InterruptedException, IOException {
        long oldChatId = 2;
        long newChatId = 3;
        client.addUpdate(buildUpgradeToSuperGroup(1, oldChatId, newChatId));

        UserInfo userInfo = new UserInfo(123,
            "alexlovkov",
            "+76663451122",
            false,
            List.of("Charmik1", "anotherTelegramAccount"),
            "",
            "");
        staffClient.add("Charmik1", userInfo);
        runFetch(1);
        dao.sync();

        TelegramRecord record = dao.get(3).join().get();
        Assert.assertEquals(3, record.getChatId());
        Assert.assertEquals("testGroupTitle", record.getName());
        Assert.assertTrue(record.isGroup());
        Assert.assertEquals(1, dao.getMap().size());
    }

    @Test
    public void successCallbackQueryTest() throws IOException, InterruptedException {
        client.addCodeResponse(200);
        staffClient.generateUsersByTelegramLogin();
        long chatId = 123;
        String uuid = UUID.randomUUID().toString();

        byte[] bytes = uuid.toString().getBytes();
        byte[] newBytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, newBytes, 1, bytes.length);
        newBytes[0] = 0;
        byte[] decodedBytes = Base64.getEncoder().encode(newBytes);
        String eventId = new String(decodedBytes);

        client.addUpdate(buildCallbackQueryUpdate(chatId, eventId));
        telegramEventsDao.insert(new TelegramEventRecord(uuid, 5, 5, "projectId1",
                        "alertId2", "subAlertId3", 12345, EvaluationStatus.Code.ALARM,
                        "",
                        new TelegramEventContext(),
                        EventAppearance.WITH_PHOTO)
                ).join();
        client.setCountDownLatch(1);
        runFetch(0);
        client.sync();
        TelegramClientStub.Message message = client.receiveMessage();
        Assert.assertTrue(message.isPhoto());
        Assert.assertEquals(chatId, message.getChatId());
    }

    @Test
    public void userChangedLogin() throws InterruptedException, IOException {
        staffClient.generateUsersByTelegramLogin();
        client.setCountDownLatch(2);
        client.addUpdate(buildStartMessage(1, "alex2", 3, "/start"));
        client.addUpdate(buildStartMessage(4, "alex5", 3, "/start"));
        client.addCodeResponse(200);
        client.addCodeResponse(200);
        runFetch(0);
        client.sync();

        System.out.println(client);
    }

    @Test
    public void nextTime() {
        {
            Instant now = Instant.parse("2021-10-05T14:05:20.123Z");
            Assert.assertEquals(Instant.parse("2021-10-05T15:05:20.123Z"), TelegramUpdatesReceiver.next(now, ButtonType.FOR_HOUR));
            Assert.assertEquals(Instant.parse("2021-10-06T07:00:00Z"), TelegramUpdatesReceiver.next(now, ButtonType.TILL_MORNING));
            Assert.assertEquals(Instant.parse("2021-10-11T07:00:00Z"), TelegramUpdatesReceiver.next(now, ButtonType.TILL_MONDAY));
        }

        {
            Instant now = Instant.parse("2021-10-05T01:00:00Z");
            Assert.assertEquals(Instant.parse("2021-10-05T02:00:00Z"), TelegramUpdatesReceiver.next(now, ButtonType.FOR_HOUR));
            Assert.assertEquals(Instant.parse("2021-10-05T07:00:00Z"), TelegramUpdatesReceiver.next(now, ButtonType.TILL_MORNING));
            Assert.assertEquals(Instant.parse("2021-10-11T07:00:00Z"), TelegramUpdatesReceiver.next(now, ButtonType.TILL_MONDAY));
        }

        {
            Instant now = Instant.parse("2021-10-18T01:00:00Z");
            Assert.assertEquals(Instant.parse("2021-10-18T07:00:00Z"), TelegramUpdatesReceiver.next(now, ButtonType.TILL_MONDAY));
        }

        {
            Instant now = Instant.parse("2021-10-18T08:00:00Z");
            Assert.assertEquals(Instant.parse("2021-10-25T07:00:00Z"), TelegramUpdatesReceiver.next(now, ButtonType.TILL_MONDAY));
        }
    }

    private TelegramUpdate buildStartMessage(
        long updateId,
        String telegramLogin,
        long chatId,
        String message) throws IOException
    {
        String startMessage = getFileContent("jsons/startMessage.json");
        String result = startMessage
            .replace("%updateId", StringEscapeUtils.escapeJson(String.valueOf(updateId)))
            .replace("%userName", StringEscapeUtils.escapeJson(telegramLogin))
            .replace("%chatId", StringEscapeUtils.escapeJson(String.valueOf(chatId)))
            .replace("%messageText", StringEscapeUtils.escapeJson(message));
        return getTelegramUpdate(result);
    }

    private TelegramUpdate buildNewChatMember(
        long updateId,
        boolean isBot,
        String telegramLogin,
        long chatId,
        String fromUser,
        String groupType,
        String chatTitle) throws IOException
    {
        String addBotToGroupChat = getFileContent("jsons/addBotToGroupChat.json");
        String result =
            addBotToGroupChat
                .replace("%updateId", StringEscapeUtils.escapeJson(String.valueOf(updateId)))
                .replace("%chatId", StringEscapeUtils.escapeJson(String.valueOf(chatId)))
                .replace("%isBot", StringEscapeUtils.escapeJson(String.valueOf(isBot)))
                .replace("%userName", StringEscapeUtils.escapeJson(telegramLogin))
                .replace("%fromUser", StringEscapeUtils.escapeJson(fromUser))
                .replace("%groupType", StringEscapeUtils.escapeJson(groupType))
                .replace("%chatTitle", StringEscapeUtils.escapeJson(chatTitle));
        return getTelegramUpdate(result);
    }

    private TelegramUpdate buildUpgradeToSuperGroup(
        long updateId,
        long oldChatId,
        long newChatId) throws IOException
    {
        String event = getFileContent("jsons/upgradeToSuperGroup.json");
        String result = event
            .replace("%updateId", StringEscapeUtils.escapeJson(String.valueOf(updateId)))
            .replace("%oldChatId", StringEscapeUtils.escapeJson(String.valueOf(oldChatId)))
            .replace("%newChatId", StringEscapeUtils.escapeJson(String.valueOf(newChatId)));
        TelegramUpdate update = getTelegramUpdate(result);
        return update;
    }

    private TelegramUpdate buildBotLeftChatUpdate(
        boolean isBot,
        String whoLeftChat,
        String chatName,
        long chatId) throws IOException
    {
        String addBotToGroupChat = getFileContent("jsons/botLeftChat.json");
        String result =
            addBotToGroupChat
                .replace("%isBot", StringEscapeUtils.escapeJson(String.valueOf(isBot)))
                .replace("%userName", StringEscapeUtils.escapeJson(whoLeftChat))
                .replace("%chatName", StringEscapeUtils.escapeJson(chatName))
                .replace("%chatId", StringEscapeUtils.escapeJson(String.valueOf(chatId)));
        return getTelegramUpdate(result);
    }

    private TelegramUpdate buildRenameChatUpdate(
        long updateId,
        long chatId,
        String newTitle) throws IOException
    {
        String addBotToGroupChat = getFileContent("jsons/renameChat.json");
        String result =
            addBotToGroupChat
                .replace("%updateId", StringEscapeUtils.escapeJson(String.valueOf(updateId)))
                .replace("%chatId", StringEscapeUtils.escapeJson(String.valueOf(chatId)))
                .replace("%newTitle", StringEscapeUtils.escapeJson(newTitle));
        return getTelegramUpdate(result);
    }

    private TelegramUpdate buildCallbackQueryUpdate(
        long chatId,
        String data) throws IOException
    {
        String addBotToGroupChat = getFileContent("jsons/callbackQuery.json");
        String result =
            addBotToGroupChat
                .replace("%chatId", StringEscapeUtils.escapeJson(String.valueOf(chatId)))
                .replace("%data", StringEscapeUtils.escapeJson(data));
        return getTelegramUpdate(result);
    }

    private String getFileContent(String fileName) throws IOException {
        URL resource = this.getClass().getResource(fileName);
        return Resources.toString(resource, StandardCharsets.UTF_8);
    }

    private TelegramUpdate getTelegramUpdate(String result) {
        return DefaultTelegramClient.parseUpdatesResponse(result).get(0);
    }
}
