import datetime

import pytest
import pytz
from unittest import mock

from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.balance2apikeys import CommonBalance, Balance2Apikeys


tz_msk = pytz.timezone('Europe/Moscow')


class FakeCommonBalance:
    def __init__(self, data_returns_get_client_contracts):
        self.data_returns_get_client_contracts = data_returns_get_client_contracts

    def get_client_contracts(
        self, client_id, contract_type='GENERAL', dt=None, person_id=None, signed=True, external_id=None
    ):
        return self.data_returns_get_client_contracts


def test_create_contract(mongomock, user, ur_person, empty_tariff):
    b2a = Balance2Apikeys()

    # отсутствует BalanceConfig у сервиса
    with pytest.raises(CommonBalance.BalanceError) as exc_info:
        b2a.create_contract(user, ur_person, empty_tariff)

    assert 'Service does not support work with Balance.' in str(exc_info.value)

    # пустой BalanceConfig у сервиса
    service = empty_tariff.service
    service.balance_config = mapper.BalanceConfig()
    service.save()
    with pytest.raises(CommonBalance.BalanceError) as exc_info:
        b2a.create_contract(user, ur_person, empty_tariff)

    assert 'Service does not support contract creating.' in str(exc_info.value)


@mock.patch(
    'billing.apikeys.apikeys.balance2apikeys.common_balance',
    new=lambda: FakeCommonBalance(data_returns_get_client_contracts=[
        {
            'FINISH_DT': datetime.datetime(2021, 3, 20, 0, 0),
            'SERVICE_START_DT': datetime.datetime(2020, 2, 1, 0, 0),
            'CURRENCY': 'RUR',
            'IS_SUSPENDED': 0,
            'MANAGER_CODE': 20453,
            'IS_ACTIVE': 1,
            'IS_SIGNED': 1,
            'CONTRACT_TYPE': 0,
            'PERSON_ID': 32268220,
            'IS_FAXED': 0,
            'SERVICES': [129],
            'PAYMENT_TYPE': 3,
            'APIKEYS_TARIFFS': [{'DT': datetime.datetime(2020, 2, 1, 0, 0),
                                 'IS_ACTIVE': 1,
                                 'TARIFFS': {
                                     'apikeys_apimaps': 'apikeys_apimaps_1000_yearprepay_ban_minus_2018'}}],
            'IS_CANCELLED': 0,
            'IS_DEACTIVATED': 0,
            'DT': datetime.datetime(2020, 2, 1, 0, 0),
            'EXTERNAL_ID': '660046/20',
            'ID': 1591232
        }, {
            'APIKEYS_TARIFFS': [
                {
                    'DT': datetime.datetime(2015, 9, 21, 0, 0),
                    'IS_ACTIVE': 1,
                    'TARIFFS': {'apikeys_apimaps': 'apikeys_apimaps_50000_ban'}
                }
            ],
            'CONTRACT_TYPE': 0,
            'CREDIT_LIMITS': [{'ACTIVITY_TYPE': None, 'CURRENCY': 'RUB', 'LIMIT': '1.17'}],
            'CURRENCY': 'RUR',
            'DT': datetime.datetime(2015, 9, 21, 0, 0),
            'EXTERNAL_ID': '95007/15',
            'ID': 276590,
            'IS_ACTIVE': 1,
            'IS_CANCELLED': 0,
            'IS_FAXED': 0,
            'IS_SIGNED': 1,
            'IS_SUSPENDED': 0,
            'MANAGER_CODE': 22554,
            'PAYMENT_TERM': 5,
            'PAYMENT_TYPE': 3,
            'PERSON_ID': 3707919,
            'SERVICES': [129, 659]
        }
    ])
)
def test_get_client_contracts(mongomock, user):
    b2a = Balance2Apikeys()

    parsed_contracts = b2a.get_balance_contracts(user)

    assert parsed_contracts[0].balance_contract_external_id == '660046/20'
    assert parsed_contracts[0].balance_contract_id == '1591232'
    assert parsed_contracts[0].finish_date == tz_msk.localize(datetime.datetime(2021, 3, 20, 0, 0))
    assert parsed_contracts[0].date == tz_msk.localize(datetime.datetime(2020, 2, 1, 0, 0))
    assert parsed_contracts[0].service_start_date == tz_msk.localize(datetime.datetime(2020, 2, 1, 0, 0))
    assert parsed_contracts[0].flow_type == 'classic_contract_flow'
    assert parsed_contracts[0].tariffless_services == []
    assert parsed_contracts[0].person_id == 32268220
    assert parsed_contracts[0].is_active is True
    assert parsed_contracts[0].is_canceled is False
    assert parsed_contracts[0].is_faxed is False
    assert parsed_contracts[0].is_signed is True
    assert parsed_contracts[0].collaterals[0].finish_date == tz_msk.localize(datetime.datetime(2021, 3, 20, 0, 0))
    assert parsed_contracts[0].collaterals[0].date == tz_msk.localize(datetime.datetime(2020, 2, 1, 0, 0))
    assert parsed_contracts[0].collaterals[0].tariffs == {'apimaps': 'apimaps_1000_yearprepay_ban_minus_2018'}

    assert parsed_contracts[1].balance_contract_external_id == '95007/15'
    assert parsed_contracts[1].balance_contract_id == '276590'
    assert parsed_contracts[1].finish_date is None
    assert parsed_contracts[1].date == tz_msk.localize(datetime.datetime(2015, 9, 21, 0, 0))
    assert parsed_contracts[1].service_start_date is None
    assert parsed_contracts[1].flow_type == 'tariffless_contract_flow'
    assert parsed_contracts[1].tariffless_services == ['apimaps', 'staticmaps', 'city', 'routingmatrix', 'mapkit']
    assert parsed_contracts[1].person_id == 3707919
    assert parsed_contracts[1].is_active is True
    assert parsed_contracts[1].is_canceled is False
    assert parsed_contracts[1].is_faxed is False
    assert parsed_contracts[1].is_signed is True
    assert parsed_contracts[1].collaterals[0].finish_date is None
    assert parsed_contracts[1].collaterals[0].date == tz_msk.localize(datetime.datetime(2015, 9, 21, 0, 0))
    assert parsed_contracts[1].collaterals[0].tariffs == {'apimaps': 'apimaps_50000_ban'}
