from maps.infra.ecstatic.tool.ecstatic_api import coordinator
from maps.infra.ecstatic.tool.client_interface import local_lock
from maps.infra.ecstatic.common.worker.lib.worker import EcstaticWorker
from maps.infra.ecstatic.common.worker.lib.status_storage import StatusStorage
from maps.infra.ecstatic.common.worker.lib.data_storage import DatasetsStorage, ContentStorage, DataStorageProxy
from maps.infra.ecstatic.common.worker.lib.paths import StoragePath
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset
from maps.infra.ecstatic.common.worker.lib.utils import Torrent, read_coordinator_config
from maps.infra.ecstatic.common.worker.lib.hook_controller import HookFailedError, HookController
from maps.pylibs.utils.lib.process import kill_process_tree

import asynctest
import asyncio
import filecmp
import itertools
import os
import pytest
import shutil
# import threading
import time
import logging
import yatest
import fcntl
import threading
from maps.pylibs.utils.lib import filesystem
from mock import patch, Mock

logger = logging.getLogger("ecstatic")
TEST_CONTENT = 'test_content'
DEFAULT_HOOK_TIMEOUTS = {'check': 300, 'remove': 300, 'postdl': 300, 'switch': 600}


def check_directories_are_same(dir1, dir2):
    assert not filecmp.dircmp(dir1, dir2).diff_files
    assert not filecmp.dircmp(dir1, dir2).left_only
    assert not filecmp.dircmp(dir1, dir2).right_only


@pytest.fixture(scope='function', autouse=True)
def work_dir(monkeypatch):
    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter(
        'ecstatic-agent:[%(asctime)s]: %(levelname)s: %(message)s')
    handler = logging.StreamHandler()
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    with filesystem.temporary_directory(os.path.abspath(yatest.common.work_path()), keep=True) as tmp_dir:
        # Mock local lock file to prevent races between tests
        def mock_local_lock_file():
            return os.path.join(tmp_dir, 'local.lock')

        monkeypatch.setattr('maps.infra.ecstatic.common.worker.lib.worker.local_lock.local_lock_file',
                            mock_local_lock_file)

        def kill_process(pid, sig, sudo=False):
            kill_process_tree(pid, sig, sudo=False)

        # we cannot use sudo on dist
        monkeypatch.setattr('maps.infra.ecstatic.common.worker.lib.hook_controller.kill_process_tree', kill_process)

        work_dir = StoragePath(os.path.join(tmp_dir, 'work'))
        shutil.copytree(yatest.common.test_source_path('data'), work_dir.base, symlinks=True)
        os.chmod(work_dir.base, 0o777)
        for path, dirs, files in os.walk(work_dir.base):
            for dir in dirs:
                os.chmod(os.path.join(path, dir), 0o777)
            for file in files:
                os.chmod(os.path.join(path, file), 0o777)

        for subdir in [work_dir.torrents,
                       work_dir.versions,
                       work_dir.content,
                       work_dir.data,
                       work_dir.preserved_data,
                       os.path.join(work_dir.root, 'hook_logs')]:
            os.makedirs(subdir)

        return work_dir


@patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.sync_torrents", Mock())
@patch("maps.infra.ecstatic.common.worker.lib.worker.EcstaticWorker._is_downloaded")
def test_datasets(mock_is_downloaded, work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS)

    status_storage = StatusStorage(os.path.join(work_dir.base, 'status'))

    with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
        torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())

    assert torrents_collection.datasets
    assert read_coordinator_config(conf_dir) == {
        "coordinator": "test_coordinator_host",
        "coordinator_tvm_id": 1234
    }

    #  check empty state
    assert not worker.data_storage.hash_to_datasets_map
    assert not worker.data_storage.active_datasets

    #  check add torrents
    worker.merge_torrents(torrents_collection, True)

    ds1 = coordinator.Dataset('dataset1:tag1', 'version1')
    ds2 = coordinator.Dataset('dataset2', 'version2')
    ds3_1 = coordinator.Dataset('dataset3', '1')
    ds3_2 = coordinator.Dataset('dataset3', '2')

    reference_datasets = {
        '4c898a23bc9d20a6b07610e1d539162603cce43a': {ds1},
        '8a8b6d1738624b32b6bc00ad0f7f56940e21d5c7': {ds2},
        'eb092896da70184737fc6258ce32fae89317aa6e': {ds3_1},
        'f6bbd4556968b13725b513440eaf98d9ef3a95d7': {ds3_2}
    }

    assert worker.data_storage.hash_to_datasets_map == reference_datasets

    #  check add torrents one more time
    worker.merge_torrents(torrents_collection, True)
    assert worker.data_storage.hash_to_datasets_map == reference_datasets

    assert status_storage.age_torrents_full_sync() is None

    #  merge to empty list without full_sync should not remove torrents
    worker.merge_torrents(coordinator.TorrentsCollection(), False)
    assert worker.data_storage.hash_to_datasets_map == reference_datasets

    #  merge to empty list with full_sync removes torrents
    worker.merge_torrents(coordinator.TorrentsCollection(), True)
    assert worker.data_storage.hash_to_datasets_map == {}

    worker.merge_torrents(torrents_collection, False)
    assert worker.data_storage.hash_to_datasets_map == reference_datasets

    #  add dataset data as if it were downloaded by the torrent client
    with open(os.path.join(
            os.path.join(work_dir.root, 'torrents'),
            '4c898a23bc9d20a6b07610e1d539162603cce43a.data/file'), 'w') as f:
        f.write(TEST_CONTENT)

    def fake_is_downloaded_ds1(dataset):
        return dataset == ds1

    def fake_is_downloaded_no_ds1(dataset):
        return False

    mock_is_downloaded.side_effect = fake_is_downloaded_ds1

    postdl_done, postdl_failed = worker.run_postdl([])
    assert not postdl_done
    assert not postdl_failed

    switch_failed = asyncio.run(worker.run_switch([]))
    assert not switch_failed

    postdl_done, postdl_failed = worker.run_postdl([ds1])
    assert postdl_done == [ds1]
    assert not postdl_failed

    assert status_storage.age_working() < 5

    switch_failed = asyncio.run(worker.run_switch([ds1]))
    assert worker.data_storage.active_datasets == [ds1]
    assert not switch_failed

    #  check hooks run
    hook_logs_dir = os.path.join(work_dir.root, 'hook_logs')
    for hook in ['test_hook.postdl.log', 'test_hook.switch.log']:
        with open(os.path.join(hook_logs_dir, hook)) as f:
            lines = list(map(str.strip, f.readlines()))
            assert len(lines) == 2
            assert lines[0] == 'tag1'
            assert lines[1] == TEST_CONTENT

    #  try to prepare active dataset to __CURRENT__ state
    assert worker.data_storage.active_datasets == [ds1]
    assert worker.prepare_switch_list([coordinator.Dataset('dataset1:tag1', '__CURRENT__')]) == []
    assert worker.data_storage.active_datasets == [ds1]

    #  try to prepare active dataset to __NONE__ state
    disable_ds1 = worker.prepare_switch_list([coordinator.Dataset('dataset1:tag1', '__NONE__')])
    assert disable_ds1 == [coordinator.Dataset('dataset1:tag1', None)]
    assert worker.data_storage.active_datasets == [ds1]

    # check that datasets which disappeared from the disk are ignored
    mock_is_downloaded.side_effect = fake_is_downloaded_no_ds1
    assert worker.prepare_switch_list([ds1]) == []
    assert worker.data_storage.active_datasets == [ds1]
    mock_is_downloaded.side_effect = fake_is_downloaded_ds1

    #  deactivate active dataset
    switch_failed = asyncio.run(worker.run_switch(disable_ds1))
    assert not switch_failed
    assert worker.data_storage.active_datasets == []
    #  still have in downloaded state
    assert ds1 in worker.data_storage.hash_to_datasets_map[
        '4c898a23bc9d20a6b07610e1d539162603cce43a']

    #  switch hook on this version always return 1
    switch_failed = asyncio.run(worker.run_switch([ds3_1]))
    assert worker.data_storage.active_datasets == []
    assert len(switch_failed) == 1 and switch_failed[0][0] == ds3_1

    assert ds3_1 not in worker.data_storage.active_datasets
    assert worker.data_storage.is_switch_failed(ds3_1.name)

    #  switch failed dataset to __CURRENT__ should remove it
    ds3_current = worker.prepare_switch_list([coordinator.Dataset('dataset3', '__CURRENT__')])
    assert ds3_current == [coordinator.Dataset('dataset3', '')]
    switch_failed = asyncio.run(worker.run_switch(ds3_current))
    assert worker.data_storage.active_datasets == []
    assert switch_failed == []
    assert not worker.data_storage.is_switch_failed(ds3_1.name)

    #  this switch return 0
    switch_failed = asyncio.run(worker.run_switch([ds3_2]))
    assert worker.data_storage.active_datasets == [ds3_2]
    assert not switch_failed

    #  check switch back to previously deactivated
    switch_failed = asyncio.run(worker.run_switch([ds1]))
    assert ds1 in worker.data_storage.active_datasets
    assert len(worker.data_storage.active_datasets) == 2
    assert not switch_failed

    #  torrents collection with one dataset removed
    with open(os.path.join(work_dir.base, 'torrents_cropped.pb'), 'rb') as torrents_pb:
        cropped_torrents = coordinator.TorrentsCollection(torrents_pb.read())

    dataset1_hash = '4c898a23bc9d20a6b07610e1d539162603cce43a'
    assert dataset1_hash in torrents_collection.datasets
    assert dataset1_hash not in cropped_torrents.datasets
    assert dataset1_hash in worker.data_storage.hash_to_datasets_map

    worker.merge_torrents(cropped_torrents, True)

    #  check we do not remove active dataset
    assert dataset1_hash in worker.data_storage.hash_to_datasets_map
    assert ds1 in worker.data_storage.active_datasets

    switch_failed = asyncio.run(worker.run_switch(disable_ds1))
    assert not switch_failed

    # now we remove
    worker.merge_torrents(cropped_torrents, True)
    assert dataset1_hash not in worker.data_storage.hash_to_datasets_map

    assert dataset1_hash not in os.listdir(os.path.join(work_dir.root, 'content'))

    with open(os.path.join(hook_logs_dir, 'test_hook.remove.log')) as f:
        lines = list(map(str.strip, f.readlines()))
        assert len(lines) == 2
        assert lines[0] == 'tag1'
        assert lines[1] == 'version1'


@patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.sync_torrents", Mock())
def test_storage_mode(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        storage_mode=True)
    StatusStorage(os.path.join(work_dir.base, 'status'))


@pytest.mark.parametrize(
    'need_content_path, need_torrent_path, need_torrent_file, need_protobuf_file, need_dataset_symlink',
    set(itertools.product((True, False), repeat=5)) - {tuple(False for i in range(5))})
def test_datasets_storage_remove_obsolete_datasets(need_content_path, need_torrent_path, need_torrent_file,
                                                   need_protobuf_file, need_dataset_symlink, work_dir):
    not_delete_files = need_content_path and need_torrent_path and (need_torrent_file or need_protobuf_file) and need_dataset_symlink

    content_path = os.path.join(work_dir.content, '4c898a23bc9d20a6b07610e1d539162603cce43a')
    torrent_path = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.data')
    torrent_file = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.torrent')
    protobuf_file = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.pb')
    dataset_symlink = os.path.join(work_dir.versions, "ds_1")
    if need_content_path:
        os.mkdir(content_path)

    if need_torrent_path:
        os.symlink(os.path.relpath(content_path, work_dir.torrents), torrent_path)

    if need_dataset_symlink:
        os.symlink(os.path.relpath(content_path, work_dir.versions), dataset_symlink)

    if need_torrent_file:
        with open(torrent_file, 'w') as f:
            f.write('4c898a23bc9d20a6b07610e1d539162603cce43a torrent file')

    if need_protobuf_file:
        with open(protobuf_file, 'w') as f:
            f.write('4c898a23bc9d20a6b07610e1d539162603cce43a protobuf file')

    storage = DatasetsStorage(work_dir)

    ds1 = coordinator.Dataset('ds', '1')

    reference_datasets = {
    }

    obsolete_datasets = {
        '4c898a23bc9d20a6b07610e1d539162603cce43a': {ds1},
    } if need_dataset_symlink else {'4c898a23bc9d20a6b07610e1d539162603cce43a': set()}

    if not_delete_files:
        obsolete_datasets, reference_datasets = reference_datasets, obsolete_datasets

    assert storage.obsolete_datasets == obsolete_datasets

    storage.remove_obsolete_data()

    assert storage.hash_to_datasets_map == reference_datasets

    assert not os.path.exists(torrent_file)

    assert not_delete_files == os.path.exists(content_path)
    assert not_delete_files == os.path.exists(torrent_path)
    assert not_delete_files == os.path.exists(protobuf_file)
    assert not_delete_files == os.path.exists(dataset_symlink)


