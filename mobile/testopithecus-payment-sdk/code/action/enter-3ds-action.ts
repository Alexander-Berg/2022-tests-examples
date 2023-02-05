import { Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { PaymentMethodSelectedComponent } from '../component/payment-method-selected-component'
import { PaymentResultComponent } from '../component/payment-result-component'
import { Fill3ds, Fill3dsFeature } from '../feature/fill-3ds-feature'

export class Enter3dsAction extends BaseSimpleAction<Fill3ds, MBTComponent> {
  public constructor(protected readonly code: MBTActionType) {
    super('Enter3dsAction')
  }
  public performImpl(modelOrApplication: Fill3ds, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.fill3dsCode(this.code)
    return new PaymentResultComponent()
  }

  public requiredFeature(): Feature<Fill3ds> {
    return Fill3dsFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class PreselectEnter3dsAction extends Enter3dsAction {
  public constructor() {
    super('200')
  }

  public performImpl(modelOrApplication: Fill3ds, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.fill3dsCode(this.code)
    return new PaymentMethodSelectedComponent()
  }
}

export class Close3dsAction extends BaseSimpleAction<Fill3ds, MBTComponent> {
  public constructor() {
    super('Close3dsAction')
  }
  public performImpl(modelOrApplication: Fill3ds, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.close3dsPage()
    return new PaymentResultComponent()
  }

  public requiredFeature(): Feature<Fill3ds> {
    return Fill3dsFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}
