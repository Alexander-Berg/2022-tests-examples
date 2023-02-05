import { Int32, Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class SbpSampleBankFeature extends Feature<SbpSampleBank> {
  public static get: SbpSampleBankFeature = new SbpSampleBankFeature()

  private constructor() {
    super('SbpSampleBankFeature', 'Allow to interact with sample bank interface')
  }
}

export interface SbpSampleBank {
  waitForBankInterface(mSec: Int32): Throwing<boolean>
  clickApprovePurchase(): Throwing<void>
}
