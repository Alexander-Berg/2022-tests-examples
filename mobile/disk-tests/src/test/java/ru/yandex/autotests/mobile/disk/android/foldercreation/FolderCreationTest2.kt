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
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Create folders")
@UserTags("createFolder")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FolderCreationTest2 : FolderCreationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1491")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCreateFolderWithSlash() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotCreateFolderWithIncorrectName(FilesAndFolders.TEST_FOLDER_WITH_SLASH_NAME)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.TEST_FOLDER_WITH_SLASH_NAME)
    }

    @Test
    @TmsLink("1495")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCreateFolderHierarchy() {
        onBasePage.openFiles()
        //go to bottom of folders hierarchy
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.createFolder(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.openFolder(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.createFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)

        //go to up of folders hierarchy
        onFiles.pressHardBack()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.pressHardBack()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.pressHardBack()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1504")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldCreateFolderWhenFilesMoving() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenMoveDialogForFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.createFolderOnMoveDialog(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1501")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldNotCreateFolderInReadOnlyDirectory() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldNotSeeFabButton()
    }

    @Test
    @TmsLink("1502")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldCreateFolderInFullAccessDirectory() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.createFolder(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
    }

    @Test
    @TmsLink("1487")
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.DOWNLOADS, FilesAndFolders.SCREENSHOTS, FilesAndFolders.YANDEX_IMAGES, FilesAndFolders.SOCIAL_NETWORKS])
    @Category(FullRegress::class)
    fun shouldCreateDirectoryWithSystemName() {
        onBasePage.openFiles()
        for (dirName in arrayOf(
            FilesAndFolders.CAMERA_UPLOADS,
            FilesAndFolders.DOWNLOADS,
            FilesAndFolders.SCREENSHOTS,
            FilesAndFolders.YANDEX_IMAGES,
            FilesAndFolders.SOCIAL_NETWORKS
        )) {
            onFiles.createFolder(dirName)
            onFiles.shouldFilesOrFoldersExist(dirName)
            onFiles.updateFileList()
            onFiles.shouldFilesOrFoldersExist(dirName)
        }
    }

    @Test
    @TmsLink("1489")
    @Category(FullRegress::class)
    fun shouldNotCreateFolderWithEmptyName() {
        onBasePage.openFiles()
        onFiles.shouldNotCreateFolderWithIncorrectName(StringUtils.EMPTY)
    }
}
