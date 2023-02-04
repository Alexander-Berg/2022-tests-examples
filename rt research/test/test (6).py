# coding: utf-8

import datetime
import mock
import responses

from irt.monitoring.libs.money_wow_statistics import get_wow_statistics, get_stat_solomon_sensors, evaluate_diff


@responses.activate
def test_main():
    responses.add(
        responses.POST,
        "https://artmon.bsadm.yandex-team.ru/cgi-bin/data.cgi",
        status=200,
        json={
            "items": {
                "rows": [{"cost": 12345, "utc": 1588798800000 + 300000 * x, "series_id": "bsclickhouse,700333"} for x in range(5)],
                "compared": [{"cost": 67890, "utc": 1588194000000 + 300000 * x, "series_id": "bsclickhouse,700333"} for x in range(5)],
            },
        }
    )

    with mock.patch('datetime.datetime', wraps=datetime.datetime, now=lambda: datetime.datetime(2021, 5, 1, 12)):
        value_diff, monitor_all_sds, iso_timestamp = get_wow_statistics("")
        ok_diff = evaluate_diff(value_diff)
        assert len(get_stat_solomon_sensors(value_diff, ok_diff, monitor_all_sds, iso_timestamp)) > 0
