# coding=utf-8
from decimal import Decimal

import pytest
from hamcrest import is_, none, equal_to

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Pytest
from simpleapi.common.payment_methods import TrustWebPage, Via, Card, ApplePay, Subsidy, LinkedCard, Cash
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number
from simpleapi.steps import check_steps as check
from simpleapi.steps import simple_steps as simple

__author__ = 'fellow'


class Data(object):
    DATA_WITH_CARD = DataObject(card=get_card())
    test_link_paymethods = [
        marks.web_in_browser(
            DATA_WITH_CARD.new(
                paymethod1=TrustWebPage(Via.card(card=DATA_WITH_CARD.card, save_card=True), in_browser=True),
                paymethod2=TrustWebPage(Via.linked_card(card=DATA_WITH_CARD.card), in_browser=True)),
        )
    ]
    services = [
        Services.MARKETPLACE,
        Services.NEW_MARKET,
        Services.BLUE_MARKET_PAYMENTS,
        Services.RED_MARKET_PAYMENTS,
    ]
    test_data = [
        DataObject(paymethod=TrustWebPage(Via.card(card=get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=TrustWebPage(Via.linked_card(card=get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all),
        # DataObject(paymethod=TrustWebPage(Via.card(card=get_card()), in_browser=True,
        #                                   template_tag=defaults.TemplateTag.mobile),
        #            user_type=uids.Types.random_from_all),
        # DataObject(paymethod=TrustWebPage(Via.linked_card(card=get_card()), in_browser=True,
        #                                   template_tag=defaults.TemplateTag.mobile),
        #            user_type=uids.Types.random_from_all),
        DataObject(paymethod=LinkedCard(card=get_card()), user_type=uids.Types.random_from_all),
        DataObject(paymethod=LinkedCard(card=get_card()), user_type=uids.Types.random_from_all),
        # https://st.yandex-team.ru/TRUST-2464 - добавлена возможность анонимной оплаты через мобильное приложение
        DataObject(paymethod=Card(card=get_card(), payment_mode=defaults.PaymentMode.API_PAYMENT),
                   user_type=uids.Types.anonymous),
        # https://st.yandex-team.ru/TRUST-2464 - добавлена возможность оплаты через apple_token
        marks.apple_pay(
            DataObject(paymethod=ApplePay(bind_token=False, pass_to_supply_payment_data=True),
                       user_type=uids.Types.random_from_all)),
    ]

    test_data_red_market = [
        DataObject(paymethod=TrustWebPage(Via.card(card=get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=Card(card=get_card(), payment_mode=defaults.PaymentMode.API_PAYMENT),
                   user_type=uids.Types.anonymous),
    ]
    test_data_no_save_card = [
        DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]
    services_currency = [
        Services.RED_MARKET_SUBSIDY
    ]
    services_with_qty = [
        Services.NEW_MARKET,
        Services.BLUE_MARKET_PAYMENTS,
        Services.BLUE_MARKET_SUBSIDY,
        Services.BLUE_MARKET_REFUNDS
    ]
    test_data_orders_structure_qty = [
        DataObject(orders_structure=[{'currency': 'RUB', 'price': defaults.Order.price,
                                      'fiscal_nds': defaults.Fiscal.NDS.nds_none,
                                      'fiscal_title': defaults.Fiscal.fiscal_title,
                                      'qty': defaults.Order.qty}, ], descr='One order, qty is integer number'),
        DataObject(orders_structure=[{'currency': 'RUB', 'price': defaults.Order.price,
                                      'fiscal_nds': defaults.Fiscal.NDS.nds_none,
                                      'fiscal_title': defaults.Fiscal.fiscal_title,
                                      'qty': 22.50}, ], descr='One order, qty is float number'),
        DataObject(orders_structure=[{'currency': 'RUB', 'price': defaults.Order.price,
                                      'fiscal_nds': defaults.Fiscal.NDS.nds_none,
                                      'fiscal_title': defaults.Fiscal.fiscal_title,
                                      'qty': 22.50}, {'currency': 'RUB', 'price': defaults.Order.price,
                                                      'fiscal_nds': defaults.Fiscal.NDS.nds_none,
                                                      'fiscal_title': defaults.Fiscal.fiscal_title,
                                                      'qty': 0.50}], descr='Two orders, different qtys')
    ]


def ids_currency(val):
    return "currency={}".format(val)


@reporter.feature(features.Service.Marketplace)
class TestMarketplace(object):
    @marks.ym_h2h_processing
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('service', [Services.MARKETPLACE, Services.NEW_MARKET, Services.BLUE_MARKET_PAYMENTS],
                             ids=DataObject.ids_service)
    @pytest.mark.parametrize('paymethods', Data.test_link_paymethods, ids=DataObject.ids)
    def test_link_card_and_payment(self, service, paymethods):
        user = uids.get_random_of(uids.all_)

        with reporter.step(u'Совершаем платеж и сохраняем карту, которой совершали оплату'):
            paymethod = paymethods.paymethod1
            orders = simple.form_orders_for_create(service, user)

            simple.process_payment(service, user=user, orders=orders,
                                   paymethod=paymethod, need_postauthorize=True)

        with reporter.step(u'Оплачиваем новую корзину сохраненной картой'):
            paymethod = paymethods.paymethod2
            orders = simple.form_orders_for_create(service, user)

            simple.process_payment(service, user=user, orders=orders, paymethod=paymethod,
                                   need_postauthorize=True)

    @pytest.mark.yamoney
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_pay_by_yamoney(self, service):
        paymethod = TrustWebPage(Via.yandex_money())
        user = uids.get_random_of_type(uids.Type(pool=uids.secret, name='test_wo_proxy_old'))
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user)

        simple.process_payment(service, user=user, orders=orders,
                               paymethod=paymethod, need_postauthorize=True)

    @marks.ym_h2h_processing
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize(*Pytest.combine_set(Pytest.ParamsSet(names='test_data',
                                                                  values=[
                                                                      Data.test_data, Data.test_data_red_market]),
                                                 Pytest.ParamsSet(names='service',
                                                                  values=[
                                                                      [Services.MARKETPLACE,
                                                                       Services.NEW_MARKET,
                                                                       Services.BLUE_MARKET_PAYMENTS],
                                                                      [Services.RED_MARKET_PAYMENTS],
                                                                  ])),
                             ids=lambda test_data, service: '{} {}'.format(
                                 DataObject.ids_service(service), DataObject.ids(test_data))
                             )
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle_with_refund(self, test_data, orders_structure, service):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user=user, orders_structure=orders_structure,
                                            paymethod=paymethod, need_postauthorize=True)

            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'],
                                  basket=basket, user=user)

    @marks.simple_internal_logic
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize(*Pytest.combine_set(Pytest.ParamsSet(names='service',
                                                                  values=[Data.services,
                                                                          Data.services_currency]),
                                                 Pytest.ParamsSet(names='currency',
                                                                  values=[['RUB'],
                                                                          ['USD', 'EUR', 'RUB'],
                                                                          ])),
                             ids=lambda service, currency: '{}-{}'.format(
                                 DataObject.ids_service(service), ids_currency(currency))
                             )
    def test_subsidy_payments(self, orders_structure, service, currency):
        paymethod = Subsidy()
        user = uids.get_random_of_type(uids.Types.random_from_all)

        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user=user, orders_structure=orders_structure,
                                            paymethod=paymethod, currency=currency)
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])

            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'],
                                  basket=basket, user=user)

    @marks.simple_internal_logic
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('service', [Services.NEW_MARKET, Services.RED_MARKET_PAYMENTS], ids=DataObject.ids_service)
    def test_cash_payments(self, orders_structure, service):
        paymethod = Cash()
        user = uids.get_random_of_type(uids.Types.random_from_all)

        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user=user, orders_structure=orders_structure,
                                            paymethod=paymethod)

            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])

            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'],
                                  basket=basket, user=user)

    @marks.ym_h2h_processing
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('length', [16, 17, 18, 19], ids=DataObject.ids_card_length)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_card_serial_numbers(self, service, length):
        paymethod = TrustWebPage(Via.card(get_card(length=length)), in_browser=True)
        user = uids.get_random_of(uids.mimino)
        with check_mode(CheckMode.FAILED):
            simple.process_payment(service, user=user, orders_structure=Data.orders_structure[0],
                                   paymethod=paymethod, need_postauthorize=True)

    # У NEW_MARKET чекбокс сохранения карты невидимый, карта не привязывается
    @marks.ym_h2h_processing
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_no_save_card, ids=DataObject.ids)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_pay_and_doesnt_save_card(self, test_data, service):
        with reporter.step('Оплачиваем корзину и НЕ сохраняем карту при этом'):
            paymethod, user_type = test_data.paymethod, test_data.user_type
            user = uids.get_random_of_type(user_type)
            simple.process_payment(service, user=user, paymethod=paymethod, need_postauthorize=True)
        check.check_that(simple.find_card_by_masked_number(service=service, user=user,
                                                           number=get_masked_number(
                                                                paymethod.via.card['card_number'])),
                         is_(none()),
                         step=u'Проверяем что после оплаты карта не привязалась к пользователю',
                         error=u'После оплаты со снятым чекбоксом привязки карты '
                               u'карта все равно привязалась к пользователю')

    @marks.ym_h2h_processing
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('service', Data.services_with_qty, ids=DataObject.ids_service)
    @pytest.mark.parametrize('test_data', Data.test_data_orders_structure_qty, ids=DataObject.ids)
    def test_payment_with_qty(self, service, test_data):
        paymethod = Subsidy()
        user = uids.get_random_of_type(uids.Types.random_from_all)
        orders_structure = test_data.orders_structure
        qtys = []
        for order in orders_structure:
            qtys.append(str(format(Decimal(order['qty']), '.2f')))
        with check_mode(CheckMode.FAILED):
            orders = simple.form_orders_for_create(service, user, orders_structure)
            basket = simple.create_basket(service, user, paymethod_id=paymethod.id,
                                          orders=orders)
            pay_basket_resp = simple.pay_basket(service, user=user,
                                                purchase_token=basket['purchase_token'])
            check_basket_resp = simple.check_basket(service, purchase_token=basket['purchase_token'])
            for i in range(len(pay_basket_resp['orders'])):
                check.check_that(pay_basket_resp['orders'][i]['current_qty'], is_(equal_to(qtys[i])),
                                 step=u'Проверяем, что qty в PayBasket отображается верно',
                                 error=u'qty в PayBasket отображается неверно')
                check.check_that(check_basket_resp['orders'][i]['current_qty'], is_(equal_to(str(qtys[i]))),
                                 step=u'Проверяем, что qty в CheckBasket отображается верно',
                                 error=u'qty в CheckBasket отображается неверно')


if __name__ == '__main__':
    pytest.main()
