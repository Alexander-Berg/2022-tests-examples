import mock

import pytest
import yp.data_model as data_model
from yp_proto.yp.client.api.proto import pod_agent_pb2

from infra.mc_rsc.src import consts
from infra.mc_rsc.src import model
from infra.mc_rsc.src import podutil
from infra.mc_rsc.src import pod_id_generator
from infra.mc_rsc.src import yputil


NODE_SEGMENT_LABEL = 'fake-node-segment-label'
NODE_SEGMENT_ATTR = 'fake-node-segment-attr'


def test_is_pod_ready():
    pod = mock.Mock()
    pod.status.eviction.state = data_model.ES_NONE
    pod.status.agent.pod_agent_payload.status.revision = 1
    pod.spec.pod_agent_payload.spec.revision = 1
    pod.status.agent.state = data_model.PCS_STARTED
    pod.status.agent.pod_agent_payload.status.workloads = ['boo']
    pod.status.agent.pod_agent_payload.status.ready.status = pod_agent_pb2.EConditionStatus_TRUE
    pod.status.scheduling.node_id = 'some-node'
    pod.meta.creation_time = 1
    with mock.patch('time.time') as mocked_time:
        mocked_time.return_value = 2
        assert podutil.is_pod_ready(pod)
        pod.status.scheduling.node_id = ''
        assert not podutil.is_pod_ready(pod)
        pod.status.scheduling.node_id = 'some-node'
        pod.spec.pod_agent_payload.spec.revision = 2
        assert not podutil.is_pod_ready(pod)
        pod.spec.pod_agent_payload.spec.revision = 1
        pod.status.agent.state = data_model.PCS_UNKNOWN
        assert not podutil.is_pod_ready(pod)
        pod.status.agent.state = data_model.PCS_STARTED
        pod.status.eviction.state = data_model.ES_ACKNOWLEDGED
        assert not podutil.is_pod_ready(pod)


def test_is_pod_eviction_requested():
    pod = mock.Mock()
    pod.status.eviction.state = data_model.ES_REQUESTED
    assert podutil.is_pod_eviction_requested(pod)


def test_is_pod_spec_updateable():
    spec = data_model.TPodSpec()
    spec.resource_requests.memory_guarantee = 111
    spec.resource_requests.memory_limit = 222
    spec.resource_requests.anonymous_memory_limit = 333
    r = spec.disk_volume_requests.add()
    r.id = 'dvr'
    r.storage_class = 'hdd'
    r.quota_policy.capacity = 1111
    r = spec.ip6_address_requests.add()
    r.network_id = '_NET_'
    r.vlan_id = 'vlan-id'
    d = spec.host_devices.add()
    d.path = '/dev/kvm'
    d.mode = 'r'
    spec.pod_agent_payload.spec.revision = 2
    spec.pod_agent_payload.spec.id = 'pag'
    spec.pod_agent_payload.meta.url = "example.yandex-team.ru"

    s = data_model.TPodSpec()
    t = data_model.TPodSpec()
    assert podutil.is_pod_spec_updateable(s, t) is True

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is True

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.account_id = 'acc-id-1'
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    s.account_id = 'acc-id-2'
    assert podutil.is_pod_spec_updateable(s, t) is True

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.resource_requests.memory_guarantee = s.resource_requests.memory_guarantee + 1
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is False

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.disk_volume_requests[0].storage_class = 'ssd'
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is False

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.ip6_address_requests[0].network_id = '_NET_1_'
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is False

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.ip6_address_requests[0].network_id = '_NET_1_'
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t, allow_resources=True) is False

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.host_devices[0].mode = 'rw'
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is False

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.pod_agent_payload.spec.revision = 3
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is True

    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.pod_agent_payload.meta.url = "yandex.ru"
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is True

    s = data_model.TPodSpec()
    s.resource_requests.slot = 123
    t = data_model.TPodSpec()
    t.resource_requests.slot = 456
    assert podutil.is_pod_spec_updateable(s, t) is True

    s = data_model.TPodSpec()
    s.resource_requests.anonymous_memory_limit = 1
    t = data_model.TPodSpec()
    t.resource_requests.anonymous_memory_limit = 1
    assert podutil.is_pod_spec_updateable(s, t) is True
    t.resource_requests.anonymous_memory_limit = s.resource_requests.anonymous_memory_limit + 1
    t.resource_requests.vcpu_limit = s.resource_requests.vcpu_limit + 1
    t.resource_requests.dirty_memory_limit = s.resource_requests.dirty_memory_limit + 1
    assert podutil.is_pod_spec_updateable(s, t) is True


