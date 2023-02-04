import pytest

from infra.walle.server.tests.lib.util import (
    monkeypatch_inventory_get_host_info_and_check_status,
    monkeypatch_network_get_current_host_switch_port,
    ObjectMocker,
)
from walle import restrictions
from walle.clients import bot, abc
from walle.constants import BotHostStatus
from walle.errors import BadRequestError
from walle.expert.types import CheckType
from walle.hosts import HostType, HostState, HostStatus
from walle.maintenance_plot.model import MaintenancePlotModel
from walle.projects import (
    Project,
    DEFAULT_CMS_NAME,
    get_default_shadow_project_automation_limits,
    get_default_host_limits,
)
from walle.shadow_hosts import (
    get_or_create_shadow_project,
    ShadowProjectInfo,
    get_or_create_maintenance_plot,
    ShadowMaintenancePlotInfo,
    get_or_create_project_with_maintenance_plot,
    SHADOW_PREFIX,
    BotHostInfo,
    get_or_create_shadow_host,
    cleanup_empty_projects_and_unlinked_maintenance_plots,
)

ID = "test-shadow-id"
NAME = "test shadow name"
REASON = "test reason"
BOT_PROJECT_ID = 123321
SLUG = "test-slug"


@pytest.fixture
def shadow_projects(walle_test):
    mocker = ObjectMocker(
        Project,
        {
            "cms": DEFAULT_CMS_NAME,
            "cms_max_busy_hosts": 0,
            "cms_settings": [
                {
                    "cms": DEFAULT_CMS_NAME,
                    "cms_max_busy_hosts": 0,
                    "temporary_unreachable_enabled": False,
                }
            ],
            "healing_automation": {"enabled": False},
            "dns_automation": {"enabled": False},
            "automation_limits": get_default_shadow_project_automation_limits(),
            "host_limits": get_default_host_limits(),
            "validate_bot_project_id": True,
            "type": HostType.SHADOW_SERVER,
            "manually_disabled_checks": [
                CheckType.SSH,
                CheckType.UNREACHABLE,
                CheckType.NETMON,
                CheckType.WALLE_RACK,
                CheckType.META,
            ],
            "default_host_restrictions": [restrictions.AUTOMATION, restrictions.REBOOT],
            "tags": [HostType.SHADOW_SERVER],
        },
    )

    mocker.objects.extend(walle_test.projects.objects)

    return mocker


def test_create_shadow_project(shadow_projects, mp):
    mp.function(bot.is_valid_oebs_project, return_value=True)

    project_info = ShadowProjectInfo(id=ID, name=NAME, reason=REASON, bot_project_id=BOT_PROJECT_ID)

    get_or_create_shadow_project(project_info)

    shadow_projects.mock(
        dict(id=project_info.id, name=project_info.name, bot_project_id=project_info.bot_project_id), save=False
    )
    shadow_projects.assert_equal()


def test_fail_on_non_valid_bot_project_id(shadow_projects, mp):
    mp.function(bot.is_valid_oebs_project, return_value=False)
    project_info = ShadowProjectInfo(id=ID, name=NAME, reason=REASON, bot_project_id=BOT_PROJECT_ID)

    with pytest.raises(BadRequestError):
        get_or_create_shadow_project(project_info)

    shadow_projects.assert_equal()


def test_create_maintenance_plot(walle_test, mp):
    plot_info = ShadowMaintenancePlotInfo(id=ID, abc_service_slug=SLUG, name=NAME, reason=REASON)

    walle_test.maintenance_plots.mock(
        dict(
            id=ID,
            meta_info=dict(abc_service_slug=SLUG, name=NAME),
            common_settings=dict(
                maintenance_approvers=dict(
                    logins=[],
                    abc_roles_codes=[abc.Role.PRODUCT_HEAD],
                    abc_role_scope_slugs=[],
                    abc_duty_schedule_slugs=[],
                ),
                common_scenario_settings=dict(
                    total_number_of_active_hosts=0,
                ),
            ),
        ),
        save=False,
    )

    plot = get_or_create_maintenance_plot(plot_info)

    assert (
        plot.id == plot_info.id
        and plot.meta_info["name"] == plot_info.name
        and plot.meta_info["abc_service_slug"] == plot_info.abc_service_slug
        and all(
            settings["settings"]["enable_manual_approval_after_hosts_power_off"] for settings in plot.scenarios_settings
        )
    )


