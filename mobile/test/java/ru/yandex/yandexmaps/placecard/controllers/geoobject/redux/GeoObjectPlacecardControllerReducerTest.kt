package ru.yandex.yandexmaps.placecard.controllers.geoobject.redux

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.yandex.mapkit.GeoObject
import com.yandex.runtime.any.Collection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import ru.yandex.yandexmaps.business.common.entrances.Entrance
import ru.yandex.yandexmaps.common.mapkit.uri.buildOrgUri
import ru.yandex.yandexmaps.common.mapkit.uri.buildPinUri
import ru.yandex.yandexmaps.common.utils.CalendarUtils
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.core.search.SearchOrigin
import ru.yandex.yandexmaps.placecard.PlacecardItem
import ru.yandex.yandexmaps.placecard.actionsblock.ActionsBlockState
import ru.yandex.yandexmaps.placecard.controllers.geoobject.anchors.LogicalAnchor
import ru.yandex.yandexmaps.placecard.controllers.geoobject.booking.datespicker.items.ChangeBookingDateFocus
import ru.yandex.yandexmaps.placecard.controllers.geoobject.booking.datespicker.items.CloseBookingDatesController
import ru.yandex.yandexmaps.placecard.controllers.geoobject.booking.datespicker.items.SetBookingDateFromPicker
import ru.yandex.yandexmaps.placecard.controllers.geoobject.booking.redux.BookingDatesControllerState
import ru.yandex.yandexmaps.placecard.controllers.geoobject.epics.loading.GeoObjectAsyncLoading
import ru.yandex.yandexmaps.placecard.controllers.geoobject.epics.loading.GeoObjectPlacecardDataSource
import ru.yandex.yandexmaps.placecard.controllers.geoobject.internal.PlacecardGeoObjectResolver
import ru.yandex.yandexmaps.placecard.controllers.geoobject.redux.entrances.EntrancesState
import ru.yandex.yandexmaps.placecard.controllers.geoobject.redux.entrances.SelectEntrance
import ru.yandex.yandexmaps.placecard.controllers.geoobject.redux.entrances.ShowEntrances
import ru.yandex.yandexmaps.placecard.items.booking.OpenBookingDatesChooser
import ru.yandex.yandexmaps.placecard.items.error.ErrorItem
import ru.yandex.yandexmaps.placecard.items.reviews.loading.ReviewsLoadingItem
import ru.yandex.yandexmaps.placecard.items.stub.StubItem
import ru.yandex.yandexmaps.redux.Action
import ru.yandex.yandexmaps.tabs.main.api.ReviewsLoadingResult

class GeoObjectPlacecardControllerReducerTest {

    //region GeoObjectLoadingAction
    @Test
    fun `reduce by loading started action with pin uri source`() {
        val source = GeoObjectPlacecardDataSource.ByUri(Point(.0, .0).buildPinUri(), SearchOrigin.USER)
        reduceStateByStartedAction(
            source = source,
            expectedItems = listOf(StubItem.Toponym)
        )
    }

    @Test
    fun `reduce by loading started action with geo uri source`() {
        val source = GeoObjectPlacecardDataSource.ByUri("ymapsbm1://geo", SearchOrigin.USER)
        reduceStateByStartedAction(
            source = source,
            expectedItems = listOf(StubItem.Toponym)
        )
    }

    @Test
    fun `reduce by loading started action with org uri source`() {
        val source = GeoObjectPlacecardDataSource.ByUri("oid".buildOrgUri(), SearchOrigin.USER)
        reduceStateByStartedAction(
            source = source,
            expectedItems = listOf(StubItem.Business)
        )
    }

    @Test
    fun `reduce by loading started action with transit uri source`() {
        val source = GeoObjectPlacecardDataSource.ByUri("ymapsbm1://transit", SearchOrigin.USER)
        reduceStateByStartedAction(
            source = source,
            expectedItems = listOf(StubItem.MtStop(title = null))
        )
    }

    @Test
    fun `reduce by loading started action with geoobject source`() {
        val geoObject: GeoObject = mock {
            on { metadataContainer } doReturn mock(Collection::class.java)
        }
        val source = GeoObjectPlacecardDataSource.ByGeoObject(geoObject, 0, null, 0, false)
        reduceStateByStartedAction(
            source = source,
            expectedItems = emptyList()
        )
    }

