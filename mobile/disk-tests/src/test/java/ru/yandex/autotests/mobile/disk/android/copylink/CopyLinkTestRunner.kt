package ru.yandex.autotests.mobile.disk.android.copylink

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.NameHolder

open class CopyLinkTestRunner {
    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps

    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var onShareDiskApi: DiskApiSteps
}
