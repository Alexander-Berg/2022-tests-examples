import numpy as np
import pandas as pd
from pandas.testing import assert_frame_equal

from datacloud.dev_utils.data.data_utils import array_tostring
from datacloud.ml_utils.benchmark_v2.transformers import (
    DecodeAndFillNanToMeanAndHit,
    OneHotTransformer,
    SelectColumnAndLogitAndFillNanToMeanAndHit,
    YuidDaysTransformer,
    PhoneWatchLogTransformer,
    SelectColumnsAndFillNanToMeanAndHit,
    SelectColumnsAndFillNan,
    TopMultiLabelBinarizer,
    TopKMeansOnMLB,
    LocationsTransformer,
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

    assert_frame_equal(transformed, expected_result)


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
    ]).set_index('external_id').T.reset_index(drop=True).T

    transformer = OneHotTransformer(columns=['operator'])
    transformer.fit(train_data)
    transformed = transformer.transform(test_data)

    assert_frame_equal(transformed, expected_result)


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

    assert_frame_equal(transformed, expected_result)


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

    assert_frame_equal(transformed, expected_result)


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

    assert_frame_equal(transformed, expected_result)


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

    assert_frame_equal(transformed, expected_result)


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
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert_frame_equal(transformed, expected_result)


def test_select_columns_with_logit():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'x1': 0.5, 'x2': 0.5},
        {'external_id': '2', 'x1': 0.9, 'x2': None},
        {'external_id': '3', 'x1': None, 'x2': None},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 'x1': 0., 'x2': 0., 'hit_flg': True},
        {'external_id': '2', 'x1': 2.1972245773362196, 'x2': 0., 'hit_flg': True},
        {'external_id': '3', 'x1': 1.0986122886681098, 'x2': 0., 'hit_flg': False},
    ]).set_index('external_id')[['x1', 'x2', 'hit_flg']]

    transformer = SelectColumnAndLogitAndFillNanToMeanAndHit(column=['x1', 'x2'])
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert_frame_equal(transformed, expected_result)


def test_select_columns_aslist():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'features': [0., 0.]},
        {'external_id': '2', 'features': [1., None]},
        {'external_id': '3', 'features': [None, None]},
    ]).set_index('external_id')

    expected_result = pd.DataFrame.from_records([
        {'external_id': '1', 0: 0., 1: 0.0, 'hit_flg': True},
        {'external_id': '2', 0: 1., 1: 0.0, 'hit_flg': True},
        {'external_id': '3', 0: 0.5, 1: 0.0, 'hit_flg': False},
    ]).set_index('external_id')[[0, 1, 'hit_flg']]

    transformer = SelectColumnsAndFillNanToMeanAndHit(columns='features')
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)

    assert_frame_equal(transformed, expected_result)


def test_top_multilabel_binarizer():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'country_ids': [1, 2, 3]},
        {'external_id': '2', 'country_ids': [2, 3, 4]},
        {'external_id': '3', 'country_ids': [3, 4]},
    ]).set_index('external_id')

    expected_result = np.array([
        [1, 1, 0],
        [1, 1, 1],
        [0, 1, 1],
    ])
    transformer = TopMultiLabelBinarizer('country_ids', 3)
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)
    assert np.array_equal(transformed, expected_result)


def test_top_kmeans_on_mlb():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'country_ids': [1]},
        {'external_id': '2', 'country_ids': [1]},
        {'external_id': '3', 'country_ids': [2]},
    ]).set_index('external_id')

    expected_result = np.array([
        [1, 0],
        [1, 0],
        [0, 1],
    ])
    transformer = TopKMeansOnMLB('country_ids', 1, 2)
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)
    assert np.array_equal(transformed, expected_result)


def test_locations_transformer():
    sample_data = pd.DataFrame.from_records([
        {'external_id': '1', 'country_ids': [1], 'region_ids': [100], 'mode_city_id': 1000},
        {'external_id': '2', 'country_ids': [1], 'region_ids': [100], 'mode_city_id': 1000},
        {'external_id': '3', 'country_ids': [2], 'region_ids': [101], 'mode_city_id': 1000},
    ]).set_index('external_id')
    sample_data['mode_city_type'] = 6
    sample_data['mode_country_id'] = sample_data['country_ids'].map(lambda x: x[0])
    sample_data['mode_region_id'] = sample_data['region_ids'].map(lambda x: x[0])

    expected_result = (
        pd.DataFrame.from_dict(
            {
                'external_id': ['1', '2', '3'],
                'mode_city_id': [1000, 1000, 1000],
                'mode_city_type': [6, 6, 6],
                'mode_country_id': [1, 1, 0],
                'mode_region_id': [100, 100, 0],
                'was_abroad': [0, 0, 1],
                'country_ids_kmeans_0': [0.325349, 0.325349, 0.674651],
                'region_ids_kmeans_0': [0.325349, 0.325349, 0.674651],
            },
        ).astype({'was_abroad': np.uint8}).set_index('external_id')[
            ['mode_city_id', 'mode_city_type', 'mode_country_id', 'mode_region_id', 'was_abroad',
             'country_ids_kmeans_0', 'region_ids_kmeans_0']]
    )
    transformer = LocationsTransformer(1, 1, 0, 1, 1, 0, 1, 1)
    transformer.fit(sample_data)
    transformed = transformer.transform(sample_data)
    assert_frame_equal(transformed, expected_result)
