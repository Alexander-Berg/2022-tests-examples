import pytest
import kern
import mmap

MB = 1 << 20


def do_mmap(fd, size, **kwargs):
    m = mmap.mmap(fd, size, **kwargs)
    for offset in range(0, size, 4096):
        m.seek(offset)
        m.read_byte()
    m.close()


def do_stockpile(size, iterations=10):
    ret = 0
    for i in range(0, iterations):
        ret = kern.madvise(0, size, kern.MADV_STOCKPILE)
        if ret == 0:
            break
    return ret


def _create_cgroups(cgroups, make_temp_file, make_cgroup, make_task, logger):
    parent = None
    for limit, usage in cgroups:
        memcg = make_cgroup('memory', parent=parent)
        if limit is not None:
            memcg['limit_in_bytes'] = limit

        logger.info('populate pagecache for cgroup={}'.format(memcg.name))
        for size, flags in usage:
            f = make_temp_file(size=size)
            task = make_task(cgroups=[memcg])
            assert memcg.has_task(task.pid)
            task.call_func(do_mmap, f.fileno(), size, flags=flags)

        yield memcg
        parent = memcg


@pytest.mark.parametrize('cgroups,stockpile,expected_retcode', [
    (
        [
            (100*MB,    [(10*MB, mmap.MAP_SHARED)]),
            (None,      [(50*MB, mmap.MAP_SHARED)]),
            (None,      [(1*MB,  mmap.MAP_SHARED | kern.MAP_LOCKED)]),
        ],
        40*MB,
        0,
    ),
    (
        [
            (100*MB,    [(10*MB, mmap.MAP_SHARED)]),
            (None,      [(20*MB, mmap.MAP_SHARED | kern.MAP_LOCKED), (30, mmap.MAP_SHARED)]),
            (None,      [(1*MB,  mmap.MAP_SHARED | kern.MAP_LOCKED)]),
        ],
        40*MB,
        0
    ),
])
@pytest.mark.xfail(not kern.kernel_in("4.19.162-40", "5.4.90-7"), reason="KERNEL-511 not fixed")
def test_madvise_stockpile_cg_hierarchy(cgroups, stockpile, expected_retcode, make_temp_file, make_cgroup, make_task, logger):
    """
    https://st.yandex-team.ru/KERNEL-511
    """
    memcgs = list(_create_cgroups(cgroups, make_temp_file, make_cgroup, make_task, logger))

    memcg1_mem_usage = memcgs[0]['usage_in_bytes'] - memcgs[1]['usage_in_bytes']

    task = make_task(cgroups=[memcgs[-1]])
    assert memcgs[-1].has_task(task.pid)
    assert task.call_func(do_stockpile, stockpile) == expected_retcode

    for (mem_limit, _), memcg in zip(cgroups, memcgs):
        if mem_limit is not None:
            assert mem_limit - memcg['usage_in_bytes'] > stockpile

    assert memcgs[0]['usage_in_bytes'] - memcgs[1]['usage_in_bytes'] == pytest.approx(memcg1_mem_usage, 0.1)
