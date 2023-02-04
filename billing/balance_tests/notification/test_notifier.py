# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import mock
import pytest
import httpretty
import hamcrest as hm
from itertools import chain

from butils import logger
from medium.medium_servant import MediumXmlRpcInvoker
from balance import core, mapper
from balance import constants as cst
from notifier import data_objects as notifier_objects
from balance.constants import *
from balance.actions.process_completions import ProcessCompletions
import balance.muzzle_util as ut
from balance.utils.notify import Message
from balance.utils.xmlrpc_proxy import MockServerProxy
from balance.actions.cashback.utils import round_bonus

from tests.base import BalanceTest
import tests.object_builder as ob
from tests.balance_tests.pay_policy.pay_policy_common import create_pay_policy

log = logger.get_logger()

ACT_MONTH = mapper.ActMonth()


class TestNotifyMarketRefund(object):
    @pytest.fixture
    def payment(self, session):
        payment = ob.TrustPaymentBuilder.construct(session, service_id=ServiceId.BLUE_REF)
        payment.notify_url = 'https://yandex.ru'
        return payment

    def test_ok(self, session, payment, tvm_client_mock):
        ob.FiscalReceiptBuilder.construct(session, payment=payment)
        message = Message(payment.id, NOTIFY_MARKET_REFUND_OPCODE, timestamp=None)
        with mock.patch('notifier.data_objects.http_post') as http_post_mock:
            http_post_mock.return_value = (200,)
            assert notifier_objects.BaseInfo.process(session, message) == 1

    def test_bad_response(self, session, payment, tvm_client_mock):
        ob.FiscalReceiptBuilder.construct(session, payment=payment)
        message = Message(payment.id, NOTIFY_MARKET_REFUND_OPCODE, timestamp=None)
        with mock.patch('notifier.data_objects.http_post') as http_post_mock, \
            pytest.raises(notifier_objects.BadServiceResponseEx):
            http_post_mock.return_value = (404,)
            notifier_objects.BaseInfo.process(session, message)

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_no_receipts(self, session, payment):
        message = Message(payment.id, NOTIFY_MARKET_REFUND_OPCODE, timestamp=None)
        with pytest.raises(AssertionError) as exc_info:
            notifier_objects.BaseInfo.process(session, message)
        assert exc_info.value.args[0] == 'no fiscal_receipts found'

    @pytest.mark.parametrize(
        'ticket',
        (
            pytest.param(ob.generate_character_string(5), id='5char random'),
            pytest.param('abc:123-xyz_987', id='special symbols')
        )
    )
    def test_tvm(self, session, payment, tvm_client_mock, ticket):
        ob.FiscalReceiptBuilder.construct(session, payment=payment)
        message = Message(payment.id, NOTIFY_MARKET_REFUND_OPCODE, timestamp=None)
        tvm_client_mock.get_service_ticket_for.return_value = ticket

        with mock.patch('butils.post.do_http_call') as do_http_call_mock:
            do_http_call_mock.return_value = mock.MagicMock(status=200)
            assert notifier_objects.BaseInfo.process(session, message) == 1
        assert do_http_call_mock.call_args.kwargs['request'].get_header(TVM2_SERVICE_TICKET_HEADER) == ticket


class TestNotifier(BalanceTest):

    def test_market_order_notification(self):
        client = ob.ClientBuilder()
        rows = [ob.BasketItemBuilder(order=ob.OrderBuilder(
            client=client,
            service=ob.Getter(mapper.Service, 11)),
            quantity=400)
            for i in (1, 2, 3)]
        request_b = ob.RequestBuilder(basket=ob.BasketBuilder(rows=rows, client=client))
        invoice = ob.InvoiceBuilder(request=request_b,
                                    ).build(self.session).obj
        from balance.actions.invoice_turnon import InvoiceTurnOn
        InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()
        self.session.flush()

        order1 = rows[0].order.obj
        order1.calculate_consumption(shipment_info={'Bucks': 200}, dt=datetime.datetime.today())

        res_match = hm.has_entries({
            'ConsumeCurrency': 'RUB',
            'ServiceID': 11,
            'ConsumeSum': '40000',
            'Signal': 1,
            'SignalDescription': 'Order balance have been changed',
            'ConsumeAmount': '40000',
            'CompletionQty': '0',
            'ConsumeQty': '400',
        })
        info = notifier_objects.BaseInfo.get_notification_info(self.session, NOTIFY_ORDER_OPCODE, order1.id)[1]
        hm.assert_that(info['args'][0], res_match)

        order2 = ob.OrderBuilder(client=client, service=ob.Getter(mapper.Service, 11), group_order_id=order1.id).build(
            self.session).obj
        self.session.flush()
        order2.calculate_consumption(shipment_info={'Bucks': 100}, dt=datetime.datetime.today())

        from balance.actions.unified_account import UnifiedAccount

        ua = UnifiedAccount(self.session, order1)
        ua.transfer2group()

        assert order2.consume_qty == order2.completion_qty

        self.session.flush()

        res_match = hm.has_entries({
            'ConsumeQty': '400',
            'ConsumeCurrency': 'RUB',
            'ServiceID': 11,
            'ConsumeSum': '40000',
            'Signal': 1,
            'SignalDescription': 'Order balance have been changed',
            'ConsumeAmount': '40000',
            'CompletionQty': '300',
        })
        info = notifier_objects.BaseInfo.get_notification_info(self.session, NOTIFY_ORDER_OPCODE, order1.id)[1]
        hm.assert_that(info['args'][0], res_match)

        res_match = hm.has_entries({
            'ConsumeQty': '0',
            'ConsumeCurrency': 'RUB',
            'ServiceID': 11,
            'ConsumeSum': '0',
            'Signal': 1,
            'SignalDescription': 'Order balance have been changed',
            'ConsumeAmount': '10000',
            'CompletionQty': '100',
        })
        info = notifier_objects.BaseInfo.get_notification_info(self.session, NOTIFY_ORDER_OPCODE, order2.id)[1]
        arg = info['args'][0]
        hm.assert_that(arg, res_match)
        hm.assert_that(D(arg['ConsumeQty']) + D(arg['ConsumeSum']), hm.equal_to(0))


