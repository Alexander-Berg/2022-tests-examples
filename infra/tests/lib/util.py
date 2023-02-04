"""Unit test utilities."""

import datetime
import http.client
import json
import logging
import os
import random
import re
import sys
import time
import typing as tp
from contextlib import contextmanager
from unittest import mock
from uuid import UUID

import library.python.resource as resource
import pytest
import six

import walle.expert.types
from walle.authorization import blackbox
from walle.constants import HostType
from walle.idm.project_role_managers import ProjectRole
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.mixins import BaseStage

try:
    from _pytest.monkeypatch import MonkeyPatch
except ImportError:
    from _pytest.monkeypatch import monkeypatch as MonkeyPatch

from unittest.mock import Mock, call, create_autospec
from mongoengine import Q
from sepelib.core import config
from sepelib.core.exceptions import Error, LogicalError
from sepelib.flask.testcase import FlaskTestCase
from sepelib.mongo.mock import ObjectMocker, Database
from sepelib.mongo.util import register_database, IS_PYMONGO_2

import walle.admin_requests.request as admin_requests
import walle.application
import walle.clients.deploy
import walle.expert.automation_plot
import walle.expert.decisionmakers
import walle.failure_reports.base
import walle.fsm_stages.common
import walle.operations_log.operations as operations_log
import walle.stages
import walle.tasks
import walle.util.tasks
import walle.util.host_health
from walle import (
    audit_log,
    authorization,
    fsm_stages,
    network,
    constants as walle_constants,
    restrictions,
    constants,
    hosts,
)
from walle.application import app
from walle.audit_log import LogEntry
from walle.clients import bot, ipmiproxy, deploy, eine, inventory, ssh, startrek, utils as client_utils, ok
from walle.clients.cms import CmsTaskAction
from walle.clients.eine import ProfileMode, EineProfileTags
from walle.clients.juggler import JugglerDowntimeName
from walle.constants import (
    SshOperation,
    PROVISIONER_LUI,
    EINE_PROFILES_WITH_DC_SUPPORT,
    PROVISIONER_EINE,
    DEFAULT_DNS_TTL,
    NetworkTarget,
    EINE_IPXE_FROM_CDROM_PROFILE,
    FLEXY_EINE_PROFILE,
)
from walle.dns.dns_fixer import _try_fix_host_dns_records
from walle.expert import dmc, juggler
from walle.expert.automation import healing_automation
from walle.expert.constants import (
    NETMON_REACTION_TIMEOUT,
    RACK_FAILURE_REACTION_TIMEOUT,
    JUGGLER_PUSH_PERIOD,
    NETWORK_CHECKS_REACTION_TIMEOUT,
)
from walle.expert.decision import Decision
from walle.expert.types import WalleAction, CheckType, CheckGroup, CheckStatus, HwWatcherCheckStatus
from walle.expert.failure_log import FailureLog
from walle.expert.juggler import get_health_status_reasons
from walle.expert.rules.base import AbstractRule
from walle.fsm_stages.common import (
    register_stage,
    get_current_stage,
    get_parent_stage,
    enter_task,
    StageTerminals,
    PARENT_STAGE_RETRY_STATUS,
)
from walle.fsm_stages.constants import EineProfileOperation
from walle.host_fsm import fsm
from walle.host_health import HealthCheck
from walle.hosts import (
    Host,
    HostState,
    HostOperationState,
    HostStatus,
    HealthStatus,
    HostLocation,
    Task,
    TaskType,
    DeployConfiguration,
    HostPlatform,
    StateExpire,
    DMCRule,
)
from walle.host_network import HostNetwork
from walle.locks import (
    CronJobInterruptableLock,
    ProjectInterruptableLock,
    HostInterruptableLock,
    HostHealingInterruptableGlobalLock,
    AutomationPlotInterruptableLock,
    ScenarioInterruptableLock,
    MaintenancePlotInterruptableLock,
    ScenarioAcquiringInterruptableGlobalLock,
)
from walle.maintenance_plot.model import MaintenancePlotModel
from walle.models import timestamp
from walle.network import HostNetworkLocationInfo
from walle.operations_log.constants import Operation
from walle.preorders import Preorder
from walle.projects import Project, Notifications, NotificationRecipients
from walle.physical_location_tree import LocationNamesMap
from walle.stages import Stage, Stages
from walle.statbox.handlers import WalleStatboxLogger, JsonSerializer
from walle.util.misc import drop_none, filter_dict_keys
from walle.util.tasks import DEFAULT_PROFILE_TAGS
from walle.scenario.constants import ScriptName, WORK_STATUS_LABEL_NAME, ScenarioWorkStatus
from walle.scenario.scenario import Scenario, ScenarioFsmStatus
from walle.scenario.script import noop_script
from walle.scenario.stage_info import StageInfo
from walle._tasks.task_creator import (
    get_reboot_task_stages,
    create_new_task,
    get_power_on_task_stages,
    get_power_off_task_stages,
    get_delete_host_task_stages,
    get_vlan_switch_task_stages,
    get_bot_acquirement_task_stages,
    get_kexec_reboot_task_stages,
)
from walle._tasks.task_args import (
    RebootTaskArgs,
    PowerOnTaskArgs,
    PowerOffTaskArgs,
    DeleteHostTaskArgs,
    VlanSwitchTaskArgs,
    EnsureHostPreorderAcquirementTaskArgs,
    ProjectSwitchingArgs,
    SwitchToMaintenanceTaskArgs,
    PrepareTaskArgs,
    FqdnDeinvalidationArgs,
    HostReleaseArgs,
    KexecRebootTaskArgs,
)
from walle.restrictions import strip_restrictions
from walle.expert.rules.utils import repair_hardware_params
from walle.admin_requests.constants import RequestTypes


TEST_HOST = "iva1-5895.search.yandex.net"
TEST_HOST_INV = 100749323
"""A real test host for DNS lookups and other read only operations."""


def any_task_status():
    return random.choice(HostStatus.ALL_TASK)


def predefined_task_status():
    return HostStatus.ALL_TASK[0]


def any_steady_status():
    return random.choice(HostStatus.ALL_STEADY)


PARENT_STAGE = "parent-mock"
PREV_STAGE = "prev-mock"
NEXT_STAGE = "next-mock"
PARENT_NEXT_STAGE = "parent-next-mock"

AUDIT_LOG_ID = "audit-log-id-mock"
HOST_UUID = "00000000-0000-0000-0000-000000000000"

NATIVE_VLAN = 666

_UNSPECIFIED = object()

BOT_PROJECT_ID = 100009
SEED = 1337


def mock_uuid_for_inv(inv):
    return "00000000000000000000{:0>12}".format(inv)


class MockLimitManager:
    @contextmanager
    def check_limit(self, func, rps, max_concurrent, concurrent_timeout):
        yield


class TestCase(FlaskTestCase):
    api_user = "mocked-user"
    api_issuer = api_user + "@"

    host_profile = "host-profile-mock"

    project_provisioner = walle_constants.PROVISIONER_EINE
    project_deploy_config = "project-deploy-config-mock"

    host_provisioner = walle_constants.PROVISIONER_LUI
    host_deploy_config = "host-deploy-config-mock"

    ipmi_mac = "aa:bb:cc:dd:ee:ff"
    ipmi_host = "ipmi.host.mock.yandex.net"
    macs = ["00:00:00:00:00:00", "11:11:11:11:11:11"]

    _app_initialized = False

    # kind of class level @property
    # for xdist to work we need random properties to be initialized only after module initialization,
    # when all workers have already set same random seed
    __project_profile = None

    def __init__(self, *args, **kwargs):
        random.seed(SEED)
        if not TestCase._app_initialized:
            app.init_role()
            app.setup_logging(level=logging.DEBUG)
            app.setup_flask()
            app.flask.limit_manager = MockLimitManager()
            TestCase._app_initialized = True

        self.__health_db = kwargs.pop("healthdb", False)
        self.__patchers = []
        self.__monkeypatch = MonkeyPatch()

        self.app = walle.application.app.flask
        self.lightweight_db_mocking = True
        super().__init__(*args, **kwargs)

    def setUp(self):
        super().setUp()

        if self.__health_db:
            try:
                db = Database(lightweight=True)
                host, port = (db.connection.host, db.connection.port) if IS_PYMONGO_2 else db.connection.address
                health_database_uri = "mongodb://{}:{}/health".format(host, port)
                monkeypatch_config(self.__monkeypatch, "health-mongodb.uri", health_database_uri)
                register_database("health-mongodb", alias="health")
            except Exception:
                self.tearDown()
                raise

        self.statbox_logger = WalleStatboxLogger(
            log_type="test", logger=logging.getLogger("tskv.mock"), serializer=JsonSerializer()
        )

        self.__patch(mock.patch("walle.util.api._DEFAULT_STRICT_API", True))
        self.__patch(mock.patch.dict(config.get_value("automation"), {"enabled": True}))
        self.__patch(mock.patch.dict(config.get_value("cms"), {"namespace": "unit-tests"}))
        self.__patch(
            mock.patch(
                "walle.authorization.blackbox._authenticate_cached",
                return_value=blackbox.AuthInfo(issuer=self.api_issuer, session_id=None),
            )
        )
        self.__patch(mock.patch("walle.util.notifications.send_email"))

        self.projects = ObjectMocker(
            Project,
            {
                "type": walle_constants.HostType.SERVER,
                "owned_vlans": [],
                "cms": walle.projects.DEFAULT_CMS_NAME,
                "cms_max_busy_hosts": 5,
                "cms_settings": [
                    {
                        "cms": walle.projects.DEFAULT_CMS_NAME,
                        "cms_max_busy_hosts": 5,
                    }
                ],
                "profile": self.project_profile(),
                "profile_tags": [],
                "provisioner": self.project_provisioner,
                "deploy_config": self.project_deploy_config,
                "vlan_scheme": walle_constants.VLAN_SCHEME_STATIC,
                "native_vlan": NATIVE_VLAN,
                "healing_automation": {"enabled": True},
                "dns_automation": {"enabled": True},
                "automation_limits": walle.projects.get_default_project_automation_limits(),
                "host_limits": walle.projects.get_default_host_limits(),
                "notifications": Notifications(recipients=NotificationRecipients()),
                "bot_project_id": BOT_PROJECT_ID,
                "maintenance_plot_id": "mocked-maintenance-plot-id",
            },
        )

        self.default_project = self.mock_project(
            {
                "id": "mocked-default-project",
            }
        )

        self.scenarios = ObjectMocker(
            Scenario,
            {
                "issuer": self.api_issuer,
                "next_check_time": 0,
                "scenario_id": 0,
                "name": "mocked_scenario",
                "scenario_type": noop_script.name,
                "action_time": 0,
                "creation_time": 0,
                "status": ScenarioFsmStatus.CREATED,
                "stage_info": StageInfo(uid="0"),
            },
        )
        self.hosts_stage_info = ObjectMocker(HostStageInfo, {})

        self.preorders = ObjectMocker(
            Preorder,
            {
                "owner": self.api_user,
                "issuer": self.api_issuer,
                "project": self.default_project.id,
                "bot_project": BOT_PROJECT_ID,
                "prepare": False,
                "audit_log_id": "audit-log-id-mock",
                "processed": False,
            },
        )

        self.hosts = ObjectMocker(
            Host,
            {
                "inv": 9999,
                "name": "default",
                "type": walle_constants.HostType.SERVER,
                "ipmi_mac": self.ipmi_mac,
                "macs": self.macs,
                "state": HostState.FREE,
                "state_time": timestamp(),
                "state_author": self.api_issuer,
                "state_audit_log_id": AUDIT_LOG_ID,
                "operation_state": HostOperationState.OPERATION,
                "status": HostStatus.READY,
                "status_time": timestamp(),
                "status_author": self.api_issuer,
                "status_audit_log_id": AUDIT_LOG_ID,
                "project": self.default_project.id,
                "location": mock_location(),
                "platform": HostPlatform(system="system_model", board="board_model"),
            },
        )
        self.host_network = ObjectMocker(HostNetwork, {})
        self.failure_log = ObjectMocker(FailureLog)
        self.audit_log = ObjectMocker(
            audit_log.LogEntry,
            {
                "issuer": authorization.ISSUER_WALLE,
                "status": audit_log.STATUS_COMPLETED,
                "type": audit_log.TYPE_REBOOT_HOST,
                "time": timestamp(),
                "status_time": timestamp(),
            },
        )

        if self.__health_db:
            self.health_checks = ObjectMocker(HealthCheck)

        self.admin_requests = ObjectMocker(
            admin_requests._AdminRequest,
            {
                "id": admin_requests._request_id(admin_requests.RequestTypes.ALL_TYPES[0], 0),
                "time": timestamp(),
                "bot_id": 0,
                "type": admin_requests.RequestTypes.ALL_TYPES[0].type,
                "host_inv": 0,
                "host_uuid": "00000000000000000000000000000000",
            },
        )

        self.failure_reports = ObjectMocker(
            walle.failure_reports.base.ErrorReportModel,
            {
                "report_key": "BURNE-10001",
                "stream_key": "mock-stream-key",
                "report_date": datetime.date.today(),
                "create_time": int(time.time()),
                "last_update_time": int(time.time()),
                "hosts": [
                    {
                        "inv": 1,
                        "name": "hostname-mock",
                        "status": "status-mock",
                        "project": self.default_project.id,
                        "reason": "reason-mock",
                        "section": "MockReportSection",
                        "report_timestamp": timestamp(),
                    }
                ],
            },
        )

        self.automation_plot = ObjectMocker(
            walle.expert.automation_plot.AutomationPlot,
            {
                "id": "plot-mock",
                "name": "Plot mock",
                "owners": ["wall-e", "@ya_group"],
                "checks": [
                    {
                        "name": "UNREACHABLE",
                        "enabled": True,
                        "reboot": True,
                        "redeploy": True,
                    }
                ],
            },
        )

        self.location_names_map = ObjectMocker(
            walle.physical_location_tree.LocationNamesMap, {"path": "MOCK|PATH", "name": "mock_name"}
        )

        self.dmc_rules = ObjectMocker(
            walle.hosts.DMCRule,
            {
                "id": 0,
                "rule_query": {"physical_location__nin": ["COUNTRY|CITY|DATACENTER|QUEUE|RACK"]},
            },
        )

        self.maintenance_plots = ObjectMocker(
            walle.maintenance_plot.model.MaintenancePlotModel,
            {
                "id": "mocked-maintenance-plot-id",
                "meta_info": {
                    "abc_service_slug": "mock-abc-service-slug",
                    "name": "Maintenance plot mock",
                },
                "common_settings": {
                    "maintenance_approvers": {"logins": ["login-1-mock", "login-2-mock"]},
                    "common_scenario_settings": {
                        "total_number_of_active_hosts": 100,
                        "dont_allow_start_scenario_if_total_number_of_active_hosts_more_than": 1000,
                    },
                },
                "scenarios_settings": [
                    {
                        "scenario_type": ScriptName.NOOP,
                        "settings": {
                            "foo": 42,
                        },
                    },
                ],
                "gc_enabled": False,
            },
        )

    def tearDown(self):
        self.__monkeypatch.undo()
        while self.__patchers:
            patcher = self.__patchers.pop()
            patcher.stop()

        super().tearDown()

    @classmethod
    def create(cls, request, healthdb=None):
        test = cls("setUp", healthdb=healthdb)
        test.setUp()
        request.addfinalizer(test.tearDown)
        return test

    def mock_host_network(self, overrides=None, host=None, *args, **kwargs):
        if overrides:
            overrides = overrides.copy()
        else:
            overrides = {}
        if host:
            overrides.setdefault("uuid", host.uuid)
            if host.active_mac:
                overrides.setdefault("active_mac", host.active_mac)
            if host.active_mac_source:
                overrides.setdefault("active_mac_source", host.active_mac_source)
            if host.ips:
                overrides.setdefault("ips", host.ips)
            if host.location:
                if host.location.switch:
                    overrides.setdefault("network_switch", host.location.switch)
                if host.location.port:
                    overrides.setdefault("network_port", host.location.port)
                if host.location.network_source:
                    overrides.setdefault("network_source", host.location.network_source)
        return self.host_network.mock(overrides, *args, **kwargs)

    def mock_host(self, overrides=None, task_kwargs=None, *args, **kwargs):
        if overrides:
            overrides = overrides.copy()
        else:
            overrides = {}

        if "inv" in overrides:
            if "save" not in kwargs or kwargs["save"]:
                overrides.setdefault("uuid", mock_uuid_for_inv(overrides["inv"]))
            overrides.setdefault("name", "mocked-{}.mock".format(overrides["inv"]))

        if overrides.get("state") == HostState.ASSIGNED:
            overrides.setdefault("provisioner", walle_constants.PROVISIONER_LUI)
            overrides.setdefault("config", "config-mock")

        if overrides.get("status") in HostStatus.ALL_TASK:
            overrides.setdefault("task", mock_task(**task_kwargs if task_kwargs is not None else {}))

        overrides.setdefault("type", HostType.SERVER)

        return self.hosts.mock(overrides, *args, **kwargs)

    def mock_project(self, overrides, *args, **kwargs):
        overrides = overrides.copy()
        overrides.setdefault("name", "Name for " + overrides["id"])
        overrides, roles = extract_project_roles(overrides)
        project = self.projects.mock(overrides, *args, **kwargs)
        set_project_roles(project, roles)
        return project

    def mock_scenario(
        self, overrides=None, resolve_uuids=True, work_status: tp.Optional[ScenarioWorkStatus] = None, *args, **kwargs
    ):
        if overrides:
            overrides = overrides.copy()
        else:
            overrides = {}

        if "hosts" in overrides:
            overrides["hosts"] = Scenario.create_list_of_host_states(overrides["hosts"], resolve_uuids=resolve_uuids)
        if "hosts_states" in overrides:
            overrides["hosts"] = overrides["hosts_states"]
            del overrides["hosts_states"]

        if overrides.get('labels') is None:
            overrides["labels"] = {}
        labels = overrides["labels"]
        if not labels.get(WORK_STATUS_LABEL_NAME):
            labels[WORK_STATUS_LABEL_NAME] = work_status or ScenarioWorkStatus.CREATED

        return self.scenarios.mock(overrides, *args, **kwargs)

    def mock_maintenance_plot(self, *args, **kwargs):
        return self.maintenance_plots.mock(*args, **kwargs)

    def mock_dmc_rule(self, *args, **kwargs):
        return self.dmc_rules.mock(*args, **kwargs)

    def mock_projects(self, **kwargs):
        for id in range(999, 996, -1):
            self.projects.mock(dict({"id": "mocked-{}".format(id), "name": "Mocked #{}".format(id)}, **kwargs))

    @classmethod
    def project_profile(cls):
        if cls.__project_profile is None:
            cls.__project_profile = FLEXY_EINE_PROFILE
        return cls.__project_profile

    def __patch(self, patcher):
        patcher.start()
        self.__patchers.append(patcher)


