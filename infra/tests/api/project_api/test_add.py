"""Tests project adding API."""

import pytest
import http.client

import walle.projects
from infra.walle.server.tests.lib.util import (
    TestCase,
    drop_none,
    hbf_project_id,
    add_dns_domain_and_mtn_to_request,
    add_dns_domain_and_vlan_scheme_to_project,
    BOT_PROJECT_ID,
)
from sepelib.mongo.mock import ObjectMocker
from tests.api.common import project_settings
from walle import authorization
from walle.clients import racktables, idm, cauth
from walle.constants import TESTING_ENV_NAME, ROBOT_WALLE_OWNER, HostType
from walle.idm import project_push
from walle.projects import Project, Notifications, NotificationRecipients, CauthSettingsDocument

CMS_TVM_APP_ID = 100500
EXISTING_AUTOMATION_PLOT = "rtc-automation-plot"
NON_EXISTING_AUTOMATION_PLOT = "market-automation-plot"


@pytest.fixture
def test(request, monkeypatch_bot_projects, monkeypatch_abc_get_service_by_id, monkeypatch_production_env):
    _test = TestCase.create(request)
    _test.automation_plot.mock({"id": EXISTING_AUTOMATION_PLOT, "name": "Automation Plot"})
    return _test


@pytest.yield_fixture()
def project_idm_push_called(enable_idm_push, project_idm_add_node_mock, project_idm_request_ownership_mock):
    yield
    assert project_idm_add_node_mock.called
    assert project_idm_request_ownership_mock.called


@pytest.fixture
def projects(test):
    # Use our own mocker with custom defaults suitable for the tests

    mocker = ObjectMocker(
        Project,
        {
            "owners": [test.api_user],
            "cms": walle.projects.DEFAULT_CMS_NAME,
            "cms_max_busy_hosts": 5,
            "cms_settings": [
                {
                    "cms": walle.projects.DEFAULT_CMS_NAME,
                    "cms_max_busy_hosts": 5,
                    "temporary_unreachable_enabled": False,
                }
            ],
            "healing_automation": {"enabled": False},
            "dns_automation": {"enabled": False},
            "automation_limits": walle.projects.get_default_project_automation_limits(),
            "host_limits": walle.projects.get_default_host_limits(),
            "notifications": _get_notifications_by_owners([test.api_user]),
            "bot_project_id": BOT_PROJECT_ID,
            "validate_bot_project_id": True,
        },
    )

    mocker.objects.extend(test.projects.objects)

    return mocker


def remove_owners(project):
    """owners are not saved on project creation (because they must be processed by IDM first),
    but we must pass them into project creation handle
    """
    wo_owners = dict(project)
    wo_owners["owners"] = []
    return wo_owners


def test_unauthenticated(test, unauthenticated):
    result = test.api_client.post("/v1/projects", data=project_settings())
    assert result.status_code == http.client.UNAUTHORIZED
    test.projects.assert_equal()


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects",
    "monkeypatch_check_deploy_conf",
    "monkeypatch_staff_get_user_groups",
    "project_idm_push_called",
)
@pytest.mark.parametrize("as_admin", [True, False])
def test_add(mp, test, projects, as_admin):
    if as_admin:
        mp.config("authorization.admins", [test.api_user])

    project_json = project_settings()
    projects.mock(remove_owners(project_json), save=False)
    result = test.api_client.post("/v1/projects", data=project_json)
    assert result.status_code == http.client.CREATED

    projects.assert_equal()


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects",
    "monkeypatch_check_deploy_conf",
    "monkeypatch_staff_get_user_groups",
    "project_idm_push_called",
)
def test_add_with_mixed_case_letters(mp, test, projects, monkeypatch_abc_get_service_by_id):
    resp = {
        "slug": "sOmE_sErViCe",
        "id": pytest.MOCKED_PLANNER_ID,
    }
    monkeypatch_abc_get_service_by_id.return_value = resp
    project_json = project_settings()
    projects.mock(remove_owners(project_json), save=False)
    result = test.api_client.post("/v1/projects", data=project_json)
    assert result.status_code == http.client.CREATED

    projects.assert_equal()


