package ru.yandex.qe.dispenser.domain.util.clickhouse;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ClickHouseClientTest {

    private ClickHouseClient ch;

    @BeforeEach
    public void setUp() throws Exception {
        ch = new ClickHouseClient.Builder()
                .setHost("dispenser-sas.haze.yandex.net")
                .setPort(8123)
                .setDatabase("test")
                .build();
        Assumptions.assumeTrue(ch.ping(), "Cannot find testing ClickHouse service!");
    }

    @Test
    public void ping() throws Exception {
        assertTrue(ch.ping());
        final ClickHouseClient ch2 = new ClickHouseClient.Builder()
                .setHost("xxx.haze.yandex.net")
                .setPort(8123)
                .setDatabase("test")
                .build();
        assertFalse(ch2.ping());
    }

    @Test
    public void createDatabase() throws Exception {
        ch.createDatabase(false);
        try {
            ch.createDatabase(true);
            fail();
        } catch (final ClickHouseClientException ignored) {
        }
    }

    @Test
    public void tableExists() throws Exception {
        assertFalse(ch.tableExists("a" + UUID.randomUUID().toString().replaceAll("-", "")));
    }

    @Test
    public void createTable() throws Exception {
        final Map<String, ClickHouseDataType> fields = new HashMap<>();
        fields.put("field1", ClickHouseDataTypeFactory.DATE.create());
        fields.put("field2", ClickHouseDataTypeFactory.FIXED_STRING.create(16));
        fields.put("field3", ClickHouseDataTypeFactory.U16.create());
        final String tableName = "test";
        if (ch.tableExists(tableName)) {
            ch.dropTable(tableName, true);
            assertFalse(ch.tableExists(tableName));
        }
        final ClickHouseEngine engine = ClickHouseEngineFactory.MERGE_TREE.create(
                "field1",
                ClickHouseEngineFactory.paramsTuple("field2", "field3"),
                8192
        );
        ch.createTable(tableName, engine, true, fields);
        assertTrue(ch.tableExists(tableName));
    }

    @Test
    public void insert() throws Exception {
        final Map<String, ClickHouseDataType> fields = new HashMap<>();
        fields.put("field1", ClickHouseDataTypeFactory.STRING.create());
        fields.put("field2", ClickHouseDataTypeFactory.I32.create());
        fields.put("field3", ClickHouseDataTypeFactory.F64.create());
        final String tableName = "itest";
        if (ch.tableExists(tableName)) {
            ch.dropTable(tableName, true);
            assertFalse(ch.tableExists(tableName));
        }
        ch.createTable(tableName, ClickHouseEngineFactory.MEMORY.create(), true, fields);
        assertTrue(ch.tableExists(tableName));
        ch.insert(tableName, new TestDataProvider(), "field1", "field2", "field3");
    }

    private static final class C {

        public final String a;
        public final int b;
        public final double c;


        C(final String a, final int b, final double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private static final class TestDataProvider extends InsertDataProvider {

        private final Iterator<C> iterator;

        TestDataProvider() {
            final List<C> result = new ArrayList<>();
            result.add(new C("s1", 1, 1.0));
            result.add(new C("s2", 2, 2.0));
            result.add(new C("s3", 3, 3.0));
            iterator = result.iterator();
        }

        @Override
        protected byte[] next() {
            final C c = iterator.next();
            return fieldsToBytes(c.a, String.valueOf(c.b), String.valueOf(c.c));
        }

        @Override
        protected boolean hasNext() {
            return iterator.hasNext();
        }
    }

}
