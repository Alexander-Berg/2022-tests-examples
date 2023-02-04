import pytest
import requests
import time
from datetime import date, datetime, timedelta

from maps.pylibs.utils.lib.common import wait_until
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request,
    env_patch_request,
    create_route_env,
    cleanup_route_orders,
    push_positions,
    post_order_sequence,
    get_unistat,
    clean_unistat_signals
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.ya_courier import (
    DEFAULT_UPDATE_ROUTE_STATES_PERIOD_S,
    DEFAULT_UPDATE_ROUTE_STATES_DELAY_S,
    DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S,
    DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S
)

_TRACK = [
    (55.736294, 37.582708),
    (55.735834, 37.584918),
    (55.734696, 37.584853),
]
_LOCATION = (55.733827, 37.588722)

BAD_DEPOT_DATA = {
    'name': 'Склад 1',
    'address': 'ул. Льва Толстого, 16',
    'time_interval': '0.00:00-23:59',
    'lon': 89,
    'lat': 89,
    'description': 'курьерский подъезд',
    'service_duration_s': -600,
    'order_service_duration_s': -10,
}

TIME_TO_UPDATE_S = 2


def _make_track(track_pos, timestamp):
    return [(track_pos[0], track_pos[1], timestamp)]


@pytest.fixture(scope='function')
def courier_positions_env(system_env_with_db):
    env = system_env_with_db
    prefix = 'route_states_update_'

    with create_route_env(env, prefix + 'id') as route_env, \
            create_route_env(env, prefix + 'id-error', depot_data=BAD_DEPOT_DATA) as route_error_env, \
            create_route_env(env, prefix + 'id-empty') as route_empty_env, \
            create_route_env(env, prefix + 'id-badtime', route_date=date.today().isoformat()) as route_badtime_env:

        yield {
            'route_env': route_env,
            'route_empty_env': route_empty_env,
            'route_error_env': route_error_env,
            'route_badtime_env': route_badtime_env
        }


def get_route_updated_at(system_env_with_db, route_env, date_filter=None):
    if date_filter is None:
        date_filter = date.today()
    response = env_get_request(
        system_env_with_db,
        '{}/route-states?date={}&courier_number={}'.format(
            api_path_with_company_id(system_env_with_db),
            date_filter.isoformat(),
            route_env['courier']['number']
        ),
    )
    assert response.status_code == requests.codes.ok

    updated_at = response.json()[0]['updated_at']
    return updated_at


@skip_if_remote
def test_task_route_states_update(system_env_with_db, courier_positions_env):
    updated_at = get_route_updated_at(system_env_with_db, courier_positions_env['route_env'])
    push_positions(system_env_with_db,
                   courier_positions_env['route_env']['courier']['id'],
                   courier_positions_env['route_env']['route']['id'],
                   track=_make_track(_TRACK[0], time.time()))

    updated_at_empty = get_route_updated_at(system_env_with_db, courier_positions_env['route_empty_env'])
    push_positions(system_env_with_db,
                   courier_positions_env['route_empty_env']['courier']['id'],
                   courier_positions_env['route_empty_env']['route']['id'],
                   track=_make_track(_TRACK[0], time.time()))
    cleanup_route_orders(courier_positions_env['route_empty_env']['route']['id'], system_env_with_db)

    updated_at_error_route = get_route_updated_at(system_env_with_db, courier_positions_env['route_error_env'])
    push_positions(system_env_with_db,
                   courier_positions_env['route_error_env']['courier']['id'],
                   courier_positions_env['route_error_env']['route']['id'],
                   track=_make_track(_TRACK[0], time.time()))

    today = date.today()
    yesterday_timestamp = int((datetime(today.year, today.month, today.day) - timedelta(hours=1)).strftime("%s"))
    yesterday_track = _make_track(_TRACK[0], yesterday_timestamp)
    updated_at_badtime_route = get_route_updated_at(system_env_with_db, courier_positions_env['route_badtime_env'])
    push_positions(system_env_with_db,
                   courier_positions_env['route_badtime_env']['courier']['id'],
                   courier_positions_env['route_badtime_env']['route']['id'],
                   track=yesterday_track)

    assert get_unistat(system_env_with_db) is not None

    time.sleep(DEFAULT_UPDATE_ROUTE_STATES_DELAY_S + DEFAULT_UPDATE_ROUTE_STATES_PERIOD_S
               + len(courier_positions_env) * TIME_TO_UPDATE_S)

    updated_at2 = get_route_updated_at(system_env_with_db, courier_positions_env['route_env'])
    updated_at2_empty = get_route_updated_at(system_env_with_db, courier_positions_env['route_empty_env'])
    updated_at2_error_route = get_route_updated_at(system_env_with_db, courier_positions_env['route_error_env'])
    updated_at2_badtime_route = get_route_updated_at(system_env_with_db, courier_positions_env['route_badtime_env'])

    assert updated_at2 > updated_at
    assert updated_at2_empty > updated_at_empty
    assert updated_at2_error_route == updated_at_error_route
    assert updated_at2_badtime_route > updated_at_badtime_route

    j = get_unistat(system_env_with_db)
    assert len(j) > 0
    signals = clean_unistat_signals(j)
    assert 'dirty_routes_count_axxx' in signals
    assert 'error_routes_count_summ' in signals
    assert signals['error_routes_count_summ'] > 0
    assert signals['dirty_routes_count_axxx'] > 0
    assert 'dirty_routes_count_hgram_dhhh' in signals
    assert any(h[1] > 0 for h in signals['dirty_routes_count_hgram_dhhh'])

    old_error_routes_count = signals['error_routes_count_summ']
    for i in range(5):
        get_route_updated_at(system_env_with_db, courier_positions_env['route_error_env'])
    j = get_unistat(system_env_with_db)
    assert len(j) > 0
    signals = clean_unistat_signals(j)
    assert signals['error_routes_count_summ'] <= old_error_routes_count + 1


