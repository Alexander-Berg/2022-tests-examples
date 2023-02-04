"""Tests host adding API."""

from uuid import UUID

import pytest
import http.client

import walle.host_operations
import walle.projects
from infra.walle.server.tests.lib.util import (
    TestCase,
    monkeypatch_request_params_validation,
    patch,
    mock_physical_location_kwargs,
    monkeypatch_inventory_get_host_info_and_check_status,
    mock_host_adding,
    monkeypatch_network_get_current_host_switch_port,
    BOT_PROJECT_ID,
    mock_host_adding_in_maintenance,
    mock_startrek_client,
    monkeypatch_config,
)
from sepelib.core import constants
from walle import constants as walle_constants, restrictions
from walle.clients import bot, deploy, inventory
from walle.clients.cms import CmsTaskAction
from walle.constants import NetworkTarget
from walle.hosts import HostState, HostStatus, HostLocation, HostPlatform
from walle.models import timestamp
from walle.network import HostNetworkLocationInfo
from walle.physical_location_tree import LocationNamesMap
from walle.util.deploy_config import DeployConfigPolicies
from walle.util.misc import drop_none


@pytest.fixture
def test(request, mp, monkeypatch_timestamp, monkeypatch_audit_log):
    monkeypatch_request_params_validation(mp)
    mp.function(walle.host_operations._exists_in_dns, side_effect=lambda fqdn: fqdn != "missing-in-dns.mock")

    mp.function(
        bot.missed_preordered_hosts,
        return_value={
            10: {"inv": 10, "fqdn": "missing-in-preorder-10.mock", "order": 9999},
            11: {"inv": 11, "fqdn": "missing-in-preorder-11.mock", "order": 9999},
        },
    )

    return TestCase.create(request)


@pytest.fixture
def shortnames(test):
    LocationNamesMap(path="country-mock|city-mock|dc-mock", name="mdc").save(force_insert=True)
    LocationNamesMap(path="country-mock|city-mock|dc-mock|queue-mock", name="m-queue").save(force_insert=True)


def _request_parameters(host, exclude=None, **extra):
    """Converts a Host document into a JSON suitable for add_host API."""

    fields = ["inv", "name", "project"]
    if host.state in HostState.ALL_ASSIGNED:
        fields.extend(
            ("provisioner", "config", "deploy_config_policy", "deploy_tags", "deploy_network", "restrictions")
        )
    else:
        fields.append("state")

    if exclude is not None:
        fields = list(set(fields) - set(exclude))

    return drop_none(dict(host.to_api_obj(fields), **extra))


def test_unauthenticated(test, unauthenticated):
    host = test.mock_host({"inv": 0}, add=False, save=False)
    result = test.api_client.post("/v1/hosts", data=_request_parameters(host))
    assert result.status_code == http.client.UNAUTHORIZED
    test.hosts.assert_equal()


def test_unauthorized(test, mp, unauthorized_host):
    monkeypatch_inventory_get_host_info_and_check_status(mp)

    host = test.mock_host({"inv": 0}, add=False, save=False)
    result = test.api_client.post("/v1/hosts", data=_request_parameters(host))
    assert result.status_code == http.client.FORBIDDEN
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "shortnames")
@pytest.mark.parametrize("lui_exception", [deploy.DeployInternalError, deploy._DeployApiError])
def test_add_host_failed_set_config(mp, test, lui_exception):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)

    project = test.mock_project({"id": "some-id"})
    request_params = {
        "inv": 0,
        "name": "mocked-0.mock",
        "project": project.id,
        "provisioner": walle_constants.PROVISIONER_LUI,
        "config": "test-config",
        "restrictions": [],
        "instant": True,
    }

    lui_client = mp.function(deploy.get_lui_client)
    lui_client(provider=None).setup_host.side_effect = lui_exception("foo", "bar")

    result = test.api_client.post("/v1/hosts", data=request_params)
    assert result.status_code == http.client.INTERNAL_SERVER_ERROR
    assert "message" in result.json
    assert "Internal error occurred".lower() in result.json["message"].lower()
    lui_client(provider=None).setup_host.assert_called_once_with("mocked-0.mock", mac=None, config_name="test-config")

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_add_host_no_shortname_for_location(mp, test):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    monkeypatch_config(mp, "shortnames.cities_with_disabled_name_autogeneration", ["city-mock"])

    project = test.mock_project({"id": "some-id"})
    request_params = {
        "inv": 0,
        "name": "mocked-0.mock",
        "project": project.id,
        "provisioner": walle_constants.PROVISIONER_LUI,
        "config": "test-config",
        "restrictions": [],
        "instant": True,
    }

    result = test.api_client.post("/v1/hosts", data=request_params)
    assert result.status_code == http.client.CONFLICT
    assert "does not have a short name" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("provisioner", walle_constants.PROVISIONERS + [None])
