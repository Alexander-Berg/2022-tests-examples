// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM model/keyboard-model.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public open class KeyboardModel: KeyboardProtocol {
    private var numericKeyboardShown: Boolean = false
    private var alphabeticalKeyboardShown: Boolean = false
    open fun setNumericKeyboardStatus(shown: Boolean): Unit {
        this.numericKeyboardShown = shown
    }

    open fun setAlphabeticalKeyboardStatus(shown: Boolean): Unit {
        this.numericKeyboardShown = shown
    }

    open override fun isNumericKeyboardShown(): Boolean {
        return this.numericKeyboardShown
    }

    open override fun isAlphabeticalKeyboardShown(): Boolean {
        return this.alphabeticalKeyboardShown
    }

    open override fun isKeyboardShown(): Boolean {
        return this.numericKeyboardShown || this.alphabeticalKeyboardShown
    }

    open override fun minimizeKeyboard(): Unit {
        this.alphabeticalKeyboardShown = false
        this.numericKeyboardShown = false
    }

}

