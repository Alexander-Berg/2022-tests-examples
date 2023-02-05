import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Int32, Throwing } from '../../../../common/ys'

export interface UnbindCard {
  waitForUnbindCard(mSec: Int32): Throwing<boolean>

  tapOnEditButton(): Throwing<void>

  isEditButtonShown(): Throwing<boolean>

  tapOnDoneButton(): Throwing<void>

  isDoneButtonShown(): Throwing<boolean>

  unbindCard(index: Int32): Throwing<void>

  getCards(): string[]
}

export class UnbindCardFeature extends Feature<UnbindCard> {
  public static get: UnbindCardFeature = new UnbindCardFeature()

  private constructor() {
    super('UnbindCardFeature', 'Unbind card screen')
  }
}
