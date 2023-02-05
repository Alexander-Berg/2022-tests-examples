package ru.yandex.market.di.yandexplayer

import ru.yandex.video.ott.data.dto.VhVideoData
import ru.yandex.video.ott.data.repository.ManifestRepository
import ru.yandex.video.player.utils.future
import java.util.concurrent.Future
import javax.inject.Inject

class TestManifestRepository @Inject constructor() : ManifestRepository<VhVideoData> {
    override fun loadVideoData(contentId: String): Future<VhVideoData> {
        return future {
            VhVideoData(
                manifestUrl = contentId,
                contentId = contentId
            )
        }
    }
}