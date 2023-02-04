package ru.yandex.webmaster3.storage.util.ydb.querybuilder;

import java.util.List;

import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.storage.util.ydb.ExecuteQuery;
import ru.yandex.webmaster3.storage.util.ydb.querybuilder.typesafe.Field;
import ru.yandex.webmaster3.storage.util.ydb.querybuilder.typesafe.Fields;
import ru.yandex.webmaster3.storage.util.ydb.querybuilder.typesafe.ValueDataMapper;

/**
 * ishalaru
 * 10.08.2020
 **/
public class BatchInsertTest {


    @Test
    public void test() {
        ExecuteQuery executeQuery = null;
        String control = "PRAGMA TablePathPrefix = 'test_prefix';\n" +
                "DECLARE $data as \"List<Struct<\n" +
                "t_a: Int32,t_b: Utf8>>\";\n" +
                "REPLACE INTO test_table\n" +
                "SELECT\n" +
                "t_a,t_b\n" +
                "FROM AS_TABLE($data);\n";
        final BatchInsert batchInsert = new BatchInsert("test_prefix", "test_table", MAPPER, List.of(new TestItem(1, "123")), executeQuery);
        String value = batchInsert.toQueryString();
        Assert.assertEquals("Compare values", control, value);
    }

    static final ValueDataMapper<TestItem> MAPPER = ValueDataMapper.create(
            Pair.of(F.A, e -> F.A.get(e.getA())),
            Pair.of(F.B, e -> F.B.get(e.getB()))
    );

    interface F {
        Field<Integer> A = Fields.intField("t_a");
        Field<String> B = Fields.stringField("t_b");
    }

    @Value
    public class TestItem {
        int a;
        String b;
    }
}