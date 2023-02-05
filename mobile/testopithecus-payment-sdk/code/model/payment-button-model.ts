import { Double, Nullable, stringToDouble, stringToInt32, Throwing } from '../../../../common/ys'
import { PaymentButton } from '../feature/payment-button-feature'

export class PaymentButtonModel implements PaymentButton {
  private buttonText: string = ''
  private labelText: string = ''
  private enabled: boolean = false
  private buttonAction: Nullable<(enabled: boolean) => Throwing<void>> = null

  public constructor(private readonly amount: string, private readonly currency: string) {
    this.buttonText = PaymentButtonLabel.pay
    this.labelText = PaymentButtonLabel.label(this.currency, this.amount)
  }

  public getButtonText(): Throwing<string> {
    return this.buttonText
  }

  public setButtonText(value: string): void {
    this.buttonText = value
  }

  public getLabelText(): Throwing<string> {
    return this.labelText
  }

  public setLabelText(value: string): void {
    this.labelText = value
  }

  public setButtonAction(buttonAction: (enabled: boolean) => Throwing<void>): void {
    this.buttonAction = buttonAction
  }

  public isEnabled(): Throwing<boolean> {
    return this.enabled
  }

  public setEnabledInModel(value: boolean): Throwing<void> {
    this.enabled = value
  }

  public pressButton(): Throwing<void> {
    if (this.buttonAction !== null) {
      const action = this.buttonAction!
      action(this.enabled)
    }
  }
}

export class PaymentButtonLabel {
  public static readonly addSberbankCard: string = 'Add SberBank card'
  public static readonly addCard: string = 'Add'
  public static readonly select: string = 'Select'
  public static readonly close: string = 'Close'
  public static readonly pay: string = 'Pay'
  public static readonly enterCvv: string = 'Enter CVV'

  public static label(currency: string, amount: string): string {
    // it is hard to make real fraction check because of lack of Math implementation in crossplatform
    // but this is test-only code, so do the little tricky string check
    const amountNumber: Nullable<Double> = stringToDouble(amount)
    const parts = amount.split('.')
    if (amountNumber === null || parts.length !== 2) {
      return `${currency} ${amount}`
    }
    if (stringToInt32(parts[1]) === 0) {
      return `${currency} ${parts[0]}`
    } else {
      return `${currency} ${amount}`
    }
  }
}
