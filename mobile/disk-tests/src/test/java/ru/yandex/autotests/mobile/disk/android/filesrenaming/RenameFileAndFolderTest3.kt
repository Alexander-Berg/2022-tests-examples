package ru.yandex.autotests.mobile.disk.android.filesrenaming

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.apache.commons.lang3.StringUtils
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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Rename files and folders")
@UserTags("renaming")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class RenameFileAndFolderTest3 : RenameFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1628")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldRenameActionBeDisabledInReadOnlyDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.RENAME)
    }

    @Test
    @TmsLink("2437")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromOfflineWhenFileRenamedInOfflineSection() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.shouldRenameFileOrFolderAddedToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onBasePage.pressHardBack() //close offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("2438")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenFolderRenamedInOfflineSection() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.shouldRenameFileOrFolderAddedToOffline(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.RENAMED_FOLDER
        )
        onOffline.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onBasePage.pressHardBack() //close offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("1625")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldRenameReadOnlyDirOnlyForParticipant() {
        val name = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(name)
        onFiles.renameFileOrFolder(name, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onShareDiskApi.shouldFolderExist(name)
    }

    @Test
    @TmsLink("1624")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RW))
    @Category(FullRegress::class)
    fun shouldRenameFullAccessDirOnlyForParticipant() {
        val name = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(name)
        onFiles.renameFileOrFolder(name, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onShareDiskApi.shouldFolderExist(name)
    }

    @Test
    @TmsLink("2316")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotRenameFileToEmptyName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotRenameFileOrFolderToIncorrectName(FilesAndFolders.ORIGINAL_FILE, StringUtils.EMPTY)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }
}
