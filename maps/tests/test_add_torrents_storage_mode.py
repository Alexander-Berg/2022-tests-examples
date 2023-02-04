import pytest
from dataclasses import dataclass, field

from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_polling_job import CoordinatorPollingJob
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import ContentStorage


@dataclass
class AddTorrentsStorageModeCase:
    name: str
    initial_hashes: set[str] = field(default_factory=set)
    new_hashes: set[str] = field(default_factory=set)

    def __str__(self) -> str:
        return self.name


CASES = [
    AddTorrentsStorageModeCase(
        name='initial_add',
        initial_hashes=set(),
        new_hashes={'hash'}
    ),
    AddTorrentsStorageModeCase(
        name='append_torrent',
        initial_hashes={'hash'},
        new_hashes={'hash1'}
    ),
    AddTorrentsStorageModeCase(
        name='empty_response',
        initial_hashes={'hash'},
        new_hashes=set()
    ),
    AddTorrentsStorageModeCase(
        name='already_exists',
        initial_hashes={'hash'},
        new_hashes={'hash'}
    )
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_add_torrents_storage_mode(
        storage_coordinator_polling_job: CoordinatorPollingJob,
        storage_torrents_collection_factory,
        content_storage: ContentStorage,
        case: AddTorrentsStorageModeCase
):
    if case.initial_hashes:
        await storage_coordinator_polling_job._full_sync_torrents(
            storage_torrents_collection_factory(case.initial_hashes))
        assert len(storage_coordinator_polling_job._ymtorrent_queue) == len(case.initial_hashes)
        storage_coordinator_polling_job._ymtorrent_queue.clear()
    await storage_coordinator_polling_job._add_torrents(storage_torrents_collection_factory(case.new_hashes))
    hashes_for_ymtorrent = {elem.torrent.hash for elem in storage_coordinator_polling_job._ymtorrent_queue.get()}
    assert hashes_for_ymtorrent == case.new_hashes - case.initial_hashes
    for hash in hashes_for_ymtorrent:
        assert hash in content_storage.hash_to_datasets_map
