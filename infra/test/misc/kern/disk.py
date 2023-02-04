from __future__ import print_function

import os
import time
import json
import subprocess
import tempfile
import logging

from .misc import pop_unit, run, run_output
from . import kernel


if not hasattr(os, 'ST_RDONLY'):
    os.ST_RDONLY = 1
    os.ST_NOSUID = 2
    os.ST_NODEV = 4
    os.ST_NOEXEC = 8
    os.ST_NOATIME = 1024
    os.ST_RELATIME = 4096


class Disk(object):
    def __init__(self, dev=None, path=None, disks=None, logger=None):
        if dev is None:
            self.dev = os.stat(path).st_dev
        elif isinstance(dev, int):
            self.dev = dev
        elif dev.startswith('/dev/'):
            self.dev = os.stat(dev).st_rdev
        elif ':' in dev:
            self.dev = Disk.parse_majmin(dev)
        else:
            self.dev = Disk.parse_majmin(open('/sys/class/block/' + dev + '/dev').read())

        self.major = os.major(self.dev)
        self.minor = os.minor(self.dev)
        self.majmin = Disk.format_majmin(self.dev)

        self.fs_type = None
        self.fs_path = None
        self.fs_temp = None

        self.ovl_path = None

        self.mnt_identify()

        if self.fs_path is not None:
            self.frontend_paths = [self.fs_path]    # FIXME bind mounts
            self.fs_identify()

        self.dev_path = None
        self.sysfs_path = None
        self.backends = []

        if os.major(self.dev) == 0:
            self.kind = 'virtual'
            self.name = self.kind + '_' + self.majmin
        elif not os.path.exists('/sys/dev/block/' + self.majmin):
            self.kind = 'unregistered'
            self.name = self.kind + '_' + self.majmin
        else:
            self.name = os.path.basename(os.readlink('/sys/dev/block/' + self.majmin))
            self.dev_path = '/dev/' + self.name
            self.sysfs_path = '/sys/class/block/' + self.name

        self.logger = logger or logging.getLogger(str(self))

        self.model = None
        self.serial = None
        self.firmware = None
        self.wwn = None
        self.rotational = None
        self.discard = None
        self.final = False
        self.replication = 1
        self.udev = {}
        self.sysfs_disk = self.sysfs_path

        if self.sysfs_path is None:
            return

        self.bdi = BDI(self.majmin, self.logger)
        self.size = self.int_attr('size') * 512
        self.size_gb = float(self.size) / 2**30

        if self.has_attr('partition'):
            self.kind = 'partition'
            self.partition = self.int_attr('partition')
            self.partition_disk = os.path.basename(os.path.dirname(os.readlink(self.sysfs_path)))
            self.backends = [self.partition_disk]
            self.sysfs_disk = '/sys/class/block/' + self.partition_disk

            if self.has_attr('../queue/rotational'):
                self.rotational = self.int_attr('../queue/rotational') != 0

            if self.has_attr('../queue/discard_granularity'):
                self.discard = self.int_attr('../queue/discard_granularity') != 0
        else:
            self.kind = None
            for k in ['sd', 'vd', 'md', 'dm', 'nvme', 'loop', 'ram', 'nullb']:
                if self.name.startswith(k):
                    self.kind = k
                    break

        self.udev_identify()

        self.fs_uuid = self.udev.get('ID_FS_UUID')
        self.fs_label = self.udev.get('ID_FS_LABEL')
        self.partition_uuid = self.udev.get('ID_PART_ENTRY_UUID')
        self.partition_table_uuid = self.udev.get('ID_PART_TABLE_UUID')

        if self.fs_type is None:
            self.fs_type = self.udev.get('ID_FS_TYPE')

        if disks is not None:
            self.frontend_identify(disks)

        if os.path.isdir(self.sysfs_path + '/slaves'):
            self.backends = os.listdir(self.sysfs_path + '/slaves')

        self.backend_identify()

        if self.has_attr('queue/rotational'):
            self.rotational = self.int_attr('queue/rotational') != 0

        if self.has_attr('queue/discard_granularity'):
            self.discard = self.int_attr('queue/discard_granularity') != 0

        self.scheduler = None
        if self.has_attr('queue/scheduler'):
            for sched in self.read_attr('queue/scheduler').split():
                if sched[0] == '[' and sched[-1] == ']':
                    self.scheduler = sched[1:-1]
                    break
                elif len(sched) > 0:
                    self.scheduler = sched

        self.nr_hw_queues = 1
        if self.has_attr('mq'):
            self.nr_hw_queues = len(os.listdir(self.sysfs_path + '/' + "mq"))

        if self.kind == 'sd':
            self.final = True
            self.sata_identify()
        elif self.kind == 'nvme':
            self.final = True
            self.nvme_identify()
        elif self.kind == 'vd':
            self.final = True
        elif self.kind == 'md':
            self.md_identify()

    @staticmethod
    def all_disks(nonzero=True, final=False):
        disks = [Disk(dev) for dev in os.listdir('/sys/class/block')]
        result = []
        for disk in disks:
            if nonzero and disk.size == 0:
                continue
            if final and not disk.final:
                continue
            disk.frontend_identify(disks)
            result.append(disk)
        return result

    @staticmethod
    def fs_disks(path):
        disks = Disk.all_disks()
        disk = Disk(path=path, disks=disks)
        if disk.final:
            return [disk]
        return [Disk(dev, disks=disks) for dev in disk.final_backends]

    @staticmethod
    def parse_majmin(majmin):
        m = majmin.split(':', 1)
        return os.makedev(int(m[0]), int(m[1]))

    @staticmethod
    def format_majmin(dev):
        return '{}:{}'.format(os.major(dev), os.minor(dev))

    @staticmethod
    def normalize_hwid(a):
        a = a.repalce(' ', '_')
        a = a.repalce('-', '_')
        a = a.strip('_')
        a = a.upper()
        return a

    @staticmethod
    def compare_hwid(a, b):
        if a == b:
            return 100
        a = Disk.normalize_hwid(a)
        b = Disk.normalize_hwid(b)
        if a == b:
            return 99
        if len(a) > len(b):
            a, b = b, a
        if a and (a in b):
            return 100 * len(b) / len(a)
        return 0

    def __repr__(self):
        return '<Disk {} {}>'.format(self.majmin, self.name)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.destroy()

    def destroy(self):
        pass

    def has_attr(self, attr):
        return os.path.exists(self.sysfs_path + '/' + attr)

    def read_attr(self, attr):
        with open(self.sysfs_path + '/' + attr) as f:
            return f.read()[:-1]

    def int_attr(self, attr):
        return int(self.read_attr(attr))

    def write_attr(self, attr, value):
        with open(self.sysfs_path + '/' + attr, 'w') as f:
            f.write(str(value))

    def stat(self):
        return DiskStat(self.read_attr('stat'))

    def backend_identify(self):
        queue = [self.name]
        self.final_backends = []

        while queue:
            name = queue.pop(0)
            if os.path.exists('/sys/class/block/' + name + '/partition'):
                link = os.readlink('/sys/class/block/' + name)
                disk = os.path.basename(os.path.dirname(link))
                queue.append(disk)
            else:
                slaves = os.listdir('/sys/class/block/' + name + '/slaves')
                if slaves:
                    queue.extend(slaves)
                elif name not in self.final_backends and name != self.name:
                    self.final_backends.append(name)

    def frontend_identify(self, disks=None):
        if disks is None:
            disks = Disk.all_disks()
        self.frontend_disks = []
        self.frontend_paths = []
        if self.fs_path is not None:
            self.frontend_paths.append(self.fs_path)
        for disk in disks:
            if self.name in disk.final_backends:
                self.frontend_disks.append(disk.name)
                if disk.fs_path is not None:
                    self.frontend_paths.append(disk.fs_path)

    def udev_identify(self):
        env = {}
        out = subprocess.check_output(['udevadm', 'info', '-q', 'property', '-p', self.sysfs_path])
        out = out.decode('utf-8')
        for line in out.splitlines():
            key, val = line.split('=', 1)
            env[key] = val
        self.udev = env

    def mnt_identify(self):
        with open('/proc/self/mountinfo') as f:
            for line in f:
                m_id, p_id, dev, bind, mnt, opts = line.split(None, 5)
                if dev != self.majmin or bind != '/':
                    continue

                m_opts, opts = opts.split(None, 1)
                m_tags, opts = opts.split(' - ', 1)
                m_type, m_dev, fs_opts = opts.split(None, 2)

                self.fs_path = mnt
                self.fs_type = m_type
                self.fs_dev = m_dev

                self.fs_mount_id = int(m_id)
                self.fs_parent_id = int(p_id)
                self.fs_mount_opts = m_opts.split(',')

                tags = {}
                for t in m_tags.split():
                    if ':' in t:
                        k, v = t.split(':', 1)
                        tags[k] = int(v)
                    else:
                        tags[k] = True
                self.fs_mount_tags = tags

                self.fs_opts = fs_opts.strip().split(',')

                break

    def fs_identify(self):
        st = os.statvfs(self.fs_path)

        self.fs_space = st.f_blocks * st.f_frsize
        self.fs_space_used = (st.f_blocks - st.f_bfree) * st.f_frsize
        self.fs_space_free = st.f_bfree * st.f_frsize
        self.fs_space_avail = st.f_bavail * st.f_frsize

        self.fs_files = st.f_files
        self.fs_files_used = st.f_files - st.f_ffree
        self.fs_files_free = st.f_ffree
        self.fs_files_avail = st.f_favail

        self.fs_rdonly = st.f_flag & os.ST_RDONLY != 0
        self.fs_nosuid = st.f_flag & os.ST_NOSUID != 0
        self.fs_nodev = st.f_flag & os.ST_NODEV != 0
        self.fs_noexec = st.f_flag & os.ST_NOEXEC != 0
        self.fs_noatime = st.f_flag & os.ST_NOATIME != 0
        self.fs_relatime = st.f_flag & os.ST_RELATIME != 0

    def sata_identify(self):
        # sysfs scsi attributes are shorter than ata
        self.model = self.udev.get('ID_MODEL')
        self.serial = self.udev.get('ID_SERIAL_SHORT')
        self.firmware = self.udev.get('ID_REVISION')

        if 'ID_WWN_WITH_EXTENSION' in self.udev:
            self.wwn = 'wwn-' + self.udev['ID_WWN_WITH_EXTENSION']
        elif 'ID_WWN' in self.udev:
            self.wwn = 'wwn-' + self.udev['ID_WWN']

    def nvme_ctrl_id(self):
        out = subprocess.check_output(['sudo', 'nvme', 'id-ctrl', '/dev/' + self.name, '-o', 'json'])
        return json.loads(out.decode('utf-8'))

    def nvme_ns_id(self):
        out = subprocess.check_output(['sudo', 'nvme', 'id-ns', '/dev/' + self.name, '-o', 'json'])
        return json.loads(out.decode('utf-8'))

    def nvme_identify(self):
        if self.has_attr('wwid'):
            self.model = self.read_attr('device/model').strip()
            self.serial = self.read_attr('device/serial').strip()
            self.firmware = self.read_attr('device/firmware_rev').strip()
            self.wwn = 'nvme-' + self.read_attr('wwid').strip()
        else:
            try:
                ctrl = self.nvme_ctrl_id()
            except:
                raise

            assert ctrl['ver'] <= 0x010200  # FIXME use uuid from ns-descs

            self.model = ctrl['mn'].strip()
            self.serial = ctrl['sn'].strip()
            self.firmware = ctrl['fr'].strip()

            try:
                ns = self.nvme_ns_id()
            except:
                return

            if ns['nguid'] != "0" * 32:
                self.wwn = "nvme-eui." + ns['nguid']
            elif ns['eui64'] != "0" * 16:
                self.wwn = "nvme-eui." + ns['eui64']

    def md_identify(self):
        uuid = self.udev.get('MD_UUID')
        if uuid is not None:
            self.wwn = "md-uuid-" + uuid

        self.raid_level = self.read_attr('md/level')
        self.raid_chunk_size = self.int_attr('md/chunk_size')

        # FIXME
        if self.raid_level == 'raid1' or self.raid_level == 'raid10':
            self.replication = 2

        self.raid_degraded = None
        if self.has_attr('md/degraded'):
            self.raid_degraded = self.int_attr('md/degraded')

        self.raid_sync_action = None
        if self.has_attr('md/sync_action'):
            self.raid_sync_action = self.read_attr('md/sync_action')

    def dump_json(self):
        return json.dumps(self.__dict__, sort_keys=True, indent=4, separators=(',', ': '))

    def mkfs(self, fs_type='ext4', mkfs_opts=[]):
        assert self.fs_path is None
        run(['mkfs', '-t', fs_type] + mkfs_opts + [self.dev_path], logger=self.logger)
        self.fs_type = fs_type

    def mount(self, path=None, fs_opts=None):
        assert self.fs_path is None
        if path is None:
            assert self.fs_temp is None
            self.fs_temp = tempfile.mkdtemp(prefix='test-')
            path = self.fs_temp
        opts = []
        if fs_opts is not None:
            opts = ['-o', ','.join(fs_opts)]
        run(['mount', '-t', self.fs_type] + opts + [self.dev_path, path], logger=self.logger)
        self.fs_path = path

    def mount_overlay(self, path, fs_opts=None):
        assert self.fs_path is not None
        assert self.ovl_path is None
        opts = []
        if fs_opts is not None:
            opts = ['-o', ','.join(fs_opts)]
        run(['mount', '-t', 'overlay'] + opts + ['overlay', path], logger=self.logger)
        self.ovl_path = path

    def umount(self):
        assert self.fs_path is not None
        if self.ovl_path is not None:
            run(['umount', self.ovl_path], logger=self.logger)
        run(['umount', self.fs_path], logger=self.logger)
        if self.fs_temp is not None:
            os.rmdir(self.fs_temp)
            self.fs_temp = None


