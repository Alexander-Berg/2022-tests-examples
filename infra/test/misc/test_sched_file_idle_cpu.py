import pytest
import kern
import subprocess


@pytest.mark.skipif(kern.virtual_machine(), reason='require hardware machine')
@pytest.mark.skipif(not kern.read_int('/sys/devices/system/cpu/smt/active', default=0), reason='require SMT')
@pytest.mark.parametrize(
    ('threads', 'expected'),
    [
        (1, 0.0),
        (2, 0.0),
        (kern.nr_cpus() // 2, pytest.approx(0.0, abs=0.1)),
        (kern.nr_cpus(), pytest.approx(1.0, abs=0.1)),
    ],
    ids=['one', 'two', 'half', 'full'],
)
def test_sched_smt_clash(logger, find_bin, lock_perf, threads, expected):
    try:
        sched_features = kern.read_str('/sys/kernel/debug/sched_features').split()
    except:
        sched_features = []

    logger.info('sched features: %s', ' '.join(sched_features))

    metric = 'SMT_2T_Utilization'
    perf_bin = find_bin('perf')
    stress_bin = find_bin('stress-ng')

    if metric not in kern.perf_list(['metrics'], perf_bin=perf_bin):
        pytest.skip('No perf metric ' + metric)

    lock_perf()

    res = kern.perf_stat(
        ['-a', '-M', metric],
        [stress_bin, '-c', str(threads), '-t', '3'],
        perf_bin=perf_bin
    )
    assert res[metric] == expected


@pytest.mark.parametrize('threads', [1, kern.nr_cpus() // 2, kern.nr_cpus() - 1])
def test_schbench(logger, find_bin, lock_perf, threads):
    schbench_bin = find_bin('schbench')

    lock_perf()

    out = kern.run_output(
        [schbench_bin, '-m', '1', '-t', str(threads), '-r', '10'],
        stderr=subprocess.STDOUT,
        timeout=60
    )

    logger.info('schbench result: %s', out)

    for l in out.splitlines():
        if l.startswith('\t*99.0th'):
            result = int(l.split(':', 2)[1])
            break
    else:
        pytest.fail()

    assert result < 1000
