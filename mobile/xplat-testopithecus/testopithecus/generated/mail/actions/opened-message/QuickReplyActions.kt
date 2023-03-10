// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/opened-message/quick-reply-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class QuickReplyTapOnTextFieldAction(): BaseSimpleAction<QuickReply, MBTComponent>(QuickReplyTapOnTextFieldAction.type) {
    open override fun requiredFeature(): Feature<QuickReply> {
        return QuickReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: QuickReply): Boolean {
        return model.isQuickReplyShown()
    }

    open override fun performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.tapOnTextField()
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "QuickReplyTapOnTextFieldAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "QuickReplyTapOnTextFieldAction"
    }
}

public open class QuickReplySetTextFieldAction(private val text: String, unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE): BaseSimpleAction<QuickReply, MBTComponent>(QuickReplySetTextFieldAction.type) {
    open override fun requiredFeature(): Feature<QuickReply> {
        return QuickReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: QuickReply): Boolean {
        return model.isQuickReplyShown()
    }

    open override fun performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.setTextFieldValue(this.text)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "QuickReplySetTextFieldAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "QuickReplySetTextFieldAction"
    }
}

public open class QuickReplyPasteToTextFieldAction(private val text: String, unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE): BaseSimpleAction<QuickReply, MBTComponent>(QuickReplyPasteToTextFieldAction.type) {
    open override fun requiredFeature(): Feature<QuickReply> {
        return QuickReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: QuickReply): Boolean {
        return model.isQuickReplyShown()
    }

    open override fun performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.pasteTextFieldValue(this.text)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "QuickReplyPasteToTextFieldAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "QuickReplyPasteToTextFieldAction"
    }
}

public open class QuickReplyTapOnComposeButtonAction(): BaseSimpleAction<QuickReply, MBTComponent>(QuickReplyTapOnComposeButtonAction.type) {
    open override fun requiredFeature(): Feature<QuickReply> {
        return QuickReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: QuickReply): Boolean {
        return model.isQuickReplyShown()
    }

    open override fun performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.tapOnComposeButton()
        return ComposeComponent()
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "QuickReplyTapOnComposeButtonAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "QuickReplyTapOnComposeButtonAction"
    }
}

public open class QuickReplyTapOnSendButtonAction(): BaseSimpleAction<QuickReply, MBTComponent>(QuickReplyTapOnSendButtonAction.type) {
    open override fun requiredFeature(): Feature<QuickReply> {
        return QuickReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: QuickReply): Boolean {
        val isSendButtonEnabled = model.isSendButtonEnabled()
        return model.isQuickReplyShown() && isSendButtonEnabled
    }

    open override fun performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.tapOnSendButton()
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "QuickReplyTapOnSendButtonAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "QuickReplyTapOnSendButtonAction"
    }
}

public open class SmartReplyTapOnSmartReplyAction(private val order: Int, unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE): BaseSimpleAction<SmartReply, MBTComponent>(SmartReplyTapOnSmartReplyAction.type) {
    open override fun requiredFeature(): Feature<SmartReply> {
        return SmartReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: SmartReply): Boolean {
        val smartRepliesCount = model.getSmartReplies().size
        return model.isSmartRepliesShown() && this.order < smartRepliesCount
    }

    open override fun performImpl(modelOrApplication: SmartReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.tapOnSmartReply(this.order)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "SmartReplyTapOnSmartReplyAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "SmartReplyTapOnSmartReplyAction"
    }
}

public open class SmartReplyCloseSmartReplyAction(private val order: Int, unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE): BaseSimpleAction<SmartReply, MBTComponent>(SmartReplyCloseSmartReplyAction.type) {
    open override fun requiredFeature(): Feature<SmartReply> {
        return SmartReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: SmartReply): Boolean {
        return model.isSmartRepliesShown()
    }

    open override fun performImpl(modelOrApplication: SmartReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.closeSmartReply(this.order)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "SmartReplyCloseSmartReplyAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "SmartReplyCloseSmartReplyAction"
    }
}

public open class SmartReplyCloseAllSmartRepliesAction(): BaseSimpleAction<SmartReply, MBTComponent>(SmartReplyCloseAllSmartRepliesAction.type) {
    open override fun requiredFeature(): Feature<SmartReply> {
        return SmartReplyFeature.`get`
    }

    open override fun canBePerformedImpl(model: SmartReply): Boolean {
        return model.isSmartRepliesShown()
    }

    open override fun performImpl(modelOrApplication: SmartReply, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.closeAllSmartReplies()
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "SmartReplyCloseAllSmartRepliesAction"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "SmartReplyCloseAllSmartRepliesAction"
    }
}

