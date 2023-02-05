package com.yandex.mail.fakeserver

import android.net.Uri
import android.util.Patterns
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.yandex.mail.LoginData
import com.yandex.mail.entity.FolderType
import com.yandex.mail.entity.FolderType.ARCHIVE
import com.yandex.mail.entity.FolderType.DRAFT
import com.yandex.mail.entity.FolderType.INBOX
import com.yandex.mail.entity.FolderType.OUTGOING
import com.yandex.mail.entity.FolderType.SENT
import com.yandex.mail.entity.FolderType.SPAM
import com.yandex.mail.entity.FolderType.TEMPLATES
import com.yandex.mail.entity.FolderType.TRASH
import com.yandex.mail.fakeserver.FoldersList.createEmptyUserFolder
import com.yandex.mail.fakeserver.LabelsList.createEmptyUserLabel
import com.yandex.mail.generators.ContainersGenerator
import com.yandex.mail.generators.MessagesGenerator
import com.yandex.mail.network.MailApi
import com.yandex.mail.network.json.response.UploadAttachmentResponse
import com.yandex.mail.network.request.ByTypeRequest
import com.yandex.mail.network.request.FolderMessagesRequest
import com.yandex.mail.network.request.FolderThreadsRequest
import com.yandex.mail.network.request.LabelRequest
import com.yandex.mail.network.request.MailSendRequest
import com.yandex.mail.network.request.Requests
import com.yandex.mail.network.request.SearchRequest
import com.yandex.mail.network.request.SmartReplyRequest
import com.yandex.mail.network.request.ThreadRequest
import com.yandex.mail.network.response.AbookJson
import com.yandex.mail.network.response.AbookSuggestJson
import com.yandex.mail.network.response.AbookSuggestJson.ContactsWrapper
import com.yandex.mail.network.response.ArchiveResponseJson
import com.yandex.mail.network.response.AvaResponseJson
import com.yandex.mail.network.response.AvaResponseJson.ProfileContainer
import com.yandex.mail.network.response.CheckLinkResponseJson
import com.yandex.mail.network.response.ComposeGenerateIdResponse
import com.yandex.mail.network.response.FolderJson
import com.yandex.mail.network.response.FolderTaskJson
import com.yandex.mail.network.response.Header
import com.yandex.mail.network.response.JsonUrlResponse
import com.yandex.mail.network.response.LabelJson
import com.yandex.mail.network.response.LabelTaskJson
import com.yandex.mail.network.response.MailishProviderJson
import com.yandex.mail.network.response.MessageBodyJson
import com.yandex.mail.network.response.MessageMetaJson
import com.yandex.mail.network.response.MessagesJson
import com.yandex.mail.network.response.RecipientJson
import com.yandex.mail.network.response.ResponseWithStatus
import com.yandex.mail.network.response.SaveDraftResponse
import com.yandex.mail.network.response.SearchSuggestResponse
import com.yandex.mail.network.response.SmartReplyResponse
import com.yandex.mail.network.response.Status
import com.yandex.mail.network.response.Status.Companion.STATUS_OK
import com.yandex.mail.network.response.StatusContainer
import com.yandex.mail.network.response.SyncStatusJson
import com.yandex.mail.network.response.ThreadsJson
import com.yandex.mail.network.response.TranslateResponse
import com.yandex.mail.network.response.TranslationLanguagesResponse
import com.yandex.mail.network.response.XlistResponse
import com.yandex.mail.network.response.XlistStatus
import com.yandex.mail.search.presenter.SearchSuggestPresenter.Companion.SUGGEST_FIRST_STEP_TIMEOUT
import com.yandex.mail.search.presenter.SearchSuggestPresenter.Companion.SUGGEST_SECOND_STEP_TIMEOUT
import com.yandex.mail.storage.MessageStatus.Type
import com.yandex.mail.storage.entities.IMPORTANT
import com.yandex.mail.tools.MockNetworkTools.getDecodedParams
import com.yandex.mail.tools.MockNetworkTools.getGetParams
import com.yandex.mail.tools.MockNetworkTools.getMethod
import com.yandex.mail.tools.MockNetworkTools.getOkResponse
import com.yandex.mail.tools.MockServerTools.createAuthErrorStatus
import com.yandex.mail.tools.MockServerTools.createOkStatus
import com.yandex.mail.tools.MockServerTools.createPermErrorStatus
import com.yandex.mail.tools.MockServerTools.createTempErrorStatus
import com.yandex.mail.tools.MockServerTools.extractAttachmentFromMultipart
import com.yandex.mail.util.GsonIgnoreStrategy
import com.yandex.mail.wrappers.AttachmentWrapper
import com.yandex.mail.wrappers.ContactWrapper
import com.yandex.mail.wrappers.FolderWrapper
import com.yandex.mail.wrappers.FolderWrapper.FolderWrapperBuilder
import com.yandex.mail.wrappers.LabelWrapper
import com.yandex.mail.wrappers.LabelWrapper.LabelWrapperBuilder
import com.yandex.mail.wrappers.MessageWrapper
import com.yandex.mail.wrappers.MessageWrapper.MessageWrapperBuilder
import com.yandex.mail.wrappers.SettingsWrapper
import com.yandex.mail.wrappers.ThreadWrapper
import com.yandex.mail.wrappers.ThreadWrapper.ThreadWrapperBuilder
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Collections.sort

/**
 * Server side account abstraction
 */
class AccountWrapper(private val fakeServer: FakeServer, @JvmField val loginData: LoginData, private val areTabsEnabled: Boolean) {

    private val gson: Gson

    private val containersGenerator = ContainersGenerator()

    private val messagesGenerator = MessagesGenerator()

    @JvmField
    val attachments = Attachments()

    @JvmField
    val folders: FoldersList

    @JvmField
    val labels: LabelsList

