import { Merchant } from '../../../payment-sdk/code/models/merchant'
import { Payer } from '../../../payment-sdk/code/models/payer'

export class PaymentConfig {
  public constructor(
    public readonly payer: Payer,
    public readonly merchant: Merchant,
    public readonly token: string,
    public readonly order: string,
  ) {}
}
