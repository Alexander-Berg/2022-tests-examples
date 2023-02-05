import { Int32, Throwing } from '../../../../common/ys'
import { SbpSampleBank } from '../feature/sbp-sample-bank-feature'

export class SbpSampleBankModel implements SbpSampleBank {
  public waitForBankInterface(mSec: Int32): Throwing<boolean> {
    return true
  }

  public clickApprovePurchase(): Throwing<void> {
    return
  }
}
