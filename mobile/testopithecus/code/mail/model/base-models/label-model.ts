import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { Int32, int64, Nullable, setToArray, Throwing } from '../../../../../../common/ys'
import { resolveThrow } from '../../../utils/mail-utils'
import { ApplyLabel } from '../../feature/apply-label-feature'
import { MarkableImportant } from '../../feature/base-action-features'
import { LabelName } from '../../feature/folder-list-features'
import { CreatableLabel } from '../../feature/manageable-container-features'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { ContextMenuModel } from '../messages-list/context-menu-model'
import { GroupModeModel } from '../messages-list/group-mode-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { OpenMessageModel } from '../opened-message/open-message-model'

export class MarkableImportantModel implements MarkableImportant {
  public constructor(private model: MessageListDisplayModel, private accHandler: MailAppModelHandler) {}

  public markAsImportant(order: Int32): Throwing<void> {
    for (const mid of this.model.getThreadByOrder(order)) {
      this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.important = true
    }
  }

  public markAsUnimportant(order: Int32): Throwing<void> {
    for (const mid of this.model.getThreadByOrder(order)) {
      this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.important = false
    }
  }
}

export class LabelModel implements CreatableLabel {
  public constructor(private accHandler: MailAppModelHandler) {}

  public applyLabelsToMessages(mids: Set<MessageId>, labelNames: LabelName[]): void {
    labelNames.forEach((labelName) => {
      if (!this.accHandler.getCurrentAccount().messagesDB.getLabelList().includes(labelName)) {
        resolveThrow(() => this.createLabel(labelName), null)
      }
      this.accHandler.getCurrentAccount().messagesDB.applyLabelToMessages(labelName, mids)
    })
  }

  public removeLabelsFromMessages(mids: Set<MessageId>, labelNames: LabelName[]): void {
    labelNames.forEach((labelName) => {
      this.accHandler.getCurrentAccount().messagesDB.removeLabelFromMessages(labelName, mids)
    })
  }

  public getMessageLabels(mid: MessageId): Set<LabelName> {
    return this.accHandler.getCurrentAccount().messagesDB.getMessageLabels(mid)
  }

  public getMessagesLabels(mids: Set<MessageId>): Set<LabelName> {
    const labels = new Set<LabelName>()
    for (const mid of setToArray(mids)) {
      this.getMessageLabels(mid).forEach((label) => labels.add(label))
    }
    return labels
  }

  public createLabel(labelName: string): Throwing<void> {
    this.accHandler.getCurrentAccount().messagesDB.createLabel(labelName)
  }

  public removeLabel(labelName: string): Throwing<void> {
    this.accHandler.getCurrentAccount().messagesDB.removeLabel(labelName)
  }
}

export class ApplyLabelModel implements ApplyLabel {
  public constructor(
    private accHandler: MailAppModelHandler,
    private openMessageModel: OpenMessageModel,
    private contextMenuModel: ContextMenuModel,
    private messageListDisplayModel: MessageListDisplayModel,
    private groupModeModel: GroupModeModel,
    private labelModel: LabelModel,
  ) {}

  private getMids(): Throwing<Nullable<Set<MessageId>>> {
    if (this.openMessageModel.openedMessage !== int64(-1)) {
      return new Set([this.openMessageModel.openedMessage])
    }

    const contextMenuOrder = this.contextMenuModel.getOrderOfMessageWithOpenedContextMenu()
    if (contextMenuOrder !== -1) {
      return new Set([this.messageListDisplayModel.getMessageId(contextMenuOrder)])
    }

    const groupModeSelectedOrders = this.groupModeModel.getSelectedMessages()
    if (groupModeSelectedOrders.size > 0) {
      return new Set(this.messageListDisplayModel.getMidsByOrders(groupModeSelectedOrders))
    }
    return null
  }

  private selectedToAdd: LabelName[] = []
  private deselectedToRemove: LabelName[] = []

  public tapOnCreateLabel(): Throwing<void> {
    // do nothing
  }

  public getLabelList(): Throwing<LabelName[]> {
    return this.accHandler.getCurrentAccount().messagesDB.getLabelList()
  }

  public getSelectedLabels(): Throwing<LabelName[]> {
    const selected = this.labelModel.getMessagesLabels(
      requireNonNull(this.getMids(), 'There is no opened/selected messages'),
    )
    this.deselectedToRemove.forEach((label) => selected.delete(label))
    this.selectedToAdd.forEach((label) => selected.add(label))
    return setToArray(selected)
  }

  public selectLabelsToAdd(labelNames: LabelName[]): Throwing<void> {
    this.selectedToAdd = labelNames
  }

  public deselectLabelsToRemove(labelNames: LabelName[]): Throwing<void> {
    this.deselectedToRemove = labelNames
  }

  public tapOnDoneButton(): Throwing<void> {
    const mids = requireNonNull(this.getMids(), 'There is no opened/selected messages')
    this.labelModel.removeLabelsFromMessages(mids, this.deselectedToRemove)
    this.labelModel.applyLabelsToMessages(mids, this.selectedToAdd)
    this.selectedToAdd = []
    this.deselectedToRemove = []
    this.contextMenuModel.close()
    this.groupModeModel.unselectAllMessages()
  }
}
