package com.yandex.frankenstein.filters.methods;

import com.yandex.frankenstein.annotations.handlers.TestCaseIdHandler;
import com.yandex.frankenstein.settings.TestCases;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class TestCasesMethodFilterTest {

    private static final int TEST_CASE_ID = 42;
    private static final String DESCRIPTION = "some description";

    @Mock private TestCases mTestCases;
    @Mock private JsonDigger mJsonDigger;
    @Mock private Predicate<List<String>> mMatcher;
    @Mock private TestCaseIdHandler mTestCaseIdHandler;

    @Mock private FrameworkMethod mMethod;
    @Mock private List<Object> mTestParameters;

    private TestCasesMethodFilter mTestCasesIgnoranceFilter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mTestCasesIgnoranceFilter = new TestCasesMethodFilter(mTestCases, mJsonDigger, mMatcher, DESCRIPTION,
                mTestCaseIdHandler);

        final JSONObject json = new JSONObject("{\"314\":{}}");

        when(mTestCaseIdHandler.getTestCaseId(mMethod, mTestParameters)).thenReturn(TEST_CASE_ID);
        when(mTestCases.getTestCase(TEST_CASE_ID)).thenReturn(json);
        when(mJsonDigger.get(json)).thenReturn(new JSONArray("[\"first\", \"second\"]"));
    }

    @Test
    public void testGetDescription() {
        final TestCasesMethodFilter attributesIgnoranceFilter = new TestCasesMethodFilter(mTestCases,
                Collections.emptyList(), mMatcher, DESCRIPTION);

        assertThat(attributesIgnoranceFilter.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    public void testIsIgnored() {
        when(mMatcher.test(Arrays.asList("first", "second"))).thenReturn(true);

        assertThat(mTestCasesIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isTrue();
    }

    @Test
    public void testShouldPrint() {
        assertThat(mTestCasesIgnoranceFilter.shouldPrint()).isTrue();
    }

    @Test
    public void testIsIgnoredIfDoesNotIgnored() {
        when(mMatcher.test(Arrays.asList("first", "second"))).thenReturn(false);

        assertThat(mTestCasesIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isFalse();
    }
}
