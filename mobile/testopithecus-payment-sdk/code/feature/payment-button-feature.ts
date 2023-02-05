import { Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class PaymentButtonFeature extends Feature<PaymentButton> {
  public static get: PaymentButtonFeature = new PaymentButtonFeature()

  private constructor() {
    super('PaymentButton', 'Represent "Pay" button')
  }
}

export interface PaymentButton {
  isEnabled(): Throwing<boolean>

  getButtonText(): Throwing<string>

  getLabelText(): Throwing<string>

  pressButton(): Throwing<void>

  setEnabledInModel(value: boolean): Throwing<void>
}
