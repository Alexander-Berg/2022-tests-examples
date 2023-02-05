package ru.yandex.autotests.mobile.disk.android.aviary

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
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.FilesSteps
import ru.yandex.autotests.mobile.disk.android.steps.OfflineSteps
import ru.yandex.autotests.mobile.disk.android.steps.PreviewSteps
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.NameHolder
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Aviary")
@UserTags("aviary")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class AviaryTest {
    companion object {
        @ClassRule
        @JvmField
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
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Test
    @TmsLink("1745")
    @UploadFiles(filePaths = [FilesAndFolders.PNG_IMAGE, FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.PNG_IMAGE, FilesAndFolders.FILE_FOR_VIEWING_1])
    @Category(BusinessLogic::class)
    fun shouldEditActionBePresented() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.shouldEditorActionBePresented()
        onPreview.pressHardBack() //close menu
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //close editor
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldEditorActionBePresented()
        onPreview.pressHardBack()
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //close editor
    }

    @Test
    @TmsLink("1748")
    @UploadFiles(filePaths = [FilesAndFolders.PNG_IMAGE])
    @DeleteFiles(files = [FilesAndFolders.PNG_IMAGE])
    @Category(Regression::class)
    fun shouldSeeEditorButtonOnPreviewWhenImageOpenedOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PNG_IMAGE)
        onBasePage.openOffline()
        onOffline.openFileIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
    }

    @Test
    @TmsLink("1746")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.PNG_IMAGE])
    @Category(Regression::class)
    fun shouldEditActionBePresentedOnFileIntoFullAccessDir() {
        val fullAccessDir = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(fullAccessDir)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
    }

    @Test
    @TmsLink("1747")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.PNG_IMAGE])
    @Category(Regression::class)
    fun shouldEditActionBeNotPresentedOnFileIntoReadOnlyDir() {
        val readOnlyDir = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(readOnlyDir)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.shouldNotEditorActionBePresented()
    }

    @Test
    @TmsLink("1760")
    @UploadFiles(filePaths = [FilesAndFolders.PNG_IMAGE])
    @DeleteFiles(files = [FilesAndFolders.PNG_IMAGE])
    @Category(FullRegress::class)
    fun shouldCloseAviaryByDoneButtonWhenFileNotChanged() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressDone()
        onPreview.shouldSeeToastWithMessage(ToastMessages.EDITING_CANCELED)
        onPreview.shouldBeNotOnEditor()
        onPreview.shouldNotSeePreviewControls()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PNG_IMAGE)
    }

    @Test
    @TmsLink("1761")
    @UploadFiles(filePaths = [FilesAndFolders.PNG_IMAGE])
    @DeleteFiles(files = [FilesAndFolders.PNG_IMAGE])
    @Category(FullRegress::class)
    fun shouldCloseAviaryByHardBack() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack()
        onPreview.shouldBeNotOnEditor()
        onPreview.shouldNotSeePreviewControls()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PNG_IMAGE)
    }
}
