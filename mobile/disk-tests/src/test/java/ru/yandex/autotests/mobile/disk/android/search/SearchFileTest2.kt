package ru.yandex.autotests.mobile.disk.android.search

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Search files and folders")
@UserTags("search")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class SearchFileTest2 : SearchFileTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1729")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(FullRegress::class)
    fun shouldOpenFolderFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.switchToAirplaneMode()
        onFiles.shouldOpenSearch()
        onSearch.searchFilesOrFolders(FilesAndFolders.UPLOAD_FOLDER)
        onSearch.shouldSeeSearchCurrentFolderStub()
        onSearch.approveSearchInCurrentFolder()
        onSearch.openFileOrFolderFromSearch(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldUnableToFileListStubBePresented()
        onFiles.navigateUp()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.UPLOAD_FOLDER)
        onBasePage.shouldBeOnFiles()
        onSearch.shouldSearchBeDisabled()
    }

    @Test
    @TmsLink("4646")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_SEARCH])
    @Category(FullRegress::class)
    fun shouldSaveToOfflineFileFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.searchFilesOrFolders(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.shouldSearchBeEnabled()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onBasePage.pressHardBack()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
    }

    @Test
    @TmsLink("5407")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldRenameFolderFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, 1)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onSearch.shouldSearchBeEnabled()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onSearch.shouldDisplayedSearchingResult(1)
        onSearch.pressHardBack()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("4639")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(
        files = [
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.TARGET_FOLDER
        ]
    )
    @Category(FullRegress::class)
    fun shouldMoveFileFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.ORIGINAL_PREFIX, 2)
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE
        )
        onSearch.shouldSearchBeEnabled()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onSearch.shouldDisplayedSearchingResult(2)
        onBasePage.pressHardBack()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("4647")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldShareFileFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.ORIGINAL_PREFIX, 2)
        onFiles.shouldCopyLinkForFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldSeeToastWithMessage(ToastMessages.COPIED_TO_CLIPBOARD)
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onSearch.shouldSearchBeEnabled()
        onSearch.pressHardBack()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
    }
}
