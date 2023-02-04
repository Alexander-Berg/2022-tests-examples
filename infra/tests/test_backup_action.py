import base64
import datetime

import freezegun
import inject
import mock
import pytest
import yp.data_model as data_model
from yt import yson

from infra.swatlib import sandbox
from infra.qyp.proto_lib import vmagent_api_pb2, vmset_pb2, qdm_pb2
from infra.qyp.vmproxy.src import vm_instance
from infra.qyp.vmproxy.src.lib.yp import yputil
from infra.qyp.vmproxy.src.action import backup as backup_action


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_run_vmagent_backup():
    data_pb = vmagent_api_pb2.VMActionRequest()
    data_pb.action = vmagent_api_pb2.VMActionRequest.BACKUP
    encoded = base64.b64encode(data_pb.SerializeToString())
    ctx = mock.Mock()
    ctx.pod_ctl.get_backup_list.return_value = []
    ctx.qdm_client.backup_status.return_value = None
    ctx.qdm_client.backup_list.return_value = []
    sandbox_client = mock.Mock()

    def configure(binder):
        binder.bind(sandbox.ISandboxClient, sandbox_client)

    inject.clear_and_configure(configure)
    # Case 1: gencfg/nanny instance
    host = 'sas1-1234'
    port = '1234'
    instance = vm_instance.HostVMInstance(host, port, 'service')
    backup_action.run(instance, ctx)
    ctx.vmagent_client.action.assert_called_with(url='http://{}:{}'.format(host, port), data=encoded)
    # Case 2: yp pod, no vmagent version
    ctx.reset_mock()
    pod_id = 'pod_id'
    pod = data_model.TPod()
    container_ip = '::1'
    port = '1234'
    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    with pytest.raises(ValueError):
        backup_action.run(instance, ctx)
    # Case 3: old vmagent version
    ctx.reset_mock()
    del pod.labels.attributes[:]
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.14'))
    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    with pytest.raises(ValueError):
        backup_action.run(instance, ctx)
    # Case 4: run vmagent backup
    ctx.reset_mock()
    del pod.labels.attributes[:]
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.15'))
    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    backup_action.run(instance, ctx)
    ctx.vmagent_client.action.assert_called_with(url='http://[{}]:{}'.format(container_ip, port), data=encoded)
    # Case 5: vmagent with sb, but one already in progress
    ctx.reset_mock()
    del pod.labels.attributes[:]
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.18'))
    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    backup = vmset_pb2.Backup()
    backup.status.state = vmset_pb2.BackupStatus.IN_PROGRESS
    ctx.pod_ctl.get_backup_list.return_value = [yputil.dumps_proto(backup)]
    with pytest.raises(ValueError):
        backup_action.run(instance, ctx)
    # Case 6: vmagent with sb, ok
    ctx.reset_mock()
    del pod.labels.attributes[:]
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.18'))
    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    ctx.pod_ctl.get_backup_list.return_value = []
    ctx.vmagent_client.status.return_value = ''
    sandbox_task_id = '12345'
    sandbox_client._client.post.return_value = {'id': sandbox_task_id}
    with mock.patch('uuid.uuid4', return_value='new_version'):
        backup_action.run(instance, ctx)
    sandbox_req = {
        'type': 'BACKUP_QEMU_VM',
        'owner': 'QEMU_BACKUP',
        'description': 'QEMU backup task for pod "{}" from cluster "{}"'.format(
            instance.pod_id, ctx.pod_ctl_factory.cluster
        ),
        'priority': ('SERVICE', 'HIGH'),
        'custom_fields': [
            {
                'name': 'cluster',
                'value': ctx.pod_ctl_factory.cluster,
            },
            {
                'name': 'vm_id',
                'value': instance.pod_id,
            },
            {
                'name': 'storage',
                'value': 'SANDBOX_RESOURCE',
            },
        ],
        'requirements': {
            'disk_space': 1024 ** 3,  # 1Gb
        },
    }
    sandbox_client._client.post.assert_called_with('task', json=sandbox_req)
    sandbox_client._client.put.assert_called_with('batch/tasks/start', json={'id': [sandbox_task_id]})

    backup = vmset_pb2.Backup()
    backup.meta.id = 'new_version'
    backup.meta.creation_time.GetCurrentTime()
    backup.spec.type = vmset_pb2.BackupSpec.SANDBOX_RESOURCE
    backup.spec.vm_id = instance.pod_id
    backup.spec.sandbox_task_id = sandbox_task_id
    backup.spec.generation = 0
    backup.status.state = vmset_pb2.BackupStatus.PLANNED
    ctx.pod_ctl.update_backup_list.assert_called_with(pod, [yputil.dumps_proto(backup)])


