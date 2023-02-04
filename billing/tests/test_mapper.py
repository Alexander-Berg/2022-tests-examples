import datetime as dt
from agency_rewards.rewards.mapper import RunCalc


def test_run_calc_set_finish():
    run_calc = RunCalc(1, 'run', '1.0', 'stable', dt.datetime.now())
    run_calc.set_finish()
    assert run_calc.finish_dt is not None

    example_error = 'error' * 1000
    run_calc.set_finish(example_error)
    assert run_calc.error == example_error[:250] + ' ... ' + example_error[-250:]
