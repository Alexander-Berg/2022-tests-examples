import { Int32, Throwing } from '../../../../common/ys'
import { SbpExtendedBanksList } from '../feature/sbp-banks-list-feature'

export class SbpExtendedBanksListModel implements SbpExtendedBanksList {
  private banks: string[] = ['Sample Bank Ltd.', 'OLOLO Bank Ltd.']
  private query: string = ''

  public getBanksList(): string[] {
    if (this.query.length === 0) {
      return this.banks
    }
    // TODO: add ignorecase support
    return this.banks.filter((name) => name.includes(this.query))
  }

  public enterSearchQuery(query: string): Throwing<void> {
    this.query = query
    return
  }

  public tapOnSearch(): Throwing<void> {
    return
  }

  public waitForInterface(mSec: Int32): Throwing<boolean> {
    return true
  }

  public isEmptyMessageDisplayed(): Throwing<boolean> {
    return this.getBanksList().length === 0
  }
}
