from datetime import timedelta

import pytest

from maps_adv.common.helpers import coro_mock, dt
from maps_adv.statistics.beekeeper.lib.steps import (
    ChargesCalculator,
    ContextCollector,
    FreeEventsPorter,
    PaidEventsPorter,
)


@pytest.fixture
def packet_size_calculator_mock():
    mock = coro_mock()
    mock.coro.return_value = {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
    }

    return mock


@pytest.fixture
def context_collector_step(
    packet_size_calculator_mock, adv_store_client_mock, billing_client_mock, ch_config
):
    return ContextCollector(
        packet_size_calculator=packet_size_calculator_mock,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        ch_client_params=ch_config,
    )


@pytest.fixture
def charges_calculator_step():
    return ChargesCalculator()


@pytest.fixture
def paid_events_porter_step(ch_config):
    return PaidEventsPorter(
        ch_client_params=ch_config, ch_max_memory_usage=10 * 1024 * 1024 * 1024
    )


@pytest.fixture
def free_events_porter_step(ch_config):
    return FreeEventsPorter(
        ch_client_params=ch_config,
        ch_max_memory_usage=10 * 1024 * 1024 * 1024,
        event_group_id_time_threshold=timedelta(minutes=30),
    )
