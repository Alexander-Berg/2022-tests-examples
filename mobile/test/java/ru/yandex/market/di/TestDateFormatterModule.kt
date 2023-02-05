package ru.yandex.market.di

import dagger.Module
import dagger.Provides
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.datetime.DateTimeProvider
import javax.inject.Singleton

@Module
object TestDateFormatterModule {
    @Provides
    @Singleton
    fun provideDateFormatter(resourceManager: ResourcesManager, dateTimeProvider: DateTimeProvider): DateFormatter {
        return DateFormatter(resourceManager, dateTimeProvider)
    }
}