def extract_project_roles(overrides):
    """Project roles are not set directly to Project fields, but rather set via project role managers"""
    roles = {role: members for role, members in overrides.pop("roles", {}).items() if role in ProjectRole.ALL}

    # owners are treated in a special way to keep compatibility with all the tests passing owners field
    # (which is not even stored in Project anymore, but we don't want to rewrite all the tests)
    if ProjectRole.OWNER not in roles:
        roles[ProjectRole.OWNER] = overrides.pop("owners", [TestCase.api_user])

    return overrides, roles


def set_project_roles(project, roles):
    for role, members in roles.items():
        role_manager = ProjectRole.get_role_manager(role, project)
        # remove previously set members if any
        for existing_member in role_manager.list_members():
            role_manager.remove_member(existing_member)

        if members is not None:
            for member in members:
                role_manager.add_member(member)


# Test utils


def generate_host_action_authentication_tests(module_globals, action="", data=None, methods="POST"):
    if isinstance(methods, str):
        methods = (methods,)

    def generate(method):
        @pytest.mark.parametrize("host_id_field", ["inv", "name"])
        def test_unauthenticated(test, unauthenticated, host_id_field):
            host = test.mock_host({"inv": 0})
            result = test.api_client.open(hosts_api_url(host, host_id_field, action), method=method, data=data)
            assert result.status_code == http.client.UNAUTHORIZED
            test.hosts.assert_equal()

        @pytest.mark.parametrize("host_id_field", ["inv", "name"])
        def test_unauthorized(test, unauthorized_host, host_id_field):
            host = test.mock_host({"inv": 0})
            result = test.api_client.open(hosts_api_url(host, host_id_field, action), method=method, data=data)
            assert result.status_code == http.client.FORBIDDEN
            test.hosts.assert_equal()

        for test in test_unauthenticated, test_unauthorized:
            name = test.__name__ + "_" + method
            if name in module_globals:
                raise Error("Can't register '{}' test: it already exists.", name)

            module_globals[name] = test

    for method in methods:
        generate(method)


def hosts_api_url(host, host_id_field="name", action=""):
    host_id = str(getattr(host, host_id_field))
    return "/v1/hosts/" + host_id + action


def check_stage_initialization(test, stage, status=None):
    host = test.mock_host({"task": mock_task(stages=[stage], stage_uid=None)})

    expected_stage = Stage.from_json(stage.to_json())
    expected_stage.status_time = timestamp()

    if status is not None:
        expected_stage.status = status

    enter_task(host)
    assert json.loads(stage.to_json()) == json.loads(expected_stage.to_json())


def handle_host(host, suppress_internal_errors=tuple()):
    fsm._handle_host(host.uuid, host.task.task_id, suppress_internal_errors=suppress_internal_errors)


def cancel_stage(host):
    host = host.copy()
    stage = get_current_stage(host)
    handler = walle.fsm_stages.common._get_stage_config(stage.name).cancellation_handler
    handler(host, stage)


def handle_failure(host, decision, reasons, automation_plot=None):
    return dmc.handle_failure(healing_automation(host.project, plot=automation_plot), host.copy(), decision, reasons)


def handle_monitoring(host, decision, reasons, automation_plot=None):
    return dmc.handle_monitoring(healing_automation(host.project, plot=automation_plot), host.copy(), decision, reasons)


def fix_dns(host):
    with patch("walle.clients.dns.slayer_dns_api.DnsClient", autospec=False):
        _try_fix_host_dns_records(host.copy(), Q(inv=host.inv), {host.project: host.get_project()})


# Object mocking


def mock_object(obj, return_value=_UNSPECIFIED, side_effect=_UNSPECIFIED):
    kwargs = drop_none(dict(return_value=return_value, side_effect=side_effect), none=_UNSPECIFIED)
    return create_autospec(obj, spec_set=True, **kwargs)


def mock_location(**kwargs):
    location_kwargs = mock_network_location_kwargs()
    location_kwargs["network_source"] = location_kwargs.pop("source")
    location_kwargs.pop("timestamp")

    location_kwargs.update(physical_timestamp=timestamp(), **mock_physical_location_kwargs())

    if kwargs:
        location_kwargs.update(**kwargs)

    return HostLocation(**location_kwargs)


def mock_host_health_status(
    juggler_check_time=None,
    checks=CheckType.ALL,
    check_overrides=(),
    check_status=None,
    human_reasons=None,
    reasons=None,
    **kwargs,
):
    kwargs.setdefault("status", HealthStatus.STATUS_OK)

    if reasons is None:
        reasons = mock_status_reasons(
            juggler_check_time,
            enabled_checks=checks,
            check_overrides=check_overrides,
            check_status=check_status,
            **kwargs,
        )

    check_statuses = {}
    for check_type, check in reasons.items():
        check_statuses[walle.expert.types.get_walle_check_type(check_type)] = check["status"]

    for check in checks:
        if walle.expert.types.get_walle_check_type(check) not in check_statuses:
            check_statuses[walle.expert.types.get_walle_check_type(check)] = CheckStatus.VOID

    if human_reasons is None:
        human_reasons = []
        for check, status in check_statuses.items():
            if status != CheckStatus.PASSED:
                human_reasons.append(check + "." + status)

    kwargs.setdefault("check_statuses", check_statuses)
    kwargs.setdefault("reasons", sorted(human_reasons) or None)

    return HealthStatus(**kwargs)


def mock_status_reasons(
    juggler_check_time=None, enabled_checks=CheckType.ALL, check_overrides=(), check_status=None, **kwargs
):
    checks = kwargs.pop("checks", None)
    if checks is None:
        checks = mock_host_health_checks(
            juggler_check_time=juggler_check_time,
            enabled_checks=enabled_checks,
            check_overrides=check_overrides,
            check_status=check_status,
            **kwargs,
        )

    return get_health_status_reasons(checks, enabled_checks, kwargs.pop("check_min_time", None))


def mock_host_health_checks(
    juggler_check_time=None, enabled_checks=CheckType.ALL, check_overrides=(), check_status=None, **kwargs
):
    cur_time = timestamp()

    event_time = kwargs.setdefault("event_time", cur_time)
    status_mtime = kwargs.pop("status_mtime", (juggler_check_time or event_time) - NETWORK_CHECKS_REACTION_TIMEOUT - 1)
    stale_timestamp = kwargs.pop("stale_timestamp", None)
    effective_timestamp = kwargs.pop("effective_timestamp", None)

    if check_status is None:
        if kwargs.get("status", HealthStatus.STATUS_OK) == HealthStatus.STATUS_OK:
            check_status = CheckStatus.PASSED
        else:
            check_status = CheckStatus.FAILED
    check_overrides_dict = {check["type"]: check for check in check_overrides}

    def check_time(check_type):
        recovering_checks = {
            CheckType.NETMON: NETMON_REACTION_TIMEOUT,
            CheckType.WALLE_RACK: RACK_FAILURE_REACTION_TIMEOUT,
        }
        return event_time - (recovering_checks[check_type] + 1 if check_type in recovering_checks else 0)

    checks = [
        {
            "type": check,
            "status": CheckStatus.PASSED if check in CheckType.ALL_INFRASTRUCTURE else check_status,
            "timestamp": juggler_check_time or event_time,
            "status_mtime": status_mtime,
            "stale_timestamp": stale_timestamp or (juggler_check_time or event_time) + JUGGLER_PUSH_PERIOD,
            "effective_timestamp": effective_timestamp or juggler_check_time or check_time(check),
            "metadata": {
                "mountpoints": {"/": "0%"},
                "reason": ["Reason mock"],
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    # temporary prosthetics: rule can ignore CRIT because it can not fix some types of problems.
                    "reason": ["availability: Reason mock" if check == CheckType.GPU else "Reason mock"],
                    "socket": 1,
                    "device_list": [],
                    "eine_code": ["GPU_MISSING"] if check == CheckType.GPU else ["INFINIBAND_MISMATCH"],
                },
                "results": {
                    "ecc": {
                        "status": HwWatcherCheckStatus.FAILED,
                        "slot": 0,
                        "reason": ["Reason mock"],
                        "comment": "ecc error",
                    },
                    "mem": {
                        "status": HwWatcherCheckStatus.FAILED,
                        "reason": ["Reason mock"],
                        "comment": "mem error",
                    },
                },
            },
        }
        for check in enabled_checks
        if check not in check_overrides_dict
    ]

    checks.extend(check_overrides)
    return checks


def mock_decision(action, params=None, checks=None, reason=None, failure_type=None, redeploy=True):
    action_restrictions = None
    if params is None:
        if action == WalleAction.CHANGE_DISK:
            params = {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "shelf_inv": "10009",
                "errors": ["error1", "error2"],
                "redeploy": redeploy,
                "diskperformance": "somevalue",
            }
            action_restrictions = [restrictions.AUTOMATED_REDEPLOY]
        elif action == WalleAction.REPAIR_HARDWARE:
            params = {
                "operation": Operation.REPAIR_HARDWARE.type,
                "request_type": "request-type-mock",
                "errors": ["error1", "error2"],
            }
    elif action == WalleAction.PROFILE and params["profile_mode"] == ProfileMode.DISK_RW_TEST:
        action_restrictions = [restrictions.AUTOMATED_REDEPLOY, restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP]
        params.setdefault("check_limits", True)

    return Decision(
        action,
        reason or "Reason mock.",
        params,
        checks=checks,
        restrictions=action_restrictions,
        failure_type=failure_type,
    )


class MockRule(AbstractRule):
    def __init__(self, fail=False, wait=False):
        self.fail = fail
        self.wait = wait

    def apply(self, host, reasons, enabled_checks):
        if self.fail:
            return Decision.failure("Mock fail reason")
        elif self.wait:
            return Decision.wait("Mock wait reason")
        return Decision.healthy("Mock healthy reason")


