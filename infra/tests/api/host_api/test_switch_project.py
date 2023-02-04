"""Tests project switching."""

import pytest
import http.client

import infra.walle.server.tests.lib.util as test_util
from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    mock_schedule_project_switching,
    BOT_PROJECT_ID,
)
from walle import restrictions
from walle.audit_log import LogEntry
from walle.constants import HostType
from walle.errors import InvalidHostStateError
from walle.hosts import HostState, HostStatus
from walle.projects import DEFAULT_CMS_NAME
from walle.util.misc import drop_none

ALLOWED_STATES = [HostState.FREE, HostState.ASSIGNED, HostState.MAINTENANCE]


@pytest.fixture
def monkeypatch_audit_log_erase_disks(monkeypatch):
    test_util.monkeypatch_audit_log(monkeypatch, patch_create=False)


@pytest.fixture
def test_erase_disks(monkeypatch_timestamp, monkeypatch_audit_log_erase_disks, request):
    return TestCase.create(request)


@pytest.fixture
def test(monkeypatch_timestamp, monkeypatch_audit_log, request):
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), "/switch-project", {"project": "project-mock", "release": False})


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY) - {HostStatus.INVALID})
def test_reject_by_status(test, status):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host({"inv": 0, "state": HostState.ASSIGNED, "status": status})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": project.id, "release": False}
    )

    allowed_states = [HostState.FREE, HostState.ASSIGNED, HostState.MAINTENANCE]
    allowed_statuses = HostStatus.ALL_STEADY
    error_message = InvalidHostStateError(host, allowed_states=allowed_states, allowed_statuses=allowed_statuses)

    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == str(error_message)
    test.hosts.assert_equal()


def test_reject_by_maintenance(test, mock_maintenance_host):
    project = test.mock_project({"id": "some-id"})
    host = mock_maintenance_host(test)

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": project.id, "release": False}
    )

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )
    test.hosts.assert_equal()


def test_allow_by_ignore_maintenance(test, mock_maintenance_host):
    project = test.mock_project({"id": "some-id"})
    host = mock_maintenance_host(test)

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        query_string="ignore_maintenance=true",
        data={"project": project.id, "release": False},
    )

    assert result.status_code == http.client.OK

    mock_schedule_project_switching(host, project.id, fix_default_cms=True)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


def test_reject_by_host_vlans(test):
    host = test.mock_host({"inv": 0, "state": HostState.ASSIGNED, "extra_vlans": [111, 222, 333]})
    project = test.mock_project({"id": "some-id", "owned_vlans": [222, 333, 444]})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": project.id, "release": False}
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == (
        "Can't switch the host to '{}' project without releasing it: "
        "The host have extra VLANs that '{}' project doesn't have access to: 111.".format(project.id, project.id)
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "source_url,source_version,target_url,target_version",
    (
        ("https://equal.cms.net", "v1.0", "https://equal.cms.net", "v1.3"),
        ("https://source.cms.net", "v1.3", "https://target.cms.net", "v1.3"),
        ("https://source.cms.net", "v1.0", "https://target.cms.net", "v1.3"),
    ),
)
def test_reject_by_diff_cms(test, source_url, source_version, target_url, target_version):
    source_project = test.mock_project(
        {
            "id": "source-id",
            "cms": source_url,
            "cms_api_version": source_version,
            "cms_settings": [{"cms": source_url, "cms_api_version": source_version}],
        }
    )
    target_project = test.mock_project(
        {
            "id": "target-id",
            "cms": target_url,
            "cms_api_version": target_version,
            "cms_settings": [{"cms": target_url, "cms_api_version": target_version}],
        }
    )
    host = test.mock_host(
        {
            "project": source_project.id,
            "inv": 0,
            "state": HostState.MAINTENANCE,
            "cms_task_id": "mock-cms-task-id",
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": target_project.id, "release": False}
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == (
        "Can't switch project for host on maintenance without releasing it: "
        "CMS on projects do not match. Use force to ignore this."
    )
    test.hosts.assert_equal()


def test_maintenance_host_no_release_with_force(test):
    source_project = test.mock_project(
        {"id": "source-id", "cms": "https://source.cms.net", "cms_settings": [{"cms": "https://source.cms.net"}]}
    )
    target_project = test.mock_project(
        {"id": "target-id", "cms": "https://target.cms.net", "cms_settings": [{"cms": "https://target.cms.net"}]}
    )
    host = test.mock_host(
        {
            "project": source_project.id,
            "inv": 0,
            "state": HostState.MAINTENANCE,
            "cms_task_id": "mock-cms-task-id",
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        data={"project": target_project.id, "release": False, "force": True},
    )

    mock_schedule_project_switching(host, target_project.id)
    assert result.status_code == http.client.OK

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "source_cms,target_cms",
    (
        (DEFAULT_CMS_NAME, "https://cms.example.com"),
        ("https://cms.example.com", DEFAULT_CMS_NAME),
        (DEFAULT_CMS_NAME, DEFAULT_CMS_NAME),
    ),
)
def test_maintenance_host_no_release_with_force_default_cms_used(test, source_cms, target_cms):
    source_project = test.mock_project({"id": "source-id", "cms": source_cms})
    target_project = test.mock_project({"id": "target-id", "cms": target_cms})
    host = test.mock_host(
        {
            "project": source_project.id,
            "inv": 0,
            "state": HostState.MAINTENANCE,
            "cms_task_id": "mock-cms-task-id",
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        data={"project": target_project.id, "release": False, "force": True},
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Can't switch project for host on maintenance without releasing it: "
        "default CMS is used in one of the projects."
    )
    test.hosts.assert_equal()


def test_switching_to_current_project(test):
    host = test.mock_host({"inv": 0})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": host.project, "release": False}
    )

    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == "The host is already in '{}' project.".format(host.project)
    test.hosts.assert_equal()


def test_switching_to_project_with_different_type(test):
    host = test.mock_host({"inv": 0, "type": HostType.SERVER})
    target_project = test.mock_project({"id": "some-id", "type": HostType.MAC})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": target_project.id, "release": False}
    )

    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()


def test_switching_to_missing_project(test):
    host = test.mock_host({"inv": 0})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": "invalid", "release": False}
    )

    assert result.status_code == http.client.NOT_FOUND
    assert result.json["message"] == "The specified project ID doesn't exist."
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", set(ALLOWED_STATES) - {HostState.MAINTENANCE})
@pytest.mark.all_status_owner_combinations()
def test_switching_without_release(test, state, status, owner):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host({"state": state, "status": status, "status_author": owner})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": project.id, "release": False}
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(host, project.id)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("ignore_cms", [True, False])
@pytest.mark.parametrize("disable_admin_requests", [None, True, False])
def test_switching_without_release_with_bot_project_id(test, ignore_cms, disable_admin_requests):
    project = test.mock_project({"id": "some-id", "bot_project_id": BOT_PROJECT_ID})
    host = test.mock_host()

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        data=drop_none(
            {
                "project": project.id,
                "release": False,
                "ignore_cms": ignore_cms,
                "disable_admin_requests": disable_admin_requests,
            }
        ),
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(
        host,
        project.id,
        bot_project_id=BOT_PROJECT_ID,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
    )
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("restrictions", [None, [], [restrictions.AUTOMATION]])
def test_switching_without_release_with_restrictions(test, state, restrictions):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host({"state": state})
    assert host.restrictions is None

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        data=drop_none({"project": project.id, "release": False, "restrictions": restrictions}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(host, project.id, host_restrictions=restrictions)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.parametrize("release", (True, False))
def test_switching_on_probation_is_forbidden(test, release):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host({"state": HostState.PROBATION})
    assert host.restrictions is None

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data=drop_none({"project": project.id, "release": release})
    )
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


