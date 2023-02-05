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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.*
import java.util.concurrent.TimeUnit

@Feature("Video player")
@UserTags("video_player_read_only")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class VideoPlayerTests3 : DiskTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    override lateinit var locator: StepsLocator

    @Test
    @TmsLink("4104")
    @Category(BusinessLogic::class)
    fun shouldVideoScrollWithProgressBarTap() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    scrollVideoTo("40:00")
                    shouldVideoPlayerTimeBeInRange(from = 30, to = 45, TimeUnit.MINUTES)
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    tapOnVideoProgressBar("50:00")
                    shouldVideoPlayerTimeBeInRange(from = 45, to = 55, TimeUnit.MINUTES)
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    tapOnVideoProgressBar("20:00")
                    shouldVideoPlayerTimeBeInRange(from = 10, to = 30, TimeUnit.MINUTES)
                }
            }
        }
    }

    @Test
    @TmsLink("4094")
    @Category(BusinessLogic::class)
    fun shouldOpenVideoFromPhotoSection() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(FILE_FOR_VIEWING_1)
                clickItem(FILE_FOR_VIEWING_1) {
                    swipeLeftToRight()
                    swipeLeftToRight()
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    shouldVideoControllersBeDisplayed()
                }
            }
        }
    }

    @Test
    @TmsLink("4095")
    @Category(BusinessLogic::class)
    fun shouldPerformPlayAndPause() {
        onNavigationPage {
            openFiles {
                openFolder(TARGET_FOLDER)
                shouldOpenVideoIntoViewer(ONE_HOUR_VIDEO) {
                    shouldClickPlayVideoButton()
                    closeBannerIfPresented()
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    shouldVideoControllersBeDisplayed()
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
    @TmsLink("4108")
    @Category(BusinessLogic::class)
    fun shouldVideoQualityBeAutoOnWiFi() {
        onNavigationPage {
            switchToWifi()
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    shouldClickPauseVideoButton()
                    openPhotoInformation()
                    shouldVideoQualityBeAuto()
                }
            }
        }
    }

    @Test
    @TmsLink("4109")
    @Category(BusinessLogic::class)
    fun shouldVideoQualityChange() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    scrollVideoTo("20:00")
                    shouldVideoPlayerTimeBeInRange(from = 10, to = 25, TimeUnit.MINUTES)
                    shouldClickPauseVideoButton()
                    openPhotoInformation()
                    shouldVideoQualityBeAuto()
                    switchVideoQualityTo240()
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    shouldClickPauseVideoButton()
                    shouldVideoPlayerTimeBeInRange(from = 10, to = 25, TimeUnit.MINUTES)
                    openPhotoInformation()
                    shouldVideoQualityBe240()
                    switchVideoQualityTo720()
                    wait(delay = 5, TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    shouldClickPauseVideoButton()
                    shouldVideoPlayerTimeBeInRange(from = 10, to = 25, TimeUnit.MINUTES)
                    openPhotoInformation()
                    shouldVideoQualityBe720()
                }
            }
        }
    }

    @Test
    @TmsLink("6934")
    @Category(Regression::class)
    fun shouldOpenInfoPanelWithSwipeInLandscape() {
        onNavigationPage {
            openFiles {
                openFolder(UPLOAD_FOLDER)
                shouldOpenVideoIntoViewer(VIDEO_FOR_VIEWING) {
                    rotate(ScreenOrientation.LANDSCAPE)
                    wait(delay = 3, TimeUnit.SECONDS)
                    shouldSeePreviewControls(playingVideo = true)
                    swipeDownToUpNTimes(1)
                    wait(delay = 2, TimeUnit.SECONDS)
                    shouldPhotoHasName(VIDEO_FOR_VIEWING)
                    shouldPhotoHasClickablePath(UPLOAD_FOLDER)
                }
            }
        }
    }

    @Test
    @TmsLink("6117")
    @Category(Regression::class)
    fun shouldCloseVideoPlayerWithHardwareBackButton() {
        onNavigationPage {
            openFiles {
                openFolder(TARGET_FOLDER)
                shouldOpenVideoIntoViewer(ONE_HOUR_VIDEO) {
                    wait(delay = 3, TimeUnit.SECONDS)
                    shouldSeePreviewControls(playingVideo = true)
                    shouldClickPlayVideoButton()
                    wait(delay = 5, TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    pressHardBack()
                }
                shouldFolderBeOpened(TARGET_FOLDER)
            }
        }
    }

    @Test
    @TmsLink("6116")
    @Category(Regression::class)
    fun shouldCloseVideoPlayerWithActionbarBackButton() {
        onNavigationPage {
            openFiles {
                openFolder(TARGET_FOLDER)
                shouldOpenVideoIntoViewer(ONE_HOUR_VIDEO) {
                    wait(delay = 3, TimeUnit.SECONDS)
                    shouldSeePreviewControls(playingVideo = true)
                    wait(delay = 2, TimeUnit.SECONDS)
                    shouldClickPlayVideoButton()
                    shouldSeeVideoPreviewControls(playingVideo = true)
                    tapOnVideoPlayer()
                    closePreview()
                }
                shouldFolderBeOpened(TARGET_FOLDER)
            }
        }
    }
}
