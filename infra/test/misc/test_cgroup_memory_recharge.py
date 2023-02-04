import kern
import mmap
import os
import pytest
import time


MB = 1024 ** 2
GB = 1024 ** 3


def do_mmap(fd, size, access):
    flags = mmap.MAP_SHARED
    if access == 'mlock':
        flags |= kern.MAP_LOCKED
    if access == 'populate':
        flags |= kern.MAP_POPULATE
    m = mmap.mmap(fd, size, flags)
    if access == 'touch':
        for offset in range(0, size, 4096):
            m.seek(offset)
            m.read_byte()
    m.close()


@pytest.fixture()
def stabilize_cg_subsys_count():
    def _stabilize_cg_subsys_count(subsys, max_iter_cnt, check_delay):
        prev_subsys_cnt = kern.cgroup.subsys_count(subsys)

        for i in range(max_iter_cnt):
            time.sleep(check_delay)

            curr_subsys_cnt = kern.cgroup.subsys_count(subsys)
            if prev_subsys_cnt == curr_subsys_cnt:
                return
            prev_subsys_cnt = curr_subsys_cnt

        pytest.fail('cgroup subsys count is not stable')

    yield _stabilize_cg_subsys_count


class CgEventWaiter():
    def __init__(self, cg, cg_attr, eventfd_cnt=0, eventfd_flags=0):
        self.cg = cg
        self.cg_attr = cg_attr

        self.event_fd = kern.eventfd(eventfd_cnt, eventfd_flags)
        self.control_fd = os.open(self.cg.attr_path(self.cg_attr), os.O_RDONLY)

        self.cg.write_attr('cgroup.event_control', ' '.join([str(self.event_fd), str(self.control_fd)]))

    def wait(self):
        num = os.read(self.event_fd, 8)
        return num

    def unregister(self):
        if self.control_fd:
            os.close(self.control_fd)
            self.control_fd = None

        if self.event_fd:
            os.close(self.event_fd)
            self.event_fd = None


@pytest.fixture
def make_cg_event_waiter():
    waiters = []

    def _make_cg_event_waiter(cg, cg_attr, eventfd_cnt=0, eventfd_flags=0):
        w = CgEventWaiter(cg, cg_attr, eventfd_cnt=eventfd_cnt, eventfd_flags=eventfd_flags)
        waiters.append(w)
        return w

    yield _make_cg_event_waiter

    for w in waiters:
        w.unregister()


@pytest.mark.parametrize('cg_hierarchy', ['plain', 'nested'])
def test_recharge_and_check(logger, make_cgroup, make_task, make_temp_file,
                            stabilize_cg_subsys_count, cg_hierarchy, make_cg_event_waiter):
    stabilize_cg_subsys_count('memory', 2, 20)

    size = 100 * MB
    check_retries = 10

    parent_cg = None
    if cg_hierarchy == 'nested':
        parent_cg = make_cgroup('memory')

    memcg_count = kern.cgroup.subsys_count('memory')
    logger.debug('initial memcg_count={}'.format(memcg_count))

    memcg_1 = make_cgroup('memory', parent=parent_cg)
    memcg_2 = make_cgroup('memory', parent=parent_cg)
    assert kern.cgroup.subsys_count('memory') == memcg_count + 2

    oom_waiter = make_cg_event_waiter(memcg_1, 'memory.oom_control')

    f = make_temp_file('/dev/shm')
    f.truncate(size)

    task_1 = make_task(cgroups=[memcg_1])
    assert memcg_1['usage_in_bytes'] < size * 0.2

    task_2 = make_task(cgroups=[memcg_2])
    assert memcg_2['usage_in_bytes'] < size * 0.2

    task_1.call_func(do_mmap, f.fileno(), size, 'touch')
    logger.debug('task_1 from memcg_1 touched file')
    assert memcg_1['usage_in_bytes'] == pytest.approx(size, rel=0.2)
    if cg_hierarchy == 'nested' and parent_cg['use_hierarchy']:
        assert parent_cg['usage_in_bytes'] == pytest.approx(size, rel=0.2)

    task_1.stop()
    memcg_1.remove()
    logger.debug('stopped task_1, removed memcg_1, but memcg_1 should stay')
    assert kern.cgroup.subsys_count('memory') == memcg_count + 2

    # blocking 'wait()' waiting for kernel event about cg destruction to start recharging
    logger.debug('waiting for cg event...')
    oom_waiter.wait()

    task_2.call_func(do_mmap, f.fileno(), size, 'touch')
    logger.debug('task_2 from memcg_2 touched file, should recharge')
    assert memcg_2['usage_in_bytes'] == pytest.approx(size, rel=0.2)

    logger.debug('memcg_1 should be gone now')
    for i in range(0, check_retries):
        logger.debug('checking for {} time'.format(i + 1))
        if kern.cgroup.subsys_count('memory') == memcg_count + 1:
            break
        time.sleep(1)

    assert kern.cgroup.subsys_count('memory') == memcg_count + 1


