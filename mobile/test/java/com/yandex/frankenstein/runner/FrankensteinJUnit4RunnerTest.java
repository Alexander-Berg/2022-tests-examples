package com.yandex.frankenstein.runner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FrankensteinJUnit4RunnerTest {

    @Mock private ExecutorFactoryHolder mExecutorFactoryHolder;
    @Mock private TestExecutor mTestExecutor;

    private FrankensteinJUnit4Runner mFrankensteinRunner;

    @Mock private List<FrameworkMethod> mFilteredChildren;
    @Mock private FrameworkMethod mMethod;
    @Mock private RunNotifier mNotifier;
    @Mock private Object mTarget;

    @Before
    public void setUp() throws InitializationError {
        MockitoAnnotations.initMocks(this);

        when(mTestExecutor.getChildren(anyList())).thenReturn(mFilteredChildren);
        when(mExecutorFactoryHolder.createTestExecutor(any())).thenReturn(mTestExecutor);
        mFrankensteinRunner = new FrankensteinJUnit4Runner(mExecutorFactoryHolder, getClass());
    }

    @Test
    public void testGetChildren() {
        final List<FrameworkMethod> actualFilteredChildren = mFrankensteinRunner.getChildren();

        assertThat(actualFilteredChildren).isEqualTo(mFilteredChildren);
    }

    @Test
    public void testRunChild() {
        mFrankensteinRunner.runChild(mMethod, mNotifier);

        verify(mTestExecutor).runChild(eq(mMethod), any(), eq(mNotifier), anyBoolean(), any());
    }

    @Test
    public void testGetTestRules() {
        mFrankensteinRunner.getTestRules(mTarget);

        verify(mTestExecutor).getTestRules(any());
    }
}
