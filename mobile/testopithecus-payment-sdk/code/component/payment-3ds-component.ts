import { assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../common/ys'
import { Fill3dsFeature } from '../feature/fill-3ds-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class Page3dsComponent implements MBTComponent {
  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appFeature = Fill3dsFeature.get.castIfSupported(application)
    if (appFeature !== null) {
      assertTrue(
        appFeature.waitFor3dsPage(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
        `3ds page does not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} milliseconds`,
      )
    }
  }

  public getComponentType(): MBTComponentType {
    return 'Web page with 3ds verification'
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
