package ru.yandex.yandexnavi.guidance_lib_test_app

import android.content.Context
import com.yandex.navikit.notifications.NotificationData
import com.yandex.navikit.notifications.NotificationFreedriveDataProvider
import ru.yandex.yandexnavi.ui.util.extensions.resourceId

class NotificationFreedriveDataProviderImpl(
    private val context: Context
) : NotificationFreedriveDataProvider {
    override fun provideNotificationData(): NotificationData {
        return NotificationData(
            context.resourceId(R.drawable.bg_freedrive_notification_running),
            "Freedrive in guidance-lib-testapp",
            "Some interesting description",
            true,
            null,
            null,
            null,
            false
        )
    }
}