@pytest.mark.parametrize('need_content_path, need_torrent_path, need_torrent_file, need_protobuf_file',
                         set(itertools.product((True, False), repeat=4)) - {tuple(False for i in range(4))})
def test_content_storage_remove_obsolete_torrents(need_content_path, need_torrent_path, need_torrent_file,
                                                  need_protobuf_file, work_dir):
    not_delete_files = need_content_path and need_torrent_path and (need_torrent_file or need_protobuf_file)

    content_path = os.path.join(work_dir.content, '4c898a23bc9d20a6b07610e1d539162603cce43a')
    torrent_path = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.data')
    torrent_file = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.torrent')
    protobuf_file = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.pb')
    if need_content_path:
        os.mkdir(content_path)

    if need_torrent_path:
        os.symlink(os.path.relpath(content_path, work_dir.torrents), torrent_path)

    if need_torrent_file:
        with open(torrent_file, 'w') as f:
            f.write('4c898a23bc9d20a6b07610e1d539162603cce43a torrent file')

    if need_protobuf_file:
        with open(protobuf_file, 'w') as f:
            f.write('4c898a23bc9d20a6b07610e1d539162603cce43a protobuf file')

    storage = ContentStorage(work_dir)

    reference_datasets = {
    }

    obsolete_datasets = {
        '4c898a23bc9d20a6b07610e1d539162603cce43a': set()
    }

    if not_delete_files:
        obsolete_datasets, reference_datasets = reference_datasets, obsolete_datasets

    assert storage.obsolete_datasets == obsolete_datasets

    storage.remove_obsolete_data()

    assert storage.hash_to_datasets_map == reference_datasets

    assert not os.path.exists(torrent_file)

    assert not_delete_files == os.path.exists(content_path)
    assert not_delete_files == os.path.exists(torrent_path)
    assert not_delete_files == os.path.exists(protobuf_file)


@patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.purge_torrents")
@patch('maps.infra.ecstatic.common.worker.lib.hook_controller.HookController.trigger_remove')
def test_worker_call_remove_hook_for_obsolete_datasets_client_mode(trigger_remove_mock, purge_torrents_mock, work_dir):
    trigger_remove_mock.side_effect = asynctest.CoroutineMock()
    purge_torrents_mock.side_effect = Mock()

    content_path = os.path.join(work_dir.content, '4c898a23bc9d20a6b07610e1d539162603cce43a')
    torrent_path = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.data')
    #    pb_file = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.pb')
    dataset_symlink = os.path.join(work_dir.versions, "ds_1")

    os.mkdir(content_path)
    os.symlink(os.path.relpath(content_path, work_dir.torrents), torrent_path)
    os.symlink(os.path.relpath(content_path, work_dir.versions), dataset_symlink)

    #    with open(pb_file, 'w') as f:
    #       f.write('4c898a23bc9d20a6b07610e1d539162603cce43a pb file')

    conf_dir = os.path.join(work_dir.base, 'config')
    EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path='ymtorrent.socket',
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        storage_mode=False)

    trigger_remove_mock.assert_called_once_with(coordinator.Dataset('ds', '1'))
    purge_torrents_mock.assert_called_once_with({'4c898a23bc9d20a6b07610e1d539162603cce43a': ...}.keys())


@patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.purge_torrents")
@patch('maps.infra.ecstatic.common.worker.lib.hook_controller.HookController.trigger_remove')
def test_worker_call_remove_hook_for_obsolete_datasets_storage_mode(trigger_remove_mock, purge_torrents_mock, work_dir):
    trigger_remove_mock.side_effect = asynctest.CoroutineMock()
    purge_torrents_mock.side_effect = Mock()

    content_path = os.path.join(work_dir.content, '4c898a23bc9d20a6b07610e1d539162603cce43a')
    torrent_path = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.data')
    #    pb_file = os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.pb')

    os.mkdir(content_path)
    os.symlink(os.path.relpath(content_path, work_dir.torrents), torrent_path)

    #    with open(pb_file, 'w') as f:
    #       f.write('4c898a23bc9d20a6b07610e1d539162603cce43a pb file')

    conf_dir = os.path.join(work_dir.base, 'config')
    EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path='ymtorrent.socket',
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        storage_mode=True)

    trigger_remove_mock.assert_not_called()
    purge_torrents_mock.assert_called_once_with({'4c898a23bc9d20a6b07610e1d539162603cce43a': ...}.keys())


def test_datasets_storage_add_torrents(work_dir):
    data_storage = DatasetsStorage(work_dir)

    data_storage.add_datasets(
        Torrent('4c898a23bc9d20a6b07610e1d539162603cce43a', b'4c898a23bc9d20a6b07610e1d539162603cce43a content'),
        [Dataset('name', 'version')])

    assert not os.path.exists(os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.torrent'))
    assert os.path.exists(os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.pb'))


def test_datasets_storage_remove_torrent(work_dir):
    data_storage = DatasetsStorage(work_dir)

    data_storage.add_datasets(
        Torrent('4c898a23bc9d20a6b07610e1d539162603cce43a', b'4c898a23bc9d20a6b07610e1d539162603cce43a content'),
        [Dataset('name', 'version')])

    data_storage.remove_datasets('4c898a23bc9d20a6b07610e1d539162603cce43a', [Dataset('name', 'version')])

    assert not os.path.exists(os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.torrent'))
    assert not os.path.exists(os.path.join(work_dir.torrents, '4c898a23bc9d20a6b07610e1d539162603cce43a.pb'))


def test_add_torrent(work_dir):
    with filesystem.temporary_directory() as dir:
        conf_dir = os.path.join(work_dir.base, 'config')
        logger.info('launching ymtorrent')
        socket = 'unix:{}/ymtorrent.sock'.format(dir)
        status_file_path = os.path.join(work_dir.base, 'status', 'ymtorrent.status')
        worker = EcstaticWorker(
            conf_dir=conf_dir,
            status_dir=os.path.join(work_dir.base, 'status'),
            storages=[work_dir],
            socket_path=socket,
            hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
            storage_mode=False)
        ymtorrent_process = yatest.common.execute([yatest.common.binary_path('maps/infra/ecstatic/ymtorrent/bin/ymtorrent'),
                                                   '--debug', '1', '--grpc-socket', socket, '-d',
                                                   yatest.common.output_path(), '-S', status_file_path, '-D'],
                                                  wait=False)
        logger.info("ymtorrent is launched")
        time.sleep(1)
        with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
            torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())
        assert torrents_collection.datasets
        worker.merge_torrents(torrents_collection, True)
        time.sleep(5)
        assert ymtorrent_process.running
        status = worker.status_storage.ymtorrent_status()
        assert set(torrents_collection.datasets.keys()) == set(status.keys())

        ymtorrent_process.kill()


def test_add_torrent_storage_mode(work_dir):
    with filesystem.temporary_directory() as dir:
        conf_dir = os.path.join(work_dir.base, 'config')
        logger.info('launching ymtorrent')
        socket = 'unix:{}/ymtorrent.sock'.format(dir)
        status_file_path = os.path.join(work_dir.base, 'status', 'ymtorrent.status')
        worker = EcstaticWorker(
            conf_dir=conf_dir,
            status_dir=os.path.join(work_dir.base, 'status'),
            storages=[work_dir],
            socket_path=socket,
            hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
            storage_mode=True)
        ymtorrent_process = yatest.common.execute([yatest.common.binary_path('maps/infra/ecstatic/ymtorrent/bin/ymtorrent'),
                                                   '--debug', '1', '--grpc-socket', socket, '-d',
                                                   yatest.common.output_path(), '-S', status_file_path, '-D'],
                                                  wait=False)
        logger.info("ymtorrent is launched")
        time.sleep(1)
        with open(os.path.join(work_dir.base, 'storage_torrents.pb'), 'rb') as torrents_pb:
            torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())
        worker.merge_torrents(torrents_collection, True)
        time.sleep(5)
        assert ymtorrent_process.running
        status = worker.status_storage.ymtorrent_status()
        torrent_hashes = {'4c898a23bc9d20a6b07610e1d539162603cce43a',
                          'eb092896da70184737fc6258ce32fae89317aa6e',
                          'f6bbd4556968b13725b513440eaf98d9ef3a95d7',
                          '8a8b6d1738624b32b6bc00ad0f7f56940e21d5c7'}
        assert torrent_hashes == set(status.keys())

        ymtorrent_process.kill()


def test_remove_torrent(work_dir):
    with filesystem.temporary_directory() as dir:
        conf_dir = os.path.join(work_dir.base, 'config')
        logger.info('launching ymtorrent')
        socket = 'unix:{}/ymtorrent.sock'.format(dir)
        status_file_path = os.path.join(work_dir.base, 'status', 'ymtorrent.status')
        worker = EcstaticWorker(
            conf_dir=conf_dir,
            status_dir=os.path.join(work_dir.base, 'status'),
            storages=[work_dir],
            socket_path=socket,
            hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
            storage_mode=False)
        ymtorrent_process = yatest.common.execute([yatest.common.binary_path('maps/infra/ecstatic/ymtorrent/bin/ymtorrent'),
                                                   '--grpc-socket', socket, '-d',
                                                   yatest.common.output_path(), '-S', status_file_path, '-D'],
                                                  wait=False)
        logger.info("ymtorrent is launched")
        time.sleep(1)
        with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
            torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())
        worker.merge_torrents(torrents_collection, True)
        with open(os.path.join(work_dir.base, 'torrents_cropped.pb'), 'rb') as torrents_pb:
            cropped_torrents = coordinator.TorrentsCollection(torrents_pb.read())
        dataset1_hash = '4c898a23bc9d20a6b07610e1d539162603cce43a'
        assert len(list(cropped_torrents.datasets.keys())) > 0
        worker.merge_torrents(cropped_torrents, True)
        time.sleep(5)
        assert ymtorrent_process.running
        status = worker.status_storage.ymtorrent_status()
        assert dataset1_hash not in status.keys()

        assert set(cropped_torrents.datasets.keys()) == set(status.keys())

        ymtorrent_process.kill()


