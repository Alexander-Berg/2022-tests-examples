from infra.walle.server.tests.lib.util import (
    monkeypatch_inventory_get_host_info_and_check_status,
    monkeypatch_network_get_current_host_switch_port,
    monkeypatch_function,
)
from walle.clients import bot, abc
from walle.cron.shadow_hosts_sync import _sync_shadow_hosts
from walle.hosts import HostType, Host
from walle.maintenance_plot.model import MaintenancePlotModel
from walle.projects import Project


def monkeypatch_abc_service(mp, start_id=1):
    def _get_abc_data(*args, **kwargs):
        nonlocal start_id
        cur_id = start_id
        start_id += 1
        return {"id": cur_id, "slug": str(cur_id)}

    monkeypatch_function(mp, abc.get_service_by_id, module=abc, side_effect=_get_abc_data)


def test_sync_creates_projects_and_plots_and_add_hosts(walle_test, mp):
    mp.function(bot.is_valid_oebs_project, return_value=True)
    monkeypatch_abc_service(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=1, incr_bot_project_id_on_call=True)
    monkeypatch_network_get_current_host_switch_port(mp)
    mp.function(bot.missed_preordered_hosts, return_value={})
    mp.function(bot.get_oebs_projects, return_value={1: {"planner_id": 1}, 2: {"planner_id": 2}, 3: {"planner_id": 3}})
    mp.function(
        bot.iter_hosts_info,
        return_value=[
            {"inv": 1, "name": "mocked-1.mock", "oebs_status": "unknown", "planner_id": 1},
            {"inv": 2, "name": "mocked-2.mock", "oebs_status": "unknown", "planner_id": 2},
            {"inv": 3, "name": "mocked-3.mock", "oebs_status": "unknown", "planner_id": 3},
        ],
    )

    assert Project.objects().count() == 1  # There exists default project with id mocked-default-project
    assert MaintenancePlotModel.objects().count() == 0
    assert Host.objects().count() == 0

    _sync_shadow_hosts()

    for host in Host.objects():
        assert Project.objects(id=host.project).count() == 1
        project = Project.objects().get(id=host.project)
        assert MaintenancePlotModel.objects(id=project.maintenance_plot_id)
    assert Project.objects().count() == 4
    assert MaintenancePlotModel.objects().count() == 3
    assert Host.objects().count() == 3


def test_sync_add_hosts_to_existing_project(walle_test, mp):
    mp.function(bot.is_valid_oebs_project, return_value=True)
    monkeypatch_abc_service(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=1)
    monkeypatch_network_get_current_host_switch_port(mp)
    mp.function(bot.missed_preordered_hosts, return_value={})
    mp.function(bot.get_oebs_projects, return_value={1: {"planner_id": 1}})
    mp.function(
        bot.iter_hosts_info,
        return_value=[{"inv": 1, "name": "mocked-1.mock", "oebs_status": "unknown", "planner_id": 1}],
    )

    walle_test.mock_project({"id": "shadow-1", "bot_project_id": 1, "type": HostType.SHADOW_SERVER})

    assert Project.objects().count() == 2  # There exists default project with id mocked-default-project
    assert MaintenancePlotModel.objects().count() == 0
    assert Host.objects().count() == 0

    _sync_shadow_hosts()

    for host in Host.objects():
        assert Project.objects(id=host.project).count() == 1
        project = Project.objects().get(id=host.project)
        assert MaintenancePlotModel.objects(id=project.maintenance_plot_id)
    assert Project.objects().count() == 2  # There exists default project with id mocked-default-project
    assert MaintenancePlotModel.objects().count() == 1
    assert Host.objects().count() == 1


def test_sync_swap_hosts_between_projects(walle_test, mp):
    mp.function(bot.is_valid_oebs_project, return_value=True)
    monkeypatch_abc_service(mp, start_id=2)
    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=2)
    monkeypatch_network_get_current_host_switch_port(mp)
    mp.function(bot.missed_preordered_hosts, return_value={})
    mp.function(bot.get_oebs_projects, return_value={1: {"planner_id": 1}, 2: {"planner_id": 2}})
    mp.function(
        bot.iter_hosts_info,
        return_value=[{"inv": 1, "name": "mocked-1.mock", "oebs_status": "unknown", "planner_id": 2}],
    )

    old_project = walle_test.mock_project({"id": "shadow-1", "bot_project_id": 1, "type": HostType.SHADOW_SERVER})
    host = walle_test.mock_host(
        {"inv": 1, "name": "mocked-1.mock", "type": HostType.SHADOW_SERVER, "project": old_project.id}
    )
    new_project = walle_test.mock_project({"id": "shadow-2", "bot_project_id": 2, "type": HostType.SHADOW_SERVER})

    assert Project.objects().count() == 3  # There exists default project with id mocked-default-project
    assert Host.objects().count() == 1

    _sync_shadow_hosts()

    assert Project.objects().count() == 2  # There exists default project with id mocked-default-project
    assert Host.objects().count() == 1
    updated_host = Host.get_by_inv(host.inv)
    assert updated_host.project == new_project.id


def test_sync_remove_invalid_hosts(walle_test, mp):
    mp.function(bot.is_valid_oebs_project, return_value=True)
    monkeypatch_abc_service(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=1)
    monkeypatch_network_get_current_host_switch_port(mp)
    mp.function(bot.missed_preordered_hosts, return_value={})
    mp.function(bot.get_oebs_projects, return_value={1: {"planner_id": 1}})
    mp.function(bot.iter_hosts_info, return_value=[])

    project = walle_test.mock_project({"id": "shadow-1", "bot_project_id": 1, "type": HostType.SHADOW_SERVER})
    walle_test.mock_host({"inv": 1, "type": HostType.SHADOW_SERVER, "project": project.id})

    assert Project.objects().count() == 2  # There exists default project with id mocked-default-project
    assert MaintenancePlotModel.objects().count() == 0
    assert Host.objects().count() == 1

    _sync_shadow_hosts()

    assert Project.objects().count() == 1  # There exists default project with id mocked-default-project
    assert MaintenancePlotModel.objects().count() == 0
    assert Host.objects().count() == 0


def test_sync_rename_hosts(walle_test, mp):
    mp.function(bot.is_valid_oebs_project, return_value=True)
    monkeypatch_abc_service(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp, bot_project_id=1)
    monkeypatch_network_get_current_host_switch_port(mp)
    mp.function(bot.missed_preordered_hosts, return_value={})
    mp.function(bot.get_oebs_projects, return_value={1: {"planner_id": 1}})
    mp.function(
        bot.iter_hosts_info,
        return_value=[{"inv": 1, "name": "mocked-2.mock", "oebs_status": "unknown", "planner_id": 1}],
    )

    project = walle_test.mock_project({"id": "shadow-1", "bot_project_id": 1, "type": HostType.SHADOW_SERVER})
    host = walle_test.mock_host(
        {"inv": 1, "name": "mocked-1.mock", "type": HostType.SHADOW_SERVER, "project": project.id}
    )

    assert Project.objects().count() == 2  # There exists default project with id mocked-default-project
    assert Host.objects().count() == 1

    _sync_shadow_hosts()

    assert Project.objects().count() == 2  # There exists default project with id mocked-default-project
    assert Host.objects().count() == 1
    updated_host = Host.get_by_inv(host.inv)
    assert updated_host.name == "mocked-2.mock"
