package ru.yandex.yandexmaps.multiplatform.polling.internal.testimpl

import ru.yandex.yandexmaps.multiplatform.polling.internal.utils.PollingTimeProvider

internal class TestTimeProvider(override var currentTimeMillis: Long) : PollingTimeProvider
