import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Int32, Throwing } from '../../../../common/ys'

export class PaymentResultFeature extends Feature<PaymentResultProvider> {
  public static readonly get: PaymentResultFeature = new PaymentResultFeature()

  private constructor() {
    super('PaymentResultFeature', 'Expose methods to work with payment result')
  }
}

export interface PaymentResultProvider {
  waitForCompletion(mSec: Int32): Throwing<boolean>

  getResultMessage(): Throwing<string>

  closeResultScreen(): Throwing<void>
}
