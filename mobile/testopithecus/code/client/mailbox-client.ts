import {
  ApplyFilterRuleRequest,
  CreateFilterRuleRequest,
  CreateUpdateFilterRuleRequestData,
  DeleteFilterRuleRequest,
  DisableFilterRuleRequest,
  EnableFilterRuleRequest,
  ListFilterRuleRequest,
  UpdateFilterRuleRequest,
} from '../../../mapi/code/api/entities/filters/filter-requests'
import {
  filterIdFromJSONItem,
  CreateUpdateFilterRuleResponse,
  filtersFromJSONItem,
  FiltersResponseData,
} from '../../../mapi/code/api/entities/filters/filter-responses'
import {
  AbookContactsResponseData,
  contactIdsFromJSONItem,
  contactsFromJSONItem,
} from '../../../mapi/code/api/entities/abook/abook-responses'
import { TranslateMessageRequest } from '../../../mapi/code/api/entities/translator/translate-message-request'
import {
  translatedMessageFromJSONItem,
  TranslatedMessageResponse,
} from '../../../mapi/code/api/entities/translator/translate-message-response'
import { TranslationLangsRequest } from '../../../mapi/code/api/entities/translator/translator-request'
import {
  translationLangsFromJSONItem,
  TranslationLangsResponse,
} from '../../../mapi/code/api/entities/translator/translator-response'
import { Result } from '../../../common/code/result/result'
import { SyncNetwork } from '../../../testopithecus-common/code/client/network/sync-network'
import { Int32, Nullable } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { ArrayJSONItem, JSONItem, JSONItemGetDebugDescription, MapJSONItem } from '../../../common/code/json/json-types'
import { Logger } from '../../../common/code/logging/logger'
import { ID, LabelID } from '../../../mapi/code/api/common/id'
import {
  ABookTopRequest,
  CreateAbookContactsRequest,
  CreateAbookContactsRequestData,
  DeleteAbookContactsRequest,
  GetAbookContactsRequest,
  GetAbookContactsRequestData,
} from '../../../mapi/code/api/entities/abook/abook-request'
import { ArchiveMessagesNetworkRequest } from '../../../mapi/code/api/entities/actions/archive-messages-network-request'
import { DeleteMessagesNetworkRequest } from '../../../mapi/code/api/entities/actions/delete-messages-network-request'
import { MarkReadNetworkRequest } from '../../../mapi/code/api/entities/actions/mark-read-network-request'
import { MarkSpamNetworkRequest } from '../../../mapi/code/api/entities/actions/mark-spam-network-request'
import { MarkWithLabelsNetworkRequest } from '../../../mapi/code/api/entities/actions/mark-with-labels-network-request'
import { MoveToFolderNetworkRequest } from '../../../mapi/code/api/entities/actions/move-to-folder-network-request'
import { SendMailNetworkRequest } from '../../../mapi/code/api/entities/actions/send-mail-network-request'
import {
  SetParametersItem,
  SetParametersItems,
  SetParametersRequest,
} from '../../../mapi/code/api/entities/actions/set-parameters-request'
import { MessageBodyPayload, messageBodyResponseFromJSONItem } from '../../../mapi/code/api/entities/body/message-body'
import { MessageBodyRequest } from '../../../mapi/code/api/entities/body/message-body-request'
import { Contact, contactFromABookTopJSONItem } from '../../../mapi/code/api/entities/contact/contact'
import { ContainersRequest } from '../../../mapi/code/api/entities/container/containers-request'
import { MailSendRequestBuilder } from '../../../mapi/code/api/entities/draft/mail-send-request'
import { CreateFolderRequest } from '../../../mapi/code/api/entities/folder/create-folder-request'
import { Folder, folderFromJSONItem } from '../../../mapi/code/api/entities/folder/folder'
import { CreateLabelRequest } from '../../../mapi/code/api/entities/label/create-label-request'
import { DeleteLabelRequest } from '../../../mapi/code/api/entities/label/delete-label-request'
import { Label, LabelData, labelFromJSONItem, LabelType } from '../../../mapi/code/api/entities/label/label'
import { MessageMeta } from '../../../mapi/code/api/entities/message/message-meta'
import { MessageRequestItem } from '../../../mapi/code/api/entities/message/message-request-item'
import { MessagesRequestPack } from '../../../mapi/code/api/entities/message/messages-request-pack'
import { messageResponseFromJSONItem } from '../../../mapi/code/api/entities/message/messages-response'
import { SearchQueryRequest } from '../../../mapi/code/api/entities/search/search-query-request'
import { searchResponseFromJSONItem } from '../../../mapi/code/api/entities/search/search-response'
import {
  DeleteQueryFromZeroSuggestRequest,
  SaveQueryToZeroSuggestRequest,
  SuggestRequest,
  ZeroSuggestRequest,
} from '../../../mapi/code/api/entities/search/suggest-request'
import {
  suggestFromJSONItem,
  SuggestResponse,
  zeroSuggestFromJSONItem,
  ZeroSuggestResponse,
} from '../../../mapi/code/api/entities/search/suggest-response'
import {
  SettingsResponse,
  settingsResponseFromJSONItem,
} from '../../../mapi/code/api/entities/settings/settings-entities'
import { SettingsRequest } from '../../../mapi/code/api/entities/settings/settings-request'
import { DefaultNetworkInterceptor } from '../../../mapi/code/api/network/default-network-interceptor'
import { NetworkExtra } from '../../../mapi/code/api/network/network-extra'
import { NetworkRequest } from '../../../common/code/network/network-request'
import { Platform, PlatformType } from '../../../common/code/network/platform'
import { FullMessage, MessageId } from '../mail/model/mail-model'
import { MBTPlatform } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AccountsManager } from '../../../testopithecus-common/code/users/accounts-manager'
import { OAuthUserAccount, UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { fail } from './../../../testopithecus-common/code/utils/error-thrower'
import { PublicBackendConfig } from './public-backend-config'

export class MailboxClient {
  public constructor(
    private readonly platform: MBTPlatform,
    public readonly oauthAccount: OAuthUserAccount,
    private network: SyncNetwork,
    private jsonSerializer: JSONSerializer,
    public logger: Logger,
  ) {}

  public getFolderList(withTabs: boolean = false): Folder[] {
    const request = new ContainersRequest(withTabs)
    const jsonArray = this.getJsonResponse(request) as ArrayJSONItem
    const folders: Folder[] = []
    jsonArray.asArray().forEach((folderItem) => {
      const fid = (folderItem as MapJSONItem).get('fid')
      if (fid !== null) {
        folders.push(folderFromJSONItem(folderItem)!)
      }
    })
    return folders
  }

  public getFolderByName(name: string, withTabs: boolean = false): Folder {
    const result = this.getFolderList(withTabs).filter((f) => f.name === name)
    if (result.length === 0) {
      fail(`На бэке нет папки '${name}'!`)
    }
    return result[0]
  }

  public getLabelList(): Label[] {
    const request = new ContainersRequest()
    const jsonArray = this.getJsonResponse(request) as ArrayJSONItem
    const labels: Label[] = []
    jsonArray.asArray().forEach((labelItem) => {
      const lid = (labelItem as MapJSONItem).get('lid')
      if (lid !== null) {
        labels.push(labelFromJSONItem(labelItem)!)
      }
    })
    return labels
  }

  public getLabelByName(name: string): Label {
    const result = this.getLabelList().filter((l) => l.name === name)
    if (result.length === 0) {
      fail(`На бэке нет папки '${name}'!`)
    }
    return result[0]
  }

  public getCustomUserLabelsList(): Label[] {
    return this.getLabelList().filter((label) => label.type === LabelType.user)
  }

  public getAllContactsList(limit: Int32): Contact[] {
    const request = new ABookTopRequest(limit)
    return this.getContactsList(request)
  }

  public getContacts(data: GetAbookContactsRequestData): AbookContactsResponseData {
    const request = new GetAbookContactsRequest(data)
    const response = this.getJsonResponse(request) as MapJSONItem

    return contactsFromJSONItem(response)!
  }

  public createContacts(newContacts: CreateAbookContactsRequestData): string[] {
    const request = new CreateAbookContactsRequest(newContacts)
    const response = this.getJsonResponse(request) as MapJSONItem

    return contactIdsFromJSONItem(response)!
  }

  public deleteContacts(contactIds: string[]): string[] {
    const request = new DeleteAbookContactsRequest(contactIds)
    const response = this.getJsonResponse(request) as MapJSONItem

    return contactIdsFromJSONItem(response)!
  }

  public getMessagesInFolder(fid: ID, limit: Int32, withTabs: boolean = false): MessageMeta[] {
    const messageRequestItem = MessageRequestItem.messagesInFolder(fid, 0, limit)
    const request = new MessagesRequestPack([messageRequestItem], false, withTabs)
    return this.getMessagesList(request)
  }

  public getThreadsInFolder(fid: ID, limit: Int32, withTabs: boolean = false): MessageMeta[] {
    const messageRequestItem = MessageRequestItem.threads(fid, 0, limit)
    const request = new MessagesRequestPack([messageRequestItem], false, withTabs)
    return this.getMessagesList(request)
  }

  public getMessagesInThread(tid: ID, limit: Int32, withTabs: boolean = false): MessageMeta[] {
    const messageRequestItem = MessageRequestItem.messagesInThread(tid, 0, limit)
    const request = new MessagesRequestPack([messageRequestItem], false, withTabs)
    return this.getMessagesList(request)
  }

  public getSettings(): SettingsResponse {
    const request = new SettingsRequest()
    const response = this.getJsonResponse(request)
    return settingsResponseFromJSONItem(response)!
  }

  public createFilter(data: CreateUpdateFilterRuleRequestData): CreateUpdateFilterRuleResponse {
    const request = new CreateFilterRuleRequest(data)
    const response = this.getJsonResponse(request, 3)
    return filterIdFromJSONItem(response)!
  }

  public updateFilter(data: CreateUpdateFilterRuleRequestData): CreateUpdateFilterRuleResponse {
    const request = new UpdateFilterRuleRequest(data)
    const response = this.getJsonResponse(request)
    return filterIdFromJSONItem(response)!
  }

  public listFilter(): FiltersResponseData {
    const request = new ListFilterRuleRequest()
    const response = this.getJsonResponse(request)
    return filtersFromJSONItem(response)!
  }

  public applyFilter(id: string): void {
    const request = new ApplyFilterRuleRequest(id)
    this.executeRequest(request)
  }

  public deleteFilter(id: string): void {
    const request = new DeleteFilterRuleRequest(id)
    this.executeRequest(request)
  }

  public disableFilter(id: string): void {
    const request = new DisableFilterRuleRequest(id)
    this.executeRequest(request)
  }

  public enableFilter(id: string): void {
    const request = new EnableFilterRuleRequest(id)
    this.executeRequest(request)
  }

  public markMessageAsRead(mid: ID): void {
    const request = new MarkReadNetworkRequest([mid], [], true)
    this.executeRequest(request)
  }

  public markMessageAsUnread(mid: ID): void {
    const request = new MarkReadNetworkRequest([mid], [], false)
    this.executeRequest(request)
  }

  public markThreadAsRead(tid: ID): void {
    const request = new MarkReadNetworkRequest([], [tid], true)
    this.executeRequest(request)
  }

  public markThreadAsUnread(tid: ID): void {
    const request = new MarkReadNetworkRequest([], [tid], false)
    this.executeRequest(request)
  }

  public markMessagesWithLabel(mids: ID[], lid: LabelID): void {
    const request = new MarkWithLabelsNetworkRequest(mids, [], [lid], true)
    this.executeRequest(request)
  }

  public unmarkMessagesWithLabel(mids: ID[], lid: LabelID): void {
    const request = new MarkWithLabelsNetworkRequest(mids, [], [lid], false)
    this.executeRequest(request)
  }

  public markThreadWithLabel(tid: ID, lid: LabelID): void {
    const request = new MarkWithLabelsNetworkRequest([], [tid], [lid], true)
    this.executeRequest(request)
  }

  public unmarkThreadWithLabel(tid: ID, lid: LabelID): void {
    const request = new MarkWithLabelsNetworkRequest([], [tid], [lid], false)
    this.executeRequest(request)
  }

  public removeMessageByThreadId(fid: ID, tid: ID): void {
    const request = new DeleteMessagesNetworkRequest([], [tid], fid)
    this.executeRequest(request)
  }

  public moveThreadToFolder(tid: ID, fid: ID): void {
    const request = new MoveToFolderNetworkRequest([], [tid], fid, fid) // TODO
    this.executeRequest(request)
  }

  public moveMessageToFolder(mid: ID, fid: ID): void {
    const request = new MoveToFolderNetworkRequest([mid], [], fid, fid) // TODO
    this.executeRequest(request)
  }

  public createFolder(name: string): void {
    const request = new CreateFolderRequest(name, null, null)
    this.executeRequest(request)
  }

  public createLabel(label: LabelData): void {
    const request = new CreateLabelRequest(label)
    this.executeRequest(request)
  }

  public deleteLabel(lid: LabelID): void {
    const request = new DeleteLabelRequest(lid)
    this.executeRequest(request)
  }

  public sendMessage(to: string, subject: string, text: string, references: Nullable<string> = null): void {
    const settings = this.getSettings()
    const composeCheck = settings.payload!.accountInformation.composeCheck
    const task = new MailSendRequestBuilder()
      .setTo(to)
      .setComposeCheck(composeCheck)
      .setSubject(subject)
      .setBody(text)
      .setReferences(references)
      .build()
    this.getJsonResponse(new SendMailNetworkRequest(task))
  }

  public getMessageBody(mid: ID): MessageBodyPayload {
    const request = new MessageBodyRequest([mid], true)
    const json = this.getJsonResponse(request)
    const response = messageBodyResponseFromJSONItem(json)
    return response![0].payload!
  }

  public getMessageReference(mid: ID): string {
    return this.getMessageBody(mid).info.rfcId
  }

  public setParameter(key: string, value: string): void {
    const request = new SetParametersRequest(new SetParametersItems([new SetParametersItem(key, value)]))
    this.executeRequest(request)
  }

  public moveToSpam(fid: ID, tid: ID): void {
    const request = new MarkSpamNetworkRequest([], [tid], fid, true)
    this.executeRequest(request)
  }

  public archive(local: string, tid: ID): void {
    const request = new ArchiveMessagesNetworkRequest([], [tid], local)
    this.executeRequest(request)
  }

  public getSearchResults(query: string): Map<MessageId, FullMessage> {
    const request = new SearchQueryRequest(query)
    const messages: Map<MessageId, FullMessage> = new Map()
    const response = searchResponseFromJSONItem(this.getJsonResponse(request))
    response!.messages.forEach((message) => {
      messages.set(message.mid, FullMessage.fromMeta(message))
    })
    return messages
  }

  public getZeroSuggest(): ZeroSuggestResponse[] {
    const request = new ZeroSuggestRequest()
    const response = this.getJsonResponse(request) as ArrayJSONItem
    const suggests: ZeroSuggestResponse[] = []

    response.asArray().forEach((suggest) => {
      suggests.push(zeroSuggestFromJSONItem(suggest)!)
    })

    return suggests
  }

  public getTranslationLangs(): TranslationLangsResponse {
    const request = new TranslationLangsRequest()
    const response = this.getJsonResponse(request) as MapJSONItem

    return translationLangsFromJSONItem(response)!
  }

  public translateMessage(mid: ID, targetLang: string): TranslatedMessageResponse {
    const request = new TranslateMessageRequest(targetLang, mid.toString())
    const response = this.getJsonResponse(request) as MapJSONItem

    return translatedMessageFromJSONItem(response)!
  }

  public saveQueryToZeroSuggest(query: string): void {
    const request = new SaveQueryToZeroSuggestRequest(query)
    this.executeRequest(request)
  }

  public deleteQueryFromZeroSuggest(query: string): void {
    const request = new DeleteQueryFromZeroSuggestRequest(query)
    this.executeRequest(request)
  }

  public getSuggests(query: string): SuggestResponse[] {
    const request = new SuggestRequest(query)
    const response = this.getJsonResponse(request) as ArrayJSONItem
    const suggests: SuggestResponse[] = []

    response.asArray().forEach((suggest) => {
      suggests.push(suggestFromJSONItem(suggest)!)
    })

    return suggests
  }

  private getMessagesList(request: NetworkRequest): MessageMeta[] {
    const response = this.getJsonResponse(request)
    const messageResponse = messageResponseFromJSONItem(response)!
    const messages: MessageMeta[] = []
    messageResponse.payload![0].items.forEach((message) => {
      messages.push(message)
    })
    // messages.sort((m1, m2) => int64ToInt32(m2.timestamp - m1.timestamp));
    return messages
  }

  private getContactsList(request: NetworkRequest): Contact[] {
    const jsonMap = this.getJsonResponse(request) as MapJSONItem
    const contacts: Contact[] = []
    const jsonContactArray = jsonMap.getMap('contacts')!.get('contact') as ArrayJSONItem
    jsonContactArray.asArray().forEach((contactItem) => {
      const contact = contactFromABookTopJSONItem(contactItem)
      if (contact !== null) {
        contacts.push(contact)
      }
    })
    return contacts
  }

  private getJsonResponse(request: NetworkRequest, retries: Int32 = 0): JSONItem {
    const jsonString = this.executeRequest(request, retries)
    const response = this.jsonSerializer.deserialize(jsonString)
    this.logger.info(JSONItemGetDebugDescription(response.getValue()))
    return response.getValue()
  }

  private executeRequest(request: NetworkRequest, retries: Int32 = 0): string {
    const host = PublicBackendConfig.baseUrl(this.oauthAccount.type)
    const requestEnricher = new DefaultNetworkInterceptor(
      new PlatformImpl(this.convertPlatform(this.platform), false),
      'testopithecus',
      () => new NetworkExtra(true, 'fake'),
    )
    const result: Result<string> = this.network.syncExecuteWithRetries(
      retries,
      host,
      requestEnricher.interceptSync(request),
      this.oauthAccount.oauthToken,
    )
    if (result.isError()) {
      fail(result.getError().message)
    }
    return result.getValue()
  }

  private convertPlatform(platform: MBTPlatform): PlatformType {
    switch (platform) {
      case MBTPlatform.IOS:
        return PlatformType.ios
      case MBTPlatform.Desktop:
        return PlatformType.electron
      default:
        return PlatformType.android
    }
  }
}

export class MailboxClientHandler {
  public clientsManager: AccountsManager

  public constructor(public mailboxClients: MailboxClient[]) {
    this.clientsManager = new AccountsManager(mailboxClients.map((client) => client.oauthAccount.account))
  }

  public loginToAccount(account: UserAccount): void {
    this.clientsManager.logInToAccount(account)
  }

  public switchToClientForAccountWithLogin(login: string): void {
    this.clientsManager.switchToAccount(login)
  }

  public getCurrentClient(): MailboxClient {
    return this.mailboxClients[this.clientsManager.currentAccount!]
  }

  public getLoggedInAccounts(): UserAccount[] {
    return this.clientsManager.getLoggedInAccounts()
  }

  public revokeToken(account: UserAccount): void {
    return this.clientsManager.revokeToken(account)
  }

  public exitFromReloginWindow(): void {
    this.clientsManager.exitFromReloginWindow()
  }
}

class PlatformImpl implements Platform {
  public constructor(public readonly type: PlatformType, public readonly isTablet: boolean) {}
}
