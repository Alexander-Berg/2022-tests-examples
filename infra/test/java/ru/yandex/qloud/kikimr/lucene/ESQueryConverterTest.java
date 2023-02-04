package ru.yandex.qloud.kikimr.lucene;

import org.junit.Test;
import ru.yandex.qloud.kikimr.search.KikimrYqlQueryBuilder;
import ru.yandex.qloud.kikimr.search.QueryWhereCondition;

import static org.junit.Assert.assertEquals;

/**
 * @author violin
 */
public class ESQueryConverterTest {
    private static ESQueryConverter esQueryConverter = new ESQueryConverter(SchemaProvider.getDefaultSchema());

    @Test
    public void testEmptyQuery() {
        assertEquals("TRUE", where(""));
    }

    @Test
    public void testBoolAndQuery() {
        assertEquals("(TRUE AND ((level = 30) AND (level = 10)))", where("level:10 AND level:30"));
    }

    @Test
    public void testBoolOrQuery() {
        assertEquals("(TRUE AND ((level = 30) OR (level = 10)))", where("level:10 OR level:30"));
    }

    @Test
    public void testBoolComplexQuery() {
        assertEquals("(TRUE AND ((TRUE AND ((level = 30) OR (level = 10))) AND (message ILIKE '%test%')))", where("message:test AND (level:10 OR level:30)"));
    }

    @Test(expected = ESQueryParserException.class)
    public void testInvalidQuery() {
        where("SELECT * FROM test LIMIT 50;");
    }

    @Test
    public void testRangeQuery() {
        assertEquals("(TRUE AND (level <= 1000))", where("level:[* TO 1000]"));
        assertEquals("(TRUE AND (level >= 100) AND (level <= 1000))", where("level:[100 TO 1000]"));
        assertEquals("(TRUE AND (level >= 100))", where("level:[100 TO *]"));

        assertEquals("(TRUE AND (level < 1000))", where("level:{* TO 1000}"));
        assertEquals("(TRUE AND (level > 100) AND (level < 1000))", where("level:{100 TO 1000}"));
        assertEquals("(TRUE AND (level > 100))", where("level:{100 TO *}"));

        assertEquals("(TRUE AND (level >= 100) AND (level < 1000))", where("level:[100 TO 1000}"));
        assertEquals("(TRUE AND (level > 100) AND (level <= 1000))", where("level:{100 TO 1000]"));
    }

