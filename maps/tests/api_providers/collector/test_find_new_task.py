import pytest
from marshmallow import ValidationError

from maps_adv.stat_controller.server.lib.api_providers import Collector
from maps_adv.stat_controller.server.tests.tools import coro_mock, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain():
    class Domain:
        find_new = coro_mock()

    return Domain()


@pytest.fixture
def provider(domain):
    return Collector(domain)


@pytest.mark.parametrize("value", ("", None))
async def test_raises_for_invalid_input(value, provider):
    with pytest.raises(ValidationError):
        await provider.find_new(executor_id=value)


async def test_returns_task_data(domain, provider):
    domain.find_new.coro.return_value = {
        "timing_from": dt("2019-05-02 01:19:30"),
        "timing_to": dt("2019-05-02 01:29:30"),
        "id": 1,
    }
    got = await provider.find_new(executor_id="sample_executor_id")

    assert got == {
        "timing_from": "2019-05-02T01:19:30+00:00",
        "timing_to": "2019-05-02T01:29:30+00:00",
        "id": 1,
    }
