import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertStringEquals } from '../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../common/ys'
import { LicenseAgreementFeature } from '../feature/license-agreement-feature'

export class LicenseAgreementComponent implements MBTComponent {
  public static readonly type: string = 'LicenseAgreementComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelLicenseAgreementFeature = LicenseAgreementFeature.get.castIfSupported(model)
    const appLicenseAgreementFeature = LicenseAgreementFeature.get.castIfSupported(application)

    if (modelLicenseAgreementFeature !== null && appLicenseAgreementFeature !== null) {
      const modelLicenseAgreement = modelLicenseAgreementFeature.getFullLicenseAgreement()
      const appLicenseAgreement = appLicenseAgreementFeature.getFullLicenseAgreement()

      assertStringEquals(modelLicenseAgreement, appLicenseAgreement, 'Incorrect License agreement full text')
    }
  }

  public getComponentType(): MBTComponentType {
    return LicenseAgreementComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