def test_switching_without_release_on_default_cms(test):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "cms_task_id": "mock-cms-task-id",
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data=drop_none({"project": project.id, "release": False})
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(host, project.id, fix_default_cms=True)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


def test_switching_free_with_release(test):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"), data={"project": project.id, "release": True}
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(host, project.id)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("bot_project_id", [None, BOT_PROJECT_ID])
@pytest.mark.parametrize("ignore_cms", [True, False])
@pytest.mark.parametrize("disable_admin_requests", [None, True, False])
@pytest.mark.parametrize(
    "state,cms_task_id,drop_cms_task",
    ((HostState.ASSIGNED, None, False), (HostState.MAINTENANCE, "mock-cms-task-id", True)),
)
def test_switching_with_release(
    test, monkeypatch, bot_project_id, ignore_cms, disable_admin_requests, state, cms_task_id, drop_cms_task
):
    project = test.mock_project({"id": "some-id", "bot_project_id": bot_project_id})
    host = test.mock_host({"state": state, "cms_task_id": cms_task_id})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        data=drop_none(
            {
                "project": project.id,
                "release": True,
                "ignore_cms": ignore_cms,
                "disable_admin_requests": disable_admin_requests,
            }
        ),
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(
        host,
        project.id,
        release=True,
        bot_project_id=bot_project_id,
        ignore_cms=ignore_cms,
        disable_admin_requests=disable_admin_requests,
        drop_cms_task=drop_cms_task,
    )
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("erase_disks", [True, False, None])
def test_switching_erase_disks(test, erase_disks):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host({"state": HostState.ASSIGNED, "cms_task_id": "mock-cms-task-id"})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        data=drop_none({"project": project.id, "release": True, "erase_disks": erase_disks}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(
        host, project.id, release=True, erase_disks=erase_disks is not False, drop_cms_task=True
    )
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("erase_disks", [True, False, None])
def test_audit_log_erase_disk(test_erase_disks, erase_disks):
    project = test_erase_disks.mock_project({"id": "some-id"})
    host = test_erase_disks.mock_host({"state": HostState.ASSIGNED, "cms_task_id": "mock-cms-task-id"})

    result = test_erase_disks.api_client.post(
        hosts_api_url(host, action="/switch-project"),
        data=drop_none({"project": project.id, "release": True, "erase_disks": erase_disks}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_project_switching(
        host, project.id, release=True, erase_disks=erase_disks is not False, drop_cms_task=True
    )
    erase_disks_audit_log = get_payload_by_task(host.task)["erase_disks"]

    assert erase_disks_audit_log if erase_disks is not False else not erase_disks_audit_log
    assert result.json == host.to_api_obj()

    test_erase_disks.hosts.assert_equal()


def get_payload_by_task(task):
    return LogEntry.objects.only("payload").get(id=task.audit_log_id).payload