def mock_task(
    stage=None,
    stage_status=None,
    stage_params=None,
    stage_data=None,
    stage_temp_data=None,
    stage_status_time=None,
    stage_terminators=None,
    parent_stage_name=PARENT_STAGE,
    parent_next_stage_name=PARENT_NEXT_STAGE,
    parent_stage_params=None,
    parent_stage_data=None,
    previous_stage_data=None,
    **kwargs,
):
    kwargs.setdefault("task_id", 0)
    kwargs.setdefault("type", TaskType.AUTOMATED_HEALING)
    kwargs.setdefault(
        "owner",
        authorization.ISSUER_WALLE
        if kwargs["type"] == TaskType.AUTOMATED_HEALING
        else authorization.ISSUER_ANONYMOUS_USER,
    )
    kwargs.setdefault("audit_log_id", audit_log._uuid())
    kwargs.setdefault("target_status", HostStatus.READY)

    if stage is not None:
        if isinstance(stage, str):
            stage = Stage(
                name=stage,
                params=stage_params,
                status=stage_status,
                status_time=stage_status_time,
                data=stage_data,
                temp_data=stage_temp_data,
                terminators=stage_terminators,
            )

        stage.status_time = stage.status_time or timestamp()
        kwargs.setdefault(
            "stages",
            [
                Stage(
                    name=parent_stage_name,
                    params=parent_stage_params,
                    data=parent_stage_data,
                    stages=[
                        Stage(name=PREV_STAGE, data=previous_stage_data),
                        stage,
                        Stage(name=NEXT_STAGE),
                    ],
                ),
                Stage(name=parent_next_stage_name),
            ],
        )

    kwargs["stages"] = walle.stages.set_uids(kwargs.get("stages", []))

    if stage is not None:
        kwargs.setdefault("stage_uid", stage.uid)
        kwargs.setdefault("stage_name", stage.name)

    kwargs.setdefault("status", "status-mock")
    kwargs.setdefault("next_check", timestamp())
    kwargs.setdefault("revision", 0)

    return Task(**kwargs)


def mock_host_macs():
    return ["00:00:00:00:00:10", "00:00:00:00:00:00", "00:00:00:00:00:01"]


def mock_physical_location_kwargs(bot=False):
    if bot:
        short_names = {}
    else:
        short_names = dict(short_datacenter_name="mdc", short_queue_name="m-queue")
    return dict(
        short_names,
        country="country-mock",
        city="city-mock",
        datacenter="dc-mock",
        queue="queue-mock",
        rack="rack-mock",
        unit="unit-mock",
    )


def mock_network_location_kwargs():
    return dict(
        switch="switch-mock", port="port-mock", timestamp=timestamp(), source=walle_constants.NETWORK_SOURCE_EINE
    )


def mock_startrek_client(mp, issue_status="open"):
    client = mp.function(startrek.get_client, module=startrek).return_value
    client.get_issue.return_value = {"status": {"key": issue_status}}
    client.add_comment.return_value = {"id": "some_comment_id"}
    return client


def get_mocked_ok_client():
    mocked_ok_client = Mock()
    mocked_ok_client.attach_mock(Mock(return_value=ok.Approvement(None, None, None)), "get_approvement")
    mocked_ok_client.attach_mock(Mock(return_value=ok.Approvement(None, None, None, uuid=1)), "create_approvement")
    mocked_ok_client.attach_mock(Mock(), "close_approvement")
    return mocked_ok_client


def mock_ok_client(mp):
    ok_client = get_mocked_ok_client()
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=ok_client)
    return ok_client


# Monkey patching tools


def patch(target, autospec=True, return_value=_UNSPECIFIED, side_effect=_UNSPECIFIED):
    kwargs = drop_none(dict(return_value=return_value, side_effect=side_effect), none=_UNSPECIFIED)

    if autospec:
        kwargs.update(autospec=True)
    else:
        kwargs.update(new_callable=Mock, spec_set=True)

    return mock.patch(target, **kwargs)


def patch_attr(monkeypatch, obj, attr, return_value=_UNSPECIFIED, side_effect=_UNSPECIFIED):
    mock_obj = mock_object(getattr(obj, attr), return_value=return_value, side_effect=side_effect)
    monkeypatch.setattr(obj, attr, mock_obj)
    return mock_obj


def monkeypatch_function(
    monkeypatch, func, module=None, return_value=_UNSPECIFIED, side_effect=_UNSPECIFIED, wrap_original=False
):
    if module is None:
        module = sys.modules[func.__module__]

    # wrap_original is used to wrap method with mock, but still call the original code
    if wrap_original:
        if side_effect != _UNSPECIFIED or return_value != _UNSPECIFIED:
            raise RuntimeError("wrap_original disables side_effect and return_value")
        return_value = None
        side_effect = getattr(module, func.__name__)

    kwargs = drop_none(dict(return_value=return_value, side_effect=side_effect), none=_UNSPECIFIED)
    func_mock = create_autospec(func, spec_set=True, **kwargs)
    monkeypatch.setattr(module, func.__name__, func_mock)
    return func_mock


def monkeypatch_method(
    monkeypatch, method, obj=None, return_value=_UNSPECIFIED, side_effect=_UNSPECIFIED, wrap_original=False
):
    # wrap_original is used to wrap method with mock, but still call the original code
    if wrap_original:
        if side_effect != _UNSPECIFIED or return_value != _UNSPECIFIED:
            raise RuntimeError("wrap_original disables side_effect and return_value")
        return_value = None

    method_name = method.__name__

    if not obj:
        for attr in ("__self__", "im_class"):
            try:
                obj = getattr(method, attr)
            except AttributeError:
                pass
    if not obj:
        raise RuntimeError("Pass a holder class of this staticmethod so that I could patch it.")

    import inspect

    obj_cls = obj if inspect.isclass(obj) else type(obj)
    for cls in inspect.getmro(obj_cls):
        if hasattr(cls, "__dict__") and method_name in cls.__dict__:
            if isinstance(cls.__dict__[method_name], (staticmethod, classmethod)):
                attribute = cls.__dict__[method_name]
                method_type = type(attribute)
                mock_obj = mock_object(attribute.__func__, return_value=return_value, side_effect=side_effect)
                monkeypatch.setattr(cls, method_name, method_type(mock_obj))
                return mock_obj

    if wrap_original:
        orig_method = getattr(obj, method_name)
        side_effect = orig_method

    return patch_attr(monkeypatch, obj=obj, attr=method_name, return_value=return_value, side_effect=side_effect)


# Wall-E objects/functions monkey patching


def monkeypatch_check_rule(monkeypatch, decision, check_type, check_rule):
    monkeypatch_method(monkeypatch, check_rule.apply, return_value=decision, obj=check_rule)

    reasons = mock_status_reasons()
    reasons[check_type]["status"] = CheckStatus.FAILED
    monkeypatch_function(monkeypatch, juggler.get_host_health_reasons, return_value=reasons)

    return reasons


def monkeypatch_config(monkeypatch, key, value):
    section_key = key.rsplit(".", 1)
    if len(section_key) == 1:
        monkeypatch.setitem(config._CONFIG, key, value)
        return
    section, key = section_key
    monkeypatch.setitem(config.get_value(section), key, value)


def mock_box_config(mp, monkeypatch_config, root_boxes_section, box_section_name, box_section_content):
    monkeypatch_config(mp, root_boxes_section, {})
    monkeypatch_config(mp, f"{root_boxes_section}.{box_section_name}", box_section_content)


def monkeypatch_audit_log(monkeypatch, uuid=AUDIT_LOG_ID, time=None, patch_create=True):
    if uuid is not None:
        monkeypatch.setattr(audit_log, "_uuid", lambda: uuid)

    if time is not None:
        monkeypatch.setattr(audit_log, "_time", lambda: time)

    if patch_create:
        monkeypatch.setattr(audit_log, "create", lambda *args, **kwargs: LogEntry(id=uuid))


def monkeypatch_host_uuid(monkeypatch, uuid_=HOST_UUID):
    monkeypatch_function(monkeypatch, hosts.uuid4, module=hosts, return_value=UUID(uuid_))


def monkeypatch_max_errors(monkeypatch, config_name, dont_allow):
    monkeypatch_config(monkeypatch, config_name, 1 if dont_allow else 2)


def monkeypatch_locks(monkeypatch):
    for lock_class in (
        AutomationPlotInterruptableLock,
        ProjectInterruptableLock,
        HostInterruptableLock,
        HostHealingInterruptableGlobalLock,
        ScenarioInterruptableLock,
        CronJobInterruptableLock,
        MaintenancePlotInterruptableLock,
        ScenarioAcquiringInterruptableGlobalLock,
    ):
        monkeypatch.setattr(lock_class, "__init__", lambda *args, **kwargs: None)
        monkeypatch.setattr(lock_class, "acquire", lambda *args, **kwargs: True)
        monkeypatch.setattr(lock_class, "acquired", lambda *args, **kwargs: True)
        monkeypatch.setattr(lock_class, "release", lambda *args, **kwargs: None)


def mock_response(data=None, status_code=200, headers=_UNSPECIFIED, as_json=True, **kwargs):
    resp = client_utils.requests.Response()
    resp.status_code = status_code

    if headers is not _UNSPECIFIED:
        resp.headers = headers
    elif as_json:
        resp.headers = {"Content-Type": "application/json"}

    resp._content = six.ensure_binary(json.dumps(data) if as_json else data)

    for attrname, val in kwargs.items():
        setattr(resp, attrname, val)

    return resp


def monkeypatch_request(monkeypatch, return_value=_UNSPECIFIED, side_effect=_UNSPECIFIED):
    kwargs = drop_none(dict(return_value=return_value, side_effect=side_effect), none=_UNSPECIFIED)
    return monkeypatch_function(monkeypatch, client_utils.requests.request, module=client_utils.requests, **kwargs)


def monkeypatch_inventory_get_host_info_and_check_status(
    monkeypatch,
    hostname=_UNSPECIFIED,
    bot_project_id=BOT_PROJECT_ID,
    ipmi_mac=TestCase.ipmi_mac,
    incr_bot_project_id_on_call=False,
):
    def get_host_info_and_check_status(inv_or_name, **kwargs):
        nonlocal bot_project_id
        cur_bot_project_id = bot_project_id
        if incr_bot_project_id_on_call:
            bot_project_id += 1

        if isinstance(inv_or_name, int):
            inv = inv_or_name
            return inventory.BotHostInfo(
                inv=inv,
                name=("mocked-{}.mock".format(inv) if hostname is _UNSPECIFIED else hostname),
                ipmi_mac=ipmi_mac,
                macs=TestCase.macs,
                location=bot.HardwareLocation(**mock_physical_location_kwargs(bot=True)),
                bot_project_id=cur_bot_project_id,
                platform=HostPlatform(system='system_model', board='board_model'),
            )
        else:
            name = inv_or_name.lower()
            return inventory.BotHostInfo(
                inv=int(re.sub(r"[-a-zA-Z.]", "", name) or "0"),
                name=name,
                ipmi_mac=ipmi_mac,
                macs=TestCase.macs,
                location=bot.HardwareLocation(**mock_physical_location_kwargs(bot=True)),
                bot_project_id=cur_bot_project_id,
                platform=HostPlatform(system='system_model', board='board_model'),
            )

    monkeypatch_function(
        monkeypatch,
        inventory.get_host_info_and_check_status,
        module=inventory,
        side_effect=get_host_info_and_check_status,
    )


def monkeypatch_network_get_current_host_switch_port(monkeypatch):
    monkeypatch_function(
        monkeypatch,
        network.get_current_host_switch_port,
        side_effect=lambda *args, **kwargs: HostNetworkLocationInfo(**mock_network_location_kwargs()),
    )


def monkeypatch_request_params_validation(monkeypatch):
    monkeypatch_function(monkeypatch, inventory.check_deploy_configuration)


def monkeypatch_expert(monkeypatch, enabled):
    monkeypatch_config(monkeypatch, "expert_system.enabled", enabled)


def monkeypatch_enabled_checks(monkeypatch, disabled=None):
    enabled_checks = set(CheckType.ALL) - set(disabled or [])
    monkeypatch.setattr(walle.expert.automation_plot, "_get_enabled_checks", lambda project_id: enabled_checks)


def monkeypatch_automation_plot_id(monkeypatch, plot_id):
    mocked_decision_maker_cache_key = walle.expert.decisionmakers._DecisionMakerCacheKey(
        plot_id,
        has_infiniband=False,
    )
    monkeypatch.method(
        walle.expert.decisionmakers._DecisionMakerCacheKey.from_project,
        return_value=mocked_decision_maker_cache_key,
        obj=walle.expert.decisionmakers._DecisionMakerCacheKey,
    )


def mock_config_content_dm(disk_type="hdd"):
    config = {
        "md": {},
        "parted": {"{}_1".format(disk_type): ["grub", "*"]},
        "fs": {
            "lv0": [
                "ext4",
                "-b 4096",
                "/",
                "barrier=1,noatime,lazytime",
            ],
            "lv1": [
                "ext4",
                "-b 4096",
                "/home",
                "barrier=1,noatime,lazytime,nosuid,nodev",
            ],
            "lv2": [
                "ext4",
                "-b 4096",
                "/place",
                "barrier=1,noatime,lazytime,nosuid,nodev",
            ],
        },
        "lvm": {
            "lv1": ["home", "6G", "vg0", "--addtag diskman.sys=true"],
            "vg0": [disk_type, "{}_1_2".format(disk_type), "--addtag diskman=true"],
            "lv0": ["root", "40G", "vg0", "--addtag diskman.sys=true"],
            "lv2": ["place", "300G", "vg0", "--addtag diskman.sys=true"],
        },
        "system": {
            "destroy_all_partition_tables": True,
        },
    }
    return config


# Clients monkey patching


def monkeypatch_eine_client_for_host(
    monkeypatch,
    host,
    status=eine.EineProfileStatus.STOPPED,
    profile=None,
    tags=None,
    local_tags=None,
    eaas_ticket=False,
    switch_info=None,
    mock_internal_error=False,
    mock_persistent_error=False,
):
    client = mock_object(eine.EineClient)

    client.get_tags.return_value = set(tags or [])
    client.set_host_location.return_value = None

    if profile:
        client.get_profiles.return_value = [profile]
    else:
        client.get_profiles.return_value = walle_constants.EINE_PROFILES_WITH_DC_SUPPORT + [
            walle_constants.FLEXY_EINE_PROFILE,
            walle_constants.EINE_IPXE_FROM_CDROM_PROFILE,
        ]

    if status is None:
        client.get_host_status.side_effect = eine.EinePersistentError("Mock persistent error")
    else:
        deploy_configuration = host.get_deploy_configuration()
        properties = {"props": {}}
        if eaas_ticket:
            properties["props"]["otrs_ticket"] = {"value": "ITDC-0"}

        if switch_info:
            properties["props"]["switch"] = {"value": switch_info, "timestamp": timestamp() - 100}

        if not properties["props"]:
            properties = {}  # remove whole attribute from response

        client.get_host_status.return_value = eine.EineHostStatus(
            dict(
                {
                    "in_use": True,
                    "einstellung": {
                        "profile_name": deploy_configuration.config if profile is None else profile,
                        "status": eine.EineProfileStatus.ALL.index(status),
                        "assigned_at": timestamp(),
                        "updated_at": timestamp(),
                        "tags_local": ["eaas"] if local_tags is None else local_tags,
                        "current_stage": 0,
                        "stages": [
                            {
                                "stage": "stage-mock",
                                "status": eine.EineStageState.ALL.index(eine.EineStageState.RUNNING),
                            }
                        ],
                    },
                },
                **properties,
            )
        )

    for method in "assign_profile", "get_host_status", "set_host_location", "get_tags", "update_tags", "get_profiles":
        mocked_method = getattr(client, method)

        if mock_internal_error:
            mocked_method.side_effect = eine.EineInternalError("Mocked internal Einstellung error.")
        elif mock_persistent_error:
            mocked_method.side_effect = eine.EinePersistentError("Mocked persistent Einstellung error.")

    monkeypatch.setattr(eine, "get_client", lambda provider: client)
    return client


