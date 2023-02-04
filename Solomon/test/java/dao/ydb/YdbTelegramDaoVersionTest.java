package ru.yandex.solomon.alert.dao.ydb;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.solomon.alert.dao.TelegramDao;
import ru.yandex.solomon.alert.dao.TelegramRecord;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;


/**
 * @author Vladimir Gordiychuk
 **/
@YaIgnore
public class YdbTelegramDaoVersionTest {

    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private TelegramDao min;
    private TelegramDao max;

    @Before
    public void setUp() throws Exception {
        assumeThat(YdbSchemaVersion.MIN, not(equalTo(YdbSchemaVersion.MAX)));

        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = "/local/Solomon/" + testName.getMethodName();
        min = YdbTelegramDaoFactory.create(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MIN);
        max = YdbTelegramDaoFactory.create(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MAX);

        min.createSchemaForTests().join();
        max.createSchemaForTests().join();
    }

    @Test
    public void dataMigratedAfterSchemaInit() {
        List<TelegramRecord> source = IntStream.range(0, 1000)
            .mapToObj(index -> randomRecord())
            .sorted(Comparator.comparing(TelegramRecord::getChatId))
            .collect(Collectors.toList());

        inserts(source, min);
        max.migrate().join();

        long[] result = max.findAll().join()
            .stream()
            .mapToLong(TelegramRecord::getChatId)
            .sorted()
            .toArray();

        long[] expected = source.stream()
            .mapToLong(TelegramRecord::getChatId)
            .sorted()
            .toArray();

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void changesForNewSchemaAlwaysDuplicatedToMin() {
        var v1 = randomRecord();

        max.upsert(v1).join();
        assertEquals(v1, fetchOne(min));
        assertEquals(v1, fetchOne(max));

        var v2 = TelegramRecord.createForChat(v1.getChatId(), "new super name");

        max.upsert(v2).join();
        assertEquals(v2, fetchOne(min));
        assertEquals(v2, fetchOne(max));

        max.deleteById(v2.getChatId()).join();
        assertEquals(List.of(), min.findAll().join());
        assertEquals(List.of(), max.findAll().join());
    }

    @Test
    public void repeatMigrateIfRequiredUpdates() {
        var v1 = randomRecord();

        max.upsert(v1).join();
        assertEquals(v1, fetchOne(min));
        assertEquals(v1, fetchOne(max));

        var v2 = TelegramRecord.createForChat(v1.getChatId(), "new super name");

        min.upsert(v2).join();
        assertEquals(v2, fetchOne(min));

        var v3 = TelegramRecord.createForChat(v1.getChatId(), "new super name 2");

        min.upsert(v3).join();
        assertEquals(v3, fetchOne(min));

        max.migrate().join();
        assertEquals(v3, fetchOne(min));
        assertEquals(v3, fetchOne(max));
    }

    @Test
    public void repeatMigrateIfRequiredDeletes() {
        var one = randomRecord();
        var two = randomRecord();
        var tree = randomRecord();

        max.upsert(one).join();
        max.upsert(two).join();
        max.upsert(tree).join();

        min.deleteById(one.getChatId()).join();
        min.deleteById(two.getChatId()).join();
        min.deleteById(tree.getChatId()).join();

        max.migrate().join();
        assertEquals(List.of(), min.findAll().join());
        assertEquals(List.of(), max.findAll().join());
    }

    @Test
    public void repeatMigrateIfRequiredInserts() {
        var record = randomRecord();
        min.upsert(record).join();

        max.migrate().join();
        assertEquals(record, fetchOne(min));
        assertEquals(record, fetchOne(max));
    }

    private TelegramRecord randomRecord() {
        if (ThreadLocalRandom.current().nextBoolean()) {
            return TelegramRecord.createForUser(ThreadLocalRandom.current().nextLong(), UUID.randomUUID().toString());
        } else {
            return TelegramRecord.createForChat(ThreadLocalRandom.current().nextLong(), UUID.randomUUID().toString());
        }
    }

    private TelegramRecord fetchOne(TelegramDao dao) {
        return Iterables.getOnlyElement(dao.findAll().join());
    }

    private void inserts(List<TelegramRecord> records, TelegramDao dao) {
        for (var record : records) {
            dao.upsert(record).join();
        }
    }
}
