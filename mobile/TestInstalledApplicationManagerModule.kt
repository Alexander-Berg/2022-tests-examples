package ru.yandex.market.di.module

import android.content.Context
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.di.module.feature.InstalledApplicationManagerModule
import ru.yandex.market.mocks.StateFacade
import ru.yandex.market.mocks.MockInstalledApplicationManager
import ru.yandex.market.mocks.local.mapper.InstalledApplicationManagerMapper
import ru.yandex.market.util.manager.InstalledApplicationManager
import javax.inject.Inject

class TestInstalledApplicationManagerModule @Inject constructor(
    private val stateFacade: StateFacade,
    private val mapper: InstalledApplicationManagerMapper,
) : InstalledApplicationManagerModule() {

    override fun provideInstalledApplicationManagerModule(
        context: Context,
        resourcesManager: ResourcesManager
    ): InstalledApplicationManager {
        return MockInstalledApplicationManager(stateFacade,mapper)
    }
}