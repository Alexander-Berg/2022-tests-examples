import { Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTComponent } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { PaymentResultComponent } from '../component/payment-result-component'
import { SbpSampleBank, SbpSampleBankFeature } from '../feature/sbp-sample-bank-feature'

export class ApproveSbpPurchaseAction extends BaseSimpleAction<SbpSampleBank, MBTComponent> {
  public constructor() {
    super('ApproveSbpPurchaseAction')
  }
  public performImpl(modelOrApplication: SbpSampleBank, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.clickApprovePurchase()
    return new PaymentResultComponent()
  }

  public requiredFeature(): Feature<SbpSampleBank> {
    return SbpSampleBankFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}
