package ru.yandex.solomon.alert.dao;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.misc.random.Random2;

/**
 * @author alexlovkov
 */
public abstract class TelegramDaoTest {
    private static Random2 random = new Random2();

    protected abstract TelegramDao getDao();

    @Test
    public void testInsertAndGet() {
        getDao().createSchemaForTests().join();
        List<TelegramRecord> records = IntStream.range(0, 10)
            .mapToObj(i -> new TelegramRecord(random.nextInt(), random.nextString(i), random.nextBoolean()))
            .collect(Collectors.toList());

        CompletableFutures.allOf(
            records.stream().map(record -> getDao().upsert(record)).collect(Collectors.toList()))
            .join();

        ListF<Optional<TelegramRecord>> getRecords = CompletableFutures.allOf(
            records.stream()
                .map(record -> getDao().get(record.getChatId()))
                .collect(Collectors.toList())).join();

        for (var currentRecord : getRecords) {
            TelegramRecord recordFromDao = currentRecord.get();
            Assert.assertTrue(records.contains(recordFromDao));
        }
    }

    @Test
    public void testUpdateOne() {
        getDao().createSchemaForTests().join();
        List<TelegramRecord> records = IntStream.range(0, 10)
            .mapToObj(i -> new TelegramRecord(random.nextInt(), random.nextString(i), random.nextBoolean()))
            .collect(Collectors.toList());

        for (TelegramRecord record : records) {
            getDao().upsert(record).join();
        }

        TelegramRecord old = records.get(5);
        TelegramRecord updatedRecord = new TelegramRecord(old.getChatId(), old.getName() + "@WWE", !old.isGroup());

        getDao().upsert(updatedRecord).join();
        TelegramRecord fromDao = getDao().get(old.getChatId()).join().get();
        Assert.assertEquals(old.getChatId(), fromDao.getChatId());
        Assert.assertEquals(updatedRecord, fromDao);
    }

    @Test
    public void testDeleteOne() {
        getDao().createSchemaForTests().join();
        int n = 10;
        List<TelegramRecord> records = IntStream.range(0, n)
            .mapToObj(i -> new TelegramRecord(random.nextInt(), random.nextString(i), random.nextBoolean()))
            .collect(Collectors.toList());

        for (TelegramRecord record : records) {
            getDao().upsert(record).join();
        }
        TelegramRecord old = records.get(n - 1);
        getDao().deleteById(old.getChatId()).join();

        Assert.assertEquals(Optional.empty(), getDao().get(old.getChatId()).join());

        for (int i = 0; i < records.size() - 1; i++) {
            Assert.assertEquals(records.get(i), getDao().get(records.get(i).getChatId()).join().get());
        }
        Assert.assertEquals(n - 1, getDao().findAll().join().size());
    }

    protected TelegramRecord randomRecord() {
        return new TelegramRecord(random.nextInt(), random.nextString(random.nextInt(9) + 1), random.nextBoolean());
    }
}
