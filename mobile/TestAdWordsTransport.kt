package ru.yandex.market.di

import ru.yandex.market.analytics.adwords.AdWordsTransport

class TestAdWordsTransport : AdWordsTransport {

    override fun sendEvent(eventName: String, params: Map<String, *>) = Unit

}