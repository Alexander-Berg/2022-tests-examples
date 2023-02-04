import mock
import pytest
from google.protobuf import message as pb_message

import yp.client
import yp.common
import yp.data_model as data_model
import yt_yson_bindings
import yt.yson as yson
from yp_proto.yp.client.api.proto import object_service_pb2
from infra.mc_rsc.src.consts import DEFAULT_OBJECT_SELECTORS
from infra.mc_rsc.src.lib.loaders import make_attr_paths


def _make_value_payload(obj):
    rv = object_service_pb2.TPayload()
    if isinstance(obj, pb_message.Message):
        rv.yson = yt_yson_bindings.dumps_proto(obj)
    else:
        rv.yson = yson.dumps(obj)
    return rv


def _make_value_payload_enum(enum_proto, number):
    rv = object_service_pb2.TPayload()
    rv.yson = yp.client.get_proto_enum_value_name(
        yp.client.to_proto_enum_by_number(enum_proto, number),
    )
    return rv


def test_select_object_ids(yp_client):
    ids = ['id1', 'id2', 'id3']
    batch_size = 2
    continuation_token = 'cont'

    resp1 = object_service_pb2.TRspSelectObjects()
    resp1.continuation_token = continuation_token
    r = resp1.results.add()
    r.value_payloads.extend([_make_value_payload(ids[0])])
    r = resp1.results.add()
    r.value_payloads.extend([_make_value_payload(ids[1])])
    resp2 = object_service_pb2.TRspSelectObjects()
    resp2.continuation_token = 'any-token'
    r = resp2.results.add()
    r.value_payloads.extend([_make_value_payload(ids[2])])
    yp_client.stub.SelectObjects.side_effect = [resp1, resp2]

    expected_req1 = object_service_pb2.TReqSelectObjects()
    expected_req1.object_type = data_model.OT_POD
    expected_req1.limit.value = batch_size
    expected_req1.selector.paths.extend(['/meta/id'])
    expected_req1.format = object_service_pb2.PF_YSON
    expected_req1.options.fetch_timestamps = False

    expected_req2 = object_service_pb2.TReqSelectObjects()
    expected_req2.CopyFrom(expected_req1)
    expected_req2.options.continuation_token = continuation_token

    selected_ids = yp_client.select_object_ids(object_type=data_model.OT_POD,
                                               object_class=data_model.TPod,
                                               batch_size=batch_size)
    assert selected_ids == ids
    calls = yp_client.stub.method_calls
    assert len(calls) == 2
    assert calls[0] == mock.call.SelectObjects(expected_req1)
    assert calls[1] == mock.call.SelectObjects(expected_req2)


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
    tid = 'tid'

    resp = object_service_pb2.TRspRemoveObjects()
    yp_client.stub.RemoveObject.side_effect = [resp]

    yp_client.remove_pod_set(ps.meta.id, transaction_id=tid)

    expected_req = object_service_pb2.TReqRemoveObject()
    expected_req.object_type = data_model.OT_POD_SET
    expected_req.object_id = ps.meta.id
    expected_req.transaction_id = tid

    assert yp_client.stub.method_calls[-1] == mock.call.RemoveObject(expected_req)


def _make_expected_req_get_object(obj_type, ignore_nonexistent=True):
    expected_req = object_service_pb2.TReqGetObject()
    expected_req.object_type = obj_type
    expected_req.selector.paths.extend(['/meta'])
    expected_req.object_id = 'id'
    if ignore_nonexistent:
        expected_req.options.ignore_nonexistent = True
    expected_req.format = object_service_pb2.PF_YSON
    return expected_req


