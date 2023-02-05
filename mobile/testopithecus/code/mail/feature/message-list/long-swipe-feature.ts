import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class LongSwipeFeature extends Feature<LongSwipe> {
  public static get: LongSwipeFeature = new LongSwipeFeature()

  private constructor() {
    super('LongSwipe', 'Архивирование/Удаление через длинный свайп')
  }
}

export interface LongSwipe {
  deleteMessageByLongSwipe(order: Int32, confirmDeletionIfNeeded: boolean): Throwing<void>

  archiveMessageByLongSwipe(order: Int32): Throwing<void>
}
