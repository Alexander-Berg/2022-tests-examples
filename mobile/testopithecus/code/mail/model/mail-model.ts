import { FilterResponseData } from '../../../../mapi/code/api/entities/filters/filter-responses'
import { Contact } from '../../../../mapi/code/api/entities/contact/contact'
import { Int32, Int64, Nullable, range, setToArray, stringToInt32 } from '../../../../../common/ys'
import { Log } from '../../../../common/code/logging/logger'
import { ID } from '../../../../mapi/code/api/common/id'
import { Attachment, MessageMeta } from '../../../../mapi/code/api/entities/message/message-meta'
import { SignaturePlace } from '../../../../mapi/code/api/entities/settings/settings-entities'
import { App, FeatureID, FeatureRegistry } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, DeviceType } from '../../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel } from '../../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { AccountsManager } from '../../../../testopithecus-common/code/users/accounts-manager'
import { UserAccount } from '../../../../testopithecus-common/code/users/user-pool'
import { fail } from '../../../../testopithecus-common/code/utils/error-thrower'
import { copyArray, copySet } from '../../../../testopithecus-common/code/utils/utils'
import { MailboxClient } from '../../client/mailbox-client'
import { reduced } from '../../utils/mail-utils'
import { ApplicationRunningStateFeature } from '../feature/application-running-state-feature'
import { ApplyLabelFeature } from '../feature/apply-label-feature'
import { BackendActionsFeature } from '../feature/backend-actions-feature'
import {
  ArchiveMessageFeature,
  DeleteMessageFeature,
  MarkableImportantFeature,
  MarkableReadFeature,
  SpamableFeature,
} from '../feature/base-action-features'
import {
  ComposeBodyFeature,
  ComposeFeature,
  ComposeSenderSuggestFeature,
  ComposeRecipientFieldsFeature,
  ComposeSubjectFeature,
  ComposeRecipientSuggestFeature,
  WysiwygFeature,
} from '../feature/compose/compose-features'
import {
  FilterConditionLogicFeature,
  FilterCreateOrUpdateRuleFeature,
  FilterUpdateRuleMoreFeature,
  FiltersListFeature,
} from '../feature/settings/filters-features'
import {
  ClearFolderInFolderListFeature,
  FilterNavigatorFeature,
  FolderName,
  FolderNavigatorFeature,
  LabelNavigatorFeature,
} from '../feature/folder-list-features'
import {
  AccountsListFeature,
  CustomMailServiceLoginFeature,
  ExpiringTokenFeature,
  GoogleLoginFeature,
  HotmailLoginFeature,
  Login,
  MailRuLoginFeature,
  MultiAccountFeature,
  OutlookLoginFeature,
  RamblerLoginFeature,
  YahooLoginFeature,
  YandexLoginFeature,
  YandexTeamLoginFeature,
} from '../feature/login-features'
import {
  AttachmentView,
  FullMessageView,
  MessageView,
  MessageViewerFeature,
  MessageViewerAndroidFeature,
  ThreadViewNavigatorFeature,
} from '../feature/mail-view-features'
import {
  CreatableFolderFeature,
  CreatableLabelFeature,
  ManageableFolderFeature,
  ManageableLabelFeature,
} from '../feature/manageable-container-features'
import { ContainerGetterFeature } from '../feature/message-list/container-getter-feature'
import { ContextMenuFeature } from '../feature/message-list/context-menu-feature'
import {
  ExpandableThreadsFeature,
  ExpandableThreadsModelFeature,
} from '../feature/message-list/expandable-threads-feature'
import { GroupModeFeature } from '../feature/message-list/group-mode-feature'
import { LongSwipeFeature } from '../feature/message-list/long-swipe-feature'
import { MessageListDisplayFeature } from '../feature/message-list/message-list-display-feature'
import { ShortSwipeFeature } from '../feature/message-list/short-swipe-feature'
import { StoriesBlockFeature } from '../feature/message-list/stories-block-feature'
import { UndoFeature } from '../feature/message-list/undo-feature'
import { MoveToFolderFeature } from '../feature/move-to-folder-feature'
import { PinFeature } from '../feature/pin-feature'
import { QuickReplyFeature, SmartReplyFeature } from '../feature/quick-reply-features'
import { RotatableFeature } from '../feature/rotatable-feature'
import { AdvancedSearchFeature, SearchFeature, ZeroSuggestFeature } from '../feature/search-features'
import { AboutSettingsFeature } from '../feature/settings/about-settings-feature'
import {
  AccountSettingsFeature,
  AndroidAccountSettingsFeature,
  IosAccountSettingsFeature,
  NotificationOption,
  NotificationSound,
} from '../feature/settings/account-settings-feature'
import {
  AndroidGeneralSettingsFeature,
  GeneralSettingsFeature,
  IosGeneralSettingsFeature,
} from '../feature/settings/general-settings-feature'
import {
  AndroidRootSettingsFeature,
  IOSRootSettingsFeature,
  RootSettingsFeature,
} from '../feature/settings/root-settings-feature'
import { SnapshotValidatingFeature } from '../feature/snapshot-validating-feature'
import { SwitchContext2PaneFeature } from '../feature/switch-context-in-2pane-feature'
import {
  ShtorkaAndroidFeature,
  ShtorkaFeature,
  ShtorkaIOSFeature,
  TabBarAndroidFeature,
  TabBarFeature,
  TabBarIOSFeature,
} from '../feature/tab-bar-feature'
import { TabsFeature } from '../feature/tabs-feature'
import {
  LanguageName,
  TranslatorBarFeature,
  TranslatorLanguageListFeature,
  TranslatorLanguageListSearchFeature,
  TranslatorSettingsFeature,
} from '../feature/translator-features'
import { ValidatorFeature } from '../feature/validator-feature'
import { ArchiveMessageModel } from './base-models/archive-message-model'
import { DeleteMessageModel } from './base-models/delete-message-model'
import { ApplyLabelModel, LabelModel, MarkableImportantModel } from './base-models/label-model'
import { MarkableReadModel } from './base-models/markable-read-model'
import { MoveToFolderModel } from './base-models/move-to-folder-model'
import { SpamableModel } from './base-models/spamable-model'
import { ComposeModel } from './compose/compose-model'
import { WysiwygModel } from './compose/wysiwyg-model'
import {
  FilterConditionLogicModel,
  FilterCreateOrUpdateRuleModel,
  FilterUpdateRuleMoreModel,
  FiltersListModel,
} from './settings/filters-models'
import { DefaultFolderName } from './folder-data-model'
import { ApplicationRunningStateModel } from './general/application-running-state-model'
import { RotatableModel } from './general/rotatable-model'
import { CreatableFolderModel } from './left-column/creatable-folder-model'
import { FolderNavigatorModel } from './left-column/folder-navigator-model'
import { ManageFoldersModel } from './left-column/manage-folders-model'
import { ManageLabelsModel } from './left-column/manage-labels-model'
import { TabsModel } from './left-column/tabs-model'
import { ClearFolderModel } from './left-column/clear-folder-model'
import { AccountManagerModel } from './login/account-manager-model'
import { ExpiringTokenModel } from './login/expiring-token-model'
import { LoginModel } from './login/login-model'
import { MultiAccountModel } from './login/multi-account-model'
import { MailboxModelHasher } from './mailbox-model-hasher'
import { AdvancedSearchModel } from './messages-list/advanced-search-model'
import { ContainerGetterModel } from './messages-list/container-getter-model'
import { ContextMenuModel } from './messages-list/context-menu-model'
import { ExpandableThreadsModel, ReadOnlyExpandableThreadsModel } from './messages-list/expandable-threads-model'
import { GroupModeModel } from './messages-list/group-mode-model'
import { LongSwipeModel } from './messages-list/long-swipe-model'
import { MessageListDisplayModel } from './messages-list/message-list-display-model'
import { ShortSwipeModel } from './messages-list/short-swipe-model'
import { UndoModel } from './messages-list/undo-model'
import { OpenMessageModel } from './opened-message/open-message-model'
import { QuickReplyModel, SmartReplyModel } from './opened-message/quick-reply-models'
import { SearchModel } from './search/search-model'
import { ZeroSuggestModel } from './search/zero-suggest-model'
import { AboutSettingsModel } from './settings/about-settings-model'
import { AccountSettingModel } from './settings/account-settings-model'
import { GeneralSettingsModel } from './settings/general-settings-model'
import { PinModel } from './settings/pin-model'
import { RootSettingsModel } from './settings/root-settings-model'
import { SnapshotValidatingModel } from './snapshot-validating-model'
import { StoriesBlockModel, StoriesBlockViewCounter } from './stories/stories-block-model'
import { MessageListDatabase } from './supplementary/message-list-database'
import { SwitchContextIn2paneModel } from './switch-context-in-2pane-model'
import {
  ShtorkaAndroidModel,
  ShtorkaIOSModel,
  ShtorkaModel,
  TabBarAndroidModel,
  TabBarIOSModel,
  TabBarModel,
} from './tab-bar-model'
import { ValidatorModel } from './validator-model'
import {
  TranslatorBarModel,
  TranslatorLanguageName,
  TranslatorLanguageListModel,
  TranslatorLanguageListSearchModel,
  TranslatorSettingsModel,
} from './translator-models'
import { BackendActionsModel } from './backend-actions-model'

