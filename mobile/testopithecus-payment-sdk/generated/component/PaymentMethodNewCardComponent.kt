// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM component/payment-method-new-card-component.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class PaymentMethodNewCardComponent: MBTComponent {
    open override fun assertMatches(model: App, application: App): Unit {
        val appFillNewCard = FillNewCardFeature.`get`.forceCast(application)
        assertTrue(appFillNewCard.waitForNewCardScreen(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT), "Payment new card screen was not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} seconds")
        val modelScreenTitle = PaymentScreenTitleFeature.`get`.castIfSupported(model)
        val appScreenTitle = PaymentScreenTitleFeature.`get`.castIfSupported(application)
        if (modelScreenTitle != null && appScreenTitle != null) {
            val modelTitle = modelScreenTitle.getTitle()
            val appTitle = appScreenTitle.getTitle()
            assertStringEquals(modelTitle, appTitle, "Screen title mismatch")
        }
        val modelFillNewCard = FillNewCardFeature.`get`.castIfSupported(model)
        val newCardMode = modelFillNewCard!!.getNewCardMode()
        val modelPaymentDetails = ReadPaymentDetailsFeature.`get`.castIfSupported(model)
        if (modelFillNewCard != null) {
            val modelCardNumber = modelFillNewCard.getFieldValue(NewCardField.cardNumber)
            val appCardNumber = appFillNewCard.getFieldValue(NewCardField.cardNumber)
            assertStringEquals(modelCardNumber, appCardNumber, "Card number is incorrect")
            val modelExpirationDate = modelFillNewCard.getFieldValue(NewCardField.expirationDate)
            val appExpirationDate = appFillNewCard.getFieldValue(NewCardField.expirationDate)
            assertStringEquals(modelExpirationDate, appExpirationDate, "Expiration date is incorrect")
            val modelCVV = formatCvv(modelFillNewCard.getFieldValue(NewCardField.cvv))
            val appCVV = formatCvv(appFillNewCard.getFieldValue(NewCardField.cvv))
            assertStringEquals(modelCVV, appCVV, "CVV is incorrect")
            if (newCardMode != NewCardMode.bind) {
                val modelBackButton = modelFillNewCard.isBackButtonShown()
                val appBackButton = appFillNewCard.isBackButtonShown()
                assertBooleanEquals(modelBackButton, appBackButton, "Back button visibility state is incorrect")
                if (modelPaymentDetails!!.getAuthorizationMode() == AuthorizationMode.authorized && newCardMode == NewCardMode.pay) {
                    val modelSaveCardCheckbox = modelFillNewCard.isSaveCardCheckboxEnabled()
                    val appSaveCardCheckbox = appFillNewCard.isSaveCardCheckboxEnabled()
                    assertBooleanEquals(modelSaveCardCheckbox, appSaveCardCheckbox, "Save card checkbox value is incorrect")
                }
            }
        }
        val modelLicenseAgreementFeature = LicenseAgreementFeature.`get`.castIfSupported(model)
        val appLicenseAgreementFeature = LicenseAgreementFeature.`get`.castIfSupported(application)
        if (modelLicenseAgreementFeature != null && appLicenseAgreementFeature != null) {
            val modelLicenseAgreementShown = modelLicenseAgreementFeature.isLicenseAgreementShown()
            val appLicenseAgreementShown = appLicenseAgreementFeature.isLicenseAgreementShown()
            assertBooleanEquals(modelLicenseAgreementShown, appLicenseAgreementShown, "Incorrect License agreement shown status")
            val modelLicenseAgreement = modelLicenseAgreementFeature.getLicenseAgreement()
            val appLicenseAgreement = appLicenseAgreementFeature.getLicenseAgreement()
            assertStringEquals(modelLicenseAgreement, appLicenseAgreement, "Incorrect License agreement text")
        }
        val modelKeyboard = KeyboardFeature.`get`.castIfSupported(model)
        val appKeyboard = KeyboardFeature.`get`.castIfSupported(application)
        if (modelKeyboard != null && appKeyboard != null) {
            val modelNumKeyboardShown = modelKeyboard.isNumericKeyboardShown()
            val appNumKeyboardShown = appKeyboard.isNumericKeyboardShown()
            assertBooleanEquals(modelNumKeyboardShown, appNumKeyboardShown, "Numeric keyboard status is incorrect")
            val modelAlphKeyboardShown = modelKeyboard.isAlphabeticalKeyboardShown()
            val appAlphKeyboardShown = appKeyboard.isAlphabeticalKeyboardShown()
            assertBooleanEquals(modelAlphKeyboardShown, appAlphKeyboardShown, "Alphabetical keyboard status is incorrect")
        }
        val modelFieldsValidator = NewCardFieldsValidatorFeature.`get`.castIfSupported(model)
        val appFieldsValidator = NewCardFieldsValidatorFeature.`get`.castIfSupported(application)
        if (modelFieldsValidator != null && appFieldsValidator != null) {
            val modelCardNumberErrorText = modelFieldsValidator.getCardNumberErrorText()
            val appCardNumberErrorText = appFieldsValidator.getCardNumberErrorText()
            assertStringEquals(modelCardNumberErrorText, appCardNumberErrorText, "Card number error message is incorrect")
            val modelCvvErrorText = modelFieldsValidator.getCvvErrorText()
            val appCvvErrorText = appFieldsValidator.getCvvErrorText()
            assertStringEquals(modelCvvErrorText, appCvvErrorText, "Cvv error message is incorrect")
            val modelExpirationDateErrorText = modelFieldsValidator.getExpirationDateErrorText()
            val appExpirationDateErrorText = appFieldsValidator.getExpirationDateErrorText()
            assertStringEquals(modelExpirationDateErrorText, appExpirationDateErrorText, "Expiration date error message is incorrect")
        }
        if (modelPaymentDetails!!.isPersonalInfoShown() && newCardMode != NewCardMode.bind) {
            val modelPersonalInfoFields = PersonalInformationFeature.`get`.castIfSupported(model)
            val appPersonalInfoFields = PersonalInformationFeature.`get`.castIfSupported(application)
            if (modelPersonalInfoFields != null && appPersonalInfoFields != null) {
                val modelName = modelPersonalInfoFields.getFieldValue(PersonalInformationField.firstName)
                val appName = appPersonalInfoFields.getFieldValue(PersonalInformationField.firstName)
                assertStringEquals(modelName, appName, "Name is incorrect")
                val modelLastName = modelPersonalInfoFields.getFieldValue(PersonalInformationField.lastName)
                val appLastName = appPersonalInfoFields.getFieldValue(PersonalInformationField.lastName)
                assertStringEquals(modelLastName, appLastName, "Last name is incorrect")
                val modelEmail = modelPersonalInfoFields.getFieldValue(PersonalInformationField.email)
                val appEmail = appPersonalInfoFields.getFieldValue(PersonalInformationField.email)
                assertStringEquals(modelEmail, appEmail, "Email is incorrect")
                val modelPhoneNumber = modelPersonalInfoFields.getFieldValue(PersonalInformationField.phoneNumber)
                val appPhoneNumber = appPersonalInfoFields.getFieldValue(PersonalInformationField.phoneNumber)
                assertStringEquals(modelPhoneNumber, appPhoneNumber, "Phone number is incorrect")
            }
            val modelPersInfoFieldsValidator = PersonalInformationFieldsValidatorFeature.`get`.castIfSupported(model)
            val appPersInfoFieldsValidator = PersonalInformationFieldsValidatorFeature.`get`.castIfSupported(application)
            if (modelPersInfoFieldsValidator != null && appPersInfoFieldsValidator != null) {
                val modelEmailErrorText = modelPersInfoFieldsValidator.getEmailErrorText()
                val appEmailErrorText = appPersInfoFieldsValidator.getEmailErrorText()
                assertStringEquals(modelEmailErrorText, appEmailErrorText, "Email error message is incorrect")
                val modelPhoneNumberErrorText = modelPersInfoFieldsValidator.getPhoneNumberErrorText()
                val appPhoneNumberErrorText = appPersInfoFieldsValidator.getPhoneNumberErrorText()
                assertStringEquals(modelPhoneNumberErrorText, appPhoneNumberErrorText, "Phone number error message is incorrect")
            }
        }
        val modelPaymentButton = PaymentButtonFeature.`get`.castIfSupported(model)
        val appPaymentButton = PaymentButtonFeature.`get`.castIfSupported(application)
        if (modelPaymentButton != null && appPaymentButton != null) {
            val modelPaymentButtonEnabled = modelPaymentButton.isEnabled()
            val appPaymentButtonEnabled = appPaymentButton.isEnabled()
            assertBooleanEquals(modelPaymentButtonEnabled, appPaymentButtonEnabled, "Payment button status is incorrect")
            val modelPaymentButtonText = modelPaymentButton.getButtonText()
            val appPaymentButtonText = appPaymentButton.getButtonText()
            assertStringEquals(modelPaymentButtonText, appPaymentButtonText, "Pay button text mismatch")
        }
    }

    open override fun getComponentType(): MBTComponentType {
        return PaymentMethodNewCardComponent.type
    }

    open override fun tostring(): String {
        return this.getComponentType()
    }

    companion object {
        @JvmStatic val type: String = "PaymentMethodNewCardComponent"
    }
}

