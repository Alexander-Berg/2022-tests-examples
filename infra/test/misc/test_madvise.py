import pytest
import errno
import kern
import mmap
import os
import ctypes

MB = 1024 ** 2


@pytest.mark.xfail(not kern.kernel_in("4.4.161-65", "4.19.17-2", "5.4.14-1"), reason="MADV_POPULATE not implemented")
def test_madvise_populate():
    """
    https://st.yandex-team.ru/KERNEL-89
    """
    size = 40960
    pid = os.getpid()

    data = mmap.mmap(-1, size, mmap.MAP_PRIVATE | mmap.MAP_ANONYMOUS, mmap.PROT_READ | mmap.PROT_WRITE)
    addr = kern.buffer_address(data)

    count=0
    for p in kern.PageMap.iter_vm_pages(pid, addr, size):
        count += 1
        assert str(p) == "<NoPage>"
    assert count == 10

    ret = kern.madvise(addr, size, kern.MADV_POPULATE)
    assert ret == 0

    expected_flags = (kern.KPF_REFERENCED | kern.KPF_UPTODATE | kern.KPF_ACTIVE | kern.KPF_ANON)
    for p in kern.PageMap.iter_vm_pages(pid, addr, size):
        assert p.flags & expected_flags == expected_flags

    ret = kern.madvise(addr, size, kern.MADV_DONTNEED)
    assert ret == 0

    for p in kern.PageMap.iter_vm_pages(pid, addr, size):
        assert str(p) == "<NoPage>"


class MmapTask(kern.Task):
    def init(self, data_size, data_access):
        self.data_size = data_size
        self.data_access = data_access

    def mmap(self, fd):
        flags = mmap.MAP_SHARED
        if 'mlock' in self.data_access:
            flags |= kern.MAP_LOCKED
        if 'populate' in self.data_access:
            flags |= kern.MAP_POPULATE

        self.m = mmap.mmap(fd, self.data_size, flags)
        if 'touch' in self.data_access:
            for offset in range(0, self.data_size, 4096):
                self.m.seek(offset)
                self.m.read_byte()

        if 'dirty' in self.data_access:
            for offset in range(0, self.data_size, 4096):
                self.m.seek(offset)
                self.m.write_byte(0xFF)

    def munmap(self):
        self.m.close()
        self.m = None

    def mmap_munmap(self, fd):
        self.mmap(fd)
        self.munmap()


def do_stockpile(size, iterations=10):
    for i in range(0, iterations):
        ret = kern.madvise(0, size, kern.MADV_STOCKPILE)
        if ret == 0:
            break
        else:
            ret = ctypes.get_errno()
    return ret


@pytest.mark.xfail(not kern.kernel_in("4.4.161-66", "4.19.17-2", "5.4.14-1"), reason="MADV_STOCKPILE not implemented")
@pytest.mark.parametrize('size_to_free_mb,expected_retcode', [
    (10, 0),
    (99, errno.EAGAIN),
    (101, errno.ENOMEM)
])
def test_madvice_stockpile(make_temp_file, make_cgroup, make_fio, make_task, logger,
                                 size_to_free_mb, expected_retcode):

    """
    https://st.yandex-team.ru/KERNEL-186
    """
    size = 100 * MB
    file_size = size * 2
    size_to_free = size_to_free_mb * MB

    f = make_temp_file()

    memcg = make_cgroup('memory')
    memcg['limit_in_bytes'] = size

    # Lock 2MB
    mlock_task = make_task(task_cls=MmapTask, data_size=2 * MB, data_access=['mlock', 'dirty'], cgroups=[memcg])
    mlock_task.call_mmap(-1)
    assert memcg['usage_in_bytes'] >= mlock_task.data_size

    task = make_task(task_cls=MmapTask, data_size=file_size, data_access=['touch'], cgroups=[memcg])
    assert memcg.has_task(task.pid)

    logger.info('populate pagecache until memory_limit')
    f.truncate(file_size)
    task.call_mmap_munmap(f.fileno())
    assert memcg['usage_in_bytes'] >= size * 0.9

    ret = task.call_func(do_stockpile, size_to_free)

    logger.debug('usage after stockpile: {}'.format(memcg['usage_in_bytes']))
    assert expected_retcode == ret