export class ScreenTitle {
  public static readonly rootSettings: string = 'Settings'
  public static readonly helpAndFeedback: string = 'Help and feedback'

  public static folder(folder: FolderName, unreadCounter: Int32): string {
    return `${folder} ${unreadCounter}`
  }
}

export class Message implements MessageView {
  public constructor(
    public from: string,
    public subject: string,
    public timestamp: Int64,
    public firstLine: string,
    public threadCounter: Nullable<Int32> = null,
    public read: boolean = false,
    public important: boolean = false,
    public attachments: AttachmentView[] = [],
    public to: string = '(No recipients)',
  ) {}

  public static fromMeta(meta: MessageMeta): Message {
    return new Message(
      meta.sender,
      meta.subjectText,
      meta.timestamp,
      meta.firstLine,
      meta.threadCount === null ? null : stringToInt32(meta.threadCount!),
      !meta.unread,
      meta.lid.includes('7'),
      meta.attachments === null ? [] : meta.attachments!.attachments.map((item) => MessageAttach.fromMetaAttach(item)),
    )
  }

  public static matches(
    first: MessageView,
    second: MessageView,
    isCompactMode: boolean = false,
    isFull: boolean = false,
  ): boolean {
    // let matchAttachments = true
    let matchFrom: boolean
    const matchImportant: boolean = isCompactMode ? true : first.important === second.important
    // const matchTimestamp: boolean =
    //   first.timestamp - second.timestamp > -24 * 60 * 60 * 1000 &&
    //   first.timestamp - second.timestamp < 24 * 60 * 60 * 1000
    const firstAttachmentsLength: Int32 = first.attachments.length
    const secondAttachmentsLength: Int32 = second.attachments.length

    if (first.from.length > second.from.length) {
      matchFrom = first.from.includes(second.from)
    } else {
      matchFrom = second.from.includes(first.from)
    }
    // TODO: разобраться с селекторами iOS
    // if (firstAttachmentsLength !== secondAttachmentsLength) {
    //   const firstAttachNames = first.attachments.map((attach) => attach.displayName)
    //   const secondAttachNames = second.attachments.map((attach) => attach.displayName)
    //   Log.error(`Different attaches: ${firstAttachNames} and ${secondAttachNames}`)
    //   return false
    // }
    // for (const item of range(0, minInt32(maxInt32(secondAttachmentsLength, 1), firstAttachmentsLength))) {
    //   const secondView = second.attachments[item]
    //   const firstView = first.attachments[item]
    //   if (secondView.displayName !== firstView.displayName) {
    //     return false
    //   }
    // }

    if (first.subject !== second.subject) {
      Log.error(`Different subjects: ${first.subject}, ${second.subject}`)
      return false
    }

    if (first.read !== second.read) {
      Log.error(`Different read status: ${first.read}, ${second.read}`)
      return false
    }

    if (firstAttachmentsLength !== secondAttachmentsLength) {
      Log.error(`Different attachments length: ${firstAttachmentsLength}, ${secondAttachmentsLength}`)
      return false
    }

    if (first.threadCounter !== second.threadCounter) {
      Log.error(`Different thread counter: ${first.threadCounter}, ${second.threadCounter}`)
      return false
    }

    if (first.firstLine.substr(0, 10) !== second.firstLine.substr(0, 10) && !isFull) {
      Log.error(`Different first line: ${first.firstLine.substr(0, 10)}, ${second.firstLine.substr(0, 10)}`)
      return false
    }

    if (!matchImportant) {
      Log.error(`Different importance status: ${first.important}, ${second.important}`)
      return false
    }

    if (!matchFrom) {
      Log.error(`Different froms (one must be included into other): ${first.from}, ${second.from}`)
      return false
    }

    // TODO: time in ios message list is complicated
    // if (!matchTimestamp && isFull) {
    //   Log.error(`Different timestamp (allowed difference no more than 24h): ${first.timestamp}, ${second.timestamp}`)
    //   return false
    // }
    return true
  }

