import http.client as httplib

import pytest

from infra.walle.server.tests.lib.util import TestCase, monkeypatch_function
from tests.api.maintenance_plot_api.mocks import MOCK_MAINTENANCE_PLOT_OBJ
from walle.clients import abc, staff
from walle.scenario.constants import ScriptName


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.usefixtures("unauthenticated")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_meta_info_unauthenticated(test):
    plot = MOCK_MAINTENANCE_PLOT_OBJ
    result = test.api_client.put(
        "/v1/maintenance-plots/{}/meta_info".format(plot.id),
        data=plot.meta_info.to_dict(),
    )

    assert result.status_code == httplib.UNAUTHORIZED
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_meta_info_not_authorized(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    plot = test.maintenance_plots.mock({"id": "plot-id"})
    result = test.api_client.put(
        "/v1/maintenance-plots/{}/meta_info".format(plot.id), data={"abc_service_slug": "bbb", "name": "bbb"}
    )

    assert result.status_code == httplib.FORBIDDEN
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
@pytest.mark.parametrize(["abc_service_slug", "name"], [("aaa", "aaa"), ("aaa", "")])
def test_modify_meta_info_admin_allowed(test, abc_service_slug, name):
    plot = test.maintenance_plots.mock({"id": "plot-id", "meta_info": {"abc_service_slug": "bbb", "name": "bbb"}})

    request = dict(abc_service_slug=abc_service_slug, name=name)
    result = test.api_client.put("/v1/maintenance-plots/{}/meta_info".format(plot.id), data=request)

    assert result.status_code == httplib.OK

    plot.meta_info["abc_service_slug"] = abc_service_slug
    plot.meta_info["name"] = name

    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
@pytest.mark.parametrize(["abc_service_slug", "name"], [("aaa", "aaa"), ("aaa", "")])
def test_modify_meta_info_authorized_user_allowed(test, mp, abc_service_slug, name):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": TestCase.api_user},
            }
        ],
    )
    plot = test.maintenance_plots.mock({"id": "plot-id", "meta_info": {"abc_service_slug": "bbb", "name": "bbb"}})

    request = dict(abc_service_slug=abc_service_slug, name=name)
    result = test.api_client.put("/v1/maintenance-plots/{}/meta_info".format(plot.id), data=request)

    assert result.status_code == httplib.OK

    plot.meta_info["abc_service_slug"] = abc_service_slug
    plot.meta_info["name"] = name

    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("unauthenticated")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_common_settings_unauthenticated(test):
    plot = MOCK_MAINTENANCE_PLOT_OBJ
    result = test.api_client.put(
        "/v1/maintenance-plots/{}/common_settings".format(plot.id),
        data=plot.common_settings.to_dict(),
    )

    assert result.status_code == httplib.UNAUTHORIZED
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_common_settings_not_authorized(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    monkeypatch_function(mp, func=staff.check_logins, module=staff, return_value=["aaaaa", "bbbbb"])

    new_maintenance_approvers = {
        "logins": ["aaaaa", "bbbbb"],
        "abc_roles_codes": ["aa", "bb"],
        "abc_role_scope_slugs": ["aaa", "bbb"],
    }
    plot = test.maintenance_plots.mock({"id": "plot-id"})
    result = test.api_client.put(
        "/v1/maintenance-plots/{}/common_settings".format(plot.id),
        data=dict(maintenance_approvers=new_maintenance_approvers),
    )

    assert result.status_code == httplib.FORBIDDEN
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_common_settings_admin_allowed(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    monkeypatch_function(mp, func=staff.check_logins, module=staff, return_value=["aaaaa", "bbbbb"])

    new_maintenance_approvers = {
        "logins": ["aaaaa", "bbbbb"],
        "abc_roles_codes": ["aa", "bb"],
        "abc_role_scope_slugs": ["aaa", "bbb"],
        "abc_duty_schedule_slugs": ["aaaa", "bbbb"],
    }
    new_common_scenario_settings = {
        "total_number_of_active_hosts": 10,
        "dont_allow_start_scenario_if_total_number_of_active_hosts_more_than": 20,
    }
    plot = test.maintenance_plots.mock(
        {
            "id": "plot-id",
            "common_settings": {
                "maintenance_approvers": {
                    "logins": ["login-1-mock", "login-2-mock"],
                    "abc_roles_codes": ["abc-role-code-1-mock", "abc-role-code-2-mock"],
                    "abc_role_scope_slugs": ["abc-role-scope-slug-1-mock", "abc-role-scope-slug-2-mock"],
                }
            },
        }
    )

    request = dict(
        maintenance_approvers=new_maintenance_approvers, common_scenario_settings=new_common_scenario_settings
    )
    result = test.api_client.put("/v1/maintenance-plots/{}/common_settings".format(plot.id), data=request)

    assert result.status_code == httplib.OK

    plot.common_settings["maintenance_approvers"] = new_maintenance_approvers
    plot.common_settings["common_scenario_settings"] = new_common_scenario_settings

    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_common_settings_authorized_user_allowed(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": TestCase.api_user},
            }
        ],
    )
    monkeypatch_function(mp, func=staff.check_logins, module=staff, return_value=["aaaaa", "bbbbb"])

    new_maintenance_approvers = {
        "logins": ["aaaaa", "bbbbb"],
        "abc_roles_codes": ["aa", "bb"],
        "abc_role_scope_slugs": ["aaa", "bbb"],
        "abc_duty_schedule_slugs": ["aaaa", "bbbb"],
    }
    new_common_scenario_settings = {
        "total_number_of_active_hosts": 10,
        "dont_allow_start_scenario_if_total_number_of_active_hosts_more_than": 20,
    }
    plot = test.maintenance_plots.mock(
        {
            "id": "plot-id",
            "common_settings": {
                "maintenance_approvers": {
                    "logins": ["login-1-mock", "login-2-mock"],
                    "abc_roles_codes": ["abc-role-code-1-mock", "abc-role-code-2-mock"],
                    "abc_role_scope_slugs": ["abc-role-scope-slug-1-mock", "abc-role-scope-slug-2-mock"],
                }
            },
        }
    )

    request = dict(
        maintenance_approvers=new_maintenance_approvers, common_scenario_settings=new_common_scenario_settings
    )
    result = test.api_client.put("/v1/maintenance-plots/{}/common_settings".format(plot.id), data=request)

    assert result.status_code == httplib.OK

    plot.common_settings["maintenance_approvers"] = new_maintenance_approvers
    plot.common_settings["common_scenario_settings"] = new_common_scenario_settings

    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
