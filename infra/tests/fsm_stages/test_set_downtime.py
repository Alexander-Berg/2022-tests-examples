"""Tests setting downtime for the host in Juggler."""

from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
    monkeypatch_config,
    any_task_status,
    monkeypatch_method,
    mock_retry_current_stage,
)
from walle.clients import juggler
from walle.clients.juggler import JugglerDowntimeName, JugglerDowntimeTypes
from walle.fsm_stages.common import get_current_stage
from walle.fsm_stages.set_downtime import STAGE_RETRY_INTERVAL, ERROR_TEMPLATE
from walle.hosts import HostState
from walle.stages import Stages, Stage
from walle.util.misc import drop_none

MAX_RETRY_COUNT = 2


@pytest.fixture()
def test(request, mp, monkeypatch_timestamp, mp_juggler_source):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.SET_DOWNTIME))


@pytest.mark.parametrize("on_downtime", (True, False, None))
@pytest.mark.parametrize("side_effect", (None, Exception("mocked error")))
@pytest.mark.parametrize("juggler_downtime_name", JugglerDowntimeName.ALL)
@pytest.mark.parametrize("initial_downtime_ids", ([1], [1, 2, 3], []))
@pytest.mark.parametrize("retry_count", [None, 0, 1])
def test_set(mp, test, on_downtime, side_effect, juggler_downtime_name, initial_downtime_ids, retry_count):
    monkeypatch_method(
        mp,
        juggler.JugglerClient.get_fqdn_to_downtimes_map,
        obj=juggler.JugglerClient,
        return_value={1: initial_downtime_ids},
    )
    monkeypatch_config(mp, "set_downtime.max_retries_count", 2)
    mock_juggler_api_client = Mock(**{"set_downtimes.side_effect": side_effect})
    mp.function(juggler._get_juggler_client, return_value=mock_juggler_api_client)

    stage_data = {"retries": retry_count} if retry_count is not None else None

    host = test.mock_host(
        dict(
            inv=1,
            state=HostState.ASSIGNED,
            status=any_task_status(),
            on_downtime=on_downtime,
            task=mock_task(
                stage=Stages.SET_DOWNTIME,
                stage_data=stage_data,
                stage_params=drop_none({"juggler_downtime_name": juggler_downtime_name}),
            ),
        )
    )

    handle_host(host)

    dt_params = JugglerDowntimeTypes.by_suffix_name(juggler_downtime_name)
    mock_juggler_api_client.set_downtimes.assert_called_once_with(
        filters=[juggler.sdk.DowntimeSelector(host=host.name)],
        description="Wall-E is processing '{}' task on the host".format(host.status),
        end_time=dt_params.end_time,
        source="wall-e.unittest.{}".format(juggler_downtime_name),
    )

    if not side_effect:
        host.on_downtime = True
        mock_complete_current_stage(host, inc_revision=1)
    else:
        retries = 1 if not retry_count else retry_count + 1
        get_current_stage(host).set_data("retries", retries)
        if retries < MAX_RETRY_COUNT:
            mock_retry_current_stage(
                host, Stages.SET_DOWNTIME, error=ERROR_TEMPLATE.format("mocked error"), check_after=STAGE_RETRY_INTERVAL
            )
        else:
            mock_complete_current_stage(host)

    test.hosts.assert_equal()