class TestOrderNotify(object):
    VALID_IFACE_VERSION = 2
    INVALID_IFACE_VERSION = 100500
    not_exists_snp_msg = 'uri for service_id=%d not found in t_service_notify_params'

    @pytest.fixture(name='cashback')
    def create_cashback(self, session, client, **kw):
        return ob.ClientCashbackBuilder.construct(
            session,
            client=client,
            **kw
        )

    @pytest.fixture
    def service(self, session):
        service = ob.ServiceBuilder().build(session).obj
        create_pay_policy(
            session, firm_id=1, region_id=225, service_id=service.id,
            paymethods_params=[('USD', 1001)]
        )
        return service

    @pytest.fixture(name='order')
    def create_order(self, session, service, product_id=None):
        return ob.OrderBuilder.construct(session, service=service, product_id=product_id)

    @staticmethod
    def consume_order(session, order, quantity=10):
        basket = ob.BasketBuilder(
            client=order.client,
            rows=[ob.BasketItemBuilder(order=order, quantity=quantity)]
        )
        request = ob.RequestBuilder(basket=basket)
        invoice = ob.InvoiceBuilder(request=request).build(session).obj
        invoice.create_receipt(int(invoice.total_sum))
        invoice.turn_on_rows(cut_agava=True)
        pc = ProcessCompletions(order)
        pc.process_completions(qty=int(order.consume_qty) // 2)

    @pytest.mark.parametrize(
        'create, builder_params, expected_error_message',
        [
            pytest.param(False, {}, not_exists_snp_msg, id='no_params'),
            pytest.param(True, {'test_url': None}, not_exists_snp_msg, id='no_url'),
            pytest.param(True, {'hidden': True}, not_exists_snp_msg, id='hidden_params'),
            pytest.param(True, {'iface_version': INVALID_IFACE_VERSION},
                         'Unsupported interface version in t_service_notify_params for service=%s',
                         id='invalid_iface_version'),
        ],
    )
    def test_skip_notification(self, session, service, order, create, builder_params, expected_error_message):
        if create:
            ob.ServiceNotifyParamsBuilder.construct(
                session, service=service, **builder_params
            )

        with pytest.raises(notifier_objects.SkipNotification) as e:
            notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)
        expected_error_message %= service.id
        assert e.value.message == expected_error_message

    def test_service_2(self, session, order):
        self.consume_order(session, order)
        snp = ob.ServiceNotifyParamsBuilder(
            service=order.service,
            iface_version=2,
        ).build(session).obj
        status, body = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)
        assert status == 0
        hm.assert_that(body, hm.has_entries({
            'path': 'BalanceClient.NotifyOrder'.split('.'),
            'url': snp.test_url,
            'protocol': snp.protocol,
            'kwargs': {},
            'args': hm.all_of(hm.has_length(7), hm.contains(
                order.service_id,
                order.service_order_id,
                hm.anything('Tid or scn'),
                1,  # Signal
                'Order balance have been changed',  # SignalDescription
                ut.decimal_to_str(order.consume_sum),
                ut.decimal_to_str(order.consume_qty)
            ))
        }))

    def test_service_3(self, session, order):
        self.consume_order(session, order)
        snp = ob.ServiceNotifyParamsBuilder(
            service=order.service,
            iface_version=3,
        ).build(session).obj
        status, body = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)
        assert status == 0
        hm.assert_that(body, hm.has_entries({
            'path': 'BalanceClient.NotifyOrder2'.split('.'),
            'url': snp.test_url,
            'protocol': snp.protocol,
            'kwargs': {},
            'args': hm.all_of(
                hm.has_length(1),
                hm.contains(hm.all_of(
                    hm.has_length(10),
                    hm.has_entries({
                        'ServiceID': order.service_id,
                        'ServiceOrderID': order.service_order_id,
                        'ConsumeQty': ut.decimal_to_str(order.consume_qty),
                        'ConsumeSum': ut.decimal_to_str(order.consume_sum),
                        'ConsumeAmount': ut.decimal_to_str(order.consume_sum),
                        'CompletionQty': ut.decimal_to_str(order.completion_qty),
                        'ConsumeCurrency': order.consumes[0].invoice.iso_currency,
                        'Tid': hm.anything(),
                        'Signal': 1,
                        'SignalDescription': 'Order balance have been changed'
                    })
                ))
            )
        }))

    @pytest.mark.parametrize('order_total_qty', [0, 1])
    def test_service_3_total_consume_qty(self, session, order, order_total_qty):
        order.service.balance_service.unified_account_type = ServiceUAType.enqueue
        order.main_order = 1
        child_order = ob.OrderBuilder(service=order.service, parent_group_order=order).build(session).obj
        self.consume_order(session, order, 10)
        self.consume_order(session, child_order, 16)

        snp = ob.ServiceNotifyParamsBuilder(
            service=order.service,
            iface_version=3,
            order_total_qty=order_total_qty,
        ).build(session).obj
        status, body = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)
        assert status == 0
        expected_args = {
            'ServiceID': order.service_id,
            'ServiceOrderID': order.service_order_id,
            'ConsumeQty': '10',
            'ConsumeSum': '1000',
            'ConsumeAmount': '1000',
            'CompletionQty': '5',
            'ConsumeCurrency': order.consumes[0].invoice.iso_currency,
            'Tid': hm.anything(),
            'Signal': 1,
            'SignalDescription': 'Order balance have been changed'
        }
        if order_total_qty:
            expected_args['TotalConsumeQty'] = '26'
        hm.assert_that(body, hm.has_entries({
            'path': 'BalanceClient.NotifyOrder2'.split('.'),
            'url': snp.test_url,
            'protocol': snp.protocol,
            'kwargs': {},
            'args': hm.all_of(
                hm.has_length(1),
                hm.contains(hm.all_of(
                    hm.has_length(len(expected_args)),
                    hm.has_entries(expected_args)
                ))
            )
        }))

    @mock.patch('notifier.data_objects.BaseInfo.get_server',
                new=mock.Mock(return_value=MockServerProxy(url=None, protocol=None)))
    def test_process_success(self, session, order):
        self.consume_order(session, order)
        ob.ServiceNotifyParamsBuilder(
            service=order.service,
            iface_version=2,
        ).build(session)
        message = Message(order.id, NOTIFY_ORDER_OPCODE, timestamp=None)
        assert notifier_objects.BaseInfo.process(session, message) == 1

    @pytest.mark.parametrize('product_id', [cst.DIRECT_PRODUCT_ID, cst.DIRECT_PRODUCT_RUB_ID])
    @pytest.mark.parametrize('w_cashback', [True, False])
    def test_cashback_qty(self, session, w_cashback, product_id):

        order = self.create_order(session,
                                  service=session.query(mapper.Service).getone(cst.ServiceId.DIRECT),
                                  product_id=product_id)
        if w_cashback:
            self.create_cashback(session, order.client, iso_currency='RUB',
                                 bonus=D('50'), service_id=cst.ServiceId.DIRECT)
        self.consume_order(session, order, 10)
        session.flush()
        status, body = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)
        assert status == 0
        if w_cashback:
            cashback_qty = round_bonus(sum([co.calc_cashback_bonus_raw(co.current_qty) for co in order.consumes]))
        else:
            cashback_qty = 0
        hm.assert_that(body, hm.has_entries({
            'path': 'BalanceClient.NotifyOrder2'.split('.'),
            'kwargs': {},
            'args': hm.all_of(
                hm.has_length(1),
                hm.contains(hm.all_of(
                    hm.has_length(12),
                    hm.has_entries({
                        'ServiceID': order.service_id,
                        'ServiceOrderID': order.service_order_id,
                        'ConsumeQty': ut.decimal_to_str(order.consume_qty),
                        'ConsumeSum': ut.decimal_to_str(order.consume_sum),
                        'ConsumeAmount': ut.decimal_to_str(order.consume_sum),
                        'CompletionQty': ut.decimal_to_str(order.completion_qty),
                        'ConsumeCurrency': order.consumes[0].invoice.iso_currency,
                        'CashbackCurrentQty': ut.decimal_to_str(cashback_qty),
                        'ProductCurrency': 'RUB' if product_id == cst.DIRECT_PRODUCT_RUB_ID else '',
                        'Tid': hm.anything(),
                        'Signal': 1,
                        'SignalDescription': 'Order balance have been changed'
                    })
                ))
            )
        }))

    @pytest.mark.parametrize('product_id, cashback_qty, total_cashback_qty, total_consume_qty', [
        [cst.DIRECT_PRODUCT_ID, D('1.666666'), D('3.333332'), D('30')],
        [cst.DIRECT_PRODUCT_RUB_ID, D('50'), D('100'), D('30')]
    ])
    @pytest.mark.parametrize('w_cashback', [
        True,
                                            False
                                            ])
    def test_total_cashback_current_qty(self, session, product_id, w_cashback, total_cashback_qty, total_consume_qty,
                                        cashback_qty):
        parent_order = self.create_order(session,
                                         service=session.query(mapper.Service).getone(cst.ServiceId.DIRECT),
                                         product_id=product_id)

        child_order = self.create_order(session,
                                        service=session.query(mapper.Service).getone(cst.ServiceId.DIRECT),
                                        product_id=product_id)
        if w_cashback:
            self.create_cashback(session, parent_order.client, iso_currency='RUB',
                                 bonus=D('50'), service_id=cst.ServiceId.DIRECT)

            self.create_cashback(session, child_order.client, iso_currency='RUB',
                                 bonus=D('50'), service_id=cst.ServiceId.DIRECT)

            total_consume_qty += total_cashback_qty

        self.consume_order(session, child_order, 5)
        self.consume_order(session, parent_order, 10)
        self.consume_order(session, parent_order, 15)

        child_order.group_order_id = parent_order.id
        parent_order.main_order = 1
        session.flush()
        status, body = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, parent_order.id)
        assert status == 0

        if w_cashback:
            cashback_qty = cashback_qty
            total_cashback_qty = total_cashback_qty
            total_consume_qty = total_consume_qty
        else:
            cashback_qty = 0
            total_cashback_qty = 0
            total_consume_qty = total_consume_qty
        hm.assert_that(body, hm.has_entries({
            'path': 'BalanceClient.NotifyOrder2'.split('.'),
            'kwargs': {},
            'args': hm.all_of(
                hm.has_length(1),
                hm.contains(hm.all_of(
                    hm.has_length(14),
                    hm.has_entries({
                        'ServiceID': parent_order.service_id,
                        'ServiceOrderID': parent_order.service_order_id,
                        'ConsumeQty': ut.decimal_to_str(parent_order.consume_qty),
                        'ConsumeSum': ut.decimal_to_str(parent_order.consume_sum),
                        'ConsumeAmount': ut.decimal_to_str(parent_order.consume_sum),
                        'CompletionQty': ut.decimal_to_str(parent_order.completion_qty),
                        'ConsumeCurrency': parent_order.consumes[0].invoice.iso_currency,
                        'CashbackCurrentQty': ut.decimal_to_str(cashback_qty),
                        'ProductCurrency': 'RUB' if product_id == cst.DIRECT_PRODUCT_RUB_ID else '',
                        'TotalConsumeQty': ut.decimal_to_str(total_consume_qty),
                        'TotalCashbackCurrentQty': ut.decimal_to_str(total_cashback_qty),
                        'Tid': hm.anything(),
                        'Signal': 1,
                        'SignalDescription': 'Order balance have been changed'
                    })
                ))
            )
        }))


