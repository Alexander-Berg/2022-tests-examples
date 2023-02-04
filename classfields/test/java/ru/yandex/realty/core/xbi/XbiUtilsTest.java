package ru.yandex.realty.core.xbi;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.serialization.parser.xbi.XbiUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author aherman
 */
public class XbiUtilsTest {
    @Test
    public void testSetters() {
        Map<String, Method> map = XbiUtils.findSetters(Simple2.class);
        Assert.assertEquals(11, map.size());
    }
}
