"""Tests host powering on/off."""

from unittest.mock import call

import pytest

import walle.util.tasks
from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    monkeypatch_clients_for_host,
    mock_commit_stage_changes,
    check_stage_initialization,
    mock_complete_current_stage,
    mock_retry_parent_stage,
    mock_status_reasons,
    mock_complete_parent_stage,
    mock_schedule_host_profiling,
    monkeypatch_audit_log,
)
from walle.clients import ipmiproxy
from walle.clients.eine import EineProfileTags
from walle.expert import juggler
from walle.expert.constants import JUGGLER_PUSH_PERIOD
from walle.expert.types import CheckType, CheckStatus
from walle.fsm_stages import power_ipmi, power_post_util
from walle.fsm_stages.common import get_current_stage
from walle.models import timestamp
from walle.stages import Stage, Stages
from walle.util.misc import drop_none


@pytest.fixture
def test(request, monkeypatch_timestamp, mp):
    monkeypatch_audit_log(mp)
    return TestCase.create(request)


# Stage initialization


def test_initiate_power_off(test):
    check_stage_initialization(test, Stage(name=Stages.POWER_OFF), status=power_ipmi._STATUS_POWER_OFF)


def test_initiate_power_off_soft(test):
    check_stage_initialization(
        test, Stage(name=Stages.POWER_OFF, params={"soft": True}), status=power_ipmi._STATUS_SOFT_POWER_OFF
    )


def test_initiate_power_on(test):
    check_stage_initialization(test, Stage(name=Stages.POWER_ON), status=power_ipmi._STATUS_POWER_ON)


# Soft power off processing

_SSH_CHECK_MOCK = {
    "type": CheckType.SSH,
    "timestamp": timestamp(),
    "status_mtime": timestamp(),
    "stale_timestamp": timestamp() + JUGGLER_PUSH_PERIOD,
    "effective_timestamp": timestamp(),
}


@pytest.mark.parametrize(
    ["reasons", "poweroff_timeout"],
    [
        (None, power_ipmi._POWER_ON_OF_TIMEOUT),
        (mock_status_reasons(check_status=CheckStatus.PASSED), power_ipmi._SOFT_POWER_OFF_TIMEOUT),
        (mock_status_reasons(check_status=CheckStatus.FAILED), power_ipmi._POWER_ON_OF_TIMEOUT),
    ]
    + [
        (
            mock_status_reasons(
                check_status=CheckStatus.FAILED, check_overrides=[dict(_SSH_CHECK_MOCK, status=status)]
            ),
            power_ipmi._SOFT_POWER_OFF_TIMEOUT,
        )
        for status in (CheckStatus.MISSING, CheckStatus.SUSPECTED, CheckStatus.PASSED)
    ],
)
def test_soft_power_off(test, mp, reasons, poweroff_timeout):
    mp.function(juggler.get_host_health_reasons, return_value=reasons)

    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(name=Stages.POWER_OFF, status=power_ipmi._STATUS_SOFT_POWER_OFF, params={"soft": True})
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on(), call.hardware.soft_power_off()]

    get_current_stage(host).set_temp_data("soft_poweroff_timeout", poweroff_timeout)
    mock_commit_stage_changes(
        host, status=power_ipmi._STATUS_SOFT_POWERING_OFF, check_after=power_ipmi._HARDWARE_CHECK_PERIOD
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize("soft_poweroff_timeout", [None, 1])
def test_soft_power_off_pending(test, mp, soft_poweroff_timeout):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_OFF,
                    status=power_ipmi._STATUS_SOFT_POWERING_OFF,
                    status_time=timestamp(),
                    params={"soft": True},
                    temp_data=drop_none({"soft_poweroff_timeout": soft_poweroff_timeout}),
                )
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_commit_stage_changes(host, check_after=power_ipmi._HARDWARE_CHECK_PERIOD)
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "soft_poweroff_timeout", [None, power_ipmi._POWER_ON_OF_TIMEOUT, power_ipmi._SOFT_POWER_OFF_TIMEOUT]
)
def test_soft_power_off_timeout(test, mp, soft_poweroff_timeout):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_OFF,
                    status=power_ipmi._STATUS_SOFT_POWERING_OFF,
                    status_time=timestamp() - (soft_poweroff_timeout or power_ipmi._POWER_ON_OF_TIMEOUT),
                    params={"soft": True},
                    temp_data=drop_none({"soft_poweroff_timeout": soft_poweroff_timeout}),
                )
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_commit_stage_changes(host, status=power_ipmi._STATUS_POWER_OFF, check_now=True)
    test.hosts.assert_equal()


