import pytest
import json
import click
import mock
from functools import partial
from click.testing import CliRunner, Result

from infra.qyp.vmctl.src import helpers
from infra.qyp.vmctl.src import ctl_options
from infra.qyp.proto_lib import vmset_api_pb2, vmset_pb2, vmagent_pb2


def call_options_cls(options_cls, *args, **kwargs):  # type: (...) -> Result
    @click.command()
    @options_cls.decorate
    def command(**command_kwargs):
        _args = options_cls.build_from_kwargs(command_kwargs)
        print json.dumps(_args.command_options)

    runner = CliRunner()
    return runner.invoke(command, *args, **kwargs)


def test_create_options():
    result = call_options_cls(ctl_options.CreateOptions, [
        '--node-segment', 'dev',
        '--cluster', 'MAN',
        '--default-image', 'xenial',
        '--vcpu', '2',
        '--pod-id', 'test',
        '--logins', 'test-user', 'test-user2',
        '--groups', '12', '12', '34',
        '--abc', 'personal',
    ])
    assert result.exit_code == 0, result.output
    result = json.loads(result.output)

    assert result['logins'] == ['test-user', 'test-user2']
    assert result['groups'] == ['12', '12', '34']
    assert result['type'] == 'linux'
    assert result['network_id'] == '_SEARCHSAND_'
    assert result['rb_torrent']


def test_create_options_tmp_quota():
    options = [
        '--node-segment', 'dev',
        '--cluster', 'MAN',
        '--default-image', 'xenial',
        '--vcpu', '2',
        '--pod-id', 'test',
    ]
    call = partial(call_options_cls, ctl_options.CreateOptions)
    result = call(options)
    assert result.exit_code == 1, result.output
    assert 'You are going to use temporary account' in result.output

    result = call(options, input='y')
    assert result.exit_code == 0, result.output

    result = call(options, input='n')
    assert result.exit_code == 1, result.output

    result = call(options + ['-y'])
    assert result.exit_code == 0, result.output


def test_create(vmctl_invoke, mock_server):
    mock_server.add_proto_response('/api/CreateVm/', vmset_api_pb2.CreateVmResponse())

    result = vmctl_invoke(['create',
                           '--abc', '0',
                           '--node-segment', 'dev',
                           '--cluster', 'MAN',
                           '--network-id', '_SEARCHSAND_',
                           '--default-image', 'xenial',
                           '--vcpu', '2',
                           '--pod-id', 'test',
                           ])
    assert result.exit_code == 0, result.output


def test_create_with_personal_quota(vmctl_invoke, mock_server):
    list_user_accounts_resp = vmset_api_pb2.ListUserAccountsResponse()
    personal_account = list_user_accounts_resp.accounts.add()
    personal_account.type = vmset_pb2.Account.PERSONAL
    limits = helpers.UsageAcc(
        cpu=8000,
        hdd=1000 * (1024 ** 3),
        ssd=1000 * (1024 ** 3),
        mem=24 * (1024 ** 3)
    )
    personal_account.personal.limits.per_segment[helpers.PersonalAccountHelper.DEV_SEGMENT].CopyFrom(
        limits.to_resource_info())

    mock_server.add_proto_response('/api/ListUserAccounts/', list_user_accounts_resp)
    mock_server.add_proto_response('/api/CreateVm/', vmset_api_pb2.CreateVmResponse())

    result = vmctl_invoke(['create',
                           '--cluster', 'MAN',
                           '--abc', 'personal',
                           '--pod-id', 'test',
                           '--node-segment', 'dev',
                           '--default-image', 'xenial',
                           '--vcpu', '2',
                           ])
    assert result.exit_code == 0, result.output


