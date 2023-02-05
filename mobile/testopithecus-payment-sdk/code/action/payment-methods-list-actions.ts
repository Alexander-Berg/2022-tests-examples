import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import { Int32, Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Page3dsComponent } from '../component/payment-3ds-component'
import { PaymentMethodNewCardComponent } from '../component/payment-method-new-card-component'
import { PaymentMethodSelectedComponent } from '../component/payment-method-selected-component'
import { PaymentMethodSelectionComponent } from '../component/payment-method-selection-component'
import { PaymentResultComponent } from '../component/payment-result-component'
import { PreselectCvvComponent } from '../component/preselect-cvv-component'
import { SampleAppComponent } from '../component/sample-app-component'
import { PaymentButtonFeature } from '../feature/payment-button-feature'
import { ReadPaymentDetailsFeature } from '../feature/payment-details-feature'
import {
  PaymentMethodsList,
  PaymentMethodsListFeature,
  Preselect,
  PreselectFeature,
} from '../feature/payment-methods-list-feature'
import { PaymentMethodName } from '../payment-sdk-data'

export class SelectPaymentMethodAction extends BaseSimpleAction<PaymentMethodsList, PaymentMethodSelectionComponent> {
  public constructor(private methodIndex: Int32) {
    super('SelectPaymentMethod')
  }

  public performImpl(
    modelOrApplication: PaymentMethodsList,
    currentComponent: PaymentMethodSelectionComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.selectMethod(this.methodIndex)
    return new PaymentMethodSelectedComponent()
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const methodCanBeSelected = PaymentMethodsListFeature.get.forceCast(model).getMethods().length > this.methodIndex
    return super.canBePerformed(model) && methodCanBeSelected
  }

  public requiredFeature(): Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ClickNewCardPaymentMethodAction extends BaseSimpleAction<PaymentMethodsList, MBTComponent> {
  public static readonly type: MBTActionType = 'ClickNewCardPaymentMethodAction'

  public constructor() {
    super(ClickNewCardPaymentMethodAction.type)
  }

  public requiredFeature(): Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.get
  }

  public performImpl(modelOrApplication: PaymentMethodsList, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.clickNewCard()
    return new PaymentMethodNewCardComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class SelectSbpMethodAction extends BaseSimpleAction<PaymentMethodsList, PaymentMethodSelectionComponent> {
  public constructor() {
    super('SelectSbpMethod')
  }

  public performImpl(
    modelOrApplication: PaymentMethodsList,
    currentComponent: PaymentMethodSelectionComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.selectSbpMethod()
    return new PaymentMethodSelectedComponent()
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const methodCanBeSelected = PaymentMethodsListFeature.get
      .forceCast(model)
      .getMethods()
      .includes(PaymentMethodName.sbp)
    return super.canBePerformed(model) && methodCanBeSelected
  }

  public requiredFeature(): Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class TapOnCashPaymentMethodAction extends BaseSimpleAction<Preselect, MBTComponent> {
  public static readonly type: MBTActionType = 'TapOnCashPaymentMethodAction'

  public constructor() {
    super(TapOnCashPaymentMethodAction.type)
  }

  public requiredFeature(): Feature<Preselect> {
    return PreselectFeature.get
  }

  public performImpl(modelOrApplication: Preselect, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.selectCash()
    return new PaymentMethodSelectedComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class PreselectTapOnOtherCardAction extends BaseSimpleAction<Preselect, MBTComponent> {
  public static readonly type: MBTActionType = 'PreselectTapOnOtherCardAction'

  public constructor() {
    super(PreselectTapOnOtherCardAction.type)
  }

  public requiredFeature(): Feature<Preselect> {
    return PreselectFeature.get
  }

  public performImpl(modelOrApplication: Preselect, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnOtherCard()
    return new PaymentMethodNewCardComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class PreselectTapOnAddCardAction extends BaseSimpleAction<Preselect, MBTComponent> {
  public static readonly type: MBTActionType = 'PreselectTapOnAddCardAction'

  public constructor() {
    super(PreselectTapOnAddCardAction.type)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return PaymentButtonFeature.get.forceCast(model).isEnabled()
  }

  public requiredFeature(): Feature<Preselect> {
    return PreselectFeature.get
  }

  public performImpl(modelOrApplication: Preselect, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnAddCard()
    return new Page3dsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class PressSelectButtonAction implements MBTAction {
  public static readonly type: MBTActionType = 'PressSelectButtonAction'

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelPreselect = PreselectFeature.get.forceCast(model)
    const appPreselect = PreselectFeature.get.forceCast(application)
    const readPaymentDetails = ReadPaymentDetailsFeature.get.forceCast(model)

    modelPreselect.tapOnSelectButton()
    appPreselect.tapOnSelectButton()

    if (modelPreselect.isCashSelected()) {
      return new SampleAppComponent()
    }

    return readPaymentDetails.getForceCvv() ? new PreselectCvvComponent() : new PaymentResultComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PreselectFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return PressSelectButtonAction.type
  }
}

export class TapOnCvvFieldOfSelectPaymentMethodAction extends BaseSimpleAction<PaymentMethodsList, MBTComponent> {
  public static readonly type: MBTActionType = 'TapOnCvvFieldOfSelectPaymentMethodAction'

  public constructor() {
    super(TapOnCvvFieldOfSelectPaymentMethodAction.type)
  }

  public performImpl(modelOrApplication: PaymentMethodsList, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnCvvField()
    return currentComponent
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isSomeMethodSelected = PaymentMethodsListFeature.get.forceCast(model).getSelected() >= 0
    return super.canBePerformed(model) && isSomeMethodSelected
  }

  public requiredFeature(): Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class EnterCvvForSelectPaymentMethodAction extends BaseSimpleAction<PaymentMethodsList, MBTComponent> {
  public static readonly type: MBTActionType = 'EnterCvvForSelectPaymentMethodAction'

  public constructor(private cvv: MBTActionType) {
    super(EnterCvvForSelectPaymentMethodAction.type)
  }

  public performImpl(modelOrApplication: PaymentMethodsList, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnCvvField()
    modelOrApplication.setCvvFieldValue(this.cvv)
    return currentComponent
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isSomeMethodSelected = PaymentMethodsListFeature.get.forceCast(model).getSelected() >= 0
    return super.canBePerformed(model) && isSomeMethodSelected
  }

  public requiredFeature(): Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}
