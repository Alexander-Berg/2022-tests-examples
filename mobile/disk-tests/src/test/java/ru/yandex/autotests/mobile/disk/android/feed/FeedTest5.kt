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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.FileActions

@Feature("Feed")
@UserTags("feed")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest5 : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("5454")
    @Category(Regression::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 5)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldNotSeeNavigationInContentBlock() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 5)
        onFeed.expandFeedBlock()
        onFeed.shouldNotSeeAddButton()
        onBasePage.shouldNotSeeTabs()
    }

    @Test
    @TmsLink("6859")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @CleanFeedFiles
    fun shouldUpdateFeedOnPullToRefresh() {
        diskApiSteps.uploadFiles(IMAGE_BLOCK_DIR, UploadFileType.IMAGE, 5, 0)
        onFeed.updateFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 5)
    }

    @Test
    @TmsLink("6855")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 1)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayMenuForFilesBlockWithOneFile() {
        onBasePage.openFeed()
        onFeed.tapOnContentBlockMenu(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 1, 1)
        onFeed.shouldDisplayFeedBlockMenu(FileActions.HIDE_FROM_FEED, FileActions.SHARE_ALBUM)
    }

    @Test
    @TmsLink("6860")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 2)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDisplayMenuForFilesBlockWithTwoFile() {
        onBasePage.openFeed()
        onFeed.tapOnContentBlockMenu(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 2, 2)
        onFeed.shouldDisplayFeedBlockMenu(FileActions.HIDE_FROM_FEED)
    }

    @Test
    @TmsLink("7495")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 3)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldOpenContentBlockWowGrid() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 3)
        onFeed.expandFeedBlock()
        onContentBlock.shouldWowGridBeOpened()
    }

    @Test
    @TmsLink("7510")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 2)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldOpenFileFolder() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 2, 2)
        onFeed.expandFeedBlock()
        onContentBlock.shouldFileGridBeOpened()
        onContentBlock.openFileFolder()
        onFiles.shouldFileListBeDisplayed()
    }

    @Test
    @TmsLink("7005")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 2)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldHidePhotoSelectionBlockFromPopup() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 2)
        onFeed.expandFeedBlock()
        onContentBlock.shouldHideBlock()
        onBasePage.shouldBeOnFeed()
        onFeed.shouldNotSeeContentBlockForDirectory(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR)
    }

    @Test
    @TmsLink("7026")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 2)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldNotHidePhotoSelectionBlockFromPopup() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 2)
        onFeed.expandFeedBlock()
        onContentBlock.shouldNotHideBlock()
    }
}
