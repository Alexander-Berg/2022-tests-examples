from unittest import mock

from maps.garden.sdk.module_traits import module_traits as mt
from maps.garden.libs_server.module import storage_interface
from maps.garden.scheduler.lib.remote_module_syncer import RemoteModuleSyncer


def test_remote_module_sync():
    module_manager = mock.Mock()
    module_manager.get_all_module_names.return_value = ["test1", "test2"]
    module_manager.get_version_registration_infos.side_effect = [
        (
            storage_interface.ModuleVersionInfo(
                module_version="1",
                remote_path="//tmp/some_path",
                sandbox_task_id="1",
                module_traits=mt.ModuleTraits(
                    name="test1",
                    type=mt.ModuleType.MAP,
                ),
            ),
        ),
        (
            storage_interface.ModuleVersionInfo(
                module_version="2",
                remote_path="//tmp/some_other_path",
                sandbox_task_id="2",
                module_traits=mt.ModuleTraits(
                    name="test2",
                    type=mt.ModuleType.MAP,
                ),
            ),
        ),
    ]

    yt_client = mock.Mock()
    yt_client.exists.side_effect = [
        True,  # remote file for module "test1" exists
        False  # remote file for module "test1" does not exist
    ]

    with mock.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager):
        syncer = RemoteModuleSyncer(server_settings={}, delay_executor=None)
        syncer._sync(yt_client)
        module_manager.ensure_remote_existence.assert_called_once_with(
            module_name="test2",
            module_version="2",
        )
