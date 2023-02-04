import json

import pytest

from sepelib.core import config
from walle import constants, restrictions
from walle.clients import inventory, racktables, staff, bot
from walle.clients.cms import CmsApiVersion
from walle.constants import (
    EINE_PROFILES_WITH_DC_SUPPORT,
    EINE_NOP_PROFILE,
    MTN_IP_METHOD_MAC,
    MTN_IP_METHOD_HOSTNAME,
    MTN_EXTRA_VLANS,
    MTN_NATIVE_VLAN,
    VLAN_SCHEME_MTN_HOSTID,
    VLAN_SCHEME_MTN,
    ROBOT_WALLE_OWNER,
)
from walle.errors import RequestValidationError, DNSDomainNotAllowedInCertificator, BadRequestError
from walle.expert.automation import project_automation
from walle.idm.project_role_managers import get_project_idm_role_prefix
from walle.idm.role_storage import is_role_member
from walle.projects import Project, DEFAULT_CMS_NAME, AutomationSwitch, CmsSettingsDocument
from walle.util.deploy_config import DeployConfigPolicies
from walle.util.misc import concat_dicts
from walle.util.notifications import (
    SEVERITY_AUDIT,
    SEVERITY_INFO,
    SEVERITY_WARNING,
    SEVERITY_BOT,
    SEVERITY_ERROR,
    SEVERITY_CRITICAL,
)


@pytest.fixture
def pbuilder(walle_test):
    from walle.project_builder import ProjectBuilder

    builder = ProjectBuilder(walle_test.api_issuer)
    return builder


@pytest.fixture
def auth_disabled(mp):
    mp.config("authorization.enabled", False)


def cmp_projects(pbuilder, expected):
    got = pbuilder.build()
    expected = Project(**expected)
    assert got._data == expected._data


class TestSetName:
    def test_normal_name(self, pbuilder):
        name = "Normal name"
        pbuilder.set_name(name)
        cmp_projects(pbuilder, {"name": name})

    def test_invalid_name(self, pbuilder):
        with pytest.raises(RequestValidationError):
            pbuilder.set_name("Some\nname")


class TestSetProjectTags:
    def test_normal_tags(self, pbuilder):
        pbuilder.set_project_tags(["#test", "rtc"])
        cmp_projects(pbuilder, {"tags": ["rtc", "test"]})

    def test_no_tags(self, pbuilder):
        pbuilder.set_project_tags(None)
        cmp_projects(pbuilder, {"tags": None})


class TestSetDnsDomain:
    @pytest.mark.parametrize("domain", [None, "search.yandex.net"])
    def test_set(self, pbuilder, domain):
        pbuilder.set_dns_domain(domain)
        cmp_projects(pbuilder, {"dns_domain": domain})


class TestSetCertificateDeploy:
    def test_disabled(self, pbuilder):
        pbuilder.set_certificate_deploy(None)
        cmp_projects(pbuilder, {"certificate_deploy": False})

    def test_wo_dns_domain(self, pbuilder):
        pbuilder.set_fields(dns_domain=None)
        with pytest.raises(RequestValidationError) as exc:
            pbuilder.set_certificate_deploy(True)
        assert str(exc.value) == "Request validation error: Cannot enable certificator without DNS domain."

    def test_dns_domain_not_allowed(self, pbuilder, mp):
        mp.config("certificator.allowed_dns_domains_re", [r"yandex\.net"])
        pbuilder.set_fields(dns_domain="yandex.ru")

        with pytest.raises(DNSDomainNotAllowedInCertificator):
            pbuilder.set_certificate_deploy(True)

    def test_right(self, pbuilder, mp):
        mp.config("certificator.allowed_dns_domains_re", [r"yandex\.net"])
        dns_domain = "search.yandex.net"
        pbuilder.set_fields(dns_domain=dns_domain)

        pbuilder.set_certificate_deploy(True)
        cmp_projects(pbuilder, {"certificate_deploy": True, "dns_domain": dns_domain})


