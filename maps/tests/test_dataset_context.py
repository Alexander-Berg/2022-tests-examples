import datetime
import mock
import os
import pytest
import subprocess

from contextlib import contextmanager

import maps.analyzer.pylibs.datasets.datasets as test_module


class MockedDatasetContext(test_module.DatasetContext):
    def __init__(self, check_exists=False):
        super(MockedDatasetContext, self).__init__(
            datetime.date(2021, 1, 10),
            datetime.date(2021, 1, 17)
        )
        self.dir = '/tmp_dir'
        if not check_exists:
            self._check_exists = lambda *args: None


@contextmanager
def popen_mock(returncode):
    orig_popen = subprocess.Popen
    subprocess.Popen = mock.MagicMock()
    process_mock = mock.Mock()
    process_mock.configure_mock(**{
        'wait.return_value': returncode,
        'returncode': returncode,
    })
    subprocess.Popen.return_value = process_mock

    yield process_mock

    subprocess.Popen = orig_popen


def test_add_file():
    with MockedDatasetContext(check_exists=False) as ds:
        assert ds.registered_files == set([])
        ds.add_file('file1.mms')
        ds.add_file('file2.mms')
        assert ds.registered_files == set([os.path.join(ds.dir, 'file1.mms'), os.path.join(ds.dir, 'file2.mms')])


def test_build():
    binary_path = 'path_to_binary'
    to_args_list_return = ['--output', 'path_to_output']

    ytc = mock.Mock()
    ytc.read_table.return_value = []

    params = mock.Mock()
    params.to_args_list.return_value = to_args_list_return

    with popen_mock(0) as proc_mock:
        with MockedDatasetContext(check_exists=False) as ds:
            ds.build(ytc, '//table_path', binary_path, params)
            assert proc_mock.wait.called
            assert proc_mock.returncode == 0

    with popen_mock(1) as proc_mock:
        with pytest.raises(Exception):
            with MockedDatasetContext(check_exists=False) as ds:
                ds.build(ytc, '//table_path', binary_path, params)
                assert proc_mock.wait.called
                assert proc_mock.returncode == 1


@mock.patch('test_dataset_context.test_module.envkit.yt.set_expiration_period')
def test_upload_to_yt(set_exp_per):
    expiration_period = 10
    file_name = 'file.mms'
    yt_destination_root = '//yt_path'
    ytc = mock.Mock()
    with MockedDatasetContext(check_exists=False) as ds:
        ds.add_file(file_name)
        local_file_path = os.path.join(ds.dir, file_name)
        yt_dataset_destination = os.path.join(yt_destination_root, ds.version)
        yt_dest_path = os.path.join(yt_dataset_destination, file_name)
        ds.upload_to_yt(ytc, yt_destination_root, expiration_period)
        ytc.smart_upload_file.assert_called_once_with(
            local_file_path, destination=yt_dest_path, placement_strategy='replace'
        )
        set_exp_per.assert_called_once_with(ytc, yt_dataset_destination, datetime.timedelta(expiration_period))


@mock.patch('test_dataset_context.test_module.set_dates_attributes')
@mock.patch('test_dataset_context.test_module.envkit.yt.copy_with_expiration_period')
def test_save_table(copy_exp_per, set_dates_attr):
    expiration_period = 10
    src_table = '//src_table'
    table_name = 'table_name'
    yt_destination_root = '//yt_path'
    ytc = mock.Mock()
    with MockedDatasetContext(check_exists=False) as ds:
        ds.save_table(ytc, src_table, yt_destination_root, table_name, expiration_period)
        yt_dest_path = os.path.join(yt_destination_root, ds.version, table_name)
        copy_exp_per.assert_called_once_with(ytc, src_table, yt_dest_path, expiration_period)
        set_dates_attr.assert_called_once_with(ytc, yt_dest_path, ds.begin, ds.end)


@mock.patch('test_dataset_context.test_module.copy_to_dataset')
def test_copy_to_dataset(copy_to_dataset):
    ds_name = 'dataset_name'
    file_name = 'file.mms'
    with MockedDatasetContext(check_exists=False) as ds:
        ds.add_file(file_name)
        ds.upload_to_ecstatic(ds_name)
        local_file_path = os.path.join(ds.dir, file_name)
        copy_to_dataset.assert_called_once_with(ds_name, ds.version, local_file_path, file_name)


def test_dataset_context_non_existing():
    with MockedDatasetContext(check_exists=True) as ds:
        ds.add_file('file1.mms')
        ds.add_file('file2.mms')
        with pytest.raises(Exception):
            ds.upload_to_ecstatic('dataset_name')
        with pytest.raises(Exception):
            ytc = mock.Mock()
            ds.upload_to_yt(ytc, '//yt_path')


def test_dataset_context_empty():
    with MockedDatasetContext(check_exists=True) as ds:
        with pytest.raises(Exception):
            ds.upload_to_ecstatic('dataset_name')
        with pytest.raises(Exception):
            ytc = mock.Mock()
            ds.upload_to_yt(ytc, '//yt_path')
