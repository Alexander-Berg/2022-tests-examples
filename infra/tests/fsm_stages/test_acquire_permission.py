"""Tests acquiring permission for task processing."""


from datetime import datetime
from unittest.mock import call, ANY

import pytest

import walle.projects
from infra.walle.server.tests.lib.util import (
    TestCase,
    patch,
    mock_task,
    handle_host,
    mock_fail_current_stage,
    mock_commit_stage_changes,
    mock_complete_current_stage,
    patch_attr,
    monkeypatch_config,
    check_stage_initialization,
)
from sepelib.core import constants
from walle import audit_log, authorization
from walle.clients.cms import (
    _BaseCmsClient,
    CmsTaskType,
    CmsTaskAction,
    CmsTaskStatus,
    CmsConnectionError,
    CmsTaskRejectedError,
    CmsApiError,
    MaintenancePriority,
    get_cms_client,
    CmsApiVersion,
)
from walle.clients.tvm import TvmApiError
from walle.expert.failure_types import FailureType
from walle.fsm_stages import acquire_permission
from walle.hosts import HostState, HostStatus, TaskType
from walle.models import timestamp, monkeypatch_timestamp
from walle.scenario.constants import ScriptName
from walle.scenario.data_storage.types import HostGroupSource
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import MaintenancePlotHostGroupSource
from walle.scenario.script_args import ItdcMaintenanceParams
from walle.stages import Stages, Stage
from walle.util.limits import nice_period
from walle.util.misc import drop_none
from walle.util.workdays import to_timestamp


def _get_mocked_location_field(host):
    return {"switch": host.location.switch, "port": host.location.port}


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.ACQUIRE_PERMISSION), status=acquire_permission._STATUS_WALLE)


@pytest.mark.parametrize("state", HostState.ALL_IGNORED_LIMITS_COUNTING)
def test_task_processing_limits(test, mp, state):
    tasks_num = len(HostStatus.ALL_TASK)
    assert tasks_num > 1

    host_specs = [
        {"owner": "user1@", "type": TaskType.MANUAL},
        {"owner": authorization.ISSUER_WALLE, "type": TaskType.AUTOMATED_ACTION},
        {"owner": authorization.ISSUER_WALLE, "type": TaskType.AUTOMATED_HEALING},
        {"owner": "user2@", "type": TaskType.MANUAL},
    ]

    test.mock_host(
        {
            "inv": 0,
            "state": state,
            "status": HostStatus.ALL_TASK[0],
            "task": mock_task(task_id=0, type=TaskType.AUTOMATED_ACTION, owner=authorization.ISSUER_WALLE, stages=[]),
        }
    )

    for host_spec in host_specs:
        hosts = host_spec.setdefault("hosts", [])
        for id, status in enumerate(HostStatus.ALL_TASK):
            host_id = len(test.hosts.objects) + 1
            hosts.append(
                test.mock_host(
                    {
                        "inv": host_id,
                        "state": HostState.ASSIGNED,
                        "status": status,
                        "task": mock_task(task_id=host_id, type=host_spec["type"], owner=host_spec["owner"], stages=[]),
                    }
                )
            )

    monkeypatch_config(mp, "task_processing.max_processing_tasks_per_user", tasks_num - 1)
    monkeypatch_config(mp, "task_processing.max_processing_healing_tasks_per_project", tasks_num - 1)
    monkeypatch_config(mp, "task_processing.max_processing_automated_tasks_per_project", tasks_num - 1)

    for host_spec in host_specs:
        for host_id, host in enumerate(host_spec["hosts"]):
            if host_id < tasks_num - 1:
                acquire_permission._check_limits(host)
            else:
                with pytest.raises(acquire_permission._TaskLimitsExceededError):
                    acquire_permission._check_limits(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", HostState.ALL)
@patch("walle.fsm_stages.acquire_permission._check_limits")
def test_walle_accept(check_limits, test, state):
    host = test.mock_host(
        {
            "state": state,
            "task": mock_task(stage=Stages.ACQUIRE_PERMISSION, stage_status=acquire_permission._STATUS_WALLE),
        }
    )

    handle_host(host)
    assert check_limits.call_count == 1

    mock_commit_stage_changes(host, status=acquire_permission._STATUS_CALENDAR, check_now=True)
    test.hosts.assert_equal()


@patch("walle.fsm_stages.acquire_permission._check_limits")
def test_walle_accept_ignore_cms(check_limits, test):
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stages.ACQUIRE_PERMISSION, stage_status=acquire_permission._STATUS_WALLE, ignore_cms=True
            )
        }
    )
    handle_host(host)
    assert check_limits.call_count == 1
    mock_commit_stage_changes(host, status=acquire_permission._STATUS_CALENDAR, check_now=True)
    test.hosts.assert_equal()


