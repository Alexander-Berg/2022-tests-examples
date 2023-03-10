// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM action/new-card-actions.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class FillNewCardDataAction(private val card: BoundCard, private val save: Boolean): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return FillNewCardFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        return true
    }

    private fun tapAndSetValue(model: FillNewCard, application: FillNewCard, `field`: NewCardField, value: String): Unit {
        model.tapOnField(`field`)
        application.tapOnField(`field`)
        model.setFieldValue(`field`, value)
        application.setFieldValue(`field`, value)
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        val modelFillNewCard = FillNewCardFeature.`get`.forceCast(model)
        val appFillNewCard = FillNewCardFeature.`get`.forceCast(application)
        val expirationDate = "${this.card.expirationMonth}${this.card.expirationYear}"
        this.tapAndSetValue(modelFillNewCard, appFillNewCard, NewCardField.cardNumber, this.card.cardNumber)
        this.tapAndSetValue(modelFillNewCard, appFillNewCard, NewCardField.expirationDate, expirationDate)
        this.tapAndSetValue(modelFillNewCard, appFillNewCard, NewCardField.cvv, this.card.cvv)
        val modelReadPaymentDetails = ReadPaymentDetailsFeature.`get`.forceCast(model)
        if (modelReadPaymentDetails.getAuthorizationMode() == AuthorizationMode.authorized && modelFillNewCard.getNewCardMode() == NewCardMode.pay) {
            modelFillNewCard.setSaveCardCheckboxEnabled(this.save)
            appFillNewCard.setSaveCardCheckboxEnabled(this.save)
        }
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "FillNewCardDataAction"
    }

    open override fun getActionType(): String {
        return FillNewCardDataAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "FillNewCardDataAction"
    }
}

public open class TapOnNewCardFieldAction(private val `field`: NewCardField, unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE): BaseSimpleAction<FillNewCard, MBTComponent>(TapOnNewCardFieldAction.type) {
    open override fun requiredFeature(): Feature<FillNewCard> {
        return FillNewCardFeature.`get`
    }

    open override fun performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.tapOnField(this.`field`)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TapOnNewCardFieldAction"
    }
}

public open class FillNewCardFieldAction(private val `field`: NewCardField, private val value: String): BaseSimpleAction<FillNewCard, MBTComponent>(FillNewCardFieldAction.type) {
    open override fun requiredFeature(): Feature<FillNewCard> {
        return FillNewCardFeature.`get`
    }

    open override fun performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.setFieldValue(this.`field`, this.value)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "FillNewCardFieldAction"
    }
}

public open class TapAndFillNewCardFieldAction(private val `field`: NewCardField, private val value: String): BaseSimpleAction<FillNewCard, MBTComponent>(TapAndFillNewCardFieldAction.type) {
    open override fun requiredFeature(): Feature<FillNewCard> {
        return FillNewCardFeature.`get`
    }

    open override fun performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.tapOnField(this.`field`)
        modelOrApplication.setFieldValue(this.`field`, this.value)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TapAndFillNewCardFieldAction"
    }
}

public open class TapAndPasteNewCardFieldAction(private val `field`: NewCardField, private val value: String): BaseSimpleAction<FillNewCard, MBTComponent>(TapAndPasteNewCardFieldAction.type) {
    open override fun requiredFeature(): Feature<FillNewCard> {
        return FillNewCardFeature.`get`
    }

    open override fun performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.tapOnField(this.`field`)
        modelOrApplication.pasteFieldValue(this.`field`, this.value)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TapAndPasteNewCardFieldAction"
    }
}

public open class PasteNewCardFieldAction(private val `field`: NewCardField, private val value: String): BaseSimpleAction<FillNewCard, MBTComponent>(PasteNewCardFieldAction.type) {
    open override fun requiredFeature(): Feature<FillNewCard> {
        return FillNewCardFeature.`get`
    }

    open override fun performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.pasteFieldValue(this.`field`, this.value)
        return currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "PasteNewCardFieldAction"
    }
}

public open class TapOnNewCardBackButtonAction: MBTAction {
    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        FillNewCardFeature.`get`.forceCast(model).tapOnBackButton()
        FillNewCardFeature.`get`.forceCast(application).tapOnBackButton()
        return requireNonNull(history.previousDifferentComponent, "There is no previous screen")
    }

    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return FillNewCardFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        return true
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return this.getActionType()
    }

    open override fun getActionType(): MBTActionType {
        return TapOnNewCardBackButtonAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TapOnNewCardBackButtonAction"
    }
}