def test_rewrite_qemu_volumes_from_backup_spec():
    # case: unsupported qdm backup spec version
    qemu_vm_spec = vmset_pb2.QemuVMSpec()
    backup_spec = qdm_pb2.QDMBackupSpec(qdm_spec_version=1000)
    with pytest.raises(ValueError):
        backup_action.rewrite_qemu_volumes_from_backup_spec(qemu_vm_spec, backup_spec)

    # case: qdm backup spec without vmspec field (1 file in filemap and no filemeta)
    qemu_vm_spec = vmset_pb2.QemuVMSpec()
    backup_spec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=backup_action.QDM_BACKUP_SPEC_VERSION_1,
        rev_id='test',
        filemap=[
            qdm_pb2.QDMBackupFileSpec(path='layer.img', size=10 * 1024 ** 3),

        ]
    )
    backup_action.rewrite_qemu_volumes_from_backup_spec(qemu_vm_spec, backup_spec)
    assert len(qemu_vm_spec.volumes) == 1
    assert qemu_vm_spec.volumes[0].name == yputil.MAIN_VOLUME_NAME
    assert qemu_vm_spec.volumes[0].pod_mount_path == yputil.MAIN_VOLUME_POD_MOUNT_PATH
    assert qemu_vm_spec.volumes[0].vm_mount_path == yputil.MAIN_VOLUME_VM_MOUNT_PATH
    assert qemu_vm_spec.volumes[0].resource_url == "qdm:test/layer.img"
    assert qemu_vm_spec.volumes[0].image_type == vmset_pb2.Volume.RAW
    assert qemu_vm_spec.volumes[0].capacity == 10 * 1024 ** 3
    assert not qemu_vm_spec.volumes[0].storage_class

    # case: qdm backup spec without vmspec field(2 file in filemap and no filemeta)
    qemu_vm_spec = vmset_pb2.QemuVMSpec()
    backup_spec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=backup_action.QDM_BACKUP_SPEC_VERSION_1,
        rev_id='test',
        filemap=[
            qdm_pb2.QDMBackupFileSpec(path='layer.img', size=10 * 1024 ** 3),
            qdm_pb2.QDMBackupFileSpec(path='current.qcow2', size=10 * 1024 ** 3),
        ]
    )
    backup_action.rewrite_qemu_volumes_from_backup_spec(qemu_vm_spec, backup_spec)
    assert len(qemu_vm_spec.volumes) == 1
    assert qemu_vm_spec.volumes[0].name == yputil.MAIN_VOLUME_NAME
    assert qemu_vm_spec.volumes[0].pod_mount_path == yputil.MAIN_VOLUME_POD_MOUNT_PATH
    assert qemu_vm_spec.volumes[0].vm_mount_path == yputil.MAIN_VOLUME_VM_MOUNT_PATH
    assert qemu_vm_spec.volumes[0].resource_url == "qdm:test/layer.img,qdm:test/current.qcow2"
    assert qemu_vm_spec.volumes[0].image_type == vmset_pb2.Volume.DELTA
    assert qemu_vm_spec.volumes[0].capacity == 20 * 1024 ** 3
    assert not qemu_vm_spec.volumes[0].storage_class

    # case: qdm backup spec with vmspec
    qemu_vm_spec = vmset_pb2.QemuVMSpec()
    backup_spec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=backup_action.QDM_BACKUP_SPEC_VERSION_1,
        rev_id='test',
        filemap=[
            qdm_pb2.QDMBackupFileSpec(
                path='main/layer.img',
                size=10 * 1024 ** 3, meta=qdm_pb2.QDMBackupFileMeta(volume_index=0)
            ),
            qdm_pb2.QDMBackupFileSpec(
                path='extra/layer.img',
                size=10 * 1024 ** 3,
                meta=qdm_pb2.QDMBackupFileMeta(volume_index=1)
            ),
        ],
        vmspec=vmset_pb2.VM(
            spec=vmset_pb2.VMSpec(qemu=vmset_pb2.QemuVMSpec(volumes=[
                vmset_pb2.Volume(name=yputil.MAIN_VOLUME_NAME, capacity=120 * 1024 ** 3),
                vmset_pb2.Volume(name='extra', capacity=120 * 1024 ** 3),
            ]))
        )
    )
    backup_action.rewrite_qemu_volumes_from_backup_spec(qemu_vm_spec, backup_spec)
    assert len(qemu_vm_spec.volumes) == 2
    assert qemu_vm_spec.volumes[0].name == yputil.MAIN_VOLUME_NAME
    assert qemu_vm_spec.volumes[0].resource_url == "qdm:test/main/layer.img"
    assert qemu_vm_spec.volumes[0].image_type == vmset_pb2.Volume.RAW

    assert qemu_vm_spec.volumes[1].name == 'extra'
    assert qemu_vm_spec.volumes[1].resource_url == "qdm:test/extra/layer.img"
    assert qemu_vm_spec.volumes[1].image_type == vmset_pb2.Volume.RAW
