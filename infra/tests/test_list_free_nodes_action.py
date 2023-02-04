from infra.qyp.vmproxy.src.action import list_free_nodes as list_free_nodes_action
from infra.qyp.proto_lib import vmset_pb2

from yp import data_model
from yp_proto.yp.client.api.proto import object_service_pb2
from yt import yson
from infra.qyp.vmproxy.src.lib.yp import yputil


Gb = 1024 ** 3


def list_resources():
    default_node_id = 'sas1-1111.search.yandex.net'
    # CPU 1000
    cpu_resp = object_service_pb2.TAttributeList()
    cpu_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.cpu.total_capacity = 10000
    cpu_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.cpu.capacity = 1000
    cpu_resp.values.append(yputil.dumps_proto(res_free))
    # Memory 100Gb
    mem_resp = object_service_pb2.TAttributeList()
    mem_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.memory.total_capacity = 1000 * Gb
    mem_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.memory.capacity = 100 * Gb
    mem_resp.values.append(yputil.dumps_proto(res_free))
    # SSD 1000Gb
    ssd_resp = object_service_pb2.TAttributeList()
    ssd_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.disk.storage_class = 'ssd'
    res_spec.disk.total_capacity = 10000 * Gb
    res_spec.disk.total_bandwidth = 314572800
    ssd_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.disk.capacity = 1000 * Gb
    res_free.disk.bandwidth = 1000
    ssd_resp.values.append(yputil.dumps_proto(res_free))
    # HDD 2000Gb
    hdd_resp = object_service_pb2.TAttributeList()
    hdd_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.disk.storage_class = 'hdd'
    res_spec.disk.total_capacity = 10000 * Gb
    res_spec.disk.total_bandwidth = 314572800
    hdd_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.disk.capacity = 2000 * Gb
    res_free.disk.bandwidth = 1000
    hdd_resp.values.append(yputil.dumps_proto(res_free))
    # GPU gpu_tesla_k40
    gpu_resp = object_service_pb2.TAttributeList()
    gpu_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.gpu.uuid = '1'
    res_spec.gpu.model = 'gpu_tesla_k40'
    res_spec.gpu.total_memory = 10000
    gpu_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.gpu.capacity = 1
    gpu_resp.values.append(yputil.dumps_proto(res_free))

    list_resource_rsp = object_service_pb2.TRspSelectObjects()
    # sas1-1111
    list_resource_rsp.results.extend([cpu_resp, mem_resp, ssd_resp, hdd_resp, gpu_resp, gpu_resp])
    # sas1-2222
    for resp in (cpu_resp, mem_resp, ssd_resp):
        resp.values[0] = yson.dumps('sas1-2222.search.yandex.net')
    list_resource_rsp.results.extend([cpu_resp, mem_resp, ssd_resp])
    # sas1-3333
    for resp in (cpu_resp, mem_resp, ssd_resp):
        resp.values[0] = yson.dumps('sas1-3333.search.yandex.net')
    list_resource_rsp.results.extend([cpu_resp, mem_resp, ssd_resp])
    # sas1-4444
    for resp in (cpu_resp, mem_resp):
        resp.values[0] = yson.dumps('sas1-4444.search.yandex.net')
    list_resource_rsp.results.extend([cpu_resp, mem_resp, ssd_resp])
    return list_resource_rsp.results


def test_list_free_nodes(ctx_mock):
    default_segment = 'default'
    ctx_mock.pod_ctl.get_nodes_spec.return_value = {
        'sas1-1111.search.yandex.net': {},
        'sas1-2222.search.yandex.net': {'network_module_id': 'SAS1-1'},
        'sas1-3333.search.yandex.net': {'network_module_id': 'SAS1-2'},
        'sas1-4444.search.yandex.net': {},
        'sas1-5555.search.yandex.net': {},
        'sas1-6666.search.yandex.net': {},
    }
    ctx_mock.pod_ctl.get_allocated_node_ids.return_value = [
        'sas1-5555.search.yandex.net',
        'sas1-6666.search.yandex.net'
    ]
    ctx_mock.pod_ctl.get_free_network_ids.return_value = frozenset(['SAS1-2'])
    ctx_mock.pod_ctl.list_resources_by_nodes.return_value = list_resources()
    ctx_mock.pod_ctl.get_all_daemon_set_pod_set_ids.return_value = [
        'yp-daemonset-sas-test-rtc-sla-tentacles-production',
        'yp-daemonset-man-pre-rtc-sla-tentacles-production',
        'yp-daemonset-sas-rtc-sla-tentacles-production',
        'yp-daemonset-man-rtc-sla-tentacles-production',
        'yp-daemonset-vla-rtc-sla-tentacles-production',
        'yp-daemonset-iva-rtc-sla-tentacles-production',
        'yp-daemonset-myt-rtc-sla-tentacles-production',
    ]

    expected_nodes = {
        'sas1-1111.search.yandex.net': vmset_pb2.ResourceInfo(
            cpu=1000,
            mem=100 * Gb,
            disk_per_storage={'ssd': 960 * Gb, 'hdd': 1950 * Gb},
            internet_address=0,
            gpu_per_model={
                'gpu_tesla_k40': 2
            },
            io_guarantees_per_storage={'ssd': 1000, 'hdd': 1000},
        ),
        'sas1-2222.search.yandex.net': vmset_pb2.ResourceInfo(
            cpu=1000,
            mem=100 * Gb,
            disk_per_storage={'ssd': 960 * Gb},
            internet_address=0,
            io_guarantees_per_storage={'ssd': 1000},
        ),
        'sas1-3333.search.yandex.net': vmset_pb2.ResourceInfo(
            cpu=1000,
            mem=100 * Gb,
            disk_per_storage={'ssd': 960 * Gb},
            internet_address=1,
            io_guarantees_per_storage={'ssd': 1000},
        ),
    }

    free_nodes = list_free_nodes_action.run(ctx_mock, default_segment)
    assert free_nodes == expected_nodes