class TestInvoiceNotifications(object):

    def test_prepayment(self, session):
        order = ob.OrderBuilder(service_id=129).build(session).obj
        client = order.client
        basket = ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=666)]
        )
        request = ob.RequestBuilder(basket=basket, firm_id=1)
        invoice = ob.InvoiceBuilder(request=request).build(session).obj
        invoice.create_receipt(123456)
        invoice.turn_on_rows()
        invoice.close_invoice(datetime.datetime.now())

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_INVOICE_OPCODE, invoice.id)[1]
        assert info['path'] == ['BalanceClient', 'NotifyInvoice']
        assert info['args'][0] == {
            'Invoice': invoice.external_id,
            'RequestID': str(invoice.request_id),
            'ClientID': str(invoice.client_id),
            'FirmID': '1',
            'Currency': 'RUB',
            'ReceiptSum': '123456.00',
            'ConsumeSum': '66600.00',
            'ActSum': '66600.00',
        }

    def test_personal_account(self, session):
        client = ob.ClientBuilder().build(session).obj
        contract = ob.ContractBuilder(
            dt=datetime.datetime.now() - datetime.timedelta(days=66),
            client=client,
            person=ob.PersonBuilder(client=client),
            commission=1,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            payment_term_max=60,
            personal_account=1,
            currency=810,
            lift_credit_on_payment=1,
            commission_type=57,
            repayment_on_consume=1,
            credit_limit_single=1666666,
            services={129},
            is_signed=datetime.datetime.now(),
            firm=1,
        ).build(session).obj
        session.flush()

        order = ob.OrderBuilder(client=client, service_id=129).build(session).obj
        basket = ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=666)]
        )
        request = ob.RequestBuilder(basket=basket).build(session).obj
        pa, = core.Core(session).pay_on_credit(request.id, 1003, contract.person_id, contract.id)
        pa.create_receipt(666)
        pa.close_invoice(datetime.datetime.now())

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_INVOICE_OPCODE, pa.id)[1]
        assert info['path'] == ['BalanceClient', 'NotifyInvoice']
        assert info['args'][0] == {
            'Invoice': pa.external_id,
            'ClientID': str(pa.client_id),
            'FirmID': '1',
            'Contract': contract.external_id,
            'Currency': 'RUB',
            'ReceiptSum': '666.00',
            'ConsumeSum': '66600.00',
            'ActSum': '66600.00',
        }


