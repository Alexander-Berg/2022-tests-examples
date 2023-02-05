package ru.yandex.disk.video

import android.media.MediaPlayer
import android.net.Uri
import org.mockito.kotlin.*
import com.yandex.disk.rest.exceptions.http.HttpCodeException
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import ru.yandex.disk.FileItem
import ru.yandex.disk.Storage
import ru.yandex.disk.audio.AudioFocusHelper
import ru.yandex.disk.audio.HeadsetReceiver
import ru.yandex.disk.connectivity.NetworkState
import ru.yandex.disk.remote.VideoUrlsApi
import ru.yandex.disk.remote.webdav.WebdavClient
import ru.yandex.disk.stats.AnalyticEventKeys
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.TestEnvironment
import ru.yandex.disk.util.ReflectionUtils
import ru.yandex.disk.util.error.ErrorKeys
import ru.yandex.disk.util.error.ErrorReporter
import ru.yandex.disk.video.delegate.PlayerDelegate
import ru.yandex.disk.video.meta.FetchMetaStrategy
import ru.yandex.disk.video.meta.FetchMetaStrategyFactory
import ru.yandex.disk.video.meta.VideoDescription
import rx.Observable
import rx.Single
import rx.plugins.RxJavaHooks
import rx.schedulers.Schedulers
import java.io.File

@Config(manifest = Config.NONE)
class VideoPlayerPresenterTest : AndroidTestCase2() {

    private val nativeApi = mock<VideoPlayerNativeApi>()
    private var storage = mock<Storage>()
    private val webdavPool = mock<WebdavClient.Pool>()
    private val networkState = mock<NetworkState>()
    private val fetchStrategy = mock<FetchMetaStrategy>()
    private val metaStrategyFactory = mock<FetchMetaStrategyFactory> {
        on { create(anyString()) } doReturn fetchStrategy
    }
    private val errorReporter = mock<ErrorReporter>()
    private val playerView = mock<VideoPlayerView>()
    private val playerDelegate = mock<PlayerDelegate>()
    private val headsetReceiver = mock<HeadsetReceiver>()
    private val audioFocusHelper = mock<AudioFocusHelper>()
    private val mediaPlayer = mock<MediaPlayer> {
        on { duration } doReturn 60000
    }

    override fun setUp() {
        super.setUp()
        setupRx()
    }

    override fun tearDown() {
        resetRx()
        super.tearDown()
    }

    @Test
    fun `should attach`() {
        val presenter = defaultPresenter()
        injectEmptyState(presenter)
        presenter.attach(playerView)

        verify(metaStrategyFactory).create(eq(VideoSourceType.PUBLIC_LINK))
        verify(playerDelegate).setOnPreparedListener(eq(presenter))
        verify(playerDelegate).setOnErrorListener(eq(presenter))
        verify(playerDelegate).setOnCompletionListener(eq(presenter))
        verify(headsetReceiver).register(eq(presenter))
        verify(audioFocusHelper, never()).requestFocus()
    }

    @Test
    fun `should release`() {
        val presenter = defaultPresenter()
        injectEmptyState(presenter)
        presenter.attach(playerView)
        presenter.release()

        verify(headsetReceiver).unregister()
        verify(audioFocusHelper).setAudioFocusListener(eq(null))
        verify(audioFocusHelper).abandonFocus()
        verify(playerDelegate).destroy()
    }

    private fun injectEmptyState(presenter: VideoPlayerPresenter) {
        // in some cases we don't need to start playback flow
        // so use empty state as initial to prevent state machine execution
        ReflectionUtils.setField(presenter, "playbackState", mock<PlaybackState>())
    }

    @Test
    fun `should switch to progress`() {
        // in order to stay in progress state && prevent playback flow
        val description = Observable.never<VideoDescription>().toSingle()
        val stream = Observable.never<VideoStreamInfo>().toSingle()
        mockFetchStrategy(description, stream)

        val presenter = defaultPresenter()
        presenter.attach(playerView)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(playerDelegate, atLeastOnce()).currentPosition
        verify(playerView).updateProgressVisibility(eq(true))
        verify(playerView).updateBarsVisibility(eq(true))
    }

    @Test
    fun `should switch to stream error`() {
        val err = Throwable(HttpCodeException(VideoStreamInfo.ERROR_BUSY))
        val streamInfo = VideoStreamInfo.error(err)
        mockFetchStrategy(null, streamInfo)

        val presenter = defaultPresenter()
        presenter.attach(playerView)

        verifyError(PlayerError.REMOTE_BUSY)
    }

