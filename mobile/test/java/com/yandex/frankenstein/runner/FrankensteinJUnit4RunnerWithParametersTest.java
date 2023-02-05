package com.yandex.frankenstein.runner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.TestWithParameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FrankensteinJUnit4RunnerWithParametersTest {

    private static final String NAME = "testName";

    @Mock private ExecutorFactoryHolder mExecutorFactoryHolder;
    @Mock private TestExecutor mTestExecutor;

    private FrankensteinJUnit4RunnerWithParameters mFrankensteinRunnerWithParameters;

    @Mock private TestClass mTestClass;
    @Mock private List<Object> mParameters;
    @Mock private List<FrameworkMethod> mFilteredChildren;
    @Mock private FrameworkMethod mMethod;
    @Mock private RunNotifier mNotifier;
    @Mock private Object mTarget;

    @Before
    public void setUp() throws InitializationError {
        MockitoAnnotations.initMocks(this);

        when(mTestExecutor.getChildren(any())).thenReturn(mFilteredChildren);
        when(mExecutorFactoryHolder.createTestExecutor(any(), eq(mParameters))).thenReturn(mTestExecutor);

        //noinspection unchecked
        when(mTestClass.getJavaClass()).thenReturn((Class) getClass());

        final TestWithParameters testWithParameters = mock(TestWithParameters.class);
        when(testWithParameters.getTestClass()).thenReturn(mTestClass);
        when(testWithParameters.getParameters()).thenReturn(mParameters);
        when(testWithParameters.getName()).thenReturn(NAME);

        mFrankensteinRunnerWithParameters =
                new FrankensteinJUnit4RunnerWithParameters(mExecutorFactoryHolder, testWithParameters);
    }

    @Test
    public void testGetChildren() {
        final List<FrameworkMethod> actualFilteredChildren = mFrankensteinRunnerWithParameters.getChildren();

        assertThat(actualFilteredChildren).isEqualTo(mFilteredChildren);
    }

    @Test
    public void testRunChild() {
        mFrankensteinRunnerWithParameters.runChild(mMethod, mNotifier);

        verify(mTestExecutor).runChild(eq(mMethod), any(), eq(mNotifier), anyBoolean(), any());
    }

    @Test
    public void testGetTestRules() {
        mFrankensteinRunnerWithParameters.getTestRules(mTarget);

        verify(mTestExecutor).getTestRules(any());
    }
}
