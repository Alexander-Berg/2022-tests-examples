package ru.yandex.disk.feed

import android.content.Intent
import android.content.pm.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.robolectric.annotation.Config
import ru.yandex.auth.AccountManager
import ru.yandex.auth.YandexAccount
import ru.yandex.disk.CredentialsManager
import ru.yandex.disk.test.AndroidTestCase2
import rx.Single
import rx.android.plugins.RxAndroidPlugins
import rx.android.plugins.RxAndroidSchedulersHook
import rx.observers.TestSubscriber
import rx.plugins.RxJavaHooks
import rx.schedulers.Schedulers

@Config(manifest = Config.NONE)
class BrowserAuthorizerTest : AndroidTestCase2() {
    private val packageManager = mock<PackageManager>()
    private val accountManager = mock<AccountManager>()
    private val credentialsManager = mock<CredentialsManager>()

    private val authorizer =
            BrowserAuthorizer(packageManager, accountManager, credentialsManager)

    private val rxAndroidSchedulersHook = object : RxAndroidSchedulersHook() {
        override fun getMainThreadScheduler() = Schedulers.immediate()
    }

    override fun setUp() {
        whenever(credentialsManager.activeAccount).then { mock<YandexAccount>() }
        whenever(accountManager.authUrl(any(), any())).thenAnswer { invocation ->
            val url = invocation?.arguments?.get(1)
            Single.just("<auth url>?redirect=$url")
        }

        RxJavaHooks.setOnIOScheduler({ Schedulers.immediate() })
        RxJavaHooks.setOnComputationScheduler({ Schedulers.immediate() })
        RxAndroidPlugins.getInstance().registerSchedulersHook(rxAndroidSchedulersHook)
    }

    private fun setupPackageManager(appPackageName: String) {
        whenever(packageManager.resolveActivity(any(), eq(PackageManager.MATCH_DEFAULT_ONLY)))
                .then {
                    ResolveInfo().apply {
                        activityInfo = ActivityInfo().apply {
                            packageName = appPackageName
                            name = "Application"
                        }
                    }
                }
        whenever(packageManager.getPackageInfo(any<String>(), eq(PackageManager.GET_SIGNATURES)))
                .then {
                    PackageInfo().apply {
                        signatures = arrayOf(Signature(CHROME_SIGNATURE))
                    }
                }
        whenever(packageManager.getApplicationInfo(any(), eq(0)))
                .then {
                    ApplicationInfo().apply {
                        flags = 0
                    }
                }
    }

    override fun tearDown() {
        RxJavaHooks.reset()
        RxAndroidPlugins.getInstance().reset()
    }

    @Test
    fun `should create browser intent`() {
        setupPackageManager("com.android.chrome")

        val intents = createNotImplicitBrowserIntent()
        assertThat(intents, hasSize(1))

        val intent = intents!![0]
        assertThat(intent.action, equalTo(Intent.ACTION_VIEW))
        assertThat(intent.dataString, equalTo("<auth url>?redirect=https://yandex.com"))
    }

    @Test
    fun `should not create intent for unknown app`() {
        setupPackageManager("io.best.browser")

        val intents = createNotImplicitBrowserIntent()
        assertThat(intents, hasSize(0))
    }

    @Test
    fun `should not create intent if default resolver`() {
        setupPackageManager("com.android.internal.app.ResolverActivity")

        val intents = createNotImplicitBrowserIntent()
        assertThat(intents, hasSize(0))
    }

    @Test
    fun `should not create intent if huawei resolver`() {
        setupPackageManager("com.huawei.android.internal.app.HwResolverActivity")

        val intents = createNotImplicitBrowserIntent()
        assertThat(intents, hasSize(0))
    }

    private fun createBrowserIntent(): List<Intent>? {
        val observer = authorizer.createBrowserIntent("https://yandex.com")
        val subscriber = TestSubscriber<Intent>()
        observer.subscribe(subscriber)

        return subscriber.onNextEvents
    }

    private fun createNotImplicitBrowserIntent(): List<Intent>? {
        return createBrowserIntent()?.filter { it.component != null }
    }
}

private const val CHROME_SIGNATURE = "308204433082032ba003020102020900c2e08746644a308d300d06092a864" +
        "886f70d01010405003074310b3009060355040613025553311330110603550408130a43616c69666f726e69613" +
        "11630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632" +
        "e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d303830383" +
        "2313233313333345a170d3336303130373233313333345a3074310b30090603550406130255533113301106035" +
        "50408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773114301206035" +
        "5040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e060355040313074" +
        "16e64726f696430820120300d06092a864886f70d01010105000382010d00308201080282010100ab562e00d83" +
        "ba208ae0a966f124e29da11f2ab56d08f58e2cca91303e9b754d372f640a71b1dcb130967624e4656a7776a921" +
        "93db2e5bfb724a91e77188b0e6a47a43b33d9609b77183145ccdf7b2e586674c9e1565b1f4c6a5955bff251a63" +
        "dabf9c55c27222252e875e4f8154a645f897168c0b1bfc612eabf785769bb34aa7984dc7e2ea2764cae8307d8c" +
        "17154d7ee5f64a51a44a602c249054157dc02cd5f5c0e55fbef8519fbe327f0b1511692c5a06f19d18385f5c4d" +
        "bc2d6b93f68cc2979c70e18ab93866b3bd5db8999552a0e3b4c99df58fb918bedc182ba35e003c1b4b10dd244a" +
        "8ee24fffd333872ab5221985edab0fc0d0b145b6aa192858e79020103a381d93081d6301d0603551d0e0416041" +
        "4c77d8cc2211756259a7fd382df6be398e4d786a53081a60603551d2304819e30819b8014c77d8cc2211756259" +
        "a7fd382df6be398e4d786a5a178a4763074310b3009060355040613025553311330110603550408130a43616c6" +
        "9666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f6" +
        "76c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964820" +
        "900c2e08746644a308d300c0603551d13040530030101ff300d06092a864886f70d010104050003820101006dd" +
        "252ceef85302c360aaace939bcff2cca904bb5d7a1661f8ae46b2994204d0ff4a68c7ed1a531ec4595a623ce60" +
        "763b167297a7ae35712c407f208f0cb109429124d7b106219c084ca3eb3f9ad5fb871ef92269a8be28bf16d44c" +
        "8d9a08e6cb2f005bb3fe2cb96447e868e731076ad45b33f6009ea19c161e62641aa99271dfd5228c5c587875dd" +
        "b7f452758d661f6cc0cccb7352e424cc4365c523532f7325137593c4ae341f4db41edda0d0b1071a7c440f0fe9" +
        "ea01cb627ca674369d084bd2fd911ff06cdbf2cfa10dc0f893ae35762919048c7efc64c7144178342f70581c9d" +
        "e573af55b390dd7fdb9418631895d5f759f30112687ff621410c069308a"
