package ru.yandex.autotests.mobile.disk.android.filesrenaming

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.PhotosSteps
import ru.yandex.autotests.mobile.disk.android.steps.PreviewSteps
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Rename photos")
@UserTags("renamePhotos")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class RenamePhotoTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Test
    @TmsLink("4320")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.RENAMED_PHOTO])
    @Category(FullRegress::class)
    fun renamePhotoFromGallery() {
        onBasePage.openPhotos()
        onPhotos.selectRandomPhotoByLongClick()
        onPhotos.shouldRenamePhoto(FilesAndFolders.RENAMED_PHOTO)
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.RENAMED_PHOTO)
    }
}
