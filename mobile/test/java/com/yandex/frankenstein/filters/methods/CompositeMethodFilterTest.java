package com.yandex.frankenstein.filters.methods;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CompositeMethodFilterTest {

    private static final String FIRST_FILTER_DESCRIPTION = "first filter description";
    private static final String SECOND_FILTER_DESCRIPTION = "second filter description";

    @Mock private MethodFilter mFirstTestMethodFilter;
    @Mock private MethodFilter mSecondTestMethodFilter;

    @Mock private FrameworkMethod mMethod;
    @Mock private List<Object> mTestParameters;

    private CompositeMethodFilter mCompositeIgnoranceFilter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mFirstTestMethodFilter.getDescription()).thenReturn(FIRST_FILTER_DESCRIPTION);
        when(mSecondTestMethodFilter.getDescription()).thenReturn(SECOND_FILTER_DESCRIPTION);

        when(mFirstTestMethodFilter.shouldPrint()).thenReturn(true);
        when(mSecondTestMethodFilter.shouldPrint()).thenReturn(true);

        mCompositeIgnoranceFilter = new CompositeMethodFilter(mFirstTestMethodFilter, mSecondTestMethodFilter);
    }

    @Test
    public void testEmptyCompositeFilter() {
        final CompositeMethodFilter compositeIgnoranceFilter = new CompositeMethodFilter();
        final boolean isIgnored = compositeIgnoranceFilter.isIgnored(mMethod, mTestParameters);

        assertThat(isIgnored).isFalse();
    }

    @Test
    public void testIfDoNotCallFilter() {
        assertThat(mCompositeIgnoranceFilter.getDescription()).isEmpty();
        assertThat(mCompositeIgnoranceFilter.shouldPrint()).isFalse();
    }

    @Test
    public void testIfNoneIsIgnored() {
        when(mFirstTestMethodFilter.isIgnored(any(), any())).thenReturn(false);
        when(mSecondTestMethodFilter.isIgnored(any(), any())).thenReturn(false);

        assertThat(mCompositeIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isFalse();
        assertThat(mCompositeIgnoranceFilter.getDescription()).isEmpty();
        assertThat(mCompositeIgnoranceFilter.shouldPrint()).isFalse();
    }

    @Test
    public void testIfIgnoredByFirst() {
        when(mFirstTestMethodFilter.isIgnored(any(), any())).thenReturn(true);
        when(mSecondTestMethodFilter.isIgnored(any(), any())).thenReturn(false);

        assertThat(mCompositeIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isTrue();
        assertThat(mCompositeIgnoranceFilter.getDescription()).isEqualTo(FIRST_FILTER_DESCRIPTION);
        assertThat(mCompositeIgnoranceFilter.shouldPrint()).isTrue();
    }

    @Test
    public void testIfIgnoredBySecond() {
        when(mFirstTestMethodFilter.isIgnored(any(), any())).thenReturn(false);
        when(mSecondTestMethodFilter.isIgnored(any(), any())).thenReturn(true);

        assertThat(mCompositeIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isTrue();
        assertThat(mCompositeIgnoranceFilter.getDescription()).isEqualTo(SECOND_FILTER_DESCRIPTION);
        assertThat(mCompositeIgnoranceFilter.shouldPrint()).isTrue();
    }

    @Test
    public void testIfFilteredByMany() {
        when(mFirstTestMethodFilter.isIgnored(any(), any())).thenReturn(true);
        when(mSecondTestMethodFilter.isIgnored(any(), any())).thenReturn(true);

        assertThat(mCompositeIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isTrue();
        assertThat(mCompositeIgnoranceFilter.getDescription())
                .isEqualTo(FIRST_FILTER_DESCRIPTION + System.lineSeparator() + SECOND_FILTER_DESCRIPTION);
        assertThat(mCompositeIgnoranceFilter.shouldPrint()).isTrue();
    }

    @Test
    public void testIfIgnoredByFilterThatShouldNotPrint() {
        when(mFirstTestMethodFilter.isIgnored(any(), any())).thenReturn(true);
        when(mFirstTestMethodFilter.shouldPrint()).thenReturn(false);
        when(mSecondTestMethodFilter.isIgnored(any(), any())).thenReturn(true);

        assertThat(mCompositeIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isTrue();
        assertThat(mCompositeIgnoranceFilter.getDescription())
                .isEqualTo(SECOND_FILTER_DESCRIPTION);
        assertThat(mCompositeIgnoranceFilter.shouldPrint()).isTrue();
    }

    @Test
    public void testIfIgnoredOnlyByFilterThatShouldNotPrint() {
        when(mFirstTestMethodFilter.isIgnored(any(), any())).thenReturn(true);
        when(mFirstTestMethodFilter.shouldPrint()).thenReturn(false);
        when(mSecondTestMethodFilter.isIgnored(any(), any())).thenReturn(false);

        assertThat(mCompositeIgnoranceFilter.isIgnored(mMethod, mTestParameters)).isTrue();
        assertThat(mCompositeIgnoranceFilter.getDescription()).isEmpty();
        assertThat(mCompositeIgnoranceFilter.shouldPrint()).isFalse();
    }
}
