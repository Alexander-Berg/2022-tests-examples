import pytest

from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import NotFoundError, ForbiddenError


def test_torrent_auth(coordinator):
    # green way
    coordinator.torrents(tvm_id=9, host='tvm1')

    # for unknown host must return 404
    with pytest.raises(NotFoundError):
        coordinator.torrents(host='unknown_host')

    # other tvm
    with pytest.raises(ForbiddenError):
        coordinator.torrents(tvm_id=10, host='tvm1')

    # 200 for hosts with not setted up tvm in config
    coordinator.torrents(tvm_id=9, host='a1')


def test_single_host(coordinator):
    coordinator.torrents(host='host.sas.yp-c.yandex.net')
