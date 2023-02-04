import pytest
from dataclasses import dataclass, field

from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_polling_job import CoordinatorPollingJob
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import ContentStorage


@dataclass
class FullSyncStorageModeCase:
    name: str
    initial_hashes: set[str] = field(default_factory=set)
    full_sync_hashes: set[str] = field(default_factory=set)

    def __str__(self) -> str:
        return self.name


CASES = [
    FullSyncStorageModeCase(
        name='initial_add',
        initial_hashes=set(),
        full_sync_hashes={'hash'}
    ),
    FullSyncStorageModeCase(
        name='append_torrent',
        initial_hashes={'hash'},
        full_sync_hashes={'hash1', 'hash'}
    ),
    FullSyncStorageModeCase(
        name='no_torrents',
        initial_hashes={'hash'},
        full_sync_hashes=set()
    ),
    FullSyncStorageModeCase(
        name='no_change',
        initial_hashes={'hash'},
        full_sync_hashes={'hash'}
    ),
    FullSyncStorageModeCase(
        name='remove_one_torrent',
        initial_hashes={'hash', 'hash1'},
        full_sync_hashes={'hash'}
    )
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_full_sync_torrents_storage_mode(
        storage_coordinator_polling_job: CoordinatorPollingJob,
        content_storage: ContentStorage,
        storage_torrents_collection_factory,
        case: FullSyncStorageModeCase
):
    if case.initial_hashes:
        await storage_coordinator_polling_job._full_sync_torrents(
            storage_torrents_collection_factory(case.initial_hashes))
        assert len(storage_coordinator_polling_job._ymtorrent_queue) == 1
        storage_coordinator_polling_job._ymtorrent_queue.clear()
    await storage_coordinator_polling_job._full_sync_torrents(storage_torrents_collection_factory(case.full_sync_hashes))
    assert len(storage_coordinator_polling_job._ymtorrent_queue) == 1
    ymtorrent_hashes = {torrent.hash for torrent in storage_coordinator_polling_job._ymtorrent_queue.get()[0].torrents}
    assert ymtorrent_hashes == case.full_sync_hashes
    for hash in case.initial_hashes - case.full_sync_hashes:
        assert hash not in content_storage.hash_to_datasets_map
    for hash in case.full_sync_hashes:
        assert hash in content_storage.hash_to_datasets_map
