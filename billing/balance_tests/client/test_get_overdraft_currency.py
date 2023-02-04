# -*- coding: utf-8 -*-

from collections import namedtuple
import datetime

import mock
import pytest

from balance import mapper, exc
from balance.constants import ServiceId, FirmId
from tests import object_builder as ob


ServiceFirmOverdraftParams = namedtuple(
    'ServiceFirmOverdraftParams',
    ['service_id', 'firm_id', 'start_dt', 'end_dt', 'payment_term_id', 'use_working_cal', 'fixed_currency', 'thresholds', 'turnover_firms']
)

OVERDRAFT_PARAMS_RUB = ServiceFirmOverdraftParams(
    **{'service_id': ServiceId.DIRECT, 'firm_id': FirmId.YANDEX_OOO, 'start_dt': None, 'end_dt': None,
       'payment_term_id': 15, 'use_working_cal': 1,
       'fixed_currency': 0, 'thresholds': {'RUB': 1},
       'turnover_firms': [FirmId.YANDEX_OOO]}
)
OVERDRAFT_PARAMS_RUB_FIXED = ServiceFirmOverdraftParams(
    **{'service_id': ServiceId.DIRECT, 'firm_id': FirmId.YANDEX_OOO, 'start_dt': None, 'end_dt': None,
       'payment_term_id': 15, 'use_working_cal': 1,
       'fixed_currency': 1, 'thresholds': {'RUB': 1},
       'turnover_firms': [FirmId.YANDEX_OOO]}
)
OVERDRAFT_PARAMS_BYN = ServiceFirmOverdraftParams(
    **{'service_id': ServiceId.DIRECT, 'firm_id': FirmId.YANDEX_OOO, 'start_dt': None, 'end_dt': None,
       'payment_term_id': 15, 'use_working_cal': 1,
       'fixed_currency': 0, 'thresholds': {'BYN': 1},
       'turnover_firms': [FirmId.YANDEX_OOO]}
)

YESTERDAY = datetime.date.today() - datetime.timedelta(days=1)
TOMORROW = datetime.date.today() + datetime.timedelta(days=1)


def create_client(session, is_agency=0):
    return ob.ClientBuilder(is_agency=is_agency).build(session).obj


def create_client_service_data(client, service_id=ServiceId.DIRECT, **kwargs):
    client_service_data = ob.create_client_service_data(**kwargs)
    client.service_data[service_id] = client_service_data
    return client_service_data


@pytest.mark.parametrize('is_agency', [0, 1])
@pytest.mark.parametrize(
    'with_overdraft, is_multicurrency, firm_id, service_firm_overdraft_params, expected_overdraft_currency',
    [
        pytest.param(False, False, None, [], None,
                     id='no params for bucks client with unspecified firm'),
        pytest.param(False, False, FirmId.YANDEX_OOO, [], None,
                     id='no params for bucks client with specified firm'),
        pytest.param(False, False, None, [OVERDRAFT_PARAMS_RUB], None,
                     id='unfixed currency for bucks client with unspecified firm'),
        pytest.param(False, False, FirmId.YANDEX_OOO, [OVERDRAFT_PARAMS_RUB], None,
                     id='unfixed currency for bucks client with specified firm'),
        pytest.param(False, False, None, [OVERDRAFT_PARAMS_RUB_FIXED], None,
                     id='fixed currency for bucks client with unspecified firm'),
        pytest.param(False, False, FirmId.YANDEX_OOO, [OVERDRAFT_PARAMS_RUB_FIXED], 'RUB',
                     id='fixed currency for bucks client with specified firm'),

        pytest.param(False, True, None, [], 'KZT',
                     id='no params for money client with unspecified firm'),
        pytest.param(False, True, FirmId.YANDEX_OOO, [], 'KZT',
                     id='no params for money client with specified firm'),
        pytest.param(False, True, None, [OVERDRAFT_PARAMS_RUB], 'KZT',
                     id='unfixed currency for money client with unspecified firm'),
        pytest.param(False, True, FirmId.YANDEX_OOO, [OVERDRAFT_PARAMS_RUB], 'KZT',
                     id='unfixed currency for money client with specified firm'),
        pytest.param(False, True, None, [OVERDRAFT_PARAMS_RUB_FIXED], 'KZT',
                     id='fixed currency for money client with unspecified firm'),
        pytest.param(False, True, FirmId.YANDEX_OOO, [OVERDRAFT_PARAMS_RUB_FIXED], 'KZT',
                     id='fixed currency for money client with specified firm'),

        pytest.param(True, True, None, [], 'KZT',
                     id='no params for money client with overdraft with unspecified firm'),
        pytest.param(True, True, FirmId.YANDEX_OOO, [], 'BYN',
                     id='no params for money client with overdraft with specified firm'),
        pytest.param(True, True, None, [OVERDRAFT_PARAMS_RUB], 'KZT',
                     id='unfixed currency for money client with overdraft with unspecified firm'),
        pytest.param(True, True, FirmId.YANDEX_OOO, [OVERDRAFT_PARAMS_RUB], 'BYN',
                     id='unfixed currency for money client with overdraft with specified firm'),
        pytest.param(True, True, None, [OVERDRAFT_PARAMS_RUB_FIXED], 'KZT',
                     id='fixed currency for money client with overdraft with unspecified firm'),
        pytest.param(True, True, FirmId.YANDEX_OOO, [OVERDRAFT_PARAMS_RUB_FIXED], 'BYN',
                     id='fixed currency for money client with overdraft with specified firm'),
    ]
)
def test_get_overdraft_currency(
    session, is_agency, with_overdraft,
    is_multicurrency, firm_id, service_firm_overdraft_params, expected_overdraft_currency
):
    client = create_client(session, is_agency)
    if is_multicurrency:
        create_client_service_data(client, currency='KZT')

    if with_overdraft:
        client.overdraft[(ServiceId.DIRECT, FirmId.YANDEX_OOO)] = mapper.ClientOverdraft(
            ServiceId.DIRECT, FirmId.YANDEX_OOO, limit=1, limit_wo_tax=1, iso_currency='BYN'
        )

    session.flush()

    with mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', return_value=service_firm_overdraft_params):
        overdraft_currency = client.get_overdraft_currency(ServiceId.DIRECT, firm_id)

    assert overdraft_currency == expected_overdraft_currency
