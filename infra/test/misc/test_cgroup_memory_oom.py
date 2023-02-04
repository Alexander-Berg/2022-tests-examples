import pytest
import kern
import time


@pytest.mark.xfail(not kern.kernel_in("4.19.60-12", "5.4.14-1"), reason="not implemented for cgroup-v1")
@pytest.mark.parametrize('oom_score_adj,should_be_dead, stress_opts', [
    pytest.param(None, True, ['--brk', '1', '--oomable'], id='brk-1-all_oomable'),
    pytest.param(0, True, ['--brk', '1'], id='brk-1-parent_oomable'),
    pytest.param(None, False, ['--brk', '1'], id='brk-1-nonoomable'),

])
def test_memory_oom_group(logger, find_bin, make_cgroup, make_task,
                          oom_score_adj, should_be_dead, stress_opts):
    """
    https://st.yandex-team.ru/KERNEL-269
    FIXME: currently this itest fails because kill logic works incorrectly
    """
    stress_bin = find_bin('stress-ng')

    cg1 = make_cgroup('memory')
    cg2 = make_cgroup('memory')

    size = 100 << 20

    cg1['limit_in_bytes'] = int(size * 1.5)
    cg2['limit_in_bytes'] = int(size * 1.5)

    cg1.set_attr('memory.oom.group', 1)
    cg2.set_attr('memory.oom.group', 1)

    task1 = make_task(cgroups=[cg1])
    task2 = make_task(cgroups=[cg2])

    assert cg1['usage_in_bytes'] < size
    assert cg2['usage_in_bytes'] < size

    task1.call([stress_bin, '--vm', '1', '--vm-bytes', str(size), '--timeout', str(10)], recv=False)

    # Force task cleanup if test fail in order to prevent task leak.
    cg2.force_cleanup = True
    task2.call([stress_bin] + stress_opts, recv=False)

    if oom_score_adj is not None:
        # fix oom_score_adj only after stress-ng started
        time.sleep(1)
        for p in cg2.processes():
            try:
                kern.task_write_attr(p, "oom_score_adj", str(oom_score_adj))
            except:
                # tasks was killed under us, ignore it
                pass

    task1.recv()
    assert cg1.get_stats('memory.oom_control')['oom_kill'] == 0

    # At this moment all tasks in cg2 shoule be killed because of group.oom
    assert cg2.get_stats('memory.oom_control')['oom_kill'] > 0

    if should_be_dead:
        assert not cg2.processes()
    else:
        assert cg2.processes()
        cg2.kill_all(wait=True)
        cg2.force_cleanup = False
