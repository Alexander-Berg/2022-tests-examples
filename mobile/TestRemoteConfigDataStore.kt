package ru.yandex.market.common.featureconfigs.datastore

import androidx.annotation.CheckResult
import io.reactivex.Completable
import io.reactivex.Single

internal class TestRemoteConfigDataStore : RemoteConfigDataStore {

    @CheckResult
    override fun setup(): Completable {
        return Completable.complete()
    }

    @CheckResult
    override fun fetch(): Completable {
        return Completable.complete()
    }

    @CheckResult
    override fun fetchIsSuccessful(): Single<Boolean> {
        return Single.just(true)
    }

    override fun getRemoteConfig(key: String): String? {
        return null
    }

    override fun getCachedConfig(key: String): String? {
        return null
    }

    override fun getAllRemoteConfigs(): Map<String, String> {
        return mapOf()
    }

}