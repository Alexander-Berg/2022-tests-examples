package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnAddPhoneScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationCompleteScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.*
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author matek3022 on 2020-07-16.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ContactsTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormActivityTestRule(launchActivity = false)
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule,
        draftRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun shouldRewriteEmailAndName() {
        val name = "Peter Jonson"
        val email = "peterJ@yandex.ru"
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerPatchUserProfile(name = name, email = email)
            registerValidation()
            registerDraft()
            registerPublishForm()
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            containsContactNameField("John")
            tapOn(lookup.matchesContactNameField())
            tapOn(lookup.matchesContactNameClearButton())
            containsContactNameField()
            typeText(lookup.matchesContactNameFieldValue(), name)
            containsContactNameField(name)

            containsContactEmailField("john@gmail.com")
            tapOn(lookup.matchesContactEmailField())
            tapOn(lookup.matchesContactEmailClearButton())
            containsContactEmailField()
            typeText(lookup.matchesContactEmailFieldValue(), email)
            containsContactEmailField(email)

            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil { isPromoShown() }
        }
    }

    @Test
    fun shouldUncheckPhone() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerPatchUserProfile(phoneSelected = false)
            registerValidation()
            registerDraft()
            registerPublishForm()
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            containsContactPhoneField(FIRST_CARD_SHOW_PHONE, true)

            tapOn(lookup.matchesContactPhoneFieldSelector(FIRST_CARD_SHOW_PHONE))

            containsContactPhoneField(FIRST_CARD_SHOW_PHONE, false)

            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil { isPromoShown() }
        }
    }

    @Test
    fun shouldUncheckPhoneAndAddNewPhone() {
        val code = "410060"
        configureWebServer {
            registerUserProfile()
            registerUserProfileTwoPhones()
            registerUserProfile()
            registerPatchUserProfileSecondPhone()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerPassportPhoneBind(SECOND_FULL_PHONE)
            registerPassportPhoneConfirm(code)
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            containsContactPhoneField(FIRST_CARD_SHOW_PHONE, true)
            tapOn(lookup.matchesContactPhoneField(FIRST_CARD_SHOW_PHONE))

            scrollToPosition(lookup.matchesAddContactPhoneButton()).tapOn()

            performOnAddPhoneScreen {
                waitUntil { isPhoneNumberExpandedToolbarShown() }
                collapseAppBar()
                waitUntil { isPhoneNumberCollapsedToolbarShown() }

                isPhoneNumberDescriptionShown()
                containsPhoneNumberField("+7")
                isSubmitPhoneButtonNotShown()

                typeText(lookup.matchesPhoneNumberFieldValue(), SECOND_FULL_PHONE_TYPED)
                containsPhoneNumberField(SECOND_FULL_PHONE_WITH_DELIMITERS)
                isSubmitPhoneButtonShown()

                tapOn(lookup.matchesSubmitPhoneButton())

                waitUntil { isConfirmCodeCollapsedToolbarShown() }
                containsConfirmCodeField()

                typeText(lookup.matchesConfirmCodeFieldValue(), code, closeKeyboard = false)
            }

            waitUntil {
                containsContactPhoneField(SECOND_CARD_SHOW_PHONE, true)
            }
            containsContactPhoneField(FIRST_CARD_SHOW_PHONE, false)

            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil { isPromoShown() }
        }
    }

    @Test
    fun shouldShowErrorToastsWhenAddPhoneAndCode() {
        val code = "410060"
        configureWebServer {
            registerUserProfile()
            registerUserProfileTwoPhones()
            registerPassportPhoneBind(SECOND_FULL_PHONE, "TOO_MANY_REQUESTS")
            registerPassportPhoneBind(SECOND_FULL_PHONE, "PHONE_ALREADY_BOUND")
            registerPassportPhoneBind(SECOND_FULL_PHONE, "PHONE_BLOCKED")
            registerPassportPhoneBind(SECOND_FULL_PHONE, "PHONE_BAD_NUM_FORMAT")
            registerPassportPhoneBind(SECOND_FULL_PHONE, "OTHER")
            registerPassportPhoneBind(SECOND_FULL_PHONE)
            registerPassportPhoneConfirm(code, "PHONE_BAD_CONFIRMATION_CODE")
            registerPassportPhoneConfirm(code, "OTHER")
            registerPassportPhoneConfirm(code)
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            containsContactPhoneField(FIRST_CARD_SHOW_PHONE, true)
            tapOn(lookup.matchesContactPhoneField(FIRST_CARD_SHOW_PHONE))

            scrollToPosition(lookup.matchesAddContactPhoneButton()).tapOn()

            performOnAddPhoneScreen {
                waitUntil { isPhoneNumberExpandedToolbarShown() }
                collapseAppBar()
                waitUntil { isPhoneNumberCollapsedToolbarShown() }

                tapOn(lookup.matchesPhoneNumberClearButton())
                typeText(lookup.matchesPhoneNumberFieldValue(), SECOND_FULL_PHONE)

                tapOn(lookup.matchesSubmitPhoneButton())
                waitUntil {
                    containsPhoneNumberField(SECOND_FULL_PHONE_WITH_DELIMITERS)
                    containsError(getResourceString(R.string.phone_error_retries_exceeded))
                }

                tapOn(lookup.matchesSubmitPhoneButton())
                waitUntil {
                    containsPhoneNumberField(SECOND_FULL_PHONE_WITH_DELIMITERS)
                    containsError(getResourceString(R.string.phone_error_already_in_account))
                }

                tapOn(lookup.matchesSubmitPhoneButton())
                waitUntil {
                    containsPhoneNumberField(SECOND_FULL_PHONE_WITH_DELIMITERS)
                    containsError(getResourceString(R.string.phone_error_blocked))
                }

                tapOn(lookup.matchesSubmitPhoneButton())
                waitUntil {
                    containsPhoneNumberField(SECOND_FULL_PHONE_WITH_DELIMITERS)
                    containsError(getResourceString(R.string.phone_error_bad_format))
                }

                tapOn(lookup.matchesSubmitPhoneButton())
                waitUntil {
                    containsError(getResourceString(R.string.error_try_again))
                }

                tapOn(lookup.matchesRetryButton())
                waitUntil { isConfirmCodeCollapsedToolbarShown() }

                typeText(lookup.matchesConfirmCodeFieldValue(), code, closeKeyboard = false)
                waitUntil {
                    containsConfirmCodeField(code)
                    containsError(
                        getResourceString(R.string.phone_error_incorrect_confirmation_code)
                    )
                }

                tapOn(lookup.matchesConfirmCodeClearButton())
                typeText(lookup.matchesConfirmCodeFieldValue(), code, closeKeyboard = false)
                waitUntil {
                    containsError(getResourceString(R.string.error_try_again))
                }

                tapOn(lookup.matchesRetryButton())
            }

            waitUntil {
                containsContactPhoneField(SECOND_CARD_SHOW_PHONE, true)
            }
            containsContactPhoneField(FIRST_CARD_SHOW_PHONE, false)
        }
    }

    @Test
    fun shouldShowEmailAndPhoneEmptyAndAddIt() {
        val email = "john@gmail.com"
        val code = "410060"
        configureWebServer {
            registerEmptyUserProfile()
            registerUserProfile()
            registerPatchUserProfile()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerPassportPhoneBind(FIRST_FULL_PHONE)
            registerPassportPhoneConfirm(code)
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            containsContactEmailField()

            typeText(lookup.matchesContactEmailFieldValue(), email)
            containsContactEmailField(email)

            containsAddPhoneButton()

            tapOn(lookup.matchesAddContactPhoneButton())

            performOnAddPhoneScreen {
                waitUntil { isPhoneNumberExpandedToolbarShown() }
                collapseAppBar()
                waitUntil { isPhoneNumberCollapsedToolbarShown() }

                tapOn(lookup.matchesPhoneNumberClearButton())
                typeText(lookup.matchesPhoneNumberFieldValue(), FIRST_FULL_PHONE)
                tapOn(lookup.matchesSubmitPhoneButton())

                waitUntil { isConfirmCodeCollapsedToolbarShown() }
                typeText(lookup.matchesConfirmCodeFieldValue(), code, closeKeyboard = false)
            }

            waitUntil {
                containsContactPhoneField(FIRST_CARD_SHOW_PHONE, true)
            }

            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil { isPromoShown() }
        }
    }

    @Test
    fun shouldChangeComChannelsFromDefaultCallsAndChatsToCalls() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerValidation()
            registerPatchUserProfile(chatsAllowed = false)
            registerDraft()
            registerPublishForm()
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()

            containsComChannelsField(lookup.matchesComChannelsSelectorCallsAndChats())
            onView(lookup.matchesComChannelsSelectorCalls()).tapOn()

            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil { isPromoShown() }
        }
    }

    @Test
    fun shouldChangeComChannelsFromCallsToCallsAndChats() {
        configureWebServer {
            registerUserProfileComChannelCalls()
            registerUserProfileComChannelCalls()
            registerValidation()
            registerPatchUserProfile(chatsAllowed = true)
            registerDraft()
            registerPublishForm()
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()

            containsComChannelsField(lookup.matchesComChannelsSelectorCalls())
            onView(lookup.matchesComChannelsSelectorCallsAndChats()).tapOn()

            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil { isPromoShown() }
        }
    }

    @Test
    fun shouldShowComChannelsWithCallsAndChatsWhenNewUser() {
        configureWebServer {
            registerNewUserProfile()
            registerNewUserProfile()
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()

            containsComChannelsField(lookup.matchesComChannelsSelectorCallsAndChats())
        }
    }

    @Test
    fun shouldNotShowComChannelsWhenJuridicUser() {
        configureWebServer {
            registerJuridicUserProfile()
            registerJuridicUserProfile()
        }
        prepareDraftScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()

            doesNotContainComChannelsField()
        }
    }

    private fun prepareDraftScreen() {
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
    }

    private fun DispatcherRegistry.registerPatchUserProfile(
        name: String = "John",
        email: String = "john@gmail.com",
        phoneId: String = "1",
        phoneSelected: Boolean = true,
        chatsAllowed: Boolean = true
    ) {
        val comChannels = if (chatsAllowed) {
            "[\"COM_CALLS\",\"COM_CHATS\"]"
        } else {
            "[\"COM_CALLS\"]"
        }
        register(
            request {
                method("PATCH")
                path("1.0/user")
                body(
                    """{
                                    "user": {
                                        "name": "$name",
                                        "email": "$email",
                                        "phones": [
                                            {
                                                "id": "$phoneId",
                                                "select": $phoneSelected
                                            }
                                        ],
                                        "redirectPhones":true,
                                        "allowedCommunicationChannels":$comChannels
                                    }
                                }"""
                )
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerPatchUserProfileSecondPhone() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                body(
                    """{
                                    "user": {
                                        "name": "John",
                                        "email": "john@gmail.com",
                                        "phones": [
                                            {
                                                "id": "1",
                                                "select": false
                                            },
                                            {
                                                "id": "2",
                                                "select": true
                                            }
                                        ],
                                        "redirectPhones":true,
                                        "allowedCommunicationChannels":["COM_CALLS","COM_CHATS"]
                                    }
                                }"""
                )
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfileTwoPhones() {
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "valid": true,
                                    "user": {
                                        "name": "John",
                                        "status": "active",
                                        "phones": [
                                            {
                                                "id": "1",
                                                "phone": "$FIRST_CARD_SHOW_PHONE",
                                                "select": true,
                                                "fullPhone": "+71112223344"
                                            },
                                            {
                                                "id": "2",
                                                "phone": "$SECOND_CARD_SHOW_PHONE",
                                                "select": true,
                                                "fullPhone": "$SECOND_FULL_PHONE"
                                            }
                                        ],
                                        "email" : "john@gmail.com",
                                        "type": "OWNER",
                                        "redirectPhones": true,
                                        "paymentType": "NATURAL_PERSON",
                                        "capaUser": false
                                    }
                                }
                            }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfileComChannelCalls() {
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "valid": true,
                                    "user": {
                                        "name": "John",
                                        "status": "active",
                                        "phones": [
                                            {
                                                "id": "1",
                                                "phone": "$FIRST_CARD_SHOW_PHONE",
                                                "select": true,
                                                "fullPhone": "+71112223344"
                                            }
                                        ],
                                        "email" : "john@gmail.com",
                                        "type": "OWNER",
                                        "redirectPhones": true,
                                        "allowedCommunicationChannels": ["COM_CALLS"],
                                        "paymentType": "NATURAL_PERSON"
                                    }
                                }
                            }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerNewUserProfile() {
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                assetBody("user/newUser.json")
            }
        )
    }

    private fun DispatcherRegistry.registerJuridicUserProfile() {
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "valid": true,
                                    "user": {
                                        "name": "John",
                                        "status": "active",
                                        "phones": [
                                            {
                                                "id": "1",
                                                "phone": "$FIRST_CARD_SHOW_PHONE",
                                                "select": true,
                                                "fullPhone": "+71112223344"
                                            }
                                        ],
                                        "email" : "john@gmail.com",
                                        "type": "AGENCY",
                                        "paymentType": "JURIDICAL_PERSON",
                                        "allowedCommunicationChannels": ["COM_CALLS"]
                                    }
                                }
                            }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyUserProfile() {
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                assetBody("user/userOwnerEmpty.json")
            }
        )
    }

    private fun DispatcherRegistry.registerValidation() {
        register(
            request {
                method("POST")
                path("1.0/user/offers/validation")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerPublishForm() {
        register(
            request {
                method("PUT")
                path("1.0/user/offers/draft/1234")
                queryParam("publish", "true")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    companion object {
        private const val SECOND_FULL_PHONE = "+79376666666"
        private const val SECOND_FULL_PHONE_TYPED = "9376666666"
        private const val SECOND_FULL_PHONE_WITH_DELIMITERS = "+7 (937) 666-66-66"
        private const val SECOND_CARD_SHOW_PHONE = "+7937*****66"
        private const val FIRST_FULL_PHONE = "+71112223344"
        private const val FIRST_CARD_SHOW_PHONE = "+7111*****44"
    }
}
