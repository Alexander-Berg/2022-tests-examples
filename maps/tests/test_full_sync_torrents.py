import os
import pytest
import itertools
from dataclasses import dataclass, field

from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_polling_job import CoordinatorPollingJob
from maps.infra.ecstatic.common.experimental_worker.lib.ymtorrent_job import YmtorrentJobAction
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset, Coordinator
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy, DatasetState


@dataclass
class FullSyncTestCase:
    name: str
    datasets_to_add: dict[str, set[Dataset]]
    restricted_to_remove_datasets: set[Dataset] = field(default_factory=set)
    initial_datasets: dict[str, set[Dataset]] = field(default_factory=dict)
    active_datasets: set[Dataset] = field(default_factory=set)

    def __str__(self):
        return self.name


CASES = [
    FullSyncTestCase(
        name='simple_add',
        datasets_to_add={'hash': {Dataset('ds', '1')}}
    ),
    FullSyncTestCase(
        name='remove',
        initial_datasets={'hash': {Dataset('ds', '1')}},
        datasets_to_add={}
    ),
    FullSyncTestCase(
        name='restricted_to_remove',
        initial_datasets={'hash': {Dataset('ds', '1')}},
        datasets_to_add={},
        restricted_to_remove_datasets={Dataset('ds', '1')}
    ),
    FullSyncTestCase(
        name='do_not_remove_active_version',
        initial_datasets={'hash': {Dataset('ds', '1')}},
        datasets_to_add={},
        active_datasets={Dataset('ds', '1')},
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_full_sync_torrents(coordinator_polling_job: CoordinatorPollingJob,
                                  torrents_collection_factory,
                                  data_storage: DataStorageProxy,
                                  coordinator: Coordinator,
                                  case: FullSyncTestCase):
    if case.initial_datasets:
        coordinator.host_torrents.return_value = torrents_collection_factory(case.initial_datasets)
        await coordinator_polling_job._update_torrents()
        assert len(coordinator_polling_job._ymtorrent_queue) == 1
        coordinator_polling_job._ymtorrent_queue.get()
    hashes_to_save = set()
    for ds in case.restricted_to_remove_datasets:
        for hash, datasets in itertools.chain(case.initial_datasets.items(), case.datasets_to_add.items()):
            for dataset in datasets:
                if dataset == ds:
                    hashes_to_save.add(hash)

    for dataset in case.restricted_to_remove_datasets:
        data_storage._storages[0]._datasets_states[dataset] = DatasetState.POSTDL_DONE

    for dataset in case.active_datasets:
        data_storage.set_active_version(dataset)

    coordinator_polling_job._last_sync = 0
    coordinator.host_torrents.return_value = torrents_collection_factory(case.datasets_to_add)
    await coordinator_polling_job._update_torrents()
    assert len(coordinator_polling_job._ymtorrent_queue) == 1
    ymtorrent_request = coordinator_polling_job._ymtorrent_queue.get()[0]
    assert ymtorrent_request.type == YmtorrentJobAction.FULL_SYNC_TORRENTS

    storage_path = coordinator_polling_job._data_storage._storages[0]._path
    all_content_hashes = {os.path.basename(path) for path in os.listdir(storage_path.content)}
    all_torrents_hashes = {os.path.basename(path[:-len('.pb')])
                           for path in os.listdir(storage_path.torrents)
                           if path.endswith('.pb')}
    all_data_hashes = {os.path.basename(path[:-len('.data')])
                       for path in os.listdir(storage_path.torrents)
                       if path.endswith('.data')}
    ymtorrent_hashes = {torrent.hash for torrent in ymtorrent_request.torrents}
    all_test_hashes = set(case.datasets_to_add.keys()) | hashes_to_save
    assert all_test_hashes == all_torrents_hashes
    assert all_test_hashes == all_data_hashes
    assert all_test_hashes == all_content_hashes
    assert all_test_hashes == ymtorrent_hashes
    assert set(data_storage.hash_to_datasets_map.keys()) == all_test_hashes
    assert set(
        itertools.chain.from_iterable(data_storage.hash_to_datasets_map.values())
    ) == set(data_storage._storages[0]._datasets_states.keys())
    assert set(data_storage._storages[0]._dataset_to_hash_map.values()) == all_test_hashes
    assert set(data_storage._storages[0]._dataset_to_hash_map.keys()) == set(
        data_storage._storages[0]._datasets_states.keys()
    )

    assert coordinator_polling_job._hooks_queue.qsize() == len(hashes_to_save)
    called_hooks = set()
    while not coordinator_polling_job._hooks_queue.empty():
        hook = coordinator_polling_job._hooks_queue.get_nowait()
        assert hook.dataset not in case.active_datasets
        called_hooks.add(data_storage.torrent_hash(hook.dataset))

    assert called_hooks == hashes_to_save