class DiskStat(object):
    def __init__(self, text):
        self.clock_time = time.time()

        data = [int(a) for a in text.split()]

        self.has_discard = len(data) >= 15
        self.has_flush = len(data) >= 17

        data += [0 for i in range(17 - len(data))]

        self.read_ios = data[0]
        self.read_merges = data[1]
        self.read_bytes = data[2] * 512
        self.read_time = data[3] / 1000.

        self.write_ios = data[4]
        self.write_merge = data[5]
        self.write_bytes = data[6] * 512
        self.write_time = data[7] / 1000.

        self.inflight_ios = data[8]
        self.busy_time = data[9] / 1000.
        self.total_time = data[10] / 1000.

        self.discard_ios = data[11]
        self.discard_merges = data[12]
        self.discard_bytes = data[13] * 512
        self.discard_time = data[14] / 1000.

        self.flush_ios = data[15]
        self.flush_time = data[16] / 1000.

    def __sub__(self, other):
        diff = DiskStat("")
        for key in self.__dict__:
            if key.startswith('has_'):
                setattr(diff, key, getattr(self, key) and getattr(other, key))
            else:
                setattr(diff, key, getattr(self, key) - getattr(other, key))
        return diff

    def __xor__(self, other):
        diff = DiskStat("")
        dt = self.clock_time - other.clock_time
        for key in self.__dict__:
            if key.startswith('has_'):
                setattr(diff, key, getattr(self, key) and getattr(other, key))
            else:
                setattr(diff, key, (getattr(self, key) - getattr(other, key)) / dt)
        return diff

    def dump_json(self):
        return json.dumps(self.__dict__, sort_keys=True, indent=4, separators=(',', ': '))