@patch(
    "walle.fsm_stages.acquire_permission._check_limits",
    side_effect=acquire_permission._TaskLimitsExceededError("Mocked limit exceeded error"),
)
def test_walle_reject(check_limits, test):
    host = test.mock_host(
        {"task": mock_task(stage=Stages.ACQUIRE_PERMISSION, stage_status=acquire_permission._STATUS_WALLE)}
    )
    handle_host(host)
    assert check_limits.call_count == 1
    mock_commit_stage_changes(
        host, status_message="Mocked limit exceeded error", check_after=acquire_permission._LIMITS_CHECK_PERIOD
    )
    test.hosts.assert_equal()


class TestAcquireCmsApi:
    @staticmethod
    def mock_host(test, stage_params={}, task_params={}, host_params={}, cms_url="default"):
        project = test.mock_project(
            {
                "cms_settings": [
                    {
                        "cms": cms_url,
                        "cms_api_version": CmsApiVersion.V1_0,
                        "cms_tvm_app_id": 11110000,
                        "cms_max_busy_hosts": 1,
                    }
                ],
                "id": "project-id-mock",
            }
        )

        host_params["project"] = project.id
        stage_params = dict({"action": CmsTaskAction.REBOOT, "workdays": False}, **stage_params)
        task_params = dict(
            {
                "type": TaskType.AUTOMATED_HEALING,
                "cms_task_id": "mock-cms-task-id",
                "stage": Stages.ACQUIRE_PERMISSION,
                "stage_status": acquire_permission._STATUS_CMS,
                "stage_params": stage_params,
            },
            **task_params
        )
        return test.mock_host(dict(task=mock_task(**task_params), **host_params))

    def test_cms_stage_new_task_id(self, test, mp):
        host = self.mock_host(test, task_params={"cms_task_id": None, "iss_banned": True})
        update_payload_mock = mp.function(audit_log.update_payload)

        get_task = patch_attr(mp, _BaseCmsClient, "get_task", return_value=None)
        add_task = patch_attr(mp, _BaseCmsClient, "add_task", return_value={"status": CmsTaskStatus.OK})
        mp.function(get_cms_client, module=walle.projects, return_value=_BaseCmsClient)

        assert host.task.cms_task_id is None
        handle_host(host)

        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]
        assert add_task.mock_calls == [
            call(
                ANY,
                host.task.get_cms_task_id(),
                CmsTaskType.AUTOMATED,
                host.task.owner,
                CmsTaskAction.REBOOT,
                [host.name],
                task_group=None,
                comment=None,
                extra=None,
                failure=None,
                check_names=None,
                failure_type=None,
                location=_get_mocked_location_field(host),
                maintenance_info=ANY,
                scenario_info=ANY,
            )
        ]

        mock_complete_current_stage(host, inc_revision=1)
        host.task.cms_task_id = host.task.get_cms_task_id()

        update_payload_mock.assert_called_once_with(host.task.audit_log_id, {"cms_task_id": host.task.cms_task_id})

        test.hosts.assert_equal()

    def test_cms_stage_existing_task_id(self, test, mp):
        cms_task_id = "mock-cms-task-id"
        host = self.mock_host(test, task_params={"cms_task_id": cms_task_id, "iss_banned": True})

        get_task = patch_attr(
            mp,
            _BaseCmsClient,
            "get_task",
            return_value={
                "status": CmsTaskStatus.OK,
                "hosts": [host.name],
                "message": "allowed",
            },
        )
        mp.function(get_cms_client, module=walle.projects, return_value=_BaseCmsClient)

        assert host.task.cms_task_id is not None
        handle_host(host)

        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]

        mock_complete_current_stage(host)

        host.task.cms_task_id = cms_task_id
        test.hosts.assert_equal()

    def test_cms_stage_host_retained_task_id(self, test, mp):
        cms_task_id = "mock-cms-task-id"
        host = self.mock_host(
            test, task_params={"cms_task_id": None, "iss_banned": True}, host_params={"cms_task_id": cms_task_id}
        )

        get_task = patch_attr(
            mp,
            _BaseCmsClient,
            "get_task",
            return_value={
                "status": CmsTaskStatus.OK,
                "hosts": [host.name],
                "message": "allowed",
            },
        )
        mp.function(get_cms_client, module=walle.projects, return_value=_BaseCmsClient)

        assert host.task.cms_task_id is None
        handle_host(host)

        assert get_task.mock_calls == [call(ANY, cms_task_id)]

        mock_complete_current_stage(host, inc_revision=1)

        host.task.cms_task_id = cms_task_id
        test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "stage_params",
        [
            # all args here
            {
                "task_group": "mock-group-id",
                "comment": "comment-mock",
                "extra": {"extra": "mock"},
                "failure": "some-failure",
                "check_names": ["ssh", "udp"],
                "failure_type": "mem_ecc",
            },
            # partial
            {"comment": "comment-mock", "extra": {"extra": "mock"}},
            {"comment": "comment-mock"},
            {"task_group": "mock-group-id"},
            {"extra": {"extra": "mock"}},
            {"check_names": ["ssh", "UNREACHABLE"]},
            {"comment": None, "extra": None},
            {},
        ],
    )
    @pytest.mark.parametrize("task_issuer", ["mock_user", "some_other_mock_user"])
    def test_cms_stage_params(self, test, mp, stage_params, task_issuer):
        host = self.mock_host(test, stage_params=stage_params, task_params={"iss_banned": True, "owner": task_issuer})

        get_task = patch_attr(mp, _BaseCmsClient, "get_task", return_value=None)
        add_task = patch_attr(mp, _BaseCmsClient, "add_task", return_value={"status": CmsTaskStatus.OK})
        mp.function(get_cms_client, module=walle.projects, return_value=_BaseCmsClient)

        handle_host(host)

        mock_maintenance_info = drop_none(
            {
                "id": host.task.get_cms_task_id(),
                "kind": CmsTaskAction.REBOOT,
                "message": stage_params.get("comment"),
                "disruptive": False,
                "estimated_duration": 20 * constants.MINUTE_SECONDS,
                "node_set_id": stage_params.get("task_group", host.task.get_cms_task_id()),
                "priority": MaintenancePriority.NORMAL,
                "labels": {"issuer": task_issuer},
            }
        )
        expected_call_kwargs = {
            "task_group": None,
            "comment": None,
            "extra": None,
            "failure": None,
            "failure_type": None,
            "check_names": None,
            "location": _get_mocked_location_field(host),
            "maintenance_info": mock_maintenance_info,
            "scenario_info": ANY,
        }
        expected_call_kwargs.update(stage_params)

        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]
        assert add_task.mock_calls == [
            call(
                ANY,
                host.task.get_cms_task_id(),
                CmsTaskType.AUTOMATED,
                host.task.owner,
                CmsTaskAction.REBOOT,
                [host.name],
                **expected_call_kwargs
            )
        ]

        mock_complete_current_stage(host)

        test.hosts.assert_equal()

    def test_force_new_cms_task(self, test, mp):
        host = self.mock_host(
            test,
            stage_params={"force_new_cms_task": True},
            task_params={"iss_banned": True, "cms_task_id": None},
            host_params={"state": HostState.MAINTENANCE, "cms_task_id": "running"},
        )

        get_task = patch_attr(mp, _BaseCmsClient, "get_task", return_value=None)
        add_task = patch_attr(mp, _BaseCmsClient, "add_task", return_value={"status": CmsTaskStatus.OK})
        mp.function(get_cms_client, module=walle.projects, return_value=_BaseCmsClient)
        update_payload_mock = mp.function(audit_log.update_payload)

        handle_host(host)

        expected_call_kwargs = {
            "comment": None,
            "location": _get_mocked_location_field(host),
            "extra": None,
            "failure": None,
            "failure_type": None,
            "task_group": None,
            "check_names": None,
            "maintenance_info": ANY,
            "scenario_info": ANY,
        }

        assert host.task.get_cms_task_id() != host.cms_task_id
        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]
        assert add_task.mock_calls == [
            call(
                ANY,
                host.task.get_cms_task_id(),
                CmsTaskType.AUTOMATED,
                host.task.owner,
                CmsTaskAction.REBOOT,
                [host.name],
                **expected_call_kwargs
            )
        ]

        mock_complete_current_stage(host, inc_revision=1)

        host.task.cms_task_id = host.task.get_cms_task_id()
        update_payload_mock.assert_called_once_with(host.task.audit_log_id, {"cms_task_id": host.task.cms_task_id})
        test.hosts.assert_equal()

    @pytest.mark.parametrize("status", (CmsTaskStatus.IN_PROCESS, CmsTaskStatus.OK))
    def test_cms_new(self, test, mp, status):
        host = self.mock_host(
            test, task_params={"iss_banned": True}, stage_params={"failure_type": FailureType.AVAILABILITY.name}
        )

        get_task = patch_attr(mp, _BaseCmsClient, "get_task", return_value=None)
        add_task = patch_attr(mp, _BaseCmsClient, "add_task", return_value={"status": status})

        handle_host(host)
        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]
        assert add_task.mock_calls == [
            call(
                ANY,
                host.task.get_cms_task_id(),
                CmsTaskType.AUTOMATED,
                host.task.owner,
                CmsTaskAction.REBOOT,
                [host.name],
                task_group=None,
                dry_run=False,
                comment=None,
                location=_get_mocked_location_field(host),
                extra=None,
                failure=None,
                failure_type=FailureType.AVAILABILITY.name,
                check_names=None,
                maintenance_info=ANY,
                scenario_info=ANY,
            )
        ]

        if status == CmsTaskStatus.IN_PROCESS:
            mock_commit_stage_changes(
                host,
                status_message="CMS hasn't allowed to process the host yet: [no message provided].",
                check_after=acquire_permission._LIMITS_CHECK_PERIOD,
            )
        else:
            mock_complete_current_stage(host)

        test.hosts.assert_equal()

    def test_cms_new_rejected(self, mp, test):
        host = self.mock_host(test, task_params={"type": TaskType.MANUAL})

        get_task = patch_attr(mp, _BaseCmsClient, "get_task", return_value=None)
        add_task = patch_attr(
            mp, _BaseCmsClient, "add_task", side_effect=CmsTaskRejectedError("Mocked task rejected error")
        )

        handle_host(host)
        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]
        assert add_task.mock_calls == [
            call(
                ANY,
                host.task.get_cms_task_id(),
                CmsTaskType.MANUAL,
                host.task.owner,
                CmsTaskAction.REBOOT,
                [host.name],
                task_group=None,
                dry_run=False,
                comment=None,
                location=_get_mocked_location_field(host),
                extra=None,
                failure=None,
                failure_type=None,
                check_names=None,
                maintenance_info=ANY,
                scenario_info=ANY,
            )
        ]
        mock_fail_current_stage(host, reason="CMS rejected the request: Mocked task rejected error.")

        test.hosts.assert_equal()

    @pytest.mark.parametrize("status", (CmsTaskStatus.OK, CmsTaskStatus.IN_PROCESS, CmsTaskStatus.REJECTED))
    def test_cms_existing(self, test, mp, status):
        host = self.mock_host(test, task_params={"iss_banned": True})

        cms_reject_message = "Mocked error"
        get_task = patch_attr(
            mp,
            _BaseCmsClient,
            "get_task",
            return_value={
                "status": status,
                "hosts": [host.name],
                "message": cms_reject_message,
            },
        )

        handle_host(host)
        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]

        if status == CmsTaskStatus.IN_PROCESS:
            mock_commit_stage_changes(
                host,
                status_message="CMS hasn't allowed to process the host yet: {}.".format(cms_reject_message),
                check_after=acquire_permission._LIMITS_CHECK_PERIOD,
            )
        elif status == CmsTaskStatus.OK:
            mock_complete_current_stage(host)
        elif status == CmsTaskStatus.REJECTED:
            mock_fail_current_stage(host, reason="CMS rejected the request: {}.".format(cms_reject_message))
        else:
            assert False

        test.hosts.assert_equal()

    @pytest.mark.parametrize(
        ["get_task_side_effect", "add_task_side_effect", "error_message"],
        [
            (
                CmsApiError(503, "msg error", "error: {}", "mock"),
                [None],
                "CMS has not allowed to perform reboot action during given timeout (1 week). Reason: error: mock",
            ),
            (
                CmsConnectionError("error-mock"),
                [None],
                "CMS has not allowed to perform reboot action during given timeout (1 week). Reason: error-mock",
            ),
            ([None], CmsApiError(503, "msg error", "error: {}", "mock"), "error: mock"),
            ([None], CmsConnectionError("error-mock"), "error-mock"),
            ([None], TvmApiError("error mock"), "Error in communication with TVM API: error mock"),
        ],
    )
    def test_cms_request_error(self, test, mp, get_task_side_effect, add_task_side_effect, error_message):
        host = self.mock_host(
            test, task_params={"stage_status_time": timestamp() - acquire_permission._CMS_WAIT_TIMEOUT}
        )

        patch_attr(mp, _BaseCmsClient, "get_task", side_effect=get_task_side_effect)
        patch_attr(mp, _BaseCmsClient, "add_task", side_effect=add_task_side_effect)

        handle_host(host)
        mock_commit_stage_changes(host, error=error_message, check_after=acquire_permission._LIMITS_CHECK_PERIOD)
        test.hosts.assert_equal()

    @pytest.mark.parametrize(
        ["cms_url", "cms_report_config", "timeout"],
        [
            ("default", [], acquire_permission._CMS_WAIT_TIMEOUT),  # no settings at all
            (
                "https://example.com/",
                [{"url_match": r".*example\.com.*"}],  # no timeout, use default
                acquire_permission._CMS_WAIT_TIMEOUT,
            ),
            (
                "https://example.com/",
                [{"url_match": r".*example\.com.*", "acquire_timeout": "2d"}],
                2 * constants.DAY_SECONDS,
            ),
            (
                "https://example.com/",
                [{"url_match": r".*other\.com.*", "acquire_timeout": "2d"}],  # no timeout for our cms, use default
                acquire_permission._CMS_WAIT_TIMEOUT,
            ),
        ],
    )
    def test_cms_wait_timeout(self, test, mp, cms_url, cms_report_config, timeout):
        mp.config("failure_reports.report_params.cms_report_params", cms_report_config)
        mp.function(get_cms_client, module=walle.projects, return_value=_BaseCmsClient)

        host = self.mock_host(test, task_params={"stage_status_time": timestamp() - timeout}, cms_url=cms_url)

        get_task = patch_attr(
            mp,
            _BaseCmsClient,
            "get_task",
            return_value={
                "status": CmsTaskStatus.IN_PROCESS,
                "hosts": [host.name],
                "message": "Mocked error",
            },
        )

        handle_host(host)
        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]

        error_message = (
            "CMS has not allowed to perform reboot action during given timeout ({}). "
            "Reason: Mocked error".format(nice_period(timeout))
        )
        mock_commit_stage_changes(host, error=error_message, check_after=acquire_permission._LIMITS_CHECK_PERIOD)
        test.hosts.assert_equal()


