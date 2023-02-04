package ru.auto.ara.test.chat.room

import android.Manifest.permission.CAMERA
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher.Companion.GENERAL_UPLOAD_SIGN
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher.Companion.SIGN
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher.Companion.TECH_SUPPORT_UPLOAD_SIGN
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher.Companion.WITH_TECH_SUPPORT_UPLOAD
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetTechSupportChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.postChatMessage
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.ChatRoomRobotChecker
import ru.auto.ara.core.robot.chat.MUTE
import ru.auto.ara.core.robot.chat.checkChatPicker
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.chat.performChatPicker
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.clickToolbarMenu
import ru.auto.ara.core.utils.createImageAndGetUriString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.feature.chats.model.MessageStatus

@RunWith(AndroidJUnit4::class)
class TechSupportRoomTest {

    private val bootstrapRequestWatcher = RequestWatcher()
    private val uploadRequestWatcher = RequestWatcher()
    private val messagesDispatcher = DispatcherHolder(GetRoomMessagesDispatcher.getEmptyResponse())

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            messagesDispatcher,
            GetChatMessagesBootstrapDispatcher(
                authorId = "5a4becb2853f8029",
                bootstrapWatcher = bootstrapRequestWatcher,
                uploadWatcher = uploadRequestWatcher
            ),
            GetRoomSpamMessagesDispatcher.getEmptyResponse(),
            GetChatRoomDispatcher("autoru_and_vibiralshik"),
            GetTechSupportChatRoomDispatcher()
        )
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        GrantPermissionsRule.grant(CAMERA),
        activityTestRule,
        SetupAuthRule()
    )

    @Test
    fun shouldOpenTechSupportChatWithPhotoPicker() {
        openTechSupportChat(deeplink = SUPPORT_CAMERA_DEEPLINK)

        checkChatPicker {
            isImagePickerDisplayed()
        }
    }

    @Test
    fun shouldSendPhotoToTechSupportNamespaceIfTookFromCamera() {
        webServerRule.routing {
            postChatMessage("common_message")
        }
        openTechSupportChat()

        performChatRoom { openImagePicker() }
        withIntents {
            performCommon {
                interceptActionImageCaptureIntentWithTestImage {
                    performChatPicker { openCamera() }
                }
            }
        }
        checkChatRoom {
            isMessageStatus(1, MessageStatus.SENT)
        }

        bootstrapRequestWatcher.checkQueryParameter(WITH_TECH_SUPPORT_UPLOAD, true.toString())
        uploadRequestWatcher.checkQueryParameter(SIGN, TECH_SUPPORT_UPLOAD_SIGN)
    }

    @Test
    fun shouldSendPhotoOrdinaryNamespaceIfTookFromGallery() {
        openTechSupportChat()

        performChatRoom { openImagePicker() }
        withIntents {
            performCommon { interceptActionPickIntent(createImageAndGetUriString()) }
            performChatPicker { openExternalGallery() }
        }
        checkChatRoom {
            isMessageStatus(1, MessageStatus.SENT)
        }

        bootstrapRequestWatcher.checkQueryParameter(WITH_TECH_SUPPORT_UPLOAD, false.toString())
        uploadRequestWatcher.checkQueryParameter(SIGN, GENERAL_UPLOAD_SIGN )
    }

    @Test
    fun shouldDisplayToolbarMenuWithButtonMuteNotifications() {
        openTechSupportChat()
        checkChatRoom {
            clickToolbarMenu()
            isAvailiableActionsDisplayed(listOf(MUTE))
        }
    }

    @Test
    fun shouldHideReadStatus() {
        messagesDispatcher.innerDispatcher = GetRoomMessagesDispatcher.getRoomWithTwoOutcomingTechSupportMessages()
        openTechSupportChat()

        checkChatRoom {
            // They both sould be same
            isMessageStatus("Это сообщение собеседник прочитал", MessageStatus.SENT)
            isMessageStatus("Это сообщение собеседник ещё не успел прочесть", MessageStatus.SENT)
        }
    }

    @Test
    fun shouldHideReadStatusOnImageMessages() {
        messagesDispatcher.innerDispatcher = GetRoomMessagesDispatcher.getRoomWithTwoOutcomingTechSupportImageMessages()
        openTechSupportChat()

        checkChatRoom {
            // They both sould be same
            isMessageStatus(1, MessageStatus.SENT)
            isMessageStatus(2, MessageStatus.SENT)
        }
    }

    @Test
    fun shouldOpenCameraWhenClickOnDeeplink() {
        messagesDispatcher.innerDispatcher = GetRoomMessagesDispatcher.getRoomWithTechSupportMessage()
        openTechSupportChat()

        performChatRoom {
            interactions.onMessageFormattingText()
                .waitUntilIsDisplayed()
                .performClickClickableSpan("Фото!")
        }
        checkChatPicker { isImagePickerDisplayed() }
    }

    private fun openTechSupportChat(deeplink: String = SUPPORT_DEEPLINK) {
        activityTestRule.launchDeepLinkActivity(deeplink)
        ChatRoomRobotChecker.closeKeyboard(3000L)
    }

    companion object {
        private const val SUPPORT_DEEPLINK = "autoru://app/techsupport"
        private const val SUPPORT_CAMERA_DEEPLINK = "$SUPPORT_DEEPLINK/camera"
    }
}