@pytest.mark.auto_overdraft
class TestNotifyOverdraftLimits(object):
    @pytest.fixture(autouse=True)
    def patch_config(request, session):
        session.config.__dict__['USE_SERVICE_LIMIT_WO_TAX'] = getattr(request, 'param', 1)

    @staticmethod
    def get_person(session, params=None):
        if params is None:
            params = {}
        firm_id = params.get('firm_id', cst.FirmId.YANDEX_OOO)
        service_id = params.get('service_id', cst.ServiceId.DIRECT)
        iso_currency = params.get('iso_currency', 'RUB')
        overdraft_limit = params.get('overdraft_limit', 10000)
        limit_wo_tax = params.get('limit_wo_tax', None)

        client = ob.ClientBuilder(is_agency=False).build(session).obj
        client.set_currency(service_id, iso_currency, ACT_MONTH.begin_dt, cst.CONVERT_TYPE_COPY)
        person = ob.PersonBuilder(client=client, type=params.get('person_type', 'ph')).build(session).obj

        service = session.query(mapper.Service).getone(service_id)
        person.client.set_overdraft_limit(service, firm_id, overdraft_limit, iso_currency, limit_wo_tax)

        session.flush()
        return person

    @staticmethod
    def overdraft_params(session, person, params=None):
        if params is None:
            params = {}
        service_id = params.get('service_id', cst.ServiceId.DIRECT)
        overdraft_limit = params.get('overdraft_limit', 10000)
        payment_method_cc = params.get('payment_method_cc', 'bank')

        overdraft_params = ob.OverdraftParamsBuilder(
            client=person.client,
            person=person,
            service_id=service_id,
            payment_method_cc=payment_method_cc,
            iso_currency=person.client.currency_on(),
            client_limit=overdraft_limit,
        ).build(session).obj
        session.add(overdraft_params)
        return overdraft_params

    @staticmethod
    def create_order(session, person, params=None, main_order=None, is_main_order=True):
        if params is None:
            params = {}
        product_id = params.get('product_id', DIRECT_PRODUCT_RUB_ID)
        service_id = params.get('service_id', cst.ServiceId.DIRECT)
        if main_order:
            order = ob.OrderBuilder(
                client=person.client,
                product=ob.Getter(mapper.Product, product_id),
                service_id=service_id,
                group_order_id=main_order.id
            ).build(session).obj
        else:
            order = ob.OrderBuilder(
                client=person.client,
                product=ob.Getter(mapper.Product, product_id),
                service_id=service_id,
                main_order=is_main_order
            ).build(session).obj
        return order

    @staticmethod
    def overdraft_invoice(session, person, basket_items, params=None):
        if params is None:
            params = {}
        paysys_id = params.get('paysys_id', ob.PAYSYS_ID)
        invoice = ob.InvoiceBuilder(
            person=person,
            paysys=ob.Getter(mapper.Paysys, paysys_id),
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=person.client,
                    rows=[
                        ob.BasketItemBuilder(order=child_order, quantity=quantity)
                        for child_order, quantity in basket_items
                    ]
                )
            ),
            overdraft=True
        ).build(session).obj
        invoice.turn_on_rows(cut_agava=True)
        return invoice

    @pytest.mark.parametrize('is_auto_ov, is_main_order',
                             [
                                 (True, False),
                                 (False, True),
                             ],
                             ids=[
                                 'order not main',
                                 'not auto_ov client',
                             ]
                             )
    def test_notification_without_overdraft_info(self, session, is_auto_ov, is_main_order):
        quantity = 10

        person = self.get_person(session)

        if is_auto_ov:
            self.overdraft_params(session, person)

        order_1 = self.create_order(session, person, is_main_order=is_main_order)
        order_2 = self.create_order(session, person, main_order=order_1 if is_main_order else None,
                                    is_main_order=False)
        self.overdraft_invoice(session, person, [(order_2, quantity)])

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE,
                                                               order_1.id)[1]
        notify_params = info['args'][0]
        assert info['path'] == ['BalanceClient', 'NotifyOrder2']
        assert notify_params.get('OverdraftSpentQty', None) is None
        assert notify_params['ProductCurrency'] == 'RUB'
        assert notify_params['ServiceID'] == ServiceId.DIRECT
        assert notify_params['ServiceOrderID'] == order_1.service_order_id
        assert notify_params.get('OverdraftLimit', None) is None
        if is_main_order:
            assert notify_params['TotalConsumeQty'] == '10'
            assert notify_params['TotalCashbackCurrentQty'] == '0'
        else:
            assert notify_params['ConsumeQty'] == '0'

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE,
                                                               order_2.id)[1]
        notify_params = info['args'][0]
        assert info['path'] == ['BalanceClient', 'NotifyOrder2']
        assert notify_params.get('OverdraftSpentQty') is None
        assert notify_params['ProductCurrency'] == 'RUB'
        assert notify_params['ServiceID'] == ServiceId.DIRECT
        assert notify_params['ConsumeQty'] == '10'
        assert notify_params['ServiceOrderID'] == order_2.service_order_id
        assert notify_params.get('OverdraftLimit') is None

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE,
                                                               person.client.id)[1]
        notify_params = info['args'][0]

        assert info['path'] == ['BalanceClient', 'NotifyClient2']
        assert notify_params['ClientID'] == str(person.client_id)
        assert notify_params['ClientCurrency'] == person.client.currency_on()
        assert notify_params['OverdraftLimit'] == '10000.00'
        assert notify_params['OverdraftSpent'] == '%.2f' % quantity

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person.client.overdraft.values()[0].id)[1]
        notify_params = info['args'][0]

        assert info['path'] == ['BalanceClient', 'NotifyClient2']
        assert notify_params['ClientID'] == str(person.client_id)
        assert notify_params['ClientCurrency'] == person.client.currency_on()
        assert notify_params['OverdraftLimit'] == '10000.00'
        assert notify_params['OverdraftSpent'] == '%.2f' % quantity

    @pytest.mark.parametrize('completion_qtys, match_ov_spent_qty',
                             [
                                 ([D('0'), D('0')], D('0')),
                                 ([D('11.66'), D('34.78')], D('46.44')),
                                 ([D('45'), D('45')], D('90'))
                             ],
                             ids=[
                                 'order have not been shipped',
                                 'order have been partially shipped',
                                 'order have been completely shipped'
                             ],
                             )
    def test_rub(self, session, completion_qtys, match_ov_spent_qty):
        overdraft_limit = 900

        person = self.get_person(session, dict(overdraft_limit=overdraft_limit))
        self.overdraft_params(session, person, dict(overdraft_limit=90))

        main_order = self.create_order(session, person)
        order_1 = self.create_order(session, person, main_order=main_order)
        order_2 = self.create_order(session, person, main_order=main_order)
        self.overdraft_invoice(session, person, [(order_1, completion_qtys[0]), (order_2, completion_qtys[1])])

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, main_order.id)[1]
        notify_params = info['args'][0]
        assert info['path'] == ['BalanceClient', 'NotifyOrder2']
        assert notify_params['OverdraftSpentQty'] == '%.2f' % match_ov_spent_qty
        assert notify_params['ProductCurrency'] == 'RUB'
        assert notify_params['ServiceID'] == ServiceId.DIRECT
        assert notify_params['TotalConsumeQty'] == str(match_ov_spent_qty)
        assert notify_params['TotalCashbackCurrentQty'] == '0'
        assert notify_params['ServiceOrderID'] == main_order.service_order_id
        assert notify_params['OverdraftLimit'] == '%.2f' % overdraft_limit

        for order_i, order in enumerate((order_1, order_2)):
            info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)[1]
            notify_params = info['args'][0]
            assert info['path'] == ['BalanceClient', 'NotifyOrder2']
            assert notify_params.get('OverdraftSpentQty', None) is None
            assert notify_params['ProductCurrency'] == 'RUB'
            assert notify_params['ServiceID'] == ServiceId.DIRECT
            assert notify_params['ConsumeQty'] == str(completion_qtys[order_i])
            assert notify_params['ServiceOrderID'] == order.service_order_id
            assert notify_params.get('OverdraftLimit', None) is None

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE,
                                                               person.client.id)[1]
        notify_params = info['args'][0]

        assert info['path'] == ['BalanceClient', 'NotifyClient2']
        assert notify_params['ClientID'] == str(person.client_id)
        assert notify_params['ClientCurrency'] == person.client.currency_on()
        assert notify_params['OverdraftLimit'] == '%.2f' % overdraft_limit
        assert notify_params['OverdraftSpent'] == '%.2f' % match_ov_spent_qty

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person.client.overdraft.values()[0].id)[1]
        notify_params = info['args'][0]

        assert info['path'] == ['BalanceClient', 'NotifyClient2']
        assert notify_params['ClientID'] == str(person.client_id)
        assert notify_params['ClientCurrency'] == person.client.currency_on()
        assert notify_params['OverdraftLimit'] == '%.2f' % overdraft_limit
        assert notify_params['OverdraftSpent'] == '%.2f' % match_ov_spent_qty

    @pytest.mark.parametrize('completion_qtys, match_ov_spent_qty',
                             [
                                 ([D('0'), D('0')], D('0')),
                                 ([D('11.66'), D('34.78')], D('46.44')),
                                 ([D('45'), D('45')], D('90'))
                             ],
                             ids=[
                                 'order have not been shipped',
                                 'order have been partially shipped',
                                 'order have been completely shipped'
                             ],
                             )
    @pytest.mark.parametrize(
        'client_params, invoice_params',
        [
            [
                dict(iso_currency='BYN', overdraft_limit=12000, limit_wo_tax=10000, person_type='byp'),
                dict(product_id=DIRECT_PRODUCT_QUASI_BYN_ID, paysys_id=1102),
            ],
            [
                dict(iso_currency='KZT', overdraft_limit=11200, limit_wo_tax=10000, person_type='kzp'),
                dict(product_id=DIRECT_PRODUCT_QUASI_KZT_ID, paysys_id=1121)
            ],
        ]
    )
    def test_quasicurrency(self, session, client_params, invoice_params, completion_qtys, match_ov_spent_qty):
        person = self.get_person(session, client_params)
        self.overdraft_params(session, person)

        main_order = self.create_order(session, person, params={'product_id': invoice_params['product_id']})
        order_1 = self.create_order(session, person, main_order=main_order,
                                    params={'product_id': invoice_params['product_id']})
        order_2 = self.create_order(session, person, main_order=main_order,
                                    params={'product_id': invoice_params['product_id']})
        self.overdraft_invoice(session, person,
                               [(order_1, completion_qtys[0]), (order_2, completion_qtys[1])],
                               params={'paysys_id': invoice_params['paysys_id']})

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, main_order.id)[1]
        notify_params = info['args'][0]
        assert info['path'] == ['BalanceClient', 'NotifyOrder2']
        assert notify_params['OverdraftSpentQty'] == '%.2f' % match_ov_spent_qty
        assert notify_params['OverdraftLimit'] == '%.2f' % client_params['limit_wo_tax']
        assert notify_params['ProductCurrency'] == ''
        assert notify_params['ServiceID'] == ServiceId.DIRECT
        assert notify_params['TotalConsumeQty'] == str(match_ov_spent_qty)
        assert notify_params['TotalCashbackCurrentQty'] == '0'
        assert notify_params['ServiceOrderID'] == main_order.service_order_id

        for order_i, order in enumerate((order_1, order_2)):
            info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)[1]
            notify_params = info['args'][0]
            assert info['path'] == ['BalanceClient', 'NotifyOrder2']
            assert notify_params.get('OverdraftSpentQty', None) is None
            assert notify_params['ProductCurrency'] == ''
            assert notify_params['ServiceID'] == ServiceId.DIRECT
            assert notify_params['ConsumeQty'] == str(completion_qtys[order_i])
            assert notify_params['ServiceOrderID'] == order.service_order_id
            assert notify_params.get('OverdraftLimit', None) is None

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE,
                                                               person.client.id)[1]
        notify_params = info['args'][0]

        assert info['path'] == ['BalanceClient', 'NotifyClient2']
        assert notify_params['ClientID'] == str(person.client_id)
        assert notify_params['ClientCurrency'] == person.client.currency_on()
        assert notify_params['OverdraftLimit'] == '%.2f' % client_params['limit_wo_tax']
        assert notify_params['OverdraftSpent'] == '%.2f' % match_ov_spent_qty

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person.client.overdraft.values()[0].id)[1]
        notify_params = info['args'][0]

        assert info['path'] == ['BalanceClient', 'NotifyClient2']
        assert notify_params['ClientID'] == str(person.client_id)
        assert notify_params['ClientCurrency'] == person.client.currency_on()
        assert notify_params['OverdraftLimit'] == '%.2f' % client_params['limit_wo_tax']
        assert notify_params['OverdraftSpent'] == '%.2f' % match_ov_spent_qty

    def test_few_limits(self, session):
        overdraft_limit = 900
        person = self.get_person(session, dict(overdraft_limit=overdraft_limit))

        service = session.query(mapper.Service).getone(cst.ServiceId.DIRECT)
        overdraft_limits = {(service.id, 9): overdraft_limit * 2, (service.id, 27): 0}
        allowed_firms = [params.firm_id for params in mapper.ServiceFirmOverdraftParams.get(session, service.id)]
        person.client.set_overdraft_limit(service, 9, overdraft_limits[(service.id, 9)], 'RUB', None)
        person.client.set_overdraft_limit(service, 27, overdraft_limit / 2, 'RUB', None)
        person.client.set_overdraft_limit(service, 27, overdraft_limits[(service.id, 27)], 'RUB', None)

        self.overdraft_params(session, person, dict(overdraft_limit=90))

        main_order = self.create_order(session, person)
        order_1 = self.create_order(session, person, main_order=main_order)
        order_2 = self.create_order(session, person, main_order=main_order)
        self.overdraft_invoice(session, person, [(order_1, 45), (order_2, 45)])

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, main_order.id)[1]
        notify_params = info['args'][0]
        assert info['path'] == ['BalanceClient', 'NotifyOrder2']
        assert notify_params['OverdraftSpentQty'] == '%.2f' % 90
        assert notify_params['ProductCurrency'] == 'RUB'
        assert notify_params['ServiceID'] == ServiceId.DIRECT
        assert notify_params['TotalConsumeQty'] == str(90)
        assert notify_params['TotalCashbackCurrentQty'] == '0'
        assert notify_params['ServiceOrderID'] == main_order.service_order_id
        assert notify_params['OverdraftLimit'] == '%.2f' % overdraft_limit

        for order_i, order in enumerate((order_1, order_2)):
            info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_ORDER_OPCODE, order.id)[1]
            notify_params = info['args'][0]
            assert info['path'] == ['BalanceClient', 'NotifyOrder2']
            assert notify_params.get('OverdraftSpentQty', None) is None
            assert notify_params['ProductCurrency'] == 'RUB'
            assert notify_params['ServiceID'] == ServiceId.DIRECT
            assert notify_params['ConsumeQty'] == str(45)
            assert notify_params['ServiceOrderID'] == order.service_order_id
            assert notify_params.get('OverdraftLimit', None) is None

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE,
                                                               person.client.id)[1]
        notify_params = info['args'][0]

        assert info['path'] == ['BalanceClient', 'NotifyClient2']
        assert notify_params['ClientID'] == str(person.client_id)
        assert notify_params['ClientCurrency'] == person.client.currency_on()
        assert notify_params['OverdraftLimit'] == '%.2f' % overdraft_limit
        assert notify_params['OverdraftSpent'] == '%.2f' % 90

        for service_id, firm_id in overdraft_limits:
            info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                                   person.client.overdraft[(service_id, firm_id)].id)[1]
            notify_params = info['args'][0]

            assert info['path'] == ['BalanceClient', 'NotifyClient2']
            assert notify_params['ClientID'] == str(person.client_id)
            assert notify_params['ClientCurrency'] == person.client.currency_on()
            assert notify_params['OverdraftLimit'] == '%.2f' % (
                overdraft_limits[(service_id, firm_id)] if firm_id in allowed_firms else 0)
            assert notify_params['OverdraftSpent'] == '%.2f' % 90


