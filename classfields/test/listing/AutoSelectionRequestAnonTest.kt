package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.actions.step
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.breadcrumbs.getBreadcrumbsMitsubishiModels
import ru.auto.ara.core.dispatchers.user.postUserPhones
import ru.auto.ara.core.dispatchers.search_offers.MarkModelFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.checkLogin
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.autoselectionrequestform.checkAutoSelectionForm
import ru.auto.ara.core.robot.autoselectionrequestform.performAutoSelectionForm
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.useroffers.checkOffers
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class AutoSelectionRequestAnonTest {

    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule {
            postUserPhones()
            userSetup()
            postLoginOrRegisterSuccess()
            getBreadcrumbsMitsubishiModels()
            delegateDispatchers(
                PostSearchOffersDispatcher("new_cars_auto_selection_form_flags"),
                MarkModelFiltersDispatcher("application_mark_models")
            )
        },
        DisableAdsRule(),
        activityRule
    )

    @Before
    fun setup() {
        activityRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragmentBundles.searchFeedBundle(
                VehicleCategory.CARS,
                StateGroup.NEW
            )
        )
        performSearchFeed {
            waitFirstPage()
            scrollToItemBelowOfferSnippet(1)
        }
    }

    @Test
    fun checkOpenFormButtonOnSnippet() {
        checkOffers {
            isAutoSelectionFormActionDisplayed(FIRST_OFFER_INDEX, getResourceString(R.string.get_best_price))
        }
    }

    @Test
    fun checkFormFields() {
        openForm()
        checkAutoSelectionForm {
            checkFormComposition(MARK_FROM_FEED, MODEL_FROM_FEED, isExpanded = false)
        }
    }

    @Test
    fun shouldExpandMarkModelWhenModelIsAbsent() {
        openForm()
        performAutoSelectionForm {
            expandMarkModelButton()
        }
        checkAutoSelectionForm {
            checkMarkModelExpanded(MARK_FROM_FEED, MODEL_FROM_FEED)
        }
        performAutoSelectionForm {
            clearModel()
        }
        checkAutoSelectionForm {
            checkMarkModelExpanded(MARK_FROM_FEED, null)
        }
    }

    @Test
    fun shouldCollapseMarkModelWhenNewModelIsSelected() {
        openForm()
        performAutoSelectionForm {
            expandMarkModelButton()
            clearModel()
            openModelPicker()
        }
        performModel {
            tapOnItemWithName(NEW_SELECTED_MODEL)
        }
        checkAutoSelectionForm {
            checkMarkModelCollapsed(MARK_FROM_FEED, NEW_SELECTED_MODEL)
        }
    }

    @Test
    fun shouldExpandMarkModelWhenOnlyNewMarkIsSelected() {
        openForm()
        performAutoSelectionForm {
            expandMarkModelButton()
            openMarkPicker()
        }
        performMark {
            interactions.onAcceptButton().performClick()
        }
        checkAutoSelectionForm {
            checkMarkModelExpanded(MARK_FROM_FEED, null)
        }
    }

    @Test
    fun shouldNotFillPhoneOnNotLoggedUser() {
        openForm()
        checkAutoSelectionForm {
            checkPhoneIsEmpty()
        }
    }

    @Test
    fun shouldLoginWhenClickPhoneField() {
        openForm()
        checkAutoSelectionForm {
            isPhoneEmptyWithPlaceholderText()
        }
        performAutoSelectionForm {
            openAddPhoneForm()
        }
        checkLogin {
            isTitle(R.string.selection_form_phone_number)
        }
        performLogin {
            loginWithPhoneAndCode(PHONE, CODE)
        }
        checkAutoSelectionForm {
            checkPhoneHasNumber(PHONE)
        }
    }

    @Test
    fun shouldRedirectWhenTermsClicked() {
        openForm()
        watchWebView {
            performAutoSelectionForm {
                openTerms()
            }
        }.checkResult {
            checkTitleMatches(R.string.selection_form_terms_title)
            checkUrlMatches("https://yandex.ru/legal/autoru_terms_of_service/")
        }
    }

    private fun openForm() = step("open application form") {
        performOffers {
            interactions.onSecondActionButton(FIRST_OFFER_INDEX).performClick()
        }
    }

    companion object {
        private const val FIRST_OFFER_INDEX = 5

        private const val MARK_FROM_FEED = "Mitsubishi"
        private const val MODEL_FROM_FEED = "Pajero Sport"
        private const val NEW_SELECTED_MODEL = "Outlander"

        private const val PHONE = "+7 (000) 000-00-00"
        private const val CODE = "0000"

        private const val COMMENTARY_TEXT = "Хочу чтобы быстро ездила врум-врум"
    }
}