def test_make_enumerated_id_pods():
    PS_ID = 'enumerated-ps-id.very-very-long-long-string'
    g = pod_id_generator.EnumeratedPodIdGenerator
    ps_prefix_len = g.ID_PREFIX_MAX_LENGTH - g.DEFAULT_SALT_LENGTH - 1
    PS_ID_PREFIX = PS_ID[:ps_prefix_len].lower().replace('.', '-')
    EXISTED_POD_INDEXES = set([0, 2, 3])

    pod_storage = mock.Mock()
    pod_storage.get.side_effect = lambda p_id, cluster: int(p_id.rsplit('-', 1)[1]) in EXISTED_POD_INDEXES
    tpl = data_model.TPod()
    tpl.meta.pod_set_id = PS_ID
    tpl.spec.resource_requests.memory_limit = 111

    pods = podutil.make_enumerated_id_pods(pod_template=tpl,
                                           count=3,
                                           pod_storage=pod_storage,
                                           cluster='sas')

    assert len(pods) == 3
    expected_pod_indexes = [1, 4, 5]
    for n, p in enumerate(pods):
        idx = expected_pod_indexes[n]
        prefix = p.meta.id.rsplit('-', 1)[0]
        assert len(prefix) <= pod_id_generator.EnumeratedPodIdGenerator.ID_PREFIX_MAX_LENGTH
        assert p.meta.id.startswith(PS_ID_PREFIX)
        assert p.meta.id.endswith(str(idx))
        assert int(yputil.get_label(p.labels, consts.POD_INDEX_LABEL)) == idx
        assert p.spec.resource_requests == tpl.spec.resource_requests


def test_make_enumerated_id_pods_by_other_pods():
    PS_ID = 'enumerated-ps'
    SALT = 'salt'

    pods_dict = {}
    for p_idx in xrange(3):
        p_id = '{}-{}-{}'.format(PS_ID, SALT, p_idx)
        p = data_model.TPod()
        p.meta.id = p_id
        yputil.set_label(p.labels, consts.POD_INDEX_LABEL, p_idx)
        pods_dict[p.meta.id] = p

    pod_storage = mock.Mock()
    pod_storage.get.side_effect = lambda p_id, cluster: pods_dict.get(p_id)

    tpl = data_model.TPod()
    tpl.meta.pod_set_id = PS_ID
    tpl.spec.resource_requests.memory_limit = 111

    new_pods = podutil.make_enumerated_id_pods_by_other_pods(pod_template=tpl,
                                                             pod_ids=pods_dict.keys(),
                                                             pod_storage=pod_storage,
                                                             cluster='sas')

    assert len(new_pods) == 3
    for new_p in new_pods:
        assert new_p.meta.id in pods_dict
        p = pods_dict.pop(new_p.meta.id)
        new_p_idx = yputil.get_label(new_p.labels, consts.POD_INDEX_LABEL)
        assert new_p_idx == yputil.get_label(p.labels, consts.POD_INDEX_LABEL)
        assert new_p.spec.resource_requests == tpl.spec.resource_requests


@pytest.fixture
def rs_node_segment():
    rs = data_model.TReplicaSet()
    rs.spec.node_segment_id = NODE_SEGMENT_ATTR
    rs.spec.account_id = 'fake-account'
    yputil.set_label(rs.spec.pod_template_spec.labels,
                     consts.TEMP_NODE_SEGMENT_LABEL,
                     NODE_SEGMENT_LABEL)
    return model.ReplicaSet(rs, cluster='fake')


@pytest.fixture
def default_acl():
    return [podutil.make_default_access_control_entry(['fake-user'])]


def test_make_pod_set_node_segment_label(rs_node_segment, default_acl):
    ps = podutil.make_pod_set(mc_rs=rs_node_segment,
                              default_acl=default_acl,
                              labels=rs_node_segment.labels,
                              cluster='fake')
    assert ps.spec.node_segment_id == NODE_SEGMENT_LABEL
    assert ps.spec.account_id == rs_node_segment.spec.account_id


def test_make_pod_set_node_segment_attr(rs_node_segment, default_acl):
    rs_node_segment.spec.pod_template_spec.labels.Clear()
    ps = podutil.make_pod_set(mc_rs=rs_node_segment,
                              default_acl=default_acl,
                              labels=rs_node_segment.labels,
                              cluster='fake')
    assert ps.spec.node_segment_id == NODE_SEGMENT_ATTR
    assert ps.spec.account_id == rs_node_segment.spec.account_id


def test_make_pod_set_node_segment_default(rs_node_segment, default_acl):
    rs_node_segment.spec.pod_template_spec.labels.Clear()
    rs_node_segment.spec.node_segment_id = ''
    ps = podutil.make_pod_set(mc_rs=rs_node_segment,
                              default_acl=default_acl,
                              labels=rs_node_segment.labels,
                              cluster='fake')
    assert ps.spec.node_segment_id == consts.DEFAULT_NODE_SEGMENT_ID
    assert ps.spec.account_id == rs_node_segment.spec.account_id
