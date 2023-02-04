import pandas as pd
import numpy as np
from mock import patch
from unittest.mock import Mock

from maps.infra.monitoring.sla_calculator.core.schema import column, Int64, Boolean
from maps.infra.monitoring.sla_calculator.core.service import Service
from maps.infra.monitoring.sla_calculator.core.statbox import (
    previous_dates, load_statuses,
    enrich_statuses_with_external_sla, _description_for_lsr
)

from helpers import DataframeEQ, PresetFactory


def test_previous_dates():
    assert previous_dates('2017-01-01', 1) == ['2017-01-01']
    assert previous_dates('2017-01-02', 2) == ['2017-01-02', '2017-01-01']
    assert previous_dates('2017-01-02', 3) == ['2017-01-02', '2017-01-01', '2016-12-31']


@patch('maps.infra.monitoring.sla_calculator.core.statbox.SLA_PERIODS', new=[3])
@patch('maps.infra.monitoring.sla_calculator.core.statbox.EXTRA_DAYS_TO_FILL_HOLES', new=0)
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.get')
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.put')
def test_load_statuses(mock_cache_put, mock_cache_get):
    def preset_statuses(date: str, **kwargs) -> pd.DataFrame:
        return {
            '2017-01-02': PresetFactory.statuses('a', '2017-01-02', good=2, bad=0),
            '2017-01-01': PresetFactory.statuses('a', '2017-01-01', good=1, bad=0),
            '2016-12-31': PresetFactory.statuses('a', '2016-12-31', good=31, bad=0),
        }[date]

    mock_statuses_calc = Mock(side_effect=preset_statuses)
    # Cache contains single day
    mock_cache_get.return_value = preset_statuses('2017-01-01')

    test_service = Service(
        name='test_service',
        selectors={'the_answer': 42},
        statuses_calculator=mock_statuses_calc
    )
    # SLA_PERIODS=[3], so calculating 3 days up to 2017-01-02
    statuses = load_statuses(test_service, '2017-01-02', force=False, test_run=False)

    # Checked all 3 days in cache
    assert mock_cache_get.call_args_list == [
        ({'begin': '2016-12-31', 'end': '2017-01-02'}, )
    ]
    # Calculcated missing 2 days
    assert mock_statuses_calc.call_args_list == [
        ({'date': '2016-12-31', 'the_answer': 42}, ),
        ({'date': '2017-01-02', 'the_answer': 42}, )
    ]
    # Stopred missing 2 days stored in cache
    assert mock_cache_put.call_args_list == [
        ((DataframeEQ(preset_statuses('2016-12-31').assign(external_good=True)), False), ),
        ((DataframeEQ(preset_statuses('2017-01-02').assign(external_good=True)), False), )
    ]

    count = statuses[statuses['status'] == 200].set_index('date')['amount']
    for date in ['2016-12-31', '2017-01-01', '2017-01-02']:
        assert count[date] == int(date[-2:])


@patch('maps.infra.monitoring.sla_calculator.core.statbox.SLA_PERIODS', new=[2])
@patch('maps.infra.monitoring.sla_calculator.core.statbox.EXTRA_DAYS_TO_FILL_HOLES', new=0)
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.get')
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.put')
def test_load_statuses_force(mock_cache_put, mock_cache_get):
    def preset_statuses(date: str, **kwargs) -> pd.DataFrame:
        return {
            '2016-12-31': PresetFactory.statuses('a', date, good=31, bad=0),
            '2017-01-01': PresetFactory.statuses('a', date, good=1, bad=0),
            '2017-01-02': PresetFactory.statuses('a', date, good=2, bad=0)
        }[date]
    def mock_statuses_cache(begin: str, end: str) -> pd.DataFrame:
        return pd.concat([
            preset_statuses(date)
            for date in PresetFactory.dates_sequence(begin, end)
        ])

    mock_statuses_calc = Mock(side_effect=preset_statuses)
    # Cache contains all dates
    mock_cache_get.side_effect = mock_statuses_cache

    test_service = Service(
        name='test_service',
        selectors={'the_answer': 42},
        statuses_calculator=mock_statuses_calc
    )
    # SLA_PERIODS=[2], so calculating 2 days up to 2017-01-02
    statuses = load_statuses(test_service, '2017-01-02', force=True, test_run=False)

    # Not checked '2017-01-02' in cash, due to force=True
    assert mock_cache_get.call_args_list == [
        ({'begin': '2017-01-01', 'end': '2017-01-01'}, )  #
    ]
    # Forced '2017-01-02' recalculated and put in cache
    assert mock_statuses_calc.call_args_list == [
        ({'date': '2017-01-02', 'the_answer': 42}, )
    ]
    assert mock_cache_put.call_args_list == [
        ((DataframeEQ(preset_statuses('2017-01-02').assign(external_good=True)), True), )
    ]

    count = statuses[statuses['status'] == 200].set_index('date')['amount']
    for date in ['2017-01-01', '2017-01-02']:
        assert count[date] == int(date[-2:])


