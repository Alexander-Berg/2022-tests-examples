package ru.yandex.autotests.mobile.disk.android.cache

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.steps.*

open class CacheTestRunner {
    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onAdb: AdbSteps
}
