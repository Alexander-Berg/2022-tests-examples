package ru.yandex.market.checkout.data.store

import com.annimon.stream.Optional
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.base.redux.store.LegacyCompatibleAppStateStore
import ru.yandex.market.base.redux.store.configureStore
import ru.yandex.market.checkout.domain.model.presetDeliveryAvailabilityStatusTestInstance
import ru.yandex.market.clean.domain.model.pickup.favoritePickupTestInstance
import ru.yandex.market.clean.domain.model.usercontact.userContactTestInstance
import ru.yandex.market.data.order.options.deliveryOptionTestInstance
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.redux.reducers.AppReducer
import ru.yandex.market.redux.states.AppState

class CheckoutGeneralInfoDataStoreTest {

    private val reduxStore = LegacyCompatibleAppStateStore(
        appStateStore = configureStore(AppState()) { reducer = AppReducer() },
        reduxCommonHealthAnalytics = mock()
    )

    private lateinit var generalInfoDataStore: CheckoutGeneralInfoDataStore

    @Before
    fun setUp() {
        generalInfoDataStore = CheckoutGeneralInfoDataStore(reduxStore)
    }

    @Test
    fun `Can observe and update delivery availability statuses`() {
        val updatedStatuses = listOf(COURIER_STATUS, PICKUP_STATUS)

        val availabilityStatusesObserver = generalInfoDataStore.observeDeliveryAvailabilityStatuses().test()
        val updateStatusObserver = generalInfoDataStore.setDeliveryAvailabilityStatuses(updatedStatuses).test()

        updateStatusObserver
            .assertNoErrors()
            .assertComplete()

        availabilityStatusesObserver
            .assertValueCount(2)
            .assertValueAt(0, Optional.empty())
            .assertValueAt(1, Optional.of(updatedStatuses))
    }

    @Test
    fun `Can observe and update user addresses`() {
        val userAddresses = listOf(
            userAddressTestInstance(regionId = 1L, serverId = "1"),
            userAddressTestInstance(regionId = 2L, serverId = "2")
        )

        val userAddressesObserver = generalInfoDataStore.observeUserAddresses().test()
        val updateAddressesObserver = generalInfoDataStore.setUserAddresses(userAddresses).test()

        updateAddressesObserver
            .assertNoErrors()
            .assertComplete()

        userAddressesObserver
            .assertValueCount(2)
            .assertValueAt(0, Optional.empty())
            .assertValueAt(1, Optional.of(userAddresses))
    }

    @Test
    fun `Can observe and update user contacts`() {
        val updatedContacts = listOf(
            userContactTestInstance(id = "1"),
            userContactTestInstance(id = "2")
        )

        val userContactsObserver = generalInfoDataStore.observeUserContacts().test()
        val updateContactsObserver = generalInfoDataStore.setUserContacts(updatedContacts).test()

        updateContactsObserver
            .assertNoErrors()
            .assertComplete()

        userContactsObserver
            .assertValueCount(2)
            .assertValueAt(0, Optional.empty())
            .assertValueAt(1, Optional.of(updatedContacts))
    }

    @Test
    fun `Can observe and update favorite pickups`() {
        val updatedPickups = listOf(
            favoritePickupTestInstance(id = "1"),
            favoritePickupTestInstance(id = "2")
        )

        val favoritePickupsObserver = generalInfoDataStore.observeUserFavoritePickups().test()
        val updateFavoritePickupsObserver = generalInfoDataStore.setUserFavoritePickups(updatedPickups).test()

        updateFavoritePickupsObserver
            .assertNoErrors()
            .assertComplete()

        favoritePickupsObserver
            .assertValueCount(2)
            .assertValueAt(0, Optional.empty())
            .assertValueAt(1, Optional.of(updatedPickups))
    }

    @Test
    fun `Can get and update delivary options`() {
        val updatedDeliveryOptions = mapOf(
            "1" to listOf(
                deliveryOptionTestInstance(id = "1"),
                deliveryOptionTestInstance(id = "2"),
            ),
            "2" to listOf(
                deliveryOptionTestInstance(id = "3"),
                deliveryOptionTestInstance(id = "4"),
            )
        )

        val updateDeliveryOptionsObserver = generalInfoDataStore.setDeliveryOptions(updatedDeliveryOptions).test()
        val deliveryOptionsObserver = generalInfoDataStore.getDeliveryOptions().test()

        updateDeliveryOptionsObserver
            .assertNoErrors()
            .assertComplete()

        deliveryOptionsObserver
            .assertValueCount(1)
            .assertValueAt(0, updatedDeliveryOptions)
    }

    @Test
    fun `Can observe and update prioritized split ids`() {
        val prioritizedSplitIds = setOf("1", "2", "3")

        val prioritizedSplitIdsObserver = generalInfoDataStore.getPrioritizedSplitIdsStream().test()
        val updatePrioritizedSplitIdsObserver = generalInfoDataStore.setPrioritizedSplitIds(prioritizedSplitIds)
            .test()

        updatePrioritizedSplitIdsObserver
            .assertNoErrors()
            .assertComplete()

        prioritizedSplitIdsObserver
            .assertValueCount(2)
            .assertValueAt(0, Optional.empty())
            .assertValueAt(1, Optional.of(prioritizedSplitIds))
    }

    private companion object {

        private val COURIER_STATUS = presetDeliveryAvailabilityStatusTestInstance(
            presetId = "1",
            deliveryType = DeliveryType.DELIVERY
        )

        private val PICKUP_STATUS = presetDeliveryAvailabilityStatusTestInstance(
            presetId = "2",
            deliveryType = DeliveryType.PICKUP
        )
    }
}
