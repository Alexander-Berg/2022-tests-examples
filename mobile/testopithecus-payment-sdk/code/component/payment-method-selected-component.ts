import { Throwing } from '../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../testopithecus-common/code/utils/assert'
import { KeyboardFeature } from '../feature/keyboard-feature'
import { LicenseAgreementFeature } from '../feature/license-agreement-feature'
import { PaymentButtonFeature } from '../feature/payment-button-feature'
import { ReadPaymentDetailsFeature } from '../feature/payment-details-feature'
import {
  ApplePayFeature,
  GooglePayFeature,
  MethodsListMode,
  PaymentMethodsListFeature,
  SBPFeature,
} from '../feature/payment-methods-list-feature'
import { PaymentScreenTitleFeature } from '../feature/payment-screen-title-feature'
import { PersonalInformationFeature } from '../feature/personal-information-feature'
import { PersonalInformationFieldsValidatorFeature } from '../feature/personal-information-fields-validator-feature'
import { UnbindCardFeature } from '../feature/unbind-card-feature'
import { PersonalInformationField } from '../model/personal-information-model'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class PaymentMethodSelectedComponent implements MBTComponent {
  public static readonly type: string = 'PaymentSDK payment methods with selected option'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appMethodsListFeature = PaymentMethodsListFeature.get.forceCast(application)
    assertTrue(
      appMethodsListFeature.waitForPaymentMethods(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
      `Payment methods screen was not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} seconds`,
    )

    const modelScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(model)
    const appScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(application)

    if (modelScreenTitle !== null && appScreenTitle !== null) {
      const modelTitle = modelScreenTitle.getTitle()
      const appTitle = appScreenTitle.getTitle()
      assertStringEquals(modelTitle, appTitle, 'Screen title mismatch')
    }

    const modelMethodsListFeature = PaymentMethodsListFeature.get.castIfSupported(model)

    if (modelMethodsListFeature !== null) {
      const modelMethods = modelMethodsListFeature.getMethods()
      const appMethods = appMethodsListFeature.getMethods()

      assertInt32Equals(modelMethods.length, appMethods.length, 'Incorrect number of payment methods')

      for (const modelMethod of modelMethods) {
        assertTrue(appMethods.includes(modelMethod), 'Incorrect payment method')
      }
    }

    const modelUnbindCardFeature = UnbindCardFeature.get.castIfSupported(model)
    const appUnbindCardFeature = UnbindCardFeature.get.castIfSupported(application)

    if (modelUnbindCardFeature !== null && appUnbindCardFeature !== null) {
      const modelEditButton = modelUnbindCardFeature.isEditButtonShown()
      const appEditButton = appUnbindCardFeature.isEditButtonShown()

      assertBooleanEquals(modelEditButton, appEditButton, 'Incorrect Edit button status')
    }

    const modelApplePayFeature = ApplePayFeature.get.castIfSupported(model)
    const appApplePayFeature = ApplePayFeature.get.castIfSupported(application)

    if (modelApplePayFeature !== null && appApplePayFeature !== null) {
      const modelApplePayAvailable = modelApplePayFeature.isAvailable()
      const appApplePayAvailable = appApplePayFeature.isAvailable()

      assertBooleanEquals(modelApplePayAvailable, appApplePayAvailable, 'Incorrect ApplePay availability status')
    }

    const modelGooglePayFeature = GooglePayFeature.get.castIfSupported(model)
    const appGooglePayFeature = GooglePayFeature.get.castIfSupported(application)

    if (modelGooglePayFeature !== null && appGooglePayFeature !== null) {
      const modelGooglePayAvailable = modelGooglePayFeature.isAvailable()
      const appGooglePayAvailable = appGooglePayFeature.isAvailable()

      assertBooleanEquals(modelGooglePayAvailable, appGooglePayAvailable, 'Incorrect GooglePay availability status')
    }

    const modelSBPFeature = SBPFeature.get.castIfSupported(model)
    const appSBPFeature = SBPFeature.get.castIfSupported(application)

    if (modelSBPFeature !== null && appSBPFeature !== null) {
      const modelSBPAvailable = modelSBPFeature.isAvailable()
      const appSBPAvailable = appSBPFeature.isAvailable()

      assertBooleanEquals(modelSBPAvailable, appSBPAvailable, 'Incorrect SBP availability status')
    }

    const modelKeyboard = KeyboardFeature.get.castIfSupported(model)
    const appKeyboard = KeyboardFeature.get.castIfSupported(application)

    if (modelKeyboard !== null && appKeyboard !== null) {
      const modelNumKeyboardShown = modelKeyboard.isNumericKeyboardShown()
      const appNumKeyboardShown = appKeyboard.isNumericKeyboardShown()

      assertBooleanEquals(modelNumKeyboardShown, appNumKeyboardShown, 'Numeric keyboard status is incorrect')

      const modelAlphKeyboardShown = modelKeyboard.isAlphabeticalKeyboardShown()
      const appAlphKeyboardShown = appKeyboard.isAlphabeticalKeyboardShown()

      assertBooleanEquals(modelAlphKeyboardShown, appAlphKeyboardShown, 'Alphabetical keyboard status is incorrect')
    }

    const modelPaymentButtonFeature = PaymentButtonFeature.get.castIfSupported(model)
    const appPaymentButtonFeature = PaymentButtonFeature.get.castIfSupported(application)

    if (modelPaymentButtonFeature !== null && appPaymentButtonFeature !== null) {
      const isModelPayButtonEnabled = modelPaymentButtonFeature.isEnabled()
      const isAppPayButtonEnabled = appPaymentButtonFeature.isEnabled()
      assertBooleanEquals(isModelPayButtonEnabled, isAppPayButtonEnabled, 'Payment button enabled status is incorrect')

      const modelPayButtonText = modelPaymentButtonFeature.getButtonText()
      const appPayButtonText = appPaymentButtonFeature.getButtonText()
      assertStringEquals(modelPayButtonText, appPayButtonText, 'Pay button text mismatch')

      if (modelMethodsListFeature!.getMethodsListMode() !== MethodsListMode.preselect) {
        const modelPaymentButtonLabelText = modelPaymentButtonFeature.getLabelText()
        const appPaymentButtonLabelText = appPaymentButtonFeature.getLabelText()
        assertStringEquals(modelPaymentButtonLabelText, appPaymentButtonLabelText, 'Pay button label text mismatch')
      }
    }

    const modelLicenseAgreementFeature = LicenseAgreementFeature.get.castIfSupported(model)
    const appLicenseAgreementFeature = LicenseAgreementFeature.get.castIfSupported(application)

    if (modelLicenseAgreementFeature !== null && appLicenseAgreementFeature !== null) {
      const modelLicenseAgreementShown = modelLicenseAgreementFeature.isLicenseAgreementShown()
      const appLicenseAgreementShown = appLicenseAgreementFeature.isLicenseAgreementShown()

      assertBooleanEquals(
        modelLicenseAgreementShown,
        appLicenseAgreementShown,
        'Incorrect License agreement shown status',
      )

      const modelLicenseAgreement = modelLicenseAgreementFeature.getLicenseAgreement()
      const appLicenseAgreement = appLicenseAgreementFeature.getLicenseAgreement()

      assertStringEquals(modelLicenseAgreement, appLicenseAgreement, 'Incorrect License agreement text')
    }

    const modelPaymentDetails = ReadPaymentDetailsFeature.get.castIfSupported(model)

    if (modelPaymentDetails!.isPersonalInfoShown()) {
      const modelPersonalInfoFields = PersonalInformationFeature.get.castIfSupported(model)
      const appPersonalInfoFields = PersonalInformationFeature.get.castIfSupported(application)

      if (modelPersonalInfoFields !== null && appPersonalInfoFields !== null) {
        const modelName = modelPersonalInfoFields.getFieldValue(PersonalInformationField.firstName)
        const appName = appPersonalInfoFields.getFieldValue(PersonalInformationField.firstName)

        assertStringEquals(modelName, appName, 'Name is incorrect')

        const modelLastName = modelPersonalInfoFields.getFieldValue(PersonalInformationField.lastName)
        const appLastName = appPersonalInfoFields.getFieldValue(PersonalInformationField.lastName)

        assertStringEquals(modelLastName, appLastName, 'Last name is incorrect')

        const modelEmail = modelPersonalInfoFields.getFieldValue(PersonalInformationField.email)
        const appEmail = appPersonalInfoFields.getFieldValue(PersonalInformationField.email)

        assertStringEquals(modelEmail, appEmail, 'Email is incorrect')

        const modelPhoneNumber = modelPersonalInfoFields.getFieldValue(PersonalInformationField.phoneNumber)
        const appPhoneNumber = appPersonalInfoFields.getFieldValue(PersonalInformationField.phoneNumber)

        assertStringEquals(modelPhoneNumber, appPhoneNumber, 'Phone number is incorrect')
      }

      const modelPersInfoFieldsValidator = PersonalInformationFieldsValidatorFeature.get.castIfSupported(model)
      const appPersInfoFieldsValidator = PersonalInformationFieldsValidatorFeature.get.castIfSupported(application)

      if (modelPersInfoFieldsValidator !== null && appPersInfoFieldsValidator !== null) {
        if (modelPaymentDetails!.isPersonalInfoShown()) {
          const modelEmailErrorText = modelPersInfoFieldsValidator.getEmailErrorText()
          const appEmailErrorText = appPersInfoFieldsValidator.getEmailErrorText()

          assertStringEquals(modelEmailErrorText, appEmailErrorText, 'Email error message is incorrect')

          const modelPhoneNumberErrorText = modelPersInfoFieldsValidator.getPhoneNumberErrorText()
          const appPhoneNumberErrorText = appPersInfoFieldsValidator.getPhoneNumberErrorText()

          assertStringEquals(
            modelPhoneNumberErrorText,
            appPhoneNumberErrorText,
            'Phone number error message is incorrect',
          )
        }
      }
    }
  }

  public getComponentType(): MBTComponentType {
    return PaymentMethodSelectedComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
