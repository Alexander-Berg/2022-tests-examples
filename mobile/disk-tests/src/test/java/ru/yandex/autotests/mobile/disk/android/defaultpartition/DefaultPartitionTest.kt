package ru.yandex.autotests.mobile.disk.android.defaultpartition

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.TabNames

@Feature("Default Tab")
@UserTags("defaultTab")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DefaultPartitionTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onDefaultPartition: DefaultPartitionSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onRecentApps: RecentAppsSteps
    @Test
    @TmsLink("3343")
    @Category(Regression::class)
    fun shouldSetFilesAsDefaultPartition() {
        onBasePage.openPhotos()
        onBasePage.openSettings()
        onSettings.openOpenAtLaunchSection()
        onDefaultPartition.shouldOpenChangeDefaultPartitonDialog()
        onDefaultPartition.shouldChangeDefaultPartitonToUsualPartition(TabNames.FILES.toString())
        onDefaultPartition.shouldDefaultPartitionBe(TabNames.FILES.toString())
        onMobile.openAppSwitcher()
        onRecentApps.shouldCloseAllAppsInBackground()
        onLogin.startAppAndCloseWizards()
        onBasePage.shouldBeOnFiles()
    }

    @Test
    @TmsLink("3337")
    @Category(FullRegress::class)
    fun shouldFeedBeDefaultPartitionByDefault() {
        onBasePage.openSettings()
        onSettings.openOpenAtLaunchSection()
        onDefaultPartition.shouldDefaultPartitionBe(TabNames.FEED.toString())
    }

    @Test
    @TmsLink("3351")
    @Category(FullRegress::class)
    fun shouldSetAllPhotoAsDefaultPartition() {
        onBasePage.openSettings()
        onSettings.openOpenAtLaunchSection()
        onDefaultPartition.shouldOpenChangeDefaultPartitonDialog()
        onDefaultPartition.shouldChangeDefaultPartitonToUsualPartition(TabNames.PHOTOS.toString())
        onDefaultPartition.shouldDefaultPartitionBe(TabNames.PHOTOS.toString())
        onMobile.pressHardBack()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFeed()
        onMobile.openAppSwitcher()
        onRecentApps.shouldCloseAllAppsInBackground()
        onLogin.startAppAndCloseWizards()
        onBasePage.shouldBeOnPhotos()
    }

    @Test
    @TmsLink("3353")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldSetFeedAsDefaultPartition() {
        onBasePage.openSettings()
        onSettings.openOpenAtLaunchSection()
        onDefaultPartition.shouldOpenChangeDefaultPartitonDialog()
        onDefaultPartition.shouldChangeDefaultPartitonToUsualPartition(TabNames.FEED.toString())
        onDefaultPartition.shouldDefaultPartitionBe(TabNames.FEED.toString())
        onMobile.pressHardBack()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onMobile.openAppSwitcher()
        onRecentApps.shouldCloseAllAppsInBackground()
        onLogin.startAppAndCloseWizards()
        onBasePage.shouldBeOnFeed()
    }

    @Test
    @TmsLink("3354")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSetCustomFolderAsDefaultPartition() {
        onBasePage.openSettings()
        onSettings.openOpenAtLaunchSection()
        onDefaultPartition.shouldOpenChangeDefaultPartitonDialog()
        onDefaultPartition.shouldChangeDefaultPartitonToCustomFolder(FilesAndFolders.TARGET_FOLDER)
        onDefaultPartition.shouldDefaultPartitionBe(FilesAndFolders.TARGET_FOLDER)
        onMobile.pressHardBack()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFeed()
        onMobile.openAppSwitcher()
        onRecentApps.shouldCloseAllAppsInBackground()
        onLogin.startAppAndCloseWizards()
        onFiles.shouldFolderBeOpened(FilesAndFolders.TARGET_FOLDER)
    }
}
