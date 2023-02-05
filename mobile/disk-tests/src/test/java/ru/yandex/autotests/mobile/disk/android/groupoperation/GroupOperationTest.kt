package ru.yandex.autotests.mobile.disk.android.groupoperation

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Group mode")
@UserTags("groupMode")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GroupOperationTest : GroupOperationTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1800")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_GO])
    @Category(Regression::class)
    fun shouldGroupOperationModeEnabledOnFiles() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldEnableGroupOperationMode()
        onFiles.shouldNFilesBeCounted(0)
        onFiles.shouldNCheckboxesBeEnabled(0)
    }

    @Test
    @TmsLink("4257")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldCloseGroupOperationByCloseButtonClicked() {
        onBasePage.openPhotos()
        onPhotos.selectRandomPhotoByLongClick()
        onGroupMode.shouldCloseButtonBeEnabled()
        onGroupMode.shouldClickCloseButton()
        onGroupMode.shouldCloseButtonNotBeDisplayed()
    }

    @Test
    @TmsLink("4335")
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldSaveStateIfRotationChanged() {
        onBasePage.openPhotos()
        onPhotos.selectNPhoto(3)
        onPhotos.shouldNFilesBeCounted(3)
        onPhotos.rotate(ScreenOrientation.LANDSCAPE)
        onPhotos.shouldNFilesBeCounted(3)
    }

    @Test
    @TmsLink("4291")
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldCloseGroupModeIfMomentUnselected() {
        onBasePage.openPhotos()
        onPhotos.selectMoment()
        onPhotos.shouldNFilesBeCounted(5)
        onPhotos.clickMoment()
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("4984")
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldMomentBeSelectedIfAllPhotosSelected() {
        onBasePage.openPhotos()
        onPhotos.selectNPhoto(5)
        onPhotos.shouldMomentBeSelected()
    }

    @Test
    @TmsLink("4264")
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldPhotosCountChanged() {
        onBasePage.openPhotos()
        onPhotos.selectNPhoto(3)
        onPhotos.shouldNFilesBeCounted(3)
    }

    @Test
    @TmsLink("4263")
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldPhotosBeSelectedIfMomentSelected() {
        onBasePage.openPhotos()
        onPhotos.selectMoment()
        onPhotos.shouldNFilesBeCounted(5)
        onPhotos.shouldNCheckboxesBeSelected(5)
    }

    @Test
    @TmsLink("4258")
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD, FilesAndFolders.FOURTH, FilesAndFolders.FIFTH],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldCloseGroupModeIfPhotosUnselected() {
        onBasePage.openPhotos()
        onPhotos.selectRandomPhotoByLongClick()
        onPhotos.selectNPhoto(2)
        onPhotos.shouldNFilesBeCounted(3)
        onPhotos.deselectNPhoto(3)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("6239")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.FOLDER_1])
    @DeleteFiles(files = [FilesAndFolders.FOLDER_1, FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldGroupOperationsBeActivatedAfterPullToRefresh() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FOLDER_1, FilesAndFolders.ORIGINAL_FILE)
        onFiles.updateFileList()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.FOLDER_1, FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.shouldNFilesBeCounted(2)
    }

    @Test
    @TmsLink("6240")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @UploadFiles(filePaths = [FilesAndFolders.FIRST], targetFolder = FilesAndFolders.CAMERA_UPLOADS)
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    fun shouldOpenGroupOperationsIntoCameraFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldEnableGroupOperationMode()
        onFiles.shouldNCheckboxesBeEnabled(0)
        onGroupMode.shouldMoreOptionsButtonBeEnabled()
    }
}
