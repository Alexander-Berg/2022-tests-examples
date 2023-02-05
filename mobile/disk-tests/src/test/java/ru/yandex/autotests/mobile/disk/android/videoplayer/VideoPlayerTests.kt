package ru.yandex.autotests.mobile.disk.android.videoplayer

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
import ru.yandex.autotests.mobile.disk.android.DiskTest
import ru.yandex.autotests.mobile.disk.android.StepsLocator
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders.DCIM_FULL_PATH
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.ONE_HOUR_VIDEO
import java.util.concurrent.TimeUnit

@Feature("Video player")
@UserTags("videoplayer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class VideoPlayerTests : DiskTest {
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
    @TmsLink("4170")
    @UploadFiles(filePaths = [ONE_HOUR_VIDEO])
    @PushFileToDevice(filePaths = [ONE_HOUR_VIDEO], targetFolder = DCIM_FULL_PATH)
    @DeleteFiles(files = [ONE_HOUR_VIDEO])
    @DeleteMediaOnDevice(files = [DCIM_FULL_PATH + ONE_HOUR_VIDEO])
    @Category(BusinessLogic::class)
    fun shouldOpenAndCloseVideoPlayerCloudLocalFile() {
        prepareCloudLocalVideoFile()
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    shouldVideoPlayerTimeBeInRange(from = 1, to = 60, TimeUnit.SECONDS)
                    pressHardBack()
                }
                shouldBeOnPhotos()
            }
        }
    }

    @Test
    @TmsLink("4174")
    @UploadFiles(filePaths = [ONE_HOUR_VIDEO])
    @PushFileToDevice(filePaths = [ONE_HOUR_VIDEO], targetFolder = DCIM_FULL_PATH)
    @DeleteFiles(files = [ONE_HOUR_VIDEO])
    @DeleteMediaOnDevice(files = [DCIM_FULL_PATH + ONE_HOUR_VIDEO])
    @Category(BusinessLogic::class)
    fun shouldPerformPlayAndPauseOnCloudLocalVideoFile() {
        prepareCloudLocalVideoFile()
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    shouldClickPauseVideoButton()
                    shouldVideoPlayerTimeBeInRange(from = 5, to = 30, TimeUnit.SECONDS)
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    shouldVideoPlayerTimeBeInRange(from = 5, to = 30, TimeUnit.SECONDS)
                }
            }
        }
    }

    @Test
    @TmsLink("4175")
    @UploadFiles(filePaths = [ONE_HOUR_VIDEO])
    @PushFileToDevice(filePaths = [ONE_HOUR_VIDEO], targetFolder = DCIM_FULL_PATH)
    @DeleteFiles(files = [ONE_HOUR_VIDEO])
    @DeleteMediaOnDevice(files = [DCIM_FULL_PATH + ONE_HOUR_VIDEO])
    @Category(BusinessLogic::class)
    fun shouldVideoControlsPersistOnRotateCloudLocalVideoFile() {
        prepareCloudLocalVideoFile()
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    shouldNotSeePreviewControls()
                    shouldVideoControllersNotBeDisplayed()
                    rotate(ScreenOrientation.LANDSCAPE)
                    shouldNotSeePreviewControls()
                    shouldVideoControllersNotBeDisplayed()
                    tapOnVideoPlayer()
                    shouldSeePreviewControls(playingVideo = true)
                    shouldVideoControllersBeDisplayed()
                    rotate(ScreenOrientation.PORTRAIT)
                    shouldSeePreviewControls(playingVideo = true)
                    shouldVideoControllersBeDisplayed()
                }
            }
        }
    }

    @Test
    @TmsLink("4176")
    @UploadFiles(filePaths = [ONE_HOUR_VIDEO])
    @PushFileToDevice(filePaths = [ONE_HOUR_VIDEO], targetFolder = DCIM_FULL_PATH)
    @DeleteFiles(files = [ONE_HOUR_VIDEO])
    @DeleteMediaOnDevice(files = [DCIM_FULL_PATH + ONE_HOUR_VIDEO])
    @Category(BusinessLogic::class)
    fun shouldCloudLocalVideoScrollWithProgressBarPinDrag() {
        prepareCloudLocalVideoFile()
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    scrollVideoTo(time = "40:00")
                    shouldVideoPlayerTimeBeInRange(from = 30, to = 40, TimeUnit.MINUTES)
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    scrollVideoTo(time = "20:00")
                    shouldVideoPlayerTimeBeInRange(from = 10, to = 30, TimeUnit.MINUTES)
                }
            }
        }
    }

    @Test
    @TmsLink("4177")
    @UploadFiles(filePaths = [ONE_HOUR_VIDEO])
    @PushFileToDevice(filePaths = [ONE_HOUR_VIDEO], targetFolder = DCIM_FULL_PATH)
    @DeleteFiles(files = [ONE_HOUR_VIDEO])
    @DeleteMediaOnDevice(files = [DCIM_FULL_PATH + ONE_HOUR_VIDEO])
    @Category(BusinessLogic::class)
    fun shouldCloudLocalVideoScrollWithProgressBarTap() {
        prepareCloudLocalVideoFile()
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    scrollVideoTo(time = "40:00")
                    shouldVideoPlayerTimeBeInRange(from = 30, to = 45, TimeUnit.MINUTES)
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    tapOnVideoProgressBar(time = "50:00")
                    shouldVideoPlayerTimeBeInRange(from = 45, to = 55, TimeUnit.MINUTES)
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    tapOnVideoProgressBar(time = "20:00")
                    shouldVideoPlayerTimeBeInRange(from = 10, to = 30, TimeUnit.MINUTES)
                }
            }
        }
    }

    private fun prepareCloudLocalVideoFile() {
        onNavigationPage {
            enableVideoAutoupload()
            wait(delay = 10, TimeUnit.SECONDS)
            openPhotos {
                shouldAutouploadStatusBeNotDisplayed()
            }
        }
    }
}