@pytest.mark.parametrize("deploy_tags", [["host-tag"], [], None])
@pytest.mark.parametrize("deploy_config", ["test-host-config", None])
@pytest.mark.parametrize("deploy_config_policy", [None] + DeployConfigPolicies.get_all_names())
@pytest.mark.parametrize("deploy_network", NetworkTarget.DEPLOYABLE + [None])
def test_add_host(mp, test, provisioner, deploy_config, deploy_config_policy, deploy_tags, deploy_network):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    lui_client = mp.function(deploy.get_lui_client)

    project = test.mock_project(
        {
            "id": "some-id",
            "default_host_restrictions": [restrictions.AUTOMATED_REBOOT],
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-project-config",
            "deploy_tags": ["project-tag"],
        }
    )

    host_params = {
        "inv": 0,
        "ipmi_mac": test.ipmi_mac,
    }

    mp.function(bot.get_host_info, return_value=None)

    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "project": project.id,
            "provisioner": provisioner,
            "config": deploy_config,
            "deploy_config_policy": deploy_config_policy,
            "deploy_tags": deploy_tags,
            "deploy_network": deploy_network,
            **host_params,
        },
        save=False,
    )
    mock_host_adding(host, location=host_params.get("location", None))

    expected_deploy_config = deploy_config or project.deploy_config

    extra = {}
    result = test.api_client.post("/v1/hosts", data=dict(_request_parameters(host, **extra), instant=True))
    host.deploy_tags = deploy_tags or None  # we replace empty list with None down there.

    if not provisioner and not deploy_config and not deploy_network:
        # no deploy params at all, inherit from project
        host.provisioner = None
        host.config = None
        host.deploy_config_policy = None
    elif not provisioner and not deploy_config and deploy_network:
        # deploy network is given, fetch other params from the project and store them on the host
        host.provisioner = project.provisioner
        host.config = project.deploy_config
        host.deploy_config_policy = project.deploy_config_policy
        host.deploy_tags = project.deploy_tags
    elif not provisioner:
        # deploy config is given, get other params from the project and store them on the host
        host.provisioner = walle_constants.PROVISIONER_LUI

    if (not deploy_config and deploy_tags is not None) or (provisioner and not deploy_config):
        assert result.status_code == http.client.BAD_REQUEST
        return
    else:
        assert result.status_code == http.client.CREATED
        assert result.json == host.to_api_obj()

    if provisioner in [walle_constants.PROVISIONER_LUI, None]:
        # None defaults to PROVISIONER_LUI
        lui_client(provider=None).setup_host.assert_called_once_with(
            "mocked-0.mock", mac=None, config_name=expected_deploy_config
        )
    else:
        assert not lui_client(provider=None).setup_host.called

    host.restrictions = project.default_host_restrictions
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("provisioner", walle_constants.PROVISIONERS + [None])
@pytest.mark.parametrize("deploy_tags", [["host-tag"], [], None])
@pytest.mark.parametrize("deploy_config", ["test-host-config", None])
@pytest.mark.parametrize("ignore_cms", [True, False, None])
@pytest.mark.parametrize("instant", [False, None])
@pytest.mark.parametrize("check", [True, False, None])
def test_add_host_with_task(mp, test, ignore_cms, instant, check, provisioner, deploy_config, deploy_tags):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    lui_client = mp.function(deploy.get_lui_client)

    project = test.mock_project(
        {
            "id": "some-id",
            "default_host_restrictions": [restrictions.AUTOMATED_REBOOT],
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-project-config",
            "deploy_tags": ["project-tag"],
        }
    )

    host = test.mock_host(
        {
            "inv": 0,
            "ipmi_mac": test.ipmi_mac,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "project": project.id,
            "config": deploy_config,
            "deploy_tags": deploy_tags,
            "provisioner": provisioner,
        },
        save=False,
    )

    expected_deploy_config = deploy_config or project.deploy_config

    result = test.api_client.post(
        "/v1/hosts",
        data=_request_parameters(
            host,
            ignore_cms=ignore_cms,
            instant=instant,
            check=check,
        ),
    )

    host.deploy_tags = deploy_tags or None  # we replace empty list with None down there.
    mock_host_adding(
        host,
        task=True,
        ignore_cms=ignore_cms,
        check=check is not False,
        deploy_config=expected_deploy_config if provisioner in (walle_constants.PROVISIONER_LUI, None) else None,
    )

    if (not deploy_config and deploy_tags is not None) or (provisioner and not deploy_config):
        assert result.status_code == http.client.BAD_REQUEST
        return
    else:
        assert result.status_code == http.client.CREATED
        assert result.json == host.to_api_obj()

    if not provisioner and not deploy_config:
        host.provisioner = None
        host.config = None
    elif not provisioner:
        host.provisioner = walle_constants.PROVISIONER_LUI

    assert not lui_client(provider=None).setup_host.called

    host.restrictions = project.default_host_restrictions
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("instant", [True, False, None])
def test_add_missing_in_dns(test, instant, mp):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)

    project = test.mock_project(
        {
            "id": "some-id",
            "provisioner": walle_constants.PROVISIONER_EINE,
            "deploy_config": "test-config",
        }
    )

    host = test.mock_host(
        {
            "inv": 0,
            "name": "missing-in-dns.mock",
            "state": HostState.ASSIGNED,
            "status": HostStatus.DEAD,  # If host is missing in DNS it should be added in dead status
            "project": project.id,
        },
        save=False,
    )
    del host.config
    del host.provisioner

    result = test.api_client.post(
        "/v1/hosts", data=drop_none({"name": host.name, "project": host.project, "instant": instant})
    )
    mock_host_adding(host, task=not instant)
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames")
@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("state", set(HostState.ALL) - {HostState.PROBATION, HostState.MAINTENANCE})
@pytest.mark.parametrize("inv_or_name", [{"inv": 11}, {"name": "missing-in-preorder-10.mock"}])
def test_add_missing_preorder(test, instant, state, mp, inv_or_name):
    # test routine checks host is collected from preorders.

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)

    project = test.mock_project(
        {
            "id": "some-id",
            "provisioner": walle_constants.PROVISIONER_EINE,
            "deploy_config": "test-config",
        }
    )

    result = test.api_client.post(
        "/v1/hosts", data=dict(drop_none({"project": project.id, "instant": instant, "state": state}), **inv_or_name)
    )

    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("absent_in_dns", [True, False])
