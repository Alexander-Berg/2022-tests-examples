package ru.yandex.webmaster.common.addurl;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.util.collections.Cf;
import ru.yandex.webmaster.common.util.TopKCounter;

/**
 * @author aherman
 */
public class AddUrlRateLimiterTest {
    @Test
    public void testToOwnerName() throws Exception {
        Assert.assertEquals("yandex.net", AddUrlRateLimiter.toOwnerName(
                "wmtest.people.yandex.net",
                Collections.<String>emptySet()
        ));

        Assert.assertEquals("people.yandex.net", AddUrlRateLimiter.toOwnerName(
                "wmtest.people.yandex.net",
                Cf.set("yandex.net")
        ));
    }

    @Test
    public void testErrorLimit() throws Exception {
        Assert.assertTrue(AddUrlRateLimiter.checkLimit(new TopKCounter.Measure<>("yandex.net", 10, 2), 0, 10, 0.1f));
        Assert.assertFalse(AddUrlRateLimiter.checkLimit(new TopKCounter.Measure<>("yandex.net", 10, 2), 0, 10, 0.3f));
    }

    @Test
    public void testMaxRequest() throws Exception {
        Assert.assertTrue(AddUrlRateLimiter.checkLimit(new TopKCounter.Measure<>("yandex.net", 10, 2), 0, 10, 0.1f));
        Assert.assertFalse(AddUrlRateLimiter.checkLimit(new TopKCounter.Measure<>("yandex.net", 10, 1), 0, 10, 0.1f));
    }
}
