# coding=utf-8

import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Pytest
from simpleapi.common import logger
from simpleapi.common.payment_methods import LinkedCard, YandexMoney
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, Private
from simpleapi.matchers.deep_equals import deep_equals_to
from simpleapi.steps import balance_steps as balance
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()

service = Services.DIRECT


class Multicurrency(object):
    SERVICE_ORDER_ID = 3110699
    CLIENT_ID = 736521


class Data(object):
    DATA_RUB = DataObject(currency='RUB', region_id=225).new(person_type=defaults.Person.PH,
                                                             product_id=defaults.Direct.product_id)
    DATA_UAH = DataObject(currency='UAH', region_id=187).new(person_type=defaults.Person.UA,
                                                             product_id=defaults.Direct.product_id_uah)

    general_rub = [
        DATA_RUB.new(orders_structure={'qty': defaults.Order.qty, 'service_order_id': Multicurrency.SERVICE_ORDER_ID},
                     descr='Converted multicurrency client, currency=RUB'),
        # DATA_RUB.new(orders_structure={'qty': defaults.Order.qty, 'unmoderated': 1},
        #              descr='Multicurrency client, unmoderated order'),
        DATA_RUB.new(orders_structure={'qty': defaults.Order.qty},
                     descr='Multicurrency client, moderated order, currency=RUB'),
    ]
    general_uah = [
        DATA_UAH.new(orders_structure={'qty': defaults.Order.qty, 'currency': 'UAH'},
                     descr='Multicurrency client, moderated order, currency=UAH'),
    ]
    paymethods_rub = [
        DataObject(paymethod=LinkedCard(card=get_card()), user_type=uids.Types.random_from_all),
        pytest.mark.yamoney(
            DataObject(paymethod=YandexMoney(), user_type=uids.Type(pool=uids.secret, name='test_wo_proxy_old'))),
    ]
    paymethods_uah = [
        DataObject(paymethod=LinkedCard(card=Private.Valid.card_uah), user_type=uids.Types.random_from_all),
    ]

    # todo fellow: это должен быть универсальный метод в DataObject
    @staticmethod
    def astuple(data):
        return data.astuple(keys=('orders_structure',
                                  'currency',
                                  'region_id',
                                  'person_type',
                                  'product_id'))


@reporter.feature(features.Service.Direct)
class TestDirect(object):
    @staticmethod
    def prepare_data(user_type, orders_structure, person_type, region_id):
        with reporter.step(u'Подготавливаем клиента и пользователя в Балансе'):
            user = uids.get_random_of_type(user_type)
            is_converted = orders_structure.get('service_order_id') is not None
            if is_converted:
                client = Multicurrency.CLIENT_ID
            else:
                _, client = balance.create_client(region_id=region_id)
                balance.create_person(client, default_person=person_type)

            balance.associate_user_to_client(client, uid=user.uid)
            return user, client, is_converted

    @pytest.mark.no_parallel
    @reporter.story(stories.General.Autopayment)
    @pytest.mark.parametrize(*Pytest.combine_set(Pytest.ParamsSet(names='general_data',
                                                                  values=[Data.general_rub,
                                                                          # Data.general_uah
                                                                          ]),
                                                 Pytest.ParamsSet(names='paymethods_data',
                                                                  values=[Data.paymethods_rub,
                                                                          # Data.paymethods_uah
                                                                          ])),
                             ids=lambda general_data, paymethods_data: '{}-{}'.format(
                                 DataObject.ids(general_data), DataObject.ids_paymethod(paymethods_data)))
    def test_autopayment(self, general_data, paymethods_data):

        orders_structure, currency, region_id, person_type, product_id = Data.astuple(general_data)

        user, client, is_converted = self.prepare_data(paymethods_data.user_type,
                                                       orders_structure,
                                                       person_type,
                                                       region_id)
        paymethod = paymethods_data.paymethod
        paymethod.init(service=service, user=user)
        orders = simple.form_orders_for_create(service, client=client,
                                               service_product_id=product_id,
                                               orders_structure=orders_structure, from_balance=True)

        with reporter.step(u'Cимулируем автоплатеж так как это делает Директ'):
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
                                     deep_equals_to(expected.basket_direct_autopaid(paymethod,
                                                                                    orders,
                                                                                    initial_amount=initial_amount,
                                                                                    initial_qty=initial_qty,
                                                                                    is_converted=is_converted,
                                                                                    currency=currency)))


if __name__ == '__main__':
    pytest.main()