@pytest.mark.parametrize("instant", [True, False, None])
def test_add_free_host(mp, instant, absent_in_dns, test):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    project = test.mock_project({"id": "some-id", "default_host_restrictions": [restrictions.AUTOMATED_REBOOT]})

    host = test.mock_host(
        drop_none(
            {
                "inv": 0,
                "name": "missing-in-dns.mock" if absent_in_dns else None,
                "state": HostState.FREE,
                "project": project.id,
            }
        ),
        save=False,
    )
    mock_host_adding(host)

    result = test.api_client.post(
        "/v1/hosts",
        data=drop_none({"name": host.name, "project": host.project, "state": HostState.FREE, "instant": instant}),
    )
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames")
@pytest.mark.parametrize("parameter", ["ignore_cms", "check", "disable_admin_requests", "with_auto_healing", "dns"])
@pytest.mark.parametrize(
    ["instant", "state"],
    [[True, HostState.FREE], [True, HostState.ASSIGNED], [True, None], [False, HostState.FREE], [None, HostState.FREE]],
)
def test_add_instant_forbidden_params(mp, test, instant, state, parameter):
    # should reject with message like '$parameter is forbidden'
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    project = test.mock_project({"id": "some-id", "default_host_restrictions": [restrictions.AUTOMATED_REBOOT]})

    host = test.mock_host(
        {
            "inv": 0,
            "state": state,
            "project": project.id,
        },
        add=False,
        save=False,
    )
    mock_host_adding(host)

    result = test.api_client.post("/v1/hosts", data=_request_parameters(host, instant=instant, **{parameter: True}))
    assert result.status_code == http.client.BAD_REQUEST
    message = "instant (or in '{}' state) host adding".format(HostState.FREE)
    assert result.json["message"] == (
        "Request validation error: Parameter is not suitable for {}: {}".format(message, parameter)
    )

    test.hosts.assert_equal()  # test that host wasn't saved


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("instant", [True, False])
def test_add_by_name(test, instant, mp):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    mp.function(deploy.get_lui_client)

    project = test.mock_project(
        {
            "id": "some-id",
            "default_host_restrictions": [restrictions.REBOOT],
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-config",
        }
    )

    host = test.mock_host(
        {
            "inv": 666,
            "state": HostState.ASSIGNED,
            "project": project.id,
            "restrictions": project.default_host_restrictions,
        },
        save=False,
    )
    del host.config
    del host.provisioner

    result = test.api_client.post("/v1/hosts", data={"name": host.name, "project": host.project, "instant": instant})
    mock_host_adding(host, task=not instant, deploy_config="test-config")
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("instant", [True, False])
def test_add_by_name_mixed_case(test, instant, mp):
    host_inv = 666
    host_name = "HostName-{}.mock".format(host_inv)

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp, hostname=host_name.lower())
    mp.function(deploy.get_lui_client)

    project = test.mock_project(
        {
            "id": "some-id",
            "default_host_restrictions": [restrictions.REBOOT],
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-config",
        }
    )

    host = test.mock_host(
        {
            "inv": host_inv,
            "name": host_name.lower(),
            "state": HostState.ASSIGNED,
            "project": project.id,
            "restrictions": project.default_host_restrictions,
        },
        save=False,
    )
    del host.config
    del host.provisioner

    result = test.api_client.post("/v1/hosts", data={"name": host_name, "project": host.project, "instant": instant})
    mock_host_adding(host, task=not instant, deploy_config="test-config")
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("instant", [True, False])
def test_add_with_empty_restrictions(test, instant, mp):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    mp.function(deploy.get_lui_client)

    host = test.mock_host(
        {
            "inv": 1,
            "state": HostState.ASSIGNED,
            "config": "test-config",
            "restrictions": [],
        },
        save=False,
    )

    result = test.api_client.post("/v1/hosts", data=_request_parameters(host, instant=instant))
    mock_host_adding(host, task=not instant, deploy_config="test-config")
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()
    host.restrictions = None

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("instant", [True, False])
def test_add_with_restrictions(test, instant, mp):
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    mp.function(deploy.get_lui_client)

    host = test.mock_host(
        {
            "inv": 1,
            "state": HostState.ASSIGNED,
            "config": "test-config",
            "restrictions": [restrictions.AUTOMATION, restrictions.AUTOMATION],
        },
        save=False,
    )
    host.restrictions = [restrictions.AUTOMATION]

    result = test.api_client.post("/v1/hosts", data=_request_parameters(host, instant=instant))
    mock_host_adding(host, task=not instant, deploy_config="test-config")
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("status", [HostStatus.READY, HostStatus.DEAD])
@pytest.mark.parametrize("instant", [True, False])
def test_add_with_status(test, mp, instant, status):
    test.mock_project(
        {
            "id": "test-project",
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-config-222",
        }
    )

    host = test.mock_host(
        {
            "project": "test-project",
            "inv": 1,
            "state": HostState.ASSIGNED,
            "status": status,
        },
        save=False,
    )
    del host.provisioner
    del host.config

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    mp.function(deploy.get_lui_client)

    result = test.api_client.post(
        "/v1/hosts", data={"name": host.name, "project": host.project, "status": status, "instant": instant}
    )

    mock_host_adding(host, task=not instant, deploy_config="test-config-222")
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("set_timeout_time", [True, False])
@pytest.mark.parametrize("timeout_status", HostStatus.ALL_ASSIGNED)
def test_add_with_maintenance_and_timeout(test, mp, startrek_client, set_timeout_time, timeout_status):
    ticket_key = "MOCK-1234"
    project = test.mock_project({"id": "test-project"})

    host = test.mock_host(
        {
            "project": project.id,
            "inv": 1,
            "state": HostState.MAINTENANCE,
            "status": HostStatus.MANUAL,
        },
        save=False,
    )

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)

    maintenance_properties = {
        "timeout_time": timestamp() + constants.DAY_SECONDS if set_timeout_time else None,
        "timeout_status": timeout_status,
        "ticket_key": ticket_key,
    }
    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": host.name,
            "project": host.project,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": drop_none(maintenance_properties),
        },
    )
    mock_host_adding_in_maintenance(host, maintenance_properties=maintenance_properties)
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()
    assert host.task.keep_downtime

    if set_timeout_time:
        startrek_client.get_issue.assert_not_called()
    else:
        startrek_client.get_issue.assert_called_once_with(ticket_key)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "startrek_client")
