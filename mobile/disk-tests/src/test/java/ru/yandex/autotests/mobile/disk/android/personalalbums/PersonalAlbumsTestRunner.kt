package ru.yandex.autotests.mobile.disk.android.personalalbums

import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import javax.inject.Inject

open class PersonalAlbumsTestRunner {
    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onAlbums: AlbumsSteps

    @Inject
    lateinit var onOfflineFolder: OfflineSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onPersonalAlbum: PersonalAlbumSteps

    @Inject
    lateinit var onEditTextDialog: EditTextDialogSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onContentBlock: ContentBlockSteps

    @Inject
    lateinit var groupModeSteps: GroupModeSteps

    @Inject
    lateinit var onShareMenu: ShareMenuSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onAlert: CommonAlertSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps

    @Inject
    lateinit var onAdb: AdbSteps

    @Inject
    lateinit var onTrash: TrashSteps

    @Inject
    lateinit var onShare: ShareMenuSteps
}
