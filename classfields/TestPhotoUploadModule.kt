package ru.auto.ara.core.di.module

import android.content.Context
import ru.auto.ara.di.module.PhotoUploadModule
import ru.auto.data.ErrorCode
import ru.auto.data.managers.ExternalFileManager
import ru.auto.data.managers.MediaStoreExternalFileManager
import ru.auto.data.model.data.offer.Photo
import ru.auto.data.network.exception.ApiException
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.repository.IPhotoUploadRepository
import rx.Completable
import rx.Observable
import java.util.concurrent.TimeUnit

class TestPhotoUploadModule(context: Context) : PhotoUploadModule(context) {

    override fun provideExternalFileManager(): ExternalFileManager =
        TestMediaStoreExternalFileManager(MediaStoreExternalFileManager(context))

    override fun providePhotoUploadRepositoryFactory(externalFileManager: ExternalFileManager): PhotoUploadRepositoryFactory =
        object : PhotoUploadRepositoryFactory {
            override fun create(scalaApi: ScalaApi, category: String): IPhotoUploadRepository = TestPhotoUploadRepository()
        }


    class TestMediaStoreExternalFileManager(
        private val delegate: ExternalFileManager
    ) : ExternalFileManager by delegate


    class TestPhotoUploadRepository : IPhotoUploadRepository {

        enum class UploadingPhotoStatus {
            UPLOADING,
            UPLOADED,
            FAILED
        }

        override fun uploadImage(url: String, uriString: String): Observable<Photo> = getUploadPhotoObservable()

        override fun uploadImageByPath(url: String, filePath: String): Completable = Completable.complete()

        override fun uploadPhotoMds(url: String, uriString: String): Observable<Photo> = getUploadPhotoObservable()

        override fun deleteImage(offerId: String, photoId: String): Completable = Completable.complete()

        @Suppress("MaxLineLength")
        private fun getUploadPhotoObservable(): Observable<Photo> =
            when (uploadingPhotoStatus) {
                UploadingPhotoStatus.UPLOADED -> Observable.just(Photo(
                    name = "9466ef1a7628b73b4e332bf67acc0c32",
                    namespace = "autoru-carfax",
                    groupId = 1397950,
                    wideScreen = "https://images.mds-proxy.test.avto.ru/get-autoru-carfax/1397950/9466ef1a7628b73b4e332bf67acc0c32/1200x900n",
                    large = "https://images.mds-proxy.test.avto.ru/get-autoru-carfax/1397950/9466ef1a7628b73b4e332bf67acc0c32/1200x900",
                    medium = "https://images.mds-proxy.test.avto.ru/get-autoru-carfax/1397950/9466ef1a7628b73b4e332bf67acc0c32/832x624",
                    micro = "https://images.mds-proxy.test.avto.ru/get-autoru-carfax/1397950/9466ef1a7628b73b4e332bf67acc0c32/92x69",
                    small = "https://images.mds-proxy.test.avto.ru/get-autoru-carfax/1397950/9466ef1a7628b73b4e332bf67acc0c32/320x240",
                    thumb = "https://images.mds-proxy.test.avto.ru/get-autoru-carfax/1397950/9466ef1a7628b73b4e332bf67acc0c32/thumb_m",
                    full = "https://images.mds-proxy.test.avto.ru/get-autoru-carfax/1397950/9466ef1a7628b73b4e332bf67acc0c32/full",
                    rawSizes = mapOf(
                        "thumb_m" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/thumb_m",
                        "832x624" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/832x624",
                        "full" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/full",
                        "320x240" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/320x240",
                        "1200x900" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/1200x900",
                        "small" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/small",
                        "120x90" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/120x90",
                        "92x69" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/92x69",
                        "456x342" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/456x342",
                        "1200x900n" to "//images.mds-proxy.test.avto.ru/get-autoru-vos/1600942/5b92e3ba0046644113cbd18d836d1a81/1200x900n",
                    ),
                )).delay(uploadingDelay, TimeUnit.MILLISECONDS)

                UploadingPhotoStatus.UPLOADING -> Observable.just(Photo(
                    name = "",
                    large = "content://com.google.android.apps.photos.contentprovider/-1/1/content%3A%2F%2Fmedia%2Fexternal%2Fimages%2Fmedia%2F63/ORIGINAL/NONE/891287921",
                    progress = 55,
                ))

                UploadingPhotoStatus.FAILED -> Observable.just(null)
            }
            .map { it ?: throw ApiException(ErrorCode.PHOTO_UPLOAD_ERROR) }

        companion object {

            var uploadingPhotoStatus: UploadingPhotoStatus = UploadingPhotoStatus.UPLOADED

            private const val UPLOADED_PHOTO_DELAY_IN_MILLIS = 150L
            var uploadingDelay = UPLOADED_PHOTO_DELAY_IN_MILLIS

        }

    }

}
