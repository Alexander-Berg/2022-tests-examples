import {
  arrayToSet,
  Int32,
  int64,
  Int64,
  int64ToInt32,
  Nullable,
  range,
  setToArray,
  undefinedToNull,
} from '../../../../../../common/ys'
import { HashBuilder } from '../../../../../testopithecus-common/code/mbt/walk/hash/hash-builder'
import { fail } from '../../../../../testopithecus-common/code/utils/error-thrower'
import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { FolderName, LabelName } from '../../feature/folder-list-features'
import { MessageView } from '../../feature/mail-view-features'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { LanguageName } from '../../feature/translator-features'
import { DefaultFolderName } from '../folder-data-model'
import { FullMessage, MessageId } from '../mail-model'

export class MessageListDatabase {
  public constructor(
    private readonly messages: Map<MessageId, FullMessage>,
    private readonly folderToMessages: Map<FolderName, Set<MessageId>>,
    private readonly labelToMessages: Map<LabelName, Set<MessageId>>,
    private tabsToMessages: Map<FolderName, Set<MessageId>>,
    private threads: Set<MessageId>[],
  ) {
    this.tabsToMessages.set(DefaultFolderName.inbox, new Set<MessageId>())
    this.tabsToMessages.set(DefaultFolderName.socialNetworks, new Set<MessageId>())
    this.tabsToMessages.set(DefaultFolderName.mailingLists, new Set<MessageId>())
  }

  public setMailDBHash(builder: HashBuilder): void {
    for (const thread of this.threads) {
      builder.addInt(19)
      for (const message of thread.values()) {
        builder.addInt64(message)
      }
    }

    builder.addInt(17)
    this.messages.forEach((message, id) => {
      builder.addInt64(id).addInt64(this.getMessageHash(message))
    })

    builder.addInt(13)
    this.folderToMessages.forEach((ids, folder) => {
      for (const id of ids.values()) {
        builder.addInt64(id).addString(folder)
      }
    })
  }

  public copy(): MessageListDatabase {
    const messagesCopy = new Map<MessageId, FullMessage>()
    for (const mid of this.messages.keys()) {
      messagesCopy.set(mid, this.messages.get(mid)!.copy())
    }
    const threadsCopy: Set<MessageId>[] = []
    this.threads.forEach((thread) => {
      const threadCopy = new Set(thread)
      threadsCopy.push(threadCopy)
    })
    const folderToMessagesCopy: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>()
    this.folderToMessages.forEach((mids, folderName) => folderToMessagesCopy.set(folderName, new Set(mids)))
    const labelToMessagesCopy: Map<LabelName, Set<MessageId>> = new Map<LabelName, Set<MessageId>>()
    this.labelToMessages.forEach((lids, labelName) => labelToMessagesCopy.set(labelName, new Set(lids)))
    const tabsToMessagesCopy: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>()
    this.tabsToMessages.forEach((mids, folderName) => tabsToMessagesCopy.set(folderName, new Set(mids)))
    return new MessageListDatabase(
      messagesCopy,
      folderToMessagesCopy,
      labelToMessagesCopy,
      tabsToMessagesCopy,
      threadsCopy,
    )
  }

  public getTabsToMessage(folderName: FolderName): Set<MessageId> {
    if (!this.tabsToMessages.has(folderName)) {
      fail(`Это папка не таб!`)
    }
    const messages = undefinedToNull(this.tabsToMessages.get(folderName))
    return messages!
  }

  public getLabelList(): LabelName[] {
    const labels: LabelName[] = []
    this.labelToMessages.forEach((mids, label) => labels.push(label))
    return labels
  }

  public getFolderList(): FolderName[] {
    const folders: FolderName[] = []
    this.folderToMessages.forEach((mids, folder) => folders.push(folder))
    return folders
  }

  public getUserFolders(): FolderName[] {
    const excludedFolders = [
      DefaultFolderName.inbox,
      DefaultFolderName.socialNetworks,
      DefaultFolderName.mailingLists,
      DefaultFolderName.trash,
      DefaultFolderName.sent,
      DefaultFolderName.archive,
      DefaultFolderName.spam,
      DefaultFolderName.outgoing,
      DefaultFolderName.draft,
      DefaultFolderName.template,
    ]
    return this.getFolderList().filter((folderName) => !excludedFolders.includes(folderName))
  }

