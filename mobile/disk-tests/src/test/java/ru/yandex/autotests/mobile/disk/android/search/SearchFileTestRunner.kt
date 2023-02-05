package ru.yandex.autotests.mobile.disk.android.search

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.steps.*

open class SearchFileTestRunner {
    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onSearch: SearchSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onFeed: FeedSteps
}
