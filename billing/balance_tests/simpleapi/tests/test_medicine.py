# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Pytest
from simpleapi.common.payment_methods import LinkedCard, ApplePay
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories, marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.steps import web_steps as web

__author__ = 'fellow'

service = Services.MEDICINE_PAY


class Data(object):
    test_data = [
        DataObject(paymethod=LinkedCard(card=get_card(),
                                        list_payment_methods_callback=payments_api.PaymentMethods.get),
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=LinkedCard(card=get_card(),
                                        list_payment_methods_callback=payments_api.PaymentMethods.get),
                   user_type=uids.Types.random_autoremove),

    ]
    test_data_applepay = [
        marks.apple_pay(DataObject(paymethod=ApplePay(bind_token=True), user_type=uids.Types.random_from_all)),
    ]

    orders_structure = [
        [{'region_id': 225, 'currency': 'RUB', 'price': 10},
         {'region_id': 225, 'currency': 'RUB', 'price': 20.5},
         ],
        None
    ]
    orders_structure_applepay = [
        ({'region_id': 225, 'currency': 'RUB', 'price': 10},)
    ]


@reporter.feature(features.Service.Medicine)
class TestMedicine(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize(*Pytest.combine_set(Pytest.ParamsSet(names='test_data',
                                                                  values=[
                                                                      Data.test_data,
                                                                      Data.test_data_applepay,
                                                                  ]),
                                                 Pytest.ParamsSet(names='orders_structure',
                                                                  values=[
                                                                      Data.orders_structure,
                                                                      Data.orders_structure_applepay,
                                                                  ])),
                             ids=lambda test_data, orders_structure: '{} {}'.format(
                                 DataObject.ids(test_data), DataObject.ids_orders(orders_structure))
                             )
    def test_base_payment_cycle(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure,
                                                   need_clearing=True)

            payments_api.Refunds.process(service, user, basket['purchase_token'])

    @reporter.story(stories.General.BindingCard)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_payment_after_binding(self, orders_structure):
        user_type = uids.Types.random_from_all
        user = uids.get_random_of_type(user_type)

        card = get_card()
        paymethod = LinkedCard(card)

        with check_mode(CheckMode.FAILED):
            resp = payments_api.Bindings.create(service, user)
            binding = payments_api.Bindings.start(service, user, resp['purchase_token'])
            web.Medicine.bind_card(card, binding['binding_url'])
            payments_api.Wait.until_binding_done(service, user, resp['purchase_token'])
            trust_payment_id = mongo.Payment.get_id_by_purchase_token(purchase_token=resp['purchase_token'])
            mongo.Refund.wait_until_done(trust_payment_id)

            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure,
                                                   need_clearing=True)
            payments_api.Refunds.process(service, user, basket['purchase_token'])


if __name__ == '__main__':
    pytest.main()
