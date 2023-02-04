package ru.yandex.webmaster3.storage.util.ydb.querybuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.yandex.ydb.table.values.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.webmaster3.storage.util.ydb.querybuilder.typesafe.DataMapper;
import ru.yandex.webmaster3.storage.util.ydb.querybuilder.typesafe.Field;
import ru.yandex.webmaster3.storage.util.ydb.querybuilder.typesafe.Fields;


/**
 * ishalaru
 * 17.06.2020
 **/
@Ignore
public class SelectTest {
    public static DataMapper<Pair<String, String>> ROW_MAPPER = DataMapper.create(Fields.stringField("first"), Fields.stringField("second"),
            (a, b) -> Pair.of(a, b));

    public static DataMapper<CountData> CNT_DATA_MAPPER = DataMapper.create(F.ID, F.DATE, F.COMMENT.count("sum_cnt"),
            (a, b, c) -> new CountData(a, b, c));

    @lombok.Value
    public static class CountData {
        Integer Id;
        LocalDate date;
        Long cnt;
    }

    @Test
    public void simpleSelectTest() {
        final Select select = new Select("tablePrefix",
                "test",
                ROW_MAPPER
                , null);
        final String s = select.toQueryString();
        String query = """
                --!syntax_v1
                PRAGMA TablePathPrefix = 'tablePrefix';
                SELECT\s
                `first`,
                `second`
                FROM test
                ;""";

        Assert.assertEquals("Compare simple query", query, s);
    }

    @Test
    public void simpleSelectWithLimitTest() {
        final Select select = new Select("tablePrefix", "test", ROW_MAPPER, null).limit(10);
        final String s = select.toQueryString();
        String query = """
                --!syntax_v1
                PRAGMA TablePathPrefix = 'tablePrefix';
                DECLARE $skip as Int32;
                DECLARE $limit as Int32;
                SELECT\s
                `first`,
                `second`
                FROM test
                LIMIT $skip, $limit
                ;""";
        Assert.assertEquals("Compare simple query", query, s);
        final Map<String, Value> parameters = select.getParameters();
        Assert.assertEquals("parameters count", 2, parameters.size());
        Assert.assertEquals("parameters value", 10, parameters.get("$limit").asData().getInt32());
        Assert.assertEquals("parameters value", 0, parameters.get("$skip").asData().getInt32());
    }

    @Test
    public void simpleSelectWithLimitAndSkipTest() {
        final Select select = new Select("tablePrefix", "test", ROW_MAPPER, null).limit(10, 12);
        final String s = select.toQueryString();
        String query = """
                --!syntax_v1
                PRAGMA TablePathPrefix = 'tablePrefix';
                DECLARE $skip as Int32;
                DECLARE $limit as Int32;
                SELECT\s
                `first`,
                `second`
                FROM test
                LIMIT $skip, $limit
                ;""";
        Assert.assertEquals("Compare simple query", query, s);
        final Map<String, Value> parameters = select.getParameters();
        Assert.assertEquals("parameters count", 2, parameters.size());
        Assert.assertEquals("parameters value", 12, parameters.get("$limit").asData().getInt32());
        Assert.assertEquals("parameters value", 10, parameters.get("$skip").asData().getInt32());
    }

    @Test
    public void simpleSelectWithOrderBy() {
        final Select select = new Select("tablePrefix", "test", ROW_MAPPER, null)
                .order(QueryBuilder.asc("first"))
                .order(QueryBuilder.desc("tree"));
        String query = """
                --!syntax_v1
                PRAGMA TablePathPrefix = 'tablePrefix';
                SELECT\s
                `first`,
                `second`,
                `tree`
                FROM test
                ORDER BY `first` ASC,`tree` DESC
                ;""";
        Assert.assertEquals("Compare simple query", query, select.toQueryString());
    }

    @Test
    public void selectWithQuery() {
        final Select select = new Select("tablePrefix", "test", ROW_MAPPER, null);
        select.where(QueryBuilder.in("tree", List.of(1, 2, 3, 4)))
                .and(QueryBuilder.like("second", "test"))
                .and(QueryBuilder.or(QueryBuilder.notNull("first"),
                        QueryBuilder.and(QueryBuilder.or(QueryBuilder.eq("four", 4), QueryBuilder.eq("four", 44)),
                                QueryBuilder.eq("four", 17))));
        String query = """
                --!syntax_v1
                PRAGMA TablePathPrefix = 'tablePrefix';
                DECLARE $parameter_1 as "List<Int32>";
                DECLARE $parameter_2 as Utf8;
                DECLARE $parameter_3 as Int32;
                DECLARE $parameter_4 as Int32;
                DECLARE $parameter_5 as Int32;
                SELECT\s
                `first`,
                `second`
                FROM test
                WHERE\s
                tree in $parameter_1 and\s
                second  LIKE  $parameter_2 and\s
                (first IS NOT NULL
                 OR ((four = $parameter_3 OR four = $parameter_4 )  AND four = $parameter_5 )  )\s
                ;""";
        Assert.assertEquals("Compare query with where", query, select.toQueryString());
    }

