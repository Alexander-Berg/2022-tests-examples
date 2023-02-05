package ru.yandex.autotests.mobile.disk.android.favoritesalbum

import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import javax.inject.Inject
import javax.inject.Named

open class FavoritesAlbumTestRunner {
    companion object {
        const val IMAGE_BLOCK_DIR = "image_block_dir"
    }

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onAlbums: AlbumsSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onContentBlock: ContentBlockSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onOfflineFolder: OfflineSteps

    @Inject
    lateinit var onEditTextDialog: EditTextDialogSteps

    @Inject
    lateinit var onShare: ShareMenuSteps

    @Named(AccountConstants.TEST_USER)
    lateinit var diskApiSteps: DiskApiSteps
}
