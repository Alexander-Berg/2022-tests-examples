package ru.yandex.autotests.mobile.disk.android.files

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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.FilesSteps
import ru.yandex.autotests.mobile.disk.android.steps.GroupModeSteps
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Files")
@UserTags("filesDisplayMode")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FilesTest {
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
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Test
    @TmsLink("6242")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFilesGroupModeStateBeSavedAfterPullToRefresh() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldEnableGroupOperationMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onGroupMode.shouldFilesOrFolderBeNotSelected(FilesAndFolders.THIRD)
        onFiles.updateFileList()
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onGroupMode.shouldFilesOrFolderBeNotSelected(FilesAndFolders.THIRD)
    }

    @Test
    @TmsLink("7205")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.TARGET_FOLDER])
    fun shouldFilesElementsBeVisible() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onBasePage.shouldProfilePicBeDisplayed()
        onFiles.shouldBubblesBeVisible(true)
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FIRST, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeFabButton()
    }

    @Test
    @TmsLink("7959")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.TARGET_FOLDER])
    fun shouldFilesGridBeDefault() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onBasePage.shouldProfilePicBeDisplayed()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFileListDisplayTypeBeGrid()
    }

    @Test
    @TmsLink("7968")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    fun shouldFilesBeInGridModeIfSeveralFilesForFilesPartition() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.shouldChangeLayoutOptionBeDisplayed()
    }

    @Test
    @TmsLink("7970")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFilesBeInGridModeIfSeveralFilesForFolder() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.shouldChangeLayoutOptionBeDisplayed()
    }

    @Test
    @TmsLink("7971")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFilesBeInListModeIfSingleFilesForFolder() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.deleteFilesOrFolders(FilesAndFolders.FIRST)
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7973")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @DeleteFiles(files = [FilesAndFolders.FIRST])
    fun shouldGroupModeBeActivatedByLongTap() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FIRST)
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldNCheckboxesBeChecked(1)
        onGroupMode.shouldNFilesBeCounted(1)
        onGroupMode.shouldMarkAsOfflineButtonBeEnabled()
        onGroupMode.shouldShareButtonBeEnabled()
        onFiles.shouldNotSeeFabButton()
    }

    @Test
    @TmsLink("7976")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFilesGroupModeStateBeSavedAfterRotation() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldEnableGroupOperationMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onGroupMode.shouldFilesOrFolderBeNotSelected(FilesAndFolders.THIRD)
        onBasePage.rotate(ScreenOrientation.LANDSCAPE)
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onGroupMode.shouldFilesOrFolderBeNotSelected(FilesAndFolders.THIRD)
    }
}