class TestClientOverdraftInfo(object):
    @pytest.fixture(autouse=True)
    def patch_config(request, session):
        session.config.__dict__['USE_SERVICE_LIMIT_WO_TAX'] = getattr(request, 'param', 1)

    @staticmethod
    def get_person_with_overdraft(session, params=None):
        if params is None:
            params = {}
        firm_id = params.get('firm_id', cst.FirmId.YANDEX_OOO)
        service_id = params.get('service_id', cst.ServiceId.DIRECT)
        iso_currency = params.get('iso_currency', 'RUB')
        overdraft_limit = params.get('overdraft_limit', 10000)
        limit_wo_tax = params.get('limit_wo_tax', None)

        client = ob.ClientBuilder(is_agency=False).build(session).obj
        client.set_currency(service_id, iso_currency, ACT_MONTH.begin_dt, cst.CONVERT_TYPE_COPY)
        person = ob.PersonBuilder(client=client, type=params.get('person_type', 'ph')).build(session).obj

        service = session.query(mapper.Service).getone(service_id)
        person.client.set_overdraft_limit(service, firm_id, overdraft_limit, iso_currency, limit_wo_tax)

        session.flush()
        return person

    @staticmethod
    def create_order(session, person, params=None):
        if params is None:
            params = {}
        product_id = params.get('product_id', DIRECT_PRODUCT_RUB_ID)
        service_id = params.get('service_id', cst.ServiceId.DIRECT)
        order = ob.OrderBuilder(
            client=person.client,
            product=ob.Getter(mapper.Product, product_id),
            service_id=service_id
        ).build(session).obj
        return order

    @staticmethod
    def overdraft_invoice(session, person, basket_items, params=None):
        if params is None:
            params = {}
        paysys_id = params.get('paysys_id', ob.PAYSYS_ID)
        invoice = ob.InvoiceBuilder(
            person=person,
            paysys=ob.Getter(mapper.Paysys, paysys_id),
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=person.client,
                    rows=[
                        ob.BasketItemBuilder(order=child_order, quantity=quantity)
                        for child_order, quantity in basket_items
                    ]
                )
            ),
            overdraft=True
        ).build(session).obj
        invoice.turn_on_rows(cut_agava=True)
        return invoice

    def test_min_payment_term_no_invoice(self, session):
        person_1 = self.get_person_with_overdraft(session)

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_1.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['MinPaymentTerm'] == '0000-00-00'

    @pytest.mark.parametrize('payment_term_dts',
                             [
                                 (datetime.datetime.now() + datetime.timedelta(days=5), None),
                                 (None, datetime.datetime.now() + datetime.timedelta(days=5)),
                             ]
                             )
    def test_min_payment_term_eq_clients(self, session, payment_term_dts):
        person_1 = self.get_person_with_overdraft(session)
        order_1 = self.create_order(session, person_1)
        invoice_1 = self.overdraft_invoice(session, person_1, [(order_1, 90)])

        person_2 = self.get_person_with_overdraft(session)
        order_2 = self.create_order(session, person_2)
        invoice_2 = self.overdraft_invoice(session, person_2, [(order_2, 90)])

        person_2.client.make_equivalent(person_1.client)

        if payment_term_dts[0]:
            invoice_1.payment_term_dt = payment_term_dts[0]
        if payment_term_dts[1]:
            invoice_2.payment_term_dt = payment_term_dts[1]
        session.flush()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_1.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['MinPaymentTerm'] == (datetime.datetime.now() + datetime.timedelta(days=5)).strftime(
            '%Y-%m-%d')

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_2.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['MinPaymentTerm'] == (datetime.datetime.now() + datetime.timedelta(days=5)).strftime(
            '%Y-%m-%d')

    @pytest.mark.parametrize('payment_term_dts',
                             [
                                 (datetime.datetime.now() + datetime.timedelta(days=5), None),
                                 (None, datetime.datetime.now() + datetime.timedelta(days=5)),
                             ]
                             )
    def test_min_payment_term_brand_clients(self, session, payment_term_dts):
        person_1 = self.get_person_with_overdraft(session)
        order_1 = self.create_order(session, person_1)
        invoice_1 = self.overdraft_invoice(session, person_1, [(order_1, 90)])

        person_2 = self.get_person_with_overdraft(session)
        order_2 = self.create_order(session, person_2)
        invoice_2 = self.overdraft_invoice(session, person_2, [(order_2, 90)])

        ob.create_brand(
            session,
            [(
                datetime.datetime.now() - datetime.timedelta(10),
                [person_1.client, person_2.client]
            )],
            datetime.datetime.now() + datetime.timedelta(20)
        )
        session.expire_all()

        if payment_term_dts[0]:
            invoice_1.payment_term_dt = payment_term_dts[0]
        if payment_term_dts[1]:
            invoice_2.payment_term_dt = payment_term_dts[1]
        session.flush()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_1.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['MinPaymentTerm'] == (datetime.datetime.now() + datetime.timedelta(days=5)).strftime(
            '%Y-%m-%d')

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_2.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['MinPaymentTerm'] == (datetime.datetime.now() + datetime.timedelta(days=5)).strftime(
            '%Y-%m-%d')


