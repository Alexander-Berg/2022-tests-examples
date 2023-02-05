import { Int32, Throwing } from '../../../../common/ys'
import { SbpBanksList } from '../feature/sbp-banks-list-feature'

export class SbpBanksListModel implements SbpBanksList {
  public waitBankListInterface(mSec: Int32): Throwing<boolean> {
    return true
  }

  public clickAnotherBank(): Throwing<void> {
    return
  }
}
