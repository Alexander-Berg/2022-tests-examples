package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnRoomsTotalDialog
import org.junit.Test

/**
 * @author scrooge on 09.07.2019.
 */
class RoomsTotalTest : FilterParamTest() {

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithRoomsTotalTwoSelected() {
        shouldChangeOffersCountWhenTotalRoomsSet(TotalRooms.TWO)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithRoomsTotalThreeSelected() {
        shouldChangeOffersCountWhenTotalRoomsSet(TotalRooms.THREE)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithRoomsTotalFourSelected() {
        shouldChangeOffersCountWhenTotalRoomsSet(TotalRooms.FOUR)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithRoomsTotalFiveSelected() {
        shouldChangeOffersCountWhenTotalRoomsSet(TotalRooms.FIVE)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithRoomsTotalSixSelected() {
        shouldChangeOffersCountWhenTotalRoomsSet(TotalRooms.SIX)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithRoomsTotalSevenPlusSelected() {
        shouldChangeOffersCountWhenTotalRoomsSet(TotalRooms.SEVEN_PLUS)
    }

    private fun shouldChangeOffersCountWhenTotalRoomsSet(
        roomsTotal: TotalRooms
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.ROOM.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldRoomsTotal())
                    .tapOn()
                performOnRoomsTotalDialog {
                    tapOn(roomsTotal.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }
                isRoomsTotalEquals(roomsTotal.expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.ROOM.param,
                roomsTotal.param
            )
        )
    }
}
