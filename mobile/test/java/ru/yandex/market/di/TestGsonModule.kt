package ru.yandex.market.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import ru.yandex.market.gson.GsonFactory

@Module
class TestGsonModule {

    @Provides
    fun gson(): Gson {
        return GsonFactory.get()
    }

    @Provides
    fun gsonBuilder(): GsonBuilder {
        return GsonFactory.defaultBuilder()
    }
}