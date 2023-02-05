package ru.yandex.market.common.featureconfigs.di

import ru.yandex.market.common.featureconfigs.datastore.FapiRemoteConfigDataStore
import ru.yandex.market.common.featureconfigs.datastore.RemoteConfigDataStore
import ru.yandex.market.common.featureconfigs.datastore.TestRemoteConfigDataStore
import ru.yandex.market.common.featureconfigs.provider.ConfigDataStoreProvider
import toxin.definition.DefinitionRegistrar

fun DefinitionRegistrar.overrideFeatureConfigsModule() {

    singleton<RemoteConfigDataStore>(allowOverride = true) {
        TestRemoteConfigDataStore()
    }

    singleton<ConfigDataStoreProvider>(allowOverride = true) {
        val productionRemoteConfigDataStore = get<RemoteConfigDataStore>()
        val fapiRemoteConfigDataStore = getLazy<FapiRemoteConfigDataStore>()
        ConfigDataStoreProvider(
            productionRemoteConfigDataStore = productionRemoteConfigDataStore,
            fapiRemoteConfigDataStore = fapiRemoteConfigDataStore
        )
    }

}
