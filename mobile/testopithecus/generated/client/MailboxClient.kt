// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/mailbox-client.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class MailboxClient(private val platform: MBTPlatform, val oauthAccount: OAuthUserAccount, private var network: SyncNetwork, private var jsonSerializer: JSONSerializer, var logger: Logger) {
    open fun getFolderList(withTabs: Boolean = false): YSArray<Folder> {
        val request = ContainersRequest(withTabs)
        val jsonArray = this.getJsonResponse(request) as ArrayJSONItem
        val folders: YSArray<Folder> = mutableListOf()
        jsonArray.asArray().forEach(__LBL__MailboxClient_1@ {
            folderItem ->
            val fid = (folderItem as MapJSONItem).`get`("fid")
            if (fid != null) {
                folders.add(folderFromJSONItem(folderItem)!!)
            }
        })
        return folders
    }

    open fun getFolderByName(name: String, withTabs: Boolean = false): Folder {
        val result = this.getFolderList(withTabs).filter( {
            f ->
            f.name == name
        })
        if (result.size == 0) {
            fail("На бэке нет папки '${name}'!")
        }
        return result[0]
    }

    open fun getLabelList(): YSArray<Label> {
        val request = ContainersRequest()
        val jsonArray = this.getJsonResponse(request) as ArrayJSONItem
        val labels: YSArray<Label> = mutableListOf()
        jsonArray.asArray().forEach(__LBL__MailboxClient_2@ {
            labelItem ->
            val lid = (labelItem as MapJSONItem).`get`("lid")
            if (lid != null) {
                labels.add(labelFromJSONItem(labelItem)!!)
            }
        })
        return labels
    }

    open fun getLabelByName(name: String): Label {
        val result = this.getLabelList().filter( {
            l ->
            l.name == name
        })
        if (result.size == 0) {
            fail("На бэке нет папки '${name}'!")
        }
        return result[0]
    }

    open fun getCustomUserLabelsList(): YSArray<Label> {
        return this.getLabelList().filter( {
            label ->
            label.type == LabelType.user
        })
    }

    open fun getAllContactsList(limit: Int): YSArray<Contact> {
        val request = ABookTopRequest(limit)
        return this.getContactsList(request)
    }

    open fun getContacts(data: GetAbookContactsRequestData): AbookContactsResponseData {
        val request = GetAbookContactsRequest(data)
        val response = this.getJsonResponse(request) as MapJSONItem
        return contactsFromJSONItem(response)!!
    }

    open fun createContacts(newContacts: CreateAbookContactsRequestData): YSArray<String> {
        val request = CreateAbookContactsRequest(newContacts)
        val response = this.getJsonResponse(request) as MapJSONItem
        return contactIdsFromJSONItem(response)!!
    }

    open fun deleteContacts(contactIds: YSArray<String>): YSArray<String> {
        val request = DeleteAbookContactsRequest(contactIds)
        val response = this.getJsonResponse(request) as MapJSONItem
        return contactIdsFromJSONItem(response)!!
    }

    open fun getMessagesInFolder(fid: ID, limit: Int, withTabs: Boolean = false): YSArray<MessageMeta> {
        val messageRequestItem = MessageRequestItem.messagesInFolder(fid, 0, limit)
        val request = MessagesRequestPack(mutableListOf(messageRequestItem), false, withTabs)
        return this.getMessagesList(request)
    }

    open fun getThreadsInFolder(fid: ID, limit: Int, withTabs: Boolean = false): YSArray<MessageMeta> {
        val messageRequestItem = MessageRequestItem.threads(fid, 0, limit)
        val request = MessagesRequestPack(mutableListOf(messageRequestItem), false, withTabs)
        return this.getMessagesList(request)
    }

    open fun getMessagesInThread(tid: ID, limit: Int, withTabs: Boolean = false): YSArray<MessageMeta> {
        val messageRequestItem = MessageRequestItem.messagesInThread(tid, 0, limit)
        val request = MessagesRequestPack(mutableListOf(messageRequestItem), false, withTabs)
        return this.getMessagesList(request)
    }

    open fun getSettings(): SettingsResponse {
        val request = SettingsRequest()
        val response = this.getJsonResponse(request)
        return settingsResponseFromJSONItem(response)!!
    }

    open fun createFilter(data: CreateUpdateFilterRuleRequestData): CreateUpdateFilterRuleResponse {
        val request = CreateFilterRuleRequest(data)
        val response = this.getJsonResponse(request, 3)
        return filterIdFromJSONItem(response)!!
    }

    open fun updateFilter(data: CreateUpdateFilterRuleRequestData): CreateUpdateFilterRuleResponse {
        val request = UpdateFilterRuleRequest(data)
        val response = this.getJsonResponse(request)
        return filterIdFromJSONItem(response)!!
    }

    open fun listFilter(): FiltersResponseData {
        val request = ListFilterRuleRequest()
        val response = this.getJsonResponse(request)
        return filtersFromJSONItem(response)!!
    }

    open fun applyFilter(id: String): Unit {
        val request = ApplyFilterRuleRequest(id)
        this.executeRequest(request)
    }

    open fun deleteFilter(id: String): Unit {
        val request = DeleteFilterRuleRequest(id)
        this.executeRequest(request)
    }

    open fun disableFilter(id: String): Unit {
        val request = DisableFilterRuleRequest(id)
        this.executeRequest(request)
    }

    open fun enableFilter(id: String): Unit {
        val request = EnableFilterRuleRequest(id)
        this.executeRequest(request)
    }

    open fun markMessageAsRead(mid: ID): Unit {
        val request = MarkReadNetworkRequest(mutableListOf(mid), mutableListOf(), true)
        this.executeRequest(request)
    }

    open fun markMessageAsUnread(mid: ID): Unit {
        val request = MarkReadNetworkRequest(mutableListOf(mid), mutableListOf(), false)
        this.executeRequest(request)
    }

    open fun markThreadAsRead(tid: ID): Unit {
        val request = MarkReadNetworkRequest(mutableListOf(), mutableListOf(tid), true)
        this.executeRequest(request)
    }

    open fun markThreadAsUnread(tid: ID): Unit {
        val request = MarkReadNetworkRequest(mutableListOf(), mutableListOf(tid), false)
        this.executeRequest(request)
    }

    open fun markMessagesWithLabel(mids: YSArray<ID>, lid: LabelID): Unit {
        val request = MarkWithLabelsNetworkRequest(mids, mutableListOf(), mutableListOf(lid), true)
        this.executeRequest(request)
    }

    open fun unmarkMessagesWithLabel(mids: YSArray<ID>, lid: LabelID): Unit {
        val request = MarkWithLabelsNetworkRequest(mids, mutableListOf(), mutableListOf(lid), false)
        this.executeRequest(request)
    }

    open fun markThreadWithLabel(tid: ID, lid: LabelID): Unit {
        val request = MarkWithLabelsNetworkRequest(mutableListOf(), mutableListOf(tid), mutableListOf(lid), true)
        this.executeRequest(request)
    }

    open fun unmarkThreadWithLabel(tid: ID, lid: LabelID): Unit {
        val request = MarkWithLabelsNetworkRequest(mutableListOf(), mutableListOf(tid), mutableListOf(lid), false)
        this.executeRequest(request)
    }

    open fun removeMessageByThreadId(fid: ID, tid: ID): Unit {
        val request = DeleteMessagesNetworkRequest(mutableListOf(), mutableListOf(tid), fid)
        this.executeRequest(request)
    }

    open fun moveThreadToFolder(tid: ID, fid: ID): Unit {
        val request = MoveToFolderNetworkRequest(mutableListOf(), mutableListOf(tid), fid, fid)
        this.executeRequest(request)
    }

    open fun moveMessageToFolder(mid: ID, fid: ID): Unit {
        val request = MoveToFolderNetworkRequest(mutableListOf(mid), mutableListOf(), fid, fid)
        this.executeRequest(request)
    }

    open fun createFolder(name: String): Unit {
        val request = CreateFolderRequest(name, null, null)
        this.executeRequest(request)
    }

    open fun createLabel(label: LabelData): Unit {
        val request = CreateLabelRequest(label)
        this.executeRequest(request)
    }

    open fun deleteLabel(lid: LabelID): Unit {
        val request = DeleteLabelRequest(lid)
        this.executeRequest(request)
    }

    open fun sendMessage(to: String, subject: String, text: String, references: String? = null): Unit {
        val settings = this.getSettings()
        val composeCheck = settings.payload!!.accountInformation.composeCheck
        val task = MailSendRequestBuilder().setTo(to).setComposeCheck(composeCheck).setSubject(subject).setBody(text).setReferences(references).build()
        this.getJsonResponse(SendMailNetworkRequest(task))
    }

    open fun getMessageBody(mid: ID): MessageBodyPayload {
        val request = MessageBodyRequest(mutableListOf(mid), true)
        val json = this.getJsonResponse(request)
        val response = messageBodyResponseFromJSONItem(json)
        return response!![0].payload!!
    }

    open fun getMessageReference(mid: ID): String {
        return this.getMessageBody(mid).info.rfcId
    }

    open fun setParameter(key: String, value: String): Unit {
        val request = SetParametersRequest(SetParametersItems(mutableListOf(SetParametersItem(key, value))))
        this.executeRequest(request)
    }

    open fun moveToSpam(fid: ID, tid: ID): Unit {
        val request = MarkSpamNetworkRequest(mutableListOf(), mutableListOf(tid), fid, true)
        this.executeRequest(request)
    }

    open fun archive(local: String, tid: ID): Unit {
        val request = ArchiveMessagesNetworkRequest(mutableListOf(), mutableListOf(tid), local)
        this.executeRequest(request)
    }

    open fun getSearchResults(query: String): YSMap<MessageId, FullMessage> {
        val request = SearchQueryRequest(query)
        val messages: YSMap<MessageId, FullMessage> = mutableMapOf()
        val response = searchResponseFromJSONItem(this.getJsonResponse(request))
        response!!.messages.forEach(__LBL__MailboxClient_3@ {
            message ->
            messages.set(message.mid, FullMessage.fromMeta(message))
        })
        return messages
    }

    open fun getZeroSuggest(): YSArray<ZeroSuggestResponse> {
        val request = ZeroSuggestRequest()
        val response = this.getJsonResponse(request) as ArrayJSONItem
        val suggests: YSArray<ZeroSuggestResponse> = mutableListOf()
        response.asArray().forEach(__LBL__MailboxClient_4@ {
            suggest ->
            suggests.add(zeroSuggestFromJSONItem(suggest)!!)
        })
        return suggests
    }

    open fun getTranslationLangs(): TranslationLangsResponse {
        val request = TranslationLangsRequest()
        val response = this.getJsonResponse(request) as MapJSONItem
        return translationLangsFromJSONItem(response)!!
    }

    open fun translateMessage(mid: ID, targetLang: String): TranslatedMessageResponse {
        val request = TranslateMessageRequest(targetLang, mid.toString())
        val response = this.getJsonResponse(request) as MapJSONItem
        return translatedMessageFromJSONItem(response)!!
    }

    open fun saveQueryToZeroSuggest(query: String): Unit {
        val request = SaveQueryToZeroSuggestRequest(query)
        this.executeRequest(request)
    }

    open fun deleteQueryFromZeroSuggest(query: String): Unit {
        val request = DeleteQueryFromZeroSuggestRequest(query)
        this.executeRequest(request)
    }

    open fun getSuggests(query: String): YSArray<SuggestResponse> {
        val request = SuggestRequest(query)
        val response = this.getJsonResponse(request) as ArrayJSONItem
        val suggests: YSArray<SuggestResponse> = mutableListOf()
        response.asArray().forEach(__LBL__MailboxClient_5@ {
            suggest ->
            suggests.add(suggestFromJSONItem(suggest)!!)
        })
        return suggests
    }

    private fun getMessagesList(request: NetworkRequest): YSArray<MessageMeta> {
        val response = this.getJsonResponse(request)
        val messageResponse = messageResponseFromJSONItem(response)!!
        val messages: YSArray<MessageMeta> = mutableListOf()
        messageResponse.payload!![0].items.forEach(__LBL__MailboxClient_6@ {
            message ->
            messages.add(message)
        })
        return messages
    }

    private fun getContactsList(request: NetworkRequest): YSArray<Contact> {
        val jsonMap = this.getJsonResponse(request) as MapJSONItem
        val contacts: YSArray<Contact> = mutableListOf()
        val jsonContactArray = jsonMap.getMap("contacts")!!.`get`("contact") as ArrayJSONItem
        jsonContactArray.asArray().forEach(__LBL__MailboxClient_7@ {
            contactItem ->
            val contact = contactFromABookTopJSONItem(contactItem)
            if (contact != null) {
                contacts.add(contact)
            }
        })
        return contacts
    }

    private fun getJsonResponse(request: NetworkRequest, retries: Int = 0): JSONItem {
        val jsonString = this.executeRequest(request, retries)
        val response = this.jsonSerializer.deserialize(jsonString)
        this.logger.info(JSONItemGetDebugDescription(response.getValue()))
        return response.getValue()
    }

    private fun executeRequest(request: NetworkRequest, retries: Int = 0): String {
        val host = PublicBackendConfig.baseUrl(this.oauthAccount.type)
        val requestEnricher = DefaultNetworkInterceptor(PlatformImpl(this.convertPlatform(this.platform), false), "testopithecus",  {
             ->
            NetworkExtra(true, "fake")
        }
)
        val result: Result<String> = this.network.syncExecuteWithRetries(retries, host, requestEnricher.interceptSync(request), this.oauthAccount.oauthToken)
        if (result.isError()) {
            fail(result.getError().message)
        }
        return result.getValue()
    }

    private fun convertPlatform(platform: MBTPlatform): PlatformType {
        when (platform) {
            MBTPlatform.IOS -> {
                return PlatformType.ios
            }
            MBTPlatform.Desktop -> {
                return PlatformType.electron
            }
            else -> {
                return PlatformType.android
            }
        }
    }

}

public open class MailboxClientHandler(var mailboxClients: YSArray<MailboxClient>) {
    var clientsManager: AccountsManager
    init {
        this.clientsManager = AccountsManager(mailboxClients.map( {
            client ->
            client.oauthAccount.account
        }))
    }
    open fun loginToAccount(account: UserAccount): Unit {
        this.clientsManager.logInToAccount(account)
    }

    open fun switchToClientForAccountWithLogin(login: String): Unit {
        this.clientsManager.switchToAccount(login)
    }

    open fun getCurrentClient(): MailboxClient {
        return this.mailboxClients[this.clientsManager.currentAccount!!]
    }

    open fun getLoggedInAccounts(): YSArray<UserAccount> {
        return this.clientsManager.getLoggedInAccounts()
    }

    open fun revokeToken(account: UserAccount): Unit {
        return this.clientsManager.revokeToken(account)
    }

    open fun exitFromReloginWindow(): Unit {
        this.clientsManager.exitFromReloginWindow()
    }

}

private open class PlatformImpl(override val type: PlatformType, override val isTablet: Boolean): Platform {
}

