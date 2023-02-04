# coding=utf-8
import pytest
from hamcrest import equal_to, is_, not_none, is_in

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import TrustWebPage, Via, ApplePay, \
    LinkedCard, GooglePay
from simpleapi.common.utils import DataObject
from simpleapi.common.utils import current_scheme_is
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, Sberbank
from simpleapi.matchers.deep_equals import deep_contains, deep_equals_to
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import expected_steps
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps.fiscal_page_steps import FiscalPageTakeData

__author__ = 'slppls'

PAYMENTS_COUNT = 2


def get_price_after_discount(price):
    return float(price - price * 0.1)


def check_fiscal_payment(service, user, basket,
                         price=defaults.Order.price, tax_type=defaults.Fiscal.NDS.nds_none):
    fiscal_info = payments_api.Payments.receipts_payment(service, user, basket['purchase_token'])
    receipt_content = fiscal_info['receipt_content']
    for row in receipt_content['rows']:
        # когда на тесте включена заглушка для касс, то во всех чеках пробивается фиксированная сумма 10.00
        check.check_that(row['price'], is_in(("%.2f" % price, '10.00')),
                         step=u'Проверяем соответствие цены в чеке и платеже',
                         error=u'Некорректная цена в чеке')
        check.check_that(row['tax_type'], is_(equal_to(tax_type)),
                         step=u'Проверяем соответствие ндс в чеке и платеже',
                         error=u'Некорректный ндс в чеке')
    for payment in receipt_content['payments']:
        check.check_that(payment['payment_type'], is_(equal_to('card')),
                         step=u'Проверяем, что в чеке пробивается карточный платеж',
                         error=u'Некорректный тип оплаты в чеке')

    """
    TRUST-3635

    То что бьётся в даркспирите.
    В принципе напрямую это не к этой таске, но по сути проверяется тем, что проверяется пробивка в даркспирите.
    А пробивка в даркспирите проверяется тем, что в t_fiscal_receipt появляется retrieve_uri
    (с) @sage
    """
    if current_scheme_is('BS'):
        ds_retrieve_uri = db_steps.bs_or_ng_by_service(service).\
            get_darkspirit_retrieve_uri(purchase_token=basket.get('purchase_token'))
        check.check_that(ds_retrieve_uri, is_(not_none()),
                         step=u'Проверяем что чек пробился в даркспирите',
                         error=u'Чек не пробился в даркспирите')


def check_fiscal_refund(service, user, basket, trust_refund_id,
                        price=defaults.Order.price, tax_type=defaults.Fiscal.NDS.nds_none):
    # refund == reversal in fiscal logic.
    fiscal_info = payments_api.Payments.receipts_refund(service, user, basket['purchase_token'], trust_refund_id)
    # fiscal_info = payments_api.Payments.receipts_refund(service, user, basket['trust_payment_id'], trust_refund_id)
    receipt_content = fiscal_info['receipt_content']
    for row in receipt_content['rows']:
        # когда на тесте включена заглушка для касс, то во всех чеках пробивается фиксированная сумма 10.00
        check.check_that(row['price'], is_in(("%.2f" % price, '10.00')),
                         step=u'Проверяем соответствие цены в чеке и платеже',
                         error=u'Некорректная цена в чеке')
        check.check_that(row['tax_type'], is_(equal_to(tax_type)),
                         step=u'Проверяем соответствие ндс в чеке и платеже',
                         error=u'Некорректный ндс в чеке')
    check.check_that(receipt_content['receipt_type'], is_(equal_to('return_income')),
                     step=u'Проверяем, что в чеке пробивается возврат',
                     error=u'Некорректный тип чека')


