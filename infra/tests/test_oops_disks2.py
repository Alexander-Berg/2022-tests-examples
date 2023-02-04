import mock
import collections
from infra.rtc.nodeinfo.lib.modules import oops_disks2

UDEV_DATA = '''S:disk/by-uuid/477d7458-36d2-4da3-a85f-1ffa0f1d0c48
S:disk/by-partuuid/2a9f96b1-9279-4d4f-ac8e-6da8f76e261f
S:disk/by-path/pci-0000:00:05.0-part1
S:disk/by-path/virtio-pci-0000:00:05.0-part1
S:disk/by-label/rootfs
W:13
I:1012523
E:ID_SERIAL=model_serial_mock
E:ID_PART_TABLE_TYPE=gpt
E:ID_PART_TABLE_UUID=d7503d39-ab86-4f32-984f-c03cc1520b14
E:ID_PATH=pci-0000:00:05.0
E:ID_PATH_TAG=pci-0000_00_05_0
E:ID_FS_LABEL=rootfs
E:ID_FS_LABEL_ENC=rootfs
E:ID_FS_UUID=477d7458-36d2-4da3-a85f-1ffa0f1d0c48
E:ID_FS_UUID_ENC=477d7458-36d2-4da3-a85f-1ffa0f1d0c48
E:ID_FS_VERSION=1.0
E:ID_FS_TYPE=ext4
E:ID_FS_USAGE=filesystem
E:ID_PART_ENTRY_SCHEME=gpt
E:ID_PART_ENTRY_UUID=2a9f96b1-9279-4d4f-ac8e-6da8f76e261f
E:ID_PART_ENTRY_TYPE=0fc63daf-8483-4772-8e79-3d69d8477de4
E:ID_PART_ENTRY_FLAGS=0x4
E:ID_PART_ENTRY_NUMBER=1
E:ID_PART_ENTRY_OFFSET=2048
E:ID_PART_ENTRY_SIZE=74857567
E:ID_PART_ENTRY_DISK=252:0
G:systemd
'''


def test_get_mountpoint_numbers():
    def _listdir(_):
        return ['disk0', 'disk1', 'nvme0', 'ssd0', 'logs', 'run', 'something']

    def _maj_min(path):
        m = {
            '/': (8, 2),
            '/ssd': (8, 2),
            '/place': (9, 1),
            '/home': (8, 2),
            '/yt/disk0': (10, 1),
            '/yt/disk1': (11, 1),
            '/yt/nvme0': (12, 1),
            '/yt/ssd0': (13, 1),
            '/yt/logs': (8, 2),
            '/yt/run': (8, 2),
            '/yt/something': (8, 2),
        }
        return m[path]

    expected = {
        (8, 2): '/',
        (9, 1): '/place',
        (10, 1): '/yt/disk0',
        (11, 1): '/yt/disk1',
        (12, 1): '/yt/nvme0',
        (13, 1): '/yt/ssd0',
    }
    rv = oops_disks2.get_mountpoint_numbers(_listdir, _maj_min, lambda x: True)
    assert rv == expected


def test_read_udev_info():
    mopen = mock.mock_open(read_data=UDEV_DATA)
    udev = oops_disks2.read_udev_info(1, 2, udev_prefix='/udev', open_func=mopen)
    mopen.assert_called_once_with('/udev/b1:2', 'r')
    assert udev['ID_FS_UUID'] == '477d7458-36d2-4da3-a85f-1ffa0f1d0c48'