    @JvmField
    val messages: Messages

    private val fraudLinks = ArrayList<String>()

    private val contacts: MutableList<ContactWrapper> = mutableListOf()

    private val failedRequests: MutableSet<String> = mutableSetOf()

    fun getSettings(): SettingsWrapper {
        return settings
    }

    fun setSettings(settings: SettingsWrapper): AccountWrapper {
        this.settings = settings
        return this
    }

    // TODO handle in a nicer way
    private var settings: SettingsWrapper

    private val networkRequestsDispatcher: Dispatcher

    /**
     * rules for custom behaviour of fakeServer
     * first of [CustomResponseRule] which [CustomResponseRule.match] return true for request will be applied
     */
    private val customResponseRules: MutableList<CustomResponseRule>

    /**
     * Maps API method ("settings", "messages", etc.) to the recorded requests of this type
     */
    private val recordedRequestsObservables = HashMap<String, PublishSubject<RecordedRequest>>()

    private val composeIdGenerator: Int = 0

    init {
        this.folders = FoldersList.generateDefault(containersGenerator)
        this.labels = LabelsList.generateDefault(containersGenerator)
        this.messages = Messages()
        this.settings = SettingsWrapper.defaultSettings(loginData).build()

        if (labels.getByName("Important").serverLid != IMPORTANT_LID) {
            throw IllegalStateException("Important label must have id = $IMPORTANT_LID")
        }

        gson = GsonBuilder()
            .addSerializationExclusionStrategy(GsonIgnoreStrategy())
            .registerTypeAdapterFactory(XlistResponse.XlistTypeAdapterFactory())
            .registerTypeAdapterFactory(Requests.RequestsTypeAdapterFactory())
            .registerTypeAdapter(RecipientJson::class.java, RecipientJson.RecipientTypeAdapter())
            .registerTypeAdapter(SearchSuggestResponse.Target::class.java, SearchSuggestResponse.TargetDeserializer())
            .create()

        this.networkRequestsDispatcher = buildApiCallsMapDispatcher(
            Pair("attach", attachResponseRule),
            Pair("ava", avaResponseRule),
            Pair("messages", messagesResponseRule),
            Pair("message_body", messageBodyResponseRule),
            Pair("push", pushResponseRule),
            Pair("search", searchResponseRule),
            Pair("bytype", searchByTypeResponseRule),
            Pair("send", sendResponseRule),
            Pair("generate_operation_id", generateComposeIdResponseRule),
            Pair("store", storeResponseRule),
            Pair("settings", settingsResponseRule),
            Pair("upload", uploadRule),
            Pair("xlist", xlistResponseRule),
            Pair("mark_with_label", markWithLabelResponseRule),
            Pair("mark_read", markReadResponseRule),
            Pair("mark_unread", markUnreadResponseRule),
            Pair("reset_fresh", resetFreshResponseRule),
//            Pair("get_newsletters", getNewslettersResponseRule),
            Pair("only_new", loadUnreadResponseRule),
            Pair("with_attachments", loadMessagesWithAttacmentResponseRule),
            Pair("vdirect", checkLinkResponseRule),
            Pair("set_settings", setSettingsResponceRule),
            Pair("abook_suggest", abookSuggestResponseRule),
            Pair("abook_top", abookTopResponseRule),
            Pair("clear_folder", clearFolderResponseRule),
            Pair("move_to_folder", moveToFolderResponseRule),
            Pair("archive", archiveResponseRule),
            Pair("delete_items", deleteResponseRule),
            Pair("foo", spamResponseRule),
            Pair("antifoo", unspamResponseRule),
            Pair("create_folder", createFolderResponseRule),
            Pair("update_folder", updateFolderResponseRule),
            Pair("delete_folder", deleteFolderResponseRule),
            Pair("create_label", createLabelResponseRule),
            Pair("update_label", updateLabelResponseRule),
            Pair("delete_label", deleteLabelResponseRule),
            Pair("quick_reply_suggestions", quickReplySuggestionsRule),
            Pair("search_suggest", searchSuggestRule),
            Pair("provider", providerResponse),
            Pair("translate_message", translateResponseRule),
            Pair("translation_langs", translationLangsResponseRule),
            Pair("set_parameters", setParametersResponseRule),
            Pair("sync_status", syncStatusResponseRule),
            Pair("purge_items", setParametersResponseRule)
        )
        this.customResponseRules = mutableListOf()
    }

    fun addFolders(vararg folders: FolderWrapper): AccountWrapper {
        return addFolders(folders.toList())
    }

    fun addFolders(folders: Iterable<FolderWrapper>): AccountWrapper {
        return addFolders(folders.toList())
    }

    fun addFolders(f: List<FolderWrapper>): AccountWrapper {
        for (ff in f) {
            folders.add(ff)
        }
        return this
    }

    fun clearFolders() {
        folders.clear()
    }

    val inboxFolder: FolderWrapper
        get() = folders.getByType(INBOX)

    val spamFolder: FolderWrapper
        get() = folders.getByType(SPAM)

    val archiveFolder: FolderWrapper
        get() = folders.getByType(ARCHIVE)

    val draftFolder: FolderWrapper
        get() = folders.getByType(DRAFT)

    val templateFolder: FolderWrapper
        get() = folders.getByType(TEMPLATES)

    val trashFolder: FolderWrapper
        get() = folders.getByType(TRASH)

    val sentFolder: FolderWrapper
        get() = folders.getByType(SENT)

    val outgoingFolder: FolderWrapper
        get() = folders.getByType(OUTGOING)

    val importantLabel: LabelWrapper
        get() = labels.getByType(IMPORTANT)

    fun setFailedRequests(request: String) {
        failedRequests.add(request)
    }

    fun newFolder(name: String): FolderWrapperBuilder {
        return createEmptyUserFolder(containersGenerator).name(name)
    }

