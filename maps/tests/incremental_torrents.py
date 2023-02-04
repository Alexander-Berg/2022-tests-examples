import time

import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import GoneError

from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


def test_incremental_torrents(coordinator):
    watermark = None
    all_hosts = ['storage11', 'a1', 'b1']

    def check_changes(
            watermark,
            host,
            torrents=[],
            datasets=[]):
        response = coordinator.torrents(host=host, since=watermark, tvm_id=12345)
        assert response.all_torrents == set(torrents)
        assert response.all_datasets == set(datasets)

    def check_no_changes(watermark, hosts):
        for host in hosts:
            check_changes(watermark, host)

    def update_watermark():
        storage_watermark = coordinator.torrents(host='storage11', since=watermark, tvm_id=12345).watermark

        for host in all_hosts:
            assert coordinator.torrents(host=host, tvm_id=12345).watermark == storage_watermark

        check_no_changes(storage_watermark, all_hosts)
        return storage_watermark

    # initially everything should be clean, watermark isn't initialized
    for host in all_hosts:
        check_changes(None, host)

    # upload the dataset without moving it into branches (establishes the watermark)
    pkg_a1_hash = coordinator.upload('pkg-a:1', '1.0', 'gen-ab1', tvm_id=1)

    check_no_changes(watermark, ['a1', 'b1'])

    check_changes(watermark, 'storage11', [pkg_a1_hash])

    watermark = update_watermark()

    with pytest.raises(GoneError):
        coordinator.torrents(host='storage11', since='000000000000000000000000', tvm_id=12345)

    # move it
    time.sleep(1)
    coordinator.step_in('pkg-a', '1.0', 'gen-ab1', tag=':1', tvm_id=1)
    check_no_changes(watermark, ['storage11', 'b1'])

    check_changes(
        watermark,
        'a1',
        [pkg_a1_hash], [Dataset('pkg-a:1', '1.0')])

    watermark = update_watermark()

    before_pkg_b_watermark = watermark
    # branches are independent
    time.sleep(1)
    pkg_b11_hash = coordinator.upload('pkg-b', '1.1', 'gen-ab1', tvm_id=1)
    time.sleep(1)
    coordinator.step_in('pkg-b', '1.1', 'gen-ab1', tvm_id=1)

    check_no_changes(watermark, ['a1'])

    check_changes(watermark, 'storage11', [pkg_b11_hash])

    check_changes(
        watermark,
        'b1',
        [pkg_b11_hash], [Dataset('pkg-b', '1.1')])

    watermark = update_watermark()

    # different watermarks work correctly
    time.sleep(1)
    pkg_b12_hash = coordinator.upload('pkg-b', '1.2', 'gen-ab1', tvm_id=1)

    check_no_changes(watermark, ['a1', 'b1'])

    check_changes(watermark, 'storage11', [pkg_b12_hash])

    watermark = update_watermark()

    time.sleep(1)
    coordinator.step_in('pkg-b', '1.2', 'gen-ab1', tvm_id=1)

    check_no_changes(watermark, ['storage11', 'a1'])

    check_changes(
        watermark,
        'b1',
        [pkg_b12_hash], [Dataset('pkg-b', '1.2')])

    # go back in time
    watermark = before_pkg_b_watermark

    check_no_changes(watermark, ['a1'])

    check_changes(
        watermark,
        'b1',
        [pkg_b11_hash, pkg_b12_hash],
        [Dataset('pkg-b', '1.1'), Dataset('pkg-b', '1.2')])

    check_changes(
        watermark,
        'storage11',
        [pkg_b11_hash, pkg_b12_hash])

    watermark = update_watermark()

    # uploading tags pushes new data
    time.sleep(1)
    pkg_a2_hash = coordinator.upload('pkg-a:2', '1.0', 'gen-ab1', tvm_id=1)

    check_no_changes(watermark, ['b1'])

    check_changes(watermark, 'storage11', [pkg_a2_hash])

    # old data arrives as well for now
    check_changes(
        watermark,
        'a1',
        [pkg_a2_hash, pkg_a1_hash],
        [Dataset('pkg-a:2', '1.0'),
         Dataset('pkg-a:1', '1.0')])

    watermark = update_watermark()

    # reuploading works correctly
    time.sleep(1)
    coordinator.remove('pkg-b', '1.2', 'gen-ab1', tvm_id=1)
    time.sleep(1)
    pkg_b12_hash = coordinator.upload('pkg-b', '1.2', 'gen-ab1', tvm_id=1)

    check_no_changes(watermark, ['a1', 'b1'])

    check_changes(watermark, 'storage11', [pkg_b12_hash])

    watermark = update_watermark()

    time.sleep(1)
    coordinator.step_in('pkg-b', '1.2', 'gen-ab1', tvm_id=1)

    check_no_changes(watermark, ['storage11', 'a1'])

    check_changes(
        watermark,
        'b1',
        [pkg_b12_hash], [Dataset('pkg-b', '1.2')])
