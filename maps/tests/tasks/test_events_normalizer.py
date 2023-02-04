from datetime import timedelta
from unittest.mock import Mock

import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.statistics.beekeeper.lib.tasks import events_normalizer

pytestmark = [pytest.mark.asyncio]


async def test_runs_events_normalizer_as_expected(
    config,
    ch_config,
    adv_store_client_mock,
    billing_client_mock,
    mocker,
    warden_client_mock,
):
    normalizer = Mock()
    normalizer.return_value = coro_mock()
    mocker.patch(
        "maps_adv.statistics.beekeeper.lib.tasks.AppMetricOnlyNormalizerTask",
        normalizer,
    )
    context = Mock()
    context.client = warden_client_mock

    await events_normalizer(context, config=config, ch_config=ch_config)

    normalizer.assert_called_once_with(
        max_packet_size=timedelta(seconds=360),
        min_packet_size=timedelta(seconds=180),
        lag=timedelta(seconds=90),
        ch_client_params=ch_config,
        ch_query_id=config["CH_NORMALIZER_QUERY_ID"],
    )
    normalizer.return_value.coro.assert_called_once_with(warden_client_mock)


async def test_does_not_raise_if_run_task_with_additional_params(
    config, ch_config, mocker
):
    normalizer = Mock()
    normalizer.return_value = coro_mock()
    mocker.patch(
        "maps_adv.statistics.beekeeper.lib.tasks.AppMetricOnlyNormalizerTask",
        normalizer,
    )

    await events_normalizer(
        Mock(), config=config, ch_config=ch_config, imposible_additional_param="param"
    )