    fun newChildFolder(parent: FolderWrapper, name: String): FolderWrapperBuilder {
        return createEmptyUserFolder(containersGenerator)
            .parent(parent.serverFid)
            .name(parent.name + "|" + name)
    }

    fun newLabel(name: String): LabelWrapperBuilder {
        return createEmptyUserLabel(containersGenerator).displayName(name)
    }

    fun removeMessages(vararg messagesToRemove: MessageWrapper) {
        removeMessages(messagesToRemove.toList())
    }

    fun removeMessages(messagesToRemove: List<MessageWrapper>) {
        messages.remove(messagesToRemove)
    }

    fun removeFolder(folderWrapper: FolderWrapper): Boolean {
        return folders.remove(folderWrapper)
    }

    fun addMessages(messages: List<MessageWrapper>): AccountWrapper {
        for (message in messages) {
            val sameMessages = this.messages.messages.filter { it.mid == message.mid }
            if (!sameMessages.isEmpty()) {
                //noinspection
                this.messages.messages[this.messages.messages.indexOf(sameMessages[0])] = message
            } else {
                this.messages.messages.add(message)
            }
        }
        return this
    }

    fun addLabels(l: List<LabelWrapper>) = apply { labels.labelsList.addAll(l) }

    fun addLabels(labels: Iterable<LabelWrapper>) = addLabels(labels.toList())

    fun addLabels(vararg extraLabels: LabelWrapper) = addLabels(extraLabels.toList())

    fun addMessages(vararg m: MessageWrapper) = addMessages(m.toList())

    fun addThreads(vararg wrappers: ThreadWrapper) = addThreads(wrappers.toList())

    fun addThreads(wrappers: List<ThreadWrapper>) = apply {
        for ((_, messages1) in wrappers) {
            addMessages(messages1)
        }
    }

    fun newReadMessage(folder: FolderWrapper): MessageWrapperBuilder {
        return messagesGenerator.makeEmptyReadMessage().folder(folder)
    }

    fun newReadMessages(folder: FolderWrapper, count: Int): List<MessageWrapper> {
        val builders = mutableListOf<MessageWrapperBuilder>()
        for (i in 0 until count) {
            builders.add(newReadMessage(folder))
        }
        return builders.map { it.build() }
    }

    fun newReadMessagesBuilders(folder: FolderWrapper, count: Int): List<MessageWrapperBuilder> {
        val builders = List<MessageWrapperBuilder>(count, { newReadMessage(folder) })
        return builders
    }

    fun newUnreadMessage(): MessageWrapperBuilder {
        return messagesGenerator.makeEmptyUnreadMessage()
    }

    fun newUnreadMessage(folder: FolderWrapper): MessageWrapperBuilder {
        return newUnreadMessage().folder(folder)
    }

    fun newUnreadMessages(folder: FolderWrapper, count: Int): List<MessageWrapper> {
        val builders = List(count, { newUnreadMessage(folder) })
        return builders.map { it.build() }
    }

    fun newUnreadMessagesBuilders(folder: FolderWrapper, count: Int): List<MessageWrapperBuilder> {
        val builders = List(count, { newUnreadMessage(folder) })
        return builders
    }

    fun newThread(vararg messageBuilders: MessageWrapperBuilder): ThreadWrapperBuilder {
        return newThread(null, messageBuilders.toList())
    }

    fun newThread(fid: String?, vararg messageBuilders: MessageWrapperBuilder): ThreadWrapperBuilder {
        return newThread(fid, messageBuilders.toList())
    }

    fun newThread(messageBuilders: List<MessageWrapperBuilder>): ThreadWrapperBuilder {
        val serverTid = containersGenerator.nextTid()
        return ThreadWrapper.builder()
            .tid(serverTid)
            .messages(messageBuilders.map { builder -> builder.tid(serverTid).build() })
    }

    fun newThread(
        tid: String?,
        messageBuilders: List<MessageWrapperBuilder>
    ): ThreadWrapperBuilder {
        val serverTid = tid ?: containersGenerator.nextTid()
        return ThreadWrapper.builder()
            .tid(serverTid)
            .messages(messageBuilders.map { builder -> builder.tid(serverTid).build() })
    }

    fun newContact(email: String, firstName: String, lastName: String): ContactWrapper.ContactWrapperBuilder {
        return ContactWrapper.builder()
            .cid("0") // TODO we might want to generate proper ids at some point
            .email(email)
            .first(firstName)
            .last(lastName)
    }

    fun addContacts(vararg newContacts: ContactWrapper) {
        addContacts(newContacts.toList())
    }

    fun addContacts(newContacts: Collection<ContactWrapper>) = contacts.addAll(newContacts)

    fun moveMessages(messages: List<MessageWrapper>, target: FolderWrapper) {
        for (message in messages) {
            message.folder = target
        }
    }

    private fun getFolderJson(folder: FolderWrapper): FolderJson {
        val all = messages.withServerFid(folder)
        val unread = all.filter { it.isUnread }
        val options = FolderJson.Options(folder.position)

        return FolderJson.create(
            folder.name,
            unread.size,
            all.size,
            folder.type,
            java.lang.Long.valueOf(folder.serverFid),
            if (folder.parent.isEmpty()) null else java.lang.Long.valueOf(folder.parent),
            options
        )
    }

    private fun getLabelJson(label: LabelWrapper) = LabelJson.create(
        label.displayName,
        0, // always zero by server design
        messages.withServerLid(label).size,
        label.type,
        label.serverLid,
        "000000"
    )

    fun addFraudLink(link: String) {
        fraudLinks.add(link)
    }

