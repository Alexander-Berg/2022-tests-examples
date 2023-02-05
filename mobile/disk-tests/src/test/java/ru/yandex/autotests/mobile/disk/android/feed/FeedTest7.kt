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
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Feed")
@UserTags("feed")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest7 : FeedTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6880")
    @Category(BusinessLogic::class)
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.FIRST])
    @CleanFeedFiles
    fun shouldDisplayNew1x1MediaBlockWith1Preview1After1ImageUploadToROSharedDir() {
        val shareFolder = nameHolder.name
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.SHARED_IMAGE_UPLOAD, shareFolder, 1, 1)
    }

    @Test
    @TmsLink("6641")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @CleanFeedFiles
    fun DisplayNew1x1MediaBlockWith1Preview1After1ImageUploadToFolder() {
        diskApiSteps.uploadFiles(IMAGE_BLOCK_DIR, UploadFileType.IMAGE, 1, 1)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_UPLOAD, IMAGE_BLOCK_DIR, 1, 1)
    }

    @Test
    @TmsLink("6795")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 1)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldChangeFilesBlockFrom1x1With1PreviewsTo2x4With7PreviewsAfter6FileDownload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 1, 1)
        diskApiSteps.uploadFiles(TEXT_BLOCK_DIR, UploadFileType.TEXT, 6, 1)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 7, 7)
    }

    @Test
    @TmsLink("6796")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.TEXT, count = 4)], targetFolder = TEXT_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldChangeFileBlockFrom4x1With4ItemsTo4x2With5ItemsAfter1FileUpload() {
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 4, 4)
        diskApiSteps.uploadFiles(TEXT_BLOCK_DIR, UploadFileType.TEXT, 1, 4)
        onFeed.updateFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 5, 5)
    }

    @Test
    @TmsLink("6771")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [TEXT_BLOCK_DIR])
    @CleanFeedFiles
    fun DisplayNew1x1FileBlockWith1Preview1After1FileUploadToFolder() {
        diskApiSteps.uploadFiles(TEXT_BLOCK_DIR, UploadFileType.TEXT, 1, 0)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 1, 1)
        diskApiSteps.uploadFiles(TEXT_BLOCK_DIR, UploadFileType.TEXT, 1, 1)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 2, 2)
        diskApiSteps.uploadFiles(TEXT_BLOCK_DIR, UploadFileType.TEXT, 2, 2)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 4, 4)
        diskApiSteps.uploadFiles(TEXT_BLOCK_DIR, UploadFileType.TEXT, 5, 4)
        onFeed.shouldSeeContentBlock(FeedContentBlockType.OTHER_UPLOAD, TEXT_BLOCK_DIR, 8, 9)
    }

    @Test
    @TmsLink("6655")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 1)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @CleanFeedFiles
    fun shouldDisplayNew1x1ContentBlockWith1PreviewAfter1ImageAutoupload() {
        enableUnlimitedPhotoAutoupload()
        onBasePage.openFeed()
        onFeed.shouldSeeContentBlock(FeedContentBlockType.IMAGE_AUTOUPLOAD, "Photos", 1, 1)
    }
}
