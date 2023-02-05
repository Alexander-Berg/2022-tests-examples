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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Files")
@UserTags("filesDisplayMode")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FilesDisplayModeTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onRecentApps: RecentAppsSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var diskApiSteps: DiskApiSteps

    @Test
    @TmsLink("7991")
    @Category(Regression::class)
    @PrepareFilesDisplayModeContent
    fun shouldListDisplayTypeSavedOnAppRestore() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
        onMobile.openAppSwitcher()
        onRecentApps.shouldCloseAllAppsInBackground()
        onLogin.startAppAndCloseWizards()
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7990")
    @Category(BusinessLogic::class)
    @PrepareFilesDisplayModeContent
    fun shouldGridDisplayTypeSavedOnAppRestore() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.switchToGridLayout()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onMobile.openAppSwitcher()
        onRecentApps.shouldCloseAllAppsInBackground()
        onLogin.startAppAndCloseWizards()
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFileListDisplayTypeBeGrid()
    }

    @Test
    @TmsLink("7980")
    @Category(BusinessLogic::class)
    @PrepareFilesDisplayModeContent
    fun shouldGridDisplayTypeBeChangedOnList() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.switchToGridLayout()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7982")
    @Category(BusinessLogic::class)
    @PrepareFilesDisplayModeContent
    fun shouldListDisplayTypeBeChangedOnGrid() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
        onFiles.switchToGridLayout()
        onFiles.shouldFileListDisplayTypeBeGrid()
    }

    @Test
    @TmsLink("7985")
    @Category(BusinessLogic::class)
    @PrepareFilesDisplayModeContent
    fun shouldListDisplayTypeBeAppliedForAllFiles() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.switchToGridLayout()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7986")
    @Category(BusinessLogic::class)
    @PrepareFilesDisplayModeContent
    fun shouldGridDisplayTypeBeAppliedForAllFiles() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.switchToGridLayout()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeGrid()
    }

    @Test
    @TmsLink("7989")
    @Category(Regression::class)
    @AuthorizationTest
    @PrepareFilesDisplayModeContent
    fun shouldGridDisplayTypeBeSavedOnRelogin() {
        shouldDisplayTypeBeSavedOnRelogin(
                {
                    onFiles.switchToGridLayout()
                    onFiles.shouldFileListDisplayTypeBeGrid()
                }
        ) { onFiles.shouldFileListDisplayTypeBeGrid() }
    }

    @Test
    @TmsLink("7988")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    @PrepareFilesDisplayModeContent
    fun shouldListDisplayTypeBeSavedOnRelogin() {
        shouldDisplayTypeBeSavedOnRelogin(
                {
                    onFiles.switchToListLayout()
                    onFiles.shouldFileListDisplayTypeBeList()
                }
        ) { onFiles.shouldFileListDisplayTypeBeList() }
    }

    @Test
    @TmsLink("7963")
    @Category(Regression::class)
    @AuthorizationTest
    @PrepareFilesDisplayModeContent
    fun shouldGridLayoutWhenMoreFiles() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeGrid()
    }

    @Test
    @TmsLink("7965")
    @Category(Regression::class)
    @AuthorizationTest
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    fun shouldLayoutWhenNoFiles() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("7979")
    @Category(Regression::class)
    @AuthorizationTest
    @PrepareFilesDisplayModeContent
    fun shouldChangeGridLayoutToListInFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7983")
    @Category(Regression::class)
    @AuthorizationTest
    @PrepareFilesDisplayModeContent
    fun shouldChangeGridLayoutToListInFiles() {
        onBasePage.openFiles()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
        onFiles.switchToGridLayout()
        onFiles.shouldFileListDisplayTypeBeGrid()
    }

    @Test
    @TmsLink("7984")
    @Category(Regression::class)
    @AuthorizationTest
    @PrepareFilesDisplayModeContent
    fun shouldChangeGridLayoutInFilesWhenChangeGridToListInFolders() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
        onBasePage.pressHardBack()
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7987")
    @Category(Regression::class)
    @AuthorizationTest
    @PrepareFilesDisplayModeContent
    fun shouldChangeGridLayoutInFoldersWhenChangeGridToListInFiles() {
        onBasePage.openFiles()
        onFiles.shouldFileListDisplayTypeBeGrid()
        onFiles.switchToListLayout()
        onFiles.shouldFileListDisplayTypeBeList()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListDisplayTypeBeList()
    }

    @Test
    @TmsLink("7964")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldGridLayoutNotEnabledForSingleFileInFolder() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.shouldFileListDisplayTypeBeList()
        onFiles.shouldChangeLayoutOptionBeNotDisplayed()
    }

    @Test
    @TmsLink("7969")
    @Category(BusinessLogic::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldGridLayoutNotEnabledForSingleFileInFolder2() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeEmptyFolderStub()
        diskApiSteps.uploadFileToFolder(FilesAndFolders.FIRST, FilesAndFolders.TARGET_FOLDER)
        onFiles.wait(3, TimeUnit.SECONDS)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FIRST)
        onFiles.shouldFileListDisplayTypeBeList()
        onFiles.shouldChangeLayoutOptionBeNotDisplayed()
    }

    private fun shouldDisplayTypeBeSavedOnRelogin(prepare: Runnable, check: Runnable) {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        prepare.run()
        onBasePage.logout()
        onBasePage.wait(10, TimeUnit.SECONDS) //wait for logout
        onLogin.shouldAutologinIntoFirstAccount()
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        check.run()
    }

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.TARGET_FOLDER])
    annotation class PrepareFilesDisplayModeContent
}
