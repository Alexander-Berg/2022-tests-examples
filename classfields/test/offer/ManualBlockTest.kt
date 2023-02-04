package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 17.11.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ManualBlockTest {

    private var activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun shouldShowManualBlockWithSeveralArticles() {
        configureWebServer {
            registerOffer()
            registerBlogPosts("manualTest/blogPostsSeveralItems.json")
        }
        createImageOnExternalDir("firstArticle", rColor = 0, gColor = 255, bColor = 0)
        createImageOnExternalDir("secondArticle", rColor = 0, gColor = 0, bColor = 255)
        createImageOnExternalDir("thirdArticle", rColor = 255, gColor = 0, bColor = 0)

        activityTestRule.launchActivity()
        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsManualBlock() }
            isManualBlockMatches("ManualBlockTest/shouldShowManualBlockWithSeveralArticlesStart")
            onView(lookup.matchesManualArticle("Покупаем квартиру")).tapOn()
        }

        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals(
                    "https://realty.yandex.ru/journal/post/pokupaem/" +
                        "?utm_source=app_android_offer&only-content=true"
                )
            }
            pressBack()
        }

        performOnOfferCardScreen {
            scrollManualArticlesToPosition(lookup.matchesShowMoreManualArticlesItem())
            isManualBlockMatches("ManualBlockTest/shouldShowManualBlockWithSeveralArticlesEnd")
            onView(lookup.matchesShowMoreManualArticlesItem()).tapOn()
        }

        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals(
                    "https://realty.yandex.ru/journal/" +
                        "?utm_source=app_android_offer&only-content=true"
                )
            }
        }
    }

    @Test
    fun shouldShowManualBlockWhenNoArticles() {
        configureWebServer {
            registerOffer()
            registerBlogPosts("manualTest/blogPostsNoItems.json")
        }
        activityTestRule.launchActivity()
        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsManualBlock() }
            scrollManualArticlesToPosition(lookup.matchesShowMoreManualArticlesItem())
                .check(matches(isCompletelyDisplayed()))
        }
    }

    @Test
    fun shouldShowManualBlockError() {
        configureWebServer {
            registerOffer()
        }
        activityTestRule.launchActivity()
        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsManualError() }
        }
    }

    private fun DispatcherRegistry.registerBlogPosts(fileName: String) {
        register(
            request {
                path("1.0/blog/posts")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("excerpt/cardWithViewsSellApartment.json")
            }
        )
    }
}
