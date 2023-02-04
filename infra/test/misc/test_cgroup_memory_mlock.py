import kern
import mmap
import os
import pytest
import sys
import time


MB = 1024 ** 2
XATTR_NAME = 'user.yndx.mlock'


class Mmap(object):
    def __init__(self, label, file, size, access):
        self.label = label
        self.file = file
        self.size = size
        self.access = access
        self.m = None

    def mmap(self):
        assert self.m is None

        flags = mmap.MAP_SHARED
        if 'mlock' in self.access:
            flags |= kern.MAP_LOCKED
        if 'populate' in self.access:
            flags |= kern.MAP_POPULATE

        fd = -1 if self.file is None else self.file.fileno()

        self.m = mmap.mmap(fd, self.size, flags)
        if 'touch' in self.access:
            self.touch()
        if 'dirty' in self.access:
            self.dirty()

    def munmap(self):
        assert self.m is not None
        self.m.close()
        self.m = None

    def munlock(self):
        assert self.m is not None
        addr = kern.buffer_address(self.m)
        kern.munlock(addr, self.size)

    def touch(self):
        assert self.m is not None
        for offset in range(0, self.size, 4096):
            self.m.seek(offset)
            self.m.read_byte()

    def dirty(self):
        assert self.m is not None
        for offset in range(0, self.size, 4096):
            self.m.seek(offset)
            self.m.write_byte(0xFF)


class MmapTask(kern.Task):
    def init(self, props):
        self.mmap_list = []
        for prop in props:
            (label, file, size, access) = prop
            obj = Mmap(label, file, size, access)
            self.mmap_list.append(obj)
        assert len(self.mmap_list) > 0

    def mmap(self):
        for m in self.mmap_list:
            m.mmap()

    def munmap(self):
        for m in self.mmap_list:
            m.munmap()

    def munlock(self):
        for m in self.mmap_list:
            m.munlock()

    def munlockall(self):
        kern.munlockall()

    def check_page_flags(self, expected_flags):
        assert len(self.mmap_list) == len(expected_flags)

        pid = os.getpid()

        for mm, fl in zip(self.mmap_list, expected_flags):
            addr = kern.buffer_address(mm.m)
            for p in kern.PageMap.iter_vm_pages(pid, addr, mm.size):
                assert p.flags & fl == fl


def reopen(oldfile):
    newfile = open(oldfile.name, oldfile.mode)
    assert newfile is not None
    return newfile


def file_set_exec(file):
    os.fchmod(file.fileno(), 0o777)
    return reopen(file)


def file_set_xattr(file):
    kern.fsetxattr(file.fileno(), XATTR_NAME, b'\x01', 0)
    return reopen(file)


@pytest.mark.xfail(not kern.kernel_in('5.4.174-29'), reason="not implemented")
def test_memory_cgroup_mlock_populate(logger, make_temp_file, make_cgroup, make_task):
    file_size = 50 * MB

    memcg = make_cgroup('memory')
    memcg['mlock_policy'] = 1 # lock all files

    file = make_temp_file()
    file.truncate(file_size)

    props = [('file', file, file_size, ['populate'])]
    expected_flags = [(kern.KPF_UNEVICTABLE | kern.KPF_MLOCKED)]

    task = make_task(task_cls=MmapTask, props=props, cgroups=[memcg])

    task.call_mmap()
    task.call_check_page_flags(expected_flags)
    task.call_munmap()


@pytest.mark.xfail(not kern.kernel_in('5.4.174-29'), reason="not implemented")
@pytest.mark.parametrize('method', ['munlock', 'munlockall'])
@pytest.mark.parametrize('policy, access_flags, should_be_mlocked', [
    pytest.param(0, ['populate', 'mlock'], False),
    pytest.param(1, ['populate'], True),
])
def test_memory_cgroup_mlock_check_unlock(logger, make_temp_file, make_cgroup, make_task,
                                          method, policy, access_flags, should_be_mlocked):
    file_size = 50 * MB

    memcg = make_cgroup('memory')
    memcg['mlock_policy'] = policy

    file = make_temp_file()
    file.truncate(file_size)

    props = [('file', file, file_size, access_flags)]
    expected_flags_locked = [(kern.KPF_UNEVICTABLE | kern.KPF_MLOCKED)]
    expected_flags_unlocked = [0]
    expected_flags_2nd = expected_flags_locked if should_be_mlocked else expected_flags_unlocked

    task = make_task(task_cls=MmapTask, props=props, cgroups=[memcg])

    task.call_mmap()
    task.call_check_page_flags(expected_flags_locked)

    if method == 'munlock':
        task.call_munlock()
    elif method == 'munlockall':
        task.call_munlockall()
    else:
        pytest.fail('unknown method')

    task.call_check_page_flags(expected_flags_2nd)
    task.call_munmap()



