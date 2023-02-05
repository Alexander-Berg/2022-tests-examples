package com.yandex.frankenstein.runner;

import com.yandex.frankenstein.annotations.TestCaseId;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ConfigurableRunnerTest {

    private final ConfigurableRunner mDefaultRunner = new ConfigurableRunner() {};

    private final ConfigurableRunner mCustomRunner = new ConfigurableRunner() {

        @NotNull
        @Override
        public Collection<Class<? extends Annotation>> getTestMethodAnnotations() {
            return Arrays.asList(Test.class, TestCaseId.class);
        }
    };

    @Mock private TestClass mTestClass;
    @Mock private FrameworkMethod mMethodWithTest;
    @Mock private FrameworkMethod mMethodWithTestCaseId;
    @Mock private FrameworkMethod mMethodWithTestAndTestCaseId;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mTestClass.getAnnotatedMethods(Test.class))
                .thenReturn(Arrays.asList(mMethodWithTest, mMethodWithTestAndTestCaseId));

        when(mTestClass.getAnnotatedMethods(TestCaseId.class))
                .thenReturn(Arrays.asList(mMethodWithTestCaseId, mMethodWithTestAndTestCaseId));
    }

    @Test
    public void testGetDefaultTestMethodAnnotations() {
        assertThat(mDefaultRunner.getTestMethodAnnotations()).containsExactly(Test.class);
    }

    @Test
    public void testComputeDefaultTestMethods() {
        assertThat(mDefaultRunner.computeTestMethods(mTestClass))
                .containsExactlyInAnyOrder(mMethodWithTest, mMethodWithTestAndTestCaseId);
    }

    @Test
    public void testComputeCustomTestMethods() {
        assertThat(mCustomRunner.computeTestMethods(mTestClass))
                .containsExactlyInAnyOrder(mMethodWithTest, mMethodWithTestCaseId, mMethodWithTestAndTestCaseId);
    }
}