  public getMessages(): MessageId[] {
    const messages: MessageId[] = []
    this.messages.forEach((msg, mid) => messages.push(mid))
    return messages
  }

  public getMessageIdList(filter: MessageListDatabaseFilter): MessageId[] {
    const filteredMessages: MessageId[] = []
    this.messages.forEach((_, mid) => {
      if (this.isMessageInFilter(mid, filter)) {
        filteredMessages.push(mid)
      }
    })
    return this.buildMessageIdList(filteredMessages, filter.getLimit(), filter.getIsInThreadMode())
  }

  public getMessageList(filter: MessageListDatabaseFilter): MessageView[] {
    return this.getMessageListFromIds(this.getMessageIdList(filter), filter.getIsInThreadMode())
  }

  public isMessageInFilter(mid: MessageId, filter: MessageListDatabaseFilter): boolean {
    if (filter.getContainer() !== null) {
      if (!this.isMessageInContainer(mid, filter)) {
        return false
      }
    }
    if (filter.getFolder() !== null) {
      if (!this.isMessageInFolder(mid, filter.getFolder()!)) {
        return false
      }
    }
    if (filter.getLabel() !== null) {
      if (!this.isMessageInLabel(mid, filter.getLabel()!)) {
        return false
      }
    }
    if (filter.getIsImportantOnly()) {
      if (!this.isMessageImportant(mid)) {
        return false
      }
    }
    if (filter.getIsUnreadOnly()) {
      if (!this.isMessageUnread(mid)) {
        return false
      }
    }
    for (const folderName of filter.getExcludedFolders()) {
      if (this.isMessageInFolder(mid, folderName)) {
        return false
      }
    }
    return true
  }

  public getMessagesInThreadByMid(mid: MessageId): MessageId[] {
    const orderInThreads = this.findThread(mid)
    if (orderInThreads === null) {
      return [mid]
    }
    const threadMids = this.threads[orderInThreads]
    const sortedMids = setToArray(threadMids)
    sortedMids.sort((m1, m2) => {
      return int64ToInt32(this.storedMessage(m2).mutableHead.timestamp - this.storedMessage(m1).mutableHead.timestamp)
    })
    return sortedMids
  }

  public makeMessageThreadView(threadMid: MessageId): FullMessage {
    const threadView = this.storedMessage(threadMid).copy()
    if (!this.isMessageInFolder(threadMid, DefaultFolderName.trash)) {
      threadView.mutableHead.threadCounter = this.getMessagesInThreadByMid(threadMid).filter(
        (mid) => !this.isMessageInFolder(mid, DefaultFolderName.trash),
      ).length
    }
    if (threadView.mutableHead.threadCounter === 1) {
      threadView.mutableHead.threadCounter = null
    }
    threadView.mutableHead.read =
      this.getMessagesInThreadByMid(threadMid).filter((mid) => !this.storedMessage(mid).head.read).length === 0
    return threadView
  }

  public createFolder(folderName: FolderName): void {
    if (this.folderToMessages.has(folderName)) {
      fail('Такая папка уже существует!')
    }
    this.folderToMessages.set(folderName, new Set<MessageId>())
  }

  public removeFolder(folderName: FolderName): void {
    if (!this.folderToMessages.has(folderName)) {
      fail('Невозможно удалить папку. Такой папки нет!')
    }
    this.folderToMessages.delete(folderName)
  }

  public renameFolder(folderName: FolderName, newFolderName: FolderName): void {
    if (this.folderToMessages.has(newFolderName) || !this.folderToMessages.has(folderName)) {
      fail('Невозможно переименовать. Папки нет, либо папка с таким именем уже существует!')
    }
    this.folderToMessages.set(newFolderName, this.folderToMessages.get(folderName)!)
    this.folderToMessages.delete(folderName)
  }

  private updateTabsToMessages(mid: MessageId, folderName: FolderName): void {
    this.tabsToMessages.forEach((msgIds, _folder) => msgIds.delete(mid))
    if (
      folderName === DefaultFolderName.mailingLists ||
      folderName === DefaultFolderName.socialNetworks ||
      folderName === DefaultFolderName.inbox
    ) {
      this.tabsToMessages.get(folderName)?.add(mid)
    }
  }

