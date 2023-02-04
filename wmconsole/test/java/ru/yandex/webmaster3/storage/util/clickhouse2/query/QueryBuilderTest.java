package ru.yandex.webmaster3.storage.util.clickhouse2.query;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;

/**
 * @author aherman
 */
public class QueryBuilderTest {
    @Test
    public void testInsert() throws Exception {
        UUID uuid = UUID.fromString("aa4e9030-2129-11e6-98dc-efa443e9e064");
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        String actual = QueryBuilder.insertInto("db1", "table1")
                .value("int", 1)
                .value("long", 1L)
                .value("uuid", uuid)
                .value("text", "test1")
                .value("textComplex", "test \t\n\0\r 'test1' test")
                .value("date", new LocalDate("2016-05-10"))
                .value("dateTime", new DateTime("2016-05-10T10:20:30+03:00", DateTimeZone.forOffsetHours(3)))
                .value("hostId", hostId)
                .toString();

        String expected = "INSERT INTO db1.table1(int, long, uuid, text, textComplex, date, dateTime, hostId) "
                + "VALUES(1, 1, 'aa4e9030-2129-11e6-98dc-efa443e9e064', 'test1', 'test \\t\\n\\0\\r \\'test1\\' test', '2016-05-10', '2016-05-10 10:20:30', 'http:example.com:80')";
        Assert.assertEquals(expected, actual);
    }
}