@pytest.mark.parametrize("timeout_status", HostStatus.ALL_ASSIGNED)
@pytest.mark.parametrize("cms_task_action", [CmsTaskAction.REBOOT, CmsTaskAction.PREPARE, CmsTaskAction.REDEPLOY])
def test_add_on_maintenance_with_cms_task(test, mp, timeout_status, cms_task_action):
    ticket_key = "MOCK-1234"
    project = test.mock_project({"id": "test-project"})

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)

    maintenance_properties = {
        "timeout_status": timeout_status,
        "ticket_key": ticket_key,
        "cms_task_action": cms_task_action,
    }
    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": "mock.host",
            "project": project.id,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": drop_none(maintenance_properties),
        },
    )

    assert result.status_code == http.client.BAD_REQUEST


@pytest.mark.parametrize("timeout_status", HostStatus.ALL_ASSIGNED)
def test_add_with_maintenance_no_ticket_no_timeout(test, timeout_status):
    project = test.mock_project({"id": "test-project"})

    maintenance_properties = {"timeout_status": timeout_status}
    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": "mock.host",
            "project": project.id,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": maintenance_properties,
        },
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == "Request validation error: You must specify ticket key for exiting maintenance."

    test.hosts.assert_equal()


@pytest.mark.parametrize("timeout_status", HostStatus.ALL_ASSIGNED)
def test_add_with_maintenance_no_ticket_with_timeout(test, timeout_status):
    project = test.mock_project({"id": "test-project"})

    maintenance_properties = {"timeout_status": timeout_status, "timeout_time": timestamp() + constants.DAY_SECONDS}
    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": "mock.host",
            "project": project.id,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": maintenance_properties,
        },
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == "Request validation error: You must specify ticket key for exiting maintenance."

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("timeout_status", HostStatus.ALL_ASSIGNED)
def test_add_with_maintenance_closed_ticket_with_timeout(test, mp, timeout_status):
    project = test.mock_project({"id": "test-project"})
    startrek_client = mock_startrek_client(mp, "closed")
    ticket_key = "MOCK-1234"

    host = test.mock_host(
        {
            "project": project.id,
            "inv": 0,
            "state": HostState.MAINTENANCE,
            "status": HostStatus.MANUAL,
        },
        save=False,
    )
    del host.provisioner
    del host.config

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)

    maintenance_properties = {
        "timeout_time": timestamp() + constants.DAY_SECONDS,
        "timeout_status": timeout_status,
        "ticket_key": ticket_key,
    }
    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": host.name,
            "project": host.project,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": drop_none(maintenance_properties),
        },
    )
    mock_host_adding_in_maintenance(host, maintenance_properties=maintenance_properties)
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()
    assert host.task.keep_downtime

    startrek_client.get_issue.assert_not_called()

    test.hosts.assert_equal()