class TestSetDeployConfiguration:
    @pytest.mark.parametrize("certificate", [True, False])
    def test_check_deploy_configuration_is_called(self, pbuilder, mp, certificate):
        cdc_mock = mp.function(inventory.check_deploy_configuration)
        pbuilder.set_fields(certificate_deploy=certificate)

        provisioner, config, tags = constants.PROVISIONER_LUI, "web", []
        deploy_config_policy, network = DeployConfigPolicies.PASSTHROUGH, constants.NetworkTarget.PROJECT
        # check_deploy_configuration is tested separately
        pbuilder.set_deploy_configuration(provisioner, config, deploy_config_policy, tags, network)

        cdc_mock.assert_called_once_with(provisioner, config, None, tags, certificate, network, deploy_config_policy)
        cmp_projects(
            pbuilder,
            {
                "certificate_deploy": certificate,
                "provisioner": provisioner,
                "deploy_config": config,
                "deploy_network": network,
                "deploy_tags": tags,
                "deploy_config_policy": deploy_config_policy,
            },
        )


class TestSetProfile:
    @pytest.mark.parametrize("profile", EINE_PROFILES_WITH_DC_SUPPORT)
    def test_set_profile(self, pbuilder, profile):
        pbuilder.set_profile(profile)
        cmp_projects(pbuilder, {"profile": profile})

    def test_wrong_profile(self, pbuilder):
        with pytest.raises(BadRequestError):
            pbuilder.set_profile(EINE_NOP_PROFILE)

    def test_set_no_profile(self, pbuilder):
        pbuilder.set_profile(None)
        cmp_projects(pbuilder, {"profile": None})


class TestSetProfileTags:
    def test_set_profile_tags(self, pbuilder):
        pbuilder.set_profile_tags(["bb", "aa", "bb"])
        cmp_projects(pbuilder, {"profile_tags": ["aa", "bb"]})

    def test_set_no_tags(self, pbuilder):
        pbuilder.set_profile_tags(None)
        cmp_projects(pbuilder, {"profile_tags": None})


class TestSetDefaultHostRestrictions:
    def test_set(self, pbuilder, mp):
        host_restrictions = [restrictions.AUTOMATED_REBOOT]
        strip_restrictions_mock = mp.function(restrictions.strip_restrictions, return_value=host_restrictions)

        pbuilder.set_default_host_restrictions(host_restrictions)
        assert strip_restrictions_mock.called
        cmp_projects(pbuilder, {"default_host_restrictions": host_restrictions})

    def test_set_no_restrictions(self, pbuilder):
        host_restrictions = None
        pbuilder.set_profile_tags(host_restrictions)
        cmp_projects(pbuilder, {"default_host_restrictions": host_restrictions})


base_hbf_settings = {
    "extra_vlans": MTN_EXTRA_VLANS,
    "native_vlan": MTN_NATIVE_VLAN,
}


class TestSetHbfProjectId:
    @pytest.fixture
    def get_hbf_projects_mock(self, walle_test, mp):
        mp.function(racktables.get_hbf_projects, return_value={0xFF: "HBF_PROJECT_MACRO"})

    @pytest.mark.parametrize(
        "hbf_project_id, ip_method, expected",
        [
            (None, None, {}),
            ("ff", None, concat_dicts(base_hbf_settings, {"hbf_project_id": 0xFF, "vlan_scheme": VLAN_SCHEME_MTN})),
            (
                "ff",
                MTN_IP_METHOD_MAC,
                concat_dicts(base_hbf_settings, {"hbf_project_id": 0xFF, "vlan_scheme": VLAN_SCHEME_MTN}),
            ),
            (
                "ff",
                MTN_IP_METHOD_HOSTNAME,
                concat_dicts(base_hbf_settings, {"hbf_project_id": 0xFF, "vlan_scheme": VLAN_SCHEME_MTN_HOSTID}),
            ),
        ],
    )
    def test_set(self, pbuilder, get_hbf_projects_mock, hbf_project_id, ip_method, expected):
        pbuilder.set_hbf_project_id(hbf_project_id, ip_method)
        cmp_projects(pbuilder, expected)


class TestCalculateOwners:
    def test_owners_none(self, walle_test, pbuilder):
        assert pbuilder.calculate_owners(None) == [walle_test.api_user]

    def test_owners_none_auth_disabled(self, auth_disabled, pbuilder):
        assert pbuilder.calculate_owners(None) == []

    def test_owners_empty(self, pbuilder):
        with pytest.raises(BadRequestError):
            assert pbuilder.calculate_owners([])

    def test_set_valid(self, pbuilder, mp):
        mp.function(staff.check_owners, side_effect=lambda o: sorted(o))
        assert pbuilder.calculate_owners(["user", "@group"]) == ["@group", "user"]

    def test_set_invalid(self, pbuilder, mp):
        mp.function(staff.check_owners, side_effect=staff.InvalidOwnerError("very invalid"))
        with pytest.raises(BadRequestError):
            pbuilder.calculate_owners(["user", "@group"])


