# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta
import hamcrest as hm
from balance.completions_fetcher.configurable_partner_completion import BaseExtractor, RawExtractor, FieldExtractor, \
    FieldWithStatusExtractor, HealthExtractor, BalalaykaExtractor, FetcherError


# RAW EXTRACTOR
def test_raw_extractor():
    extractor = RawExtractor()
    expected = [1, 2, 3]
    actual = extractor.process(expected)
    hm.assert_that(actual, hm.equal_to(expected))


# FIELD EXTRACTOR
@pytest.mark.parametrize('data, field, expected', [
    [
        {'data': [1, 2, 3]}, 'data', [1, 2, 3]
    ],
    [
        {'data': {'items':  [1, 2, 3]}}, 'data.items', [1, 2, 3]
    ],
    [
        {'data': {'items':  []}}, 'data.items', []
    ],
    [
        {'data': {'items':  []}}, 'data', {'items':  []}
    ],
    [
        None, 'data', None
    ],
])
def test_field_extractor(data, field, expected):
    extractor = FieldExtractor(field=field)
    actual = extractor.process(data)
    hm.assert_that(actual, hm.equal_to(expected))


# FIELD WITH STATUS CHECK EXTRACTOR
@pytest.mark.parametrize('data, field, status_field, ok_status, with_data_ready_check, start_dt, expected', [
    [
        {'data': [1, 2, 3], 'status': 200},
        'data', 'status', 200, False, None, [1, 2, 3]
    ],
    [
        {'data': {'items':  [1, 2, 3], 'result': 'OK'}},
        'data.items', 'data.result', 'OK', False, None, [1, 2, 3]
    ],
    [
        {'data': {'items':  [], 'result': 'BAD'}},
        'data.items', 'data.result', 'OK', True, datetime.now() + timedelta(days=1), []
    ]
])
def test_field_with_status_extractor(data, field, status_field, ok_status,
                                     with_data_ready_check, start_dt, expected):
    extractor = FieldWithStatusExtractor(field=field, status_field=status_field,
                                         ok_status=ok_status, start_dt=start_dt,
                                         with_data_ready_check=with_data_ready_check)
    actual = extractor.process(data)
    hm.assert_that(actual, hm.equal_to(expected))


@pytest.mark.parametrize(
    'data, status_field, ok_status, with_data_ready_check, start_dt, error_fields, expected_error',
    [
        # если статус неуспешный и не проверяем готовность данных
        [
            {'data': [], 'status': 'bad', 'error': 'Custom'},
            'status', 'ok', False, None, ['error'], 'Custom'
        ],
        # если статус неуспешный, готовность проверяем и данные готовы
        [
            {'data': [], 'status': 'bad', 'messages': {'errors': ['1', '2', '3']}},
            'status', 'ok', True, datetime.now() - timedelta(days=1),
            ['status', 'messages.errors'], "bad ['1', '2', '3']"
        ]
    ])
def test_field_with_status_extractor_raise(data, status_field, ok_status,
                                           with_data_ready_check, start_dt, error_fields,
                                           expected_error):
    extractor = FieldWithStatusExtractor(field='field', status_field=status_field,
                                         ok_status=ok_status, start_dt=start_dt,
                                         with_data_ready_check=with_data_ready_check,
                                         error_fields=error_fields)
    with pytest.raises(FetcherError) as e:
        extractor.process(data)
    assert str(e.value).endswith(expected_error)


# HEALTH EXTRACTOR
@pytest.mark.parametrize('data, expected', [
    [
        {
          "data": [
            {
              "attributes": {
                "ClientID": 15919809,
                "appointments": [
                  {
                    "data": {
                      "attributes": {
                        "appointment_id": 1,
                        "price": 100,
                        "service_date": "2019-01-20T15:59:45.165483+03:00"
                      }
                    }
                  },
                  {
                    "data": {
                      "attributes": {
                        "appointment_id": 2,
                        "price": 200,
                        "service_date": "2019-01-20T16:00:45.165483+03:00"
                      }
                    }
                  }
                ]
              }
            },
            {
              "attributes": {
                "ClientID": 15919890,
                "appointments": [
                  {
                    "data": {
                      "attributes": {
                        "appointment_id": 3,
                        "price": 300,
                        "service_date": "2019-01-21T17:59:45.165483+03:00"
                      }
                    }
                  }
                ]
              }
            }
          ]
        },
        [
            {'client_id': 15919809, 'price': 100, 'service_dt': '2019-01-20', 'appointment_id': 1},
            {'client_id': 15919809, 'price': 200, 'service_dt': '2019-01-20', 'appointment_id': 2},
            {'client_id': 15919890, 'price': 300, 'service_dt': '2019-01-21', 'appointment_id': 3},
        ]
    ]
])
def test_health_extractor(data, expected):
    extractor = HealthExtractor()
    actual = extractor.process(data)
    actual = list(actual)
    hm.assert_that(actual, hm.equal_to(expected))


# BALALAYKA EXTRACTOR
@pytest.mark.parametrize('data, expected', [
    [
        [
            {
                'currency': 'RUB',
                'doc_number': 535232,
                'payment_system_answer': '{"processedDT": "1999-01-01T10:21:38.792+03:00"}',
                'metadata': '{"client_id": 100}',
                'summ': '1.00',
                't_acc_type': 'wallet',
            },
            {
                'currency': 'RUB',
                'doc_number': 635232,
                'payment_system_answer': '{"items": [{"time_processed": "2000-01-01T10:21:38.792+03:00"}]}',
                'metadata': '{"client_id": 300}',
                'summ': '3.52',
            }
        ],
        [
            {'price': '1.00', 'dt': '1999-01-01', 'transaction_dt': '1999-01-01', 'client_id': 100,
             'transaction_id': 535232, 'currency': 'RUB', 'payment_type': 'wallet'},
            {'price': '3.52', 'dt': '2000-01-01', 'transaction_dt': '2000-01-01', 'client_id': 300,
             'transaction_id': 635232, 'currency': 'RUB', 'payment_type': None}
        ]
    ]
])
def test_balalayka_extractor(data, expected):
    extractor = BalalaykaExtractor()
    actual = extractor.process(data)
    actual = list(actual)
    hm.assert_that(actual, hm.equal_to(expected))


# BASE EXTRACTOR
@pytest.mark.parametrize('expected_class', [
    RawExtractor, FieldExtractor, FieldWithStatusExtractor,
    HealthExtractor, BalalaykaExtractor
])
def test_children(expected_class):
    actual_class = BaseExtractor.children.get(expected_class.id)
    assert actual_class == expected_class, 'Extractor class is different than expected'



