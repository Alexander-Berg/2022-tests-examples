package ru.yandex.tanker.gradle.android.types;

import org.fest.assertions.api.Assertions;
import org.junit.Test;

public class StringValueTest {

    @Test
    public void partialEscapeTest() throws Exception {
        assertEscapesAs("\'", "\\'");
        assertEscapesAs("\\'", "\\'");
        assertEscapesAs("Well it's working!", "Well it\\'s working!");
        assertEscapesAs("Well it\\'s working!", "Well it\\'s working!");
        assertEscapesAs("wow'", "wow\\'");
        assertEscapesAs("wow\\'", "wow\\'");
        assertEscapesAs("'m fine", "\\'m fine");
        assertEscapesAs("\\'m fine", "\\'m fine");

        assertEscapesAs("a>b", "a\\>b");
        assertEscapesAs("a\\>b", "a\\>b");

        assertEscapesAs("a<b", "a\\<b");
        assertEscapesAs("a\\<b", "a\\<b");

        assertEscapesAs("a&b", "a\\&b");
        assertEscapesAs("a\\&b", "a\\&b");

        assertEscapesAs("a\"b", "a\\\"b");
        assertEscapesAs("a\\\"b", "a\\\"b");
    }

    private void assertEscapesAs(String in, String res) {
        Assertions.assertThat(StringValue.partialEscape(in)).isEqualTo(res);
    }
}
