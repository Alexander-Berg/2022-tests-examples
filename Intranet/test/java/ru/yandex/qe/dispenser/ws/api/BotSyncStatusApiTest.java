package ru.yandex.qe.dispenser.ws.api;

import java.time.Instant;

import javax.ws.rs.HttpMethod;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiBotSyncStatus;
import ru.yandex.qe.dispenser.domain.bot.BotSyncStatus;
import ru.yandex.qe.dispenser.domain.dao.bot.monitoring.BotSyncStatusDao;


public final class BotSyncStatusApiTest extends ApiTestBase {

    private final static Instant INSTANT_1 = Instant.ofEpochMilli(92554380000L);
    private final static Instant INSTANT_2 = Instant.ofEpochMilli(92555380000L);

    @Autowired
    private BotSyncStatusDao botSyncStatusDao;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        botSyncStatusDao.clean();
    }

    @Test
    public void getEmptySyncStatusTest() {
        final DiBotSyncStatus diBotSyncStatus = dispenser().botSyncStatus().get().perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/bot-sync-status");
        assertLastResponseEquals("/body/bot/status/empty.json");
        assertJsonEquals("/body/bot/status/empty.json", diBotSyncStatus);
    }

    @Test
    public void getSyncStatusTest() {
        botSyncStatusDao.upsert(BotSyncStatus.builder()
                .status(DiBotSyncStatus.Status.FAIL)
                .date(INSTANT_1)
                .errors(ImmutableList.of("Error 1", "Error 2"))
                .build());

        DiBotSyncStatus diBotSyncStatus = dispenser().botSyncStatus().get().perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/bot-sync-status");
        assertLastResponseEquals("/body/bot/status/fail.json");
        assertJsonEquals("/body/bot/status/fail.json", diBotSyncStatus);

        botSyncStatusDao.upsert(BotSyncStatus.builder()
                .status(DiBotSyncStatus.Status.SUCCESS)
                .date(INSTANT_2)
                .build());

        diBotSyncStatus = dispenser().botSyncStatus().get().perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/bot-sync-status");
        assertLastResponseEquals("/body/bot/status/success.json");
        assertJsonEquals("/body/bot/status/success.json", diBotSyncStatus);
    }
}
