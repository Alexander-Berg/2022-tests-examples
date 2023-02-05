package ru.yandex.yandexnavi.projected.testapp.impl

import com.yandex.mapkit.road_events.EventTag
import com.yandex.navikit.projected.ui.road_events.AvailableRoadEventsProvider

class AvailableRoadEventsProviderImpl : AvailableRoadEventsProvider {

    override fun availableRoadEvents(): List<EventTag> {
        return listOf<EventTag>(
            EventTag.DRAWBRIDGE,
            EventTag.CLOSED,
            EventTag.RECONSTRUCTION,
            EventTag.ACCIDENT,
            EventTag.TRAFFIC_ALERT,
            /**
             * Potentially dangerous zones
             */
            EventTag.DANGER,
            EventTag.SCHOOL,
            EventTag.OVERTAKING_DANGER,
            EventTag.PEDESTRIAN_DANGER,
            EventTag.CROSS_ROAD_DANGER,
            /**
             * Traffic code control tags
             */
            EventTag.POLICE,
            EventTag.LANE_CONTROL,
            EventTag.ROAD_MARKING_CONTROL,
            EventTag.CROSS_ROAD_CONTROL,
            EventTag.NO_STOPPING_CONTROL,
            EventTag.MOBILE_CONTROL,
            EventTag.SPEED_CONTROL
        )
    }
}
