package ru.yandex.market.util.phone;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class PhoneNumberFormatterTest {

    @RunWith(Parameterized.class)
    public static class FormatTest {

        @Parameterized.Parameter
        public String input;

        @Parameterized.Parameter(1)
        public String expected;

        @Parameterized.Parameters(name = "{index}: \"{0}\" → \"{1}\"")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {null, ""},
                    {"", ""},
                    {"+", "+"},
                    {"8", "+7 ("},
                    {"1", "+7 ("},
                    {"A", ""},
                    {"AA", ""},
                    {"71", "+7 (1"},
                    {"712", "+7 (12"},
                    {"7123", "+7 (123) "},
                    {"71234", "+7 (123) 4"},
                    {"712345", "+7 (123) 45"},
                    {"7123456", "+7 (123) 456-"},
                    {"71234567", "+7 (123) 456-7"},
                    {"7a1b2c3d4e5r6f7d", "+7 (123) 456-7"},
                    {"712345678", "+7 (123) 456-78-"},
                    {"7123456789", "+7 (123) 456-78-9"},
                    {"71234567890", "+7 (123) 456-78-90"},
                    {"71234567890123", "+7 (123) 456-78-90"},
                    {"A7B1C2D3E4F5G6D7890123", "+7 (123) 456-78-90"},
                    {"3", "+7 ("},
            });
        }

        @Test
        public void testFormattedOutputMatchExpectations() {
            final CharSequence result = new PhoneNumberFormatter().format(input);
            assertThat(CharBuffer.wrap(result), equalTo(CharBuffer.wrap(expected)));
        }
    }

    @RunWith(Parameterized.class)
    public static class HandleBackslashTest {

        @Parameterized.Parameter
        public String input;

        @Parameterized.Parameter(1)
        public int cursorPosition;

        @Parameterized.Parameter(2)
        public String expected;

        @Parameterized.Parameters(name = "{index}: [\"{0}\", {1}] → \"{2}\"")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {null, 0, ""},
                    {"", 0, ""},
                    {"+", 1, ""},
                    {"+7 (", 4, ""},
                    {"A", 1, ""},
                    {"AA", 2, ""},
                    {"+7 (1", 5, "+7 ("},
                    {"+7 (12", 6, "+7 (1"},
                    {"+7 (123) ", 8, "+7 (12"},
                    {"+7 (123) 4", 10, "+7 (123) "},
                    {"+7 (123) 45", 11, "+7 (123) 4"},
                    {"+7 (123) 456-", 13, "+7 (123) 45"},
                    {"+7 (123) 456-7", 14, "+7 (123) 456-"},
                    {"+7 (123) 456-78-", 16, "+7 (123) 456-7"},
                    {"+7 (123) 456-78-9", 17, "+7 (123) 456-78-"},
                    {"+7 (123) 456-78-90", 18, "+7 (123) 456-78-9"},
                    {"+7 (123) 456-78-90", 17, "+7 (123) 456-78-0"},
                    {"+7 (123) 456-78-90", 16, "+7 (123) 456-79-0"},
                    {"+7 (123) 456-78-90", 15, "+7 (123) 456-79-0"},
                    {"+7 (123) 456-78-90", 14, "+7 (123) 456-89-0"},
                    {"+7 (123) 456-78-90", 13, "+7 (123) 457-89-0"},
                    {"+7 (123) 456-78-90", 12, "+7 (123) 457-89-0"},
                    {"+7 (123) 456-78-90", 11, "+7 (123) 467-89-0"},
                    {"+7 (123) 456-78-90", 10, "+7 (123) 567-89-0"},
                    {"+7 (123) 456-78-90", 9, "+7 (124) 567-89-0"},
                    {"+7 (123) 456-78-90", 8, "+7 (124) 567-89-0"},
                    {"+7 (123) 456-78-90", 7, "+7 (124) 567-89-0"},
                    {"+7 (123) 456-78-90", 6, "+7 (134) 567-89-0"},
                    {"+7 (123) 456-78-90", 5, "+7 (234) 567-89-0"},
                    {"+7 (123) 456-78-90", 4, "+7 (234) 567-89-0"},
                    {"+7 (123) 456-78-90", 3, "+7 (234) 567-89-0"},
                    {"+7 (123) 456-78-90", 2, "+7 (234) 567-89-0"},
                    {"+7 (123) 456-78-90", 1, "+7 (123) 456-78-90"},
                    {"+7 (123) 456-78-90", 0, "+7 (123) 456-78-90"},
            });
        }

        @Test
        public void testFormattedOutputMatchExpectations() {
            final CharSequence result = new PhoneNumberFormatter().handleBackspace(
                    input, cursorPosition);
            assertThat(CharBuffer.wrap(result), equalTo(CharBuffer.wrap(expected)));
        }
    }
}