class BDI(object):
    def __init__(self, majmin, logger=None):
        self.majmin = majmin
        self.sysfs_path = "/sys/devices/virtual/bdi/" + self.majmin
        self.logger = logger or logging.getLogger(str(self))

    def attrs(self, prefix=''):
        names = []
        for name in os.listdir(self.sysfs_path):
            attr_path = os.path.join(self.sysfs_path, name)
            if name.startswith(prefix) and os.path.isfile(attr_path):
                names.append(name)
        return names

    def attr_path(self, attr):
        return os.path.join(self.sysfs_path, attr)

    def has_attr(self, attr):
        ret = os.path.isfile(self.attr_path(attr))
        self.logger.debug('has_attr %s -> %s', attr, ret)
        return ret

    def read_attr(self, attr):
        val = open(self.attr_path(attr), 'rb').read().decode('utf-8')
        self.logger.debug('read %s -> %s', attr, val.rstrip())
        return val

    def int_attr(self, attr):
        return int(self.read_attr(attr))

    def write_attr(self, attr, val):
        self.logger.debug('write %s <- %s', attr, val)
        open(self.attr_path(attr), 'wb', 0).write(val.encode('utf-8'))


class NullBlkDisk(Disk):
    def __init__(self, **kwargs):
        self.nullblk_module = None
        self.nullblk_config = None
        dev = self.create(**kwargs)
        super().__init__(dev=dev)

    @staticmethod
    def _init_config():
        if not os.path.ismount('/sys/kernel/config'):
            subprocess.call(['mount', '-t', 'configfs', 'configfs', '/sys/kernel/config'])
            if not os.path.ismount("/sys/kernel/config"):
                return False
        if not os.path.isdir('/sys/kernel/config/nullb'):
            kernel.load_module('null_blk', nr_devices=0)
        if not os.path.isdir('/sys/kernel/config/nullb'):
            kernel.remove_module('null_blk')
            return False
        return True

    @staticmethod
    def features():
        if hasattr(NullBlkDisk, '_features'):
            return getattr(NullBlkDisk, '_features')
        if NullBlkDisk._init_config():
            ret = open('/sys/kernel/config/nullb/features').read().split(',')
        else:
            ret = []
        setattr(NullBlkDisk, '_features', ret)
        return ret

    def nullblk_get_config(self, key):
        with open(self.nullblk_config + '/' + key, 'r') as f:
            return f.read()

    def nullblk_set_config(self, key, val):
        with open(self.nullblk_config + '/' + key, 'w') as f:
            f.write(str(val))

    def create(self, count=1, **kwargs):
        if count == 0 and self._init_config():
            # configfs interface since 4.14
            self.nullblk_config = tempfile.mkdtemp(dir="/sys/kernel/config/nullb", prefix='test-')
            try:
                size = pop_unit(kwargs, 'size', '>Mi')
                self.nullblk_set_config('size', size)
                for key, val in kwargs.items():
                    self.nullblk_set_config(key, val)
                self.nullblk_set_config('power', 1)
            except:
                os.rmdir(self.nullblk_config)
                self.nullblk_config = None
                raise
            index = int(self.nullblk_get_config('index'))
            dev = 'nullb{}'.format(index)
        elif kwargs.get('memory_backed', False):
            # fallback to brd
            if kernel.module_loaded('brd'):
                kernel.remove_module('brd')
            size = pop_unit(kwargs, 'size', '>Ki')
            kernel.load_module('brd', rd_nr=count, rd_size=size)
            self.nullblk_module = 'brd'
            dev = 'ram0'
        else:
            if kernel.module_loaded('null_blk'):
                kernel.remove_module('null_blk')
            size = pop_unit(kwargs, 'size', '>Gi')
            kernel.load_module('null_blk', nr_devices=count, gb=size, **kwargs)
            self.nullblk_module = 'null_blk'
            dev = 'nullb0'
        return dev

    def destroy(self):
        if self.nullblk_config is not None:
            self.nullblk_set_config('power', 0)
            os.rmdir(self.nullblk_config)
            self.nullblk_config = None
        if self.nullblk_module is not None:
            kernel.remove_module(self.nullblk_module)
            self.nullblk_module = None