    @Test
    fun `reduce by loading started action with point source`() {
        val source = GeoObjectPlacecardDataSource.ByPoint(Point(.0, .0), SearchOrigin.USER)
        reduceStateByStartedAction(
            source = source,
            expectedItems = listOf(StubItem.Toponym)
        )
    }

    @Test
    fun `reduce by loading started action with tappable source`() {
        val geoObject: GeoObject = mock {
            on { metadataContainer } doReturn mock(Collection::class.java)
        }
        val source = GeoObjectPlacecardDataSource.ByTappable(geoObject, Point(.0, .0), SearchOrigin.USER)
        reduceStateByStartedAction(
            source = source,
            expectedItems = listOf(StubItem.Toponym)
        )
    }

    @Test
    fun `reduce by loading started action with billboard source`() {
        val geoObject: GeoObject = mock {
            on { metadataContainer } doReturn mock(Collection::class.java)
        }
        val source = GeoObjectPlacecardDataSource.ByBillboard(geoObject, "", emptyList(), SearchOrigin.USER)
        reduceStateByStartedAction(
            source = source,
            expectedItems = listOf(StubItem.Business)
        )
    }

    @Test
    fun `reduce by loading started action with entrance source`() {
        val geoObject = mock(GeoObject::class.java)
        val entrance: Entrance = mock {
            on { point } doReturn mock(Point::class.java)
        }
        val source = GeoObjectPlacecardDataSource.ByEntrance(geoObject, null, 0, entrance, 0, null, false)
        reduceStateByStartedAction(
            source = source,
            expectedItems = emptyList()
        )
    }

    private fun reduceStateByStartedAction(source: GeoObjectPlacecardDataSource, expectedItems: List<PlacecardItem>) {
        val state = GeoObjectPlacecardControllerState(
            defaultAnchor = LogicalAnchor.SUMMARY,
            source = source,
            tabsState = null,
            experiments = PlacecardExperiments(
                pickUpBk = false,
                taxiPricesWithDiscount = false,
                yandexEatsTakeaway = false,
            ),
            debugExperiments = PlacecardDebugExperiments("")
        )
        val actualState = state.reduce(GeoObjectAsyncLoading.Started(source))
        val expectedState = state.copy(commonItems = expectedItems)
        assertEquals(expectedState, actualState)
    }

    @Test
    fun `reduce by loading error action`() {
        val state = initialState()
        val actualState = state.reduce(GeoObjectAsyncLoading.Error)
        val expectedState = state.copy(
            commonItems = listOf(ErrorItem()),
            loadingState = GeoObjectLoadingState.Error()
        )
        assertEquals(expectedState, actualState)
    }

    @Test
    fun `reduce by initial composing success action`() {
        reduceByAfterSuccess(expectedItems = emptyList())
    }
    //endregion

    //region ReviewsLoadingResult
    @Test
    fun `reduce by reviews loading action`() {
        reduceByAfterSuccess(ReviewsLoadingResult.Loading(false), initialItems = listOf(ReviewsLoadingItem()), expectedItems = listOf(ReviewsLoadingItem()))
    }

    //endregion

    //region OpenBookingDatesChooser
    @Test
    fun `reduce by open booking dates chooser`() {
        reduceByAfterSuccess(
            OpenBookingDatesChooser(dateFrom = 0, dateTill = 0),
            bookingDatesControllerState = BookingDatesControllerState(isOpened = true, fromDate = 0, tillDate = 0, focus = BookingDatesControllerState.Focus.FROM)
        )
    }
    //endregion

    //region BookingDatesControllerAction
    @Test
    fun `reduce by change booking date focus action`() {
        reduceByAfterSuccess(
            OpenBookingDatesChooser(dateFrom = 0, dateTill = 0), ChangeBookingDateFocus(BookingDatesControllerState.Focus.TILL),
            bookingDatesControllerState = BookingDatesControllerState(isOpened = true, fromDate = 0, tillDate = 0, focus = BookingDatesControllerState.Focus.TILL)
        )
    }

