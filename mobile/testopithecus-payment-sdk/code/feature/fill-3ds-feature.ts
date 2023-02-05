import { Int32, Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class Fill3dsFeature extends Feature<Fill3ds> {
  public static get: Fill3dsFeature = new Fill3dsFeature()

  private constructor() {
    super('Fill3dsFeature', 'Feature to enter 3ds on bank site')
  }
}

export interface Fill3ds {
  waitFor3dsPage(mSec: Int32): Throwing<boolean>

  fill3dsCode(code: string): Throwing<void>

  close3dsPage(): Throwing<void>
}
