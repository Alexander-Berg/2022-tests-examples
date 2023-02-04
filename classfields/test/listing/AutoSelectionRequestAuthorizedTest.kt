package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.SuccessDispatcher
import ru.auto.ara.core.dispatchers.user.postUserPhones
import ru.auto.ara.core.dispatchers.breadcrumbs.getBreadcrumbsPorscheModels
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
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class AutoSelectionRequestAuthorizedTest {
    private val requestWatcher = RequestWatcher()

    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    private val webServerRule = WebServerRule {
        postUserPhones()
        userSetup()
        getBreadcrumbsPorscheModels()
        delegateDispatchers(
            PostSearchOffersDispatcher("new_cars_auto_selection_form_flags"),
            MarkModelFiltersDispatcher("application_mark_models"),
            SuccessDispatcher(requestWatcher) { url, _ ->
                url.encodedPath.contains("match-applications")
            }
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityRule,
        SetupAuthRule()
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
        performOffers {
            interactions.onSecondActionButton(FIRST_OFFER_INDEX).performClick()
        }
    }

    @Test
    fun shouldFillPhoneForLoggedUser() {
        checkAutoSelectionForm {
            checkPhoneHasNumber(LOGGED_USER_PHONE)
        }
    }

    @Test
    fun shouldRedirectWhenAddingNewPhone() {
        performAutoSelectionForm {
            openPhonePickerDialog()
        }
        checkAutoSelectionForm {
            isPhonePickerVisible()
        }
        performAutoSelectionForm {
            openAddPhoneFromPicker()
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
    fun shouldSendFormWithFilledFields() {
        performAutoSelectionForm {
            selectTradeIn(isExpanded = false)
            selectCredit(isExpanded = false)
            sendForm()
        }
        requestWatcher.checkRequestBodyParameters(
            MARK_PATH to NW_MARK,
            MODEL_PATH to NW_MODEL,
            USER_PHONE_PATH to NW_PHONE,
            USER_ID_PATH to USER_ID,
            CREDIT_PATH to "true"
        )
        requestWatcher.checkBody {
            asObject {
                get(TRADE_IN_PATH).asObject {
                    assert(values.isEmpty())
                }
            }
        }
    }

    @Test
    fun shouldSendChangedValuesFromFilledFields() {
        performAutoSelectionForm {
            expandMarkModelButton()
            openMarkPicker()
        }
        performMark {
            tapOnItemWithName(NEW_MARK)
        }
        performModel {
            tapOnItemWithName(NEW_MODEL)
        }
        performAutoSelectionForm { sendForm() }
        requestWatcher.checkRequestBodyParameters(
            MARK_PATH to NW_NEW_MARK,
            MODEL_PATH to NW_NEW_MODEL,
            USER_PHONE_PATH to NW_PHONE,
            USER_ID_PATH to USER_ID,
            CREDIT_PATH to "false"
        )
        requestWatcher.checkNotRequestBodyParameter(TRADE_IN_PATH)
    }

    companion object {
        private const val FIRST_OFFER_INDEX = 5

        private const val LOGGED_USER_PHONE = "+7 (000) 000-00-00"
        private const val PHONE = "+7 (000) 000-00-11"
        private const val CODE = "0000"

        private const val NW_MARK = "MITSUBISHI"
        private const val NW_MODEL = "PAJERO_SPORT"

        private const val NEW_MARK = "Porsche"
        private const val NEW_MODEL = "Macan"
        private const val NW_NEW_MARK = "PORSCHE"
        private const val NW_NEW_MODEL = "MACAN"

        private const val NW_PHONE = "+70000000000"
        private const val USER_ID = "id00000000"
        private const val MARK_PATH = "user_proposal.search_params.catalog_filter.0.mark"
        private const val MODEL_PATH = "user_proposal.search_params.catalog_filter.0.model"
        private const val CREDIT_PATH = "user_info.credit_info.is_possible"
        private const val TRADE_IN_PATH = "user_offer"
        private const val USER_PHONE_PATH = "user_info.phone"
        private const val USER_ID_PATH = "user_info.user_id"
    }
}
