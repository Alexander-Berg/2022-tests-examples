package ru.yandex.solomon.labels;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author checat
 */
public class LabelValidatorTest {

    @Test
    public void test() {
        Assert.assertFalse(LabelValidator.isValidName("!asdffr"));
        Assert.assertFalse(LabelValidator.isValidName(""));
        Assert.assertFalse(LabelValidator.isValidName("+"));
        Assert.assertTrue(LabelValidator.isValidName("0"));
        Assert.assertTrue(LabelValidator.isValidName("-"));
        Assert.assertTrue(LabelValidator.isValidName("calc-udf"));
        Assert.assertTrue(LabelValidator.isValidName("cswkdfugjbsjv"));
        Assert.assertTrue(LabelValidator.isValidName("-0123456789qwertyuiopdfgkzvmQWERTYASDFGHJZXCVBNM_"));
    }
}
