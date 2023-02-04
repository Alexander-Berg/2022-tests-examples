package ru.yandex.realty.phone;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.model.offer.PhoneNumber;

public class PhoneParserTest {

    @Test
    public void test1() {
        PhoneNumber pn1 = PhoneParser.simple("+79111234567");
        Assert.assertEquals("7", pn1.getRegion());
        Assert.assertEquals("911", pn1.getCode());
        Assert.assertEquals("1234567", pn1.getNumber());

        PhoneNumber pn2 = PhoneParser.simple("8 (911)123-45-67");
        Assert.assertEquals("8", pn2.getRegion());
        Assert.assertEquals("911", pn2.getCode());
        Assert.assertEquals("1234567", pn2.getNumber());
    }

}
