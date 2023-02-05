package com.yandex.frankenstein.filters.methods;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DefinitionMethodFilterTest {

    @Mock private TestCasesMethodFilter mFilter;
    @Mock private FrameworkMethod mMethod;
    @Mock private List<Object> mTestParameters;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsIgnored() {
        when(mFilter.isIgnored(mMethod, mTestParameters)).thenReturn(true);

        final DefinitionMethodFilter filter = new DefinitionMethodFilter(mFilter);
        assertThat(filter.isIgnored(mMethod, mTestParameters)).isTrue();
    }

    @Test
    public void testShouldPrintIfTrue() {
        when(mFilter.shouldPrint()).thenReturn(true);

        final DefinitionMethodFilter filter = new DefinitionMethodFilter(mFilter);
        assertThat(filter.shouldPrint()).isTrue();
    }

    @Test
    public void testShouldPrintIfFalse() {
        when(mFilter.shouldPrint()).thenReturn(false);

        final DefinitionMethodFilter filter = new DefinitionMethodFilter(mFilter);
        assertThat(filter.shouldPrint()).isFalse();
    }

    @Test
    public void testGetDescription() {
        when(mFilter.getDescription()).thenReturn("description");

        final DefinitionMethodFilter filter = new DefinitionMethodFilter(mFilter);
        assertThat(filter.getDescription()).isEqualTo("description");
    }
}
