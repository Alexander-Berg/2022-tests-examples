"""Tests project GET API."""

import http.client

import pytest

from infra.walle.server.tests.lib import dns as dns_test_lib
from infra.walle.server.tests.lib.idm_util import make_role_dict
from infra.walle.server.tests.lib.util import TestCase, set_project_owners
from tests.api.common import toggle_reboot_via_ssh
from walle import restrictions, boxes
from walle.clients import idm, staff, cms
from walle.idm.project_role_managers import ProjectRole
from walle.projects import Project

USER = TestCase.api_user


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_get_project(test, iterate_authentication):
    test.mock_projects()
    project = test.mock_project({"id": "some-id", "default_host_restrictions": [restrictions.REBOOT]})

    result = test.api_client.get("/v1/projects/" + project.id)
    assert result.status_code == http.client.OK
    assert result.json == project.to_api_obj()

    result = test.api_client.get("/v1/projects/" + project.id, query_string={"fields": "name"})
    assert result.status_code == http.client.OK
    assert result.json == {"name": project.name}

    test.projects.assert_equal()


class TestSyntheticRolesField:
    @pytest.mark.parametrize(
        "role, members", [(ProjectRole.USER, ["some-user", "@group"]), (ProjectRole.NOC_ACCESS, ["@svc_doortortc"])]
    )
    def test_get_one_project(self, test, role, members):
        project = test.mock_project({"id": "some-id"})
        self.add_roles(project, {role: members})

        result = test.api_client.get("/v1/projects/" + project.id, query_string={"fields": "roles,name"})
        assert result.status_code == http.client.OK
        assert result.json["name"] == project.name
        assert result.json["roles"][role] == members

        test.projects.assert_equal()

    @pytest.mark.parametrize("role", ProjectRole.ALL)
    def test_get_one_project_expand_roles(self, mp, test, role):
        project = test.mock_project({"id": "some-id", "roles": {role: ["user", "@group_incl_user", "@group2"]}})
        mp.function(
            staff.batch_get_groups_members,
            return_value={"@group_incl_user": ["user", "user2"], "@group2": ["user2", "user3"]},
        )

        result = test.api_client.get(
            "/v1/projects/" + project.id, query_string={"fields": "roles,name", "expand_groups": True}
        )
        assert result.status_code == http.client.OK
        assert result.json["name"] == project.name
        assert result.json["roles"][role] == ["user", "user2", "user3"]

        test.projects.assert_equal()

    def test_get_multiple_projects(self, test):
        prj1 = test.mock_project({"id": "first-id"})
        prj1_roles = {"user": ["feeble_sre", "@mighty_manager_gang"]}
        self.add_roles(prj1, prj1_roles)

        prj2 = test.mock_project({"id": "second-id"})
        prj2_roles = {"noc_access": ["@srv_noc_hideout"], "user": ["disempowered_user", "feeble_sre"]}
        self.add_roles(prj2, prj2_roles)

        resp = test.api_client.get("/v1/projects", query_string={"fields": "roles,name"})
        assert resp.status_code == http.client.OK
        res = resp.json["result"]

        assert res[0]["name"] == "Name for first-id"
        assert res[0]["roles"]["user"] == prj1_roles["user"]
        assert res[0]["roles"]["noc_access"] == []

        assert res[1]["name"] == "Name for mocked-default-project"
        assert res[1]["roles"]["user"] == []
        assert res[1]["roles"]["noc_access"] == []

        assert res[2]["name"] == "Name for second-id"
        assert res[2]["roles"]["user"] == prj2_roles["user"]
        assert res[2]["roles"]["noc_access"] == prj2_roles["noc_access"]

        test.projects.assert_equal()

    def add_roles(self, project, role_to_members):
        for role in role_to_members:
            role_manager = ProjectRole.get_role_manager(role, project)
            for member in role_to_members[role]:
                role_manager.add_member(member)


def test_get_missing_project(test):
    test.mock_projects()

    result = test.api_client.get("/v1/projects/some-invalid-id")
    assert result.status_code == http.client.NOT_FOUND

    test.projects.assert_equal()


class TestSyntheticRebootViaSshField:
    @pytest.mark.parametrize(
        "enable_reboot_via_ssh,set_field,expected",
        [(False, True, None), (False, False, None), (True, False, True), (True, True, True)],
    )
    def test_postprocessor_overrides_project_field(self, test, enable_reboot_via_ssh, set_field, expected):
        """Even if project.reboot_via_ssh is set, ProjectPostprocessor must take field value from role storage"""
        project_params = {"id": "some-id"}
        if enable_reboot_via_ssh:
            project_params["reboot_via_ssh"] = True
        project = test.mock_project(project_params)
        toggle_reboot_via_ssh(project, enable=enable_reboot_via_ssh)

        result = test.api_client.get("/v1/projects/" + project.id, query_string={"fields": "reboot_via_ssh,name"})
        assert result.status_code == http.client.OK
        assert result.json.get("reboot_via_ssh", None) == expected

        test.projects.assert_equal()