@patch('maps.infra.monitoring.sla_calculator.core.statbox.SLA_PERIODS', new=[3])
@patch('maps.infra.monitoring.sla_calculator.core.statbox.EXTRA_DAYS_TO_FILL_HOLES', new=0)
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.get')
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.put')
def test_load_statuses_custom_column(mock_cache_put, mock_cache_get):
    def preset_statuses(date: str, **kwargs) -> pd.DataFrame:
        return {
            '2017-01-02': PresetFactory.statuses_custom_column('a', '2017-01-02', good2=11, bad3=5, good4=2, bad5=23),
            '2017-01-01': PresetFactory.statuses_custom_column('a', '2017-01-01', good2=2, bad3=5, good4=1, bad5=23),
            '2016-12-31': PresetFactory.statuses_custom_column('a', '2016-12-31', good2=59, bad3=41, good4=31, bad5=79)
        }[date]

    mock_statuses_calc = Mock(side_effect=preset_statuses)
    # Cache contains single day
    mock_cache_get.return_value = preset_statuses('2017-01-01')

    test_service = Service(
        name='test_service',
        selectors={'the_answer': 42, 'groups': {'the_question': '"unknown"'}},
        columns=[column('custom_column', Int64, help='A service specific column')],
        statuses_calculator=mock_statuses_calc
    )
    # SLA_PERIODS=[3], so calculating 3 days up to 2017-01-02
    statuses = load_statuses(test_service, '2017-01-02', force=False, test_run=False)

    # Checked all 3 days in cache
    assert mock_cache_get.call_args_list == [
        ({'begin': '2016-12-31', 'end': '2017-01-02'}, )
    ]
    # Calculated 2 days not found in cache
    assert mock_statuses_calc.call_args_list == [
        ({'date': '2016-12-31', 'the_answer': 42, 'groups': {'the_question': '"unknown"'}}, ),
        ({'date': '2017-01-02', 'the_answer': 42, 'groups': {'the_question': '"unknown"'}}, )
    ]
    # Store missing 2 days in the cache
    assert mock_cache_put.call_args_list == [
        ((DataframeEQ(preset_statuses('2016-12-31').assign(external_good=True)), False), ),
        ((DataframeEQ(preset_statuses('2017-01-02').assign(external_good=True)), False), )
    ]

    count = statuses[
        (statuses['status'] == 200) & (statuses['custom_column'] >= 4)
    ].set_index('date')['amount']

    for date in ['2016-12-31', '2017-01-01', '2017-01-02']:
        assert count[date] == int(date[-2:])


