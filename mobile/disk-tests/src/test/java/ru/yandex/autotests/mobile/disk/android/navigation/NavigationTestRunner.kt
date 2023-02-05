package ru.yandex.autotests.mobile.disk.android.navigation

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.steps.*

open class NavigationTestRunner {
    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onTrash: TrashSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Inject
    lateinit var onRecentApps: RecentAppsSteps

    @Inject
    lateinit var onFeedPage: FeedSteps

    @Inject
    lateinit var onContentBlock: ContentBlockSteps

    @Inject
    lateinit var onAlbums: AlbumsSteps

    @Inject
    lateinit var onFileList: FileListSteps

    @Inject
    lateinit var onProfile: ProfileSteps

    @Inject
    lateinit var onNotesApi: NotesApiSteps

    @Inject
    lateinit var onNotes: NotesSteps
}