  public threadSize(): Int32 {
    const counter = this.threadCounter
    return counter !== null ? counter : 1
  }

  public tostring(): string {
    const attachNames: string[] = this.attachments.map((attach) => attach.displayName)
    return `MessageView(from=${this.from}, subject=${this.subject}, timestamp=${this.timestamp}, read=${this.read}, important=${this.important}, threadCounter=${this.threadCounter}, firstLine=${this.firstLine}, attachments=${attachNames})`
  }

  public copy(): Message {
    return new Message(
      this.from,
      this.subject,
      this.timestamp,
      this.firstLine,
      this.threadCounter,
      this.read,
      this.important,
      this.attachments,
      this.to,
    )
  }
}

export class MessageAttach implements AttachmentView {
  public constructor(public readonly displayName: string) {}

  public static fromMetaAttach(item: Attachment): AttachmentView {
    return new MessageAttach(item.displayName)
  }
}

export class FullMessage implements FullMessageView {
  public readonly head: MessageView
  public mutableHead: Message // sorry, swift is peace of shit

  public constructor(
    head: Message,
    public to: Set<string> = new Set<string>(),
    public body: string = '',
    public lang: LanguageName = TranslatorLanguageName.english,
    public readonly translations: Map<LanguageName, string> = new Map<LanguageName, string>(),
    public readonly quickReply: boolean = false,
    public readonly smartReplies: string[] = [],
  ) {
    this.head = head
    this.mutableHead = head
  }

  public static fromMeta(
    meta: MessageMeta,
    body: string = '',
    lang: LanguageName = TranslatorLanguageName.english,
    translations: Map<LanguageName, string> = new Map<LanguageName, string>(),
    quickReply: boolean = false,
    smartReplies: string[] = [],
  ): FullMessage {
    return new FullMessage(
      Message.fromMeta(meta),
      new Set<string>(),
      body,
      lang,
      translations,
      quickReply,
      smartReplies,
    )
  }

  public static matches(first: FullMessageView, second: FullMessageView): boolean {
    for (const to of first.to.values()) {
      if (!second.to.has(to)) {
        Log.error(`Different to: ${first.to.values()}, ${second.to.values()}`)
        return false
      }
    }
    if (first.body !== second.body) {
      Log.error(`Different body: ${first.body}, ${second.body}`)
      return false
    }
    return Message.matches(first.head, second.head, false, true)
  }

  public tostring(): string {
    return `(${this.head.tostring()}, to=${this.to.values()}, body=${this.body}, quickReply=${
      this.quickReply
    }, smartReplies=${this.smartReplies})`
  }

  public copy(): FullMessage {
    const translationsCopy = new Map<LanguageName, string>()
    this.translations.forEach((translation, language) => translationsCopy.set(language, translation))
    return new FullMessage(
      this.mutableHead.copy(),
      this.to,
      this.body,
      this.lang,
      translationsCopy,
      this.quickReply,
      this.smartReplies,
    )
  }
}

export type MessageId = ID
export type FolderId = ID

export class MailAppModelHandler {
  public accountsManager: AccountsManager

  public constructor(public accountsData: AccountMailboxData[]) {
    this.accountsManager = new AccountsManager(accountsData.map((data) => data.client.oauthAccount.account))
  }

  public choseAccountFromAccountsManager(account: UserAccount): void {
    this.accountsManager.logInToAccount(account)
  }

