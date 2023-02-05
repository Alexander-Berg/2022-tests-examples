import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Int32, Throwing } from '../../../../common/ys'
import { PaymentMethodSelectedComponent } from '../component/payment-method-selected-component'
import { UnbindCardComponent } from '../component/unbind-card-component'
import { UnbindCard, UnbindCardFeature } from '../feature/unbind-card-feature'

export class UnbindCardAction extends BaseSimpleAction<UnbindCard, UnbindCardComponent> {
  public static readonly type: MBTActionType = 'UnbindCardAction'

  public constructor(private index: Int32) {
    super(UnbindCardAction.type)
  }

  public requiredFeature(): Feature<UnbindCard> {
    return UnbindCardFeature.get
  }

  public performImpl(modelOrApplication: UnbindCard, currentComponent: UnbindCardComponent): Throwing<MBTComponent> {
    const numberOfCards = modelOrApplication.getCards().length
    modelOrApplication.unbindCard(this.index)
    return numberOfCards - 1 > 0 ? new UnbindCardComponent() : new PaymentMethodSelectedComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class TapOnEditButtonAction extends BaseSimpleAction<UnbindCard, MBTComponent> {
  public static readonly type: MBTActionType = 'TapOnEditButtonAction'

  public constructor() {
    super(TapOnEditButtonAction.type)
  }

  public requiredFeature(): Feature<UnbindCard> {
    return UnbindCardFeature.get
  }

  public performImpl(modelOrApplication: UnbindCard, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnEditButton()
    return new UnbindCardComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class TapOnDoneButtonAction extends BaseSimpleAction<UnbindCard, UnbindCardComponent> {
  public static readonly type: MBTActionType = 'TapOnDoneButtonAction'

  public constructor() {
    super(TapOnDoneButtonAction.type)
  }

  public requiredFeature(): Feature<UnbindCard> {
    return UnbindCardFeature.get
  }

  public performImpl(modelOrApplication: UnbindCard, currentComponent: UnbindCardComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnDoneButton()
    return new PaymentMethodSelectedComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}
