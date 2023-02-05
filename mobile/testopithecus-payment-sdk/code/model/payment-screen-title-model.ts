import { Throwing } from '../../../../common/ys'
import { PaymentScreenTitle } from '../feature/payment-screen-title-feature'

export class PaymentScreenTitleModel implements PaymentScreenTitle {
  private title: string = ''

  public getTitle(): Throwing<string> {
    return this.title
  }

  public setTitle(title: string): Throwing<void> {
    this.title = title
  }
}

export class PaymentScreenTitleLabel {
  public static readonly cardPayment: string = 'Card payment'
  public static readonly paymentMethod: string = 'Payment method'
  public static readonly addSberbankCard: string = 'Add SberBank card'
  public static readonly personalInformation: string = 'Personal information'
  public static readonly addCard: string = 'Add card'
}