class TestSyntheticOwnersField:
    @pytest.mark.parametrize(
        "owners_in_project,owners_in_storage",
        [
            (None, []),
            ([], ["user_we_care_about"]),
            (["nevermind", "@svc_in_vain"], ["@very_important_group"]),
            (["no_one_cares"], []),
        ],
    )
    def test_postprocessor_overrides_project_field(self, test, owners_in_project, owners_in_storage):
        project = test.mock_project({"id": "some-id"})
        project.owners = owners_in_project
        project.save()
        set_project_owners(project, owners_in_storage)

        result = test.api_client.get("/v1/projects/" + project.id, query_string={"fields": "owners,name"})
        assert result.status_code == http.client.OK
        assert set(result.json["owners"]) == set(owners_in_storage)

        test.projects.assert_equal()


def test_get_projects(test, iterate_authentication):
    test.mock_projects()

    result = test.api_client.get("/v1/projects")
    assert result.status_code == http.client.OK
    assert result.json == {
        "result": [project.to_api_obj() for project in sorted(test.projects.objects, key=lambda project: project.id)]
    }

    test.projects.assert_equal()


def test_get_projects_with_fields(test):
    test.mock_projects()

    result = test.api_client.get("/v1/projects?fields=name")
    assert result.status_code == http.client.OK
    assert result.json == {
        "result": [{"name": project.name} for project in sorted(test.projects.objects, key=lambda project: project.id)]
    }

    test.projects.assert_equal()


def test_get_projects_by_tag(test, iterate_authentication):
    test.mock_projects()
    for ind, tags in enumerate([["rtc"], ["mtn"], ["rtc", "mtn"], ["qloud", "mtn"]]):
        test.projects.objects[ind].tags = tags
        test.projects.objects[ind].save()

    result = test.api_client.get("/v1/projects?tags=mtn")
    assert result.status_code == http.client.OK
    assert result.json == {
        "result": [project.to_api_obj() for project in sorted(test.projects.objects[1:], key=lambda prj: prj.id)]
    }

    test.projects.assert_equal()


def test_get_projects_by_tags_intersection(test, iterate_authentication):
    test.mock_projects()
    for ind, tags in enumerate([["rtc"], ["mtn"], ["rtc", "mtn"], ["qloud", "mtn"]]):
        test.projects.objects[ind].tags = tags
        test.projects.objects[ind].save()

    result = test.api_client.get("/v1/projects?tags=rtc,mtn")
    assert result.status_code == http.client.OK
    assert result.json == {
        "result": [project.to_api_obj() for project in sorted([test.projects.objects[2]], key=lambda prj: prj.id)]
    }

    test.projects.assert_equal()


@pytest.mark.parametrize(
    ["healing_enabled", "dns_enabled"], [(True, False), (True, True), (False, True), (False, False)]
)
def test_get_projects_with_old_automation_field(test, healing_enabled, dns_enabled):
    """Test support for old client."""
    test.mock_projects(
        healing_automation={"enabled": healing_enabled},
        dns_automation={"enabled": dns_enabled},
    )

    result = test.api_client.get("/v1/projects?fields=name,enable_automation")
    assert result.status_code == http.client.OK
    assert result.json == {
        "result": [
            {"name": project.name, "enable_automation": project.healing_automation.enabled}
            for project in sorted(test.projects.objects, key=lambda project: project.id)
        ]
    }

    test.projects.assert_equal()


@pytest.mark.parametrize(
    "fields", [("healing_automation",), ("dns_automation",), ("healing_automation", "dns_automation")]
)
@pytest.mark.parametrize("automation_enabled", [True, False])
def test_get_projects_with_new_automation_field(test, fields, automation_enabled):
    """Test support for old client."""
    test.mock_projects(**{field: {"enabled": automation_enabled} for field in fields})

    result = test.api_client.get("/v1/projects?fields=name,{}".format(",".join(f + ".enabled" for f in fields)))
    assert result.status_code == http.client.OK
    assert result.json == {
        "result": [
            dict({f: {"enabled": getattr(project, f).enabled} for f in fields}, name=project.name)
            for project in sorted(test.projects.objects, key=lambda project: project.id)
        ]
    }

    test.projects.assert_equal()


