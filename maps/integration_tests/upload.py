import filecmp
import glob
import os

import yatest


def test_uploaded_structure(storage, ecstatic_tool):
    dataset_dir = os.path.join(yatest.common.work_path(), 'dataset_dir')
    os.mkdir(dataset_dir)
    with open(os.path.join(dataset_dir, 'file'), 'wb') as file:
        file.write(b'11232141242351223534645756851235467632456782435678')

    ecstatic_tool.upload_dataset(
        dataset_name='pkg-a',
        dataset_version='1.0',
        directory=dataset_dir,
        branches=['+stable'])

    data_dir = f'{storage.storage_dir}/yandex/maps/ecstatic'

    torrent_hash = '29edbad92fba706e9ae98ed5f68613a296d091d5'
    needed_content = {
        '/', '/data', '/preserved_data', '/versions',
        '/content',
        f'/content/{torrent_hash}',
        f'/content/{torrent_hash}/file',
        '/torrents',
        f'/torrents/{torrent_hash}.pb',
        f'/torrents/{torrent_hash}.data',
        f'/torrents/{torrent_hash}.data/file',
    }
    current_content = {
        path.replace(data_dir, '')
        for path in glob.glob(data_dir + '/**', recursive=True)
    }

    assert needed_content == current_content


def test_upload(storage, ecstatic_tool):
    dataset_dir = os.path.join(yatest.common.work_path(), 'dataset')
    os.mkdir(dataset_dir)
    with open(os.path.join(dataset_dir, 'file'), 'wb') as file:
        data = os.urandom(1 << 27)
        for _ in range(10):
            file.write(data)

    ecstatic_tool.upload_dataset(
        dataset_name='pkg-a',
        dataset_version='1.0',
        directory=dataset_dir,
        branches=['+stable'])

    downloaded_dataset_dir = os.path.join(yatest.common.work_path(), 'downloaded_dataset')

    ecstatic_tool.download_datasets(datasets_args=[('pkg-a', '1.0', downloaded_dataset_dir)])

    assert not filecmp.dircmp(dataset_dir, downloaded_dataset_dir).diff_files
    assert not filecmp.dircmp(dataset_dir, downloaded_dataset_dir).left_only
    assert not filecmp.dircmp(dataset_dir, downloaded_dataset_dir).right_only
    with open(os.path.join(dataset_dir, 'file'), 'rb') as source_file,\
            open(os.path.join(downloaded_dataset_dir, 'file'), 'rb') as destination_file:
        assert source_file.read() == destination_file.read()
