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
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.MoveFilesToTrashSnackBarMessages

@Feature("Offline folders")
@UserTags("offlineFolders")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFoldersTest5 : OfflineFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1062")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldAddTwoFoldersWithSameNameToOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFileListHasSize(2)
        onFiles.shouldSpecificAmountOfObjectWithSpecificNameExist(FilesAndFolders.ORIGINAL_FOLDER, 2)
    }

    @Test
    @TmsLink("1065")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeOfflineStubAfterDeletingLastFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openSettings()
        var currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.not(Matchers.equalTo(0L)))
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.deleteFromOffline(FilesAndFolders.TARGET_FOLDER)
        onOffline.shouldSeeOfflineStub()
        onOffline.navigateUp()
        onBasePage.openSettings()
        currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.equalTo(0L))
    }

    @Test
    @TmsLink("1066")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenDeletedOnClient() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.deleteFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1067")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onUserDiskApi.removeFiles(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1069")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemoveFileFromOfflineFolderWhenDeletedOnClient() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineFileMarker(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateToRoot() //escape to Files
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1070")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemoveFileFromOfflineFolderWhenDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateToRoot() //escape to Files
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.removeFiles(FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineFileMarker(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateToRoot() //escape to Files
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1071")
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteContainerFolderOfOfflineFolderOnClient() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onFiles.deleteFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FOLDER)
    }
}
