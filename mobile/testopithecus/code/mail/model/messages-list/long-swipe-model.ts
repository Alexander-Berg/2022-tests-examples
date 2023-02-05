import { Int32, Throwing } from '../../../../../../common/ys'
import { LongSwipe } from '../../feature/message-list/long-swipe-feature'
import { ArchiveMessageModel } from '../base-models/archive-message-model'
import { DeleteMessageModel } from '../base-models/delete-message-model'

export class LongSwipeModel implements LongSwipe {
  public constructor(private deleteMessage: DeleteMessageModel, private archiveMessage: ArchiveMessageModel) {}

  public archiveMessageByLongSwipe(order: Int32): Throwing<void> {
    this.archiveMessage.archiveMessage(order)
  }

  public deleteMessageByLongSwipe(order: Int32, confirmDeletionIfNeeded: boolean): Throwing<void> {
    if (confirmDeletionIfNeeded) {
      this.deleteMessage.deleteMessage(order)
    }
  }
}
