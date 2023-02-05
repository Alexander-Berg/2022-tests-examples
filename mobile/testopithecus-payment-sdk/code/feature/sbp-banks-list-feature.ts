import { Int32, Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class SbpBanksListFeature extends Feature<SbpBanksList> {
  public static get: SbpBanksListFeature = new SbpBanksListFeature()

  private constructor() {
    super('SbpBankListFeature', 'Allow to interact with sbp banks list')
  }
}

export interface SbpBanksList {
  waitBankListInterface(mSec: Int32): Throwing<boolean>
  clickAnotherBank(): Throwing<void>
}

export class SbpExtendedBanksListFeature extends Feature<SbpExtendedBanksList> {
  public static get: SbpExtendedBanksListFeature = new SbpExtendedBanksListFeature()

  private constructor() {
    super('SbpExtendedBanksListFeature', 'Allow to interact with extended sbp banks list')
  }
}

export interface SbpExtendedBanksList {
  waitForInterface(mSec: Int32): Throwing<boolean>
  tapOnSearch(): Throwing<void>
  enterSearchQuery(query: string): Throwing<void>
  getBanksList(): string[]
  isEmptyMessageDisplayed(): Throwing<boolean>
}
