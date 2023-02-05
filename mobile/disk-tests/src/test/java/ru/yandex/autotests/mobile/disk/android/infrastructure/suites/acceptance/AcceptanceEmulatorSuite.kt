package ru.yandex.autotests.mobile.disk.android.infrastructure.suites.acceptance

import org.junit.experimental.categories.Categories
import org.junit.experimental.categories.Categories.IncludeCategory
import org.junit.runner.RunWith
import org.junit.runners.Suite.SuiteClasses
import ru.yandex.autotests.mobile.disk.android.aviary.AviaryTest
import ru.yandex.autotests.mobile.disk.android.cache.CacheTest
import ru.yandex.autotests.mobile.disk.android.cache.CacheTest2
import ru.yandex.autotests.mobile.disk.android.copyfiles.CopyFilesTest
import ru.yandex.autotests.mobile.disk.android.copyfiles.CopyFilesTest2
import ru.yandex.autotests.mobile.disk.android.copyfiles.CopyFilesTest3
import ru.yandex.autotests.mobile.disk.android.copylink.CopyLinkTest
import ru.yandex.autotests.mobile.disk.android.copylink.CopyLinkTest2
import ru.yandex.autotests.mobile.disk.android.defaultpartition.DefaultPartitionTest
import ru.yandex.autotests.mobile.disk.android.delete.DeleteFilesAndFoldersTest
import ru.yandex.autotests.mobile.disk.android.delete.DeleteFilesAndFoldersTest2
import ru.yandex.autotests.mobile.disk.android.delete.DeleteFilesAndFoldersTest3
import ru.yandex.autotests.mobile.disk.android.deletelink.DeleteLinkTest
import ru.yandex.autotests.mobile.disk.android.deletelink.DeleteLinkTest2
import ru.yandex.autotests.mobile.disk.android.deletelink.DeleteLinkTest3
import ru.yandex.autotests.mobile.disk.android.diskui.DiskUITest
import ru.yandex.autotests.mobile.disk.android.download.DownloadTest
import ru.yandex.autotests.mobile.disk.android.download.DownloadTest2
import ru.yandex.autotests.mobile.disk.android.favoritesalbum.FavoritesAlbumTest
import ru.yandex.autotests.mobile.disk.android.favoritesalbum.FavoritesAlbumTest2
import ru.yandex.autotests.mobile.disk.android.favoritesalbum.FavoritesAlbumTest3
import ru.yandex.autotests.mobile.disk.android.favoritesalbum.FavoritesAlbumTest4
import ru.yandex.autotests.mobile.disk.android.feed.*
import ru.yandex.autotests.mobile.disk.android.feedback.FeedbackFormTest
import ru.yandex.autotests.mobile.disk.android.files.EmptyFilesTest
import ru.yandex.autotests.mobile.disk.android.files.FilesDisplayModeTest
import ru.yandex.autotests.mobile.disk.android.files.FilesTest
import ru.yandex.autotests.mobile.disk.android.filesmoving.*
import ru.yandex.autotests.mobile.disk.android.filesrenaming.*
import ru.yandex.autotests.mobile.disk.android.foldercreation.FolderCreationTest
import ru.yandex.autotests.mobile.disk.android.foldercreation.FolderCreationTest2
import ru.yandex.autotests.mobile.disk.android.geoalbums.GeoAlbumsNewUserTest
import ru.yandex.autotests.mobile.disk.android.geoalbums.GeoAlbumsTest
import ru.yandex.autotests.mobile.disk.android.geoalbums.GeoAlbumsTest2
import ru.yandex.autotests.mobile.disk.android.geoalbums.GeoAlbumsTest3
import ru.yandex.autotests.mobile.disk.android.groupoperation.GroupOperationTest
import ru.yandex.autotests.mobile.disk.android.groupoperation.GroupOperationTest2
import ru.yandex.autotests.mobile.disk.android.groupoperation.GroupOperationTest3
import ru.yandex.autotests.mobile.disk.android.groupoperation.GroupOperationTest4
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
import ru.yandex.autotests.mobile.disk.android.login.LoginTest
import ru.yandex.autotests.mobile.disk.android.navigation.*
import ru.yandex.autotests.mobile.disk.android.notes.NotesTest
import ru.yandex.autotests.mobile.disk.android.notes.NotesTest2
import ru.yandex.autotests.mobile.disk.android.notes.SearchInNotesTest
import ru.yandex.autotests.mobile.disk.android.notifications.NotificationsTest
import ru.yandex.autotests.mobile.disk.android.offlinefiles.*
import ru.yandex.autotests.mobile.disk.android.offlinefolders.*
import ru.yandex.autotests.mobile.disk.android.personalalbums.*
import ru.yandex.autotests.mobile.disk.android.photos.*
import ru.yandex.autotests.mobile.disk.android.photoviewer.*
import ru.yandex.autotests.mobile.disk.android.pincode.PinCodeTest
import ru.yandex.autotests.mobile.disk.android.publicpage.PublicTest
import ru.yandex.autotests.mobile.disk.android.runtimepermissions.RuntimePermissionsTest
import ru.yandex.autotests.mobile.disk.android.search.SearchFileTest
import ru.yandex.autotests.mobile.disk.android.search.SearchFileTest2
import ru.yandex.autotests.mobile.disk.android.settings.SettingsTest
import ru.yandex.autotests.mobile.disk.android.sharedfolder.SharedFolderTest
import ru.yandex.autotests.mobile.disk.android.sharedfolder.SharedFolderTest2
import ru.yandex.autotests.mobile.disk.android.sharedfolder.SharedFolderTest3
import ru.yandex.autotests.mobile.disk.android.shareoriginal.ShareOriginalFileTest
import ru.yandex.autotests.mobile.disk.android.sort.SortTest
import ru.yandex.autotests.mobile.disk.android.telemost.TelemostTest
import ru.yandex.autotests.mobile.disk.android.theme.DarkThemeTests
import ru.yandex.autotests.mobile.disk.android.trash.TrashTest
import ru.yandex.autotests.mobile.disk.android.upload.UploadOverFabTest
import ru.yandex.autotests.mobile.disk.android.videoplayer.VideoPlayerTests
import ru.yandex.autotests.mobile.disk.android.videoplayer.VideoPlayerTests2
import ru.yandex.autotests.mobile.disk.android.videoplayer.VideoPlayerTests3