def test_get_pod_set_ignore(yp_client):
    obj = data_model.TPodSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.value_payloads.extend([_make_value_payload(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_POD_SET)

    o = yp_client.get_pod_set_ignore('id', selectors=['/meta'])
    assert o == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_get_replica_set_ignore(yp_client):
    obj = data_model.TReplicaSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.value_payloads.extend([_make_value_payload(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_REPLICA_SET)

    o = yp_client.get_replica_set_ignore('id', selectors=['/meta'])
    assert o == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_get_multi_cluster_replica_set_ignore(yp_client):
    obj = data_model.TMultiClusterReplicaSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.value_payloads.extend([_make_value_payload(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_MULTI_CLUSTER_REPLICA_SET)

    o = yp_client.get_multi_cluster_replica_set_ignore('id', selectors=['/meta'])
    assert o == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_get_replica_set(yp_client):
    obj = data_model.TReplicaSet()
    obj.meta.id = 'id'

    resp1 = object_service_pb2.TRspGetObject()
    resp1.result.value_payloads.extend([_make_value_payload(obj.meta)])
    yp_client.stub.GetObject.side_effect = [resp1]

    expected_req = _make_expected_req_get_object(data_model.OT_REPLICA_SET, ignore_nonexistent=False)
    expected_req.timestamp = 100

    rs = yp_client.get_replica_set('id', selectors=['/meta'], timestamp=100)
    assert rs == obj
    assert yp_client.stub.method_calls[-1] == mock.call.GetObject(expected_req)


def test_make_and_load_object_attrs(yp_client):
    selectors = [
        "/meta",
        "/spec",
        "/spec/pod_agent_payload/spec/revision",
        "/spec/pod_agent_payload/spec/target_state",
        "/status/agent"
    ]
    paths = make_attr_paths(selectors)

    fake_obj = data_model.TPod()
    fake_obj.meta.id = "179"
    fake_obj.spec.pod_agent_payload.spec.revision = 1
    fake_obj.spec.pod_agent_payload.spec.target_state = data_model.EPodAgentTargetState_REMOVED
    fake_obj.spec.iss_payload = "a"
    fake_obj.spec.node_id = "2"
    fake_obj.status.agent.iss_payload = "b"
    fake_obj.status.generation_number = 1

    value_payloads = [_make_value_payload(fake_obj.meta),
                      _make_value_payload(fake_obj.spec),
                      _make_value_payload(fake_obj.spec.pod_agent_payload.spec.revision),
                      _make_value_payload_enum(
                          data_model.EPodAgentTargetState,
                          fake_obj.spec.pod_agent_payload.spec.target_state),
                      _make_value_payload(fake_obj.status.agent)]

    result = yp_client.loader.load_object(data_model.TPod, selectors, value_payloads)
    assert result.meta == fake_obj.meta
    assert result.spec == fake_obj.spec
    assert result.spec.pod_agent_payload.spec.revision == fake_obj.spec.pod_agent_payload.spec.revision
    assert result.status.agent.iss_payload == "b"
    assert result.status.generation_number == 0

    assert result == yp_client.loader.load_object_attrs(data_model.TPod, paths, value_payloads)


def test_pod_set_exists(yp_client):
    ps = data_model.TPodSet()
    ps.meta.id = 'ps'
    resp = object_service_pb2.TRspGetObject()
    r = resp.result
    r.value_payloads.extend([_make_value_payload(ps)])
    resp2 = object_service_pb2.TRspGetObject()
    r = resp.result
    r.value_payloads.extend([])
    yp_client.stub.GetObject.side_effect = [resp, resp2]

    assert yp_client.pod_set_exists('ps')

    expected_req = object_service_pb2.TReqGetObject()
    expected_req.object_type = data_model.OT_POD_SET
    expected_req.object_id = ps.meta.id
    expected_req.options.ignore_nonexistent = True
    expected_req.format = object_service_pb2.PF_YSON
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

    yp_client.remove_pods([p1.meta.id, p2.meta.id], 'tr')

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
    ps.spec.node_segment_id = 'default'
    ps.spec.account_id = 'account'
    c = ps.spec.antiaffinity_constraints.add()
    c.key = 'node'
    c.max_pods = 1

    expected_req = object_service_pb2.TReqUpdateObject()
    expected_req.object_type = data_model.OT_POD_SET
    expected_req.object_id = 'ps-id'
    r = expected_req.set_updates.add()
    r.path = '/spec/antiaffinity_constraints'
    r.value = yson.dumps([yp.common.protobuf_to_dict(ac) for ac in ps.spec.antiaffinity_constraints])
    r = expected_req.set_updates.add()
    r.path = '/spec/node_segment_id'
    r.value = yson.dumps(ps.spec.node_segment_id)
    r = expected_req.set_updates.add()
    r.path = '/spec/account_id'
    r.value = yson.dumps(ps.spec.account_id)
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


def _add_pod_to_update_pods_request(req, pod):
    r = req.subrequests.add()
    r.object_type = data_model.OT_POD
    r.object_id = pod.meta.id

    upd = r.set_updates.add()
    upd.path = '/spec'
    upd.value = yt_yson_bindings.dumps_proto(pod.spec)

    if not pod.spec.secrets:
        upd = r.set_updates.add()
        upd.path = '/spec/secrets'
        upd.value = yt_yson_bindings.dumps({})

    for attr in pod.labels.attributes:
        upd = r.set_updates.add()
        upd.path = '/labels/{}'.format(attr.key)
        upd.value = attr.value

    upd = r.set_updates.add()
    upd.path = '/annotations'
    upd.value = yt_yson_bindings.dumps_proto(pod.annotations)


def test_update_pods(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    attr = p1.labels.attributes.add()
    attr.key = 'foo'
    attr.value = yson.dumps('bar')
    p2 = data_model.TPod()
    p2.meta.id = 'p2'
    pods = [p1, p2]

    # Case 1: update pods in one batch
    yp_client.update_pods(pods, batch_size=2)
    expected_req = object_service_pb2.TReqUpdateObjects()
    for pod in pods:
        _add_pod_to_update_pods_request(expected_req, pod)
    assert yp_client.stub.method_calls[0] == mock.call.UpdateObjects(expected_req)

    yp_client.stub.reset_mock()
    # Case 2: update pods in 2 batches concurrently
    yp_client.update_pods(pods, batch_size=1)
    expected_reqs = []
    for pod in pods:
        req = object_service_pb2.TReqUpdateObjects()
        _add_pod_to_update_pods_request(req, pod)
        expected_reqs.append(req)
    assert yp_client.stub.method_calls[0] == mock.call.UpdateObjects(expected_reqs[0])
    assert yp_client.stub.method_calls[1] == mock.call.UpdateObjects(expected_reqs[1])

    yp_client.stub.reset_mock()

    # Case 3: update pods in 2 batches concurrently whe first batch failed
    def update_pods_side_effect(req):
        if req.subrequests[0].object_id == p1.meta.id:
            raise Exception('update pods exception')

    yp_client.stub.UpdateObjects.side_effect = update_pods_side_effect

    with pytest.raises(Exception) as exc_info:
        yp_client.update_pods(pods, batch_size=1)
    assert exc_info.value.args[0] == 'update pods exception'

    expected_reqs = []
    for pod in pods:
        req = object_service_pb2.TReqUpdateObjects()
        _add_pod_to_update_pods_request(req, pod)
        expected_reqs.append(req)
    assert yp_client.stub.method_calls[0] == mock.call.UpdateObjects(expected_reqs[0])
    assert yp_client.stub.method_calls[1] == mock.call.UpdateObjects(expected_reqs[1])


def test_update_pods_acknowledge_eviction(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'
    pod_ids = [p1.meta.id, p2.meta.id]

    msg = 'some_message'

    resp = object_service_pb2.TRspUpdateObjects()
    yp_client.stub.UpdateObjects.side_effect = [resp]

    yp_client.update_pods_acknowledge_eviction(pod_ids, msg)

    expected_req = object_service_pb2.TReqUpdateObjects()
    yson_value = yson.dumps({'message': msg})
    for p_id in pod_ids:
        r = expected_req.subrequests.add()
        r.object_type = data_model.OT_POD
        r.object_id = p_id
        upd = r.set_updates.add()
        upd.path = '/control/acknowledge_eviction'
        upd.value = yson_value

    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObjects(expected_req)


def test_update_pods_target_state_to_removed(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'
    pod_ids = [p1.meta.id, p2.meta.id]
    target_state = data_model.EPodAgentTargetState_REMOVED

    resp = object_service_pb2.TRspUpdateObjects()
    yp_client.stub.UpdateObjects.side_effect = [resp]

    yp_client.update_pods_target_state(pod_ids, target_state)

    yson_value = yson.dumps('removed')
    expected_req = object_service_pb2.TReqUpdateObjects()
    for p_id in pod_ids:
        r = expected_req.subrequests.add()
        r.object_type = data_model.OT_POD
        r.object_id = p_id
        upd = r.set_updates.add()
        upd.path = '/spec/pod_agent_payload/spec/target_state'
        upd.value = yson_value

    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObjects(expected_req)


def test_update_pods_target_state_to_active(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'
    pod_ids = [p1.meta.id, p2.meta.id]
    target_state = data_model.EPodAgentTargetState_ACTIVE

    resp = object_service_pb2.TRspUpdateObjects()
    yp_client.stub.UpdateObjects.side_effect = [resp]

    yp_client.update_pods_target_state(pod_ids, target_state)

    yson_value = yson.dumps('active')
    expected_req = object_service_pb2.TReqUpdateObjects()
    for p_id in pod_ids:
        r = expected_req.subrequests.add()
        r.object_type = data_model.OT_POD
        r.object_id = p_id
        upd = r.set_updates.add()
        upd.path = '/spec/pod_agent_payload/spec/target_state'
        upd.value = yson_value

    assert yp_client.stub.method_calls[-1] == mock.call.UpdateObjects(expected_req)


def test_safe_update_pods(yp_client):
    p1 = data_model.TPod()
    p1.meta.id = 'p1'
    p2 = data_model.TPod()
    p2.meta.id = 'p2'
    pods = [p1, p2]

    resp = yp_client.safe_update_pods(pods)
    assert resp == []

    expected_reqs = []
    for pod in pods:
        req = object_service_pb2.TReqUpdateObjects()
        _add_pod_to_update_pods_request(req, pod)
        expected_reqs.append(req)

    assert yp_client.stub.method_calls[0] == mock.call.UpdateObjects(expected_reqs[0])
    assert yp_client.stub.method_calls[1] == mock.call.UpdateObjects(expected_reqs[1])

    yp_client.stub.reset_mock()

    def update_pods_side_effect(req):
        if req.subrequests[0].object_id == p1.meta.id:
            raise yp.common.YpPodSchedulingFailure('scheduling failure')

    yp_client.stub.UpdateObjects.side_effect = update_pods_side_effect

    resp = yp_client.safe_update_pods(pods)
    assert resp == pods[:1]
