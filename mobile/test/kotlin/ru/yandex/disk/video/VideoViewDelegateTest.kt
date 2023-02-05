package ru.yandex.disk.video

import android.widget.VideoView
import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.video.delegate.VideoViewDelegate

private const val TEST_URL: String = "ya.ru"

class VideoViewDelegateTest {

    private val videoView = mock<VideoView>()
    private val delegate = VideoViewDelegate(videoView)

    @Test
    fun `should delegate`() {
        touchDelegate()
        verifyDelegation(true)
    }

    @Test
    fun `should not delegate`() {
        delegate.destroy()
        touchDelegate()
        verifyDelegation(false)
    }

    @Test
    fun `should destroy`() {
        delegate.destroy()
        verify(videoView).setOnCompletionListener(isNull())
        verify(videoView).setOnPreparedListener(isNull())
        verify(videoView).setOnErrorListener(isNull())
    }

    private fun verifyDelegation(shouldDelegate: Boolean) {
        val count = if (shouldDelegate) 1 else 0
        verify(videoView, times(count)).setVideoPath(eq(TEST_URL))
        // called twice from delegate.start() && delegate.play()
        verify(videoView, times(count * 2)).start()
        verify(videoView, times(count)).pause()
        // called twice from delegate.start() && delegate.stop()
        verify(videoView, times(count * 2)).stopPlayback()
        verify(videoView, times(count)).seekTo(eq(100))
        verify(videoView, times(count)).keepScreenOn = eq(true)
        verify(videoView, times(count)).isPlaying
        verify(videoView, times(count)).currentPosition
        verify(videoView, times(count)).bufferPercentage
        verify(videoView, times(count)).setOnCompletionListener(any())
        verify(videoView, times(count)).setOnPreparedListener(any())
        verify(videoView, times(count)).setOnErrorListener(any())
    }

    private fun touchDelegate() {
        delegate.play()
        delegate.start(TEST_URL)
        delegate.pause()
        delegate.stop()
        delegate.seekTo(100)
        delegate.setKeepScreenOn(true)
        delegate.isPlaying
        delegate.currentPosition
        delegate.bufferPercentage
        delegate.setOnCompletionListener{}
        delegate.setOnPreparedListener{}
        delegate.setOnErrorListener{ _, _, _ -> false}
    }
}
