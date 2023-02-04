"""Tests project modify API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none, BOT_PROJECT_ID
from walle import projects, restrictions
from walle.clients import racktables, inventory
from walle.clients.cauth import CauthFlowType, CauthSource, CAuthKeySources
from walle.clients.cms import CmsApiVersion
from walle.constants import (
    PROVISIONERS,
    PROVISIONER_EINE,
    PROVISIONER_LUI,
    EINE_PROFILES_WITH_DC_SUPPORT,
    NetworkTarget,
    VLAN_SCHEME_MTN,
    MTN_NATIVE_VLAN,
    MTN_EXTRA_VLANS,
    VLAN_SCHEME_MTN_HOSTID,
    MTN_IP_METHOD_HOSTNAME,
    TESTING_ENV_NAME,
    FLEXY_EINE_PROFILE,
)
from walle.errors import InvalidDeployConfiguration
from walle.util.deploy_config import DeployConfigPolicies


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id, method=method, data={"name": "Other name"})
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id, method=method, data={"name": "Other name"})
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_name(test, method):
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    project.name = "Some other name"

    result = test.api_client.open("/v1/projects/" + project.id, method=method, data={"name": project.name})
    assert result.status_code == http.client.OK

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_duplicated_name(test, method):
    project1 = test.mock_project({"id": "one", "name": "One"})
    project2 = test.mock_project({"id": "two", "name": "Two"})

    result = test.api_client.open("/v1/projects/" + project2.id, method=method, data={"name": project1.name})
    assert result.status_code == http.client.CONFLICT

    test.projects.assert_equal()


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
def test_cms(test, api_version, mock_service_tvm_app_ids, mock_get_planner_id_by_bot_project_id):
    tvm_app_id = 500
    mock_service_tvm_app_ids([tvm_app_id])
    mock_get_planner_id_by_bot_project_id()

    project = test.mock_project({"id": "some-id", "name": "Some name"})
    project.cms = "http://project.yandex-team.ru/cms"
    project.cms_max_busy_hosts = None
    project.cms_api_version = api_version
    project.cms_tvm_app_id = tvm_app_id
    project.cms_settings = [
        {
            "temporary_unreachable_enabled": False,
            "cms_api_version": api_version,
            "cms_tvm_app_id": tvm_app_id,
            "cms": "http://project.yandex-team.ru/cms",
        }
    ]
    result = test.api_client.post(
        "/v1/projects/" + project.id,
        data={"cms": drop_none({"url": project.cms, "api_version": api_version, "tvm_app_id": tvm_app_id})},
    )
    assert result.status_code == http.client.OK
    test.projects.assert_equal()

    project.cms = projects.DEFAULT_CMS_NAME
    project.cms_max_busy_hosts = 1
    del project.cms_api_version
    del project.cms_tvm_app_id
    project.cms_settings = [
        {"temporary_unreachable_enabled": False, "cms_max_busy_hosts": 1, "cms": projects.DEFAULT_CMS_NAME}
    ]
    result = test.api_client.post("/v1/projects/" + project.id, data={"cms": {"url": project.cms, "max_busy_hosts": 1}})
    assert result.status_code == http.client.OK
    test.projects.assert_equal()


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
@pytest.mark.parametrize("prev_cms_tvm_app_id, cur_cms_tvm_app_id", [(None, 500), (500, 600)])
def test_cms_tvm_app_id(
    test,
    api_version,
    prev_cms_tvm_app_id,
    cur_cms_tvm_app_id,
    mock_get_planner_id_by_bot_project_id,
    mock_service_tvm_app_ids,
):
    mock_get_planner_id_by_bot_project_id()
    mock_service_tvm_app_ids([cur_cms_tvm_app_id])

    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "cms": "http://project.yandex-team.ru/cms",
            "cms_max_busy_hosts": None,
            "cms_api_version": api_version,
            "cms_tvm_app_id": prev_cms_tvm_app_id,
        }
    )

    result = test.api_client.post(
        "/v1/projects/" + project.id,
        data={"cms": drop_none({"url": project.cms, "api_version": api_version, "tvm_app_id": cur_cms_tvm_app_id})},
    )
    assert result.status_code == http.client.OK

    project.cms_tvm_app_id = cur_cms_tvm_app_id
    project.cms_settings = [
        {
            "temporary_unreachable_enabled": False,
            "cms_api_version": api_version,
            "cms_tvm_app_id": cur_cms_tvm_app_id,
            "cms": project.cms,
        }
    ]
    test.projects.assert_equal()


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
@pytest.mark.parametrize("prev_cms_tvm_app_id, cur_cms_tvm_app_id", [(None, 500), (500, 600)])
def test_cms_tvm_app_id_not_belonging_to_service(
    test,
    api_version,
    prev_cms_tvm_app_id,
    cur_cms_tvm_app_id,
    mock_get_planner_id_by_bot_project_id,
    mock_service_tvm_app_ids,
    mock_abc_get_service_slug,
):
    mock_get_planner_id_by_bot_project_id()
    mock_service_tvm_app_ids()
    mock_abc_get_service_slug()

    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "cms": "http://project.yandex-team.ru/cms",
            "cms_max_busy_hosts": None,
            "cms_api_version": api_version,
            "cms_tvm_app_id": prev_cms_tvm_app_id,
        }
    )

    result = test.api_client.post(
        "/v1/projects/" + project.id,
        data={"cms": drop_none({"url": project.cms, "api_version": api_version, "tvm_app_id": cur_cms_tvm_app_id})},
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == "CMS TVM app id {} is not registered in ABC service {}".format(
        cur_cms_tvm_app_id,
        "some_service",
    )

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("tags", [[], ["#test"], ["#yt", "#test"]])
def test_modify_tags(test, method, tags):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "tags": ["test"]}))
    result = test.api_client.open("/v1/projects/" + project.id, method=method, data={"tags": tags})

    if tags:
        project.tags = sorted([tag.strip("#") for tag in tags])
    else:
        del project.tags

    assert result.status_code == http.client.OK
    test.projects.assert_equal()


@pytest.mark.parametrize("old_hbf_project_id", [None, 0x96E4])
@pytest.mark.parametrize("ip_method", [None, "mac"])
def test_hbf_project_id(test, old_hbf_project_id, mp, ip_method):
    _HBF_PROJECT_ID_STR = "984e3"
    _HBF_PROJECT_ID_INT = 0x984E3

    mp.function(racktables.get_hbf_projects, return_value={_HBF_PROJECT_ID_INT: "HBF_PROJECT_MACRO"})

    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "hbf_project_id": old_hbf_project_id}))

    result = test.api_client.post(
        "/v1/projects/" + project.id, data=drop_none({"hbf_project_id": _HBF_PROJECT_ID_STR, "ip_method": ip_method})
    )
    assert result.status_code == http.client.OK

    project.hbf_project_id = _HBF_PROJECT_ID_INT
    project.vlan_scheme = VLAN_SCHEME_MTN
    project.native_vlan = MTN_NATIVE_VLAN
    project.extra_vlans = MTN_EXTRA_VLANS

    test.projects.assert_equal()


@pytest.mark.parametrize("old_hbf_project_id", [None, 0x96E4])
def test_hbf_project_id_with_hostid_ip_method(test, old_hbf_project_id, mp):
    _HBF_PROJECT_ID_STR = "984e3"
    _HBF_PROJECT_ID_INT = 0x984E3

    mp.function(racktables.get_hbf_projects, return_value={_HBF_PROJECT_ID_INT: "HBF_PROJECT_MACRO"})

    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "hbf_project_id": old_hbf_project_id}))

    result = test.api_client.post(
        "/v1/projects/" + project.id, data={"hbf_project_id": _HBF_PROJECT_ID_STR, "ip_method": MTN_IP_METHOD_HOSTNAME}
    )
    assert result.status_code == http.client.OK

    project.hbf_project_id = _HBF_PROJECT_ID_INT
    project.vlan_scheme = VLAN_SCHEME_MTN_HOSTID
    project.native_vlan = MTN_NATIVE_VLAN
    project.extra_vlans = MTN_EXTRA_VLANS

    test.projects.assert_equal()


@pytest.mark.parametrize("bot_project_id", [None, 600019])
def test_bot_project_id(test, bot_project_id):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "bot_project_id": bot_project_id}))

    result = test.api_client.post("/v1/projects/" + project.id, data={"bot_project_id": BOT_PROJECT_ID})
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.mark.parametrize("bot_project_id", [None, 600019])
def test_validate_bot_project_id_to_false(test, bot_project_id):
    project = test.mock_project(
        drop_none(
            {"id": "some-id", "name": "Some name", "bot_project_id": bot_project_id, "validate_bot_project_id": True}
        )
    )

    fail_result = test.api_client.post("/v1/projects/" + project.id, data={"validate_bot_project_id": False})
    assert fail_result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.mark.parametrize("bot_project_id", [None, 600019])
def test_validate_bot_project_id_to_true(test, bot_project_id):
    project = test.mock_project(
        drop_none(
            {"id": "some-id", "name": "Some name", "bot_project_id": bot_project_id, "validate_bot_project_id": True}
        )
    )

    ok_result = test.api_client.post("/v1/projects/" + project.id, data={"validate_bot_project_id": True})
    assert ok_result.status_code == http.client.OK
    test.projects.assert_equal()


@pytest.mark.parametrize("bot_project_id", [None, 600019])
def test_validate_bot_project_id_admin_to_false(test, bot_project_id, authorized_admin):
    project = test.mock_project(
        drop_none(
            {"id": "some-id", "name": "Some name", "bot_project_id": bot_project_id, "validate_bot_project_id": False}
        )
    )

    ok_result = test.api_client.post("/v1/projects/" + project.id, data={"validate_bot_project_id": False})
    assert ok_result.status_code == http.client.OK

    test.projects.assert_equal()


def test_modify_with_custom_cms_and_max_busy_hosts(test):
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    result = test.api_client.post(
        "/v1/projects/" + project.id,
        data={"name": "Some name", "cms": {"url": "http://foo.bar/zar", "max_busy_hosts": 10}},
    )
    assert result.status_code == http.client.BAD_REQUEST
    test.projects.assert_equal()


@pytest.mark.parametrize("max_busy_hosts", [None, 0, "", "-"])
def test_modify_default_cms_invalid_max_busy_hosts(test, max_busy_hosts):
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    result = test.api_client.post(
        "/v1/projects/" + project.id,
        data={
            "name": "Some name",
            "cms": drop_none({"url": projects.DEFAULT_CMS_NAME, "max_busy_hosts": max_busy_hosts}),
        },
    )
    assert result.status_code == http.client.BAD_REQUEST
    test.projects.assert_equal()


@pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
def test_modify_default_cms_to_custom_without_cms_tvm_app_id(test, api_version):
    project = test.mock_project(
        {"id": "some-id", "name": "Some name", "cms": projects.DEFAULT_CMS_NAME, "cms_max_busy_hosts": 5}
    )
    result = test.api_client.post(
        "/v1/projects/" + project.id,
        data={"cms": {"url": "https://project.yandex-team.ru/cms/", "api_version": api_version}},
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "CMS TVM app id must be set for non-default CMSes. If your CMS does not support "
        "TVM yet, please contact Wall-e administrators"
    )
    test.projects.assert_equal()


def test_default_profile(test):
    project = test.mock_project({"id": "some-id"})
    del project.profile
    del project.profile_tags
    project.save()

    tags = ["_set:do-not-use-snmp-64", "a", "b"]

    result = test.api_client.put(
        "/v1/projects/{}/host-profiling-config".format(project.id),
        data={"profile": "profile-mock", "profile_tags": tags},
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == (
        "At this time only '{}' profile can be set as default "
        "host profile.".format(", ".join(EINE_PROFILES_WITH_DC_SUPPORT))
    )
    test.projects.assert_equal()

    chosen_profile = FLEXY_EINE_PROFILE
    result = test.api_client.put(
        "/v1/projects/{}/host-profiling-config".format(project.id),
        data={"profile": chosen_profile, "profile_tags": tags},
    )
    project.profile = chosen_profile
    project.profile_tags = tags[:]
    assert result.status_code == http.client.OK
    test.projects.assert_equal()

    tags = ["_set:need-lan-flash-1000", "c", "d"]

    result = test.api_client.put(
        "/v1/projects/{}/host-profiling-config".format(project.id),
        data={"profile": chosen_profile, "profile_tags": tags},
    )
    project.profile = chosen_profile
    project.profile_tags = tags[:]
    assert result.status_code == http.client.OK
    test.projects.assert_equal()


@pytest.mark.parametrize("provisioner", PROVISIONERS + [None])
@pytest.mark.parametrize("deploy_config", ["some-config", None])
@pytest.mark.parametrize("deploy_config_policy", DeployConfigPolicies.get_all_names() + [None])
@pytest.mark.parametrize("deploy_tags", [["_set:do-not-use-snmp-64"], [], None])
@pytest.mark.parametrize("deploy_network", NetworkTarget.DEPLOYABLE + [None])
def test_provisioner_and_config_and_tags(
    mp, test, provisioner, deploy_config, deploy_config_policy, deploy_tags, deploy_network
):
    need_certificate = False
    raise_error = (
        deploy_config is None
        or (provisioner == PROVISIONER_LUI and deploy_tags is not None)
        or (provisioner == PROVISIONER_EINE and deploy_config_policy is not None)
    )
    if raise_error:
        mp.function(inventory.check_deploy_configuration, side_effect=InvalidDeployConfiguration(""))
    else:
        mp.function(inventory.check_deploy_configuration)

    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "provisioner": PROVISIONER_EINE,
            "deploy_config": "project-deploy-config",
            "deploy_tags": ["some-tag"],
        }
    )

    result = test.api_client.put(
        "/v1/projects/{}/host-provisioner-config".format(project.id),
        data=drop_none(
            {
                "provisioner": provisioner,
                "deploy_config": deploy_config,
                "deploy_config_policy": deploy_config_policy,
                "deploy_tags": deploy_tags,
                "deploy_network": deploy_network,
            }
        ),
    )

    default_tags = None
    if provisioner != PROVISIONER_LUI and deploy_tags is None:
        deploy_tags = ["some-tag"]
    deploy_tags = deploy_tags or default_tags

    if deploy_config is not None:
        inventory.check_deploy_configuration.assert_called_once_with(
            provisioner or PROVISIONER_EINE,
            deploy_config,
            None,
            deploy_tags,
            need_certificate,
            deploy_network,
            deploy_config_policy,
        )
    else:
        assert not inventory.check_deploy_configuration.called

    if raise_error:
        assert result.status_code == http.client.BAD_REQUEST
        return

    assert result.status_code == http.client.OK

    if deploy_config is not None:
        project.deploy_config = deploy_config
        if provisioner is not None:
            project.provisioner = provisioner
        if deploy_network:
            project.deploy_network = deploy_network
        if deploy_config_policy:
            project.deploy_config_policy = deploy_config_policy

    project.deploy_tags = deploy_tags

    test.projects.assert_equal()


@pytest.mark.parametrize("originally_enabled", [False, True])
@pytest.mark.parametrize("enabled", [False, True])
@pytest.mark.usefixtures("mock_certificator_allowed_domain_list")
def test_certificate_deploy(test, mp, originally_enabled, enabled):
    project = test.mock_project(
        drop_none(
            {
                "id": "some-id",
                "name": "Some name",
                "dns_domain": "search.yandex.net",
                "certificate_deploy": originally_enabled,
            }
        )
    )

    result = test.api_client.post("/v1/projects/" + project.id, data={"certificate_deploy": enabled})
    assert result.status_code == http.client.OK

    project.certificate_deploy = enabled
    test.projects.assert_equal()


@pytest.mark.usefixtures("mock_certificator_allowed_domain_list")
@pytest.mark.parametrize("dns_domain", ("notinwl.yandex.net", "blabusyt.yandex.net"))
def test_certificate_deploy_with_dns_domain_not_allowed(test, mp, dns_domain):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "dns_domain": dns_domain}))

    result = test.api_client.post("/v1/projects/" + project.id, data={"certificate_deploy": True})
    assert result.status_code == http.client.BAD_REQUEST

    test.projects.assert_equal()


@pytest.mark.parametrize("originally_existed", [False, True])
def test_setting_new_hostname_template(test, mp, originally_existed):
    project = test.mock_project(
        drop_none(
            {
                "id": "some-id",
                "name": "Some name",
                "dns_domain": "search.yandex.net",
                "host_shortname_template": "{location}-{index}" if originally_existed else None,
            }
        )
    )

    new_hostname_template = "hostname-{location}-{index}"
    result = test.api_client.post("/v1/projects/" + project.id, data={"host_shortname_template": new_hostname_template})
    assert result.status_code == http.client.OK

    project.host_shortname_template = new_hostname_template
    test.projects.assert_equal()


def test_new_hostname_template_is_validated(test, mp):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "dns_domain": "search.yandex.net"}))

    new_hostname_template = "1-host.name-{location}-{index}"
    result = test.api_client.post("/v1/projects/" + project.id, data={"host_shortname_template": new_hostname_template})
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'] == (
        "Request validation error: host shortname template does not describe a valid host shortname."
    )

    test.projects.assert_equal()


def test_hostname_template_only_allowed_when_dns_domain_set(test, mp):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name"}))

    result = test.api_client.post("/v1/projects/" + project.id, data={"host_shortname_template": "{location}-{index}"})
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'] == "Can not use custom host shortname template in project without dns settings."
    test.projects.assert_equal()


def test_restrictions(test):
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    project.name = "Some other name"

    result = test.api_client.post(
        "/v1/projects/" + project.id,
        data={
            "name": project.name,
            "default_host_restrictions": [
                restrictions.AUTOMATED_REBOOT,
                restrictions.AUTOMATED_REDEPLOY,
                restrictions.REDEPLOY,
            ],
        },
    )
    assert result.status_code == http.client.OK
    project.default_host_restrictions = [restrictions.AUTOMATED_REBOOT, restrictions.REDEPLOY]
    test.projects.assert_equal()

    result = test.api_client.post("/v1/projects/" + project.id, data={"default_host_restrictions": []})
    assert result.status_code == http.client.OK
    del project.default_host_restrictions
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_missing(test, method):
    test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/some-other-id", method=method, data={"name": "Some other ID"})
    assert result.status_code == http.client.NOT_FOUND

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_wrong_env_regular_user(test, method, mp):
    env_name = TESTING_ENV_NAME
    mp.config("environment.name", env_name)
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    new_name = "Some other name"

    result = test.api_client.open("/v1/projects/" + project.id, method=method, data={"name": new_name})
    assert result.status_code == http.client.FORBIDDEN
    assert result.json[
        "message"
    ] == "Authorization failure: This method is available only for Wall-E admins on {} environment.".format(env_name)

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_wrong_env_admin(test, method, mp):
    env_name = TESTING_ENV_NAME
    mp.config("authorization.admins", [test.api_user])
    mp.config("environment.name", env_name)
    project = test.mock_project({"id": "some-id", "name": "Some name"})
    project.name = "Some other name"

    result = test.api_client.open("/v1/projects/" + project.id, method=method, data={"name": project.name})
    assert result.status_code == http.client.OK

    test.projects.assert_equal()


@pytest.mark.parametrize("trusted_source", (CauthSource.WALLE, None))
@pytest.mark.parametrize("flow_type", CauthFlowType.ALL + [None])
@pytest.mark.parametrize("key_sources", (None, [CAuthKeySources.SECURE, CAuthKeySources.STAFF]))
@pytest.mark.parametrize("secure_ca_list_url", (None, "https://secure_ca_list_url"))
@pytest.mark.parametrize("insecure_ca_list_url", (None, "https://insecure_ca_list_url"))
@pytest.mark.parametrize("krl_url", (None, "https://krl_url"))
@pytest.mark.parametrize("sudo_ca_list_url", (None, "https://sudo_ca_list_url"))
def test_modify_cauth_settings(
    test, flow_type, trusted_source, key_sources, secure_ca_list_url, insecure_ca_list_url, krl_url, sudo_ca_list_url
):
    project = test.mock_project({"id": "some-id", "name": "Some name"})
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
        project.cauth_settings = projects.CauthSettingsDocument(**cauth_settings)

    cauth_params = drop_none(
        dict(
            cauth_key_sources=key_sources,
            cauth_secure_ca_list_url=secure_ca_list_url,
            cauth_insecure_ca_list_url=insecure_ca_list_url,
            cauth_krl_url=krl_url,
            cauth_sudo_ca_list_url=sudo_ca_list_url,
        )
    )
    if flow_type:
        cauth_params.update(
            {
                "cauth_flow_type": flow_type,
                "cauth_trusted_sources": [trusted_source] if trusted_source else [],
            }
        )

    result = test.api_client.post("/v1/projects/" + project.id, data={"name": project.name, **cauth_params})
    if flow_type == CauthFlowType.BACKEND_SOURCES and not trusted_source:
        assert result.status_code == http.client.BAD_REQUEST
        return

    assert result.status_code == http.client.OK
    test.projects.assert_equal()


@pytest.mark.parametrize("trusted_sources", ([CauthSource.WALLE], CauthSource.ALL, None))
def test_modify_cauth_trusted_sources(test, trusted_sources):
    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "cauth_settings": projects.CauthSettingsDocument(
                flow_type=CauthFlowType.BACKEND_SOURCES,
                trusted_sources=CauthSource.ALL,
            ),
        }
    )

    result = test.api_client.post(
        "/v1/projects/" + project.id, data={"name": project.name, "cauth_trusted_sources": trusted_sources}
    )
    if not trusted_sources:
        assert result.status_code == http.client.BAD_REQUEST
        return

    project.cauth_settings.trusted_sources = trusted_sources
    assert result.status_code == http.client.OK
    test.projects.assert_equal()
