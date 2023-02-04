package ru.auto.data.interactor.chat

import android.content.SharedPreferences
import com.google.gson.Gson
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.data.model.chat.ChatDialog
import ru.auto.data.model.chat.ChatType
import ru.auto.data.repository.IApp2AppCallInfoRepository
import ru.auto.feature.chats.dialogs.data.IDialogsRepository
import ru.auto.feature.chats.messages.MessagesInteractor
import ru.auto.feature.chats.messages.MessagesInteractor.Companion.ADDRESS_COMBINATION
import ru.auto.feature.chats.messages.MessagesInteractor.Companion.ADDRESS_TEXT
import ru.auto.feature.chats.messages.MessagesInteractor.Companion.DEFAULT_GEO
import ru.auto.feature.chats.messages.data.IMessagePresetsRepository
import ru.auto.feature.chats.messages.data.IMessagesRepository
import ru.auto.feature.chats.model.ChatMessage
import ru.auto.feature.chats.model.ChatMessagesResult
import ru.auto.feature.chats.model.LocalMessageId
import ru.auto.feature.chats.model.MessageId
import ru.auto.feature.chats.model.MessagePayload
import ru.auto.feature.chats.model.MessageStatus
import ru.auto.feature.chats.model.MessagesContext
import ru.auto.feature.chats.model.MessagesDialogContext
import ru.auto.feature.chats.model.MimeType
import ru.auto.feature.chats.model.ServerMessageId
import rx.Completable
import rx.Observable
import rx.Single
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class MessagesInteractorTest {
    private val DIALOG_ID = "DIALOG_ID"
    private val USER_ID = "USER_ID"
    private val SENDER_ID = "SENDER_ID"
    private val MESSAGE_TEXT: String = "MESSAGE_TEXT"
    private val IMAGE_URI: String = "IMAGE_URI"
    private val MESSAGE_SERVER_ID = "MESSAGE_SERVER_ID"
    private val PAGE_SIZE: Int = 100
    private val messageId: MessageId =
        ServerMessageId(MESSAGE_SERVER_ID, "localId")
    private val messageAddressID = LocalMessageId(localId = "addresslocalId")

    private val messagesContext: MessagesContext =
        MessagesDialogContext(DIALOG_ID, "") // it is agnostic to the type od messages context
    private val messagePayload: MessagePayload =
        MessagePayload(MESSAGE_TEXT, MimeType.TEXT_PLAIN)

    private val messageAddressWidgetPayload: MessagePayload =
        MessagePayload(Gson().toJson(DEFAULT_GEO), MimeType.TEXT_PLAIN, isWidget = true)

    private val messagesWithSpecialWords = ADDRESS_COMBINATION.map { words ->
        MessagePayload(words.map { it.first }.joinToString(separator = " "), MimeType.TEXT_PLAIN)
    }.plus(MessagePayload(ADDRESS_TEXT.plus(" $MESSAGE_TEXT"), MimeType.TEXT_PLAIN))

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
    private val chatDialog: ChatDialog = ChatDialog(
        id = DIALOG_ID,
        photo = null,
        title = "dialog title",
        subject = null,
        lastMessage = null,
        hasUnreadMessages = false,
        users = emptyList(),
        currentUserId = USER_ID,
        created = Date(),
        updated = Date(),
        isMuted = false,
        chatType = ChatType.ROOM_TYPE_OFFER,
        isBlocked = false,
        pinGroup = 0,
        chatOnly = false,
        lastMessageIsSpam = false,
        lastMessageServerId = null
    )
    private val chatMessagesResult = ChatMessagesResult(
        canLoadMore = false,
        data = listOf(message),
        currentUserId = USER_ID,
        loadingMessage = message,
        presets = emptyList()
    )

    private val currentDate = Date()
    private val messageAddressWidget: ChatMessage =
        ChatMessage(
            id = messageAddressID,
            dialogId = DIALOG_ID,
            payload = messageAddressWidgetPayload,
            createdAt = currentDate,
            status = MessageStatus.SENT,
            userId = SENDER_ID,
            attachments = emptyList()
        )


    private val prefs: SharedPreferences = mock()
    private val messagesRepo: IMessagesRepository = mock()
    private val dialogsRepo: IDialogsRepository = mock() {
        on { observeChatUsersForDialog(any()) } doReturn (Observable.just(emptyList()))
    }
    private val presetsRepo: IMessagePresetsRepository = mock {
        on { getPresets(any()) } doReturn (Single.just(emptyList()))
        on { getSentPresetsForDialog(any()) } doReturn (Single.just(emptySet()))
    }
    private val app2appCallInfoRepo: IApp2AppCallInfoRepository = mock()


    @Before
    fun setUp() {
        whenever(dialogsRepo.getDialog(messagesContext)).thenReturn(Single.just(chatDialog))
        whenever(messagesRepo.preloadMessages(eq(DIALOG_ID), any())).thenReturn(Completable.complete())
        whenever(messagesRepo.queueSendMessage(eq(USER_ID), eq(DIALOG_ID), any())).thenReturn(Completable.complete())
        whenever(messagesRepo.queueUploadImage(eq(USER_ID), eq(DIALOG_ID), any(), any())).thenReturn(Completable.complete())
    }

    @Test
    fun `it caches given dialog`() {
        whenever(messagesRepo.getMessages(any(), any())).thenReturn(Observable.never())
        val interactor = MessagesInteractor(
            messagesContext = messagesContext,
            dialogsRepo = dialogsRepo,
            messagesRepo = messagesRepo,
            presetsRepo = presetsRepo,
            prefs = prefs,
            app2AppCallInfoRepository = app2appCallInfoRepo
        )

        interactor.getDialog().test().assertCompleted()
        interactor.getDialog().test().assertCompleted()

        verify(dialogsRepo, times(1)).getDialog(any())
    }

    @Test
    fun `it extracts dialog`() {
        whenever(messagesRepo.getMessages(any(), any())).thenReturn(Observable.never())
        val interactor = MessagesInteractor(
            messagesContext = messagesContext,
            dialogsRepo = dialogsRepo,
            messagesRepo = messagesRepo,
            presetsRepo = presetsRepo,
            prefs = prefs,
            app2AppCallInfoRepository = app2appCallInfoRepo
        )

        val value = interactor.getDialog().toBlocking().value()

        assertEquals(chatDialog, value.dialog)
    }

    @Test
    fun `it extracts messages`() {
        whenever(messagesRepo.getMessages(any(), any())).thenReturn(Observable.just(chatMessagesResult))
        val interactor = MessagesInteractor(
            messagesContext = messagesContext,
            dialogsRepo = dialogsRepo,
            messagesRepo = messagesRepo,
            presetsRepo = presetsRepo,
            prefs = prefs,
            app2AppCallInfoRepository = app2appCallInfoRepo
        )

        val value = interactor.getDialog().toBlocking().value().messages.toBlocking().first()

        assertEquals(listOf(message), value.data)
        assertEquals(USER_ID, value.currentUserId)
    }


    @Test
    fun `it extracts dialog params and queues message`() {
        val interactor = MessagesInteractor(
            messagesContext = messagesContext,
            dialogsRepo = dialogsRepo,
            messagesRepo = messagesRepo,
            presetsRepo = presetsRepo,
            prefs = prefs,
            app2AppCallInfoRepository = app2appCallInfoRepo
        )

        interactor.sendMessage(messagePayload).test().assertCompleted()

        verify(messagesRepo, times(1)).queueSendMessage(USER_ID, DIALOG_ID, messagePayload)
    }

    @Test
    fun `it extracts dialog params and queues image`() {
        val interactor = MessagesInteractor(
            messagesContext = messagesContext,
            dialogsRepo = dialogsRepo,
            messagesRepo = messagesRepo,
            presetsRepo = presetsRepo,
            prefs = prefs,
            app2AppCallInfoRepository = app2appCallInfoRepo
        )

        interactor.sendImage(IMAGE_URI, false).test().assertCompleted()

        verify(messagesRepo, times(1)).queueUploadImage(USER_ID, DIALOG_ID, IMAGE_URI, false)
    }

    @Test
    fun `it commands to load older messages`() {
        whenever(messagesRepo.getMessages(any(), any())).thenReturn(
            Observable.concat(
                Observable.just(chatMessagesResult),
                Observable.never()
            )
        )
        whenever(messagesRepo.loadOlderMessages(eq(DIALOG_ID), any())).thenReturn(Completable.complete())
        val interactor = MessagesInteractor(
            messagesContext = messagesContext,
            dialogsRepo = dialogsRepo,
            messagesRepo = messagesRepo,
            presetsRepo = presetsRepo,
            prefs = prefs,
            app2AppCallInfoRepository = app2appCallInfoRepo
        )
        interactor.getDialog().subscribe { it.messages.test() }

        interactor.loadOlderMessages().test().assertCompleted()

        verify(messagesRepo, times(1)).loadOlderMessages(DIALOG_ID, PAGE_SIZE)
    }

    @Test
    fun `it extracts address widget`() {
        val interactor = MessagesInteractor(
            messagesContext = messagesContext,
            dialogsRepo = dialogsRepo,
            messagesRepo = messagesRepo,
            presetsRepo = presetsRepo,
            prefs = prefs,
            app2AppCallInfoRepository = app2appCallInfoRepo
        )

        messagesWithSpecialWords.forEach { payLoad ->
            val message = ChatMessage(
                id = messageId,
                dialogId = DIALOG_ID,
                payload = payLoad,
                createdAt = currentDate,
                status = MessageStatus.SENT,
                userId = SENDER_ID,
                attachments = emptyList()
            )

            val chatMessagesResult = ChatMessagesResult(
                canLoadMore = false,
                data = listOf(message),
                currentUserId = SENDER_ID,
                loadingMessage = message,
                presets = emptyList()
            )

            whenever(messagesRepo.getMessages(any(), any())).thenReturn(Observable.just(chatMessagesResult))

            val value = interactor.getDialog().toBlocking().value().messages.toBlocking().first()
            val expectedList = listOf(message, messageAddressWidget)
            assertEquals(expectedList, value.data)
        }
    }

    @Test
    fun `it does not extract address widget in RoomType each`() {
        ChatType.values().toList().minus(ChatType.ROOM_TYPE_OFFER).forEach { chatType ->
            val dialog = chatDialog.copy(chatType = chatType)
            whenever(dialogsRepo.getDialog(messagesContext)).thenReturn(Single.just(dialog))
            val interactor = MessagesInteractor(
                messagesContext = messagesContext,
                dialogsRepo = dialogsRepo,
                messagesRepo = messagesRepo,
                presetsRepo = presetsRepo,
                prefs = prefs,
                app2AppCallInfoRepository = app2appCallInfoRepo
            )
            messagesWithSpecialWords.forEach { payLoad ->
                val message = ChatMessage(
                    id = messageId,
                    dialogId = DIALOG_ID,
                    payload = payLoad,
                    createdAt = currentDate,
                    status = MessageStatus.SENT,
                    userId = SENDER_ID,
                    attachments = emptyList()
                )

                val chatMessagesResult = ChatMessagesResult(
                    canLoadMore = false,
                    data = listOf(message),
                    currentUserId = SENDER_ID,
                    loadingMessage = message,
                    presets = emptyList()
                )
                whenever(messagesRepo.getMessages(any(), any())).thenReturn(Observable.just(chatMessagesResult))

                val value = interactor.getDialog().toBlocking().value().messages.toBlocking().first()
                val expectedList = listOf(message)
                assertEquals(expectedList, value.data)
            }

        }
    }
}
