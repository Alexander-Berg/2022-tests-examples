import { Throwing } from '../../../../common/ys'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTComponent } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { SampleAppComponent } from '../component/sample-app-component'
import { PaymentResultFeature, PaymentResultProvider } from '../feature/payment-result-feature'

export class ClosePaymentResultScreenAction extends BaseSimpleAction<PaymentResultProvider, MBTComponent> {
  public constructor() {
    super('ClosePaymentResultScreenAction')
  }

  public performImpl(
    modelOrApplication: PaymentResultProvider,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.closeResultScreen()
    return new SampleAppComponent()
  }

  public requiredFeature(): Feature<PaymentResultProvider> {
    return PaymentResultFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}
