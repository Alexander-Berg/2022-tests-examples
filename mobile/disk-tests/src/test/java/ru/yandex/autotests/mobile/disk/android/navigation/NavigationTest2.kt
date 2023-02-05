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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
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
class NavigationTest2 : NavigationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("5464")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @DeleteFiles(files = [FilesAndFolders.FIRST])
    fun shouldFilesPartitionRestoreAfterGroupOperationClosed() {
        onBasePage.openFiles()
        onFiles.shouldEnableGroupOperationMode()
        onBasePage.shouldBeOnGroupMode()
        onBasePage.shouldNotSeeTabs()
        onFiles.shouldBubblesBeVisible(false)
        onFiles.shouldNotSeeFabButton()
        onGroupMode.shouldClickCloseButton()
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldBubblesBeVisible(true)
        onFiles.shouldSeeFabButton()
        onBasePage.shouldSeeTabs()
    }

    @Test
    @TmsLink("5451")
    @Category(Regression::class)
    fun shouldNavigationBeDisplayed() {
        onBasePage.shouldSeeFeedTab()
        onBasePage.shouldSeeFilesTab()
        onBasePage.shouldSeePhotosTab()
        onBasePage.shouldSeeAlbumsTab()
        onBasePage.shouldSeeMail360Tab()
    }

    @Test
    @TmsLink("5658")
    @Category(Regression::class)
    fun shouldBackNavigateToPhotos() {
        onBasePage.openPhotos()
        onBasePage.shouldBeOnPhotos()
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnPhotos()
        onBasePage.pressHardBack()
        onBasePage.shouldApplicationBeNotInForeground()
    }

    @Test
    @TmsLink("5659")
    @Category(Quarantine::class)
    fun shouldBackNavigateToNotes() {
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnNotes()
        onBasePage.pressHardBack()
        onBasePage.shouldApplicationBeNotInForeground()
    }

    @Test
    @TmsLink("5657")
    @Category(Regression::class)
    fun shouldBackNavigateToFiles() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFiles()
        onBasePage.pressHardBack()
        onBasePage.shouldApplicationBeNotInForeground()
    }

    @Test
    @TmsLink("5656")
    @Category(Regression::class)
    fun shouldBackNavigateToFeed() {
        onBasePage.openFeed()
        onBasePage.shouldBeOnFeed()
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFeed()
        onBasePage.pressHardBack()
        onBasePage.shouldApplicationBeNotInForeground()
    }

    @Test
    @TmsLink("5533")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    fun shouldBackToFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFeed()
        onBasePage.shouldBeOnFeed()
        onBasePage.pressHardBack()
        onFiles.shouldFolderBeOpened(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("5660")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    fun shouldFilesFromFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFiles()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFeed()
    }
}
