package ru.yandex.autotests.mobile.disk.android.copylink

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
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.*
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Copy link")
@UserTags("copyLink")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class CopyLinkTest2 : CopyLinkTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2479")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(Quarantine::class)
    fun shouldShareButtonNotPresentedForFolderIntoReadOnlyDir() {
        val sharedFolder = nameHolder.name
        onShareDiskApi.createFolders(sharedFolder + "/" + FilesAndFolders.TARGET_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.shouldShareButtonBeNotPresented()
    }

    @Test
    @TmsLink("2481")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Quarantine::class)
    fun shouldShareFileIntoFullAccessDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2838")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldBeDisplayedSharingMenu() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickShareMenu()
        onGroupMode.shouldShareLinkVariantBeEnabled(false)
        onGroupMode.shouldShareOriginalFileVariantBeEnabled()
        onBasePage.pressHardBack()
        onBasePage.closeGroupMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickShareMenu()
        onGroupMode.shouldNotDisplaySharingMenu(true)
        onBasePage.pressHardBack()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER)
        onDiskApi.unpublishFile(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickShareMenu()
        onGroupMode.shouldNotDisplaySharingMenu(false)
        onBasePage.pressHardBack()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1223")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.CAMERA_UPLOADS)
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(FullRegress::class)
    fun shouldCopyLinkToFilesIntoCameraUploads() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("1242")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldRenamePublicFile() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("1244")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldRenamePublicFolder() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.RENAMED_FOLDER)
    }

    @Test
    @TmsLink("1243")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMovePublicFile() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1245")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMovePublicFolder() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("2832")
    @UploadFileWithName(filePath = FilesAndFolders.ORIGINAL_FILE, name = FilesAndFolders.SYMBOL_ORIGINAL_FILE) // and \
    @DeleteFiles(files = [FilesAndFolders.SYMBOL_ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldCopyLinkToFileWithSymbolsName() {
        onBasePage.openFiles()
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.SYMBOL_ORIGINAL_FILE)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.SYMBOL_ORIGINAL_FILE)
    }
}
