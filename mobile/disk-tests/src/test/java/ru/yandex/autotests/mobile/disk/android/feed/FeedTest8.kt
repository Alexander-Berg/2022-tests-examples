package ru.yandex.autotests.mobile.disk.android.feed

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.EasyViewerTrailer
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders.UPLOAD_FOLDER

@Feature("Feed")
@UserTags("feed_read_only")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedTest8 : FeedTestRunner(), DiskTest {
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
    @TmsLink("5427")
    @Category(BusinessLogic::class)
    fun shouldUseWowGridByDefaultInContentBlock() {
        with(onFeed) {
            updateFeed()
            shouldSeePhotoSelectionBlock()
            expandPhotoSelectionBlock()
        }
        onContentBlock.shouldWowGridBeOpened()
    }

    @Test
    @TmsLink("5441")
    @Category(BusinessLogic::class)
    fun shouldHideActionBarDuringWowGridScroll() {
        with(onFeed) {
            updateFeed()
            shouldSeePhotoSelectionBlock()
            expandPhotoSelectionBlock()
        }
        with(onContentBlock) {
            shouldWowGridBeOpened()
            shouldSeeActionBar()
            swipeDownToUpNTimes(1)
            shouldNotSeeActionBar()
            shouldNotSeeNavigationButton()
            rotate(ScreenOrientation.LANDSCAPE)
            shouldNotSeeActionBar()
            shouldNotSeeNavigationButton()
            swipeDownToUpNTimes(10)
            shouldSeeNavigationButton()
            shouldNotSeeActionBar()
        }
    }

    @Test
    @TmsLink("5442")
    @Category(BusinessLogic::class)
    fun shouldShowActionBarDuringWowGridScroll() {
        with(onFeed) {
            updateFeed()
            shouldSeePhotoSelectionBlock()
            expandPhotoSelectionBlock()
        }
        with(onContentBlock) {
            shouldWowGridBeOpened()
            swipeDownToUpNTimes(8)
            shouldNotSeeActionBar()
            swipeUpToDownNTimes(1)
            shouldSeeActionBar()
            rotate(ScreenOrientation.LANDSCAPE)
            shouldSeeActionBar()
            swipeUpToDownNTimes(1)
            shouldSeeActionBar()
        }
    }

    @Test
    @TmsLink("7932")
    @Category(BusinessLogic::class)
    @EasyViewerTrailer
    fun shouldNavigateToPhotoSelectionBlockFromViewer() {
        onNavigationPage {
            openFiles {
                openFolder(UPLOAD_FOLDER)
                shouldOpenImageIntoViewer("9.jpg") {
                    longSwipeRightToLeft()
                    shouldOpenViewerTrailerBeDisplayed()
                    openSuggestedPhotoSelectionBlock {
                        shouldBeContentBlockOpened()
                    }
                }
            }
        }
    }

    @Test
    @TmsLink("5579")
    @Category(Regression::class)
    fun shouldBeSwitchedContentBlockGridFromWowToBig() {
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.shouldGridChangedFromWowToBig()
    }

    @Test
    @TmsLink("5580")
    @Category(Regression::class)
    fun shouldBeSwitchedContentBlockGridFromBigToWow() {
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.shouldGridBeChangeable()
    }

    @Test
    @TmsLink("5581")
    @Category(Regression::class)
    fun shouldBeSwitchedGridContentBlockFromWowToBigInMenu() {
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.openMenu()
        onContentBlock.enableBigGridInMenu()
        onContentBlock.shouldSimpleGridBeOpened()
    }

    @Test
    @TmsLink("5582")
    @Category(Regression::class)
    fun shouldBeSwitchedGridContentBlockFromBigToWowInMenu() {
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.shouldWowGridBeOpened()
        onContentBlock.openMenu()
        onContentBlock.enableBigGridInMenu()
        onContentBlock.shouldSimpleGridBeOpened()
        onContentBlock.openMenu()
        onContentBlock.enableWowGridInMenu()
        onContentBlock.shouldWowGridBeOpened()
    }

    @Test
    @TmsLink("5597")
    @Category(Regression::class)
    fun shouldSeeOptionsInPhotoSelectionBlockAfterOrientationChanged() {
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.expandPhotoSelectionBlock()
        onContentBlock.openMenu()
        onContentBlock.rotate(ScreenOrientation.LANDSCAPE)
        onGroupMode.shouldActionBePresented(FileActions.CHANGE_LAYOUT)
        onGroupMode.shouldActionBePresented(FileActions.SHARE_ALBUM)
        onGroupMode.shouldActionBePresented(FileActions.SELECT_ONLY)
        onGroupMode.shouldActionBePresented(FileActions.HIDE_FROM_FEED)
    }

    @Test
    @TmsLink("5422")
    @Category(Regression::class)
    fun shouldSeePhotoSelectionBlock() {
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
    }

    @Test
    @TmsLink("7816")
    @Category(Regression::class)
    fun shouldSeeOptionsOfPhotoSelectionBlockAfterOrientationChangedInFeed() {
        onBasePage.openFeed()
        onFeed.shouldSeePhotoSelectionBlock()
        onFeed.openPhotoSelectionBlockMenu()
        onFeed.rotate(ScreenOrientation.LANDSCAPE)
        onFeed.rotate(ScreenOrientation.PORTRAIT)
        onGroupMode.shouldActionBePresented(FileActions.HIDE_FROM_FEED)
        onGroupMode.shouldActionBePresented(FileActions.SHARE_ALBUM)
    }
}