def monkeypatch_deploy_client_for_host(
    monkeypatch,
    host,
    status=walle.clients.deploy.STATUS_INVALID,
    status_time=None,
    lui_status=None,
    macs=None,
    config=None,
    fail_count=0,
    redeploy_fail_count=0,
    mock_internal_error=False,
    mock_config_list=None,
    mock_persistent_error=False,
    persistent_error_methods=None,
    internal_error_methods=None,
):
    persistent_error_methods = persistent_error_methods or []
    internal_error_methods = internal_error_methods or []
    client = mock_object(deploy.DeployClient)

    if config is None and host.task is not None:
        stage = get_current_stage(host, only_if_exists=True)
        if stage is not None and stage.name in (Stages.ASSIGN_LUI_CONFIG, Stages.LUI_INSTALL):
            try:
                config = DeployConfiguration(*get_parent_stage(host, stage=stage).get_param("config")).config
            except Error:
                config = stage.get_param("config")

    side_effect_internal = deploy.DeployInternalError("Mocked internal LUI error.")
    side_effect_persistent = deploy.DeployPersistentError("Mocked persistent LUI error.")

    if (
        "get_deploy_status" not in persistent_error_methods
        and "get_deploy_status" not in internal_error_methods
        and not mock_persistent_error
        and not mock_internal_error
    ):
        client.schedule_redeploy.return_value = {"lui_status": "PENDING", "fail_count": redeploy_fail_count}

        def get_deploy_status(*args, **kwargs):
            if macs is None:
                deploy_macs = mock_host_macs()
            elif not isinstance(macs, list):
                deploy_macs = macs()
            else:
                deploy_macs = macs

            return {
                "macs": deploy_macs,
                "config": host.get_config() if config is None else config,
                "lui_status": "provisioner-status-mock" if lui_status is None else lui_status,
                "status": status,
                "description": "",
                "fail_count": fail_count,
                "modify_time": timestamp() if status_time is None else status_time,
            }

        client.get_deploy_status.side_effect = get_deploy_status

    if mock_internal_error or mock_persistent_error or internal_error_methods or persistent_error_methods:
        for attr in dir(deploy.DeployClient):
            if not attr.startswith("_") and callable(getattr(deploy.DeployClient, attr)):
                if mock_internal_error or attr in internal_error_methods:
                    getattr(client, attr).side_effect = side_effect_internal
                elif mock_persistent_error or attr in persistent_error_methods:
                    getattr(client, attr).side_effect = side_effect_persistent

    monkeypatch.setattr(deploy, "get_client", lambda provider: client)
    if mock_config_list is None:
        monkeypatch_function(monkeypatch, deploy.get_deploy_configs, return_value=[config])
    else:
        monkeypatch_function(monkeypatch, deploy.get_deploy_configs, return_value=mock_config_list)

    return client


def monkeypatch_hw_client_for_host(
    monkeypatch,
    host,
    power_on=True,
    hw_available=True,
    mock_internal_hw_error=False,
    mock_hw_lookup_error=False,
    raw_cmd_result=None,
):
    client = mock_object(ipmiproxy.IpmiProxyClient)

    if mock_internal_hw_error:
        side_effect = ipmiproxy.InternalError("Internal hardware error mock.")
    elif mock_hw_lookup_error:
        side_effect = ipmiproxy.IpmiHostMissingError(TestCase.ipmi_host)
    elif not hw_available:
        side_effect = ipmiproxy.HostHwError("Hardware error mock.")
    else:
        client.is_power_on.return_value = power_on
        client.raw_command.return_value = raw_cmd_result if raw_cmd_result else {"success": False}
        side_effect = None

    if side_effect is not None:
        for attr in dir(ipmiproxy.IpmiProxyClient):
            if not attr.startswith("_") and callable(getattr(ipmiproxy.IpmiProxyClient, attr)):
                getattr(client, attr).side_effect = side_effect

    def get_client(provider, human_id):
        assert human_id == host.human_id()
        return client

    monkeypatch.setattr(ipmiproxy, "get_client", get_client)

    return client


def monkeypatch_ssh_client_for_host(monkeypatch, host, boot_id=None, mock_ssh_error=False, mock_ssh_auth_error=False):
    client = mock_object(ssh.SshClient)

    if mock_ssh_error:
        side_effect = ssh.SshConnectionFailedError("ssh connection failed mock")
    elif mock_ssh_auth_error:
        side_effect = ssh.SshAuthenticationError("ssh authentication failed mock")
    else:
        side_effect = None

    if side_effect is not None:
        for attr in dir(ssh.SshClient):
            if not attr.startswith("_") and callable(getattr(ssh.SshClient, attr)):
                getattr(client, attr).side_effect = side_effect

    else:
        client.get_boot_id.return_value = boot_id

    @contextmanager
    def mock_get_ssh_client(hostname):
        assert hostname == host.name
        if mock_ssh_auth_error:
            raise side_effect
        yield client

    monkeypatch.setattr(ssh, "get_client", mock_get_ssh_client)

    return client


def monkeypatch_clients_for_host(
    monkeypatch,
    host,
    power_on=_UNSPECIFIED,
    hw_available=_UNSPECIFIED,
    status_time=_UNSPECIFIED,
    mock_internal_hw_error=_UNSPECIFIED,
    mock_hw_lookup_error=_UNSPECIFIED,
    deploy_status=_UNSPECIFIED,
    lui_deploy_status=_UNSPECIFIED,
    deploy_macs=_UNSPECIFIED,
    deploy_config=_UNSPECIFIED,
    deploy_config_list=_UNSPECIFIED,
    deploy_fail_count=_UNSPECIFIED,
    redeploy_fail_count=_UNSPECIFIED,
    mock_internal_deploy_error=_UNSPECIFIED,
    mock_persistent_deploy_error=_UNSPECIFIED,
    profile_status=_UNSPECIFIED,
    profile_name=_UNSPECIFIED,
    eaas_ticket=_UNSPECIFIED,
    switch_info=_UNSPECIFIED,
    mock_internal_profile_error=_UNSPECIFIED,
    mock_persistent_profile_error=_UNSPECIFIED,
    tags=_UNSPECIFIED,
    profile_local_tags=_UNSPECIFIED,
    deploy_internal_error_methods=_UNSPECIFIED,
    deploy_persistent_error_methods=_UNSPECIFIED,
    boot_id=_UNSPECIFIED,
    mock_ssh_error=_UNSPECIFIED,
    mock_ssh_auth_error=_UNSPECIFIED,
    raw_cmd_result=_UNSPECIFIED,
):
    clients = Mock()

    hw_kwargs = dict(
        power_on=power_on,
        hw_available=hw_available,
        mock_internal_hw_error=mock_internal_hw_error,
        mock_hw_lookup_error=mock_hw_lookup_error,
        raw_cmd_result=raw_cmd_result,
    )

    deploy_kwargs = dict(
        status=deploy_status,
        status_time=status_time,
        lui_status=lui_deploy_status,
        macs=deploy_macs,
        config=deploy_config,
        fail_count=deploy_fail_count,
        redeploy_fail_count=redeploy_fail_count,
        mock_config_list=deploy_config_list,
        mock_internal_error=mock_internal_deploy_error,
        mock_persistent_error=mock_persistent_deploy_error,
        internal_error_methods=deploy_internal_error_methods,
        persistent_error_methods=deploy_persistent_error_methods,
    )

    eine_kwargs = dict(
        status=profile_status,
        profile=profile_name,
        mock_internal_error=mock_internal_profile_error,
        mock_persistent_error=mock_persistent_profile_error,
        tags=tags,
        local_tags=profile_local_tags,
        eaas_ticket=eaas_ticket,
        switch_info=switch_info,
    )

    ssh_kwargs = dict(boot_id=boot_id, mock_ssh_error=mock_ssh_error, mock_ssh_auth_error=mock_ssh_auth_error)

    for kwargs in hw_kwargs, deploy_kwargs, eine_kwargs, ssh_kwargs:
        for arg, value in list(kwargs.items()):
            if value is _UNSPECIFIED:
                del kwargs[arg]

    hw_client = monkeypatch_hw_client_for_host(monkeypatch, host, **hw_kwargs)
    clients.attach_mock(hw_client, "hardware")

    deploy_client = monkeypatch_deploy_client_for_host(monkeypatch, host, **deploy_kwargs)
    clients.attach_mock(deploy_client, "deploy")

    eine_client = monkeypatch_eine_client_for_host(monkeypatch, host, **eine_kwargs)
    clients.attach_mock(eine_client, "eine")

    ssh_client = monkeypatch_ssh_client_for_host(monkeypatch, host, **ssh_kwargs)
    clients.attach_mock(ssh_client, "ssh")

    return clients


