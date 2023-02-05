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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.CreateUser
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@CreateUser
@AuthorizationTest
@Feature("Geo Albums")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GeoAlbumsTest2 : GeoAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6685")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldDeleteCloudyLocalFileFromDevice() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyLocalItem(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE)
        onGroupMode.clickDeleteFromDevice()
        onAlbumsPage.shouldDisplayItemWithStatus(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE, MediaItem.Status.CLOUDY)
        onBasePage.openPhotosWithDailyLayout()
        onPhotosPage.sendPushesUntilAppeared(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE, MediaItem.Status.CLOUDY)
    }

    @Test
    @TmsLink("6686")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldDeleteCloudyLocalFileTotally() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyLocalItem(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE)
        onGroupMode.clickDeleteAndChooseFromDiskAndDevice()
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE)
        onBasePage.openPhotosWithDailyLayout()
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_LOCAL_FILE)
    }

    @Test
    @TmsLink("6687")
    @PrepareGeoAlbums
    @Category(Regression::class)
    fun shouldDeleteCloudyFile() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onGroupMode.clickDeleteAndChooseFromDisk()
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onBasePage.openPhotosWithDailyLayout()
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
    }

    @Test
    @TmsLink("6698")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldShareCloudyLocalFileLink() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.longTapCloudyLocalItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.SHARE_LINK)
        onShare.shouldSeeShareMenu()
    }

    @Test
    @TmsLink("6705")
    @PrepareGeoAlbums
    @Category(Regression::class)
    fun shouldDeletePhotoFromViewer() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.clickItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.GEO_FILE_AFTER_CLOUDY)
        onPreview.pressHardBackNTimes(2)
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
    }

    @Test
    @TmsLink("6885")
    @PrepareGeoAlbums
    @Category(Regression::class)
    fun shouldExcludePhotoFromViewer() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.clickItem(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.GEO_FILE_AFTER_CLOUDY)
        onPreview.pressHardBackNTimes(2)
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_CLOUDY_FILE_NAME)
    }

    @Test
    @TmsLink("7072")
    @PrepareGeoAlbums
    @Category(Quarantine::class)
    fun shouldExcludeVideoFromViewer() {
        onBasePage.openAlbums()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.clickItem(FilesAndFolders.GEO_VIDEO_FILE)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.GEO_FILE_AFTER_VIDEO)
        onPreview.pressHardBackNTimes(2)
        onAlbumsPage.shouldDisplayAnyItemsButNot(FilesAndFolders.GEO_VIDEO_FILE)
    }
}
