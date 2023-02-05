package ru.yandex.autotests.mobile.disk.android.navigation

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Navigation")
@UserTags("navigation")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class NavigationTest4 : NavigationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("5453")
    @Category(Regression::class)
    fun shouldResetToDefaultStateWhenFeedScrolledDown() {
        onBasePage.openFeed()
        onBasePage.swipeDownToUpNTimes(10)
        onBasePage.openPhotos()
        onBasePage.openFeed()
        onBasePage.shouldBeOnFeed()
    }

    @Test
    @TmsLink("5478")
    @Category(Regression::class)
    @AuthorizationTest
    fun shouldResetToDefaultStateWhenFilesScrolledDown() {
        onBasePage.openFiles()
        onBasePage.swipeDownToUpNTimes(10)
        onBasePage.shouldBeOnFilesAfterScroll()
        onBasePage.swipeUpToDownNTimes(10)
        onBasePage.shouldBeOnFiles()
    }

    @Test
    @TmsLink("5486")
    @Category(Regression::class)
    fun shouldOpenLastSectionWhenDoubleBackFromAlbum() {
        onBasePage.openFiles()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(FilesAndFolders.CAMERA)
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnAlbums()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFiles()
    }

    @Test
    @TmsLink("4353")
    @Category(Regression::class)
    fun shouldOpenPhotosByTapOnTab() {
        onBasePage.openPhotos()
        onBasePage.shouldBeOnPhotos()
    }

    @Test
    @TmsLink("4117")
    @Category(Regression::class)
    fun shouldOpenAlbumsListAfterOpenToIcon() {
        onBasePage.openAlbums()
        onBasePage.pressHomeButton()
        onMobile.returnAppFromBackground()
        onBasePage.shouldBeOnAlbums()
    }

    @Test
    @TmsLink("5480")
    @Category(Regression::class)
    fun shouldResetToDefaultStatePhotoWhenTapByTab() {
        onBasePage.openPhotos()
        onBasePage.swipeUpToDownNTimes(3)
        onBasePage.openPhotos()
        onBasePage.shouldBeOnPhotos()
    }

    @Test
    @TmsLink("6895")
    @Category(Regression::class)
    fun shouldOpenTrash() {
        onBasePage.openFiles()
        onFiles.openTrash()
        onTrash.shouldBeOnTrashScreen()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnFiles()
    }

    @Test
    @TmsLink("5652")
    @Category(Quarantine::class)
    fun shouldOpenPhotosAfterBackVista() {
        onBasePage.openPhotos()
        onBasePage.wait(3, TimeUnit.SECONDS)
        onPhotos.openVista()
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnPhotos()
    }

    @Test
    @TmsLink("7974")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FILE_FOR_VIEWING_1])
    fun shouldFilesGroupActionModeBeActivated() {
        onBasePage.openFiles()
        onFiles.shouldEnableGroupOperationMode()
        onGroupMode.shouldCloseButtonBeEnabled()
        onGroupMode.shouldNFilesBeCounted(0)
        onGroupMode.shouldMarkAsOfflineButtonBeDisabled()
        onGroupMode.shouldShareButtonBeDisabled()
        onGroupMode.shouldMoreOptionsButtonBeEnabled()
        onGroupMode.shouldNCheckboxesBeChecked(0)
        onFiles.shouldNotSeeFabButton()
    }
}
