package com.yandex.frankenstein.filters.methods;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ParametrizedDataMethodFilterTest {

    @Mock private Predicate<List<Object>> mMatcher;

    @Mock private FrameworkMethod mMethod;
    @Mock private List<Object> mTestParameters;

    private String mDescription = "some description";
    private ParametrizedDataMethodFilter mParametrizedDataIgnoranceFilter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mParametrizedDataIgnoranceFilter = new ParametrizedDataMethodFilter(mMatcher, mDescription);
    }

    @Test
    public void testGetDescription() {
        final ParametrizedDataMethodFilter parametrizedDataIgnoranceFilter =
                new ParametrizedDataMethodFilter(mMatcher, mDescription);
        assertThat(parametrizedDataIgnoranceFilter.getDescription()).isEqualTo(mDescription);
    }

    @Test
    public void testShouldPrint() {
        final ParametrizedDataMethodFilter parametrizedDataIgnoranceFilter =
                new ParametrizedDataMethodFilter(mMatcher, mDescription);
        assertThat(parametrizedDataIgnoranceFilter.shouldPrint()).isTrue();
    }

    @Test
    public void testIsIgnored() {
        when(mMatcher.test(mTestParameters)).thenReturn(true);

        assertThat(mParametrizedDataIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isTrue();
    }

    @Test
    public void testIsIgnoredIfDoesNotIgnored() {
        when(mMatcher.test(mTestParameters)).thenReturn(false);

        assertThat(mParametrizedDataIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isFalse();
    }
}
