import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import ServerError


def test_db_wipe(mongo, coordinator):
    coordinator.http_get('/ping')

    torrent_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    assert torrent_hash in coordinator.torrents(host='storage11', tvm_id=12345).all_torrents

    mongo.drop_database()

    with pytest.raises(ServerError):
        coordinator.torrents(host='')
