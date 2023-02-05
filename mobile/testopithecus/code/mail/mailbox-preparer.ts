import { all, promise, reject, resolve } from '../../../../common/xpromise-support'
import {
  Int32,
  int32ToString,
  Int64,
  int64,
  int64ToString,
  Nullable,
  range,
  Throwing,
  undefinedToNull,
  YSError,
} from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { Logger } from '../../../common/code/logging/logger'
import { XPromise } from '../../../common/code/promise/xpromise'
import { getVoid } from '../../../common/code/result/result'
import { ID } from '../../../mapi/code/api/common/id'
import {
  AbookContactName,
  AbookContactsSortType,
  CreateAbookContactData,
  CreateAbookContactsRequestData,
  GetAbookContactsRequestData,
} from '../../../mapi/code/api/entities/abook/abook-request'
import { Contact } from '../../../mapi/code/api/entities/contact/contact'
import {
  CreateUpdateFilterRuleRequestData,
  FilterAction,
  FilterActionType,
  FilterAttachmentType,
  FilterCondition,
  FilterLetterType,
  FilterLogicType,
} from '../../../mapi/code/api/entities/filters/filter-requests'
import { Label, LabelData, LabelType } from '../../../mapi/code/api/entities/label/label'
import { MessageMeta } from '../../../mapi/code/api/entities/message/message-meta'
import { SyncNetwork } from '../../../testopithecus-common/code/client/network/sync-network'
import {
  AccountDataPreparer,
  AccountDataPreparerProvider,
} from '../../../testopithecus-common/code/mbt/test/account-data-preparer'
import { AccountType2, MBTPlatform } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModelProvider } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { OauthService } from '../../../testopithecus-common/code/users/oauth-service'
import { OAuthUserAccount, UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { fail } from '../../../testopithecus-common/code/utils/error-thrower'
import { SyncSleep } from '../../../testopithecus-common/code/utils/sync-sleep'
import { currentTimeMs, getYSError, requireNonNull } from '../../../testopithecus-common/code/utils/utils'
import { MailboxClient } from '../client/mailbox-client'
import { PublicBackendConfig } from '../client/public-backend-config'
import { WebApiRequest } from '../client/web-api-request'
import { formatFolderName, formatLogin } from '../utils/mail-utils'
import { FolderName, LabelName } from './feature/folder-list-features'
import { Imap, ImapFolderDisplay, ImapProvider } from './imap'
import { MailboxDownloader } from './mailbox-downloader'
import { MessageTimeProvider } from './message-time-provider'
import { DefaultFolderName, FolderBackendName, isTab, tabNameToFid } from './model/folder-data-model'

export class FolderSpec {
  public constructor(public name: string, public messages: MessageSpec[]) {}
}

export class UserSpec {
  public constructor(public email: string, public name: string) {}
}

export class AttachmentSpec {
  public constructor(public title: string, public contentType: string, public contentBase64: string) {}

  public static withName(title: string): AttachmentSpec {
    return new AttachmentSpec(title, 'application/octet-stream', 'ZXhhbXBsZQ==')
  }
}

export class MessageSpec {
  public sender: UserSpec
  public subject: string
  public textBody: string
  public timestamp: Date
  public toReceivers: UserSpec[]
  public labels: LabelData[]
  public systemLabels: LabelType[]
  public attachments: AttachmentSpec[]

  public constructor(builder: MessageSpecBuilder) {
    this.sender = requireNonNull(builder.sender, 'Sender required!')
    this.subject = requireNonNull(builder.subject, 'Subject required!')
    this.textBody = requireNonNull(builder.textBody, 'Body text required!')
    this.timestamp = requireNonNull(builder.timestamp, 'Timestamp required!')
    this.toReceivers = builder.toReceivers
    this.labels = builder.labels
    this.attachments = builder.attachments
    this.systemLabels = builder.systemLabels
  }

  public static builder(): MessageSpecBuilder {
    return new MessageSpecBuilder()
  }

  public static create(subject: string, timestamp: Nullable<Date> = null): MessageSpec {
    return this.builder()
      .withSender(new UserSpec('testbotauto@yandex.ru', 'Other User'))
      .withSubject(subject)
      .withTextBody('first line')
      .withTimestamp(timestamp !== null ? timestamp : new Date('2019-07-20T17:03:06.000Z'))
      .build()
  }
}

export class MessageSpecBuilder {
  public sender: Nullable<UserSpec> = null
  public subject: Nullable<string> = null
  public textBody: Nullable<string> = null
  public timestamp: Nullable<Date> = null
  public toReceivers: UserSpec[] = []
  public labels: LabelData[] = []
  public systemLabels: LabelType[] = []
  public attachments: AttachmentSpec[] = []

  public withSender(sender: UserSpec): MessageSpecBuilder {
    this.sender = sender
    return this
  }

  public withSubject(subject: string): MessageSpecBuilder {
    this.subject = subject
    return this
  }

  public withTextBody(textBody: string): MessageSpecBuilder {
    this.textBody = textBody
    return this
  }

  public withTimestamp(timestamp: Date): MessageSpecBuilder {
    this.timestamp = timestamp
    return this
  }

  public addReceiver(receiver: UserSpec): MessageSpecBuilder {
    this.toReceivers.push(receiver)
    return this
  }

  public addLabels(labels: LabelData[]): MessageSpecBuilder {
    labels.forEach((label) => this.labels.push(label))
    return this
  }

  public withSystemLabel(labelType: LabelType): MessageSpecBuilder {
    this.systemLabels.push(labelType)
    return this
  }

  public addAttachments(attachments: AttachmentSpec[]): MessageSpecBuilder {
    attachments.forEach((attachment) => this.attachments.push(attachment))
    return this
  }

  public withDefaults(): MessageSpecBuilder {
    this.sender = new UserSpec('testbotauto@yandex.ru', 'Other User')
    this.subject = 'subj'
    this.textBody = 'first line'
    this.timestamp = new Date('2019-07-20T17:03:06.000Z')
    return this
  }

  public build(): MessageSpec {
    return new MessageSpec(this)
  }
}

export class FilterRule {
  public conditions: FilterCondition[]
  public logic: FilterLogicType
  public actions: FilterAction[]
  public id: Nullable<string>
  public name: string
  public attachment: FilterAttachmentType
  public letter: FilterLetterType
  public stop: boolean
  public enabled: boolean

  public constructor(builder: FilterRuleBuilder) {
    this.conditions = builder.conditions
    this.logic = builder.logic
    this.actions = builder.actions
    this.id = builder.id
    this.name = builder.name
    this.attachment = builder.attachment
    this.letter = builder.letter
    this.stop = builder.stop
    this.enabled = builder.enabled
  }

  public static builder(): FilterRuleBuilder {
    return new FilterRuleBuilder()
  }
}

export class FilterRuleBuilder {
  public conditions: FilterCondition[] = []
  public logic: FilterLogicType = FilterLogicType.and
  public actions: FilterAction[] = []
  public id: Nullable<string> = null
  public name: string = ''
  public attachment: FilterAttachmentType = FilterAttachmentType.all
  public letter: FilterLetterType = FilterLetterType.nospam
  public stop: boolean = false
  public enabled: boolean = true

  public setCondition(condition: FilterCondition): FilterRuleBuilder {
    this.conditions.push(condition)
    return this
  }

  public setLogic(logicType: FilterLogicType): FilterRuleBuilder {
    this.logic = logicType
    return this
  }

  public setAction(action: FilterAction): FilterRuleBuilder {
    this.actions.push(action)
    return this
  }

  public setId(id: string): FilterRuleBuilder {
    this.id = id
    return this
  }

  public setName(name: string): FilterRuleBuilder {
    this.name = name
    return this
  }

  public setAttachmentType(attachmentType: FilterAttachmentType): FilterRuleBuilder {
    this.attachment = attachmentType
    return this
  }

  public setLetter(letterType: FilterLetterType): FilterRuleBuilder {
    this.letter = letterType
    return this
  }

  public setStop(stop: boolean): FilterRuleBuilder {
    this.stop = stop
    return this
  }

  public setEnable(enable: boolean): FilterRuleBuilder {
    this.enabled = enable
    return this
  }

  public build(): FilterRule {
    return new FilterRule(this)
  }
}

export class MailAccountSpec {
  public constructor(public readonly login: string, public readonly password: string, public readonly host: string) {}

  public static fromUserAccount(account: UserAccount, host: string): MailAccountSpec {
    return new MailAccountSpec(account.login, account.password, host)
  }
}

export class PreparingMailbox {
  public mailAccount: MailAccountSpec
  public folders: FolderSpec[] = []
  public labelsWithoutMessage: LabelData[] = []
  public zeroSuggests: string[] = []
  public filters: FilterRule[] = []
  public contacts: Contact[] = []
  public isTabEnabled: boolean = false

  public constructor(builder: MailboxBuilder) {
    this.mailAccount = builder.mailAccount
    builder.folders.forEach((messages, name) => {
      this.folders.push(new FolderSpec(name, messages))
    })
    this.isTabEnabled = builder.isTabEnabled
    builder.labelsWithoutMessage.forEach((label) => this.labelsWithoutMessage.push(label))
    builder.zeroSuggests.forEach((query) => this.zeroSuggests.push(query))
    builder.contacts.forEach((contact) => this.contacts.push(contact))
    builder.filters.forEach((filter) => this.filters.push(filter))
  }
}

export class MailboxPreparerProvider extends AccountDataPreparerProvider<MailboxBuilder> {
  public constructor(
    public readonly platform: MBTPlatform,
    public readonly jsonSerializer: JSONSerializer,
    public readonly network: SyncNetwork,
    public readonly logger: Logger,
    public readonly sleep: SyncSleep,
    public readonly imap: ImapProvider,
  ) {
    super()
  }

  public provide(lockedAccount: UserAccount, type: AccountType2): MailboxBuilder {
    const mailAccount = MailAccountSpec.fromUserAccount(lockedAccount, this.getImapHost(type))
    return new MailboxBuilder(mailAccount, this)
  }

  public provideModelDownloader(
    fulfilledPreparers: MailboxBuilder[],
    accountsWithTokens: OAuthUserAccount[],
  ): AppModelProvider {
    const clients = accountsWithTokens.map(
      (accWithToken) => new MailboxClient(this.platform, accWithToken, this.network, this.jsonSerializer, this.logger),
    )
    return new MailboxDownloader(clients, this.logger)
  }

  public getOAuthAccount(account: UserAccount, type: AccountType2): Throwing<OAuthUserAccount> {
    const token = this.createOauthService().getToken(account, type)
    return new OAuthUserAccount(account, token, type)
  }

  private createOauthService(): OauthService {
    return new OauthService(PublicBackendConfig.mailApplicationCredentials, this.network, this.jsonSerializer)
  }

  private getImapHost(accountType: AccountType2): string {
    switch (accountType) {
      case AccountType2.Yandex:
        return 'imap.yandex.ru'
      case AccountType2.YandexTeam:
        return 'imap.yandex-team.ru'
      case AccountType2.Yahoo:
        return 'imap.mail.yahoo.com'
      case AccountType2.Google:
        return 'imap.google.com'
      case AccountType2.Mail:
        return 'imap.mail.ru'
      case AccountType2.Rambler:
        return 'imap.rambler.ru'
      case AccountType2.Hotmail:
        return 'outlook.office365.com'
      case AccountType2.Outlook:
        return 'outlook.office365.com'
      case AccountType2.Other:
      default:
        return 'imap.yandex.ru'
    }
    return 'imap.yandex.ru'
  }
}

export class MailboxBuilder implements AccountDataPreparer {
  public readonly folders: Map<FolderName, MessageSpec[]> = new Map<FolderName, MessageSpec[]>()
  public readonly labelsWithoutMessage: LabelData[] = []
  private readonly timestampProvider: MessageTimeProvider = new MessageTimeProvider()
  public readonly zeroSuggests: string[] = []
  public readonly contacts: Contact[] = []
  public readonly filters: FilterRule[] = []
  private currentFolder: FolderName = DefaultFolderName.inbox
  public isTabEnabled: boolean = false
  public sendMessagesViaApi: boolean = false

  public constructor(
    public readonly mailAccount: MailAccountSpec,
    private readonly delegate: MailboxPreparerProvider,
  ) {}

  public nextMessage(subject: string): MailboxBuilder {
    const timestamp = this.timestampProvider.nextTime()
    this.addMessageToFolder(this.currentFolder, MessageSpec.create(subject, timestamp))
    return this
  }

  public nextManyMessage(size: Int32): MailboxBuilder {
    for (const i of range(0, size)) {
      this.nextMessage(`Message${i}`)
    }
    return this
  }

  public nextCustomMessage(msg: MessageSpecBuilder): MailboxBuilder {
    const timestamp = this.timestampProvider.nextTime()
    this.addMessageToFolder(this.currentFolder, msg.withTimestamp(timestamp).build())
    return this
  }

  public nextThread(subject: string, threadSize: Int32): MailboxBuilder {
    for (const _ of range(0, threadSize)) {
      this.nextMessage(subject)
    }
    return this
  }

  public switchFolder(folderName: FolderName, parentFolders: FolderName[] = []): MailboxBuilder {
    this.currentFolder = formatFolderName(folderName, parentFolders)
    return this
  }

  public turnOnTab(): MailboxBuilder {
    this.isTabEnabled = true
    this.currentFolder = FolderBackendName.inbox
    return this
  }

  public sendMessageViaMobileApi(): MailboxBuilder {
    this.sendMessagesViaApi = true
    return this
  }

  public addMessageToFolder(
    folderName: FolderName,
    message: MessageSpec,
    parentFolders: FolderName[] = [],
  ): MailboxBuilder {
    this.createFolder(folderName, parentFolders)
    const folderMessage = this.folders.get(formatFolderName(folderName, parentFolders))!
    folderMessage.push(message)
    return this
  }

  public createFolder(folderName: FolderName, parentFolders: FolderName[] = []): MailboxBuilder {
    const folder = formatFolderName(folderName, parentFolders)
    if (!this.folders.has(folder)) {
      this.folders.set(folder, [])
    }
    return this
  }

  public createLabel(label: LabelData): MailboxBuilder {
    if (this.labelsWithoutMessage.filter((labelData) => LabelData.matches(labelData, label)).length === 0) {
      this.labelsWithoutMessage.push(label)
    }
    return this
  }

  public createContact(contact: Contact): MailboxBuilder {
    this.contacts.push(contact)
    return this
  }

  public createFilter(filter: FilterRule): MailboxBuilder {
    this.filters.push(filter)
    return this
  }

  public saveQueryToZeroSuggest(query: string): MailboxBuilder {
    if (!this.zeroSuggests.includes(query)) {
      this.zeroSuggests.push(query)
    }
    return this
  }

  public build(): PreparingMailbox {
    return new PreparingMailbox(this)
  }

  public prepare(account: OAuthUserAccount): XPromise<void> {
    try {
      new WebApiRequest(account.type).enableImap(this.delegate.network, account.oauthToken)
    } catch (e) {
      this.delegate.logger.error(`Включение настройки IMAP завершилось с ошибкой ${e}`)
    }
    const imap = this.delegate.imap.provide(this.mailAccount)
    const client = new MailboxClient(
      this.delegate.platform,
      account,
      this.delegate.network,
      this.delegate.jsonSerializer,
      this.delegate.logger,
    )

    let senderClient: Nullable<MailboxClient> = null
    let senderAccount: Nullable<OAuthUserAccount> = null
    if (this.sendMessagesViaApi) {
      try {
        senderAccount = this.delegate.getOAuthAccount(
          new UserAccount('yndx-message-sender@yandex.ru', 'qwerty123asdf'),
          AccountType2.Yandex,
        )
      } catch (e) {
        return reject(getYSError(e))
      }
      senderClient = new MailboxClient(
        this.delegate.platform,
        senderAccount,
        this.delegate.network,
        this.delegate.jsonSerializer,
        this.delegate.logger,
      )
    }
    const preparer = new MailboxPreparer(imap, client, senderClient, this.delegate.sleep, this.delegate.logger)
    return preparer.prepare(this)
  }
}

export class MailboxPreparer {
  public constructor(
    private imap: Imap,
    private client: MailboxClient,
    private senderClient: Nullable<MailboxClient> = null,
    private syncSleep: SyncSleep,
    private logger: Logger,
  ) {
    client.logger.info('Используй меня полностью')
  }

  public prepare(builder: MailboxBuilder): XPromise<void> {
    const mailbox = builder.build()
    this.logger.info(
      `Готовим ящик ${this.client.oauthAccount.account.login} / ${this.client.oauthAccount.account.password}`,
    )
    return this.connect()
      .flatThen((_) => this.clearMailbox(mailbox.mailAccount))
      .flatThen((_) => this.createMailbox(mailbox))
      .flatThen((_) => this.disconnect())
      .then((_) => this.setTabEnableState(mailbox.isTabEnabled))
      .then((_) => this.waitForSync(mailbox))
      .then((_) => this.adjustMailbox(mailbox))
  }

  private waitForSync(mailbox: PreparingMailbox): void {
    const ttl = int64(5 * 60 * 1000) // 5 min
    const deadline = currentTimeMs() + ttl
    while (currentTimeMs() < deadline) {
      if (this.isInSync(mailbox)) {
        return
      }
      this.syncSleep.sleepMs(5000)
    }
    fail(`Не могу дождаться синхронизации ящика в течение ${ttl}мс`)
  }

  private isInSync(mailbox: PreparingMailbox): boolean {
    for (const folder of mailbox.folders) {
      const fid = this.client.getFolderByName(this.getRelevantIfTab(folder.name), mailbox.isTabEnabled).fid
      const backendMessages = this.client.getMessagesInFolder(fid, folder.messages.length + 1, mailbox.isTabEnabled)
      const actualMessagesCount = backendMessages.length
      const expectedMessagesCount = folder.messages.length
      if (actualMessagesCount !== expectedMessagesCount) {
        this.logger.info(
          `Еще не засинкана папка ${folder.name}, жду ${expectedMessagesCount} писем, а там пока ${actualMessagesCount}`,
        )
        return false
      }
    }
    const foldersCount = mailbox.folders.length
    this.logger.info(`Проверил ${foldersCount} папок, вроде ящик засинкан`)
    return true
  }

  private clearMailbox(account: MailAccountSpec): XPromise<void> {
    this.logger.info(`Clearing mailbox for ${account.login}`)
    return this.imap
      .fetchAllFolders()
      .flatThen((folders) => this.clearFolders(folders))
      .then((_) => this.deleteAllLabels())
  }

  private clearFolders(folders: ImapFolderDisplay[]): XPromise<void> {
    folders.push(new ImapFolderDisplay(DefaultFolderName.trash))
    folders.push(new ImapFolderDisplay('Корзина'))
    // Добавление данных папок в конец списка необходимо для форсированного удаления писем которые были в удаленных пользовательских папках
    let result = resolve(getVoid())
    for (const folder of folders) {
      result = result.flatThen((_) => {
        return this.hasFolder(folder.name).flatThen((has) => {
          return has
            ? this.isDefaultFolder(folder.name)
              ? this.clearFolder(folder.name)
              : this.deleteFolder(folder.name)
            : resolve(getVoid())
        })
      })
    }
    return result
  }

  private createMailbox(mailbox: PreparingMailbox): XPromise<void> {
    this.logger.info(`Creating mailbox for ${mailbox.mailAccount.login}`)
    return this.senderClient !== null
      ? this.createMailboxViaApi(mailbox)
      : this.alll(mailbox.folders.map((f) => this.populateFolder(f)))
  }

  private createMailboxViaApi(mailbox: PreparingMailbox): XPromise<void> {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    const email = formatLogin(self.client.oauthAccount.account.login)
    mailbox.folders
      .filter((folder) => folder.name === DefaultFolderName.inbox)[0]
      .messages.forEach((message) => {
        self.senderClient!.sendMessage(email, message.subject, message.textBody)
      })
    return resolve(getVoid())
  }

  private getMessagesMeta(mailbox: PreparingMailbox): MessageMeta[] {
    const messagesMeta: MessageMeta[] = []
    for (const folder of mailbox.folders) {
      const fid = this.client.getFolderByName(folder.name, mailbox.isTabEnabled).fid
      this.client
        .getMessagesInFolder(fid, folder.messages.length + 1, mailbox.isTabEnabled)
        .forEach((m) => messagesMeta.push(m))
    }
    return messagesMeta
  }

  private getMidsForInboxTab(mailbox: PreparingMailbox, tabName: FolderName): ID[] {
    const messages = mailbox.folders.filter((folder) => folder.name === tabName)[0].messages
    const mids: ID[] = []
    this.client
      .getMessagesInFolder(int64(-10), messages.length + 1, mailbox.isTabEnabled)
      .forEach((m) => mids.push(m.mid))
    return mids
  }

  private adjustMailbox(mailbox: PreparingMailbox): void {
    this.logger.info(`Adjusting mailbox for ${mailbox.mailAccount.login} with backend operations`)
    this.populateLabels(mailbox)
    this.prepareZeroSuggest(mailbox)
    this.prepareContacts(mailbox)
    this.prepareFilters(mailbox)
    mailbox.folders.map((folder) => this.moveMessageToTabIfNeeded(folder.name, mailbox))
  }

  private populateFolder(folder: FolderSpec): XPromise<void> {
    const name = this.getInboxIfTab(folder.name)
    const createFolder: XPromise<void> = this.isDefaultFolder(name)
      ? this.imap.openFolder(name).then((_) => getVoid())
      : this.createFolder(name)
    return createFolder.flatThen((_) => this.alll(folder.messages.map((msg) => this.createMessage(folder.name, msg))))
  }

  private populateLabels(mailbox: PreparingMailbox): void {
    const labelsToMsgTimestamps: Map<LabelName, Int64[]> = new Map<LabelName, Int64[]>()
    const labelToColor: Map<LabelName, string> = new Map<LabelName, string>()
    mailbox.folders.forEach((folder) =>
      folder.messages.forEach((msg) => {
        msg.labels.forEach((label) => {
          if (labelsToMsgTimestamps.has(label.name)) {
            labelsToMsgTimestamps.get(label.name)!.push(int64(msg.timestamp.getTime()))
          } else {
            labelsToMsgTimestamps.set(label.name, [int64(msg.timestamp.getTime())])
          }
          if (!labelToColor.has(label.name)) {
            labelToColor.set(label.name, label.color)
          }
        })
        msg.systemLabels.forEach((labelType) => {
          const lid = this.client.getLabelList().filter((label) => label.type === labelType)[0].lid
          const messagesMeta: ID[] = this.getMessagesMeta(mailbox)
            .filter((message) => int64(msg.timestamp.getTime()) === message.timestamp)
            .map((message) => message.mid)
          this.client.markMessagesWithLabel(messagesMeta, lid)
        })
      }),
    )
    labelsToMsgTimestamps.forEach((timestamps, labelName) => {
      mailbox.labelsWithoutMessage = mailbox.labelsWithoutMessage.filter((label) => label.name !== labelName)
      this.createLabel(new LabelData(labelName, labelToColor.get(labelName)!))
      this.markMessagesWithLabel(new LabelData(labelName), timestamps, mailbox)
    })
    mailbox.labelsWithoutMessage.forEach((label) => this.createLabel(label))
  }

  private prepareZeroSuggest(mailbox: PreparingMailbox): void {
    const currentZeroSuggest = this.getZeroSuggests()
    currentZeroSuggest.forEach((query) => this.deleteQuery(query))
    mailbox.zeroSuggests.forEach((query) => this.saveQuery(query))
  }

  private prepareFilters(mailbox: PreparingMailbox): void {
    const currentFilterIds = this.getFilters()
    currentFilterIds.forEach((id) => this.deleteFilter(id))
    mailbox.filters.forEach((filterData) => {
      // replace folderName/labelName by fid/lid
      filterData.actions.forEach((action) => {
        if (action.key === FilterActionType.applyLabel && action.value !== null) {
          const lid = this.client.getLabelByName(action.value!).lid
          action.value = lid
        }
        if (action.key === FilterActionType.moveToFolder && action.value !== null) {
          const fid = this.client.getFolderByName(action.value!).fid
          action.value = int64ToString(fid)
        }
      })
      const id = this.createFilter(filterData)
      if (!filterData.enabled) {
        this.disableFilter(id)
      }
    })
  }

  private prepareContacts(mailbox: PreparingMailbox): void {
    const contactsToDelete = this.getContactsIds()
    if (contactsToDelete.length > 0) {
      this.deleteContacts(contactsToDelete)
    }
    if (mailbox.contacts.length > 0) {
      this.createContacts(mailbox.contacts)
    }
  }

  private connect(): XPromise<void> {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.setParameter('enable_imap', 'true')
    self.client.setParameter('enable_imap_auth_plain', 'on')
    self.client.setParameter('disable_imap_autoexpunge', '')
    return promise((resolve, reject) => {
      self.imap.connect((error) => {
        self.handle(resolve, reject, error, 'Connected!')
      })
    })
  }

  private disconnect(): XPromise<void> {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return promise((resolve, reject) => {
      self.imap.disconnect((error) => {
        self.handle(resolve, reject, error, `Disconnected!`)
      })
    })
  }

  private clearFolder(folder: string): XPromise<void> {
    this.logger.info(`Clearing folder ${folder}`)
    return this.imap
      .openFolder(folder)
      .flatThen((f) => this.deleteMessages(folder, f.messageCount))
      .flatThen((_) => this.expungeFolder(folder))
  }

  private hasFolder(folder: string): XPromise<boolean> {
    return this.imap.fetchAllFolders().then((folders) => folders.map((f) => f.name).includes(folder))
  }

  private deleteMessages(folder: string, messageCount: Int32): XPromise<void> {
    if (messageCount === 0) {
      return resolve(getVoid())
    }
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return promise((resolve, reject) => {
      self.imap.deleteMessages(folder, messageCount, (error) => {
        self.handle(resolve, reject, error, `Deleted ${messageCount} messages from ${folder}`)
      })
    })
  }

  private createFolder(folder: string): XPromise<void> {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return promise((resolve, reject) => {
      self.imap.createFolder(folder, (error) => {
        self.handle(resolve, reject, error, `[CREATE] Created folder ${folder}`)
      })
    })
  }

  private setTabEnableState(enabled: boolean): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.setParameter('show_folders_tabs', enabled ? 'on' : '')
  }

  private createLabel(label: LabelData): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.createLabel(label)
  }

  private deleteAllLabels(): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.getCustomUserLabelsList().forEach((label) => self.client.deleteLabel(label.lid))
  }

  private markMessagesWithLabel(labelData: LabelData, timestamps: Int64[], mailbox: PreparingMailbox): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    const labelToMark: Label = requireNonNull(
      undefinedToNull(
        this.client
          .getLabelList()
          .filter((label) => label.name! === labelData.name)
          .pop(),
      ),
      `Не смог найти созданную метку с именем ${labelData.name}`,
    )
    const messagesMeta: ID[] = this.getMessagesMeta(mailbox)
      .filter((message) => timestamps.includes(message.timestamp))
      .map((message) => message.mid)
    self.client.markMessagesWithLabel(messagesMeta, labelToMark.lid)
  }

  private moveMessageToTabIfNeeded(tabName: FolderName, mailbox: PreparingMailbox): void {
    if (!isTab(tabName)) {
      return
    }
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    const mids: ID[] = this.getMidsForInboxTab(mailbox, tabName)
    for (const mid of mids) {
      self.client.moveMessageToFolder(mid, tabNameToFid(tabName))
    }
  }

  private createMessage(folder: string, message: MessageSpec): XPromise<void> {
    const folderName = this.getInboxIfTab(folder)
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return promise((resolve, reject) => {
      self.imap.appendMessage(folderName, message, (error) => {
        self.handle(resolve, reject, error, `[APPEND] Created message in folder ${folderName}`)
      })
    })
  }

  private deleteFolder(folder: string): XPromise<void> {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return promise((resolve, reject) => {
      self.imap.deleteFolder(folder, (error) => {
        self.handle(resolve, reject, error, `Deleted folder ${folder}`)
      })
    })
  }

  private expungeFolder(folder: string): XPromise<void> {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return promise((resolve, reject) => {
      self.imap.expungeFolder(folder, (error) => {
        self.handle(resolve, reject, error, `Expunged folder ${folder}`)
      })
    })
  }

  private getContactsIds(): string[] {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return self.client
      .getContacts(new GetAbookContactsRequestData(30, 0, AbookContactsSortType.alpha, null, null))
      .contacts.map((contact) => int32ToString(contact.id))
  }

  private deleteContacts(contactIds: string[]): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.deleteContacts(contactIds)
  }

  private createContacts(contacts: Contact[]): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.createContacts(
      new CreateAbookContactsRequestData(
        contacts.map(
          (contact) =>
            new CreateAbookContactData(
              new AbookContactName(contact.name, null, null, null, null),
              [contact.email],
              null,
              null,
              null,
              null,
            ),
        ),
      ),
    )
  }

  private getZeroSuggests(): string[] {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return self.client.getZeroSuggest().map((suggest) => suggest.show_text)
  }

  private getFilters(): string[] {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return self.client.listFilter().rules.map((rule) => rule.id)
  }

  private deleteFilter(id: string): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.deleteFilter(id)
  }

  private createFilter(data: FilterRule): string {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    return self.client.createFilter(
      new CreateUpdateFilterRuleRequestData(
        data.conditions,
        data.logic,
        data.actions,
        data.id,
        data.name,
        data.attachment,
        data.letter,
        data.stop,
      ),
    ).id
  }

  private disableFilter(id: string): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.disableFilter(id)
  }

  private saveQuery(query: string): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.saveQueryToZeroSuggest(query)
  }

  private deleteQuery(query: string): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this
    self.client.deleteQueryFromZeroSuggest(query)
  }

  private handle(
    resolve: (result: void) => void,
    reject: (error: YSError) => void,
    error: Nullable<YSError>,
    okMessage: string,
  ): void {
    if (error !== null) {
      reject(error!)
    } else {
      this.logger.info(okMessage)
      resolve(getVoid())
    }
  }

  private alll(promises: XPromise<void>[]): XPromise<void> {
    return all(promises).then((_) => getVoid())
  }

  private getInboxIfTab(folderName: FolderName): FolderName {
    return isTab(folderName) ? DefaultFolderName.inbox : folderName
  }

  private getRelevantIfTab(folderName: FolderName): FolderName {
    return isTab(folderName) ? FolderBackendName.inbox : folderName
  }

  private isDefaultFolder(name: string): boolean {
    const defaultNames = [
      'INBOX',
      'INBOX/Social',
      'INBOX/Newsletters',
      'Отправленные',
      'Черновики',
      'Спам',
      'Корзина',
      'Yandex',
      'Drafts|template',
      DefaultFolderName.inbox,
      DefaultFolderName.trash,
      DefaultFolderName.draft,
      DefaultFolderName.template,
      DefaultFolderName.outgoing,
      DefaultFolderName.sent,
      DefaultFolderName.spam,
      DefaultFolderName.archive,
    ]
    return defaultNames.includes(name)
  }
}
