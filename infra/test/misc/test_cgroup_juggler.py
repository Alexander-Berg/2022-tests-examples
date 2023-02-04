import kern
import logging
import pytest
import psutil
import random
import time

MB = 1024 ** 2
GB = 1024 ** 3


class CgGenPool(object):
    def __init__(self, make_cgroup, each_cg_num=2, subsys=['blkio', 'cpuset', 'memory'], logger=None):
        assert each_cg_num >= 2
        assert len(subsys) > 0

        self.each_cg_num = each_cg_num
        self.subsys = subsys
        self.cg_num = each_cg_num * len(subsys)
        self.logger = logger or logging.getLogger()

        self.i = 0
        self.cgroups = []

        for i in range(self.each_cg_num):
            for subsys in self.subsys:
                params = {}
                if subsys == 'cpuset':
                    params['cpuset.cpus'] = kern.cgroup.root_cgroup('cpuset').get_attr('cpuset.cpus')
                    params['cpuset.mems'] = kern.cgroup.root_cgroup('cpuset').get_attr('cpuset.mems')
                self.cgroups.append(make_cgroup(subsys, **params))

    def get(self):
        i_prev = self.i
        self.i = (i_prev + 1) % self.cg_num
        return self.cgroups[i_prev]

    def put(self, cg):
        assert cg in self.cgroups
        return

    def destroy(self):
        for cg in self.cgroups:
            cg.remove()


class CgGenNew(object):
    def __init__(self, make_cgroup, subsys=['blkio', 'cpuset', 'memory'], logger=None):
        assert len(subsys) > 0

        self.i = 0
        self.subsys_count = len(subsys)
        self.subsys = subsys
        self.logger = logger or logging.getLogger()

        self.cgroups = []

        self.make_cgroup = make_cgroup

    def get(self):
        subsys = self.subsys[self.i]

        params = {}
        if subsys == 'cpuset':
            params['cpuset.cpus'] = kern.cgroup.root_cgroup('cpuset').get_attr('cpuset.cpus')
            params['cpuset.mems'] = kern.cgroup.root_cgroup('cpuset').get_attr('cpuset.mems')

        cg = self.make_cgroup(subsys, **params)
        self.logger.debug("CG_JUGGLER: created cg_path={}".format(cg.path))
        self.cgroups.append(cg)

        self.i = (self.i + 1) % self.subsys_count
        return cg

    def put(self, cg):
        assert cg in self.cgroups

        self.logger.debug("CG_JUGGLER: destroying requested cg_path={}, cg_threads={}".format(cg.path, cg.threads()))
        for prev_cg in self.cgroups:
            if prev_cg.subsys == cg.subsys and len(prev_cg.threads()) == 0:
                self.logger.debug("CG_JUGGLER: destroying cg_path={}".format(prev_cg.path))
                prev_cg.remove()
                self.cgroups.remove(prev_cg)

        return

    def destroy(self):
        for cg in self.cgroups:
            self.logger.debug("CG_JUGGLER: destroy(): destroying cg_path={}".format(cg.path))
            cg.remove()


class CgGenRand(object):
    def __init__(self, make_cgroup, subsys=['blkio', 'cpuset', 'memory'], logger=None):
        assert len(subsys) > 0

        self.subsys = subsys
        self.logger = logger or logging.getLogger()

        self.cgroups = []
        self.make_cgroup = make_cgroup

        for subsys in self.subsys:
            self.cgroups.append(kern.cgroup.root_cgroup(subsys))

    def get(self):
        # False - get already existing cg
        # True  - create new cg with random parent
        is_new = random.choice([False, True])
        parent = random.choice(self.cgroups)

        if not is_new:
            return parent

        params = {}
        if parent.subsys == 'cpuset':
            params['cpuset.cpus'] = parent.get_attr('cpuset.cpus')
            params['cpuset.mems'] = parent.get_attr('cpuset.mems')

        child = self.make_cgroup(parent.subsys, parent=parent, **params)
        self.cgroups.append(child)

        return child

    def put(self, cg):
        assert cg in self.cgroups
        return

    def destroy(self):
        for cg in self.cgroups:
            if cg.is_root():
                continue
            cg.remove()


@pytest.fixture
def make_cg_gen(make_cgroup):
    gens = []

    def _make_cg_gen(gen_cls, **kwargs):
        gen = gen_cls(make_cgroup, **kwargs)
        gens.append(gen)
        return gen

    yield _make_cg_gen

    for gen in gens:
        gen.destroy()


@pytest.fixture
def make_sd_disk(make_disk):
    def _make_sd_disk(**kwargs):
        return make_disk(kern.disk.ScsiDebugDisk, **kwargs)
    return _make_sd_disk