@patch('maps.infra.monitoring.sla_calculator.core.statbox.SLA_PERIODS', new=[3])
@patch('maps.infra.monitoring.sla_calculator.core.statbox.EXTRA_DAYS_TO_FILL_HOLES', new=0)
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.get')
@patch('maps.infra.monitoring.sla_calculator.core.cache.YqlResultCache.put')
def test_load_statuses_empty_date(mock_cache_put, mock_cache_get):
    def preset_cache_statuses(begin: str, end: str) -> pd.DataFrame:
        cache_contents = {
            '2020-03-01': PresetFactory.statuses('/route', '2020-03-01', good=10, bad=1)
                .assign(jams_are_fresh=True, external_good=True),
            # '2020-02-29' is missing
            # '2020-02-28' contains blank
            '2020-02-28': PresetFactory.statuses_blank_date('2020-02-28')
                .assign(jams_are_fresh=False, external_good=False)
        }
        return pd.concat([
            cache_contents[date] for date in PresetFactory.dates_sequence(begin, end)
            if date in cache_contents
        ])

    mock_statuses_calc = Mock(return_value=pd.DataFrame())  # statuses calculator return empty result
    mock_cache_get.side_effect = preset_cache_statuses

    test_service = Service(
        name='test_service',
        selectors={'endpoints': ['/route']},
        columns=[column('jams_are_fresh', Boolean, help='Jams are fresh')],
        statuses_calculator=mock_statuses_calc
    )
    # SLA_PERIODS=[3], calculating 3 days up to 2020-03-01
    statuses = load_statuses(test_service, '2020-03-01', force=False, test_run=False)

    # Checked all 3 days in cache
    assert mock_cache_get.call_args_list == [
        ({'begin': '2020-02-28', 'end': '2020-03-01'}, )
    ]
    # Calculated 1 days not found in cache
    assert mock_statuses_calc.call_args_list == [
        ({'date': '2020-02-29', 'endpoints': ['/route']}, ),
    ]
    # Stored blank day in the cache
    assert mock_cache_put.call_args_list == [
        ((DataframeEQ(PresetFactory.statuses_blank_date('2020-02-29')
                        .assign(external_good=False, jams_are_fresh=False)), False), )
    ]

    expected = pd.DataFrame({
        'date':           ['2020-02-28', '2020-02-29', '2020-03-01', '2020-03-01'],
        'endpoint':       [''          , ''          , '/route'    , '/route'],
        'status':         [0           , 0           , 200         , 500],
        'amount':         [0.0         , 0.0         , 10.0        , 1.0],
        'too_long':       [False       , False       , False       , False],
        'jams_are_fresh': [False       , False       , True        , True],
        'external_good':  [False       , False       , True        , True],
    })
    pd.testing.assert_frame_equal(
        statuses.sort_values(by=['date'], ignore_index=True),
        expected.sort_values(by=['date'], ignore_index=True),
    )


def test_enrich_statuses():
    statuses = pd.DataFrame(
        columns=['date', 'endpoint', 'status', 'amount', 'too_long'],
        data=[('2020-10-10', '/ping', 200, 940, False),
              ('2020-10-10', '/ping', 200, 10, True),
              ('2020-10-10', '/ping', 503, 20, False),
              ('2020-10-10', '/ping', 503, 30, True),
              ('2020-10-10', '/test', 404, 100, False)]
    )

    external_sla = pd.DataFrame(
        columns=['date', 'endpoint', 'sla'],
        data=[('2020-10-10', '/ping', 0.95),
              ('2020-10-10', '/tiles', 0.99)]
    )

    enriched_statuses = enrich_statuses_with_external_sla(statuses, external_sla)

    expected_statuses = pd.DataFrame(
        columns=['date', 'endpoint', 'status', 'amount', 'too_long', 'external_good'],
        data=[('2020-10-10', '/ping', 200, 893,  False, True),
              ('2020-10-10', '/ping', 200, 47,  False, False),
              ('2020-10-10', '/ping', 200, 9.5, True, True),
              ('2020-10-10', '/ping', 200, 0.5, True, False),
              ('2020-10-10', '/ping', 503, 19,  False, True),
              ('2020-10-10', '/ping', 503, 1,  False, False),
              ('2020-10-10', '/ping', 503, 28.5, True, True),
              ('2020-10-10', '/ping', 503, 1.5, True, False),
              ('2020-10-10', '/test', 404, 100, False, True),
              ('2020-10-10', '/tiles', np.nan, np.nan, np.nan, True),
              ('2020-10-10', '/tiles', np.nan, np.nan, np.nan, False)]
    )

    assert enriched_statuses.round(3).equals(expected_statuses.round(3))


@patch('maps.infra.monitoring.sla_calculator.core.statbox.SLA_VERY_LOW_LIMIT', new=.707)
def test_description_for_lsr():
    return _description_for_lsr('2021-07-31', .808, .909, 'abcdef123', 'maps_core_teapot')


@patch('maps.infra.monitoring.sla_calculator.core.statbox.SLA_VERY_LOW_LIMIT', new=.707)
def test_description_very_low_for_lsr():
    return _description_for_lsr('2021-07-31', .606, .909, 'abcdef123', 'maps_core_teapot')