    private fun containersResponse(withTabs: Boolean, md5: String?): String {
        var status = XlistStatus(createOkStatus(), "md5", 1)

        val responseXlist = if (withTabs) {
            folders.foldersList.filter { folder -> folder.type != INBOX.serverType }
        } else {
            folders.foldersList.filter { folder ->
                folder.type !in setOf(
                    FolderType.TAB_RELEVANT.serverType,
                    FolderType.TAB_NEWS.serverType,
                    FolderType.TAB_SOCIAL.serverType
                )
            }
        }

        val response = XlistResponse(
            status,
            responseXlist.map { this.getFolderJson(it) },
            labels.labelsList.map { this.getLabelJson(it) }
        )
        status = XlistStatus(createOkStatus(), getMd5(gson.toJson(response)), 1)
        val md5CheckFailed = status.md5 != md5

        val responseWithMd5 = XlistResponse(
            status,
            if (md5CheckFailed) {
                response.folders
            } else {
                emptyList()
            },
            if (md5CheckFailed) {
                response.labels
            } else {
                emptyList()
            }
        )
        return gson.toJson(responseWithMd5)
    }

    @Throws(InterruptedException::class)
    fun dispatchNetworkRequest(request: RecordedRequest): MockResponse {
        for (customRule in customResponseRules) {
            if (customRule.match(request)) {
                return customRule.getResponse(request)
            }
        }
        return networkRequestsDispatcher.dispatch(request)
    }

    /**
     * Archive is not present by default, so this method adds it if you need
     */
    fun addArchiveFolder() {
        addFolders(FoldersList.createEmptyFolder(containersGenerator).type(ARCHIVE).name("Archive").build())
    }

    /**
     * Template is not present by default, so this method adds it if you need
     */
    fun addTemplateFolder() {
        addFolders(FoldersList.createEmptyFolder(containersGenerator).type(TEMPLATES).name("Template").build())
    }

