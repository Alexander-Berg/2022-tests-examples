import mock
import pytest
import subprocess

from infra.qyp.vmagent.src import resource_manager
from infra.qyp.proto_lib import qdm_pb2


def test_skynet_cli():
    sky_net_cli = resource_manager.SkynetCli()
    fake_resource = 'rbtorrent:any_url'
    fake_path = '/home'
    fake_file_path = '/home/test_file'

    with mock.patch('subprocess.check_output') as check_output_mock:
        # case: get resource success
        sky_net_cli.get(fake_resource, fake_path)
        check_output_mock.assert_called_with(['sky', "get", "-u", "-d", fake_path, fake_resource],
                                             stderr=subprocess.STDOUT)

        # case: share resource success
        check_output_mock.return_value = fake_resource
        res = sky_net_cli.share(fake_file_path)
        check_output_mock.assert_called_with(['sky', 'share', '-d', '/home', 'test_file'])
        assert res == fake_resource

        # case: get resource failed
        check_output_mock.side_effect = subprocess.CalledProcessError(6, "sky get")
        with pytest.raises(sky_net_cli.SkynetError) as error:
            sky_net_cli.get(fake_resource, fake_path)
        assert error.value.message == 'sky get error: 6, None'

        # case: share resource failed
        check_output_mock.side_effect = subprocess.CalledProcessError(6, "sky get")
        with pytest.raises(sky_net_cli.SkynetError) as error:
            sky_net_cli.share(fake_file_path)
        assert error.value.message == 'sky share error: 6, None'


def test_qdm_cli(tmpdir):
    extras_folder = tmpdir.join('extras_folder')
    sky_net_cli_mock = mock.Mock()
    qdm_cli = resource_manager.QDMCli(str(extras_folder), sky_net_cli_mock)
    assert qdm_cli.vmagent_version == 'unknown'

    qdm_cli = resource_manager.QDMCli(str(extras_folder), sky_net_cli_mock, '___VMAGENT_VERSION___')
    assert qdm_cli.vmagent_version == 'dev'

    qdm_cli = resource_manager.QDMCli(str(extras_folder), sky_net_cli_mock, '0.28')
    assert qdm_cli.vmagent_version == '0.28'

    # case: test get_qdm_cli
    with mock.patch('requests.get') as requests_get_mock:
        requests_ret = mock.Mock()
        requests_ret.text = 'rbtorrent:qdm_cli_res_id'
        requests_get_mock.return_value = requests_ret

        with pytest.raises(qdm_cli.QdmError) as error:
            qdm_cli.get_qdm_cli('up', 'fake')
        assert error.value.message == 'Unable to find downloaded qdm-mds-cli'

        def sky_net_get(resource_url, target_dir):
            extras_folder.join('qdm-mds-cli').write('1', ensure=True)

        sky_net_cli_mock.get.side_effect = sky_net_get

        res = qdm_cli.get_qdm_cli('up', 'fake')
        sky_net_cli_mock.get.assert_called_with('rbtorrent:qdm_cli_res_id', str(extras_folder))
        requests_get_mock.assert_called_with(
            'http://qdm.yandex-team.ru/api/v1/client_resource?op=up&key=fake&vmagent=0.28')
        assert res == str(extras_folder.join('qdm-mds-cli'))

    # case: test get
    with mock.patch('subprocess.check_output') as check_output_mock:
        qdm_cli.get_qdm_cli = mock.Mock()
        qdm_cli.get_qdm_cli.return_value = str(extras_folder.join('qdm-mds-cli'))
        log_file_path = str(tmpdir.join('logfile'))
        progress_file_path = str(tmpdir.join('progress_file'))
        result_dir_path = str(tmpdir)
        # case: test get success
        qdm_cli.get('qdm:test', result_dir_path, log_file_path, progress_file_path,
                    'test_vm_id', 'test_cluster', 'test_node_id')
        check_output_mock.assert_called_with([
            str(extras_folder.join('qdm-mds-cli')),
            '--logfile', log_file_path,
            '--progress-file', progress_file_path,
            'download',
            '--rev', 'qdm:test',
            '--vm-id', 'test_vm_id',
            '--cluster', 'test_cluster',
            '--node-id', 'test_node_id',
            result_dir_path
        ], stderr=subprocess.STDOUT)

        # case: test get error
        check_output_mock.side_effect = subprocess.CalledProcessError(1, 'test')
        with pytest.raises(qdm_cli.QdmError):
            qdm_cli.get('qdm:test', result_dir_path, log_file_path, progress_file_path,
                        'test_vm_id', 'test_cluster', 'test_node_id')

    # case: test upload
    with mock.patch('subprocess.Popen') as subprocess_mock:
        qdm_cli.get_qdm_cli = mock.Mock()
        log_file_path = str(tmpdir.join('logfile'))
        qdm_cli.get_qdm_cli.return_value = str(extras_folder.join('qdm-mds-cli'))
        upload_request = qdm_pb2.QDMBackupSpec()
        upload_request.qdm_spec_version = 1
        qdm_cli.upload('fake', upload_request, log_file_path, progress_file_path)

        subprocess_mock.assert_called_with(
            [
                str(extras_folder.join('qdm-mds-cli')),
                '--logfile', log_file_path,
                '--progress-file', progress_file_path,
                'upload',
                '--qdm-key', 'fake',
                '--spec', '{\n"qdm_spec_version": 1\n}'
            ],
            stdin=subprocess.PIPE, stderr=subprocess.STDOUT, stdout=subprocess.PIPE
        )

        # test upload cancel
        failed_proc = mock.Mock()
        failed_proc.returncode = -9
        failed_proc.communicate.return_value = ('', '')

        subprocess_mock.return_value = failed_proc
        with pytest.raises(qdm_cli.QdmError):
            qdm_cli.upload('fake', upload_request, log_file_path, progress_file_path)


def test_resource_manager(tmpdir):
    extras_folder = tmpdir.join('extras_folder')
    logs_folder = tmpdir.join('logs_folder')
    target_folder = tmpdir.join('target_folder')
    source_file = tmpdir.join('source.file')
    _resource_manager = resource_manager.ResourceManager('test_vm_id', 'test_cluster', 'test_node_id',
                                                         str(extras_folder), str(logs_folder), '0.28')
    _resource_manager._skynet_cli = mock.Mock()
    _resource_manager._qdm_cli = mock.Mock()

    # case: get resource qdm
    _resource_manager.get_resource('qdm:test', target_folder)
    _resource_manager._qdm_cli.get.assert_called_with('qdm:test', target_folder, str(logs_folder.join('qdm.log')),
                                                      str(extras_folder.join('data_transfer_state.json')), 'test_vm_id',
                                                      'test_cluster', 'test_node_id')

    # case: get resource rbtorrent
    _resource_manager.get_resource('rbtorrent:test', target_folder)
    _resource_manager._skynet_cli.get.assert_called_with('rbtorrent:test', target_folder)

    # case: upload_resource
    upload_spec = mock.Mock()
    _resource_manager.upload_resource('fake', upload_spec)
    _resource_manager._qdm_cli.upload.assert_called_with('fake', upload_spec, str(logs_folder.join('qdm.log')),
                                                         str(extras_folder.join('data_transfer_state.json')))

    # case: share_resource
    _resource_manager.share_resource(source_file)
    _resource_manager._skynet_cli.share.assert_called_with(source_file)
