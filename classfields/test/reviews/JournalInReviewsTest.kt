package ru.auto.ara.test.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher
import ru.auto.ara.core.dispatchers.magazine.GetMagazineSnippetsDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsListingDispatcher
import ru.auto.ara.core.interaction.transporttab.MainInteractions
import ru.auto.ara.core.robot.reviews.performReviewFeedTab
import ru.auto.ara.core.robot.reviews.performReviewsFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.ui.activity.SimpleSecondLevelActivity
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import ru.auto.feature.reviews.search.ui.viewmodel.ReviewFeedContext
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class JournalInReviewsTest {

    private val webServerRule = WebServerRule {
        delegateDispatcher(GetReviewsListingDispatcher.generic())
    }
    private val simpleSecondLevelActivityRule = lazyActivityScenarioRule<SimpleSecondLevelActivity>()
    private val mainActivityRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        simpleSecondLevelActivityRule,
        mainActivityRule
    )

    @Test
    fun shouldOpenJournalOnTestsSnippetTap() {
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.empty(),
                GetMagazineSnippetsDispatcher.tests()
            )
        }
        tapOnJournalAndCheckWebView(
            articleName = "tripletestbmwmercedesaudi",
            utmCampaign = "reviews_test-drive"
        )
    }

    @Test
    fun shouldOpenJournalWithExplainOnSnippetTap() {
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.empty(),
                GetMagazineSnippetsDispatcher.explain()
            )
        }
        tapOnJournalAndCheckWebView(
            articleName = "bmwxold",
            utmCampaign = "reviews_explain"
        )
    }

    private fun tapOnJournalAndCheckWebView(articleName: String, utmCampaign: String) {
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true, mark = "BMW", model = "3 серия"))
        watchWebView {
            performReviewsFeed {
                collapseToolbar()
                scrollToJournalItem(3)
                clickOnJournalItem(3)
            }
        }.checkResult {
            checkTitleMatches(getResourceString(R.string.journal))
            checkUrlMatches(
                "https://mag.auto.ru/article/$articleName/" +
                    "?from=autoru_app" +
                    "&utm_source=auto-ru" +
                    "&utm_medium=cpm" +
                    "&utm_content=$articleName" +
                    "&utm_campaign=$utmCampaign"
            )
        }
    }

    @Test
    fun shouldShowNoSnippetsWhenNoMarkModelSelected() {
        val magazineWatcher = RequestWatcher()
        webServerRule.routing { delegateDispatcher(GetMagazineSnippetsDispatcher.empty(magazineWatcher)) }
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true))

        performReviewsFeed {
            collapseToolbar()
            waitForFirstSnippet()
        }.checkResult {
            checkNoJournalSnippetAtPosition(1..20)
        }
        magazineWatcher.checkRequestWasNotCalled()
    }

    @Test
    fun shouldShowNoSnippetsWhenOnlyMarkSelected() {
        val magazineWatcher = RequestWatcher()
        webServerRule.routing { delegateDispatcher(GetMagazineSnippetsDispatcher.empty(magazineWatcher)) }
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true, mark = "BMW"))

        performReviewsFeed {
            collapseToolbar()
            waitForFirstSnippet()
        }.checkResult {
            checkNoJournalSnippetAtPosition(1..20)
        }
        magazineWatcher.checkRequestWasNotCalled()
    }

    @Test
    fun shouldShow2SnippetsIfMarkModelGenSelectedAndWeHaveBothTestsAndExplains() {
        val magazineWatcher = RequestWatcher()
        val magazineWatcher2 = RequestWatcher()
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.explain(magazineWatcher),
                GetMagazineSnippetsDispatcher.tests(magazineWatcher2)
            )
        }
        launchFeedFragment(
            ReviewFeedContext(
                asFirstLevel = true,
                mark = "BMW",
                model = "3 серия",
                generation = "2015 - 2020 VI (F3x) Рестайлинг"
            )
        )

        performReviewsFeed {
            collapseToolbar()
            waitForFirstSnippet()
        }.checkResult {
            checkJournalSnippetAtPosition(3, "Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE")
        }
        performReviewsFeed {
            collapseToolbar()
            scrollToJournalItem(6)
        }.checkResult {
            checkJournalSnippetAtPosition(6, "Хочу купить старый Х5, поможете?")
        }

        val commonParams = listOf("mark" to "BMW", "model" to "3ER", "page_size" to "1", "super_gen_id" to "20548423")
        magazineWatcher.checkQueryParameters(commonParams + ("category" to "Разбор"))
        magazineWatcher2.checkQueryParameters(commonParams + ("category" to "Тесты"))
    }

    @Test
    fun shouldShow2SnippetsIfMarkModelSelectedAndWeHaveBothTestsAndExplains() {
        val magazineWatcher = RequestWatcher()
        val magazineWatcher2 = RequestWatcher()
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.explain(magazineWatcher),
                GetMagazineSnippetsDispatcher.tests(magazineWatcher2)
            )
        }
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true, mark = "BMW", model = "3 серия"))

        performReviewsFeed {
            collapseToolbar()
            scrollToJournalItem(3)
        }.checkResult {
            checkJournalSnippetAtPosition(3, "Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE")
        }
        performReviewsFeed {
            collapseToolbar()
            scrollToJournalItem(6)
        }.checkResult {
            checkJournalSnippetAtPosition(6, "Хочу купить старый Х5, поможете?")
        }
        val commonParams = listOf("mark" to "BMW", "model" to "3ER", "page_size" to "1")
        magazineWatcher.checkQueryParameters(commonParams + ("category" to "Разбор"))
        magazineWatcher2.checkQueryParameters(commonParams + ("category" to "Тесты"))
    }

    @Test
    fun shouldShow2SnippetsWhenNoFeedIsLoaded() {
        webServerRule.routing {
            delegateDispatchers(
                GetReviewsListingDispatcher.empty(),
                GetMagazineSnippetsDispatcher.explain(),
                GetMagazineSnippetsDispatcher.tests()
            )
        }
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true, mark = "BMW", model = "3 серия"))

        performReviewsFeed {
            collapseToolbar()
        }.checkResult {
            checkJournalSnippetAtPosition(1, "Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE")
            checkJournalSnippetAtPosition(2, "Хочу купить старый Х5, поможете?")
        }
    }

    @Test
    fun shouldShowNoSnippetsOnSecondPage() {
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.explain(),
                GetMagazineSnippetsDispatcher.tests()
            )
        }
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true, mark = "BMW"))

        val magazineWatcher = RequestWatcher()
        val magazineWatcher2 = RequestWatcher()
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.explain(magazineWatcher),
                GetMagazineSnippetsDispatcher.tests(magazineWatcher2)
            )
        }

        performReviewsFeed {
            collapseToolbar()
            scrollToEnd()
            waitSecondPageLoaded()
        }.checkResult {
            checkNoJournalSnippetAtPosition(20..38)
        }

        magazineWatcher.checkRequestWasNotCalled()
        magazineWatcher2.checkRequestWasNotCalled()
    }

    @Test
    fun shouldShow1SnippetIfMarkSelectedAndWeHaveOnlyTests() {
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.empty(),
                GetMagazineSnippetsDispatcher.tests()
            )
        }
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true, mark = "BMW", model = "3 серия"))

        performReviewsFeed {
            collapseToolbar()
            waitForFirstSnippet()
        }.checkResult {
            checkJournalSnippetAtPosition(3, "Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE")
            checkNoJournalSnippetAtPosition(4..6)
        }
    }

    @Test
    fun shouldShow1SnippetIfMarkSelectedAndWeHaveOnlyExplains() {
        webServerRule.routing {
            delegateDispatchers(
                GetMagazineSnippetsDispatcher.empty(),
                GetMagazineSnippetsDispatcher.explain()
            )
        }
        launchFeedFragment(ReviewFeedContext(asFirstLevel = true, mark = "BMW", model = "3 серия"))

        performReviewsFeed {
            collapseToolbar()
            waitForFirstSnippet()
        }.checkResult {
            checkJournalSnippetAtPosition(3, "Хочу купить старый Х5, поможете?")
            checkNoJournalSnippetAtPosition(4..6)
        }
    }

    private fun launchFeedFragment(reviewFeedContext: ReviewFeedContext) {
        mainActivityRule.launchActivity()
        performMain { openMainTab(R.string.reviews) }
        MainInteractions.collapseMainToolbar()
        performReviewFeedTab {
            clickOnMMGItem()
            reviewFeedContext.mark?.let { clickOnMarkField(it) }
            reviewFeedContext.model?.let { clickOnModelField(it) }
            webServerRule.routing { delegateDispatcher(BreadcrumbsSuggestDispatcher.error()) }
            reviewFeedContext.generation?.let { clickOnGenerationField(it) } ?: clickOnShowButton()
        }
        waitSomething(1, TimeUnit.SECONDS) // else toolbar will collapse not fully
    }
}
