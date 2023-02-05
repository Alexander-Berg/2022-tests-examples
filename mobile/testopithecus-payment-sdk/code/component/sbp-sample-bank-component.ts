import { Throwing } from '../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { SbpSampleBankFeature } from '../feature/sbp-sample-bank-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class SbpSampleBankComponent implements MBTComponent {
  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appFeature = SbpSampleBankFeature.get.castIfSupported(application)
    if (appFeature !== null) {
      assertTrue(
        appFeature.waitForBankInterface(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
        `Sample bank interface does not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} milliseconds`,
      )
    }
  }

  public getComponentType(): MBTComponentType {
    return 'SbpSampleBankComponent'
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
