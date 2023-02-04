from datetime import datetime, timedelta
import dateutil.tz

import pytest

from ya_courier_backend.models.route import ACTIVE_ROUTE_MAX_EARLINESS_H, ACTIVE_ROUTE_MAX_LATENESS_H

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    create_route_env, push_imei_positions,
    get_courier_position_list, patch_company,
)


ORDER_WINDOWS_START_HOUR = 12
ORDER_WINDOWS_END_HOUR = 14


@pytest.mark.parametrize(
    'time_zone_str',
    [
        'Europe/Moscow',
        'Etc/GMT-14',
        'Etc/GMT+12'
    ]
)
@pytest.mark.parametrize(
    'route_start, route_finish, courier_times, expected_track_length, imei',
    [
        (None, None, [ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H - 1], 0, 12345678901112100),
        (None, None, [ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H], 1, 12345678901112101),
        (None, None, [ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H + 1], 1, 12345678901112102),
        (None, None, [ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H - 1], 1, 12345678901112103),
        (None, None, [ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H], 1, 12345678901112104),
        (None, None, [ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H + 1], 0, 12345678901112105),
        (None, None, [ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H, ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H],
            2, 12345678901112106),
        (None, None, [ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H - 1, ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H + 1],
            0, 12345678901112107),
        (None, None, [ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H - 1, ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H],
            1, 12345678901112108),
        (None, None, [ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H, ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H + 1],
            1, 12345678901112109),
        ('10:00:00', None, [9], 0, 12345678901112110),
        ('10:00:00', None, [10], 1, 12345678901112111),
        ('10:00:00', None, [9, 10], 1, 12345678901112112),
        (None, '16:00:00', [17], 0, 12345678901112113),
        (None, '16:00:00', [15], 1, 12345678901112114),
        (None, '16:00:00', [15, 17], 1, 12345678901112115),
        ('10:00:00', '16:00:00', [9, 11, 15, 17], 2, 12345678901112116),
        ('00:00:00', "{:02d}:00".format(ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H + 2), [ORDER_WINDOWS_END_HOUR + ACTIVE_ROUTE_MAX_LATENESS_H + 1],
            1, 12345678901112117),
        ("{:02d}:00".format(ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H - 2), '1.00:00', [ORDER_WINDOWS_START_HOUR - ACTIVE_ROUTE_MAX_EARLINESS_H - 1],
            1, 12345678901112118)
    ]
)
def test_push_gps_tracker_positions(system_env_with_db, time_zone_str, route_start, route_finish, courier_times, expected_track_length, imei):
    """
    Push track positions and check if they were actually saved
    """
    patch_company(system_env_with_db, {"tracking_start_h": 0})
    imei = 12345678901112131
    route_datetime = datetime(2019, 7, 31, 0, 0, 0, tzinfo=dateutil.tz.gettz(time_zone_str))
    with create_route_env(system_env_with_db, f"test_route_start_tracking-{imei}",
                          route_date=route_datetime.date().isoformat(),
                          order_locations=[{"lat": 55.733827, "lon": 37.588722}],
                          imei=imei,
                          time_intervals=["{:02d}:00-{:02d}:00".format(ORDER_WINDOWS_START_HOUR, ORDER_WINDOWS_END_HOUR)],
                          depot_data={'lat': 55.55, 'lon': 33.33, 'address': 'Some address', 'time_zone': time_zone_str},
                          route_start=route_start,
                          route_finish=route_finish) as route_env:
        route = route_env['route']
        courier = route_env['courier']

        track = [(55.736294, 37.582708, route_datetime + timedelta(hours=x)) for x in courier_times]

        push_imei_positions(system_env_with_db, imei, start_datetime=None, track=track)

        positions = get_courier_position_list(system_env_with_db, courier['id'], route['id'])
        assert len(positions) == expected_track_length
