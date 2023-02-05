package com.yandex.frankenstein.runner;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ExecutorFactoryHolderTest {

    @Mock private static TestExecutor.Factory sExecutorFactoryDelegate;

    @Mock private TestExecutor mTestExecutor;
    @Mock private RunnerWithExecutorFactory mRunnerWithExecutorFactory;
    @Mock private RunnerWithoutExecutorFactory mRunnerWithoutExecutorFactory;
    @Mock private RunnerWithPrivateExecutorFactory mRunnerWithPrivateExecutorFactory;
    @Mock private RunnerWithAbstractExecutorFactory mRunnerWithAbstractExecutorFactory;
    @Mock private List<Object> mParameters;

    private final ExecutorFactoryHolder mExecutorFactoryHolder = new ExecutorFactoryHolder();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateTestExecutor() {
        when(sExecutorFactoryDelegate.createTestExecutor(Collections.emptyList())).thenReturn(mTestExecutor);

        final TestExecutor actualTestExecutor =
                mExecutorFactoryHolder.createTestExecutor(mRunnerWithExecutorFactory);

        assertThat(actualTestExecutor).isEqualTo(mTestExecutor);
    }

    @Test
    public void testCreateTestExecutorWithParameters() {
        when(sExecutorFactoryDelegate.createTestExecutor(mParameters)).thenReturn(mTestExecutor);

        final TestExecutor actualExecutor =
                mExecutorFactoryHolder.createTestExecutor(mRunnerWithExecutorFactory, mParameters);

        assertThat(actualExecutor).isEqualTo(mTestExecutor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTestExecutorWithoutExecutorFactory() {
        mExecutorFactoryHolder.createTestExecutor(mRunnerWithoutExecutorFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateTestExecutorWithPrivateExecutorFactory() {
        mExecutorFactoryHolder.createTestExecutor(mRunnerWithPrivateExecutorFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateTestExecutorWithAbstractExecutorFactory() {
        mExecutorFactoryHolder.createTestExecutor(mRunnerWithAbstractExecutorFactory);
    }

    private static class PrivateTestExecutorFactory extends TestExecutorFactoryDelegation {}

    @SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
    abstract static class AbstractTestExecutorFactory extends TestExecutorFactoryDelegation {}

    static class TestExecutorFactoryDelegation implements TestExecutor.Factory {

        @NotNull
        @Override
        public TestExecutor createTestExecutor(@NotNull List<?> testParameters) {
            return sExecutorFactoryDelegate.createTestExecutor(testParameters);
        }
    }

    @TestExecutorFactory(TestExecutorFactoryDelegation.class)
    private static class RunnerWithExecutorFactory extends RunnerWithoutExecutorFactory {}

    @TestExecutorFactory(PrivateTestExecutorFactory.class)
    private static class RunnerWithPrivateExecutorFactory extends RunnerWithoutExecutorFactory {}

    @TestExecutorFactory(AbstractTestExecutorFactory.class)
    private static class RunnerWithAbstractExecutorFactory extends RunnerWithoutExecutorFactory {}

    private static class RunnerWithoutExecutorFactory extends Runner {

        @Override
        public Description getDescription() {
            return null;
        }

        @Override
        public void run(final RunNotifier notifier) {}
    }
}
