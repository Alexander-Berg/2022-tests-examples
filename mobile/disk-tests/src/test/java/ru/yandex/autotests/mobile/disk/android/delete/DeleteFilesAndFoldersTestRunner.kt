package ru.yandex.autotests.mobile.disk.android.delete

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.NameHolder

open class DeleteFilesAndFoldersTestRunner {
    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var sharedAccount: Account

    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onUserDiskApi: DiskApiSteps

    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var onShareDiskApi: DiskApiSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onTrash: TrashSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onSearch: SearchSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onAlbums: AlbumsSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onPersonalAlbum: PersonalAlbumSteps
}
