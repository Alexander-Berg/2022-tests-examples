package ru.yandex.autotests.mobile.disk.android.deletelink

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.PublishFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Delete link")
@UserTags("deleteLink")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DeleteLinkTest2 : DeleteLinkTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2143")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldNotRemoveLinkForReadOnlyDir() {
        val sharedFolder = nameHolder.name
        onShareDiskApi.publishFile(sharedFolder)
        onBasePage.openFiles()
        deletePublicLink(sharedFolder)
        onFiles.shouldSeePublicFileMark(sharedFolder)
    }

    @Test
    @TmsLink("2131")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(
        FullRegress::class
    )
    fun shouldNotSeePublicMarkerForUnpublishedOfflineFile() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldSeePublicFileMarker(FilesAndFolders.ORIGINAL_FILE)
        onOffline.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onOffline.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2477")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @PublishFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(FullRegress::class)
    fun shouldRemovePublicLinkFromPhotoOnViewer() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.removeShareLinkForCurrentFile()
        onPreview.closePreview()
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("2478")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE])
    @PublishFiles(files = [FilesAndFolders.BIG_FILE])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE])
    @Category(FullRegress::class)
    fun shouldRemovePublicLinkFromVideoOnViewer() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.BIG_FILE)
        onFiles.shouldOpenVideoIntoViewer(FilesAndFolders.BIG_FILE)
        onPreview.removeShareLinkForCurrentFile()
        onPreview.closePreview()
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("1246")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotSeePublicLinkWhenDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onUserDiskApi.unpublishFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.swipeUpToDownNTimes(1) //swipe up to p2r
        onFiles.updateFileList()
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2758")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemovePublicLinkFromFolderOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
        deletePublicLink(FilesAndFolders.ORIGINAL_FOLDER)
        onOffline.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.updateFileList()
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("2122")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(FullRegress::class)
    fun shouldNotDeleteLinkMenuItemForUnsharedFilesAndFoders() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE_LINK)
        onGroupMode.pressHardBack()
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE_LINK)
        onGroupMode.pressHardBack()
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE_LINK)
        onGroupMode.pressHardBack()
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onOffline.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE_LINK)
        onGroupMode.pressHardBack()
        onGroupMode.shouldDeselectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openPhotos()
        onAllPhotos.selectPhoto()
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.shouldPhotosActionNotBePresented(FileActions.DELETE_LINK)
    }
}
