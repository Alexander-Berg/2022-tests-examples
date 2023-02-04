import kern
import pytest
import subprocess


@pytest.mark.xfail(not kern.kernel_in("5.4.64"), reason="not implemented")
def test_log_fatal_signals(dmesg, logger, sysctl, make_cgroup, make_task):
    sysctl['kernel.log_fatal_signals'] = 1
    cg_pids = make_cgroup('pids')
    cg_freezer = make_cgroup('freezer')
    logger.info("cgroup pids path: {}".format(cg_pids.name))
    logger.info("cgroup freezer path: {}".format(cg_freezer.name))
    tests = {
        'test1': [[cg_pids, cg_freezer], cg_freezer.name],
        'test2': [[cg_pids], cg_pids.name]
    }
    for t in tests.values():
        task = make_task(cgroups=t[0])
        task.call(['timeout', '0.1', 'sleep', '1'], recv=False)
        ptr = '.* \\(sleep\\), uid 0, cgroup {}: exited on signal 15'.format(t[1])
        try:
            match = dmesg.match(ptr, timeout=1)
        except subprocess.TimeoutExpired:
            raise Exception("dmesg not matched, expected: {}".format(ptr))
        else:
            logger.info("dmesg matched successfully: {}".format(match))