def test_list_vm_options():
    result = call_options_cls(ctl_options.ListVmOptions, [
        '--mode', 'yp',
        '--format', 'json',
        '--cluster', 'MAN', 'SAS', 'VLA',
        '--login', 'test',
        '--login-all',
        '--node-segment', 'dev', 'default',
        '--fields', 'name', 'location', 'segment',
        '--abc', 'personal', '1', '2',
        '--unused-only',
    ])
    assert result.exit_code == 0, result.output
    result = json.loads(result.output)

    assert result['login'] == 'test'
    assert result['list_yp_cluster'] == ['MAN', 'SAS', 'VLA']
    assert result['node_segment'] == ['dev', 'default']
    assert result['fields'] == ['name', 'location', 'segment']
    assert result['abc'] == ['personal', '1', '2']
    assert result['unused_only']


def test_list_vm(vmctl_invoke, mock_server):
    list_vms_response = vmset_api_pb2.ListYpVmResponse()
    vm = list_vms_response.vms.add()  # type: vmset_pb2.VM
    vm.meta.id = 'test'
    vm.spec.qemu.node_segment = 'dev'
    vm.spec.qemu.resource_requests.vcpu_guarantee = 1
    vm.spec.qemu.resource_requests.memory_guarantee = 100
    volume = vm.spec.qemu.volumes.add()
    volume.name = '/qemu-persistent'
    volume.capacity = 100 * (1024 ** 3)
    vm.meta.auth.owners.logins.extend(['test', 'test2'])
    vm.meta.auth.owners.group_ids.extend(['1', '2'])
    vm.spec.account_id = '1'
    vm.spec.vmagent_version = '1.29'
    mock_server.add_proto_response('/api/ListYpVm/', list_vms_response)

    result = vmctl_invoke(['list', '--format', 'json'])
    assert result.exit_code == 0, result.output

    try:
        json_result = json.loads(result.output)
    except Exception as e:
        json_result = []
    assert isinstance(json_result, list) and len(json_result) == 1, result.output

    result = vmctl_invoke(['list', '--format', 'text'])
    assert result.exit_code == 0, result.output


def test_allocate(vmctl_invoke, mock_server):
    allocate_response = vmset_api_pb2.AllocateVmResponse()
    mock_server.add_proto_response('/api/AllocateVm/', allocate_response)

    result = vmctl_invoke(['allocate',
                           '--abc', '0',
                           '--node-segment', 'dev',
                           '--cluster', 'MAN',
                           '--network-id', '__SEARCHSAND__',
                           '--pod-id', 'test',
                           ])

    assert result.exit_code == 0, result.output


def test_dealocate(vmctl_invoke, mock_server):
    deallocate_response = vmset_api_pb2.DeallocateVmResponse()
    mock_server.add_proto_response('/api/DeallocateVm/', deallocate_response)

    result = vmctl_invoke(['deallocate', '--pod-id', 'test', '--cluster', 'MAN', '-y'])
    assert result.exit_code == 0, result.output


def test_update(vmctl_invoke, mock_server):
    vm = vmset_pb2.VM()
    vm.meta.id = 'test'
    vm.spec.qemu.node_segment = 'dev'
    vm.spec.qemu.resource_requests.vcpu_guarantee = 1
    vm.spec.qemu.resource_requests.memory_guarantee = 100
    volume = vm.spec.qemu.volumes.add()
    volume.name = '/qemu-persistent'
    volume.capacity = 100 * (1024 ** 3)
    vm.meta.auth.owners.logins.extend(['test', 'test2'])
    vm.meta.auth.owners.group_ids.extend(['1', '2'])
    vm.spec.account_id = '1'
    vm.spec.vmagent_version = '1.29'
    personal_acc = vmset_pb2.Account(type=vmset_pb2.Account.PERSONAL)
    personal_acc.personal.limits.per_segment['dev'].cpu = 2000
    personal_acc.personal.limits.per_segment['dev'].mem = 8196
    personal_acc.personal.limits.per_segment['dev'].disk_per_storage['ssd'] = 307200

    get_response = vmset_api_pb2.GetVmResponse(vm=vm)
    update_response = vmset_api_pb2.UpdateVmResponse(vm=vm)
    list_user_accounts_response = vmset_api_pb2.ListUserAccountsResponse(accounts=[personal_acc])

    mock_server.add_proto_response('/api/GetVm/', get_response)
    mock_server.add_proto_response('/api/UpdateVm/', update_response)
    mock_server.add_proto_response('/api/ListUserAccounts/', list_user_accounts_response)

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--logins', 'test', 'test'
                           ])
    assert result.exit_code == 0, result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           ], input='y')
    assert result.exit_code == 0, result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '-y'
                           ])
    assert result.exit_code == 0, result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--network-id', '_NETS_'
                           ])
    assert result.exit_code == 1, result.output
    assert 'VM will be stopped during this action' in result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--default-image', 'xenial'
                           ])
    assert result.exit_code == 1, result.output
    assert 'All data in selected virtual machine will be deleted' in result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--node-id', 'man1-1234.search.yandex.net'
                           ])
    assert result.exit_code == 1, result.output
    assert 'All data in selected virtual machine will be deleted' in result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--remove-node-id'
                           ])
    assert result.exit_code == 1, result.output
    assert 'All data in selected virtual machine will be deleted' in result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--node-id', 'man1-1234.search.yandex.net',
                           '--abc', 'personal',
                           '-y',
                           ])
    assert result.exit_code == 2, result.output
    assert 'Forcing node in personal quota is not allowed' in result.output


