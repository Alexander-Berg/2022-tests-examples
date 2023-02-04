# coding=utf-8

import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.payment_methods import LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.matchers.deep_equals import deep_equals_to
from simpleapi.steps import balance_steps as balance
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()

service = Services.TRANSLATE


class Multicurrency(object):
    SERVICE_ORDER_ID = 13
    CLIENT_ID = 7307630


class Data(object):
    DATA_RUB = DataObject(currency='RUB', region_id=225).new(person_type=defaults.Person.PH,
                                                             product_id=defaults.Direct.product_id)

    general = [
        DATA_RUB.new(orders_structure={'qty': defaults.Order.qty, 'service_order_id': Multicurrency.SERVICE_ORDER_ID}),
    ]
    paymethods = [
        DataObject(paymethod=LinkedCard(card=get_card()), user_type=uids.Types.random_from_all),
    ]

    # todo fellow: это должен быть универсальный метод в DataObject
    @staticmethod
    def astuple(data):
        return data.astuple(keys=('orders_structure',
                                  'currency',
                                  'region_id',
                                  'person_type',
                                  'product_id'))


@reporter.feature(features.Service.Translator)
class TestTranslator(object):
    @staticmethod
    def prepare_data(user_type):
        with reporter.step(u'Подготавливаем клиента и пользователя в Балансе'):
            user = uids.get_random_of_type(user_type)
            client = Multicurrency.CLIENT_ID
            balance.associate_user_to_client(client, uid=user.uid)
            return user, client,

    @reporter.story(stories.General.Autopayment)
    @pytest.mark.parametrize('general_data', Data.general, ids=DataObject.ids)
    @pytest.mark.parametrize('paymethods_data', Data.paymethods, ids=DataObject.ids_paymethod)
    def test_autopayment(self, general_data, paymethods_data):
        orders_structure, currency, region_id, person_type, product_id = Data.astuple(general_data)

        user, client = self.prepare_data(paymethods_data.user_type)

        paymethod = paymethods_data.paymethod
        paymethod.init(service=service, user=user)
        orders = simple.form_orders_for_create(service, client=client,
                                               service_product_id=product_id,
                                               orders_structure=orders_structure, from_balance=True)

        with reporter.step(u'Cимулируем автоплатеж'):
            for _ in range(3):
                with check_mode(CheckMode.FAILED):
                    basket = simple.create_basket(service, user=user,
                                                  orders=orders,
                                                  paymethod_id=paymethod.id,
                                                  currency=currency)

                    initial_amount, initial_qty = \
                        simple.get_basket_initial_amount_and_qty(service, user=user,
                                                                 trust_payment_id=basket['trust_payment_id'])

                    simple.pay_basket(service, user=user,
                                      trust_payment_id=basket['trust_payment_id'])
                    trust.pay_by(paymethod, service, purchase_token=basket['purchase_token'])
                    simple.wait_until_payment_done(service, user=user,
                                                   trust_payment_id=basket['trust_payment_id'])

                    basket_paid = simple.check_basket(service, user=user,
                                                      trust_payment_id=basket['trust_payment_id'])
                    check.check_that(basket_paid,
                                     deep_equals_to(expected.basket_translator_autopaid(paymethod, orders,
                                                                                        initial_amount=initial_amount,
                                                                                        initial_qty=initial_qty,
                                                                                        currency=currency)))


if __name__ == '__main__':
    pytest.main()
