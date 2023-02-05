package ru.yandex.yandexmaps.placecard.controllers.geoobject.redux

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.yandexmaps.common.mapkit.Money
import ru.yandex.yandexmaps.placecard.PlacecardItem
import ru.yandex.yandexmaps.placecard.items.booking.BookingBottomSeparator
import ru.yandex.yandexmaps.placecard.items.booking.BookingConditionsItem
import ru.yandex.yandexmaps.placecard.items.booking.BookingMoreItem
import ru.yandex.yandexmaps.placecard.items.booking.BookingNothingFoundItem
import ru.yandex.yandexmaps.placecard.items.booking.BookingProgressItem
import ru.yandex.yandexmaps.placecard.items.booking.BookingProposalItem
import ru.yandex.yandexmaps.redux.Action
import ru.yandex.yandexmaps.tabs.main.api.MainTabContentState
import ru.yandex.yandexmaps.tabs.main.api.booking.ConfirmBookingDates
import ru.yandex.yandexmaps.tabs.main.internal.booking.BookingResponse
import ru.yandex.yandexmaps.tabs.main.internal.booking.OpenBookingProposal
import ru.yandex.yandexmaps.tabs.main.internal.booking.ShowBookingNothingFound
import ru.yandex.yandexmaps.tabs.main.internal.booking.ShowBookingOffers
import ru.yandex.yandexmaps.tabs.main.internal.booking.ShowBookingSearchProgress
import ru.yandex.yandexmaps.tabs.main.internal.booking.ShowMoreBookingVariants
import ru.yandex.yandexmaps.tabs.main.internal.redux.reduce
import java.util.concurrent.TimeUnit

class MainTabReducerTest {

    //region BookingItemsReducingAction
    @Test
    fun `reduce by show booking search progress action`() {
        val bookingConditionsItem = BookingConditionsItem(0, 0, 2)
        reduceBy(
            ShowBookingSearchProgress,
            initialItems = listOf(bookingConditionsItem),
            expectedItems = listOf(bookingConditionsItem, BookingProgressItem, BookingBottomSeparator)
        )
    }

    @Test
    fun `reduce by show booking nothing found action`() {
        val bookingConditionsItem = BookingConditionsItem(0, 0, 2)
        reduceBy(
            ShowBookingNothingFound("", "", 2),
            initialItems = listOf(bookingConditionsItem),
            expectedItems = listOf(bookingConditionsItem, BookingNothingFoundItem, BookingBottomSeparator)
        )
    }

    @Test
    fun `reduce by show booking offers action with 3 offers`() {
        val bookingConditionsItem = BookingConditionsItem(0, 0, 2)
        val offersResponse = BookingResponse(
            BookingResponse.Conditions(checkInDate = 0, nightsAmount = 1, guestsAmount = 2),
            listOf(bookingOffer(1), bookingOffer(2), bookingOffer(3))
        )
        reduceBy(
            ShowBookingOffers(offersResponse),
            initialItems = listOf(bookingConditionsItem),
            expectedItems = listOf(
                BookingConditionsItem(dateFrom = 0, dateTill = TimeUnit.DAYS.toMillis(1), guestsAmount = 2),
                bookingProposalItem(1),
                bookingProposalItem(2),
                bookingProposalItem(3),
                BookingBottomSeparator
            )
        )
    }

    @Test
    fun `reduce by show booking offers action with more than 3 offers`() {
        val bookingConditionsItem = BookingConditionsItem(0, 0, 2)
        val offersResponse = BookingResponse(
            BookingResponse.Conditions(checkInDate = 0, nightsAmount = 1, guestsAmount = 2),
            listOf(bookingOffer(1), bookingOffer(2), bookingOffer(3), bookingOffer(4))
        )
        reduceBy(
            ShowBookingOffers(offersResponse),
            initialItems = listOf(bookingConditionsItem),
            expectedItems = listOf(
                BookingConditionsItem(dateFrom = 0, dateTill = TimeUnit.DAYS.toMillis(1), guestsAmount = 2),
                bookingProposalItem(1),
                bookingProposalItem(2),
                bookingProposalItem(3),
                BookingMoreItem(ShowMoreBookingVariants(listOf(bookingProposalItem(4)))),
                BookingBottomSeparator

            )
        )
    }

    @Test
    fun `reduce by show more booking variants action`() {
        val bookingConditionsItem = BookingConditionsItem(0, 0, 2)
        val offersResponse = BookingResponse(
            BookingResponse.Conditions(checkInDate = 0, nightsAmount = 1, guestsAmount = 2),
            listOf(bookingOffer(1), bookingOffer(2), bookingOffer(3), bookingOffer(4))
        )
        val expectedItems = listOf(
            BookingConditionsItem(dateFrom = 0, dateTill = TimeUnit.DAYS.toMillis(1), guestsAmount = 2),
            bookingProposalItem(1),
            bookingProposalItem(2),
            bookingProposalItem(3),
            bookingProposalItem(4),
            BookingBottomSeparator

        )
        reduceBy(
            ShowBookingOffers(offersResponse), ShowMoreBookingVariants(listOf(bookingProposalItem(4))),
            initialItems = listOf(bookingConditionsItem),
            expectedItems = expectedItems
        )
    }

    private fun bookingOffer(index: Int) = BookingResponse.Offer("partner$index", listOf(BookingResponse.Link("", "link$index")), null, Money(index.toDouble(), "", ""))

    private fun bookingProposalItem(index: Int) = BookingProposalItem(title = "partner$index", price = "", iconUriTemplate = null, openProposalAction = OpenBookingProposal(uri = "link$index", checkIn = 0, checkOut = TimeUnit.DAYS.toMillis(1), adultsNumber = 2, dataProvider = "partner$index", position = index - 1, minPrice = index, currency = ""))

    //endregion

    //region ConfirmBookingDates
    @Test
    fun `reduce by confirm booking dates`() {
        val bookingConditionsItem = BookingConditionsItem(0, 0, 2)
        val offersResponse = BookingResponse(BookingResponse.Conditions(checkInDate = 0, nightsAmount = 1, guestsAmount = 2), listOf(bookingOffer(1)))
        reduceBy(
            ShowBookingOffers(offersResponse), ConfirmBookingDates(dateFrom = 1, dateTill = 2, adultsNumber = 2),
            initialItems = listOf(bookingConditionsItem),
            expectedItems = listOf(
                BookingConditionsItem(dateFrom = 1, dateTill = 2, guestsAmount = 2),
                bookingProposalItem(1),
                BookingBottomSeparator
            )
        )
    }

    //endregion

    private fun reduceBy(
        vararg actions: Action,
        initialItems: List<PlacecardItem> = emptyList(),
        expectedItems: List<PlacecardItem> = emptyList(),
    ) {
        val state = initialState().copy(items = initialItems)

        val actualState = actions.fold(state) { it, action -> it.reduce(action) }

        val expectedState = state.copy(
            items = expectedItems
        )

        assertEquals(expectedState, actualState)
    }

    private fun initialState(): MainTabContentState {
        return MainTabContentState()
    }
}