class RamDisk(Disk):
    def __init__(self, **kwargs):
        dev = self.create(**kwargs)
        super().__init__(dev=dev)

    def create(self, count=1, **kwargs):
        if kernel.module_loaded('brd'):
            kernel.remove_module('brd')
        size = pop_unit(kwargs, 'size', '>Ki', 1024)
        kernel.load_module('brd', rd_nr=count, rd_size=size, **kwargs)
        return 'ram0'

    def destroy(self):
        if kernel.module_loaded('brd'):
            kernel.remove_module('brd')


class DmLinearDisk(Disk):
    def __init__(self, **kwargs):
        dev = self.create(**kwargs)
        super().__init__(dev=dev)

    def create(self, backend, name='test_dm_linear'):
        run(["dmsetup", "create", name, "--table", "0 {} linear {} 0".format(backend.size >> 9, backend.dev_path)])
        return '/dev/mapper/' + name

    def destroy(self):
        if os.path.exists(self.dev_path):
            run(["dmsetup",  "remove", "-f", self.dev_path])


class DmStripedDisk(Disk):
    def __init__(self, **kwargs):
        dev = self.create(**kwargs)
        super().__init__(dev=dev)

    def create(self, backends, name='test_dm_striped'):
        total_size = 0
        dev_paths = ""
        for backend in backends:
            total_size += backend.size >> 9
            dev_paths += "{} 0 ".format(backend.dev_path)

        run(["dmsetup", "create", name, "--table", "0 {} striped {} 128 {}".format(total_size, len(backends), dev_paths)])
        return '/dev/mapper/' + name

    def destroy(self):
        if os.path.exists(self.dev_path):
            run(["dmsetup",  "remove", "-f", self.dev_path])


