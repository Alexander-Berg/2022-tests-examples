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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Navigation")
@UserTags("navigation")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class NavigationTest : NavigationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1986")
    @Category(Acceptance::class) //BusinessLogic
    fun shouldNavigateBetweenPartitions() {
        onBasePage.openFeed()
        onBasePage.shouldBeOnFeed()
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onBasePage.openPhotos()
        onBasePage.shouldBeOnPhotos()
        onBasePage.openAlbums()
        onBasePage.shouldBeOnAlbums()
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
    }

    @Test
    @TmsLink("4118")
    @Category(Regression::class)
    fun shouldOpenAlbumAfterOpenIcon() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(FilesAndFolders.CAMERA)
        onBasePage.pressHomeButton()
        onBasePage.returnAppFromBackground()
        onAlbums.shouldBeOnAlbum(FilesAndFolders.CAMERA)
    }

    @Test
    @TmsLink("5525")
    @Category(Regression::class)
    @AuthorizationTest
    fun shouldOpenNotesList() {
        onBasePage.openNotes()
        onBasePage.swipeDownToUpNTimes(3)
        val filesList: List<String?>
        val newFilesList: List<String?>
        filesList = onNotes.getNotesListOnScreen()
        onNotes.tapOnNote(1)
        onNotes.tapInBody()
        onNotes.pressHardBack()
        onNotes.pressHardBack()
        onBasePage.shouldBeOnNotes()
        newFilesList = onNotes.getNotesListOnScreen()
        onFileList.shouldListsEqually(filesList, newFilesList)
    }

    @Test
    @TmsLink("5531")
    @Category(Quarantine::class)
    fun shouldSwitchingToScrolledFiles() {
        onBasePage.openFiles()
        onBasePage.wait(2, TimeUnit.MILLISECONDS)
        onBasePage.swipeDownToUpNTimes(2)
        val filesList: List<String?>
        val newFilesList: List<String?>
        filesList = onFileList.fileListOnScreen
        onBasePage.openPhotos()
        onBasePage.openFiles()
        newFilesList = onFileList.fileListOnScreen
        onFileList.shouldListsEqually(filesList, newFilesList)
    }

    @Test
    @TmsLink("5539")
    @Category(Quarantine::class)
    fun shouldSwitchingToScrolledNotes() {
        onBasePage.openNotes()
        onBasePage.swipeDownToUpNTimes(2)
        val filesList: List<String?>
        val newFilesList: List<String?>
        filesList = onNotes.getNotesListOnScreen()
        onBasePage.openPhotos()
        onBasePage.openNotes()
        newFilesList = onNotes.getNotesListOnScreen()
        onFileList.shouldListsEqually(filesList, newFilesList)
    }

    @Test
    @TmsLink("4111")
    @Category(Regression::class)
    fun shouldOpenToScrolledPhotos() {
        onBasePage.openPhotos()
        onBasePage.swipeDownToUpNTimes(3)
        val filesList: List<String?>
        val newFilesList: List<String?>
        filesList = onPhotos.photoListOnScreen
        onBasePage.pressHomeButton()
        onMobile.returnAppFromBackground()
        newFilesList = onPhotos.photoListOnScreen
        onPhotos.shouldListsEqually(filesList, newFilesList)
    }

    @Test
    @TmsLink("4119")
    @Category(Regression::class)
    fun shouldOpenToScrolledFiles() {
        onBasePage.openFiles()
        onBasePage.swipeDownToUpNTimes(2)
        val filesList: List<String?>
        val newFilesList: List<String?>
        filesList = onFileList.fileListOnScreen
        onBasePage.pressHomeButton()
        onMobile.openAppSwitcher()
        onMobile.openAppSwitcher()
        newFilesList = onFileList.fileListOnScreen
        onFileList.shouldListsEqually(filesList, newFilesList)
    }

    @Test
    @TmsLink("5530")
    @Category(Quarantine::class)
    fun shouldSwitchingToScrolledFeed() {
        onBasePage.openFeed()
        onBasePage.swipeDownToUpNTimes(4)
        val filesList: List<String?>
        val newFilesList: List<String?>
        filesList = onFeedPage.getFeedListOnScreen()
        onBasePage.openFiles()
        onBasePage.openFeed()
        newFilesList = onFeedPage.getFeedListOnScreen()
        onFileList.shouldListsEqually(filesList, newFilesList)
    }

    @Test
    @TmsLink("5535")
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 30)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @Category(Quarantine::class)
    fun shouldSwitchingToScrolledPhotos() {
        onBasePage.openPhotos()
        onBasePage.swipeDownToUpNTimes(1)
        val filesList: List<String?>
        val newFilesList: List<String?>
        filesList = onPhotos.photoListOnScreen
        onBasePage.openFiles()
        onBasePage.openPhotos()
        newFilesList = onPhotos.photoListOnScreen
        onFileList.shouldListsEqually(filesList, newFilesList)
    }

    @Test
    @TmsLink("8040")
    @Category(Regression::class)
    fun openNotesFrom360Tab() {
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
    }

    @Test
    @TmsLink("8033")
    @Category(Regression::class)
    fun openDiskFrom360Tab() {
        onBasePage.openDisk()
        onBasePage.shouldBeOnFeed()
    }

    @Test
    @TmsLink("8027")
    @Category(Regression::class)
    fun open360Tab() {
        onBasePage.openMail360Tab()
        onBasePage.shouldBeOnMail360Tab()
    }

    @Test
    @TmsLink("8026")
    @Category(Regression::class)
    fun shouldSeeNavigationBar() {
        onBasePage.shouldSeeFeedTab()
        onBasePage.shouldSeeFilesTab()
        onBasePage.shouldSeePhotosTab()
        onBasePage.shouldSeeAlbumsTab()
        onBasePage.shouldSeeMail360Tab()
    }

    @Test
    @TmsLink("8036")
    @Category(Regression::class)
    fun openTelemostFrom360Tab() {
        onBasePage.openTelemost()
        onBasePage.shouldBeOnTelemost()
    }
}