class TestSetNotifications:
    def test_from_owners(self, pbuilder):
        owners = sorted(["login", "login2", "@group"])
        pbuilder.set_notifications([], owners)

        project = pbuilder.build()

        assert json.loads(project.notifications.to_json()) == {
            "recipients": {
                SEVERITY_AUDIT: [],
                SEVERITY_INFO: [],
                SEVERITY_WARNING: sorted(["login2@yandex-team.ru", "login@yandex-team.ru"]),
                SEVERITY_BOT: sorted(["login2@yandex-team.ru", "login@yandex-team.ru"]),
                SEVERITY_ERROR: [],
                SEVERITY_CRITICAL: sorted(["login2@yandex-team.ru", "login@yandex-team.ru"]),
            }
        }

    def test_from_recipients(self, pbuilder):
        owners = []
        recipients = {
            SEVERITY_ERROR: ["login@yandex-team.ru"],
            SEVERITY_INFO: ["severe-login@yandex-team.ru"],
        }
        pbuilder.set_notifications(recipients, owners)

        project = pbuilder.build()
        assert project.notifications.to_mongo() == {
            "recipients": {
                SEVERITY_AUDIT: [],
                SEVERITY_INFO: ["severe-login@yandex-team.ru"],
                SEVERITY_WARNING: [],
                SEVERITY_BOT: [],
                SEVERITY_ERROR: ["login@yandex-team.ru"],
                SEVERITY_CRITICAL: [],
            }
        }


class TestSetCmsSettings:
    def test_set_none(self, pbuilder):
        pbuilder.set_cms_settings(None, None)
        cmp_projects(
            pbuilder,
            {
                "cms": DEFAULT_CMS_NAME,
                "cms_api_version": None,
                "cms_max_busy_hosts": 5,
                "cms_tvm_app_id": None,
                "cms_settings": [
                    {
                        "cms": DEFAULT_CMS_NAME,
                        "cms_api_version": None,
                        "cms_max_busy_hosts": 5,
                        "cms_tvm_app_id": None,
                    }
                ],
            },
        )

    def test_set_default_cms_through_old_fields(self, pbuilder):
        pbuilder.set_cms_settings({"url": DEFAULT_CMS_NAME, "max_busy_hosts": 3}, None)
        cmp_projects(
            pbuilder,
            {
                "cms": DEFAULT_CMS_NAME,
                "cms_api_version": None,
                "cms_max_busy_hosts": 3,
                "cms_tvm_app_id": None,
                "cms_settings": [
                    {"cms": DEFAULT_CMS_NAME, "cms_api_version": None, "cms_max_busy_hosts": 3, "cms_tvm_app_id": None}
                ],
            },
        )

    def test_set_default_cms_through_new_fields(self, pbuilder):
        pbuilder.set_cms_settings(None, [{"url": DEFAULT_CMS_NAME, "max_busy_hosts": 3}])
        cmp_projects(
            pbuilder,
            {
                "cms": DEFAULT_CMS_NAME,
                "cms_api_version": None,
                "cms_max_busy_hosts": 3,
                "cms_tvm_app_id": None,
                "cms_settings": [
                    {"cms": DEFAULT_CMS_NAME, "cms_api_version": None, "cms_max_busy_hosts": 3, "cms_tvm_app_id": None}
                ],
            },
        )

    @pytest.mark.parametrize(
        "api_version, api_url",
        [(v, DEFAULT_CMS_NAME) for v in CmsApiVersion.ALL_CMS_API],
    )
    def test_redundant_tvm_app_id_through_old_fields(self, pbuilder, api_version, api_url):
        settings = dict(max_busy_hosts=5, api_version=api_version, url=api_url, tvm_app_id=100500)
        with pytest.raises(BadRequestError) as exc:
            pbuilder.set_cms_settings(settings, None)
        assert str(exc.value) == "CMS doesn't support TVM, but tvm app id was passed"

    @pytest.mark.parametrize(
        "api_version, api_url",
        [(v, DEFAULT_CMS_NAME) for v in CmsApiVersion.ALL_CMS_API],
    )
    def test_redundant_tvm_app_id_through_new_fields(self, pbuilder, api_version, api_url):
        settings = dict(max_busy_hosts=5, api_version=api_version, url=api_url, tvm_app_id=100500)
        with pytest.raises(BadRequestError) as exc:
            pbuilder.set_cms_settings(None, [settings])
        assert str(exc.value) == "CMS doesn't support TVM, but tvm app id was passed"

    @pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
    @pytest.mark.parametrize("tvm_app_id", [None, 100500])
    @pytest.mark.parametrize(
        "api_url",
        [
            "http://project.yandex-team.ru/cms",
            "https://project.yandex-team.ru/cms/",
            "http://project.yandex-team.ru:8081/cms",
            "https://project.yandex-team.ru:8081/cms",
            "http://project.yandex-team.ru:8081/cms",
            "https://project.yandex-team.ru:8081/cms",
            "http://dev.n.host.search.yandex.net:8081/cms",
            "https://dev.n.host.search.yandex.net:8081/cms",
            "http://dev.n.host.search.yandex.net:8081",
            "https://dev.n.host.search.yandex.net:8081",
            "http://capi-sas.yandex-team.ru:29100/cms",
            "https://capi-sas.yandex-team.ru:29100/cms",
            "https://project.yandex-team.ru/cms/v1.1",
            "https://project.yandex-team.ru/cms/v1.1/",
        ],
    )
    def test_sets_fields(self, pbuilder, api_version, tvm_app_id, api_url):
        settings = dict(api_version=api_version, url=api_url, tvm_app_id=tvm_app_id)
        pbuilder.set_cms_settings(settings, None)
        cmp_projects(
            pbuilder,
            {
                "cms": api_url,
                "cms_api_version": api_version,
                "cms_max_busy_hosts": None,
                "cms_tvm_app_id": tvm_app_id,
                "cms_settings": [
                    {
                        "cms": api_url,
                        "cms_api_version": api_version,
                        "cms_max_busy_hosts": None,
                        "cms_tvm_app_id": tvm_app_id,
                    }
                ],
            },
        )


