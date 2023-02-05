package ru.yandex.autotests.mobile.disk.android.upload

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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.CreateFoldersOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.TouchFileOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders.*
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.*
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.NameHolder
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Upload files and folder by FAB")
@UserTags("upload")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class UploadOverFabTest {
    companion object {
        @JvmField
        @ClassRule
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Test
    @TmsLink("1275")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(Quarantine::class)
    fun shouldNotSeeFabButtonIntoReadOnlyDir() {
        val name = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(name)
        onFiles.shouldNotSeeFabButton()
    }

    @Test
    @TmsLink("1256")
    @PushFileToDevice(filePaths = [ORIGINAL_FILE], targetFolder = FOLDER_255_SYMBOLS_FULL_PATH)
    @CreateFoldersOnDevice(folders = [FOLDER_255_SYMBOLS_FULL_PATH])
    @DeleteFilesOnDevice(filesPath = [FOLDER_255_SYMBOLS_FULL_PATH])
    @DeleteFiles(files = [NAME_255_SYMBOLS])
    @Category(FullRegress::class)
    fun shouldUploadFolderWith255SymbolsInName() {
        onBasePage.openFiles()
        onFiles.openAddFileDialog()
        onGroupMode.selectFilesOrFoldersOnGroupMode(NAME_255_SYMBOLS)
        onFiles.pressUploadToDisk()
        onFiles.shouldFilesOrFoldersExist(NAME_255_SYMBOLS)
        onFiles.openFolder(NAME_255_SYMBOLS)
        onFiles.shouldFilesOrFoldersExist(ORIGINAL_FILE)
        onFiles.waitDownloadComplete()
        onFiles.shouldFilesOrFoldersExist(ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1257")
    @Category(FullRegress::class)
    fun shouldSeeToastWhenPressOnUploadButtonWithoutSelectedFiles() {
        onBasePage.openFiles()
        onFiles.openAddFileDialog()
        onFiles.pressUploadToDisk()
        onFiles.shouldSeeToastWithMessage(ToastMessages.SELECT_FILES_TO_UPLOAD_TOAST)
    }

    @Test
    @TmsLink("1266")
    @Category(FullRegress::class)
    fun shouldSeeAddMenuAfterSwitchingOrientation() {
        onBasePage.openFiles()
        onFiles.pressFAB()
        onFiles.shouldSeeFabMenu()
        onFiles.rotate(ScreenOrientation.LANDSCAPE)
        onFiles.shouldSeeFabMenu()
        onFiles.pressOnFileFromDevice()
        onFiles.shouldFileManagerBePresented()
    }

    @Test
    @TmsLink("1265")
    @TouchFileOnDevice(filePath = FILE_255_SYMBOLS_FULL_PATH)
    @DeleteFiles(files = [NAME_255_SYMBOLS])
    @DeleteFilesOnDevice(filesPath = [FILE_255_SYMBOLS_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldUploadFileWith255SymbolsInName() {
        onBasePage.openFiles()
        onFiles.openAddFileDialog()
        onGroupMode.selectFilesOrFoldersOnGroupMode(NAME_255_SYMBOLS)
        onFiles.pressUploadToDisk()
        onFiles.shouldFilesOrFoldersExist(NAME_255_SYMBOLS)
        onFiles.waitDownloadComplete()
        onFiles.shouldFilesOrFoldersExist(NAME_255_SYMBOLS)
    }

    @Test
    @TmsLink("2887")
    @UploadFiles(filePaths = [FIRST])
    @PushFileToDevice(filePaths = [FIRST])
    @DeleteFiles(files = [FIRST])
    @DeleteFilesOnDevice(filesPath = [STORAGE_ROOT + FIRST])
    @Category(FullRegress::class)
    fun shouldNotReplaceAlreadyExistedFileWhenUploadCanceled() {
        onDiskApi.updateFile(FIRST, DISK_ROOT, SECOND) //Replace file content
        onBasePage.openFiles()
        onFiles.updateFileList()
        onFiles.shouldOpenImageIntoViewer(FIRST)
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.closePreview()
        onFiles.openAddFileDialog()
        onGroupMode.selectFilesOrFoldersOnGroupMode(FIRST)
        onFiles.pressUploadToDisk()
        onFiles.shouldSeeReplaceFileAlert()
        onFiles.cancelFileReplacing()
        onFiles.shouldNotSeeQueuedMark()
        onFiles.shouldOpenImageIntoViewer(FIRST)
        onPreview.shouldCurrentImageBe(Images.SECOND)
    }
}
