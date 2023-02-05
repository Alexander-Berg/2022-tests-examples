import { Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
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
import { PaymentMethodSelectedComponent } from '../component/payment-method-selected-component'
import { PaymentResultComponent } from '../component/payment-result-component'
import { SbpBanksListComponent } from '../component/sbp-banks-list-component'
import { SbpSampleBankComponent } from '../component/sbp-sample-bank-component'
import { PaymentButton, PaymentButtonFeature } from '../feature/payment-button-feature'
import { ReadPaymentDetailsFeature } from '../feature/payment-details-feature'
import { MethodsListMode, PaymentMethodsListFeature } from '../feature/payment-methods-list-feature'
import { PaymentMethodName } from '../payment-sdk-data'

export class PressPaymentButtonAction implements MBTAction {
  public static readonly type: MBTActionType = 'PressPaymentButtonAction'

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelPaymentButton = PaymentButtonFeature.get.forceCast(model)
    const appPaymentButton = PaymentButtonFeature.get.forceCast(application)

    const modelReadPaymentDetails = ReadPaymentDetailsFeature.get.forceCast(model)
    const isPreselectModeEnabled =
      PaymentMethodsListFeature.get.forceCast(model).getMethodsListMode() === MethodsListMode.preselect

    const selectedMethod = PaymentMethodsListFeature.get.forceCast(model).getSelected()
    const shouldShowSbp =
      selectedMethod !== -1 &&
      PaymentMethodsListFeature.get.forceCast(model).getMethods()[selectedMethod] === PaymentMethodName.sbp
    modelPaymentButton.pressButton()
    appPaymentButton.pressButton()

    if (history.currentComponent.tostring() === 'PaymentMethodNewCardComponent' && isPreselectModeEnabled) {
      return new PaymentMethodSelectedComponent()
    }
    if (
      history.currentComponent.tostring() === 'SbpBanksListComponent' ||
      history.currentComponent.tostring() === 'SbpExtendedBanksListComponent'
    ) {
      return new SbpSampleBankComponent()
    }

    if (shouldShowSbp) {
      return new SbpBanksListComponent()
    }

    return modelReadPaymentDetails.getExpected3ds() !== null ? new Page3dsComponent() : new PaymentResultComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PaymentButtonFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return PaymentButtonFeature.get.forceCast(model).isEnabled()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return PressPaymentButtonAction.type
  }
}

export class SetPaymentButtonStatusAction extends BaseSimpleAction<PaymentButton, MBTComponent> {
  public constructor(private readonly enabled: boolean = true) {
    super('SetPaymentButtonStatusAction')
  }

  public performImpl(paymentButton: PaymentButton, currentComponent: MBTComponent): Throwing<MBTComponent> {
    paymentButton.setEnabledInModel(this.enabled)
    return currentComponent
  }

  public requiredFeature(): Feature<PaymentButton> {
    return PaymentButtonFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}
