package com.yandex.frankenstein.annotations.handlers;

import org.json.JSONObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IgnoredTestInfoEncoderTest {

    private static final int TEST_CASE_ID = 42;
    private static final String REASON = "whatever";

    private final IgnoredTestInfoEncoder mIgnoredTestInfoEncoder = new IgnoredTestInfoEncoder();

    @Test
    public void testEncode() {
        final JSONObject info = new JSONObject(mIgnoredTestInfoEncoder.encode(TEST_CASE_ID, REASON));

        assertThat(info)
                .usingComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .isEqualTo(new JSONObject().put("case id", TEST_CASE_ID).put("reason", REASON));
    }
}
