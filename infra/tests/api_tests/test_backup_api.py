import datetime

import mock
from infra.qyp.proto_lib import vmset_api_pb2, vmset_pb2
from infra.qyp.vmproxy.src.lib.yp import yputil
from infra.qyp.vmproxy.src.web import vmset_service

import freezegun
import inject
import pytest
import yp.data_model as data_model
from infra.swatlib.auth import staff
from infra.swatlib.rpc import exceptions


def test_list_backup(ctx_mock, call):
    pod = data_model.TPod()
    ctx_mock.pod_ctl.get_pod.return_value = pod
    req = vmset_api_pb2.ListBackupRequest()
    req.vm_id = 'real-pod-id'
    backup_1 = vmset_pb2.Backup()
    backup_1.meta.id = '1'
    backup_2 = vmset_pb2.Backup()
    backup_2.meta.id = '2'
    backup_3 = vmset_pb2.Backup()
    backup_3.meta.id = '3'
    ctx_mock.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(x) for x in (backup_1, backup_2, backup_3)]
    rsp = call(vmset_service.list_backup, req)
    assert [x.meta.id for x in rsp.backups] == ['1', '2', '3']

    ctx_mock.reset_mock()
    with pytest.raises(exceptions.BadRequestError):
        req.vm_id = ''
        call(vmset_service.list_backup, req)


def test_list_user_backups(ctx_mock, call):
    staff_mock = mock.Mock()

    def configure(binder):
        binder.bind(staff.IStaffClient, staff_mock)

    inject.clear_and_configure(configure)
    day1 = datetime.datetime(2020, 1, 1)
    day2 = datetime.datetime(2020, 2, 1)
    backup_1 = vmset_pb2.Backup()
    backup_1.meta.id = '1'
    backup_1.meta.creation_time.FromDatetime(day1)
    backup_1.spec.vm_id = 'test-1.test_sas'
    backup_1.spec.vm_meta.id = 'test-1'
    backup_1.spec.vm_spec.qemu.node_segment = 'default'
    backup_2 = vmset_pb2.Backup()
    backup_2.meta.id = '2'
    backup_2.meta.creation_time.FromDatetime(day2)
    backup_2.spec.vm_id = 'test-1.test_sas'
    backup_2.spec.vm_meta.id = 'test-1'
    backup_2.spec.vm_spec.qemu.node_segment = 'default'
    backup_3 = vmset_pb2.Backup()
    backup_3.meta.id = '3'
    backup_3.meta.creation_time.FromDatetime(day2)
    backup_3.spec.vm_id = 'test-3.test_sas'
    backup_3.spec.vm_meta.id = 'test-3'
    backup_3.spec.vm_spec.qemu.node_segment = 'dev'
    backup_4 = vmset_pb2.Backup()
    backup_4.meta.id = '4'
    backup_4.meta.creation_time.FromDatetime(day2)
    backup_4.spec.vm_id = 'test-4.test_sas'
    backup_4.spec.vm_meta.id = 'test-4'
    backup_4.spec.vm_spec.qemu.node_segment = 'default'
    backup_5 = vmset_pb2.Backup()
    backup_5.meta.id = '5'
    backup_5.meta.creation_time.FromDatetime(day2)
    backup_5.spec.vm_id = 'test-5.sas'
    backup_5.spec.vm_meta.id = 'test-5'
    backup_5.spec.vm_spec.qemu.node_segment = 'default'
    ctx_mock.qdm_client.user_backup_list.return_value = (backup_1, backup_2, backup_3, backup_4, backup_5)
    ctx_mock.pod_ctl.get_alive_pods.return_value = ('test-4',)

    # Case 1: found by user
    req = vmset_api_pb2.ListUserBackupsRequest()
    req.query.login = 'user'
    rsp = call(vmset_service.list_user_backups, req)
    assert [x.meta.id for x in rsp.backups] == ['1', '2', '3', '4']
    # Case 2: filter by vm_name
    req = vmset_api_pb2.ListUserBackupsRequest()
    req.query.login = 'user'
    req.query.vm_name = 'test-1'
    rsp = call(vmset_service.list_user_backups, req)
    assert [x.meta.id for x in rsp.backups] == ['1', '2']
    # Case 3: filter by segment
    req = vmset_api_pb2.ListUserBackupsRequest()
    req.query.login = 'user'
    req.query.segment.append('dev')
    rsp = call(vmset_service.list_user_backups, req)
    assert [x.meta.id for x in rsp.backups] == ['3']
    # Case 4: filter by dates
    req = vmset_api_pb2.ListUserBackupsRequest()
    req.query.login = 'user'
    req.query.creation_time_gte.FromDatetime(datetime.datetime(2020, 1, 1))
    req.query.creation_time_lte.FromDatetime(datetime.datetime(2020, 1, 31))
    rsp = call(vmset_service.list_user_backups, req)
    assert [x.meta.id for x in rsp.backups] == ['1']
    # Case 5: filter by vm_alive
    req = vmset_api_pb2.ListUserBackupsRequest()
    req.query.login = 'user'
    req.query.vm_alive = vmset_pb2.BackupFindQuery.ALIVE_ONLY
    rsp = call(vmset_service.list_user_backups, req)
    assert [x.meta.id for x in rsp.backups] == ['4']


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_backup(ctx_mock, call):
    pod = data_model.TPod()
    ctx_mock.pod_ctl.get_pod.return_value = pod
    req = vmset_api_pb2.CreateBackupRequest()
    req.spec.type = vmset_pb2.BackupSpec.SANDBOX_RESOURCE
    req.spec.vm_id = 'real-pod-id'
    req.spec.sandbox_task_id = '123456'

    expected = vmset_pb2.Backup()
    expected.meta.id = 'new_version'
    expected.meta.creation_time.GetCurrentTime()
    expected.spec.CopyFrom(req.spec)
    expected.status.state = vmset_pb2.BackupStatus.IN_PROGRESS
    # Case 1: already existed
    backup = vmset_pb2.Backup()
    backup.spec.sandbox_task_id = req.spec.sandbox_task_id
    backup_list = [yputil.dumps_proto(backup)]
    ctx_mock.pod_ctl.get_backup_list.return_value = backup_list
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.create_backup, req)
    # Case 2: other backup in progress
    ctx_mock.reset_mock()
    backup = vmset_pb2.Backup()
    backup.status.state = vmset_pb2.BackupStatus.IN_PROGRESS
    backup_list = [yputil.dumps_proto(backup)]
    ctx_mock.pod_ctl.get_backup_list.return_value = backup_list
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.create_backup, req)
    # Case 3: ok, empty list
    ctx_mock.reset_mock()
    ctx_mock.pod_ctl.get_backup_list.return_value = []
    with mock.patch('uuid.uuid4', return_value='new_version'):
        call(vmset_service.create_backup, req)
    ctx_mock.pod_ctl.update_backup_list.assert_called_with(pod, [yputil.dumps_proto(expected)])
    # Case 4: ok again
    ctx_mock.reset_mock()
    backup = vmset_pb2.Backup()
    backup.status.state = vmset_pb2.BackupStatus.COMPLETED
    ctx_mock.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(backup)]
    with mock.patch('uuid.uuid4', return_value='new_version'):
        call(vmset_service.create_backup, req)
    ctx_mock.pod_ctl.update_backup_list.assert_called_with(pod, [yputil.dumps_proto(x) for x in (backup, expected)])
    # Case 5: sb-task exist
    ctx_mock.reset_mock()
    backup.spec.sandbox_task_id = '123456'
    ctx_mock.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(backup)]
    with pytest.raises(exceptions.ConflictError):
        call(vmset_service.create_backup, req)
    # Case 6: user doesn't have access
    ctx_mock.reset_mock()
    ctx_mock.pod_ctl.check_write_permission.return_value = False
    with pytest.raises(exceptions.ForbiddenError):
        call(vmset_service.create_backup, req)
    # Case 7: not exists vm_id in request
    ctx_mock.reset_mock()
    req.spec.sandbox_task_id = ''
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.create_backup, req)
    # Case 8: not exist id in request
    ctx_mock.reset_mock()
    req.spec.vm_id = ''
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.create_backup, req)