def mock_host_adding(
    host,
    manual=True,
    task=False,
    ignore_cms=None,
    check=True,
    deploy_config=None,
    location=None,
    dns=None,
    reason=None,
    assign_bot_project_id=True,
):
    host.set_status(host.status, get_issuer(manual), AUDIT_LOG_ID, confirmed=task, reason=reason)
    host.set_state(host.state, get_issuer(manual), AUDIT_LOG_ID, reason=reason)

    if location is None:
        location = mock_location()
    host.location = location
    host.rename_time = timestamp()

    if task:
        builder = walle.util.tasks.StageBuilder()
        builder.stage(Stages.ACQUIRE_PERMISSION, action=CmsTaskAction.PREPARE)

        if assign_bot_project_id:
            builder.stage(Stages.ASSIGN_BOT_PROJECT, bot_project_id=BOT_PROJECT_ID)

        builder.stage(name=Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
        builder.stage(name=Stages.SET_PROBATION, reason=reason)
        builder.add_stages(walle.util.tasks.get_power_on_stages())
        network_update_configuration = host.deduce_profile_configuration(profile_mode=ProfileMode.SWP_UP)
        builder.add_stages(walle._tasks.stages.get_network_update_stages(network_update_configuration, full=False))
        builder.stage(Stages.SWITCH_VLANS, network=NetworkTarget.PROJECT)

        if dns:
            builder.stage(Stages.SETUP_DNS)

        if deploy_config:
            builder.stage(Stages.LUI_SETUP, config=deploy_config)

        builder.stage(name=Stages.LOG_COMPLETED_OPERATION, operation=Operation.ADD.type)

        host.task = walle.util.tasks.new_task(
            task_type=TaskType.MANUAL if manual else TaskType.AUTOMATED_ACTION,
            stages=builder.get_stages(),
            host=host,
            ignore_cms=ignore_cms,
            target_status=host.status,
            monitor_on_completion=check,
            checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY,
            keep_downtime=True,
            **_new_task_params(manual),
        )
        host.task.task_id -= 1
        status = (
            Operation.SWITCH_TO_MAINTENANCE.host_status
            if host.state == HostState.MAINTENANCE
            else Operation.ADD.host_status
        )
        host.set_status(status, get_issuer(manual), host.task.audit_log_id, reason=reason)


def mock_host_adding_in_maintenance(
    host, manual=True, ignore_cms=None, reason=None, assign_bot_project_id=True, maintenance_properties=None, task=True
):
    state_expire = StateExpire(
        time=maintenance_properties.get("timeout_time"),
        ticket=maintenance_properties["ticket_key"],
        status=maintenance_properties.get("timeout_status", HostStatus.READY),
        issuer=get_issuer(manual),
    )
    host.set_state(host.state, get_issuer(manual), AUDIT_LOG_ID, expire=state_expire, reason=reason)
    host.set_status(host.status, get_issuer(manual), AUDIT_LOG_ID, confirmed=True, reason=reason)
    host.ticket = maintenance_properties["ticket_key"]
    host.operation_state = maintenance_properties.get("operation_state", HostOperationState.OPERATION)

    host.location = mock_location()
    host.rename_time = timestamp()

    if task:
        builder = walle.util.tasks.StageBuilder()
        cms_task_action = CmsTaskAction.PREPARE
        builder.stage(Stages.ACQUIRE_PERMISSION, action=cms_task_action)

        if assign_bot_project_id:
            builder.stage(Stages.ASSIGN_BOT_PROJECT, bot_project_id=BOT_PROJECT_ID)

        builder.stage(name=Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
        builder.stage(name=Stages.LOG_COMPLETED_OPERATION, operation=Operation.ADD.type)

        host.task = walle.util.tasks.new_task(
            task_type=TaskType.MANUAL if manual else TaskType.AUTOMATED_ACTION,
            stages=builder.get_stages(),
            host=host,
            ignore_cms=ignore_cms,
            checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY,
            keep_downtime=True,
            **_new_task_params(manual),
        )
        host.task.task_id -= 1
        status = Operation.ADD.host_status
        host.set_status(status, get_issuer(manual), host.task.audit_log_id, reason=reason)


# Stage actions mocking


def mock_switch_stage(host, current_stage, next_stage, check_after, initial_status=None, error=None):
    task = host.task

    del current_stage.status
    del current_stage.status_time
    del current_stage.temp_data

    next_stage.status = initial_status
    next_stage.status_time = timestamp()

    task.stage_uid = next_stage.uid
    task.stage_name = next_stage.name
    task.status = ":".join(filter(None, [next_stage.name, initial_status]))
    task.next_check = timestamp() + check_after
    task.error = error
    task.revision += 1


def mock_commit_stage_changes(
    host, status=None, status_message=None, error=None, extra_fields=None, temp_data=None, inc_revision=0, **kwargs
):
    fsm_stages.common._change_task_state(
        host, stage_status=status, status_message=status_message, error=error, **kwargs
    )

    if temp_data:
        stage = get_current_stage(host)
        for k, v in temp_data.items():
            stage.set_temp_data(k, v)

    host.task.revision += inc_revision
    assert host.task.error is error


def mock_complete_current_stage(
    host,
    expected_name=NEXT_STAGE,
    expected_status="mocked-next-stage-status",
    inc_revision=0,
    expected_parent_data=None,
    expected_data=None,
):
    stage = get_current_stage(host)
    del stage.status
    del stage.status_time
    del stage.temp_data

    stage = walle.stages.get_next(host.task.stages, stage.uid)
    assert stage.name == expected_name

    if expected_data is not None:
        get_current_stage(host).data = expected_data

    host.task.stage_uid = stage.uid
    host.task.stage_name = stage.name

    if expected_parent_data is not None:
        get_parent_stage(host).data = expected_parent_data

    fsm_stages.common._set_current_stage_status(host, stage, expected_status)
    mock_commit_stage_changes(host, status=expected_status, inc_revision=inc_revision, check_now=True)


def mock_complete_parent_stage(
    host, expected_name=PARENT_NEXT_STAGE, expected_status="mocked-parent-next-stage-status"
):
    stage = get_current_stage(host)
    del stage.status
    del stage.status_time
    del stage.temp_data

    stage = walle.stages.get_parent(host.task.stages, stage.uid)
    host.task.stage_uid = stage.uid
    host.task.stage_name = stage.name

    mock_complete_current_stage(host, expected_name=expected_name, expected_status=expected_status)


def mock_complete_parent_of_composite_stage(host):
    stage = get_current_stage(host)

    composite_stage = walle.stages.get_parent(host.task.stages, stage.uid)
    parent_stage = walle.stages.get_parent(host.task.stages, composite_stage.uid)

    host.task.stage_uid = parent_stage.uid
    host.task.stage_name = parent_stage.name

    mock_complete_current_stage(host)


mock_skip_current_stage = mock_complete_current_stage


def mock_retry_current_stage(
    host, expected_name, expected_status=None, error=None, persistent_error=None, inc_revision=0, **kwargs
):
    stage = get_current_stage(host)
    del stage.status
    del stage.status_time
    del stage.temp_data

    assert stage.name == expected_name

    host.task.stage_uid = stage.uid
    host.task.stage_name = stage.name

    if error is None and persistent_error is not None:
        error = persistent_error

    fsm_stages.common._set_current_stage_status(host, stage, expected_status)
    fsm_stages.common._set_persistent_error(stage, persistent_error)

    mock_commit_stage_changes(host, error=error, inc_revision=inc_revision, **kwargs)


def mock_retry_parent_stage(host, error=None, **kwargs):
    current_stage = get_current_stage(host)
    parent_stage = get_parent_stage(host)
    for stage in current_stage, parent_stage:
        del stage.status
        del stage.status_time
        del stage.temp_data

    if error:
        parent_stage.set_data("stage_error", error)

    host.task.stage_uid = parent_stage.uid
    host.task.stage_name = parent_stage.name
    host.task.error = error

    fsm_stages.common._set_current_stage_status(host, parent_stage, PARENT_STAGE_RETRY_STATUS)
    mock_commit_stage_changes(host, error=error, **kwargs)


def mock_fail_current_stage(host, reason=None):
    mock_host_deactivation(host, reason=reason)


def mock_stage_internal_error(host, error):
    host.task.error = error
    host.task.next_check = timestamp() + walle.fsm_stages.common.ERROR_CHECK_PERIOD


# Task actions mocking
def mock_schedule_host_deletion(host, lui=None, ignore_cms=None, disable_admin_requests=None):
    new_task_params = _new_task_params(manual=True)
    task_args = DeleteHostTaskArgs(
        issuer=new_task_params['issuer'],
        task_type=TaskType.MANUAL,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        lui=lui,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
    )
    sb = get_delete_host_task_stages(task_args)
    host.task = create_new_task(host, task_args, new_task_params['audit_entry'], sb)
    host.task.task_id -= 1
    host.set_status(Operation.DELETE.host_status, get_issuer(manual=True), host.task.audit_log_id)


def mock_schedule_wait_for_bot_acquirement(host):
    new_task_params = _new_task_params(manual=False)
    task_args = EnsureHostPreorderAcquirementTaskArgs(
        issuer=new_task_params["issuer"],
        task_type=TaskType.AUTOMATED_ACTION,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
    )
    sb = get_bot_acquirement_task_stages(task_args)
    host.task = create_new_task(host, task_args, new_task_params["audit_entry"], sb)
    host.task.task_id -= 1
    host.set_status(
        Operation.BOT_WAIT_FOR_HOST_ACQUIREMENT.host_status, get_issuer(manual=False), host.task.audit_log_id
    )


# TODO Modify to test CMS task properly
def mock_schedule_project_switching(
    host,
    project_id,
    release=False,
    erase_disks=True,
    bot_project_id=BOT_PROJECT_ID,
    host_restrictions=None,
    ignore_cms=False,
    disable_admin_requests=None,
    drop_cms_task=False,
    fix_default_cms=False,
    force_new_cms_task=True,
    manual=True,
    task_type=TaskType.MANUAL,
):
    builder = walle.util.tasks.StageBuilder()

    if host.state == HostState.FREE:
        ignore_cms = True

    host_restrictions = strip_restrictions(host_restrictions, strip_to_none=True)

    if release and host.state != HostState.FREE:
        builder.stage(
            Stages.ACQUIRE_PERMISSION,
            action=CmsTaskAction.DEACTIVATE,
            force_new_cms_task=force_new_cms_task,
            ignore_cms=ignore_cms,
        )

        host_restrictions = None  # we do not allow to set custom restrictions when releasing host
        builder.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DELETED_HOST)

        if erase_disks:
            builder.add_stages(
                walle.util.tasks.get_profile_stages(
                    EineProfileOperation.RELEASE, FLEXY_EINE_PROFILE, profile_tags=[EineProfileTags.DRIVES_WIPE]
                )
            )

        terminators = {StageTerminals.FAIL: StageTerminals.SKIP} if disable_admin_requests else None
        builder.stage(Stages.POWER_OFF, soft=False, terminators=terminators)

        builder.stage(
            Stages.SWITCH_VLANS,
            network=NetworkTarget.PARKING,
            terminators={StageTerminals.SWITCH_MISSING: StageTerminals.SKIP},
        )
        builder.stage(Stages.SETUP_DNS, clear=True)
        builder.stage(Stages.LUI_REMOVE)

        with builder.nested(Stages.SET_HOSTNAME) as nested:
            nested.stage(Stages.ALLOCATE_HOSTNAME, free=True)
            nested.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DELETED_HOST)
            nested.stage(Stages.ASSIGN_HOSTNAME)
        if drop_cms_task:
            builder.stage(Stages.DROP_CMS_TASK, cms_task_id=host.cms_task_id)
        builder.stage(Stages.COMPLETE_RELEASING)
    else:
        builder.stage(Stages.ACQUIRE_PERMISSION, action=CmsTaskAction.DEACTIVATE, ignore_cms=ignore_cms)
        if fix_default_cms:
            builder.stage(
                Stages.SWITCH_DEFAULT_CMS_PROJECT,
                cms_task_id=host.cms_task_id,
                source_project_id=host.project,
                target_project_id=project_id,
            )

    builder.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.TRANSITION_STATE_HOST)
    builder.stage(Stages.SWITCH_PROJECT, project=project_id, host_restrictions=host_restrictions)
    builder.stage(Stages.ADD_HOST_TO_CAUTH, terminators={StageTerminals.FAIL: StageTerminals.SKIP})

    if bot_project_id is not None:
        builder.stage(Stages.ASSIGN_BOT_PROJECT, bot_project_id=bot_project_id)

    new_task_params = _new_task_params(manual=manual)
    task_args = ProjectSwitchingArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        operation_restrictions=host_restrictions,
        release=release,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
        erase_disks=erase_disks,
        current_project_id=host.project,
        target_project_id=project_id,
        target_project_bot_project_id=bot_project_id,
        host_state=host.state,
        monitor_on_completion=False,
        cms_task_id=host.cms_task_id,
    )
    host.task = create_new_task(host, task_args, new_task_params['audit_entry'], builder)
    host.task.task_id -= 1
    host.set_status(Operation.SWITCH_PROJECT.host_status, get_issuer(manual=manual), host.task.audit_log_id)


def mock_schedule_release_host(
    host,
    erase_disks=True,
    ignore_cms=False,
    disable_admin_requests=None,
    drop_cms_task=False,
    force_new_cms_task=False,
    manual=True,
    task_type=TaskType.MANUAL,
    reason=None,
):
    builder = walle.util.tasks.StageBuilder()

    if host.state != HostState.FREE:
        force_new_cms_task = True
    else:
        ignore_cms = True

    builder.stage(
        Stages.ACQUIRE_PERMISSION,
        action=CmsTaskAction.DEACTIVATE,
        force_new_cms_task=force_new_cms_task,
        ignore_cms=ignore_cms,
        comment=reason,
    )

    builder.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DELETED_HOST)

    if erase_disks:
        builder.add_stages(
            walle.util.tasks.get_profile_stages(
                EineProfileOperation.RELEASE, FLEXY_EINE_PROFILE, profile_tags=[EineProfileTags.DRIVES_WIPE]
            )
        )

    terminators = {StageTerminals.FAIL: StageTerminals.SKIP} if disable_admin_requests else None
    builder.stage(Stages.POWER_OFF, soft=False, terminators=terminators)

    builder.stage(
        Stages.SWITCH_VLANS,
        network=NetworkTarget.PARKING,
        terminators={StageTerminals.SWITCH_MISSING: StageTerminals.SKIP},
    )
    builder.stage(Stages.SETUP_DNS, clear=True)
    builder.stage(Stages.LUI_REMOVE)

    with builder.nested(Stages.SET_HOSTNAME) as nested:
        nested.stage(Stages.ALLOCATE_HOSTNAME, free=True)
        nested.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DELETED_HOST)
        nested.stage(Stages.ASSIGN_HOSTNAME)
    if drop_cms_task:
        builder.stage(Stages.DROP_CMS_TASK, cms_task_id=host.cms_task_id)
    builder.stage(Stages.COMPLETE_RELEASING)

    new_task_params = _new_task_params(manual=manual)

    task_args = HostReleaseArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
        erase_disks=erase_disks,
        current_project_id=host.project,
        host_state=host.state,
        monitor_on_completion=False,
        cms_task_id=host.cms_task_id,
    )
    host.task = create_new_task(host, task_args, new_task_params['audit_entry'], builder)
    host.task.task_id -= 1
    host.set_status(
        Operation.RELEASE_HOST.host_status, get_issuer(manual=manual), host.task.audit_log_id, reason=reason
    )


def mock_schedule_maintenance(
    host,
    timeout_time=None,
    timeout_status=HostStatus.READY,
    ticket_key="MOCK-0001",
    power_off=False,
    ignore_cms=False,
    disable_admin_requests=False,
    workdays_only=None,
    cms_task_action=CmsTaskAction.PROFILE,
    cms_task_group=None,
    operation_state=HostOperationState.OPERATION,
    manual=True,
    issuer=None,
    reason=None,
    task_type=TaskType.MANUAL,
):
    new_task_params = _new_task_params(manual=manual, issuer=issuer)
    task_args = SwitchToMaintenanceTaskArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        disable_admin_requests=disable_admin_requests,
        monitor_on_completion=False,
        timeout_time=timeout_time,
        timeout_status=timeout_status,
        ticket_key=ticket_key,
        power_off=power_off,
        ignore_cms=ignore_cms,
        keep_downtime=True,
        reason=reason,
    )
    sb = walle.util.tasks.StageBuilder()
    sb.stage(
        name=Stages.ACQUIRE_PERMISSION,
        action=cms_task_action,
        task_group=cms_task_group,
        comment=reason,
        workdays=workdays_only,
    )
    sb.stage(name=Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
    sb.stage(
        name=Stages.SET_MAINTENANCE,
        ticket_key=ticket_key,
        timeout_time=timeout_time,
        timeout_status=timeout_status,
        operation_state=operation_state,
        reason=reason,
    )
    if power_off:
        sb.stage(name=Stages.POWER_OFF, soft=True)

    host.task = create_new_task(host, task_args, new_task_params['audit_entry'], sb)

    host.task.task_id -= 1
    host.ticket = ticket_key
    host.set_status(
        Operation.SWITCH_TO_MAINTENANCE.host_status, new_task_params['issuer'], host.task.audit_log_id, reason=reason
    )


def mock_schedule_assigned(
    host,
    status,
    manual=True,
    disable_admin_requests=False,
    power_on=False,
    reason=None,
    unset_ticket=False,
    with_auto_healing=None,
    from_scenario=False,
    checks_for_use=None,
    without_ipmi=False,
):
    sb = walle.util.tasks.StageBuilder()
    if power_on and not without_ipmi:
        sb.add_stages(walle.util.tasks.get_power_on_stages())

    sb.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.TRANSITION_STATE_HOST)
    sb.stage(Stages.SET_ASSIGNED, reason=reason)
    sb.stage(Stages.LOG_COMPLETED_OPERATION, operation=Operation.SWITCH_TO_ASSIGNED.type)

    host.task = walle.util.tasks.new_task(
        task_type=TaskType.MANUAL if manual else TaskType.AUTOMATED_ACTION,
        stages=sb.get_stages(),
        host=host,
        disable_admin_requests=disable_admin_requests,
        target_status=status,
        monitor_on_completion=manual or from_scenario,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY if (manual or from_scenario) else None,
        with_auto_healing=with_auto_healing,
        checks_for_use=checks_for_use,
        **_new_task_params(manual=manual),
    )
    host.task.task_id -= 1
    host.set_status(
        Operation.SWITCH_TO_ASSIGNED.host_status, get_issuer(manual=manual), host.task.audit_log_id, reason=reason
    )
    if unset_ticket:
        del host.ticket


def mock_schedule_host_power_on(
    host,
    expected_notify_fsm_calls=None,
    disable_admin_requests=False,
    check=True,
    with_auto_healing=None,
    check_post_code_override=None,
    task_type=TaskType.MANUAL,
    extra_checks=(),
):
    new_task_params = _new_task_params(manual=True)
    check_post_code, _ = (
        walle.util.tasks.check_post_code_allowed(host, task_type, with_auto_healing)
        if check_post_code_override is None
        else (
            check_post_code_override,
            None,
        )
    )
    checks_to_monitor = sorted(set(CheckGroup.NETWORK_AVAILABILITY) | set(extra_checks))

    task_args = PowerOnTaskArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        disable_admin_requests=disable_admin_requests,
        monitor_on_completion=check,
        with_auto_healing=with_auto_healing,
        check_post_code=check_post_code,
        checks_to_monitor=checks_to_monitor,
        ignore_cms=True,
    )

    sb = get_power_on_task_stages(task_args)
    host.task = create_new_task(host, task_args, new_task_params["audit_entry"], sb)

    host.task.task_id -= 1

    host.set_status(Operation.POWER_ON.host_status, get_issuer(manual=True), host.task.audit_log_id)

    if expected_notify_fsm_calls is not None:
        expected_notify_fsm_calls.append(call())


