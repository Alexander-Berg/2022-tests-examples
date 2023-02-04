import pytest
from marshmallow import ValidationError

from maps_adv.stat_controller.server.lib.api_providers import Charger
from maps_adv.stat_controller.server.lib.domains import ChargerStatus
from maps_adv.stat_controller.server.tests.tools import coro_mock, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain():
    class Domain:
        find_new = coro_mock()

    return Domain()


@pytest.fixture
def provider(domain):
    return Charger(domain)


async def test_returns_task_data(domain, provider):
    domain.find_new.coro.return_value = {
        "id": 1,
        "timing_from": dt("1999-05-05 04:40:00"),
        "timing_to": dt("1999-05-05 04:59:31"),
        "status": ChargerStatus.accepted,
    }

    got = await provider.find_new(executor_id="executor0")

    assert got == {
        "id": 1,
        "timing_from": "1999-05-05T04:40:00+00:00",
        "timing_to": "1999-05-05T04:59:31+00:00",
        "status": "accepted",
    }


async def test_returns_reanimated_task_data(domain, provider):
    domain.find_new.coro.return_value = {
        "id": 1,
        "timing_from": dt("1999-05-05 04:40:00"),
        "timing_to": dt("1999-05-05 04:59:31"),
        "execution_state": "some_state",
        "status": ChargerStatus.context_received,
    }

    got = await provider.find_new(executor_id="executor0")

    assert got == {
        "id": 1,
        "timing_from": "1999-05-05T04:40:00+00:00",
        "timing_to": "1999-05-05T04:59:31+00:00",
        "status": "context_received",
        "execution_state": "some_state",
    }


@pytest.mark.parametrize("value", ("", None))
async def test_raises_for_invalid_input(value, provider):
    with pytest.raises(ValidationError):
        await provider.find_new(executor_id=value)
