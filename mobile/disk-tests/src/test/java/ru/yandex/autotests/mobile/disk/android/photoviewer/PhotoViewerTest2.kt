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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.*
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.SortFiles

@Feature("Photo viewer")
@UserTags("photoviewer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoViewerTest2 : PhotoViewerTestRunner() {
    companion object {
        @JvmField
        @ClassRule
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6102")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FIRST, SECOND, THIRD, FOURTH, FIFTH], sorted = true)
    @DeleteFiles(files = [FIRST, SECOND, THIRD, FOURTH, FIFTH])
    fun shouldSeeOfflineFilesInRightSort() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FIRST, SECOND, THIRD, FOURTH, FIFTH)
        onBasePage.openOffline()
        with(onOffline) {
            waitForFilesOrFolders(FIRST, SECOND, THIRD, FOURTH, FIFTH)
            shouldFilesBeSortedInOrder(SortFiles.TERMINATOR, FIFTH, FOURTH, THIRD, SECOND, FIRST)
            openFileIntoViewer(THIRD)
        }
        with(onPreview) {
            shouldCurrentPhotoBe(Images.THIRD)
            swipeLeftToRight()
            shouldCurrentPhotoBe(Images.FOURTH)
            swipeLeftToRight()
            shouldCurrentPhotoBe(Images.FIFTH)
            swipeRightToLeft()
            swipeRightToLeft()
            swipeRightToLeft()
            shouldCurrentPhotoBe(Images.SECOND)
            swipeRightToLeft()
            shouldCurrentPhotoBe(Images.FIRST)
        }
    }

    @Test
    @TmsLink("5802")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldAddFileToOfflineFromFeedViewer() {
        openFirstImageFromFeed()
        onPreview.addCurrentFileFromOffline()
        onPreview.shouldBeOnPreview()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFeed()
        onBasePage.openOffline()
        onFiles.shouldFilesOrFoldersExist(FIRST)
    }

    @Test
    @TmsLink("5797")
    @UploadFiles(filePaths = [FIRST, SECOND])
    @DeleteFiles(files = [FIRST, SECOND])
    @Category(BusinessLogic::class)
    fun shouldDeleteFileFromFeedViewer() {
        openFirstImageFromFeed()
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.pressHardBack()
    }

    @Test
    @TmsLink("5795")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldCloseOptionsMenuByHardBackFromFeedViewer() {
        openFirstImageFromFeed()
        onPreview.openOptionsMenu()
        onPreview.shouldOptionsMenuBeVisible(true)
        onBasePage.pressHardBack()
        onPreview.shouldOptionsMenuBeVisible(false)
    }

    @Test
    @TmsLink("5760")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldCloseFeedViewerByHardBack() {
        openFirstImageFromFeed()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFeed()
    }

    @Test
    @TmsLink("5731")
    @UploadFiles(filePaths = [FILE_FOR_VIEWING_1, FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FILE_FOR_VIEWING_1, FILE_FOR_VIEWING_2])
    @Category(BusinessLogic::class)
    fun shouldOpenPreviewFromContentBlock() {
        openFirstImageFromFeed()
    }

    @Test
    @TmsLink("5753")
    @UploadFiles(filePaths = [FIRST, SECOND])
    @DeleteFiles(files = [FIRST, SECOND])
    @Category(BusinessLogic::class)
    fun shouldOpenPreviewFromContentBlockPopup() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.clickImage(FIRST)
        onPreview.shouldBeOnPreview()
    }

    @Test
    @TmsLink("5798")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldCancelFileDeleteFromFeedPreviewByHardBack() {
        openFirstImageFromFeed()
        onPreview.shouldCancelDeleteCurrentFileFromPreview()
    }

    @Test
    @TmsLink("6103")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [JPEG1, VIDEO1, AUDIO1, TEXT1], targetFolder = UPLOAD_FOLDER)
    @UploadFiles(filePaths = [JPEG2, VIDEO2], targetFolder = UPLOAD_SUBFOLDER_1)
    @UploadFiles(filePaths = [JPEG3], targetFolder = UPLOAD_SUBFOLDER_2)
    @CreateFolders(folders = [UPLOAD_FOLDER, UPLOAD_SUBFOLDER_1, UPLOAD_SUBFOLDER_2])
    @DeleteFiles(files = [UPLOAD_FOLDER])
    fun shouldSeeOnlyFilesFromSearchResult() {
        val videoName = "1.mp4"
        val imageName = "1.jpg"
        onBasePage.openFiles()
        with(onFiles) {
            updateFileList()
            shouldOpenSearch()
            searchFile("1")
            openImage(videoName)
        }
        with(onPreview) {
            openPhotoInformation()
            shouldPhotoHasName(videoName)
            swipeLeftToRight()
            shouldPhotoHasName(videoName)
            swipeRightToLeft()
            shouldPhotoHasName(imageName)
            swipeRightToLeft()
            shouldPhotoHasName(imageName)
        }
    }


}
