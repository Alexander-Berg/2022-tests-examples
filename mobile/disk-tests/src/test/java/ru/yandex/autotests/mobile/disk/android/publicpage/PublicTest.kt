package ru.yandex.autotests.mobile.disk.android.publicpage

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.DiskTest
import ru.yandex.autotests.mobile.disk.android.StepsLocator
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders.DOWNLOAD_FULL_PATH
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders.STORAGE_ROOT
import java.util.concurrent.TimeUnit


@Feature("Public")
@UserTags("public")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PublicTest : DiskTest {

    companion object {
        @ClassRule
        @JvmField
        val classRuleChain = createClassTestRules()

        // login: yndx-yda-public-data-1
        // password: qaztrewq321
        const val LINK = "https://disk.yandex.ru/d/_B3t0OSkwvBBcA"
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    override lateinit var locator: StepsLocator

    @Test
    @TmsLink("8104")
    @Category(Regression::class)
    @DeleteFilesOnDevice(filesPath = [DOWNLOAD_FULL_PATH, STORAGE_ROOT])
    fun shouldSaveFolderFromPublicLink() {
        onNavigationPage {
            openPublicLink(LINK) {
                shouldSeeDownloadButton()
                clickDownload {
                    shouldSaveArchive()
                    wait(5, TimeUnit.SECONDS)
                }
                withAdb { shouldFileExistInFolderOnDevice("test8104.zip", DOWNLOAD_FULL_PATH) }

                shouldSeeDownloadButton()
                clickDownload {
                    shouldSaveFiles {
                        pressSaveHereButton()
                        wait(5, TimeUnit.SECONDS)
                    }
                }
                withAdb { shouldFileExistInFolderOnDevice("8104.jpg", STORAGE_ROOT + "test8104/") }
            }
        }
    }
}