def test_add_with_invalid_cms(test, projects):
    result = test.api_client.post("/v1/projects", data=project_settings(cms={"url": "http://some-invalid-cms-url"}))
    assert result.status_code == http.client.BAD_REQUEST
    projects.assert_equal()


def test_add_with_custom_cms_and_max_busy_hosts(test, projects):
    result = test.api_client.post(
        "/v1/projects", data=project_settings(cms={"url": "http://foo.bar/zar", "max_busy_hosts": 10})
    )
    assert result.status_code == http.client.BAD_REQUEST
    projects.assert_equal()


@pytest.mark.parametrize("dns_domain", ["oblabla", "yp@yandex-team.net", "@1111andherestartsnormal.domain"])
def test_add_with_invalid_dns_domain(test, projects, dns_domain):
    project_json = project_settings(dns_domain=dns_domain)
    result = test.api_client.post("/v1/projects", data=project_json)
    assert result.status_code == http.client.BAD_REQUEST

    projects.assert_equal()


@pytest.mark.parametrize("max_busy_hosts", [None, 0, "", "-"])
def test_add_default_cms_no_max_busy_hosts(test, projects, max_busy_hosts):
    settings = project_settings(
        cms=drop_none({"url": walle.projects.DEFAULT_CMS_NAME, "max_busy_hosts": max_busy_hosts})
    )
    result = test.api_client.post("/v1/projects", data=settings)
    assert result.status_code == http.client.BAD_REQUEST
    projects.assert_equal()


@pytest.mark.usefixtures(
    "authorized_admin", "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf", "project_idm_push_called"
)
@pytest.mark.parametrize("enable_healing_automation", (True, False))
@pytest.mark.parametrize("enable_dns_automation", (True, False))
def test_add_with_enable_automation(mp, test, projects, enable_healing_automation, enable_dns_automation):
    hbf_project_id_str, hbf_project_id_int = hbf_project_id()

    mp.function(racktables.get_hbf_projects, return_value={hbf_project_id_int: "HBF_PROJECT_MACRO"})

    project_request_json = project_settings(
        enable_healing_automation=enable_healing_automation, enable_dns_automation=enable_dns_automation
    )
    project_request_json = add_dns_domain_and_mtn_to_request(project_request_json)

    result = test.api_client.post("/v1/projects", data=project_request_json)
    assert result.status_code == http.client.CREATED

    project_obj_json = project_settings(
        healing_automation={"enabled": enable_healing_automation}, dns_automation={"enabled": enable_dns_automation}
    )
    # we need to set mtn scheme and dns domain to enable dns automation
    project_obj_json = add_dns_domain_and_vlan_scheme_to_project(project_obj_json)

    projects.mock(remove_owners(project_obj_json), save=False)

    projects.assert_equal()


@pytest.mark.usefixtures("authorized_admin", "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf")
@pytest.mark.parametrize("missing_field", ["dns_domain", "hbf_project_id"])
def test_add_with_dns_fixer_fails_without_vlan_scheme_and_dns_domain(mp, test, projects, missing_field):
    hbf_project_id_str, hbf_project_id_int = hbf_project_id()

    mp.function(racktables.get_hbf_projects, return_value={hbf_project_id_int: "HBF_PROJECT_MACRO"})

    project_request_json = add_dns_domain_and_mtn_to_request(project_settings(enable_dns_automation=True))
    project_request_json.pop(missing_field)

    result = test.api_client.post("/v1/projects", data=project_request_json)
    assert result.status_code == http.client.BAD_REQUEST

    projects.assert_equal()


@pytest.mark.usefixtures("authorized_admin", "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf")
def test_duplicated(test, projects):
    settings = project_settings()
    project = projects.mock(settings)

    result = test.api_client.post("/v1/projects", data=project_settings(name="Some other name"))
    assert result.status_code == http.client.CONFLICT
    projects.assert_equal()

    result = test.api_client.post("/v1/projects", data=project_settings(id="some-other-id", name=project.name))
    assert result.status_code == http.client.CONFLICT
    projects.assert_equal()