class TestTVM(object):
    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_json_with_ticket(self, session, create_tvm_client_mock, tvm_client_mock):
        service = ob.ServiceBuilder.construct(session)
        order = ob.OrderBuilder.construct(session, service=service)
        snp = ob.ServiceNotifyParamsBuilder.construct(session, service=service)

        ticket = ob.generate_character_string(5)
        tvm_client_mock.get_service_ticket_for.return_value = ticket

        httpretty.register_uri(
            httpretty.POST,
            snp.test_url + '/BalanceClient/NotifyOrder2'
        )

        message = Message(order.id, NOTIFY_ORDER_OPCODE, timestamp=None)
        notifier_objects.BaseInfo.process(session, message)

        latest_requests = httpretty.httpretty.latest_requests
        assert len(latest_requests) == 1, latest_requests
        request = httpretty.httpretty.latest_requests[0]
        headers = dict(request.headers)
        assert TVM2_SERVICE_TICKET_HEADER.lower() in headers
        assert headers[TVM2_SERVICE_TICKET_HEADER.lower()] == ticket

        create_tvm_client_mock.assert_called_once_with(TVMToolAliases.YB_MEDIUM)
        tvm_client_mock.get_service_ticket_for.assert_called_once_with(snp.tvm_alias)

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    @pytest.mark.parametrize(
        ['protocol'],
        [pytest.param(notifier_objects.XML_RPC, id='xmlrpc'),
         pytest.param(notifier_objects.JSON_REST, id='json-rest')]
    )
    def test_ticket_not_passed_over_http(self, session, create_tvm_client_mock, protocol):
        service = ob.ServiceBuilder.construct(session)
        order = ob.OrderBuilder.construct(session, service=service)
        snp = ob.ServiceNotifyParamsBuilder.construct(session, service=service, url_scheme='http',
                                                      protocol=protocol)

        message = Message(order.id, NOTIFY_ORDER_OPCODE, timestamp=None)
        with pytest.raises(AssertionError) as exc_info:
            notifier_objects.BaseInfo.process(session, message)

        assert snp.test_url in str(exc_info.value)
        create_tvm_client_mock.assert_not_called()

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    @pytest.mark.parametrize(
        # Параметр нужен, чтобы убедиться, что http без тикетов поддерживается.
        ['url_scheme'],
        [pytest.param('https', id='https'),
         pytest.param('http', id='http')]
    )
    def test_json_without_ticket(self, session, create_tvm_client_mock, url_scheme):
        service = ob.ServiceBuilder.construct(session)
        order = ob.OrderBuilder.construct(session, service=service)
        snp = ob.ServiceNotifyParamsBuilder.construct(session, service=service, tvm_alias=None,
                                                      url_scheme=url_scheme)

        httpretty.register_uri(
            httpretty.POST,
            snp.test_url + '/BalanceClient/NotifyOrder2'
        )

        message = Message(order.id, NOTIFY_ORDER_OPCODE, timestamp=None)
        notifier_objects.BaseInfo.process(session, message)

        latest_requests = httpretty.httpretty.latest_requests
        assert len(latest_requests) == 1, latest_requests
        request = httpretty.httpretty.latest_requests[0]
        headers = dict(request.headers)
        assert TVM2_SERVICE_TICKET_HEADER.lower() not in headers

        create_tvm_client_mock.assert_not_called()

    @staticmethod
    def make_xmlrpc_response(value):
        return MediumXmlRpcInvoker({})._dump_xmlrpc((value,))

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_xmlrpc_with_ticket(self, session, create_tvm_client_mock, tvm_client_mock):
        service = ob.ServiceBuilder.construct(session)
        order = ob.OrderBuilder.construct(session, service=service)
        snp = ob.ServiceNotifyParamsBuilder.construct(session, service=service,
                                                      protocol=notifier_objects.XML_RPC)

        ticket = ob.generate_character_string(5)
        tvm_client_mock.get_service_ticket_for.return_value = ticket

        httpretty.register_uri(
            httpretty.POST,
            snp.test_url,
            body=self.make_xmlrpc_response((0, 'ok')),
        )

        message = Message(order.id, NOTIFY_ORDER_OPCODE, timestamp=None)
        notifier_objects.BaseInfo.process(session, message)

        latest_requests = httpretty.httpretty.latest_requests
        assert len(latest_requests) == 1, latest_requests
        request = httpretty.httpretty.latest_requests[0]
        headers = dict(request.headers)
        assert TVM2_SERVICE_TICKET_HEADER.lower() in headers
        assert headers[TVM2_SERVICE_TICKET_HEADER.lower()] == ticket

        create_tvm_client_mock.assert_called_once_with(TVMToolAliases.YB_MEDIUM)
        tvm_client_mock.get_service_ticket_for.assert_called_once_with(snp.tvm_alias)

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    @pytest.mark.parametrize(
        # Параметр нужен, чтобы убедиться, что http без тикетов поддерживается.
        ['url_scheme'],
        [pytest.param('https', id='https'),
         pytest.param('http', id='http')]
    )
    def test_xmlrpc_without_ticket(self, session, create_tvm_client_mock, url_scheme):
        service = ob.ServiceBuilder.construct(session)
        order = ob.OrderBuilder.construct(session, service=service)
        snp = ob.ServiceNotifyParamsBuilder.construct(session, service=service, tvm_alias=None,
                                                      protocol=notifier_objects.XML_RPC,
                                                      url_scheme=url_scheme)

        httpretty.register_uri(
            httpretty.POST,
            snp.test_url,
            body=self.make_xmlrpc_response((0, 'ok')),
        )

        message = Message(order.id, NOTIFY_ORDER_OPCODE, timestamp=None)
        notifier_objects.BaseInfo.process(session, message)

        latest_requests = httpretty.httpretty.latest_requests
        assert len(latest_requests) == 1, latest_requests
        request = httpretty.httpretty.latest_requests[0]
        headers = dict(request.headers)
        assert TVM2_SERVICE_TICKET_HEADER.lower() not in headers

        create_tvm_client_mock.assert_not_called()


