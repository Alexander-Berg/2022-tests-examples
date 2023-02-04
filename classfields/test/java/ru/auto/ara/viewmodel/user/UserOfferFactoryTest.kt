package ru.auto.ara.viewmodel.user

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.R
import ru.auto.ara.util.Clock
import ru.auto.ara.util.android.StringsProvider
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.ActiveService
import ru.auto.data.model.data.offer.AdditionalInfo
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.util.VAS_ALIAS_ALL_SALE_ACTIVE
import ru.auto.data.util.or0
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class UserOfferFactoryTest {

    @Test
    fun `should create correct countdown labels`() {
        val stringsProvider: StringsProvider = mock()
        stringsProvider.mockGet(R.string.till_prolongation) { arg -> "До продления %s".format(arg) }
        stringsProvider.mockGet(R.string.till_deactivation) { arg -> "До снятия %s".format(arg) }
        stringsProvider.mockGet(R.string.vas_days_till_prolong) { arg -> "%d д. до продления".format(arg) }
        stringsProvider.mockGet(R.string.vas_days_till_end) { arg -> "%d д. до снятия".format(arg) }
        val currentDate = Clock.now()

        fun offer(days: Long? = null, hours: Long? = null, minutes: Long? = null, isProlonged: Boolean) = Offer(
            id = "1",
            category = VehicleCategory.CARS,
            sellerType = SellerType.PRIVATE,
            additional = AdditionalInfo(
                originalId = "1",
                expireDate = currentDate.time
                    + days?.let { TimeUnit.DAYS.toMillis(it) }.or0()
                    + hours?.let { TimeUnit.HOURS.toMillis(it) }.or0()
                    + minutes?.let { TimeUnit.MINUTES.toMillis(it) }.or0()
            ),
            services = listOf(
                ActiveService(
                    service = VAS_ALIAS_ALL_SALE_ACTIVE,
                    isProlonged = isProlonged,
                    proposeProlongation = false
                )
            )
        )

        var offer = offer(days = 3, isProlonged = false)
        var countdown = UserOfferFactory.createCountdown(stringsProvider, offer, currentDate)
        assertEquals(expected = "3 д. до снятия", actual = countdown?.text)

        offer = offer(hours = 14, minutes = 32, isProlonged = false)
        countdown = UserOfferFactory.createCountdown(stringsProvider, offer, currentDate)
        assertEquals(expected = "До снятия 14:32", actual = countdown?.text)

        offer = offer(hours = -14, isProlonged = false)
        countdown = UserOfferFactory.createCountdown(stringsProvider, offer, currentDate)
        assertEquals(expected = "До снятия 00:00", actual = countdown?.text)

        offer = offer(days = 1, isProlonged = true)
        countdown = UserOfferFactory.createCountdown(stringsProvider, offer, currentDate)
        assertEquals(expected = "1 д. до продления", actual = countdown?.text)

        offer = offer(hours = 4, minutes = 5, isProlonged = true)
        countdown = UserOfferFactory.createCountdown(stringsProvider, offer, currentDate)
        assertEquals(expected = "До продления 04:05", actual = countdown?.text)
    }

    /**
     * Stubs call to [StringsProvider.get] with arguments: [resId] and single placeholder that will be passed to
     * [mockResult] lambda to form final result.
     */
    private inline fun StringsProvider.mockGet(resId: Int, crossinline mockResult: (arg: Any) -> String) {
        whenever(get(eq(resId), any())).thenAnswer { invocation ->
            mockResult(invocation.getArgument(1))
        }
    }
}
