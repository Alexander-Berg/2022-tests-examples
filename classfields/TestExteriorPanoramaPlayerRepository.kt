package ru.auto.ara.core.mocks_and_stubbs

import android.graphics.Bitmap
import ru.auto.ara.core.utils.createTestImage
import ru.auto.data.model.data.offer.panorama.ExteriorPanorama
import ru.auto.feature.panorama.exteriorplayer.data.IExteriorPanoramaPlayerRepository
import ru.auto.feature.panorama.exteriorplayer.model.ExtractResult
import ru.auto.feature.panorama.exteriorplayer.model.Frame
import ru.auto.feature.panorama.exteriorplayer.model.FrameInfo
import ru.auto.feature.panorama.exteriorplayer.model.FrameType
import rx.Observable
import rx.Single
import java.io.IOException

class TestExteriorPanoramaPlayerRepository : IExteriorPanoramaPlayerRepository {

    var isExtractExteriorPanoramaFramesReturnsError: Boolean = false

    override fun getExteriorPanoramaFrame(frameInfo: FrameInfo): Single<Bitmap> =
        Single.just(createTestImage(""))

    override fun extractExteriorPanoramaFrames(panorama: ExteriorPanorama): Observable<ExtractResult> =
        if (isExtractExteriorPanoramaFramesReturnsError) {
            Observable.error(IOException("extractExteriorPanoramaFrames returns error"))
        } else {
            Observable.just(
                ExtractResult(
                    frameType = FrameType.FULL,
                    decodedFrameCount = 1,
                    frameCount = 1,
                    frame = Frame(
                        type = FrameType.FULL,
                        index = 0,
                        bitmap = createTestImage("")
                    )
                )
            )
        }

}