@pytest.mark.parametrize('access', ['mlock', 'populate', 'touch'])
@pytest.mark.parametrize('memory', ['file', 'anon'])
def test_recharge0(logger, make_cgroup, make_task, make_temp_file, memory, access):
    memcg = make_cgroup('memory')

    size = 100 << 20

    memcg['limit_in_bytes'] = int(size * 1.5)

    if 'high_limit_in_bytes' in memcg:
        memcg.get_attr('memory.high_limit_in_bytes')
        memcg['high_limit_in_bytes'] = int(size * 1.5) - (64 << 12) * kern.nr_cpus()
    else:
        logger.info('high limit is not supported')

    if access != 'mlock':
        if 'recharge_on_pgfault' in memcg:
            memcg['recharge_on_pgfault']
            memcg['recharge_on_pgfault'] = 1
        else:
            logger.info('recharge on page fault is not supported, will FAIL')

    logger.info('populate file a outside')
    a = make_temp_file('/dev/shm' if memory == 'anon' else '.')
    a.truncate(size)
    do_mmap(a.fileno(), size, access)

    logger.info('populate file b outside')
    b = make_temp_file('/dev/shm' if memory == 'anon' else '.')
    b.truncate(size)
    do_mmap(b.fileno(), size, access)

    check_anon = memory == 'anon' and 'anon.usage' in memcg

    task = make_task(cgroups=[memcg])
    assert memcg['usage_in_bytes'] < size
    if check_anon:
        assert memcg['anon.usage'] < size

    logger.info('mmap file a, should recharge')
    task.call_func(do_mmap, a.fileno(), size, access)
    assert memcg['usage_in_bytes'] >= size * 0.8
    if check_anon:
        assert memcg['anon.usage'] >= size * 0.8

    if memory == 'anon' and kern.proc_meminfo().get('SwapFree', 0) < size * 2:
        pytest.skip('not enough swap space')

    logger.info('mmap file b, should recharge and push half of file a')
    task.call_func(do_mmap, b.fileno(), size, access)
    assert memcg['usage_in_bytes'] >= size * 1.3
    if check_anon:
        assert memcg['anon.usage'] >= size * 1.3

    logger.info('truncate file a, file b should stay charged')
    a.truncate(0)
    assert memcg['usage_in_bytes'] >= size * 0.8
    if check_anon:
        assert memcg['anon.usage'] >= size * 0.8

    logger.info('truncate file b, file b should gone')
    b.truncate(0)
    assert memcg['usage_in_bytes'] < size * 0.2
    if check_anon:
        assert memcg['anon.usage'] < size * 0.2


class MmapTask(kern.Task):
    def init(self, data_type, data_size, data_access):
        self.data_type = data_type
        self.data_size = data_size
        self.data_access = data_access

    def mmap(self, fd):
        flags = mmap.MAP_SHARED
        if self.data_access == 'mlock':
            flags |= kern.MAP_LOCKED
        if self.data_access == 'populate':
            flags |= kern.MAP_POPULATE
        self.m = mmap.mmap(fd, self.data_size, flags)
        if self.data_access == 'touch':
            for offset in range(0, self.data_size, 4096):
                self.m.seek(offset)
                self.m.read_byte()

    def munmap(self):
        self.m.close()
        self.m = None

    def mmap_munmap(self, fd):
        self.mmap(fd)
        self.munmap()

    def check(self, level):
        assert self.cg_memory['usage_in_bytes'] == pytest.approx(self.data_size * level, abs=self.data_size * 0.1)
        if self.data_type == 'anon' and 'anon.usage' in self.cg_memory:
            assert self.cg_memory['anon.usage'] == pytest.approx(self.data_size * level, abs=self.data_size * 0.1)


@pytest.fixture(params=[100 << 20])
def data_size(request):
    return request.param


@pytest.fixture(params=['file', 'anon'])
def data_type(request):
    return request.param


@pytest.fixture(params=['populate', 'touch', 'mlock'])
def data_access(request):
    return request.param


