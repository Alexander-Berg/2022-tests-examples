package ru.yandex.autotests.mobile.disk.android.offlinefolders

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.hamcrest.Matchers
import org.hamcrest.junit.MatcherAssert
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.DeleteSharedFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Offline folders")
@UserTags("offlineFolders")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFoldersTest2 : OfflineFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1053")
    @DeleteSharedFolders
    @Category(Regression::class)
    fun shouldAddFolderFromSharedFolder() {
        val faDir = nameHolder.generateName().name
        val roDir = nameHolder.generateName().name
        onShareDiskApi.createFolders(faDir, roDir)
        onShareDiskApi.uploadFileToFolder(FilesAndFolders.PHOTO, faDir)
        onShareDiskApi.uploadFileToFolder(FilesAndFolders.PHOTO, roDir)
        onShareDiskApi.shareFolder(faDir)
        onShareDiskApi.inviteUser(faDir, testAccount, Rights.RW)
        onShareDiskApi.shareFolder(roDir)
        onShareDiskApi.inviteUser(roDir, testAccount, Rights.RO)
        onUserDiskApi.activateInvite() //activate all invites
        val folders = arrayOf(faDir, roDir)
        for (folder in folders) {
            onBasePage.openSettings()
            val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
            onSettings.closeSettings()
            onSettings.closeProfile()
            onBasePage.openFiles()
            onFiles.updateFileList()
            onFiles.addFilesOrFoldersToOffline(folder!!)
            onBasePage.openSettings()
            val currentOfflineCacheSize = onSettings.currentOfflineSize
            MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.greaterThan(offlineCacheSizeBeforeOperation))
            onSettings.closeSettings()
            onSettings.closeProfile()
            onBasePage.openOffline()
            onOffline.shouldSeeOfflineFileMarker(folder)
            onFiles.deleteFromOffline(folder)
            onOffline.shouldSeeOfflineStub()
            onOffline.pressHardBack() //escape from offline
        }
    }

    @Test
    @TmsLink("1079")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(Regression::class)
    fun shouldUpdateOfflineFolderFileListAfterPullToRefresh() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onUserDiskApi.uploadFileToFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1083")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(Quarantine::class)
    fun shouldClearOfflineFromSettingsBySelecting() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE
        )
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onBasePage.pressHardBack()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.clearOfflineFilesBySelection()
        onGroupMode.shouldFilesOrFolderBeSelected(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE
        )
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.removeSelectedFilesFromOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.pressHardBack()
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("1096")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldDecreaseOfflineSectionSizeWhenChildFolderRemovedFromOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateToRoot() // escape from offline
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateToRoot()
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("1109")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldRemoveChildFolderFromOfflineWhenParentRenamed() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp() //escape from container folder
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldRenameOfflineAlertMessageBeDisplayed()
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.shouldFillNewFileNameFieldAndRenameFile(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1111")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldRemoveFolderFromOfflineWhenRenamed() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldRenameOfflineAlertMessageBeDisplayed()
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.shouldFillNewFileNameFieldAndRenameFile(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1068")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldRemoveFolderFromOfflineWhenFileSavingToOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.deleteFromOffline(FilesAndFolders.TARGET_FOLDER) //Big file unmark from offline when saved.
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
    }
}