    @Test
    public void testGroupBy() {
        final Select select = new Select("/ru-prestable/wmcon/test/webmaster/experiment", "comments", CNT_DATA_MAPPER, null);
        select.where(F.ID.in(List.of(1, 2, 3))).groupBy(F.ID, F.DATE);
        String expected = """
                --!syntax_v1
                PRAGMA TablePathPrefix = '/ru-prestable/wmcon/test/webmaster/experiment';
                DECLARE $parameter_1 as "List<Int32>";
                SELECT\s
                `date`,
                count(`comment`) as sum_cnt,
                `id`
                FROM comments
                WHERE\s
                `id` in $parameter_1
                GROUP BY `id`,`date`
                ;""";
        Assert.assertEquals("Compare query with group by", expected, select.toQueryString());
        Assert.assertEquals("Cnt parameters", 1, select.getParameters().size());
    }

    @Test
    public void testHaving() {
        final Select select = new Select("/ru-prestable/wmcon/test/webmaster/experiment", "comments", CNT_DATA_MAPPER, null);
        select.where(F.ID.in(List.of(1, 2, 3))).groupBy(F.ID, F.DATE).having(QueryBuilder.gt(F.COMMENT.count().toString(), 23));
        String expected = """
                --!syntax_v1
                PRAGMA TablePathPrefix = '/ru-prestable/wmcon/test/webmaster/experiment';
                DECLARE $parameter_1 as "List<Int32>";
                DECLARE $parameter_2 as Int32;
                SELECT\s
                `date`,
                count(`comment`) as sum_cnt,
                `id`
                FROM comments
                WHERE\s
                `id` in $parameter_1
                GROUP BY `id`,`date`
                HAVING\s
                count(`comment`) > $parameter_2
                ;""";
        Assert.assertEquals("Compare query with group by", expected, select.toQueryString());
        Assert.assertEquals("Cnt parameters", 2, select.getParameters().size());
    }

    @Test
    public void testSecondaryIndex() {
        final LocalDate now = LocalDate.parse("2007-12-03");
        final Select select = new Select("/ru-prestable/wmcon/test/webmaster/experiment", "comments", CNT_DATA_MAPPER, null);
        select.secondaryIndex("date_index").where(F.DATE.eq(now));
        String expected = """
                --!syntax_v1
                --!syntax_v1 PRAGMA TablePathPrefix = '/ru-prestable/wmcon/test/webmaster/experiment';
                DECLARE $parameter_1 as Date;
                SELECT\s
                `date`,
                count(`comment`) as sum_cnt,
                `id`
                FROM comments view date_index
                WHERE\s
                `date` = $parameter_1
                ;""";
        Assert.assertEquals("Compare query with group by", expected, select.toQueryString());
        Assert.assertEquals("Cnt parameters", 1, select.getParameters().size());
    }

    @Test
    public void testSelectCount() {
        final LocalDate now = LocalDate.parse("2007-12-03");

        final Select select = new Select("/ru-prestable/wmcon/test/webmaster/experiment", "comments", null, null);
        select.countAll().where(F.ID.eq(123));
        String expected = """
                --!syntax_v1
                PRAGMA TablePathPrefix = '/ru-prestable/wmcon/test/webmaster/experiment';
                DECLARE $parameter_1 as Int32;

                SELECT count(*) FROM (SELECT\s
                *
                FROM comments
                WHERE\s
                `id` = $parameter_1
                );""";
        Assert.assertEquals("Compare query with group by", expected, select.toQueryString());
        Assert.assertEquals("Cnt parameters", 1, select.getParameters().size());
    }

    @Test
    public void testSelectToLower() {
        final Select select = new Select("/ru-prestable/wmcon/test/webmaster/experiment", "comments", F.ID.combine(F.COMMENT, Pair::of), null);
        select.where(F.COMMENT.toLower().like("%test%"));
        String expected = """
                --!syntax_v1
                PRAGMA TablePathPrefix = '/ru-prestable/wmcon/test/webmaster/experiment';
                DECLARE $parameter_1 as Utf8;
                SELECT\s
                `comment`,
                `id`
                FROM comments
                WHERE\s
                String::ToLower(`comment`)  LIKE  $parameter_1
                ;""";
        Assert.assertEquals("Compare query with group by", expected, select.toQueryString());
        Assert.assertEquals("Cnt parameters", 1, select.getParameters().size());
    }

    interface F {
        Field<Integer> ID = Fields.intField("id");
        Field<LocalDate> DATE = Fields.dateField("date");
        Field<String> COMMENT = Fields.stringField("comment");
    }
}