  public moveMessageToFolder(mid: MessageId, folderName: FolderName, needUpdateTabsToMessages: boolean = true): void {
    this.folderToMessages.forEach((msgIds, _folder) => msgIds.delete(mid))
    this.demandFolderMessages(folderName).add(mid)

    if (needUpdateTabsToMessages) {
      this.updateTabsToMessages(mid, folderName)
    }
  }

  public createLabel(labelName: LabelName): void {
    if (this.labelToMessages.has(labelName)) {
      fail('Такая метка уже существует!')
    }
    this.labelToMessages.set(labelName, new Set<MessageId>())
  }

  public removeLabel(labelName: LabelName): void {
    if (!this.labelToMessages.has(labelName)) {
      fail('Такой метки нет!')
    }
    this.labelToMessages.delete(labelName)
  }

  public renameLabel(labelName: LabelName, newLabelName: LabelName): void {
    if (this.labelToMessages.has(newLabelName) || !this.labelToMessages.has(labelName)) {
      fail('Невозможно переименовать. Метки нет, либо метка с таким именем уже существует!')
    }
    this.labelToMessages.set(newLabelName, this.labelToMessages.get(labelName)!)
    this.labelToMessages.delete(labelName)
  }

  public applyLabelToMessages(labelName: LabelName, mids: Set<MessageId>): void {
    mids.forEach((mid) => this.labelToMessages.get(labelName)?.add(mid))
  }

  public removeLabelFromMessages(labelName: LabelName, mids: Set<MessageId>): void {
    mids.forEach((mid) => this.labelToMessages.get(labelName)?.delete(mid))
  }

  public getMessageLabels(mid: MessageId): Set<string> {
    const messageLabels = new Set<LabelName>()
    this.labelToMessages.forEach((mids, labelName) => {
      if (mids.has(mid)) {
        messageLabels.add(labelName)
      }
    })
    return messageLabels
  }

  public addMessage(mid: MessageId, msg: FullMessage, folderName: FolderName): void {
    if (!this.folderToMessages.has(folderName)) {
      fail(`Папки ${folderName} нет!`)
    }
    this.messages.set(mid, msg)
    this.folderToMessages.get(folderName)?.add(mid)
  }

  public addThreadMessagesToThreadWithMid(midsToAdd: MessageId[], midInThread: MessageId): void {
    let threadAdded = false
    this.threads.forEach((thread) => {
      if (thread.has(midInThread)) {
        midsToAdd.forEach((midToAdd) => thread.add(midToAdd))
        threadAdded = true
      }
    })
    if (!threadAdded) {
      midsToAdd.push(midInThread)
      this.addThread(midsToAdd)
    }
  }

  public addThread(mids: MessageId[]): void {
    this.threads.push(arrayToSet(mids))
  }

  public storedMessage(mid: MessageId, messageLanguage: Nullable<LanguageName> = null): FullMessage {
    const message = undefinedToNull(this.messages.get(mid))
    if (message === null) {
      fail(`No message with mid ${mid} in model!`)
    }
    if (messageLanguage !== null) {
      const translation = message!.translations.get(messageLanguage)!
      return new FullMessage(
        message!.mutableHead.copy(),
        message!.to,
        translation,
        messageLanguage,
        message!.translations,
        message!.quickReply,
        message!.smartReplies,
      )
    }
    return message!
  }

  public storedFolder(mid: MessageId): FolderName {
    let folderName: Nullable<FolderName> = null
    this.folderToMessages.forEach((msgIds, folder) => {
      for (const msgId of msgIds.values()) {
        if (msgId === mid) {
          folderName = folder
        }
      }
    })

    if (folderName === null) {
      fail(`No folder for message with mid ${mid} in model!`)
    }
    return folderName!
  }

  public removeMessage(id: MessageId): void {
    if (!this.messages.has(id)) {
      fail('No messages with target id')
    }

    // remove message permanently or move to trash folder
    const isInTrash = this.demandFolderMessages(DefaultFolderName.trash).has(id)
    if (isInTrash) {
      this.removeMessagePermanently(id)
    } else {
      this.moveMessageToFolder(id, DefaultFolderName.trash)
    }
  }

