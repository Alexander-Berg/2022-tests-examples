import os
import pytest
import itertools
from dataclasses import dataclass, field
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset
from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_polling_job import CoordinatorPollingJob
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy, Torrent


@dataclass
class CleanupTestCase:
    name: str
    datasets: dict[str, set[Dataset]] = field(default_factory=dict)
    restricted_to_remove: set[Dataset] = field(default_factory=set)

    removed_suffixes: dict[Dataset, set[str]] = field(default_factory=dict)  # Dataset -> /versions/ds_1 or /torrents/hash.pb
    extra_files: set[str] = field(default_factory=set)

    def __str__(self):
        return self.name


CASES = [
    CleanupTestCase(
        name='broken_torrents',
        datasets={'hash': {Dataset('ds1', '1')}},
        removed_suffixes={Dataset('ds1', '1'): {'torrents/hash.pb'}}
    ),
    CleanupTestCase(
        name='broken_torrents_need_remove',
        datasets={'hash': {Dataset('ds1', '1')}},
        restricted_to_remove={Dataset('ds1', '1')},
        removed_suffixes={Dataset('ds1', '1'): {'torrents/hash.pb'}}
    ),
    CleanupTestCase(
        name='removed_versions',
        datasets={'hash': {Dataset('ds1', '1')}},
        removed_suffixes={Dataset('ds1', '1'): {'versions/ds1_1'}}
    ),
    CleanupTestCase(
        name='obsolete_files',
        datasets={'hash': {Dataset('ds1', '1')}},
        extra_files={'torrents/lol.pb'}
    ),
    CleanupTestCase(
        name='invalid_files',
        datasets={'hash': {Dataset('ds1', '1')}},
        extra_files={'torrents/lol.kek'}
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_cleanup(coordinator_polling_job: CoordinatorPollingJob, data_storage: DataStorageProxy, case: CleanupTestCase):
    for hash, datasets in case.datasets.items():
        data_storage.add_torrent(Torrent(hash, b'content'), datasets)
    for dataset in case.restricted_to_remove:
        data_storage.set_postdl_done(dataset)

    for path_list in case.removed_suffixes.values():
        for path in path_list:
            data_storage._storages[0]._remove_file_or_directory(os.path.join(data_storage._storages[0]._path.root, path))
    for path in case.extra_files:
        with open(os.path.join(data_storage._storages[0]._path.root, path), 'w') as file:
            file.write('kek')

    await coordinator_polling_job._process_obsolete_datasets()
    for path in case.extra_files:
        assert not os.path.exists(os.path.join(data_storage._storages[0]._path.root, path))

    remove_hooks = set()
    while not coordinator_polling_job._hooks_queue.empty():
        remove_hooks.add(coordinator_polling_job._hooks_queue.get_nowait().dataset)

    for dataset in case.removed_suffixes.keys():
        if dataset in case.restricted_to_remove:
            assert dataset in remove_hooks
            assert dataset in itertools.chain.from_iterable(data_storage._storages[0]._torrents.values())
        else:
            assert dataset not in itertools.chain.from_iterable(data_storage._storages[0]._torrents.values())
