package ru.yandex.webmaster3.storage.recommendedquery.dao;

import junit.framework.TestCase;
import ru.yandex.webmaster3.storage.clickhouse.ClickhouseTableInfo;

/**
 * @author akhazhoyan 05/2018
 */
public class RecommendedQueriesTablesTest extends TestCase {


    public void testCreateMergeTableFrom() {
        ClickhouseTableInfo first = new ClickhouseTableInfo(
                null, null, null, null, null, null, null, null, "foo.abc", 1, 1
        );
        ClickhouseTableInfo second =  new ClickhouseTableInfo(
                null, null, null, null, null, null, null, null, "bar.def", 1, 1
        );
        String actual = RecommendedQueriesTables.createMergeTableQuery(first, second);
        // сильно не лезем в кишки -- не проверяем имя и схему
        String REGEX = "^CREATE TABLE \\w+\\.\\w+ \\([\\w\\s,]*\\) ENGINE = Merge\\(\\w+, '\\^\\(abc\\)\\|\\(def\\)\\$'\\)$";
        assertTrue(actual.matches(REGEX));
    }

    public void testCreateMergeTableFromPreconditions() {
        try {
            RecommendedQueriesTables.createMergeTableQuery(null, null);
            fail();
        } catch (Exception expected) {
            // pass
        }
    }
}