def test_read_device_without_slaves():
    def _isdir(path):
        m = {
            '/sys/block/sda/slaves': True,
            '/sys/block/sda/sda1/slaves': False,
            '/sys/block/sda/sda2/slaves': False,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/sda/slaves': [],
            '/sys/block/sda': ['one', 'two', 'sda1', 'sda2'],
            '/sys/block/sda/sda1': ['one', 'two'],
            '/sys/block/sda/sda2': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/sda/dev': '8:0\n',
            '/sys/block/sda/sda1/dev': '8:1\n',
            '/sys/block/sda/sda2/dev': '8:2\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b8:0': UDEV_DATA,
            '/run/udev/data/b8:1': UDEV_DATA,
            '/run/udev/data/b8:2': UDEV_DATA,
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('sda', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert len(devices) == 3
    assert devices[0].name == 'sda'
    assert devices[0].type == 'HDD'
    assert devices[0].major == 8
    assert devices[0].minor == 0
    assert devices[0].model_serial == 'model_serial_mock'
    assert devices[1].name == 'sda1'
    assert devices[2].name == 'sda2'


def test_read_device_with_slaves():
    def _isdir(path):
        m = {
            '/sys/block/md2/slaves': True,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/md2/slaves': ['sda2', 'sdb2'],
            '/sys/block/md2': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/md2/dev': '9:2\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b9:2': 'E:K1=V1\nE:MD_NAME=2\nE:K2=V2\n',
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('md2', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert len(devices) == 1
    assert devices[0].name == 'md2'
    assert devices[0].type == 'HDD'
    assert devices[0].major == 9
    assert devices[0].minor == 2
    assert devices[0].model_serial == '2'
    assert devices[0].slaves == ['sda2', 'sdb2']


def test_enumerate_devices():
    def _isfile(p):
        m = {
            '/sys/block/sda/dev': True,
            '/sys/block/sdb/dev': True,
            '/sys/block/nvme0c33n1/dev': False,
        }
        return m[p]

    def _read_device(name):
        m = {
            'sda': oops_disks2.DevInfo('sda_maj', 'sda_min', 'sda', 'mock', [], None, 'HDD'),
            'sdb': oops_disks2.DevInfo('sdb_maj', 'sdb_min', 'sdb', 'mock', [], None, 'HDD'),
        }
        return [m[name]]

    def _listdir(path):
        return ['nvme0c33n1', 'sda', 'sdb']

    devices = oops_disks2.enumerate_devices(listdir=_listdir, rd=_read_device, isfile=_isfile)
    assert len(devices) == 2
    assert ('sda_maj', 'sda_min') in devices
    assert ('sdb_maj', 'sdb_min') in devices


def test_get_dev_type():
    root = oops_disks2.DevInfo('sda_maj', 'sda_min', 'sda', 'mock', [], None, 'HDD')
    c1 = oops_disks2.DevInfo('sda_maj', 'sda_min', 'sda1', 'mock', [], root, None)
    assert oops_disks2.get_dev_type(c1) == 'HDD'
    assert oops_disks2.get_dev_type(root) == 'HDD'


def test_oops_disks2():
    def _mounts():
        return {
            (9, 2): '/',
        }

    def _devs():
        return {
            (9, 2): oops_disks2.DevInfo(9, 2, 'md2', '2', ['sda2', 'sdb2'], None, 'HDD'),
            (7, 2): oops_disks2.DevInfo(7, 2, 'sda2', 'mock', [], None, 'HDD'),
            (8, 2): oops_disks2.DevInfo(7, 2, 'sdb2', 'mock', [], None, 'HDD'),
        }
    StatVFS = collections.namedtuple('StatVFS', 'f_blocks,f_frsize')
    def _statvfs(p):
        return StatVFS(512, 4096)

    info = sorted(oops_disks2.oops_disks2(statvfs=_statvfs, mounts=_mounts, devs=_devs, add_devs=lambda *args, **kwargs: None), key=lambda x: x.name)
    assert info[0].mount_point == '/'
    assert info[0].name == 'md2'
    assert info[0].fs_size == 512*4096
    assert info[1].name == 'sda2'
    assert info[2].name == 'sdb2'


def test_read_device_name_bug():
    def _isdir(path):
        m = {
            '/sys/block/sda/slaves': True,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/sda/slaves': [],
            '/sys/block/sda': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/sda/dev': '8:0\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b8:0': 'E:ID_SERIAL=word-and-sword\n',
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('sda', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert devices[0].model_serial == 'word'


def test_read_device_name_unknown():
    def _isdir(path):
        m = {
            '/sys/block/sda/slaves': True,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/sda/slaves': [],
            '/sys/block/sda': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/sda/dev': '8:0\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b8:0': 'E:UNKNOWN=word-and-sword\n',
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('sda', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert devices[0].model_serial is None


def test_read_device_name_md():
    def _isdir(path):
        m = {
            '/sys/block/sda/slaves': True,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/sda/slaves': [],
            '/sys/block/sda': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/sda/dev': '8:0\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b8:0': 'E:MD_NAME=word-and-sword\n',
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('sda', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert devices[0].model_serial == 'word-and-sword'


def test_read_device_name_md_uuid():
    def _isdir(path):
        m = {
            '/sys/block/sda/slaves': True,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/sda/slaves': [],
            '/sys/block/sda': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/sda/dev': '8:0\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b8:0': 'E:MD_UUID=uu-id\n',
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('sda', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert devices[0].model_serial == 'uu-id'


def test_read_device_name_lvm():
    def _isdir(path):
        m = {
            '/sys/block/sda/slaves': True,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/sda/slaves': [],
            '/sys/block/sda': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/sda/dev': '8:0\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b8:0': 'E:DM_NAME=dm-name\n',
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('sda', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert devices[0].model_serial == 'dm-name'


def test_read_device_name_spaces():
    def _isdir(path):
        m = {
            '/sys/block/sda/slaves': True,
        }
        return m[path]

    def _listdir(path):
        path = path.rstrip('/')
        m = {
            '/sys/block/sda/slaves': [],
            '/sys/block/sda': ['one', 'two'],
        }
        return m[path]

    def _mopen(path, *args, **kwargs):
        m = {
            '/sys/block/sda/dev': '8:0\n',
            '/sys/block/sda/queue/rotational': '1',
            '/run/udev/data/b8:0': 'E:ID_SERIAL=word and sword\n',
        }
        if path not in m:
            raise IOError('{} does not exists'.format(path))
        return mock.mock_open(read_data=m[path])(path, *args, **kwargs)

    devices = oops_disks2.read_device('sda', open_func=_mopen, listdir=_listdir, isdir=_isdir)
    assert devices[0].model_serial == 'word_and_sword'


def test_mix_in_additional_devices():
    existing = [oops_disks2.DevInfo(9, 2, 'sda2', None, [], None, "HDD")]
    additional_devices = {'sda3'}
    def _listdir(_):
        return ['p2', 'p3', 'p4']

    def _readlink(p):
        m = {
            '/dev/disk/by-uuid/p2': '../../sda2',
            '/dev/disk/by-uuid/p3': '../../sda3',
            '/dev/disk/by-uuid/p4': '../../sda4',
        }
        return m[p]

    oops_disks2.mix_in_additional_devices(existing, additional_devices, listdir=_listdir, readlink=_readlink)
    assert len(additional_devices) == 2
    assert 'sda4' in additional_devices


def test_guess_md_parent_name():
    assert oops_disks2.guess_md_parent_name('sda') == 'sda'
    assert oops_disks2.guess_md_parent_name('sda2') == 'sda'
    assert oops_disks2.guess_md_parent_name('vda2') == 'vda'
    assert oops_disks2.guess_md_parent_name('sdp2') == 'sdp'
    assert oops_disks2.guess_md_parent_name('nvme0n1p1') == 'nvme0n1'
    assert oops_disks2.guess_md_parent_name('nvme0n1p4') == 'nvme0n1'
    assert oops_disks2.guess_md_parent_name('nvme0c33n1p2') == 'nvme0c33n1'
    assert oops_disks2.guess_md_parent_name('nvme0c33n1') == 'nvme0c33n1'


def test_oops_disk_info_eq():
    i1 = oops_disks2.OOPSDiskInfo(name='mock')
    i2 = oops_disks2.OOPSDiskInfo(name='mock')
    assert i1 == i2
    i1.name = 'sdm2'
    i1.type = None
    i2.type = 'HDD'
    i2.name = 'sdm2'
    assert i1 == i2
