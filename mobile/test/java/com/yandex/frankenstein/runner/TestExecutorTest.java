package com.yandex.frankenstein.runner;

import com.yandex.frankenstein.annotations.handlers.IgnoredTestInfoEncoder;
import com.yandex.frankenstein.annotations.handlers.TestCaseIdHandler;
import com.yandex.frankenstein.annotations.handlers.TestInfoEncoder;
import com.yandex.frankenstein.filters.ExecutionFilter;
import com.yandex.frankenstein.runner.listener.TestInfoRunListener;
import com.yandex.frankenstein.steps.MockWebServerStepProvider;
import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestExecutorTest {

    private static final boolean IGNORED_BY_RUNNER = false;
    private static final int TEST_CASE_ID = 42;
    private static final String TEST_INFO = "Test Info";
    private static final String KEY_REPETITIONS_COUNT = "frankenstein.test.repetitions.count";
    private static final String KEY_TIMEOUT_SECONDS = "frankenstein.test.timeout.seconds";

    private final PrintStream mSystemErr = System.err;

    @Mock private PrintStream mErr;
    @Mock private TestInfoEncoder mTestInfoEncoder;
    @Mock private List<Object> mTestParameters;
    @Mock private ExecutionFilter mExecutionFilter;
    @Mock private TestExecutorDelegate mDelegate;
    @Mock private TestCaseIdHandler mTestCaseIdHandler;
    @Mock private MockWebServerStepProvider mockWebServerStepProvider;

    @Mock private Method mFirstMethod;
    @Mock private Method mSecondMethod;
    @Mock private Method mThirdMethod;

    private List<FrameworkMethod> mChildren;
    private List<FrameworkMethod> mFilteredChildren;

    private TestExecutor mTestExecutor;
    private final RunNotifier mNotifier = new RunNotifier();

    @Mock private FrameworkMethod mMethod;
    @Mock private Description mDescription;
    @Mock private RunNotifier mMockNotifier;
    @Mock private Statement mStatement;

    @Captor private ArgumentCaptor<Failure> mFailureCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        System.setErr(mErr);

        mChildren = Arrays.asList(
                new FrameworkMethod(mFirstMethod),
                new FrameworkMethod(mSecondMethod),
                new FrameworkMethod(mThirdMethod)
        );
        mFilteredChildren = mChildren.subList(1, mChildren.size());

        when(mTestInfoEncoder.encode(TEST_CASE_ID, mDescription)).thenReturn(TEST_INFO);
        when(mExecutionFilter.isIgnored(mMethod, mTestParameters, IGNORED_BY_RUNNER)).thenReturn(false);
        when(mExecutionFilter.filterChildren(mChildren, mTestParameters)).thenReturn(mFilteredChildren);
        when(mTestCaseIdHandler.getTestCaseId(mDescription, mTestParameters)).thenReturn(TEST_CASE_ID);
        when(mTestCaseIdHandler.getTestCaseId(mMethod, mTestParameters)).thenReturn(TEST_CASE_ID);

        mTestExecutor = new TestExecutor(
                mTestParameters,
                mExecutionFilter,
                mDelegate,
                Collections.singletonList(new TestInfoRunListener(
                        mTestInfoEncoder,
                        new IgnoredTestInfoEncoder(),
                        mTestCaseIdHandler,
                        mTestParameters
                )),
                mockWebServerStepProvider
        );
    }

    @After
    public void tearDown() {
        System.clearProperty(KEY_TIMEOUT_SECONDS);
        System.clearProperty(KEY_REPETITIONS_COUNT);
        System.setErr(mSystemErr);
    }

    @Test
    public void testGetChildren() {
        final List<FrameworkMethod> actualChildren = mTestExecutor.getChildren(mChildren);

        assertThat(actualChildren).containsExactlyElementsOf(mFilteredChildren);
    }

    @Test
    public void testGetChildrenWithRepetitions() {
        System.setProperty(KEY_REPETITIONS_COUNT, "3");
        final List<FrameworkMethod> actualChildren = mTestExecutor.getChildren(mChildren);

        assertThat(actualChildren)
                .extracting(FrameworkMethod::getMethod)
                .containsExactly(
                        mSecondMethod, mSecondMethod, mSecondMethod, mSecondMethod,
                        mThirdMethod, mThirdMethod, mThirdMethod, mThirdMethod
                );
    }

    @Test
    public void testAddAllListenersListener() {
        final List<RunListener> listeners = Arrays.asList(
                mock(RunListener.class)
        );
        final TestExecutor executor = new TestExecutor(
                mTestParameters,
                mExecutionFilter,
                mDelegate,
                listeners,
                mockWebServerStepProvider
        );
        executor.runChild(mMethod, mDescription, mMockNotifier, IGNORED_BY_RUNNER, mStatement);

        listeners.forEach((listener) -> verify(mMockNotifier).addListener(listener));
    }

    @Test
    public void testConstructorMakesCorrectListOfListeners() {
        final TestExecutor executor = new TestExecutor(
                mTestParameters,
                new DummyFilterProvider(),
                mDelegate,
                mockWebServerStepProvider
        );
        executor.runChild(mMethod, mDescription, mMockNotifier, IGNORED_BY_RUNNER, mStatement);

        verify(mMockNotifier).addListener(any(TestInfoRunListener.class));
    }

    @Test
    public void testPrepareTestExecutor() {
        mTestExecutor.runChild(mMethod, mDescription, mMockNotifier, IGNORED_BY_RUNNER, mStatement);

        verify(mDelegate).prepare(mMethod, mTestParameters, mDescription);
    }

    @Test
    public void testTransmitTestInfo() {
        mTestExecutor.runChild(mMethod, mDescription, mNotifier, IGNORED_BY_RUNNER, mStatement);

        verify(mErr).println("FRANKENSTEIN AUTOTEST: " + TEST_INFO);
    }

    @Test
    public void testRunChild() throws Throwable {
        mTestExecutor.runChild(mMethod, mDescription, mMockNotifier, IGNORED_BY_RUNNER, mStatement);

        verify(mMockNotifier).fireTestStarted(mDescription);
        verify(mMockNotifier).fireTestFinished(mDescription);

        verify(mStatement).evaluate();
    }

    @Test
    public void testRunIgnoredChild() {
        when(mExecutionFilter.isIgnored(mMethod, mTestParameters, IGNORED_BY_RUNNER)).thenReturn(true);

        mTestExecutor.runChild(mMethod, mDescription, mMockNotifier, IGNORED_BY_RUNNER, mStatement);

        verify(mMockNotifier).fireTestIgnored(mDescription);
    }

    @Test
    public void testRunChildWithFailedAssumption() throws Throwable {
        doThrow(AssumptionViolatedException.class).when(mStatement).evaluate();

        mTestExecutor.runChild(mMethod, mDescription, mMockNotifier, IGNORED_BY_RUNNER, mStatement);

        verify(mMockNotifier).fireTestAssumptionFailed(mFailureCaptor.capture());
        verify(mMockNotifier).fireTestFinished(mDescription);

        assertThat(mFailureCaptor.getValue().getException()).isInstanceOf(AssumptionViolatedException.class);
    }

    @Test
    public void testRunChildWithFailure() throws Throwable {
        doThrow(IllegalStateException.class).when(mStatement).evaluate();

        mTestExecutor.runChild(mMethod, mDescription, mMockNotifier, IGNORED_BY_RUNNER, mStatement);

        verify(mMockNotifier).fireTestFailure(mFailureCaptor.capture());
        verify(mMockNotifier).fireTestFinished(mDescription);

        assertThat(mFailureCaptor.getValue().getException()).isInstanceOf(IllegalStateException.class);
    }
}