def test_soft_power_off_with_broken_soft_power_off_command(test, mp):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(name=Stages.POWER_OFF, status=power_ipmi._STATUS_SOFT_POWER_OFF, params={"soft": True})
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)
    ipmi_client = ipmiproxy.get_client(ipmiproxy.get_yandex_internal_provider(test.ipmi_mac), host.human_id())
    ipmi_client.soft_power_off.side_effect = ipmiproxy.BrokenIpmiCommandError("Error mock.")

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on(), call.hardware.soft_power_off()]

    mock_commit_stage_changes(host, status=power_ipmi._STATUS_POWER_OFF, check_now=True)
    test.hosts.assert_equal()


# Power off processing


def test_power_off(test, mp):
    host = test.mock_host({"task": mock_task(stage=Stage(name=Stages.POWER_OFF, status=power_ipmi._STATUS_POWER_OFF))})
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on(), call.hardware.power_off()]

    mock_commit_stage_changes(
        host, status=power_ipmi._STATUS_POWERING_OFF, check_after=power_ipmi._HARDWARE_CHECK_PERIOD
    )
    test.hosts.assert_equal()


def test_power_off_pending(test, mp):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(name=Stages.POWER_OFF, status=power_ipmi._STATUS_POWERING_OFF, status_time=timestamp())
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_commit_stage_changes(host, check_after=power_ipmi._HARDWARE_CHECK_PERIOD)
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "status",
    (
        power_ipmi._STATUS_SOFT_POWER_OFF,
        power_ipmi._STATUS_SOFT_POWERING_OFF,
        power_ipmi._STATUS_POWER_OFF,
        power_ipmi._STATUS_POWERING_OFF,
    ),
)
def test_power_off_completed(test, mp, status):
    host = test.mock_host(
        {"task": mock_task(stage=Stage(name=Stages.POWER_OFF, status=status, status_time=timestamp()))}
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=False)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_complete_current_stage(host)
    test.hosts.assert_equal()


# Power on processing


@pytest.mark.parametrize("params", ({}, {"pxe": True}))
def test_power_on(test, mp, params):
    host = test.mock_host(
        {"task": mock_task(stage=Stage(name=Stages.POWER_ON, params=params, status=power_ipmi._STATUS_POWER_ON))}
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=False)

    handle_host(host)

    assert clients.mock_calls == [call.hardware.is_power_on(), call.hardware.power_on(pxe=params.get("pxe", False))]

    mock_commit_stage_changes(
        host, status=power_ipmi._STATUS_POWERING_ON, check_after=power_ipmi._HARDWARE_CHECK_PERIOD
    )
    test.hosts.assert_equal()


def test_power_on_pending(test, mp):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(name=Stages.POWER_ON, status=power_ipmi._STATUS_POWERING_ON, status_time=timestamp())
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=False)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_commit_stage_changes(host, check_after=power_ipmi._HARDWARE_CHECK_PERIOD)
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "status,params",
    (
        (power_ipmi._STATUS_POWER_ON, None),
        (power_ipmi._STATUS_POWERING_ON, {"pxe": True}),
        (power_ipmi._STATUS_POWERING_ON, None),
    ),
)
def test_power_on_completed(test, mp, status, params):
    host = test.mock_host(
        {"task": mock_task(stage=Stage(name=Stages.POWER_ON, params=params, status=status, status_time=timestamp()))}
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_complete_current_stage(host)
    test.hosts.assert_equal()


def test_powered_on_with_pxe(test, mp):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    params={"pxe": True},
                    status=power_ipmi._STATUS_POWER_ON,
                    status_time=timestamp(),
                )
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    # When using PXE boot we must force power off if someone suddenly power it on
    mock_retry_parent_stage(host, check_after=power_ipmi._HARDWARE_CHECK_PERIOD)
    test.hosts.assert_equal()