  public logInToAccount(account: UserAccount): void {
    this.accountsManager.logInToAccount(account)
  }

  public revokeToken(account: UserAccount): void {
    this.accountsManager.revokeToken(account)
  }

  public exitFromReloginWindow(): void {
    this.accountsManager.exitFromReloginWindow()
  }

  public switchToAccountByOrder(loginOrder: Int32): void {
    this.accountsManager.switchToAccountByOrder(loginOrder)
  }

  public switchToAccountByLogin(login: Login): void {
    this.accountsManager.switchToAccount(login)
  }

  public isLoggedIn(): boolean {
    return this.accountsManager.isLoggedIn()
  }

  public getCurrentAccount(): AccountMailboxData {
    if (!this.hasCurrentAccount()) {
      fail('Account was requested, but is not set')
    }
    return this.accountsData[this.accountsManager.currentAccount!]
  }

  public getCurrentAccountType(): AccountType2 {
    if (!this.hasCurrentAccount()) {
      fail('Account was requested, but is not set')
    }
    return this.accountsData[this.accountsManager.currentAccount!].client.oauthAccount.type
  }

  public getLoggedInAccounts(): UserAccount[] {
    return this.accountsManager.getLoggedInAccounts()
  }

  public hasCurrentAccount(): boolean {
    return (
      this.accountsManager.currentAccount !== null && this.accountsManager.currentAccount! < this.accountsData.length
    )
  }

  public logoutAccount(login: Login): void {
    this.accountsManager.logoutAccount(login)
  }

  public copy(): MailAppModelHandler {
    const accountsDataCopy = this.accountsData.map((acc) => acc.copy())
    const result = new MailAppModelHandler(accountsDataCopy)
    result.accountsManager = this.accountsManager.copy()
    return result
  }
}

export class AccountSettingsModel {
  public folderToNotificationOption: Map<FolderName, NotificationOption>
  public notificationSound: NotificationSound = NotificationSound.yandexMail
  public phoneNumber: string = ''
  public accountUsingEnabled: boolean = true
  public pushNotificationForAllEnabled: boolean = true
  public themeEnabled: boolean = true
  private ignoredFolders: FolderName[] = [
    DefaultFolderName.sent,
    DefaultFolderName.trash,
    DefaultFolderName.spam,
    DefaultFolderName.draft,
    DefaultFolderName.template,
    DefaultFolderName.archive,
    DefaultFolderName.outgoing,
  ]

  public constructor(
    public groupBySubjectEnabled: boolean,
    public sortingEmailsByCategoryEnabled: boolean,
    public signature: string,
    public placeForSignature: SignaturePlace,
    public folderList: FolderName[],
  ) {
    this.folderToNotificationOption = new Map<FolderName, NotificationOption>()
    folderList
      .filter((folderName) => !this.ignoredFolders.includes(folderName))
      .forEach((folderName) => {
        this.folderToNotificationOption.set(folderName as FolderName, NotificationOption.syncAndNotifyMe)
      })
  }
}

export class AccountMailboxData {
  public constructor(
    public readonly client: MailboxClient,
    public messagesDB: MessageListDatabase,
    public defaultEmail: string,
    public aliases: string[],
    public contacts: Contact[],
    public filters: FilterResponseData[],
    public accountSettings: AccountSettingsModel,
    public zeroSuggest: string[],
    public translationLangs: LanguageName[],
    public promoteMail360: boolean,
  ) {}

  public copy(): AccountMailboxData {
    const accountSettingsCopy = new AccountSettingsModel(
      this.accountSettings.groupBySubjectEnabled,
      this.accountSettings.sortingEmailsByCategoryEnabled,
      this.accountSettings.signature,
      this.accountSettings.placeForSignature,
      this.accountSettings.folderList,
    )
    const aliasesCopy = copyArray(this.aliases)
    const contactsCopy = copyArray(this.contacts)
    const filtersCopy = copyArray(this.filters)
    const messagesDBCopy = this.messagesDB.copy()
    const zeroSuggestCopy = copyArray(this.zeroSuggest)
    const translationLangsCopy = copyArray(this.translationLangs)
    return new AccountMailboxData(
      this.client,
      messagesDBCopy,
      this.defaultEmail,
      aliasesCopy,
      contactsCopy,
      filtersCopy,
      accountSettingsCopy,
      zeroSuggestCopy,
      translationLangsCopy,
      this.promoteMail360,
    )
  }
}

export class DeviceTypeModel {
  public static readonly instance: DeviceTypeModel = new DeviceTypeModel()

  private deviceType: DeviceType = DeviceType.Phone

  public getDeviceType(): DeviceType {
    return this.deviceType
  }

  public setDeviceType(deviceType: DeviceType): void {
    this.deviceType = deviceType
  }
}