def taxi_refund_payment(order_structure, paymethod, payments_count, user_type):
    service = Services.TAXI
    user = uids.get_random_of_type(user_type)
    orders = simple.form_orders_for_create(service, user, orders_structure=order_structure)
    payments = []
    for _ in range(payments_count):
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user=user,
                                            orders=orders,
                                            paymethod=paymethod,
                                            need_postauthorize=True)
            payments.append(basket['trust_payment_id'])
    # рефандим все платежи
    for payment in payments:
        basket = simple.check_basket(service, user=user,
                                     trust_payment_id=payment)
        simple.process_refund(service,
                              trust_payment_id=basket['trust_payment_id'],
                              basket=basket, user=user)
    return simple.check_basket(service, user=user,
                               trust_payment_id=payment)


def taxi_reversal_payment(order_structure, paymethod, payments_count, user_type):
    service = Services.TAXI
    user = uids.get_random_of_type(user_type)
    orders = simple.form_orders_for_create(service, user,
                                           orders_structure=order_structure)
    payments = []
    for _ in range(payments_count):
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user=user,
                                            orders=orders,
                                            paymethod=paymethod)
            payments.append(basket['trust_payment_id'])
    for payment in payments:
        orders_for_update = simple.form_orders_for_update(orders,
                                                          default_action='cancel')
        simple.update_basket(service, orders=orders_for_update,
                             user=user,
                             trust_payment_id=payment)
    return simple.wait_until_real_postauth(service, user=user,
                                           trust_payment_id=payment)


