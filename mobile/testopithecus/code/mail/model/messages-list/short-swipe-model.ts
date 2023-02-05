import { Int32, Throwing } from '../../../../../../common/ys'
import { ShortSwipe } from '../../feature/message-list/short-swipe-feature'
import { ArchiveMessageModel } from '../base-models/archive-message-model'
import { DeleteMessageModel } from '../base-models/delete-message-model'
import { MarkableReadModel } from '../base-models/markable-read-model'

export class ShortSwipeModel implements ShortSwipe {
  public constructor(
    private deleteMessage: DeleteMessageModel,
    private archiveMessage: ArchiveMessageModel,
    private markableReadModel: MarkableReadModel,
  ) {}

  public deleteMessageByShortSwipe(order: Int32): Throwing<void> {
    this.deleteMessage.deleteMessage(order)
  }

  public archiveMessageByShortSwipe(order: Int32): Throwing<void> {
    this.archiveMessage.archiveMessage(order)
  }

  public markAsRead(order: Int32): Throwing<void> {
    this.markableReadModel.markAsRead(order)
  }

  public markAsUnread(order: Int32): Throwing<void> {
    this.markableReadModel.markAsUnread(order)
  }
}
