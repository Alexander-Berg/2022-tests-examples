package ru.yandex.market.di

import dagger.Module
import dagger.Provides
import ru.yandex.market.UnitTestDateTimeProvider
import ru.yandex.market.datetime.DateTimeProvider
import javax.inject.Singleton

@Module
object TestDateTimeModule {

    @Provides
    @Singleton
    fun provideDateTimeModule(): DateTimeProvider {
        return UnitTestDateTimeProvider()
    }

}