def mock_schedule_host_power_off(
    host,
    expected_notify_fsm_calls=None,
    ignore_cms=False,
    disable_admin_requests=False,
    reason=None,
    task_type=TaskType.MANUAL,
    with_auto_healing=None,
    manual=True,
):
    # TODO: protected member accessed from outside?
    new_task_params = _new_task_params(manual=manual)
    task_args = PowerOffTaskArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_name=host.name,
        host_inv=host.inv,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        disable_admin_requests=disable_admin_requests,
        ignore_cms=ignore_cms,
        reason=reason,
        soft=True,
        with_auto_healing=with_auto_healing,
    )

    sb = get_power_off_task_stages(task_args)
    host.task = create_new_task(host, task_args, new_task_params["audit_entry"], sb)
    host.task.task_id -= 1

    host.set_status(Operation.POWER_OFF.host_status, get_issuer(manual=manual), host.task.audit_log_id, reason=reason)

    if expected_notify_fsm_calls is not None:
        expected_notify_fsm_calls.append(call())


def mock_schedule_host_reboot(
    host,
    manual,
    expected_notify_fsm_calls=None,
    ignore_cms=False,
    ssh=SshOperation.FORBID,
    disable_admin_requests=False,
    check=True,
    with_auto_healing=None,
    extra_checks=(),
    issuer=None,
    reason=None,
    check_post_code_override=None,
    task_type=None,
    failure_type=None,
    oplog_params=None,
    use_cloud_post_processor=False,
):
    new_task_params = _new_task_params(manual)
    failure = None

    if issuer:
        new_task_params["issuer"] = issuer

    task_type = task_type if task_type else TaskType.MANUAL if manual is True else TaskType.AUTOMATED_HEALING

    if extra_checks:
        failure = walle.expert.types.get_walle_check_type(extra_checks[0])

    check_post_code, _ = (
        walle.util.tasks.check_post_code_allowed(host, task_type, with_auto_healing)
        if check_post_code_override is None
        else (
            check_post_code_override,
            None,
        )
    )
    checks_to_monitor = sorted(set(CheckGroup.NETWORK_AVAILABILITY) | {CheckType.W_META} | set(extra_checks))
    operation_restrictions = (
        restrictions.AUTOMATED_REBOOT if issuer == authorization.ISSUER_WALLE else restrictions.REBOOT
    )

    task_args = RebootTaskArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        ssh=ssh,
        disable_admin_requests=disable_admin_requests,
        monitor_on_completion=check,
        with_auto_healing=with_auto_healing,
        check_post_code=check_post_code,
        ignore_cms=ignore_cms,
        reason=reason,
        checks_to_monitor=checks_to_monitor,
        failure=failure,
        check_names=extra_checks or None,
        operation_restrictions=operation_restrictions,
        failure_type=failure_type,
        operation_log_params=oplog_params,
        without_ipmi=host.type != walle_constants.HostType.SERVER,
        use_cloud_post_processor=use_cloud_post_processor,
        profile_after_task=True,
        redeploy_after_task=True,
    )
    if use_cloud_post_processor:
        task_args.cms_action = CmsTaskAction.REDEPLOY

    sb = get_reboot_task_stages(task_args)
    host.task = create_new_task(host, task_args, new_task_params["audit_entry"], sb)
    host.task.task_id -= 1

    host.set_status(Operation.REBOOT.host_status, new_task_params['issuer'], host.task.audit_log_id, reason=reason)

    if expected_notify_fsm_calls is not None:
        expected_notify_fsm_calls.append(call())


def mock_schedule_host_kexec_reboot(
    host,
    manual,
    expected_notify_fsm_calls=None,
    ignore_cms=False,
    disable_admin_requests=False,
    check=True,
    with_auto_healing=None,
    extra_checks=(),
    issuer=None,
    reason=None,
    task_type=None,
    failure_type=None,
    oplog_params=None,
):
    new_task_params = _new_task_params(manual)
    failure = None

    if issuer:
        new_task_params["issuer"] = issuer

    task_type = task_type if task_type else TaskType.MANUAL if manual is True else TaskType.AUTOMATED_HEALING

    if extra_checks:
        failure = walle.expert.types.get_walle_check_type(extra_checks[0])

    checks_to_monitor = sorted(set(CheckGroup.NETWORK_AVAILABILITY) | {CheckType.W_META} | set(extra_checks))
    operation_restrictions = (
        restrictions.AUTOMATED_REBOOT if issuer == authorization.ISSUER_WALLE else restrictions.REBOOT
    )

    task_args = KexecRebootTaskArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        disable_admin_requests=disable_admin_requests,
        monitor_on_completion=check,
        with_auto_healing=with_auto_healing,
        ignore_cms=ignore_cms,
        reason=reason,
        checks_to_monitor=checks_to_monitor,
        failure=failure,
        check_names=extra_checks or None,
        operation_restrictions=operation_restrictions,
        failure_type=failure_type,
        operation_log_params=oplog_params,
    )

    sb = get_kexec_reboot_task_stages(task_args)
    host.task = create_new_task(host, task_args, new_task_params["audit_entry"], sb)
    host.task.task_id -= 1

    host.set_status(Operation.REBOOT.host_status, new_task_params['issuer'], host.task.audit_log_id, reason=reason)

    if expected_notify_fsm_calls is not None:
        expected_notify_fsm_calls.append(call())


def mock_schedule_host_profiling(
    host,
    manual,
    profile=None,
    profile_tags=None,
    profile_mode=None,
    provisioner=None,
    deploy_config=None,
    deploy_tags=None,
    deploy_config_forced=False,
    deploy_network=None,
    deploy_config_policy=None,
    need_certificate=False,
    extra_checks=(),
    failure=None,
    failure_type=None,
    expected_notify_fsm_calls=None,
    ignore_cms=False,
    disable_admin_requests=None,
    check=True,
    with_auto_healing=None,
    issuer=None,
    reason=None,
    task_type=None,
    operation_type=None,
    repair_request_severity=None,
    force_update_network_location=False,
    use_cloud_post_processor=False,
):
    # This code was nearly same as the original code from tasks._new_profile_and_deploy_task.
    profile, profile_tags, profile_modes = host.deduce_profile_configuration(profile, profile_tags, profile_mode)

    cms_action = CmsTaskAction.PROFILE
    if provisioner is not None or use_cloud_post_processor:
        cms_action = CmsTaskAction.REDEPLOY
    health_status_accuracy = None

    if use_cloud_post_processor:
        if provisioner is None:
            provisioner = constants.PROVISIONER_LUI
        if deploy_config is None:
            deploy_config = "config-mock"

    builder = walle.util.tasks.StageBuilder()
    builder.stage(
        Stages.ACQUIRE_PERMISSION,
        action=cms_action,
        comment=reason,
        check_names=sorted(extra_checks) or None,
        failure=failure,
        failure_type=failure_type,
    )
    builder.stage(Stages.SET_DOWNTIME)

    builder.add_stages(
        _mock_profile_stages(
            EineProfileOperation.PROFILE,
            Stages.PROFILE,
            profile,
            profile_tags,
            profile_modes,
            operation_type=operation_type,
            repair_request_severity=repair_request_severity,
            force_update_network_location=force_update_network_location,
        )
    )

    if provisioner is not None:
        deploy_configuration = DeployConfiguration(
            provisioner,
            deploy_config,
            deploy_tags,
            need_certificate,
            deploy_network or NetworkTarget.DEFAULT,
            ipxe=True,
            deploy_config_policy=deploy_config_policy,
        )

        builder.add_stages(
            _mock_deploy_stages(
                deploy_configuration,
                config_forced=deploy_config_forced,
                allow_upgrade=(not manual or with_auto_healing),
                profile_tags=profile_tags,
            )
        )

        if deploy_configuration.provisioner == PROVISIONER_LUI:
            health_status_accuracy = deploy.COMPLETED_STATUS_TIME_ACCURACY

    # Einstellung performs deploy/profile process in the service VLAN,
    # so we have switch the host to project VLANs after the profile stage,
    # unless we need to redeploy host via eine afterwards.
    builder.stage(Stages.SWITCH_VLANS, network=NetworkTarget.PROJECT)
    builder.add_stages(walle.util.tasks.get_power_on_stages())

    new_task_params = _new_task_params(manual)
    if issuer:
        new_task_params["issuer"] = issuer

    host.task = walle.tasks.new_task(
        stages=builder.get_stages(),
        host=host,
        task_type=task_type if task_type else TaskType.MANUAL if manual else TaskType.AUTOMATED_HEALING,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
        monitor_on_completion=check,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY + list(extra_checks),
        health_status_accuracy=health_status_accuracy,
        with_auto_healing=with_auto_healing,
        **new_task_params,
    )

    host.task.task_id -= 1

    host.set_status(Operation.PROFILE.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason)
    if provisioner is not None:
        host.provisioner = provisioner
        host.config = deploy_config
        host.deploy_config_policy = deploy_config_policy
        host.deploy_tags = deploy_tags
        host.deploy_network = deploy_network

    if expected_notify_fsm_calls is not None:
        expected_notify_fsm_calls.append(call())


def _mock_profile_stages(
    operation,
    stage_name,
    profile,
    profile_tags=None,
    profile_modes=None,
    allow_upgrade=False,
    operation_type=None,
    repair_request_severity=None,
    force_update_network_location=False,
):
    # This was the original code from tasks._get_profile_stages. It has evolved since, but is has not been modernized.
    terminators = None
    if allow_upgrade:
        terminators = {StageTerminals.DEPLOY_FAILED: StageTerminals.DISK_RW_AND_REDEPLOY}

    # Eine may not have enough permissions to switch host's port to the deploy VLAN, so help her.
    network_stage = Stage(
        name=Stages.NETWORK,
        stages=[
            Stage(
                name=Stages.UPDATE_NETWORK_LOCATION,
                terminators={StageTerminals.FAIL: StageTerminals.COMPLETE_PARENT},
                params=dict(force=force_update_network_location),
            ),
            Stage(
                name=Stages.SWITCH_VLANS,
                params={"network": NetworkTarget.SERVICE},
                # If switch is unknown we shouldn't fail: profiling may be the only way to determine host's switch.
                terminators={StageTerminals.SWITCH_MISSING: StageTerminals.COMPLETE_PARENT},
            ),
        ],
    )

    profile_stages = []

    if profile not in EINE_PROFILES_WITH_DC_SUPPORT:
        profile_stages += [
            # Use power cycle to detect and fix possible IPMI errors
            Stage(name=Stages.POWER_OFF, params={"soft": operation == EineProfileOperation.PROFILE}),
        ]

    profile_stages += [
        Stage(
            name=Stages.EINE_PROFILE,
            params=drop_none(
                {
                    "vlans_stage": network_stage.stages[1].get_uid(),
                    "operation": operation,
                    "profile": profile,
                    "profile_tags": profile_tags,
                    "repair_request_severity": repair_request_severity,
                }
            ),
            terminators=terminators,
        )
    ]

    if profile_modes:
        profile_stages += [
            Stage(
                name=Stages.LOG_COMPLETED_OPERATION,
                params={
                    "operation": Operation.PROFILE.type if operation_type is None else operation_type,
                    "params": {"modes": profile_modes},
                },
            )
        ]

    return [
        network_stage,
        Stage(name=stage_name, stages=profile_stages),
    ]


def _mock_deploy_stages(deploy_configuration, config_forced=False, allow_upgrade=False, profile_tags=None):
    # This was the original code from the tasks._get_deploy_stages
    sb = walle.util.tasks.StageBuilder()
    provisioner, deploy_config, deploy_tags, need_certificate, deploy_network, support_ipxe, _ = deploy_configuration

    if provisioner == PROVISIONER_LUI:
        upgrade_terminators = {
            StageTerminals.SUCCESS: StageTerminals.COMPLETE_PARENT,
            StageTerminals.DEPLOY_FAILED: StageTerminals.SKIP,
        }

        with sb.nested(Stages.DEPLOY, config=deploy_configuration._asdict(), config_forced=config_forced) as lui_stages:
            lui_stages.stage(name=Stages.GENERATE_CUSTOM_DEPLOY_CONFIG)
            lui_stages.stage(name=Stages.DROP_EINE_PROFILE)
            lui_stages.stage(name=Stages.SWITCH_VLANS, network=NetworkTarget.DEPLOY)

            lui_stages.stage(name=Stages.ISSUE_CERTIFICATE)

            lui_stages.stage(name=Stages.POWER_OFF)

            lui_stages.stage(name=Stages.ASSIGN_LUI_CONFIG)

            if deploy_configuration.ipxe:
                lui_stages.add_stages(walle.util.tasks.get_power_on_stages(pxe=True))
            else:
                lui_stages.stage(
                    name=Stages.EINE_PROFILE,
                    profile=EINE_IPXE_FROM_CDROM_PROFILE,
                    operation=EineProfileOperation.DEPLOY,
                )
                lui_stages.stage(
                    Stages.LOG_COMPLETED_OPERATION,
                    operation=Operation.PROFILE.type,
                    params=dict(operation=EineProfileOperation.DEPLOY),
                )

            lui_stages.stage(name=Stages.LUI_INSTALL, terminators=upgrade_terminators if allow_upgrade else None)
            lui_stages.stage(Stages.LUI_DEACTIVATE)

            if allow_upgrade:
                vlans_stage = lui_stages.stage(
                    Stages.SWITCH_VLANS,
                    network=NetworkTarget.SERVICE,
                    terminators={StageTerminals.SWITCH_MISSING: StageTerminals.SKIP},
                )

                deploy_stage_profile_tags = DEFAULT_PROFILE_TAGS.copy()
                if profile_tags:
                    deploy_stage_profile_tags |= set(profile_tags)

                lui_stages.stage(
                    Stages.EINE_PROFILE,
                    operation=EineProfileOperation.PROFILE,
                    profile=FLEXY_EINE_PROFILE,
                    profile_tags=sorted(deploy_stage_profile_tags),
                    vlans_stage=vlans_stage.get_uid(),
                    terminators={
                        StageTerminals.NO_ERROR_FOUND: StageTerminals.RETRY_ACTION,
                        StageTerminals.SUCCESS: StageTerminals.RETRY_ACTION,
                    },
                )
                lui_stages.stage(
                    Stages.LOG_COMPLETED_OPERATION,
                    operation=Operation.PROFILE.type,
                    params=dict(operation=EineProfileOperation.PROFILE),
                )

            if False:  # temporarily disabled WALLE-3511
                decision_params = repair_hardware_params(
                    operation_type=Operation.REPORT_SECOND_TIME_NODE.type,
                    request_type=RequestTypes.SECOND_TIME_NODE.type,
                    redeploy=False,
                )
                lui_stages.stage(
                    Stages.HW_REPAIR,
                    decision_params=decision_params,
                    decision_reason="Host failed to boot from PXE.",
                    terminators={StageTerminals.SUCCESS: StageTerminals.RETRY_ACTION},
                )

    elif provisioner == PROVISIONER_EINE:
        sb.add_stages(
            _mock_profile_stages(
                EineProfileOperation.DEPLOY,
                Stages.DEPLOY,
                deploy_config,
                profile_tags=deploy_tags,
                allow_upgrade=allow_upgrade,
            )
        )
    else:
        raise LogicalError()

    sb.stage(name=Stages.LOG_COMPLETED_OPERATION, operation=Operation.REDEPLOY.type)

    return sb.get_stages()