class TestHasEquivalentOrBrandClientsNotification(object):

    @staticmethod
    def get_person(session):
        client = ob.ClientBuilder(is_agency=False).build(session).obj
        person = ob.PersonBuilder(client=client, type='ph').build(session).obj
        session.flush()
        return person

    @pytest.mark.linked_clients
    def test_no_equi_no_brand(self, session):
        person = self.get_person(session)

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE, person.client.id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '0'

    @pytest.mark.linked_clients
    def test_equi(self, session):
        person = self.get_person(session)

        eq_client = ob.ClientBuilder.construct(session)
        eq_client.make_equivalent(person.client)
        session.flush()
        session.expire_all()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE, person.client.id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '1'

    @pytest.mark.linked_clients
    @pytest.mark.parametrize('brand_dt_delta', [-10, 10], ids=['past', 'future'])
    def test_brand(self, session, brand_dt_delta):
        person_1 = self.get_person(session)
        person_2 = self.get_person(session)

        ob.create_brand(
            session,
            [(
                datetime.datetime.now() + datetime.timedelta(brand_dt_delta),
                [person_1.client, person_2.client]
            )],
            datetime.datetime.now() + datetime.timedelta(20)
        )
        session.expire_all()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE, person_1.client.id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '1'

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE, person_2.client.id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '1'

    @pytest.mark.linked_clients
    def test_after_brand(self, session):
        person_1 = self.get_person(session)
        person_2 = self.get_person(session)

        ob.create_brand(
            session,
            [(
                datetime.datetime.now() - datetime.timedelta(10),
                [person_1.client, person_2.client]
            )],
            datetime.datetime.now() - datetime.timedelta(1)
        )
        session.expire_all()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE, person_1.client.id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '0'

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OPCODE, person_2.client.id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '0'


class TestHasEquivalentOrBrandOrdersNotification(object):

    @staticmethod
    def get_person(session):
        firm_id = cst.FirmId.YANDEX_OOO
        service_id = cst.ServiceId.DIRECT
        iso_currency = 'RUB'
        overdraft_limit = 100

        client = ob.ClientBuilder(is_agency=False).build(session).obj
        client.set_currency(service_id, iso_currency, ACT_MONTH.begin_dt, cst.CONVERT_TYPE_COPY)
        person = ob.PersonBuilder(client=client, type='ph').build(session).obj

        service = session.query(mapper.Service).getone(service_id)
        person.client.set_overdraft_limit(service, firm_id, overdraft_limit, iso_currency)

        session.flush()
        return person

    @pytest.mark.linked_clients
    def test_no_equi_no_brand(self, session):
        person = self.get_person(session)

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '0'

    @pytest.mark.linked_clients
    def test_equi(self, session):
        person = self.get_person(session)

        eq_client = ob.ClientBuilder.construct(session)
        eq_client.make_equivalent(person.client)
        session.flush()
        session.expire_all()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '1'

    @pytest.mark.linked_clients
    @pytest.mark.parametrize('brand_dt_delta', [-10, 10], ids=['past', 'future'])
    def test_brand(self, session, brand_dt_delta):
        person_1 = self.get_person(session)
        person_2 = self.get_person(session)

        ob.create_brand(
            session,
            [(
                datetime.datetime.now() + datetime.timedelta(brand_dt_delta),
                [person_1.client, person_2.client]
            )],
            datetime.datetime.now() + datetime.timedelta(20)
        )
        session.expire_all()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_1.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '1'

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_2.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '1'

    @pytest.mark.linked_clients
    def test_after_brand(self, session):
        person_1 = self.get_person(session)
        person_2 = self.get_person(session)

        ob.create_brand(
            session,
            [(
                datetime.datetime.now() - datetime.timedelta(10),
                [person_1.client, person_2.client]
            )],
            datetime.datetime.now() - datetime.timedelta(1)
        )
        session.expire_all()

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_1.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '0'

        info = notifier_objects.BaseInfo.get_notification_info(session, NOTIFY_CLIENT_OVERDRAFT_OPCODE,
                                                               person_2.client.overdraft.values()[0].id)[1]
        assert info['args'][0]['HasEquivalentOrBrandClients'] == '0'


