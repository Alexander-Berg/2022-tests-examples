package ru.auto.data.interactor.chat

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.model.AccessLevel
import ru.auto.data.model.AccessResource
import ru.auto.data.model.ResourceAlias
import ru.auto.data.model.User
import ru.auto.data.model.UserProfile
import ru.auto.data.model.chat.ChatDialog
import ru.auto.data.model.chat.ChatType
import ru.auto.data.model.chat.ChatUser
import ru.auto.data.repository.user.IUserRepository
import ru.auto.feature.chats.dialogs.DialogsInteractor
import ru.auto.feature.chats.dialogs.data.IDialogsRepository
import ru.auto.feature.chats.model.DialogAction
import rx.Observable
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class DialogsInteractorTest {

    private val DIALOG_ID = "DIALOG_ID"
    private val USER_ID = "USER_ID"

    private val basicChatStub = ChatDialog(
        id = DIALOG_ID,
        photo = null,
        title = "",
        subject = null,
        lastMessage = null,
        hasUnreadMessages = false,
        users = emptyList(),
        currentUserId = USER_ID,
        created = Date(),
        updated = Date(),
        isMuted = false,
        isBlocked = false,
        chatType = ChatType.ROOM_TYPE_OFFER,
        pinGroup = 0,
        chatOnly = false,
        lastMessageIsSpam = false,
        lastMessageServerId = null
    )

    private val chatUser = ChatUser(USER_ID, null, false, null, null, null, null)

    private val supportChatStub = basicChatStub.copy(chatType = ChatType.ROOM_TYPE_TECH_SUPPORT)
    private val mutedChatStub = basicChatStub.copy(isMuted = true)
    private val blockedByUserChatStub = basicChatStub.copy(
        isBlocked = true,
        users = listOf(chatUser.copy(blockedDialog = true))
    )
    private val blockedByOtherUserChatStub = basicChatStub.copy(
        isBlocked = true,
        users = listOf(chatUser.copy(blockedDialog = false))
    )

    private val dialogsRepository: IDialogsRepository = mock()
    private val userRepository = mock<IUserRepository>().apply {
        whenever(user).thenReturn(
            User.Authorized(
                id = "",
                userProfile = UserProfile(),
                grants = listOf(AccessResource(ResourceAlias.CHATS, AccessLevel.READ_WRITE))
            )
        )
    }

    private val interactor =
        DialogsInteractor(dialogsRepository, userRepository)


    @Test
    fun `given support chat it should be always first item`() {
        val dialogs = List(10, { basicChatStub }) + supportChatStub
        whenever(dialogsRepository.getDialogs()).thenReturn(Observable.just(dialogs))

        val sortedDialogs = interactor.observeDialogs().toBlocking().first()

        assertEquals(supportChatStub, sortedDialogs.first())
    }


    @Test
    fun `given basic chat it should make block, mute and delete actions available`() {
        val actions = interactor.getDialogActions(basicChatStub)

        assertThat(actions).containsExactlyInAnyOrder(
            DialogAction.Mute(DIALOG_ID, true),
            DialogAction.Block(DIALOG_ID, true),
            DialogAction.Remove(DIALOG_ID)
        )
    }

    @Test
    fun `given support chat it should make only mute action available`() {
        val actions = interactor.getDialogActions(supportChatStub)

        assertThat(actions).containsExactly(DialogAction.Mute(DIALOG_ID, true))
    }

    @Test
    fun `given muted chat it should make unmute action available`() {
        val actions = interactor.getDialogActions(mutedChatStub)

        assertThat(actions).contains(DialogAction.Mute(DIALOG_ID, false))
    }

    @Test
    fun `given chat blocked by current user it should make only unblock and delete actions available`() {
        val actions = interactor.getDialogActions(blockedByUserChatStub)

        assertThat(actions).containsExactlyInAnyOrder(
            DialogAction.Block(DIALOG_ID, false),
            DialogAction.Remove(DIALOG_ID)
        )
    }

    @Test
    fun `given chat blocked by another user it should make only delete action available`() {
        val actions = interactor.getDialogActions(blockedByOtherUserChatStub)

        assertThat(actions).containsExactly(DialogAction.Remove(DIALOG_ID))
    }
}
