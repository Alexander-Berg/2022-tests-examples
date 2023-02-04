import pytest
import kern
import mmap
import time

MB = 1024 ** 2


def do_mmap(fd, size):
    m = mmap.mmap(fd, size)
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


@pytest.mark.xfail(not kern.kernel_in("4.19.17-2", "5.4.14-1"), reason="MADV_STOCKPILE not implemented")
@pytest.mark.parametrize('fio_rw', ["randread", "randwrite", "randrw"])
@pytest.mark.parametrize('mem_limit,mem_free,stockpile_size,stockpile_delay,fio_rate', [
    pytest.param(1024 * MB, 200 * MB, 200 * MB, 1, "100M", id="1G_200M_200M_1sec_100Mps"),
    pytest.param(1024 * MB, 500 * MB, 500 * MB, 0.1, "300M", id="1G_500M_500M_100msec_300Mps")
])
def test_madvice_stockpile_limit(make_temp_file, make_cgroup, make_fio, make_task, logger,
                                   mem_limit, mem_free, stockpile_size, stockpile_delay, fio_rw, fio_rate):
    """
    https://st.yandex-team.ru/KERNEL-186
    """
    file_size = 1024 * MB
    f = make_temp_file()

    memcg = make_cgroup('memory')
    memcg['limit_in_bytes'] = mem_limit

    task = make_task(cgroups=[memcg])
    assert memcg.has_task(task.pid)

    logger.info('populate pagecache until memory_limit')
    f.truncate(file_size)
    task.call_func(do_mmap, f.fileno(), file_size)
    assert memcg.has_task(task.pid)

    logger.debug("new usage_mb: {}".format(memcg['usage_in_bytes'] / MB))
    assert memcg['usage_in_bytes'] >= mem_limit * 0.9

    fio = make_fio(filename=f.name, size=file_size, rw=fio_rw, bs="64k", rate=fio_rate, cgroups=[memcg],
                   runtime=10, time_based=1)
    fio.start()

    start_time = time.time()
    while time.time() - start_time < 10:
        start_usage = memcg['usage_in_bytes']

        task.call_func(do_stockpile, stockpile_size)

        cur_free = mem_limit - memcg['usage_in_bytes']
        logger.debug("start_usage_mb: {}, usage_mb: {} cur_free: {}".format(start_usage / MB,
                                                                            memcg['usage_in_bytes'] / MB,
                                                                            cur_free / MB))
        assert cur_free > mem_free * 0.9

        time.sleep(stockpile_delay)

    fio.wait()
    assert fio.proc.returncode == 0