class TestAcquireCalendar:
    @staticmethod
    def _mock_project(test):
        return test.mock_project({"id": "project-mock"})

    @classmethod
    def mock_host(cls, test, stage_params={}):
        return test.mock_host(
            dict(
                task=mock_task(
                    **dict(
                        {
                            "type": TaskType.AUTOMATED_HEALING,
                            "cms_task_id": "mock-cms-task-id",
                            "stage": Stages.ACQUIRE_PERMISSION,
                            "stage_status": acquire_permission._STATUS_CALENDAR,
                            "stage_params": dict({"action": CmsTaskAction.REBOOT}, **stage_params),
                        }
                    )
                ),
                project=cls._mock_project(test).id,
            )
        )

    @pytest.mark.parametrize("workdays_param", [None, False, "", 0, -1])
    def test_disabled(self, test, mp, workdays_param):
        host = self.mock_host(test, stage_params={"workdays": False} if workdays_param != -1 else {})
        handle_host(host)

        mock_commit_stage_changes(host, status=acquire_permission._STATUS_CMS, check_now=True)
        test.hosts.assert_equal()

    def test_now_is_working_time(self, test, mp):
        host = self.mock_host(test, stage_params={"workdays": True})
        monkeypatch_timestamp(mp, to_timestamp(datetime(2020, 6, 1, 12)))  # 01.06.2020 is Monday, it is a working day.
        handle_host(host)

        mock_commit_stage_changes(host, status=acquire_permission._STATUS_CMS, check_now=True)
        test.hosts.assert_equal()

    def test_now_is_not_working_time(self, test, mp):
        host = self.mock_host(test, stage_params={"workdays": True})
        # 06.06.2020 is Saturday, it is not a working day.
        # Next working day is Monday, working time starts at 11:00
        mock_now = to_timestamp(datetime(2020, 6, 6, 12))
        check_time = to_timestamp(datetime(2020, 6, 8, 11))
        monkeypatch_timestamp(mp, mock_now)

        handle_host(host)

        mock_commit_stage_changes(
            host,
            check_at=check_time,
            status_message="Delay processing of 'ready' task by 'wall-e' "
            "until '2020-06-08T11:00:00': workdays rule applied.",
        )
        test.hosts.assert_equal()


