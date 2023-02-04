from datetime import timedelta

import pytest

from maps_adv.statistics.beekeeper.lib.normalizer import (
    AppMetricOnlyNormalizerTask,
    MapkitAndAppMetricNormalizerTask,
)


@pytest.fixture
def task_factory(request, config):
    task_class = AppMetricOnlyNormalizerTask
    ext_params = {}

    mark_mapkit = request.node.get_closest_marker("mapkit")
    if mark_mapkit:
        task_class = MapkitAndAppMetricNormalizerTask
        ext_params = {
            "app_filter": {},
            "recognised_apps": {
                "ru.yandex.yandexnavi": "NAVIGATOR",
                "ru.yandex.yandexmaps": "MOBILE_MAPS",
                "ru.yandex.mobile.navigator": "NAVIGATOR",
                "ru.yandex.traffic": "MOBILE_MAPS",
            },
            "deduplication_window": timedelta(seconds=10),
        }

    def func(**params_overrides):
        params = {
            "min_packet_size": timedelta(seconds=1),
            "max_packet_size": timedelta(seconds=60 * 20),
            "lag": timedelta(seconds=1),
            "ch_client_params": {"host": "localhost", "port": 9001},
            "ch_query_id": config["CH_NORMALIZER_QUERY_ID"],
        }
        params.update(**ext_params)
        params.update(**params_overrides)

        return task_class(**params)

    return func
