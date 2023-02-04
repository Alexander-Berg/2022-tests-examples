import os
import filelock
import tempfile

from irt.sandbox import Sandbox

try:
    from backports.tempfile import TemporaryDirectory
except ImportError:
    from tempfile import TemporaryDirectory


def run_download_with_mode(mode):
    with TemporaryDirectory() as temp_dir:
        Sandbox().download(1542915282, temp_dir, mode=mode)
        assert os.listdir(temp_dir) == ['fake_resource']
    with TemporaryDirectory() as temp_dir:
        Sandbox().download_irt_resource('fake_resource', temp_dir, mode=mode)
        assert os.listdir(temp_dir) == ['fake_resource']


def test_sandbox():
    with filelock.FileLock(os.path.join(tempfile.gettempdir(), "irt_test_sandbox_copier.lock")):
        run_download_with_mode(mode='skynet')
    run_download_with_mode(mode='rsync')
    run_download_with_mode(mode='mds')
    run_download_with_mode(mode='http')

    with TemporaryDirectory() as temp_dir:
        Sandbox().download(1466441473, temp_dir, mode='mds')
        assert os.listdir(temp_dir) == ['graph.json']
    with TemporaryDirectory() as temp_dir:
        Sandbox().download(1466441473, temp_dir, mode='http')
        assert os.listdir(temp_dir) == ['graph.json']


def test_download_irt_resource_negative():
    with TemporaryDirectory() as temp_dir:
        resource = Sandbox().download_irt_resource('fake_resource', temp_dir, fake_attribute='I do not exist')
        assert resource is None


def test_attrs():
    assert Sandbox().attribute(1405043722, 'active') == '{}'.format(True)


def upload_example():
    # res_id = Sandbox('some_token').upload_irt_resource('path/to/file',
    #                                                    'file_name_in_sandbox',
    #                                                    'title',
    #                                                    'Description',
    #                                                    'ACCOUNT',
    #                                                    ttl=1,
    #                                                    str_param='string_value',
    #                                                    bool_param=False)
    # assert res_id is not None
    raise RuntimeError('Just example')