def test_invalid(test, projects):
    result = test.api_client.post("/v1/projects", data=project_settings(id="0123456"))
    assert result.status_code == http.client.BAD_REQUEST

    result = test.api_client.post("/v1/projects", data=project_settings(provisioner="invalid-provisioner"))
    assert result.status_code == http.client.BAD_REQUEST

    projects.assert_equal()


@pytest.mark.usefixtures(
    "authorized_admin", "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf", "project_idm_push_called"
)
def test_validate_bot_project_id_field_is_set(test, projects):
    project_data = project_settings()
    result = test.api_client.post("/v1/projects", data=project_data)
    assert result.status_code == http.client.CREATED

    project_data["validate_bot_project_id"] = True
    projects.mock(remove_owners(project_data), save=False)

    projects.assert_equal()


@pytest.mark.usefixtures(
    "authorized_admin", "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf", "project_idm_push_called"
)
def test_host_shortname_template_is_stored(test, projects):
    project_data = project_settings(host_shortname_template="{location}-{index}", dns_domain="search.yandex.net")
    result = test.api_client.post("/v1/projects", data=project_data)
    assert result.status_code == http.client.CREATED

    project = projects.mock(remove_owners(project_data), save=False)
    assert project.host_shortname_template == "{location}-{index}"
    projects.assert_equal()


@pytest.mark.usefixtures("authorized_admin", "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf")
def test_host_shortname_template_is_validated(test, projects):
    project_data = project_settings(host_shortname_template="this template is invalid", dns_domain="search.yandex.net")
    result = test.api_client.post("/v1/projects", data=project_data)

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == (
        "Request validation error: '{index}' is a required placeholder in host shortname template."
    )

    projects.assert_equal()


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf", "monkeypatch_staff_get_user_groups", "enable_idm_push"
)
def test_failed_creation_handler_deletes_project(mp, test, projects):
    mp.config("idm.system_name", "walle_test")
    exc_msg = "Oh no, IDM ceased to be!"
    mp.function(project_push.add_project_role_tree_nodes, side_effect=project_push.IDMPushException(exc_msg))
    project_json = project_settings()
    result = test.api_client.post("/v1/projects", data=project_json)
    assert result.status_code == http.client.INTERNAL_SERVER_ERROR
    assert exc_msg in result.json["message"]

    projects.assert_equal()


def _get_notifications_by_owners(owners):
    owners_emails = [authorization.get_login_email(owner) for owner in owners]
    return Notifications(
        recipients=NotificationRecipients(bot=owners_emails, warning=owners_emails, critical=owners_emails)
    )


def test_add_wrong_env_regular_user(mp, test, projects):
    env_name = TESTING_ENV_NAME
    mp.config("environment.name", env_name)

    project_json = project_settings()
    result = test.api_client.post("/v1/projects", data=project_json)
    assert result.status_code == http.client.FORBIDDEN
    assert result.json[
        "message"
    ] == "Authorization failure: This method is available only for Wall-E admins on {} environment.".format(env_name)

    projects.assert_equal()


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects",
    "monkeypatch_check_deploy_conf",
    "monkeypatch_staff_get_user_groups",
    "project_idm_push_called",
)
def test_add_wrong_env_admin(mp, test, projects):
    env_name = TESTING_ENV_NAME
    mp.config("authorization.admins", [test.api_user])
    mp.config("environment.name", env_name)

    project_json = project_settings()
    projects.mock(remove_owners(project_json), save=False)
    result = test.api_client.post("/v1/projects", data=project_json)
    assert result.status_code == http.client.CREATED

    projects.assert_equal()


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects",
    "monkeypatch_check_deploy_conf",
    "monkeypatch_staff_get_user_groups",
    "project_idm_push_called",
)
def test_add_with_enabled_reboot_via_ssh_requests_rebooter_role(mp, test, projects, batch_request_execute_mock):
    project_json = project_settings()
    projects.mock(remove_owners(project_json), save=False)
    project_json["reboot_via_ssh"] = True

    request_role_mock = mp.method(idm.BatchRequest.request_role, obj=idm.BatchRequest)
    result = test.api_client.post("/v1/projects", data=project_json)
    assert result.status_code == http.client.CREATED

    assert request_role_mock.call_count == 1
    assert request_role_mock.call_args[1] == {
        "path": ["scopes", "project", "project", "project-id", "role", "ssh_rebooter"],
        "_requester": ROBOT_WALLE_OWNER,
        "user": ROBOT_WALLE_OWNER,
    }

    projects.assert_equal()


