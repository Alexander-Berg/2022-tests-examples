package ru.yandex.autotests.mobile.disk.android.photoviewer

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images

@Feature("Photo viewer")
@UserTags("photoviewer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoViewerTest : PhotoViewerTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7048")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldOpenFileIntoPhotoViewer() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldCurrentImageBe(Images.FIRST)
    }

    @Test
    @TmsLink("6023")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(Acceptance::class)
    fun shouldOpenFileIntoPhotoViewerFromFiles() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldCurrentImageBe(Images.FIRST)
    }

    @Test
    @TmsLink("6025")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldOpenNextPreviewAfterLeftSwipe() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onMobile.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.FIRST)
    }

    @Test
    @TmsLink("6026")
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldSeeCurrentImageWhenSwipeOnRightEdgeImage() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.SECOND)
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.SECOND)
    }

    @Test
    @TmsLink("6027")
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldSeeCurrentImageWhenSwipeOnLeftEdgeImage() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FIRST)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.FIRST)
    }

    @Test
    @TmsLink("1182")
    @UploadFiles(filePaths = [FilesAndFolders.NO_PREVIEW])
    @DeleteFiles(files = [FilesAndFolders.NO_PREVIEW])
    @Category(Regression::class)
    fun shouldSeeErrorStubForFileWithoutPreview() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.NO_PREVIEW)
        onPreview.shouldSeeUnableToPreviewFilesStub()
    }

    @Test
    @TmsLink("6038")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldHideViewerControlsByTapOnImagePreview() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.ORIGINAL_FILE)
        onPreview.tapOnPhotosPreview()
        onPreview.closeBannerIfPresented()
        onPreview.shouldNotSeePreviewControls()
        onPreview.tapOnPhotosPreview()
        onPreview.shouldSeePreviewControls()
    }

    @Test
    @TmsLink("1189")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldNotOpenPreviewFromMoveWindow() {
        onBasePage.openFiles()
        onFiles.shouldOpenMoveDialogForFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.tapOnFile(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldNotBeOnPreview()
    }

    @Test
    @TmsLink("4131")
    @Category(BusinessLogic::class)
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldSeeNextImageWhenSwipeToLeftOnAllPhotosTab() {
        onBasePage.openPhotos()
        onPhotos.sendPushesUntilAppeared(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.tapOnPhotosPreview()
        onPreview.closeBannerIfPresented()
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FIRST)
    }

    @Test
    @TmsLink("6100")
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @Category(BusinessLogic::class)
    fun shouldRemoveFileFromOfflineFromOfflineViewer() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onBasePage.openOffline()
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.deleteCurrentFileFromOffline()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onBasePage.pressHardBack()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("6074")
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @Category(BusinessLogic::class)
    fun shouldRemoveFileFromOfflineFromFilesViewer() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.deleteCurrentFileFromOffline()
        onPreview.shouldBeOnPreview()
        onBasePage.pressHardBack()
        onBasePage.openOffline()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("6073")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldAddFileToOfflineFromFilesViewer() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.addCurrentFileFromOffline()
        onPreview.shouldBeOnPreview()
        onBasePage.pressHardBack()
        onBasePage.openOffline()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("6067")
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @Category(BusinessLogic::class)
    fun shouldDeleteFileFromFilesViewer() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onBasePage.pressHardBack()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("6064")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldCloseOptionsMenuByHardBackFromFilesViewer() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.openOptionsMenu()
        onPreview.shouldOptionsMenuBeVisible(true)
        onBasePage.pressHardBack()
        onPreview.shouldOptionsMenuBeVisible(false)
    }

    @Test
    @TmsLink("6061")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldOpenOptionsMenuFromFilesViewer() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.openOptionsMenu()
        onPreview.shouldImageOptionsBeVisible()
    }

    @Test
    @TmsLink("6031")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldCloseFilesViewerByHardBack() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.shouldBeOnPreview()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFiles()
    }
}
