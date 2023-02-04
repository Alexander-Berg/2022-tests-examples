package ru.auto.feature.chats.sync

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.data.repository.IScreenVisibilityRepository
import ru.auto.data.repository.user.IUserRepository
import ru.auto.feature.chats.dialogs.data.IDialogsRepository
import ru.auto.feature.chats.model.ChatMessage
import ru.auto.feature.chats.model.MessageId
import ru.auto.feature.chats.model.MessagePayload
import ru.auto.feature.chats.model.MessageStatus
import ru.auto.feature.chats.model.MimeType
import ru.auto.feature.chats.model.SendMessageDetails
import ru.auto.feature.chats.model.SendTextMessageDetails
import ru.auto.feature.chats.model.ServerMessageId
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AllureRunner::class) class ChatSyncInteractorTest {

    val messagesRepository: IMessagesSyncRepository = mock()
    val dialogsRepository: IDialogsRepository = mock()
    val screenVisibilityRepository: IScreenVisibilityRepository = mock()
    val userRepository: IUserRepository = mock()

    private val DIALOG_ID: String = "DIALOG_ID"
    private val USER_ID: String = "USER_ID"
    private val MESSAGE_SERVER_ID = "MESSAGE_SERVER_ID"
    private val MESSAGE_TEXT: String = "MESSAGE_TEXT"

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

    @Test
    fun shouldSendMessagesExactlyInOrderOnlyFiveTimesEveIfGotDuplicates() {
        val sendingMessages = List(5) {
            List(it + 1) { index ->
                SendTextMessageDetails(
                    localId = index.toString(), userId = "", dialogId = "", payload = MessagePayload(
                        value = "",
                        mimeType = MimeType.TEXT_HTML,
                        supportFeedbackParams = null,
                        replyPresets = listOf(),
                        callInfo = null
                    )
                )
            }
        }.flatten()
        println("start sending messages: ${sendingMessages.map { it.localId }.toSet()}")
        whenever(messagesRepository.getMessagesToSend()).thenReturn(Observable.from(sendingMessages))
        whenever(messagesRepository.getSocketMessages()).thenReturn(Observable.never())
        whenever(messagesRepository.sendMessage(any())).then { inv ->
            println("Got message to send: ${inv.getArgument<SendMessageDetails>(0).localId}")
            Observable.timer(50L, TimeUnit.MILLISECONDS).map { message }.toSingle()
        }
        whenever(dialogsRepository.updateLastMessage(any()))
            .thenReturn(Observable.timer(50L, TimeUnit.MILLISECONDS).toCompletable())

        val interactor = ChatSyncInteractor(
            dialogsRepo = dialogsRepository,
            messagesRepo = messagesRepository,
            screenVisibilityRepo = screenVisibilityRepository,
            userRepository = userRepository,
            isDevOrDebug = false
        )

        val latch = CountDownLatch(1)
        interactor.startSendingMessages().subscribeOn(Schedulers.io()).doOnCompleted { latch.countDown() }.subscribe()
        latch.await()

        verify(messagesRepository, times(5)).sendMessage(any())
    }
}
