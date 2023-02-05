package ru.yandex.disk.provider;

import org.junit.Test;
import ru.yandex.disk.test.AndroidTestCase2;

import static org.hamcrest.Matchers.equalTo;

public class ContentRequestFactoryTest extends AndroidTestCase2 {

    @Test
    public void shouldBuildCorrectRankSelection() throws Exception {
        final String sql = ContentRequestFactory.createRankSelection("FEED_LOADED_BLOCK_ITEMS", "v",
                new String[]{"FRACTION", "FIRST_FRACTION_ORDER", "SERVER_ORDER"}, null);
        assertThat(sql, equalTo("(SELECT COUNT() FROM " +
                "(SELECT rowid FROM FEED_LOADED_BLOCK_ITEMS AS t " +
                "WHERE FRACTION < v.FRACTION " +
                "OR (FRACTION = v.FRACTION AND FIRST_FRACTION_ORDER < v.FIRST_FRACTION_ORDER) " +
                "OR (FRACTION = v.FRACTION AND FIRST_FRACTION_ORDER = v.FIRST_FRACTION_ORDER AND SERVER_ORDER < v.SERVER_ORDER))" +
                ") AS rank"));


    }
}