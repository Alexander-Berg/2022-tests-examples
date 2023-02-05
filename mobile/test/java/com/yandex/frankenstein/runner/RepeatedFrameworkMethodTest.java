package com.yandex.frankenstein.runner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RepeatedFrameworkMethodTest {

    private static final String METHOD_NAME = "methodName";
    private static final int REPETITION = 42;

    private @Mock Method mMethod;
    private @Mock Method mAnotherMethod;

    private RepeatedFrameworkMethod mRepeatedMethod;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mRepeatedMethod = new RepeatedFrameworkMethod(mMethod, REPETITION);
    }

    @Test
    public void testGetName() {
        when(mMethod.getName()).thenReturn(METHOD_NAME);

        final String expectedName = METHOD_NAME + "[repetition = " + REPETITION + "]";

        assertThat(mRepeatedMethod.getName()).isEqualTo(expectedName);
    }

    @Test
    public void testEqualsWithString() {
        assertThat(mRepeatedMethod).isNotEqualTo(METHOD_NAME);
    }

    @Test
    public void testEqualsWithFrameworkMethod() {
        final FrameworkMethod frameworkMethod = new FrameworkMethod(mMethod);

        assertThat(mRepeatedMethod).isNotEqualTo(frameworkMethod);
    }

    @Test
    public void testEqualsWithAnotherMethod() {
        final Object anotherRepeatedMethod = new RepeatedFrameworkMethod(mAnotherMethod, REPETITION);

        assertThat(mRepeatedMethod).isNotEqualTo(anotherRepeatedMethod);
    }

    @Test
    public void testEqualsWithAnotherRepetition() {
        final Object anotherRepeatedMethod = new RepeatedFrameworkMethod(mMethod, 100);

        assertThat(mRepeatedMethod).isNotEqualTo(anotherRepeatedMethod);
    }

    @Test
    public void testEquals() {
        final Object anotherRepeatedMethod = new RepeatedFrameworkMethod(mMethod, REPETITION);

        assertThat(mRepeatedMethod).isEqualTo(anotherRepeatedMethod);
    }

    @Test
    public void testHashCode() {
        final int expectedHashCode = Objects.hash(mMethod.hashCode(), REPETITION);

        assertThat(mRepeatedMethod.hashCode()).isEqualTo(expectedHashCode);
    }
}