  public removeMessagePermanently(id: MessageId): void {
    this.folderToMessages.forEach((msgIds, _folderName) => msgIds.delete(id))
    this.messages.delete(id)
    for (const index of range(0, this.threads.length)) {
      this.threads[index].delete(id)
    }
    this.threads = this.threads.filter((thread) => thread.size !== 0)
  }

  public isContainerEmpty(container: MessageContainer): boolean {
    return (
      this.getMessageIdList(new MessageListDatabaseFilter().withContainer(container).withIsInThreadMode(false))
        .length === 0
    )
  }

  private isMessageInContainer(mid: MessageId, filter: MessageListDatabaseFilter): boolean {
    let isInContainer = false
    const container = filter.getContainer()!
    if (container !== null) {
      if (container.type === MessageContainerType.folder) {
        return this.isMessageInFolder(mid, container.name)
      }
      if (container.type === MessageContainerType.label) {
        isInContainer = this.isMessageInLabel(mid, container.name)
      }
      if (container.type === MessageContainerType.importantFilter) {
        isInContainer = this.isMessageImportant(mid)
      }
      if (container.type === MessageContainerType.unreadFilter) {
        isInContainer = this.isMessageUnread(mid)
      }
      if (container.type === MessageContainerType.search) {
        isInContainer = this.isMessageInSearchRequest(mid, container.name)
        if (
          (this.isMessageInFolder(mid, DefaultFolderName.spam) && filter.getFolder() === DefaultFolderName.spam) ||
          (this.isMessageInFolder(mid, DefaultFolderName.trash) && filter.getFolder() === DefaultFolderName.trash)
        ) {
          return isInContainer
        }
      }
      if (container.type === MessageContainerType.withAttachmentsFilter) {
        isInContainer = this.isMessageWithAttachment(mid)
      }
    }
    return (
      isInContainer &&
      !this.isMessageInFolder(mid, DefaultFolderName.trash) &&
      !this.isMessageInFolder(mid, DefaultFolderName.spam)
    )
  }

  private isMessageInFolder(mid: MessageId, folderName: FolderName): boolean {
    return requireNonNull(this.folderToMessages.get(folderName)?.has(mid), 'Нет такой папки')!
  }

  private isMessageInLabel(mid: MessageId, labelName: LabelName): boolean {
    return requireNonNull(this.labelToMessages.get(labelName)?.has(mid), 'Нет такой метки')!
  }

  private isMessageInSearchRequest(mid: MessageId, request: string): boolean {
    const msg = undefinedToNull(this.messages.get(mid))
    return (
      msg!.body.search(request) !== -1 ||
      this.isOneOfItemRelevantToRequest(msg!.to, request) ||
      msg!.head.subject.search(request) !== -1 ||
      msg!.head.from.search(request) !== -1
    )
  }

  private isMessageImportant(mid: MessageId): boolean {
    return this.messages.get(mid)!.head.important
  }

  private isMessageUnread(mid: MessageId): boolean {
    return !this.messages.get(mid)!.head.read
  }

  private isMessageWithAttachment(mid: MessageId): boolean {
    return this.messages.get(mid)!.head.attachments.length > 0
  }

  private buildMessageIdList(msgs: MessageId[], limit: Int32, isInThreadMode: boolean): MessageId[] {
    const sortedMsgs = this.sortMessagesByTimestamp(msgs)
    if (!isInThreadMode) {
      return sortedMsgs.slice(0, limit)
    }
    const threadedMessages: MessageId[] = []
    const currentAddedThreads = new Set<Int32>()
    for (const mid of sortedMsgs) {
      const threadOrder = this.findThread(mid)
      if (threadOrder === null) {
        threadedMessages.push(mid)
      } else if (!currentAddedThreads.has(threadOrder)) {
        threadedMessages.push(mid)
        currentAddedThreads.add(threadOrder)
      }
    }
    return threadedMessages.slice(0, limit)
  }

