package ru.yandex.market.test.util

import ru.yandex.market.Dependencies
import ru.yandex.market.domain.auth.model.MarketUid
import ru.yandex.market.domain.auth.model.Uuid
import ru.yandex.market.domain.auth.model.YandexUid

object IdentifierHelper {

    fun checkUuid(uuid: Uuid?) {
        val persistedUuid = Dependencies.getIdentifierRepository().getUuid()
        if (persistedUuid != uuid) {
            throw RuntimeException("Expected: $uuid, but actual: $persistedUuid")
        }
    }

    fun checkMarketUid(muid: MarketUid?) {
        val persistedMuid = Dependencies.getIdentifierRepository().getMarketUid()
        if (persistedMuid != muid) {
            throw RuntimeException("Expected: $muid, but actual: $persistedMuid")
        }
    }

    fun checkYandexUid(yuid: YandexUid?) {
        val persistedYuid = Dependencies.getIdentifierRepository().getYandexUid()
        if (persistedYuid != yuid) {
            throw RuntimeException("Expected: $yuid, but actual: $persistedYuid")
        }
    }

}