    @Test
    public void testRangeQueryFromJsonFiled() {
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} <= '600'))", where("@fields.status:[* TO 600]"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} <= '600'))", where("@fields.status:[* TO \"600\"]"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} >= '500') AND (Json::GetField(fields, 'status'){0} <= '600'))", where("@fields.status:[500 TO 600]"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} >= '500') AND (Json::GetField(fields, 'status'){0} <= '600'))", where("@fields.status:[\"500\" TO \"600\"]"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} >= '500'))", where("@fields.status:[500 TO *]"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} >= '500'))", where("@fields.status:[\"500\" TO *]"));

        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} < '600'))", where("@fields.status:{* TO 600}"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} < '600'))", where("@fields.status:{* TO \"600\"}"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} > '500') AND (Json::GetField(fields, 'status'){0} < '600'))", where("@fields.status:{500 TO 600}"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} > '500') AND (Json::GetField(fields, 'status'){0} < '600'))", where("@fields.status:{\"500\" TO \"600\"}"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} > '500'))", where("@fields.status:{500 TO *}"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} > '500'))", where("@fields.status:{\"500\" TO *}"));

        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} > '500') AND (Json::GetField(fields, 'status'){0} <= '600'))", where("@fields.status:{500 TO 600]"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} > '500') AND (Json::GetField(fields, 'status'){0} <= '600'))", where("@fields.status:{\"500\" TO \"600\"]"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} >= '500') AND (Json::GetField(fields, 'status'){0} < '600'))", where("@fields.status:[500 TO 600}"));
        assertEquals("(TRUE AND (Json::GetField(fields, 'status'){0} >= '500') AND (Json::GetField(fields, 'status'){0} < '600'))", where("@fields.status:[\"500\" TO \"600\"}"));
    }

    @Test
    public void testRegexpQuery() {
        assertEquals("(message REGEXP 'ttt')", where("message:/ttt/"));
    }

    @Test
    public void testQuoteInRegexpEscaped() {
        assertEquals("(message REGEXP 'o\\'o')", where("message:/o'o/"));
    }

    @Test
    public void testQuoteInLikeEscaped() {
        assertEquals("(message ILIKE '%o\\'o%')", where("message: o'o"));
    }

    @Test
    public void testStackTraceSearchedBySubstring() {
        assertEquals("(stackTrace ILIKE '%test%')", where("stackTrace: test"));
    }

    @Test
    public void testLevel() {
        assertEquals("(level = 40000)", where("level:(40000)"));
    }

    @Test
    public void testNoErrorOnNullPagingParameters() {
        assertEquals(
                "PRAGMA kikimr.IsolationLevel='ReadUncommitted'; "
                        + "SELECT * FROM [Root/qloud/test] WHERE (message ILIKE '%test%') ORDER BY timestamp ASC, "
                        + "qloud_instance ASC, pushclient_row_id ASC LIMIT 50;",
                q("Root/qloud/test", "test", null)
        );
    }

    @Test
    public void testAscSortForPrevRecords() {
        PagingParameters pagingParameters = new PagingParameters(-123, "back-1", -1, PagingParameters.PagingDirection.PREV);
        assertEquals(
                "PRAGMA kikimr.IsolationLevel='ReadUncommitted'; "
                        + "SELECT * FROM [Root/qloud/test] WHERE (message ILIKE '%test%') AND "
                        + "(timestamp  > -123 "
                        + "OR timestamp = -123 AND qloud_instance  > 'back-1' "
                        + "OR timestamp = -123 AND qloud_instance = 'back-1' AND pushclient_row_id  > -1"
                        + ") ORDER BY timestamp ASC, qloud_instance ASC, pushclient_row_id ASC LIMIT 50;",
                q("Root/qloud/test", "test", pagingParameters)
        );
    }

    @Test
    public void testDescSortForNextRecords() {
        PagingParameters pagingParameters = new PagingParameters(-123, "back-1", -1, PagingParameters.PagingDirection.NEXT);
        assertEquals(
                "PRAGMA kikimr.IsolationLevel='ReadUncommitted'; "
                        + "SELECT * FROM [Root/qloud/test] WHERE (message ILIKE '%test%') AND "
                        + "(timestamp  < -123 "
                        + "OR timestamp = -123 AND qloud_instance  < 'back-1' "
                        + "OR timestamp = -123 AND qloud_instance = 'back-1' AND pushclient_row_id  < -1"
                        + ") ORDER BY timestamp DESC, qloud_instance DESC, pushclient_row_id DESC LIMIT 50;",
                q("Root/qloud/test", "test", pagingParameters)
        );
    }

    @Test
    public void testYqlQuery() {
        PagingParameters pagingParameters = new PagingParameters(-123, "back-1", -1, PagingParameters.PagingDirection.NEXT);
        assertEquals(
                "PRAGMA kikimr.IsolationLevel='ReadUncommitted'; SELECT * FROM [Root/qloud/test] WHERE (message ILIKE 'test' OR host LIKE 'host-2') AND (timestamp  < -123 OR timestamp = -123 AND qloud_instance  < 'back-1' OR timestamp = -123 AND qloud_instance = 'back-1' AND pushclient_row_id  < -1) ORDER BY timestamp DESC, qloud_instance DESC, pushclient_row_id DESC LIMIT 50;",
                q("Root/qloud/test", null, "message ILIKE 'test' OR host LIKE 'host-2'", pagingParameters)
        );
    }

    @Test
    public void testYqlAndEsQuery() {
        PagingParameters pagingParameters = new PagingParameters(-123, "back-1", -1, PagingParameters.PagingDirection.NEXT);
        assertEquals(
                "PRAGMA kikimr.IsolationLevel='ReadUncommitted'; SELECT * FROM [Root/qloud/test] WHERE (message ILIKE 'test' OR host LIKE 'host-2') AND (TRUE AND ((qloud_instance LIKE 'back-1') OR (host LIKE 'host-1'))) AND (timestamp  < -123 OR timestamp = -123 AND qloud_instance  < 'back-1' OR timestamp = -123 AND qloud_instance = 'back-1' AND pushclient_row_id  < -1) ORDER BY timestamp DESC, qloud_instance DESC, pushclient_row_id DESC LIMIT 50;",
                q("Root/qloud/test", "qloud_instance: back-1 OR host: host-1", "message ILIKE 'test' OR host LIKE 'host-2'", pagingParameters)
        );
    }

    private String where(String s) {
        return esQueryConverter.convertToYqlWhereCondition(s, false);
    }

    private String q(String table, String esQuery, PagingParameters pagingParameters) {
        return q(table, esQuery, null, pagingParameters);
    }

    private String q(String table, String esQuery, String yqlQuery, PagingParameters pagingParameters) {
        return KikimrYqlQueryBuilder.buildYqlQuery(
                table, new QueryWhereCondition(esQuery, yqlQuery),
                pagingParameters, new TimeRangeFilter(null, null), false
        );
    }
}
