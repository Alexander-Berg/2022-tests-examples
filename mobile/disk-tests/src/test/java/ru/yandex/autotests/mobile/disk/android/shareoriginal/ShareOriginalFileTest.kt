package ru.yandex.autotests.mobile.disk.android.shareoriginal

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.PublishFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.FilesSteps
import ru.yandex.autotests.mobile.disk.android.steps.GroupModeSteps
import ru.yandex.autotests.mobile.disk.android.steps.OfflineSteps
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Share original file")
@UserTags("shareOriginal")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class ShareOriginalFileTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Test
    @TmsLink("2329")
    @CreateFolders(
        folders = [
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER,
        ]
    )
    @PublishFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotShareOriginalForFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.swipeDownToUpNTimes(1)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onGroupMode.clickShareMenu()
        onFiles.shouldNotSeeShareOriginalVariant()
        onFiles.shouldSeeShareLinkMenu()
        onFiles.pressHardBack()
        onBasePage.openOffline()
        onOffline.shouldSelectFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onGroupMode.clickShareMenu()
        onOffline.shouldNotSeeShareOriginalVariant()
        onFiles.shouldSeeShareLinkMenu()
        onFiles.pressHardBack()
        onOffline.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickShareMenu()
        onOffline.shouldNotSeeShareOriginalVariant()
        onFiles.shouldSeeShareLinkMenu()
    }

    @Test
    @TmsLink("2330")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(
        files = [
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_GO,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER,
        ]
    )
    @Category(FullRegress::class)
    fun shouldNotShareOriginalWhenSelectedFilesAndFolders() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_GO,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER
        )
        onGroupMode.clickShareMenu()
        onFiles.shouldNotSeeShareOriginalVariant()
        onFiles.shouldSeeShareLinkMenu()
        onFiles.pressHardBack()
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_GO,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER
        )
        onFiles.shouldUnselectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickShareMenu()
        onGroupMode.shouldShareOriginalFileVariantBeEnabled()
        onGroupMode.shouldShareLinkVariantBeEnabled(true)
    }
}
