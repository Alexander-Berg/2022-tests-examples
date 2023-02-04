import gevent.monkey

gevent.monkey.patch_all(subprocess=True)

import json
import random
from functools import partial
from itertools import count
from pprint import pformat

import pytest

pytest.register_assert_rewrite("sepelib.mongo.mock")

import sepelib.mongo.mock
import cachetools
from deepdiff import DeepDiff
from sepelib.core import config as sepelib_config
from sepelib.mongo.mock import ObjectMocker

import infra.walle.server.tests.lib.util as test_util
import walle.expert.automation
import walle.expert.dmc
import walle.hosts
import walle.models
import walle.projects
import walle.maintenance_plot
import walle.scenario.definitions.base
from infra.walle.server.tests.lib.util import mock_startrek_client, mock_ok_client, TestCase
from infra.walle.server.tests.lib.maintenance_plot_util import NoopScenarioSettings
from infra.walle.server.tests.lib.scenario_util import NoopScenarioDefinition
from walle import constants, hbf_drills
from walle.authorization import blackbox
from walle.clients import cms, bot, abc, idm, juggler
from walle.constants import PRODUCTION_ENV_NAME
from walle.errors import UnauthenticatedError, UnauthorizedError
from walle.hosts import HostState, HostStatus
from walle.scenario.constants import ScriptName
from walle.scenario.authorization import list_idm_role_members
from walle.scenario.stage_info import StageRegistry


def has_xdist(config):
    return config.pluginmanager.hasplugin("xdist")


def in_xdist_worker(config):
    return hasattr(config, "slaveinput")


def pytest_configure(config):
    config.addinivalue_line("markers", "slow: test is too slow to execute on every test run")
    config.addinivalue_line("markers", "online: test that uses external api and therefore requires network access")
    config.addinivalue_line("markers", "skip_on_cov: test is too slow to execute when run with coverage")
    config.addinivalue_line("markers", "ssh_tests: test requires real ssh access to the host, ssh key needed as well")

    sepelib_config.load(path=constants.DEFAULT_CONFIG_PATH, config_context={})


def pytest_cmdline_main(config):
    # set common random seed in workers
    if in_xdist_worker(config):
        random.seed(config.option.random_seed)


def pytest_addoption(parser):
    # Configures behaviour of infra.walle.server.tests.lib.util.generate_test_params()
    parser.addoption("--all", action="store_true", help="run all combinations")
    # Turns on some tests that couldn't run on teamcity
    parser.addoption("--ssh", action="store_true", help="run tests for ssh client")
    parser.addoption("--certificator-token", action="store", help="run tests for certificator api client")
    parser.addoption("--bot-token", action="store", help="run tests for bot api client")
    parser.addoption("--yp-token", action="store", help="run tests for yp api client")
    parser.addoption("--yp-address", action="store", help="override address for yp cluster")
    parser.addoption("--no-deepdiff", action="store_true", help="show diffs with DeepDiff")


def pytest_collection_modifyitems(config, items):
    if not config.getoption("--ssh"):
        skip_with_no_ssh = pytest.mark.skip(reason="run ssh tests only on development machine")
        for item in items:
            if "ssh_tests" in item.keywords:
                item.add_marker(skip_with_no_ssh)


@pytest.fixture(scope="session")
def bot_token(pytestconfig):
    return pytestconfig.getoption('--bot-token', skip=True)


@pytest.fixture(scope="session")
def certificator_token(pytestconfig):
    return pytestconfig.getoption('--certificator-token', skip=True)


@pytest.fixture(scope="session")
def yp_token(pytestconfig):
    return pytestconfig.getoption('--yp-token', skip=True)


@pytest.fixture(scope="session")
def yp_address(pytestconfig):
    return pytestconfig.getoption('--yp-address')


def pytest_assertrepr_compare(config, op, left, right):
    """Show objects' diffs in assert-equal fails using deepdiff"""
    if not config.getoption("--no-deepdiff", False):
        config.option.verbose = 2  # disable output truncation, deepdiff output can be large

        from unittest.mock import call

        call_type = type(call)

        def isiterable(obj):
            try:
                iter(obj)
                return not isinstance(obj, str)
            except TypeError:
                return False

        def is_call(obj):
            if isinstance(obj, call_type) or any(isinstance(item, call_type) for item in obj):
                return True

        def call_to_tuple(obj):
            if isinstance(obj, call_type):
                return tuple(obj)
            else:
                return list(map(tuple, obj))

        explanation = None
        try:
            if op == '==':
                if isiterable(left) and isiterable(right):
                    if is_call(left) or is_call(right):
                        explanation = DeepDiff(call_to_tuple(left), call_to_tuple(right))
                    else:
                        explanation = DeepDiff(left, right)

                    explanation = pformat(explanation, indent=2).split('\n')
        except Exception as e:
            explanation = [
                '(deepdiff plugin: representation of details failed.  ',
                'Probably an object has a faulty __repr__.)',
                str(e),
            ]

        return explanation


