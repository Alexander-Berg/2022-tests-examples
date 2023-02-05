import { Throwing } from '../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertBooleanEquals, assertStringEquals, assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { FillNewCardFeature } from '../feature/fill-new-card-feature'
import { KeyboardFeature } from '../feature/keyboard-feature'
import { LicenseAgreementFeature } from '../feature/license-agreement-feature'
import { NewCardFieldsValidatorFeature } from '../feature/new-card-fields-validator-feature'
import { PaymentButtonFeature } from '../feature/payment-button-feature'
import { ReadPaymentDetailsFeature } from '../feature/payment-details-feature'
import { PaymentScreenTitleFeature } from '../feature/payment-screen-title-feature'
import { PersonalInformationFeature } from '../feature/personal-information-feature'
import { PersonalInformationFieldsValidatorFeature } from '../feature/personal-information-fields-validator-feature'
import { NewCardField, NewCardMode } from '../model/fill-new-card-model'
import { PersonalInformationField } from '../model/personal-information-model'
import { AuthorizationMode } from '../sample/sample-configuration'
import { formatCvv, PaymentSdkConstants } from '../utils/payment-utils'

export class PaymentMethodNewCardComponent implements MBTComponent {
  public static readonly type: string = 'PaymentMethodNewCardComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appFillNewCard = FillNewCardFeature.get.forceCast(application)
    assertTrue(
      appFillNewCard.waitForNewCardScreen(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
      `Payment new card screen was not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} seconds`,
    )

    const modelScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(model)
    const appScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(application)
    if (modelScreenTitle !== null && appScreenTitle !== null) {
      const modelTitle = modelScreenTitle.getTitle()
      const appTitle = appScreenTitle.getTitle()
      assertStringEquals(modelTitle, appTitle, 'Screen title mismatch')
    }

    const modelFillNewCard = FillNewCardFeature.get.castIfSupported(model)
    const newCardMode = modelFillNewCard!.getNewCardMode()

    const modelPaymentDetails = ReadPaymentDetailsFeature.get.castIfSupported(model)
    if (modelFillNewCard !== null) {
      const modelCardNumber = modelFillNewCard.getFieldValue(NewCardField.cardNumber)
      const appCardNumber = appFillNewCard.getFieldValue(NewCardField.cardNumber)

      assertStringEquals(modelCardNumber, appCardNumber, 'Card number is incorrect')

      const modelExpirationDate = modelFillNewCard.getFieldValue(NewCardField.expirationDate)
      const appExpirationDate = appFillNewCard.getFieldValue(NewCardField.expirationDate)

      assertStringEquals(modelExpirationDate, appExpirationDate, 'Expiration date is incorrect')

      const modelCVV = formatCvv(modelFillNewCard.getFieldValue(NewCardField.cvv))
      const appCVV = formatCvv(appFillNewCard.getFieldValue(NewCardField.cvv))

      assertStringEquals(modelCVV, appCVV, 'CVV is incorrect')

      if (newCardMode !== NewCardMode.bind) {
        const modelBackButton = modelFillNewCard.isBackButtonShown()
        const appBackButton = appFillNewCard.isBackButtonShown()

        assertBooleanEquals(modelBackButton, appBackButton, 'Back button visibility state is incorrect')

        if (
          modelPaymentDetails!.getAuthorizationMode() === AuthorizationMode.authorized &&
          newCardMode === NewCardMode.pay
        ) {
          const modelSaveCardCheckbox = modelFillNewCard.isSaveCardCheckboxEnabled()
          const appSaveCardCheckbox = appFillNewCard.isSaveCardCheckboxEnabled()

          assertBooleanEquals(modelSaveCardCheckbox, appSaveCardCheckbox, 'Save card checkbox value is incorrect')
        }
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

    const modelFieldsValidator = NewCardFieldsValidatorFeature.get.castIfSupported(model)
    const appFieldsValidator = NewCardFieldsValidatorFeature.get.castIfSupported(application)

    if (modelFieldsValidator !== null && appFieldsValidator !== null) {
      const modelCardNumberErrorText = modelFieldsValidator.getCardNumberErrorText()
      const appCardNumberErrorText = appFieldsValidator.getCardNumberErrorText()

      assertStringEquals(modelCardNumberErrorText, appCardNumberErrorText, 'Card number error message is incorrect')

      const modelCvvErrorText = modelFieldsValidator.getCvvErrorText()
      const appCvvErrorText = appFieldsValidator.getCvvErrorText()

      assertStringEquals(modelCvvErrorText, appCvvErrorText, 'Cvv error message is incorrect')

      const modelExpirationDateErrorText = modelFieldsValidator.getExpirationDateErrorText()
      const appExpirationDateErrorText = appFieldsValidator.getExpirationDateErrorText()

      assertStringEquals(
        modelExpirationDateErrorText,
        appExpirationDateErrorText,
        'Expiration date error message is incorrect',
      )
    }

    if (modelPaymentDetails!.isPersonalInfoShown() && newCardMode !== NewCardMode.bind) {
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

    const modelPaymentButton = PaymentButtonFeature.get.castIfSupported(model)
    const appPaymentButton = PaymentButtonFeature.get.castIfSupported(application)

    if (modelPaymentButton !== null && appPaymentButton !== null) {
      const modelPaymentButtonEnabled = modelPaymentButton.isEnabled()
      const appPaymentButtonEnabled = appPaymentButton.isEnabled()

      assertBooleanEquals(modelPaymentButtonEnabled, appPaymentButtonEnabled, 'Payment button status is incorrect')

      const modelPaymentButtonText = modelPaymentButton.getButtonText()
      const appPaymentButtonText = appPaymentButton.getButtonText()

      assertStringEquals(modelPaymentButtonText, appPaymentButtonText, 'Pay button text mismatch')
    }
  }

  public getComponentType(): MBTComponentType {
    return PaymentMethodNewCardComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
