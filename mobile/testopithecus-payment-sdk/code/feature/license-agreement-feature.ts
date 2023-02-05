import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../common/ys'

export class LicenseAgreementFeature extends Feature<LicenseAgreement> {
  public static get: LicenseAgreementFeature = new LicenseAgreementFeature()

  private constructor() {
    super('LicenseAgreement', 'License agreement at bottom of an screen and separate License agreement screen')
  }
}

export interface LicenseAgreement {
  getLicenseAgreement(): Throwing<string>

  isLicenseAgreementShown(): Throwing<boolean>

  openFullLicenseAgreement(): Throwing<void>

  closeFullLicenseAgreement(): Throwing<void>

  getFullLicenseAgreement(): Throwing<string>
}