    @Test
    fun `reduce by close booking dates controller action`() {
        reduceByAfterSuccess(
            OpenBookingDatesChooser(dateFrom = 0, dateTill = 0), CloseBookingDatesController,
            bookingDatesControllerState = BookingDatesControllerState(isOpened = false, fromDate = 0, tillDate = 0, focus = BookingDatesControllerState.Focus.FROM)
        )
    }

    @Test
    fun `reduce by set booking date from picker action with from focus`() {
        reduceByAfterSuccess(
            OpenBookingDatesChooser(dateFrom = 0, dateTill = 0), SetBookingDateFromPicker(1),
            bookingDatesControllerState = BookingDatesControllerState(isOpened = true, fromDate = 1, tillDate = CalendarUtils.addDayToMillis(1, 1), focus = BookingDatesControllerState.Focus.TILL)
        )
    }

    @Test
    fun `reduce by set booking date from picker action with till focus`() {
        reduceByAfterSuccess(
            OpenBookingDatesChooser(dateFrom = 0, dateTill = 0), ChangeBookingDateFocus(BookingDatesControllerState.Focus.TILL), SetBookingDateFromPicker(1),
            bookingDatesControllerState = BookingDatesControllerState(isOpened = true, fromDate = 0, tillDate = 1, focus = BookingDatesControllerState.Focus.TILL)
        )
    }
    //endregion

    //region SelectEntrance
    @Test
    fun `reduce by select entrance action`() {
        val entrance = Entrance(name = null, point = Point(.0, .0))
        reduceByAfterSuccess(
            SelectEntrance(entrance),
            entrancesState = EntrancesState(selected = entrance)
        )
    }

    //endregion

    //region ShowEntrances
    @Test
    fun `reduce by show entrances action`() {
        reduceByAfterSuccess(
            ShowEntrances(entrances = emptyList(), layerEntranceShown = true),
            entrancesState = EntrancesState(layerEntranceShown = true)
        )
    }
    //endregion

    private fun reduceByAfterSuccess(
        vararg actions: Action,
        initialItems: List<PlacecardItem> = emptyList(),
        expectedItems: List<PlacecardItem> = emptyList(),
        bookingDatesControllerState: BookingDatesControllerState = BookingDatesControllerState(false, 0, 0, BookingDatesControllerState.Focus.FROM),
        entrancesState: EntrancesState = EntrancesState(),
    ) {
        val state = initialState()

        val successAction = successAction(initialItems)
        val actualState = actions.fold(state.reduce(successAction)) { it, action -> it.reduce(action) }

        val expectedState = state.copy(
            commonItems = expectedItems,
            loadingState = GeoObjectLoadingState.Ready(
                geoObject = successAction.result.geoObject,
                reqId = null,
                searchNumber = 0,
                point = Point(.0, .0),
                receivingTime = 0L,
                isOffline = false,
            ),
            actionsBlock = ActionsBlockState.Hidden,
            bookingDatesControllerState = bookingDatesControllerState,
            entrancesState = entrancesState
        )

        assertEquals(expectedState, actualState)
    }

    private fun initialState(): GeoObjectPlacecardControllerState {
        val source = GeoObjectPlacecardDataSource.ByUri("oid".buildOrgUri(), SearchOrigin.USER)
        return GeoObjectPlacecardControllerState(
            defaultAnchor = LogicalAnchor.SUMMARY,
            source = source,
            tabsState = null,
            experiments = PlacecardExperiments(
                pickUpBk = false,
                taxiPricesWithDiscount = false,
                yandexEatsTakeaway = false,
            ),
            debugExperiments = PlacecardDebugExperiments("")
        )
    }

    private fun successAction(items: List<PlacecardItem> = emptyList()): GeoObjectAsyncLoading.DelayedComposingSuccess {
        val geoObject: GeoObject = mock {
            on { metadataContainer } doReturn mock(Collection::class.java)
        }

        val point = Point(.0, .0)
        val result = PlacecardGeoObjectResolver.Result(
            geoObject = geoObject,
            searchNumber = 0,
            receivingTime = 0L,
            reqId = null,
            pointToUse = point,
            isOffline = false,
        )
        return GeoObjectAsyncLoading.DelayedComposingSuccess(
            items = items,
            actionsBlock = ActionsBlockState.Hidden,
            result = result,
            topGalleryState = null,
            tabsState = null
        )
    }
}
