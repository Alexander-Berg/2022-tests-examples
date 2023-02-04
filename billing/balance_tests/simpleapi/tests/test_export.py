# coding: utf-8

import time

import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services, ServiceSchemaParams
from btestlib.utils import check_mode, CheckMode
from simpleapi.common.payment_methods import LinkedCard, CompensationDiscount, Card, ApplePay, \
    Coupon, Subsidy, GooglePay
from simpleapi.common.utils import current_scheme_is, DataObject
from simpleapi.data import defaults, marks
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.data.uids_pool import anonymous, get_random_of, get_random_of_type, all_, Types
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import payments_api_steps as payments_api

"""
Тестирование экспорта bs->b0
Подробное описание https://beta.wiki.yandex-team.ru/balance/simple/export/
"""

__author__ = 'fellow'


class Data(object):
    services = [
        # Services.DISK,
        # Services.STORE,
        Services.TICKETS,
        Services.MARKETPLACE
    ]
    test_data_bs = [
        # TODO sunshineguy: Добавить YandexMoney когда(если) они заработают
        DataObject(service=Services.TICKETS,
                   paymethod=CompensationDiscount(),
                   user_type=uids.Types.random_from_all),
        DataObject(service=Services.MARKETPLACE,
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all),
        DataObject(service=Services.MARKETPLACE,
                   paymethod=Card(card=get_card(),
                                  payment_mode=defaults.PaymentMode.API_PAYMENT),
                   user_type=uids.Types.random_from_all),
        marks.apple_pay(
            DataObject(service=Services.MARKETPLACE,
                       paymethod=ApplePay(bind_token=False,
                                          pass_to_supply_payment_data=True),
                       user_type=uids.Types.random_from_all)),
        marks.google_pay(
            DataObject(service=Services.TAXI,
                       paymethod=GooglePay(),
                       user_type=uids.Types.random_from_all)),
    ]
    test_data_ng = [
        DataObject(service=Services.TAXI_DONATE,
                   paymethod=Coupon(),
                   user_type=uids.Types.random_from_all),
        DataObject(service=Services.TAXI_DONATE,
                   paymethod=Subsidy(),
                   user_type=uids.Types.random_from_all)
    ]
    services_pa = [
        Services.MARKETPLACE,
        Services.TAXI
    ]
    services_anonymous = [
        Services.TICKETS
    ]
    services_terminal = [
        Services.MARKETPLACE,
    ]
    test_data_subscriptions_xmlrpc = [
        DataObject(service=Services.MUSIC,
                   descr='Normal_subscription').new(
            product=defaults.ServiceProduct.Subscription.NORMAL.copy()),
        DataObject(service=Services.MUSIC,
                   descr='Trial_subscription').new(
            product=defaults.ServiceProduct.Subscription.TRIAL.copy()),
        DataObject(service=Services.MUSIC,
                   descr='Introductory_subscription').new(
            product=defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()),
    ]
    test_data_subscriptions_rest = [
        DataObject(service=Services.DISK,
                   descr='Normal_subscription-service=DISK').new(
            create_subs=payments_api.Subscriptions.create_normal),
        DataObject(service=Services.DISK,
                   descr='Trial_subscription-service=DISK').new(
            create_subs=payments_api.Subscriptions.create_trial),
        DataObject(service=Services.QUASAR,
                   descr='Normal_subscription-service=QUASAR').new(
            create_subs=payments_api.Subscriptions.create_normal),
        DataObject(service=Services.AFISHA_MOVIEPASS,
                   descr='Normal_subscription-service=AFISHA_MOVIEPASS').new(
            create_subs=payments_api.Subscriptions.create_normal),
    ]


