package ru.yandex.autotests.mobile.disk.android.deletelink

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.PublishFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Delete link")
@UserTags("deleteLink")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DeleteLinkTest3 : DeleteLinkTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2476")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.FILE_FOR_RESTORING])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteActionBeNotPresentedWhenNotAllSelectedFilesHasPublicLink() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.FILE_FOR_RESTORING,
            FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER
        )
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE_LINK)
    }

    @Test
    @TmsLink("2127")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldDeleteLinkOnFileAlreadyDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onUserDiskApi.unpublishFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("2130")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteLinkOnFolderWhenDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
        onUserDiskApi.unpublishFile(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.updateFileList()
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("2135")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldSeeNoInternetToastWhenDeleteLinkOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onGroupMode.shouldSeeToastWithMessage(ToastMessages.YOU_ARE_NOT_CONNECTED_TOAST)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("2138")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenDeleteLinkOnDeletedFile() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onUserDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onGroupMode.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_DELETE_LINK_TOAST)
        onBasePage.shouldBeNotOnGroupMode()
    }

    @Test
    @TmsLink("2134")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldNotSeePublicMarkerForUnpublishedFullAccessDir() {
        val sharedFolder = nameHolder.name
        onShareDiskApi.publishFile(sharedFolder)
        onBasePage.openFiles()
        onFiles.updateFileList()
        deletePublicLink(sharedFolder)
        onFiles.shouldNotSeePublicFileMark(sharedFolder)
    }

    @Test
    @TmsLink("2123")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldNotSeePublicMarkerForUnpublishedFile() {
        onBasePage.openFiles()
        deletePublicLink(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
    }
}