def test_active_dataset_not_removed(work_dir):
    with filesystem.temporary_directory() as dir:
        conf_dir = os.path.join(work_dir.base, 'config')
        logger.info('launching ymtorrent')
        socket = 'unix:{}/ymtorrent.sock'.format(dir)
        status_file_path = os.path.join(work_dir.base, 'status', 'ymtorrent.status')
        worker = EcstaticWorker(
            conf_dir=conf_dir,
            status_dir=os.path.join(work_dir.base, 'status'),
            storages=[work_dir],
            socket_path=socket,
            hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
            storage_mode=False)
        ymtorrent_process = yatest.common.execute([yatest.common.binary_path('maps/infra/ecstatic/ymtorrent/bin/ymtorrent'),
                                                   '--grpc-socket', socket, '-d',
                                                   yatest.common.output_path(), '-S', status_file_path, '-D'],
                                                  wait=False)
        logger.info("ymtorrent is launched")
        time.sleep(1)
        with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
            torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())

        worker.merge_torrents(torrents_collection, True)

        active_dataset = coordinator.Dataset('dataset1:tag1', 'version1')
        worker.data_storage.set_active_version(active_dataset)

        worker.merge_torrents(coordinator.TorrentsCollection(), True)

        assert worker.data_storage.hash_to_datasets_map == {
            '4c898a23bc9d20a6b07610e1d539162603cce43a': {active_dataset},
        }

        time.sleep(5)

        assert list(worker.status_storage.ymtorrent_status().keys()) == ['4c898a23bc9d20a6b07610e1d539162603cce43a']

        ymtorrent_process.kill()


