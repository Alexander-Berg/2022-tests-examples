package com.yandex.frankenstein.runner.listener;

import com.yandex.frankenstein.annotations.handlers.IgnoredTestInfoEncoder;
import com.yandex.frankenstein.annotations.handlers.TestCaseIdHandler;
import com.yandex.frankenstein.annotations.handlers.TestInfoEncoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintStream;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestInfoRunListenerTest {

    private static final int TEST_CASE_ID = 42;
    private static final String TEST_INFO = "Test Info";
    private static final String REASON = "some reason";

    private TestInfoRunListener mListener;
    private final PrintStream mSystemErr = System.err;

    @Mock private PrintStream mErr;
    @Mock private Description mDescription;
    @Mock private FrameworkMethod mFrameworkMethod;
    @Mock private TestInfoEncoder mTestInfoEncoder;
    @Mock private IgnoredTestInfoEncoder mIgnoredTestInfoEncoder;
    @Mock private List<Object> mTestParameters;
    @Mock private TestCaseIdHandler mTestCaseIdHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        System.setErr(mErr);

        when(mTestInfoEncoder.encode(TEST_CASE_ID, mDescription)).thenReturn(TEST_INFO);
        when(mIgnoredTestInfoEncoder.encode(TEST_CASE_ID, REASON)).thenReturn(REASON);

        when(mTestCaseIdHandler.getTestCaseId(mDescription, mTestParameters)).thenReturn(TEST_CASE_ID);
        when(mTestCaseIdHandler.getTestCaseId(mFrameworkMethod, mTestParameters)).thenReturn(TEST_CASE_ID);

        mListener = new TestInfoRunListener(mTestInfoEncoder, mIgnoredTestInfoEncoder,
                mTestCaseIdHandler, mTestParameters);
    }

    @After
    public void tearDown() {
        System.setErr(mSystemErr);
    }

    @Test
    public void testStarted() {
        mListener.testStarted(mDescription);
        verify(mErr).println("FRANKENSTEIN AUTOTEST: " + TEST_INFO);
    }

    @Test
    public void testIgnoredDescription() {
        mListener.testIgnored(mDescription);
        verify(mErr).println("FRANKENSTEIN AUTOTEST: " + TEST_INFO);
    }

    @Test
    public void testIgnoredFrameworkMethod() {
        mListener.testIgnored(mFrameworkMethod, REASON);
        verify(mErr).println("FRANKENSTEIN IGNORED AUTOTEST: " + REASON);
    }
}
