package ru.yandex.autotests.mobile.disk.android.geoalbums

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
import ru.yandex.autotests.mobile.disk.android.blocks.MediaItem
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.CreateUser
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@CreateUser
@AuthorizationTest
@Feature("Geo Albums")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GeoAlbumsTest : GeoAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6918")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldUploadPhotoToUnlim() {
        shouldAutoUploadMedia(FilesAndFolders.GEO_PHOTO_TO_UPLOAD, FilesAndFolders.GEO_PHOTO_AUTO_UPLOADED, true)
    }

    @Test
    @TmsLink("6919")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldUploadPhotoToDisk() {
        shouldUploadMediaToDisk(FilesAndFolders.GEO_PHOTO_TO_UPLOAD)
    }

    @Test
    @TmsLink("7069")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldUploadVideoToUnlim() {
        shouldAutoUploadMedia(FilesAndFolders.GEO_VIDEO_TO_UPLOAD, FilesAndFolders.GEO_VIDEO_AUTO_UPLOADED, true)
    }

    @Test
    @TmsLink("7070")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldUploadVideoToDisk() {
        shouldUploadMediaToDisk(FilesAndFolders.GEO_VIDEO_TO_UPLOAD)
    }

    @Test
    @TmsLink("7054")
    @Category(Regression::class)
    @PrepareGeoAlbums
    fun shouldExcludePhotoButShowInPhotos() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.EXCLUDE_FROM_ALBUM)
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onBasePage.openPhotosWithDailyLayout()
        onPhotosPage.sendPushesUntilAppeared(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
    }

    @Test
    @TmsLink("7053")
    @Category(Regression::class)
    @PrepareGeoAlbums
    fun shouldDeleteCloudyFileFromPhotos() {
        onBasePage.openPhotosWithDailyLayout()
        onPhotosPage.sendPushesUntilAppeared(FilesAndFolders.GEO_CLOUDY_FILE_NAME, MediaItem.Status.CLOUDY)
        onPhotosPage.longTapCloudyItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onGroupMode.clickDeleteAndChooseFromDisk()
        onPhotosPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
    }

    @Test
    @TmsLink("7068")
    @Category(Regression::class)
    @PrepareGeoAlbums
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    fun shouldDeleteInitialFileWithCheckForCopy() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.COPY)
        onFiles.shouldFilesListBeNotEmpty()
        onFiles.pressHardBackNTimes(2)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onGroupMode.shouldGroupModeNotBeActivated()
        onAlbumsPage.longTapCloudyItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onGroupMode.clickDeleteAndChooseFromDisk()
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
    }
}
