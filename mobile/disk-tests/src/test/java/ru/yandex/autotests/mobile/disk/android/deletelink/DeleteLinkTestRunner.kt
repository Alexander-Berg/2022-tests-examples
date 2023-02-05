package ru.yandex.autotests.mobile.disk.android.deletelink

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.NameHolder

open class DeleteLinkTestRunner {
    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var account: Account

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onUserDiskApi: DiskApiSteps

    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var onShareDiskApi: DiskApiSteps

    @Inject
    lateinit var onAllPhotos: PhotosSteps

    protected fun deletePublicLink(name: String) {
        onFiles.shouldSeePublicFileMark(name)
        onFiles.shouldSelectFilesOrFolders(name)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
    }
}
