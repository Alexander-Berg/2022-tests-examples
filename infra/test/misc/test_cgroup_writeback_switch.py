import pytest
import kern
from fcntl import fcntl


# https://st.yandex-team.ru/KERNEL-273


def get_writeback(fn):
    try:
        return fcntl(open(fn), 0x59410001)
    except:
        return None


def test_cgroup_writeback_switch(make_ram_disk, make_fs, make_fio, make_cgroup):

    # create memory-backed filesystem
    disk = make_ram_disk(size_MiB=4)
    fs = make_fs(disk)
    fn = fs + '/test'

    memcg = make_cgroup('memory')

    # create file with writeback linked to memcg
    fio = make_fio(cgroups=[memcg], filename=fn, rw='write', bs='4k', filesize='4k', size='4k')
    fio.run()

    print('Initial writeback', get_writeback(fn))

    # cpu-bound load which should consume all cpus
    fio = make_fio(cgroups=[memcg], filename=fn, ioengine='libaio', direct=1, rw='read', bs='4k', time_based=1, runtime=5, numjobs=kern.nr_cpus(), invalidate=0)

    # measure iops within same cgroup
    iops_before = fio.run()['read']['iops']

    # recreate cgroup, inode writeback should die after that
    memcg.remove()
    memcg.create()

    # measure iops within another cgroup while original is dead
    iops_after = fio.run()['read']['iops']

    print('Final writeback', get_writeback(fn))

    # if each read tries to switch wb cgwb_lock contention will reduce performance for cpu-bound load
    assert iops_after == pytest.approx(iops_before, rel=0.5)