@pytest.fixture(scope='function')
def order_status_env(system_env_with_db):
    env = system_env_with_db
    prefix = 'route_update_status_'

    order_locations = [{"lat": _LOCATION[0], "lon": _LOCATION[1]}]
    with create_route_env(env, prefix + 'id-no_positions') as route_no_positions_env, \
            create_route_env(env, prefix + 'id') as route_env, \
            create_route_env(env, prefix + 'id-delivered', order_locations=order_locations) as route_delivered_env, \
            create_route_env(env, prefix + 'id-sequence') as route_sequence_env, \
            create_route_env(env, prefix + 'id-following_tomorrows_route',
                             route_date=(date.today() + timedelta(days=1)).isoformat()) as route_tomorrow_env:
        push_positions(system_env_with_db,
                       route_env['courier']['id'],
                       route_env['route']['id'],
                       track=_make_track(_TRACK[0], time.time()))
        push_positions(system_env_with_db,
                       route_delivered_env['courier']['id'],
                       route_delivered_env['route']['id'],
                       track=_make_track(_LOCATION, time.time() - 600))
        push_positions(system_env_with_db,
                       route_sequence_env['courier']['id'],
                       route_sequence_env['route']['id'],
                       track=_make_track(_TRACK[0], time.time()))

        yield {
            'route_env': route_env,
            'route_delivered_env': route_delivered_env,
            'route_sequence_env': route_sequence_env,
            'route_no_positions_env': route_no_positions_env,
            'route_for_tomorrow_env': route_tomorrow_env
        }


@skip_if_remote
def test_task_route_states_update_on_status(system_env_with_db, order_status_env):
    """
    Create routes and change it with different ways:
    1. route_env - patch status of one order of the route
    2. route_delivered_env - push positions, so one of orders is marked as delivered
    3. route_sequence_env - change sequence of orders in the route
    4. route_no_positions_env - change an order's state in a route without positions
    5. route_for_tomorrow_env - change an order's state in a route which has positions and starts tomorrow
    """

    route_env = order_status_env['route_env']
    route_delivered_env = order_status_env['route_delivered_env']
    route_sequence_env = order_status_env['route_sequence_env']
    route_no_positions_env = order_status_env['route_no_positions_env']
    route_for_tomorrow_env = order_status_env['route_for_tomorrow_env']

    updated_at = get_route_updated_at(system_env_with_db, route_env)
    updated_at_delivered = get_route_updated_at(system_env_with_db, route_delivered_env)
    updated_at_sequence = get_route_updated_at(system_env_with_db, route_sequence_env)
    updated_at_no_positions = get_route_updated_at(system_env_with_db, route_no_positions_env)
    tomorrow = date.today() + timedelta(days=1)
    updated_at_tomorrows_route = get_route_updated_at(system_env_with_db, route_for_tomorrow_env, date_filter=tomorrow)

    # change status by patch order
    order_id = route_env['orders'][0]['id']
    response = env_patch_request(system_env_with_db,
                                 path=api_path_with_company_id(system_env_with_db, 'orders/{}'.format(order_id)),
                                 data={'status': 'confirmed'})
    assert response.status_code == requests.codes.ok

    # change status by mark order delivered
    push_positions(system_env_with_db,
                   route_delivered_env['courier']['id'],
                   route_delivered_env['route']['id'],
                   track=_make_track(_LOCATION, time.time() + 600))

    # change order sequence
    new_sequence = [o['id'] for o in route_sequence_env['orders'][::-1]]
    post_order_sequence(system_env_with_db,
                        route_sequence_env['route']['id'],
                        new_sequence)

    # sending positions now for tomorrow's route and forcing status update
    push_positions(system_env_with_db,
                   route_for_tomorrow_env['courier']['id'], route_for_tomorrow_env['route']['id'],
                   track=_make_track(_TRACK[0], datetime.now().replace(hour=23).timestamp()))
    assert env_patch_request(system_env_with_db,
                             path=api_path_with_company_id(system_env_with_db, 'orders/{}'.format(
                                 route_for_tomorrow_env['orders'][0]['id'])),
                             data={'status': 'confirmed'}).ok

    # make route without positions dirty
    order_id = route_no_positions_env['orders'][0]['id']
    response = env_patch_request(system_env_with_db,
                                 path=api_path_with_company_id(system_env_with_db, 'orders/{}'.format(order_id)),
                                 data={'status': 'confirmed'})
    assert response.status_code == requests.codes.ok

    def unistat_matches_expected_intermediate_values():
        j = get_unistat(system_env_with_db)
        signals = clean_unistat_signals(j)
        return (
            len(j) > 0
            and 'dirty_routes_status_count_axxx' in signals
            and 0 < signals['dirty_routes_status_count_axxx'] <= len(order_status_env)
            and 'dirty_routes_status_count_hgram_dhhh' in signals
            and any(h[1] > 0 for h in signals['dirty_routes_status_count_hgram_dhhh']))

    assert wait_until(unistat_matches_expected_intermediate_values,
                      timeout=DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S * 2)

    # check update route state
    time.sleep(DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S
               + len(order_status_env) * TIME_TO_UPDATE_S)

    updated_at2 = get_route_updated_at(system_env_with_db, route_env)
    updated_at2_delivered = get_route_updated_at(system_env_with_db, route_delivered_env)
    updated_at2_sequence = get_route_updated_at(system_env_with_db, route_sequence_env)
    updated_at2_no_positions = get_route_updated_at(system_env_with_db, route_no_positions_env)
    updated_at2_tomorrows_route = get_route_updated_at(system_env_with_db, route_for_tomorrow_env, date_filter=tomorrow)

    assert updated_at2 > updated_at
    assert updated_at2_delivered > updated_at_delivered
    assert updated_at2_sequence > updated_at_sequence
    assert updated_at2_no_positions == updated_at_no_positions
    assert updated_at2_tomorrows_route > updated_at_tomorrows_route


