package ru.auto.ara.core.feature.calls

import ru.auto.data.repository.push_token.IPushTokenRepository
import ru.auto.data.repository.push_token.TokenForPushes
import rx.Single

const val TEST_FIREBASE_TOKEN = "TEST_FIREBASE_TOKEN"

class TestPushTokenRepository : IPushTokenRepository {
    override fun requestToken(): Single<TokenForPushes> = Single.just(TokenForPushes.Firebase(TEST_FIREBASE_TOKEN))
}
