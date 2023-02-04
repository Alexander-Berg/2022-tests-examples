from infra.qyp.account_manager.src.action.list_user_accounts import (set_personal_limits, set_personal_usage,
                                                                     cast_yp_resources
                                                                     )

from infra.qyp.proto_lib import vmset_pb2
import mock
import yp.data_model as data_model
from yt_yson_bindings import dumps_proto


def test_set_personal_limits():
    lim = vmset_pb2.ResourceTotals()
    q = {
        'mem': 1024, 'cpu': 1000, 'segment': 'dev', 'internet_address': 0,
        'disk': [
            {'storage': 'ssd', 'bandwidth_guarantee': 0, 'capacity': 322122547200},
            {'storage': 'hdd', 'bandwidth_guarantee': 222, 'capacity': 111}
        ]
    }
    set_personal_limits(lim, q)
    assert lim.per_segment['dev'].mem == q['mem']
    assert lim.per_segment['dev'].cpu == q['cpu']
    assert lim.per_segment['dev'].cpu == q['cpu']
    assert lim.per_segment['dev'].cpu == q['cpu']
    lim.per_segment['dev'].disk_per_storage['ssd'] = q['disk'][0]['capacity']
    lim.per_segment['dev'].disk_per_storage['hdd'] = q['disk'][1]['capacity']


def test_set_personal_usage(pod_client_mock):
    spec1 = data_model.TPodSpec()
    spec1.resource_requests.vcpu_guarantee = 100
    spec1.resource_requests.memory_guarantee = 256
    gpu1 = spec1.gpu_requests.add()
    gpu1.model = 'model1'
    gpu1.id = 'gpuid1'
    gpu2 = spec1.gpu_requests.add()
    gpu2.model = 'model1'
    gpu2.id = 'gpuid2'
    d = spec1.disk_volume_requests.add()
    d.id = 'test_id'
    d.storage_class = 'ssd'
    d.quota_policy.capacity = 128
    spec2 = data_model.TPodSpec()
    spec2.resource_requests.vcpu_guarantee = 128
    spec2.resource_requests.memory_guarantee = 256

    pods_result = mock.Mock()
    val1 = mock.Mock()
    val1.values = [dumps_proto(spec1)]
    val2 = mock.Mock()
    val2.values = [dumps_proto(spec2)]
    pods_result.results = [val1, val2]
    pod_client_mock.get_pods.return_value = pods_result
    usage = vmset_pb2.ResourceTotals()
    set_personal_usage(pod_client_mock, usage, {'dev': ['test_id'], 'gpu_dev': ['test_id']})
    assert usage.per_segment['dev'].cpu == 228
    assert usage.per_segment['dev'].mem == 512
    assert usage.per_segment['dev'].disk_per_storage['ssd'] == 128
    assert usage.per_segment['dev'].internet_address == 0
    assert usage.per_segment['dev'].gpu_per_model['model1'] == 2
    assert usage.per_segment['gpu_dev'].cpu == 228
    assert usage.per_segment['gpu_dev'].mem == 512
    assert usage.per_segment['gpu_dev'].disk_per_storage['ssd'] == 128
    assert usage.per_segment['gpu_dev'].internet_address == 0
    assert usage.per_segment['gpu_dev'].internet_address == 0
    assert usage.per_segment['gpu_dev'].gpu_per_model['model1'] == 2


def test_cast_yp_resources():
    pb = vmset_pb2.ResourceInfo()
    r = data_model.TPerSegmentResourceTotals()
    r.memory.capacity = 64
    r.cpu.capacity = 100
    r.disk_per_storage_class['ssd'].capacity = 256
    r.disk_per_storage_class['ssd'].bandwidth = 14
    r.network.bandwidth = 8

    cast_yp_resources(pb, r)
    assert pb.mem == 64
    assert pb.cpu == 100
    assert pb.disk_per_storage['ssd'] == 256
    assert pb.io_guarantees_per_storage['ssd'] == 14
    assert pb.network_guarantee == 8