@pytest.fixture()
def mp(monkeypatch):
    """mp fixture -- just like monkeypatch, but extended with our function/method patching functionality"""
    monkeypatch.function = partial(test_util.monkeypatch_function, monkeypatch)
    monkeypatch.method = partial(test_util.monkeypatch_method, monkeypatch)
    monkeypatch.config = partial(test_util.monkeypatch_config, monkeypatch)
    monkeypatch.request = partial(test_util.monkeypatch_request, monkeypatch)

    return monkeypatch


@pytest.fixture(autouse=True)
def clear_caches():
    walle.expert.automation.GLOBAL_HEALING_AUTOMATION.is_enabled_cached.cache_clear()
    walle.expert.automation.GLOBAL_DNS_AUTOMATION.is_enabled_cached.cache_clear()
    walle.expert.automation.PROJECT_HEALING_AUTOMATION.get_project_ids_with_automation_enabled_cached.cache_clear()
    walle.expert.automation.PROJECT_DNS_AUTOMATION.get_project_ids_with_automation_enabled_cached.cache_clear()


@pytest.fixture
def disable_caches(monkeypatch):
    test_util.monkeypatch_function(monkeypatch, cachetools.TTLCache.__setitem__, module=cachetools.TTLCache)
    test_util.monkeypatch_function(monkeypatch, cachetools.LRUCache.__setitem__, module=cachetools.LRUCache)


@pytest.fixture
def unauthenticated(monkeypatch):
    return test_util.patch_attr(monkeypatch, blackbox, "authenticate", side_effect=UnauthenticatedError("Mocked error"))


@pytest.fixture
def authorized_admin(monkeypatch):
    test_util.monkeypatch_config(monkeypatch, "authorization.admins", [test_util.TestCase.api_user])


@pytest.fixture
def unauthorized_project(monkeypatch):
    test_util.patch_attr(
        monkeypatch, walle.projects.Project, "authorize", side_effect=UnauthorizedError("Mocked error")
    )


@pytest.fixture
def unauthorized_host(monkeypatch):
    test_util.patch_attr(monkeypatch, walle.hosts.Host, "authorize", side_effect=UnauthorizedError("Mocked error"))


@pytest.fixture
def monkeypatch_audit_log(monkeypatch):
    test_util.monkeypatch_audit_log(monkeypatch)


@pytest.fixture
def monkeypatch_host_uuid(monkeypatch):
    test_util.monkeypatch_host_uuid(monkeypatch)


@pytest.fixture
def monkeypatch_timestamp(monkeypatch):
    return walle.models.monkeypatch_timestamp(monkeypatch)


@pytest.fixture
def monkeypatch_production_env(monkeypatch):
    return test_util.monkeypatch_config(monkeypatch, "environment.name", PRODUCTION_ENV_NAME)


@pytest.fixture
def monkeypatch_check_percentage(monkeypatch):
    test_util.monkeypatch_config(monkeypatch, "automation.checks_percentage", {})


@pytest.fixture
def monkeypatch_locks(monkeypatch):
    test_util.monkeypatch_locks(monkeypatch)


@pytest.fixture
def cms_reject(monkeypatch):
    test_util.patch_attr(
        monkeypatch, cms._BaseCmsClient, "add_task", side_effect=cms.CmsTaskRejectedError("Mocked error")
    )


@pytest.fixture
def cms_accept(monkeypatch):
    test_util.patch_attr(monkeypatch, cms._BaseCmsClient, "add_task")


@pytest.fixture(params=[True, False])
def iterate_authentication(request, monkeypatch):
    authenticated = request.param

    if authenticated:
        test_util.patch_attr(monkeypatch, blackbox, "authenticate", return_value=test_util.TestCase.api_issuer)
    else:
        test_util.patch_attr(monkeypatch, blackbox, "authenticate", side_effect=UnauthenticatedError)