@pytest.mark.parametrize("trusted_source", (cauth.CauthSource.WALLE, None))
@pytest.mark.parametrize("flow_type", cauth.CauthFlowType.ALL + [None])
@pytest.mark.parametrize("key_sources", (None, [cauth.CAuthKeySources.SECURE, cauth.CAuthKeySources.STAFF]))
@pytest.mark.parametrize("secure_ca_list_url", (None, "https://secure_ca_list_url"))
@pytest.mark.parametrize("insecure_ca_list_url", (None, "https://insecure_ca_list_url"))
@pytest.mark.parametrize("krl_url", (None, "https://krl_url"))
@pytest.mark.parametrize("sudo_ca_list_url", (None, "https://sudo_ca_list_url"))
@pytest.mark.usefixtures(
    "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf", "monkeypatch_staff_get_user_groups"
)
def test_add_with_cauth_settings(
    test,
    projects,
    flow_type,
    trusted_source,
    key_sources,
    secure_ca_list_url,
    insecure_ca_list_url,
    krl_url,
    sudo_ca_list_url,
):
    project_json = project_settings(cauth_settings=None)
    cauth_settings = drop_none(
        dict(
            key_sources=key_sources,
            secure_ca_list_url=secure_ca_list_url,
            insecure_ca_list_url=insecure_ca_list_url,
            krl_url=krl_url,
            sudo_ca_list_url=sudo_ca_list_url,
        )
    )
    if flow_type:
        cauth_settings["flow_type"] = flow_type
        cauth_settings["trusted_sources"] = [trusted_source] if trusted_source else []

    if cauth_settings:
        project_json.update({"cauth_settings": CauthSettingsDocument(**cauth_settings)})
    projects.mock(remove_owners(project_json), save=False)

    project_request = project_settings()
    project_request.update(
        drop_none(
            dict(
                cauth_key_sources=key_sources,
                cauth_secure_ca_list_url=secure_ca_list_url,
                cauth_insecure_ca_list_url=insecure_ca_list_url,
                cauth_krl_url=krl_url,
                cauth_sudo_ca_list_url=sudo_ca_list_url,
            )
        )
    )
    if flow_type:
        project_request.update(
            {
                "cauth_flow_type": flow_type,
                "cauth_trusted_sources": [trusted_source] if trusted_source else [],
            }
        )
    result = test.api_client.post("/v1/projects", data=project_request)

    if flow_type == cauth.CauthFlowType.BACKEND_SOURCES and trusted_source is None:
        assert result.status_code == http.client.BAD_REQUEST
        return

    assert result.status_code == http.client.CREATED
    projects.assert_equal()


@pytest.mark.parametrize("host_type", HostType.get_choices())
@pytest.mark.usefixtures(
    "monkeypatch_bot_projects", "monkeypatch_check_deploy_conf", "monkeypatch_staff_get_user_groups"
)
def test_add_with_cauth_settings_and_host_type(test, projects, host_type):
    project_json = project_settings(type=host_type)
    projects.mock(remove_owners(project_json), save=False)

    result = test.api_client.post("/v1/projects", data=project_json)

    assert result.status_code == http.client.CREATED
    projects.assert_equal()
