package ru.yandex.webmaster3.storage.util.ydb.querybuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.storage.util.ydb.query.Assignment;

/**
 * ishalaru
 * 20.07.2020
 **/
public class UpdateTest {
    @Test
    public void simpleUpdateWithOneParameter() {
        String value = "PRAGMA TablePathPrefix = 'testPrefix';\n" +
                "DECLARE $parameter_0 as `Int64`;\n" +
                "DECLARE $parameter_1 as `Timestamp`;\n" +
                "DECLARE $parameter_2 as Utf8;\n" +
                "UPDATE testTable\n" +
                "SET `col1` = $parameter_0,\n" +
                "`col2` = $parameter_1\n" +
                "WHERE \n" +
                "`id_col` = $parameter_2\n;";
        DateTime dt = new DateTime(2020, 03, 6, 12, 00);
        final Update statement = new Update("testPrefix", "testTable", null)
                .with(new Assignment("col1", 100L))
                .and(new Assignment("col2", dt))
                .where(QueryBuilder.eq("id_col", "main_field")).getStatement();
        final String s = statement.toQueryString();
        Assert.assertEquals("Compare simple update", value, s);
        Assert.assertEquals(3, statement.getParameters().size());
        Assert.assertEquals(100L, statement.getParameters().get("$parameter_0").asData().getInt64());
        LocalDateTime date = LocalDateTime.of(2020, 03, 6, 12, 00);
        final LocalDateTime parameter = statement.getParameters().get("$parameter_1").asData().getTimestamp().atZone(ZoneId.systemDefault()).toLocalDateTime();
        Assert.assertEquals(date, parameter);
        Assert.assertEquals("main_field", statement.getParameters().get("$parameter_2").asData().getUtf8());
    }

}
