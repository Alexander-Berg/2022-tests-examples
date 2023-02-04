import pytest

from intranet.trip.src.lib.staff.gateway import has_fallback_assignment


pytestmark = pytest.mark.asyncio


def get_assignment_staff_response(name, value):
    return {
        'login': {
            'choices': [{
                'name': name,
                'value': value,
            }],
        },
    }


@pytest.mark.parametrize('staff_response, expected_result', (
    ({}, True),
    (get_assignment_staff_response('no assignment', -999), True),
    (get_assignment_staff_response('assignment', 12345), False),
))
async def test_has_fallback_assignment(staff_response, expected_result):
    assert has_fallback_assignment(staff_response) == expected_result
