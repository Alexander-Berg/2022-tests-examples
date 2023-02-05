package ru.yandex.autotests.mobile.disk.android.groupoperation

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Group mode")
@UserTags("groupMode")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GroupOperationTest2 : GroupOperationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1799")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldGroupOperationModeEnabledByLongTapOnFiles() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_GO)
        onFiles.shouldNFilesBeCounted(1)
        onFiles.shouldNCheckboxesBeEnabled(1)
        onGroupMode.shouldFileOrFolderHasCheckbox(FilesAndFolders.FILE_FOR_GO)
    }

    @Test
    @TmsLink("5044")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldGroupOperationModeEnabledByLongTapOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_GO)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_GO)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_GO)
        onFiles.shouldNFilesBeCounted(1)
        onFiles.shouldNCheckboxesBeEnabled(1)
        onGroupMode.shouldFileOrFolderHasCheckbox(FilesAndFolders.FILE_FOR_GO)
    }

    @Test
    @TmsLink("1797")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldGroupCloseGroupModeByCancelOnFiles() {
        onBasePage.openFiles()
        onBasePage.enableGroupOperationMode()
        onBasePage.shouldBeOnGroupMode()
        onBasePage.closeGroupMode()
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("1796")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldGroupCloseGroupModeByDeselect() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_GO)
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.FILE_FOR_GO)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("1805")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(FullRegress::class)
    fun shouldNotOpenFileInGo() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
        onBasePage.enableGroupOperationMode()
        onBasePage.shouldBeOnGroupMode()
        onFiles.shouldNotOpenFileIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("1806")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO], targetFolder = FilesAndFolders.TEST_FOLDER_NAME)
    @CreateFolders(folders = [FilesAndFolders.TEST_FOLDER_NAME])
    @DeleteFiles(files = [FilesAndFolders.TEST_FOLDER_NAME])
    @Category(FullRegress::class)
    fun shouldNotOpenFolderInGo() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
        onBasePage.enableGroupOperationMode()
        onFiles.openFolder(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FILE_FOR_GO)
    }

    @Test
    @TmsLink("2375")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldCloseGroupModeWhenFilesSelected() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_GO)
        onBasePage.shouldBeOnGroupMode()
        onBasePage.closeGroupMode()
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldNCheckboxesBeEnabled(0)
    }

    @Test
    @TmsLink("2193")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldSelectAllActionBePresentedWhenNotAllFilesAreSelected() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldEnableGroupOperationMode()
        val fileCount = 2 //Original file and original text file.
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.SELECT_ALL)
        onGroupMode.applyAction(FileActions.SELECT_ALL)
        onGroupMode.shouldNFilesBeCounted(fileCount)
        onGroupMode.shouldNCheckboxesBeChecked(fileCount)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.SELECT_ALL)
        onGroupMode.shouldActionBePresented(FileActions.CLEAR_SELECTION)
        onGroupMode.pressHardBack() //close file actions menu
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.SELECT_ALL)
        onGroupMode.shouldActionNotBePresented(FileActions.CLEAR_SELECTION)
    }
}
