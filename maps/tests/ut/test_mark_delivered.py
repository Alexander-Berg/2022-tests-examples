from datetime import datetime
from pytz import timezone
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
import pytest

from ya_courier_backend.logic.mark_delivered import detect_service_done, detect_departure
from ya_courier_backend.models import LogisticCompany, EtaType, TimeWindow, DepotInstance, Depot, Order, RouteNode
from ya_courier_backend.util.unique_intervals import UniqueIntervals, Interval


LOCATION = (55.580819, 37.906763)
LOCATION_300M_DISTANCE = (55.583506, 37.903934)
LOCATION_600M_DISTANCE = (55.585410, 37.901132)


class MockOrderNode(RouteNode):
    def __init__(self):
        self.route_id = 1
        self.route_sequence_pos = 0
        self.entity = Order(lat=LOCATION[0], lon=LOCATION[1], service_duration_s=60, shared_service_duration_s=0)


class MockDepotInstanceNode(RouteNode):
    def __init__(self, service_duration_s=60):
        self.route_id = 1
        self.route_sequence_pos = 0
        self.entity = DepotInstance(depot=Depot(service_duration_s=service_duration_s, lat=LOCATION[0], lon=LOCATION[1], mark_route_started_radius=500), depot_id=1)


COMPANY = LogisticCompany(
    mark_delivered_radius=500,
    mark_delivered_service_time_coefficient=0.5
)
ORDER_NODE = MockOrderNode()
DEPOT_NODE = MockDepotInstanceNode()


def _make_position(locaton, time):
    return {'time': time, 'lat': locaton[0], 'lon': locaton[1]}


def _get_expected_delivery_time(start_time, route_node, company=COMPANY):
    if route_node.is_order():
        service_duration_s = route_node.entity.service_duration_s
    elif route_node.is_depot_instance():
        service_duration_s = route_node.entity.depot.service_duration_s
    else:
        raise ValueError('Unhandled route_node type')

    return start_time + company.mark_delivered_service_time_coefficient * service_duration_s


def _make_delivery_position(start_pos, route_node, company=COMPANY):
    expected_delivery_time = _get_expected_delivery_time(start_pos['time'], route_node, company)
    return {**start_pos, 'time': expected_delivery_time}


def _get_dict_positions_from_tuples(list_positions):
    positions = []
    for i, pos in enumerate(list_positions):
        positions.append({'lat': pos[0], 'lon': pos[1], 'time': i})
    return positions


def _used_intervals_up_to(time):
    res = UniqueIntervals()
    res.add(Interval(0, time))
    return res


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_no_positions(node):
    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[],
        earliest_time=0) is None


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_one_position(node):
    pos = _make_position(LOCATION, 1)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos],
        earliest_time=0) is None


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_two_positions_at_location_not_enough_time(node):
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 2)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) is None


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_two_positions_at_location_enough_time(node):
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 40)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) == {'interval': {'finish': pos2, 'start': pos1},
                             'delivery_position': _make_delivery_position(pos1, node),
                             'departure_position': None,
                             'radius': 500}


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_two_positions_close_enough_time(node):
    pos1 = _make_position(LOCATION_300M_DISTANCE, 1)
    pos2 = _make_position(LOCATION_300M_DISTANCE, 40)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) == {'interval': {'finish': pos2, 'start': pos1},
                             'delivery_position': _make_delivery_position(pos1, node),
                             'departure_position': None,
                             'radius': 500}


@skip_if_remote
def test_two_positions_with_radius_far_enough_time():
    pos1 = _make_position(LOCATION_300M_DISTANCE, 1)
    pos2 = _make_position(LOCATION_300M_DISTANCE, 40)

    order_node = MockOrderNode()
    order_node.entity.mark_delivered_radius = 100

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) is None


@skip_if_remote
def test_two_positions_with_radius_close_enough_time():
    pos1 = _make_position(LOCATION_600M_DISTANCE, 1)
    pos2 = _make_position(LOCATION_600M_DISTANCE, 40)

    order_node = MockOrderNode()
    order_node.entity.mark_delivered_radius = 700

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) == {'interval': {'finish': pos2, 'start': pos1},
                             'delivery_position': _make_delivery_position(pos1, order_node),
                             'departure_position': None,
                             'radius': 700}


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_two_positions_far_enough_time(node):
    pos1 = _make_position(LOCATION_600M_DISTANCE, 1)
    pos2 = _make_position(LOCATION_600M_DISTANCE, 40)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) is None


@skip_if_remote
def test_delivery_position_on_coordinates_time_gap():
    # BBGEO-8001: courier may not send coordinates
    # continuously while delivery is in progress.
    start_time = 1
    end_time = 61

    pos1 = _make_position(LOCATION, start_time)
    pos2 = _make_position(LOCATION, end_time)

    order_node = MockOrderNode()
    order_node.entity.service_duration_s = 60

    expected_result = {'interval': {'finish': pos2, 'start': pos1},
                       'delivery_position': _make_delivery_position(pos1, order_node),
                       'departure_position': None,
                       'radius': 500}

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) == expected_result


