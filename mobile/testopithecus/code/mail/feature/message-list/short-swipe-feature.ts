import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class ShortSwipeFeature extends Feature<ShortSwipe> {
  public static get: ShortSwipeFeature = new ShortSwipeFeature()

  private constructor() {
    super('ShortSwipe', 'Архивирование/Удаление через короткий свайп')
  }
}

export interface ShortSwipe {
  deleteMessageByShortSwipe(order: Int32): Throwing<void>

  archiveMessageByShortSwipe(order: Int32): Throwing<void>

  markAsRead(order: Int32): Throwing<void>

  markAsUnread(order: Int32): Throwing<void>
}
