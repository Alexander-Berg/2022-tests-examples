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
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.*
import ru.yandex.autotests.mobile.disk.data.SortFiles

@Feature("Photo viewer")
@UserTags("photoviewer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoViewerTest6: PhotoViewerTestRunner() {
    companion object {
        @JvmField
        @ClassRule
        var classRuleChain = RulesFactory.createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6104")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FILE_FOR_VIEWING_1, FILE_FOR_VIEWING_2, BIG_FILE], targetFolder = UPLOAD_FOLDER)
    @CreateFolders(folders = [UPLOAD_FOLDER])
    @DeleteFiles(files = [UPLOAD_FOLDER])
    fun shouldSeeSearchFilesInRightOrder() {
        onBasePage.openFiles()
        with(onFiles) {
            updateFileList()
            shouldOpenSearch()
            searchFile("file")
            hideKeyboardIfShown()
            shouldFilesBeSortedInOrder(SortFiles.TERMINATOR, BIG_FILE, FILE_FOR_VIEWING_2, FILE_FOR_VIEWING_1)
            openImage(BIG_FILE)
        }
        with(onPreview) {
            openPhotoInformation()
            shouldPhotoHasName(BIG_FILE)
            swipeLeftToRight()
            shouldPhotoHasName(BIG_FILE)
            swipeRightToLeft()
            shouldPhotoHasName(FILE_FOR_VIEWING_2)
            swipeRightToLeft()
            shouldPhotoHasName(FILE_FOR_VIEWING_1)
            swipeRightToLeft()
            shouldPhotoHasName(FILE_FOR_VIEWING_1)
        }
    }
}
