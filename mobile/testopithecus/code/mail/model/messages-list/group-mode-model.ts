import { Int32, range, Throwing } from '../../../../../../common/ys'
import { GroupMode } from '../../feature/message-list/group-mode-feature'
import { ArchiveMessageModel } from '../base-models/archive-message-model'
import { DeleteMessageModel } from '../base-models/delete-message-model'
import { MarkableImportantModel } from '../base-models/label-model'
import { MarkableReadModel } from '../base-models/markable-read-model'
import { SpamableModel } from '../base-models/spamable-model'
import { MessageListDisplayModel } from './message-list-display-model'

export class GroupModeModel implements GroupMode {
  public selectedOrders: Set<Int32> = new Set<Int32>()

  public constructor(
    private markableModel: MarkableReadModel,
    private deleteMessageModel: DeleteMessageModel,
    private archiveModel: ArchiveMessageModel,
    private important: MarkableImportantModel,
    private spam: SpamableModel,
    private messageListDisplay: MessageListDisplayModel,
  ) {}

  public getNumberOfSelectedMessages(): Throwing<Int32> {
    const selectedMessages = this.messageListDisplay.getMidsByOrders(this.selectedOrders)
    return selectedMessages.length
  }

  public getSelectedMessages(): Throwing<Set<Int32>> {
    return this.selectedOrders
  }

  public isInGroupMode(): Throwing<boolean> {
    return this.selectedOrders.size !== 0
  }

  public markAsRead(): Throwing<void> {
    for (const order of this.selectedOrders.values()) {
      this.markableModel.markAsRead(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public markAsUnread(): Throwing<void> {
    for (const order of this.selectedOrders.values()) {
      this.markableModel.markAsUnread(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public delete(): Throwing<void> {
    this.deleteMessageModel.deleteMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public selectMessage(byOrder: Int32): Throwing<void> {
    this.selectedOrders.add(byOrder)
  }

  public archive(): Throwing<void> {
    this.archiveModel.archiveMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public markAsImportant(): Throwing<void> {
    for (const order of this.selectedOrders.values()) {
      this.important.markAsImportant(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public markAsNotSpam(): Throwing<void> {
    this.spam.moveFromSpamMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public markAsSpam(): Throwing<void> {
    this.spam.moveToSpamMessages(this.selectedOrders)
    this.selectedOrders = new Set<Int32>()
  }

  public markAsUnimportant(): Throwing<void> {
    for (const order of this.selectedOrders.values()) {
      this.important.markAsUnimportant(order)
    }
    this.selectedOrders = new Set<Int32>()
  }

  public openMoveToFolderScreen(): Throwing<void> {
    // do nothing
  }

  public unselectAllMessages(): Throwing<void> {
    this.selectedOrders = new Set<Int32>()
  }

  public unselectMessage(byOrder: Int32): Throwing<void> {
    this.selectedOrders.delete(byOrder)
  }

  public copy(): GroupModeModel {
    const copy = new GroupModeModel(
      this.markableModel,
      this.deleteMessageModel,
      this.archiveModel,
      this.important,
      this.spam,
      this.messageListDisplay,
    )
    copy.selectedOrders = this.selectedOrders
    return copy
  }

  public initialMessageSelect(byOrder: Int32): Throwing<void> {
    this.selectedOrders = new Set<Int32>([byOrder])
  }

  public selectAllMessages(): Throwing<void> {
    const storedMessages: Int32[] = []
    for (const i of range(0, this.messageListDisplay.getMessageList(20).length)) {
      storedMessages.push(i)
    }
    this.selectedOrders = new Set<Int32>(storedMessages)
  }

  public openApplyLabelsScreen(): Throwing<void> {
    // do nothing
  }
}
