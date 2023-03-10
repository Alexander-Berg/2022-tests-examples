// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM component/unbind-card-component.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class UnbindCardComponent: MBTComponent {
    open override fun assertMatches(model: App, application: App): Unit {
        val appUnbindCard = UnbindCardFeature.`get`.forceCast(application)
        assertTrue(appUnbindCard.waitForUnbindCard(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT), "Unbind screen was not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} seconds")
        val modelScreenTitle = PaymentScreenTitleFeature.`get`.castIfSupported(model)
        val appScreenTitle = PaymentScreenTitleFeature.`get`.castIfSupported(application)
        if (modelScreenTitle != null && appScreenTitle != null) {
            val modelTitle = modelScreenTitle.getTitle()
            val appTitle = appScreenTitle.getTitle()
            assertStringEquals(modelTitle, appTitle, "Screen title mismatch")
        }
        val modelUnbindCard = UnbindCardFeature.`get`.castIfSupported(model)
        if (modelUnbindCard != null) {
            val modelCards = modelUnbindCard.getCards()
            val appCards = appUnbindCard.getCards()
            assertInt32Equals(modelCards.size, appCards.size, "Incorrect number of bound cards")
            for (modelCard in modelCards) {
                assertTrue(appCards.contains(modelCard), "Incorrect bound card")
            }
            val modelDoneButton = modelUnbindCard.isDoneButtonShown()
            val appDoneButton = appUnbindCard.isDoneButtonShown()
            assertBooleanEquals(modelDoneButton, appDoneButton, "Incorrect done button showing status")
        }
        val modelKeyboard = KeyboardFeature.`get`.castIfSupported(model)
        val appKeyboard = KeyboardFeature.`get`.castIfSupported(application)
        if (modelKeyboard != null && appKeyboard != null) {
            val modelNumKeyboardShown = modelKeyboard.isNumericKeyboardShown()
            val appNumKeyboardShown = appKeyboard.isNumericKeyboardShown()
            assertBooleanEquals(modelNumKeyboardShown, appNumKeyboardShown, "Numeric keyboard status is incorrect")
            val modelAlphKeyboardShown = modelKeyboard.isAlphabeticalKeyboardShown()
            val appAlphKeyboardShown = appKeyboard.isAlphabeticalKeyboardShown()
            assertBooleanEquals(modelAlphKeyboardShown, appAlphKeyboardShown, "Alphabetical keyboard status is incorrect")
        }
    }

    open override fun getComponentType(): MBTComponentType {
        return UnbindCardComponent.type
    }

    open override fun tostring(): String {
        return this.getComponentType()
    }

    companion object {
        @JvmStatic val type: String = "UnbindCardComponent"
    }
}