@RunWith(Categories::class)
@IncludeCategory(Acceptance::class)
@SuiteClasses(
    AviaryTest::class,
    CacheTest::class,
    CacheTest2::class,
    CopyFilesTest::class,
    CopyFilesTest2::class,
    CopyFilesTest3::class,
    CopyLinkTest::class,
    CopyLinkTest2::class,
    DefaultPartitionTest::class,
    DeleteFilesAndFoldersTest::class,
    DeleteFilesAndFoldersTest2::class,
    DeleteFilesAndFoldersTest3::class,
    DeleteLinkTest::class,
    DeleteLinkTest2::class,
    DeleteLinkTest3::class,
    DiskUITest::class,
    DownloadTest::class,
    DownloadTest2::class,
    FeedTest::class,
    FeedTest2::class,
    FeedTest3::class,
    FeedTest4::class,
    FeedTest5::class,
    FeedTest6::class,
    FeedTest7::class,
    PhotoSelectionBlockTest::class,
    FeedbackFormTest::class,
    FilesDisplayModeTest::class,
    FilesTest::class,
    FolderCreationTest::class,
    FolderCreationTest2::class,
    GeoAlbumsTest::class,
    GeoAlbumsTest2::class,
    GeoAlbumsTest3::class,
    GeoAlbumsNewUserTest::class,
    GroupOperationTest::class,
    GroupOperationTest2::class,
    GroupOperationTest3::class,
    GroupOperationTest4::class,
    LoginTest::class,
    MoveFileAndFolderTest::class,
    MoveFileAndFolderTest2::class,
    MoveFileAndFolderTest3::class,
    MoveFileAndFolderTest4::class,
    MoveFileAndFolderTest5::class,
    MoveFileAndFolderTest6::class,
    NavigationTest::class,
    NavigationTest2::class,
    NavigationTest3::class,
    NavigationTest4::class,
    NotesTest::class,
    NotesTest2::class,
    OfflineFilesTest::class,
    OfflineFilesTest2::class,
    OfflineFilesTest3::class,
    OfflineFilesTest4::class,
    OfflineFilesTest5::class,
    OfflineFoldersTest::class,
    OfflineFoldersTest2::class,
    OfflineFoldersTest3::class,
    OfflineFoldersTest4::class,
    OfflineFoldersTest5::class,
    OfflineFoldersTest6::class,
    OfflineFoldersTest7::class,
    PhotoViewerTest::class,
    PhotoViewerTest2::class,
    PhotoViewerTest3::class,
    PhotoViewerTest4::class,
    PhotoViewerTest5::class,
    PhotoViewerTest6::class,
    PinCodeTest::class,
    PhotosOperationsTest::class,
    PhotosOperationsTest2::class,
    PhotoOperationsTest2::class,
    PhotoOperationsTest::class,
    PhotosDeleteOperationsTest::class,
    PhotosDeleteOperationsTest2::class,
    PhotoDeleteOperationsTest3::class,
    ProfileTest::class,
    RenameFileAndFolderTest::class,
    RenameFileAndFolderTest2::class,
    RenameFileAndFolderTest3::class,
    RenameFileAndFolderTest4::class,
    RenameFileAndFolderTest5::class,
    RenameFileAndFolderTest6::class,
    RenamePhotoTest::class,
    RuntimePermissionsTest::class,
    PhotoViewerPermissionsTest::class,
    SearchFileTest::class,
    SearchFileTest2::class,
    SearchInNotesTest::class,
    SettingsTest::class,
    SharedFolderTest::class,
    SharedFolderTest2::class,
    SharedFolderTest3::class,
    ShareOriginalFileTest::class,
    SortTest::class,
    TrashTest::class,
    UploadOverFabTest::class,
    AddToPersonalAlbumTest::class,
    AddToPersonalAlbumTest2::class,
    CreatePersonalAlbumTests::class,
    CreatePersonalAlbumTests2::class,
    ExcludeFromPersonalAlbumTest::class,
    ExcludeFromPersonalAlbumTest2::class,
    PersonalAlbumsActionsTest::class,
    PersonalAlbumsActionsTest2::class,
    FavoritesAlbumTest::class,
    FavoritesAlbumTest2::class,
    FavoritesAlbumTest3::class,
    FavoritesAlbumTest4::class,
    DarkThemeTests::class,
    NotificationsTest::class,
    ViewerInfoTest::class,
    ViewerTrailerTest::class,
    ViewerTrailerTest2::class,
    TelemostTest::class,
    VideoPlayerTests::class,
    VideoPlayerTests2::class,
    VideoPlayerTests3::class,
    PublicTest::class,
    EmptyFilesTest::class,
    FeedAdsTest::class
)
class AcceptanceEmulatorSuite
