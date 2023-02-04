from datetime import timedelta
from unittest.mock import Mock

import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.statistics.beekeeper.lib.tasks import events_processor

pytestmark = [pytest.mark.asyncio]


async def test_runs_pipeline_for_processing_of_events(
    config, ch_config, adv_store_client_mock, billing_client_mock, mocker
):
    charger = Mock()
    charger.return_value = coro_mock()
    mocker.patch(
        "maps_adv.statistics.beekeeper.lib.tasks.pipelines.create_charger", charger
    )
    some_obj = object()

    await events_processor(
        some_obj,
        config=config,
        ch_config=ch_config,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        build_revision=132,
    )

    charger.assert_called_once_with(
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        lag_packet_size=timedelta(seconds=90),
        max_packet_size=timedelta(seconds=360),
        min_packet_size=timedelta(seconds=180),
        time_threshold_free_events=timedelta(seconds=45),
        campaigns_for_processing=None,  # GEOPROD-4108
        ch_config=ch_config,
        build_revision=132,
        ch_max_memory_usage=1234567890,
        ch_query_id=config["CH_PROCESSOR_QUERY_ID"],
    )
    charger.return_value.coro.assert_called_once_with(some_obj)


async def test_does_not_raise_if_run_task_with_additional_params(
    config, ch_config, adv_store_client_mock, billing_client_mock, mocker
):
    charger = Mock()
    charger.return_value = coro_mock()
    mocker.patch(
        "maps_adv.statistics.beekeeper.lib.tasks.pipelines.create_charger", charger
    )

    await events_processor(
        None,
        config=config,
        ch_config=ch_config,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        imposible_additional_param="param",
    )
