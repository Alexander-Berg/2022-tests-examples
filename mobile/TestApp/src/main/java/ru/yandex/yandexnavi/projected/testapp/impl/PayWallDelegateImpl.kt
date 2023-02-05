package ru.yandex.yandexnavi.projected.testapp.impl

import android.content.Intent
import ru.yandex.yandexnavi.projected.platformkit.dependencies.paywall.PayWallDelegate

class PayWallDelegateImpl(
    override val intentToOpen: Intent
) : PayWallDelegate {

    override var listener: PayWallDelegate.Listener? = null

    override val hasPlus: Boolean
        get() = true

    override val isPlusAvailable: Boolean
        get() = true
}
