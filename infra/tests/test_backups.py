from datetime import datetime

from infra.qyp.account_manager.src.lib import qdm_client
from infra.qyp.proto_lib import vmset_pb2


def test_filter_user_backup_list():
    b1 = vmset_pb2.Backup()
    b2 = vmset_pb2.Backup()
    b3 = vmset_pb2.Backup()
    b4 = vmset_pb2.Backup()
    b5 = vmset_pb2.Backup()
    b1.spec.vm_spec.qemu.node_segment = 'default'
    b2.spec.vm_spec.qemu.node_segment = 'not_default'  # filters out
    b3.spec.vm_spec.qemu.node_segment = 'default'
    b4.spec.vm_spec.qemu.node_segment = 'default'
    b5.spec.vm_spec.qemu.node_segment = 'default'
    b1.spec.vm_id = 'test_vm'
    b2.spec.vm_id = 'test_vm'
    b3.spec.vm_id = 'not_vm'  # filters out
    b4.spec.vm_id = 'test_vm'
    b5.spec.vm_id = 'test_vm'
    b1.meta.creation_time.FromDatetime(datetime(2021, 2, 1))
    b2.meta.creation_time.FromDatetime(datetime(2021, 2, 1))
    b3.meta.creation_time.FromDatetime(datetime(2021, 2, 1))
    b4.meta.creation_time.FromDatetime(datetime(2019, 1, 1))  # filters out
    b5.meta.creation_time.FromDatetime(datetime(2021, 2, 1))
    b1.spec.cluster = 'SAS'
    b2.spec.cluster = 'SAS'
    b3.spec.cluster = 'SAS'
    b4.spec.cluster = 'SAS_TEST'
    b5.spec.cluster = 'IVA'  # filters out

    clusters = ['SAS', 'SAS_TEST']
    q = vmset_pb2.BackupFindQuery()
    q.segment.append('default')
    q.vm_name = 'test_vm'
    q.creation_time_gte.FromDatetime(datetime(2021, 1, 1))
    q.creation_time_lte.FromDatetime(datetime(2022, 1, 1))
    res = qdm_client.QDMClient._filter_user_backup_list([b1, b2, b3, b4, b5], q, clusters)
    assert b1 in res
    assert b2 not in res
    assert b3 not in res
    assert b4 not in res
    assert b5 not in res
