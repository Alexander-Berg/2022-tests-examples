package ru.yandex.autotests.mobile.disk.android.navigation

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Navigation")
@UserTags("navigation")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class NavigationTest3 : NavigationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("5466")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.FOLDER_1, FilesAndFolders.FOLDER_1 + "/" + FilesAndFolders.FOLDER_2, FilesAndFolders.FOLDER_1 + "/" + FilesAndFolders.FOLDER_2 + "/" + FilesAndFolders.FOLDER_3])
    @DeleteFiles(files = [FilesAndFolders.FOLDER_1])
    fun shouldFilesNavigationViaHardBackBeValid() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.FOLDER_1)
        onFiles.openFolder(FilesAndFolders.FOLDER_2)
        onFiles.openFolder(FilesAndFolders.FOLDER_3)
        onFiles.shouldFolderBeOpened(FilesAndFolders.FOLDER_3)
        onFiles.pressHardBack()
        onFiles.shouldFolderBeOpened(FilesAndFolders.FOLDER_2)
        onFiles.pressHardBack()
        onFiles.shouldFolderBeOpened(FilesAndFolders.FOLDER_1)
        onFiles.pressHardBack()
        onBasePage.shouldBeOnFiles()
    }

    @Test
    @TmsLink("5467")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.FOLDER_1, FilesAndFolders.FOLDER_1 + "/" + FilesAndFolders.FOLDER_2, FilesAndFolders.FOLDER_1 + "/" + FilesAndFolders.FOLDER_2 + "/" + FilesAndFolders.FOLDER_3])
    @DeleteFiles(files = [FilesAndFolders.FOLDER_1])
    fun shouldFilesNavigationViaSoftBackBeValid() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.FOLDER_1)
        onFiles.openFolder(FilesAndFolders.FOLDER_2)
        onFiles.openFolder(FilesAndFolders.FOLDER_3)
        onFiles.shouldFolderBeOpened(FilesAndFolders.FOLDER_3)
        onFiles.navigateUp()
        onFiles.shouldFolderBeOpened(FilesAndFolders.FOLDER_2)
        onFiles.navigateUp()
        onFiles.shouldFolderBeOpened(FilesAndFolders.FOLDER_1)
        onFiles.navigateUp()
        onFiles.shouldBeOnDiskRoot()
    }

    @Test
    @TmsLink("5470")
    @Category(Regression::class)
    fun shouldFilesNavigationToDownloadsBeValid() {
        onBasePage.openFiles()
        onFiles.openBubble(FilesAndFolders.DOWNLOADS)
        onFiles.shouldFolderBeOpened(FilesAndFolders.DOWNLOADS)
        onFiles.shouldBubblesBeVisible(false)
    }

    @Test
    @TmsLink("5473")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @DeleteFiles(files = [FilesAndFolders.FIRST])
    fun shouldFilesNavigationToOfflineBeValid() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FIRST)
        onFiles.openBubble(FilesAndFolders.OFFLINE)
        onFiles.shouldFolderBeOpened(FilesAndFolders.OFFLINE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FIRST)
        onFiles.shouldBubblesBeVisible(false)
    }

    @Test
    @TmsLink("5474")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.FOLDER_1])
    @UploadFiles(
        targetFolder = FilesAndFolders.FOLDER_1,
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH, FilesAndFolders.BIG_FILE, FilesAndFolders.BIG_IMAGE, FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.PHOTO, FilesAndFolders.PNG_IMAGE]
    )
    @DeleteFiles(files = [FilesAndFolders.FOLDER_1])
    fun shouldNavigationToFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.FOLDER_1)
        onFiles.shouldFolderBeOpened(FilesAndFolders.FOLDER_1)
        onFiles.shouldActionBarBeVisible(true)
        onFiles.shouldBubblesBeVisible(false)
        onFiles.shouldSeeFabButton()
        onFiles.updateFileList()
        onFiles.ensureFilesExist(
            FilesAndFolders.FIRST,
            FilesAndFolders.SECOND,
            FilesAndFolders.THIRD,
            FilesAndFolders.FOURTH,
            FilesAndFolders.FIFTH,
            FilesAndFolders.BIG_FILE,
            FilesAndFolders.BIG_IMAGE,
            FilesAndFolders.FILE_FOR_VIEWING_1,
            FilesAndFolders.FILE_FOR_VIEWING_2,
            FilesAndFolders.PHOTO,
            FilesAndFolders.PNG_IMAGE
        )
    }

    @Test
    @TmsLink("3454")
    @Category(Regression::class)
    fun shouldOpenDefaultPartition() {
        onMobile.openAppSwitcher()
        onRecentApps.shouldCloseAllAppsInBackground()
        onLogin.startAppAndCloseWizards()
        onBasePage.shouldBeOnFeed()
    }

    @Test
    @TmsLink("5458")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH])
    fun shouldBackFromContentBlockToFeed() {
        onBasePage.openFeed()
        onFeedPage.expandFeedBlock()
        onContentBlock.shouldBeContentBlockOpened()
        onFeedPage.pressHardBack()
        onBasePage.shouldBeOnFeed()
    }

    @Test
    @TmsLink("5485")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(Regression::class)
    fun shouldBackNavigateFromVistaToPhotos() {
        onBasePage.openPhotos()
        onBasePage.shouldBeOnPhotos()
        onPhotos.openVista()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnPhotos()
    }
}
