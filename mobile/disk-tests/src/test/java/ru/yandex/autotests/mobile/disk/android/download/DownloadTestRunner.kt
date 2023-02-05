package ru.yandex.autotests.mobile.disk.android.download

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants

open class DownloadTestRunner {
    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onAllPhotos: PhotosSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onAdb: AdbSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps
}
