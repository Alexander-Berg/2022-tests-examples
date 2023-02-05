package ru.yandex.autotests.mobile.disk.android.feed

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images

@Feature("Feed")
@UserTags("feed")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest4 : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6784")
    @AuthorizationTest
    @Category(BusinessLogic::class)
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @CleanFeedFiles
    fun shouldOpenImageAfterSwipeInPhotoviewer() {
        onFeed.openFirstImage()
        onPreview.shouldBeOnPreview()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.SECOND)
    }

    @Test
    @TmsLink("6868")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR, VIDEO_BLOCK_DIR, TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 5)], targetFolder = TEXT_BLOCK_DIR)
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 5)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldSeeNewEventsButton() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 5)
        onFeed.swipeDownToUpNTimes(8)
        diskApiSteps.uploadFiles(VIDEO_BLOCK_DIR, UploadFileType.VIDEO, 5)
        onPush.sendFeedDatabaseChangedPush()
        onFeed.shouldSeeNewEventsButton()
    }

    @Test
    @TmsLink("6778")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew1x1ContentBlockWith1PreviewAfter1ImageAutouploadToCameraDir() {
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        onSettings.enableLimitedPhotoAndVideoAutoupload()
        onMobile.pressHardBack()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, FilesAndFolders.CAMERA_UPLOADS, 1, 1)
    }

    @Test
    @TmsLink("6865")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldUpdateBlockOnPullToRefresh() {
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
        diskApiSteps.uploadFiles(IMAGE_BLOCK_DIR, UploadFileType.IMAGE, 5, 6)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 6)
    }

    @Test
    @TmsLink("6790")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 1)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldNotDisplayBlockAfterLastFilesWasDeleted() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 1, 1)
        diskApiSteps.removeFilesInDirectory(TEXT_BLOCK_DIR, *UploadFileType.TEXT.getDefaultFilenames(1))
        onFeed.shouldNotSeeContentBlockForDirectory(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR)
    }

    @Test
    @TmsLink("6792")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 5)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldChangeFilesBlockFrom4x2With5PreviewsTo4x1With4PreviewsAfter1FileWasDeleted() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 5, 5)
        diskApiSteps.removeFilesInDirectory(TEXT_BLOCK_DIR, *UploadFileType.TEXT.getDefaultFilenames(1))
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 4, 4)
    }

    @Test
    @TmsLink("7507")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 2)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldOpenPhoto() {
        enableUnlimitedPhotoAutoupload()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_AUTOUPLOAD, "Photos", 2, 2)
        onFeed.expandFeedBlock()
        onContentBlock.openPhoto()
        onBasePage.shouldBeOnPhotos()
    }
}