@pytest.mark.parametrize("timeout_status", HostStatus.ALL_ASSIGNED)
def test_add_with_maintenance_closed_ticket(test, mp, timeout_status):
    project = test.mock_project({"id": "test-project"})
    startrek_client = mock_startrek_client(mp, "closed")
    ticket_key = "MOCK-1234"

    maintenance_properties = {
        "timeout_status": timeout_status,
        "ticket_key": ticket_key,
    }

    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": "mock.host",
            "project": project.id,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": maintenance_properties,
        },
    )
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == "Cannot use the ticket for maintenance: the ticket is closed."
    startrek_client.get_issue.assert_called_once_with(ticket_key)

    test.hosts.assert_equal()


@pytest.mark.parametrize("timeout_status", HostStatus.ALL_ASSIGNED)
@pytest.mark.parametrize(
    "timeout_time_delta", [0, -constants.DAY_SECONDS, constants.MINUTE_SECONDS, 5 * constants.WEEK_SECONDS]
)
def test_add_with_maintenance_invalid_timeout(test, timeout_status, timeout_time_delta):
    project = test.mock_project({"id": "test-project"})

    maintenance_properties = {
        "timeout_time": timestamp() + timeout_time_delta,
        "timeout_status": timeout_status,
    }

    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": "mock.host",
            "project": project.id,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": maintenance_properties,
        },
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: Status timeout "
        "must fall into a range from 30 minutes to 28 days from current time."
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize("ticket_key", ("1234", "MOCK-MOCK-1234", "stt/MOCK-1234"))
def test_add_with_maintenance_invalid_ticket(test, ticket_key, startrek_client):
    project = test.mock_project({"id": "test-project"})
    ticket_key = "1234"

    maintenance_properties = {
        "timeout_status": HostStatus.READY,
        "ticket_key": ticket_key,
    }

    result = test.api_client.post(
        "/v1/hosts",
        data={
            "name": "mock.host",
            "project": project.id,
            "status": HostStatus.MANUAL,
            "state": HostState.MAINTENANCE,
            "maintenance_properties": maintenance_properties,
        },
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: Invalid ticket format. "
        "You need to specify the ticket key or URL to ticket."
    )
    startrek_client.get_issue.assert_not_called()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