export class MailboxModel implements AppModel {
  public static allSupportedFeatures: FeatureID[] = [
    MessageListDisplayFeature.get.name,
    FolderNavigatorFeature.get.name,
    MessageViewerFeature.get.name,
    MessageViewerAndroidFeature.get.name,
    ThreadViewNavigatorFeature.get.name,
    MarkableReadFeature.get.name,
    MarkableImportantFeature.get.name,
    GroupModeFeature.get.name,
    RotatableFeature.get.name,
    ExpandableThreadsModelFeature.get.name,
    ExpandableThreadsFeature.get.name,
    DeleteMessageFeature.get.name,
    SpamableFeature.get.name,
    CreatableFolderFeature.get.name,
    WysiwygFeature.get.name,
    ArchiveMessageFeature.get.name,
    YandexLoginFeature.get.name,
    YandexTeamLoginFeature.get.name,
    MultiAccountFeature.get.name,
    ContextMenuFeature.get.name,
    ShortSwipeFeature.get.name,
    LongSwipeFeature.get.name,
    SearchFeature.get.name,
    AdvancedSearchFeature.get.name,
    ZeroSuggestFeature.get.name,
    CreatableLabelFeature.get.name,
    LabelNavigatorFeature.get.name,
    FilterNavigatorFeature.get.name,
    ContainerGetterFeature.get.name,
    StoriesBlockFeature.get.name,
    AccountSettingsFeature.get.name,
    IosAccountSettingsFeature.get.name,
    AndroidAccountSettingsFeature.get.name,
    AboutSettingsFeature.get.name,
    GeneralSettingsFeature.get.name,
    IosGeneralSettingsFeature.get.name,
    AndroidGeneralSettingsFeature.get.name,
    RootSettingsFeature.get.name,
    IOSRootSettingsFeature.get.name,
    AndroidRootSettingsFeature.get.name,
    UndoFeature.get.name,
    MailRuLoginFeature.get.name,
    GoogleLoginFeature.get.name,
    OutlookLoginFeature.get.name,
    HotmailLoginFeature.get.name,
    RamblerLoginFeature.get.name,
    YahooLoginFeature.get.name,
    CustomMailServiceLoginFeature.get.name,
    PinFeature.get.name,
    ApplicationRunningStateFeature.get.name,
    AccountsListFeature.get.name,
    ExpiringTokenFeature.get.name,
    ManageableFolderFeature.get.name,
    ManageableLabelFeature.get.name,
    TabsFeature.get.name,
    SwitchContext2PaneFeature.get.name,
    TranslatorBarFeature.get.name,
    ValidatorFeature.get.name,
    TranslatorLanguageListFeature.get.name,
    TranslatorLanguageListSearchFeature.get.name,
    TranslatorSettingsFeature.get.name,
    QuickReplyFeature.get.name,
    SmartReplyFeature.get.name,
    ApplyLabelFeature.get.name,
    MoveToFolderFeature.get.name,
    ClearFolderInFolderListFeature.get.name,
    BackendActionsFeature.get.name,
    ShtorkaFeature.get.name,
    ShtorkaIOSFeature.get.name,
    ShtorkaAndroidFeature.get.name,
    TabBarFeature.get.name,
    TabBarIOSFeature.get.name,
    TabBarAndroidFeature.get.name,
    SnapshotValidatingFeature.get.name,
    ComposeRecipientFieldsFeature.get.name,
    ComposeRecipientSuggestFeature.get.name,
    ComposeSenderSuggestFeature.get.name,
    ComposeSubjectFeature.get.name,
    ComposeBodyFeature.get.name,
    ComposeFeature.get.name,
    FiltersListFeature.get.name,
    FilterCreateOrUpdateRuleFeature.get.name,
    FilterConditionLogicFeature.get.name,
    FilterUpdateRuleMoreFeature.get.name,
  ]

  public supportedFeatures: FeatureID[] = copyArray(MailboxModel.allSupportedFeatures)

  public readonly containerGetter: ContainerGetterModel
  public readonly messageListDisplay: MessageListDisplayModel
  public readonly markableRead: MarkableReadModel
  public readonly markableImportant: MarkableImportantModel
  public readonly messageNavigator: OpenMessageModel
  public readonly deletableMessages: DeleteMessageModel
  public readonly groupMode: GroupModeModel
  public readonly rotatable: RotatableModel
  public readonly readOnlyExpandableThreads: ReadOnlyExpandableThreadsModel
  public readonly expandableThreads: ExpandableThreadsModel
  public readonly folderNavigator: FolderNavigatorModel
  public readonly creatableFolder: CreatableFolderModel
  public readonly wysiwyg: WysiwygModel
  public readonly login: LoginModel
  public readonly multiAccount: MultiAccountModel
  public readonly contextMenu: ContextMenuModel
  public readonly shortSwipe: ShortSwipeModel
  public readonly longSwipe: LongSwipeModel
  public readonly archiveMessage: ArchiveMessageModel
  public readonly accountSettingsModel: AccountSettingModel
  public readonly generalSettingsModel: GeneralSettingsModel
  public readonly rootSettingsModel: RootSettingsModel
  public readonly aboutSettingsModel: AboutSettingsModel
  public readonly spammable: SpamableModel
  public readonly search: SearchModel
  public readonly advancedSearch: AdvancedSearchModel
  public readonly zeroSuggest: ZeroSuggestModel
  public readonly createLabel: LabelModel
  public readonly manageLabels: ManageLabelsModel
  public readonly manageFolders: ManageFoldersModel
  public readonly storiesBlockModel: StoriesBlockModel
  public readonly undo: UndoModel
  public readonly pin: PinModel
  public readonly applicationState: ApplicationRunningStateModel
  public readonly tabs: TabsModel
  public readonly accountManager: AccountManagerModel
  public readonly expiredToken: ExpiringTokenModel
  public readonly switchContext2Pane: SwitchContextIn2paneModel
  public readonly translatorBarModel: TranslatorBarModel
  public readonly translatorLanguageListModel: TranslatorLanguageListModel
  public readonly translatorLanguageListSearchModel: TranslatorLanguageListSearchModel
  public readonly translatorSettingsModel: TranslatorSettingsModel
  public readonly validatorModel: ValidatorModel
  public readonly quickReplyModel: QuickReplyModel
  public readonly smartReplyModel: SmartReplyModel
  public readonly applyLabelModel: ApplyLabelModel
  public readonly moveToFolderModel: MoveToFolderModel
  public readonly clearFolderModel: ClearFolderModel
  public readonly backendActionsModel: BackendActionsModel
  public readonly shtorkaModel: ShtorkaModel
  public readonly shtorkaIOSModel: ShtorkaIOSModel
  public readonly shtorkaAndroidModel: ShtorkaAndroidModel
  public readonly tabBarModel: TabBarModel
  public readonly tabBarIOSModel: TabBarIOSModel
  public readonly tabBarAndroidModel: TabBarAndroidModel
  public readonly snapshotValidatingModel: SnapshotValidatingModel
  public readonly composeModel: ComposeModel
  public readonly filtersListModel: FiltersListModel
  public readonly filterConditionLogicModel: FilterConditionLogicModel
  public readonly filterUpdateRuleMoreModel: FilterUpdateRuleMoreModel
  public readonly filterCreateOrUpdateRuleModel: FilterCreateOrUpdateRuleModel

