import numpy as np
import pandas as pd
from pandas.testing import assert_frame_equal
from datacloud.dev_utils.data.data_utils import array_tostring
from datacloud.ml_utils.junk.benchmark_v2.transformers import (
    DecodeAndFillNanToMeanAndHit,
    OneHotTransformer,
    SelectColumnAndLogitAndFillNanToMeanAndHit,
    YuidDaysTransformer,
    PhoneWatchLogTransformer,
    SelectColumnsAndFillNanToMeanAndHit,
    SelectColumnsAndFillNan,
    ExpandListFeatureAndFillNanToMeanAndHit,
)


def test_decode_and_fill_nan_to_mean_and_hit():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'features': array_tostring([1., 1., 0., 0.])},
        {'external_id': '2', 'features': array_tostring([1., 1., 2., 0.])},
        {'external_id': '3', 'features': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 0: 1., 1: 1., 2: 0., 3: 0., 'hit_flg': True},
        {'external_id': '2', 0: 1., 1: 1., 2: 2., 3: 0., 'hit_flg': True},
        {'external_id': '3', 0: 1., 1: 1., 2: 1., 3: 0., 'hit_flg': False},
    ]).set_index('external_id').astype(np.float32).astype({'hit_flg': bool})

    transformer = DecodeAndFillNanToMeanAndHit()
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert expected_result.equals(transformed)


def test_one_hot():
    train_data = pd.DataFrame.from_records([
        {'external_id': '1', 'operator': 'TL2'},
        {'external_id': '2', 'operator': 'MTS'},
        {'external_id': '3', 'operator': 'MTS'},
    ]).set_index('external_id')

    test_data = pd.DataFrame.from_records([
        {'external_id': '4', 'operator': 'TL2'},
        {'external_id': '5', 'operator': 'BEE'},
        {'external_id': '6', 'operator': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '4', 0: False, 1: True},
        {'external_id': '5', 0: False, 1: False},
        {'external_id': '6', 0: False, 1: False},
    ]).set_index('external_id')

    transformer = OneHotTransformer(columns=['operator'])
    transformer.fit(train_data)
    transformed = transformer.transform(test_data)

    assert expected_result.equals(transformed)


def test_select_column_with_logit():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'prediction': 0.5},
        {'external_id': '2', 'prediction': 0.9},
        {'external_id': '3', 'prediction': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 'prediction': 0., 'hit_flg': True},
        {'external_id': '2', 'prediction': 2.1972245773362196, 'hit_flg': True},
        {'external_id': '3', 'prediction': 1.0986122886681098, 'hit_flg': False},
    ]).set_index('external_id')[['prediction', 'hit_flg']]

    transformer = SelectColumnAndLogitAndFillNanToMeanAndHit(column='prediction')
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert expected_result.equals(transformed)


def test_yuid_days():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'days_from_first_yuid': 54},
        {'external_id': '2', 'days_from_first_yuid': 1000},
        {'external_id': '3', 'days_from_first_yuid': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 'days_from_first_yuid': 0.1, 'hit_flg': True},
        {'external_id': '2', 'days_from_first_yuid': 1.0, 'hit_flg': True},
        {'external_id': '3', 'days_from_first_yuid': 0.55, 'hit_flg': False},
    ]).set_index('external_id')

    transformer = YuidDaysTransformer()
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert expected_result.equals(transformed)


def test_phone_watch_log():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'avito_category_01': 2, 'avito_category_02': 0},
        {'external_id': '2', 'avito_category_01': 0, 'avito_category_02': 0},
        {'external_id': '2', 'avito_category_01': None, 'avito_category_02': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 'avito_category_01': 1.0, 'avito_category_02': 0., 'hit_flg': True},
        {'external_id': '2', 'avito_category_01': 0.0, 'avito_category_02': 0., 'hit_flg': True},
        {'external_id': '2', 'avito_category_01': 0.5, 'avito_category_02': 0., 'hit_flg': False},
    ]).set_index('external_id')

    transformer = PhoneWatchLogTransformer()
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert expected_result.equals(transformed)


def test_select_columns():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'x1': 0., 'x2': 0.},
        {'external_id': '2', 'x1': 1., 'x2': None},
        {'external_id': '3', 'x1': None, 'x2': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 'x1': 0., 'x2': 0.0, 'hit_flg': True},
        {'external_id': '2', 'x1': 1., 'x2': 0.0, 'hit_flg': True},
        {'external_id': '3', 'x1': 0.5, 'x2': 0.0, 'hit_flg': False},
    ]).set_index('external_id')[['x1', 'x2', 'hit_flg']]

    transformer = SelectColumnsAndFillNanToMeanAndHit(columns=['x1', 'x2'])
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert expected_result.equals(transformed)


def test_select_string_columns():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'x1': 'a', 'x2': '1'},
        {'external_id': '2', 'x1': 'b', 'x2': None},
        {'external_id': '3', 'x1': None, 'x2': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 'x1': 'a', 'x2': '1'},
        {'external_id': '2', 'x1': 'b', 'x2': 'missing'},
        {'external_id': '3', 'x1': '', 'x2': 'missing'},
    ]).set_index('external_id')[['x1', 'x2']]

    transformer = SelectColumnsAndFillNan(fill_values={'x1': '', 'x2': 'missing'})
    transformed = transformer.transform(sample_data)

    assert expected_result.equals(transformed)


def test_list_columns():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'features': [False, True]},
        {'external_id': '2', 'features': [False, False]},
        {'external_id': '3', 'features': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 0: 0., 1: 1., 'hit_flg': True},
        {'external_id': '2', 0: 0., 1: 0., 'hit_flg': True},
        {'external_id': '3', 0: 0., 1: 0.5, 'hit_flg': False},
    ]).set_index('external_id')[[0, 1, 'hit_flg']]

    transformer = ExpandListFeatureAndFillNanToMeanAndHit(column='features')
    transformed = transformer.fit(sample_data).transform(sample_data)
    assert_frame_equal(expected_result, transformed)