def test_denial_of_update(vmctl_invoke, mock_server):
    vm = vmset_pb2.VM()
    vm.meta.id = 'test'
    vm.spec.vmagent_version = '0.1'
    get_response = vmset_api_pb2.GetVmResponse(vm=vm)
    mock_server.add_proto_response('/api/GetVm/', get_response)

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--vcpu', '20',
                           '-y',
                           ])
    assert result.exit_code == 2, result.output
    assert 'Vmagent version is too old, please update to the latest version first' in result.output


def test_update_volumes_confirmation(vmctl_invoke, mock_server):
    vm = vmset_pb2.VM()
    vm.meta.id = 'test'
    vm.spec.vmagent_version = '1.0'
    volume = vm.spec.qemu.volumes.add()
    volume.name = '/qemu-persistent'
    volume.capacity = 100 * (1024 ** 3)
    volume = vm.spec.qemu.volumes.add()
    volume.name = 'disk1'
    volume.capacity = 100 * (1024 ** 3)
    volume = vm.spec.qemu.volumes.add()
    volume.name = 'disk2'
    volume.capacity = 100 * (1024 ** 3)
    volume = vm.spec.qemu.volumes.add()
    volume.name = 'disk3'
    volume.capacity = 100 * (1024 ** 3)
    get_response = vmset_api_pb2.GetVmResponse(vm=vm)
    mock_server.add_proto_response('/api/GetVm/', get_response)

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--extra-volume', 'name=disk1,size=100G,storage=ssd,image=rbtorrent:2',
                           ], input='y')
    assert result.exit_code == 1, result.output
    assert "All data in changed volumes ['disk1'] will be deleted" in result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--remove-volumes', 'disk1 disk2 disk100',
                           ], input='y')
    assert result.exit_code == 1, result.output
    assert "Volumes ['disk1', 'disk2'] will be removed, all data will be lost" in result.output

    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--extra-volume', 'name=disk3,size=100G,storage=ssd,image=rbtorrent:2',
                           '--remove-volumes', 'disk1 disk2 disk100',
                           ], input='y')
    assert result.exit_code == 1, result.output
    assert "Volumes ['disk1', 'disk2'] will be removed, all data will be lost" in result.output
    assert "All data in changed volumes ['disk3'] will be deleted" in result.output

    FILENAME = 'disk.yaml'
    content = """
    volumes:
      - name: disk1
        size: 100G
        storage: ssd
        image: rbtorrent:1
      - name: disk2
        size: 100G
        storage: ssd
        image: rbtorrent:2
    """
    with open(FILENAME, 'w') as f:
        f.write(content)
    result = vmctl_invoke(['update',
                           '--pod-id', 'test',
                           '--cluster', 'MAN',
                           '--extra-volumes-conf', FILENAME,
                           ], input='y')
    assert result.exit_code == 1, result.output
    assert "All data in changed volumes ['disk1', 'disk2'] will be deleted" in result.output
    assert "Volumes ['disk3'] will be removed, all data will be lost" in result.output