@pytest.mark.parametrize(
    "requested_roles, expected_requested_roles",
    [
        ([], []),
        (
            [make_role_dict(2222, "requested", "@svc_group"), make_role_dict(3333, "requested", "blubur")],
            ["@svc_group", "blubur"],
        ),
    ],
)
def test_get_requested_roles(walle_test, mp, requested_roles, expected_requested_roles):
    project = walle_test.mock_project({"id": "project-id", "owners": ["some_miserable_sre"]})
    mp.function(idm.iter_role_dicts, return_value=requested_roles)

    resp = walle_test.api_client.get("/v1/projects/{}/requested_owners".format(project.id))
    assert resp.status_code == http.client.OK
    assert resp.json["result"] == expected_requested_roles


@pytest.mark.parametrize(
    "revoking_roles, expected_revoking_roles",
    [
        ([], []),
        (
            [make_role_dict(2222, "depriving", "@svc_group"), make_role_dict(3333, "depriving", "blubur")],
            ["@svc_group", "blubur"],
        ),
    ],
)
def test_get_revoking_roles(walle_test, mp, revoking_roles, expected_revoking_roles):
    project = walle_test.mock_project({"id": "project-id", "owners": ["some_miserable_sre"]})
    iter_roles_mock = mp.function(idm.iter_role_dicts, return_value=revoking_roles)

    resp = walle_test.api_client.get("/v1/projects/{}/revoking_owners".format(project.id))
    assert iter_roles_mock.call_args[1]["state"] == ["depriving", "depriving_validation"]
    assert resp.status_code == http.client.OK
    assert resp.json["result"] == expected_revoking_roles


@pytest.mark.parametrize(
    "requested_roles, expected_requested_roles",
    [
        ([], {}),
        (
            [make_role_dict(2222, "requested", "@svc_group"), make_role_dict(3333, "requested", "blubur")],
            {"@svc_group": 2222, "blubur": 3333},
        ),
    ],
)
def test_get_requested_roles_with_id(walle_test, mp, requested_roles, expected_requested_roles):
    project = walle_test.mock_project({"id": "project-id", "owners": ["some_miserable_sre"]})
    mp.function(idm.iter_role_dicts, return_value=requested_roles)

    resp = walle_test.api_client.get("/v1/projects/{}/requested_owners_with_request_id".format(project.id))
    assert resp.status_code == http.client.OK
    assert resp.json["result"] == expected_requested_roles


@pytest.mark.parametrize(
    "roles, expected_roles",
    [
        ([], []),
        (
            [
                make_role_dict(1111, "granted", "brabur", "owner"),
                make_role_dict(2222, "requested", "@svc_group", "superuser"),
                make_role_dict(3333, "deprived", "blubur", "user"),
                make_role_dict(4444, "requested", "burbur", "noc_access"),
            ],
            [
                {"member": "brabur", "role": "owner", "role_id": 1111, "state": "active"},
                {"member": "@svc_group", "role": "superuser", "role_id": 2222, "state": "requested"},
                {"member": "burbur", "role": "noc_access", "role_id": 4444, "state": "requested"},
            ],
        ),
    ],
)
def test_roles_status(walle_test, mp, roles, expected_roles):
    project = walle_test.mock_project({"id": "project-id"})
    mp.function(idm.iter_role_dicts, return_value=roles)

    resp = walle_test.api_client.get("/v1/projects/{}/roles_state".format(project.id))
    assert resp.status_code == http.client.OK
    assert resp.json["result"] == expected_roles


@pytest.mark.parametrize(
    "revoking_roles, expected_revoking_roles",
    [
        ([], {}),
        (
            [make_role_dict(2222, "depriving", "@svc_group"), make_role_dict(3333, "depriving", "blubur")],
            {"@svc_group": 2222, "blubur": 3333},
        ),
    ],
)
def test_get_revoking_roles_with_id(walle_test, mp, revoking_roles, expected_revoking_roles):
    project = walle_test.mock_project({"id": "project-id", "owners": ["some_miserable_sre"]})
    iter_roles_mock = mp.function(idm.iter_role_dicts, return_value=revoking_roles)

    resp = walle_test.api_client.get("/v1/projects/{}/revoking_owners_with_request_id".format(project.id))
    assert iter_roles_mock.call_args[1]["state"] == ["depriving", "depriving_validation"]
    assert resp.status_code == http.client.OK
    assert resp.json["result"] == expected_revoking_roles


