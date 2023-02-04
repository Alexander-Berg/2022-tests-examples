package ru.yandex.webmaster3.storage.util.ydb.querybuilder;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.storage.util.ydb.query.DbFieldInsertAssignment;

/**
 * ishalaru
 * 20.07.2020
 **/
public class UpsertTest {

    @Test
    public void simpleTest() {
        String value = "PRAGMA TablePathPrefix = 'tablePrefix';\n" +
                "DECLARE $parameter_0 as `Utf8`;\n" +
                "DECLARE $parameter_1 as `Int64`;\n" +
                "DECLARE $parameter_2 as `Int32`;\n" +
                "UPSERT INTO tableName(`col_1`,`col_2`,`col_3`)\n" +
                "VALUES($parameter_0,$parameter_1,$parameter_2);";
        Upsert upsert = new Upsert("tablePrefix",
                "tableName",
                List.of(new DbFieldInsertAssignment("col_1", "url"),
                        new DbFieldInsertAssignment("col_2", 1223L),
                        new DbFieldInsertAssignment("col_3", 123)), null);
        Assert.assertEquals(value, upsert.toQueryString());
        Assert.assertEquals(3, upsert.getParameters().size());
        Assert.assertEquals("url", upsert.getParameters().get("$parameter_0").asData().getUtf8());
        Assert.assertEquals(1223L, upsert.getParameters().get("$parameter_1").asData().getInt64());
        Assert.assertEquals(123, upsert.getParameters().get("$parameter_2").asData().getInt32());
    }
}
