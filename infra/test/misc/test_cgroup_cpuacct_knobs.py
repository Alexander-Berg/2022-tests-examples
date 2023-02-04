import pytest
import kern


@pytest.mark.xfail(not kern.kernel_in("4.19.17-2", "5.4.14-1"), reason="not implemented")
@pytest.mark.parametrize('cfq_period,cfq_quota,num_jobs,expected_wait', [
    pytest.param(100, 50, 1, 500),
    pytest.param(100, 100, 1, 0),
    pytest.param(100, 100, 2, 1000),
    pytest.param(100, 100, 3, 2000)
])
def test_cpuacct_wait(logger, make_cgroup, find_bin, make_task,
                      cfq_period, cfq_quota, num_jobs, expected_wait):
    """
    Comit 4000e0a ("sched/cpuacct: account time tasks waiting for execution")
    """

    runtime = 3
    runtime_ns = runtime * (10 ** 9)
    stress_bin = find_bin('stress-ng')

    cg = make_cgroup('cpu')
    assert cg.has_attr('cpuacct.wait')

    cg['cpu.cfs_quota_us'] = cfq_quota * 1000
    cg['cpu.cfs_period_us'] = cfq_period * 1000

    task = make_task(cgroups=[cg])
    assert cg.has_task(task.pid)
    assert cg.get_int("cpuacct.wait") == 0

    task.check_call([stress_bin, '--cpu',  str(num_jobs), "--timeout", str(runtime)])

    wait = cg.get_int("cpuacct.wait")
    wait_percpu = cg.get_percpus("cpuacct.wait_percpu")
    assert wait == sum(wait_percpu)

    usage = cg.get_int("cpuacct.usage")
    usage_percpu = cg.get_percpus("cpuacct.usage_percpu")
    assert usage == sum(usage_percpu)

    assert usage == pytest.approx((cfq_quota * runtime_ns) / cfq_period, rel=0.15)
    assert wait == pytest.approx(expected_wait * runtime * (10 ** 6), abs=0.15 * runtime_ns)
