package com.yandex.mail.ads.util;

import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.yandex.mail.ads.util.OnAdsAppearsCompositeListener.OnAdAppearsListener;
import com.yandex.mail.runners.UnitTestRunner;
import com.yandex.mail.ui.utils.ListViewToCommonScrollListener;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import androidx.annotation.NonNull;

import static com.yandex.mail.provider.Constants.NO_ADS_POSITION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(UnitTestRunner.class)
public class OnAdsAppearsCompositeListenerTest {

    @SuppressWarnings("NullableProblems")
    @NonNull
    private AdsContainer adsContainer;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private OnAdsAppearsCompositeListener onAdsAppearsCompositeListener;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private OnAdAppearsListener adAppearsListener;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private ListView listView;

    @Test
    public void scroll_adNotVisible_fullDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);

        onAdsAppearsCompositeListener.onScroll(listView, 1, 3, 10);

        verify(adAppearsListener, never()).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adFullyVisible_fullDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adHalfVisible_fullDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(700);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener, never()).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adAlmostInvisible_fullDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(799);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener, never()).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adNotVisible_halfDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 50);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);

        onAdsAppearsCompositeListener.onScroll(listView, 1, 3, 10);

        verify(adAppearsListener, never()).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adFullyVisible_halfDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 50);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adHalfVisible_halfDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 50);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(700);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adAlmostInvisible_halfDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 50);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(799);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener, never()).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adNotVisible_zeroDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 0);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);

        onAdsAppearsCompositeListener.onScroll(listView, 1, 3, 10);

        verify(adAppearsListener, never()).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adFullyVisible_zeroDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 0);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adHalfVisible_zeroDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 0);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(700);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void scroll_adAlmostInvisible_zeroDesiredVisibility() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 0);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(799);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void overscroll_adWasVisible() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 20);

        when(listView.getHeight()).thenReturn(1800);

        onAdsAppearsCompositeListener.onScroll(listView, 14, 6, 20);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void overscrollAndScrollBack_adVisible() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 20);

        when(listView.getHeight()).thenReturn(1800);

        onAdsAppearsCompositeListener.onScroll(listView, 14, 6, 20);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        when(listView.getHeight()).thenReturn(800);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 20);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener, times(2)).onAdAppears(9);
        verify(adAppearsListener).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void scroll_containerHasNoAd() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(NO_ADS_POSITION));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener, never()).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, never()).onStoppedOnAd(9);
    }

    @Test
    public void refreshState_adAppearsAfterRefreshState() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);

        onAdsAppearsCompositeListener.resetState();

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener, times(2)).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, times(2)).onStoppedOnAd(9);
    }

    @Test
    public void refreshState_adDisappearsAfterRefreshState() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        listView.setOnScrollListener(new ListViewToCommonScrollListener(onAdsAppearsCompositeListener));

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 20);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);

        onAdsAppearsCompositeListener.resetState();

        onAdsAppearsCompositeListener.onScroll(listView, 14, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }

    @Test
    public void forceFire_withRefreshState() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        ListViewToCommonScrollListener listViewToCommonScrollListener = new ListViewToCommonScrollListener(onAdsAppearsCompositeListener);
        listView.setOnScrollListener(listViewToCommonScrollListener);

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);
        when(listView.getFirstVisiblePosition()).thenReturn(4);
        when(listView.getChildCount()).thenReturn(6);
        ListAdapter adapter = mock(ListAdapter.class);
        when(adapter.getCount()).thenReturn(10);
        when(listView.getAdapter()).thenReturn(adapter);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);

        onAdsAppearsCompositeListener.resetState();

        listViewToCommonScrollListener.forceInvoke(listView);

        verify(adAppearsListener, times(2)).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener, times(2)).onStoppedOnAd(9);
    }

    @Test
    public void forceFire_withoutRefreshState() {
        adsContainer = mock(AdsContainer.class);
        onAdsAppearsCompositeListener = new OnAdsAppearsCompositeListener(adsContainer, 100);
        adAppearsListener = mock(OnAdAppearsListener.class);
        onAdsAppearsCompositeListener.add(adAppearsListener);
        listView = mock(ListView.class);
        ListViewToCommonScrollListener listViewToCommonScrollListener = new ListViewToCommonScrollListener(onAdsAppearsCompositeListener);
        listView.setOnScrollListener(listViewToCommonScrollListener);

        when(adsContainer.getAdsPositions()).thenReturn(Collections.singletonList(9));
        when(listView.getHeight()).thenReturn(800);
        View itemView = mock(View.class);
        when(itemView.getTop()).thenReturn(600);
        when(itemView.getHeight()).thenReturn(200);
        when(listView.getChildAt(5)).thenReturn(itemView);
        when(listView.getFirstVisiblePosition()).thenReturn(4);
        when(listView.getChildCount()).thenReturn(6);
        ListAdapter adapter = mock(ListAdapter.class);
        when(adapter.getCount()).thenReturn(10);
        when(listView.getAdapter()).thenReturn(adapter);

        onAdsAppearsCompositeListener.onScroll(listView, 4, 6, 10);
        onAdsAppearsCompositeListener.onScrollStopped(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);

        listViewToCommonScrollListener.forceInvoke(listView);

        verify(adAppearsListener).onAdAppears(9);
        verify(adAppearsListener, never()).onAdDisappeared();
        verify(adAppearsListener).onStoppedOnAd(9);
    }
}