  private demandFolderMessages(folderName: FolderName): Set<MessageId> {
    const messages = undefinedToNull(this.folderToMessages.get(folderName))
    if (messages === null) {
      fail(`Модель не знает про папку '${folderName}'! Сначала ее надо создать.`)
    }
    return messages!
  }

  private isOneOfItemRelevantToRequest(items: Set<string>, request: string): boolean {
    let isRelevant = false
    const requestToFind = requireNonNull(request, 'Необходимо задать запрос для поиска!')
    items.forEach((item) => {
      if (item.search(requestToFind) !== -1) {
        isRelevant = true
      }
    })
    return isRelevant
  }

  private sortMessagesByTimestamp(unorderedMsgs: MessageId[]): MessageId[] {
    unorderedMsgs.sort((mid1, mid2) => {
      const diff = int64ToInt32(
        this.makeMessageThreadView(mid2).mutableHead.timestamp - this.makeMessageThreadView(mid1).mutableHead.timestamp,
      )
      if (diff !== 0) {
        return diff
      }
      return int64ToInt32(mid1 - mid2)
    })
    return unorderedMsgs
  }

  private findThread(mid: MessageId): Nullable<Int32> {
    for (const i of range(0, this.threads.length)) {
      if (this.threads[i].has(mid)) {
        return i
      }
    }
    return null
  }

  private getMessageListFromIds(mids: MessageId[], isInThreadMode: boolean): MessageView[] {
    return mids.map((mid) =>
      isInThreadMode ? this.makeMessageThreadView(mid).copy().head : this.storedMessage(mid).copy().head,
    )
  }

  private getMessageHash(message: FullMessage): Int64 {
    const hashBuilder: HashBuilder = new HashBuilder()
      .addString(message.head.from)
      .addBoolean(message.head.read)
      .addString(message.head.subject)
      .addBoolean(message.head.important)
      .addInt64(message.mutableHead.timestamp)
    if (message.head.threadCounter !== null) {
      hashBuilder.addInt64(int64(message.head.threadCounter!))
    } else {
      hashBuilder.addBoolean(true)
    }
    return hashBuilder.build()
  }
}

export class MessageListDatabaseFilter {
  private folder: Nullable<FolderName>
  private label: Nullable<LabelName>
  private excludedFolders: FolderName[]
  private container: Nullable<MessageContainer>
  private isInThreadMode: boolean
  private isImportantOnly: boolean
  private isUnreadOnly: boolean
  private limit: Int32

  public constructor() {
    this.folder = null
    this.label = null
    this.container = null
    this.excludedFolders = []
    this.isInThreadMode = true
    this.limit = 20
    this.isImportantOnly = false
    this.isUnreadOnly = false
  }

  public getIsInThreadMode(): boolean {
    return this.isInThreadMode
  }

  public withIsInThreadMode(value: boolean): MessageListDatabaseFilter {
    this.isInThreadMode = value
    return this
  }

  public getIsUnreadOnly(): boolean {
    return this.isUnreadOnly
  }

  public withIsUnreadOnly(): MessageListDatabaseFilter {
    this.isUnreadOnly = true
    return this
  }

  public getIsImportantOnly(): boolean {
    return this.isImportantOnly
  }

  public withIsImportantOnly(): MessageListDatabaseFilter {
    this.isImportantOnly = true
    return this
  }

  public getContainer(): MessageContainer | null {
    return this.container
  }

  public withContainer(value: MessageContainer): MessageListDatabaseFilter {
    this.container = value
    return this
  }

  public getExcludedFolders(): FolderName[] {
    return this.excludedFolders
  }

  public withExcludedFolders(value: FolderName[]): MessageListDatabaseFilter {
    this.excludedFolders = value
    return this
  }

  public getLabel(): string | null {
    return this.label
  }

  public withLabel(value: string): MessageListDatabaseFilter {
    this.label = value
    return this
  }

  public getFolder(): Nullable<FolderName> {
    return this.folder
  }

  public withFolder(value: FolderName): MessageListDatabaseFilter {
    this.folder = value
    return this
  }

  public withLimit(value: number): MessageListDatabaseFilter {
    this.limit = value
    return this
  }

  public getLimit(): Int32 {
    return this.limit
  }
}