def test_hooks_timeout(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    data_storage = DataStorageProxy([work_dir])
    hook_controller = HookController(conf_dir, data_storage, {'check': 300, 'remove': 300, 'postdl': 300, 'switch': 5})
    os.mkdir(os.path.join(work_dir.data, 'dataset2'))
    with open(os.path.join(work_dir.data, 'dataset2', 'file'), 'w') as file:
        fcntl.flock(file, fcntl.LOCK_EX)

        def assert_false():
            assert False, 'Timeout expired'

        timer = threading.Timer(6, assert_false)
        timer.start()
        with pytest.raises(HookFailedError):
            asyncio.run(hook_controller.trigger_switch(coordinator.Dataset('dataset2', None)))
        timer.cancel()
        fcntl.flock(file, fcntl.LOCK_UN)
        time.sleep(1)

        fcntl.flock(file, fcntl.LOCK_EX | fcntl.LOCK_NB)
        fcntl.flock(file, fcntl.LOCK_UN)
        assert not os.path.exists(os.path.join(work_dir.data, 'dataset2', 'file1'))


@asynctest.patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.sync_torrents", Mock())
@pytest.mark.asyncio
async def test_hardlinks(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )
    with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
        torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())

    worker.merge_torrents(torrents_collection, True)

    dataset3_1_content_path = os.path.join(work_dir.content, 'eb092896da70184737fc6258ce32fae89317aa6e')
    dataset3_2_content_path = os.path.join(work_dir.content, 'f6bbd4556968b13725b513440eaf98d9ef3a95d7')
    dataset3_hardlink_path = os.path.join(work_dir.preserved_data, 'dataset3')

    with open(os.path.join(dataset3_1_content_path, 'file'), 'w') as file:
        file.write('dataset3=1')

    with open(os.path.join(dataset3_2_content_path, 'file1'), 'w') as file:
        file.write('dataset3=2')

    await worker.run_switch([coordinator.Dataset('dataset3', '1')])
    check_directories_are_same(dataset3_hardlink_path, dataset3_1_content_path)

    await worker.run_switch([coordinator.Dataset('dataset3', None)])
    check_directories_are_same(dataset3_hardlink_path, dataset3_1_content_path)
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )
    check_directories_are_same(dataset3_hardlink_path, dataset3_1_content_path)

    await worker.run_switch([coordinator.Dataset('dataset3', '2')])
    check_directories_are_same(dataset3_hardlink_path, dataset3_2_content_path)


@patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.sync_torrents", Mock())
def test_remove_hardink(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )
    with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
        torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())

    worker.merge_torrents(torrents_collection, True)
    asyncio.run(worker.run_switch([coordinator.Dataset('dataset1:tag1', 'version1')]))
    asyncio.run(worker.run_switch([coordinator.Dataset('dataset1:tag1', None)]))

    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )

    worker.merge_torrents(coordinator.TorrentsCollection(), True)

    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )

    assert 'dataset1:tag1' not in os.listdir(work_dir.preserved_data)


@patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.sync_torrents", Mock())
@patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.purge_torrents", Mock())
def test_not_remove_hardlinks_while_purging_torrents(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )
    with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
        torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())

    worker.merge_torrents(torrents_collection, True)

    dataset3_1_content_path = os.path.join(work_dir.content, 'eb092896da70184737fc6258ce32fae89317aa6e')

    with open(os.path.join(dataset3_1_content_path, 'file'), 'w') as file:
        file.write('dataset3=1')

    asyncio.run(worker.run_switch([coordinator.Dataset('dataset3', '1')]))

    os.remove(os.path.join(work_dir.torrents, 'eb092896da70184737fc6258ce32fae89317aa6e.pb'))
    os.remove(os.path.join(work_dir.torrents, 'f6bbd4556968b13725b513440eaf98d9ef3a95d7.pb'))
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )

    assert os.path.exists(os.path.join(work_dir.preserved_data, 'dataset3'))
    assert os.listdir(os.path.join(work_dir.preserved_data, 'dataset3')) == ['file']
    with open(os.path.join(work_dir.preserved_data, 'dataset3', 'file')) as file:
        assert file.read() == 'dataset3=1'


