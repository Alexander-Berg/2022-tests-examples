import { Int32, Nullable, Throwing } from '../../../../common/ys'
import { PaymentResultProvider } from '../feature/payment-result-feature'
import { PaymentErrorType } from '../payment-sdk-data'
import { Fill3dsModel } from './fill-3ds-model'
import { FillNewCardModel, NewCardMode } from './fill-new-card-model'

export class PaymentResultModel implements PaymentResultProvider {
  public constructor(
    private readonly forcedErrorType: Nullable<PaymentErrorType>,
    private readonly code3ds: Nullable<string>,
    private readonly cvvValid: boolean,
    private readonly fillNewCardModel: FillNewCardModel,
    private readonly fill3dsModel: Fill3dsModel,
  ) {}

  public getResultMessage(): Throwing<string> {
    if (this.fillNewCardModel.getNewCardMode() === NewCardMode.bind) {
      return ResultMessage.cardAdded
    }
    if (this.isSuccess()) {
      return ResultMessage.success
    }
    if (this.forcedErrorType === null) {
      return ResultMessage.error
    }
    return paymentErrorLocalization(this.forcedErrorType)
  }

  public isSuccess(): boolean {
    return (
      !this.fill3dsModel.is3dsPageForceClosed() &&
      this.forcedErrorType === null &&
      [null, '200'].includes(this.code3ds) &&
      this.cvvValid
    )
  }

  public forcedPaymentErrorType(): Nullable<PaymentErrorType> {
    return this.forcedErrorType
  }

  public isCvvValid(): boolean {
    return this.cvvValid
  }

  public waitForCompletion(mSec: Int32): Throwing<boolean> {
    return true
  }

  public closeResultScreen(): Throwing<void> {}
}

export class ResultMessage {
  public static readonly success: string = 'Thank you for your order!\nPayment was successful'
  public static readonly error: string = 'Something went wrong.\nPlease try again'
  public static readonly cardAdded: string = 'Card added'
  public static readonly cardRemoved: string = 'Card removed'
}

export function paymentErrorLocalization(errorType: PaymentErrorType): string {
  switch (errorType) {
    case PaymentErrorType.notEnoughFunds:
      return 'Not enough funds on card'
    case PaymentErrorType.force3ds:
      return 'Failed 3DS verification'
    case PaymentErrorType.transactionNotPermittedToCard57:
    case PaymentErrorType.transactionNotPermittedToCard58:
      return 'Transaction not permitted for this card'
    case PaymentErrorType.restrictedCard36:
    case PaymentErrorType.restrictedCard62:
      return 'Invalid card'
    default:
      return ResultMessage.error
  }
  return ResultMessage.error
}
