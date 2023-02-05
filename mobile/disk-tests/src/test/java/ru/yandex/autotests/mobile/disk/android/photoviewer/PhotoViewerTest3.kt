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
import ru.yandex.autotests.mobile.disk.android.blocks.feed.FeedContentBlockType
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.SortFiles

@Feature("Photo viewer")
@UserTags("photoviewer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoViewerTest3 : PhotoViewerTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6035")
    @Category(BusinessLogic::class)
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.ONE_HOUR_VIDEO, FilesAndFolders.TEXT1, FilesAndFolders.TEXT2, FilesAndFolders.AUDIO1],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER,
        sorted = true
    )
    @UploadFiles(
        filePaths = [FilesAndFolders.JPEG1, FilesAndFolders.JPEG2, FilesAndFolders.JPEG3],
        targetFolder = FilesAndFolders.UPLOAD_SUBFOLDER_1,
        sorted = true
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER, FilesAndFolders.UPLOAD_SUBFOLDER_1])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    fun shouldSeeOnlyPhotosAndVideosFromCurrentDir() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldPhotoHasName(FilesAndFolders.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldPhotoHasName(FilesAndFolders.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldPhotoHasName(FilesAndFolders.THIRD)
        onPreview.swipeRightToLeft()
        onPreview.shouldPhotoHasName(FilesAndFolders.ONE_HOUR_VIDEO)
        onPreview.swipeRightToLeft()
        onPreview.shouldPhotoHasName(FilesAndFolders.ONE_HOUR_VIDEO)
    }

    @Test
    @TmsLink("6036")
    @Category(BusinessLogic::class)
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER,
        sorted = true
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    fun shouldSeeFilesInRightSort() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesBeSortedInOrder(
            SortFiles.TERMINATOR,
            FilesAndFolders.FIRST,
            FilesAndFolders.SECOND,
            FilesAndFolders.THIRD,
            FilesAndFolders.FOURTH,
            FilesAndFolders.FIFTH
        )
        onFiles.openImage(FilesAndFolders.THIRD)
        onPreview.shouldCurrentPhotoBe(Images.THIRD)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentPhotoBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.swipeRightToLeft()
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FOURTH)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FIFTH)
    }

    @Test
    @TmsLink("5801")
    @Category(BusinessLogic::class)
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        sorted = true
    )
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH])
    fun shouldSeeOnlyBlockFilesInFeed() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, 2, 5)
        onFeed.openFirstImage()
        onPreview.shouldCurrentPhotoBe(Images.FIFTH)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentPhotoBe(Images.FIFTH)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FOURTH)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.THIRD)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FIRST)
    }

    @Test
    @TmsLink("6070")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldCancelFileDeleteFromFilesPreviewByHardBack() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.shouldBeOnPreview()
        onPreview.shouldCancelDeleteCurrentFileFromPreview()
    }

    @Test
    @TmsLink("5762")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldFeedPhotoViewerBeRestoredFromAppSwitcher() {
        openFirstImageFromFeed()
        shouldPhotoViewerBeRestoredFromAppSwitcher()
    }

    @Test
    @TmsLink("6052")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldFilePhotoViewerBeRestoredFromAppSwitcher() {
        openFileFromFiles(FilesAndFolders.FIRST)
        shouldPhotoViewerBeRestoredFromAppSwitcher()
    }

    @Test
    @TmsLink("5774")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldControlsHideAndDisplayByFeedPreviewTap() {
        openFirstImageFromFeed()
        shouldControlsHideAndDisplayByPreviewTap()
    }

    @Test
    @TmsLink("6039")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldControlsHideAndDisplayByFilePreviewTap() {
        openFileFromFiles(FilesAndFolders.FIRST)
        shouldControlsHideAndDisplayByPreviewTap()
    }

    @Test
    @TmsLink("4132")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldSeeCurrentImageWhenSwipeOnRightEdgeImageOnAllPhotosTab() {
        onBasePage.openPhotos()
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.tapOnPhotosPreview()
        onPreview.closeBannerIfPresented()
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentPhotoBe(Images.FIRST)
    }

    @Test
    @TmsLink("7049")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldSeeCurrentImageWhenSwipeOnLeftEdgeImageOnAllPhotosTab() {
        onBasePage.openPhotos()
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.tapOnPhotosPreview()
        onPreview.closeBannerIfPresented()
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
    }
}
