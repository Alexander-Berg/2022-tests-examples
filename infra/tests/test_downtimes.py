"""Tests Juggler downtimes checking."""

from unittest.mock import call, Mock, ANY

import juggler_sdk
import juggler_sdk.downtimes as downtimes_sdk
import pytest

import walle.downtimes
from infra.walle.server.tests.lib.util import TestCase, any_task_status, any_steady_status
from sepelib.core.constants import DAY_SECONDS
from walle.clients import juggler
from walle.hosts import HostState
from walle.network import BlockedHostName
from walle.stages import Stages


@pytest.fixture
def test(request, monkeypatch_timestamp, mp, mp_juggler_source):
    monkeypatch_timestamp(mp, cur_time=0)
    return TestCase.create(request)


def test_fix_downtimes(mp, test, monkeypatch_locks):
    steady_status = any_steady_status()
    task_status = any_task_status()

    test.mock_host(dict(inv=0, name=None, state=HostState.FREE))
    test.mock_host(dict(inv=1, name=None, state=HostState.FREE, status=task_status, on_downtime=True))
    test.mock_host(dict(inv=2, name=None, state=HostState.FREE, status=task_status))

    test.mock_host(dict(inv=3, state=HostState.ASSIGNED, status=steady_status))
    test.mock_host(dict(inv=4, state=HostState.ASSIGNED, status=steady_status, on_downtime=True))  # error_downtime
    juggler_on_steady = test.mock_host(dict(inv=5, state=HostState.ASSIGNED, status=steady_status))

    test.mock_host(dict(inv=6, state=HostState.ASSIGNED, status=task_status))
    not_downtimed_task = test.mock_host(dict(inv=7, state=HostState.ASSIGNED, status=task_status, on_downtime=True))
    downtimed_task = test.mock_host(dict(inv=8, state=HostState.ASSIGNED, status=task_status, on_downtime=True))
    test.mock_host(
        dict(inv=9, state=HostState.ASSIGNED, status=task_status, on_downtime=True),
        task_kwargs=dict(stage=Stages.ALLOCATE_HOSTNAME),
    )  # downtimed_renaming

    host_not_in_walle = "mock-hostname-not-walle-host"
    host_not_in_walle_but_blocked = "mock-hostname-not-walle-but-blocked"
    BlockedHostName.store(host_not_in_walle_but_blocked)

    downtimes = downtimes_sdk.GetDowntimesResponse(
        [
            _mock_downtime("downtime-on-steady", juggler_on_steady.name),
            _mock_downtime("downtime-task", downtimed_task.name),
            _mock_downtime("downtime-not-in-wall-e", host_not_in_walle),
            _mock_downtime("downtime-blocked", host_not_in_walle_but_blocked),
            _mock_downtime("downtime-not-in-wall-e", juggler_on_steady.name),
            _mock_downtime(
                "downtime-need-to-extend",
                downtimed_task.name,
                end_time=walle.downtimes._ENDING_TIME_PERIOD - DAY_SECONDS,
            ),
            _mock_downtime(
                "downtime-dont-need-to-extend",
                downtimed_task.name,
                end_time=walle.downtimes._ENDING_TIME_PERIOD + DAY_SECONDS,
            ),
        ],
        page=1,
        page_size=100,
        total=4,
    )

    mock_juggler_api_client = Mock()
    mock_juggler_api_client.attach_mock(Mock(return_value=downtimes), "get_downtimes")
    mock_juggler_api_client.attach_mock(Mock(), "set_downtimes")
    mock_juggler_api_client.attach_mock(Mock(), "remove_downtimes")

    mp.function(juggler._get_juggler_client, return_value=mock_juggler_api_client)

    walle.downtimes._gc_jobs()

    mock_juggler_api_client.set_downtimes.assert_has_calls(
        [
            call(
                downtime_id="downtime-need-to-extend",
                end_time=juggler.JugglerDowntimeTypes.DEFAULT.end_time,
                description=None,
                filters=[juggler_sdk.DowntimeSelector(host=downtimed_task.name)],
                source=None,
            )
        ]
    )

    mock_juggler_api_client.set_downtimes.assert_has_calls(
        [
            call(
                filters=[downtimes_sdk.DowntimeSelector(host=not_downtimed_task.name)],
                source="wall-e.unittest.{}".format(juggler.JugglerDowntimeName.DEFAULT),
                description=ANY,
                end_time=juggler.JugglerDowntimeTypes.DEFAULT.end_time,
            )
        ]
    )

    assert mock_juggler_api_client.remove_downtimes.mock_calls == [
        call(["downtime-on-steady", "downtime-not-in-wall-e"])
    ]

    test.hosts.assert_equal()


def _mock_downtime(downtime_id, host_name, end_time=None):
    return downtimes_sdk.Downtime(
        downtime_id,
        [downtimes_sdk.DowntimeSelector(host=host_name)],
        end_time=end_time,
        start_time=None,
        description=None,
        source=None,
    )
