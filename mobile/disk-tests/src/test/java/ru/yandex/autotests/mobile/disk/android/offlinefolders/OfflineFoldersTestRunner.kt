package ru.yandex.autotests.mobile.disk.android.offlinefolders

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.props.AndroidConfig
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.NameHolder

open class OfflineFoldersTestRunner {
    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var testAccount: Account

    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    lateinit var config: AndroidConfig

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onTrash: TrashSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onUserDiskApi: DiskApiSteps

    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var onShareDiskApi: DiskApiSteps

    @Inject
    lateinit var onAdb: AdbSteps
}
