package ru.yandex.autotests.mobile.disk.android.foldercreation

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.apache.commons.lang3.StringUtils
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
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Create folders")
@UserTags("createFolder")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FolderCreationTest : FolderCreationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1488")
    @DeleteFiles(files = [FilesAndFolders.TEST_FOLDER_NAME])
    @Category(Regression::class)
    fun shouldViewCreatedFolderAfterUpdate() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openCreateFolderDialog()
        onFiles.shouldCreateFolderDialogBePresented()
        onMobile.shouldKeyboardShown()
        onFiles.enterNewFolderName(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
    }

    @Test
    @TmsLink("1500")
    @Category(FullRegress::class)
    fun shouldNotCreateFolderAfterCancel() {
        onBasePage.openFiles()
        onFiles.cancelFolderCreation(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.TEST_FOLDER_NAME)
    }

    @Test
    @TmsLink("2894")
    @DeleteFiles(files = [FilesAndFolders.SYMBOL_FOLDER, FilesAndFolders.TURKISH_FOLDER, FilesAndFolders.CYRILLIC_FOLDER, FilesAndFolders.UKRAINIAN_FOLDER, FilesAndFolders.EMOJI_FOLDER, FilesAndFolders.DIGIT_FOLDER])
    @Category(FullRegress::class)
    fun shouldCreateFoldersWithArtifactInName() {
        val dirNames = arrayOf(
            FilesAndFolders.SYMBOL_FOLDER,
            FilesAndFolders.TURKISH_FOLDER,
            FilesAndFolders.CYRILLIC_FOLDER,
            FilesAndFolders.UKRAINIAN_FOLDER,
            FilesAndFolders.EMOJI_FOLDER,
            FilesAndFolders.DIGIT_FOLDER
        )
        onBasePage.openFiles()
        for (dirName in dirNames) {
            onFiles.createFolder(dirName)
            onFiles.shouldFilesOrFoldersExist(dirName)
            onFiles.updateFileList()
            onFiles.shouldFilesOrFoldersExist(dirName)
        }
    }

    @Test
    @TmsLink("1497")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCreateFolderWithNameLikeAsExistedFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.createFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeeToastContainedText(ToastMessages.ERROR_OCCURRED_WHILE_CREATING_TOAST)
        onFiles.shouldFileListHasSize(1) // only one folder must be on file list
        //check folder not replaced by empty folder
        onDiskApi.shouldFileExist(FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1486")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCreateFolderWithNameLikeAsExistedFile() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.createFolder(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeeToastContainedText(ToastMessages.ERROR_OCCURRED_WHILE_CREATING_TOAST)
        onFiles.shouldFileListHasSize(1) // only one file must be on file list
        //check file not replaced by empty folder
        onDiskApi.shouldFileExist(FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1499")
    @Category(FullRegress::class)
    fun shouldNotCreateFolderWithEmptyNameWhenCanceled() {
        onBasePage.openFiles()
        onFiles.shouldNotCreateFolderWithIncorrectName(StringUtils.EMPTY)
        onFiles.shouldNotExistFilesOrFolders(StringUtils.EMPTY)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(StringUtils.EMPTY)
    }

    @Test
    @TmsLink("1493")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldTrimSpaceOnEndOfFolderName() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.createFolder(FilesAndFolders.ORIGINAL_FOLDER + StringUtils.SPACE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFileListHasSize(1) //only one folder presented
        onDiskApi.shouldFolderExist(FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1492")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldTrimSpaceOnBeginOfFolderName() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.createFolder(StringUtils.SPACE + FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFileListHasSize(1)
        onDiskApi.shouldFolderExist(FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1494")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldTrimSpaceOnEndAndBeginOfFolderName() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.createFolder(StringUtils.SPACE + FilesAndFolders.ORIGINAL_FOLDER + StringUtils.SPACE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFileListHasSize(1)
        onDiskApi.shouldFolderExist(FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1496")
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCreateFolderOnAirplaneMode() {
        onBasePage.openFiles()
        onMobile.switchToAirplaneMode()
        onFiles.createFolder(FilesAndFolders.TARGET_FOLDER)
        onMobile.shouldSeeToastContainedText(ToastMessages.ERROR_OCCURRED_WHILE_CREATING_TOAST)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onFiles.switchToData()
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
    }
}
