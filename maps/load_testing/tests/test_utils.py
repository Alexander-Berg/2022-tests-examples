import yatest.common
import json
import random

from maps.b2bgeo.tools.load_testing.lib.parametrization import (
    OneOf, Linspace, RoutingPublicApiPoints, Settings, Parametrized, RequestsProvider
)


def _get_value_space(cls, *args, **kwargs):
    t = cls(*args, **kwargs)
    results = set()
    for _ in range(200):
        results.add(t.get())
    return results


def test_simple_generators():
    random.seed(0)
    assert _get_value_space(OneOf, values=[1, 2, 3]) == {1, 2, 3}
    assert _get_value_space(OneOf, values=[1, 2, 3], weights=[0.75, 0, 0.25]) == {1, 3}

    assert _get_value_space(Linspace, min_value=1, max_value=4, step=1) == {1, 2, 3, 4}
    assert _get_value_space(Linspace, min_value=1, max_value=4, step=1, weights=[0.25, 0.5, 0, 0.25]) == {1, 2, 4}

    t = Settings(str_template="test-{ABC}-{DEF}")
    assert t.get(settings={'ABC': 1, 'DEF': 2}) == "test-1-2"


def test_distance_matrix_points_generator():
    random.seed(0)
    # either one or two points will be generated:
    expected_values = {"10.123000,20.123000", "10.123000,20.123000|10.123000,20.123000"}
    assert (_get_value_space(RoutingPublicApiPoints,
                             linspace_lat=Linspace(10.123, 10.123, 1),
                             linspace_lon=Linspace(20.123, 20.123, 1),
                             linspace_amount=Linspace(1, 2, 1))
            == expected_values)


def test_nested_generators():
    template = {
        'hardcoded': 'values',
        'payload': OneOf(
            values=[
                Settings("test-{A}"),
                Linspace(min_value=1, max_value=2, step=1)
            ]
        )
    }
    requests_provider = RequestsProvider(template)

    settings = {
        'A': 42
    }
    requests = [requests_provider.next_request(settings) for _ in range(25)]

    expected_possible_values = [
        {
            'hardcoded': 'values',
            'payload': "test-42"
        },
        {
            'hardcoded': 'values',
            'payload': 1
        },
        {
            'hardcoded': 'values',
            'payload': 2
        }
    ]
    assert all([request in expected_possible_values for request in requests])
    assert all([possible_value in requests for possible_value in expected_possible_values])


def _generator_eq(lhs, rhs):
    if type(lhs) != type(rhs):
        return False

    if isinstance(lhs, Parametrized):
        lhs_params = lhs.as_dict()
        rhs_params = rhs.as_dict()
        return _generator_eq(lhs_params, rhs_params)
    elif isinstance(lhs, dict):
        return len(lhs) == len(rhs) and all([k in rhs and _generator_eq(v, rhs[k]) for k, v in lhs.items()])
    elif isinstance(lhs, (list, tuple)):
        return len(lhs) == len(rhs) and all([_generator_eq(pair[0], pair[1]) for pair in zip(lhs, rhs)])
    else:
        return lhs == rhs


def test_deserialization():
    distance_matrix_scenario_path = \
        yatest.common.source_path('maps/b2bgeo/tools/load_testing/scenarios/distancematrix.json')

    expected_points_generator = RoutingPublicApiPoints(
        linspace_lat=Linspace(55.739552, 55.765610, 0.000026),
        linspace_lon=Linspace(37.596836, 37.631127, 0.001),
        linspace_amount=Linspace(1, 10, 1)
    )

    expected = {
        'method': 'GET',
        'timeout': 5,
        'url': Settings("https://{backend}/v2/distancematrix"),
        'verify': False,
        'params': {
            'origins': expected_points_generator,
            'destinations': expected_points_generator,
            'apikey': Settings("{apikey}"),
            'mode': OneOf(["driving", "walking", "transit"])
        }
    }

    with open(distance_matrix_scenario_path, 'r') as fin:
        j = json.load(fin)

    assert _generator_eq(expected, RequestsProvider.load(j).template)
