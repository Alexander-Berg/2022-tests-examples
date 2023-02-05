package ru.yandex.market.clean.presentation.feature.cms.item.carousel.livestream

import com.google.android.exoplayer2.Player
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.analytics.facades.LiveStreamAnalytics
import ru.yandex.market.clean.presentation.feature.cms.model.CmsLiveStreamVo
import ru.yandex.market.feature.videosnippets.ui.CarouselLiveStreamWidgetView
import ru.yandex.market.feature.videosnippets.ui.vo.LiveStreamStateVo
import ru.yandex.market.feature.videosnippets.ui.providers.LiveStreamTimerProviderImpl
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.Duration
import ru.yandex.video.player.YandexPlayer
import java.util.Date

class CarouselLiveStreamWidgetItemPresenterTest {

    private val view = mock<CarouselLiveStreamWidgetView>()
    private val yandexPlayer = mock<YandexPlayer<Player>>()
    private val useCases = mock<CarouselLiveStreamWidgetUseCases>()
    private val liveStreamStateVo = mock<LiveStreamStateVo>()
    private val schedulers = presentationSchedulersMock()

    private val cmsLiveStreamVo = mock<CmsLiveStreamVo> {
        on { startTime } doReturn Date(DUMMY_DATE)
        on { duration } doReturn Duration(DUMMY_DURATION)
    }
    private val liveStreamTimerProvider = mock<LiveStreamTimerProviderImpl> {
        on { create(any(), any(), any()) } doReturn Observable.just(liveStreamStateVo)
    }

    private val anaylytics = mock<LiveStreamAnalytics>()

    private val presenter = CarouselLiveStreamWidgetItemPresenter(
        schedulers,
        liveStreamTimerProvider,
        cmsLiveStreamVo,
        useCases,
        yandexPlayer,
        anaylytics
    )

    @Test
    fun `Should player when internet is good and toggle enabled`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(true))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.attachView(view)

        verify(yandexPlayer).prepare(DUMMY_ID, null, false)
    }

    @Test
    fun `Should not prepare player when internet is bad and toggle enabled`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(true))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.attachView(view)

        verify(yandexPlayer, never()).prepare(any<String>(), any(), any())
    }

    @Test
    fun `Should not prepare player when internet is good and toggle disabled`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(false))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.attachView(view)

        verify(yandexPlayer, never()).prepare(any<String>(), any(), any())
    }

    @Test
    fun `Should pause when video is not allowed to play`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(false))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.onItemAllowedToPlayVideo(true)

        verify(yandexPlayer).play()
        verify(yandexPlayer, never()).stop()

    }

    @Test
    fun `Should play when video is allowed to play`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(false))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.onItemAllowedToPlayVideo(false)

        verify(yandexPlayer, never()).play()
        verify(yandexPlayer).stop()
    }

    @Test
    fun `Should hide player on detach presenter`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(true))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.attachView(view)
        presenter.detachView(view)

        verify(yandexPlayer).stop()
        verify(yandexPlayer).removeObserver(any())
        verify(view).attachPlayer(null)
    }

    @Test
    fun `Should release resources when presenter destroyed`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(false))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.onDestroy()

        verify(yandexPlayer).stop()
        verify(yandexPlayer).release()
    }

    @Test
    fun `Should start count down stream time timer when view attached`() {
        whenever(useCases.isAutoPlayEnabled()).thenReturn(Single.just(false))
        whenever(cmsLiveStreamVo.translationId).thenReturn(DUMMY_ID)

        presenter.attachView(view)

        verify(view).updateState(liveStreamStateVo)
    }

    private companion object {
        const val DUMMY_ID = "DUMMY_ID"
        const val DUMMY_DATE = 1L
        const val DUMMY_DURATION = 1.0
    }
}