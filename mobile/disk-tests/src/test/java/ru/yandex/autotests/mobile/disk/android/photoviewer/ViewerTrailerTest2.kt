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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.EasyViewerTrailer
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images

@Feature("Viewer trailer")
@UserTags("trailer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
@EasyViewerTrailer
class ViewerTrailerTest2 : PhotoViewerTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7924")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldShowViewerTrailerAfterChangeOrientation() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onMobile.longSwipeRightToLeft()
        onPreview.shouldOpenViewerTrailerBeDisplayed()
        onPreview.rotate(ScreenOrientation.LANDSCAPE)
        onPreview.shouldOpenViewerTrailerBeDisplayed()
        onPreview.rotate(ScreenOrientation.PORTRAIT)
        onPreview.shouldOpenViewerTrailerBeDisplayed()
    }

    @Test
    @TmsLink("7921")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldOpenViewerTrailerOnLandscape() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onPreview.rotate(ScreenOrientation.LANDSCAPE)
        onMobile.longSwipeRightToLeft()
        onPreview.shouldOpenViewerTrailerBeDisplayed()
    }

    @Test
    @TmsLink("7918")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(BusinessLogic::class)
    fun shouldOpenViewerTrailer() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onMobile.longSwipeRightToLeft()
        onPreview.shouldOpenViewerTrailerBeDisplayed()
    }

    @Test
    @TmsLink("7926")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldClosePhotoViewerWithViewerTrailerTapOnLandscape() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onPreview.rotate(ScreenOrientation.LANDSCAPE)
        onMobile.longSwipeRightToLeft()
        onPreview.shouldOpenViewerTrailerBeDisplayed()
        onPreview.tapOnPhotosPreview()
        onPreview.shouldCurrentImageBe(Images.SECOND)
    }

    @Test
    @TmsLink("7925")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldClosePhotoViewerWithViewerTrailerTapOnPortrait() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onMobile.longSwipeRightToLeft()
        onPreview.shouldOpenViewerTrailerBeDisplayed()
        onPreview.tapOnPhotosPreview()
        onPreview.shouldCurrentImageBe(Images.SECOND)
    }
}