class ExportAfterPayment(object):
    def export_payment(self, service):
        # TODO: add currency
        query_bs = "SELECT dt, start_dt, passport_id, service_id, user_ip, resp_code, resp_desc, payment_method, " \
                   "purchase_token FROM t_payment WHERE trust_payment_id='{}'".format(self.created_payment)
        query_bo = "SELECT dt, start_dt, creator_uid AS passport_id, service_id, user_ip, resp_code, resp_desc, " \
                   "payment_method, purchase_token FROM v_payment_trust WHERE trust_payment_id='{}'" \
            .format(self.created_payment)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, service)

    def export_order(self, service):
        """
        Заказ ставится в очередь на экспорт когда прошла оплата,
        прошел рефанд или сделали поставторизацию
        """
        query_bs = "SELECT passport_id FROM t_order WHERE service_order_id='{}'".format(self.created_order)
        query_bo = "SELECT passport_id FROM t_order WHERE service_order_id='{}'".format(self.created_order)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, service)

        service_query = "SELECT service_id FROM t_order WHERE service_order_id='{}'".format(self.created_order)
        service = simple.get_service_by_id(
            db_steps.bs_or_ng_by_service(service).execute_query(service_query).get('service_id'))
        qty = 1 if service in simple.get_services_by_schema \
            (ServiceSchemaParams.TRUST_PRICE, 1) else 0
        query = "SELECT consume_qty FROM t_order WHERE service_order_id='{}'".format(self.created_order)
        check.check_bs_query(query, service=service, compare_field='consume_qty',
                             compare_value=qty, convert_to_decimal=True)

    def export_payment_order(self, service):
        """
        Таблицы PaymentOrder -> RequestOrder
        """
        # TODO: * - wrong, reduce params
        query_bs = "SELECT price AS order_sum, price, postauth_ready_dt, cancel_dt FROM t_payment_order WHERE payment_id=(" \
                   "SELECT id FROM t_payment WHERE trust_payment_id='{}')".format(self.created_payment)
        query_bo = "SELECT order_sum, price, postauth_ready_dt, cancel_dt FROM t_request_order WHERE request_id=(" \
                   "SELECT request_id FROM v_payment_trust WHERE trust_payment_id='{}')".format(self.created_payment)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, service)


def payments(request):
    """
    Create payment before tests
    """
    with reporter.step(u'Создаем заказ и корзину для проверки экспорта'):
        service, paymethod, user_type = \
            request.param.service, request.param.paymethod, request.param.user_type
        user = uids.get_random_of_type(user_type)
        request.cls.service = service
        request.cls.created_order = simple.create_service_product_and_order(service=service,
                                                                            user=user)['service_order_id']
        if service not in simple.get_services_by_schema(ServiceSchemaParams.TRUST_PRICE, 1):
            orders = [{'service_order_id': request.cls.created_order, 'price': defaults.product_price}]
        else:
            orders = [{'service_order_id': request.cls.created_order}]
        request.cls.created_payment = \
            simple.process_payment(service=service,
                                   paymethod=paymethod,
                                   user=user,
                                   orders_structure=orders,
                                   need_postauthorize=True)['trust_payment_id']
        db_steps.bs_or_ng_by_service(service).wait_export_done(trust_payment_id=request.cls.created_payment)


@pytest.fixture(scope='class', params=Data.test_data_bs, ids=DataObject.ids)
def payments_bs(request):
    payments(request)


@pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
@pytest.mark.usefixtures('payments_bs')
@reporter.feature(features.General.Export)
@reporter.story(stories.Export.AfterPayment)
class TestExportAfterPaymentBS(ExportAfterPayment):
    def test_export_payment(self):
        super(TestExportAfterPaymentBS, self).export_payment(self.service)

    def test_export_order(self):
        super(TestExportAfterPaymentBS, self).export_order(self.service)

    def test_export_payment_order(self):
        super(TestExportAfterPaymentBS, self).export_payment_order(self.service)


@pytest.fixture(scope='class', params=Data.test_data_ng, ids=DataObject.ids)
def payments_ng(request):
    payments(request)


@pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")
@pytest.mark.usefixtures('payments_ng')
@reporter.feature(features.General.Export)
@reporter.story(stories.Export.AfterPayment)
class TestExportAfterPaymentNG(ExportAfterPayment):
    def test_export_payment(self):
        super(TestExportAfterPaymentNG, self).export_payment(self.service)

    def test_export_order(self):
        super(TestExportAfterPaymentNG, self).export_order(self.service)

    def test_export_payment_order(self):
        super(TestExportAfterPaymentNG, self).export_payment_order(self.service)


@pytest.fixture(scope='class', params=Data.services_pa, ids=DataObject.ids_service)
def postauths(request):
    """
    Prepare postauth before tests
    """
    user = get_random_of(all_)
    service = request.param
    request.cls.service = service
    request.cls.created_order = simple.create_service_product_and_order(service=service,
                                                                        user=user)['service_order_id']
    request.cls.created_payment = \
        simple.process_payment(service=request.param, user=user,
                               orders_structure=[{'service_order_id': request.cls.created_order,
                                                  'price': defaults.product_price}],
                               need_postauthorize=True)[
            'trust_payment_id']

    db_steps.bs_or_ng_by_service(service).wait_postauth_export_done(trust_payment_id=request.cls.created_payment)


@pytest.mark.usefixtures('postauths')
@reporter.feature(features.General.Export)
@reporter.story(stories.Export.AfterPostauth)
class TestExportAfterPostAuthorize(object):
    def test_export_payment(self):
        query_bs_check_dt = "SELECT real_postauth_dt FROM t_payment WHERE trust_payment_id='{}'" \
            .format(self.created_payment)
        check.check_bs_query(query_bs_check_dt, self.service)
        query_bs = "SELECT dt, start_dt, passport_id, service_id, user_ip, resp_code, resp_desc, payment_method, " \
                   "purchase_token, postauth_dt FROM t_payment WHERE trust_payment_id='{}'" \
            .format(self.created_payment)
        query_bo = "SELECT dt, start_dt, creator_uid AS passport_id, service_id, user_ip, resp_code, resp_desc," \
                   "payment_method, purchase_token, postauth_dt FROM v_payment_trust " \
                   "WHERE trust_payment_id='{}'".format(self.created_payment)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, self.service)


@pytest.fixture(scope='class', params=Data.services, ids=DataObject.ids_service)
def refunds(request):
    """
    Create refund before tests
    """
    user = get_random_of(all_)
    service = request.param
    request.cls.service = request.param
    request.cls.created_order = simple.create_service_product_and_order(service=service,
                                                                        user=user)['service_order_id']
    if request.param not in simple.get_services_by_schema(ServiceSchemaParams.TRUST_PRICE, 1):
        orders = [{'service_order_id': request.cls.created_order, 'price': defaults.product_price}]
    else:
        orders = [{'service_order_id': request.cls.created_order}]
    created_payment = \
        simple.process_payment(service=service, user=user,
                               orders_structure=orders,
                               need_postauthorize=True)['trust_payment_id']

    db_steps.bs_or_ng_by_service(service=service).wait_export_done(trust_payment_id=created_payment)
    cur_time = db_steps.bs_or_ng_by_service(service=service).get_export_info(trust_payment_id=created_payment)[
        'export_dt']

    orders_for_update = [{'service_order_id': request.cls.created_order, 'action': 'clear'}]
    simple.update_basket(request.param, orders=orders_for_update, user=user,
                         trust_payment_id=created_payment)
    simple.wait_until_real_postauth(request.param, user=user,
                                    trust_payment_id=created_payment)

    orders_for_refund = [{'service_order_id': request.cls.created_order, 'delta_amount': '3'}]
    request.cls.created_refund = simple.process_refund(service=service, user=user,
                                                       orders=orders_for_refund,
                                                       trust_payment_id=created_payment)
    db_steps.bs_or_ng_by_service(service=service).wait_export_done(trust_payment_id=created_payment,
                                                                   export_time=cur_time)