@pytest.mark.cashback
class TestClientCashbackNotification(object):
    @pytest.fixture
    def client(self, session):
        return ob.ClientBuilder.construct(session)

    @pytest.fixture
    def cashback(self, session, client):
        return ob.ClientCashbackBuilder.construct(
            session,
            client=client,
            service_id=cst.ServiceId.DIRECT,
            iso_currency='RUB',
            bonus=D('666.6666667'),
        )

    @pytest.fixture
    def order(self, session, client):
        return ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_ID)

    @pytest.fixture
    def invoice(self, session, client, order):
        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=D('10'))],
            ),
        )
        return ob.InvoiceBuilder.construct(
            session,
            client=client,
            request=request,
            person=ob.PersonBuilder.construct(session, client=client),
        )

    def do_complete(self, order, qty):
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
        order.session.flush()

    def test_create_consume(self, session, client, cashback, invoice):
        invoice.turn_on_rows()
        session.expire_all()
        assert cashback.bonus == D('0.0000067')

        info = notifier_objects.BaseInfo.get_notification_info(
            session,
            cst.NOTIFY_CLIENT_CASHBACK_OPCODE,
            cashback.id,
        )
        hm.assert_that(
            info[1],
            hm.has_entries({
                'path': hm.contains('BalanceClient', 'NotifyClientCashback'),
                'args': hm.contains({
                    'ClientID': str(client.id),
                    'ServiceID': '7',
                    'IsoCurrency': 'RUB',
                    'Bonus': '0.0000067',
                    'ConsumedBonus': '666.666660',
                    'FreeConsumedBonus': '666.666660',
                }),
            }),
        )

    def test_create_consume_several_cashbacks(self, session, client, invoice):
        cashback_1 = ob.ClientCashbackBuilder.construct(
            session,
            client=client,
            service_id=cst.ServiceId.DIRECT,
            iso_currency='RUB',
            bonus=D('333'),
        )

        cashback_2 = ob.ClientCashbackBuilder.construct(
            session,
            client=client,
            service_id=cst.ServiceId.DIRECT,
            iso_currency='RUB',
            bonus=D('333.6666667'),
            finish_dt=datetime.datetime.now() + datetime.timedelta(days=3)
        )

        invoice.turn_on_rows()
        session.expire_all()

        assert cashback_1.bonus == D('0')
        assert cashback_2.bonus == D('0.0000067')

        for cashback in [cashback_1, cashback_2]:
            info = notifier_objects.BaseInfo.get_notification_info(
                session,
                cst.NOTIFY_CLIENT_CASHBACK_OPCODE,
                cashback.id,
            )
            hm.assert_that(
                info[1],
                hm.has_entries({
                    'path': hm.contains('BalanceClient', 'NotifyClientCashback'),
                    'args': hm.contains({
                        'ClientID': str(client.id),
                        'ServiceID': '7',
                        'IsoCurrency': 'RUB',
                        'Bonus': '0.0000067',
                        'ConsumedBonus': '666.666660',
                        'FreeConsumedBonus': '666.666660',
                    }),
                }),
            )

    def test_create_consume_several_cashbacks_new(self, session, client, invoice):
        cashback_1 = ob.ClientCashbackBuilder.construct(
            session,
            client=client,
            service_id=cst.ServiceId.DIRECT,
            iso_currency='RUB',
            bonus=D('333'),
        )

        finish_dt = ut.trunc_date(datetime.datetime.now() + datetime.timedelta(days=1))
        cashback_2 = ob.ClientCashbackBuilder.construct(
            session,
            client=client,
            service_id=cst.ServiceId.DIRECT,
            iso_currency='RUB',
            bonus=D('333.6666667'),
            finish_dt=finish_dt,
        )

        invoice.turn_on_rows()
        session.expire_all()

        assert cashback_1.bonus == D('0')
        assert cashback_2.bonus == D('0.0000067')

        for cashback in [cashback_1, cashback_2]:
            from balance.actions.cashback.utils import get_notification_id
            notification_id = get_notification_id(session, cashback.id, cst.ServiceId.DIRECT)
            info = notifier_objects.BaseInfo.get_notification_info(
                session,
                cst.NOTIFY_CLIENT_CASHBACK_LIST_OPCODE,
                notification_id,
            )
            hm.assert_that(
                info[1],
                hm.has_entries({
                    'path': hm.contains('BalanceClient', 'NotifyClientCashback'),
                    'args': hm.contains({
                        'ClientID': str(client.id),
                        'ServiceID': '7',
                        'Bonuses': [
                            {'IsoCurrency': 'RUB',
                             'Bonus': '0',
                             'FinishDT': 'None',
                             'ConsumedBonus': '333.000000',
                             'FreeConsumedBonus': '333.000000'},
                            {'IsoCurrency': 'RUB',
                             'Bonus': '0.0000067',
                             'FinishDT': str(finish_dt),
                             'ConsumedBonus': '333.666660',
                             'FreeConsumedBonus': '333.666660'}
                        ]
                    }),
                }),
            )

    def test_reverse_consume(self, session, client, cashback, order, invoice):
        session.config.__dict__['ALLOWED_CASHBACK_NOTIFICATIONS'] = 1
        invoice.manual_turn_on(D('1000'))
        self.do_complete(order, D('3'))

        order.transfer(None, mode=cst.TransferMode.all)
        session.expire_all()
        session.flush()

        assert cashback.bonus == D('604.5977017')

        # notify для кешбэка вызывается дважды, но нотификация получается одна
        notification = (
            session
                .execute(
                'select * from bo.t_object_notification where opcode = :opcode and object_id in (:id)',
                {'opcode': cst.NOTIFY_CLIENT_CASHBACK_OPCODE, 'id': cashback.id},
            )
                .fetchall()
        )
        assert len(notification) == 1

        info = notifier_objects.BaseInfo.get_notification_info(
            session,
            cst.NOTIFY_CLIENT_CASHBACK_OPCODE,
            cashback.id,
        )
        hm.assert_that(
            info[1],
            hm.has_entries({
                'path': hm.contains('BalanceClient', 'NotifyClientCashback'),
                'args': hm.contains({
                    'ClientID': str(client.id),
                    'ServiceID': '7',
                    'IsoCurrency': 'RUB',
                    'Bonus': '604.5977017',
                    'ConsumedBonus': '62.068965',
                    'FreeConsumedBonus': '0.000000',
                }),
            }),
        )

    @pytest.mark.parametrize(
        'cfg, is_processed',
        [
            (0, False),
            (1, True),
            ([ServiceId.DIRECT], True),
            ([ServiceId.MARKET], False),
        ]
    )
    def test_cfg(self, session, cashback, cfg, is_processed):
        session.config.__dict__['ALLOWED_CASHBACK_NOTIFICATIONS'] = cfg
        cashback.notify()

        notifications = session.execute(
            'select * from bo.t_object_notification where opcode = :opcode and object_id in (:id)',
            {'opcode': cst.NOTIFY_CLIENT_CASHBACK_OPCODE, 'id': cashback.id}
        ).fetchall()
        assert len(notifications) == (1 if is_processed else 0)
