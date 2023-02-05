package ru.yandex.market.di.yandexplayer

import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.AssetDataSource
import com.google.android.exoplayer2.upstream.TransferListener
import ru.yandex.video.player.CurrentBufferLengthProvider
import ru.yandex.video.player.MediaSourceListener
import ru.yandex.video.player.drm.ExoDrmSessionManager
import ru.yandex.video.source.MediaSourceFactory
import javax.inject.Inject

class TestMediaSourceFactory @Inject constructor() : MediaSourceFactory {
    override fun create(
        url: String,
        drmSessionManager: ExoDrmSessionManager,
        transferListener: TransferListener?,
        currentBufferLengthProvider: CurrentBufferLengthProvider?,
        mediaSourceListener: MediaSourceListener?,
    ): MediaSource {
        val context = InstrumentationRegistry.getInstrumentation().context
        return ProgressiveMediaSource.Factory { AssetDataSource(context) }
            .createMediaSource(MediaItem.fromUri(url))
    }
}