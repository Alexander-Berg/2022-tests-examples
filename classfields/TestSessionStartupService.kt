package com.yandex.mobile.realty.data.repository

import com.yandex.mobile.realty.domain.startup.repository.SessionStartupRepository
import rx.Single
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * @author rogovalex on 30.07.2021.
 */
@Singleton
class TestSessionStartupService @Inject constructor(
    private val provider: Provider<SessionStartupService>
) : SessionStartupRepository {

    var emulateStarted = true
    private val sessionStarted: SessionStartupRepository by lazy {
        SessionStartupRepository { Single.just(true) }
    }
    private val impl: SessionStartupRepository
        get() = if (emulateStarted) {
            sessionStarted
        } else {
            provider.get()
        }

    override fun getSessionStartup(): Single<Boolean> {
        return impl.getSessionStartup()
    }
}
