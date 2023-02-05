package ru.yandex.autotests.mobile.disk.android.files

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import com.google.inject.name.Named
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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.DiskApiSteps
import ru.yandex.autotests.mobile.disk.android.steps.FilesSteps
import ru.yandex.autotests.mobile.disk.android.steps.GroupModeSteps
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Files")
@UserTags("emptyFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class EmptyFilesTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var diskApiSteps: DiskApiSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Test
    @TmsLink("7972")
    @Category(BusinessLogic::class)
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    fun shouldFilesBeInListModeIfSingleFilesForRoot() {
        onBasePage.openFiles()
        cleanFolder()
        diskApiSteps.uploadFileWithName(FilesAndFolders.JPG_1, FilesAndFolders.JPG_1, "/")
        diskApiSteps.uploadFileWithName(FilesAndFolders.JPG_2, FilesAndFolders.JPG_2, "/")
        onFiles.wait(10, TimeUnit.SECONDS)
        onFiles.updateFileList()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.deleteFilesOrFolders(FilesAndFolders.JPG_1)
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7967")
    @Category(BusinessLogic::class)
    @DeleteFiles(files = [FilesAndFolders.FIRST])
    fun shouldGridLayoutNotEnabledForSingleFileForRoot() {
        onBasePage.openFiles()
        cleanFolder()
        onFiles.shouldSeeEmptyFolderStub()
        diskApiSteps.uploadFileToFolder(FilesAndFolders.FIRST, FilesAndFolders.DISK_ROOT)
        onFiles.wait(3, TimeUnit.SECONDS)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FIRST)
        onFiles.shouldFileListDisplayTypeBeList()
        onFiles.shouldChangeLayoutOptionBeNotDisplayed()
    }

    private fun cleanFolder() {
        val fileListOnScreen = onFiles.fileListOnScreen
        if (fileListOnScreen.size > 0) {
            onFiles.shouldEnableGroupOperationMode()
            onGroupMode.clickMoreOption()
            onGroupMode.applyAction(FileActions.SELECT_ALL)
            onGroupMode.clickMoreOption()
            onGroupMode.applyAction(FileActions.DELETE)
        }
    }
}
