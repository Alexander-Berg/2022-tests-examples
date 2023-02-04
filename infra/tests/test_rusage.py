from infra.ya_salt.lib import rusage


def test_get_rusage_metrics():
    m = rusage.get_rusage_metrics()
    assert set(i[0] for i in m) == {rusage.METRIC_RUSAGE_MAXRSS_MB,
                                    rusage.METRIC_RUSAGE_STIME_SEC,
                                    rusage.METRIC_RUSAGE_UTIME_SEC}