def generate_params():
    policies = [0, 1, 2, 3]
    attribs = [[], ['exec'], ['xattr'], ['exec', 'xattr']]
    flags = (kern.KPF_UNEVICTABLE | kern.KPF_MLOCKED)

    for policy in policies:
        for attrib in attribs:
            if policy == 0: # disabled
                should_be_dead = False
                expected_flags = 0
            elif policy == 1: # lockall
                should_be_dead = True
                expected_flags = 0
            elif policy == 2: # executable
                should_be_dead = False
                expected_flags = 0 if 'exec' not in attrib else flags
            elif policy == 3: # xattr
                should_be_dead = False
                expected_flags = 0 if 'xattr' not in attrib else flags
            yield pytest.param(policy, attrib, should_be_dead, expected_flags)


@pytest.mark.xfail(not kern.kernel_in('5.4.174-29'), reason="not implemented")
@pytest.mark.parametrize('policy, attr, should_be_dead, expected_flags', list(generate_params()))
def test_memory_cgroup_mlock(logger, make_temp_file, make_cgroup, make_task,
                             policy, attr, should_be_dead, expected_flags):
    file_size = 50 * MB
    anon_size = 50 * MB

    tmpfile = make_temp_file()
    tmpfile.truncate(file_size)

    tmpfile2 = make_temp_file()
    tmpfile2.truncate(file_size)

    # Attributes cached on file open so need to reopen file to take effect of exec/xattr
    file = tmpfile
    if 'exec' in attr:
        file = file_set_exec(tmpfile)
    if 'xattr' in attr:
        file = file_set_xattr(tmpfile)

    memcg = make_cgroup('memory')
    memcg['mlock_policy'] = policy
    memcg['limit_in_bytes'] = file_size + anon_size + 1 * MB # extra MB for runtime costs

    props = [
        ('file', file, file_size, ['dirty']),
        ('bufs', tmpfile2, file_size, ['populate']),
        ('anon', None, anon_size, ['populate']),
    ]

    flags = [
        expected_flags,
        0,
        0,
    ]

    task = make_task(task_cls=MmapTask, props=props, cgroups=[memcg])

    try:
        task.call_mmap()
    except EOFError:
        # Task may be killed due to oom
        pass
    except:
        raise

    oom_kill = memcg.get_stats('memory.oom_control')['oom_kill'] == 1
    assert should_be_dead == (oom_kill and not task.is_alive() and not memcg.processes())

    if not should_be_dead:
        task.call_check_page_flags(flags)
        task.call_munmap()


class MmapForkTask(MmapTask):
    def oom_score_adj_save(self):
        self.oom_score_adj = kern.task_read_attr(os.getpid(), 'oom_score_adj')

    def oom_score_adj_restore(self):
        kern.task_write_attr(os.getpid(), 'oom_score_adj', self.oom_score_adj)

    def oom_score_adj_set(self, value):
        kern.task_write_attr(os.getpid(), 'oom_score_adj', str(value))

    def invoke(self, expected_flags):
        self.mmap()
        self.check_page_flags(expected_flags)

        self.oom_score_adj_save()
        self.oom_score_adj_set(-1000)

        pid = os.fork()
        assert pid >= 0

        if pid == 0: # child
            self.oom_score_adj_restore()
            for m in self.mmap_list:
                m.dirty()
            self.check_page_flags(expected_flags)
            sys.exit(0)
        else:
            waitpid, status = os.waitpid(pid, 0)
            assert pid == waitpid
            self.oom_score_adj_restore()
            self.check_page_flags(expected_flags)


@pytest.mark.xfail(not kern.kernel_in('5.4.174-29'), reason="not implemented")
@pytest.mark.parametrize('mappings, limit_mb, child_should_be_dead', [
    pytest.param(('file', 'bufs'), 100, False),
    pytest.param(('file', 'anon'), 100, True),
])
def test_memory_cgroup_mlock_forked(logger, make_temp_file, make_cgroup, make_task,
                                    mappings, limit_mb, child_should_be_dead):
    size = 50 * MB

    tmpfiles = []
    props = []
    expected_flags = []

    def _make_file(xa):
        tmp = make_temp_file()
        tmp.truncate(size)
        tmpfiles.append(tmp)
        return tmp if not xa else file_set_xattr(tmp)

    for item in mappings:
        if item == 'file':
            props.append((item, _make_file(True), size, ['populate']))
            expected_flags.append(kern.KPF_UNEVICTABLE | kern.KPF_MLOCKED)
        elif item == 'bufs':
            props.append((item, _make_file(False), size, ['populate']))
            expected_flags.append(0)
        elif item == 'anon':
            props.append((item, None, size, ['populate']))
            expected_flags.append(0)
        else:
            pytest.fail('unknown mapping item')

    memcg = make_cgroup('memory')
    memcg['mlock_policy'] = 3 # auto mlock only files with xattr
    memcg['limit_in_bytes'] = (limit_mb + 1) * MB # extra MB for runtime costs

    task = make_task(task_cls=MmapForkTask, props=props, cgroups=[memcg])
    task.call_invoke(expected_flags)

    oom_kill = memcg.get_stats('memory.oom_control')['oom_kill'] == 1
    assert oom_kill == child_should_be_dead

    procs = memcg.processes()
    assert procs and len(procs) == 1 and procs[0] == task.pid

