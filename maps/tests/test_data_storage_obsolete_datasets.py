import itertools
import os
import pytest
import shutil

from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy, Torrent, StoragePath
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


@pytest.mark.parametrize(
    'need_content_path, need_torrent_path, need_protobuf_file, need_dataset_symlink',
    set(itertools.product((True, False), repeat=4)))
def test_datasets_storage_remove_obsolete_datasets(
        need_content_path, need_torrent_path, need_protobuf_file, need_dataset_symlink,
        data_storage: DataStorageProxy
):
    data_storage.add_torrent(
        Torrent('hash', b'content'),
        {Dataset('ds1', '1')}
    )
    work_dir: StoragePath = data_storage._storages[0]._path
    if not need_content_path:
        shutil.rmtree(os.path.join(work_dir.content, 'hash'))
    if not need_torrent_path:
        os.remove(os.path.join(work_dir.torrents, 'hash.data'))
    if not need_protobuf_file:
        os.remove(os.path.join(work_dir.torrents, 'hash.pb'))
    if not need_dataset_symlink:
        os.remove(os.path.join(work_dir.versions, 'ds1_1'))
    not_delete_files = need_content_path and need_torrent_path and need_protobuf_file and need_dataset_symlink
    if not_delete_files:
        assert data_storage.obsolete_datasets() == {}
        data_storage.remove_obsolete_data()
        assert os.path.exists(os.path.join(work_dir.content, 'hash'))
        assert os.path.exists(os.path.join(work_dir.torrents, 'hash.data'))
        assert os.path.exists(os.path.join(work_dir.torrents, 'hash.pb'))
        assert os.path.exists(os.path.join(work_dir.versions, 'ds1_1'))
        return

    assert data_storage.obsolete_datasets() == {'hash': {Dataset('ds1', '1')}}