  public constructor(public readonly mailAppModelHandler: MailAppModelHandler) {
    const accountsSettings: AccountSettingsModel[] = mailAppModelHandler.accountsData.map((account) => {
      return account.accountSettings
    })
    this.messageListDisplay = new MessageListDisplayModel(this.mailAppModelHandler)
    this.pin = new PinModel()
    this.accountSettingsModel = new AccountSettingModel(
      accountsSettings,
      this.mailAppModelHandler.accountsManager,
      this.mailAppModelHandler,
    )
    this.generalSettingsModel = new GeneralSettingsModel(this.pin)
    this.aboutSettingsModel = new AboutSettingsModel()
    this.rootSettingsModel = new RootSettingsModel(this.messageListDisplay, this.mailAppModelHandler.accountsManager)
    this.advancedSearch = new AdvancedSearchModel(this.messageListDisplay)
    this.markableRead = new MarkableReadModel(this.messageListDisplay, this.mailAppModelHandler)
    this.spammable = new SpamableModel(this.messageListDisplay, this.mailAppModelHandler)
    this.storiesBlockModel = new StoriesBlockModel()
    this.containerGetter = new ContainerGetterModel(this.messageListDisplay)
    this.createLabel = new LabelModel(this.mailAppModelHandler)
    this.manageLabels = new ManageLabelsModel(this.mailAppModelHandler)
    this.manageFolders = new ManageFoldersModel(this.mailAppModelHandler)
    this.archiveMessage = new ArchiveMessageModel(this.messageListDisplay, this.mailAppModelHandler)
    this.deletableMessages = new DeleteMessageModel(this.messageListDisplay, this.mailAppModelHandler)
    this.undo = new UndoModel(
      this.deletableMessages,
      this.archiveMessage,
      this.spammable,
      this.mailAppModelHandler,
      this.messageListDisplay,
    )
    this.filtersListModel = new FiltersListModel(this.accountSettingsModel)
    this.filterUpdateRuleMoreModel = new FilterUpdateRuleMoreModel()
    this.filterConditionLogicModel = new FilterConditionLogicModel()
    this.filterCreateOrUpdateRuleModel = new FilterCreateOrUpdateRuleModel(this.filterConditionLogicModel)
    this.folderNavigator = new FolderNavigatorModel(this.messageListDisplay, this.mailAppModelHandler, this.undo)
    this.markableImportant = new MarkableImportantModel(this.messageListDisplay, this.mailAppModelHandler)
    this.rotatable = new RotatableModel()
    this.applicationState = new ApplicationRunningStateModel()
    this.readOnlyExpandableThreads = new ReadOnlyExpandableThreadsModel(
      this.messageListDisplay,
      this.mailAppModelHandler,
    )
    this.expandableThreads = new ExpandableThreadsModel(this.readOnlyExpandableThreads, this.messageListDisplay)
    this.translatorSettingsModel = new TranslatorSettingsModel()
    this.validatorModel = new ValidatorModel()
    this.translatorBarModel = new TranslatorBarModel(this.translatorSettingsModel)
    this.translatorLanguageListModel = new TranslatorLanguageListModel(
      this.translatorSettingsModel,
      this.mailAppModelHandler,
      this.translatorBarModel,
    )
    this.wysiwyg = new WysiwygModel()
    this.composeModel = new ComposeModel(this.mailAppModelHandler)
    this.quickReplyModel = new QuickReplyModel(this.composeModel)
    this.smartReplyModel = new SmartReplyModel(this.quickReplyModel, this.generalSettingsModel)
    this.messageNavigator = new OpenMessageModel(
      this.markableImportant,
      this.expandableThreads,
      this.messageListDisplay,
      this.createLabel,
      this.deletableMessages,
      this.mailAppModelHandler,
      this.archiveMessage,
      this.translatorBarModel,
      this.translatorLanguageListModel,
      this.translatorSettingsModel,
      this.smartReplyModel,
      this.quickReplyModel,
    )
    this.search = new SearchModel(this.messageListDisplay, this.messageNavigator)
    this.zeroSuggest = new ZeroSuggestModel(this.mailAppModelHandler, this.search, this.messageListDisplay)
    this.creatableFolder = new CreatableFolderModel(this.mailAppModelHandler)
    this.contextMenu = new ContextMenuModel(
      this.deletableMessages,
      this.markableImportant,
      this.markableRead,
      this.mailAppModelHandler,
      this.messageListDisplay,
      this.spammable,
      this.composeModel,
      this.archiveMessage,
      this.messageNavigator,
      this.translatorBarModel,
    )
    this.shortSwipe = new ShortSwipeModel(this.deletableMessages, this.archiveMessage, this.markableRead)
    this.longSwipe = new LongSwipeModel(this.deletableMessages, this.archiveMessage)
    this.login = new LoginModel(mailAppModelHandler, this.messageListDisplay)
    this.multiAccount = new MultiAccountModel(mailAppModelHandler)
    this.accountManager = new AccountManagerModel(mailAppModelHandler)
    this.expiredToken = new ExpiringTokenModel(mailAppModelHandler)
    this.groupMode = new GroupModeModel(
      this.markableRead,
      this.deletableMessages,
      this.archiveMessage,
      this.markableImportant,
      this.spammable,
      this.messageListDisplay,
    )
    this.applyLabelModel = new ApplyLabelModel(
      this.mailAppModelHandler,
      this.messageNavigator,
      this.contextMenu,
      this.messageListDisplay,
      this.groupMode,
      this.createLabel,
    )
    this.moveToFolderModel = new MoveToFolderModel(
      this.mailAppModelHandler,
      this.messageNavigator,
      this.contextMenu,
      this.messageListDisplay,
      this.groupMode,
    )
    this.tabs = new TabsModel(this.messageListDisplay, this.mailAppModelHandler, this.undo)

    const storiesBlockViewCounter = new StoriesBlockViewCounter(this.storiesBlockModel, this.messageListDisplay)
    this.messageListDisplay.attach(storiesBlockViewCounter)
    this.rotatable.attach(storiesBlockViewCounter)
    this.switchContext2Pane = new SwitchContextIn2paneModel()
    this.translatorLanguageListSearchModel = new TranslatorLanguageListSearchModel(this.translatorLanguageListModel)
    this.clearFolderModel = new ClearFolderModel(mailAppModelHandler)
    this.backendActionsModel = new BackendActionsModel(mailAppModelHandler, this.creatableFolder, this.createLabel)
    this.tabBarModel = new TabBarModel(
      this.messageListDisplay,
      this.messageNavigator,
      this.groupMode,
      this.zeroSuggest,
      this.rootSettingsModel,
      this.composeModel,
      this.folderNavigator,
      DeviceTypeModel.instance,
    )
    this.tabBarIOSModel = new TabBarIOSModel(mailAppModelHandler)
    this.tabBarAndroidModel = new TabBarAndroidModel(mailAppModelHandler)
    this.shtorkaModel = new ShtorkaModel(mailAppModelHandler, this.tabBarModel)
    this.shtorkaIOSModel = new ShtorkaIOSModel()
    this.shtorkaAndroidModel = new ShtorkaAndroidModel()
    this.snapshotValidatingModel = new SnapshotValidatingModel()
  }

