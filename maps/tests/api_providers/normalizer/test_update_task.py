import pytest
from marshmallow import ValidationError

from maps_adv.stat_controller.server.lib.api_providers import Normalizer
from maps_adv.stat_controller.server.tests.tools import coro_mock, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain():
    class Domain:
        update = coro_mock()

    return Domain()


@pytest.fixture
def provider(domain):
    return Normalizer(domain)


@pytest.mark.parametrize(
    "update",
    (
        {"task_id": ""},
        {"task_id": None},
        {"status": ""},
        {"status": None},
        {"status": "bldjad"},
        {"executor_id": ""},
        {"executor_id": None},
    ),
)
async def test_raises_for_invalid_input(update, provider):
    data = {"task_id": 10, "status": "data_received", "executor_id": 20}
    data.update(update)

    with pytest.raises(ValidationError):
        await provider.update(**data)


async def test_returns_task_data(domain, provider):
    domain.update.coro.return_value = {
        "id": 1,
        "timing_from": dt("2019-05-06 01:00:00"),
        "timing_to": dt("2019-05-06 01:05:00"),
    }
    got = await provider.update(task_id=1, status="completed", executor_id="lolkek")

    assert got == {
        "id": 1,
        "timing_from": "2019-05-06T01:00:00+00:00",
        "timing_to": "2019-05-06T01:05:00+00:00",
    }
