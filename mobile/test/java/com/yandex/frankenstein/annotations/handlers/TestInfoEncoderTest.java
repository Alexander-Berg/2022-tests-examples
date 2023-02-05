package com.yandex.frankenstein.annotations.handlers;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestInfoEncoderTest {

    private static final String DISPLAY_NAME = "display name";
    private static final String METHOD_NAME = "methodName";
    private static final String CLASS_NAME = "ClassName";
    private static final int TEST_CASE_ID = 42;

    private final TestInfoEncoder mTestInfoEncoder = new TestInfoEncoder();
    private final Description mDescription = mock(Description.class);

    @Before
    public void setUp() {
        when(mDescription.getDisplayName()).thenReturn(DISPLAY_NAME);
        when(mDescription.getMethodName()).thenReturn(METHOD_NAME);
        when(mDescription.getClassName()).thenReturn(CLASS_NAME);
    }

    @Test
    public void testEncode() {
        final JSONObject info = new JSONObject(mTestInfoEncoder.encode(TEST_CASE_ID, mDescription));

        assertThat(info)
                .usingComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .isEqualTo(new JSONObject().put("case id", TEST_CASE_ID).put("method", DISPLAY_NAME)
                        .put("class", CLASS_NAME).put("name", METHOD_NAME));
    }
}
