package ru.yandex.autotests.mobile.disk.android.sort

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import com.google.inject.name.Named
import io.qameta.allure.Feature
import io.qameta.allure.Flaky
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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.SortFiles

@Feature("Sort")
@UserTags("sort")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class SortTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onSearch: SearchSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps

    @Test
    @Flaky
    @TmsLink("1442")
    @UploadFiles(
        filePaths = [SortFiles.TERMINATOR, SortFiles.AAAA, SortFiles.ABBB, SortFiles.ACCC],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(
        folders = [
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER + "/" + SortFiles.BBBB,
            FilesAndFolders.CONTAINER_FOLDER + "/" + SortFiles.CC01,
            FilesAndFolders.CONTAINER_FOLDER + "/" + SortFiles.CC02,
        ]
    )
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldSwitchFileSorting() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesBeSortedInOrder(
            SortFiles.TERMINATOR,
            SortFiles.BBBB,
            SortFiles.CC01,
            SortFiles.CC02,
            SortFiles.AAAA,
            SortFiles.ABBB,
            SortFiles.ACCC
        )
        onFiles.shouldFilesOrFoldersExist(SortFiles.BBBB) //scroll up to file list
        onBasePage.swipeUpToDownNTimes(1)
        onFiles.switchSortOrderByDate()
        //use first modified file as terminator
        onFiles.shouldFilesBeSortedInOrder(
            SortFiles.TERMINATOR,
            SortFiles.BBBB,
            SortFiles.CC01,
            SortFiles.CC02,
            SortFiles.AAAA,
            SortFiles.ABBB,
            SortFiles.ACCC
        )
    }

    @Test
    @TmsLink("1452")
    @UploadFiles(
        filePaths = [
            FilesAndFolders.FIRST,
            FilesAndFolders.SECOND,
            FilesAndFolders.THIRD,
            FilesAndFolders.PHOTO,
            FilesAndFolders.FILE_FOR_VIEWING_1,
            FilesAndFolders.FILE_FOR_VIEWING_2,
        ],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeOnlyFilesFromSearchInViewer() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        //preload preview
        onFiles.openImage(Images.FIRST.path)
        onFiles.pressHardBack()
        onFiles.openImage(Images.SECOND.path)
        onFiles.pressHardBack()
        onFiles.openImage(Images.THIRD.path)
        onFiles.pressHardBack()
        //start check
        onFiles.switchToAirplaneMode()
        onFiles.shouldOpenSearch()
        onSearch.searchFilesOrFolders("png")
        onSearch.approveSearchInCurrentFolder()
        onFiles.shouldFileListHasSize(3)
        onFiles.shouldOpenImageIntoViewer(Images.FIRST.path)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
    }

    @Test
    @TmsLink("1447")
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(FullRegress::class)
    fun shouldFileInViewerBeSortedLikeFolderSort() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldOpenImageIntoViewer(Images.FIRST.path)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
        onPreview.closePreview()
        onFiles.switchSortOrderByDate()
        onFiles.shouldOpenImageIntoViewer(Images.FIRST.path)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.THIRD)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.THIRD)
    }

    @Test
    @TmsLink("1445")
    @UploadFiles(filePaths = [SortFiles.TERMINATOR, SortFiles.AAAA, SortFiles.ABBB, SortFiles.ACCC])
    @CreateFolders(folders = [SortFiles.BBBB, SortFiles.CC01, SortFiles.CC02])
    @DeleteFiles(
        files = [
            SortFiles.AAAA,
            SortFiles.ABBB,
            SortFiles.ACCC,
            SortFiles.BBBB,
            SortFiles.CC01,
            SortFiles.CC02,
            SortFiles.TERMINATOR,
        ]
    )
    @Category(FullRegress::class)
    fun shouldFileAndFoldersBeSortedOnOfflineRoot() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(
            SortFiles.BBBB,
            SortFiles.CC01,
            SortFiles.CC02,
            SortFiles.AAAA,
            SortFiles.ABBB,
            SortFiles.ACCC,
            SortFiles.TERMINATOR
        )
        onBasePage.openOffline()
        onFiles.shouldFilesBeSortedInOrder(
            SortFiles.TERMINATOR,
            SortFiles.BBBB,
            SortFiles.CC01,
            SortFiles.CC02,
            SortFiles.ACCC,
            SortFiles.ABBB,
            SortFiles.AAAA
        )
    }

    @Test
    @TmsLink("1446")
    @UploadFiles(
        filePaths = [SortFiles.TERMINATOR, SortFiles.AAAA, SortFiles.ABBB, SortFiles.ACCC],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(
        folders = [
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.TARGET_FOLDER + "/" + SortFiles.BBBB,
            FilesAndFolders.TARGET_FOLDER + "/" + SortFiles.CC01,
            FilesAndFolders.TARGET_FOLDER + "/" + SortFiles.CC02,
        ]
    )
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldFileAndFoldersBeSortedInOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesBeSortedInOrder(
            SortFiles.TERMINATOR,
            SortFiles.BBBB,
            SortFiles.CC01,
            SortFiles.CC02,
            SortFiles.AAAA,
            SortFiles.ABBB,
            SortFiles.ACCC
        )
    }

    @Test
    @TmsLink("1449")
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD])
    @Category(FullRegress::class)
    fun shouldFileInViewerBeSortedLikeFolderSortOnOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD)
        onBasePage.openOffline()
        onFiles.shouldOpenImageIntoViewer(Images.FIRST.path)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.THIRD)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.THIRD)
    }

    @Test
    @TmsLink("1450")
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD],
        targetFolder = FilesAndFolders.CAMERA_UPLOADS
    )
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(FullRegress::class)
    fun shouldFileInViewerBeSortedLikeFolderSortInSavedToOfflineCameraUploadsFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CAMERA_UPLOADS)
        onBasePage.openOffline()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldOpenImageIntoViewer(Images.FIRST.path)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
    }

    @Test
    @TmsLink("1451")
    @UploadFiles(
        filePaths = [
            SortFiles.TERMINATOR,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_GO,
        ],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldFilesWithNonLatinCharactersSortedNearSimilarLatinCharacters() {
        onDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ONLY_LATIN,
            true
        )
        onDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.SIMILAR_LATIN,
            true
        )
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesBeSortedInOrder(
            SortFiles.TERMINATOR,
            FilesAndFolders.ONLY_LATIN,
            FilesAndFolders.SIMILAR_LATIN,
            FilesAndFolders.FILE_FOR_GO
        )
    }
}
