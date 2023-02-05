package ru.yandex.autotests.mobile.disk.android.groupoperation

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.steps.*

open class GroupOperationTestRunner {
    //TODO: Split cases for image and simple items on file list
    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onSearch: SearchSteps

    @Inject
    lateinit var onCommon: CommonsSteps
}
