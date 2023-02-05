package com.yandex.mail.ads.util;

import com.yandex.mail.ui.presenters.AdsContainerPresenter;

import org.junit.Before;
import org.junit.Test;

import androidx.annotation.NonNull;

import static com.yandex.mail.maillist.EmailListFragment.LIST_TOP_POSITION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ReloadAdOnStopListenerTest {

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private AdsContainerPresenter presenter;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private ReloadAdOnStopListener reloadAdOnStopListener;

    @Before
    public void beforeEachTest() {
        presenter = mock(AdsContainerPresenter.class);
        reloadAdOnStopListener = new ReloadAdOnStopListener(presenter);
    }

    @Test
    public void constructor_shouldNoOp() {
        verifyNoInteractions(presenter);
    }

    @Test
    public void onStoppedOnAd_shouldReloadTopAdOnFirstCall() {
        reloadAdOnStopListener.onStoppedOnAd(LIST_TOP_POSITION);
        verify(presenter).refreshAd();
    }

    @Test
    public void onStoppedOnAd_shouldNotReloadTopAdOnSecondCall() {
        reloadAdOnStopListener.onStoppedOnAd(LIST_TOP_POSITION);
        reloadAdOnStopListener.onStoppedOnAd(LIST_TOP_POSITION);
        verify(presenter).refreshAd();
    }

    @Test
    public void onStoppedOnAd_shouldReloadMiddleAdCache_onStoppingOnTopAd() {
        reloadAdOnStopListener.onStoppedOnAd(LIST_TOP_POSITION);
        verify(presenter).refreshAd();
    }

    @Test
    public void reset() {
        reloadAdOnStopListener.onStoppedOnAd(LIST_TOP_POSITION);
        reloadAdOnStopListener.onResetState();
        reloadAdOnStopListener.onStoppedOnAd(LIST_TOP_POSITION);
        verify(presenter, times(2)).refreshAd();
    }
}
