package ru.auto.ara.core

import ru.auto.feature.panorama.manager.PanoramaFileManager
import ru.auto.feature.panorama.namepicker.data.IPanoramaNamePickerRepository
import rx.Completable
import rx.Single

class TestPanoramaNamePickerRepository : IPanoramaNamePickerRepository {

    override fun selectPanoramaPath(nameCandidate: String): Single<String> =
        Single.just("$nameCandidate.${PanoramaFileManager.PANORAMA_EXT}")

    override fun renamePanorama(path: String, nameCandidate: String): Completable =
        Completable.complete()
}
