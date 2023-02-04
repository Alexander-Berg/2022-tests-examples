from datetime import datetime

import pytest
import pandas as pd
import numpy as np

import pandas.util.testing as pdt

from ads.watchman.experiments.lib.models import prediction

from .test_helpers import MockLearnedTask


def test_that_wow_prediction_data_is_well_calculated():
    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5},
        {'date': "2017-08-01 02", 'target': 0.6},
        {'date': "2017-08-01 01", 'target': 0.7},
    ]

    wow_prediction = prediction.WoWPrediction(table_stream, 'target', 1)

    expected = pd.DataFrame([
        {'date': "2017-08-01 00", 'parsed_date': datetime(2017, 8, 1, 0), 'target': 0.5, 'target_shift_1': None},
        {'date': "2017-08-01 01", 'parsed_date': datetime(2017, 8, 1, 1), 'target': 0.7, 'target_shift_1': 0.5},
        {'date': "2017-08-01 02", 'parsed_date': datetime(2017, 8, 1, 2), 'target': 0.6, 'target_shift_1': 0.7}
    ])

    # Using of assert_frame_equal is important because of careful testing of Timestamp and nan
    pdt.assert_frame_equal(wow_prediction.data.reset_index(drop=True), expected.reset_index(drop=True),
                           check_like=True, check_datetimelike_compat=True)


def test_that_wow_prediction_data_with_normalization_is_well_calculated():
    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'normalizer': 1},
        {'date': "2017-08-01 01", 'target': 1, 'normalizer': 2},
        {'date': "2017-08-01 02", 'target': 0.7, 'normalizer': 3},
    ]

    wow_prediction = prediction.WoWPrediction(table_stream, 'target', 1, 'normalizer')

    expected = pd.DataFrame([
        {'date': "2017-08-01 00", 'parsed_date': datetime(2017, 8, 1, 0),
         'target': 0.5, 'target_shift_1': np.nan, 'normalizer': 1},
        {'date': "2017-08-01 01", 'parsed_date': datetime(2017, 8, 1, 1),
         'target': 1, 'target_shift_1': 1.0, 'normalizer': 2},
        {'date': "2017-08-01 02", 'parsed_date': datetime(2017, 8, 1, 2),
         'target': 0.7, 'target_shift_1': 1.5, 'normalizer': 3}
    ])

    # Using of assert_frame_equal is important because of careful testing of Timestamp and nan
    pdt.assert_frame_equal(wow_prediction.data.reset_index(drop=True), expected.reset_index(drop=True),
                           check_like=True, check_datetimelike_compat=True, check_less_precise=1)


def test_that_wow_prediction_works_if_stream_is_empty():
    table_stream = []

    wow_prediction = prediction.WoWPrediction(table_stream, 'target', 1)

    assert [] == sorted(wow_prediction.data.to_dict('records'))


@pytest.mark.parametrize("column", ["date", "target"])
def test_that_in_wow_prediction_if_not_exists_target_column_causes_exception(column):
    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5}
    ]
    del table_stream[0][column]
    with pytest.raises(ValueError):
        prediction.WoWPrediction(table_stream, 'target', 1)


def test_that_task_prediction_data_is_well_calculated():
    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': 0.9},
        {'date': "2017-08-01 02", 'target': 0.6, 'predicted_target': 0.8},
        {'date': "2017-08-01 01", 'target': 0.7, 'predicted_target': 0.6},
    ]

    learned_task = MockLearnedTask("my_task", "target")

    task_prediction = prediction.TaskPrediction(table_stream, learned_task)

    expected = [
        {'date': "2017-08-01 00", 'parsed_date': pd.Timestamp(datetime(2017, 8, 1, 0)), 'target': 0.5, 'predicted_target': 0.9},
        {'date': "2017-08-01 01", 'parsed_date': pd.Timestamp(datetime(2017, 8, 1, 1)), 'target': 0.7, 'predicted_target': 0.6},
        {'date': "2017-08-01 02", 'parsed_date': pd.Timestamp(datetime(2017, 8, 1, 2)), 'target': 0.6, 'predicted_target': 0.8}
    ]

    assert expected == sorted(task_prediction.data.to_dict('records'), key=lambda r: r['date'])


