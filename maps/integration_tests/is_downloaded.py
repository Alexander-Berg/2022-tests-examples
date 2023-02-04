import os
import time
import grpc

from maps.infra.ecstatic.ymtorrent.py.ymtorrent import generate_torrent, compute_torrent_hash
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Coordinator, Dataset
from maps.infra.ecstatic.ymtorrent.proto import torrent_pb2, torrent_pb2_grpc


def test_is_downloaded_not_downloaded(storage, tmpdir, coordinator, coordinator_api: Coordinator):
    with open(os.path.join(tmpdir, 'file'), 'wb') as file:
        data = os.urandom(1 << 27)
        for _ in range(10):
            file.write(data)
    torrent = generate_torrent(str(tmpdir), coordinator.url)

    coordinator_api.upload_torrent_metainfo(torrent, Dataset('pkg-a', '1.0'))
    time.sleep(10)
    stub = torrent_pb2_grpc.ymtorrentStub(
        grpc.insecure_channel(storage.grpc_socket)
    )
    is_downloaded_response = stub.isDownloaded(torrent_pb2.IsDownloadedRequest(torrent_hash=compute_torrent_hash(torrent)))
    assert not is_downloaded_response.downloaded


def test_is_downloaded_downloaded(storage, ecstatic_tool, tmpdir, coordinator):
    with open(os.path.join(tmpdir, 'file'), 'wb') as file:
        data = os.urandom(1 << 27)
        for _ in range(10):
            file.write(data)
    ecstatic_tool.upload_dataset(
        dataset_name='pkg-a',
        dataset_version='1.0',
        directory=str(tmpdir),
        branches=['+stable'])
    time.sleep(10)
    torrent_hash = coordinator.torrents(host='localhost').all_torrents.pop()
    stub = torrent_pb2_grpc.ymtorrentStub(
        grpc.insecure_channel(storage.grpc_socket)
    )
    is_downloaded_response = stub.isDownloaded(
        torrent_pb2.IsDownloadedRequest(torrent_hash=torrent_hash)
    )
    assert is_downloaded_response.downloaded
