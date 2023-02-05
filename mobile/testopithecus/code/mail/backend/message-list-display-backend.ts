import { Int32, Nullable, Throwing } from '../../../../../common/ys'
import { Folder, FolderType, isFolderOfThreadedType } from '../../../../mapi/code/api/entities/folder/folder'
import { MessageMeta } from '../../../../mapi/code/api/entities/message/message-meta'
import { MailboxClient, MailboxClientHandler } from '../../client/mailbox-client'
import { MessageView } from '../feature/mail-view-features'
import { MessageListDisplay } from '../feature/message-list/message-list-display-feature'
import { FolderId, Message } from '../model/mail-model'
import { fail } from '../../../../testopithecus-common/code/utils/error-thrower'

export class MessageListDisplayBackend implements MessageListDisplay {
  private currentFolderId: Nullable<FolderId> = null

  public constructor(private clientsHandler: MailboxClientHandler) {
    // this.currentFolderId = MessageListDisplayBackend.getInbox(this.clientsHandler.currentClient!).fid
  }

  public swipeDownMessageList(): void {
    return
  }

  private static getInbox(client: MailboxClient): Folder {
    return MessageListDisplayBackend.getFolderByType(client, FolderType.inbox)
  }

  private static getFolderByType(client: MailboxClient, type: FolderType): Folder {
    return client.getFolderList().filter((f) => f.type === type)[0]
  }

  public getCurrentFolderId(): FolderId {
    if (this.currentFolderId === null) {
      this.currentFolderId = MessageListDisplayBackend.getInbox(this.clientsHandler.getCurrentClient()).fid
    }

    return this.currentFolderId!
  }

  public setCurrentFolderId(folderId: FolderId): void {
    this.currentFolderId = folderId
  }

  public getMessageList(limit: Int32): Throwing<MessageView[]> {
    return this.getMessageDTOList(limit).map((meta) => Message.fromMeta(meta))
  }

  public getMessageDTOList(limit: Int32): MessageMeta[] {
    return this.isInThreadMode()
      ? this.clientsHandler.getCurrentClient().getThreadsInFolder(this.getCurrentFolderId(), limit)
      : this.clientsHandler.getCurrentClient().getMessagesInFolder(this.getCurrentFolderId(), limit)
  }

  public unreadCounter(): Throwing<Int32> {
    return this.getCurrentFolder().unreadCounter
  }

  public refreshMessageList(): Throwing<void> {
    return
  }

  public getThreadMessage(byOrder: Int32): MessageMeta {
    const threads = this.getMessageDTOList(byOrder + 1)
    const threadsCount = threads.length
    if (threadsCount <= byOrder) {
      fail(`No thread in folder ${this.getCurrentFolderId()} by order ${byOrder}, there are ${threadsCount} threads`)
    }
    return threads[byOrder]
  }

  public getFolder(id: FolderId): Folder {
    return this.clientsHandler
      .getCurrentClient()
      .getFolderList()
      .filter((f) => f.fid === id)[0]
  }

  public getCurrentFolder(): Folder {
    return this.getFolder(this.getCurrentFolderId())
  }

  public getFolderByType(type: FolderType): Folder {
    return MessageListDisplayBackend.getFolderByType(this.clientsHandler.getCurrentClient(), type)
  }

  public getInbox(): Folder {
    return this.getFolderByType(FolderType.inbox)
  }

  public isInThreadMode(): boolean {
    const folderType = this.getCurrentFolder().type
    return isFolderOfThreadedType(folderType)
  }

  public goToAccountSwitcher(): Throwing<void> {}
}