def test_add_with_dns_check(test, mp):
    test.mock_project(
        {
            "id": "test-project",
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-config-222",
        }
    )

    host = test.mock_host(
        {
            "project": "test-project",
            "inv": 0,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
        },
        save=False,
    )

    del host.provisioner
    del host.config

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    mp.function(deploy.get_lui_client)

    result = test.api_client.post(
        "/v1/hosts", data={"name": host.name, "project": host.project, "status": HostStatus.READY, "dns": True}
    )
    mock_host_adding(host, task=True, deploy_config="test-config-222", dns=True)

    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("instant", [True, False])
def test_add_invalid(test, instant, mp):
    valid = {
        "inv": 0,
        "name": "mocked-0.mock",
        "config": "test-config",
        "project": "some-id",
        "provisioner": walle_constants.PROVISIONER_LUI,
    }

    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    mp.function(deploy.get_lui_client)

    # Project doesn't exists
    result = test.api_client.post("/v1/hosts", data=dict(valid, instant=instant))
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()

    test.mock_project({"id": valid["project"]})

    # Negative inventory number
    result = test.api_client.post("/v1/hosts", data=dict(valid, inv=-1, instant=instant))
    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()

    host = test.mock_host(valid, save=False)
    host.state = HostState.ASSIGNED

    # Everything is correct
    result = test.api_client.post("/v1/hosts", data=dict(valid, instant=instant))
    mock_host_adding(host, task=not instant, deploy_config="test-config")
    assert result.status_code == http.client.CREATED
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames")
@pytest.mark.parametrize("instant", [True, False])
def test_add_duplicated(test, instant, mp):
    monkeypatch_network_get_current_host_switch_port(mp)
    lui_client = mp.function(deploy.get_lui_client)

    test.mock_project({"id": "project1"})
    test.mock_project({"id": "project2"})

    deploy_config = "test-config"
    with patch(
        "walle.clients.inventory.get_host_info_and_check_status",
        return_value=inventory.BotHostInfo(
            inv=0,
            name="mocked-0.mock",
            ipmi_mac=test.ipmi_mac,
            macs=test.macs,
            location=bot.HardwareLocation(**mock_physical_location_kwargs(bot=True)),
            bot_project_id=BOT_PROJECT_ID,
            platform=HostPlatform(system="system_model", board="board_model"),
        ),
    ):
        uuid_mock = mp.function(
            walle.hosts.uuid4, module=walle.hosts, return_value=UUID("00000000000000000000000000000001")
        )
        host = test.mock_host(
            {
                "uuid": "00000000000000000000000000000001",
                "inv": 0,
                "name": "mocked-0.mock",
                "state": HostState.ASSIGNED,
                "config": deploy_config,
                "project": "project1",
            },
            save=False,
        )

        result = test.api_client.post("/v1/hosts", data=_request_parameters(host, instant=instant))
        mock_host_adding(host, task=not instant, deploy_config=deploy_config)
        assert result.status_code == http.client.CREATED
        test.hosts.assert_equal()
        lui_client(provider=None).setup_host.reset_mock()

        host = test.mock_host(
            {
                "inv": 0,
                "name": "mocked-0.duplicate",
                "state": HostState.ASSIGNED,
                "config": deploy_config,
                "project": "project2",
            },
            add=False,
            save=False,
        )

        result = test.api_client.post("/v1/hosts", data=_request_parameters(host, instant=instant))
        assert result.status_code == http.client.BAD_REQUEST
        assert "#0 points to mocked-0.mock, not mocked-0.duplicate" in result.json["message"]
        test.hosts.assert_equal()
        assert not lui_client(provider=None).setup_host.called

    with patch(
        "walle.clients.inventory.get_host_info_and_check_status",
        return_value=inventory.BotHostInfo(
            inv=1,
            name="mocked-0.mock",
            ipmi_mac=test.ipmi_mac,
            macs=test.macs,
            location=bot.HardwareLocation(**mock_physical_location_kwargs(bot=True)),
            bot_project_id=BOT_PROJECT_ID,
            platform=HostPlatform(system="system_model", board="board_model"),
        ),
    ):
        uuid_mock.return_value = UUID("00000000000000000000000000000002")
        lui_client(provider=None).setup_host.reset_mock()
        host = test.mock_host(
            {
                "uuid": "00000000000000000000000000000002",
                "inv": 1,
                "name": "mocked-0.mock",
                "state": HostState.ASSIGNED,
                "config": deploy_config,
                "project": "project2",
            },
            add=False,
            save=False,
        )
        result = test.api_client.post("/v1/hosts", data=_request_parameters(host, instant=instant))
        assert result.status_code == http.client.CONFLICT
        assert "project1" in result.json["message"]
        assert "project2" not in result.json["message"]
        test.hosts.assert_equal()
        assert not lui_client(provider=None).setup_host.called


