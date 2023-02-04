from ya_courier_backend.logic.route_plan import _check_route_sequence
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote

import pytest
import werkzeug.exceptions


node_type_mapping = {
    'g': 'garage',
    'd': 'depot',
    'o': 'order'
}


def _generate_route(node_types):
    route = []
    for node_type in node_types:
        route.append({"type": node_type_mapping[node_type]})
    return route


@skip_if_remote
def test_check_route_sequence():
    route = _generate_route(['g', 'd', 'o', 'o', 'd', 'g'])
    _check_route_sequence(route)

    route = _generate_route(['g', 'o', 'o', 'g'])
    _check_route_sequence(route)

    route = _generate_route(['g', 'o', 'd'])
    _check_route_sequence(route)

    route = _generate_route(['d', 'o', 'g'])
    _check_route_sequence(route)

    route = _generate_route(['g', 'd', 'd', 'g'])
    _check_route_sequence(route)

    route = _generate_route(['g', 'd', 'o'])
    _check_route_sequence(route)

    route = _generate_route(['g', 'd'])
    _check_route_sequence(route)

    route = _generate_route(['g', 'd', 'g'])
    _check_route_sequence(route)

    route = _generate_route(['o', 'g', 'd'])
    with pytest.raises(werkzeug.exceptions.UnprocessableEntity):
        _check_route_sequence(route)

    route = _generate_route(['d', 'g', 'o'])
    with pytest.raises(werkzeug.exceptions.UnprocessableEntity):
        _check_route_sequence(route)

    route = _generate_route(['o', 'd', 'o'])
    with pytest.raises(werkzeug.exceptions.UnprocessableEntity):
        _check_route_sequence(route)

    route = _generate_route(['o', 'd', 'd'])
    with pytest.raises(werkzeug.exceptions.UnprocessableEntity):
        _check_route_sequence(route)

    route = _generate_route(['d', 'd', 'o'])
    with pytest.raises(werkzeug.exceptions.UnprocessableEntity):
        _check_route_sequence(route)