class Data(object):
    BASE_DATA = DataObject(service=Services.TICKETS, paymethod=TrustWebPage(Via.card(get_card())),
                           user_type=uids.Types.random_from_all,
                           orders_structure=defaults.Order.structure_rub_one_order)
    test_data_services = [
        # params pack for service tests
        BASE_DATA.new(service=Services.TICKETS),
        BASE_DATA.new(service=Services.EVENTS_TICKETS),
        BASE_DATA.new(service=Services.EVENTS_TICKETS_NEW),
        BASE_DATA.new(service=Services.NEW_MARKET),
        BASE_DATA.new(service=Services.TAXI, paymethod=LinkedCard(card=get_card())),
        BASE_DATA.new(service=Services.TAXI, paymethod=LinkedCard(card=get_card()),
                      user_type=uids.Types.random_from_phonishes),

        # params pack for orders tests
        BASE_DATA.new(orders_structure=defaults.Order.structure_rub_two_orders),

        # params pack for paymethod tests
        BASE_DATA.new(paymethod=TrustWebPage(Via.linked_card(get_card()))),
        marks.apple_pay(
            BASE_DATA.new(paymethod=ApplePay())),
        marks.google_pay(
            BASE_DATA.new(service=Services.TAXI, paymethod=GooglePay())),
    ]
    test_data_part_reversal = [
        BASE_DATA.new(orders_structure=defaults.Order.structure_rub_two_orders,
                      clearing_plan=[{'action': 'clear'}, {'action': 'cancel'}])
    ]
    test_data_disk = [
        BASE_DATA.new(service=Services.DISK, region_id=225, currency='RUB'),
    ]
    test_data_restapi = [
        BASE_DATA.new(service=Services.MEDICINE_PAY,
                      paymethod=TrustWebPage(Via.linked_card(get_card(),
                                                             list_payment_methods_callback=payments_api.PaymentMethods.get))),
        BASE_DATA.new(service=Services.DRIVE,
                      paymethod=LinkedCard(card=Sberbank.Success.Without3DS.card_visa,
                                           list_payment_methods_callback=payments_api.PaymentMethods.get)),
        BASE_DATA.new(service=Services.QUASAR,
                      paymethod=LinkedCard(card=get_card(),
                                           list_payment_methods_callback=payments_api.PaymentMethods.get)),
    ]
    test_data_discount = [
        BASE_DATA.new(orders_structure=defaults.Order.structure_rub_fiscal)
    ]
    test_data_music = [
        BASE_DATA.new(service=Services.MUSIC, paymethod=LinkedCard(card=get_card()))
    ]
    test_data_receipt_url_and_email = [
        BASE_DATA.new(service=Services.MEDICINE_PAY),
        BASE_DATA.new(service=Services.BUSES),
    ]
    test_taxi_fiscal_data = [
        DataObject(paymethod=LinkedCard(card=get_card()), descr='refund_payment',
                   user_type=uids.Types.random_from_test_passport).new(payment=taxi_refund_payment),
        DataObject(paymethod=LinkedCard(card=get_card()), descr='reversal_payment',
                   user_type=uids.Types.random_from_test_passport).new(payment=taxi_reversal_payment),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


# @pytest.mark.no_parallel
@reporter.feature(features.General.Fiscal)
class TestFiscals(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_services, ids=DataObject.ids)
    def test_base_payment_cycle(self, test_data):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)

        orders = simple.form_orders_for_create(service, user, orders_structure)
        orders = simple.form_fiscal_in_orders(orders)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            with_fiscal=True)
            check_fiscal_payment(service, user, basket)

    @reporter.story(stories.General.Payment)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', [Data.BASE_DATA], ids=DataObject.ids)
    def test_payment_cycle_with_refund(self, test_data):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)

        orders = simple.form_orders_for_create(service, user, orders_structure)
        orders = simple.form_fiscal_in_orders(orders)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            with_fiscal=True)
            simple.process_postauthorize(service, user, basket['trust_payment_id'], orders)
            trust_refund_id = simple.process_refund(service, trust_payment_id=basket['trust_payment_id'],
                                                    basket=basket, user=user)
            check_fiscal_refund(service, user, basket, trust_refund_id)

    @reporter.story(stories.General.Payment)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', [Data.BASE_DATA], ids=DataObject.ids)
    def test_payment_cycle_with_reversal(self, test_data):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)

        orders = simple.form_orders_for_create(service, user, orders_structure)
        orders = simple.form_fiscal_in_orders(orders)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            with_fiscal=True)

            orders_for_update = simple.form_orders_for_update(orders, default_action='cancel')
            new_basket = simple.process_postauthorize(service, user, basket['trust_payment_id'],
                                                      orders_for_update=orders_for_update)
            check_fiscal_refund(service, user, new_basket, new_basket['reversal_id'])

    @reporter.story(stories.General.Payment)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_part_reversal, ids=DataObject.ids)
    def test_payment_cycle_with_part_reversal(self, test_data):
        service, paymethod, user_type, orders_structure, clearing_plan = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure, \
            test_data.clearing_plan
        user = uids.get_random_of_type(user_type)

        orders = simple.form_orders_for_create(service, user, orders_structure)
        orders = simple.form_fiscal_in_orders(orders)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            with_fiscal=True)

            orders_for_update = simple.form_orders_for_update(orders, clearing_plan)
            new_basket = simple.process_postauthorize(service, user, basket['trust_payment_id'],
                                                      orders_for_update=orders_for_update)
            check_fiscal_refund(service, user, new_basket, new_basket['reversal_id'])

    @reporter.story(stories.General.Payment)
    @pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_disk, ids=DataObject.ids)
    def test_disk_payment_cycle(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        region_id, currency = test_data.region_id, test_data.currency
        user = uids.get_random_of_type(user_type)
        paymethod.init(service=service, user=user, region_id=region_id)
        with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
            orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
            with check_mode(CheckMode.FAILED):
                basket = payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency,
                                                       region_id=region_id)
                payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])
                check_fiscal_payment(service, user, basket, price=10)
                purchase_token = basket['purchase_token']
                orders_for_refund = payments_api.Form.orders_for_refund(
                    payments_api.Payments.get(service, user, purchase_token))
                trust_refund_id = payments_api.Refunds.create(service, user, purchase_token,
                                                              orders_for_refund)['trust_refund_id']
                payments_api.Refunds.start(service, user, trust_refund_id)
                payments_api.Wait.until_refund_done(service, user, purchase_token,
                                                    trust_refund_id)
                check_fiscal_refund(service, user, basket, trust_refund_id, price=10)

    @reporter.story(stories.General.Payment)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_restapi, ids=DataObject.ids)
    def test_base_rest_payment_cycle(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders_structure = [
            {'region_id': 225, 'currency': 'RUB', 'price': 10},
            {'region_id': 225, 'currency': 'RUB', 'price': 10},
        ]

        with check_mode(CheckMode.FAILED):
            orders = payments_api.Form.orders_for_payment(service=service, user=user,
                                                          orders_structure=orders_structure, with_fiscal=True)
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders=orders, with_fiscal=True)
            check_fiscal_payment(service, user, basket, price=10)

    @reporter.story(stories.General.Discount)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_discount, ids=DataObject.ids)
    def test_discount(self, test_data):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)
        expected_price = get_price_after_discount(orders_structure[0]['price'])

        orders = simple.form_orders_for_create(service, user, orders_structure)
        orders = simple.form_fiscal_in_orders(orders)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            discounts=[defaults.Discounts.id100['id']], with_fiscal=True)
            check_fiscal_payment(service, user, basket, price=expected_price)
            simple.process_postauthorize(service, user, basket['trust_payment_id'], orders)
            trust_refund_id = simple.process_refund(service, trust_payment_id=basket['trust_payment_id'],
                                                    basket=basket, user=user)
            check_fiscal_refund(service, user, basket, trust_refund_id, price=expected_price)

    @reporter.story(stories.General.Promocodes)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', [Data.BASE_DATA], ids=DataObject.ids)
    def test_promocode(self, test_data):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)

        promocode_id = simple.process_promocode_creating(service)
        expected_price = orders_structure[0]['price'] - defaults.Promocode.promocode_amount_part

        orders = simple.form_orders_for_create(service, user)
        orders = simple.form_fiscal_in_orders(orders)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id, with_fiscal=True)
            check_fiscal_payment(service, user, basket, price=expected_price)

    @reporter.story(stories.General.Promocodes)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', [Data.BASE_DATA], ids=DataObject.ids)
    def test_discount_with_promo(self, test_data):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)

        promocode_id = simple.process_promocode_creating(service)
        expected_price = get_price_after_discount(
            orders_structure[0]['price'] - defaults.Promocode.promocode_amount_part)

        orders = simple.form_orders_for_create(service, user)
        orders = simple.form_fiscal_in_orders(orders)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id, discounts=[defaults.Discounts.id100['id'], ],
                                            with_fiscal=True)
            check_fiscal_payment(service, user, basket, price=expected_price)

    @pytest.fixture
    def create_orders_and_stop_subs(self, request):
        def create_subs_order(service, user, product, orders_structure):
            orders = simple.form_orders_for_create(service, user, orders_structure,
                                                   service_product_type=product,
                                                   fiscal_nds=defaults.Fiscal.NDS.nds_none,
                                                   fiscal_title=defaults.Fiscal.fiscal_title)

            def fin():
                for order in orders:
                    simple_bo.stop_subscription(service, service_order_id=order['service_order_id'], user=user)

            request.addfinalizer(fin)
            return orders

        return create_subs_order

    @reporter.story(stories.General.Subscription)
    @pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_music, ids=DataObject.ids)
    def test_music(self, test_data, create_orders_and_stop_subs):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)
        expected_price = 10

        product = defaults.ServiceProduct.Subscription.NORMAL.copy()
        orders = create_orders_and_stop_subs(service, user, product, orders_structure)
        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])
        basket = simple.wait_until_fiscal_done(service, user=user, purchase_token=basket['purchase_token'])
        for purchase_token in basket['orders'][0]['payments']:
            fiscal_basket = simple.check_basket(service, user=user, purchase_token=purchase_token)
            check_fiscal_payment(service, user, fiscal_basket, price=expected_price)

    @reporter.story(stories.General.Rules)
    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_receipt_url_and_email, ids=DataObject.ids)
    def test_receipt_contains_url_and_email(self, test_data):
        service, paymethod, user_type, orders_structure = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.orders_structure
        user = uids.get_random_of_type(user_type)

        expected_receipt_info = db_steps.bs().get_service_receipt_info(service)

        orders = payments_api.Form.orders_for_payment(service=service, user=user,
                                                      orders_structure=orders_structure, with_fiscal=True)
        basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                               orders=orders, with_fiscal=True)

        fiscal_info = payments_api.Payments.receipts_payment(service, user, basket['purchase_token'])
        check.check_that(fiscal_info['receipt_calculated_content'],
                         deep_contains(expected.Fiscal.receipt_with_url_and_email(email=expected_receipt_info['email'],
                                                                                  url=expected_receipt_info['url'])),
                         step=u'Проверяем, что чек содержит информацию о firm_url и firm_email',
                         error=u'Чек не содержит информацию о firm_url и firm_email')
        db_steps.bs().wait_receipt_mail_is_send(basket['purchase_token'])


@pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
@pytest.mark.parametrize('test_data', Data.test_taxi_fiscal_data, ids=DataObject.ids)
@pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
@reporter.feature(features.General.Fiscal)
class TestTaxiFiscals(object):
    @reporter.story(stories.General.Payment)
    def test_fiscal_info_check(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        basket = test_data.payment(orders_structure, paymethod, PAYMENTS_COUNT, user_type)
        servive_order_id = basket['orders'][0]['service_order_id']
        length_orders = len(basket['orders'])
        fiscal_ifno = FiscalPageTakeData(servive_order_id)
        # PAYMENTS_COUNT * 2 + 1 - обусловленно: PAYMENTS_COUNT - кол-во платежей х2 - возвраты
        # +1 - range режет верхнюю границу
        for fiscal_number in range(1, PAYMENTS_COUNT * 2 + 1):
            # Порядок следования чеков зависит от метода совершения платежа,
            # если сделать оплату последовательной т.е. pay->refund/reversal->next_pay->next_refund/reversal
            # то чеки тоже будут размещенны последовательно
            check_name = u'Возврат прихода' if fiscal_number > 2 else u'Приход'
            check.check_that(fiscal_ifno.take_fiscal_basic_data(fiscal_number),
                             deep_equals_to(expected_steps.FiscalTaxi.fiscal_basic_data(check_name)),
                             step=u'Проверяем, что заполнение базовых полей чека'
                                  u' соответствует шаблону',
                             error=u'Заполнение базовых полей чека'
                                   u' не соответствует шаблону!')
            check.check_that(fiscal_ifno.take_fiscal_total_field(fiscal_number),
                             deep_equals_to(expected_steps.FiscalTaxi.fiscal_total_field()),
                             step=u'Проверяем, что наименование итоговых и ' \
                                  u'информационных полей соответствует шаблону',
                             error=u'Наименование итоговых и информационных'
                                   u'полей не соответствует шаблону!')
            check.check_that(fiscal_ifno.take_fiscal_total_data(fiscal_number),
                             deep_equals_to(
                                 expected_steps.FiscalTaxi.fiscal_total_data(basket['amount'],
                                                                             unicode(basket['user_email']))),
                             step=u'Проверяем, что данные в итоговых и '
                                  u'информационных полях чека соответствуют шаблону',
                             error=u'Данные в итоговых и информационных'
                                   u'полях чека не соответствует шаблону!')
            for order_num in range(1, length_orders + 1):
                check.check_that(fiscal_ifno.take_fiscal_order_field(fiscal_number, order_num),
                                 deep_equals_to(
                                     expected_steps.FiscalTaxi.fiscal_order_field()),
                                 step=u'Проверяем, что наименование полей ордера '
                                      u'соответствует шаблону',
                                 error=u'Наименование полей ордера'
                                       u' не соответствует шаблону!')
                orig_amount = basket['orders'][order_num - 1]['orig_amount']
                fiscal_title = basket['orders'][order_num - 1]['fiscal_title']
                check.check_that(fiscal_ifno.take_fiscal_order_data(fiscal_number, order_num),
                                 deep_equals_to(expected_steps.FiscalTaxi.fiscal_order_data(order_num, orig_amount,
                                                                                            fiscal_title)),
                                 step=u'Проверяем, что данные в полях ордера соответствуют шаблону',
                                 error=u'Данные в полях ордера не соответствуют шаблону!')