def mock_schedule_report_task(host, checks, reason, issuer=None):
    new_task_params = _new_task_params(manual=False)
    if issuer:
        new_task_params["issuer"] = issuer

    builder = walle.util.tasks.StageBuilder()
    builder.stage(Stages.ACQUIRE_PERMISSION)
    builder.stage(Stages.REPORT, checks=checks, reason=reason),
    builder.stage(Stages.LOG_COMPLETED_OPERATION, operation=Operation.REPORT_FAILURE.type),

    host.task = walle.tasks.new_task(
        task_type=TaskType.AUTOMATED_HEALING,
        ignore_cms=True,
        stages=builder.get_stages(),
        host=host,
        monitor_on_completion=True,
        checks_to_monitor=checks,
        **new_task_params,
    )

    host.task.task_id -= 1
    host.set_status(
        Operation.REPORT_FAILURE.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason
    )


def mock_schedule_repair_rack_task(host, checks, reason, issuer=None):
    new_task_params = _new_task_params(manual=False)
    if issuer:
        new_task_params["issuer"] = issuer

    operation = Operation.REPAIR_RACK_FAILURE

    builder = walle.util.tasks.StageBuilder()
    builder.stage(Stages.ACQUIRE_PERMISSION)
    builder.stage(Stages.REPORT_RACK, checks=checks, reason=reason),
    builder.stage(Stages.LOG_COMPLETED_OPERATION, operation=operation.type),

    host.task = walle.tasks.new_task(
        task_type=TaskType.AUTOMATED_HEALING,
        ignore_cms=True,
        stages=builder.get_stages(),
        host=host,
        monitor_on_completion=True,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY,
        **new_task_params,
    )

    host.task.task_id -= 1
    host.set_status(operation.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason)


def mock_schedule_repair_rack_overheat_task(host, checks, reason, issuer=None):
    new_task_params = _new_task_params(manual=False)
    if issuer:
        new_task_params["issuer"] = issuer

    operation = Operation.REPAIR_RACK_OVERHEAT

    builder = walle.util.tasks.StageBuilder()
    builder.stage(Stages.ACQUIRE_PERMISSION)
    builder.stage(Stages.REPORT_RACK_OVERHEAT, checks=checks, reason=reason),
    builder.stage(Stages.LOG_COMPLETED_OPERATION, operation=operation.type),

    host.task = walle.tasks.new_task(
        task_type=TaskType.AUTOMATED_HEALING,
        ignore_cms=True,
        stages=builder.get_stages(),
        host=host,
        monitor_on_completion=True,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY,
        **new_task_params,
    )

    host.task.task_id -= 1
    host.set_status(operation.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason)


def mock_schedule_hardware_repair(host, decision, issuer=None, use_cloud_post_processor=False):
    new_task_params = _new_task_params(manual=False)
    params = decision.params.copy()

    if issuer:
        new_task_params["issuer"] = issuer

    builder = walle.util.tasks.StageBuilder()
    cms_action = CmsTaskAction.REDEPLOY if use_cloud_post_processor else CmsTaskAction.PROFILE
    builder.stage(
        Stages.ACQUIRE_PERMISSION,
        action=cms_action,
        comment=decision.reason,
        check_names=decision.checks,
        failure_type=decision.failure_type,
        failure=decision.failures[0],
        extra=params,
    )
    builder.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)

    operation = Operation(params.pop("operation"))
    reboot = params.pop("reboot", False)

    if reboot:
        builder.stage(Stages.POWER_OFF, soft=True)

    if operation.type == Operation.REPORT_SECOND_TIME_NODE.type:
        builder.stage(Stages.PROVIDE_DIAGNOSTIC_HOST_ACCESS)

    builder.stage(
        Stages.HW_REPAIR,
        data={"orig_decision": decision.to_dict()},
        decision_params=params,
        decision_reason=decision.reason,
    )

    if reboot:
        builder.add_stages(walle.util.tasks.get_power_on_stages())

    builder.stage(
        Stages.LOG_COMPLETED_OPERATION,
        operation=operation.type,
        params=filter_dict_keys(params, ["request_type", "slot"]),
    ),

    host.task = walle.tasks.new_task(
        task_type=TaskType.AUTOMATED_HEALING,
        stages=builder.get_stages(),
        host=host,
        monitor_on_completion=True,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY + (decision.checks or []),
        use_cloud_post_processor=use_cloud_post_processor,
        profile_after_task=True,
        redeploy_after_task=True,
        **new_task_params,
    )

    host.task.task_id -= 1
    host.set_status(operation.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=decision.reason)


def mock_schedule_bmc_reset(
    host, issuer=None, reason=None, checks=None, failure=None, failure_type=None, use_cloud_post_processor=False
):
    new_task_params = _new_task_params(manual=False)
    if issuer:
        new_task_params["issuer"] = issuer

    builder = walle.util.tasks.StageBuilder()
    cms_action = CmsTaskAction.REDEPLOY if use_cloud_post_processor else CmsTaskAction.REBOOT
    builder.stage(
        Stages.ACQUIRE_PERMISSION,
        action=cms_action,
        check_names=checks,
        failure_type=failure_type,
        failure=failure,
        comment=reason,
    )
    builder.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
    builder.stage(Stages.RESET_BMC)
    builder.stage(Stages.LOG_COMPLETED_OPERATION, operation=Operation.RESET_BMC.type)

    host.task = walle.tasks.new_task(
        task_type=TaskType.AUTOMATED_HEALING,
        stages=builder.get_stages(),
        host=host,
        ignore_cms=False,
        monitor_on_completion=True,
        checks_to_monitor=checks or [CheckType.BMC],
        use_cloud_post_processor=use_cloud_post_processor,
        profile_after_task=True,
        redeploy_after_task=True,
        **new_task_params,
    )

    host.task.task_id -= 1
    host.set_status(Operation.RESET_BMC.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason)


def mock_schedule_check_host_dns(
    host,
    disable_admin_requests=False,
    check=True,
    with_auto_healing=None,
    task_type=TaskType.MANUAL,
    reason=None,
    manual=True,
    issuer=None,
):
    task_params = _new_task_params(manual=manual)
    if issuer:
        task_params["issuer"] = issuer

    sb = walle.util.tasks.StageBuilder()
    sb.stage(name=Stages.ACQUIRE_PERMISSION)
    network_update_configuration = host.deduce_profile_configuration(profile_mode=ProfileMode.SWP_UP)
    sb.add_stages(walle._tasks.stages.get_network_update_stages(network_update_configuration, full=False))
    sb.stage(name=Stages.SETUP_DNS)
    sb.stage(name=Stages.LOG_COMPLETED_OPERATION, operation=Operation.CHECK_DNS.type)

    host.task = walle.tasks.new_task(
        task_type=task_type,
        stages=sb.get_stages(),
        host=host,
        monitor_on_completion=check,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY,
        monitoring_timeout=DEFAULT_DNS_TTL,
        ignore_cms=True,
        disable_admin_requests=disable_admin_requests,
        with_auto_healing=with_auto_healing,
        **task_params,
    )

    host.task.task_id -= 1
    host.set_status(Operation.CHECK_DNS.host_status, task_params["issuer"], host.task.audit_log_id, reason=reason)


def mock_schedule_switch_vlans(
    host,
    manual=True,
    network=None,
    vlans=None,
    native_vlan=None,
    update_network_location=False,
    reason=None,
    task_type=TaskType.MANUAL,
):
    new_task_params = _new_task_params(manual=manual)
    network_update_args = host.deduce_profile_configuration(profile_mode=ProfileMode.SWP_UP)
    task_args = VlanSwitchTaskArgs(
        issuer=new_task_params["issuer"],
        task_type=task_type,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        network_target=network,
        vlans=vlans,
        native_vlan=native_vlan,
        network_update_args=network_update_args,
        update_network_location=update_network_location,
        reason=reason,
    )
    sb = get_vlan_switch_task_stages(task_args)
    host.task = create_new_task(host, task_args, new_task_params['audit_entry'], sb)

    host.task.task_id -= 1
    host.set_status(
        Operation.SWITCH_VLANS.host_status, new_task_params['issuer'], host.task.audit_log_id, reason=reason
    )


def mock_schedule_disk_change(host, decision, issuer=None):
    new_task_params = _new_task_params(manual=False)
    if issuer:
        new_task_params["issuer"] = issuer

    if "slot" in decision.params or "serial" in decision.params:
        reason = "Reason mock."
        profile_configuration, deploy_configuration = None, None

        if decision.params.get("redeploy", False):
            deploy_configuration = host.get_deploy_configuration()

        if "NVME_LINK_DEGRADED" not in decision.params.get("eine_code", []) and (
            "slot" in decision.params or "serial" in decision.params
        ):
            if decision.params.get("redeploy", False):
                profile_configuration = walle.tasks._get_profile_configuration_needed_for_redeploy(
                    host, deploy_configuration.provisioner
                )
        else:
            profile_configuration = host.deduce_profile_configuration(profile_mode=ProfileMode.DISK_RW_TEST)

        host.task = walle.tasks._new_disk_change_task(
            host,
            decision=decision,
            profile_configuration=profile_configuration,
            deploy_configuration=host.get_deploy_configuration()
            if ("redeploy" in decision.params and decision.params["redeploy"])
            else None,
            task_type=TaskType.AUTOMATED_HEALING,
            cms_extra=filter_dict_keys(decision.params, {"slot", "serial"}),
            reason=reason,
            **new_task_params,
        )
    else:
        reason = decision.reason
        deploy_configuration = host.get_deploy_configuration()
        profile, profile_tags, profile_modes = host.deduce_profile_configuration(profile_mode=ProfileMode.DISK_RW_TEST)

        host.task = walle.tasks._new_profile_and_deploy_task(
            profile=profile,
            profile_tags=profile_tags,
            profile_modes=profile_modes,
            deploy_configuration=deploy_configuration,
            task_type=TaskType.AUTOMATED_HEALING,
            extra_checks=[CheckType.DISK],
            reason=reason,
            **new_task_params,
        )

    host.task.task_id -= 1
    host.set_status(Operation.CHANGE_DISK.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason)


def mock_schedule_host_redeployment(
    host,
    manual,
    expected_notify_fsm_calls=None,
    config=None,
    provisioner=None,
    tags=None,
    network=None,
    need_certificate=False,
    support_ipxe=True,
    extra_checks=(),
    failure=None,
    ignore_cms=False,
    disable_admin_requests=None,
    check=True,
    with_auto_healing=None,
    custom_profile_mode=None,
    issuer=None,
    reason=None,
    deploy_config_policy=None,
    task_type=None,
    failure_type=None,
):
    if config is not None:
        host.config = config

    host.deploy_tags = tags
    host.deploy_network = network
    host.deploy_config_policy = deploy_config_policy
    deploy_configuration = DeployConfiguration(
        provisioner=provisioner or host.provisioner,
        config=config or host.config,
        tags=tags,
        certificate=need_certificate,
        network=network or NetworkTarget.DEFAULT,
        ipxe=support_ipxe,
        deploy_config_policy=deploy_config_policy,
    )

    builder = walle.util.tasks.StageBuilder()
    builder.stage(
        Stages.ACQUIRE_PERMISSION,
        action=CmsTaskAction.REDEPLOY,
        comment=reason,
        check_names=sorted(extra_checks) or None,
        failure_type=failure_type,
        failure=failure,
    )
    builder.stage(Stages.SET_DOWNTIME)

    if custom_profile_mode is not None:
        profile, profile_tags, profile_modes = host.deduce_profile_configuration(profile_mode=custom_profile_mode)
        builder.add_stages(
            _mock_profile_stages(EineProfileOperation.PROFILE, Stages.PROFILE, profile, profile_tags, profile_modes)
        )
    elif task_type in TaskType.ALL_AUTOMATED:
        profile, profile_tags, profile_modes = walle.tasks._get_profile_configuration_needed_for_redeploy(
            host, deploy_configuration.provisioner
        ) or (None, None, None)
        builder.add_stages(
            _mock_profile_stages(EineProfileOperation.PROFILE, Stages.PROFILE, profile, profile_tags, profile_modes)
        )
    else:
        profile, profile_tags, profile_modes = (None, None, None)

    builder.add_stages(
        _mock_deploy_stages(
            deploy_configuration,
            config_forced=config is not None,
            allow_upgrade=(not manual or with_auto_healing),
            profile_tags=profile_tags,
        )
    )

    builder.stage(Stages.SWITCH_VLANS, network=NetworkTarget.PROJECT)
    builder.add_stages(walle.util.tasks.get_power_on_stages())

    if deploy_configuration.provisioner == PROVISIONER_LUI:
        health_status_accuracy = deploy.COMPLETED_STATUS_TIME_ACCURACY
    else:
        health_status_accuracy = None

    new_task_params = _new_task_params(manual)
    if issuer:
        new_task_params["issuer"] = issuer

    host.task = walle.tasks.new_task(
        stages=builder.get_stages(),
        host=host,
        task_type=task_type if task_type else TaskType.MANUAL if manual else TaskType.AUTOMATED_HEALING,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
        monitor_on_completion=check,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY + list(extra_checks),
        health_status_accuracy=health_status_accuracy,
        with_auto_healing=with_auto_healing,
        **new_task_params,
    )

    host.task.task_id -= 1

    host.set_status(Operation.REDEPLOY.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason)

    if expected_notify_fsm_calls is not None:
        expected_notify_fsm_calls.append(call())


def mock_prepare_task_args(
    host,
    provisioner=None,
    config=None,
    deploy_config_policy=None,
    deploy_tags=None,
    deploy_network=None,
    need_certificate=False,
    restrictions=None,
    manual=True,
    ignore_cms=False,
    disable_admin_requests=None,
    check=True,
    with_auto_healing=False,
    extra_vlans=None,
    new_task_params=None,
):
    assert not ((provisioner is None) ^ (config is None))
    if provisioner is None:
        provisioner = TestCase.project_provisioner
    if config is None:
        config = TestCase.project_deploy_config
    deploy_configuration = DeployConfiguration(
        provisioner,
        config,
        deploy_tags,
        need_certificate,
        deploy_network or NetworkTarget.DEFAULT,
        ipxe=True,
        deploy_config_policy=deploy_config_policy,
    )
    health_accuracy = (
        deploy.COMPLETED_STATUS_TIME_ACCURACY if deploy_configuration.provisioner == PROVISIONER_LUI else None
    )
    return PrepareTaskArgs(
        issuer=new_task_params['issuer'],
        task_type=TaskType.MANUAL if manual else TaskType.AUTOMATED_ACTION,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
        monitor_on_completion=check,
        checks_to_monitor=CheckGroup.NETWORK_AVAILABILITY + CheckGroup.OS_CONSISTENCY,
        operation_restrictions=restrictions,
        provisioner=provisioner,
        config=config,
        extra_vlans=extra_vlans,
        deploy_tags=deploy_tags,
        deploy_network=deploy_network,
        deploy_config_policy=deploy_config_policy,
        network_target=NetworkTarget.PROJECT,
        deploy_configuration=deploy_configuration,
        with_auto_healing=True if with_auto_healing else None,
        health_status_accuracy=health_accuracy,
    )


