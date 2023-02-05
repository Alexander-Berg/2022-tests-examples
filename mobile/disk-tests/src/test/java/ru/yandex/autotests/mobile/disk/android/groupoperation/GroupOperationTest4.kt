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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Group mode")
@UserTags("groupMode")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GroupOperationTest4 : GroupOperationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1808")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldGroupOperationEnabledInSearchWindow() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, 1)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNFilesBeCounted(1)
        onFiles.shouldNCheckboxesBeEnabled(1)
    }

    @Test
    @TmsLink("1803")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotDisplayedFABinGroupOperations() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotSeeFabButton()
    }

    @Test
    @TmsLink("1804")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotDisplayedFABinGroupOperationsAfterReturnAppFromBackground() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onCommon.runInBackground(3, TimeUnit.SECONDS)
        onFiles.shouldNotSeeFabButton()
    }

    @Test
    @TmsLink("1810")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldCloseGroupOperationModeWhenOperationProcessed() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_GO
        )
        onBasePage.shouldBeNotOnGroupMode()
        //just delete files from offline for simplify next test operation.
        onFiles.deleteFromOffline(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_GO
        )
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.COPY,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.FILE_FOR_GO
        )
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_GO)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.deleteFilesOrFolders(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_GO)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("2350")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldKeepCheckboxesWhenRotateDevice() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO)
        onFiles.rotate(ScreenOrientation.LANDSCAPE)
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO)
        onGroupMode.shouldNFilesBeCounted(2)
    }
}
