import pytest
import shutil
import os
import yatest
import psutil
from maps.infra.ecstatic.common.experimental_worker.lib.hook_controller import HookController, HookFailedError
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset
from maps.pylibs.utils.lib.process import kill_process_tree


def prepare_env(tmp_path, data_storage: DataStorageProxy, hook_name: str, hook_type: str) -> HookController:
    conf_dir = tmp_path / 'conf'
    os.mkdir(conf_dir)
    os.mkdir(conf_dir / 'ds1')
    hook_path = conf_dir / 'ds1' / f'hook.{hook_type}'
    shutil.copyfile(yatest.common.test_source_path(f'data/hooks/{hook_name}'), hook_path)
    os.chmod(hook_path, 0o777)
    hook_controller = HookController(conf_dir, data_storage, {'postdl': 5, 'switch': 5, 'remove': 5})
    return hook_controller


@pytest.mark.asyncio
async def test_fail(data_storage: DataStorageProxy, tmp_path):
    hook_controller = prepare_env(tmp_path, data_storage, 'fail.sh', 'postdl')
    with pytest.raises(HookFailedError, match=r'.+exited with code 1.+'):
        await hook_controller.trigger_postdl(Dataset('ds1', '1'))


@pytest.mark.asyncio
async def test_timeout(data_storage: DataStorageProxy, tmp_path, monkeypatch):
    def kill_process(pid, sig, sudo=False):
        kill_process_tree(pid, sig, sudo=False)
        # we cannot use sudo on dist
    monkeypatch.setattr('maps.infra.ecstatic.common.experimental_worker.lib.hook_controller.kill_process_tree',
                        kill_process)

    hook_controller = prepare_env(tmp_path, data_storage, 'sleep.sh', 'postdl')
    with pytest.raises(HookFailedError, match='Timeout'):
        await hook_controller.trigger_postdl(Dataset('ds1', '1'))

    for proc in psutil.process_iter(['name']):
        assert 'hook.postdl' not in proc.info['name'], 'Found not killed hook'


@pytest.mark.asyncio
@pytest.mark.parametrize('hook_type', ['switch', 'postdl', 'remove'])
async def test_hook_params(data_storage: DataStorageProxy, tmp_path, hook_type: str):
    hook_controller = prepare_env(tmp_path, data_storage, 'dump_params.sh', hook_type)
    dataset = Dataset('ds1:1', '1')
    if hook_type == 'switch':
        await hook_controller.trigger_switch(dataset)
    elif hook_type == 'postdl':
        await hook_controller.trigger_postdl(dataset)
    elif hook_type == 'remove':
        await hook_controller.trigger_remove(dataset)
    else:
        assert False, 'Unknown hook type'

    with open(os.path.join(data_storage._storages[0]._path.root, 'hook_env')) as file:
        env_content = file.read()
    assert f'ECSTATIC_VERSIONED_DATA_PATH={data_storage.data_version_path(dataset)}' in env_content
    assert f'ECSTATIC_ACTIVATED_DATA_PATH={data_storage.data_path(dataset)}' in env_content
    assert f'ECSTATIC_DATA_PATH={data_storage.data_path(dataset)}' in env_content
    assert 'ECSTATIC_DATA_VERSION=1' in env_content
    assert 'ECSTATIC_DATA_TAG=1' in env_content
