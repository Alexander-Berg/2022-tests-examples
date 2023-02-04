import pytest

from maps.pylibs.yt.lib import BinaryCmd, Limits
import maps.analyzer.pylibs.mt_jobs as mt_jobs


@pytest.fixture
def cmd():
    return BinaryCmd('binary')


@pytest.fixture
def limits(request):
    return Limits(tmpfs_size=request.param[0], memory_limit=request.param[1])


@pytest.fixture
def threads(request):
    return request.param


@pytest.fixture(params=(256 * 2**20,))
def data_size_per_thread(request):
    return request.param


@pytest.fixture
def mem_per_thread(request):
    return request.param


@pytest.mark.parametrize('limits, threads, mem_per_thread', [([0, 0], 10, 512 * 2**20)])
def test_set_threads(cmd, data_size_per_thread, mem_per_thread, limits, threads):
    spec, op_spec = {}, {}
    on_limits_set = mt_jobs.set_threads_count(data_size_per_thread, mem_per_thread, threads)
    on_limits_set(cmd, limits, spec, op_spec)

    assert spec['data_size_per_job'] == data_size_per_thread * threads
    assert op_spec['cpu_limit'] == threads
    assert op_spec['memory_limit'] == mem_per_thread * threads
    assert ('--threads', threads) in cmd.params.params


@pytest.mark.parametrize('limits, threads, mem_per_thread', [([0, 0], 10, 512 * 2**20)])
def test_set_threads_with_cpu_limit(cmd, data_size_per_thread, mem_per_thread, limits, threads):
    spec, op_spec = {}, {'cpu_limit': 5}
    on_limits_set = mt_jobs.set_threads_count(data_size_per_thread, mem_per_thread, threads)
    on_limits_set(cmd, limits, spec, op_spec)

    assert spec['data_size_per_job'] == data_size_per_thread * threads
    assert op_spec['cpu_limit'] == 5
    assert op_spec['memory_limit'] == mem_per_thread * threads
    assert ('--threads', threads) in cmd.params.params


@pytest.mark.parametrize('limits, threads, mem_per_thread', [([0, 0], 10, 512 * 2**20)])
def test_set_threads_with_memory_limit(cmd, data_size_per_thread, mem_per_thread, limits, threads):
    spec, op_spec = {}, {'memory_limit': 2**20}
    on_limits_set = mt_jobs.set_threads_count(data_size_per_thread, mem_per_thread, threads)
    with pytest.raises(Exception):
        on_limits_set(cmd, limits, spec, op_spec)


@pytest.mark.parametrize(
    'mem_per_thread, limits, threads',
    [(512 * 2**20, (0, 0), 1),
     (512 * 2**20, (0.25 * 2**30, 0), 1),
     (1024 * 2**20, (0, 2 ** 31), 2),
     (512 * 2**20, (2**30, 5 * 2**30), 4),
     (2**31, (1, 0), 16),
     ],
    indirect=True
)
def test_calc_cpu(cmd, data_size_per_thread, mem_per_thread, limits, threads):
    spec, op_spec = {}, {}
    on_limits_set = mt_jobs.set_threads_count(data_size_per_thread, mem_per_thread)
    on_limits_set(cmd, limits, spec, op_spec)

    assert spec['data_size_per_job'] == data_size_per_thread * threads
    assert op_spec['cpu_limit'] == threads
    assert op_spec['memory_limit'] == threads * mem_per_thread
    assert ('--threads', threads) in cmd.params.params
