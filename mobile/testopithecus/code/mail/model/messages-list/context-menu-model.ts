import { copyArray, requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { Int32, int64, Throwing } from '../../../../../../common/ys'
import { FolderName } from '../../feature/folder-list-features'
import { ContextMenu, MessageActionName } from '../../feature/message-list/context-menu-feature'
import { ArchiveMessageModel } from '../base-models/archive-message-model'
import { DeleteMessageModel } from '../base-models/delete-message-model'
import { MarkableImportantModel } from '../base-models/label-model'
import { MarkableReadModel } from '../base-models/markable-read-model'
import { SpamableModel } from '../base-models/spamable-model'
import { ComposeModel } from '../compose/compose-model'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { OpenMessageModel } from '../opened-message/open-message-model'
import { TranslatorBarModel } from '../translator-models'
import { MessageListDisplayModel } from './message-list-display-model'

export class MessageActionItem {
  public static readonly reply: MessageActionName = 'Reply'
  public static readonly replyAll: MessageActionName = 'Reply to all'
  public static readonly forward: MessageActionName = 'Forward'
  public static readonly delete: MessageActionName = 'Delete'
  public static readonly markAsUnread: MessageActionName = 'Mark as unread'
  public static readonly markAsRead: MessageActionName = 'Mark as read'
  public static readonly spam: MessageActionName = 'Spam!'
  public static readonly notSpam: MessageActionName = 'Not spam!'
  public static readonly markAsImportant: MessageActionName = 'Mark as important'
  public static readonly markAsNotImportant: MessageActionName = 'Mark as not important'
  public static readonly moveToFolder: MessageActionName = 'Move to folder'
  public static readonly applyLabel: MessageActionName = 'Apply label'
  public static readonly archive: MessageActionName = 'Archive'
  public static readonly showTranslator: MessageActionName = 'Show translator'
  public static readonly print: MessageActionName = 'Print'

  public static readonly allActions: MessageActionName[] = [
    MessageActionItem.reply,
    MessageActionItem.replyAll,
    MessageActionItem.forward,
    MessageActionItem.delete,
    MessageActionItem.markAsRead,
    MessageActionItem.markAsUnread,
    MessageActionItem.spam,
    MessageActionItem.notSpam,
    MessageActionItem.markAsImportant,
    MessageActionItem.markAsNotImportant,
    MessageActionItem.moveToFolder,
    MessageActionItem.applyLabel,
    MessageActionItem.archive,
    MessageActionItem.showTranslator,
    MessageActionItem.print,
  ]
}

export class ContextMenuModel implements ContextMenu {
  public constructor(
    private deleteMessageModel: DeleteMessageModel,
    private importantMessage: MarkableImportantModel,
    private markableRead: MarkableReadModel,
    private accHandler: MailAppModelHandler,
    private messageListDisplayModel: MessageListDisplayModel,
    private spammable: SpamableModel,
    private compose: ComposeModel,
    private archiveMessage: ArchiveMessageModel,
    private openMessageModel: OpenMessageModel,
    private translatorBarModel: TranslatorBarModel,
  ) {}

  private order: Int32 = -1

  public openFromShortSwipe(order: Int32): Throwing<void> {
    this.order = order
  }

  public openFromMessageView(): Throwing<void> {
    this.order = requireNonNull(this.openMessageModel.getOrder(), 'There is no opened message')
  }

  public close(): Throwing<void> {
    if (this.openMessageModel.getOrder() === null) {
      this.order = -1
    }
  }

  public getAvailableActions(): Throwing<MessageActionName[]> {
    const mid = this.messageListDisplayModel.getMessageId(this.order)
    const folder = this.accHandler.getCurrentAccount().messagesDB.storedFolder(mid)
    const actions = this.messageActionsByFolder(folder)

    const message = this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid)
    const isRead = message.mutableHead.read
    const isImportant = message.mutableHead.important
    const isOneRecipient = message.to.size < 2 // TODO: fix FullMessageModel

    const indexRead = actions.lastIndexOf(MessageActionItem.markAsRead)
    const indexUnread = actions.lastIndexOf(MessageActionItem.markAsUnread)

    if (indexRead !== -1 && indexUnread !== -1) {
      isRead ? actions.splice(indexRead, 1) : actions.splice(indexUnread, 1)
    }

    const indexImportant = actions.lastIndexOf(MessageActionItem.markAsImportant)
    const indexUnimportant = actions.lastIndexOf(MessageActionItem.markAsNotImportant)

    if (indexImportant !== -1 && indexUnimportant !== -1) {
      isImportant ? actions.splice(indexImportant, 1) : actions.splice(indexUnimportant, 1)
    }

    const indexReplyAll = actions.lastIndexOf(MessageActionItem.replyAll)

    if (indexReplyAll !== -1 && isOneRecipient) {
      actions.splice(indexReplyAll, 1)
    }

    const indexTranslator = actions.lastIndexOf(MessageActionItem.showTranslator)
    const isShowTranslatorButtonShown = this.isShowTranslatorButtonShown()

    if (!isShowTranslatorButtonShown && indexTranslator !== -1) {
      actions.splice(indexTranslator, 1)
    }

    return actions
  }

  public getOrderOfMessageWithOpenedContextMenu(): Throwing<Int32> {
    return this.order
  }

  private getMidOfMessageWithOpenedContextMenu(): MessageId {
    return this.messageListDisplayModel.getMessageId(this.order)
  }

  public deleteMessage(): Throwing<void> {
    this.deleteMessageModel.deleteMessage(this.order)
    this.close()
    this.closeMessageIfOpened()
  }

  public markAsImportant(): Throwing<void> {
    this.importantMessage.markAsImportant(this.order)
    this.close()
  }

  public markAsUnimportant(): Throwing<void> {
    this.importantMessage.markAsUnimportant(this.order)
    this.close()
  }

  public markAsRead(): Throwing<void> {
    this.markableRead.markAsRead(this.order)
    this.close()
  }

  public markAsUnread(): Throwing<void> {
    this.markableRead.markAsUnread(this.order)
    this.close()
  }

  public openMoveToFolderScreen(): Throwing<void> {
    // do nothing
  }

  public archive(): Throwing<void> {
    this.archiveMessage.archiveMessage(this.order)
    this.close()
    this.closeMessageIfOpened()
  }

  public markAsNotSpam(): Throwing<void> {
    this.spammable.moveFromSpam(this.order)
    this.close()
    this.closeMessageIfOpened()
  }

  public markAsSpam(): Throwing<void> {
    this.spammable.moveToSpam(this.order)
    this.close()
    this.closeMessageIfOpened()
  }

  public openApplyLabelsScreen(): Throwing<void> {
    // do nothing
  }

  public openForwardCompose(): Throwing<void> {
    this.compose.openForwardCompose(this.getMidOfMessageWithOpenedContextMenu())
    this.close()
  }

  public openReplyAllCompose(): Throwing<void> {
    this.compose.openReplyAllCompose(this.getMidOfMessageWithOpenedContextMenu())
    this.close()
  }

  public openReplyCompose(): Throwing<void> {
    this.compose.openReplyCompose(this.getMidOfMessageWithOpenedContextMenu())
    this.close()
  }

  public showTranslator(): Throwing<void> {
    return this.translatorBarModel.forceShowBar()
  }

  private isShowTranslatorButtonShown(): Throwing<boolean> {
    const isMessageOpened = this.openMessageModel.openedMessage !== int64(-1)
    const isTranslatorBarShown = this.translatorBarModel.isTranslatorBarShown()
    return !isTranslatorBarShown && isMessageOpened
  }

  private messageActionsByFolder(folder: FolderName): MessageActionName[] {
    const actions = copyArray(MessageActionItem.allActions)
    switch (folder) {
      case DefaultFolderName.archive:
        for (const folder of [MessageActionItem.archive, MessageActionItem.notSpam]) {
          actions.splice(actions.lastIndexOf(folder), 1)
        }
        return actions
      case DefaultFolderName.sent:
        for (const folder of [MessageActionItem.spam, MessageActionItem.notSpam]) {
          actions.splice(actions.lastIndexOf(folder), 1)
        }
        return actions
      case DefaultFolderName.trash:
        for (const folder of [
          MessageActionItem.markAsNotImportant,
          MessageActionItem.markAsImportant,
          MessageActionItem.notSpam,
          MessageActionItem.applyLabel,
        ]) {
          actions.splice(actions.lastIndexOf(folder), 1)
        }
        return actions
      case DefaultFolderName.spam:
        for (const folder of [
          MessageActionItem.markAsNotImportant,
          MessageActionItem.markAsImportant,
          MessageActionItem.spam,
          MessageActionItem.applyLabel,
        ]) {
          actions.splice(actions.lastIndexOf(folder), 1)
        }
        return actions
      case DefaultFolderName.draft:
        return [MessageActionItem.forward, MessageActionItem.delete]
      case DefaultFolderName.template:
        return [MessageActionItem.delete]
      default:
        actions.splice(actions.lastIndexOf(MessageActionItem.notSpam), 1)
        return actions
    }
  }

  private closeMessageIfOpened(): Throwing<void> {
    if (this.openMessageModel.openedMessage !== int64(-1)) {
      this.openMessageModel.closeMessage()
    }
  }
}
