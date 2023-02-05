package ru.yandex.autotests.mobile.disk.android.photoviewer

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
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Photo viewer")
@UserTags("photoviewer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoViewerTest5 : PhotoViewerTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6065")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldCloseOptionsMenuAfterTapOnViewer() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.openOptionsMenu()
        onPreview.rotate(ScreenOrientation.LANDSCAPE)
        onPreview.wait(2, TimeUnit.SECONDS)
        onPreview.tapAt(200, 400)
        onPreview.wait(1, TimeUnit.SECONDS)
        onPreview.shouldBeOnPreview()
    }

    @Test
    @TmsLink("6066")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldCloseOptionsMenuAfterSwipe() {
        onBasePage.rotate(ScreenOrientation.LANDSCAPE)
        onBasePage.openFiles()
        onFiles.switchToListLayout()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.openOptionsMenu()
        onPreview.shouldOptionsMenuBeVisible(true)
        onPreview.rotate(ScreenOrientation.PORTRAIT)
        onPreview.shouldOptionsMenuBeVisible(true)
    }

    @Test
    @TmsLink("6063")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldStayOnOptionsMenuAfterOrientationChange() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.openOptionsMenu()
        onPreview.wait(2, TimeUnit.SECONDS)
        onPreview.swipeUpToDownNTimes(1)
        onPreview.wait(1, TimeUnit.SECONDS)
        onPreview.shouldBeOnPreview()
    }

    @Test
    @TmsLink("6062")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Quarantine::class)
    fun shouldOpenOptionsMenuInLandscape() {
        onBasePage.rotate(ScreenOrientation.LANDSCAPE)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.openOptionsMenu()
        onPreview.shouldOptionsMenuBeVisible(true)
    }
}
