package com.yandex.frankenstein.settings;

import com.yandex.frankenstein.io.ResourceReader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCasesTest {

    private static final String TEST_CASES_FILE = "test_cases.txt";
    private static final String INVALID_JSON = "not a valid json";
    private static final String ATTRIBUTE_NAME = "attribute_name";
    private static final String MISSING_ATTRIBUTE_NAME = "missing_attribute_name";
    private static final int TEST_CASE_ID = 42;
    private static final int MISSING_TEST_CASE_ID = 100;

    private final String[] mAttributeValues = new String[]{"attribute_value"};
    private final JSONObject mAttributesJson = new JSONObject().put(ATTRIBUTE_NAME, new JSONArray(mAttributeValues));
    private final JSONObject mTestCaseJsonWithoutId = new JSONObject().put("attributes", mAttributesJson);
    private final JSONObject mTestCaseJson = new JSONObject(mTestCaseJsonWithoutId.toString()).put("id", TEST_CASE_ID);
    private final ResourceReader mResourceReader = mock(ResourceReader.class);
    private final TestCases mTestCases = createTestCases(mTestCaseJson);

    @Test
    public void testIsNotValidWhenFailedToReadResource() {
        when(mResourceReader.readAsString(TEST_CASES_FILE)).thenThrow(RuntimeException.class);
        final TestCases testCases = new TestCases(TEST_CASES_FILE, mResourceReader);

        assertThat(testCases.isValid()).isFalse();
    }

    @Test
    public void testIsNotValidWhenTestCasesIsNotJson() {
        when(mResourceReader.readAsString(TEST_CASES_FILE)).thenReturn(INVALID_JSON);
        final TestCases testCases = new TestCases(TEST_CASES_FILE, mResourceReader);

        assertThat(testCases.isValid()).isFalse();
    }

    @Test
    public void testIsNotValidWhenTestCaseIsNotJson() {
        final TestCases testCases = createTestCases(INVALID_JSON);

        assertThat(testCases.isValid()).isFalse();
    }

    @Test
    public void testIsNotValidWhenIdIsMissing() {
        final TestCases testCases = createTestCases(mTestCaseJsonWithoutId);

        assertThat(testCases.isValid()).isFalse();
    }

    @Test
    public void testIsValid() {
        assertThat(mTestCases.isValid()).isTrue();
    }

    @Test
    public void testHasId() {
        assertThat(mTestCases.has(TEST_CASE_ID)).isTrue();
    }

    @Test
    public void testDoesNotHaveId() {
        assertThat(mTestCases.has(MISSING_TEST_CASE_ID)).isFalse();
    }

    @Test
    public void testGetTestCase() {
        assertThat(mTestCases.getTestCase(TEST_CASE_ID).toString()).isEqualTo(mTestCaseJson.toString());
    }

    @Test
    public void testGetTestCaseIfMissing() {
        assertThat(mTestCases.getTestCase(MISSING_TEST_CASE_ID).toString()).isEqualTo(new JSONObject().toString());
    }

    @Test
    public void testGetAttribute() {
        assertThat(mTestCases.getAttribute(TEST_CASE_ID, ATTRIBUTE_NAME)).containsExactly(mAttributeValues);
    }

    @Test
    public void testGetMissingAttribute() {
        assertThat(mTestCases.getAttribute(TEST_CASE_ID, MISSING_ATTRIBUTE_NAME)).isEmpty();
    }

    @SafeVarargs
    private final <T> TestCases createTestCases(final T... testCases) {
        when(mResourceReader.readAsString(TEST_CASES_FILE)).thenReturn(new JSONArray(testCases).toString());
        return new TestCases(TEST_CASES_FILE, mResourceReader);
    }
}
