package ru.yandex.market.clean.presentation.feature.uservideo

import android.net.Uri
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.TestCase
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.yandex.market.clean.presentation.feature.uservideo.success.AddUserVideoSuccessTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.common.errors.ErrorVoFormatter

class AddUserVideoPresenterTest : TestCase() {

    private val useCases = mock<AddUserVideoUseCases>() {
        on { getAuthStatusStream() } doReturn Observable.just(true)
        on { uploadVideo(any()) } doReturn Single.just(MOCK_VIDEO_ID)
        on { saveUserVideo(any(), any(), any(), any()) } doReturn Completable.complete()
    }

    private val uri = mock<Uri>()

    private val router = mock<Router>()

    private val schedulers = presentationSchedulersMock()

    private val formatter = mock<AddUserVideoFormatter>() {
        on { getDurationByLocalUri(any()) } doReturn MOCK_DURATION
    }

    private val validator = mock<UserVideoSizeValidator>() {
        on { checkVideoSize(any()) } doReturn true
    }

    private val args = AddUserVideoFragment.Arguments(MOCK_MODEL_ID, MOCK_NAME, MOCK_SKU_ID)

    private val errorVoFormatter = mock<ErrorVoFormatter>()

    private val presenter =
        AddUserVideoPresenter(schedulers, router, args, useCases, formatter, errorVoFormatter, validator)

    private val view = mock<AddUserVideoView>()

    @Test
    fun `test empty state on open`() {
        presenter.attachView(view)
        verify(view).setVideoState(AddUserVideoStateVo.Empty)
    }

    @Test
    fun `test clean or remove video`() {
        presenter.attachView(view)
        presenter.onRemoveVideo()
        verify(view, times(2)).setVideoState(AddUserVideoStateVo.Empty)
    }

    @Test
    fun `test show video selector`() {
        presenter.attachView(view)
        presenter.onAddVideo()
        verify(view).showVideoSelector()
    }

    @Test
    fun `test upload video`() {
        presenter.attachView(view)
        presenter.setVideoUri(uri)
        verify(useCases).uploadVideo(uri)
    }

    @Test
    fun `test save video`() {
        presenter.attachView(view)
        presenter.setVideoUri(uri)
        presenter.saveVideo(MOCK_TEXT)
        verify(useCases).saveUserVideo(MOCK_VIDEO_ID, MOCK_MODEL_ID, MOCK_SKU_ID.toLong(), MOCK_TEXT)
        verify(router).replace(AddUserVideoSuccessTargetScreen())
    }

    companion object {
        private const val MOCK_NAME = "Имя товара"
        private const val MOCK_TEXT = "Коментарий"
        private const val MOCK_MODEL_ID = 51L
        private const val MOCK_SKU_ID = "1100051"
        private const val MOCK_DURATION = "01:00"
        private const val MOCK_VIDEO_ID = "12345"
    }
}