// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/settings/pin-model.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class PinModel(): Pin {
    private var pin: String = ""
    open override fun changePassword(newPassword: String): Unit {
        this.pin = newPassword
    }

    open override fun resetPassword(): Unit {
        this.pin = ""
    }

    open override fun enterPassword(password: String): Unit {
    }

    open override fun isLoginUsingPasswordEnabled(): Boolean {
        return this.pin != ""
    }

    open override fun turnOffLoginUsingPassword(): Unit {
        this.pin = ""
    }

    open override fun turnOnLoginUsingPassword(password: String): Unit {
        this.pin = password
    }

    open override fun waitForPinToTrigger(): Unit {
        return
    }

}

