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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.blocks.feed.FeedContentBlockType
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.GroupModeSteps
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Feed")
@UserTags("photoselection")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoSelectionBlockTest : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("5583")
    @Category(Regression::class)
    @UploadFiles(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 10)],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    fun shouldBeBigGridAllContentBlock() {
        onFeed.wait(20, TimeUnit.SECONDS)
        diskApiSteps.generatePhotoSelectionBlock(3)
        onFeed.wait(10, TimeUnit.SECONDS)
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.shouldGridChangedFromWowToBig()
        onBasePage.pressHardBack()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.openPhotoSelectionBlockMenu()
        onFeed.tapFeedBlockMenuItem("Hide from Feed")
        onFeed.updateFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldSimpleGridBeOpened()
    }

    @Test
    @AuthorizationTest
    @TmsLink("5584")
    @Category(Regression::class)
    @UploadFiles(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 10)],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    fun shouldBeBigGridContentBlockAfterPerelogin() {
        onFeed.wait(20, TimeUnit.SECONDS)
        diskApiSteps.generatePhotoSelectionBlock(3)
        onFeed.wait(10, TimeUnit.SECONDS)
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.shouldGridChangedFromWowToBig()
        onBasePage.pressHardBack()
        onBasePage.logout()
        onLogin.shouldAutologinIntoFirstAccount()
        onBasePage.shouldSeeTabs()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldSimpleGridBeOpened()
    }

    @Test
    @TmsLink("5585")
    @Category(Regression::class)
    @UploadFiles(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 10)],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    fun shouldBeWowGridAllContentBlock() {
        onFeed.wait(20, TimeUnit.SECONDS)
        diskApiSteps.generatePhotoSelectionBlock(3)
        onFeed.wait(10, TimeUnit.SECONDS)
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.shouldGridBeChangeable()
        onBasePage.pressHardBack()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.openPhotoSelectionBlockMenu()
        onFeed.tapFeedBlockMenuItem("Hide from Feed")
        onFeed.updateFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
    }

    @Test
    @AuthorizationTest
    @TmsLink("5586")
    @Category(Regression::class)
    @UploadFiles(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 10)],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    fun shouldBeWowGridContentBlockAfterPerelogin() {
        onFeed.wait(20, TimeUnit.SECONDS)
        diskApiSteps.generatePhotoSelectionBlock(3)
        onFeed.wait(10, TimeUnit.SECONDS)
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.shouldGridBeChangeable()
        onBasePage.pressHardBack()
        onBasePage.logout()
        onLogin.shouldAutologinIntoFirstAccount()
        onBasePage.shouldSeeTabs()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
    }

    @Test
    @TmsLink("6820")
    @Category(Regression::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 3)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldSeeMenuOptionsOfPhotosAutouploadBlock() {
        enableUnlimitedPhotoAutoupload()
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_AUTOUPLOAD, "Photos", 2, 3)
        onFeed.tapOnContentBlockMenu(FeedContentBlockType.IMAGE_AUTOUPLOAD, "Photos", 2, 3)
        onGroupMode.shouldActionBePresented(FileActions.SHARE_ALBUM)
        onGroupMode.shouldActionBePresented(FileActions.HIDE_FROM_FEED)
    }

    @Test
    @TmsLink("7532")
    @Category(Regression::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 5)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldSeeOptionsOfContentBlockOpenedByViewAllButton() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 2, 5)
        onFeed.expandFeedBlock()
        onContentBlock.openMenu()
        onContentBlock.rotate(ScreenOrientation.LANDSCAPE)
        onContentBlock.rotate(ScreenOrientation.PORTRAIT)
        onGroupMode.shouldActionBePresented(FileActions.SHARE_ALBUM)
        onGroupMode.shouldActionBePresented(FileActions.SELECT_ONLY)
        onGroupMode.shouldActionBePresented(FileActions.HIDE_FROM_FEED)
    }
}
