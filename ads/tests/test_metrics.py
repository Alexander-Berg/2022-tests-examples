from datetime import datetime

import pytest
import numpy as np

from ads.watchman.experiments.lib.plotting import metrics, anomalies
from ads.watchman.experiments.lib.models import prediction

from .test_helpers import MockLearnedTask


def test_period():
    p = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 5), 'learn')
    assert p.dates == {
        datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 2),
        datetime(2017, 8, 1, 3), datetime(2017, 8, 1, 4), datetime(2017, 8, 1, 5)
    }


def test_period_with_filters_is_well_calculated():
    p = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 5), 'learn',
                       filters=[
                           lambda d: d == datetime(2017, 8, 1, 3),
                           lambda d: d == datetime(2017, 8, 1, 1)])
    assert p.dates == {
        datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), datetime(2017, 8, 1, 4), datetime(2017, 8, 1, 5)
    }


def test_period_with_filters_does_not_contain_filtered_date():
    p = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 5), 'learn',
                       filters=[lambda d: d == datetime(2017, 8, 1, 3)])
    assert not p.has_date(datetime(2017, 8, 1, 3))


@pytest.mark.parametrize("pool,expected", [("learn", 0.1), ("test", 0.2)])
def test_that_engine_metric_is_well_calculated(pool, expected):

    learned_task = MockLearnedTask("my_task", "target")
    engine_metric = metrics.EngineMetric("ll_p", pool)

    table_stream = []
    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert engine_metric(applied_task) == expected


def test_that_engine_metric_returns_none_if_statistics_not_exists():

    engine_metric = metrics.EngineMetric("ll_p", "learn")

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5}
    ]

    applied_task = prediction.WoWPrediction(table_stream, 'target', 1)

    assert engine_metric(applied_task) is None


@pytest.fixture
def relative_metric():
    m = metrics.EngineMetric("ll_p", "learn")
    return metrics.RelativeMetric(m)


def test_that_relative_metric_is_well_calculated(relative_metric):
    assert relative_metric({'ll_p_learn': 0.2}, {'ll_p_learn': 0.4}) == 50


def test_that_relative_metric_returns_none_if_base_metric_not_exists(relative_metric):
    assert relative_metric({}, {}) is None


def test_that_relative_metric_returns_none_if_base_metric_is_none(relative_metric):
    assert relative_metric({'ll_p_learn': None}, {'ll_p_learn': 0.4}) is None


def test_that_relative_metric_returns_none_if_default_base_metric_is_none(relative_metric):
    assert relative_metric({'ll_p_learn': 0.2}, {'ll_p_learn': None}) is None


def test_that_mse_is_well_calculated():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), 'learn')
    mse_metric = metrics.Mse(period)

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': 1},
        {'date': "2017-08-01 01", 'target': 0.0, 'predicted_target': 1},
        {'date': "2017-08-01 02", 'target': 0.0, 'predicted_target': 1}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert mse_metric.name == "mse_learn"
    assert mse_metric(applied_task) == 0.75


def test_that_mse_is_well_calculated_if_predicted_target_contains_nan():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), 'learn')
    mse_metric = metrics.Mse(period)

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': np.nan},
        {'date': "2017-08-01 01", 'target': 0.0, 'predicted_target': 1},
        {'date': "2017-08-01 02", 'target': 0.0, 'predicted_target': 1}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert mse_metric(applied_task) == 1


def test_that_mse_is_none_if_all_predicted_targets_are_nan():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), 'learn')
    mse_metric = metrics.Mse(period)

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': np.nan},
        {'date': "2017-08-01 01", 'target': 0.0, 'predicted_target': np.nan},
        {'date': "2017-08-01 02", 'target': 0.0, 'predicted_target': np.nan}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert mse_metric(applied_task) is None


def test_that_mape_is_well_calculated():
    # TODO: merge tests for multiple metrics
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), 'learn')
    mape_metric = metrics.Mape(period)

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': 1},
        {'date': "2017-08-01 01", 'target': 0.4, 'predicted_target': 0.2},
        {'date': "2017-08-01 02", 'target': 0.6, 'predicted_target': 0.6}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert mape_metric(applied_task) == 50


def test_that_mape_is_well_calculated_if_predicted_target_contains_nan():
    # TODO: merge tests for multiple metrics
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), 'learn')
    mape_metric = metrics.Mape(period)

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': np.nan},
        {'date': "2017-08-01 01", 'target': 0.4, 'predicted_target': 0.2},
        {'date': "2017-08-01 02", 'target': 0.6, 'predicted_target': 0.6}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert mape_metric(applied_task) == 25


def test_that_mape_is_none_if_all_predicted_targets_are_nan():
    # TODO: merge tests for multiple metrics
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), 'learn')
    mape_metric = metrics.Mape(period)

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': np.nan},
        {'date': "2017-08-01 01", 'target': 0.0, 'predicted_target': np.nan},
        {'date': "2017-08-01 02", 'target': 0.0, 'predicted_target': np.nan}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert mape_metric(applied_task) is None


