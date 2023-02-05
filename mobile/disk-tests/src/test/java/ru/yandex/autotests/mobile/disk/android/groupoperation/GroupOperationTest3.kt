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
import java.util.concurrent.TimeUnit

@Feature("Group mode")
@UserTags("groupMode")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GroupOperationTest3 : GroupOperationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2194")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldClearSelectionActionBeDisabledWhenNotAllFilesAreSelected() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldEnableGroupOperationMode()
        onGroupMode.shouldNCheckboxesBeChecked(0) //nothing selected
        onGroupMode.shouldNFilesBeCounted(0)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.CLEAR_SELECTION)
        onGroupMode.shouldActionBePresented(FileActions.SELECT_ALL)
    }

    @Test
    @TmsLink("2198")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeAllActionWhenAllFilesBeSelected() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldEnableGroupOperationMode()
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.SELECT_ALL)
        onGroupMode.shouldMarkAsOfflineButtonBeEnabled()
        onGroupMode.shouldShareButtonBeEnabled()
        onGroupMode.clickShareMenu()
        onGroupMode.shouldShareLinkVariantBeEnabled(true)
        onGroupMode.shouldShareOriginalFileVariantBeEnabled()
        onGroupMode.pressHardBack() //close share menu
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(
            FileActions.MOVE,
            FileActions.COPY,
            FileActions.DOWNLOAD,
            FileActions.DELETE,
            FileActions.CLEAR_SELECTION
        )
    }

    @Test
    @TmsLink("2197")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldSelectAllAndClearSelectionActionsBeAvailable() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.wait(10, TimeUnit.SECONDS) //TODO:Try change explicit sleep to waiting animation end in MOBDISK-10287
        onGroupMode.shouldSelectAllAndClearSelectionBeAvailable()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onGroupMode.shouldSelectAllAndClearSelectionBeAvailable()
        onFiles.navigateUp() //escape from container folder;
        onBasePage.openOffline()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onGroupMode.shouldSelectAllAndClearSelectionBeAvailable()
    }

    @Test
    @TmsLink("2445")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldSelectAllAndClearSelectionActionsBeNotAvailable() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openOffline()
        onBasePage.shouldBeOnOffline()
        onGroupMode.shouldMoreActionButtonBeDisabledOnActionMode()
    }

    @Test
    @TmsLink("5024")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldGroupOperationModeEnabledOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_GO)
        onBasePage.openOffline()
        onOffline.shouldEnableGroupOperationMode()
        onOffline.shouldNFilesBeCounted(0)
        onOffline.shouldNCheckboxesBeEnabled(0)
    }

    @Test
    @TmsLink("2199")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldClearSelectionActionBeAvailable() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.shouldBeOnGroupMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.shouldNCheckboxesBeChecked(2)
        onGroupMode.shouldNFilesBeCounted(2)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.CLEAR_SELECTION)
        onGroupMode.shouldActionNotBePresented(FileActions.SELECT_ALL)
    }

    @Test
    @TmsLink("2200")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldKeepSelectionWhenApplicationReturnFromBackground() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.enableGroupOperationMode()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.SELECT_ALL)
        onGroupMode.applyAction(FileActions.SELECT_ALL)
        onGroupMode.runInBackground(3, TimeUnit.SECONDS)
        onGroupMode.shouldNCheckboxesBeChecked(3)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.CLEAR_SELECTION)
        onGroupMode.applyAction(FileActions.CLEAR_SELECTION)
        onGroupMode.runInBackground(3, TimeUnit.SECONDS)
        onGroupMode.shouldNCheckboxesBeChecked(0)
    }

    @Test
    @TmsLink("2192")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldClearSelectionBeAvailableWhenAllFilesAreSelectedBySelectAll() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onBasePage.enableGroupOperationMode()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.SELECT_ALL)
        onGroupMode.applyAction(FileActions.SELECT_ALL)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.CLEAR_SELECTION)
    }
}
