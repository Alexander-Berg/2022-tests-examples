package ru.yandex.autotests.mobile.disk.android.filesrenaming

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.props.AndroidConfig
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.NameHolder

open class RenameFileAndFolderTestRunner {
    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    lateinit var config: AndroidConfig

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onMobDiskApi: DiskApiSteps

    @Inject
    lateinit var onTrash: TrashSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var onShareDiskApi: DiskApiSteps

    @Inject
    open lateinit var onAllPhotos: PhotosSteps
}
