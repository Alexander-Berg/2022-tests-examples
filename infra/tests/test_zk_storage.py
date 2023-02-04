from awacs.lib.zk_storage import ZkStorageClient
from awacs.model.codecs import NamespaceCodec
from infra.awacs.proto import model_pb2


def test_watchable_storage_guaranteed_update(zk):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = 'test'
    ns_pb.meta.category = '0'
    ns_pb_old = model_pb2.Namespace()
    ns_pb_old.CopyFrom(ns_pb)
    ns_pb_old_2 = model_pb2.Namespace()
    ns_pb_old_2.CopyFrom(ns_pb)

    zk_storage = ZkStorageClient(zk, codec=NamespaceCodec, prefix='/namespaces')

    zk_storage.create(key='test', obj=ns_pb)
    pb = zk_storage.get('test')
    assert pb.meta.category == '0'
    assert pb.meta.generation == 0

    # test with pre-set object
    ns_pb.meta.category = '1'
    for pb in zk_storage.guaranteed_update('test', obj=ns_pb):
        pb.CopyFrom(ns_pb)
    pb = zk_storage.get('test')
    assert pb.meta.category == '1'
    assert pb.meta.generation == 1

    # and without
    for pb in zk_storage.guaranteed_update('test'):
        ns_pb.meta.category = '2'
        pb.CopyFrom(ns_pb)
    pb = zk_storage.get('test')
    assert pb.meta.category == '2'
    assert pb.meta.generation == 2

    # test older object with pre-set value
    ns_pb_old.meta.category = '3'
    for pb in zk_storage.guaranteed_update('test', obj=ns_pb_old):
        pb.CopyFrom(ns_pb_old)
    pb = zk_storage.get('test')
    assert pb.meta.category == '3'
    assert pb.meta.generation == 3

    # test older object without pre-set value
    for pb in zk_storage.guaranteed_update('test'):
        ns_pb_old_2.meta.category = '4'
        pb.CopyFrom(ns_pb_old_2)
    pb = zk_storage.get('test')
    assert pb.meta.category == '4'
    assert pb.meta.generation == 4
