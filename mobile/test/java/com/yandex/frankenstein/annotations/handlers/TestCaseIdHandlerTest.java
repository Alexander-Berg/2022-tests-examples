package com.yandex.frankenstein.annotations.handlers;

import com.yandex.frankenstein.annotations.TestCaseId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCaseIdHandlerTest {

    private static final int PARAMETERS_TEST_CASE_ID = 7;
    private static final int METHOD_TEST_CASE_ID = 42;
    private static final int CLASS_TEST_CASE_ID = 100;

    private final TestCaseIdHandler mTestCaseIdHandler = new TestCaseIdHandler();
    private final FrameworkMethod mFrameworkMethod = mock(FrameworkMethod.class);
    private final TestCaseId mTestCaseIdAnnotation = mock(TestCaseId.class);

    @Before
    public void setUp() {
        when(mFrameworkMethod.getAnnotations()).thenReturn(new Annotation[] {mTestCaseIdAnnotation});
        when(mTestCaseIdAnnotation.value()).thenReturn(METHOD_TEST_CASE_ID);
        doReturn(TestCaseId.class).when(mTestCaseIdAnnotation).annotationType();
    }

    @Test
    public void testGetTestCaseIdFromTestParameters() {
        final List<Object> testParameters = Collections.singletonList(String.valueOf(PARAMETERS_TEST_CASE_ID));
        final int actualTestCaseId = mTestCaseIdHandler.getTestCaseId(mFrameworkMethod, testParameters);

        assertThat(actualTestCaseId).isEqualTo(PARAMETERS_TEST_CASE_ID);
    }

    @Test
    public void testGetTestCaseIdFromMethodAnnotationIfFirstParameterIsStringWithoutInteger() {
        when(mFrameworkMethod.getAnnotation(TestCaseId.class)).thenReturn(mTestCaseIdAnnotation);

        final List<Object> testParameters = Collections.singletonList("whatever");
        final int actualTestCaseId = mTestCaseIdHandler.getTestCaseId(mFrameworkMethod, testParameters);

        assertThat(actualTestCaseId).isEqualTo(METHOD_TEST_CASE_ID);
    }

    @Test
    public void testGetTestCaseIdFromMethodAnnotationIfFirstParameterIsNotString() {
        when(mFrameworkMethod.getAnnotation(TestCaseId.class)).thenReturn(mTestCaseIdAnnotation);

        final List<Object> testParameters = Collections.singletonList(PARAMETERS_TEST_CASE_ID);
        final int actualTestCaseId = mTestCaseIdHandler.getTestCaseId(mFrameworkMethod, testParameters);

        assertThat(actualTestCaseId).isEqualTo(METHOD_TEST_CASE_ID);
    }

    @Test
    public void testGetTestCaseIdFromMethodAnnotationWithoutParameters() {
        when(mFrameworkMethod.getAnnotation(TestCaseId.class)).thenReturn(mTestCaseIdAnnotation);

        final List<Object> testParameters = Collections.emptyList();
        final int actualTestCaseId = mTestCaseIdHandler.getTestCaseId(mFrameworkMethod, testParameters);

        assertThat(actualTestCaseId).isEqualTo(METHOD_TEST_CASE_ID);
    }

    @Test
    public void testGetTestCaseIdFromClassAnnotation() {
        when(mFrameworkMethod.getAnnotations()).thenReturn(new Annotation[] {});
        doReturn(WithTestCaseId.class).when(mFrameworkMethod).getDeclaringClass();

        final List<Object> testParameters = Collections.emptyList();
        final int actualTestCaseId = mTestCaseIdHandler.getTestCaseId(mFrameworkMethod, testParameters);

        assertThat(actualTestCaseId).isEqualTo(CLASS_TEST_CASE_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testAbsentTestCaseId() {
        when(mFrameworkMethod.getAnnotations()).thenReturn(new Annotation[] {});
        doReturn(WithoutTestCaseId.class).when(mFrameworkMethod).getDeclaringClass();

        final List<Object> testParameters = Collections.emptyList();
        mTestCaseIdHandler.getTestCaseId(mFrameworkMethod, testParameters);
    }

    @TestCaseId(CLASS_TEST_CASE_ID)
    private static class WithTestCaseId {}

    private static class WithoutTestCaseId {}
}