class LoopdevDisk(Disk):
    def __init__(self, **kwargs):
        dev = self.create(**kwargs)
        super().__init__(dev=dev)

    def create(self, backend):
        lo = run_output(["losetup", "--find", "--show", backend]).strip()
        assert lo.startswith("/dev/loop")
        return lo

    def destroy(self):
        if os.path.exists(self.dev_path):
            run(["losetup", "--detach",  self.dev_path])


class MdRaidDisk(Disk):
    def __init__(self, **kwargs):
        dev = self.create(**kwargs)
        super().__init__(dev=dev)

    def create(self, backends, name='test_md_raid', level=0, **kwargs):
        self._backends = backends
        dev = '/dev/md/' + name
        chunk = pop_unit(kwargs, 'chunk', '>Ki', 512)
        run(['mdadm',
                  '--create', dev,
                  '--auto',
                  '--run',
                  '--name', name,
                  '--level', str(level),
                  '--chunk', str(chunk),
                  '--raid-devices', len(backends)] +
                  [b.dev_path for b in backends])
        return dev

    def destroy(self):
        if os.path.exists(self.dev_path):
            run(["mdadm",  "--stop", self.dev_path])
        for s in self._backends:
            s.destroy()
        self._backends = []


class ScsiDebugDisk(Disk):
    def __init__(self, **kwargs):
        dev = self.create(**kwargs)
        assert open('/sys/class/block/' + dev + '/device/model').read().strip() == 'scsi_debug'
        super().__init__(dev=dev)

    def create(self, count=1, **kwargs):
        size = pop_unit(kwargs, 'size', '>Mi', 1)
        kernel.load_module('scsi_debug', num_tgts=count, dev_size_mb=size, **kwargs)
        host = None
        for nm in os.listdir('/sys/bus/pseudo/drivers/scsi_debug/adapter0'):
            if nm.startswith('host'):
                host = int(nm[4:])
                break
        assert host is not None
        dev = os.listdir('/sys/bus/scsi/devices/{}:0:0:0/block/'.format(host))[0]
        return dev

    def destroy(self):
        kernel.remove_module('scsi_debug')


