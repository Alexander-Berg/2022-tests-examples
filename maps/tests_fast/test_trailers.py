import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_anchor():
    """
    checks anchors
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("trailer_and_anchor.json"),
        tools.get_test_data("trailer_and_anchor_distances.json"),
        solver_arguments={'sa_iterations': 1000000},
        expected_status="SOLVED"
    )


def test_no_anchor():
    """
    checks the same promblem without the anchor location
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("trailer_and_no_anchor.json"),
        tools.get_test_data("trailer_and_anchor_distances.json"),
        solver_arguments={'sa_iterations': 1000000},
        expected_status="PARTIAL_SOLVED"
    )


def test_parking():
    """
    In this test we need to serve one location with trailer, use parking for
    the other location and drop location which is too far away from parking.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('parking.json'),
        solver_arguments={'sa_iterations': 50000},
        expected_status='PARTIAL_SOLVED',
        expected_metrics={
            'dropped_locations_count': 1,
            'total_drop_penalty': 10000
        }
    )


def test_parking_same_point():
    """
    Checks that there are no unnecessary parkings in case of multiorders.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('parking_same_point.json'),
        solver_arguments={'sa_iterations': 50000}
    )

    route = result['routes'][0]['route']
    parking_cnt = 0
    for node in route:
        loc = node['node']['value']
        if 'type' in loc and loc['type'] == 'parking':
            parking_cnt += 1
    assert parking_cnt == 2


def test_parking_weight():
    """
    Checks that if locations with allow_trailers == false don't fit in the head, we drop them.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('parking_weight.json'),
        solver_arguments={'sa_iterations': 50000},
        expected_status='PARTIAL_SOLVED',
        expected_metrics={
            'dropped_locations_count': 1,
        }
    )


def test_everything_together():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('trailers.json'),
        solver_arguments={'sa_iterations': 1000000},
        expected_status='SOLVED'
    )

    route = result['routes'][0]['route']
    parking_cnt = 0
    anchor_decoupling_cnt = 0
    anchor_rolling_cnt = 0
    anchor_coupling_cnt = 0
    for i in range(len(route)):
        node = route[i]
        loc = node['node']['value']
        if i == 0 or ('type' in loc and loc['type'] == 'anchor' and loc.get('anchor_mode') == 'Rolling'):
            if i == 0:
                assert node['node']['type'] == 'depot'
            assert 'load_to_head' in node
            if i == 0:
                assert 'load_to_trailer' in node
        if 'type' in loc and loc['type'] == 'parking':
            parking_cnt += 1
        if 'type' in loc and loc['type'] == 'anchor':
            assert 'anchor_mode' in loc and loc['anchor_mode'] in ['Decoupling', 'Coupling', 'Rolling']
            if loc['anchor_mode'] == 'Decoupling':
                anchor_decoupling_cnt += 1
            elif loc['anchor_mode'] == 'Coupling':
                anchor_coupling_cnt += 1
            elif loc['anchor_mode'] == 'Rolling':
                anchor_rolling_cnt += 1
    assert parking_cnt == 2
    assert anchor_decoupling_cnt == 1
    assert anchor_coupling_cnt == 1
    assert anchor_rolling_cnt == 1


def test_loading_plan_overload_trailer():
    """
    Orders with allow_trailers == True don't fit in trailer, so we have to put one of them in the head.
    """
    task = tools.get_test_json('trailers.json')
    task['locations'][2]['allow_trailers'] = True
    task['locations'][3]['shipment_size']['weight_kg'] = 7
    task['locations'][4]['shipment_size']['weight_kg'] = 1
    task['locations'][5]['shipment_size']['weight_kg'] = 1
    task['locations'][6]['shipment_size']['weight_kg'] = 1

    result = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 100000},
    )

    depot = result['routes'][0]['route'][0]
    assert 'load_to_head' in depot
    assert (2 in depot['load_to_head']) != (3 in depot['load_to_head'])


def test_loading_plan_all_rolling():
    """
    At most one order can fit in the trailer, so we have to roll before each delivery.
    """
    task = tools.get_test_json('trailers.json')
    for loc in task['locations'][2:]:
        loc['allow_trailers'] = False
        loc['shipment_size']['weight_kg'] = 5
    vehicle = task['vehicles'][0]
    vehicle['capacity']['weight_kg'] = 5
    vehicle['trailer']['capacity']['weight_kg'] = 20

    result = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 100000},
    )

    route = result['routes'][0]['route']
    trailer = route[0]['load_to_trailer']
    head = -1
    for node in route:
        if 'load_to_head' in node:
            assert len(node['load_to_head']) == 1
            head = node['load_to_head'][0]
            if node['node'].get('type') != 'depot':
                assert head == trailer[0]
                del trailer[0]
        loc = node['node']['value']
        if loc.get('type') == 'delivery':
            assert loc['id'] == head


def test_loading_plan_split():
    """
    One of the orders has to be split between head and trailer to fit.
    """
    task = tools.get_test_json('trailers.json')
    for loc in task['locations'][2:]:
        loc['allow_trailers'] = False
        loc['shipment_size']['weight_kg'] = 4

    result = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 100000},
    )

    route = result['routes'][0]['route']
    head = set(route[0]['load_to_head'])
    trailer = set(route[0]['load_to_trailer'])
    assert len(head) == 3
    assert len(trailer) == 3
    for idx in trailer:
        if idx in head:
            head.remove(idx)
    assert len(head) == 2
    for node in route:
        if 'load_to_head' in node:
            for idx in node['load_to_head']:
                head.add(idx)
        loc = node['node']['value']
        if loc.get('type') == 'delivery':
            assert loc['id'] in head
            head.remove(loc['id'])
