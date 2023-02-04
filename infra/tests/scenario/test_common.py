import pytest

from infra.walle.server.tests.lib.util import monkeypatch_function
from walle.clients import calendar
from walle.scenario import common

MOCK_CALENDAR_RESPONSE = {
    'holidays': [
        {'date': '2021-02-20', 'type': 'holiday'},
        {'date': '2021-02-21', 'type': 'holiday'},
        {'date': '2021-02-22', 'type': 'holiday'},
        {'date': '2021-11-04', 'type': 'weekend'},
        {'date': '2021-11-05', 'type': 'weekend'},
        {'date': '2021-11-06', 'type': 'weekend'},
        {'date': '2021-11-07', 'type': 'weekend'},
        {'date': '2021-11-13', 'type': 'weekend'},
        {'date': '2021-11-14', 'type': 'weekend'},
        {'date': '2021-11-20', 'type': 'weekend'},
        {'date': '2021-11-21', 'type': 'weekend'},
        {'date': '2021-11-27', 'type': 'weekend'},
        {'date': '2021-11-28', 'type': 'weekend'},
        {'date': '2021-12-04', 'type': 'weekend'},
        {'date': '2021-12-05', 'type': 'weekend'},
    ]
}


@pytest.mark.parametrize(
    ["maintenance_start_time", "result"],
    [
        (1614157200, 1613638800.0),  # (2021-02-24 12:00:00, 2021-02-18 12:00:00)
        (1636448400, 1635757200.0),  # (2021-11-09 12:00:00, 2021-11-01 12:00:00)
        (1639040400, 1638781200.0),  # (2021-12-09 12:00:00 , 2021-12-06 12:00:00)
    ],
)
def get_starting_time_using_yp_sla_for_host_unloading(mp, maintenance_start_time, result):
    monkeypatch_function(mp, calendar.get_holidays, return_value=MOCK_CALENDAR_RESPONSE)
    assert common.get_request_to_maintenance_starting_time_using_yp_sla(maintenance_start_time) == result
