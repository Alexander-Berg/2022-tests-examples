from datetime import timedelta
from unittest.mock import Mock

import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.statistics.beekeeper.lib.tasks import events_normalizer_mapkit

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
        "maps_adv.statistics.beekeeper.lib.tasks.MapkitAndAppMetricNormalizerTask",
        normalizer,
    )
    context = Mock()
    context.client = warden_client_mock

    await events_normalizer_mapkit(context, config=config, ch_config=ch_config)

    normalizer.assert_called_once_with(
        max_packet_size=timedelta(seconds=360),
        min_packet_size=timedelta(seconds=180),
        lag=timedelta(seconds=90),
        deduplication_window=timedelta(seconds=240),
        ch_client_params=ch_config,
        app_filter={"ios_maps_build": 201},
        recognised_apps={
            "ru.yandex.yandexnavi": "NAVIGATOR",
            "ru.yandex.yandexmaps": "MOBILE_MAPS",
            "ru.yandex.mobile.navigator": "NAVIGATOR",
            "ru.yandex.traffic": "MOBILE_MAPS",
        },
        normalized_events_table_name="kek-table-name",
        ch_query_id=config["CH_NORMALIZER_QUERY_ID"],
    )
    normalizer.return_value.coro.assert_called_once_with(warden_client_mock)


async def test_does_not_raise_if_run_task_with_additional_params(
    config, ch_config, mocker
):
    normalizer = Mock()
    normalizer.return_value = coro_mock()
    mocker.patch(
        "maps_adv.statistics.beekeeper.lib.tasks.MapkitAndAppMetricNormalizerTask",
        normalizer,
    )

    await events_normalizer_mapkit(
        Mock(), config=config, ch_config=ch_config, imposible_additional_param="param"
    )
