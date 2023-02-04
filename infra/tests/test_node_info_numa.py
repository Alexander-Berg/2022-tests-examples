import mock
import os

from infra.rtc.nodeinfo.lib.modules import numa

MEMINFO = '''\
Node 0 MemTotal:       131993556 kB
Node 0 MemFree:        54861032 kB
Node 0 MemUsed:        77132524 kB
Node 0 Active:         10301012 kB
Node 0 Inactive:       61778460 kB
Node 0 Active(anon):    5920204 kB
Node 0 Inactive(anon):    68788 kB
Node 0 Active(file):    4380808 kB
Node 0 Inactive(file): 61709672 kB
Node 0 Unevictable:      709520 kB
Node 0 Mlocked:          709520 kB
Node 0 Dirty:              5992 kB
Node 0 Writeback:             8 kB
Node 0 FilePages:      66500024 kB
Node 0 Mapped:           550440 kB
Node 0 AnonPages:       6289040 kB
Node 0 Shmem:            210168 kB
Node 0 KernelStack:       13048 kB
Node 0 PageTables:        29168 kB
Node 0 NFS_Unstable:          0 kB
Node 0 Bounce:                0 kB
Node 0 WritebackTmp:          0 kB
Node 0 Slab:            2693792 kB
Node 0 SReclaimable:    2520416 kB
Node 0 SUnreclaim:       173376 kB
Node 0 AnonHugePages:         0 kB
Node 0 ShmemHugePages:        0 kB
Node 0 ShmemPmdMapped:        0 kB
Node 0 HugePages_Total:     0
Node 0 HugePages_Free:      0
Node 0 HugePages_Surp:      0
'''


def test_expand_list():
    assert numa.parse_range_list('0') == ['0']
    assert numa.parse_range_list('0-1') == ['0', '1']
    assert numa.parse_range_list('0-7,16-23') == [
        '0', '1', '2', '3', '4', '5', '6', '7',
        '16', '17', '18', '19', '20', '21', '22', '23'
    ]


def test_has_numa(monkeypatch):
    m = mock.Mock(return_value=True)
    monkeypatch.setattr(os.path, 'exists', m)
    assert numa.has_numa() is True
    m.assert_called_once_with('/sys/devices/system/node/online')
    m.reset_mock()
    m.return_value = False
    assert numa.has_numa() is False


def test_get_online_nodes():
    m = mock.mock_open(read_data='0-1\n')
    assert numa.get_online_nodes(m) == ['0', '1']
    m.assert_called_once_with('/sys/devices/system/node/online', 'r')


def test_node_path():
    assert numa.node_path('5') == '/sys/devices/system/node/node5'


def test_get_node_cpu_list():
    m = mock.mock_open(read_data='0-1\n')
    assert numa.get_node_cpu_list('4', m) == ['0', '1']
    m.assert_called_once_with('/sys/devices/system/node/node4/cpulist', 'r')


def test_get_node_mem_total():
    m = mock.mock_open(read_data=MEMINFO)
    assert numa.get_node_mem_total('4', m) == 131993556 * 1024
    m.assert_called_once_with('/sys/devices/system/node/node4/meminfo', 'r')


def test_get_node_info():
    cpulist = '0-1\n'
    meminfo = 'Node 0 MemTotal:       131993556 kB\n'
    def _open(path, *args, **kwargs):
        files = {
            '/sys/devices/system/node/node0/cpulist': mock.mock_open(read_data=cpulist),
            '/sys/devices/system/node/node0/meminfo': mock.mock_open(read_data=meminfo),
        }
        return files[path](path, *args, **kwargs)

    ni = numa.get_node_info('0', _open)
    assert ni.node == 0
    assert ni.cpus == [0, 1]
    assert ni.mem_total == 131993556 * 1024


def test_get_numa_info(monkeypatch):
    monkeypatch.setattr(os.path, 'exists', lambda x: True)
    cpulist = '0-1\n'
    meminfo = 'Node 0 MemTotal:       131993556 kB\n'
    def _open(path, *args, **kwargs):
        files = {
            '/sys/devices/system/node/online': mock.mock_open(read_data='0\n'),
            '/sys/devices/system/node/node0/cpulist': mock.mock_open(read_data=cpulist),
            '/sys/devices/system/node/node0/meminfo': mock.mock_open(read_data=meminfo),
        }
        return files[path](path, *args, **kwargs)
    ni = numa.get_numa_info(_open)
    assert len(ni.nodes) == 1
    assert ni.nodes[0].node == 0
    assert ni.nodes[0].cpus == [0, 1]
    assert ni.nodes[0].mem_total == 131993556 * 1024
