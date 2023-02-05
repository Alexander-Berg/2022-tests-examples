package ru.yandex.market.utils;


import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.utils.StringUtils.contentEquals;
import static ru.yandex.market.utils.StringUtils.removeFirstLetter;
import static ru.yandex.market.utils.StringUtils.removeLastLetter;
import static ru.yandex.market.utils.StringUtils.unescapeUnicode;

@RunWith(Enclosed.class)
public class StringUtilsTest {

    @RunWith(Parameterized.class)
    public static class UnicodeUnescapeParameterizedTest {

        @Parameterized.Parameter
        public String input;

        @Parameterized.Parameter(1)
        public String expected;

        @Parameterized.Parameters(name = "{index}: \"{0}\" → \"{1}\"")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"", ""},
                    {"\\u0000", "\u0000"},
                    {"\\uffff", "\uffff"},
                    {"\\ufffe", "\ufffe"},
                    {"\\ufffd", "�"},
                    {"\\u0408", "Ј"},
                    {"\\u+0408", "Ј"},
                    {"\\uuu0408", "Ј"},
                    {"\\uuu+0408", "Ј"},
                    {"\\+0408", "\\+0408"},
                    {"\\u+040", "\\u+040"},
                    {"\\u040g", "\\u040g"},
                    {"\\uuuuu", "\\uuuuu"},
                    {"\\u++0408", "\\u++0408"},
                    {"\\u++++", "\\u++++"},
                    {"\\\\u0408", "\\Ј"},
                    {"Hello!", "Hello!"},
                    {"\\u0048\\u0065\\u006C\\u006C\\u006F\\u0021", "Hello!"},
                    {"\\\\u0048\\\\u0065\\\\u006C\\\\u006C\\\\u006F\\\\u0021",
                            "\\H\\e\\l\\l\\o\\!"},
                    {"He\\u006clo!", "Hello!"},
                    {"Hello\\uuuu+", "Hello\\uuuu+"},
                    {"Hello\\u", "Hello\\u"},
                    {"Hello\\", "Hello\\"},
                    {"Hello\\u002", "Hello\\u002"},
                    {"He\\u006\\u006clo!", "He\\u006llo!"},
                    {"He\\u\\u\\\\u006clo!", "He\\u\\u\\llo!"},
                    {"He\\006clo!", "He\\006clo!"},
                    {"He\\uGA6clo!", "He\\uGA6clo!"},
                    {"He\\uuuuuu+006clo!", "Hello!"},
                    {"He\\u+006cl\\u006F!", "Hello!"}
            });
        }

        @Test
        public void testUnicodeUnescape() {
            final String result = unescapeUnicode(input);
            assertEquals(expected, result);
        }
    }

    public static class UnicodeUnescapeNullTest {

        @Test(expected = NullPointerException.class)
        public void testThrowsNullPointerExceptionForNullInput() {
            //noinspection ConstantConditions
            unescapeUnicode(null);
        }
    }

    @RunWith(Parameterized.class)
    public static class ContentEqualsParameterizedTest {

        @Parameterized.Parameter
        public CharSequence cs1;

        @Parameterized.Parameter(1)
        public CharSequence cs2;

        @Parameterized.Parameter(2)
        public Boolean expectedResult;

        @Parameterized.Parameters(name = "{index}: \"{0}\" == \"{1}\" → {2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {null, null, true},
                    {null, "", false},
                    {"", null, false},
                    {"", "", true},
                    {"", "1", false},
                    {"1", "1", true},
                    {"A", "a", false},
                    {"123", "123", true},
                    {"123", "1234", false},
            });
        }

        @Test
        public void testUnicodeUnescape() {
            final boolean result = contentEquals(cs1, cs2);
            assertEquals(expectedResult, result);
        }
    }

    public static class RemoteLettersTest {

        @Test
        public void removeFirstLetterTest() {
            assertNull(removeFirstLetter(null));
            assertEquals("", removeFirstLetter(""));
            assertEquals("", removeFirstLetter("s"));
            assertEquals("d", removeFirstLetter("sd"));
            assertEquals("werty", removeFirstLetter("qwerty"));
        }

        @Test
        public void removeLastLetterTest() {
            assertNull(removeFirstLetter(null));
            assertEquals("", removeLastLetter(""));
            assertEquals("", removeLastLetter("s"));
            assertEquals("s", removeLastLetter("sd"));
            assertEquals("qwert", removeLastLetter("qwerty"));
        }
    }

    @RunWith(Parameterized.class)
    public static class RemoveTrailingLineBreaksTest {

        @Parameterized.Parameter
        public String input;

        @Parameterized.Parameter(1)
        public String expectedResult;

        @Parameterized.Parameters(name = "{index}: \"{0}\" -> \"{1}\"")
        public static Iterable<Object[]> getData() {
            return Arrays.asList(
                    new Object[]{"", ""},
                    new Object[]{"Hello World!\n", "Hello World!"},
                    new Object[]{"Hello World!\n\n", "Hello World!"},
                    new Object[]{"Hello World!\r\n", "Hello World!"},
                    new Object[]{"Hello World!\r\n\n", "Hello World!"},
                    new Object[]{"Hello World!\r\n\r\n", "Hello World!"},
                    new Object[]{"Hello World!\r\n\r\n\n\r\n", "Hello World!"},
                    new Object[]{"Hello World!\n\r", "Hello World!\n\r"},
                    new Object[]{"Hello World!\nA", "Hello World!\nA"},
                    new Object[]{"\n", ""},
                    new Object[]{"\n\n", ""},
                    new Object[]{"\r\n", ""},
                    new Object[]{"\r\n\n", ""},
                    new Object[]{"\r\n\r\n", ""},
                    new Object[]{"\r\n\n\r\n", ""}
            );
        }

        @Test
        public void properlyRemovesTrailingLineBreaks() {
            final String formatted = StringUtils.removeTrailingLineBreaks(input);

            assertThat(formatted, equalTo(expectedResult));
        }
    }
}
