package ru.yandex.market.clean.presentation.feature.stories.story

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.StoriesAnalytics
import ru.yandex.market.clean.presentation.feature.stories.vo.ButtonAction
import ru.yandex.market.clean.presentation.feature.stories.vo.StorySkuVo
import ru.yandex.market.clean.presentation.feature.stories.vo.StorySlideVo
import ru.yandex.market.clean.presentation.feature.stories.vo.StoryVo
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.TargetScreen
import ru.yandex.market.data.deeplinks.links.Deeplink
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.realtimesignal.RealtimeSignalServiceImpl

class StoryPresenterTest {

    private val targetScreen = mock<TargetScreen<*>>()
    private val parsedDeeplink = mock<Deeplink> {
        on { targetScreen } doReturn targetScreen
    }
    private val skuVo = mock<StorySkuVo>()

    private val firstSlideMock = mock<StorySlideVo> {
        on { videoId } doReturn null
        on { buttonAction } doReturn ButtonAction.Deeplink(DUMMY_DEEP_LINK)
    }
    private val secondSlideMock = mock<StorySlideVo> {
        on { videoId } doReturn null
    }
    private val storyMock = mock<StoryVo> {
        on { slides } doReturn listOf(firstSlideMock, secondSlideMock)
        on { id } doReturn DUMMY_STORY_ID
        on { widgetPageId } doReturn DUMMY_PAGE_ID
        on { skuVo } doReturn skuVo
    }

    private val schedulers = presentationSchedulersMock()
    private val args = mock<StoryFragment.Arguments> {
        on { story } doReturn storyMock
        on { position } doReturn DUMMY_STORY_POSITION
    }
    private val storiesAnalytics = mock<StoriesAnalytics>()
    private val router = mock<Router>()
    private val useCases = mock<StoryUseCases> {
        on { setStoryShown(DUMMY_STORY_ID) } doReturn Completable.complete()
        on { parseDeeplink(DUMMY_DEEP_LINK) } doReturn Single.just(parsedDeeplink)
    }
    private val view = mock<StoryView>()
    private val realtimeSignalService = mock<RealtimeSignalServiceImpl>()

    private val presenter = StoryPresenter(
        schedulers = schedulers,
        args = args,
        storiesAnalytics = storiesAnalytics,
        router = router,
        useCases = useCases,
        realtimeSignalService,
    )

    private val eventData = StoriesAnalytics.EventData(
        promotionObject = null,
        campaignId = null,
        vendorId = null,
        widgetPageId = DUMMY_PAGE_ID,
        storyId = DUMMY_STORY_ID,
        slideElapsedTime = null,
        productId = null,
        slidePosition = DUMMY_SLIDE_POSITION,
        storyPageId = null,
        storyPreviewText = null
    )

    @Test
    fun `Should show video slide`() {
        whenever(firstSlideMock.videoId).doReturn(DUMMY_VIDEO_ID)
        whenever(firstSlideMock.isVideoSlide).doReturn(true)

        presenter.attachView(view)
        presenter.onPageShown()

        verify(view).showVideoSlide(firstSlideMock, skuVo, 0)
        verify(view, never()).showImageSlide(any(), any(), any())
    }

    @Test
    fun `Should show image slide`() {
        whenever(firstSlideMock.isImageSlide).doReturn(true)

        presenter.attachView(view)
        presenter.onPageShown()

        verify(view).showImageSlide(firstSlideMock, skuVo, 0)
        verify(view, never()).showVideoSlide(any(), any(), any())
    }

    @Test
    fun `Should send mertic that slide was shown`() {
        whenever(firstSlideMock.isImageSlide).doReturn(true)
        presenter.onPageShown()

        verify(storiesAnalytics).storySlideShown(any())
    }

    @Test
    fun `Should send metric that story was shown`() {
        presenter.onPageShown()

        verify(storiesAnalytics).storyShown(any())
    }

    @Test
    fun `Should mark story as shown when it was shown`() {
        presenter.onPageShown()

        verify(useCases).setStoryShown(DUMMY_STORY_ID)
    }

    @Test
    fun `Should stop timer and stop video when story is hidden`() {
        presenter.attachView(view)
        presenter.onStoryDisappeared(0L)

        verify(view).stopSliderTimer()
    }

