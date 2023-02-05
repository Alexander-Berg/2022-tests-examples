package ru.yandex.autotests.mobile.disk.android.feed

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
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Feed")
@UserTags("feedAds")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedAdsTest : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("5925")
    @Category(Quarantine::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO, FilesAndFolders.VIDEO1_MP4],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO, FilesAndFolders.VIDEO1_MP4],
        targetFolder = FilesAndFolders.ORIGINAL_FOLDER
    )
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    fun shouldHideAdsBlock() {
        onBasePage.openFeed()
        onBasePage.shouldBeOnFeed()
        onFeed.wait(15, TimeUnit.SECONDS)
        onFeed.updateFeed()
        onFeed.shouldScrollToAdsBlock()
        onMobile.switchToAirplaneMode()
        onFeed.shouldHideAdsBlock()
        onFeed.wait(5, TimeUnit.SECONDS)
        onFeed.shouldAdsBlockBeNotDisplayed()
    }
}
