package com.yandex.frankenstein.steps;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CompositeStepTest {

    private final Step mFirstStep = mock(Step.class);
    private final Step mSecondStep = mock(Step.class);
    private final CompositeStep mCompositeStep = new CompositeStep(mFirstStep, mSecondStep);

    @Test
    public void before() throws Throwable {
        mCompositeStep.before();

        verify(mFirstStep).before();
        verify(mSecondStep).before();
    }

    @Test
    public void after() {
        mCompositeStep.after();

        verify(mFirstStep).after();
        verify(mSecondStep).after();
    }
}