def test_list_accounts(vmctl_invoke, mock_server):
    list_accounts_response = vmset_api_pb2.ListUserAccountsResponse()
    mock_server.add_proto_response('/api/ListUserAccounts/', list_accounts_response)

    result = vmctl_invoke(['list-accounts'])
    assert result.exit_code == 0, result.output


def test_hostname(vmctl_invoke):
    result = vmctl_invoke(['hostname', '--cluster', 'MAN', '--pod-id', 'test'])
    assert result.exit_code == 0, result.output
    assert result.output.strip() == 'test.man.yp-c.yandex.net'

    result = vmctl_invoke(['hostname', '--host', 'test.yandex.ru'])
    assert result.exit_code == 0, result.output
    assert result.output.strip() == 'test.yandex.ru'


def test_status(vmctl_invoke, mock_server):
    get_status_response = vmset_api_pb2.GetStatusResponse()
    mock_server.add_proto_response('/api/GetStatus/', get_status_response)

    result = vmctl_invoke([
        'status',
        '--pod-id', 'test',
        '--cluster', 'MAN'
    ])

    assert result.exit_code == 0, result.output


def test_start_with_backup_in_progress(vmctl_invoke, mock_server):
    make_action_response = vmset_api_pb2.MakeActionResponse()
    mock_server.add_proto_response('/api/MakeAction/', make_action_response)

    list_backup_response = vmset_api_pb2.ListBackupResponse()
    _backup = list_backup_response.backups.add()
    _backup.status.state = vmset_pb2.BackupStatus.IN_PROGRESS
    mock_server.add_proto_response('/api/ListBackup/', list_backup_response)
    args = [
        'start',
        '--pod-id', 'test',
        '--cluster', 'MAN'
    ]

    result = vmctl_invoke(args)

    assert result.exit_code == 1
    assert 'Backup action are in progress now. It will fail after VM start' in result.output

    result = vmctl_invoke(args, 'y')

    assert result.exit_code == 0, result.output


@pytest.mark.parametrize('action', ['shutdown', 'poweroff', 'rescue', 'reset', 'revert', 'share-image'])
def test_vm_actions(vmctl_invoke, mock_server, action):
    make_action_response = vmset_api_pb2.MakeActionResponse()
    mock_server.add_proto_response('/api/MakeAction/', make_action_response)
    args = [
        action,
        '--pod-id', 'test',
        '--cluster', 'MAN'
    ]
    if action in ('revert', 'share-image'):
        args.append('-y')

    result = vmctl_invoke(args)

    assert result.exit_code == 0, result.output


@pytest.mark.parametrize('action', ['start', 'shutdown', 'poweroff', 'rescue', 'reset', 'revert', 'share-image'])
def test_vm_actions_direct(vmctl_invoke, mock_server, action):
    mock_server.add_json_response('/action', {}, methods=('POST',))

    args = [
        action,
        '--direct',
        '--host', mock_server.host,
        '--port', mock_server.port,
    ]

    if action in ('revert', 'share-image'):
        args.append('-y')

    result = vmctl_invoke(args)

    assert result.exit_code == 0, result.output


def test_config(vmctl_invoke, mock_server):
    make_action_response = vmset_api_pb2.MakeActionResponse()
    mock_server.add_proto_response('/api/MakeAction/', make_action_response)

    result = vmctl_invoke([
        'config',
        '--pod-id', 'test',
        '--cluster', 'MAN',
        '-c', '1',
        '-m', '10',
        '--default-image', 'xenial'
    ])

    assert result.exit_code == 1, result.output

    result = vmctl_invoke([
        'config',
        '--pod-id', 'test',
        '--cluster', 'MAN',
        '-c', '1',
        '-m', '10',
        '--default-image', 'xenial',
        '-y'
    ])

    assert result.exit_code == 0, result.output


