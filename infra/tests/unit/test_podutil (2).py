import mock

import yp.data_model as data_model
from yp_proto.yp.client.api.proto import pod_agent_pb2
from infra.rsc.src.lib import podutil
from sepelib.core import config


def test_is_pod_alive():
    pod = mock.Mock()
    pod.status.eviction.state = data_model.ES_NONE
    pod.status.agent.pod_agent_payload.status.revision = 1
    pod.spec.pod_agent_payload.spec.revision = 1
    pod.status.agent.state = data_model.PCS_STARTED
    pod.status.agent.pod_agent_payload.status.workloads = ['boo']
    pod.status.agent.pod_agent_payload.status.ready.status = pod_agent_pb2.EConditionStatus_TRUE
    assert podutil.is_pod_alive(pod)
    pod.spec.pod_agent_payload.spec.revision = 2
    assert not podutil.is_pod_alive(pod)
    pod.spec.pod_agent_payload.spec.revision = 1
    pod.status.agent.state = data_model.PCS_UNKNOWN
    assert not podutil.is_pod_alive(pod)
    pod.status.agent.state = data_model.PCS_STARTED
    pod.status.eviction.state = data_model.ES_ACKNOWLEDGED
    assert not podutil.is_pod_alive(pod)


def test_is_pod_finally_dead():
    pod = mock.Mock()
    pod.status.eviction.state = data_model.ES_REQUESTED
    assert podutil.is_pod_finally_dead(pod)


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

    config.set_value("enable_pod_agent_in_place_update", False)
    s = data_model.TPodSpec()
    s.CopyFrom(spec)
    s.pod_agent_payload.meta.url = "yandex.ru"
    t = data_model.TPodSpec()
    t.CopyFrom(spec)
    assert podutil.is_pod_spec_updateable(s, t) is False

    config.set_value("enable_pod_agent_in_place_update", True)
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