def make_temp_disk(kind, **kwargs):
    if kind == 'ram':
        return RamDisk(**kwargs)
    if kind == 'null':
        return NullBlkDisk(**kwargs)
    if kind == 'mem':
        return NullBlkDisk(memory_backed=1, **kwargs)
    if kind == 'dm':
        return DmLinearDisk(**kwargs)
    if kind == 'md':
        return MdRaidDisk(**kwargs)
    if kind == 'loop':
        return LoopdevDisk(**kwargs)
    if kind == 'sd':
        return ScsiDebugDisk(**kwargs)
    raise Exception("Unknown disk kind: " + kind)


def __rtc_disks():
    paths = []

    for path in ['/', '/home', '/place', '/ssd']:
        if os.path.ismount(path):
            paths.append(path)

    if os.path.isdir('/yt'):
        for name in os.listdir('/yt'):
            if os.path.ismount('/yt/' + name):
                paths.append('/yt/' + name)

    disks = Disk.all_disks()
    result = {}

    for path in paths:
        fs = Disk(path=path, disks=disks)
        if fs.final:
            backends = [fs]
        else:
            backends = [Disk(dev, disks=disks) for dev in fs.final_backends]

        # hdd raids might be markad as non-rotational
        rotational = False
        for disk in backends:
            rotational = rotational or disk.rotational

        result[path] = {
            'name': fs.name,
            'majmin': fs.majmin,
            'kind': fs.kind,
            'size': fs.size,
            'replication': fs.replication,
            'rotational': rotational,
            'fs_type': fs.fs_type,
            'fs_uuid': fs.fs_uuid,
            'fs_space': fs.fs_space,
            'fs_space_used': fs.fs_space_used,
            'fs_files_used': fs.fs_files_used,
            'fs_space_avail': fs.fs_space_avail,
            'fs_files_avail': fs.fs_files_avail,
        }

        result[path]['disks'] = [{
            'name': disk.name,
            'majmin': disk.majmin,
            'kind': disk.kind,
            'size': disk.size,
            'model': disk.model,
            'serial': disk.serial,
            'firmware': disk.firmware,
            'wwn': disk.wwn,
            'rotational': disk.rotational,
        } for disk in backends]

    return result


