import { Throwing } from '../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../testopithecus-common/code/utils/assert'
import { LicenseAgreementFeature } from '../feature/license-agreement-feature'
import {
  ApplePayFeature,
  GooglePayFeature,
  PaymentMethodsListFeature,
  SBPFeature,
} from '../feature/payment-methods-list-feature'
import { PaymentScreenTitleFeature } from '../feature/payment-screen-title-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class PaymentMethodSelectionComponent implements MBTComponent {
  public static readonly type: string = 'PaymentSDK start payment screen'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appMethodsListFeature = PaymentMethodsListFeature.get.forceCast(application)
    assertTrue(
      appMethodsListFeature.waitForPaymentMethods(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
      `Method selection screen was not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} seconds`,
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
    // TODO: fix PAYMENTSDK-337
    // const modelUnbindCard = UnbindCardFeature.get.castIfSupported(model)
    // const appUnbindCard = UnbindCardFeature.get.castIfSupported(application)
    //
    // if (modelUnbindCard !== null && appUnbindCard !== null) {
    //   const modelEditButton = modelUnbindCard.isEditButtonShown()
    //   const appEditButton = appUnbindCard.isEditButtonShown()
    //
    //   assertBooleanEquals(modelEditButton, appEditButton, 'Incorrect Edit button status')
    // }

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
  }

  public getComponentType(): MBTComponentType {
    return PaymentMethodSelectionComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
