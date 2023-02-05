package ru.yandex.autotests.mobile.disk.android.copyfiles

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.NameHolder
import javax.inject.Named

open class CopyFilesTestRunner {
    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onAllPhotos: PhotosSteps

    @Inject
    lateinit var onOffline: OfflineSteps
}