@pytest.fixture
def file1(data_type, data_size, make_temp_file):
    ret = make_temp_file('/dev/shm' if data_type == 'anon' else '.')
    ret.truncate(data_size)
    return ret

file2 = file1


@pytest.fixture
def ct1(make_container, data_size, data_type, data_access, logger):
    ct = make_container(MmapTask, cg_memory={}, data_type=data_type, data_size=data_size, data_access=data_access)

    if data_type == 'anon':
        logger.info('not set limit for anon to not trigger oom without swap')
    else:
        ct.cg_memory['limit_in_bytes'] = int(data_size * 1.5)

        if 'high_limit_in_bytes' in ct.cg_memory:
            ct.cg_memory.get_attr('memory.high_limit_in_bytes')
            ct.cg_memory['high_limit_in_bytes'] = int(data_size * 1.5) - (64 << 12)
        else:
            logger.info('high limit is not supported')

    if data_access != 'mlock':
        if 'recharge_on_pgfault' in ct.cg_memory:
            ct.cg_memory['recharge_on_pgfault'] = 1
        else:
            logger.info('recharge on page fault is not supported, will FAIL')

    return ct

ct2 = ct1


def test_recharge1(file1, file2, ct1, ct2, logger, data_type):

    if 'recharge_on_pgfault' in ct1.cg_memory:
        ct1.cg_memory['recharge_on_pgfault'] = 0

    ct1.check(0)
    ct2.check(0)

    logger.info('ct1 mmap-unmap file1, should charge')
    ct1.call_mmap_munmap(file1.fileno())
    ct1.check(1)
    ct2.check(0)

    logger.info('ct2 mmap-unmap file1, should recharge')
    ct2.call_mmap_munmap(file1.fileno())
    ct1.check(0)
    ct2.check(1)

    logger.info('ct1 mmap-unmap file2, should charge')
    ct1.call_mmap_munmap(file2.fileno())
    ct1.check(1)
    ct2.check(1)

    logger.info('ct2 mmap file2, should recharge and push half of file1')
    ct2.call_mmap_munmap(file2.fileno())
    ct1.check(0)
    ct2.check(2 if data_type == 'anon' else 1.5)

    logger.info('truncate file1, file2 should stay charged')
    file1.truncate(0)
    ct1.check(0)
    ct2.check(1)

    logger.info('truncate file2, file2 should disappar')
    file2.truncate(0)
    ct1.check(0)
    ct2.check(0)


@pytest.fixture(params=[0, 1, 2])
def rechage_on_mlock(request, sysctl, logger):
    if 'vm.recharge_on_mlock' in sysctl:
        sysctl['vm.recharge_on_mlock'] = request.param
        return request.param
    elif kern.kernel_in('5.4.14-1'):
        logger.info('sysctl vm.recharge_on_mlock not found, in 5.4 assume 2')
        return 2
    elif kern.kernel_in('4.4.0-0', '4.9.0-0', '4.19.0-0'):
        logger.info('sysctl vm.recharge_on_mlock not found, assume 1')
        return 1
    else:
        logger.info('sysctl vm.recharge_on_mlock not found, assume 0')
        return 0


def test_recharge2(file1, ct1, ct2, logger, data_access, rechage_on_mlock):

    if 'recharge_on_pgfault' in ct1.cg_memory:
        ct1.cg_memory['recharge_on_pgfault'] = 0

    logger.info('ct1 mmap file1, should charge')
    ct1.call_mmap(file1.fileno())
    ct1.check(1)
    ct2.check(0)

    if data_access == 'mlock':
        logger.info('ct2 mmap file1, should not recharge')
        ct2.call_mmap(file1.fileno())
        ct1.check(1)
        ct2.check(0)
    else:
        logger.info('ct2 mmap file1, should recharge')
        ct2.call_mmap(file1.fileno())
        ct1.check(0)
        ct2.check(1)

    if data_access == 'mlock' and rechage_on_mlock != 2:
        logger.info('ct1 munmap file1')
        ct1.call_munmap()
        ct1.check(1)
        ct2.check(0)

        logger.info('ct2 munmap file1')
        ct2.call_munmap()
        ct1.check(1)
        ct2.check(0)
    else:
        logger.info('ct1 munmap file1')
        ct1.call_munmap()
        ct1.check(0)
        ct2.check(1)

        logger.info('ct2 munmap file1')
        ct2.call_munmap()
        ct1.check(0)
        ct2.check(1)
