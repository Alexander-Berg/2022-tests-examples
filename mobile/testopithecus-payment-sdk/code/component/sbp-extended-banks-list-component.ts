import { Throwing } from '../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { SbpExtendedBanksListFeature } from '../feature/sbp-banks-list-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class SbpExtendedBanksListComponent implements MBTComponent {
  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appFeature = SbpExtendedBanksListFeature.get.forceCast(application)
    assertTrue(
      appFeature.waitForInterface(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
      `Extended banks list does not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} milliseconds`,
    )
    const modelFeature = SbpExtendedBanksListFeature.get.forceCast(model)

    const appShowEmpty = appFeature.isEmptyMessageDisplayed()
    const modelShowEmpty = modelFeature.isEmptyMessageDisplayed()
    assertTrue(appShowEmpty === modelShowEmpty, 'Incorect state of empty message')

    if (!appShowEmpty) {
      const appBanks = appFeature.getBanksList()
      const modelBanks = modelFeature.getBanksList()

      assertTrue(appBanks.length === modelBanks.length, 'Incorrect banks list length')
      for (const bank of modelBanks) {
        assertTrue(appBanks.includes(bank), 'Incorrect bank')
      }
    }
  }

  public getComponentType(): MBTComponentType {
    return 'SbpExtendedBanksListComponent'
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
