import kern
import pytest

KB = 1 << 10
MB = 1 << 20
NS = 10 ** 9


@pytest.fixture
def target_disk_bfq():
    disk = kern.make_temp_disk(kind='sd', size='4MiB', lbpu=1, delay=0)

    if disk.scheduler != 'bfq':
        disk.write_attr('queue/scheduler', 'bfq')
        disk.scheduler = 'bfq'

    yield disk

    disk.destroy()


def test_cgroup_blkio_leakage(make_cgsubsys_check, make_cgroup,
                              make_task, target_disk_bfq):
    if not kern.kernel_in('4.19.131-33'):
        pytest.xfail("not fixed")

    make_cgsubsys_check(subsys=['blkio'], delta=100)
    root_blkcg = kern.cgroup.root_cgroup('blkio')

    disk_path = target_disk_bfq.dev_path
    task = make_task()

    for i in range(1000):
        blkcg = make_cgroup('blkio')
        blkcg.attach(task.pid)

        task.check_call(['/bin/dd', 'if='+disk_path, 'of=/dev/null', 'bs=4k', 'count=1', 'iflag=direct', 'status=none'])

        root_blkcg.attach(task.pid)
        blkcg.remove()