    @Test
    fun `should try play cached file and switch to generic error even if network is connected`() {
        val description = VideoDescription("VideoDescription")
        val err = Throwable(HttpCodeException(VideoStreamInfo.ERROR_UNSUPPORTED))
        val streamInfo = VideoStreamInfo.error(err)
        mockFetchStrategy(description, streamInfo)

        val presenter = defaultPresenter()
        presenter.attach(playerView)

        verify(playerView).updateDescription(eq(description))
        verifyError(PlayerError.GENERIC)
    }

    private fun verifyError(expectedErrorCode: Int) {
        val inOrder = inOrder(playerView)
        inOrder.verify(playerView).updateBarsVisibility(eq(true))
        inOrder.verify(playerView).updateProgressVisibility(eq(false))
        inOrder.verify(playerView).updatePlayButton(eq(false))
        inOrder.verify(playerView).updateBarsVisibility(eq(true))
        val argumentCaptor = ArgumentCaptor.forClass(PlayerError::class.java)
        inOrder.verify(playerView).showError(argumentCaptor.capture())
        assertThat(argumentCaptor.value.code, equalTo(expectedErrorCode))
    }

    @Test
    fun `should play cached file`() {
        val file = createFileOnSD("file")
        whenever(storage.storagePath).thenReturn(file.parentFile.absolutePath)
        val fileItem = mock<FileItem>() {
            on { displayName } doReturn "DisplayName"
            on { path } doReturn file.name
        }
        val description = VideoDescription(fileItem)
        val err = Throwable(HttpCodeException(404))
        val streamInfo = VideoStreamInfo.error(err)
        mockFetchStrategy(description, streamInfo)

        val presenter = defaultPresenter()
        presenter.attach(playerView)

        verify(playerView).updateDescription(eq(description))
        verify(playerDelegate).start(eq(file.absolutePath))
    }

    @Test
    fun `should play video stream`() {
        val streamInfo = prepareForPlayStream()

        val presenter = defaultPresenter()
        presenter.attach(playerView)

        verify(streamInfo).defaultResolution
        verify(streamInfo).duration
        verify(playerView).updateResolutionMenu(eq(streamInfo), any())
        verify(playerDelegate).start(eq("https://ya.ru/video/720p"))
    }

    @Test
    fun `should save and restore playback position`() {
        whenever(playerDelegate.currentPosition).thenReturn(10)
        val presenter = startPlayback()
        presenter.onPause()
        verify(playerView).updatePlayButton(eq(false))
        verify(playerDelegate).pause()
        // ProgressState#onEnter + VideoPlayerPresenter#onPause
        verify(playerDelegate, times(2)).currentPosition
        verify(playerDelegate).setKeepScreenOn(eq(false))
        presenter.onResume()
        verify(playerDelegate).seekTo(eq(10))
        // PlayState#onEnter + PlayState#onExit + PauseState#onActivityResume + ProgressState#onEnter
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(playerView, times(4)).updateBarsVisibility(true)
    }

    @Test
    fun `should pause on music becoming noisy`() {
        val presenter = startPlayback()
        presenter.onMusicBecomingNoisy()
        verify(playerDelegate).pause()
    }

    @Test
    fun `should pause on lost audio focus`() {
        val presenter = startPlayback()
        presenter.onLostAudioFocus(true)
        verify(playerDelegate).pause()
    }

    @Test
    fun `should handle player error`() {
        val presenter = startPlayback()
        presenter.onError(mediaPlayer, -1, -1)
        verify(errorReporter).logError(
                eq(ErrorKeys.LAST_VIDEO_PLAYER_ERROR),
                any(),
                eq(AnalyticEventKeys.VIDEO_PLAYER_ERROR))
    }

    @Test
    fun `should not update view in detach`() {
        val presenter = startPlayback()
        presenter.detach()
        presenter.onError(mediaPlayer, -1, -1)
        verify(playerView, never()).showError(any())
    }

    @Test
    fun `should show error after attach`() {
        val presenter = startPlayback()
        presenter.detach()
        presenter.onError(mediaPlayer, -1, -1)
        presenter.attach(playerView)
        verify(playerView, times(1)).showError(any())
    }

    @Test
    fun `should switch resolution`() {
        val presenter = startPlayback()
        reset(playerDelegate)
        presenter.doSwitchResolution(VideoResolution.p240)

        verify(playerDelegate).pause()
        verify(nativeApi).sendEvent(eq(AnalyticEventKeys.VIDEO_STREAMING_QUALITY_CHANGED + VideoResolution.p240.resolution))
        verify(playerDelegate).start(eq("https://ya.ru/video/240p"))
        verify(playerDelegate).seekTo(eq(0))
    }

