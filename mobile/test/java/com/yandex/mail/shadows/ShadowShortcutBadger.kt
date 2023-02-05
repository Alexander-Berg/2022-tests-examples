package com.yandex.mail.shadows

import android.app.Notification
import android.content.Context
import com.yandex.mail.util.NonInstantiableException
import me.leolin.shortcutbadger.ShortcutBadgeException
import me.leolin.shortcutbadger.ShortcutBadger
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(ShortcutBadger::class)
class ShadowShortcutBadger private constructor() {

    init {
        throw NonInstantiableException()
    }

    companion object {

        var counter: Int = 0

        @JvmStatic
        @Throws(ShortcutBadgeException::class)
        @Implementation
        fun removeCountOrThrow(context: Context) {
            counter = 0
        }

        @JvmStatic
        @Throws(ShortcutBadgeException::class)
        @Implementation
        fun applyCountOrThrow(context: Context, badgeCount: Int) {
            counter = badgeCount
        }

        @JvmStatic
        @Throws(ShortcutBadgeException::class)
        @Implementation
        fun applyNotification(context: Context, notification: Notification, badgeCount: Int) {
            // no-op
        }
    }
}
