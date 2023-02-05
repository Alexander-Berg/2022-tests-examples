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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
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
import ru.yandex.autotests.mobile.disk.data.FileActions

@Feature("Feed")
@UserTags("feed")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest2 : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }
    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6721")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 9)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew2x4ContentBlockWith8PreviewAfter9ImageAutoupload() {
        enableUnlimitedPhotoAutoupload()
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_AUTOUPLOAD, "Photos", 8, 9)
    }

    @Test
    @TmsLink("6750")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.VIDEO, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayNew1x1ContentBlockWith1PreviewAfter1VideoUpload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.VIDEO_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
    }

    @Test
    @TmsLink("6756")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 1)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayNew1x1ContentBlockWith1PreviewAfter1FilesUpload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 1, 1)
    }

    @Test
    @TmsLink("6761")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 9)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayNew2x4ContentBlockWith1PreviewAfter1FilesUpload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 8, 9)
    }

    @Test
    @TmsLink("6853")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 2)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayMenuForMediaBlockWithTwoImage() {
        onBasePage.openFeed()
        onFeed.tapOnContentBlockMenu(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 2)
        onFeed.shouldDisplayFeedBlockMenu(FileActions.HIDE_FROM_FEED, FileActions.SHARE_ALBUM)
    }

    @Test
    @TmsLink("7024")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 15)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldChangeContentBlockGridWheScrolled() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.scrollContentBlockWithVisibleActionbar()
        onContentBlock.shouldGridBeChangeable()
    }

    @Test
    @TmsLink("7511")
    @Category(Acceptance::class) //BusinessLogic
    @AuthorizationTest
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 4)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDeleteBlockItemFromBigContentBlockGO() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.shouldBeBlockItemsCount(4)
        onContentBlock.shouldDeleteBlockItems(2, ContentBlockSteps.GridType.BIG)
        onContentBlock.shouldBeBlockItemsCount(2)
    }

    @Test
    @TmsLink("6793")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 8)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldChangeFilesBlockFrom4x2With8PreviewsTo1x1With1PreviewsAfter7FileWasDeleted() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 8, 8)
        diskApiSteps.removeFilesInDirectory(TEXT_BLOCK_DIR, *UploadFileType.TEXT.getDefaultFilenames(7))
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 1, 1)
    }
}