# Power on processing for platform with POST codes supported


@pytest.mark.parametrize("params", ({}, {"pxe": True}))
@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
def test_power_on_post(test, mp, params, platform_name):
    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=walle.util.tasks.get_power_on_stages(check_post_code=True, pxe=params.get("pxe", False))[0]
            ),
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=False)

    handle_host(host)

    assert clients.mock_calls == [call.hardware.is_power_on(), call.hardware.power_on(pxe=params.get("pxe", False))]

    # mock host object
    host.task.stage_uid = '1.2.1'
    host.task.revision = 1
    host.task.stage_name = Stages.POWER_ON

    mock_commit_stage_changes(
        host, status=power_ipmi._STATUS_POWERING_ON, check_after=power_ipmi._HARDWARE_CHECK_PERIOD
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
def test_power_on_pending_post(test, mp, platform_name):
    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(stage=walle.util.tasks.get_power_on_stages(check_post_code=True)[0]),
        }
    )
    # mock host...
    host.task.stages[0].stages[1].stages[0].status = power_ipmi._STATUS_POWERING_ON
    host.task.stages[0].stages[1].stages[0].status_time = timestamp()
    host.task.stage_uid = '1.2.1'
    host.task.stage_name = Stages.POWER_ON
    host.save()

    clients = monkeypatch_clients_for_host(mp, host, power_on=False)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_commit_stage_changes(host, check_after=power_ipmi._HARDWARE_CHECK_PERIOD)
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "status,params",
    (
        (power_ipmi._STATUS_POWER_ON, None),
        (power_ipmi._STATUS_POWERING_ON, {"pxe": True}),
        (power_ipmi._STATUS_POWERING_ON, None),
    ),
)
@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
def test_power_on_completed_check_post_code(test, mp, status, params, platform_name):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=walle.util.tasks.get_power_on_stages(
                    check_post_code=True, pxe=params.get("pxe", False) if params else None
                )[0]
            ),
            "platform": platform_name,
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    # mock host...
    host.task.stages[0].stages[1].stages[0].status = status
    host.task.stages[0].stages[1].stages[0].status_time = timestamp()
    host.task.stage_uid = '1.2.1'
    host.task.stage_name = Stages.POWER_ON
    host.save()

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    mock_commit_stage_changes(
        host, status=power_ipmi.STATUS_WAIT_POST_COMPLETE, check_after=power_post_util.POST_CHECK_DELAY
    )
    test.hosts.assert_equal()


