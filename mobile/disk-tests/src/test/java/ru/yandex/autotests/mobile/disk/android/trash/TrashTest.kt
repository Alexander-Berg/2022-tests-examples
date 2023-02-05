package ru.yandex.autotests.mobile.disk.android.trash

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import com.google.inject.name.Named
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CleanTrash
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.DiskApiSteps
import ru.yandex.autotests.mobile.disk.android.steps.FilesSteps
import ru.yandex.autotests.mobile.disk.android.steps.TrashSteps
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.*
import ru.yandex.autotests.mobile.disk.data.MoveFilesToTrashSnackBarMessages
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Trash")
@UserTags("trash")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class TrashTest {
    companion object {
        @JvmField
        @ClassRule
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onUserDiskApi: DiskApiSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onTrash: TrashSteps

    @Test
    @TmsLink("1567")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING, ORIGINAL_FILE])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, ORIGINAL_FILE, FOLDER_FOR_RESTORING])
    @Category(Regression::class)
    fun shouldRestoreFileOrFolderFromTrash() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, ORIGINAL_FILE, FOLDER_FOR_RESTORING)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, ORIGINAL_FILE, FOLDER_FOR_RESTORING)
        onTrash.shouldRestoreFilesFromTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onBasePage.pressHardBack()
        onFiles.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
    }

    @Test
    @TmsLink("1570")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, FOLDER_FOR_RESTORING])
    @Category(Regression::class)
    fun shouldRemoveAllFilesFromTrash() {
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.emptyTrash()
        onBasePage.shouldSeeToastWithMessage(ToastMessages.TRASH_SUCCESSFULLY_EMPTIED)
        onTrash.shouldTrashBeEmpty()
    }

    @Test
    @TmsLink("1566")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, FOLDER_FOR_RESTORING])
    @Category(FullRegress::class)
    fun shouldRestoreAllFilesFromTrash() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.shouldRestoreFilesFromTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.shouldTrashBeEmpty()
        onBasePage.pressHardBack() // escape from trash
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
    }

    @Test
    @TmsLink("1569")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, FOLDER_FOR_RESTORING])
    @Category(FullRegress::class)
    fun shouldCancelRemoveAllFilesFromTrash() {
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.shouldCancelEmptyTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
    }

    @Test
    @TmsLink("1571")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, FOLDER_FOR_RESTORING])
    @Category(FullRegress::class)
    fun shouldRemoveFilesFromTrash() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.shouldDeleteFilesFromTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.shouldTrashBeEmpty()
    }

    @Test
    @TmsLink("1572")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING, ORIGINAL_FILE])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, ORIGINAL_FILE, FOLDER_FOR_RESTORING])
    @Category(FullRegress::class)
    fun shouldSeeCachedTrashContent() {
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.navigateUp()
        onBasePage.openFiles()
        onBasePage.switchToAirplaneMode()
        onBasePage.openTrash()
        onUserDiskApi.moveFilesToTrash(ORIGINAL_FILE)
        onTrash.updateFileList()
        onTrash.shouldSeeToastWithMessage(ToastMessages.TRASH_UPDATE_ERROR_TOAST)
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.shouldNotExistFilesOrFolders(ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1573")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, FOLDER_FOR_RESTORING])
    @Category(FullRegress::class)
    fun shouldNotSeeNonCachedTrashContent() {
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onBasePage.openFiles()
        onBasePage.switchToAirplaneMode()
        onBasePage.openTrash()
        onTrash.updateFileList()
        onTrash.shouldSeeToastWithMessage(ToastMessages.TRASH_UPDATE_ERROR_TOAST)
        onTrash.shouldNotExistFilesOrFolders(FILE_FOR_RESTORING, FOLDER_FOR_RESTORING)
        onTrash.shouldSeeTrashLoadingErrorStub()
        onTrash.shouldTrashBottomPanelBeNotPresented()
    }

    @Test
    @TmsLink("1568")
    @CleanTrash
    @UploadFiles(filePaths = [ORIGINAL_FILE, ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [ORIGINAL_FILE, ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldRemoveFileFromTrashWhenRemovedOverWeb() {
        onUserDiskApi.moveFilesToTrash(ORIGINAL_FILE, ORIGINAL_TEXT_FILE)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(ORIGINAL_FILE, ORIGINAL_TEXT_FILE)
        val trashSizeBeforeOperation = onTrash.getCurrentTrashSize()
        onUserDiskApi.removeFromTrash(ORIGINAL_FILE)
        onTrash.updateFileList()
        onTrash.shouldNotExistFilesOrFolders(ORIGINAL_FILE)
        val currentTrashSize = onTrash.getCurrentTrashSize()
        MatcherAssert.assertThat(currentTrashSize, Matchers.lessThan(trashSizeBeforeOperation))
    }

    @Test
    @TmsLink("1576")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING])
    @DeleteFiles(files = [FILE_FOR_RESTORING, FILE_FOR_RESTORING_DUPLICATED])
    @Category(FullRegress::class)
    fun shouldRenameRestoredFromTrashFileWhenDuplicated() {
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING)
        onUserDiskApi.uploadFileToFolder(FILE_FOR_RESTORING, DISK_ROOT)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FILE_FOR_RESTORING)
        onTrash.shouldRestoreFilesFromTrash(FILE_FOR_RESTORING)
        onTrash.navigateUp()
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FILE_FOR_RESTORING, FILE_FOR_RESTORING_DUPLICATED)
    }

    @Test
    @TmsLink("2321")
    @CleanTrash
    @UploadFiles(filePaths = [FILE_FOR_RESTORING, FIRST, SECOND, THIRD, FOURTH, FIFTH, ORIGINAL_FILE, ORIGINAL_TEXT_FILE])
    @CreateFolders(folders = [FOLDER_FOR_RESTORING, ORIGINAL_FOLDER, TARGET_FOLDER])
    @DeleteFiles(files = [FILE_FOR_RESTORING, FIRST, SECOND, THIRD, FOURTH, FIFTH, ORIGINAL_FILE, ORIGINAL_TEXT_FILE, FOLDER_FOR_RESTORING, ORIGINAL_FOLDER, TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldTrashObjectCounterSeeCorrectValue() {
        onUserDiskApi.moveFilesToTrash(FILE_FOR_RESTORING, FIRST, SECOND, THIRD, FOURTH, FIFTH, ORIGINAL_FILE,
            ORIGINAL_TEXT_FILE, FOLDER_FOR_RESTORING, ORIGINAL_FOLDER, TARGET_FOLDER)
        onBasePage.openTrash()
        onTrash.shouldSelectFilesOrFolders(FILE_FOR_RESTORING, FIRST, SECOND, THIRD, FOURTH, FIFTH, ORIGINAL_FILE,
            ORIGINAL_TEXT_FILE, FOLDER_FOR_RESTORING, ORIGINAL_FOLDER, TARGET_FOLDER)
        onTrash.shouldTrashCounterEqual(11)
    }

    @Test
    @TmsLink("1518")
    @CleanTrash
    @UploadFiles(filePaths = [ORIGINAL_FILE], targetFolder = CONTAINER_FOLDER)
    @CreateFolders(folders = [CONTAINER_FOLDER, TARGET_FOLDER, ORIGINAL_FOLDER])
    @DeleteFiles(files = [CONTAINER_FOLDER, TARGET_FOLDER, ORIGINAL_FOLDER])
    @Category(BusinessLogic::class) //Acceptance
    fun shouldDeleteFolder() {
        onBasePage.openFiles()
        onFiles.deleteFilesOrFolders(CONTAINER_FOLDER)
        onTrash.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FOLDER)
        onTrash.shouldNotExistFilesOrFolders(CONTAINER_FOLDER)
        onUserDiskApi.uploadFileToFolder(ORIGINAL_FILE, TARGET_FOLDER)
        onUserDiskApi.createFolders(TARGET_FOLDER + "/" + ORIGINAL_FOLDER)
        onFiles.deleteFilesOrFolders(TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(TARGET_FOLDER)
        onFiles.deleteFilesOrFolders(ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(ORIGINAL_FOLDER)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(CONTAINER_FOLDER, TARGET_FOLDER, ORIGINAL_FOLDER)
    }
}