def mock_schedule_host_preparing(
    host,
    profile=None,
    provisioner=None,
    config=None,
    deploy_config_policy=None,
    deploy_tags=None,
    deploy_network=None,
    need_certificate=False,
    custom_deploy_config=False,
    restrictions=None,
    manual=True,
    bot_project_id=BOT_PROJECT_ID,
    ignore_cms=False,
    disable_admin_requests=None,
    check=True,
    with_auto_healing=False,
    extra_vlans=None,
    profile_tags=None,
    profile_mode=None,
    preorder=False,
    reason=None,
    update_firmware_needed=False,
    repair_request_severity=None,
):
    sb = walle.util.tasks.StageBuilder()

    if preorder:
        cms_comment = "Host preparing has been requested by #{} preorder added by mocked-user@.".format(preorder)
    else:
        cms_comment = None

    sb.stage(name=Stages.WAIT_FOR_BOT_PREORDER)
    sb.stage(
        name=Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DELETED_HOST
    )  # set downtime for the old host's name
    with sb.nested(Stages.SET_HOSTNAME) as set_hostname:
        set_hostname.stage(name=Stages.ALLOCATE_HOSTNAME)
        set_hostname.stage(
            name=Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT
        )  # set downtime for the new host's name
        set_hostname.stage(name=Stages.ASSIGN_HOSTNAME)
    sb.stage(
        name=Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT
    )  # set downtime for the old host's name
    sb.stage(name=Stages.ACQUIRE_PERMISSION, action=CmsTaskAction.PREPARE, comment=cms_comment, ignore_cms=ignore_cms)
    sb.stage(name=Stages.SET_PROBATION, reason=reason)

    if bot_project_id is not None:
        sb.stage(name=Stages.ASSIGN_BOT_PROJECT, bot_project_id=bot_project_id)
    if profile is not None:
        profile, profile_tags, profile_mode = host.deduce_profile_configuration(
            profile or FLEXY_EINE_PROFILE,
            profile_tags or [],
            profile_mode=profile_mode,
        )
        sb.add_stages(
            walle.util.tasks.get_profile_stages(
                EineProfileOperation.PREPARE,
                profile,
                profile_tags,
                profile_mode,
                repair_request_severity=repair_request_severity,
            )
        )
    else:
        sb.add_stages(walle.util.tasks.get_power_on_stages())
        network_update_configuration = host.deduce_profile_configuration(profile_mode=ProfileMode.SWP_UP)
        sb.add_stages(walle._tasks.stages.get_network_update_stages(network_update_configuration, full=True))

    if update_firmware_needed:
        profile, profile_tags, profile_mode = host.deduce_profile_configuration(
            profile or FLEXY_EINE_PROFILE, profile_tags or [], ProfileMode.FIRMWARE_UPDATE
        )
        sb.add_stages(
            walle.util.tasks.get_profile_stages(
                EineProfileOperation.PROFILE,
                profile,
                profile_tags,
                profile_mode,
                repair_request_severity=repair_request_severity,
            )
        )

    new_task_params = _new_task_params(manual=manual)
    task_args = mock_prepare_task_args(
        host=host,
        provisioner=provisioner,
        config=config,
        deploy_config_policy=deploy_config_policy,
        deploy_tags=deploy_tags,
        deploy_network=deploy_network,
        need_certificate=need_certificate,
        restrictions=restrictions,
        manual=manual,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
        check=check,
        with_auto_healing=with_auto_healing,
        extra_vlans=extra_vlans,
        new_task_params=new_task_params,
    )
    deploy_configuration = task_args.deploy_configuration

    sb.stage(name=Stages.SWITCH_VLANS, network=NetworkTarget.PROJECT, extra_vlans=extra_vlans)
    sb.stage(name=Stages.SETUP_DNS, create=True)
    sb.stage(name=Stages.ADD_HOST_TO_CAUTH, terminators={StageTerminals.FAIL: StageTerminals.SKIP})

    deploy_stages = walle.util.tasks.get_deploy_stages(
        deploy_configuration,
        config_forced=(task_args.config is not None),
        extra_vlans=extra_vlans,
        with_autohealing=False,
    )
    sb.add_stages(deploy_stages)

    sb.stage(
        name=Stages.COMPLETE_PREPARING,
        provisioner=task_args.provisioner if custom_deploy_config else None,
        config=config if custom_deploy_config else None,
        deploy_config_policy=deploy_config_policy if custom_deploy_config else None,
        deploy_tags=deploy_tags if custom_deploy_config else None,
        deploy_network=deploy_network if custom_deploy_config else None,
        restrictions=task_args.operation_restrictions,
        extra_vlans=extra_vlans,
    )

    host.task = create_new_task(host, task_args, new_task_params['audit_entry'], sb)

    host.task.task_id -= 1
    host.set_status(Operation.PREPARE.host_status, get_issuer(manual), host.task.audit_log_id, reason=reason)


def mock_schedule_bot_project_sync(host, bot_project_id, reason, forced_task_id=0):
    sb = walle.util.tasks.StageBuilder()
    sb.stage(Stages.ASSIGN_BOT_PROJECT, bot_project_id=bot_project_id)

    host.task = walle.util.tasks.new_task(
        task_type=TaskType.AUTOMATED_ACTION,
        stages=sb.get_stages(),
        host=host,
        monitor_on_completion=False,
        **_new_task_params(manual=False),
    )

    host.task.task_id = forced_task_id
    host.set_status(
        Operation.BOT_PROJECT_SYNC.host_status, get_issuer(manual=False), host.task.audit_log_id, reason=reason
    )


def mock_schedule_host_deactivation(
    host, manual, expected_notify_fsm_calls=None, notify=True, issuer=None, reason=None
):
    new_task_params = _new_task_params(manual)
    if issuer:
        new_task_params["issuer"] = issuer

    if reason:
        new_task_params["reason"] = reason

    host.task = walle.tasks._new_deactivate_task(
        host, task_type=TaskType.MANUAL if manual else TaskType.AUTOMATED_HEALING, **new_task_params
    )
    host.task.task_id -= 1

    host.set_status(Operation.DEACTIVATE.host_status, new_task_params["issuer"], host.task.audit_log_id, reason=reason)

    if notify and expected_notify_fsm_calls is not None:
        expected_notify_fsm_calls.append(call())


def mock_task_initialization(host, stage, initial_status=None):
    host.task.stage_uid = "1"
    host.task.stage_name = stage.name
    mock_commit_stage_changes(host, status=initial_status)
    walle.fsm_stages.common._set_current_stage_status(host, stage, initial_status)


def mock_host_deactivation(host, reason=None):
    host.set_status(HostStatus.DEAD, get_issuer(manual=False), host.task.audit_log_id, reason=reason)
    del host.task


def mock_task_completion(
    host,
    healthy_status_confirmed=True,
    log_operations=False,
    status=HostStatus.READY,
    owner=None,
    status_reason=None,
    downtime=None,
):
    if log_operations:
        for stage in walle.stages.iter_stages(host.task.stages):
            if stage.name == Stages.LOG_COMPLETED_OPERATION:
                operation = Operation(stage.get_param("operation"))
                params = stage.get_param("params", None)
                operations_log.on_completed_operation(host, operation.type, params)

    if owner is None:
        owner = get_issuer(manual=False)

    # don't just use task.owner and task.target_status here, as it is exactly the logic in fsm.complete_task
    # instead, let's specify expected values in the test code.
    host.set_status(
        status,
        owner,
        host.task.audit_log_id,
        confirmed=healthy_status_confirmed,
        reason=status_reason,
        downtime=downtime,
    )
    del host.task


mock_task_cancellation = mock_task_completion


def _new_task_params(manual, issuer=None):
    return dict(
        issuer=get_issuer(manual) if not issuer else issuer,
        audit_entry=LogEntry(id=audit_log._uuid()),
    )


def get_issuer(manual):
    return TestCase.api_issuer if manual else authorization.ISSUER_WALLE


# Stage mocks


def _stage_handler_mock(host):
    raise LogicalError


register_stage(PREV_STAGE, _stage_handler_mock, initial_status="mocked-prev-stage-status")
register_stage(NEXT_STAGE, _stage_handler_mock, initial_status="mocked-next-stage-status")
register_stage(PARENT_NEXT_STAGE, _stage_handler_mock, initial_status="mocked-parent-next-stage-status")


def hbf_project_id():
    hbf_project_id_str = "984e3"
    hbf_project_id_int = 0x984E3

    return hbf_project_id_str, hbf_project_id_int


def add_dns_domain_and_mtn_to_request(request_dict):
    hbf_project_id_str, hbf_project_id_int = hbf_project_id()

    request_dict = dict(request_dict, dns_domain="some-domain.yandex.net", hbf_project_id=hbf_project_id_str)
    return request_dict


def add_dns_domain_and_vlan_scheme_to_project(project_dict, vlan_scheme=constants.VLAN_SCHEME_MTN):
    project_dict = dict(project_dict, dns_domain="some-domain.yandex.net", vlan_scheme=vlan_scheme)

    if vlan_scheme in constants.MTN_VLAN_SCHEMES:
        hbf_project_id_str, hbf_project_id_int = hbf_project_id()
        project_dict.update(
            dict(
                hbf_project_id=hbf_project_id_int,
                vlan_scheme=vlan_scheme,
                native_vlan=constants.MTN_NATIVE_VLAN,
                extra_vlans=constants.MTN_EXTRA_VLANS,
            )
        )

    return project_dict


def mock_schedule_fqdn_deinvalidation(
    host,
    ignore_cms=False,
    disable_admin_requests=True,
    release=False,
    bot_project_id=BOT_PROJECT_ID,
    reason=None,
    drop_cms_task=False,
    clear_old_fqdn_records=False,
):
    builder = walle.util.tasks.StageBuilder()

    builder.stage(Stages.ACQUIRE_PERMISSION, action=CmsTaskAction.DEACTIVATE, ignore_cms=ignore_cms, comment=reason)
    builder.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DELETED_HOST)

    if clear_old_fqdn_records:
        builder.stage(Stages.SETUP_DNS, clear=True)
        builder.stage(Stages.LUI_REMOVE)

    builder.stage(Stages.FQDN_DEINVALIDATION)
    builder.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)

    if release:
        builder.stage(
            Stages.SWITCH_VLANS,
            network=NetworkTarget.PARKING,
            terminators={StageTerminals.SWITCH_MISSING: StageTerminals.SKIP},
        )
        builder.stage(Stages.SETUP_DNS, clear=True)
        builder.stage(Stages.LUI_REMOVE)
        builder.add_stages(
            walle.util.tasks.get_profile_stages(
                EineProfileOperation.RELEASE, FLEXY_EINE_PROFILE, profile_tags=[EineProfileTags.DRIVES_WIPE]
            )
        )

        terminators = {StageTerminals.FAIL: StageTerminals.SKIP} if disable_admin_requests else None
        builder.stage(Stages.POWER_OFF, soft=False, terminators=terminators)

        builder.stage(Stages.COMPLETE_RELEASING)
    else:
        builder.stage(Stages.ADD_HOST_TO_CAUTH, terminators={StageTerminals.FAIL: StageTerminals.SKIP})

    if drop_cms_task:
        builder.stage(Stages.DROP_CMS_TASK, cms_task_id=host.cms_task_id)

    builder.stage(Stages.ASSIGN_BOT_PROJECT, bot_project_id=bot_project_id)

    new_task_params = _new_task_params(manual=True)
    task_args = FqdnDeinvalidationArgs(
        issuer=new_task_params["issuer"],
        task_type=TaskType.MANUAL,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        release=release,
        bot_project_id=bot_project_id,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
    )
    host.task = create_new_task(host, task_args, new_task_params['audit_entry'], builder)
    host.task.task_id -= 1

    host.set_status(Operation.FQDN_DEINVALIDATION.host_status, get_issuer(manual=True), host.task.audit_log_id)
    host.status_reason = reason


def mock_hbf_drill(mocker, overrides=None, *args, **kwargs):
    if overrides:
        overrides = overrides.copy()
    else:
        overrides = {}

    if "id" in overrides:
        overrides["location"], overrides["project_macro"], overrides["start_ts"], overrides["end_ts"] = overrides[
            "id"
        ].split("|")
        overrides["start_ts"], overrides["end_ts"] = int(overrides["start_ts"]), int(overrides["end_ts"])

    obj = mocker.mock(overrides, save=False, add=False)
    obj.set_hash()

    if kwargs.get("save", True):
        obj.save()
    if kwargs.get("add", True):
        mocker.add(obj)

    return obj


def set_project_owners(project, owners):
    set_project_roles(project, {ProjectRole.OWNER: owners})


def fail_check(reasons, check_type):
    check = reasons[check_type]

    check["status"] = CheckStatus.FAILED
    if check_type == CheckType.MEMORY:
        check["metadata"]["results"]["ecc"] = {
            "slot": "DIMM-1",
            "status": HwWatcherCheckStatus.UNKNOWN,
            "reason": "reason-mock",
            "comment": "ecc errors uncorrectable were",
        }
    if check_type == CheckType.GPU:
        check["metadata"]["result"] = {"eine_code": ["GPU_MISSING"], "reason": "reason-mock"}
    if check_type == CheckType.INFINIBAND:
        check["metadata"]["result"] = {"eine_code": ["INFINIBAND_MISMATCH"], "reason": "reason-mock"}


def load_mock_data(path, to_str=True):
    if not resource.resfs_file_exists(path):
        raise RuntimeError("Fail to find {} in resources".format(path))
    res = resource.resfs_read(path)
    if to_str:
        return six.ensure_str(res)
    else:
        return res


CUSTOM_CHECK_TYPE = "custom-check-type"


def find_host_scheduler_stage(stages: tp.Iterable[BaseStage]):
    return next((s for s in stages if s.name == 'HostSchedulerStage'), None)


def string_to_ts(datetime_string: str, datetime_format: str = "%d.%m.%Y %H:%M") -> int:
    return int(datetime.datetime.strptime(datetime_string, datetime_format).timestamp())


def tier_2():
    mocked_env = os.environ.copy()
    mocked_env["BSCONFIG_ITAGS"] = "a_tier_2"
    with mock.patch("os.environ", mocked_env):
        yield


def get_mock_health_data():
    return json.loads(load_mock_data("mock_health_data.json") % {"timestamp": timestamp()})