@asynctest.patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.sync_torrents", Mock())
@pytest.mark.asyncio
async def test_hardlinks_hook_failed(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )
    with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
        torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())

    worker.merge_torrents(torrents_collection, True)

    dataset3_1_content_path = os.path.join(work_dir.content, 'eb092896da70184737fc6258ce32fae89317aa6e')
    dataset3_2_content_path = os.path.join(work_dir.content, 'f6bbd4556968b13725b513440eaf98d9ef3a95d7')
    dataset3_hardlink_path = os.path.join(work_dir.preserved_data, 'dataset3')

    with open(os.path.join(dataset3_1_content_path, 'file'), 'w') as file:
        file.write('dataset3=1')

    with open(os.path.join(dataset3_2_content_path, 'file1'), 'w') as file:
        file.write('dataset3=2')

    await worker.run_switch([coordinator.Dataset('dataset3', '1')])
    check_directories_are_same(dataset3_hardlink_path, dataset3_1_content_path)

    with patch("maps.infra.ecstatic.common.worker.lib.hook_controller.HookController.trigger_switch") as switch:
        async def foo(*args, **kwargs):
            raise HookFailedError('hook')

        switch.side_effect = foo
        await worker.run_switch([coordinator.Dataset('dataset3', '2')])
        check_directories_are_same(dataset3_hardlink_path, dataset3_2_content_path)


@asynctest.patch("maps.infra.ecstatic.common.worker.lib.ymtorrent_client.YmtorrentClient.sync_torrents", Mock())
@pytest.mark.asyncio
async def test_add_hardlink_for_already_activated_dataset(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=False
    )
    with open(os.path.join(work_dir.base, 'torrents.pb'), 'rb') as torrents_pb:
        torrents_collection = coordinator.TorrentsCollection(torrents_pb.read())

    worker.merge_torrents(torrents_collection, True)

    dataset3_2_content_path = os.path.join(work_dir.content, 'f6bbd4556968b13725b513440eaf98d9ef3a95d7')
    dataset3_hardlink_path = os.path.join(work_dir.preserved_data, 'dataset3')

    with open(os.path.join(dataset3_2_content_path, 'file'), 'w') as file:
        file.write('dataset3=2')

    await worker.run_switch([coordinator.Dataset('dataset3', '2')])
    assert not os.listdir(work_dir.preserved_data)
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=True
    )
    check_directories_are_same(dataset3_hardlink_path, dataset3_2_content_path)


def test_local_lock(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=False
    )

    with local_lock.LocalLockContextDecorator():
        with pytest.raises(local_lock.LocalLockContextDecorator.TimeoutException):
            asyncio.run(worker.run_switch([coordinator.Dataset('dataset3', '2')]))


def test_hook_controller_find_async_hooks(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    data_storage = DataStorageProxy([work_dir])
    hook_controller = HookController(conf_dir, data_storage, {'check': 300, 'remove': 300, 'postdl': 300, 'switch': 5})
    with pytest.raises(AssertionError):
        hook_controller.has_async_postdl(coordinator.Dataset('dataset5', '1.0'))

    assert hook_controller.has_async_postdl(coordinator.Dataset('dataset3', '1.0')) is False
    assert hook_controller.has_async_postdl(coordinator.Dataset('dataset4', '1.0')) is True


@patch('maps.infra.ecstatic.tool.ecstatic_api.coordinator.Coordinator', Mock())
def test_call_async_hook(work_dir):
    conf_dir = os.path.join(work_dir.base, 'config')
    worker = EcstaticWorker(
        conf_dir=conf_dir,
        status_dir=os.path.join(work_dir.base, 'status'),
        storages=[work_dir],
        socket_path="socket",
        hook_timeouts=DEFAULT_HOOK_TIMEOUTS,
        enable_preserved_data=False
    )
    worker.run_async_postdl(coordinator.Dataset('dataset4', '1.0'))
    time.sleep(2)
    assert os.path.exists(os.path.join(work_dir.data, 'dataset4', 'file1'))
