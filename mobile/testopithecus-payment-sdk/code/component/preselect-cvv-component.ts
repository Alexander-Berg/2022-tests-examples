import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertBooleanEquals, assertStringEquals, assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../common/ys'
import { LicenseAgreementFeature } from '../feature/license-agreement-feature'
import { PreselectCvvFeature } from '../feature/payment-methods-list-feature'
import { PaymentScreenTitleFeature } from '../feature/payment-screen-title-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class PreselectCvvComponent implements MBTComponent {
  public static readonly type: string = 'PreselectCvvComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appPreselectCvvFeature = PreselectCvvFeature.get.forceCast(application)
    assertTrue(
      appPreselectCvvFeature.waitForPreselectCvv(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
      `Preselect cvv screen was not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} seconds`,
    )

    const modelScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(model)
    const appScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(application)

    if (modelScreenTitle !== null && appScreenTitle !== null) {
      const modelTitle = modelScreenTitle.getTitle()
      const appTitle = appScreenTitle.getTitle()
      assertStringEquals(modelTitle, appTitle, 'Screen title mismatch')
    }

    const modelPreselectCvvFeature = PreselectCvvFeature.get.castIfSupported(model)
    if (modelPreselectCvvFeature !== null) {
      const modelCvvValue = modelPreselectCvvFeature.getCvvFieldValue()
      const appCvvValue = appPreselectCvvFeature.getCvvFieldValue()

      assertBooleanEquals(modelCvvValue === null, appCvvValue === null, 'Incorrect cvv value')

      if (modelCvvValue !== null && appCvvValue !== null) {
        assertStringEquals(modelCvvValue, appCvvValue, 'Incorrect cvv value')
      }

      const modelSelectedCardName = modelPreselectCvvFeature.getCardName()
      const appSelectedCardName = appPreselectCvvFeature.getCardName()

      assertStringEquals(modelSelectedCardName, appSelectedCardName, 'Incorrect card name')
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
    return PreselectCvvComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
