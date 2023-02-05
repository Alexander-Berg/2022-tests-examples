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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.CAMERA_UPLOADS
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.ONE_HOUR_VIDEO
import java.time.Duration
import java.util.concurrent.TimeUnit

@Feature("Video player")
@UserTags("video_player_read_only")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class VideoPlayerTests2 : DiskTest {
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
    @TmsLink("4096")
    @Category(BusinessLogic::class)
    fun shouldVideoControlsHideAndDisplay() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 10, timeUnit = TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    shouldNotSeePreviewControls()
                    shouldVideoControllersNotBeDisplayed()
                    tapOnVideoPlayer()
                    closeBannerIfPresented()
                    shouldSeePreviewControls(true)
                    shouldVideoControllersBeDisplayed()
                }
            }
        }
    }

    @Test
    @TmsLink("4098")
    @Category(BusinessLogic::class)
    fun shouldCloseVideoPlayerByHardBack() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldClickPlayVideoButton()
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    shouldVideoPlayerTimeBeInRange(from = 1, to = 60, timeUnit = TimeUnit.SECONDS)
                    pressHardBack()
                }
                shouldBeOnPhotos()
            }
        }
    }

    @Test
    @TmsLink("4100")
    @Category(BusinessLogic::class)
    fun shouldVideoControlsPersistOnRotate() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    shouldNotSeePreviewControls()
                    shouldVideoControllersNotBeDisplayed()
                    rotate(ScreenOrientation.LANDSCAPE)
                    shouldNotSeePreviewControls()
                    shouldVideoControllersNotBeDisplayed()
                    tapOnVideoPlayer()
                    shouldSeePreviewControls(true)
                    shouldVideoControllersBeDisplayed()
                    rotate(ScreenOrientation.PORTRAIT)
                    shouldSeePreviewControls(true)
                    shouldVideoControllersBeDisplayed()
                }
            }
        }
    }

    @Test
    @TmsLink("4101")
    @Category(BusinessLogic::class)
    fun shouldVideoStayOnPauseAfterAppResume() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    sendApplicationToBackground()
                    backApplicationFromBackground()
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    shouldVideoControllersBeDisplayed()
                }
            }
        }
    }

    @Test
    @TmsLink("4102")
    @Category(BusinessLogic::class)
    fun shouldVideoStayOnPauseAfterScreenLock() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    lockScreen(Duration.ofSeconds(1L))
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    shouldVideoControllersBeDisplayed()
                }
            }
        }
    }

    @Test
    @TmsLink("4103")
    @Category(BusinessLogic::class)
    fun shouldVideoScrollWithProgressBarPinDrag() {
        onNavigationPage {
            openPhotos {
                sendPushesUntilAppeared(ONE_HOUR_VIDEO)
                clickItem(ONE_HOUR_VIDEO) {
                    shouldSeePreviewControls()
                    shouldClickPlayVideoButton()
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    closeBannerIfPresented()
                    tapOnVideoPlayer()
                    scrollVideoTo(time = "40:00")
                    shouldVideoPlayerTimeBeInRange(from = 30, to = 40, timeUnit = TimeUnit.MINUTES)
                    wait(delay = 5, timeUnit = TimeUnit.SECONDS)
                    tapOnVideoPlayer()
                    scrollVideoTo("20:00")
                    shouldVideoPlayerTimeBeInRange(from = 10, to = 30, timeUnit = TimeUnit.MINUTES)
                }
            }
        }
    }

    @Test
    @TmsLink("4474")
    @Category(BusinessLogic::class)
    fun shouldSwitchToAutoVideoQualityOnCellularConnection() {
        onNavigationPage {
            openPhotos {
                switchToData()
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
}
