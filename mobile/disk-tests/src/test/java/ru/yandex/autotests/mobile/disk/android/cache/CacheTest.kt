package ru.yandex.autotests.mobile.disk.android.cache

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Cache")
@UserTags("cache")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class CacheTest : CacheTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2077")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldDownloadFolderWithFilesFromCacheAndOfflineOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.waitDownloadComplete()
        onMobile.switchToAirplaneMode()
        onFiles.navigateUp()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_1,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_2,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
    }

    @Test
    @TmsLink("2079")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_2])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(FullRegress::class)
    fun shouldDownloadOfflineFolderAndCachedFileOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_2, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_1,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
    }

    @Test
    @TmsLink("2080")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_2])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(FullRegress::class)
    fun shouldDownloadEmptyNonCachedFolderAndCachedFileOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(10, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_2, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FOLDER,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        //not download files from non cached directory
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_1,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH + FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("2081")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldDownloadOfflineFolderOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FOLDER,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_1,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_2,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH + FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("2082")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_2])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(FullRegress::class)
    fun shouldDownloadOnlyCachedFilesFromNonCachedFolderAndCachedFileOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onFiles.navigateUp()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(10, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_1,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FOLDER,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        //not download non cached files
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.FILE_FOR_VIEWING_2,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH + FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("3397")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.ORIGINAL_FILE, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotDownloadOfflineFileAndFolderWhenCanceledOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.switchToAirplaneMode()
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.cancelDownLoading()
        onFiles.shouldNotSeeCanSaveOnlyCachedFilesAlert()
        onGroupMode.shouldFileOrFolderNotHasCheckbox(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FILE,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FOLDER,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
    }
}
