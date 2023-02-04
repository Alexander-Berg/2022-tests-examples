import mock

import yp.data_model as data_model
import yt_yson_bindings
import yt.yson as yson
from yp_proto.yp.client.api.proto import object_service_pb2, deploy_pb2
from infra.rsc.src.model import yp_client as rsc_yp_client
from infra.rsc.src.model.consts import DEFAULT_OBJECT_SELECTORS


def test_max_id_query_maker():
    rs1 = data_model.TReplicaSet()
    rs1.meta.id = 'b'
    rs2 = data_model.TReplicaSet()
    rs2.meta.id = 'a'
    rs3 = data_model.TReplicaSet()
    rs3.meta.id = 'c'

    qm = rsc_yp_client.MaxIdQueryMaker('base-query')
    assert qm.make_query() == 'base-query'
    qm.update(rs1)
    assert qm.make_query() == '(base-query) AND [/meta/id] > "b"'
    qm.update(rs2)
    assert qm.make_query() == '(base-query) AND [/meta/id] > "b"'
    qm.update(rs3)
    assert qm.make_query() == '(base-query) AND [/meta/id] > "c"'


def test_pod_max_id_query_maker():
    p1 = data_model.TPod()
    p1.meta.pod_set_id, p1.meta.id = ('b', 'a')
    p2 = data_model.TPod()
    p2.meta.pod_set_id, p2.meta.id = ('a', 'b')
    p3 = data_model.TPod()
    p3.meta.pod_set_id, p3.meta.id = ('c', 'a')

    qm = rsc_yp_client.PodMaxIdQueryMaker('base-query')
    assert qm.make_query() == 'base-query'
    qm.update(p1)
    assert qm.make_query() == '(base-query) AND ([/meta/pod_set_id], [/meta/id]) > ("b", "a")'
    qm.update(p2)
    assert qm.make_query() == '(base-query) AND ([/meta/pod_set_id], [/meta/id]) > ("b", "a")'
    qm.update(p3)
    assert qm.make_query() == '(base-query) AND ([/meta/pod_set_id], [/meta/id]) > ("c", "a")'


def test_select_all_objects(yp_client):
    p1 = data_model.TPod()
    p1.meta.pod_set_id, p1.meta.id = ('b', 'a')
    p2 = data_model.TPod()
    p2.meta.pod_set_id, p2.meta.id = ('c', 'a')
    p3 = data_model.TPod()
    p3.meta.pod_set_id, p3.meta.id = ('a', 'b')
    pods = [p1, p2, p3]
    base_query = 'base-query'

    resp1 = object_service_pb2.TRspSelectObjects()
    r = resp1.results.add()
    r.value_payloads.extend([object_service_pb2.TPayload(protobuf=p1.SerializeToString())])
    r = resp1.results.add()
    r.value_payloads.extend([object_service_pb2.TPayload(protobuf=p2.SerializeToString())])
    r = resp1.results.add()
    r.value_payloads.extend([object_service_pb2.TPayload(protobuf=p3.SerializeToString())])
    resp2 = object_service_pb2.TRspSelectObjects()
    yp_client.stub.SelectObjects.side_effect = [resp1, resp2]

    expected_req = object_service_pb2.TReqSelectObjects()
    expected_req.object_type = data_model.OT_POD
    expected_req.limit.value = len(pods)
    expected_req.selector.paths.extend(['/meta'])
    expected_req.filter.query = base_query
    expected_req.format = object_service_pb2.PF_PROTOBUF
    expected_req.options.fetch_root_object = True

    g = yp_client.select_all_objects(obj_type=data_model.OT_POD,
                                     obj_class=data_model.TPod,
                                     batch_size=len(pods),
                                     query=base_query,
                                     selectors=["/meta"])
    selected_pods = list(g)
    assert selected_pods == pods
    calls = yp_client.stub.method_calls

    assert calls[-2] == mock.call.SelectObjects(expected_req)
    q = '({}) AND ([/meta/pod_set_id], [/meta/id]) > ("{}", "{}")'.format(
        base_query,
        p2.meta.pod_set_id,
        p2.meta.id
    )
    expected_req.filter.query = q
    assert calls[-1] == mock.call.SelectObjects(expected_req)


