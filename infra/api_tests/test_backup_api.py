import inject
import mock
import pytest
from infra.qyp.proto_lib import accounts_api_pb2, vmset_pb2
from yt.packages.requests import exceptions as yt_request_exceptions

from infra.qyp.account_manager.src.web import account_service
from infra.swatlib.auth import staff
from infra.swatlib.rpc import exceptions


def test_list_backup(ctx_mock, call):
    def configure(binder):
        binder.bind(staff.IStaffClient, mock.Mock())

    inject.clear_and_configure(configure)
    backup1 = vmset_pb2.Backup()
    backup2 = vmset_pb2.Backup()
    backup1.spec.vm_id = 'existing_vm'
    backup2.spec.vm_id = 'removed_vm'

    # Case 1: no vm_id
    req = accounts_api_pb2.ListBackupRequest()
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        with pytest.raises(exceptions.BadRequestError):
            call(account_service.list_backup, req)

    # Case 2: no cluster
    req.vm_id = 'test_id'
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        with pytest.raises(exceptions.BadRequestError):
            call(account_service.list_backup, req)

        # Case 3: with cluster
        ctx_mock.qdm_client.backup_list.return_value = [backup1, backup2]
        pod_ctl = mock.Mock()
        pod_ctl.get_existing_pods.return_value = {backup1.spec.vm_id}
        req.cluster = 'TEST_SAS'
        ctx_mock.pod_ctl_map['TEST_SAS'] = pod_ctl
        call(account_service.list_backup, req)
        assert backup1.status.vm_alive_status == vmset_pb2.BackupStatus.AliveStatus.ALIVE
        assert backup2.status.vm_alive_status == vmset_pb2.BackupStatus.AliveStatus.REMOVE


def test_list_user_backups(ctx_mock, call):
    def configure(binder):
        binder.bind(staff.IStaffClient, mock.Mock())

    inject.clear_and_configure(configure)
    backup1 = vmset_pb2.Backup()
    backup2 = vmset_pb2.Backup()
    backup3 = vmset_pb2.Backup()
    backup1.spec.vm_id = 'existing_vm.test_sas'
    backup2.spec.vm_id = 'removed_vm.test_sas'
    backup3.spec.vm_id = 'unknown_vm.man_pre'
    backup1.spec.cluster = 'TEST_SAS'
    backup2.spec.cluster = 'TEST_SAS'
    backup3.spec.cluster = 'MAN_PRE'

    # Case 1: no login
    req = accounts_api_pb2.ListUserBackupsRequest()
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        with pytest.raises(exceptions.BadRequestError):
            call(account_service.list_user_backups, req)

    # Case 2: ok
    req.query.login = 'test_user'
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        req.clusters.append('TEST_SAS')
        req.clusters.append('MAN_PRE')
        ctx_mock.qdm_client.user_backup_list.return_value = [backup1, backup2, backup3]

        pod_ctl = mock.Mock()
        pod_ctl.get_existing_pods.return_value = {backup1.spec.vm_id}
        ctx_mock.pod_ctl_map['TEST_SAS'] = pod_ctl
        pod_ctl_dead = mock.Mock()
        pod_ctl_dead.get_existing_pods.side_effect = yt_request_exceptions.RequestException()
        ctx_mock.pod_ctl_map['MAN_PRE'] = pod_ctl_dead

        rsp = call(account_service.list_user_backups, req)
        assert len(rsp.backups) == 3
        assert backup1.status.vm_alive_status == vmset_pb2.BackupStatus.AliveStatus.ALIVE
        assert backup2.status.vm_alive_status == vmset_pb2.BackupStatus.AliveStatus.REMOVE
        assert backup3.status.vm_alive_status == vmset_pb2.BackupStatus.AliveStatus.UNKNOWN

    # Case 3: alive filter
    with mock.patch('infra.qyp.account_manager.src.web.account_service.get_context', return_value=ctx_mock):
        req.clusters.append('TEST_SAS')
        req.clusters.append('MAN_PRE')
        req.query.vm_alive = vmset_pb2.BackupFindQuery.ALIVE_ONLY
        ctx_mock.qdm_client.user_backup_list.return_value = [backup1, backup2, backup3]

        pod_ctl = mock.Mock()
        pod_ctl.get_existing_pods.return_value = {backup1.spec.vm_id}
        ctx_mock.pod_ctl_map['TEST_SAS'] = pod_ctl
        pod_ctl_dead = mock.Mock()
        pod_ctl_dead.get_existing_pods.side_effect = yt_request_exceptions.RequestException()
        ctx_mock.pod_ctl_map['MAN_PRE'] = pod_ctl_dead

        rsp = call(account_service.list_user_backups, req)
        assert len(rsp.backups) == 1
        assert rsp.backups[0].spec.vm_id == 'existing_vm.test_sas'
