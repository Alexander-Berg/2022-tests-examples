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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.ContentBlockSteps
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Feed")
@UserTags("feed")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest6 : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6749")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 9)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayNew4x2MediaBlockWith8PreviewsAfter9ImageUpload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 8, 9)
    }

    @Test
    @TmsLink("6755")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [VIDEO_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.VIDEO, count = 9)], targetFolder = VIDEO_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayNew4x2MediaBlockWith8ItemsAfter9VideoUpload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.VIDEO_UPLOAD, VIDEO_BLOCK_DIR, 8, 9)
    }

    @Test
    @TmsLink("6762")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldNotDisplayBlockAfterLastImageWasDeleted() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
        diskApiSteps.removeFilesInDirectory(IMAGE_BLOCK_DIR, *UploadFileType.IMAGE.getDefaultFilenames(1))
        onFeed.shouldNotSeeContentBlockForDirectory(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR)
    }

    @Test
    @TmsLink("6766")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [VIDEO_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.VIDEO, count = 4)], targetFolder = VIDEO_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldChangeMediaBlockFrom2x1With2PreviewsTo4x2With8PreviewsAfter5VideoUpload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.VIDEO_UPLOAD, VIDEO_BLOCK_DIR, 2, 4)
        diskApiSteps.uploadFiles(VIDEO_BLOCK_DIR, UploadFileType.VIDEO, 5, 9)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.VIDEO_UPLOAD, VIDEO_BLOCK_DIR, 8, 9)
    }

    @Test
    @TmsLink("6671")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 3)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldHidePhotoBlockAfterDeleteAllPhotoInPopap() {
        enableUnlimitedPhotoAutoupload()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_AUTOUPLOAD, "Photos", 2, 3)
        onFeed.expandFeedBlock()
        onContentBlock.shouldBeBlockItemsCount(3)
        onContentBlock.shouldDeleteBlockItems(3, ContentBlockSteps.GridType.BIG)
        onFeed.shouldNotSeePhotoSelectionBlock()
    }

    @Test
    @TmsLink("6801")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.VIDEO, count = 1)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew1x1MediaBlockWith1PreviewsAfter1VideoAutoupload() {
        enableUnlimitedVideoAutoupload()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.VIDEO_AUTOUPLOAD, "Photos", 1, 1)
    }

    @Test
    @TmsLink("6807")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.VIDEO, count = 1)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew1x1MediaBlockWith1PreviewsAfter1VideoAutouploadToCameraDir() {
        enableLimitedPhotoAndVideoAutoupload()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.VIDEO_UPLOAD, FilesAndFolders.CAMERA_UPLOADS, 1, 1)
    }
}