def main():
    import argparse

    def pretty_json(obj):
        return json.dumps(obj, default=lambda obj: obj.__dict__,
                          sort_keys=True, indent=4, separators=(',', ': '))

    def all_disks(args):
        disks = Disk.all_disks()
        print(pretty_json(disks))

    def final_disks(args):
        disks = Disk.all_disks(final=True)
        print(pretty_json(disks))

    def fs_disk(args):
        disks = [Disk(path=args.path)]
        print(pretty_json(disks))

    def fs_final(args):
        print(args.path)
        disks = Disk.fs_disks(args.path)
        print(pretty_json(disks))

    def rtc_disks(args):
        from api.cqueue import Client
        from library.sky.hostresolver import Resolver
        import sys

        hosts = Resolver().resolveHosts(args.hosts[0])

        table = {}

        with Client() as cqueue:
            with cqueue.run(hosts, __rtc_disks) as session:
                for host, result, err in session.wait(60):
                    if err:
                        print('Error on {0}: `{1}`'.format(host, err), file=sys.stderr)
                    else:
                        table[host] = result

        print(json.dumps(table, sort_keys=True, indent=4, separators=(',', ': ')))

    parser = argparse.ArgumentParser(description='Disks')
    subparsers = parser.add_subparsers()

    parser_list = subparsers.add_parser('all', description='List all disks')
    parser_list.set_defaults(func=all_disks)

    parser_list = subparsers.add_parser('final', description='List all final backend disks')
    parser_list.set_defaults(func=final_disks)

    parser_list = subparsers.add_parser('fs', description='List backend disks for filesystem')
    parser_list.add_argument('path', nargs='?', default=os.getcwd(), help='filesystem path')
    parser_list.set_defaults(func=fs_disk)

    parser_list = subparsers.add_parser('fs-final', description='List final backend disks for filesystem')
    parser_list.add_argument('path', nargs='?', default=os.getcwd(), help='filesystem path')
    parser_list.set_defaults(func=fs_final)

    parser_list = subparsers.add_parser('rtc-disks', description='List disks for hosts')
    parser_list.add_argument('hosts', nargs=1, help='skynet expression')
    parser_list.set_defaults(func=rtc_disks)

    args = parser.parse_args()
    args.func(args)

if __name__ == '__main__':
    main()
