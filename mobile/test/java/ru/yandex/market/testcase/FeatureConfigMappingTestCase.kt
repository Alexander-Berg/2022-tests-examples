package ru.yandex.market.testcase

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.common.featureconfigs.managers.base.AbstractFeatureConfigManager
import ru.yandex.market.common.featureconfigs.repository.FeatureConfigSnapshot
import ru.yandex.market.common.featureconfigs.repository.FeatureConfigSource
import ru.yandex.market.common.featureconfigs.repository.FeatureConfigRepository
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.gson.GsonFactory

abstract class FeatureConfigMappingTestCase<T> {

    protected abstract val json: String?

    protected abstract val config: T

    private val repository = mock<FeatureConfigRepository>()

    private val gson = GsonFactory.get()

    private val scheduler = WorkerScheduler(Schedulers.trampoline())


    protected abstract fun createManager(
        dependencies: AbstractFeatureConfigManager.Dependencies
    ): AbstractFeatureConfigManager<T>

    @Test
    fun testFeatureConfig() {
        val dependencies = AbstractFeatureConfigManager.Dependencies(
            featureConfigRepository = repository,
            gson = gson,
            workerScheduler = scheduler
        )
        val manager = createManager(dependencies)
        whenever(
            repository.fetch(
                key = manager.key,
                allowOverride = true
            )
        ) doReturn FeatureConfigSnapshot(source = FeatureConfigSource.FIREBASE, value = json)

        val expected = config
        val actual = manager.get()

        assertThat(actual).isEqualTo(expected)
    }

}