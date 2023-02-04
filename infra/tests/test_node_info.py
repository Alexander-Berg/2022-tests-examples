import platform

import pytest
import mock

from infra.ya_salt.proto import ya_salt_pb2
from infra.rtc.nodeinfo.lib.modules.lshw_cpu import LSHWCPUInfo
from infra.rtc.nodeinfo.lib.modules.oops_disks2 import OOPSDiskInfo
from infra.rtc.nodeinfo.lib.modules.os_info import OSInfo
from infra.rtc.nodeinfo.lib.modules.location_info import LocationInfo
from infra.rtc.nodeinfo.lib.modules.net import NetworkInfo
from infra.rtc.nodeinfo.lib.modules import numa
from infra.rtc.nodeinfo.lib.node_info import pb_from_oops_info, pb_from_os_info, pb_from_location_info, \
    pb_from_network_info, gpu_present, pb_from_numa_info, infer_lshw_cpu_info


def test_pb_from_oops_info():
    node_info = ya_salt_pb2.NodeInfo()
    disks_info = [OOPSDiskInfo(
        "type_mock", "hw_mock", "/mount/mock", ["slave1_mock", "slave2_mock"], 1024**3, 'master_mock'
    )]
    pb_from_oops_info(node_info, disks_info)
    assert len(node_info.oops_disks) == 1
    assert node_info.oops_disks[0].hwInfo == disks_info[0].hw_info
    assert node_info.oops_disks[0].type == disks_info[0].type
    assert node_info.oops_disks[0].mountPoint == disks_info[0].mount_point
    assert node_info.oops_disks[0].fsSize == disks_info[0].fs_size
    assert node_info.oops_disks[0].slaves == disks_info[0].slaves
    assert node_info.oops_disks[0].name == disks_info[0].name


def test_pb_from_oops_info_with_none_values():
    node_info = ya_salt_pb2.NodeInfo()
    disks_info = [OOPSDiskInfo()]
    pb_from_oops_info(node_info, disks_info)
    assert len(node_info.oops_disks) == 1
    assert node_info.oops_disks[0].hwInfo is not None
    assert node_info.oops_disks[0].type is not None
    assert node_info.oops_disks[0].mountPoint is not None
    assert node_info.oops_disks[0].fsSize is not None
    assert node_info.oops_disks[0].slaves is not None
    assert node_info.oops_disks[0].name is not None


def test_pb_from_os_info():
    node_info = ya_salt_pb2.NodeInfo()
    os_info = OSInfo("Linux", "Gentoo base system 2077", "7.3.5", "dummy")
    pb_from_os_info(node_info, os_info)
    assert node_info.os_info.type == "Linux"
    assert node_info.os_info.version == "Gentoo base system 2077"
    assert node_info.os_info.kernel == "7.3.5"
    assert node_info.os_info.codename == "dummy"


def test_pb_from_os_info_with_none_values():
    node_info = ya_salt_pb2.NodeInfo()
    os_info = OSInfo()
    pb_from_os_info(node_info, os_info)
    assert node_info.os_info.type == "unknown os type"
    assert node_info.os_info.version == "unknown os version"
    assert node_info.os_info.kernel == "unknown kernel version"
    assert node_info.os_info.codename == "unknown os codename"


def test_pb_from_location_info():
    node_info = ya_salt_pb2.NodeInfo()
    location_info = LocationInfo(
        country="RU",
        city="SAS",
        building="SASTA",
        line="SAS-1.2.1",
        rack="6"
    )
    pb_from_location_info(node_info, location_info)
    assert node_info.location_info.country == "RU"
    assert node_info.location_info.city == "SAS"
    assert node_info.location_info.building == "SASTA"
    assert node_info.location_info.line == "SAS-1.2.1"
    assert node_info.location_info.rack == "6"


def test_pb_from_location_info_with_none_values():
    node_info = ya_salt_pb2.NodeInfo()
    location_info = LocationInfo()
    pb_from_location_info(node_info, location_info)
    assert node_info.location_info.country == "unknown country"
    assert node_info.location_info.city == "unknown city"
    assert node_info.location_info.building == "unknown building"
    assert node_info.location_info.line == "unknown dc line"
    assert node_info.location_info.rack == "unknown rack"


