package ru.yandex.realty.model.offer;

import org.junit.Assert;
import org.junit.Test;


public class PhoneNumberTest {

    @Test
    public void testFormatted() {
        PhoneNumber pn1 = new PhoneNumber("911", "1234567", "7");
        Assert.assertEquals("+7 (911) 123-45-67", pn1.formatted());

        PhoneNumber pn2 = new PhoneNumber(null, "9111234567", "7");
        Assert.assertEquals("+79111234567", pn2.formatted());

        PhoneNumber pn3 = new PhoneNumber("911", "1234567", "8");
        Assert.assertEquals("8 (911) 123-45-67", pn3.formatted());
    }

    @Test
    public void testFormattedCode() {
        PhoneNumber pn1 = new PhoneNumber("911", "1234567", "7");
        Assert.assertEquals("+7 (911) 1234567", pn1.formattedCode());

        PhoneNumber pn2 = new PhoneNumber(null, "9111234567", "7");
        Assert.assertEquals("+7 9111234567", pn2.formattedCode());

        PhoneNumber pn3 = new PhoneNumber("911", "1234567", "8");
        Assert.assertEquals("8 (911) 1234567", pn3.formattedCode());
    }


}