def test_that_wow_prediction_has_proper_name():
    table_stream = []

    wow_prediction = prediction.WoWPrediction(table_stream, 'target', 1)

    assert wow_prediction.name == "WoW_target"


def test_that_wow_prediction_with_normalizer_has_proper_name():
    table_stream = []

    wow_prediction = prediction.WoWPrediction(table_stream, 'target', 1, 'normalizer')

    assert wow_prediction.name == "WoW_target_vs_normalizer"


@pytest.mark.parametrize("column", ["date", "target", "predicted_target"])
def test_that_in_task_prediction_if_not_exists_target_column_causes_exception(column):
    table_stream = [
        {'date': "2017-08-01 00", 'target': 0.5, 'predicted_target': 0.9}
    ]
    del table_stream[0][column]

    learned_task = MockLearnedTask("my_task", "target")

    with pytest.raises(ValueError):
        prediction.TaskPrediction(table_stream, learned_task)


def test_that_task_prediction_works_if_stream_is_empty():
    table_stream = []

    learned_task = MockLearnedTask("my_task", "target")
    task_prediction = prediction.TaskPrediction(table_stream, learned_task)

    assert [] == sorted(task_prediction.data.to_dict('records'))


def test_baseline_model_fails_if_no_required_columns_in_stream():
    table_stream = [
        {'date': "2017-08-01 00"}
    ]

    with pytest.raises(ValueError):
        prediction.WatchmanBaselineModel(table_stream,
                                         count_col='count',
                                         target_col='target')


def make_test_baseline_model():
    target_col = 'target'
    count_col = 'count'

    table_stream = [
        {'date': "2017-08-08 00", target_col: 100.0, count_col: 5},
        {'date': "2017-08-01 00", target_col: 100.0, count_col: 5},
        {'date': "2017-08-08 02", target_col: 100.0, count_col: 20},
    ]

    return prediction.WatchmanBaselineModel(table_stream, count_col=count_col, target_col=target_col)


def test_baseline_model_predicts_average():
    model = make_test_baseline_model()

    expected_predictions = [
        ({'date': "2017-08-01 00", model.count_col: 10, model.target_col: 100.0}, 200.0),
        ({'date': "2017-08-01 02", model.count_col: 5, model.target_col: 100.0}, 25.0),
    ]

    for row, expected_prediction in expected_predictions:
        assert model.predict(row) == expected_prediction


def test_baseline_prediction_computes_correct_values():
    model = make_test_baseline_model()

    test_table = [
        {'date': "2017-08-01 00", model.count_col: 10, model.target_col: 100.0, 'expected_target': 200.0},
        {'date': "2017-08-01 02", model.count_col: 5, model.target_col: 100.0, 'expected_target': 25.0}
    ]

    baseline_prediction = prediction.WatchmanBaselinePrediction(test_table, model, 'test')
    result = baseline_prediction.data[baseline_prediction.predicted_col]
    np.testing.assert_almost_equal(result.values, baseline_prediction.data['expected_target'].values)


def test_that_in_baseline_prediction_all_columns_are_valid():
    model = make_test_baseline_model()

    test_table = [
        {'date': "2017-08-01 00", model.count_col: 10, model.target_col: 100.0},
        {'date': "2017-08-01 02", model.count_col: 5, model.target_col: 100.0}
    ]

    baseline_prediction = prediction.WatchmanBaselinePrediction(test_table, model, 'test')
    assert {baseline_prediction.target_col, baseline_prediction.predicted_col, baseline_prediction.parsed_date_col} \
        < set(baseline_prediction.data.columns)
