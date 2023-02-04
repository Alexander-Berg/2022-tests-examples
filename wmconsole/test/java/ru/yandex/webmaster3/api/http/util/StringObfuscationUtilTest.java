package ru.yandex.webmaster3.api.http.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author avhaliullin
 */
public class StringObfuscationUtilTest {
    @Test
    public void shouldBeDecoded() {
        String s = "kjeknkdfngklngf";
        String encoded = StringObfuscationUtil.encodeASCII(s);
        String decoded = StringObfuscationUtil.decodeASCII(encoded);
        Assert.assertEquals(s, decoded);
    }
}
