package ru.yandex.realty.misc.enums;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * @author Ildar Safarov
 */
public class IntEnumResolverTest extends TestCase {

    public void testValueOf() {
        Assert.assertEquals(Qualities.KINDNESS, Qualities.R.valueOf("kindness"));
        Assert.assertEquals(Qualities.EGOISM, Qualities.R.valueOf("Egoism"));
    }

    public void testFromValueO() {
        Assert.assertEquals(Qualities.UNKNOWN, Qualities.R.fromValue(0));
        Assert.assertEquals(Qualities.CRUELTY, Qualities.R.fromValue(1));
        Assert.assertEquals(Qualities.INDIFFERENCE, Qualities.R.fromValue(12));

        Assert.assertNull(Qualities.R.fromValueOrNull(100));
    }
}