@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_audit_log", "shortnames", "monkeypatch_host_uuid")
@pytest.mark.parametrize("instant", [True, False])
def test_network_location(test, instant, mp):
    test.mock_project({"id": "project1"})
    mp.function(deploy.get_lui_client)

    deploy_config = "test-config"
    host = test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "config": deploy_config,
            "project": "project1",
        },
        save=False,
    )
    host_network = test.mock_host_network({}, host=host, save=False)

    monkeypatch_inventory_get_host_info_and_check_status(mp)

    switch_info = HostNetworkLocationInfo(
        switch="test-switch", port="test-port", source=walle_constants.NETWORK_SOURCE_RACKTABLES, timestamp=123
    )

    with patch("walle.network.get_current_host_switch_port", return_value=switch_info):
        result = test.api_client.post("/v1/hosts", data=_request_parameters(host, instant=instant))

    mock_host_adding(host, task=not instant, deploy_config=deploy_config)
    host.location = HostLocation(
        switch="test-switch",
        port="test-port",
        network_source=walle_constants.NETWORK_SOURCE_RACKTABLES,
        physical_timestamp=timestamp(),
        **mock_physical_location_kwargs()
    )
    host_network.network_switch = "test-switch"
    host_network.network_port = "test-port"
    host_network.network_source = walle_constants.NETWORK_SOURCE_RACKTABLES
    host_network.network_timestamp = 123

    assert result.status_code == http.client.CREATED
    test.hosts.assert_equal()
    test.host_network.assert_equal()


@pytest.mark.usefixtures("shortnames")
def test_host_with_mismatched_bot_project_id(test, mp):
    test.mock_project({"id": "test-project", "validate_bot_project_id": True})

    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=666666)
    monkeypatch_network_get_current_host_switch_port(mp)

    result = test.api_client.post("/v1/hosts", data={"name": "mocked-1.mock", "project": "test-project"})
    assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "shortnames", "monkeypatch_host_uuid")
def test_host_with_mismatched_bot_project_id_is_added_into_old_project(test, mp):
    test.mock_project({"id": "test-project", "validate_bot_project_id": None})

    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=666666)
    monkeypatch_network_get_current_host_switch_port(mp)

    host = test.mock_host({"inv": 1, "name": "mocked-1.mock", "project": "test-project"}, save=False)
    mock_host_adding(host)

    result = test.api_client.post("/v1/hosts", data=_request_parameters(host))
    assert result.status_code == http.client.CREATED

    test.hosts.assert_equal()


@pytest.mark.usefixtures("shortnames")
@pytest.mark.parametrize("validate_bot_project_id", [True, False, None])
def test_host_without_bot_project_id(test, mp, validate_bot_project_id):
    # Host without bot project id does not have owners.
    # It is an incorrect situation, we need to handle every case we meet.
    test.mock_project({"id": "test-project", "validate_bot_project_id": validate_bot_project_id})

    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=None)
    monkeypatch_network_get_current_host_switch_port(mp)

    result = test.api_client.post("/v1/hosts", data={"name": "mocked-1.mock", "project": "test-project"})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()
