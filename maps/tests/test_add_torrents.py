import os
import time
import pytest
from dataclasses import dataclass, field

from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_polling_job import CoordinatorPollingJob
from maps.infra.ecstatic.common.experimental_worker.lib.ymtorrent_job import YmtorrentJobAction
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset, Coordinator


@dataclass
class IncrementalAddTorrentCase:
    name: str
    datasets_to_add: dict[str, set[Dataset]]
    initial_datasets: dict[str, set[Dataset]] = field(default_factory=dict)

    def __str__(self):
        return self.name


CASES = [
    IncrementalAddTorrentCase(
        name='add_new_dataset',
        datasets_to_add={'hash': {Dataset('ds1', '1')}}
    ),
    IncrementalAddTorrentCase(
        name='add_torrent_with_2_datasets',
        datasets_to_add={'hash': {
            Dataset('ds1', '1'),
            Dataset('ds2', '1')
        }}
    ),
    IncrementalAddTorrentCase(
        name='add_2_torrents',
        datasets_to_add={
            'hash': {Dataset('ds1', '1')},
            'hash1': {Dataset('ds2', '1')}
        }
    ),
    IncrementalAddTorrentCase(
        name='add_torrent_with_state',
        datasets_to_add={'hash': {
            Dataset('ds1', '1'),
        }},
        initial_datasets={'hash1': {Dataset('ds2', '1')}}
    ),
    IncrementalAddTorrentCase(
        name='append_ds_to_existing_torrent',
        datasets_to_add={'hash': {
            Dataset('ds1', '1'),
        }},
        initial_datasets={'hash': {Dataset('ds2', '1')}}
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_incremental_add_torrent(coordinator_polling_job: CoordinatorPollingJob,
                                       torrents_collection_factory,
                                       coordinator: Coordinator,
                                       case: IncrementalAddTorrentCase):
    if case.initial_datasets:
        coordinator.host_torrents.return_value = torrents_collection_factory(case.initial_datasets)
        await coordinator_polling_job._update_torrents()
        assert len(coordinator_polling_job._ymtorrent_queue) == 1
        coordinator_polling_job._ymtorrent_queue.get()

    coordinator_polling_job._last_sync = time.time()
    coordinator.host_torrents.return_value = torrents_collection_factory(case.datasets_to_add)
    await coordinator_polling_job._update_torrents()
    new_hashes = set(case.datasets_to_add.keys()) - set(case.initial_datasets.keys())
    for ymtorrent_req in coordinator_polling_job._ymtorrent_queue.get():
        assert ymtorrent_req.type == YmtorrentJobAction.INCREMENTAL_ADD_TORRENT
        assert ymtorrent_req.torrent.content == b'torrent_file'
        assert ymtorrent_req.torrent.priority == 1
        assert ymtorrent_req.torrent.hash in new_hashes
        new_hashes.remove(ymtorrent_req.torrent.hash)

    storage_path = coordinator_polling_job._data_storage._storages[0]._path
    all_content_hashes = {os.path.basename(path) for path in os.listdir(storage_path.content)}
    all_torrents_hashes = {os.path.basename(path[:-len('.pb')])
                           for path in os.listdir(storage_path.torrents)
                           if path.endswith('.pb')}
    all_data_hashes = {os.path.basename(path[:-len('.data')])
                       for path in os.listdir(storage_path.torrents)
                       if path.endswith('.data')}
    all_test_hashes = set(case.datasets_to_add.keys()) | set(case.initial_datasets.keys())
    assert all_test_hashes == all_torrents_hashes
    assert all_test_hashes == all_data_hashes
    assert all_test_hashes == all_content_hashes
    for hash in all_test_hashes:
        if hash in case.initial_datasets:
            for ds in case.initial_datasets[hash]:
                assert os.path.exists(os.path.join(storage_path.versions, f'{ds.name}_{ds.version}'))

        if hash in case.datasets_to_add:
            for ds in case.datasets_to_add[hash]:
                assert os.path.exists(os.path.join(storage_path.versions, f'{ds.name}_{ds.version}'))
