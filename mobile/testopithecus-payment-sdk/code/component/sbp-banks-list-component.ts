import { Throwing } from '../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { SbpBanksListFeature } from '../feature/sbp-banks-list-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class SbpBanksListComponent implements MBTComponent {
  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appFeature = SbpBanksListFeature.get.castIfSupported(application)
    if (appFeature !== null) {
      assertTrue(
        appFeature.waitBankListInterface(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
        `Banks list does not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} milliseconds`,
      )
    }
  }

  public getComponentType(): MBTComponentType {
    return 'SbpBanksListComponent'
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
