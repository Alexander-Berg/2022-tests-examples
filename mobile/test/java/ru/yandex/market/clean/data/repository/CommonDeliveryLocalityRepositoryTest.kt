package ru.yandex.market.clean.data.repository

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.junit.Test
import ru.beru.android.R
import ru.yandex.market.common.android.ResourcesManager

class CommonDeliveryLocalityRepositoryTest {

    private val resourcesDataStore = mock<ResourcesManager>()
    private val repository = CommonDeliveryLocalityRepository(resourcesDataStore)

    @Test
    fun `Do not throws exception when return moscow delivery availability`() {
        whenever(resourcesDataStore.getString(R.string.moscow_genitive)).thenReturn("Moscow")
        whenever(resourcesDataStore.getString(R.string.moscow_accusative)).thenReturn("Moscow")
        whenever(resourcesDataStore.getString(R.string.moscow_prepositional)).thenReturn("Moscow")
        whenever(resourcesDataStore.getString(R.string.russia)).thenReturn("Russia")
        whenever(resourcesDataStore.getString(R.string.moscow)).thenReturn("Moscow")
        whenever(resourcesDataStore.getString(R.string.moscow_region)).thenReturn("Moscow region")
        repository.moscowDeliveryAvailability
    }
}