@skip_if_remote
@pytest.mark.parametrize(
    'patch_data, push_position, is_updated',
    [
        ({'time_interval': '10:00 - 14:00'}, True, False),
        ({'time_interval': '2020-05-11T10:00:00+03:00/2020-05-11T14:00:00+03:00'}, True, False),
        ({'time_interval': '10:00 - 15:00'}, False, False),
        ({'time_interval': '10:00 - 15:00'}, True, True),
        ({'time_interval': '2020-05-11T10:00:00+03:00/2020-05-11T16:00:00+03:00'}, False, False),
        ({'time_interval': '2020-05-11T10:00:00+03:00/2020-05-11T16:00:00+03:00'}, True, True),
        ({'lat': _LOCATION[0]}, True, False),
        ({'lon': _LOCATION[1]}, True, False),
        ({'lat': _LOCATION[0] + 1}, False, False),
        ({'lat': _LOCATION[0] + 1}, True, True),
        ({'lat': _LOCATION[1] + 1}, True, True),
    ])
def test_task_route_states_update_on_patch(system_env_with_db, patch_data, push_position, is_updated):
    """
    Patch order and check if route_state is updated:
    1. time_window
    2. lat
    3. lon
    """
    order_locations = [{"lat": _LOCATION[0], "lon": _LOCATION[1]}]
    route_date = date(2020, 5, 11)  # same as in time_interval parameters above
    with create_route_env(system_env_with_db,
                          'route_update_on_time_window_lat_lon',
                          route_date=route_date.strftime("%F"),
                          order_locations=order_locations,
                          time_intervals=["10:00 - 14:00"]) as route_env:

        if push_position:
            push_positions(system_env_with_db,
                           route_env['courier']['id'],
                           route_env['route']['id'],
                           track=_make_track(_TRACK[0], datetime(2020, 5, 11, 10, 0, 0).timestamp()))
            # NB: push_positions itself marks route as dirty (dirty_position) and it can leads to
            # update route state. But update on order change is more urgent than update on position.
            # So, this test works correct if
            # DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S << DEFAULT_UPDATE_ROUTE_STATES_DELAY_S
            # and DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S < DEFAULT_UPDATE_ROUTE_STATES_DELAY_S
            assert 5 * DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S <= DEFAULT_UPDATE_ROUTE_STATES_DELAY_S
            assert DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S < DEFAULT_UPDATE_ROUTE_STATES_DELAY_S

        order_id = route_env['orders'][0]['id']
        response = env_patch_request(system_env_with_db,
                                     path=api_path_with_company_id(system_env_with_db, 'orders/{}'.format(order_id)),
                                     data=patch_data)
        assert response.status_code == requests.codes.ok
        updated_at = get_route_updated_at(system_env_with_db, route_env, date_filter=route_date)

        def get_dirty_routes_count():
            j = get_unistat(system_env_with_db)
            signals = clean_unistat_signals(j)
            return signals['dirty_routes_status_count_axxx']

        # after that sleep all dirty routes are in signal 'dirty_routes_status_count_axxx'
        time.sleep(DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S + 1)

        if is_updated:
            assert wait_until(lambda: get_dirty_routes_count() == 0,
                              timeout=DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S + TIME_TO_UPDATE_S)
        else:
            assert get_dirty_routes_count() == 0

        updated_at2 = get_route_updated_at(system_env_with_db, route_env, date_filter=route_date)
        if is_updated:
            assert updated_at2 > updated_at
        else:
            assert updated_at2 == updated_at