def test_that_std_is_well_calculated():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 5), 'learn')
    std_metric = metrics.Std(period)

    table_stream = [
        {'date': "2017-08-01 01", 'target': 1.5, 'predicted_target': 1},
        {'date': "2017-08-01 02", 'target': 1.25, 'predicted_target': 1},
        {'date': "2017-08-01 03", 'target': 1, 'predicted_target': 1},
        {'date': "2017-08-01 04", 'target': 0.75, 'predicted_target': 1},
        {'date': "2017-08-01 05", 'target': 0.5, 'predicted_target': 1}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert std_metric.name == "std_learn"
    assert std_metric(applied_task) == 0.25


def test_that_std_is_well_calculated_if_predicted_target_contains_nan():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 6), 'learn')
    std_metric = metrics.Std(period)

    table_stream = [
        {'date': "2017-08-01 01", 'target': 1.5, 'predicted_target': 1},
        {'date': "2017-08-01 02", 'target': 1.25, 'predicted_target': np.nan},
        {'date': "2017-08-01 03", 'target': 1.2, 'predicted_target': 1},  # this line left
        {'date': "2017-08-01 04", 'target': 0.75, 'predicted_target': np.nan},
        {'date': "2017-08-01 05", 'target': 0.5, 'predicted_target': 1},
        {'date': "2017-08-01 06", 'target': 1.2, 'predicted_target': 1}  # this line left
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert std_metric(applied_task) == 0


def test_that_std_is_well_calculated_on_small_period():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 3), 'learn')
    std_metric = metrics.Std(period, 1.0)  # without quantile filtering

    table_stream = [
        {'date': "2017-08-01 01", 'target': 1.5, 'predicted_target': 1},
        {'date': "2017-08-01 02", 'target': 1.25, 'predicted_target': 1},
        {'date': "2017-08-01 03", 'target': 1, 'predicted_target': 1},
        {'date': "2017-08-01 04", 'target': 0.75, 'predicted_target': 1},
        {'date': "2017-08-01 05", 'target': 0.5, 'predicted_target': 1}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert std_metric(applied_task) == 0.25


def test_that_hours_is_well_calculated():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 3), 'learn')
    hours_metric = metrics.Hours(period)

    table_stream = [
        {'date': "2017-08-01 01", 'target': 0.75, 'predicted_target': 0.1},
        {'date': "2017-08-01 02", 'target': 0.75, 'predicted_target': np.nan},
        {'date': "2017-08-01 03", 'target': 0.75, 'predicted_target': 0.2},
        {'date': "2017-08-01 04", 'target': 0.75, 'predicted_target': np.nan},
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert hours_metric(applied_task) == 2


def test_that_std_is_none_if_all_predicted_targets_are_nan():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 5), 'learn')
    std_metric = metrics.Std(period)

    table_stream = [
        {'date': "2017-08-01 01", 'target': 1.5, 'predicted_target': np.nan},
        {'date': "2017-08-01 02", 'target': 1.25, 'predicted_target': np.nan},
        {'date': "2017-08-01 03", 'target': 1.2, 'predicted_target': np.nan},
        {'date': "2017-08-01 04", 'target': 0.75, 'predicted_target': np.nan},
        {'date': "2017-08-01 05", 'target': 0.5, 'predicted_target': np.nan}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert std_metric(applied_task) is None


def get_periods():
    yield metrics.Period(datetime(2017, 8, 1, 2), datetime(2017, 8, 1, 6), 'learn')
    yield metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 5), 'learn')
    yield metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 6), 'learn')


@pytest.mark.parametrize("period", get_periods())
def test_that_bad_period_causes_exception(period):

    with pytest.raises(ValueError):
        learned_task = MockLearnedTask("my_task", "target")
        mse_metric = metrics.Mse(period)
        table_stream = [
            {'date': "2017-08-01 02", 'target': 0.5, 'predicted_target': 1},
            {'date': "2017-08-01 03", 'target': 0.0, 'predicted_target': 1},
            {'date': "2017-08-01 04", 'target': 0.0, 'predicted_target': 1},
            {'date': "2017-08-01 05", 'target': 0.5, 'predicted_target': 1}

        ]
        applied_task = prediction.TaskPrediction(table_stream, learned_task)
        mse_metric(applied_task)


def test_that_mse_on_filtered_period_is_well_calculated():
    anomaly = anomalies.Anomaly(
        description="some anomaly",
        start_date=datetime(2017, 8, 1),
        end_date=datetime(2017, 8, 1),
        start_time=datetime(2017, 8, 1, 1, 30),
        end_time=datetime(2017, 8, 1, 2, 30)

    )
    learned_task = MockLearnedTask("my_task", "target")

    anomaly_list = anomalies.AnomalyList([anomaly])
    period = metrics.Period(datetime(2017, 8, 1, 1), datetime(2017, 8, 1, 4), 'learn_wa', filters=[
        lambda d: not anomaly_list.is_normal(d)
    ])

    mse_metric = metrics.Mse(period)

    table_stream = [
        {'date': "2017-08-01 01", 'target': 0.5, 'predicted_target': 1},
        {'date': "2017-08-01 02", 'target': 0.5, 'predicted_target': 1},  # will be filtered
        {'date': "2017-08-01 03", 'target': 0.0, 'predicted_target': 1},
        {'date': "2017-08-01 04", 'target': 0.0, 'predicted_target': 1}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)

    assert mse_metric.name == "mse_learn_wa"
    assert mse_metric(applied_task) == 0.75


def test_that_compare_is_well_calculated():
    learned_task = MockLearnedTask("my_task", "target")

    period = metrics.Period(datetime(2017, 8, 1, 0), datetime(2017, 8, 1, 2), 'learn')

    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': 1},
        {'date': "2017-08-01 01", 'target': 0.0, 'predicted_target': 1},
        {'date': "2017-08-01 02", 'target': 0.0, 'predicted_target': 1}
    ]

    applied_task = prediction.TaskPrediction(table_stream, learned_task)
    compare_view = metrics.Compare([applied_task], 0)

    compare_view.add_metrics([metrics.Mse(period)])

    assert compare_view.df.to_dict('records') == [{'mse_learn': 0.75}]
