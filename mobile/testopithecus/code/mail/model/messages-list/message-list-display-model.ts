import { Int32, range, Throwing } from '../../../../../../common/ys'
import { fail } from '../../../../../testopithecus-common/code/utils/error-thrower'
import { MessageView } from '../../feature/mail-view-features'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { MessageListDisplay } from '../../feature/message-list/message-list-display-feature'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'

export class MessageListDisplayModel implements MessageListDisplay {
  private currentContainer: MessageContainer = new MessageContainer(
    DefaultFolderName.inbox,
    MessageContainerType.folder,
  )

  private listeners: ContainerListener[] = []
  public messageListFilter: MessageListDatabaseFilter

  public constructor(public accountDataHandler: MailAppModelHandler) {
    this.messageListFilter = new MessageListDatabaseFilter()
  }

  public getCurrentContainer(): MessageContainer {
    return this.currentContainer
  }

  public setCurrentContainer(container: MessageContainer): void {
    this.currentContainer = container
    this.messageListFilter = new MessageListDatabaseFilter()
    this.notifyContainerChanged()
  }

  public attach(listener: ContainerListener): void {
    this.listeners.push(listener)
  }

  // public detach(listener: ContainerListener): void {
  //   const observerIndex = this.listeners.indexOf(listener)
  //   this.listeners.splice(observerIndex, 1)
  // }

  public notifyContainerChanged(): void {
    for (const listener of this.listeners) {
      listener.containerChanged(this.currentContainer)
    }
  }

  public getMessageList(limit: Int32): Throwing<MessageView[]> {
    const messageList = this.accountDataHandler
      .getCurrentAccount()
      .messagesDB.getMessageList(
        this.messageListFilter
          .withContainer(this.currentContainer)
          .withIsInThreadMode(this.isInThreadMode())
          .withLimit(limit),
      )

    if (
      this.currentContainer.name === DefaultFolderName.sent ||
      this.currentContainer.name === DefaultFolderName.draft
    ) {
      for (const i of range(0, messageList.length, 1)) {
        messageList[i].from = messageList[i].to
      }
    }

    if (this.isInThreadMode()) {
      return messageList
    }

    for (const i of range(0, messageList.length, 1)) {
      messageList[i].threadCounter = null
    }

    return messageList
  }

  public getMessageIdList(limit: Int32): MessageId[] {
    return this.accountDataHandler
      .getCurrentAccount()
      .messagesDB.getMessageIdList(
        this.messageListFilter
          .withContainer(this.currentContainer)
          .withIsInThreadMode(this.isInThreadMode())
          .withLimit(limit),
      )
  }

  public refreshMessageList(): Throwing<void> {
    return
  }

  public swipeDownMessageList(): Throwing<void> {
    return
  }

  public unreadCounter(): Throwing<Int32> {
    throw new Error('Not implemented')
    // let unreadCounter = 0;
    // // const mids = this.getMessageIdList(folder, this.messages.size);
    // for (const mid of this.messageToFolder.keys()) {
    //   const folder = this.messageToFolder.get(mid);
    //   if (folder != null && folder.name === folderView.name) {
    //     const message = this.messages.get(mid);
    //     if (message != null && !message.read) {
    //       unreadCounter++;
    //     }
    //   }
    // }
    // return unreadCounter;
  }

  public getMessageId(order: Int32): MessageId {
    const messageIds = this.getMessageIdList(order + 1)
    if (order >= messageIds.length) {
      fail(`No message with order ${order}`)
    }
    return messageIds[order]
  }

  public isInThreadMode(): boolean {
    const notThreadableFolders: string[] = [
      DefaultFolderName.outgoing,
      DefaultFolderName.draft,
      DefaultFolderName.trash,
      DefaultFolderName.spam,
      DefaultFolderName.archive,
    ]
    if (
      this.currentContainer.type === MessageContainerType.folder &&
      notThreadableFolders.includes(this.getCurrentContainer().name)
    ) {
      return false
    }
    if (
      [
        MessageContainerType.search,
        MessageContainerType.unreadFilter,
        MessageContainerType.importantFilter,
        MessageContainerType.label,
      ].includes(this.currentContainer.type)
    ) {
      return false
    }
    return this.accountDataHandler.getCurrentAccount().accountSettings.groupBySubjectEnabled
  }

  public isInTabsMode(): boolean {
    return this.accountDataHandler.getCurrentAccount().accountSettings.sortingEmailsByCategoryEnabled
  }

  public getMidsByOrders(orders: Set<Int32>): MessageId[] {
    const mids: MessageId[] = []
    const messageListMids: MessageId[] = []
    orders.forEach((order) => messageListMids.push(this.getMessageId(order)))
    for (const messageListMid of messageListMids) {
      for (const mid of this.accountDataHandler
        .getCurrentAccount()
        .messagesDB.getMessagesInThreadByMid(messageListMid)) {
        mids.push(mid)
      }
    }
    return mids
  }

  public getThreadByOrder(order: Int32): MessageId[] {
    if (!this.isInThreadMode()) {
      return [this.getMessageId(order)]
    }
    return this.accountDataHandler.getCurrentAccount().messagesDB.getMessagesInThreadByMid(this.getMessageId(order))
  }
}

export interface ContainerListener {
  containerChanged(container: MessageContainer): void
}
