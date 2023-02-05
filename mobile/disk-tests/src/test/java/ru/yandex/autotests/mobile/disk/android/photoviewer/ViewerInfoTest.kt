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
import ru.yandex.autotests.mobile.disk.android.DiskTest
import ru.yandex.autotests.mobile.disk.android.StepsLocator
import ru.yandex.autotests.mobile.disk.android.blocks.MediaItem
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.Albums
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders.DCIM_FULL_PATH
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.*
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums.NEW_ALBUM_NAME

@Feature("Photo viewer info")
@UserTags("viewer_info_read_only")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class ViewerInfoTest : DiskTest {
    companion object {
        @JvmField
        @ClassRule
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    override lateinit var locator: StepsLocator

    @Test
    @TmsLink("4139")
    @PushMediaToDevice(filePaths = [JPG_2])
    @DeleteMediaOnDevice(files = [DCIM_FULL_PATH + JPG_2])
    @Category(BusinessLogic::class)
    fun shouldShowLocalPhotoInfo() {
        onNavigationPage {
            openPhotos {
                shouldDisplayItemWithStatus(JPG_2, MediaItem.Status.LOCAL)
                clickItem(JPG_2) {
                    checkFileInfo(JPG_2, listOf(JPG_2_PHOTO_PROPERTIES, JPG_2_PHOTO_PROPERTIES_LAND), JPGS_LOCAL_PATH)
                }
            }
        }
    }

    @Test
    @TmsLink("4154")
    @Category(BusinessLogic::class)
    fun shouldShowCloudyPhotosliceFileInfo() {
        onNavigationPage {
            openAlbums {
                clickAlbum(NEW_ALBUM_NAME) {
                    shouldDisplayItemWithStatus(JPG_1, MediaItem.Status.CLOUDY)
                    clickItem(JPG_1) {
                        checkFileInfo(JPG_1, listOf(JPG_1_PHOTO_SIZE), JPGS_REMOTE_PATH, null)
                    }
                }
            }
        }
    }

    @Test
    @TmsLink("4153")
    @PushMediaToDevice(filePaths = [JPG_1])
    @DeleteMediaOnDevice(files = [DCIM_FULL_PATH + JPG_1])
    @Category(BusinessLogic::class)
    fun shouldShowCloudyLocalPhotosliceFileInfo() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(JPG_1, MediaItem.Status.CLOUDY_LOCAL)
                shouldDisplayItemWithStatus(JPG_1, MediaItem.Status.CLOUDY_LOCAL)
                clickItem(JPG_1) {
                    checkFileInfo(JPG_1, listOf(JPG_1_PHOTO_PROPERTIES, JPG_1_PHOTO_PROPERTIES_LAND), JPGS_LOCAL_PATH, NEW_ALBUM_NAME)
                }
            }
        }
    }

    @Test
    @TmsLink("6060")
    @Category(BusinessLogic::class)
    fun shouldShowCloudyFileInfo() {
        onNavigationPage {
            openFiles {
                shouldOpenImageIntoViewer(JPG_1) {
                    checkFileInfo(JPG_1, listOf(JPG_1_PHOTO_PROPERTIES, JPG_1_PHOTO_PROPERTIES_LAND), JPGS_REMOTE_PATH, NEW_ALBUM_NAME)
                }
            }
        }
    }

    @Test
    @TmsLink("6186")
    @Category(BusinessLogic::class)
    fun shouldShowFeedFileInfo() {
        onNavigationPage {
            openFeed {
                openFirstImage {
                    checkFileInfo(JPG_1, listOf(JPG_1_PHOTO_PROPERTIES, JPG_1_PHOTO_PROPERTIES_LAND), JPGS_REMOTE_PATH, NEW_ALBUM_NAME)
                }
            }
        }
    }

    @Test
    @TmsLink("6089")
    @Category(BusinessLogic::class)
    fun shouldNavigateToAlbumFromViewer() {
        onNavigationPage {
            openFiles {
                shouldOpenImageIntoViewer(JPG_1) {
                    openPhotoInformation()
                    shouldAlbumBeDisplayed(Albums.SORT)
                    openAlbum(Albums.SORT) {
                        shouldBeOnPageWithTitle(Albums.SORT)
                        shouldDisplayOnce(JPG_1)
                    }
                }
            }
        }
    }
}