def test_create_pod_set(yp_client):
    rs_id = 'rs-id'
    account_id = 'account-id'
    node_segment_id = 'default'
    constraints = data_model.TReplicaSetSpec.TConstraints()
    ac = constraints.antiaffinity_constraints.add()
    ac.key = 'node'
    ac.max_pods = 1
    labels = data_model.TAttributeDictionary()
    attr = labels.attributes.add()
    attr.key = 'key'
    attr.value = yson.dumps('value')

    expected_ps = data_model.TPodSet()
    expected_ps.meta.id = rs_id
    a = expected_ps.meta.acl.add()
    a.action = data_model.ACA_ALLOW
    a.permissions.extend([data_model.ACP_READ,
                          data_model.ACA_WRITE,
                          data_model.ACA_CREATE,
                          data_model.ACA_SSH_ACCESS,
                          data_model.ACA_ROOT_SSH_ACCESS])
    a.subjects.extend(['root-user'])
    expected_ps.spec.node_segment_id = node_segment_id
    expected_ps.spec.account_id = account_id
    expected_ps.spec.antiaffinity_constraints.MergeFrom(constraints.antiaffinity_constraints)
    expected_ps.labels.CopyFrom(labels)

    transaction_id = 'transaction-id'
    yp_client.create_pod_set(ps=expected_ps,
                             transaction_id=transaction_id)

    expected_req = object_service_pb2.TReqCreateObject()
    expected_req.transaction_id = transaction_id
    expected_req.object_type = data_model.OT_POD_SET
    expected_req.attributes = yt_yson_bindings.dumps_proto(expected_ps)

    yp_client.stub.CreateObject.assert_called_once_with(expected_req)


def test_remove_pod_set(yp_client):
    ps = data_model.TPodSet()
    ps.meta.id = 'ps'

    resp = object_service_pb2.TRspRemoveObjects()
    yp_client.stub.RemoveObject.side_effect = [resp]

    yp_client.remove_pod_set(ps.meta.id)

    expected_req = object_service_pb2.TReqRemoveObject()
    expected_req.object_type = data_model.OT_POD_SET
    expected_req.object_id = ps.meta.id

    assert yp_client.stub.method_calls[-1] == mock.call.RemoveObject(expected_req)


def _make_expected_req_get_object(obj_type, ignore_nonexistent=True):
    expected_req = object_service_pb2.TReqGetObject()
    expected_req.object_type = obj_type
    expected_req.selector.paths.extend(['/meta'])
    expected_req.object_id = 'id'
    if ignore_nonexistent:
        expected_req.options.ignore_nonexistent = True
    return expected_req


