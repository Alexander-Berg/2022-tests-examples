package com.yandex.mail.shadows

import android.appwidget.AppWidgetManager
import android.os.Bundle
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAppWidgetManager

@Implements(AppWidgetManager::class)
class MailShadowAppWidgetManager : ShadowAppWidgetManager() {

    @Implementation
    override fun getAppWidgetOptions(appWidgetId: Int): Bundle {
        return Bundle()
    }
}
