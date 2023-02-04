package ru.yandex.webmaster3.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * @author avhaliullin
 */
public class ReflectionUtilsTest {
    @Test
    public void testCurrentMethod() {
        Assert.assertEquals("testCurrentMethod", ReflectionUtils.getCallerMethod(new JavaMethodWitness() {}).getName());
    }

    @Test
    public void testEnclosingMethodNameForLambda() {
        String expected = "testEnclosingMethodNameForLambda";
        Assert.assertEquals(expected, Optional.of(1).map(ign -> ReflectionUtils.getEnclosingMethodName(ReflectionUtils.getCallerMethod(new JavaMethodWitness() {}))).get());
    }
}