def test_get_pod_set_ignore(yp_client):
    obj = data_model.TPodSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.values.extend([yt_yson_bindings.dumps_proto(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_POD_SET)

    o = yp_client.get_pod_set_ignore('id', selectors=['/meta'])
    assert o == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_get_replica_set_ignore(yp_client):
    obj = data_model.TReplicaSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.values.extend([yt_yson_bindings.dumps_proto(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_REPLICA_SET)

    o = yp_client.get_replica_set_ignore('id', selectors=['/meta'])
    assert o == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_get_multi_cluster_replica_set_ignore(yp_client):
    obj = data_model.TMultiClusterReplicaSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.values.extend([yt_yson_bindings.dumps_proto(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_MULTI_CLUSTER_REPLICA_SET)

    o = yp_client.get_multi_cluster_replica_set_ignore('id', selectors=['/meta'])
    assert o == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_get_replica_set(yp_client):
    obj = data_model.TReplicaSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.values.extend([yt_yson_bindings.dumps_proto(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_REPLICA_SET, ignore_nonexistent=False)
    expected_req.timestamp = 100

    rs = yp_client.get_replica_set('id', selectors=['/meta'], timestamp=100)
    assert rs == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_make_and_load_object_attrs(yp_client):
    obj = data_model.TPod()
    selectors = ["/meta", "/spec", "/status/agent"]

    paths = yp_client._make_attr_paths(selectors)
    meta_attr = yp_client._make_object_attr(obj, paths[0])
    spec_attr = yp_client._make_object_attr(obj, paths[1])
    status_agent_attr = yp_client._make_object_attr(obj, paths[2])

    assert type(meta_attr) == data_model.TPodMeta
    assert type(spec_attr) == data_model.TPodSpec
    assert type(status_agent_attr) == data_model.TPodStatus.TAgent

    fake_obj = data_model.TPod()
    fake_obj.meta.id = "179"
    fake_obj.spec.iss_payload, fake_obj.spec.node_id = "a", "2"
    fake_obj.status.agent.iss_payload, fake_obj.status.generation_number = "b", 1

    values = [yt_yson_bindings.dumps_proto(fake_obj.meta),
              yt_yson_bindings.dumps_proto(fake_obj.spec),
              yt_yson_bindings.dumps_proto(fake_obj.status.agent)]

    result = yp_client._load_object(data_model.TPod, selectors, values)
    assert result.meta == fake_obj.meta
    assert result.spec == fake_obj.spec
    assert result.status.agent.iss_payload == "b"
    assert result.status.generation_number == 0

    assert result == yp_client._load_object_attrs(data_model.TPod, paths, values)


def test_pod_set_exists(yp_client):
    ps = data_model.TPodSet()
    ps.meta.id = 'ps'
    resp = object_service_pb2.TRspGetObject()
    r = resp.result
    r.values.extend([yt_yson_bindings.dumps_proto(ps)])
    resp2 = object_service_pb2.TRspGetObject()
    r = resp.result
    r.values.extend([])
    yp_client.stub.GetObject.side_effect = [resp, resp2]

    assert yp_client.pod_set_exists('ps')

    expected_req = object_service_pb2.TReqGetObject()
    expected_req.object_type = data_model.OT_POD_SET
    expected_req.object_id = ps.meta.id
    expected_req.options.ignore_nonexistent = True
    expected_req.selector.paths.extend(DEFAULT_OBJECT_SELECTORS)

    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)

    assert not yp_client.pod_set_exists('fake_ps')


def test_generate_timestamp(yp_client):
    resp = object_service_pb2.TRspGenerateTimestamp()
    resp.timestamp = 179
    yp_client.stub.GenerateTimestamp.side_effect = [resp]

    ts = yp_client.generate_timestamp()

    assert ts == 179
    assert yp_client.stub.method_calls[-1] == mock.call.GenerateTimestamp(object_service_pb2.TReqGenerateTimestamp())


def test_start_and_commit_transaction(yp_client):
    resp = object_service_pb2.TRspStartTransaction()
    resp.start_timestamp = 179
    resp.transaction_id = 'tr'
    yp_client.stub.StartTransaction.side_effect = [resp]

    t = yp_client.start_transaction()

    assert t == ('tr', 179)
    assert yp_client.stub.method_calls[-1] == mock.call.StartTransaction(object_service_pb2.TReqStartTransaction())

    resp = object_service_pb2.TRspCommitTransaction()
    resp.commit_timestamp = 200
    yp_client.stub.CommitTransaction.side_effect = [resp]

    expected_req = object_service_pb2.TReqCommitTransaction()
    expected_req.transaction_id = t[0]

    yp_client.commit_transaction(t[0])
    assert yp_client.stub.method_calls[-1] == mock.call.CommitTransaction(expected_req)


def test_create_pods(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'

    resp = object_service_pb2.TRspCreateObjects()
    r = resp.subresponses.add()
    r.object_id = p1.meta.id
    r = resp.subresponses.add()
    r.object_id = p2.meta.id
    yp_client.stub.CreateObjects.side_effect = [resp]

    yp_client.create_pods([p1, p2], 'tr')

    expected_req = object_service_pb2.TReqCreateObjects()
    expected_req.transaction_id = 'tr'
    r = expected_req.subrequests.add()
    r.object_type = data_model.OT_POD
    r.attributes = yt_yson_bindings.dumps_proto(p1)
    r = expected_req.subrequests.add()
    r.object_type = data_model.OT_POD
    r.attributes = yt_yson_bindings.dumps_proto(p2)

    assert yp_client.stub.method_calls[-1] == mock.call.CreateObjects(expected_req)


def test_remove_pods(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'

    resp = object_service_pb2.TRspRemoveObjects()
    yp_client.stub.CreateObjects.side_effect = [resp]

    yp_client.remove_pods([p1, p2], 'tr')

    expected_req = object_service_pb2.TReqRemoveObjects()
    expected_req.transaction_id = 'tr'
    r = expected_req.subrequests.add()
    r.object_type = data_model.OT_POD
    r.object_id = p1.meta.id
    r = expected_req.subrequests.add()
    r.object_type = data_model.OT_POD
    r.object_id = p2.meta.id

    assert yp_client.stub.method_calls[-1] == mock.call.RemoveObjects(expected_req)


def _make_expected_req_update_object(obj_type, path, value):
    expected_req = object_service_pb2.TReqUpdateObject()
    expected_req.object_type = obj_type
    expected_req.object_id = 'id'
    r = expected_req.set_updates.add()
    r.path = path
    r.value = yt_yson_bindings.dumps_proto(value)
    return expected_req


def test_update_pod_set(yp_client):
    ps = data_model.TPodSet()

    expected_req = object_service_pb2.TReqUpdateObject()
    expected_req.object_type = data_model.OT_POD_SET
    expected_req.object_id = 'ps-id'
    r = expected_req.set_updates.add()
    r.path = '/spec'
    r.value = yt_yson_bindings.dumps_proto(ps.spec)
    r = expected_req.set_updates.add()
    r.path = '/meta/acl'
    r.value = yson.dumps([])

    yp_client.update_pod_set('ps-id', ps)
    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObject(expected_req)


def test_update_replica_set_status(yp_client):
    resp = object_service_pb2.TRspUpdateObject()
    yp_client.stub.UpdateObject.side_effect = [resp]
    value = data_model.TReplicaSetStatus()

    expected_req = _make_expected_req_update_object(data_model.OT_REPLICA_SET, '/status', value)

    yp_client.update_replica_set_status('id', value)
    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObject(expected_req)


def test_update_replica_set_annotations(yp_client):
    resp = object_service_pb2.TRspUpdateObject()
    yp_client.stub.UpdateObject.side_effect = [resp]
    value = data_model.TAttributeDictionary()

    expected_req = _make_expected_req_update_object(data_model.OT_REPLICA_SET, '/annotations', value)
    expected_req.transaction_id = 'tr'

    yp_client.update_replica_set_annotations('id', value, 'tr')
    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObject(expected_req)


def test_update_multi_cluster_replica_set_status(yp_client):
    resp = object_service_pb2.TRspUpdateObject()
    yp_client.stub.UpdateObject.side_effect = [resp]
    value = data_model.TMultiClusterReplicaSetStatus()

    expected_req = _make_expected_req_update_object(data_model.OT_MULTI_CLUSTER_REPLICA_SET, '/status', value)

    yp_client.update_multi_cluster_replica_set_status('id', value)
    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObject(expected_req)


def test_update_pods(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'
    pods = [p1, p2]

    spec = data_model.TPodSpec()
    labels = data_model.TAttributeDictionary()
    template = deploy_pb2.TPodTemplateSpec()
    template.spec.CopyFrom(spec)
    template.labels.CopyFrom(labels)

    resp = object_service_pb2.TRspUpdateObjects()
    yp_client.stub.UpdateObjects.side_effect = [resp]

    yp_client.update_pods(pods, template)

    expected_req = object_service_pb2.TReqUpdateObjects()
    for p in pods:
        spec.pod_agent_payload.spec.id = p.meta.id
        for path, field in [('/spec', spec), ('/labels', labels)]:
            r = expected_req.subrequests.add()
            r.object_type = data_model.OT_POD
            r.object_id = p.meta.id
            upd = r.set_updates.add()
            upd.path = path
            upd.value = yt_yson_bindings.dumps_proto(field)

    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObjects(expected_req)


def test_update_pods_acknowledge_eviction(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'
    pods = [p1, p2]

    msg = 'some_message'

    resp = object_service_pb2.TRspUpdateObjects()
    yp_client.stub.UpdateObjects.side_effect = [resp]

    yp_client.update_pods_acknowledge_eviction(pods, msg)

    expected_req = object_service_pb2.TReqUpdateObjects()
    yson_value = yson.dumps({'message': msg})
    for p in pods:
        r = expected_req.subrequests.add()
        r.object_type = data_model.OT_POD
        r.object_id = p.meta.id
        upd = r.set_updates.add()
        upd.path = '/control/acknowledge_eviction'
        upd.value = yson_value

    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObjects(expected_req)
