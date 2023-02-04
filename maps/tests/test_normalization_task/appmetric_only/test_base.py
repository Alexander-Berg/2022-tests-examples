from datetime import timedelta

import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.statistics.beekeeper.lib.normalizer import NoNewData


@pytest.fixture
def calculate_packet_bounds_mock(mocker):
    mock = mocker.patch(
        "maps_adv.statistics.beekeeper.lib.normalizer.AppMetricOnlyNormalizerTask._calculate_packet_bounds",  # noqa: E501
        new_callable=coro_mock,
    )

    return mock.coro


@pytest.mark.asyncio
async def test_sends_intermediate_data_to_warden(
    calculate_packet_bounds_mock, warden_client_mock, task_factory
):
    calculate_packet_bounds_mock.return_value = (123, 456)

    task = task_factory()
    await task(warden_client=warden_client_mock)

    assert warden_client_mock.update_status.call_args[1] == dict(
        status="packet_bounds_calculated",
        metadata={"packet_start": 123, "packet_end": 456},
    )
    warden_client_mock.update_status.assert_called_with(
        status="packet_bounds_calculated",
        metadata={"packet_start": 123, "packet_end": 456},
    )


@pytest.mark.asyncio
async def test_not_sends_intermediate_data_to_warden_if_no_data_to_process(
    calculate_packet_bounds_mock, warden_client_mock, task_factory
):
    calculate_packet_bounds_mock.return_value = None

    task = task_factory()
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)

    warden_client_mock.update_status.assert_not_called()


@pytest.mark.parametrize("param", ["min_packet_size", "min_packet_size"])
@pytest.mark.parametrize("value", [timedelta(days=0), timedelta(days=-1)])
def test_raises_on_nonpositive_packet_size(task_factory, param, value):
    with pytest.raises(ValueError):
        task_factory(**{param: value})


def test_raises_on_negative_lag(task_factory):
    with pytest.raises(ValueError):
        task_factory(lag=timedelta(seconds=-2))


def test_raises_on_min_packet_size_gt_than_max_packet_size(task_factory):
    with pytest.raises(ValueError):
        task_factory(
            min_packet_size=timedelta(seconds=10), max_packet_size=timedelta(seconds=5)
        )