def test_get_or_create_project_with_maintenance_plot(walle_test, mp, shadow_projects):
    ABC_ID = 0
    planner_id = "0"
    bot_project_id = "1"
    pl_ids_to_bot_ids = {planner_id: bot_project_id}
    reason = "test"

    mp.function(bot.is_valid_oebs_project, return_value=True)
    mp.function(
        abc.get_service_by_id,
        return_value={
            "id": ABC_ID,
            "slug": SLUG,
        },
    )

    plot = walle_test.maintenance_plots.mock(
        dict(id=SHADOW_PREFIX.format(ABC_ID), meta_info=dict(abc_service_slug=SLUG, name=SHADOW_PREFIX.format(SLUG))),
        save=False,
    )
    shadow_projects.mock(
        dict(
            id=SHADOW_PREFIX.format(SLUG),
            name=SHADOW_PREFIX.format(SLUG),
            bot_project_id=bot_project_id,
            maintenance_plot_id=plot.id,
        ),
        save=False,
    )

    get_or_create_project_with_maintenance_plot(planner_id, pl_ids_to_bot_ids, reason)

    shadow_projects.assert_equal()


def test_get_or_create_shadow_host(walle_test, mp, shadow_projects):
    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=BOT_PROJECT_ID)
    monkeypatch_network_get_current_host_switch_port(mp)
    mp.function(bot.missed_preordered_hosts, return_value={})

    project = shadow_projects.mock(dict(id=ID, name=SLUG, bot_project_id=BOT_PROJECT_ID, type=HostType.SHADOW_SERVER))

    host_info = BotHostInfo(inv=1, oebs_status=BotHostStatus.OPERATION, planner_id="0", name="mock-name")

    host = get_or_create_shadow_host(host_info, project.id, reason="test")

    assert (
        host.inv == 1
        and host.state == HostState.FREE
        and host.status == HostStatus.READY
        and host.type == HostType.SHADOW_SERVER
    )


def test_cleanup_empty_projects_and_unlinked_maintenance_plots(walle_test, mp, shadow_projects):
    plot_with_link_1 = walle_test.maintenance_plots.mock(
        dict(id="test-1", meta_info=dict(abc_service_slug=SLUG, name=SLUG), gc_enabled=True)
    )
    plot_with_link_2 = walle_test.maintenance_plots.mock(
        dict(id="test-2", meta_info=dict(abc_service_slug=SLUG, name=SLUG), gc_enabled=True)
    )
    walle_test.maintenance_plots.mock(
        dict(id="test-3", meta_info=dict(abc_service_slug=SLUG, name=SLUG), gc_enabled=True)
    )

    project_without_hosts = shadow_projects.mock(
        dict(id="project-2", name="test-2", maintenance_plot_id=plot_with_link_2.id)
    )
    project_with_hosts = shadow_projects.mock(
        dict(id="project-1", name="test-1", maintenance_plot_id=plot_with_link_1.id)
    )

    walle_test.mock_host({"type": HostType.SHADOW_SERVER, "inv": 1, "project": project_with_hosts.id})

    cleanup_empty_projects_and_unlinked_maintenance_plots(reason="test")

    shadow_projects.remove(project_without_hosts)

    assert (
        MaintenancePlotModel.objects().count() == 1
        and MaintenancePlotModel.objects(id=plot_with_link_1.id).count() == 1
    )
    shadow_projects.assert_equal()
