package ru.yandex.infra.stage.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatThrowsWithMessage;

class ChecksumTest {
    private static final String UNTYPED_VALUE = "aaa";
    private static final String TYPED_VALUE = "MD5:aaa";

    @Test
    void singleArgument() {
        verifyChecksum(Checksum.fromString(TYPED_VALUE));
    }

    @Test
    void failIfTypeNotSpecified() {
        assertThatThrowsWithMessage(Exception.class,
                "Checksum must start with sha256, md5 or empty",
                () -> Checksum.fromString(UNTYPED_VALUE)
        );
    }

    @Test
    void onlyExplicitType() {
        verifyChecksum(new Checksum(UNTYPED_VALUE, Checksum.Type.MD5));
    }

    @Test
    void podAgentFormatTest() {
        assertThat(new Checksum(UNTYPED_VALUE, Checksum.Type.MD5).toAgentFormat(), equalTo("MD5:" + UNTYPED_VALUE));
    }

    @Test
    void throwIfSchemePrefixIsPresentAndForbidden() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Checksum(TYPED_VALUE, Checksum.Type.MD5));
    }

    @Test
    void emptyFormatTest() {
        assertThat(Checksum.EMPTY.toAgentFormat(), equalTo("EMPTY:"));
    }

    private static void verifyChecksum(Checksum c) {
        assertThat(c.getType(), equalTo(Checksum.Type.MD5));
        assertThat(c.getValue(), equalTo(UNTYPED_VALUE));
    }
}
