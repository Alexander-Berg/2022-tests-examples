package com.yandex.frankenstein.filters;

import com.yandex.frankenstein.annotations.TestCaseId;
import com.yandex.frankenstein.annotations.handlers.TestInfoTransmitter;
import com.yandex.frankenstein.filters.methods.MethodFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ExecutionFilterTest {

    private static final int FIRST_TEST_CASE_ID = 42;
    private static final int SECOND_TEST_CASE_ID = 43;
    private static final String BEFORE_DESCRIPTION = "first description";
    private static final String DURING_DESCRIPTION = "second description";

    private final PrintStream mSystemErr = System.err;

    @Mock private PrintStream mErr;
    @Mock private MethodFilter mBeforeTestMethodFilter;
    @Mock private MethodFilter mDuringTestMethodFilter;

    @Mock private TestInfoTransmitter mTestInfoTransmitter;

    @Mock private FrameworkMethod mFirstMethod;
    @Mock private FrameworkMethod mSecondMethod;
    @Mock private List<Object> mTestParameters;
    private final TestCaseId mFirstAnnotation = mock(TestCaseId.class);
    private final TestCaseId mSecondAnnotation = mock(TestCaseId.class);

    private ExecutionFilter mExecutionFilter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        System.setErr(mErr);

        when(mFirstMethod.getAnnotation(TestCaseId.class)).thenReturn(mFirstAnnotation);
        when(mFirstAnnotation.value()).thenReturn(FIRST_TEST_CASE_ID);
        doReturn(TestCaseId.class).when(mFirstAnnotation).annotationType();

        when(mSecondMethod.getAnnotation(TestCaseId.class)).thenReturn(mSecondAnnotation);
        when(mSecondAnnotation.value()).thenReturn(SECOND_TEST_CASE_ID);
        doReturn(TestCaseId.class).when(mSecondAnnotation).annotationType();

        mExecutionFilter = new ExecutionFilter(mBeforeTestMethodFilter, mDuringTestMethodFilter);
        when(mBeforeTestMethodFilter.getDescription()).thenReturn(BEFORE_DESCRIPTION);
        when(mDuringTestMethodFilter.getDescription()).thenReturn(DURING_DESCRIPTION);
        when(mBeforeTestMethodFilter.shouldPrint()).thenReturn(true);
        when(mDuringTestMethodFilter.shouldPrint()).thenReturn(true);
    }

    @After
    public void tearDown() {
        System.setErr(mSystemErr);
    }

    @Test
    public void testFilterChildrenIfNoFilter() {
        when(mBeforeTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(false);
        when(mBeforeTestMethodFilter.isIgnored(mSecondMethod, mTestParameters)).thenReturn(false);

        final List<FrameworkMethod> children = Arrays.asList(mFirstMethod, mSecondMethod);
        final List<FrameworkMethod> methods = mExecutionFilter.filterChildren(children, mTestParameters);

        assertThat(methods).containsExactly(mFirstMethod, mSecondMethod);
        verifyZeroInteractions(mTestInfoTransmitter);
    }

    @Test
    public void testFilterChildrenIfFirstFilter() {
        when(mBeforeTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(true);
        when(mBeforeTestMethodFilter.isIgnored(mSecondMethod, mTestParameters)).thenReturn(false);

        final List<FrameworkMethod> children = Arrays.asList(mFirstMethod, mSecondMethod);
        final List<FrameworkMethod> methods = mExecutionFilter.filterChildren(children, mTestParameters);

        assertThat(methods).containsExactly(mSecondMethod);
        final String transmitterMessage = String.format(
                "FRANKENSTEIN IGNORED AUTOTEST: {\"reason\":\"%s\",\"case id\":%d}",
                BEFORE_DESCRIPTION, FIRST_TEST_CASE_ID);
        verify(mErr).println(transmitterMessage);
    }

    @Test
    public void testFilterChildrenIfSecondFilter() {
        when(mBeforeTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(false);
        when(mBeforeTestMethodFilter.isIgnored(mSecondMethod, mTestParameters)).thenReturn(true);

        final List<FrameworkMethod> children = Arrays.asList(mFirstMethod, mSecondMethod);
        final List<FrameworkMethod> methods = mExecutionFilter.filterChildren(children, mTestParameters);

        assertThat(methods).containsExactly(mFirstMethod);
        final String transmitterMessage = String.format(
                "FRANKENSTEIN IGNORED AUTOTEST: {\"reason\":\"%s\",\"case id\":%d}",
                BEFORE_DESCRIPTION, SECOND_TEST_CASE_ID);
        verify(mErr).println(transmitterMessage);
    }

    @Test
    public void testFilterChildrenIfAllFilter() {
        when(mBeforeTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(true);
        when(mBeforeTestMethodFilter.isIgnored(mSecondMethod, mTestParameters)).thenReturn(true);

        final List<FrameworkMethod> children = Arrays.asList(mFirstMethod, mSecondMethod);
        final List<FrameworkMethod> methods = mExecutionFilter.filterChildren(children, mTestParameters);

        assertThat(methods).isEmpty();

        final String firstTransmitterMessage =
                String.format("FRANKENSTEIN IGNORED AUTOTEST: {\"reason\":\"%s\",\"case id\":%d}",
                BEFORE_DESCRIPTION, FIRST_TEST_CASE_ID);
        final String secondTransmitterMessage =
                String.format("FRANKENSTEIN IGNORED AUTOTEST: {\"reason\":\"%s\",\"case id\":%d}",
                BEFORE_DESCRIPTION, SECOND_TEST_CASE_ID);

        verify(mErr).println(firstTransmitterMessage);
        verify(mErr).println(secondTransmitterMessage);
    }

    @Test
    public void testIsIgnoredIfIgnored() {
        when(mDuringTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(true);

        final boolean isIgnored = mExecutionFilter.isIgnored(mFirstMethod, mTestParameters, false);

        assertThat(isIgnored).isTrue();
        final String transmitterMessage =
                String.format("FRANKENSTEIN IGNORED AUTOTEST: {\"reason\":\"%s\",\"case id\":%d}",
                DURING_DESCRIPTION, FIRST_TEST_CASE_ID);
        verify(mErr).println(transmitterMessage);
    }

    @Test
    public void testIsIgnoredIfNotIgnored() {
        when(mDuringTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(false);

        final boolean isIgnored = mExecutionFilter.isIgnored(mFirstMethod, mTestParameters, false);

        assertThat(isIgnored).isFalse();
        verifyZeroInteractions(mTestInfoTransmitter);
    }

    @Test
    public void testIsIgnoredIfIgnoredByRunner() {
        final boolean isIgnored = mExecutionFilter.isIgnored(mFirstMethod, mTestParameters, true);

        assertThat(isIgnored).isTrue();
        verifyZeroInteractions(mTestInfoTransmitter);
    }

    @Test
    public void testFilterChildrenIfShouldNotPrint() {
        when(mBeforeTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(true);
        when(mBeforeTestMethodFilter.shouldPrint()).thenReturn(false);

        final List<FrameworkMethod> children = Collections.singletonList(mFirstMethod);
        final List<FrameworkMethod> methods = mExecutionFilter.filterChildren(children, mTestParameters);

        assertThat(methods).isEmpty();
        verifyZeroInteractions(mErr);
    }

    @Test
    public void testIsIgnoredIfShouldNotPrint() {
        when(mDuringTestMethodFilter.isIgnored(mFirstMethod, mTestParameters)).thenReturn(true);
        when(mDuringTestMethodFilter.shouldPrint()).thenReturn(false);

        final boolean isIgnored = mExecutionFilter.isIgnored(mFirstMethod, mTestParameters, false);

        assertThat(isIgnored).isTrue();
        verifyZeroInteractions(mErr);
    }
}