@skip_if_remote
def test_delivery_position_on_coordinates_time_gap_if_moved_out_of_radius():
    start_time = 1
    end_time = 61

    # The first position is near the location, the second one
    # is after a large time gap but outside of delivery radius
    pos1 = _make_position(LOCATION, start_time)
    pos2 = _make_position(LOCATION_600M_DISTANCE, end_time)

    order_node = MockOrderNode()
    order_node.entity.service_duration_s = 60

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) is None


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_partially_before_earliest_time(node):
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 40)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=20) == {'interval': {'finish': pos2, 'start': pos1},
                              'delivery_position': _make_delivery_position(pos1, node),
                              'departure_position': None,
                              'radius': 500}


@skip_if_remote
def test_delivery_start_in_window():
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 40)

    order_node = MockOrderNode()
    order_node.entity.eta_type = EtaType.delivery_time
    time_window = TimeWindow(
        start=datetime.fromtimestamp(1, timezone('Europe/Moscow')),
        end=datetime.fromtimestamp(40, timezone('Europe/Moscow'))
    )
    order_node.entity.time_windows.append(time_window)

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) == {'interval': {'finish': pos2, 'start': pos1},
                             'delivery_position': _make_delivery_position(pos1, order_node),
                             'departure_position': None,
                             'radius': 500}


@skip_if_remote
def test_from_delivery_start_not_in_window():
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 40)

    order_node = MockOrderNode()
    order_node.entity.eta_type = EtaType.delivery_time
    time_window = TimeWindow(
        start=datetime.fromtimestamp(100, timezone('Europe/Moscow')),
        end=datetime.fromtimestamp(200, timezone('Europe/Moscow'))
    )
    order_node.entity.time_windows.append(time_window)

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) is None


@skip_if_remote
def test_delivery_start_partial_in_window():
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 60)

    window_start = 20
    window_end = 200

    order_node = MockOrderNode()
    order_node.entity.eta_type = EtaType.delivery_time
    time_window = TimeWindow(
        start=datetime.fromtimestamp(window_start, timezone('Europe/Moscow')),
        end=datetime.fromtimestamp(window_end, timezone('Europe/Moscow'))
    )
    order_node.entity.time_windows.append(time_window)

    expected_delivery_time = _get_expected_delivery_time(window_start, order_node)
    expected_delivery_position = _make_position(LOCATION, expected_delivery_time)

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) == {'interval': {'finish': pos2, 'start': pos1},
                             'delivery_position': expected_delivery_position,
                             'departure_position': None,
                             'radius': 500}


@skip_if_remote
def test_delivery_start_partial_in_window_not_enough_time():
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 60)

    order_node = MockOrderNode()
    order_node.entity.eta_type = EtaType.delivery_time
    time_window = TimeWindow(
        start=datetime.fromtimestamp(50, timezone('Europe/Moscow')),
        end=datetime.fromtimestamp(200, timezone('Europe/Moscow'))
    )
    order_node.entity.time_windows.append(time_window)

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0) is None


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_many_positions_at_location_not_enough_time(node):
    positions = []
    for idx in range(1, 20):
        positions.append(_make_position(LOCATION, idx))

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=positions,
        earliest_time=0) is None


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_many_positions_at_location_enough_time(node):
    positions = []
    for idx in range(1, 20):
        positions.append(_make_position(LOCATION, idx))

    positions.append(_make_position(LOCATION, 40))

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=positions,
        earliest_time=0) == {'interval': {'finish': positions[-1], 'start': positions[0]},
                             'delivery_position': _make_delivery_position(positions[0], node),
                             'departure_position': None,
                             'radius': 500}


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_interval_ends_after_earliest_time(node):
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 40)
    pos3 = _make_position(LOCATION_600M_DISTANCE, 50)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos1, pos2, pos3],
        earliest_time=0) == {
            'interval': {'finish': pos2, 'start': pos1},
            'delivery_position': _make_delivery_position(pos1, node),
            'departure_position': pos2,
            'radius': 500}


@skip_if_remote
@pytest.mark.parametrize('node', [ORDER_NODE, DEPOT_NODE])
def test_interval_ends_before_earliest_time(node):
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 40)
    pos3 = _make_position(LOCATION_600M_DISTANCE, 50)

    assert detect_service_done(
        node=node,
        company=COMPANY,
        courier_positions=[pos1, pos2, pos3],
        earliest_time=41) is None


@skip_if_remote
def test_detect_departure_visit_index_equals_none():
    order_node = MockOrderNode()
    order_node.entity.mark_delivered_radius = 300
    company = {'mark_delivered_radius': 200}

    visit_position = {'lat': 55.6, 'lon': 37.5}
    assert detect_departure(order_node, company, [], visit_position) == (None, 300)


