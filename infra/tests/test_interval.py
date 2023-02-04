import pytest

from infra.skylib.intervals import IntervalController


@pytest.mark.parametrize('multiplier', [1., 1.2, 1.5, 2., 5.])
@pytest.mark.parametrize('variance', [0., 0.1, 0.2, 0.5])
@pytest.mark.parametrize('maximum', [10., 33., 100., 50000.])
def test_interval_scheduling(multiplier, variance, maximum):
    initial = 1.
    eps = .001
    steps = 100
    ctl = IntervalController(
        initial=initial,
        multiplier=multiplier,
        variance=variance,
        maximum=maximum,
    )

    for step in range(steps):
        expected = min(initial * (multiplier ** step), maximum)
        lower_bound = expected * (1 - variance)
        upper_bound = expected * (1 + variance)
        assert lower_bound - ctl.interval <= eps, "Step %d" % step
        assert ctl.interval - upper_bound <= eps, "Step %d" % step
        assert ctl.interval <= maximum * (1 + variance), "Step %d" % step
        ctl.schedule_next()

    ctl.reset()
    lower_bound = initial * (1 - variance)
    upper_bound = initial * (1 + variance)
    assert lower_bound - ctl.interval <= eps
    assert ctl.interval - upper_bound <= eps
