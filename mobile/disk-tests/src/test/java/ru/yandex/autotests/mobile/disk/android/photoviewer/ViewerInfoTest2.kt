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
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.*
import java.util.concurrent.TimeUnit

@Feature("Photo viewer info")
@UserTags("viewerinfo")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class ViewerInfoTest2 : DiskTest {
    companion object {
        @JvmField
        @ClassRule
        var classRuleChain = RulesFactory.createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    override lateinit var locator: StepsLocator

    @Test
    @TmsLink("6942")
    @Category(BusinessLogic::class)
    @PushMediaToDevice(filePaths = [JPG_1])
    @DeleteMediaOnDevice(files = [JPG_1])
    @DeleteFiles(files = [PHOTOUNLUM_ROOT + JPG_1_AUTOUPLOADED, CAMERA_UPLOADS])
    fun shouldOpenPhotosFromFeedUnlimitedFile() {
        onNavigationPage {
            switchToWifi()
            enableUnlimitedPhotoAutoupload()
            wait(delay = 10, TimeUnit.SECONDS)
            openFeed {
                updateFeed()
                openFirstImage {
                    openPhotoInformation()
                    shouldPhotoLocatedIntoPhotounlim()
                    shouldClickLocationInfo()
                }
                shouldBeOnPhotos()
                onPhotos.shouldDisplayOnce(JPG_1)
            }
        }
    }
}
