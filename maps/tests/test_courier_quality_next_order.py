import time

from maps.b2bgeo.ya_courier.backend.test_lib import util
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def _call_routed_orders(system_env_with_db, route_env):
    current_location = {
        'lat': route_env['orders'][0]['lat'],
        'lon': route_env['orders'][0]['lon'],
        'timestamp': time.time()
    }
    util.query_routed_orders(system_env_with_db, route_env['courier']['id'], route_env['route']['id'], current_location)


def _mark_orders_finished(system_env_with_db, route_env, order_indices):
    for i in order_indices:
        util.finish_order(system_env_with_db, route_env['orders'][i])


def _set_order_sequence(system_env_with_db, route_env, order_indices):
    new_sequence = [route_env['orders'][i]['id'] for i in order_indices]
    util.post_order_sequence(system_env_with_db, route_env['route']['id'], new_sequence)


def _get_courier_quality_for_route(system_env_with_db, route_env):
    courier_report = util.get_courier_quality(system_env_with_db, route_env['route']['date'])
    assert len(courier_report) >= len(route_env['orders'])

    report_for_current_route = \
        [record for record in courier_report if record['route_number'] == route_env['route']['number']]
    assert len(report_for_current_route) == len(route_env['orders'])

    return report_for_current_route


def _check_delivered_orders(system_env_with_db, route_env, expected_order_indices):
    report = _get_courier_quality_for_route(system_env_with_db, route_env)
    for i, item in enumerate(report):
        assert item['order_number'] == route_env['orders'][expected_order_indices[i]]['number']


def _check_suggested_orders(system_env_with_db, route_env, expected_order_indices):
    report = _get_courier_quality_for_route(system_env_with_db, route_env)
    for i, item in enumerate(report):
        assert item['suggested_order_number'] == route_env['orders'][expected_order_indices[i]]['number']
        assert item['not_in_order'] is (item['order_number'] != item['suggested_order_number'])


def test_following_planned_sequence(system_env_with_db):
    """
    Test the following workflow:
        - a route with three orders at the same location is created
        - orders are marked finished in the order of planned order sequence
        - courier-quality report is requested for the route
            * check that 'suggested_order_number' ant 'not_in_order' for every order in
              the report are set properly
    """
    db_env = system_env_with_db
    with util.create_route_env(db_env, 'test_next_order', order_locations=[{"lat": 55.73, "lon": 37.58}]*3) as env:
        _call_routed_orders(db_env, env)
        _mark_orders_finished(db_env, env, [0, 1, 2])
        _check_delivered_orders(db_env, env, [0, 1, 2])
        _check_suggested_orders(db_env, env, [0, 1, 2])


def test_changing_planned_sequence_and_following_it(system_env_with_db):
    """
    Test the following workflow:
        - a route with three orders at the same location is created
        - first order is marked finished
        - change planned order sequence for the remaining two orders
        - courier marks the remaining orders finished in the order of the new planned sequence
        - courier-quality report is requested for the route
            * check that report shows that the courier followed the sequence
    """
    db_env = system_env_with_db
    with util.create_route_env(db_env, 'test_next_order', order_locations=[{"lat": 55.73, "lon": 37.58}]*3) as env:
        _call_routed_orders(db_env, env)
        _mark_orders_finished(db_env, env, [0])
        _set_order_sequence(db_env, env, [0, 2, 1])
        _call_routed_orders(db_env, env)  # TODO: remove this line after implementing https://st.yandex-team.ru/BBGEO-5780
        _mark_orders_finished(db_env, env, [2, 1])
        _check_delivered_orders(db_env, env, [0, 2, 1])
        _check_suggested_orders(db_env, env, [0, 2, 1])


def test_changing_planned_sequence_and_not_following_it(system_env_with_db):
    """
    Test the following workflow:
        - a route with three orders at the same location is created
        - first order is marked finished
        - change planned order sequence for the remaining two orders
        - courier marks the remaining orders finished in the order of the old planned sequence
        - courier-quality report is requested for the route
            * check that report shows that the courier didn't follow the sequence
    """
    db_env = system_env_with_db
    with util.create_route_env(db_env, 'test_next_order', order_locations=[{"lat": 55.73, "lon": 37.58}]*3) as env:
        _call_routed_orders(db_env, env)
        _mark_orders_finished(db_env, env, [0])
        _set_order_sequence(db_env, env, [0, 2, 1])
        _call_routed_orders(db_env, env)  # TODO: remove this line after implementing https://st.yandex-team.ru/BBGEO-5780
        _mark_orders_finished(db_env, env, [1, 2])
        _check_delivered_orders(db_env, env, [0, 1, 2])
        _check_suggested_orders(db_env, env, [0, 2, 2])


# TODO: Delete this test after implementing https://st.yandex-team.ru/BBGEO-5780
@skip_if_remote
def test_changing_planned_sequence_and_following_it_fails(system_env_with_db_without_updating_route_state):
    """
    Test the following workflow:
        - a route with three orders at the same location is created
        - first order is marked finished
        - change planned order sequence for the remaining two orders
        - courier marks the remaining orders finished in the order of the new planned sequence
        - courier-quality report is requested for the route
            * check that report shows that the courier didn't(!) follow the sequence.

        Note: "not following the sequence" happens only because marking orders finished
        has happened instantly right after changing planned order sequence. In this case
        our background threads simply do not have enough time to re-compute route state.
        We emulate this "didn't have enough time" by disabling background threads
        completely by using system_env_with_db_without_updating_route_state.

        After stopping using route_state_history::next_orders for courier quality report
        generation we will delete this test (the test will start failing).
        The ticket for it: https://st.yandex-team.ru/BBGEO-5780
    """
    db_env = system_env_with_db_without_updating_route_state
    with util.create_route_env(db_env, 'test_next_order', order_locations=[{"lat": 55.73, "lon": 37.58}]*3) as env:
        _call_routed_orders(db_env, env)
        _mark_orders_finished(db_env, env, [0])
        _set_order_sequence(db_env, env, [0, 2, 1])
        _mark_orders_finished(db_env, env, [2, 1])
        _check_delivered_orders(db_env, env, [0, 2, 1])
        _check_suggested_orders(db_env, env, [0, 1, 1])
