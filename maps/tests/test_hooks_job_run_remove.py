import itertools
import pytest

from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy, Torrent
from maps.infra.ecstatic.common.experimental_worker.lib.hook_controller import HookFailedError
from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJob, HooksJobQueue
from maps.infra.ecstatic.common.experimental_worker.lib.ymtorrent_job import YmtorrentJobQueue, YmtorrentJobAction
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


@pytest.mark.asyncio
async def test_remove_success(hooks_job: HooksJob, data_storage: DataStorageProxy):
    ds = Dataset('ds1', '1')
    data_storage.add_torrent(Torrent('hash', b'content'), {ds})
    await hooks_job._run_remove(HooksJobQueue.RemoveHook(ds))
    assert hooks_job._hook_controller.trigger_remove.call_count == 1
    assert len(hooks_job._ymtorrent_queue) == 1
    request: YmtorrentJobQueue.PurgeTorrent = hooks_job._ymtorrent_queue.get()[0]
    assert request.type == YmtorrentJobAction.PURGE_TORRENT
    assert request.torrent_name == data_storage._storages[0].torrent_name('hash')
    assert ds not in data_storage._storages[0]._datasets_states
    assert 'hash' not in data_storage.hash_to_datasets_map
    assert ds not in itertools.chain.from_iterable(data_storage.hash_to_datasets_map.values())


@pytest.mark.asyncio
async def test_remove_failed(hooks_job: HooksJob, data_storage: DataStorageProxy):
    hooks_job._hook_controller.trigger_remove.side_effect = HookFailedError('error')
    ds = Dataset('ds1', '1')
    data_storage.add_torrent(Torrent('hash', b'content'), {ds})
    await hooks_job._run_remove(HooksJobQueue.RemoveHook(ds))
    assert hooks_job._hook_controller.trigger_remove.call_count == 1
    assert len(hooks_job._ymtorrent_queue) == 1
    request: YmtorrentJobQueue.PurgeTorrent = hooks_job._ymtorrent_queue.get()[0]
    assert request.type == YmtorrentJobAction.PURGE_TORRENT
    assert request.torrent_name == data_storage._storages[0].torrent_name('hash')
    assert ds not in data_storage._storages[0]._datasets_states
    assert 'hash' not in data_storage.hash_to_datasets_map
    assert ds not in itertools.chain.from_iterable(data_storage.hash_to_datasets_map.values())


@pytest.mark.asyncio
async def test_do_not_remove_from_ymtorrent(hooks_job: HooksJob, data_storage: DataStorageProxy):
    ds = Dataset('ds1', '1')
    ds1 = Dataset('ds1', '2')
    data_storage.add_torrent(Torrent('hash', b'content'), {ds, ds1})
    await hooks_job._run_remove(HooksJobQueue.RemoveHook(ds))
    assert hooks_job._hook_controller.trigger_remove.call_count == 1
    assert len(hooks_job._ymtorrent_queue) == 0
    assert ds not in data_storage._storages[0]._datasets_states
    assert ds1 in data_storage._storages[0]._datasets_states
    assert 'hash' in data_storage.hash_to_datasets_map
    assert ds1 in itertools.chain.from_iterable(data_storage.hash_to_datasets_map.values())
