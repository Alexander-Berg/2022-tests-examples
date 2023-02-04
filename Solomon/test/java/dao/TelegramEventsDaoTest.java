package ru.yandex.solomon.alert.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.misc.random.Random2;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.notification.channel.telegram.EventAppearance;

import static junit.framework.TestCase.assertTrue;

/**
 * @author alexlovkov
 */
public abstract class TelegramEventsDaoTest {
    private static final Random2 random = new Random2();

    protected abstract TelegramEventsDao getDao();

    @Test
    public void insertAndFind() {
        var dao = getDao();
        dao.createSchemaForTests().join();

        List<TelegramEventRecord> records = IntStream.range(0, 10)
            .mapToObj(i -> randomTelegramEvent(random.nextInt(), random.nextString(i)))
            .collect(Collectors.toList());

        CompletableFutures.allOf(
            records.stream().map(dao::insert).collect(Collectors.toList()))
            .join();

        ListF<Optional<TelegramEventRecord>> getRecords = CompletableFutures.allOf(
            records.stream()
                .map(record -> dao.find(record.getId()))
                .collect(Collectors.toList())).join();

        for (var currentRecord : getRecords) {
            assertTrue(currentRecord.isPresent());
            assertTrue(records.contains(currentRecord.get()));
        }
    }

    @Test
    public void updateContext() {
        var dao = getDao();
        dao.createSchemaForTests().join();

        var record = randomTelegramEvent(random.nextInt(), random.nextString(10));
        dao.insert(record).join();

        record.getContext().labelsSelectors = "{foo=bar}";

        dao.updateContext(record).join();

        var read = dao.find(record.getId()).join().get();
        Assert.assertEquals("{foo=bar}", read.getContext().labelsSelectors);
        Assert.assertEquals(record.getContext().muteToMillis, read.getContext().muteToMillis);
    }

    @Test
    public void deleteOlderThan() {
        var dao = getDao();
        dao.createSchemaForTests().join();

        List<TelegramEventRecord> records = new ArrayList<>();
        int n = 100;
        for (int i = 0; i < n; i++) {
            TelegramEventRecord record =
                randomTelegramEvent(i, random.nextString(i));
            records.add(record);
        }
        CompletableFutures.allOf(
            records.stream().map(dao::insert).collect(Collectors.toList()))
            .join();

        for (int i = 0; i < n; i++) {
            Assert.assertEquals(records.get(i), dao.find(records.get(i).getId()).join().get());
        }
        dao.deleteOlderThan(Instant.ofEpochMilli(50)).join();

        for (int i = 0; i < 50; i++) {
            Assert.assertEquals(Optional.empty(), dao.find(records.get(i).getId()).join());
        }
        for (int i = 50; i < n; i++) {
            Assert.assertEquals(records.get(i), dao.find(records.get(i).getId()).join().get());
        }
    }

    private TelegramEventRecord randomTelegramEvent(int createdAt, String s) {
        return new TelegramEventRecord(UUID.randomUUID().toString(), createdAt, random.nextLong(), s,
                s, s, random.nextLong(), EvaluationStatus.Code.ALARM,
                "",
                randomContext(),
                EventAppearance.WITH_PHOTO);
    }

    private TelegramEventContext randomContext() {
        var ctx = new TelegramEventContext();
        if (random.nextBoolean()) {
            ctx.muteToMillis = 1_000_000_000L + random.nextLong(2_000_000_000L);
        }
        if (random.nextBoolean()) {
            if (random.nextBoolean()) {
                ctx.labelsSelectors = "{host='*sas*'}";
            } else {
                ctx.labelsSelectors = "{service='coremon'}";
            }
        }
        return ctx;
    }

    protected TelegramEventRecord randomRecord() {
        var uuid = UUID.randomUUID().toString();
        var createdAt = random.nextLong();
        var evaluatedAt = random.nextLong();
        var messageId = random.nextLong();
        var projectId = UUID.randomUUID().toString();

        var alertId = "";
        var subAlertId = "";
        var status = EvaluationStatus.Code.NO_DATA;
        var muteId = "";

        if (random.nextBoolean()) {
            alertId = UUID.randomUUID().toString();
            subAlertId = random.nextBoolean() ? "" : UUID.randomUUID().toString();
            status = switch (random.nextInt(5)) {
                case 0 -> EvaluationStatus.Code.NO_DATA;
                case 1 -> EvaluationStatus.Code.OK;
                case 2 -> EvaluationStatus.Code.WARN;
                case 3 -> EvaluationStatus.Code.ALARM;
                case 4 -> EvaluationStatus.Code.ERROR;
                default -> throw new IllegalArgumentException("unreachable");
            };
        } else {
            muteId = UUID.randomUUID().toString();
        }
        var appearance = EventAppearance.forNumber(random.nextInt(EventAppearance.values().length));
        return new TelegramEventRecord(uuid, createdAt, messageId, projectId,
                alertId, subAlertId, evaluatedAt, status,
                muteId,
                new TelegramEventContext(),
                appearance);
    }

}
