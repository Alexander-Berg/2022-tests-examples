package com.yandex.mobile.realty.test.savedsearch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SavedSearchesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.*
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 23/06/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedSearchesTest {

    private val activityTestRule = SavedSearchesActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule,
    )

    @Test
    fun shouldRefreshEmptyList() {
        configureWebServer {
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    setBody("""{"response": {}}""")
                }
            )
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/single1.json")
                }
            )
        }

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { isFullscreenEmptyViewShown() }

                performOnFullscreenEmpty {
                    isImageEquals(R.drawable.ill_no_searches)
                    isTitleEquals(R.string.my_searches_list_empty_title)
                    isDescriptionEquals(R.string.my_searches_list_empty_text)
                }

                isChangeButtonHidden()
                isDoneButtonHidden()

                refreshList()

                waitUntil { containsSnippet("saved-search-1") }
                isChangeButtonShown()
                isDoneButtonHidden()
            }
        }
    }

    @Test
    fun shouldRetryError() {
        configureWebServer {
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                error()
            )
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/single1.json")
                }
            )
        }

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { isFullscreenErrorViewShown() }
                isChangeButtonHidden()
                isDoneButtonHidden()

                performOnFullscreenError {
                    isImageEquals(R.drawable.ill_error)
                    isTitleEquals(R.string.error_load_title)
                    isDescriptionEquals(R.string.error_description_retry)
                    isRetryTextEquals(R.string.retry)

                    tapOn(lookup.matchesRetryButton())
                }

                waitUntil { containsSnippet("saved-search-1") }
                isChangeButtonShown()
                isDoneButtonHidden()
            }
        }
    }

    @Test
    fun shouldRefreshNotEmptyList() {
        configureWebServer {
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/single1.json")
                }
            )
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/single2.json")
                }
            )
        }

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { containsSnippet("saved-search-1") }
                isChangeButtonShown()
                isDoneButtonHidden()

                refreshList()

                waitUntil { containsSnippet("saved-search-2") }
                doesNotContainsSnippet("saved-search-1")
                isChangeButtonShown()
                isDoneButtonHidden()
            }
        }
    }

    @Test
    fun shouldDeleteSavedSearch() {
        val dispatcher = DispatcherRegistry()
        dispatcher.register(
            request {
                method("GET")
                path("2.0/savedSearch")
            },
            response {
                assetBody("savedSearchesTest/single1.json")
            }
        )
        val expectedDeleteRequest = dispatcher.register(
            request {
                method("DELETE")
                path("2.0/savedSearch/saved-search-1")
            },
            success()
        )
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { containsSnippet("saved-search-1") }

                tapOn(lookup.matchesChangeButton())

                performOnSnippet("saved-search-1") {
                    tapOn(lookup.matchesDeleteButton())
                }

                performOnConfirmationDialog {
                    waitUntil { isConfirmationDialogContentShown() }
                    confirm()
                }

                waitUntil { isFullscreenEmptyViewShown() }

                performOnFullscreenEmpty {
                    isImageEquals(R.drawable.ill_no_searches)
                    isTitleEquals(R.string.my_searches_list_empty_title)
                    isDescriptionEquals(R.string.my_searches_list_empty_text)
                }

                isChangeButtonHidden()
                isDoneButtonHidden()

                waitUntil { expectedDeleteRequest.isOccured() }
            }
        }
    }

    @Test
    fun shouldChangeMode() {
        configureWebServer {
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/double.json")
                }
            )
        }

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { containsSnippet("saved-search-1") }
                performOnSnippet("saved-search-1") {
                    isNewItemsBadgeShown()
                    isNewItemsBadgeTextEquals("10")
                    isDeleteButtonHidden()
                    isNotificationViewChecked()
                }
                containsSnippet("saved-search-2")
                performOnSnippet("saved-search-2") {
                    isNewItemsBadgeHidden()
                    isDeleteButtonHidden()
                    isNotificationViewNotChecked()
                }
                isChangeButtonShown()
                isDoneButtonHidden()

                tapOn(lookup.matchesChangeButton())

                containsSnippet("saved-search-1")
                performOnSnippet("saved-search-1") {
                    isNewItemsBadgeHidden()
                    isDeleteButtonShown()
                    isNotificationViewChecked()
                }
                containsSnippet("saved-search-2")
                performOnSnippet("saved-search-2") {
                    isNewItemsBadgeHidden()
                    isDeleteButtonShown()
                    isNotificationViewNotChecked()
                }
                isChangeButtonHidden()
                isDoneButtonShown()

                tapOn(lookup.matchesDoneButton())

                containsSnippet("saved-search-1")
                performOnSnippet("saved-search-1") {
                    isNewItemsBadgeShown()
                    isNewItemsBadgeTextEquals("10")
                    isDeleteButtonHidden()
                    isNotificationViewChecked()
                }
                containsSnippet("saved-search-2")
                performOnSnippet("saved-search-2") {
                    isNewItemsBadgeHidden()
                    isDeleteButtonHidden()
                    isNotificationViewNotChecked()
                }
                isChangeButtonShown()
                isDoneButtonHidden()
            }
        }
    }

    @Test
    fun shouldVisitSavedSearch() {
        val dispatcher = DispatcherRegistry()
        dispatcher.register(
            request {
                method("GET")
                path("2.0/savedSearch")
            },
            response {
                assetBody("savedSearchesTest/single1.json")
            }
        )
        dispatcher.register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("savedSearchesTest/offerWithSiteSearch.json")
            }
        )
        val expectedVisitRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/savedSearch/saved-search-1/visit")
            },
            success()
        )
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            waitUntil { isSubscriptionsIndicatorShown() }

            performOnSavedSearchesScreen {
                waitUntil { containsSnippet("saved-search-1") }
                performOnSnippet("saved-search-1") {
                    isNewItemsBadgeShown()
                    isNewItemsBadgeTextEquals("10")
                }

                tapOn(lookup.matchesSnippetView("saved-search-1"))
            }
        }

        performOnSavedSearchOffersScreen {
            waitUntil { containsOfferSnippet("1") }
            waitUntil { expectedVisitRequest.isOccured() }

            pressBack()
        }

        performOnFavoritesScreen {
            waitUntil { isSubscriptionsIndicatorHidden() }

            performOnSavedSearchesScreen {
                containsSnippet("saved-search-1")
                performOnSnippet("saved-search-1") {
                    isNewItemsBadgeHidden()
                }
            }
        }
    }

    @Test
    fun shouldChangeSavedSearchName() {
        val dispatcher = DispatcherRegistry()
        dispatcher.register(
            request {
                method("GET")
                path("2.0/savedSearch")
            },
            response {
                assetBody("savedSearchesTest/single1.json")
            }
        )
        val expectedUpdateRequest = dispatcher.register(
            request {
                method("PATCH")
                path("2.0/savedSearch/saved-search-1")
            },
            success()
        )
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { containsSnippet("saved-search-1") }
                performOnSnippet("saved-search-1") {
                    isNameEquals("Москва")
                }

                tapOn(lookup.matchesSnippetView("saved-search-1"))
            }
        }

        performOnSavedSearchOffersScreen {
            waitUntil { isParamsButtonShown() }

            tapOn(lookup.matchesParamsButton())
        }

        performOnSavedSearchParamsScreen {
            isNameEquals("Москва")
            isGeoRegionButtonHidden()
            isGeoDescriptionShown()
            isGeoDescriptionEquals("Область поиска: Город Москва")

            tapOn(lookup.matchesClearNameButton())

            isNameEquals("")

            typeText(lookup.matchesNameInputView(), "Updatedname")

            tapOn(lookup.matchesUpdateSearchButton())
        }

        performOnSavedSearchOffersScreen {
            pressBack()
        }

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                containsSnippet("saved-search-1")
                performOnSnippet("saved-search-1") {
                    isNameEquals("Updatedname")
                }

                waitUntil { expectedUpdateRequest.isOccured() }
            }
        }
    }

    @Test
    fun shouldClearSavedSearchesOnLogout() {
        configureWebServer {
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/single1.json")
                }
            )
        }

        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { containsSnippet("saved-search-1") }

                authorizationRule.logout()

                waitUntil { isFullscreenEmptyViewShown() }
            }
        }
    }

    @Test
    fun shouldLoadSavedSearchesOnLogin() {
        configureWebServer {
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    setBody("""{"response": {}}""")
                }
            )
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/single1.json")
                }
            )
        }

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnSavedSearchesScreen {
                waitUntil { isFullscreenEmptyViewShown() }

                authorizationRule.setUserAuthorized()

                waitUntil { containsSnippet("saved-search-1") }
            }
        }
    }

    private fun ConfirmationDialogRobot.isConfirmationDialogContentShown() {
        isTitleEquals("Удаление подписки")
        isMessageEquals("Вы действительно хотите удалить подписку?")
        isNegativeButtonTextEquals("Отмена")
        isPositiveButtonTextEquals("удалить")
    }
}
