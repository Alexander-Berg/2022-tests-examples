package ru.auto.feature.chats.messages.data.database

import com.google.gson.Gson
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.data.repository.LegacyNetworkInfoRepository
import ru.auto.data.exception.NotFoundException
import ru.auto.data.managers.ExternalFileManager
import ru.auto.data.model.Size
import ru.auto.data.model.chat.ChatDialog
import ru.auto.data.model.chat.ChatType
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.network.scala.response.chat.MessageListingResponse
import ru.auto.data.prefs.IPrefsDelegate
import ru.auto.feature.chats.messages.data.MessagesRepository
import ru.auto.feature.chats.model.ChatMessage
import ru.auto.feature.chats.model.MessageId
import ru.auto.feature.chats.model.MessagePayload
import ru.auto.feature.chats.model.MessageStatus
import ru.auto.feature.chats.model.MimeType
import ru.auto.feature.chats.model.ServerMessageId
import ru.auto.feature.chats.sync.socket.XivaSocketService
import rx.Completable
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class MessagesRepositoryTestIMessagesInterface {

    private val api: ScalaApi = mock()
    private val socketService: XivaSocketService = mock()
    private val externalFileManager: ExternalFileManager = mock()
    private val prefs: IPrefsDelegate = mock()
    private val messagesStorage: ChatMessageStorage = mock()
    private val legacyNetworkRepository: LegacyNetworkInfoRepository = mock()

    private val DIALOG_ID: String = "DIALOG_ID"
    private val USER_ID: String = "USER_ID"
    private val PAGE_SIZE: Int = 100
    private val MESSAGE_SERVER_ID = "MESSAGE_SERVER_ID"
    private val MESSAGE_TEXT: String = "MESSAGE_TEXT"
    private val IMAGE_URI: String = "IMAGE_URI"

    private val messagePayload: MessagePayload =
        MessagePayload(MESSAGE_TEXT, MimeType.TEXT_PLAIN)
    private val messageId: MessageId =
        ServerMessageId(MESSAGE_SERVER_ID, "localId")
    private val message: ChatMessage =
        ChatMessage(
            id = messageId,
            dialogId = DIALOG_ID,
            payload = messagePayload,
            createdAt = Date(),
            status = MessageStatus.SENDING,
            userId = USER_ID,
            attachments = emptyList()
        )

    @Suppress("MaxLineLength") // json is hard to format
    private val messageListingResponse = Gson()
        .fromJson<MessageListingResponse>("{\"messages\":[{\"id\":\"11e80a77032a260090e383ed82db1d41\",\"room_id\":\"05336a7263e59f183c2329992eac9297\",\"author\":\"6c0ba9cc088296aa\",\"created\":\"2018-02-05T13:18:10.528Z\",\"payload\":{\"content_type\":\"TEXT_PLAIN\",\"value\":\"hello broh\"},\"provided_id\":\"67a19c33-a2b8-4a99-aca3-1daf2ac5320f\"},{\"id\":\"11e80a772503eb8090e383ed82db1d41\",\"room_id\":\"05336a7263e59f183c2329992eac9297\",\"author\":\"1b22cf5f3f05e43a\",\"created\":\"2018-02-05T13:19:07.321Z\",\"payload\":{\"content_type\":\"TEXT_PLAIN\",\"value\":\"hello, hello\"},\"provided_id\":\"4d556866-2980-4f0f-8771-0469c0679ce0\"},{\"id\":\"11e80a772a00786090e383ed82db1d41\",\"room_id\":\"05336a7263e59f183c2329992eac9297\",\"author\":\"1b22cf5f3f05e43a\",\"created\":\"2018-02-05T13:19:15.686Z\",\"payload\":{\"content_type\":\"TEXT_PLAIN\",\"value\":\"how youre doing?\"},\"provided_id\":\"3beb79d6-1ebb-4cf1-a09d-c8520cd46074\"},{\"id\":\"11e80a776051a6f090e383ed82db1d41\",\"room_id\":\"05336a7263e59f183c2329992eac9297\",\"author\":\"6c0ba9cc088296aa\",\"created\":\"2018-02-05T13:20:46.815Z\",\"payload\":{\"content_type\":\"TEXT_PLAIN\",\"value\":\"how about you?\"},\"provided_id\":\"82af84bf-ede7-42cf-bf38-172ade356d00\"},{\"id\":\"11e80a7824d93c9090e383ed82db1d41\",\"room_id\":\"05336a7263e59f183c2329992eac9297\",\"author\":\"1b22cf5f3f05e43a\",\"created\":\"2018-02-05T13:26:16.537Z\",\"payload\":{\"content_type\":\"TEXT_PLAIN\",\"value\":\"hey broh\"},\"provided_id\":\"8791de06-c129-40c4-b27b-85713c3244cc\"}]}", MessageListingResponse::class.java)
    private val chatDialog = ChatDialog(
        id = DIALOG_ID,
        photo = null,
        title = "",
        subject = null,
        lastMessage = null,
        hasUnreadMessages = false,
        users = emptyList(),
        currentUserId = "",
        created = Date(),
        updated = Date(),
        isBlocked = false,
        chatType = ChatType.ROOM_TYPE_OFFER,
        isMuted = false,
        pinGroup = 0,
        chatOnly = false,
        lastMessageServerId = null,
        lastMessageIsSpam = false
    )

    @Before
    fun setUp() {
        whenever(messagesStorage.upsertMessage(any()))
            .thenReturn(Completable.complete())
        whenever(
            messagesStorage.updateMessageMarkLoadedLast(
                any(),
                any()
            )
        ).thenReturn(Completable.complete())
    }

    @Test
    fun `it queues message sending`() {
        val repo = MessagesRepository(
            api,
            socketService,
            externalFileManager,
            prefs,
            messagesStorage
        )
        val captor = argumentCaptor<ChatMessage>()

        repo.queueSendMessage(USER_ID, DIALOG_ID, messagePayload).test().assertCompleted()

        verify(messagesStorage).upsertMessage(captor.capture())
        assertEquals(MessageStatus.SENDING, captor.firstValue.status)
        assertEquals(messagePayload, captor.firstValue.payload)
    }

    @Test
    fun `it queues image sending`() {
        whenever(externalFileManager.getImageSize(any()))
            .thenReturn(Size(width = 0, height = 0))
        val repo = MessagesRepository(
            api,
            socketService,
            externalFileManager,
            prefs,
            messagesStorage
        )
        val captor = argumentCaptor<ChatMessage>()

        repo.queueUploadImage(USER_ID, DIALOG_ID, IMAGE_URI, false).test().assertCompleted()

        verify(messagesStorage).upsertMessage(captor.capture())
        assertEquals(MessageStatus.SENDING, captor.firstValue.status)
        Assertions.assertThat(captor.firstValue.attachments.isNotEmpty())
    }

    @Test
    @Ignore
    fun `it gets messages from storage`() {
        whenever(messagesStorage.getMessagesByDialog(eq(DIALOG_ID))).thenReturn(
            Observable.concat(
                Observable.just(listOf(message)),
                Observable.never<List<ChatMessage>>()
            )
        )
        whenever(
            api.getChatMessages(
                eq(DIALOG_ID),
                anyOrNull(),
                eq(PAGE_SIZE),
                any()
            )
        ).thenReturn(Single.error(NotFoundException()))
        val repo = MessagesRepository(
            api,
            socketService,
            externalFileManager,
            prefs,
            messagesStorage
        )

        repo.getMessages(chatDialog, PAGE_SIZE).test()

        verify(messagesStorage).getMessagesByDialog(DIALOG_ID)
    }

    @Test
    @Ignore
    fun `it pulls messages from api`() {
        val subject = BehaviorSubject.create<List<ChatMessage>>(emptyList())
        whenever(messagesStorage.getMessagesByDialog(eq(DIALOG_ID))).thenReturn(subject)
        whenever(
            api.getChatMessages(
                eq(DIALOG_ID),
                anyOrNull(),
                eq(PAGE_SIZE),
                any()
            )
        ).thenReturn(Single.just(messageListingResponse))
        val messagesCaptor = argumentCaptor<ChatMessage>()
        whenever(messagesStorage.upsertMessage(messagesCaptor.capture()))
            .then { subject.onNext(messagesCaptor.allValues) }
            .thenReturn(Completable.complete())
        val repo = MessagesRepository(
            api,
            socketService,
            externalFileManager,
            prefs,
            messagesStorage
        )

        repo.getMessages(chatDialog, PAGE_SIZE).test()

        verify(api).getChatMessages(
            eq(DIALOG_ID),
            anyOrNull(),
            eq(PAGE_SIZE),
            any()
        )
    }

    @Ignore
    @Test
    fun `it pulls older messages from api`() {
        whenever(
            api.getChatMessages(
                eq(DIALOG_ID),
                eq(messageId.id),
                any(),
                any()
            )
        ).thenReturn(Single.just(MessageListingResponse()))
        whenever(messagesStorage.upsertAll(any()))
            .thenReturn(Completable.complete())
        whenever(
            messagesStorage.upsertAllAndMark(
                any(),
                any()
            )
        ).thenReturn(Completable.complete())
        whenever(messagesStorage.getLatestLoadedMessage()).thenReturn(Single.just(message))
        val repo = MessagesRepository(
            api,
            socketService,
            externalFileManager,
            prefs,
            messagesStorage
        )

        repo.loadOlderMessages(DIALOG_ID, PAGE_SIZE).test().assertCompleted()

        verify(api).getChatMessages(
            eq(DIALOG_ID),
            eq(messageId.id),
            eq(PAGE_SIZE),
            eq(false)
        )
    }

}