  public getFeature(feature: FeatureID): any {
    return new FeatureRegistry()
      .register(MessageListDisplayFeature.get, this.messageListDisplay)
      .register(FolderNavigatorFeature.get, this.folderNavigator)
      .register(MessageViewerFeature.get, this.messageNavigator)
      .register(MessageViewerAndroidFeature.get, this.messageNavigator)
      .register(ThreadViewNavigatorFeature.get, this.messageNavigator)
      .register(MarkableReadFeature.get, this.markableRead)
      .register(MarkableImportantFeature.get, this.markableImportant)
      .register(GroupModeFeature.get, this.groupMode)
      .register(RotatableFeature.get, this.rotatable)
      .register(ExpandableThreadsModelFeature.get, this.readOnlyExpandableThreads)
      .register(ExpandableThreadsFeature.get, this.expandableThreads)
      .register(DeleteMessageFeature.get, this.deletableMessages)
      .register(SpamableFeature.get, this.spammable)
      .register(CreatableFolderFeature.get, this.creatableFolder)
      .register(WysiwygFeature.get, this.wysiwyg)
      .register(YandexLoginFeature.get, this.login)
      .register(YandexTeamLoginFeature.get, this.login)
      .register(ContextMenuFeature.get, this.contextMenu)
      .register(ShortSwipeFeature.get, this.shortSwipe)
      .register(LongSwipeFeature.get, this.longSwipe)
      .register(ArchiveMessageFeature.get, this.archiveMessage)
      .register(UndoFeature.get, this.undo)
      .register(MultiAccountFeature.get, this.multiAccount)
      .register(SearchFeature.get, this.search)
      .register(AdvancedSearchFeature.get, this.advancedSearch)
      .register(ZeroSuggestFeature.get, this.zeroSuggest)
      .register(CreatableLabelFeature.get, this.createLabel)
      .register(LabelNavigatorFeature.get, this.folderNavigator)
      .register(FilterNavigatorFeature.get, this.folderNavigator)
      .register(ContainerGetterFeature.get, this.containerGetter)
      .register(AccountSettingsFeature.get, this.accountSettingsModel)
      .register(IosAccountSettingsFeature.get, this.accountSettingsModel)
      .register(AndroidAccountSettingsFeature.get, this.accountSettingsModel)
      .register(AboutSettingsFeature.get, this.aboutSettingsModel)
      .register(GeneralSettingsFeature.get, this.generalSettingsModel)
      .register(IosGeneralSettingsFeature.get, this.generalSettingsModel)
      .register(AndroidGeneralSettingsFeature.get, this.generalSettingsModel)
      .register(RootSettingsFeature.get, this.rootSettingsModel)
      .register(IOSRootSettingsFeature.get, this.rootSettingsModel)
      .register(AndroidRootSettingsFeature.get, this.rootSettingsModel)
      .register(StoriesBlockFeature.get, this.storiesBlockModel)
      .register(MailRuLoginFeature.get, this.login)
      .register(GoogleLoginFeature.get, this.login)
      .register(OutlookLoginFeature.get, this.login)
      .register(HotmailLoginFeature.get, this.login)
      .register(RamblerLoginFeature.get, this.login)
      .register(YahooLoginFeature.get, this.login)
      .register(CustomMailServiceLoginFeature.get, this.login)
      .register(PinFeature.get, this.pin)
      .register(ApplicationRunningStateFeature.get, this.applicationState)
      .register(ManageableFolderFeature.get, this.manageFolders)
      .register(ManageableLabelFeature.get, this.manageLabels)
      .register(TabsFeature.get, this.tabs)
      .register(AccountsListFeature.get, this.accountManager)
      .register(ExpiringTokenFeature.get, this.expiredToken)
      .register(SwitchContext2PaneFeature.get, this.switchContext2Pane)
      .register(TranslatorBarFeature.get, this.translatorBarModel)
      .register(ValidatorFeature.get, this.validatorModel)
      .register(TranslatorLanguageListFeature.get, this.translatorLanguageListModel)
      .register(TranslatorLanguageListSearchFeature.get, this.translatorLanguageListSearchModel)
      .register(TranslatorSettingsFeature.get, this.translatorSettingsModel)
      .register(QuickReplyFeature.get, this.quickReplyModel)
      .register(SmartReplyFeature.get, this.smartReplyModel)
      .register(ApplyLabelFeature.get, this.applyLabelModel)
      .register(MoveToFolderFeature.get, this.moveToFolderModel)
      .register(ClearFolderInFolderListFeature.get, this.clearFolderModel)
      .register(BackendActionsFeature.get, this.backendActionsModel)
      .register(TabBarFeature.get, this.tabBarModel)
      .register(TabBarIOSFeature.get, this.tabBarIOSModel)
      .register(TabBarAndroidFeature.get, this.tabBarAndroidModel)
      .register(ShtorkaFeature.get, this.shtorkaModel)
      .register(ShtorkaIOSFeature.get, this.shtorkaIOSModel)
      .register(ShtorkaAndroidFeature.get, this.shtorkaAndroidModel)
      .register(SnapshotValidatingFeature.get, this.snapshotValidatingModel)
      .register(ComposeRecipientFieldsFeature.get, this.composeModel)
      .register(ComposeRecipientSuggestFeature.get, this.composeModel)
      .register(ComposeSenderSuggestFeature.get, this.composeModel)
      .register(ComposeSubjectFeature.get, this.composeModel)
      .register(ComposeBodyFeature.get, this.composeModel)
      .register(ComposeFeature.get, this.composeModel)
      .register(FilterCreateOrUpdateRuleFeature.get, this.filterCreateOrUpdateRuleModel)
      .register(FiltersListFeature.get, this.filtersListModel)
      .get(feature)
  }