@skip_if_remote
def test_detect_departure():
    order_node = MockOrderNode()
    order_node.entity.mark_delivered_radius = 400
    company = {'mark_delivered_radius': 200}

    visit_position, pos_shifted_300, pos_shifted_600 = _get_dict_positions_from_tuples([LOCATION,
                                                                                        LOCATION_300M_DISTANCE,
                                                                                        LOCATION_600M_DISTANCE])

    courier_positions = [visit_position, pos_shifted_300, pos_shifted_600]
    assert detect_departure(order_node, company, courier_positions, visit_position) == (pos_shifted_300, 400)


@skip_if_remote
def test_detect_departure_all_positions_inside_radius():
    order_node = MockOrderNode()
    order_node.entity.mark_delivered_radius = 700
    company = {'mark_delivered_radius': 800}

    visit_position, pos_shifted_300, pos_shifted_600 = _get_dict_positions_from_tuples([LOCATION,
                                                                                        LOCATION_300M_DISTANCE,
                                                                                        LOCATION_600M_DISTANCE])

    courier_positions = [visit_position, pos_shifted_300, pos_shifted_600]
    assert detect_departure(order_node, company, courier_positions, visit_position) == (None, 700)


@skip_if_remote
def test_detect_departure_visit_position_is_last_position():
    order_node = MockOrderNode()
    order_node.entity.mark_delivered_radius = 700
    company = {'mark_delivered_radius': 800}

    visit_position, pos_shifted_300, pos_shifted_600 = _get_dict_positions_from_tuples([LOCATION,
                                                                                        LOCATION_300M_DISTANCE,
                                                                                        LOCATION_600M_DISTANCE])

    courier_positions = [pos_shifted_300, pos_shifted_600, visit_position]
    assert detect_departure(order_node, company, courier_positions, visit_position) == (None, 700)


@skip_if_remote
def test_detect_depot_visit_null_service_duration_s():
    assert detect_service_done(
        node=MockDepotInstanceNode(service_duration_s=None),
        company=COMPANY,
        courier_positions=[],
        earliest_time=0) is None


@skip_if_remote
def test_next_delivery_starts_after_the_last_one():
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 60)

    order_node = MockOrderNode()
    order_node.entity.eta_type = EtaType.arrival_time

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0,
        used_intervals=_used_intervals_up_to(1),
    ) == {
        "interval": {"finish": pos2, "start": pos1},
        "delivery_position": _make_delivery_position(pos1, order_node),
        "departure_position": None,
        "radius": 500,
    }

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0,
        used_intervals=_used_intervals_up_to(30),
    ) == {
        "interval": {"finish": pos2, "start": pos1},
        "delivery_position": _make_position(LOCATION, _get_expected_delivery_time(30, order_node)),
        "departure_position": None,
        "radius": 500,
    }

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0,
        used_intervals=_used_intervals_up_to(31),
    ) is None


@skip_if_remote
def test_next_delivery_starts_after_maximum_of_last_one_and_window_start():
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 60)

    order_node = MockOrderNode()
    time_window = TimeWindow(
        start=datetime.fromtimestamp(20, timezone('Europe/Moscow')),
        end=datetime.fromtimestamp(200, timezone('Europe/Moscow'))
    )
    order_node.entity.time_windows.append(time_window)
    order_node.entity.eta_type = EtaType.delivery_time

    assert detect_service_done(
        node=order_node, company=COMPANY, courier_positions=[pos1, pos2], earliest_time=0
    ) == {
        "interval": {"finish": pos2, "start": pos1},
        "delivery_position": _make_position(LOCATION, _get_expected_delivery_time(20, order_node)),
        "departure_position": None,
        "radius": 500,
    }

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0,
        used_intervals=_used_intervals_up_to(30),
    ) == {
        "interval": {"finish": pos2, "start": pos1},
        "delivery_position": _make_position(LOCATION, _get_expected_delivery_time(30, order_node)),
        "departure_position": None,
        "radius": 500,
    }


@skip_if_remote
def test_delivery_with_zero_service_duration_is_processed_at_the_end_of_interval():
    pos1 = _make_position(LOCATION, 1)
    pos2 = _make_position(LOCATION, 60)

    order_node = MockOrderNode()
    order_node.entity.service_duration_s = 0
    order_node.entity.shared_service_duration_s = 10

    assert detect_service_done(
        node=order_node,
        company=COMPANY,
        courier_positions=[pos1, pos2],
        earliest_time=0,
        used_intervals=_used_intervals_up_to(60),
    ) == {
        "interval": {"finish": pos2, "start": pos1},
        "delivery_position": _make_position(LOCATION, _get_expected_delivery_time(60, order_node)),
        "departure_position": None,
        "radius": 500,
    }