class TestSetAutomationPlot:
    @pytest.mark.parametrize("val", [None, "rtc"])
    def test_set(self, pbuilder, val):
        pbuilder.set_automation_plot_id(val)
        cmp_projects(pbuilder, {"automation_plot_id": val})


class TestSetBotProjectId:
    bot_project_id = 100009

    @pytest.fixture
    def valid_bot_project_id(self, mp):
        mp.function(bot.is_valid_oebs_project, return_value=True)

    def test_no_bot_project_id(self, pbuilder):
        with pytest.raises(BadRequestError):
            pbuilder.set_bot_project_id(None)

    def test_no_bot_project_id_auth_disabled(self, auth_disabled, pbuilder):
        pbuilder.set_bot_project_id(None)
        cmp_projects(pbuilder, {"bot_project_id": None})

    def test_invalid_bot_project_id(self, pbuilder, mp):
        mp.function(bot.is_valid_oebs_project, return_value=False)
        with pytest.raises(BadRequestError) as exc:
            pbuilder.set_bot_project_id(self.bot_project_id)
        assert str(exc.value) == "Invalid bot project id: {}".format(self.bot_project_id)

    def test_not_allowed_bot_project_id(self, pbuilder, mp):
        allowed_bot_project_ids = [self.bot_project_id - 1, self.bot_project_id + 1]
        mp.config("bot.allowed_bot_project_ids", allowed_bot_project_ids)
        with pytest.raises(BadRequestError) as exc:
            pbuilder.set_bot_project_id(self.bot_project_id)
            assert str(exc.value) == (
                "Bot project id '{}' not allowed. List of allowed Bot project IDs: '{}.'",
                self.bot_project_id,
                sorted(allowed_bot_project_ids),
            )

    def test_set_as_admin(self, pbuilder, authorized_admin, valid_bot_project_id):
        pbuilder.set_bot_project_id(self.bot_project_id)
        cmp_projects(pbuilder, {"bot_project_id": self.bot_project_id})

    def test_set_as_nonadmin_authenticates_by_bot_project_id(self, pbuilder, mp, valid_bot_project_id):
        pbuilder.set_cms_settings({"url": DEFAULT_CMS_NAME, "max_busy_hosts": 3}, None)
        from walle.views.api.project_api.common import authenticate_user_by_bot_project_id
        from walle.views.helpers.validators import check_cms_tvm_app_id_requirements

        auth_mock = mp.function(authenticate_user_by_bot_project_id)  # tested separately
        reqs_mock = mp.function(check_cms_tvm_app_id_requirements)  # tested separately

        pbuilder.set_bot_project_id(
            self.bot_project_id,
            [CmsSettingsDocument(cms=DEFAULT_CMS_NAME, cms_api_version=CmsApiVersion.V1_0, cms_max_busy_hosts=3)],
        )
        assert auth_mock.called
        assert reqs_mock.called
        cmp_projects(
            pbuilder,
            {
                "bot_project_id": self.bot_project_id,
                "cms": "default",
                "cms_api_version": None,
                "cms_max_busy_hosts": 3,
                "cms_tvm_app_id": None,
                "cms_settings": [
                    {
                        "cms": "default",
                        "cms_api_version": None,
                        "cms_max_busy_hosts": 3,
                        "cms_tvm_app_id": None,
                    }
                ],
            },
        )