@pytest.mark.parametrize(
    ["abc_result", "staff_result"],
    [
        ([{"person": {"login": TestCase.api_user}}], ["aaaaa"]),
        ([], ["aaaaa", "bbbbb"]),
    ],
)
def test_modify_common_settings_with_validation_error_from_outer_system(test, mp, abc_result, staff_result):
    monkeypatch_function(mp, func=abc.get_service_members, module=abc, return_value=abc_result)
    monkeypatch_function(mp, func=staff.check_logins, module=staff, return_value=staff_result)

    new_maintenance_approvers = {
        "logins": ["aaaaa", "bbbbb"],
        "abc_roles_codes": ["aa", "bb"],
        "abc_role_scope_slugs": ["aaa", "bbb"],
    }
    plot = test.maintenance_plots.mock(
        {
            "id": "plot-id",
            "common_settings": {
                "maintenance_approvers": {
                    "logins": ["login-1-mock", "login-2-mock"],
                    "abc_roles_codes": ["abc-role-code-1-mock", "abc-role-code-2-mock"],
                    "abc_role_scope_slugs": ["abc-role-scope-slug-1-mock", "abc-role-scope-slug-2-mock"],
                }
            },
        }
    )

    request = dict(maintenance_approvers=new_maintenance_approvers)
    result = test.api_client.put("/v1/maintenance-plots/{}/common_settings".format(plot.id), data=request)

    assert result.status_code == httplib.BAD_REQUEST

    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("unauthenticated")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_scenarios_settings_unauthenticated(test):
    plot = MOCK_MAINTENANCE_PLOT_OBJ
    result = test.api_client.put(
        "/v1/maintenance-plots/{}/scenarios_settings".format(plot.id),
        data={"scenarios_settings": [s.to_dict() for s in plot.scenarios_settings]},
    )

    assert result.status_code == httplib.UNAUTHORIZED
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_scenarios_settings_not_authorized(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    new_scenarios_settings = {
        "scenarios_settings": [
            {
                "scenario_type": ScriptName.NOC_HARD,
                "settings": {
                    "request_cms_x_seconds_before_maintenance_start_time": 86 * 60 * 60,
                },
            },
        ]
    }
    plot = test.maintenance_plots.mock({"id": "plot-id"})
    result = test.api_client.put(
        "/v1/maintenance-plots/{}/scenarios_settings".format(plot.id), data=new_scenarios_settings
    )

    assert result.status_code == httplib.FORBIDDEN
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_scenarios_settings_admin_allowed(test):
    new_scenarios_settings = {
        "scenarios_settings": [
            {
                "scenario_type": ScriptName.NOC_HARD,
                "settings": {
                    "enable_redeploy_after_change_of_mac_address": False,
                    "get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time": False,
                    "ignore_cms_on_host_operations": False,
                    "request_cms_x_seconds_before_maintenance_start_time": 86,
                    "get_approvers_to_ticket_if_hosts_not_in_maintenance_by_x_seconds": 50,
                    "use_yp_sla": False,
                    "approval_sla": 90,
                    "enable_manual_approval_after_hosts_power_off": False,
                },
            },
        ]
    }
    plot = test.maintenance_plots.mock({"id": "plot-id"})

    result = test.api_client.put(
        "/v1/maintenance-plots/{}/scenarios_settings".format(plot.id), data=new_scenarios_settings
    )

    assert result.status_code == httplib.OK

    plot.scenarios_settings = new_scenarios_settings["scenarios_settings"]

    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_scenarios_settings_authorized_user_allowed(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": TestCase.api_user},
            }
        ],
    )
    new_scenarios_settings = {
        "scenarios_settings": [
            {
                "scenario_type": ScriptName.NOC_HARD,
                "settings": {
                    "enable_redeploy_after_change_of_mac_address": False,
                    "get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time": False,
                    "ignore_cms_on_host_operations": False,
                    "request_cms_x_seconds_before_maintenance_start_time": 86,
                    "get_approvers_to_ticket_if_hosts_not_in_maintenance_by_x_seconds": 50,
                    "use_yp_sla": False,
                    "approval_sla": 90,
                    "enable_manual_approval_after_hosts_power_off": False,
                },
            },
        ]
    }
    plot = test.maintenance_plots.mock({"id": "plot-id"})

    result = test.api_client.put(
        "/v1/maintenance-plots/{}/scenarios_settings".format(plot.id), data=new_scenarios_settings
    )

    assert result.status_code == httplib.OK

    plot.scenarios_settings = new_scenarios_settings["scenarios_settings"]

    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_modify_scenarios_settings_with_gc_enabled(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": TestCase.api_user},
            }
        ],
    )
    new_scenarios_settings = {
        "scenarios_settings": [
            {
                "scenario_type": ScriptName.NOC_HARD,
                "settings": {
                    "enable_redeploy_after_change_of_mac_address": False,
                    "get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time": False,
                    "ignore_cms_on_host_operations": False,
                    "request_cms_x_seconds_before_maintenance_start_time": 86,
                    "get_approvers_to_ticket_if_hosts_not_in_maintenance_by_x_seconds": 50,
                    "use_yp_sla": False,
                    "approval_sla": 90,
                    "enable_manual_approval_after_hosts_power_off": False,
                },
            },
        ]
    }
    plot = test.maintenance_plots.mock({"id": "plot-id", "gc_enabled": True})

    result = test.api_client.put(
        "/v1/maintenance-plots/{}/scenarios_settings".format(plot.id), data=new_scenarios_settings
    )

    assert result.status_code == httplib.BAD_REQUEST

    test.maintenance_plots.assert_equal()
