package ru.yandex.autotests.mobile.disk.android.offlinefiles

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.props.AndroidConfig
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.NameHolder

open class OfflineFilesTestRunner {
    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var account: Account

    @Inject
    lateinit var config: AndroidConfig

    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onAdb: AdbSteps

    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var onShareDiskApi: DiskApiSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps
}