def test_backup(vmctl_invoke, mock_server):
    backup_response = vmset_api_pb2.BackupVmResponse()
    mock_server.add_proto_response('/api/BackupVm/', backup_response)

    result = vmctl_invoke([
        'backup',
        '--pod-id', 'test',
        '--cluster', 'MAN',
    ])

    assert result.exit_code == 1, result.output
    assert 'VM will be stopped during this action' in result.output

    vm = vmset_pb2.VM()
    vm.spec.vmagent_version = '1.29'

    get_response = vmset_api_pb2.GetVmResponse(vm=vm)
    mock_server.add_proto_response('/api/GetVm/', get_response)

    result = vmctl_invoke([
        'backup',
        '--pod-id', 'test',
        '--cluster', 'MAN',
        '-y'
    ])

    assert result.exit_code == 0, result.output


def test_backup_with_update_vmagent(vmctl_invoke, mock_server):
    vm = vmset_pb2.VM()
    vm.spec.vmagent_version = '0.14'

    get_response = vmset_api_pb2.GetVmResponse(vm=vm)
    mock_server.add_proto_response('/api/GetVm/', get_response)

    status_resp = vmset_api_pb2.GetStatusResponse()
    status_resp.state.type = vmagent_pb2.VMState.RUNNING
    mock_server.add_proto_response('/api/GetStatus/', status_resp)

    update_response = vmset_api_pb2.UpdateVmResponse()
    mock_server.add_proto_response('/api/UpdateVm/', update_response)

    backup_response = vmset_api_pb2.BackupVmResponse()
    mock_server.add_proto_response('/api/BackupVm/', backup_response)

    result = vmctl_invoke([
        '--verbose',
        'backup',
        '--pod-id', 'test',
        '--cluster', 'MAN',
        '-y'
    ])

    assert result.exit_code == 0, result.output


def test_list_backup(vmctl_invoke, mock_server):
    list_backup_response = vmset_api_pb2.ListBackupResponse()
    mock_server.add_proto_response('/api/ListBackup/', list_backup_response)

    result = vmctl_invoke([
        'list-backup',
        '--pod-id', 'test',
        '--cluster', 'MAN',
    ])

    assert result.exit_code == 0, result.output


def test_remove_backup(vmctl_invoke, mock_server):
    remove_backup_response = vmset_api_pb2.RemoveBackupResponse()

    mock_server.add_proto_response('/api/RemoveBackup/', remove_backup_response)

    result = vmctl_invoke([
        'remove-backup',
        '--id', '1',
        '--pod-id', 'test',
        '--cluster', 'MAN',
    ])

    assert result.exit_code == 1, result.output

    result = vmctl_invoke([
        'remove-backup',
        '--id', '1',
        '--pod-id', 'test',
        '--cluster', 'MAN',
        '-y'
    ])

    assert result.exit_code == 0, result.output


def test_vnc(vmctl_invoke, mock_server):
    get_status_response = vmset_api_pb2.GetStatusResponse()
    get_status_response.config.access_info.vnc_password = '1111'
    get_status_response.state.type = vmagent_pb2.VMState.RUNNING
    mock_server.add_proto_response('/api/GetStatus/', get_status_response)
    webbrowser_mock = mock.Mock()
    with mock.patch('webbrowser.open_new', webbrowser_mock):
        result = vmctl_invoke([
            'vnc',
            '--pod-id', 'test',
            '--cluster', 'MAN',
        ])
        assert result.exit_code == 0, result.output
        webbrowser_mock.assert_called_with(
            'https://rtc-kvm-novnc.yandex-team.ru/vnc.html?path=?cluster%3DMAN%26pod_id%3Dtest&password=1111')


def test_ui(vmctl_invoke):
    webbrowser_mock = mock.Mock()
    with mock.patch('webbrowser.open_new', webbrowser_mock):
        result = vmctl_invoke([
            'ui'
        ])
        assert result.exit_code == 0, result.output
        webbrowser_mock.assert_called_with('https://qyp.yandex-team.ru')