class TestAcquireScenarioParamsInCmsApi:
    @staticmethod
    def mock_host(test, stage_params={}, task_params={}, host_params={}, cms_url="default"):
        project = test.mock_project(
            {
                "cms_settings": [
                    {
                        "cms": cms_url,
                        "cms_api_version": CmsApiVersion.V1_0,
                        "cms_tvm_app_id": 11110000,
                        "cms_max_busy_hosts": 1,
                    }
                ],
                "id": "project-id-mock",
            }
        )

        host_params["project"] = project.id
        stage_params = dict({"action": CmsTaskAction.REBOOT, "workdays": False}, **stage_params)
        task_params = dict(
            {
                "type": TaskType.AUTOMATED_HEALING,
                "cms_task_id": "mock-cms-task-id",
                "stage": Stages.ACQUIRE_PERMISSION,
                "stage_status": acquire_permission._STATUS_CMS,
                "stage_params": stage_params,
            },
            **task_params
        )
        return test.mock_host(dict(task=mock_task(**task_params), **host_params))

    @pytest.mark.parametrize("status", (CmsTaskStatus.IN_PROCESS, CmsTaskStatus.OK))
    @pytest.mark.parametrize("maintenance_start_time", (0, None, 999))
    @pytest.mark.parametrize("maintenance_end_time", (0, None, 999))
    def test_scenario_info_for_cms(self, test, mp, status, maintenance_start_time, maintenance_end_time):
        scenario = test.mock_scenario(dict(scenario_type=ScriptName.ITDC_MAINTENANCE))
        data_storage = get_data_storage(scenario)
        host_groups_sources = [HostGroupSource(0, MaintenancePlotHostGroupSource(maintenance_plot_id="any"))]
        data_storage.write_host_groups_sources(host_groups_sources)
        scenario_parameters = ItdcMaintenanceParams(
            maintenance_start_time=maintenance_start_time, maintenance_end_time=maintenance_end_time
        )
        data_storage.write_scenario_parameters(scenario_parameters)
        scenario.save(validate=False)

        host = self.mock_host(
            test,
            task_params={"iss_banned": True},
            stage_params={"failure_type": FailureType.AVAILABILITY.name},
            host_params=dict(scenario_id=scenario.scenario_id),
        )
        assert host.scenario_id == scenario.scenario_id

        mock_scenario_info = dict(
            scenario_id=scenario.scenario_id,
            scenario_type=scenario.scenario_type,
            maintenance_start_time=maintenance_start_time,
            maintenance_end_time=maintenance_end_time,
        )

        get_task = patch_attr(mp, _BaseCmsClient, "get_task", return_value=None)
        add_task = patch_attr(mp, _BaseCmsClient, "add_task", return_value={"status": status})
        mp.function(get_cms_client, module=walle.projects, return_value=_BaseCmsClient)

        handle_host(host)
        assert get_task.mock_calls == [call(ANY, host.task.get_cms_task_id())]
        assert add_task.mock_calls == [
            call(
                ANY,
                host.task.get_cms_task_id(),
                CmsTaskType.AUTOMATED,
                host.task.owner,
                CmsTaskAction.REBOOT,
                [host.name],
                task_group=None,
                comment=None,
                location=_get_mocked_location_field(host),
                extra=None,
                failure=None,
                failure_type=FailureType.AVAILABILITY.name,
                check_names=None,
                maintenance_info=ANY,
                scenario_info=mock_scenario_info,
            )
        ]

        if status == CmsTaskStatus.IN_PROCESS:
            mock_commit_stage_changes(
                host,
                status_message="CMS hasn't allowed to process the host yet: [no message provided].",
                check_after=acquire_permission._LIMITS_CHECK_PERIOD,
            )
        else:
            mock_complete_current_stage(host)

        test.hosts.assert_equal()