@pytest.mark.usefixtures('refunds')
@reporter.feature(features.General.Export)
@reporter.story(stories.Export.AfterRefund)
class TestExportAfterRefund(object):
    def test_export_payment(self):
        query_bs = "SELECT -amount as amount, -quantity as quantity FROM t_payment_order WHERE payment_id=(" \
                   "SELECT id FROM t_payment WHERE trust_refund_id='{}')".format(self.created_refund)
        query_bo = "SELECT order_sum, quantity FROM t_request_order WHERE request_id=(" \
                   "SELECT request_id FROM v_payment_refund WHERE orig_payment_id=(" \
                   "SELECT orig_payment_id FROM t_refund WHERE trust_refund_id='{}'))".format(self.created_refund)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, self.service)

    def test_export_order(self):
        query = "SELECT consume_qty FROM t_order WHERE service_order_id='{}'".format(self.created_order)
        check.check_bs_query(query, service=self.service, compare_field='consume_qty',
                             compare_value=0, convert_to_decimal=True)


@pytest.fixture(scope='class', params=Data.services_anonymous, ids=DataObject.ids_service)
def basket_two_orders(request):
    user = anonymous

    service = request.param
    request.cls.service = service

    orders_structure = defaults.Order.structure_rub_two_orders
    orders = simple.form_orders_for_create(service, user, orders_structure)

    request.cls.created_payment = \
        simple.process_payment(service=service, user=user, orders=orders,
                               need_postauthorize=True)['trust_payment_id']
    request.cls.created_orders = (order['service_order_id'] for order in orders)

    db_steps.bs_or_ng_by_service(service).wait_export_done(trust_payment_id=request.cls.created_payment)


@pytest.mark.usefixtures('basket_two_orders')
@reporter.feature(features.General.Export)
@reporter.story(stories.Export.AfterPayment)
class TestExportAnonymousBasketTwoOrders(object):
    def test_export_payment(self):
        # TODO: add currency
        query_bs = "SELECT dt, start_dt, passport_id, service_id, user_ip, resp_code, resp_desc, payment_method, " \
                   "purchase_token FROM t_payment WHERE trust_payment_id='{}'".format(self.created_payment)
        query_bo = "SELECT dt, start_dt, creator_uid AS passport_id, service_id, user_ip, resp_code, resp_desc, " \
                   "payment_method, purchase_token FROM v_payment_trust WHERE trust_payment_id='{}'" \
            .format(self.created_payment)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, self.service)

    def test_export_order(self):
        """
        Заказ ставится в очередь на экспорт когда прошла оплата,
        прошел рефанд или сделали поставторизацию
        """
        for created_order in self.created_orders:
            query_bs = "SELECT passport_id FROM t_order WHERE service_order_id='{}'".format(created_order)
            query_bo = "SELECT passport_id FROM t_order WHERE service_order_id='{}'".format(created_order)
            check.queries_bo_bs_got_same_data(query_bo, query_bs, self.service)

            service_query = "SELECT service_id FROM t_order WHERE service_order_id='{}'".format(created_order)
            service = simple.get_service_by_id(db_steps.bs().execute_query(service_query).get('service_id'))
            qty = 1 if service in simple.get_services_by_schema \
                (ServiceSchemaParams.TRUST_PRICE, 1) else 0

            query = "SELECT consume_qty FROM t_order WHERE service_order_id='{}'".format(created_order)
            check.check_bs_query(query, service=self.service, compare_field='consume_qty',
                                 compare_value=qty, convert_to_decimal=True)

    def test_export_payment_order(self):
        """
        Таблицы PaymentOrder -> RequestOrder
        """
        # TODO: * - wrong, reduce params
        query_bs = "SELECT price AS order_sum, price, postauth_ready_dt, cancel_dt FROM t_payment_order WHERE payment_id=(" \
                   "SELECT id FROM t_payment WHERE trust_payment_id='{}')".format(self.created_payment)
        query_bo = "SELECT order_sum, price, postauth_ready_dt, cancel_dt FROM t_request_order WHERE request_id=(" \
                   "SELECT request_id FROM v_payment_trust WHERE trust_payment_id='{}')".format(self.created_payment)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, self.service)


@pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")
@reporter.feature(features.General.Export)
@reporter.story(stories.Export.Subscription)
class TestExportSubscriptions(object):
    @staticmethod
    def check_records_in_databases(param_name, param_value, service):
        query_bs = "SELECT dt, start_dt, passport_id, service_id, user_ip, " \
                   "resp_code, resp_desc, payment_method, purchase_token, amount " \
                   "FROM t_payment " \
                   "WHERE {} ='{}'".format(param_name, param_value)
        query_bo = "SELECT dt, start_dt, creator_uid AS passport_id, service_id, user_ip, resp_code, resp_desc, " \
                   "payment_method, purchase_token, amount " \
                   "FROM v_payment_trust " \
                   "WHERE {} ='{}'".format(param_name, param_value)
        check.queries_bo_bs_got_same_data(query_bo, query_bs, service)

    @staticmethod
    @pytest.fixture
    def create_and_stop_subscription_finally(request):
        def create_subs_order(user, product, developer_payload=None, service=Services.MUSIC):
            orders = simple.form_orders_for_create(service, user,
                                                   service_product_type=product,
                                                   developer_payload=developer_payload)

            def fin():
                for order in orders:
                    simple_bo.stop_subscription(service, service_order_id=order['service_order_id'], user=user)

            request.addfinalizer(fin)
            return orders

        return create_subs_order

    @pytest.mark.parametrize('test_data', Data.test_data_subscriptions_xmlrpc, ids=DataObject.ids)
    def test_export_subscription_period_xmlrpc(self, create_and_stop_subscription_finally, test_data):
        """
        По мотивам TRUST-3497
        Триальные платежи по музыке в таблицу ложатся с amount=0 и он 0 как  None в bo записывал (с) @buivich
        """
        paymethod = LinkedCard(card=get_card())
        user_type = Types.random_from_all
        product, service = test_data.product, test_data.service
        user = get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, service=service)
        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)
        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])
        self.check_records_in_databases('trust_payment_id', basket['trust_payment_id'], service)

    @pytest.mark.parametrize('test_data', Data.test_data_subscriptions_rest, ids=DataObject.ids)
    def test_export_subscription_period_rest(self, test_data):
        paymethod = LinkedCard(card=get_card())
        user_type = Types.random_from_all
        service = test_data.service
        user = get_random_of_type(user_type)
        with test_data.create_subs(service, user) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUR', 'region_id': '225', "qty": 1}]
            with check_mode(CheckMode.FAILED):
                basket = payments_api.Payments.process(service, paymethod, user=user, orders=orders)
                payments_api.Wait.until_trial_subscription_continuation(service, user, subs['order_id'])
        self.check_records_in_databases('purchase_token', basket['purchase_token'], service)


@pytest.fixture(scope='class', params=Data.services_terminal, ids=DataObject.ids_service)
def terminal(request):
    """
    terminal creates after service product with shop params is created
    """

    shop_params = defaults.Marketplace.shop_params_test
    service_product_id = simple.get_service_product_id(request.param)
    _, request.cls.partner_id = simple.create_partner(request.param)

    simple.create_service_product(request.param, service_product_id,
                                  partner_id=request.cls.partner_id,
                                  shop_params=shop_params)

    request.cls.service_product_id = \
        db_steps.bs().get_product_by_external_id(service_product_id, request.param).get('id')
    time.sleep(5 * 60)


@pytest.mark.usefixtures('terminal')
@reporter.feature(features.General.Export)
class TestExportTerminal(object):
    @reporter.story(stories.Export.Terminal)
    def test_export_terminal_to_bo(self):
        query_bs = "SELECT dt, processing_id, payment_method_id, currency, firm_id, pri_id, sec_id, aux_id " \
                   "FROM t_terminal WHERE partner_id={} AND service_product_id={}" \
            .format(self.partner_id, self.service_product_id)
        query_bo = query_bs
        check.queries_bo_bs_got_same_data(query_bo, query_bs, service=Services.MARKETPLACE)


if __name__ == '__main__':
    pytest.main()