@pytest.mark.parametrize('gen_cls', [CgGenPool, CgGenNew, CgGenRand])
@pytest.mark.parametrize('workload_size', [pytest.param(1 * GB, id='1GB')])
@pytest.mark.parametrize('workload_runtime', [pytest.param(30, id='30sec')])
@pytest.mark.parametrize('workload_type, workload_opts', [
    pytest.param('fio', ['--name=test', '--ioengine=psync',
                         '--bs=4k', '-rw=randrw', '--direct=1',
                         '--time_based=1'], id='fio'),
    pytest.param('stress-ng', ['--vm', '1', '--quiet'], id='stress-ng:vm'),
    pytest.param('stress-ng', ['--iomix', '1', '--quiet'], id='stress-ng:io'),
    pytest.param('fsstress', ['-p', '1', '-n', '200000',
                              '-f', 'fsync=0', '-f', 'fdatasync=0', '-f', 'sync=0'],
                              id='fsstress'),
])
def test_cgroup_juggler(logger, make_fio, make_sd_disk, make_ram_disk, make_fs, make_cg_gen, make_task, find_bin,
                        gen_cls, workload_runtime, workload_size, workload_type, workload_opts):

    cg_gen = make_cg_gen(gen_cls=gen_cls, subsys=['blkio', 'memory', 'cpuset'])

    prev_cg = None
    curr_cg = cg_gen.get()

    if workload_type == 'fio':
        disk = make_sd_disk(size=workload_size, lbpu=1, delay=0)
        workload_opts.append('--filename='+disk.dev_path)

        workload_opts.append('--size='+str(workload_size))
        workload_opts.append('--runtime='+str(workload_runtime + 5))

    if workload_type == 'stress-ng':
        disk = make_sd_disk(size=workload_size*1.1, lbpu=1, delay=0)
        fs = make_fs(disk, mkfs_opts=['-q'])
        workload_opts.extend(['--temp-path', fs])

        if workload_opts[0] == '--vm':
            workload_opts.extend(['--vm-bytes', str(workload_size)])
        else:
            workload_opts.extend(['--iomix-bytes', str(workload_size)])

        workload_opts.extend(['--timeout', str(workload_runtime + 5)])

    if workload_type == 'fsstress':
        disk = make_sd_disk(size=workload_size, lbpu=1, delay=0)
        fs = make_fs(disk, fs_type='ext4', mkfs_opts=['-q'])
        workload_opts.extend(['-d', fs])

    workload_bin = find_bin(workload_type)
    workload = make_task(cgroups=[curr_cg])

    workload_cmd = [workload_bin] + workload_opts
    logger.debug("CG_JUGGLER: workload cmd: {}".format(workload_cmd))

    start_time = time.monotonic()
    workload.check_call(workload_cmd, recv=False)
    logger.debug("CG_JUGGLER: i=0: '{}' -> cg={}, pids={}, tids={}, stat={}".format(
                 workload_type, curr_cg.path,
                 curr_cg.processes() if not curr_cg.is_root() else 'root_cg',
                 curr_cg.threads() if not curr_cg.is_root() else 'root_cg',
                 curr_cg.get_stat('blkio.bfq.io_service_bytes', 'Total') if curr_cg.subsys == 'blkio'
                 else curr_cg['memory.usage_in_bytes'] if curr_cg.subsys == 'memory'
                 else curr_cg.get_attr('cpuset.cpus')))

    i = 0
    cg_move_delay = 0.1

    parent = psutil.Process(workload.pid)
    while time.monotonic() - start_time < workload_runtime:
        prev_cg = curr_cg
        curr_cg = cg_gen.get()

        children = parent.children(recursive=True)
        children.append(parent)
        for child in children:
            try:
                curr_cg.attach(child.pid)
            except ProcessLookupError:
                logger.debug("task {} was killed under us".format(child.pid))

        i = i + 1
        logger.debug("CG_JUGGLER: i={}: '{}' -> cg={}, pids={}, tids={}, stat={}".format(
                     i, workload_type, curr_cg.path,
                     curr_cg.processes() if not curr_cg.is_root() else 'root_cg',
                     curr_cg.threads() if not curr_cg.is_root() else 'root_cg',
                     curr_cg.get_stat('blkio.bfq.io_service_bytes', 'Total') if curr_cg.subsys == 'blkio'
                     else curr_cg['memory.usage_in_bytes'] if curr_cg.subsys == 'memory'
                     else curr_cg.get_attr('cpuset.cpus')))

        cg_gen.put(prev_cg)
        time.sleep(cg_move_delay)
    workload.stop()