class TestRebootViaSsh:
    def test_auth_disabled_sets_without_delay(self, auth_disabled, pbuilder, monkeypatch_locks):
        pbuilder.set_fields(id="project_id")
        assert pbuilder.set_reboot_via_ssh(True)
        role_path = get_project_idm_role_prefix(pbuilder.build().id) + ["role", "ssh_rebooter"]
        assert is_role_member(role_path, ROBOT_WALLE_OWNER)

    def test_auth_disabled_doesnt_set_false(self, auth_disabled, pbuilder):
        assert not pbuilder.set_reboot_via_ssh(False)
        cmp_projects(pbuilder, {})

    @pytest.mark.parametrize("enable", [True, False])
    def test_auth_enabled(self, pbuilder, enable):
        assert pbuilder.set_reboot_via_ssh(enable) == enable
        cmp_projects(pbuilder, {})


class TestEnableDnsAutomation:
    def test_handle_none(self, pbuilder):
        pbuilder.set_enable_dns_automation(None)
        cmp_projects(pbuilder, {"dns_automation": AutomationSwitch(enabled=False)})

    def test_handle_false(self, pbuilder):
        pbuilder.set_enable_dns_automation(False)
        cmp_projects(pbuilder, {"dns_automation": AutomationSwitch(enabled=False)})

    def test_validates_vlan_and_dns_domain_if_enabled(self, pbuilder, mp):
        validator_mock = mp.method(
            project_automation.PROJECT_DNS_AUTOMATION.validate_project_dns_settings,
            obj=project_automation.PROJECT_DNS_AUTOMATION,
        )
        pbuilder.set_enable_dns_automation(True)
        assert validator_mock.called
        cmp_projects(pbuilder, {"dns_automation": AutomationSwitch(enabled=True)})

    def test_raises_understandable_error_if_needed_fields_are_not_set(self, pbuilder, mp):
        validator_mock = mp.method(
            project_automation.PROJECT_DNS_AUTOMATION.validate_project_dns_settings,
            obj=project_automation.PROJECT_DNS_AUTOMATION,
            side_effect=project_automation.DNSAutomationValidationError("some_field", "is not set!"),
        )

        with pytest.raises(BadRequestError) as exc:
            pbuilder.set_enable_dns_automation(True)

        assert validator_mock.called
        expected_msg = (
            "DNS healing automation can be enabled only for projects in MTN with DNS domain set: "
            + "Field 'some_field' is not set!"
        )
        assert str(exc.value) == expected_msg


class TestEnableHealingAutomation:
    def test_handle_none(self, pbuilder):
        pbuilder.set_enable_healing_automation(None)
        cmp_projects(pbuilder, {"healing_automation": AutomationSwitch(enabled=False)})

    @pytest.mark.parametrize("enable", [True, False])
    def test_handle_bool(self, pbuilder, enable):
        pbuilder.set_enable_healing_automation(enable)
        cmp_projects(pbuilder, {"healing_automation": AutomationSwitch(enabled=enable)})


class TestSetOwnedVlans:
    @pytest.mark.parametrize("vlans", [None, []])
    def test_handle_empty(self, pbuilder, vlans):
        pbuilder.set_owned_vlans(vlans)
        cmp_projects(pbuilder, {"owned_vlans": []})

    @pytest.mark.parametrize("vlans", [[688], [788, 688]])
    def test_set(self, pbuilder, vlans, authorized_admin):
        pbuilder.set_owned_vlans(vlans)
        cmp_projects(pbuilder, {"owned_vlans": sorted(vlans)})

    @pytest.mark.parametrize("vlans", [[688], [788, 688]])
    def test_set_as_nonadmin_raises(self, pbuilder, vlans):
        with pytest.raises(BadRequestError) as exc:
            pbuilder.set_owned_vlans(vlans)
        assert str(exc.value) == "Only Wall-e administrators can set project's owned VLANs"
