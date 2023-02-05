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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.ContentBlockSteps
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Feed")
@UserTags("feed")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6823")
    @Category(Quarantine::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldHidePhotoSelectionBlockFromBlockMenu() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
        diskApiSteps.generatePhotoSelectionBlock(1)
        onFeed.openPhotoSelectionBlockMenu()
        onFeed.tapFeedBlockMenuItem("Hide from Feed")
        onFeed.shouldNotSeePhotoSelectionBlock()
    }

    @Test
    @TmsLink("7502")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 3)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldChangeContentBlockGrid() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 3)
        onFeed.expandFeedBlock()
        onContentBlock.shouldGridBeChangeable()
    }

    @Test
    @TmsLink("7500")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 2)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldOpenContentBlockFileGrid() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 2, 2)
        onFeed.expandFeedBlock()
        onContentBlock.shouldFileGridBeOpened()
    }

    @Test
    @TmsLink("7496")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 25)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldOpenContentBlockPhotosliceGrid() {
        onBasePage.openFeed()
        onFeed.wait(20, TimeUnit.SECONDS)
        onFeed.updateFeed()
        onFeed.expandFeedBlock()
        onContentBlock.shouldPhotosliceGridBeOpened()
    }

    @Test
    @TmsLink("7513")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 25)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDeleteBlockItemFromPhotosliceContentBlockGO() {
        onBasePage.openFeed()
        onFeed.wait(20, TimeUnit.SECONDS)
        onFeed.updateFeed()
        onFeed.expandFeedBlock()
        onContentBlock.shouldBeBlockItemsCount(25)
        onContentBlock.shouldDeleteBlockItems(2, ContentBlockSteps.GridType.PHOTOSLICE)
        onContentBlock.shouldBeBlockItemsCount(23)
        onContentBlock.shouldPhotosliceGridBeOpened()
    }

    @Test
    @TmsLink("7514")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 4)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDeleteBlockItemFromFileContentBlockGO() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 4, 4)
        onFeed.expandFeedBlock()
        onContentBlock.shouldBeBlockItemsCount(4)
        onContentBlock.shouldDeleteBlockItems(2, ContentBlockSteps.GridType.FILES)
        onContentBlock.shouldBeBlockItemsCount(2)
    }

    @Test
    @TmsLink("7520")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 3)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDeleteAllBlockItemFromBigContentBlock() {
        onBasePage.openFeed()
        onFeed.wait(20, TimeUnit.SECONDS)
        onFeed.updateFeed()
        onFeed.expandFeedBlock()
        onContentBlock.shouldBeBlockItemsCount(3)
        onContentBlock.shouldDeleteBlockItems(3, ContentBlockSteps.GridType.BIG)
        onBasePage.shouldBeOnFeed()
        onFeed.shouldNotSeeContentBlockForDirectory(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR)
    }

    @Test
    @TmsLink("7516")
    @Category(Quarantine::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 8)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDeleteBlockItemsFromBigContentBlockFromViewer() {
        onBasePage.openFeed()
        onFeed.wait(20, TimeUnit.SECONDS)
        onFeed.updateFeed()
        onFeed.expandFeedBlock()
        onContentBlock.shouldBeBlockItemsCount(8)
        shouldDeleteFirstBlockItemFromViewer(ContentBlockSteps.GridType.BIG)
        onContentBlock.shouldBeBlockItemsCount(7)
    }

    @Test
    @TmsLink("7517")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 25)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldDeleteBlockItemsFromPhotosliceContentBlockFromViewer() {
        onBasePage.openFeed()
        onFeed.wait(20, TimeUnit.SECONDS)
        onFeed.updateFeed()
        onFeed.expandFeedBlock()
        onContentBlock.shouldBeBlockItemsCount(25)
        shouldDeleteFirstBlockItemFromViewer(ContentBlockSteps.GridType.PHOTOSLICE)
        onContentBlock.shouldBeBlockItemsCount(24)
    }

    @Test
    @TmsLink("5574")
    @Category(Quarantine::class)
    @UploadFiles(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 10)],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    fun shouldDeleteFileInWowGrid() {
        val fileToDelete = "2.jpg"
        onFeed.wait(20, TimeUnit.SECONDS)
        diskApiSteps.generatePhotoSelectionBlock(10)
        onFeed.wait(10, TimeUnit.SECONDS)
        onFeed.updateFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.selectImages(fileToDelete)
        onContentBlock.deleteSelectedImages()
        onContentBlock.wait(2, TimeUnit.SECONDS)
        onContentBlock.shouldNotSeeImage(fileToDelete)
        onContentBlock.shouldWowGridBeOpened()
    }
}
