import pytest
from unittest import mock

from maps.infra.ecstatic.common.experimental_worker.lib.ymtorrent_job import YmtorrentJob, Torrent
from maps.infra.ecstatic.ymtorrent.proto import torrent_pb2
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


@pytest.mark.asyncio
async def test_incremental_add_torrent(ymtorrent_job: YmtorrentJob, ymtorrent_stub, data_storage: DataStorageProxy):
    data_storage.add_torrent(Torrent(hash='hash',
                                     content=b'content',
                                     priority=1
                                     ), [Dataset('ds', '1')])

    await ymtorrent_job._sync_torrents(
        {Torrent(hash='hash', content=b'content', priority=1)}, full_sync=False
    )

    assert ymtorrent_stub.addTorrents.call_count == 1
    assert ymtorrent_stub.addTorrents.call_args_list[0] == mock.call(
        torrent_pb2.AddTorrentRequests(
            full_sync=False,
            requests=[
                torrent_pb2.AddTorrentRequest(
                    torrent_hash='hash',
                    torrent_content=b'content',
                    priority=1,
                    torrent_name=data_storage.torrent_name('hash')
                )]
        ),
        timeout=60
    )


@pytest.mark.asyncio
async def test_full_sync_torrents(ymtorrent_job: YmtorrentJob, ymtorrent_stub, data_storage: DataStorageProxy):
    data_storage.add_torrent(Torrent(hash='hash',
                                     content=b'content',
                                     priority=1
                                     ), [Dataset('ds', '1')])
    await ymtorrent_job._sync_torrents(
        {Torrent(hash='hash', content=b'content', priority=1)}, full_sync=True
    )
    assert ymtorrent_stub.addTorrents.call_count == 1
    assert ymtorrent_stub.addTorrents.call_args_list[0] == mock.call(
        torrent_pb2.AddTorrentRequests(
            full_sync=True,
            requests=[
                torrent_pb2.AddTorrentRequest(
                    torrent_hash='hash',
                    torrent_content=b'content',
                    priority=1,
                    torrent_name=data_storage.torrent_name('hash')
                )]
        ),
        timeout=120
    )


@pytest.mark.asyncio
async def test_purge_torrents(ymtorrent_job: YmtorrentJob, ymtorrent_stub, data_storage: DataStorageProxy):
    data_storage.add_torrent(Torrent(hash='hash',
                                     content=b'content',
                                     priority=1
                                     ), [Dataset('ds', '1')])
    await ymtorrent_job._purge_torrents({'hash'})
    assert ymtorrent_stub.purgeTorrents.call_count == 1
    assert ymtorrent_stub.purgeTorrents.call_args_list[0] == mock.call(
        torrent_pb2.PurgeTorrentsRequest(
            torrent_names=['hash']
        ),
        timeout=60
    )