@pytest.mark.parametrize(
    "roles, user_groups, expected_is_owner",
    [
        ({"owner": []}, {}, False),
        ({"owner": [USER]}, {}, True),
        ({"owner": ["@owner_group"]}, {"@owner_group"}, True),
        ({"owner": ["@other_group"]}, {"@owner_group"}, False),
        ({"superuser": [USER], "owner": []}, {}, True),
        ({"superuser": ["@superuser_group"]}, {"@superuser_group"}, True),
        ({"superuser": ["@other_group"], "owner": []}, {"@superuser_group"}, False),
        ({"user": [USER], "owner": []}, {}, True),
        ({"user": ["@user_group"], "owner": []}, {"@user_group"}, True),
        ({"user": ["@other_group"], "owner": []}, {"@user_group"}, False),
    ],
)
def test_is_project_owner(walle_test, mp, roles, user_groups, expected_is_owner):
    mp.function(staff.get_user_groups, return_value=user_groups)

    project = walle_test.mock_project({"id": "project-id", "roles": roles})

    resp = walle_test.api_client.get("/v1/projects/{}/is_project_owner/{}".format(project.id, walle_test.api_user))
    assert resp.status_code == http.client.OK
    assert resp.json["is_owner"] == expected_is_owner


def test_get_cms_settings(walle_test, mp, mock_service_tvm_app_ids):
    CMS_URL = "http://project.yandex-team.ru/cms"
    CMS_TVM_APP_ID = 500

    mock_service_tvm_app_ids([CMS_TVM_APP_ID])

    data = {
        "id": "some-id",
        "name": "Some name",
        "cms": CMS_URL,
        "cms_max_busy_hosts": None,
        "cms_api_version": cms.CmsApiVersion.V1_4,
        "cms_tvm_app_id": CMS_TVM_APP_ID,
        "cms_settings": [
            {
                "temporary_unreachable_enabled": False,
                "cms_api_version": cms.CmsApiVersion.V1_4,
                "cms_tvm_app_id": CMS_TVM_APP_ID,
                "cms": CMS_URL,
            }
        ],
    }

    project = walle_test.mock_project(data)
    del data["cms_max_busy_hosts"]

    result = walle_test.api_client.get(
        "/v1/projects/{}?fields=id,name,cms_settings,"
        "cms,cms_max_busy_hosts,cms_api_version,cms_tvm_app_id".format(project.id)
    )
    assert result.status_code == http.client.OK
    assert result.json == data

    walle_test.projects.assert_equal()


def test_get_slayer_dns_zone(walle_test):
    project_id = "some"
    walle_test.mock_project({"id": project_id, "name": "Some name"})
    result = walle_test.api_client.get(f"/v1/projects/{project_id}?fields=name,yc_dns_zone")

    assert result.status_code == http.client.OK
    assert result.json[Project.YC_DNS_ZONE_CALCULATED_FIELD]["link"] == ""
    assert result.json[Project.YC_DNS_ZONE_CALCULATED_FIELD]["domain"] == ""


def test_get_rurikk_dns_zone(walle_test, dns_box):
    walle_test.mock_project(
        {
            "id": dns_test_lib.TEST_DNS_BOX_PROJECT,
            "name": "Some name",
            "yc_dns_zone_id": dns_test_lib.TEST_DNS_ZONE_ID,
        }
    )
    result = walle_test.api_client.get(f"/v1/projects/{dns_test_lib.TEST_DNS_BOX_PROJECT}?fields=name,yc_dns_zone")

    assert result.status_code == http.client.OK
    link = result.json[Project.YC_DNS_ZONE_CALCULATED_FIELD]["link"]
    assert dns_test_lib.TEST_CONSOLE_URL in link
    assert dns_test_lib.TEST_DNS_ZONE_ID in link
    assert dns_test_lib.TEST_FOLDER_ID in link
    assert result.json[Project.YC_DNS_ZONE_CALCULATED_FIELD]["domain"] == dns_test_lib.TEST_RURIKK_DNS_ZONE


def test_boxes(mp, walle_test, dns_box):
    project_id = "some"
    dns_box_name = "dns_box"
    eine_box_name = "dns_box"
    mp.config(
        boxes.CONFIG_BOX_MAPPING_SECTION,
        {
            project_id: {
                boxes.BoxType.dns: dns_box_name,
                boxes.BoxType.eine: eine_box_name,
            }
        },
    )
    walle_test.mock_project({"id": project_id})

    result = walle_test.api_client.get(f"/v1/projects/{project_id}?fields=boxes")

    assert result.status_code == http.client.OK
    assert result.json["boxes"][boxes.BoxType.dns] == dns_box_name
    assert result.json["boxes"][boxes.BoxType.eine] == eine_box_name
