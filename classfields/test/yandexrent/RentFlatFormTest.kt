package com.yandex.mobile.realty.test.yandexrent

import android.Manifest
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.DeepLinkIntentCommand
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.rule.*
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.*
import com.yandex.mobile.realty.utils.JsonObjectBuilder
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 02/12/2021.
 */
@LargeTest
class RentFlatFormTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)
    private val mockLocationRule = MockLocationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        mockLocationRule,
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION),
        activityTestRule
    )

    @Test
    fun createAndConfirmFlatDraftFromServicesPromo() {
        val dispatcher = DispatcherRegistry()
        val submitNotificationsRequest: ExpectedRequest
        with(dispatcher) {
            registerServicesInfo()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/newUser.json")
            registerUserProfile("user/newUser.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = false))

            registerGetNearAuroraAddress()
            registerUpdateDraft { draftWithAddressSubmitBody() }
            registerOwnerServicesInfo()
            registerOwnerRentFlats(status = "DRAFT")

            registerUpdateDraft { draftWithFullSubmitBody() }
            registerOwnerServicesInfo()
            registerOwnerRentFlats(status = "WAITING_FOR_CONFIRMATION")
            registerRequestSmsCode()
            submitNotificationsRequest = registerSubmitNotificationsConfiguration(
                notificationsConfiguration(enabled = true)
            )

            registerSubmitSmsCode()
            registerOwnerServicesInfo()
            registerOwnerRentFlats(status = "CONFIRMED")
        }
        configureWebServer(dispatcher)

        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.contains(this) }
            rentPromoRentOutButton.click()
        }

        authorizationRule.registerAuthorizationIntent()

        onScreen<WebViewScreen> {
            val openFormScript = "(function() { mobileAppInjector.onOpenFormPressed(); })();"
            webView.invoke { evaluateJavascript(openFormScript) }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            isContentStateMatches(getTestRelatedFilePath("editingContent"))
            listView.scrollTo(addressItem).click()
        }

        onScreen<AddressRootScreen> {
            onScreen<AddressSelectScreen> {
                waitUntil { contentView.isCompletelyDisplayed() }
                addressView.isTextEquals(SHORT_AURORA_ADDRESS)
                confirmAddressButton.click()
            }
        }

        onScreen<RentFlatFormScreen> {
            addressItem.view.isTextEquals(AURORA_ADDRESS_WITHOUT_COUNTRY)
            listView.scrollTo(flatNumberItem)
                .invoke { inputView.typeText(FLAT_NUMBER) }
            listView.scrollTo(firstNameItem)
                .invoke { inputView.typeText(FIRST_NAME) }
            listView.scrollTo(lastNameItem)
                .invoke { inputView.typeText(LAST_NAME) }
            listView.scrollTo(middleNameItem)
                .invoke { inputView.typeText(MIDDLE_NAME) }
            listView.scrollTo(phoneItem)
                .invoke { inputView.typeText(PHONE) }
            listView.scrollTo(emailItem)
                .invoke { inputView.typeText(EMAIL) }
            listView.scrollTo(submitButton).click()

            listView.waitUntil {
                contains(smsCodeItem)
                contains(resendSmsButton)
            }
            submitNotificationsRequest.isOccured()
            closeKeyboard()
            isContentStateMatches(getTestRelatedFilePath("confirmationContent"))
            listView.scrollTo(smsCodeItem)
                .invoke { inputView.typeText(SMS_CODE) }
            listView.scrollTo(confirmButton).click()

            listView.waitUntil { contains(successItem) }
            isContentStateMatches(getTestRelatedFilePath("successContent"))
            successButton.click()
        }

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .invoke {
                    subtitleView.isTextEquals(R.string.yandex_rent_flat_owner_status_confirmed)
                }
        }
    }

    @Test
    fun openDraftWithFullData() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft(fullDraftData(FULL_AURORA_ADDRESS))
            registerResolveGeoByAddress(FULL_AURORA_ADDRESS, "geocoderAddressAurora.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
            registerGetAuroraAddress()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            isContentStateMatches(getTestRelatedFilePath("editingContent"))
            listView.scrollTo(addressItem).click()
        }

        onScreen<AddressRootScreen> {
            onScreen<AddressSelectScreen> {
                waitUntil { contentView.isCompletelyDisplayed() }
                addressView.isTextEquals(SHORT_AURORA_ADDRESS)
            }
        }
    }

    @Test
    fun openDraftWithNotResolvedAddress() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft(fullDraftData(NOT_RESOLVING_ADDRESS))
            registerResolveGeoByAddress(NOT_RESOLVING_ADDRESS, "geocoderEmptyResult.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
            registerGetNearAuroraAddress()
        }

        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            isContentStateMatches(getTestRelatedFilePath("editingContent"))
            listView.scrollTo(addressItem).click()
        }

        onScreen<AddressRootScreen> {
            onScreen<AddressSelectScreen> {
                waitUntil { contentView.isCompletelyDisplayed() }
                addressView.isTextEquals(SHORT_AURORA_ADDRESS)
            }
        }
    }

    @Test
    fun openNewDraftWithPreFilledPersonDataFromRentUser() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo(fullRentUserData())
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            isContentStateMatches(getTestRelatedFilePath("editingContent"))
        }
    }

    @Test
    fun openNewDraftWithPreFilledPersonDataFromProfile() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/userOwner.json")
            registerUserProfile("user/userOwner.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
        }

        authorizationRule.setAccountData(
            email = "mike@gmail.com",
            firstName = "Mike",
            lastName = "Brown"
        )
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            isContentStateMatches(getTestRelatedFilePath("editingContent"))
        }
    }

    @Test
    fun shouldNotSubmitNotificationsAcceptanceWhenNotChecked() {
        val dispatcher = DispatcherRegistry()
        val submitNotificationsRequest: ExpectedRequest
        with(dispatcher) {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft(fullDraftData(FULL_AURORA_ADDRESS))
            registerResolveGeoByAddress(FULL_AURORA_ADDRESS, "geocoderAddressAurora.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = false))

            registerUpdateDraft()
            registerRequestSmsCode()
            submitNotificationsRequest = registerSubmitNotificationsConfiguration()
        }
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            notificationsAcceptanceItem
                .waitUntil { listView.contains(this) }
                .apply { click() }
                .isViewStateMatches(getTestRelatedFilePath("notChecked"))

            listView.scrollTo(submitButton).click()

            listView.waitUntil { contains(smsCodeItem) }
            submitNotificationsRequest.isNotOccurred()
            closeKeyboard()
            pressBack()

            notificationsAcceptanceItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notChecked"))
        }
    }

    @Test
    fun shouldSubmitNotificationsAcceptanceAfterError() {
        val dispatcher = DispatcherRegistry()
        val submitNotificationsRequest: ExpectedRequest
        with(dispatcher) {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft(fullDraftData(FULL_AURORA_ADDRESS))
            registerResolveGeoByAddress(FULL_AURORA_ADDRESS, "geocoderAddressAurora.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = false))

            registerUpdateDraft()
            registerRequestSmsCode()
            registerSubmitNotificationsConfigurationError()

            registerUpdateDraft()
            registerRequestSmsCode()
            submitNotificationsRequest = registerSubmitNotificationsConfiguration()
        }
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            listView.scrollTo(submitButton).click()

            listView.waitUntil { contains(smsCodeItem) }
            closeKeyboard()
            pressBack()

            notificationsAcceptanceItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("checked"))
            listView.scrollTo(submitButton).click()

            listView.waitUntil { contains(smsCodeItem) }
            submitNotificationsRequest.isOccured()
            closeKeyboard()
            pressBack()

            waitUntil { listView.contains(addressItem) }
            listView.doesNotContain(notificationsAcceptanceItem)
        }
    }

    @Test
    fun shouldNowShowNotificationsAcceptanceWhenAccepted() {
        val dispatcher = DispatcherRegistry()
        val submitNotificationsRequest: ExpectedRequest
        with(dispatcher) {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft(fullDraftData(FULL_AURORA_ADDRESS))
            registerResolveGeoByAddress(FULL_AURORA_ADDRESS, "geocoderAddressAurora.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))

            registerUpdateDraft()
            registerRequestSmsCode()
            submitNotificationsRequest = registerSubmitNotificationsConfiguration()
        }
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            isContentStateMatches(getTestRelatedFilePath("hiddenNotificationsAcceptance"))
            listView.scrollTo(submitButton).click()

            listView.waitUntil { contains(smsCodeItem) }
            submitNotificationsRequest.isNotOccurred()
        }
    }

    @Test
    fun openAnnotationLinks() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/newUser.json")
            registerUserProfile("user/newUser.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
            registerUpdateDraft()
            registerRequestSmsCode()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            annotationItem.waitUntil { listView.contains(this) }
                .tapOnLinkText(R.string.yandex_rent_application_form_terms_of_use_action)
        }

        onScreen<WebViewScreen> {
            val expectedUrl = "https://yandex.ru/legal/lease_termsofuse/?only-content=true"
            waitUntil { webView.isPageUrlEquals(expectedUrl) }
            pressBack()
        }

        onScreen<RentFlatFormScreen> {
            listView.scrollTo(submitButton).click()

            waitUntil { listView.contains(smsCodeItem) }
            listView.scrollTo(annotationItem)
                .tapOnLinkText(R.string.yandex_rent_application_form_owner_license_action)
        }

        onScreen<WebViewScreen> {
            val expectedUrl = "https://yandex.ru/legal/realty_lease_landlord/?only-content=true"
            waitUntil { webView.isPageUrlEquals(expectedUrl) }
        }
    }

    @Test
    fun postponeDraft() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/newUser.json")
            registerUserProfile("user/newUser.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
            registerGetNearAuroraAddress()

            registerUpdateDraft { draftWithAddressSubmitBody() }
            registerOwnerServicesInfo()
            registerOwnerRentFlats()

            registerUpdateDraft { draftWithFullSubmitBody() }
            registerOwnerServicesInfo()
            registerOwnerRentFlats(status = "DRAFT")
        }

        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            addressItem.waitUntil { listView.contains(this) }.click()
        }

        onScreen<AddressRootScreen> {
            onScreen<AddressSelectScreen> {
                waitUntil { contentView.isCompletelyDisplayed() }
                addressView.isTextEquals(SHORT_AURORA_ADDRESS)
                confirmAddressButton.click()
            }
        }

        onScreen<RentFlatFormScreen> {
            addressItem.view.isTextEquals(AURORA_ADDRESS_WITHOUT_COUNTRY)
            listView.scrollTo(flatNumberItem)
                .invoke { inputView.typeText(FLAT_NUMBER) }
            listView.scrollTo(firstNameItem)
                .invoke { inputView.typeText(FIRST_NAME) }
            listView.scrollTo(lastNameItem)
                .invoke { inputView.typeText(LAST_NAME) }
            listView.scrollTo(middleNameItem)
                .invoke { inputView.typeText(MIDDLE_NAME) }
            listView.scrollTo(phoneItem)
                .invoke { inputView.typeText(PHONE) }
            listView.scrollTo(emailItem)
                .invoke { inputView.typeText(EMAIL) }
            listView.scrollTo(postponeButton).click()
        }

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .invoke {
                    subtitleView.isTextEquals(R.string.yandex_rent_flat_owner_status_draft)
                }
        }
    }

    @Test
    fun showErrors() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerRequiredFeaturesError()
            registerNoRequiredFeatures()
            registerDraftError()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/newUser.json")
            registerUserProfile("user/newUser.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))

            registerUpdateDraftError()

            registerUpdateDraft()
            registerRequestSmsCodeValidationErrors()

            registerUpdateDraft()
            registerRequestSmsCodeConflictError()

            registerUpdateDraft()
            registerRequestSmsCode()

            registerSubmitSmsCodeValidationError()
            registerSubmitSmsCodeConflictError()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(fullscreenErrorItem) }
            isContentStateMatches(getTestRelatedFilePath("loadingError"))
            retryButton.click()

            waitUntil { listView.contains(fullscreenErrorItem) }
            isContentStateMatches(getTestRelatedFilePath("loadingError"))
            retryButton.click()

            postponeButton.waitUntil { listView.contains(this) }.click()

            waitUntil {
                toastView(getResourceString(R.string.error_update_yandex_rent_form))
                    .isCompletelyDisplayed()
            }
            listView.scrollTo(submitButton).click()

            waitUntil { listView.contains(fieldErrorItem("Укажите адрес")) }
            isContentStateMatches(getTestRelatedFilePath("editingValidationErrors"))
            listView.scrollTo(submitButton).click()

            waitUntil { toastView("Превышен лимит отправки смс").isCompletelyDisplayed() }
            listView.scrollTo(phoneItem)
                .invoke { inputView.typeText(PHONE) }
            listView.scrollTo(submitButton).click()

            listView.waitUntil {
                contains(smsCodeItem)
                contains(resendSmsButton)
            }
            closeKeyboard()
            listView.scrollTo(confirmButton).click()

            waitUntil { listView.contains(fieldErrorItem("Код неверный")) }
            closeKeyboard()
            isContentStateMatches(getTestRelatedFilePath("codeValidationError"))
            listView.scrollTo(confirmButton).click()

            waitUntil { toastView("Код недействителен").isCompletelyDisplayed() }
        }
    }

    @Test
    fun showUpdateDialogWhenUnknownValidationError() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo(fullRentUserData())
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))

            registerUpdateDraft()
            registerRequestSmsCodeUnknownValidationError()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        registerMarketIntent()

        onScreen<RentFlatFormScreen> {
            submitButton.waitUntil { listView.contains(this) }
                .click()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches("dialog/needAppUpdateDialog")
                confirmButton.click()
                intended(matchesMarketIntent())
            }

            contentView.doesNotExist()
        }
    }

    @Test
    fun showNeedUpdateWhenUnknownRequiredFeature() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerUnknownRequiredFeatures()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        registerMarketIntent()

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(needUpdateItem) }
            isContentStateMatches(getTestRelatedFilePath("content"))
            updateButton.click()
            intended(matchesMarketIntent())
        }
    }

    @Test
    fun openFormFromDeeplinkWhenAuthorized() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/newUser.json")
            registerUserProfile("user/newUser.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
        }

        authorizationRule.setUserAuthorized()
        DeepLinkIntentCommand.execute("https://arenda.yandex.ru/lk/sdat-kvartiry")

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun openFormFromDeeplinkWithRealtyScheme() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/newUser.json")
            registerUserProfile("user/newUser.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
        }

        authorizationRule.setUserAuthorized()
        DeepLinkIntentCommand.execute(
            "yandexrealty://arenda.yandex.ru/lk/sdat-kvartiry/?adj_t=4inllqd_xgufl8n"
        )

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(addressItem) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun openFormFromDeeplinkWhenUnauthorized() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats()
            registerNoRequiredFeatures()
            registerDraft()
            registerRentUserInfo()
            registerUserProfile("user/newUser.json")
            registerUserProfile("user/newUser.json")
            registerNotificationsConfiguration(notificationsConfiguration(enabled = true))
        }

        Intents.init()
        authorizationRule.registerAuthorizationIntent()
        DeepLinkIntentCommand.execute("https://arenda.yandex.ru/lk/sdat-kvartiry")

        onScreen<RentFlatFormScreen> {
            waitUntil { listView.contains(needLogInItem) }
            isContentStateMatches(getTestRelatedFilePath("unauthorized"))
            logInButton.click()

            waitUntil { listView.contains(addressItem) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
        }
    }

    private fun fullRentUserData(): JsonObject {
        return jsonObject {
            "person" to jsonObject {
                "name" to FIRST_NAME
                "surname" to LAST_NAME
                "patronymic" to MIDDLE_NAME
            }
            "phone" to PHONE
            "email" to EMAIL
        }
    }

    private fun fullDraftData(address: String): JsonObject {
        return jsonObject {
            "status" to "DRAFT"
            "address" to jsonObject {
                "address" to address
                "flatNumber" to FLAT_NUMBER
            }
            "person" to jsonObject {
                "name" to FIRST_NAME
                "surname" to LAST_NAME
                "patronymic" to MIDDLE_NAME
            }
            "phone" to PHONE
            "email" to EMAIL
        }
    }

    private fun JsonObjectBuilder.draftWithFullSubmitBody() {
        "address" to FULL_AURORA_ADDRESS
        "flatNumber" to FLAT_NUMBER
        "person" to jsonObject {
            "name" to FIRST_NAME
            "surname" to LAST_NAME
            "patronymic" to MIDDLE_NAME
        }
        "phone" to PHONE
        "email" to EMAIL
    }

    private fun JsonObjectBuilder.draftWithAddressSubmitBody() {
        "address" to FULL_AURORA_ADDRESS
        "person" to jsonObject { }
    }

    private fun DispatcherRegistry.registerNoRequiredFeatures() {
        register(
            request {
                method("GET")
                path("1.0/device/requiredFeature")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "rentFlatDraft" to jsonArrayOf()
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerRequiredFeaturesError() {
        register(
            request {
                method("GET")
                path("1.0/device/requiredFeature")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerUnknownRequiredFeatures() {
        register(
            request {
                method("GET")
                path("1.0/device/requiredFeature")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "rentFlatDraft" to jsonArrayOf("NEW_FEATURE")
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerDraft(draft: JsonObject? = null) {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/draft")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        draft?.let { "flat" to draft }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerDraftError() {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/draft")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerUserProfile(
        responseFileName: String
    ) {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerRentUserInfo(user: JsonObject = JsonObject()) {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "user" to user
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerGetNearAuroraAddress() {
        register(
            request {
                method("GET")
                path("1.0/addressGeocoder.json")
                queryParam("latitude", NEAR_AURORA_LATITUDE.toString())
                queryParam("longitude", NEAR_AURORA_LONGITUDE.toString())
                queryParam("category", "APARTMENT")
            },
            response {
                assetBody("geocoderAddressAurora.json")
            }
        )
    }

    private fun DispatcherRegistry.registerGetAuroraAddress() {
        register(
            request {
                method("GET")
                path("1.0/addressGeocoder.json")
                queryParam("latitude", AURORA_LATITUDE.toString())
                queryParam("longitude", AURORA_LONGITUDE.toString())
                queryParam("category", "APARTMENT")
            },
            response {
                assetBody("geocoderAddressAurora.json")
            }
        )
    }

    private fun DispatcherRegistry.registerResolveGeoByAddress(
        address: String,
        responseFileName: String
    ) {
        register(
            request {
                method("GET")
                path("1.0/addressGeocoder.json")
                queryParam("address", Uri.encode(address, "UTF-8"))
                queryParam("category", "APARTMENT")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerUpdateDraft(
        draftBody: (JsonObjectBuilder.() -> Unit)? = null
    ) {
        register(
            request {
                method("PUT")
                path("2.0/rent/user/me/flats/draft")
                draftBody?.let { jsonBody(it) }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerUpdateDraftError() {
        register(
            request {
                method("PUT")
                path("2.0/rent/user/me/flats/draft")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerRequestSmsCode() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/draft/confirmation-code/request")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "sentSmsInfo" to jsonObject {
                            "codeLength" to 5
                            "requestId" to REQUEST_ID
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerRequestSmsCodeValidationErrors() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/draft/confirmation-code/request")
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "VALIDATION_ERROR"
                        "data" to jsonObject {
                            "validationErrors" to jsonArrayOf(
                                jsonObject {
                                    "parameter" to "/address/address"
                                    "localizedDescription" to "Укажите адрес"
                                    "code" to "EMPTY_ADDRESS"
                                },
                                jsonObject {
                                    "parameter" to "/address/flatNumber"
                                    "localizedDescription" to "Укажите квартиру"
                                    "code" to "EMPTY_FLAT_NUMBER"
                                },
                                jsonObject {
                                    "parameter" to "/person/name"
                                    "localizedDescription" to "Укажите имя"
                                    "code" to "EMPTY_PERSON_NAME"
                                },
                                jsonObject {
                                    "parameter" to "/person/surname"
                                    "localizedDescription" to "Укажите фамилию"
                                    "code" to "EMPTY_PERSON_NAME"
                                },
                                jsonObject {
                                    "parameter" to "/person/patronymic"
                                    "localizedDescription" to "Укажите отчество"
                                    "code" to "EMPTY_PERSON_NAME"
                                },
                                jsonObject {
                                    "parameter" to "/phone"
                                    "localizedDescription" to "Укажите телефон"
                                    "code" to "EMPTY_PHONE"
                                },
                                jsonObject {
                                    "parameter" to "/email"
                                    "localizedDescription" to "Укажите почту"
                                    "code" to "EMPTY_EMAIL"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerRequestSmsCodeUnknownValidationError() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/draft/confirmation-code/request")
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "VALIDATION_ERROR"
                        "data" to jsonObject {
                            "validationErrors" to jsonArrayOf(
                                jsonObject {
                                    "parameter" to "/address/newField"
                                    "localizedDescription" to "Заполните новое поле"
                                    "code" to "EMPTY_NEW_FIELD"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerRequestSmsCodeConflictError() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/draft/confirmation-code/request")
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to "Превышен лимит отправки смс"
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerSubmitSmsCode() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/draft/confirmation-code/submit")
                jsonBody {
                    "confirmSmsInfo" to jsonObject {
                        "code" to SMS_CODE
                        "requestId" to REQUEST_ID
                    }
                }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerSubmitSmsCodeValidationError() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/draft/confirmation-code/submit")
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "VALIDATION_ERROR"
                        "data" to jsonObject {
                            "validationErrors" to jsonArrayOf(
                                jsonObject {
                                    "parameter" to "/confirmSmsInfo/code"
                                    "localizedDescription" to "Код неверный"
                                    "code" to "INVALID_CODE"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerSubmitSmsCodeConflictError() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/draft/confirmation-code/submit")
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to "Код недействителен"
                    }
                }
            }
        )
    }

    private fun notificationsConfiguration(enabled: Boolean): JsonObject {
        return jsonObject {
            "id" to "rent_marketing_campaigns"
            "methods" to jsonArrayOf(
                jsonObject {
                    "deliveryType" to "DELIVERY_TYPE_EMAIL"
                    "enabled" to enabled
                },
                jsonObject {
                    "deliveryType" to "DELIVERY_TYPE_SMS"
                    "enabled" to enabled
                }
            )
        }
    }

    private fun DispatcherRegistry.registerNotificationsConfiguration(
        configuration: JsonObject
    ) {
        register(
            request {
                method("GET")
                path("1.0/user/notifications")
                queryParam("deliveryTypes", "sms")
                queryParam("deliveryTypes", "email")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "values" to jsonArrayOf(configuration)
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerSubmitNotificationsConfiguration(
        body: JsonObject? = null
    ): ExpectedRequest {
        return register(
            request {
                method("PATCH")
                path("1.0/user/notification")
                body?.let { jsonBody(it) }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerSubmitNotificationsConfigurationError() {
        register(
            request {
                method("PATCH")
                path("1.0/user/notification")
            },
            error()
        )
    }

    private companion object {

        const val NEAR_AURORA_LATITUDE = 55.734655
        const val NEAR_AURORA_LONGITUDE = 37.642313
        const val AURORA_LATITUDE = 55.73552
        const val AURORA_LONGITUDE = 37.642475
        const val SHORT_AURORA_ADDRESS = "Садовническая улица, 82с2"
        const val AURORA_ADDRESS_WITHOUT_COUNTRY = "Москва, Садовническая улица, 82с2"
        const val FULL_AURORA_ADDRESS = "Россия, Москва, Садовническая улица, 82с2"
        const val NOT_RESOLVING_ADDRESS = "Неизвестный город, д. 12"
        const val FLAT_NUMBER = "386"
        const val FIRST_NAME = "John"
        const val LAST_NAME = "Smith"
        const val MIDDLE_NAME = "Great"
        const val PHONE = "+79112223344"
        const val EMAIL = "john@gmail.com"
        const val SMS_CODE = "48122"
        const val REQUEST_ID = "8ddd3c24c66d"
    }
}
