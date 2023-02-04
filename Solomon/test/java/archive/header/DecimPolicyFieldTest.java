package ru.yandex.solomon.codec.archive.header;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class DecimPolicyFieldTest {

    @Test
    public void merge() {
        Assert.assertEquals(0, DecimPolicyField.merge((short) 0, (short) 0));
        Assert.assertEquals(10, DecimPolicyField.merge((short) 0, (short) 10));
        Assert.assertEquals(10, DecimPolicyField.merge((short) 10, (short) 0));
        Assert.assertEquals(11, DecimPolicyField.merge((short) 10, (short) 11));
        Assert.assertEquals(11, DecimPolicyField.merge((short) 12, (short) 11));
    }

}
