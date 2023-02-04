package ru.auto.ara.core

import android.net.Uri
import ru.auto.feature.panorama.list.data.IPanoramasListRepository
import ru.auto.feature.panorama.list.model.Panorama
import rx.Completable
import rx.Single

class TestPanoramasListRepository : IPanoramasListRepository {

    override fun loadPanoramas(): Single<List<Panorama>> =
        Single.fromCallable { PANORAMAS.toList() }

    override fun removePanoramas(uriStringList: List<String>): Completable =
        Completable.fromAction { PANORAMAS.removeAll { it.uriString in uriStringList } }

    override fun getShareUri(uriString: String): Uri =
        Uri.parse(uriString)

    override fun isShouldShowPanoramasInfo(): Single<Boolean> =
        Single.fromCallable { IS_SHOULD_SHOW_PANORAMAS_INFO }

    override fun setShouldShowPanoramasInfo(isShow: Boolean): Completable =
        Completable.fromAction { IS_SHOULD_SHOW_PANORAMAS_INFO = isShow }

    companion object {
        val PANORAMAS = mutableListOf<Panorama>()
        var IS_SHOULD_SHOW_PANORAMAS_INFO = true
    }
}
