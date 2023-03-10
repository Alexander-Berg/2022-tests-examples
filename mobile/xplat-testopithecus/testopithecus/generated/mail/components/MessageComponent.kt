// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/message-component.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class MessageComponent: MBTComponent {
    open override fun getComponentType(): String {
        return MessageComponent.type
    }

    open override fun assertMatches(model: App, application: App): Unit {
        val messageNavigatorModel = MessageViewerFeature.`get`.castIfSupported(model)
        val messageNavigatorApp = MessageViewerFeature.`get`.castIfSupported(application)
        val androidMessageNavigatorModel = MessageViewerAndroidFeature.`get`.castIfSupported(model)
        val androidMessageNavigatorApp = MessageViewerAndroidFeature.`get`.castIfSupported(application)
        if (messageNavigatorModel != null && messageNavigatorApp != null) {
            val openedMessageInModel = messageNavigatorModel.getOpenedMessage()
            val openedMessageInApp = messageNavigatorApp.getOpenedMessage()
            assertTrue(FullMessage.matches(openedMessageInModel, openedMessageInApp), "Opened messages are different, model: ${openedMessageInModel.tostring()}, actual: ${openedMessageInApp.tostring()}")
            val messageLabelsInModel = messageNavigatorModel.getLabels()
            val messageLabelsInApp = messageNavigatorApp.getLabels()
            for (label in messageLabelsInModel.values()) {
                assertBooleanEquals(true, messageLabelsInApp.has(label), "Missing label: ${label}. Model: ${messageLabelsInModel.values()}. App: ${messageLabelsInApp.values()}")
            }
            assertTrue(messageLabelsInModel.size == messageLabelsInApp.size, "Labels are different. Model: ${messageLabelsInModel.values()}. App: ${messageLabelsInApp.values()}")
        }
        val modelTranslatorBar = TranslatorBarFeature.`get`.castIfSupported(model)
        val appTranslatorBar = TranslatorBarFeature.`get`.castIfSupported(application)
        if (modelTranslatorBar != null && appTranslatorBar != null) {
            val modelTranslatorBarShown = modelTranslatorBar.isTranslatorBarShown()
            val appTranslatorBarShown = appTranslatorBar.isTranslatorBarShown()
            assertBooleanEquals(modelTranslatorBarShown, appTranslatorBarShown, "Translator bar show status is incorrect")
            if (appTranslatorBarShown) {
                var modelTargetLanguage = modelTranslatorBar.getTargetLanguage().toLowerCase()
                val appTargetLanguage = appTranslatorBar.getTargetLanguage().toLowerCase()
                var modelSourceLanguage = modelTranslatorBar.getSourceLanguage().toLowerCase()
                val appSourceLanguage = appTranslatorBar.getSourceLanguage().toLowerCase()
                if (androidMessageNavigatorApp != null && androidMessageNavigatorModel != null) {
                    val languageMessage = androidMessageNavigatorModel.getDefaultSourceLanguage().toLowerCase()
                    modelTargetLanguage = this.setLanguageInModelIfAndroid(modelTargetLanguage, appTargetLanguage, languageMessage)
                    modelSourceLanguage = this.setLanguageInModelIfAndroid(modelSourceLanguage, appSourceLanguage, languageMessage)
                    if (appSourceLanguage != languageMessage) {
                        modelSourceLanguage = appSourceLanguage
                    } else if (modelSourceLanguage == modelTargetLanguage) {
                        modelSourceLanguage = TranslatorLanguageName.select
                    }
                }
                assertStringEquals(modelTargetLanguage, appTargetLanguage, "Translator bar source language is incorrect")
                assertStringEquals(modelSourceLanguage, appSourceLanguage, "Translator bar source language is incorrect")
                val modelSubmitButtonLabel = modelTranslatorBar.getSubmitButtonLabel()
                val appSubmitButtonLabel = appTranslatorBar.getSubmitButtonLabel()
                assertStringEquals(modelSubmitButtonLabel, appSubmitButtonLabel, "Submit button label is incorrect")
            }
        }
        val modelQuickReply = QuickReplyFeature.`get`.castIfSupported(model)
        val appQuickReply = QuickReplyFeature.`get`.castIfSupported(application)
        if (modelQuickReply != null && appQuickReply != null) {
            val modelQuickReplyShown = modelQuickReply.isQuickReplyShown()
            val appQuickReplyShown = appQuickReply.isQuickReplyShown()
            assertBooleanEquals(modelQuickReplyShown, appQuickReplyShown, "Quick reply show status is incorrect")
            if (appQuickReplyShown) {
                val modelTextFieldValue = modelQuickReply.getTextFieldValue()
                val appTextFieldValue = appQuickReply.getTextFieldValue()
                assertStringEquals(modelTextFieldValue, appTextFieldValue, "Quick reply text field value is incorrect")
                if (modelTextFieldValue != "") {
                    val modelQuickReplyTextFieldExpanded = modelQuickReply.isQuickReplyTextFieldExpanded()
                    val appQuickReplyTextFieldExpanded = appQuickReply.isQuickReplyTextFieldExpanded()
                    assertBooleanEquals(modelQuickReplyTextFieldExpanded, appQuickReplyTextFieldExpanded, "Quick reply text field expand status is incorrect")
                }
                val modelSendButtonEnabled = modelQuickReply.isSendButtonEnabled()
                val appSendButtonEnabled = appQuickReply.isSendButtonEnabled()
                assertBooleanEquals(modelSendButtonEnabled, appSendButtonEnabled, "Send button enable status is incorrect")
                val modelSmartReply = SmartReplyFeature.`get`.castIfSupported(model)
                val appSmartReply = SmartReplyFeature.`get`.castIfSupported(application)
                if (modelSmartReply != null && appSmartReply != null) {
                    val modelSmartRepliesShown = modelSmartReply.isSmartRepliesShown()
                    val appSmartRepliesShown = appSmartReply.isSmartRepliesShown()
                    assertBooleanEquals(modelSmartRepliesShown, appSmartRepliesShown, "Smart replies shown status is incorrect")
                    if (modelSmartRepliesShown) {
                        val modelSmartReplies = modelSmartReply.getSmartReplies()
                        val appSmartReplies = appSmartReply.getSmartReplies()
                        assertInt32Equals(modelSmartReplies.size, appSmartReplies.size, "Incorrect number of smart replies")
                        for (modelSmartReply in modelSmartReplies) {
                            assertTrue(appSmartReplies.contains(modelSmartReply), "There is no smart reply with label ${modelSmartReply}")
                        }
                    }
                }
            }
        }
        TabBarComponent().assertMatches(model, application)
    }

    open override fun tostring(): String {
        return this.getComponentType()
    }

    private fun setLanguageInModelIfAndroid(modelLanguage: LanguageName, appLanguage: LanguageName, messageLanguage: LanguageName): LanguageName {
        return if (modelLanguage == "auto" && appLanguage != "auto") messageLanguage else modelLanguage
    }

    companion object {
        @JvmStatic val type: String = "MessageComponent"
    }
}

public open class AllMessageActions: MBTComponentActions {
    open override fun getActions(_model: App): YSArray<MBTAction> {
        val actions: YSArray<MBTAction> = mutableListOf()
        actions.add(MessageViewBackToMailListAction())
        return actions
    }

}