def test_pb_from_network_info():
    node_info = ya_salt_pb2.NodeInfo()
    net_info = NetworkInfo(
        'eth0',
        10000,
        'host.mock',
        'fb-host.mock',
        'fc00::/64',
        'fc01::/64',
        'fc02::0:1',
        'fc02::0:2'
    )
    pb_from_network_info(node_info, net_info)
    assert node_info.network_info.interface_name == 'eth0'
    assert node_info.network_info.bandwidth == 10000
    assert node_info.network_info.bb_fqdn == 'host.mock'
    assert node_info.network_info.fb_fqdn == 'fb-host.mock'
    assert node_info.network_info.mtn_prefix == 'fc00::/64'
    assert node_info.network_info.mtn_fb_prefix == 'fc01::/64'
    assert node_info.network_info.bb_ipv6_addr == 'fc02::0:1'
    assert node_info.network_info.fb_ipv6_addr == 'fc02::0:2'


def test_pb_from_network_info_with_none_values():
    node_info = ya_salt_pb2.NodeInfo()
    net_info = NetworkInfo()
    with pytest.raises(Exception):
        pb_from_network_info(node_info, net_info)


def test_gpu_present():
    tags = {'rtc.gpu-none', 'rtc', 'yasm'}
    assert gpu_present(tags) is False
    tags.remove('rtc.gpu-none')
    assert gpu_present(tags) is False
    tags.add('rtc.gpu-nvidia')
    assert gpu_present(tags) is True
    tags.remove('rtc.gpu-nvidia')
    tags.add('rtc.gpu-nvidia-vfio')
    assert gpu_present(tags) is True
    tags.remove('rtc.gpu-nvidia-vfio')
    tags.add('rtc.gpu-amd')
    assert gpu_present(tags) is True


def test_pb_from_numa_info():
    ni = ya_salt_pb2.NodeInfo()
    numa_info = numa.NUMAInfo([numa.NUMANodeInfo(0, [0, 1], 1024**4), numa.NUMANodeInfo(1, [2, 3], 1024**4)])
    pb_from_numa_info(ni, numa_info)
    assert len(ni.cpu_info.numa_nodes) == 2
    assert len(ni.mem_info.numa_nodes) == 2
    assert ni.cpu_info.numa_nodes[0].node == 0
    assert ni.cpu_info.numa_nodes[0].cpus == [0, 1]
    assert ni.cpu_info.numa_nodes[1].node == 1
    assert ni.cpu_info.numa_nodes[1].cpus == [2, 3]
    assert ni.mem_info.numa_nodes[0].node == 0
    assert ni.mem_info.numa_nodes[0].total_bytes == 1024 ** 4
    assert ni.mem_info.numa_nodes[1].node == 1
    assert ni.mem_info.numa_nodes[1].total_bytes == 1024 ** 4


def test_infer_lshw_cpu_info_amd64(monkeypatch):
    monkeypatch.setattr(platform, 'machine', lambda: 'amd64')
    ni = ya_salt_pb2.NodeInfo()
    err = infer_lshw_cpu_info(ni, None)
    assert err is None
    assert ni.cpu_info.model_name == ''


def test_infer_lshw_cpu_info_aarch64(monkeypatch):
    monkeypatch.setattr(platform, 'machine', lambda: 'aarch64')
    ni = ya_salt_pb2.NodeInfo()
    lshw_rv = [
        LSHWCPUInfo("ARMv8 (Q80-30)", "Ampere(R)", 80, 80),
        LSHWCPUInfo("ARMv8 (Q80-30)", "Ampere(R)", 80, 80),
    ]
    lshw = mock.Mock(return_value=(lshw_rv, None,))
    err = infer_lshw_cpu_info(ni, lshw)
    assert err is None
    assert ni.cpu_info.model_name == 'ARMv8 (Q80-30)'
    assert ni.cpu_info.vendor_id == 'Ampere(R)'
    assert ni.cpu_info.cpus == 160
    assert ni.cpu_info.cores == 160
    assert ni.cpu_info.sockets == 2

    lshw.reset_mock()
    lshw.return_value=(None, 'error mock',)
    err = infer_lshw_cpu_info(ni, lshw)
    assert err == 'error mock'