  public copy(): AppModel {
    const accountDataHandlerCopy = this.mailAppModelHandler.copy()
    const model: MailboxModel = new MailboxModel(accountDataHandlerCopy)
    model.supportedFeatures = this.supportedFeatures
    model.messageNavigator.openedMessage = this.messageNavigator.openedMessage
    model.rotatable.landscape = this.rotatable.landscape
    if (this.groupMode.selectedOrders !== null) {
      model.groupMode.selectedOrders = copySet(this.groupMode.selectedOrders)
    }
    for (const id of this.readOnlyExpandableThreads.expanded.values()) {
      model.readOnlyExpandableThreads.expanded.add(id)
    }
    return model
  }

  public getCurrentStateHash(): Int64 {
    return new MailboxModelHasher().getMailboxModelHash(this)
  }

  public async dump(model: App): Promise<string> {
    let s = ''
    if (!this.messageListDisplay.accountDataHandler.hasCurrentAccount()) {
      return 'There is no logged in account'
    }
    const threadMids = this.messageListDisplay.getMessageIdList(10)
    s += `${this.messageListDisplay.getCurrentContainer().name}\n` // todo тут должно быть что-то другое
    for (const i of range(0, threadMids.length)) {
      const threadMid = threadMids[i]
      if (this.messageListDisplay.isInThreadMode()) {
        const thread = this.mailAppModelHandler.getCurrentAccount().messagesDB.makeMessageThreadView(threadMid)
        const msgHead = thread.mutableHead
        const threadSelector = msgHead.threadCounter !== null ? `${msgHead.threadCounter!}v` : ''
        s +=
          `${reduced(threadMid)} ${msgHead.from}\t${msgHead.read ? 'o' : '*'}` +
          `\t${msgHead.subject}\t${threadSelector}\t${msgHead.timestamp}\n`
        if (msgHead.threadCounter !== null) {
          for (const mid of this.mailAppModelHandler
            .getCurrentAccount()
            .messagesDB.getMessagesInThreadByMid(threadMid)) {
            s += this.dumpMessage(mid)
          }
        }
      } else {
        s += this.dumpMessage(threadMid)
      }
    }
    return s
  }

  private dumpMessage(mid: MessageId): string {
    const messagesDB = this.mailAppModelHandler.getCurrentAccount().messagesDB
    const message = messagesDB.storedMessage(mid)
    return (
      `\t\t${reduced(mid)} ${message.head.from}\t${message.head.read ? 'o' : '*'}\t${message.head.subject}` +
      `\t${messagesDB.storedFolder(mid)}\t${message.mutableHead.timestamp}` +
      `\t${setToArray(messagesDB.getMessageLabels(mid)).join(',')}\n`
    )
  }
}
