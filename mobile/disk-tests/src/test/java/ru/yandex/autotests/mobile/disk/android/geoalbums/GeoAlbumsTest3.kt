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
import ru.yandex.autotests.mobile.disk.android.pages.albums.GeoAlbumsPage
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.CreateUser
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@CreateUser
@AuthorizationTest
@Feature("Geo Albums")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GeoAlbumsTest3 : GeoAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6706")
    @PrepareGeoAlbums
    @Category(Regression::class)
    fun shouldDeleteLastFileFromViewer() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.swipeDownToUpNTimes(8)
        onAlbumsPage.clickItem(FilesAndFolders.GEO_LAST_FILE)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.GEO_FILE_BEFORE_LAST)
        onPreview.pressHardBackNTimes(2)
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_LAST_FILE)
    }

    @Test
    @TmsLink("7047")
    @Category(Regression::class)
    @PrepareGeoAlbums
    fun shouldExcludeLastFileFromViewer() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.swipeDownToUpNTimes(8)
        onAlbumsPage.clickItem(FilesAndFolders.GEO_LAST_FILE)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.GEO_FILE_BEFORE_LAST)
        onPreview.pressHardBackNTimes(2)
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_LAST_FILE)
    }

    @Test
    @TmsLink("7084")
    @PrepareGeoAlbums
    @Category(Regression::class)
    fun shouldExcludePhoto() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.EXCLUDE_FROM_ALBUM)
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
    }

    @Test
    @TmsLink("7086")
    @PrepareGeoAlbums
    @Category(Regression::class)
    fun shouldCameFromViewerInfoPanel() {
        onBasePage.openAlbums()
        onAlbumsPage.shouldDisplayGeoMetaAlbum()
        onBasePage.openPhotosWithDailyLayout()
        onPhotosPage.sendPushesUntilAppeared(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onPhotosPage.clickItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onPreview.openPhotoInformation()
        onPreview.openAlbum(GeoAlbumsPage.ZURICH_ALBUM_NAME)
        onBasePage.shouldBeOnPageWithTitle(GeoAlbumsPage.ZURICH_ALBUM_NAME)
    }

    @Test
    @TmsLink("6917")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldUploadPhotoToCamera() {
        shouldAutoUploadMedia(FilesAndFolders.GEO_PHOTO_TO_UPLOAD, FilesAndFolders.GEO_PHOTO_AUTO_UPLOADED, false)
    }

    @Test
    @TmsLink("6684")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldDeleteCloudyLocalFileFromDisk() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyLocalItem(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE)
        onGroupMode.clickDeleteAndChooseOnlyFromDisk()
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE)
        onBasePage.openPhotosWithDailyLayout()
        onPhotosPage.sendPushesUntilAppeared(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE, MediaItem.Status.LOCAL)
    }

    @Test
    @TmsLink("7071")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldUploadVideoToCamera() {
        shouldAutoUploadMedia(FilesAndFolders.GEO_VIDEO_TO_UPLOAD, FilesAndFolders.GEO_VIDEO_AUTO_UPLOADED, false)
    }
}
