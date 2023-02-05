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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Feed")
@UserTags("feed")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest3 : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6772")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew1x1MediaBlockWith1PreviewAfter1ImageUnlimAutoupload() {
        enableUnlimitedPhotoAutoupload()
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_AUTOUPLOAD, "Photos", 1, 1)
    }

    @Test
    @TmsLink("6806")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.VIDEO, count = 9)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew4x2MediaBlockWith8PreviewsAfter9VideoUnlimAutoupload() {
        enableUnlimitedVideoAutoupload()
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.VIDEO_AUTOUPLOAD, "Photos", 8, 9)
    }

    @Test
    @TmsLink("6783")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 9)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew4x2MediaBlockWith8PreviewsAfter9ImageAutouploadToCameraDir() {
        enableLimitedPhotoAndVideoAutoupload()
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, FilesAndFolders.CAMERA_UPLOADS, 8, 9)
    }

    @Test
    @TmsLink("6879")
    @Category(BusinessLogic::class)
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.FIRST])
    fun shouldDisplayNew1x1MediaBlockWith1Preview1After1ImageUploadToRWSharedDir() {
        val shareFolder = nameHolder.name
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.SHARED_IMAGE_UPLOAD, shareFolder, 1, 1)
    }

    @Test
    @TmsLink("6849")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayMenuForMediaBlockWithSingleImage() {
        onBasePage.openFeed()
        onFeed.tapOnContentBlockMenu(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
        onFeed.shouldDisplayFeedBlockMenu(FileActions.HIDE_FROM_FEED, FileActions.SHARE_ALBUM)
    }

    @Test
    @TmsLink("6645")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldCancelBlockHiding() {
        onBasePage.openFeed()
        onFeed.tapOnContentBlockMenu(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
        onFeed.tapFeedBlockMenuItem("Hide from Feed")
        onFeed.tapSnackbarAction("Block removed from Feed", "CANCEL")
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
    }

    @Test
    @TmsLink("6644")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldHideContentBlockFromBlockMenu() {
        onBasePage.openFeed()
        onFeed.tapOnContentBlockMenu(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
        onFeed.tapFeedBlockMenuItem("Hide from Feed")
        onFeed.shouldNotSeeContentBlockForDirectory(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR)
    }
}
