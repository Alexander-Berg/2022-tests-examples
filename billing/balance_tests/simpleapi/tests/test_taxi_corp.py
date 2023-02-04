# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common import logger
from simpleapi.common.payment_methods import Compensation, Cash
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.steps import simple_steps as simple

__author__ = 'fellow'
'''
https://wiki.yandex-team.ru/balance/tz/taxicorporate/

'''

log = logger.get_logger()

service = Services.TAXI_CORP

pytestmark = marks.simple_internal_logic


class Data(object):
    compensation_data = [
        DataObject(paymethod=Compensation(),
                   country_data=defaults.CountryData.Russia,
                   user_type=uids.Types.random_from_all),
    ]
    cash_data = [
        DataObject(paymethod=Cash(),
                   country_data=defaults.CountryData.Russia,
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=Cash(),
                   country_data=defaults.CountryData.Armenia,
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=Cash(),
                   country_data=defaults.CountryData.Kazakhstan,
                   user_type=uids.Types.random_from_all),
    ]

    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.Service.TaxiCorp)
class TestTaxiCorp(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data',
                             Data.cash_data +
                             Data.compensation_data,
                             ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle(self, test_data, orders_structure):
        user_type, paymethod, country = test_data.user_type, test_data.paymethod, test_data.country_data
        currency = country['currency']
        user = uids.get_random_of_type(user_type)
        basket = simple.process_payment(service, user, orders_structure=orders_structure, paymethod=paymethod,
                                        need_postauthorize=True, currency=currency)
        simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

if __name__ == '__main__':
    pytest.main()
