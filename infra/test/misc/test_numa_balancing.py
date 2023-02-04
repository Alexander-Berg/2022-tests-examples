from contextlib import contextmanager
from functools import partial
import mmap
import pytest
import re
from tempfile import TemporaryFile
import time
import kern
import os


PAGE_SIZE = 4096
FILE_SIZE = 1 << 30


@contextmanager
def _make_pagecache_mmap(size):
    with TemporaryFile() as f:
        f.truncate(size)
        with mmap.mmap(f.fileno(), size, mmap.MAP_SHARED, mmap.PROT_READ) as m:
            yield m


@contextmanager
def _make_anon_mmap(size, locked=False):
    flags = mmap.MAP_PRIVATE | mmap.MAP_ANON | kern.MAP_POPULATE
    if locked:
        flags |= kern.MAP_LOCKED
    with mmap.mmap(0, size, flags, mmap.PROT_READ | mmap.PROT_WRITE) as m:
        yield m


def _burn_cpu(t):
    s = time.clock_gettime(time.CLOCK_THREAD_CPUTIME_ID)
    while time.clock_gettime(time.CLOCK_THREAD_CPUTIME_ID) - s < t:
        1+1


def _do_reads(m, size):
    # page migrates on two consecutive hits
    for j in range(2):
        # numa balancer runs once in 1 second
        _burn_cpu(2)
        for offset in range(0, size, PAGE_SIZE):
            m.seek(offset)
            m.read_byte()


def _cgroup_numa(memcg, prefix):
    for line in memcg.get_lines('memory.numa_stat'):
        if prefix.match(line):
            results = re.findall(r'N(\d+)=(\d+)', line)
            return {int(x): int(int(y) * PAGE_SIZE) for x, y in results}
    raise LookupError


def __test_numa_mem_migration(memcg, make_mmap, size, mem_prefix):
    nodes = kern.list_numa_nodes()

    with make_mmap(size) as m:
        for n in nodes:
            os.sched_setaffinity(0, kern.numa_cpus(n))
            _do_reads(m, size)
            ns = _cgroup_numa(memcg, mem_prefix)

            min_value = size - size*0.01
            assert ns[n] >= min_value, "node={} numa_stat:\n{}".format(n, memcg.get_attr("memory.numa_stat"))
            assert max(ns.items(), key=lambda x: x[1])[0] == n, "node={} numa_stat:\n{}".format(n, memcg.get_attr("memory.numa_stat"))


@pytest.mark.skipif(kern.nr_numa_nodes() < 2, reason="not numa")
@pytest.mark.parametrize('make_mmap,mem_prefix', [
    pytest.param(_make_pagecache_mmap, re.compile('^file='), marks=pytest.mark.skipif(not kern.kernel_in('5.4.142-22'), reason="not fixed")),
    pytest.param(_make_anon_mmap, re.compile('^anon=')),
    pytest.param(partial(_make_anon_mmap, locked=True), re.compile('^unevictable=')),
])
def test_numa_mem_migration(make_cgroup, make_task, make_mmap, mem_prefix, sysctl):
    memcg = make_cgroup('memory')
    task = make_task(cgroups=[memcg])
    sysctl["kernel.numa_balancing"] = 1
    sysctl["kernel.numa_balancing_scan_period_max_ms"] = 1000
    size = 1 << 25

    task.call_func(__test_numa_mem_migration, memcg, make_mmap, size, mem_prefix)

    assert not memcg.removed()
    task.stop()
    memcg.remove()
    kern.set_sysctl("vm/drop_caches", 3)
    for i in range(5):
        if memcg.removed():
            break
        time.sleep(1)
    else:
        assert memcg.removed()


def __test_numa_cpu_migration(tmpname, cpucg):
    nodes = kern.list_numa_nodes()
    size = FILE_SIZE

    for node in nodes[:2]:
        kern.membind([node])
        with open(tmpname, 'w+') as f:
            f.truncate(size)

            m = mmap.mmap(f.fileno(), size, mmap.MAP_SHARED, mmap.PROT_READ)

            _do_reads(m, size)
            usage = cpucg.get_percpus('cpuacct.usage_percpu')
            _do_reads(m, size)
            usage1 = cpucg.get_percpus('cpuacct.usage_percpu')
            dusage = [
                sum(usage1[cpu] - usage[cpu] for cpu in kern.numa_cpus(n))
                for n in nodes
            ]
            m.close()
            assert max(nodes, key=lambda n: dusage[n]) == node, (node, dusage)


@pytest.mark.skipif(kern.nr_numa_nodes() < 2, reason="not numa")
@pytest.mark.skip(reason="not fixed")
def test_numa_cpu_migration(make_cgroup, make_temp_file, make_task):
    cpucg = make_cgroup('cpuacct')

    task = make_task(cgroups=[cpucg])

    with make_temp_file() as tmp:
        task.call_func(__test_numa_cpu_migration, tmp.name, cpucg)