@pytest.fixture
def database(request):
    db = sepelib.mongo.mock.Database(lightweight=True)
    request.addfinalizer(db.close)
    return db.connection


@pytest.fixture
def walle_test(request, monkeypatch_timestamp):
    return test_util.TestCase.create(request)


@pytest.fixture
def next_inv():
    return partial(next, count())


@pytest.fixture
def mock_ready_host(next_inv):
    def host_mocker(test, **overrides):
        return test.mock_host(
            dict({"inv": next_inv(), "state": HostState.ASSIGNED, "status": HostStatus.READY}, **overrides)
        )

    return host_mocker


@pytest.fixture(params=HostState.ALL_ASSIGNED)
def mock_assigned_host(request, next_inv):
    def host_mocker(test, **overrides):
        return test.mock_host(
            dict({"inv": next_inv(), "state": request.param, "status": HostStatus.READY}, **overrides)
        )

    return host_mocker


@pytest.fixture
def mock_maintenance_host(next_inv):
    def host_mocker(test, overrides={}, **kwargs):
        return test.mock_host(
            dict(
                {
                    "inv": next_inv(),
                    "state": HostState.MAINTENANCE,
                    "status": HostStatus.default(HostState.MAINTENANCE),
                    "state_author": "other-user@",
                    "status_author": "other-user@",
                    "state_expire": {
                        "ticket": "ticket-mock",
                        "status": HostStatus.default(HostState.ASSIGNED),
                        "issuer": "other-user@",
                    },
                    "cms_task_id": "mock-cms-task-id",
                    "on_downtime": True,
                },
                **overrides
            ),
            **kwargs
        )

    return host_mocker


@pytest.fixture(params=(False, True))
def exceed_max_error_count(request):
    return request.param


@pytest.fixture()
def load_test_json():
    def load(rel_filename):
        data = test_util.load_mock_data(rel_filename)
        return json.loads(data)

    return load


@pytest.fixture
def startrek_client(mp):
    return mock_startrek_client(mp)


@pytest.fixture
def ok_client(mp):
    return mock_ok_client(mp)


@pytest.fixture
def mock_stage_registry(mp):
    return mp.setattr(StageRegistry, "ITEMS", StageRegistry.ITEMS.copy())


@pytest.fixture
def mock_service_tvm_app_ids(mp):
    def mocker(ids=()):
        mp.function(abc.get_service_tvm_app_ids, return_value=ids)

    return mocker


@pytest.fixture
def mock_get_planner_id_by_bot_project_id(mp):
    def mocker(planner_id=11111):
        mp.function(bot.get_planner_id_by_bot_project_id, return_value=planner_id)

    return mocker


@pytest.fixture
def mock_abc_get_service_slug(mp):
    def mocker(service_slug="some_service"):
        mp.function(abc.get_service_slug, return_value=service_slug)

    return mocker


@pytest.fixture
def hbf_drills_mocker(walle_test):
    mocker = ObjectMocker(hbf_drills.HbfDrill, {"project_ips": ["444a@2a02:6b8:c00::/40"], "exclude_ips": []})
    return mocker


@pytest.fixture
def mp_juggler_source(mp):
    mp.config("juggler.source", "wall-e.unittest")


@pytest.fixture
def authorized_scenario_user(mp):
    mp.function(list_idm_role_members, return_value=[TestCase.api_user])


@pytest.fixture
def batch_request_execute_mock(mp):
    return mp.method(idm.BatchRequest.execute, obj=idm.BatchRequest)


@pytest.fixture
def send_event_mock(mp):
    return mp.function(juggler.send_event)


@pytest.fixture
def monkeypatches_for_noop_scenario(mp):
    all_scenarios_types_copy = walle.scenario.definitions.base.ALL_SCENARIOS_DEFINITIONS.copy()
    all_scenarios_types_copy.append(NoopScenarioDefinition)
    mp.setattr(walle.scenario.definitions.base, "ALL_SCENARIOS_DEFINITIONS", all_scenarios_types_copy)
    scenario_types_settings_map_copy = walle.maintenance_plot.constants.SCENARIO_TYPES_SETTINGS_MAP.copy()
    mp.setattr(
        walle.maintenance_plot.constants,
        "SCENARIO_TYPES_SETTINGS_MAP",
        {
            ScriptName.NOOP: NoopScenarioSettings,
        }
        | scenario_types_settings_map_copy,
    )
