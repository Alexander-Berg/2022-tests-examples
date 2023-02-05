package com.yandex.frankenstein.filters.methods;

import com.yandex.frankenstein.settings.TestCases;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class SuiteMethodFilterTest {

    @Mock private MethodFilter mTestMethodFilter;
    @Mock private TestCases mTestCases;

    @Mock private FrameworkMethod mMethod;
    @Mock private List<Object> mTestParameters;

    private SuiteMethodFilter mSuiteFilter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mSuiteFilter = new SuiteMethodFilter(mTestMethodFilter);
    }

    @Test
    public void testIgnoreIfInSuite() {
        when(mTestMethodFilter.isIgnored(mMethod, mTestParameters)).thenReturn(true);
        final boolean isIgnored = mSuiteFilter.isIgnored(mMethod, mTestParameters);

        assertThat(isIgnored).isTrue();
    }

    @Test
    public void testDoNotIgnoreIfNotInSuite() {
        when(mTestMethodFilter.isIgnored(mMethod, mTestParameters)).thenReturn(false);
        final boolean isIgnored = mSuiteFilter.isIgnored(mMethod, mTestParameters);

        assertThat(isIgnored).isFalse();
    }

    @Test
    public void testShouldPrintIfTrue() {
        when(mTestMethodFilter.shouldPrint()).thenReturn(true);
        assertThat(mSuiteFilter.shouldPrint()).isTrue();
    }

    @Test
    public void testShouldPrintIfFalse() {
        when(mTestMethodFilter.shouldPrint()).thenReturn(false);
        assertThat(mSuiteFilter.shouldPrint()).isFalse();

    }

    @Test
    public void testGetDescription() {
        final SuiteMethodFilter suiteFilter = new SuiteMethodFilter(mTestCases);
        assertThat(suiteFilter.getDescription()).isEqualTo("Suite filter");
    }
}