    /**
     * handles XLIST
     */
    val xlistResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val withTabs = request.requestUrl!!.queryParameter("withTabs")
            val md5 = request.requestUrl!!.queryParameter("md5")
            getOkResponse(containersResponse(withTabs == "1", md5))
        }

    /**
     * handles MARK_WITH_LABEL
     */
    val markWithLabelResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val tids = params.getOrDefault("tids", "").split(",")
            val mids = params.getOrDefault("mids", "").split(",")
            val lid = params.get("lid")!!
            val mark = params.get("mark") == "1"

            val label = labels.getByServerLid(lid)
            val allMessages = tids.flatMap { messages.withServerTid(it) } + mids.map { messages.withServerMid(it) }
            for (message in allMessages) {
                val labels = message.labels
                if (mark) {
                    if (!labels.contains(label)) {
                        labels.add(label)
                    }
                } else {
                    labels.remove(label)
                }
            }

            makeDummyResponseStatus()
        }

    /**
     * handles MOVE_TO_FOLDER
     */
    val moveToFolderResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            val allMessages = mids.map { messages.withServerMid(it) }

            val fid = params.get("fid")!!

            val folder = folders.getByFid(fid)

            for (message in allMessages) {
                message.folder = folder
            }
            makeDummyResponseStatus()
        }

    /**
     * handles move to ARCHIVE
     */
    val archiveResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            val allMessages = mids.map { messages.withServerMid(it) }

            val folder = archiveFolder

            for (message in allMessages) {
                message.folder = folder
            }

            val response = ArchiveResponseJson(Status(STATUS_OK, null, null, null))
            response.archiveFid = java.lang.Long.valueOf(folder.serverFid)

            getOkResponse(gson.toJson(response))
        }

    /**
     * handles DELETE
     */
    val deleteResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            val allMessages = mids.map { messages.withServerMid(it) }

            val folder = trashFolder
            val messagesToRemove = ArrayList<MessageWrapper>()

            for (message in allMessages) {
                if (message.folder == trashFolder) {
                    messagesToRemove.add(message)
                } else {
                    message.folder = folder
                }
            }
            messages.remove(messagesToRemove)
            makeDummyResponseStatus()
        }

    /**
     * handles MARK AS SPAM
     */
    val spamResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            val allMessages = mids.map { messages.withServerMid(it) }

            val folder = spamFolder

            for (message in allMessages) {
                message.folder = folder
            }
            makeDummyResponseStatus()
        }

    /**
     * handles MARK UNSPAM
     * all messages from any folders after mark with unspam go to Inbox.
     */
    val unspamResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            val allMessages = mids.map { messages.withServerMid(it) }

            val folder = inboxFolder

            for (message in allMessages) {
                message.folder = folder
            }
            makeDummyResponseStatus()
        }

    /**
     * handles MARK_READ
     */
    val markReadResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            mids.map { messages.withServerMid(it) }.forEach { message -> message.read = true }
            makeDummyResponseStatus()
        }

    /**
     * handles MARK_UNREAD
     */
    val markUnreadResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            mids.map { messages.withServerMid(it) }.forEach { message -> message.read = false }
            makeDummyResponseStatus()
        }

    /**
     * handles MESSAGES
     */
    val messagesResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val reqs = gson.fromJson<Requests<*>>(request.body.readString(Charset.defaultCharset()), Requests::class.java)
            val responsesWithStatuses = ArrayList<ResponseWithStatus>()

            for (req in reqs.requests) {
                if (req is FolderThreadsRequest) {
                    val fr = req
                    val wrapper = folders.getByFid(fr.fid.toString())
                    val last = fr.last
                    val md5 = fr.md5
                    if (fr.threaded) {
                        responsesWithStatuses.add(generateThreadsBatch(messages.getThreadsIn(wrapper).take(last), fr.fid, md5))
                    } else {
                        if (fr is FolderMessagesRequest && fr.unread) {
                            responsesWithStatuses.add(generateMessageBatch(messages.unreadInFolder(wrapper).take(last), md5))
                        } else {
                            responsesWithStatuses.add(generateMessageBatch(messages.getMessagesIn(wrapper).take(last), md5))
                        }
                    }
                } else if (req is ThreadRequest) {
                    val tr = req
                    val (_, messages1) = ThreadWrapper.builder()
                        .tid(tr.tid.toString())
                        .messages(messages.messages.filter { it.tid == tr.tid.toString() })
                        .build()
                    responsesWithStatuses.add(generateMessageBatch(messages1, null))
                } else if (req is LabelRequest) {
                    responsesWithStatuses.add(generateMessageBatch(messages.withServerLid(labels.getByServerLid(req.lid!!)), null))
                } else {
                    throw IllegalStateException("Unexpected class: " + req!!.javaClass)
                }
            }

            getOkResponse(gson.toJson(responsesWithStatuses.toTypedArray()))
        }

    fun generateThreadsBatch(thread: List<ThreadWrapper>, fid: Long, md5: String): ThreadsJson {
        val header = Header(
            1,
            "0",
            thread.size,
            thread
                .map { (_, messages) -> messages.count { it.isUnread } }
                .sum(),
            true,
            1
        )

        sort(thread, THREAD_ORDER_COMPARATOR)
        val threadMetas = thread.map { t -> t.generateThreadMeta(fid) }

        val batch = ThreadsJson.MessageBatch(threadMetas)
        var threadsJson = ThreadsJson(header, batch, null)

        val calcedMd5 = getMd5(gson.toJson(threadsJson))

        threadsJson = threadsJson.copy(header = header.copy(md5 = calcedMd5))
        if (calcedMd5 == md5) {
            threadsJson = threadsJson.copy(header = header.copy(modified = false), messageBatch = ThreadsJson.MessageBatch(emptyList()))
        }

        return threadsJson
    }

    fun generateMessageBatch(messages: List<MessageWrapper>, md5: String?): MessagesJson {
        val header = Header(
            1,
            "0",
            messages.size,
            messages.count { it.isUnread },
            true,
            1
        )

        sort(messages, MESSAGE_ORDER_COMPARATOR)
        val metaJsons = messages.map { it.generateMessageMeta() }


        val batch = MessagesJson.MessageBatch(metaJsons)
        var messagesJson = MessagesJson(header, batch, null)
        val calcedMd5 = getMd5(gson.toJson(messagesJson))

        messagesJson = messagesJson.copy(header = header.copy(md5 = calcedMd5))
        if (calcedMd5 == md5) {
            messagesJson = messagesJson.copy(header = header.copy(modified = false), messageBatch = MessagesJson.MessageBatch(emptyList()))
        }

        return messagesJson
    }

    /**
     * handles SEARCH
     *
     *
     * Does not handle labels, pagination and only searches in subject and body currently
     */
    // apply paging
    val searchResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val requestString = request.body.readUtf8()
            val searchRequest = gson.fromJson(requestString, SearchRequest::class.java)

            val page = searchRequest.page_number
            val messageLimit = searchRequest.msg_limit
            val query = searchRequest.query
            val lastItem = page * messageLimit
            val firstItem = lastItem - messageLimit

            val foldersToSearchIn: List<FolderWrapper>
            if (searchRequest.fid != null) {
                foldersToSearchIn = listOf(folders.getByFid(searchRequest.fid.toString()))
            } else {
                foldersToSearchIn = folders.foldersList
            }

            var messages = foldersToSearchIn
                .flatMap { this.messages.withServerFid(it) }
                .filter { message ->
                    if (query == null) {
                        return@filter false // todo: null-query should not be filtered
                    }
                    if (message.subjText.contains(query)) {
                        return@filter true
                    }
                    if (message.content?.contains(query) == true) {
                        return@filter true
                    }
                    return@filter false
                }
            if (messages.size < firstItem + 1) {
                messages = emptyList<MessageWrapper>()
            } else {
                messages = messages.subList(firstItem, Math.min(lastItem, messages.size))
            }

            val batch = MessageBatchWithStatus(createOkStatus(), messages.map { it.generateMessageMeta() })
            getOkResponse(gson.toJson(batch))
        }

    /**
     * handles BYTYPE
     *
     *
     * doesn't handle pagination at the moment
     */
    val searchByTypeResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val requestString = request.body.readUtf8()
            val searchRequest = gson.fromJson<ByTypeRequest>(requestString, ByTypeRequest::class.java)

            val type = searchRequest.type
            val messages: List<MessageWrapper>
            if (type != null) {
                val types = type.split(",").map { Type.fromId(type.toInt()) }
                messages = this.messages.withTypes(types)
            } else {
                messages = this.messages.messages
            }
            val batch = MessageBatchWithStatus(createOkStatus(), messages.map { it.generateMessageMeta() })
            getOkResponse(gson.toJson(batch))
        }

    /**
     * handles LOAD_UNREAD
     */
    val loadUnreadResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.path!!)
            val last = Integer.parseInt(params["last"]!!)
            val messages = this.messages.unread().take(last)
            val batch = MessageBatchWithStatus(createOkStatus(), messages.map { it.generateMessageMeta() })
            getOkResponse(gson.toJson(batch))
        }

    /**
     * handles WITH_ATTACH
     */
    val loadMessagesWithAttacmentResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.requestLine)
            val last = Integer.parseInt(params["last"]!!)
            val messages = this.messages.withAttachment().take(last)
            val messageMetas = messages.map { it.generateMessageMeta() }

            val batch = MessageBatchWithStatus(createOkStatus(), messageMetas)

            getOkResponse(gson.toJson(batch))
        }

    /**
     * handles MESSAGE_BODY
     */
    val messageBodyResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val mids = params.getOrDefault("mids", "").split(",")
            val wrappers = mids.map { messages.withServerMid(it) }
            getOkResponse(MessageWrapper.generateMessageContentsResponse(gson, wrappers))
        }

    /**
     * handles SEND

     * @return dummy response
     */
    val sendResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val requestString = request.body.readUtf8()
            val mailSendRequest = gson.fromJson(requestString, MailSendRequest::class.java)

            val messageAttaches = if (mailSendRequest.attachIds == null)
                emptyList()
            else
                listOf(*mailSendRequest.attachIds!!)
                    .map { attachments.getById(it) }

            for (i in messageAttaches.indices) {
                messageAttaches[i].hid = "1." + (i + 1)
            }

            val sentMessage = messagesGenerator.makeEmptyReadMessage()
                .subjText(mailSendRequest.subject)
                .attachments(messageAttaches)
                .content(mailSendRequest.body)
                .folder(sentFolder)
                .build()
            addMessages(sentMessage)

            makeDummyResponseWithStatus()
        }

    val generateComposeIdResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule {
            val response = ComposeGenerateIdResponse(composeIdGenerator.inc().toString())
            getOkResponse(gson.toJson(response))
        }

    /**
     * handles STORE
     */
    val storeResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val mailSendRequest = gson.fromJson<MailSendRequest>(request.body.readUtf8(), MailSendRequest::class.java)

            val messageWrapperBuilder = newReadMessage(draftFolder)
                .from(mailSendRequest.fromMailbox, mailSendRequest.fromName)
                .to(mailSendRequest.to)
                .content(mailSendRequest.body)
                .subjText(mailSendRequest.subject)

            mailSendRequest.cc?.let { messageWrapperBuilder.cc(it) }
            mailSendRequest.bcc?.let { messageWrapperBuilder.bcc(it) }

            val message = messageWrapperBuilder.build()

            addMessages(message)
            val response = SaveDraftResponse(createOkStatus(), message.mid, message.folder.serverFid, null, null, null)
            getOkResponse(gson.toJson(response))
        }

    /**
     * handles RESET_FRESH
     */
    val resetFreshResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request -> makeDummyResponseWithStatus() }

    /**
     * handles get_newsletters
     */
    val getNewslettersResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request -> makeDummyResponseWithStatus() }

    /**
     * handles PUSH
     */
    val pushResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val token = request.requestUrl!!.queryParameter("push_token")
            val status = when (token) {
                "auth_error_token" -> createAuthErrorStatus()
                "perm_error_token" -> createPermErrorStatus()
                "temp_error_token" -> createTempErrorStatus()
                else -> createOkStatus()
            }
            val statusContainer = StatusContainer(status)
            getOkResponse(gson.toJson(statusContainer))
        }

    /**
     * handles AVA
     */
    val avaResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val emails = params.getOrDefault("emails", "").split(",")

            val containers = emails.map { email ->
                val files = fakeServer.files
                generateProfileContainer(
                    "name of $email",
                    email,
                    fakeServer.wrapPath(files.getAvatarPath(email)).toString()
                )
            }
            val response = generateAvaResponse(containers)
            getOkResponse(gson.toJson(response))
        }

    /**
     * handles ATTACH
     */
    val attachResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val uri = Uri.parse(request.path)
            val name = uri.getQueryParameter("name")!!

            val files = fakeServer.files
            val jurl = JsonUrlResponse(createOkStatus(), fakeServer.wrapPath(files.getAttachmentPath(name)).toString())
            getOkResponse(gson.toJson(jurl))
        }

    /**
     * handles UPLOAD
     */
    val uploadRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->

            val nameAndContent = extractAttachmentFromMultipart(request)
            val name = nameAndContent.first
            val content = nameAndContent.second

            val files = fakeServer.files
            files.addAttachment(name, content)
            val url = fakeServer.wrapPath(files.getAttachmentPath(name)).toString()

            val wrapper = AttachmentWrapper.builder()
                .disk(false)
                .hid("")
                .mimeType("")
                .attachmentClass("")
                .name(name)
                .size(content.size.toLong())
                .previewSupported(true)
                .url(url)
                .build()

            val id = attachments.add(wrapper)

            val resp = UploadAttachmentResponse(createOkStatus(), id, url, "contentType")

            getOkResponse(gson.toJson(resp))
        }

    /**
     * handles SETTINGS
     */
    val settingsResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request -> getOkResponse(gson.toJson(settings.generateSettingsResponse(areTabsEnabled))) }

    /**
     * handles VDIRECT
     */
    val checkLinkResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val url = request.requestUrl!!.queryParameter("url")

            val response = CheckLinkResponseJson()
            response.status = createOkStatus()

            val linkStatus: CheckLinkResponseJson.Status

            if (!Patterns.WEB_URL.matcher(url).matches()) {
                linkStatus = CheckLinkResponseJson.Status.BAD_REQUEST
            } else if (fraudLinks.contains(url)) {
                linkStatus = CheckLinkResponseJson.Status.PHISHING
            } else {
                linkStatus = CheckLinkResponseJson.Status.CLEAN
            }

            response.vdirectStatus = linkStatus
            getOkResponse(gson.toJson(response))
        }

    /**
     * handles SET_SETTINGS

     * @return dummy response
     */
    val setSettingsResponceRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { makeDummyResponseStatus() }

    /**
     * handles ABOOK_SUGGEST
     */
    val abookSuggestResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val query = params.get("query")!!

            val result = contacts.filter { (_, email, first, last) -> email.contains(query) || first.contains(query) || last.contains(query) }

            val contactsWrapper = ContactsWrapper("rev", result.map { it.generateSuggestContact() }.toTypedArray())
            val abookResponse = AbookSuggestJson(createOkStatus(), contactsWrapper)
            getOkResponse(gson.toJson(abookResponse))
        }

    /**
     * handles ABOOK_TOP
     */
    val abookTopResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getGetParams(request)
            val count = Integer.parseInt(params["n"]!!)

            val result = contacts.take(count)

            val contacts = AbookJson.Contacts(result.size, result.map { it.generateContact() }.toTypedArray())

            val response = AbookJson(createOkStatus(), contacts)

            getOkResponse(gson.toJson(response))
        }

    /**
     * handles CLEAR_FOLDER
     */
    val clearFolderResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            val fid = params.get("fid")!!

            val messagesInFolder = messages.getMessagesIn(folders.getByFid(fid))
            removeMessages(messagesInFolder)

            makeDummyResponseStatus()
        }

    /**
     * Handles CREATE_FOLDER
     */
    val createFolderResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            if (!params.containsKey("name")) {
                throw IllegalArgumentException("param name is required")
            }

            val name = params.get("name")!!

            if (folders.isPresentFolderWithName(name)) {
                return@MockWebServerResponseRule permErrorFolderResponse()
            }

            val folderWrapperBuilder = createEmptyUserFolder(containersGenerator)
                .name(name)
            if (params.containsKey("parent_fid")) {
                folderWrapperBuilder.parent(params.get("parent_fid")!!)
            }
            val folder = folderWrapperBuilder.build()
            folders.foldersList.add(folder)

            val response = FolderTaskJson(java.lang.Long.parseLong(folder.serverFid), createOkStatus(), emptyList(), "taskType")
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handles UPDATE_FOLDER
     */
    val updateFolderResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            if (!params.containsKey("fid")) {
                throw IllegalArgumentException("param fid is required")
            }

            val fid = params.get("fid")!!

            if (!folders.isPresentFolderWithFid(fid)) {
                return@MockWebServerResponseRule permErrorFolderResponse()
            }

            val folder = folders.getByFid(fid)
            if (params.containsKey("name")) {
                folder.name = params.get("name")!!
            }

            params["parent_fid"]?.let { parentFid ->
                if (!folders.isPresentFolderWithFid(parentFid)) {
                    return@MockWebServerResponseRule permErrorFolderResponse()
                }
                folder.parent = parentFid
            }

            val response = FolderTaskJson(null, createOkStatus(), emptyList(), "taskType")
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handles DELETE_FOLDER
     * Doesn't delete any child folder of parent folder
     */
    val deleteFolderResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            if (!params.containsKey("fid")) {
                throw IllegalArgumentException("param fid is required")
            }

            val fid = params.get("fid")!!

            if (!folders.isPresentFolderWithFid(fid)) {
                return@MockWebServerResponseRule permErrorFolderResponse()
            }

            folders.removeFolderByFid(fid)
            val response = FolderTaskJson(null, createOkStatus(), emptyList(), "sync")
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle CREATE_LABEL
     * Does not support symbol and color field
     */
    val createLabelResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            if (!(params.containsKey("name") && params.containsKey("type"))) {
                throw IllegalArgumentException("params name and type is required")
            }

            val name = params.get("name")!!
            val type = Integer.parseInt(params["type"]!!)

            if (labels.isPresentLabelWithName(name)) {
                return@MockWebServerResponseRule permErrorLabelResponse()
            }

            val label = createEmptyUserLabel(containersGenerator)
                .displayName(name)
                .type(type)
                .build()
            labels.labelsList.add(label)

            val response = LabelTaskJson(label.serverLid, createOkStatus(), emptyList(), "taskType")
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle UPDATE_LABEL
     * Color parameter ignored
     */
    val updateLabelResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            if (!(params.containsKey("lid") && params.containsKey("name"))) {
                throw IllegalArgumentException("params lid and name is required")
            }

            val lid = params.get("lid")!!
            val name = params.get("name")!!

            if (!labels.isPresentLabelWithServerLid(lid)) {
                return@MockWebServerResponseRule permErrorLabelResponse()
            }

            val oldLabel = labels.getByServerLid(lid)
            labels.removeLabelByLid(lid)
            labels.labelsList.add(oldLabel.copy(displayName = name))

            val response = LabelTaskJson(null, createOkStatus(), emptyList(), "taskType")
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle DELETE_LABEL
     */
    val deleteLabelResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            if (!params.containsKey("lid")) {
                throw IllegalArgumentException("param lid is required")
            }

            val lid = params.get("lid")!!

            if (!labels.isPresentLabelWithServerLid(lid)) {
                return@MockWebServerResponseRule permErrorLabelResponse()
            }

            labels.removeLabelByLid(lid)

            val response = LabelTaskJson(null, createOkStatus(), emptyList(), "sync")
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle QUICK_REPLY_SUGGESTIONS
     */
    val quickReplySuggestionsRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            gson.fromJson<SmartReplyRequest>(request.body.readString(Charset.defaultCharset()), SmartReplyRequest::class.java)
            val response = listOf(
                SmartReplyResponse("First", "first", 0),
                SmartReplyResponse("Second", "second", 1),
                SmartReplyResponse("Third", "thrid", 2)
            )
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle SEARCH_SUGGEST
     */
    val searchSuggestRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.path!!)

            val timeout =
                if (params["timeout"] == null) SUGGEST_FIRST_STEP_TIMEOUT else Integer.parseInt(params["timeout"]!!)

            val headers: HashMap<String, Any> = hashMapOf()
            val status = if (timeout == SUGGEST_SECOND_STEP_TIMEOUT) null else "1"
            status?.let { headers[MailApi.STATUS_HEADER_PARAM] = it }

            val suggest = gson.fromJson(
                InputStreamReader(
                    AccountWrapper::class.java.classLoader!!.getResourceAsStream("search_suggest_response.json")
                ),
                SearchSuggestResponse::class.java
            )

            getOkResponse(headers, gson.toJson(listOf(suggest)).toByteArray(), true)
        }

    /**
     * Handle PROVIDER
     */
    val providerResponse: MockWebServerResponseRule
        get() = MockWebServerResponseRule {
            val response = MailishProviderJson(createOkStatus(), "gmail")
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle TRANSLATE_MESSAGE
     */
    val translateResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule { request ->
            val params = getDecodedParams(request.body.readUtf8())
            if (!params.containsKey("mid")) {
                throw IllegalArgumentException("param mid is required")
            }

            val response = TranslateResponse(
                "subject", MessageBodyJson.Info(1L), listOf(
                    MessageBodyJson.Body(
                        "hid", "content", "contentType", "lang", "originalLang"
                    )
                )
            )
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle TRANSLATION_LANGS
     */
    val translationLangsResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule {
            val response = TranslationLanguagesResponse(
                listOf(
                    TranslationLanguagesResponse.TranslationLanguageResponse("ru", "Russian"),
                    TranslationLanguagesResponse.TranslationLanguageResponse("en", "English"),
                    TranslationLanguagesResponse.TranslationLanguageResponse("kr", "Korean"),
                    TranslationLanguagesResponse.TranslationLanguageResponse("jp", "Japanese")
                )
            )
            getOkResponse(gson.toJson(response))
        }

    /**
     * Handle SET_PARAMETERS
     */
    val setParametersResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule {
            makeDummyResponseStatus()
        }

    /**
     * Handle PURGE
     */
    val purgeResponseRule: MockWebServerResponseRule = MockWebServerResponseRule { getOkResponse("") }

    /**
     * Handle SYNC_STATUS
     */
    val syncStatusResponseRule: MockWebServerResponseRule
        get() = MockWebServerResponseRule {
            val response = SyncStatusJson(createOkStatus(), emptyList())

            getOkResponse(gson.toJson(response))
        }

    fun observeRecordedRequestsFor(apiMethod: String): Observable<RecordedRequest> {
        return recordedRequestsObservables[apiMethod]!!
    }

    private fun buildApiCallsMapDispatcher(vararg entries: Pair<String, MockWebServerResponseRule>): Dispatcher {
        val rulesMap = HashMap<String, MockWebServerResponseRule>()
        for (entry in entries) {
            val method = entry.first
            val rule = entry.second
            rulesMap[method] = rule
            recordedRequestsObservables[method] = PublishSubject.create<RecordedRequest>()
        }

        return object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                val method = getMethod(request)

                recordedRequestsObservables[method]!!.onNext(request)

                if (failedRequests.contains(method)) {
                    throw RuntimeException("The response is marked as failed")
                }
                val rule = rulesMap[method] ?: throw RuntimeException("No response rule found for method " + method)
                return rule.getResponse(request)
            }
        }
    }

    /**
     * add custom rule to handle network request
     * it's useful in case you want to emulate server errors for some requests
     */
    fun addCustomResponseRule(customResponseRule: CustomResponseRule) {
        customResponseRules.add(customResponseRule)
    }

    fun removeCustomResponseRule(customResponseRule: CustomResponseRule): Boolean {
        return customResponseRules.remove(customResponseRule)
    }

    private fun permErrorFolderResponse(): MockResponse {
        val response = FolderTaskJson(null, createPermErrorStatus(), emptyList(), "taskType")
        return getOkResponse(gson.toJson(response))
    }

    private fun permErrorLabelResponse(): MockResponse {
        val response = LabelTaskJson(null, createPermErrorStatus(), emptyList(), "taskType")
        return getOkResponse(gson.toJson(response))
    }

    private fun permErrorPushResponse(): MockResponse {
        val response = ResponseWithStatusSimple(createPermErrorStatus())
        return getOkResponse(gson.toJson(response))
    }

    private fun tempErrorPushResponse(): MockResponse {
        val response = ResponseWithStatusSimple(createTempErrorStatus())
        return getOkResponse(gson.toJson(response))
    }

    /**
     * We use MessageBatch as response from server. So it needs status.
     */
    data class MessageBatchWithStatus(val status: Status, val messages: List<MessageMetaJson>)

    data class ResponseWithStatusSimple(val status: Status)

    companion object {

        /**
         * We need to order message in thread by timestamp. If message does not include timestamp it will go last as if (timestamp == 0).
         */
        @JvmField
        val MESSAGE_ORDER_COMPARATOR: Comparator<MessageWrapper> = compareByDescending { it.timestamp.time }

        // todo wtf: descending?!
        @JvmField
        val THREAD_ORDER_COMPARATOR: Comparator<ThreadWrapper> = compareByDescending(ThreadWrapper::timestamp)

        @JvmField
        val IMPORTANT_LID = "2000"

        private fun generateProfileContainer(name: String, email: String, url: String): ProfileContainer {
            val profile = ProfileContainer.Profile(
                name,
                "userName",
                "url",
                url,
                "id",
                System.currentTimeMillis()
            )

            return ProfileContainer(email, profile)
        }

        private fun generateAvaResponse(containers: List<ProfileContainer>): AvaResponseJson {
            return AvaResponseJson(createOkStatus(), containers)
        }

        private fun makeDummyResponseWithStatus(): MockResponse {
            val gson = GsonBuilder().create() // provide it in some way
            val response = ResponseWithStatusSimple(createOkStatus())
            return getOkResponse(gson.toJson(response))
        }

        private fun makeDummyResponseStatus(): MockResponse {
            val gson = GsonBuilder().create() // provide it in some way
            val response = createOkStatus()
            return getOkResponse(gson.toJson(response))
        }

        private fun getMd5(value: String): String {
            val md = MessageDigest.getInstance("MD5")
            md.update(value.toByteArray(Charsets.UTF_8))
            val digest = md.digest()
            val sb = StringBuilder()
            for (b in digest) {
                sb.append(String.format("%02x", (0xFF and b.toInt())))
            }
            return sb.toString()
        }
    }
}
