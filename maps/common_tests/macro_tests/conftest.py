import pytest
import flask

from maps.garden.libs_server.test_utils.task_handler import UnittestTaskHandler
from maps.garden.libs_server.test_utils.common import server_root_path

from maps.garden.libs_server.autostart import subprocess_autostart_manager as am
from maps.garden.libs_server.build import build_utils
from maps.garden.scheduler.lib import run_scheduler
from maps.garden.server.lib import run_garden_server

from . import module_manager


def _emulate_autostarter(
    trigger_build,
    target_module,
    target_contour_name,
    related_builds,
    attempt,
):
    source_ids = [trigger_build.full_id]

    for config in target_module.traits.configs:
        config_build = max(related_builds, key=lambda b: (b.name == config, b.id))
        if config_build.name == config:
            source_ids.append(config_build.full_id)

    return build_utils.create_new_build(source_ids, {}), None


@pytest.fixture
def garden_client(
    db,
    prepare_server,
    patched_server_config,
    patched_environment_settings,
    patched_regions,
    mocker,
):
    mocker.patch.object(am.SubprocessAutostartManager, "_call_module_via_proto", side_effect=_emulate_autostarter)

    test_module_manager = module_manager.TestModuleManager(db)
    mocker.patch(
        "maps.garden.libs_server.module.module_manager.ModuleManager",
        return_value=test_module_manager)
    mocker.patch(
        "maps.garden.scheduler.lib.graph_manager.get_module_graph",
        side_effect=module_manager.get_module_graph)

    mocker.patch("maps.garden.scheduler.lib.load_state.YtHandlerAdaptor", UnittestTaskHandler)
    mocker.patch("maps.garden.scheduler.lib.builds_scheduler.POLLING_INTERVAL_SEC", 0.01)
    mocker.patch("maps.garden.scheduler.lib.module_builds_cleaner.POLLING_INTERVAL_SEC", 1)

    app_client = flask.Flask("server", root_path=server_root_path())
    app_client.testing = True
    app_scheduler = flask.Flask("scheduler")
    app_scheduler.testing = True

    run_garden_server.prepare_app(patched_server_config, app_client)
    with run_scheduler.prepare_app(patched_server_config, app_scheduler), \
         app_client.test_client() as client:
        yield client
