package com.yandex.mobile.realty.test.chats

import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.ChatMessagesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createImageAndGetUriString
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.registerPhoneConfirmation
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.robot.performOnGalleryScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author matek3022 on 2020-02-07.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ChatMessagesTest : BaseTest() {

    private val activityTestRule = ChatMessagesActivityTestRule(
        chatId = CHAT_ID,
        launchActivity = false
    )
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldSendImageMessage() {
        configureWebServer {
            registerTechSupportChat()
            registerEmptyListMessages()
            registerSendPhotoMessage("1")
            registerUploadPhotoMessage("1")
        }

        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()
        registerGetContentIntent(createImageAndGetUriString())

        performOnChatMessagesScreen {
            waitUntil { isSelectImageButtonShown() }

            tapOn(lookup.matchesSelectImageButton())

            waitUntil {
                containsImageMessage()
                isImageMessageImageShown()
                isImageMessageRefreshHidden()
                isImageMessageProgressHidden()
            }
        }
    }

    @Test
    fun shouldOpenImageScreen() {
        val image = createImageAndGetUriString(
            rColor = 0xAA,
            gColor = 0xAA,
            bColor = 0xAA,
        )
        configureWebServer {
            registerTechSupportChat()
            registerPhotoMessage(image)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsImageMessage() }
            tapOn(lookup.matchesImageMessageImageView())
        }
        performOnGalleryScreen {
            isPhotoShown()
            isViewStateMatches(getTestRelatedFilePath("galleryScreen"))
        }
    }

    @Test
    fun shouldShowEmptyTechSupportChat() {
        configureWebServer {
            registerTechSupportChat()
            registerEmptyListMessages()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isFullscreenEmptyViewShown() }

            performOnFullscreenEmpty {
                isImageEquals(R.drawable.ill_support_chat)
                isTitleEquals(R.string.chat_empty_messages_title)
                isDescriptionEquals(R.string.chat_support_description_full)
            }

            isInputFieldShown()
            isSelectImageButtonShown()
            isSendButtonShown()
            isSendButtonDisabled()
        }
    }

    @Test
    fun shouldShowFullScreenError() {
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isFullscreenErrorViewShown() }

            performOnFullscreenError {
                isImageEquals(R.drawable.ill_error)
                isTitleEquals(R.string.error_load_title)
                isDescriptionEquals(R.string.error_description_retry)
                isRetryTextEquals(R.string.retry)
            }
        }
    }

    @Test
    fun shouldShowSecondPageError() {
        configureWebServer {
            registerTechSupportChat()
            registerFirstPageMessages()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isSecondPageErrorShown() }
        }
    }

    @Test
    fun shouldShowFirstAndSecondPages() {
        configureWebServer {
            registerTechSupportChat()
            registerSecondPageMessages()
            registerFirstPageMessages()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsTextMessage("not mine first") }
            waitUntil { containsTextMessage("mine second") }
        }
    }

    @Test
    fun shouldSendTextMessage() {
        val message = "text send"
        configureWebServer {
            registerTechSupportChat()
            registerEmptyListMessages()
            registerSendTextMessage("1", message)
        }

        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isInputFieldShown() }
            isInputFieldEmpty()
            isSendButtonDisabled()

            typeText(lookup.matchesInputField(), message)

            isSendButtonEnabled()
            tapOn(lookup.matchesSendButton())

            isInputFieldEmpty()
            isSendButtonDisabled()
            waitUntil { containsTextMessage(message) }
            waitUntil { isTextMessageSent(message) }
        }
    }

    @Test
    fun shouldShowPollEnabled() {
        configureWebServer {
            registerTechSupportChat()
            registerPollEnabledPage("poll-id-1")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsPollMessage() }
            isPollShown()
            isAllPollButtonsEnabled()
        }
    }

    @Test
    fun shouldPollVoteBad() {
        val pollId = "poll-id-1"
        val dispatcher = DispatcherRegistry()
        dispatcher.registerTechSupportChat()
        dispatcher.registerPollEnabledPage(pollId)
        val expectedVoteRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/chat/tech-support/poll/$pollId")
                queryParam("rating", "1")
            },
            success()
        )
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsPollMessage() }
            isPollShown()
            tapOn(lookup.matchesPollBadButton())
            waitUntil { isBadButtonInPollSelected() }
            waitUntil { expectedVoteRequest.isOccured() }
        }
    }

    @Test
    fun shouldPollVoteNormal() {
        val pollId = "poll-id-1"
        val dispatcher = DispatcherRegistry()
        dispatcher.registerTechSupportChat()
        dispatcher.registerPollEnabledPage(pollId)
        val expectedVoteRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/chat/tech-support/poll/$pollId")
                queryParam("rating", "2")
            },
            success()
        )
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsPollMessage() }
            isPollShown()
            tapOn(lookup.matchesPollNormalButton())
            waitUntil { isNormalButtonInPollSelected() }
            waitUntil { expectedVoteRequest.isOccured() }
        }
    }

    @Test
    fun shouldPollVoteGood() {
        val pollId = "poll-id-1"
        val dispatcher = DispatcherRegistry()
        dispatcher.registerTechSupportChat()
        dispatcher.registerPollEnabledPage(pollId)
        val expectedVoteRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/chat/tech-support/poll/$pollId")
                queryParam("rating", "3")
            },
            success()
        )
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsPollMessage() }
            isPollShown()
            tapOn(lookup.matchesPollGoodButton())
            waitUntil { isGoodButtonInPollSelected() }
            waitUntil { expectedVoteRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowPollNotEnabled() {
        configureWebServer {
            registerTechSupportChat()
            registerPollNotEnabledPage()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsPollMessage() }
            isPollShown()
            isAllPollButtonsDisabled()
        }
    }

    @Test
    fun shouldSendFeedbackPresetMessage() {
        val firstPreset = "All good"
        val secondPreset = "Too long"
        configureWebServer {
            registerTechSupportChat()
            registerPresetsEnabledPage()
            registerSendFeedbackPresetMessage("1", firstPreset, "1", "all_good")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsTextMessage("Preset message") }
            isPresetWithTextShown(firstPreset)
            isPresetWithTextShown(secondPreset)
            tapOn(lookup.matchesPresetWithText(firstPreset))
            waitUntil { containsTextMessage(firstPreset) }
            waitUntil { isTextMessageSent(firstPreset) }
        }
    }

    @Test
    fun shouldPreservePresetsAfterMessageSent() {
        val testOutputMessage = "Test output message"
        val firstPreset = "All good"
        val secondPreset = "Too long"
        configureWebServer {
            registerTechSupportChat()
            registerPresetsEnabledPage()
            registerSendTextMessage("1", testOutputMessage)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsTextMessage("Preset message") }
            isPresetWithTextShown(firstPreset)
            isPresetWithTextShown(secondPreset)
            typeText(lookup.matchesInputField(), testOutputMessage)
            tapOn(lookup.matchesSendButton())
            waitUntil { containsTextMessage(testOutputMessage) }
            containsTextMessage("Preset message")
            isPresetWithTextShown(firstPreset)
            isPresetWithTextShown(secondPreset)
        }
    }

    @Test
    fun shouldShowAndSendResponsePresets() {
        val incomingMessage = "Incoming message"
        val incomingMessageId = "123"
        val preset1 = "1" to "Yes"
        val preset2 = "2" to "No"
        configureWebServer {
            registerTechSupportChat()
            registerIncomingMessageWithPresets(
                text = incomingMessage,
                messageId = incomingMessageId,
                presets = listOf(preset1, preset2)
            )
            registerSendPresetMessage("1", preset1)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsTextMessage(incomingMessage) }
            isResponsePresetWithTextShown(preset1.second)
            isResponsePresetWithTextShown(preset2.second)
            tapOn(lookup.matchesResponsePresetWithText(preset1.second))
            waitUntil { containsTextMessage(preset1.second) }
            waitUntil { isTextMessageSent(preset1.second) }
            doesNotContainResponsePresets()
        }
    }

    @Test
    fun shouldHideResponsePresetsAfterTextSend() {
        val incomingMessage = "Incoming message"
        val outputMessage = "Output message"
        val preset1 = "1" to "Yes"
        val preset2 = "2" to "No"
        configureWebServer {
            registerTechSupportChat()
            registerIncomingMessageWithPresets(
                text = incomingMessage,
                messageId = "123",
                presets = listOf(preset1, preset2)
            )
            registerSendTextMessage("1", outputMessage)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsTextMessage(incomingMessage) }
            isResponsePresetWithTextShown(preset1.second)
            isResponsePresetWithTextShown(preset2.second)
            typeText(lookup.matchesInputField(), outputMessage)
            tapOn(lookup.matchesSendButton())
            waitUntil { containsTextMessage(outputMessage) }
            waitUntil { isTextMessageSent(outputMessage) }
            doesNotContainResponsePresets()
        }
    }

    @Test
    fun shouldShowAndSendResponsePresetsWithoutIds() {
        val incomingMessage = "Incoming message"
        val incomingMessageId = "123"
        val preset1 = null to "Yes"
        val preset2 = null to "No"
        configureWebServer {
            registerTechSupportChat()
            registerIncomingMessageWithPresets(
                text = incomingMessage,
                messageId = incomingMessageId,
                presets = listOf(preset1, preset2)
            )
            registerSendPresetMessage("1", preset1)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { containsTextMessage(incomingMessage) }
            isResponsePresetWithTextShown(preset1.second)
            isResponsePresetWithTextShown(preset2.second)
            tapOn(lookup.matchesResponsePresetWithText(preset1.second))
            waitUntil { containsTextMessage(preset1.second) }
            waitUntil { isTextMessageSent(preset1.second) }
            doesNotContainResponsePresets()
        }
    }

    @Test
    fun shouldConfirmPhone() {
        val incomingMessage = "Confirm phone"
        val preset = "Confirm"
        val infoMessage = getResourceString(R.string.chat_phone_confirmed_message)

        val dispatcher = DispatcherRegistry()
        dispatcher.registerTechSupportChat()
        dispatcher.registerPhoneConfirmationRequestMessage(
            text = incomingMessage,
            action = preset
        )
        val sendMessageRequest = dispatcher.registerPhoneConfirmationResponseMessage(infoMessage)
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerPhoneConfirmation()

        onScreen<ChatMessagesScreen> {
            actionButtonFrame(preset)
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(
                        "ChatMessagesTest/shouldConfirmPhone/confirmationRequested"
                    )
                }
                .button
                .click()

            infoMessage(infoMessage).waitUntil { listView.contains(this) }
            root.isViewStateMatches("ChatMessagesTest/shouldConfirmPhone/phoneConfirmed")

            waitUntil { sendMessageRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowHtmlTextMessageWithList() {
        val incomingMessage =
            """Список:<ul><li>Элемент 1</li><li>Элемент 2</li><li>Элемент 3</li></ul>"""
        val formattedIncomingMessage = "Список:\n• Элемент 1\n• Элемент 2\n• Элемент 3"
        configureWebServer {
            registerTechSupportChat()
            registerIncomingMessageWithHtmlTags(incomingMessage)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            incomingTextMessage(formattedIncomingMessage).waitUntil { listView.contains(this) }
            root.isViewStateMatches("ChatMessagesTest/shouldShowHtmlTextMessageWithList/message")
        }
    }

    @Test
    fun shouldShowHtmlTextMessageWithLinks() {
        val firstTagLinkUrl = "https://realty.yandex.ru"
        val firstTagLinkText = "tag link"
        val secondTagLinkUrl = "https://realty.yandex.ru/management-new/"
        val secondTagLinkText = "realty.yandex.ru"
        val plainLinkUrl = "https://arenda.yandex.ru/"
        val plainLinkText = "arenda.yandex.ru/"
        val incomingMessage = """<a href=\"$firstTagLinkUrl\">$firstTagLinkText</a>""" +
            """<br/><a href=\"$secondTagLinkUrl\">$secondTagLinkText</a>""" +
            """<br/>$plainLinkText<br/>some text"""
        val formattedIncomingMessage =
            "$firstTagLinkText\n$secondTagLinkText\n$plainLinkText\nsome text"
        configureWebServer {
            registerTechSupportChat()
            registerIncomingMessageWithHtmlTags(incomingMessage)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesOpenLinkIntent(firstTagLinkUrl), null)
        registerResultOkIntent(matchesOpenLinkIntent(secondTagLinkUrl), null)
        registerResultOkIntent(matchesOpenLinkIntent(plainLinkUrl), null)

        onScreen<ChatMessagesScreen> {
            incomingTextMessage(formattedIncomingMessage)
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(
                        "ChatMessagesTest/shouldShowHtmlTextMessageWithLinks/message"
                    )
                }
                .textView
                .apply {
                    tapOnLinkText(firstTagLinkText)
                    intended(matchesOpenLinkIntent(firstTagLinkUrl))

                    tapOnLinkText(secondTagLinkText)
                    intended(matchesOpenLinkIntent(secondTagLinkUrl))

                    tapOnLinkText(plainLinkText)
                    intended(matchesOpenLinkIntent(plainLinkUrl))
                }
        }
    }

    @Test
    fun shouldShowHtmlTextMessageWithTypefaceTags() {
        val incomingMessage =
            """<b>bold</b><br/><i>italic</i>\n<u>underlined</u>"""
        val formattedIncomingMessage = "bold\nitalic\nunderlined"
        configureWebServer {
            registerTechSupportChat()
            registerIncomingMessageWithHtmlTags(incomingMessage)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            incomingTextMessage(formattedIncomingMessage).waitUntil { listView.contains(this) }
            root.isViewStateMatches(
                "ChatMessagesTest/shouldShowHtmlTextMessageWithTypefaceTags/message"
            )
        }
    }

    @Test
    fun shouldShowPlainTextMessageWithLink() {
        val plainLinkUrl = "https://arenda.yandex.ru/"
        configureWebServer {
            registerTechSupportChat()
            registerIncomingMessageWithHtmlTags(plainLinkUrl)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesOpenLinkIntent(plainLinkUrl), null)

        onScreen<ChatMessagesScreen> {
            incomingTextMessage(plainLinkUrl)
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(
                        "ChatMessagesTest/shouldShowPlainTextMessageWithLink/message"
                    )
                }
                .textView
                .tapOnLinkText(plainLinkUrl)
            intended(matchesOpenLinkIntent(plainLinkUrl))
        }
    }

    @Test
    fun shouldSendChatOpened() {
        val dispatcher = DispatcherRegistry()
        dispatcher.registerTechSupportChat()
        dispatcher.registerEmptyListMessages()
        val expectedChatOpenedRequest = dispatcher.registerSendChatOpened(CHAT_ID)
        configureWebServer(dispatcher)

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            waitUntil { expectedChatOpenedRequest.isOccured() }
        }
    }

    private fun matchesOpenLinkIntent(url: String): Matcher<Intent> {
        return NamedIntentMatcher(
            "Открытие ссылки \"$url\"",
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(url)
            )
        )
    }

    private fun DispatcherRegistry.registerPresetsEnabledPage() {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[{
                          "id": "1",
                          "roomId": "$CHAT_ID",
                          "author": "techSupport",
                          "created": "${getValidStringDate()}",
                          "payload": {
                            "contentType": "TEXT_PLAIN",
                            "value": "Preset message"
                          },
                          "isSilent": true,
                          "properties": {
                            "techSupportFeedback": {
                              "hash": "1",
                              "presets": [
                                {
                                  "id": "all_good",
                                  "value": "All good"
                                },
                                {
                                  "id": "too_long",
                                  "value": "Too long"
                                }
                              ],
                              "ttl": "604800"
                            },
                            "type": "TECH_SUPPORT_FEEDBACK_REQUEST"
                          }
                        }]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerPollEnabledPage(pollId: String) {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[{
                          "id": "1",
                          "roomId": "$CHAT_ID",
                          "author": "techSupport",
                          "created": "${getValidStringDate()}",
                          "payload": {
                            "contentType": "TEXT_PLAIN",
                            "value": "This is poll"
                          },
                          "properties": {
                            "techSupportPoll": {
                              "hash": "$pollId",
                              "ttl": "604800"
                            },
                            "type": "TECH_SUPPORT_POLL"
                          }
                        }]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerPollNotEnabledPage() {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[{
                          "id": "1",
                          "roomId": "$CHAT_ID",
                          "author": "techSupport",
                          "created": "${getValidStringDate()}",
                          "payload": {
                            "contentType": "TEXT_PLAIN",
                            "value": "This is poll"
                          },
                          "properties": {
                            "techSupportPoll": {
                              "hash": "1",
                              "ttl": "1"
                            },
                            "type": "TECH_SUPPORT_POLL"
                          }
                        }]}}"""
                )
            }
        )
    }

    /**
     * пустой список региструруем 2 раза, т.к. может быть ситуация с 2 запросами подряд, когда
     * вызывается обновление из - за screenHolder и из - за getMessages при старте экрана
     */
    private fun DispatcherRegistry.registerEmptyListMessages() {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerSendPhotoMessage(localId: String) {
        register(
            request {
                path("2.0/chat/uploader/bootstrap/room/$CHAT_ID")
                queryParam("provided_id", localId)
            },
            response {
                setBody("""{"response":{"uploadImageUrl":"https://localhost:8080/upload"}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerUploadPhotoMessage(localId: String) {
        register(
            request {
                path("upload")
            },
            response {
                setBody(
                    """{"response":{
                            "id": "a",
                            "roomId": "$CHAT_ID",
                            "author": "$TEST_UID",
                            "created": "${getValidStringDate()}",
                            "payload": {},
                            "providedId": "$localId",
                            "properties": {},
                            "attachments": [{
                                "image": {
                                    "sizes": {
                                        "1200x1200":"https://localhost:8080/image/a"
                                    }
                                }
                            }]
                        }}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerSendTextMessage(localId: String, text: String) {
        register(
            request {
                method("POST")
                path("2.0/chat/messages")
            },
            response {
                setBody(
                    """{"response":{
                            "id": "a",
                            "roomId": "$CHAT_ID",
                            "author": "$TEST_UID",
                            "created": "${getValidStringDate()}",
                            "payload": {
                                  "contentType": "TEXT_HTML",
                                  "value": "$text"
                                },
                            "providedId": "$localId",
                            "properties": {}
                        }}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerSendPresetMessage(
        localId: String,
        preset: Pair<String?, String>
    ) {
        register(
            request {
                method("POST")
                path("2.0/chat/messages")
            },
            response {
                setBody(
                    """{"response":{
                            "id": "a",
                            "roomId": "$CHAT_ID",
                            "author": "$TEST_UID",
                            "created": "${getValidStringDate()}",
                            "payload": {
                                  "contentType": "TEXT_HTML",
                                  "value": "${preset.second}"
                                },
                            "providedId": "$localId"
                        }}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerSendFeedbackPresetMessage(
        localId: String,
        text: String,
        hash: String,
        presetId: String
    ) {
        register(
            request {
                method("POST")
                path("2.0/chat/messages")
            },
            response {
                setBody(
                    """{"response":{
                            "id": "a",
                            "roomId": "$CHAT_ID",
                            "author": "$TEST_UID",
                            "created": "${getValidStringDate()}",
                            "payload": {
                                  "contentType": "TEXT_HTML",
                                  "value": "$text"
                                },
                            "providedId": "$localId",
                            "properties": {
                                "type": "TECH_SUPPORT_FEEDBACK_RESPONSE",
                                "techSupportFeedback": {
                                    "hash": "$hash",
                                    "selectedPreset": "$presetId"
                                }
                            }
                        }}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerFirstPageMessages() {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[{
                                "id": "1",
                                "roomId": "$CHAT_ID",
                                "author": "techSupport",
                                "created": "2019-12-26T13:53:17.698Z",
                                "payload": {
                                  "contentType": "TEXT_HTML",
                                  "value": "not mine first"
                                },
                                "properties": {}}]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerSecondPageMessages() {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                queryParam("from", "1")
            },
            response {
                setBody(
                    """{"response":{"messages":[
                                {
                                "id": "2",
                                "roomId": "$CHAT_ID",
                                "author": "$TEST_UID",
                                "created": "2019-12-26T13:53:16.698Z",
                                "payload": {
                                "contentType": "TEXT_HTML",
                                "value": "mine second"
                                },"properties": {}},
                                {
                                "id": "1",
                                "roomId": "$CHAT_ID",
                                "author": "techSupport",
                                "created": "2019-12-26T13:53:17.698Z",
                                "payload": {
                                "contentType": "TEXT_HTML",
                                "value": "not mine first"
                                },"properties": {}}
                                ]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerPhotoMessage(url: String) {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[{
                                "id": "1",
                                "roomId": "$CHAT_ID",
                                "author": "techSupport",
                                "created": "2019-12-26T13:53:17.698Z",
                                "payload": {
                                  "contentType": "TEXT_HTML"
                                },
                                "attachments": [
                                    {
                                        "image": {
                                            "sizes": {
                                                "1200x1200": "$url"
                                            }
                                        }
                                    }
                                ],
                                "properties": {}}]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerTechSupportChat() {
        register(
            request {
                path("2.0/chat/room/$CHAT_ID")
            },
            response {
                setBody(
                    """
                        {
                          "response": {
                            "id": "$CHAT_ID",
                            "created": "2019-12-27T11:54:26.912Z",
                            "updated": "2020-12-16T18:24:08.842Z",
                            "userIds": [
                              "$TEST_UID",
                              "techSupport"
                            ],
                            "properties": {
                              "type": "techsupport"
                            },
                            "creator": "$TEST_UID",
                            "users": [
                              {
                                "id": "$TEST_UID",
                                "activeRoom": true,
                                "averageReplyDelayMinutes": 0,
                                "roomLastRead": "2020-12-29T08:07:30.081Z",
                                "profile": {
                                  "alias": "erik.burygin",
                                  "clientId": "$TEST_UID"
                                }
                              },
                              {
                                "id": "techSupport",
                                "activeRoom": true,
                                "averageReplyDelayMinutes": 0,
                                "roomLastRead": "2020-12-10T15:11:41.764Z",
                                "profile": {
                                  "alias": "Я.Недвижимость"
                                }
                              }
                            ],
                            "me": "$TEST_UID",
                            "subject": {
                              "title": "Чат с техподдержкой"
                            }
                          }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerIncomingMessageWithPresets(
        text: String,
        messageId: String,
        presets: List<Pair<String?, String>>
    ) {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[{
                                "id": "$messageId",
                                "roomId": "$CHAT_ID",
                                "author": "techSupport",
                                "created": "2019-12-26T13:53:17.698Z",
                                "payload": {
                                  "contentType": "TEXT_HTML",
                                  "value": "$text"
                                },
                                "properties": {
                                  "type": "ACTIONS",
                                  "keyboard": {
                                    "buttons": [
                                      ${
                    presets.joinToString { (id, text) ->
                        buildString {
                            append("{")
                            id?.let { append("\"id\":\"$it\",") }
                            append("\"value\":\"$text\"")
                            append("}")
                        }
                    }
                    }
                                    ]
                                  }
                                }}]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerIncomingMessageWithHtmlTags(text: String) {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{
                                "response":{
                                    "messages":[
                                        {
                                            "id": "1",
                                            "roomId": "$CHAT_ID",
                                            "author": "techSupport",
                                            "created": "2019-12-26T13:53:17.698Z",
                                            "payload": {
                                              "contentType": "TEXT_HTML",
                                              "value": "$text"
                                            }
                                        }
                                    ]
                                }
                             }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerPhoneConfirmationRequestMessage(
        text: String,
        action: String
    ) {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody(
                    """{"response":{"messages":[{
                                "id": "0",
                                "roomId": "$CHAT_ID",
                                "author": "techSupport",
                                "created": "2019-12-26T13:53:17.698Z",
                                "payload": {
                                  "contentType": "TEXT_HTML",
                                  "value": "$text"
                                },
                                "properties": {
                                  "type": "REALTY_DEVCHAT_PHONE_CONFIRMATION_REQUEST",
                                  "keyboard": {
                                    "buttons": [
                                      { "id": "1", "value": "$action" }
                                    ]
                                  }
                                }}]}}"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerPhoneConfirmationResponseMessage(
        text: String
    ): ExpectedRequest {
        return register(
            request {
                method("POST")
                path("2.0/chat/messages")
            },
            response {
                setBody(
                    """{
                                "response":{
                                    "id": "a",
                                    "roomId": "$CHAT_ID",
                                    "author": "$TEST_UID",
                                    "created": "${getValidStringDate()}",
                                    "payload": {
                                        "contentType": "TEXT_HTML",
                                        "value": "$text"
                                     },
                                    "providedId": "1",
                                    "properties": {
                                        "type": "REALTY_DEVCHAT_PHONE_CONFIRMED"
                                    }
                                }
                             }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerSendChatOpened(id: String): ExpectedRequest {
        return register(
            request {
                method("PUT")
                path("2.0/chat/room/$id/open")
            },
            success()
        )
    }

    private fun getValidStringDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        return formatter.format(Date(System.currentTimeMillis() - 100 * 1000L))
    }

    companion object {
        private const val CHAT_ID = "test_chat_id"
        private const val TEST_UID = 1
    }
}