    @Test
    fun `Should send metric when story next story tapped`() {
        presenter.onNextStoryTapped(DUMMY_ELAPSED_TIME)

        verify(storiesAnalytics).slideTapped(
            eventData.copy(slideElapsedTime = DUMMY_ELAPSED_TIME),
            StoriesAnalytics.EventData.TapDirection.FORWARD
        )
    }

    @Test
    fun `Should show next slide when next slide tapped and next slide exists`() {
        presenter.attachView(view)
        presenter.onNextStoryTapped(DUMMY_ELAPSED_TIME)

        verify(view).showImageSlide(secondSlideMock, skuVo, 1)
    }

    @Test
    fun `Should show next story when next slide tapped and slide is last`() {
        presenter.attachView(view)
        presenter.onNextStoryTapped(DUMMY_ELAPSED_TIME)
        presenter.onNextStoryTapped(DUMMY_ELAPSED_TIME)

        inOrder(view).apply {
            verify(view).showImageSlide(secondSlideMock, skuVo, 1)
            verify(view).navigateToNextStory(args.position, args.story)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `Should not send metric that story was tapped when slide finished`() {
        presenter.onSlideFinished()

        verify(storiesAnalytics, never()).slideTapped(any(), any())
    }

    @Test
    fun `Should show next story when last slide finished`() {
        presenter.attachView(view)
        presenter.onNextStoryTapped(DUMMY_ELAPSED_TIME)
        presenter.onSlideFinished()

        inOrder(view).apply {
            verify(view).showImageSlide(secondSlideMock, skuVo, 1)
            verify(view).navigateToNextStory(args.position, args.story)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `Should show next slide when not last slide finished`() {
        presenter.attachView(view)
        presenter.onSlideFinished()

        verify(view).showImageSlide(secondSlideMock, skuVo, 1)
    }

    @Test
    fun `Should send metric when story previous story tapped`() {
        presenter.onNextStoryTapped(DUMMY_ELAPSED_TIME)
        presenter.onPreviousStoryTapped(DUMMY_ELAPSED_TIME)

        verify(storiesAnalytics).slideTapped(
            eventData.copy(slideElapsedTime = DUMMY_ELAPSED_TIME, slidePosition = 1),
            StoriesAnalytics.EventData.TapDirection.BACK
        )
    }

    @Test
    fun `Should show previous slide when previous slide tapped and previous slide exists`() {
        presenter.attachView(view)
        presenter.onNextStoryTapped(DUMMY_ELAPSED_TIME)
        presenter.onPreviousStoryTapped(DUMMY_ELAPSED_TIME)

        inOrder(view).apply {
            verify(view).showImageSlide(secondSlideMock, skuVo, 1)
            verify(view).showImageSlide(firstSlideMock, skuVo, 0)
        }
    }

    @Test
    fun `Should show previous story when previous slide tapped and slide is not first`() {
        presenter.attachView(view)
        presenter.onPreviousStoryTapped(DUMMY_ELAPSED_TIME)

        verify(view).navigateToPreviousStory(DUMMY_STORY_POSITION, args.story)
    }

    @Test
    fun `Should send metric when close clicked`() {
        presenter.onCloseButtonClicked(DUMMY_ELAPSED_TIME)

        verify(storiesAnalytics).closeButtonClicked(eventData.copy(slideElapsedTime = DUMMY_ELAPSED_TIME))
    }

    @Test
    fun `Should send metric and invoke router back with url when story button clicked`() {
        presenter.attachView(view)
        presenter.onStoryButtonClicked(DUMMY_ELAPSED_TIME)

        verify(storiesAnalytics).buttonClicked(eventData.copy(slideElapsedTime = DUMMY_ELAPSED_TIME), DUMMY_DEEP_LINK)
        verify(router).back()
        verify(router).navigateTo(targetScreen)
    }

    private companion object {
        const val DUMMY_STORY_ID = "DUMMY_STORY_ID"
        const val DUMMY_PAGE_ID = "DUMMY_PAGE_ID"
        const val DUMMY_VIDEO_ID = "DUMMY_VIDEO_ID"
        const val DUMMY_DEEP_LINK = "DUMMY_DEEP_LINK"
        const val DUMMY_STORY_POSITION = 0
        const val DUMMY_SLIDE_POSITION = 0
        const val DUMMY_ELAPSED_TIME = 0L
    }
}