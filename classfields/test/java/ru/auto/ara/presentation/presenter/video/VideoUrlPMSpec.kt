package ru.auto.ara.presentation.presenter.video

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import ru.auto.ara.R
import ru.auto.ara.di.ComponentManager
import ru.auto.ara.interactor.VideoLinkInteractor
import ru.auto.ara.network.external.response.GetYouTubeVideoInfoResponse
import ru.auto.ara.presentation.ViewModelViewStub
import ru.auto.ara.router.Navigator
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.error.ErrorFactory
import ru.auto.ara.viewmodel.video.SimpleVideoViewModel
import ru.auto.ara.viewmodel.video.VideoUrlViewModel
import rx.Single

/**
 * @author themishkun on 15/11/2018.
 */
internal class VideoUrlPMSpec : DescribeSpec({
    val strings: StringsProvider = mock()
    val navigator: Navigator = mock()
    val interactor: VideoLinkInteractor = mock()
    val errorFactory: ErrorFactory = mock {
        on(it.createSnackError(any())) doReturn ("ERROR")
    }
    val componentManager: ComponentManager = mock()
    val listener: VideoUrlPM.ListenerProvider = mock()
    val view = ViewModelViewStub<VideoUrlViewModel>()
    val presenterFromState: (VideoUrlViewModel) -> VideoUrlPM = { state ->
        VideoUrlPM(
            strings = strings,
            interactor = interactor,
            componentManager = componentManager,
            router = navigator,
            errorFactory = errorFactory,
            model = state,
            listenerProvider = listener
        )
    }
    describe("start with initial state") {
        val presenter = presenterFromState(VideoUrlViewModel())
        presenter.bind(view)
        context("given nonempty input") {
            val INPUT = "INPUT"
            presenter.onInputChanged(INPUT)
            it("sets this input to state") {
                view.state.input shouldBe INPUT
            }
            it("enables clear") {
                view.state.clearEnabled shouldBe true
            }
        }
        context("given empty input") {
            presenter.onInputChanged("")
            it("disables clear") {
                view.state.clearEnabled shouldBe false
            }
            it("does not execute search") {
                presenter.onSearchClicked()
                verifyZeroInteractions(interactor)
            }
        }
    }
    describe("start with some input on which interactor succeeds") {
        val TITLE = "TITLE"
        val THUMB_URL = "THUMB_URL"
        val VIDEO: GetYouTubeVideoInfoResponse.YouTubeVideoInfo = mock {
            on(it.title) doReturn TITLE
            on(it.isFound) doReturn true
            on(it.thumbUrl) doReturn THUMB_URL
        }
        whenever(interactor.getYouTubeVideoInfo(any())).thenReturn(Single.just(VIDEO))
        val INPUT = "INPUT"
        val presenter = presenterFromState(VideoUrlViewModel(input = INPUT))
        presenter.bind(view)
        context("click search") {
            presenter.onSearchClicked()
            it("executes search with given input") {
                verify(interactor).getYouTubeVideoInfo(eq(INPUT))
            }
            it("sets video parameters") {
                view.state.video?.title shouldBe VIDEO.title
                view.state.video?.url shouldBe INPUT
                view.state.video?.thumbUrl shouldBe VIDEO.thumbUrl
            }
            it("enables accept button") {
                view.state.acceptEnabled shouldBe true
            }
            it("disables loading") {
                view.state.isLoading shouldBe false
            }
        }
    }
    describe("start with some input on which interactor fails") {
        val INPUT = "INPUT"
        val presenter = presenterFromState(VideoUrlViewModel(input = INPUT))
        presenter.bind(view)
        context("failure is ${IllegalArgumentException::class}") {
            val INVALID_LINK = "ERR_MSG1"
            whenever(strings.get(eq(R.string.invalid_link_format))).thenReturn(INVALID_LINK)
            whenever(interactor.getYouTubeVideoInfo(any())).thenReturn(Single.error(IllegalArgumentException("stub")))
            presenter.onSearchClicked()
            it("sets error to invalid_link_format") {
                view.state.error shouldBe INVALID_LINK
            }
        }
        context("failure is ${NoSuchElementException::class}") {
            val CANNOT_LOAD = "ERR_MSG2"
            whenever(strings.get(eq(R.string.cannot_load_video))).thenReturn(CANNOT_LOAD)
            whenever(interactor.getYouTubeVideoInfo(any())).thenReturn(Single.error(NoSuchElementException()))
            presenter.onSearchClicked()
            it("sets error to cannot_load_video") {
                view.state.error shouldBe CANNOT_LOAD
            }
        }
    }
    describe("start with some state") {
        checkAll(Arb.string(), Arb.string().orNull(), Arb.string()) { input, error, video ->
            val presenter = presenterFromState(
                VideoUrlViewModel(
                    input = input,
                    error = error,
                    acceptEnabled = true,
                    clearEnabled = true,
                    video = SimpleVideoViewModel(video, video, video)
                )
            )
            presenter.bind(view)
            it("sets default model on clear")
            presenter.onRemoveVideoClicked()
            view.state shouldBe VideoUrlViewModel()
        }
    }
})
