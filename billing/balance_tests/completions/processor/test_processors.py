# -*- coding: utf-8 -*-
import pytest
import mock
from datetime import datetime
from balance.completions_fetcher.configurable_partner_completion import ClusterBaseProcessor


@pytest.mark.parametrize('start_dt, end_dt, date_cluster, expected_calls', [
    [
        datetime(2019, 9, 1), datetime(2019, 9, 2), 1,
        [mock.call(datetime(2019, 9, i), datetime(2019, 9, i)) for i in range(1, 3)]
    ],
    [
        datetime(2019, 9, 1), datetime(2019, 9, 10), 2,
        [mock.call(datetime(2019, 9, i), datetime(2019, 9, i + 1)) for i in range(1, 10, 2)]
    ],
    # с укорачиванием последнего отрезка
    [
        datetime(2019, 9, 1), datetime(2019, 9, 24), 10,
        [
            mock.call(datetime(2019, 9, 1), datetime(2019, 9, 10)),
            mock.call(datetime(2019, 9, 11), datetime(2019, 9, 20)),
            mock.call(datetime(2019, 9, 21), datetime(2019, 9, 24)),
        ]
    ],
    [
        datetime(2019, 9, 1), datetime(2019, 9, 30), 60,
        [
            mock.call(datetime(2019, 9, 1), datetime(2019, 9, 30)),
        ]
    ],
])
def test_cluster_processor_iteration(start_dt, end_dt, date_cluster, expected_calls):
    config = {'partition_exchange': False}  # выключаем клонирование сессии для упрощения логики теста
    proc = ClusterBaseProcessor(date_cluster=date_cluster, config=config,
                                start_dt=start_dt, end_dt=end_dt, session=None)
    with mock.patch.object(proc, 'get_processor') as getter_mock:
        proc.process()

    expected_call_count = len(expected_calls)
    assert getter_mock.call_count == expected_call_count, 'Expected calls count is {expected}, but got {actual}'\
        .format(expected=expected_call_count, actual=getter_mock.call_count)
    getter_mock.assert_has_calls(expected_calls, any_order=True)


@pytest.mark.parametrize('date_cluster, expected_error', [
    [0, 'Cluster size is not specified'],
    [-1, 'Cluster size can not be negative'],
    [1.25, 'Cluster size should be int']
])
def test_cluster_processor_raise_on_incorrect_date_cluster(date_cluster, expected_error):
    config = {'partition_exchange': False}  # выключаем клонирование сессии для упрощения логики теста
    with pytest.raises(ValueError) as e:
        ClusterBaseProcessor(date_cluster=date_cluster, config=config, start_dt=None, end_dt=None, session=None)
    assert (str(e.value)).startswith(expected_error)


@pytest.mark.parametrize('start_dt, end_dt, date_cluster, is_chained, expected_call_count', [
    [datetime(2019, 9, 1), datetime(2019, 9, 5), 1, False, 5],  # non chained raises after all
    [datetime(2019, 9, 1), datetime(2019, 9, 5), 1, True, 1]  # chained raises immediately
])
def test_cluster_processor_raise_on_chained(start_dt, end_dt, date_cluster, is_chained, expected_call_count):
    def raise_error(*args, **kwargs):
        raise ValueError('Error')

    processor_mock = mock.MagicMock()
    processor_mock.process = mock.MagicMock(side_effect=raise_error)
    config = {'partition_exchange': False}  # выключаем клонирование сессии для упрощения логики теста
    proc = ClusterBaseProcessor(date_cluster=date_cluster, config=config,
                                start_dt=start_dt, end_dt=end_dt, session=None)

    with mock.patch.object(proc, 'get_processor', return_value=processor_mock) as getter_mock:
        with pytest.raises(Exception):
            proc.process(is_chained=is_chained)
    assert getter_mock.call_count == expected_call_count, 'Expected calls count is {expected}, but got {actual}' \
        .format(expected=expected_call_count, actual=processor_mock.call_count)
