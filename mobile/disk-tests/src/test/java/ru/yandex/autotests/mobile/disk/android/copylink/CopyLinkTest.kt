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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("Copy link")
@UserTags("copyLink")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class CopyLinkTest : CopyLinkTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1219")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(BusinessLogic::class)
    fun shouldCopyLinkWhenFolderBecomePublic() {
        onBasePage.openFiles()
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1221")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldCopyLinkWhenFileBecomePublic() {
        onBasePage.openFiles()
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1220")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Quarantine::class)
    fun shouldJustShareOriginalFileInReadOnlyDirWhenPressShareButton() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickShareMenu()
        onGroupMode.shouldSeeShareOriginalMenu()
    }

    @Test
    @TmsLink("2482")
    @SharedFolder
    @Category(Quarantine::class)
    fun shouldShareFolderIntoFullAccessDir() {
        val sharedFolder = nameHolder.name
        onShareDiskApi.createFolders(sharedFolder + "/" + FilesAndFolders.TARGET_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1229")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(Regression::class)
    fun shouldCopyLinkFromPhotoOnViewer() {
        onBasePage.openFiles()
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldShareLinkForCurrentFile()
        onPreview.closePreview()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("2480")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE])
    @Category(Regression::class)
    fun shouldCopyPublicLinkFromVideoOnViewer() {
        onBasePage.openFiles()
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.BIG_FILE)
        onFiles.shouldOpenVideoIntoViewer(FilesAndFolders.BIG_FILE)
        onPreview.shouldShareLinkForCurrentFile()
        onPreview.closePreview()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("2747")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldCopyLinkToFileOnOfflinePage() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onOffline.shouldSeePublicFileMarker(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2748")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldCopyLinkToFolderOnOfflinePage() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1224")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.CAMERA_UPLOADS + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(Regression::class)
    fun shouldCopyLinkToFolderIntoCameraUploads() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeePublicFileMarkOnFolderGridRepresentation(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("2483")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Quarantine::class)
    fun shouldPublishFullAccessDir() {
        val name = nameHolder.name
        onShareDiskApi.createFolders(name + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.shouldNotSeePublicFileMark(name)
        onFiles.shouldCopyLinkForFilesOrFolders(name)
        onFiles.shouldSeePublicFileMark(name)
    }

    @Test
    @TmsLink("2484")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Quarantine::class)
    fun shouldNotPublishReadOnlyDir() {
        val name = nameHolder.name
        onShareDiskApi.createFolders(name + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.shouldNotSeePublicFileMark(name)
        onFiles.shouldSelectFilesOrFolders(name)
        onGroupMode.clickShareMenu()
        //TODO: Add checking toast when switch to UiAutomator2.
        onFiles.shouldNotSeePublicFileMark(name)
    }

    @Test
    @TmsLink("2833")
    @CreateFolders(folders = [FilesAndFolders.SYMBOL_FOLDER]) //add \
    @DeleteFiles(files = [FilesAndFolders.SYMBOL_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyLinkToFolderWithSymbolsName() {
        onBasePage.openFiles()
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.SYMBOL_FOLDER)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.SYMBOL_FOLDER)
    }

    @Test
    @TmsLink("1225")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotPublishReadOnlyDirWhenPublishWithAnotherFiles() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldNotSeePublicFileMark(folderName, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(folderName, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickShareMenu()
        onFiles.shouldSeeToastWithMessage(ToastMessages.SOME_SELECTED_FOLDERS_CANNOT_BE_SHARE_TOAST)
        onFiles.clickOnCopyLink()
        onFiles.shouldNotSeePublicFileMark(folderName)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1230")
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldShareOriginalWhenShareFileInReadOnlyDirOnPreview() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(folderName)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.ORIGINAL_FILE)
        onPreview.shouldShareOriginalFile()
    }

    @Test
    @TmsLink("2839")
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.PHOTO])
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldSeeCorrectVariantsWhenTryToShareFilesAndFoldersInReadOnlyFolder() {
        val folderName = nameHolder.name
        onShareDiskApi.createFolders(folderName + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onShareDiskApi.createFolders(folderName + "/" + FilesAndFolders.TARGET_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(folderName)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.PHOTO)
        onGroupMode.clickShareMenu()
        onGroupMode.shouldSeeShareOriginalMenu()
        onGroupMode.pressHardBack()
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.PHOTO,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER
        )
        onGroupMode.shouldShareButtonBeNotPresented()
        onBasePage.closeGroupMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onGroupMode.shouldShareButtonBeNotPresented()
    }

    @Test
    @TmsLink("1233")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldNotCopyLinkOnAirplaneMode() {
        onBasePage.openFiles()
        onBasePage.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_GO
        )
        onGroupMode.clickShareMenu()
        onGroupMode.clickOnShareSingleFileLink()
        onGroupMode.shouldSeeToastWithMessage(ToastMessages.YOU_ARE_NOT_CONNECTED_TOAST)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("1235")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldNotCopyLinkFromPreviewOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onFiles.switchToAirplaneMode()
        onFiles.wait(5, TimeUnit.SECONDS)
        onPreview.tryShareLink()
        onPreview.shouldSeeToastContainedText(ToastMessages.YOU_ARE_NOT_CONNECTED_TOAST)
    }

    @Test
    @TmsLink("1240")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE, FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE, FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(FullRegress::class)
    fun shouldCopyLinkOnVideoFromDiskPreviewOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.BIG_FILE, FilesAndFolders.FILE_FOR_VIEWING_1)
        onBasePage.openOffline()
        onOffline.openFileIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldShareLinkForCurrentFile()
        onPreview.shouldSeeToastWithMessage(ToastMessages.COPIED_TO_CLIPBOARD)
        onPreview.pressHardBack()
        onOffline.shouldSeePublicFileMarker(FilesAndFolders.BIG_FILE)
    }
}