    @Test
    fun `should update progress`() {
        whenever(playerDelegate.currentPosition).thenReturn(30000)
        whenever(playerDelegate.bufferPercentage).thenReturn(70)

        startPlayback()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(playerDelegate, atLeastOnce()).currentPosition
        verify(playerView, atLeastOnce()).updateProgress(eq(500))
        verify(playerView, atLeastOnce()).updateSecondaryProgress(eq(700))
        verify(playerView, atLeastOnce()).updateCurrentTime(eq(30000))
    }

    @Test
    fun `should switch to complete`() {
        val presenter = startPlayback()
        reset(playerView)
        reset(playerDelegate)
        presenter.onCompletion(mediaPlayer)
        verify(playerView).updatePlayButton(eq(false))
        verify(playerView, times(2)).updateBarsVisibility(eq(true))
        verify(playerDelegate).pause()
    }

    @Test
    fun `should toggle play pause`() {
        val presenter = startPlayback()
        reset(playerView)
        reset(playerDelegate)
        presenter.togglePlayPause()
        presenter.togglePlayPause()
        presenter.togglePlayPause()
        presenter.togglePlayPause()
        presenter.togglePlayPause()
        verify(playerDelegate, times(3)).pause()
        verify(playerDelegate, times(2)).play()
    }

    @Test
    fun `should switch to rewind and back`() {
        val presenter = startPlayback()
        reset(playerView)
        reset(playerDelegate)
        presenter.onStartTracking()
        presenter.onProgressChanged(500)
        presenter.onStopTracking()
        verify(playerView).updateCurrentTime(30000)
        verify(playerDelegate).pause()
        verify(playerDelegate).play()
        verify(playerDelegate).seekTo(eq(30000))
    }

    private fun startPlayback(): VideoPlayerPresenter {
        val presenter = defaultPresenter()
        prepareForPlayStream()
        presenter.attach(playerView)
        presenter.onPrepared(mediaPlayer)
        return presenter
    }

    private fun prepareForPlayStream(): VideoStreamInfo {
        val description = VideoDescription("VideoDescription")
        val videoUrlsResponse = mockVideoUrlsResponse()
        val resolutionPolicy = mock<DefaultVideoResolutionPolicy> {
            on { getDefaultResolutionFromSet(any()) } doReturn VideoResolution.p720
        }
        val streamInfo = spy(VideoStreamInfo.make(videoUrlsResponse, resolutionPolicy))
        mockFetchStrategy(description, streamInfo)

        val client = mock<WebdavClient> {
            on { resolveUrl(anyString()) }.then { url -> url.getArgument(0) }
        }

        whenever(webdavPool.anonymClient).thenReturn(client)

        return streamInfo
    }

    private fun mockVideoUrlsResponse(): VideoUrlsApi.VideoUrlsResponse {
        val urlItems = ArrayList<VideoUrlsApi.VideoUrlItem>()
        for (item in VideoResolution.values()) {
            val links = VideoUrlsApi.Links("http://ya.ru/video/$item", "https://ya.ru/video/$item")
            urlItems.add(VideoUrlsApi.VideoUrlItem(links, item.resolution))
        }
        return VideoUrlsApi.VideoUrlsResponse(60000, urlItems)
    }

    private fun createFileOnSD(fileName: String): File {
        val testDirectory = TestEnvironment.getTestRootDirectory()
        val file = File(testDirectory, fileName)
        file.createNewFile()
        return file
    }

    private fun setupRx() {
        RxJavaHooks.setOnIOScheduler({ Schedulers.immediate() })
    }

    private fun resetRx() {
        RxJavaHooks.reset()
    }

    private fun mockFetchStrategy(description: VideoDescription?, streamInfo: VideoStreamInfo) {
        mockFetchStrategy(Single.just(description), Single.just(streamInfo))
    }

    private fun mockFetchStrategy(description: Single<VideoDescription?>, streamInfo: Single<VideoStreamInfo>) {
        fetchStrategy.apply {
            whenever(loadDescription(any())).thenReturn(description)
            whenever(loadStreamingInfo(any())).thenReturn(streamInfo)
        }
    }

    private fun defaultPresenter() = createPresenter(VideoSourceType.PUBLIC_LINK, Uri.parse("https://yadi.sk/d/bKIUFadu3Mcdem"))

    private fun createPresenter(@VideoSourceType type: String, source: Uri): VideoPlayerPresenter {
        val config = PlayerConfig(
            type = type,
            source = source,
            nativeApi = nativeApi,
            autoplay = true,
            fetchMetaStrategyFactory = metaStrategyFactory
        )
        val presenter = VideoPlayerPresenter(config, storage, networkState,
                errorReporter, headsetReceiver, audioFocusHelper, Schedulers.immediate())
        presenter.init(null, playerDelegate)
        return presenter
    }

}
