package ru.yandex.autotests.mobile.disk.android.offlinefiles

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileWithName
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.DeleteSharedFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Offline files")
@UserTags("offlineFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFilesTest2 : OfflineFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("978")
    @DeleteSharedFolders
    @Category(Regression::class)
    fun shouldAddFileFromSharedFolders() {
        val fullAccessDir = nameHolder.generateName().name
        val readOnlyDir = nameHolder.generateName().name
        onShareDiskApi.createFolders(fullAccessDir, readOnlyDir)
        onShareDiskApi.uploadFileToFolder(FilesAndFolders.ORIGINAL_FILE, fullAccessDir)
        onShareDiskApi.uploadFileToFolder(FilesAndFolders.ORIGINAL_FILE, readOnlyDir)
        onShareDiskApi.shareFolder(fullAccessDir)
        onShareDiskApi.inviteUser(fullAccessDir, account, Rights.RW)
        onShareDiskApi.shareFolder(readOnlyDir)
        onShareDiskApi.inviteUser(readOnlyDir, account, Rights.RO)
        onDiskApi.activateInvite()
        onBasePage.openFiles()
        onFiles.openFolder(fullAccessDir)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineFileMarker(FilesAndFolders.ORIGINAL_FILE)
        onFiles.deleteFromOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openFiles()
        onFiles.openFolder(readOnlyDir)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineFileMarker(FilesAndFolders.ORIGINAL_FILE)
        onFiles.deleteFromOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("979")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @UploadFileWithName(filePath = FilesAndFolders.ORIGINAL_FILE, name = FilesAndFolders.TURKISH_ORIGINAL_FILE)
    @UploadFileWithName(filePath = FilesAndFolders.ORIGINAL_FILE, name = FilesAndFolders.SYMBOL_ORIGINAL_FILE)
    @UploadFileWithName(filePath = FilesAndFolders.ORIGINAL_FILE, name = FilesAndFolders.LINE_BREAK_ORIGINAL_FILE)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TURKISH_ORIGINAL_FILE, FilesAndFolders.SYMBOL_ORIGINAL_FILE, FilesAndFolders.LINE_BREAK_ORIGINAL_FILE])
    @Category(Quarantine::class)
    fun shouldAddToOfflineFilesWithArtifactInName() {
        onBasePage.openFiles()
        onFiles.switchToListLayout()
        onFiles.shouldFilesOrFoldersExist(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.TURKISH_ORIGINAL_FILE,
            FilesAndFolders.SYMBOL_ORIGINAL_FILE,
            FilesAndFolders.LINE_BREAK_ORIGINAL_FILE
        )
        onBasePage.swipeUpToDownNTimes(2)
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(
            FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TURKISH_ORIGINAL_FILE,
            FilesAndFolders.SYMBOL_ORIGINAL_FILE, FilesAndFolders.LINE_BREAK_ORIGINAL_FILE
        )
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.greaterThan(offlineCacheSizeBeforeOperation))
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineFileMarker(
            FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TURKISH_ORIGINAL_FILE,
            FilesAndFolders.SYMBOL_ORIGINAL_FILE, FilesAndFolders.LINE_BREAK_ORIGINAL_FILE
        )
    }

    @Test
    @TmsLink("986")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldRemoveFileFromOfflineWhenFileBeDeletedOnOfflineTab() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.deleteFromOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("988")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldRemoveFileFromOfflineWhenFileBeDeletedOnFilesTab() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("1005")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(Regression::class)
    fun shouldGetLinkForOfflineFiles() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_VIEWING_1)
        onBasePage.openOffline()
        onOffline.openFileIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldShareLinkForCurrentFile()
        onPreview.closePreview()
        onOffline.shouldSeePublicFileMarker(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("1006")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(Quarantine::class)
    fun shouldFullyClearOfflineFromSetting() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.clearOfflineFilesCompletely()
        onSettings.wait(10, TimeUnit.SECONDS) //wait for offline drop
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineStub()
    }

    @Test
    @TmsLink("1007")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(Quarantine::class)
    fun shouldClearOfflineFromSettingsBySelecting() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.clearOfflineFilesBySelection()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.removeSelectedFilesFromOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("992")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldDeleteSharedFoldersFromOfflineWhenFolderBecomeUnshared() {
        val name = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(name)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() // escape from shared folders
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onShareDiskApi.changeRights(name, account, Rights.RO)
        onOffline.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onShareDiskApi.kickFromGroup(name, account)
        onOffline.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
    }
}