def test_update_backup(ctx_mock, call):
    pod = data_model.TPod()
    ctx_mock.pod_ctl.get_pod.return_value = pod
    req = vmset_api_pb2.UpdateBackupRequest()
    req.id = 'new_version'
    req.vm_id = 'real-pod-id'
    req.status.state = vmset_pb2.BackupStatus.COMPLETED
    req.status.url = 'rbtorrent:fake'

    # Case 1: not found
    backup = vmset_pb2.Backup()
    backup.meta.id = 'old_version'
    ctx_mock.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(backup)]
    with pytest.raises(exceptions.NotFoundError):
        call(vmset_service.update_backup, req)
    # Case 2: ok
    ctx_mock.reset_mock()
    backup = vmset_pb2.Backup()
    backup.meta.id = req.id
    backup_1 = vmset_pb2.Backup()
    backup_1.meta.id = 'other_one'
    ctx_mock.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(x) for x in (backup, backup_1)]
    call(vmset_service.update_backup, req)
    backup.status.CopyFrom(req.status)
    ctx_mock.pod_ctl.update_backup_list.assert_called_with(pod, [yputil.dumps_proto(x) for x in (backup, backup_1)])
    # Case 3: user doesn't have access
    ctx_mock.reset_mock()
    ctx_mock.pod_ctl.check_write_permission.return_value = False
    with pytest.raises(exceptions.ForbiddenError):
        call(vmset_service.update_backup, req)
    # Case 4: not exists vm_id in request
    ctx_mock.reset_mock()
    req.vm_id = ''
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.update_backup, req)
    # Case 5: not exist id in request
    ctx_mock.reset_mock()
    req.id = ''
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.update_backup, req)


def test_remove_backup(ctx_mock, call):
    pod = data_model.TPod()
    ctx_mock.pod_ctl.get_pod.return_value = pod
    req = vmset_api_pb2.RemoveBackupRequest()
    req.id = 'new_version'
    req.vm_id = 'real-pod-id'
    # Case 1: not found
    backup = vmset_pb2.Backup()
    backup.meta.id = 'old_version'
    ctx_mock.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(backup)]
    with pytest.raises(exceptions.NotFoundError):
        call(vmset_service.remove_backup, req)
    # Case 2: ok
    ctx_mock.reset_mock()
    backup = vmset_pb2.Backup()
    backup.meta.id = req.id
    backup_1 = vmset_pb2.Backup()
    backup_1.meta.id = 'old_version'
    ctx_mock.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(x) for x in (backup, backup_1)]
    call(vmset_service.remove_backup, req)
    ctx_mock.pod_ctl.update_backup_list.assert_called_with(pod, [yputil.dumps_proto(backup_1)])
    # Case 3: user doesn't have access
    ctx_mock.reset_mock()
    ctx_mock.pod_ctl.check_write_permission.return_value = False
    with pytest.raises(exceptions.ForbiddenError):
        call(vmset_service.remove_backup, req)
    # Case 4: not exists id in request
    ctx_mock.reset_mock()
    req.id = ''
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.remove_backup, req)
    # Case 5: not exists vm_id in request
    ctx_mock.reset_mock()
    req.vm_id = ''
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.remove_backup, req)