def test_powered_on_with_pxe_check_post_code(test, mp):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    params={"pxe": True},
                    status=power_ipmi._STATUS_POWER_ON,
                    status_time=timestamp(),
                )
            )
        }
    )
    clients = monkeypatch_clients_for_host(mp, host, power_on=True)

    handle_host(host)
    assert clients.mock_calls == [call.hardware.is_power_on()]

    # When using PXE boot we must force power off if someone suddenly power it on
    mock_retry_parent_stage(host, check_after=power_ipmi._HARDWARE_CHECK_PERIOD)
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
def test_wait_post_complete_last_post_seen_ok(test, mp, platform_name, upgrade_to_profile):
    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data={"last_post_code_seen": 0xB2, "last_post_code_seen_times": 3},
                )
            ),
        }
    )

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": " 00 40 b2 80"})

    handle_host(host)

    mock_complete_parent_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["expected_code", "expected_times", "post_line", "stage_temp_data"],
    [
        (0xB2, 1, " 00 40 b2 80", {}),
        (0xB9, 1, " 00 40 b9 80", {"last_post_code_seen": 0xB2, "last_post_code_seen_times": 1}),
        (0xB2, 1, " 00 40 b2 80", {"last_post_code_seen": 0xB9, "last_post_code_seen_times": 1}),
        (0xB2, 2, " 00 40 b2 80", {"last_post_code_seen": 0xB2, "last_post_code_seen_times": 1}),
        (0xB2, 3, " 00 40 b2 80", {"last_post_code_seen": 0xB2, "last_post_code_seen_times": 2}),
    ],
)
def test_wait_post_complete_last_post_seen_unstable_ok(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data, expected_code, expected_times
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    mock_commit_stage_changes(
        host,
        check_after=power_post_util.POST_CHECK_PERIOD,
        temp_data={
            "last_post_code_seen": expected_code,
            "last_post_code_seen_times": expected_times,
        },
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        (" 00 40 b2 80", {"last_post_code_seen": 0xB2, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_last_post_seen_unstable_ok_handle_post_code(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    mock_complete_parent_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        (" 00 40 b9 80", {"last_post_code_seen": 0xB9, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_last_post_seen_unstable_mem_problem_handle_post_code(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    if upgrade_to_profile:
        profile_tags = [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD]
        mock_schedule_host_profiling(
            host,
            manual=False,
            profile_tags=profile_tags,
            reason="Upgrading the task to 'profile': POST reported memory problem, profiling host",
        )
    else:
        mock_complete_current_stage(host)

    test.hosts.assert_equal()


@pytest.mark.xfail(reason="should be removed after PlatformGigabyte.get_post_problem_for_code will be fixed")
@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        (" 00 40 ff 80", {"last_post_code_seen": 0xFF, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_last_post_seen_unstable_unknown_problem_handle_post_code(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    if upgrade_to_profile:
        mock_schedule_host_profiling(
            host,
            manual=False,
            reason="Upgrading the task to 'profile': POST reported unknown problem, profiling host",
        )
    else:
        mock_complete_current_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        (" 00 40 b2 80", {"last_post_code_seen": 0xB2, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_stage_timed_out_ok_handle_post_code(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )
    stage = get_current_stage(host)
    stage.status_time = timestamp() - power_post_util.POST_CHECK_TIMEOUT - 1
    host.save()

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    mock_complete_parent_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        (" 00 40 b2 80", {"last_post_code_seen": 0xFF, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_stage_timed_out_nok_ok_handle_post_code(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )
    stage = get_current_stage(host)
    stage.status_time = timestamp() - power_post_util.POST_CHECK_TIMEOUT - 1
    host.save()

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    mock_complete_parent_stage(host)

    test.hosts.assert_equal()


@pytest.mark.xfail(reason="should be removed after PlatformGigabyte.get_post_problem_for_code will be fixed")
@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        (" 00 40 ff 80", {"last_post_code_seen": 0xB9, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_stage_timed_out_unknown_problem_handle_post_code(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )
    stage = get_current_stage(host)
    stage.status_time = timestamp() - power_post_util.POST_CHECK_TIMEOUT - 1
    host.save()

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    if upgrade_to_profile:
        mock_schedule_host_profiling(
            host,
            manual=False,
            reason="Upgrading the task to 'profile': POST reported unknown problem, profiling host",
        )
    else:
        mock_complete_current_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        (" 00 40 b9 80", {"last_post_code_seen": 0xFF, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_stage_timed_out_memory_problem_handle_post_code(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )
    stage = get_current_stage(host)
    stage.status_time = timestamp() - power_post_util.POST_CHECK_TIMEOUT - 1
    host.save()

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    if upgrade_to_profile:
        profile_tags = [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD]
        mock_schedule_host_profiling(
            host,
            manual=False,
            profile_tags=profile_tags,
            reason="Upgrading the task to 'profile': POST reported memory problem, profiling host",
        )
    else:
        mock_complete_current_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("upgrade_to_profile", (True, False))
@pytest.mark.parametrize(
    ["post_line", "stage_temp_data"],
    [
        ("unparsable-post-mock", {"last_post_code_seen": None, "last_post_code_seen_times": 3}),
    ],
)
def test_wait_post_complete_last_post_seen_unparsable_post_line(
    test, mp, platform_name, upgrade_to_profile, post_line, stage_temp_data
):

    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(
                    name=Stages.POWER_ON,
                    status=power_ipmi.STATUS_WAIT_POST_COMPLETE,
                    status_time=timestamp(),
                    params={"upgrade_to_profile": upgrade_to_profile},
                    temp_data=stage_temp_data,
                )
            ),
        }
    )

    monkeypatch_clients_for_host(mp, host, power_on=True, raw_cmd_result={"success": True, "message": post_line})
    handle_host(host)

    mock_complete_parent_stage(host)

    test.hosts.assert_equal()
