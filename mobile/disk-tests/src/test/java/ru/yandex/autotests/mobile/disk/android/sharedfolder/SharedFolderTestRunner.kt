package ru.yandex.autotests.mobile.disk.android.sharedfolder

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.props.AndroidConfig
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.NameHolder

open class SharedFolderTestRunner {
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
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    @Named(AccountConstants.SHARE_USER)
    lateinit var onShareDiskApi: DiskApiSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onUserDiskApi: DiskApiSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    companion object {
        @JvmStatic
        protected val anotherAccount = Account("etestuser1", "etestpass1", "")
    }
}
