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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.MoveFilesToTrashSnackBarMessages
import java.util.*

@Feature("Search files and folders")
@UserTags("search")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class SearchFileTest : SearchFileTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1730")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_SEARCH])
    @Category(Quarantine::class)
    fun shouldOpenFileFromLocalSearch() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.swipeUpToDownNTimes(2)
        onFiles.switchToAirplaneMode()
        onFiles.shouldOpenSearch()
        onSearch.searchFilesOrFolders(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.shouldSeeSearchCurrentFolderStub()
        onSearch.approveSearchInCurrentFolder()
        onSearch.openFileOrFolderFromSearch(FilesAndFolders.FILE_FOR_SEARCH)
        onPreview.shouldBeOnPreview()
    }

    @Test
    @TmsLink("5408")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(FullRegress::class)
    fun shouldOpenCachedFolderFromSearch() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.navigateUp()
        onFiles.switchToAirplaneMode()
        onFiles.shouldOpenSearch()
        onSearch.searchFilesOrFolders(FilesAndFolders.UPLOAD_FOLDER)
        onSearch.shouldSeeSearchCurrentFolderStub()
        onSearch.approveSearchInCurrentFolder()
        onSearch.openFileOrFolderFromSearch(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.navigateUp()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldBeOnDiskRoot()
        onSearch.shouldSearchBeDisabled()
    }

    @Test
    @TmsLink("1727")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldFoundFileByNamePart() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldOpenSearch()
        onFiles.switchToAirplaneMode()
        onSearch.searchFilesOrFolders(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.shouldSeeSearchCurrentFolderStub()
        onSearch.approveSearchInCurrentFolder()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.searchFilesOrFolders("jpg")
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.searchFilesOrFolders(FilesAndFolders.FILE_FOR_SEARCH.substring(0, 5))
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.searchFilesOrFolders(FilesAndFolders.FILE_FOR_SEARCH.uppercase(Locale.getDefault()))
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
    }

    @Test
    @TmsLink("1728")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldNotFoundFileNotExist() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.switchToAirplaneMode()
        onFiles.shouldOpenSearch()
        onSearch.searchFilesOrFolders(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.shouldSeeSearchCurrentFolderStub()
        onSearch.approveSearchInCurrentFolder()
        onSearch.shouldSeeNoResultsFoundStub()
    }

    @Test
    @TmsLink("1723")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_SEARCH],
        targetFolder = FilesAndFolders.LONG_NAME_LESS_255_SYMBOLS
    )
    @CreateFolders(folders = [FilesAndFolders.LONG_NAME_LESS_255_SYMBOLS])
    @DeleteFiles(files = [FilesAndFolders.LONG_NAME_LESS_255_SYMBOLS])
    @Category(Regression::class)
    fun shouldFindLongName() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.LONG_NAME_LESS_255_SYMBOLS, 1)
        onSearch.shouldSearchFieldContainsText(FilesAndFolders.LONG_NAME_LESS_255_SYMBOLS)
        onSearch.openFileOrFolderFromSearch(FilesAndFolders.LONG_NAME_LESS_255_SYMBOLS)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
    }

    @Test
    @TmsLink("1743")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH])
    @CreateFolders(
        folders = [
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER,
        ]
    )
    @DeleteFiles(
        files = [
            FilesAndFolders.FILE_FOR_SEARCH,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER,
        ]
    )
    @Category(Regression::class)
    fun shouldFindFolderFromMoveDialog() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.shouldOpenMoveDialogForFile(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.shouldOpenModalSearch()
        onSearch.searchFilesOrFolders(FilesAndFolders.FOLDER_POSTFIX)
        onSearch.hideKeyboardIfShown()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.pressHardBack()
        onFiles.shouldTargetListBeMoreThan(1)
        onSearch.shouldSearchBeDisabled()
    }

    @Test
    @TmsLink("4642")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.ORIGINAL_PREFIX, 2)
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILES)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onSearch.shouldSearchBeEnabled()
        onSearch.pressHardBack()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("4577")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldKeepSearchResultsWhenOrientationChangedOnFiles() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, 1)
        onSearch.pressHardBack() //close keyboard
        onSearch.shouldKeyboardNotShown()
        onSearch.rotate(ScreenOrientation.LANDSCAPE)
        onSearch.shouldSearchBeEnabled()
        onSearch.shouldDisplayedSearchingResult(1)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldKeyboardNotShown()
    }

    @Test
    @TmsLink("3396")
    @PushFileToDevice(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + "/" + FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldKeepSearchResultsWhenOrientationChangedOnFileManager() {
        onBasePage.openFiles()
        onFiles.openAddFileDialog()
        onFiles.shouldOpenSearchOnFileManager()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, 1)
        onSearch.hideKeyboardIfShown()
        onSearch.rotate(ScreenOrientation.LANDSCAPE)
        onSearch.shouldSearchBeEnabled()
        onSearch.shouldDisplayedSearchingResult(1)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("5476")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_SEARCH])
    @Category(Regression::class)
    fun shouldShowResultsFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.swipeUpToDownNTimes(1)
        onFiles.shouldOpenSearch()
        onSearch.shouldFindNFilesOrFolders(FilesAndFolders.FILE_FOR_SEARCH, 1)
        onSearch.shouldDisplayedSearchingResult(1)
        onSearch.closeSearch()
        onBasePage.shouldBeOnFiles()
    }

    @Test
    @TmsLink("4606")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFileBeSearchedByExtension() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(".JPG")
        onFiles.shouldSearchResultContainsFile(FilesAndFolders.FILE_FOR_SEARCH)
    }

    @Test
    @TmsLink("4570")
    @Category(Regression::class)
    @CreateFolders(
        folders = [
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
        ]
    )
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFolderBeSearchedByName() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSearchResultContainsFile(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("4572")
    @Category(Regression::class)
    @CreateFolders(
        folders = [
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER + "/" + FilesAndFolders.RENAMED_FOLDER,
        ]
    )
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldSearchedFolderBreadcrumbBeDisplayed() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldSearchResultContainsFile(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldSearchResultWithNameShouldHasBreadcrumb(
            FilesAndFolders.RENAMED_FOLDER,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER + "/"
        )
    }

    @Test
    @TmsLink("4574")
    @Category(Regression::class)
    @CreateFolders(
        folders = [
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER,
        ]
    )
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldOpenFolderFromSearchResult() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSearchResultContainsFile(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFileOrFolderFromSearchResults(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSearchBeClosed()
        onFiles.shouldFolderBeOpened(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onFiles.shouldFolderBeOpened(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSearchBeClosed()
    }

    @Test
    @TmsLink("4566")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldSearchResultsDoNotClosePhotoViewer() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.openFileOrFolderFromSearch(FilesAndFolders.FILE_FOR_SEARCH)
        onPreview.shouldBeOnPreview()
        onPreview.closePreview()
        onSearch.shouldSearchBeEnabled()
        onSearch.shouldDisplayedSearchingResult(1)
    }

    @Test
    @TmsLink("4590")
    @Category(Regression::class)
    fun shouldStubBeDisplayedIfNoSearchResult() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile("adisk-4590")
        onSearch.shouldSeeNoResultsFoundStub()
    }

    @Test
    @TmsLink("4593")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldGroupModeBePresentedWhileSearch() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.TARGET_FOLDER)
        onFiles.longTapOnFile(FilesAndFolders.TARGET_FOLDER)
        onBasePage.shouldBeOnGroupMode()
    }

    @Test
    @TmsLink("4562")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFileBeSearchedByName() {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile("openFromSearchFile")
        onFiles.shouldSearchResultContainsFile(FilesAndFolders.FILE_FOR_SEARCH)
    }

    @Test
    @TmsLink("5598")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    fun shouldFileBeSearchedFromFeed() {
        onBasePage.openFeed()
        onBasePage.shouldBeOnFeed()
        onFeed.shouldOpenModalSearch()
        onFeed.searchFile("openFromSearchFile")
        onFeed.shouldSearchResultContainsFile(FilesAndFolders.FILE_FOR_SEARCH)
        onSearch.closeSearch()
        onBasePage.shouldBeOnFeed()
    }
}
