# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.payment_methods import Cash
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()

service = Services.DOSTAVKA
PAYMENTS_COUNT = 3  # count of payment in multipayment case
PARTNER_ID = 2237685  # see TRUST-894 for details

pytestmark = marks.simple_internal_logic

"""
https://st.yandex-team.ru/TRUST-894
"""


class Data(object):
    test_data = [
        DataObject(paymethod=Cash(partner_id=PARTNER_ID), user_type=uids.Types.anonymous),
    ]

    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.Service.Dostavka)
class TestDostavka(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_multipayment_cycle(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user, orders_structure=orders_structure)
        payments = []
        with reporter.step(u'Совершаем несколько платежей (мультиплатеж)'):
            for _ in range(PAYMENTS_COUNT):
                with check_mode(CheckMode.FAILED):
                    basket = simple.create_basket(service, user=user,
                                                  orders=orders,
                                                  paymethod_id=paymethod.id)
                    payment_form = simple.pay_basket(service, user=user,
                                                     trust_payment_id=basket['trust_payment_id']).get(
                        'payment_form')
                    trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                                 purchase_token=basket['purchase_token'])

                    basket = simple.wait_until_payment_done(service, user=user,
                                                            purchase_token=basket['purchase_token'])

                    payments.append(basket['trust_payment_id'])

            for payment in payments:
                basket = simple.check_basket(service, user=user, trust_payment_id=payment)
                simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)


if __name__ == '__main__':
    pytest.main()
