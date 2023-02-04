package ru.yandex.auto.core.util;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Tests for {@link StringIntern}
 *
 * @author incubos
 */
public class StringInternTest {
  public static final String INTERNED = "13";

  @Test
  public void testIntern() {
    final String s1 = "12";
    final String s2 = "23";

    final String s = s1.substring(0, 1) + s2.substring(1, 2);

    assertNotSame(s.intern(), s);
    assertSame(s.intern().intern(), s.intern());
  }
}
