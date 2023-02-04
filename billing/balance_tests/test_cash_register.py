# -*- coding: utf-8 -*-

import pickle
import re
import collections
from decimal import Decimal as D

import datetime
from dateutil.relativedelta import relativedelta
import hamcrest
import mock
import pytest

from balance import constants as cst
from balance import mapper, core
from balance import multilang_support
import balance.actions.acts as a_a
from balance.actions import promocodes
from balance.actions.consumption import reverse_consume
from balance.processors import cash_register
from tests.balance_tests.invoices.invoice_common import create_personal_account, create_invoice as _create_invoice
from tests.balance_tests.thirdparty_transaction.common import TestThirdpartyTransactions
from balance.actions.invoice_create import generate_split_invoices
from tests import object_builder as ob

multilang_support.set_trans_dir('trans_dir')


class MockConfig(cash_register._Config):
    def __init__(self):
        self.email = 'balance-unittests-666@yandex-team.ru'
        self.whitespirit_url_format = 'https://whitespirit-unittests-INN.paysys.yandex.net/v1'
        self.darkspirit_url = 'https://darkspirit-unittests.paysys.yandex.net/v1'
        self.receipt_render_url = 'https://check-back.paysys.yandex.net'


@pytest.fixture
def config():
    return MockConfig()


@pytest.fixture(scope='module')
def direct_product_rub_fullname(modular_session):
    return modular_session.query(mapper.Product).getone(cst.DIRECT_PRODUCT_RUB_ID).fullname


def create_invoice(
    session,
    products_qtys,
    paysys_id=1002,
    overdraft=0,
    person_type='ph',
    firm_id=cst.FirmId.YANDEX_OOO,
    cashback=False, promocode=False
):
    client = ob.ClientBuilder().build(session).obj

    if cashback:
        ob.ClientCashbackBuilder.construct(
            session,
            client=client,
            bonus=D('100')
        )

    if promocode:
        pc_group = ob.PromoCodeGroupBuilder.construct(
            session,
            calc_class_name='FixedSumBonusPromoCodeGroup',
            calc_params={
                # adjust_quantity и apply_on_create общие для всех типов промокодов
                'adjust_quantity': 0,  # увеличиваем количество (иначе уменьшаем сумму)
                'apply_on_create': 1,  # применяем при создании счёта иначе при включении (оплате)
                # остальные зависят от типа
                'currency_bonuses': {"RUB": D('100')},
                'reference_currency': 'RUB',
            },
        )
        pc = pc_group.promocodes[0]
        promocodes.reserve_promo_code(client, pc)

    invoice = ob.InvoiceBuilder(
        firm=ob.Getter(mapper.Firm, firm_id),
        paysys=ob.Getter(mapper.Paysys, paysys_id),  # card
        person=ob.PersonBuilder(client=client, type=person_type),
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(
                        quantity=qty,
                        order=ob.OrderBuilder(
                            client=client,
                            product=ob.Getter(mapper.Product, product_id)
                        )
                    )
                    for product_id, qty in products_qtys
                ]
            )
        ),
        overdraft=overdraft
    ).build(session).obj
    return invoice


def create_fictive_invoice(session):
    month = mapper.ActMonth()
    contract = ob.ContractBuilder.construct(session, payment_type=3, dt=month.begin_dt, services=[cst.ServiceId.DIRECT])

    order = ob.OrderBuilder.construct(session,
                                      client=contract.client,
                                      product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_ID))

    paysys = session.query(mapper.Paysys).getone(1002)

    request = ob.RequestBuilder.construct(session,
                                          basket=ob.BasketBuilder(
                                              client=contract.client,
                                              rows=[ob.BasketItemBuilder(
                                                  quantity=D('10'),
                                                  order=order
                                              )
                                              ]
                                          )
                                          )
    session.flush()
    basket = mapper.Basket(contract.client, [ro.basket_item() for ro in request.request_orders])

    invoice_kwargs = dict(credit=2,
                          temporary=False,
                          paysys=paysys,
                          person=contract.person,
                          contract=contract,
                          pay_on_credit=True)
    invoices = generate_split_invoices(session, basket, invoice_kwargs, request=request)
    invoice, = invoices
    session.flush()

    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()
    session.flush()

    order.calculate_consumption(month.document_dt, {order.shipment_type: 10})

    acc = a_a.ActAccounter(contract.client, month, None, [invoice.deferpay.id], [], force=True)
    act = acc.do()[0]
    return invoice, act.invoice


def create_y_invoice(session):
    contract = ob.ContractBuilder(
        commission=0,
        firm=1,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=session.now(),
    ).build(session).obj
    contract.client.is_agency = 1
    orders = [ob.OrderBuilder(product=ob.Getter(mapper.Product, 1475),
                              service=ob.Getter(mapper.Service, 7),
                              client=ob.ClientBuilder.construct(session),
                              agency=contract.client
                              ).build(session).obj for _ in range(1)]

    basket = ob.BasketBuilder(
        client=contract.client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for order in orders for o, qty in [(order, 10)]
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj
    coreobj = core.Core(request.session)
    pa, = coreobj.pay_on_credit(
        request_id=request.id,
        paysys_id=1002,
        person_id=contract.person.id,
        contract_id=contract.id
    )
    request.session.flush()
    now = datetime.datetime.now()
    for order in orders:
        order.calculate_consumption(now, {order.shipment_type: 10})
    act, = a_a.ActAccounter(
        pa.client,
        mapper.ActMonth(for_month=now),
        invoices=[pa.id], dps=[],
        force=1
    ).do()
    invoice = act.invoice
    pa.session.flush()
    return invoice


@pytest.fixture
def service(session):
    service = ob.ServiceBuilder.construct(session)
    service.balance_service.extra_pay = True
    service.balance_service.chargenote_scheme = True
    session.flush()

    return service


def create_chargenote(session, service):
    pa = create_personal_account(session, service_id=service.id)
    return _create_invoice(
        pa.session,
        100,
        1033,
        service_id=service.id,
        contract=pa.contract,
        requested_type='charge_note',
        turn_on_rows=False
    )


def create_payment(invoice, amount=None):
    payment = ob.CardPaymentBuilder(invoice=invoice).build(invoice.session).obj
    payment.turn_on(amount=amount)
    return payment


PRICE_CASES = [
    (D(1000), D(1), D(1), False,  # amount, quantity, type_rate
     D(1000), D(1), D(1)),  # new_price, new_quantity, new_type_rate

    (D(1000), D(30), D(1), False,
     D('33333.33'), D('0.030'), D(1000)),

    (D(1000), D(100000000000), D(1), False,
     D('0.01'), D(100000), D(1000000)),

    (D(1000), D('0.0001'), D(1), False,
     D(1000), D('1'), D(1)),

    (D('0.01'), D(1000), D(1), False,
     D('0.01'), D(1), D(1000)),

    (D('64.91'), D('4234.42423'), D(1), False,
     D('15.33'), D('4.234'), D(1000)),

    (D('49999.99'), D('49999.987'), D(1), False,
     D(1), D('49999.987'), D(1)),

    (D('2924'), D('2924.0046'), D(1), False,
     D('1000'), D('2.924'), D(1000)),

    (D('35521.35'), D('35521.3453'), D(1), True,
     D('35521.35'), D('1'), D(1)),

    (D('15588.27'), D('15588.265'), D(1), True,
     D('15588.27'), D('1'), D(1))
]


@pytest.mark.parametrize(
    ['amount', 'quantity', 'type_rate', 'is_money_product', 'new_price', 'new_quantity', 'new_type_rate'], PRICE_CASES,
    indirect=False, ids=None, scope=None)
def test_calc_price(amount, quantity, type_rate, is_money_product, new_price, new_quantity, new_type_rate):
    assert cash_register.format_amount(new_quantity * new_price) == cash_register.format_amount(amount)

    rounded_quantity, rounded_price, rounded_type_rate = cash_register.calc_price(amount, quantity, type_rate,
                                                                                  is_money_product)

    assert D(cash_register.format_qty(rounded_quantity)) == new_quantity
    assert D(cash_register.format_price(rounded_price)) == new_price
    assert rounded_type_rate == new_type_rate


@pytest.mark.receipt
class TestCreateReceiptParamsBO(object):
    @pytest.mark.parametrize('product_id', [cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_PRODUCT_ID])
    @pytest.mark.parametrize('paysys_id', [1002, 1003])
    @pytest.mark.parametrize('w_payment', [True, False])
    def test_is_paid(self, session, paysys_id, w_payment, product_id):
        invoice = create_invoice(session, [(product_id, 10)], paysys_id=paysys_id)
        if w_payment:
            create_payment(invoice)
        if paysys_id == 1002 and w_payment:
            assert invoice.is_paid
        else:
            assert not invoice.is_paid

    @pytest.mark.parametrize('payment_sum_delta',
                             [
                                 D('-0.01'),
                                 D('0'),
                                 D('0.1')
                             ])
    def test_with_not_money_product_card_payment(self, session, config, payment_sum_delta):
        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_ID, 10),
                (cst.DIRECT_PRODUCT_ID, 20),
                (cst.DIRECT_PRODUCT_ID, 30),
            ]
        )
        assert invoice.paysys.instant == 1
        payment = create_payment(invoice, invoice.total_sum + payment_sum_delta)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)

        prod_name = session.query(mapper.Product).getone(cst.DIRECT_PRODUCT_ID).fullname

        if payment_sum_delta < 0:
            payment_type = 'partial_prepayment_wo_delivery'
        else:
            payment_type = 'full_prepayment_wo_delivery'
        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '30.00',
                        'payment_type_type': payment_type,
                        'tax_type': 'nds_20_120',
                        'qty': '10.000'
                    },
                    {
                        'text': prod_name,
                        'price': '30.00',
                        'payment_type_type': payment_type,
                        'tax_type': 'nds_20_120',
                        'qty': '20.000'
                    },
                    {
                        'text': prod_name,
                        'price': '30.00',
                        'payment_type_type': payment_type,
                        'tax_type': 'nds_20_120',
                        'qty': '30.000'
                    }
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '1800.00', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'balance-unittests-666@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id', 'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    @pytest.mark.parametrize('payment_sum_delta',
                             [
                                 D('-0.01'),
                                 D('0'),
                                 D('0.1')
                             ])
    def test_with_money_product(self, session, config, payment_sum_delta):
        config_ = collections.namedtuple('Config', ['email'])

        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_RUB_ID, 10),
                (cst.DIRECT_PRODUCT_RUB_ID, D('13.3453'))
            ]
        )
        assert invoice.money_product

        payment = create_payment(invoice, amount=invoice.total_sum + payment_sum_delta)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(
            payment, config_(email='no-email@yandex-team.ru')
        )

        prod_name = session.query(mapper.Product).getone(cst.DIRECT_PRODUCT_ID).fullname

        if payment_sum_delta < 0:
            payment_type = 'partial_prepayment_wo_delivery'
        else:
            payment_type = 'full_prepayment_wo_delivery'

        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '10.00',
                        'payment_type_type': payment_type,
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                    {
                        'text': prod_name,
                        'price': '13.35',
                        'payment_type_type': payment_type,
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '23.35', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'no-email@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id',
                                              'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_w_and_wo_money_product(self, session, config):
        config_ = collections.namedtuple('Config', ['email'])

        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_ID, 10),
                (cst.DIRECT_PRODUCT_RUB_ID, D('13.3453'))
            ]
        )
        assert invoice.money_product

        payment = create_payment(invoice)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(
            payment, config_(email='no-email@yandex-team.ru')
        )

        prod_name = session.query(mapper.Product).getone(
            cst.DIRECT_PRODUCT_ID).fullname
        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '30.00',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '10.000'
                    },
                    {
                        'text': prod_name,
                        'price': '13.35',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '313.35', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'no-email@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id',
                                              'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_weird_qty_wo_money_product(self, session, config):
        config_ = collections.namedtuple('Config', ['email'])

        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_ID, D('333.333333'))
            ]
        )

        payment = create_payment(invoice)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(
            payment, config_(email='no-email@yandex-team.ru')
        )

        prod_name = session.query(mapper.Product).getone(
            cst.DIRECT_PRODUCT_ID).fullname
        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '10000.00',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    }
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '10000.00', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'no-email@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id',
                                              'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_cashback(self, session, config):
        config_ = collections.namedtuple('Config', ['email'])

        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_ID, 10),
                (cst.DIRECT_PRODUCT_RUB_ID, D('13.3453'))
            ],
            cashback=True
        )
        invoice.turn_on_rows()
        for q in invoice.consumes:
            assert q.cashback_usage_id

        payment = create_payment(invoice)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(
            payment, config_(email='no-email@yandex-team.ru')
        )

        prod_name = session.query(mapper.Product).getone(
            cst.DIRECT_PRODUCT_ID).fullname
        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '30.00',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '10.000'
                    },
                    {
                        'text': prod_name,
                        'price': '13.35',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '313.35', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'no-email@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id',
                                              'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_promocode(self, session, config):
        config_ = collections.namedtuple('Config', ['email'])

        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_RUB_ID, D('1013.3453'))
            ],
            promocode=True
        )
        invoice.turn_on_rows(apply_promocode=True)
        for q in invoice.consumes:
            assert q.discount_obj.promo_code

        payment = create_payment(invoice)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(
            payment, config_(email='no-email@yandex-team.ru')
        )

        prod_name = session.query(mapper.Product).getone(
            cst.DIRECT_PRODUCT_ID).fullname
        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '893.35',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '893.35', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'no-email@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id',
                                              'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_rows_invoice_orders(self, session, config, direct_product_rub_fullname):
        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_RUB_ID, 10),
                (cst.DIRECT_PRODUCT_RUB_ID, 20),
                (cst.DIRECT_PRODUCT_RUB_ID, 30),
            ]
        )
        payment = create_payment(invoice)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)

        prod_name = direct_product_rub_fullname
        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '10.00',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                    {
                        'text': prod_name,
                        'price': '20.00',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                    {
                        'text': prod_name,
                        'price': '30.00',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    }
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '60.00', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'balance-unittests-666@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id', 'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    @pytest.mark.parametrize('payment_sum_delta',
                             [
                                 D('-0.01'),
                                 D('0'),
                                 D('0.1')
                             ])
    @pytest.mark.parametrize('w_act', [
        True,
        False
    ])
    @pytest.mark.parametrize('w_hidden_act', [
        True,
        False
    ])
    def test_rows_consumes(self, session, config, direct_product_rub_fullname, payment_sum_delta, w_act, w_hidden_act):
        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_RUB_ID, 10),
                (cst.DIRECT_PRODUCT_RUB_ID, 20),
                (cst.DIRECT_PRODUCT_RUB_ID, 30),
            ],
            overdraft=1
        )
        invoice.turn_on_rows()
        reverse_consume(invoice.consumes[0], None, 5)
        payment = create_payment(invoice, amount=invoice.total_sum + payment_sum_delta)
        month = mapper.ActMonth()
        order = invoice.invoice_orders[0].order
        order.calculate_consumption(month.document_dt, {order.shipment_type: 10})
        if w_act:
            acc = a_a.ActAccounter(invoice.client, month, None, [], [invoice.id], force=True)
            acc.do()

            if w_hidden_act:
                invoice.acts[0].hide()


        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)

        if w_act and not w_hidden_act:
            payment_type = 'credit_after_delivery'
        else:
            if payment_sum_delta < 0:
                payment_type = 'partial_prepayment_wo_delivery'
            else:
                payment_type = 'full_prepayment_wo_delivery'

        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': direct_product_rub_fullname,
                        'price': '55.00',
                        'payment_type_type': payment_type,
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '55.00', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'balance-unittests-666@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id', 'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_rows_consume_reversed(self, session, config, direct_product_rub_fullname):
        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_ID, 2),
                (cst.DIRECT_PRODUCT_RUB_ID, 30),
            ],
            overdraft=1
        )
        invoice.turn_on_rows()
        reverse_consume(invoice.consumes[0], None, 2)
        payment = create_payment(invoice)

        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)

        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': direct_product_rub_fullname,
                        'price': '30.00',
                        'payment_type_type': 'partial_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '30.00', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'balance-unittests-666@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id', 'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_rows_reduced_invoice_orders(self, session, config, direct_product_rub_fullname):
        invoice = create_invoice(
            session,
            [
                (cst.DIRECT_PRODUCT_RUB_ID, 11),
                (cst.DIRECT_PRODUCT_RUB_ID, 20),
                (cst.DIRECT_PRODUCT_RUB_ID, 30),
            ]
        )
        payment = create_payment(invoice)
        payment.amount = D('59.47')

        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)

        prod_name = direct_product_rub_fullname
        assert receipt_params == {
            'receipt_content': {
                'rows': [
                    {
                        'text': prod_name,
                        'price': '10.72',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                    {
                        'text': prod_name,
                        'price': '19.50',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    },
                    {
                        'text': prod_name,
                        'price': '29.25',
                        'payment_type_type': 'full_prepayment_wo_delivery',
                        'tax_type': 'nds_20_120',
                        'qty': '1.000'
                    }
                ],
                'agent_type': 'none_agent',
                'payments': [{'amount': '59.47', 'payment_type': 'card'}],
                'firm_url': u'direct.yandex.ru',
                'firm_reply_email': u'mailer@direct.yandex.ru',
                'firm_inn': invoice.firm.inn,
                'taxation_type': 'OSN',
                'client_email_or_phone': 'balance-unittests-666@yandex-team.ru',
                'receipt_type': 'income',
                'additional_user_requisite': {'name': 'balance_payment_id', 'value': str(payment.id)},
            },
            'trace': {'service_id': 7, 'paysys_id': 1002}
        }

    def test_client_email_invoice(self, session, config):
        config.email = None
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)])
        invoice.receipt_email = 'balance666@yandex-team.ru'
        invoice.person.email = 'balance777@yandex-team.ru'
        payment = create_payment(invoice)

        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)
        assert receipt_params['receipt_content']['client_email_or_phone'] == 'balance666@yandex-team.ru'

    @pytest.mark.parametrize('payment_sum_delta',
                             [
                                 D('-0.01'),
                                 D('0'),
                                 D('0.1')
                             ])
    def test_charge_note_product_name(self, session, config, service, payment_sum_delta):
        config.email = None
        charge_note = create_chargenote(session, service)
        payment = create_payment(charge_note, amount=charge_note.total_sum + payment_sum_delta)
        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)
        hamcrest.assert_that(receipt_params,
                             hamcrest.has_entries(
                                 {'receipt_content': hamcrest.has_entries(
                                     {"rows": hamcrest.contains(hamcrest.has_entries(
                                         {'text': u'Услуги «Яндекс.Директ», лицевой счет номер {}'.format(
                                             charge_note.external_id),
                                             'payment_type_type': 'full_prepayment_wo_delivery'}))})}))

    def test_client_email_person(self, session, config):
        config.email = None
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)])
        invoice.person.email = 'balance777@yandex-team.ru'
        payment = create_payment(invoice)

        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)
        assert receipt_params['receipt_content']['client_email_or_phone'] == 'balance777@yandex-team.ru'

    @pytest.mark.parametrize('payment_sum_delta',
                             [
                                 D('-0.01'),
                                 D('0'),
                                 D('0.1')
                             ])
    @pytest.mark.parametrize('w_act', [
        True,
        False
    ])
    @pytest.mark.parametrize('w_hidden_act', [True, False])
    def test_fictive_invoice_payment_type(self, session, config, payment_sum_delta, w_act, w_hidden_act):
        fictive_invoice, repayment_invoice = create_fictive_invoice(session)
        payment = create_payment(repayment_invoice, amount=repayment_invoice.total_sum + payment_sum_delta)

        if w_hidden_act:
            repayment_invoice.acts[0].hide()

        if not w_act:
            # цепляю акт на другой счет, не получается по другому выставить счет на погашение без акта
            repayment_invoice.acts[0].invoice = fictive_invoice
            session.flush()

        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)

        if w_act and not w_hidden_act:
            payment_type = 'credit_after_delivery'
        else:
            if payment_sum_delta < 0:
                payment_type = 'partial_prepayment_wo_delivery'
            else:
                payment_type = 'full_prepayment_wo_delivery'
        hamcrest.assert_that(receipt_params,
                             hamcrest.has_entries(
                                 {'receipt_content': hamcrest.has_entries(
                                     {"rows": hamcrest.contains(hamcrest.has_entries(
                                         {'payment_type_type': payment_type}))})}))

    @pytest.mark.parametrize('payment_sum_delta',
                             [
                                 D('-0.01'),
                                 D('0'),
                                 D('0.1')
                             ])
    @pytest.mark.parametrize('w_act', [
        True,
        False
    ])
    @pytest.mark.parametrize('w_hidden_act', [
        True,
        False
    ])
    def test_y_invoice_invoice_payment_type(self, session, config, payment_sum_delta, w_act, w_hidden_act):
        invoice = create_y_invoice(session)
        payment = create_payment(invoice, amount=invoice.total_sum + payment_sum_delta)

        if w_hidden_act:
            invoice.acts[0].hide()

        if not w_act:
            # цепляю акт на другой счет, не получается по другому выставить счет на погашение без акта
            invoice.acts[0].invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_RUB_ID, 1)])
            session.flush()

        receipt_params = cash_register.create_fiscal_receipt_params_bo(payment, config)
        if w_act and not w_hidden_act:
            payment_type = 'credit_after_delivery'
        else:
            if payment_sum_delta < 0:
                payment_type = 'partial_prepayment_wo_delivery'
            else:
                payment_type = 'full_prepayment_wo_delivery'
        hamcrest.assert_that(receipt_params,
                             hamcrest.has_entries(
                                 {'receipt_content': hamcrest.has_entries(
                                     {"rows": hamcrest.contains(hamcrest.has_entries(
                                         {'payment_type_type': payment_type}))})}))


@pytest.mark.receipt
@pytest.mark.export
class TestCreateReceipt(object):

    @property
    def firm_b(self):
        return ob.Getter(mapper.Firm, cst.FirmId.YANDEX_OOO)

    export_type = 'CASH_REGISTER'

    receipt_data = {u'document_index': 10,
                    u'dt': u'2017-06-16 18:56:00',
                    u'firm': {u'inn': u'7736207543',
                              u'name': u'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС"',
                              u'reply_email': u'info@check.yandex.ru'},
                    u'fn': {u'model': u'ФН-1', u'sn': u'8710000100594230'},
                    u'fp': 2340446449L,
                    u'id': 20,
                    u'kkt': {u'automatic_machine_number': u'whitespirit1f',
                             u'rn': u'0000608686020245',
                             u'sn': u'00000000381001921143',
                             u'url': u'http://yandex.ru'},
                    u'location': {u'address': u'141004, Россия, Московская обл., г. Мытищи, ул. Силикатная, д. 19',
                                  u'description': u'Мытищи 3',
                                  u'id': u'666'},
                    u'ofd': {u'inn': u'7709364346',
                             u'name': u'АКЦИОНЕРНОЕ ОБЩЕСТВО "ЭНЕРГЕТИЧЕСКИЕ СИСТЕМЫ И КОММУНИКАЦИИ"'},
                    u'ofd_ticket_received': 0,
                    u'receipt_calculated_content': {u'money_received_total': u'14986.00',
                                                    u'rows': [{u'amount': u'14986.00',
                                                               u'payment_type_type': u'full_prepayment_wo_delivery',
                                                               u'price': u'1.00',
                                                               u'qty': u'14986',
                                                               u'tax_amount': u'2286.00',
                                                               u'tax_pct': u'18.00',
                                                               u'tax_type': u'nds_20_120',
                                                               u'text': u'Турция'}],
                                                    u'tax_totals': [{u'tax_amount': u'2286.00',
                                                                     u'tax_pct': u'18.00',
                                                                     u'tax_type': u'nds_20_120'}],
                                                    u'total': u'14986.00',
                                                    u'totals': [{u'amount': u'14986.00',
                                                                 u'payment_type': u'card'}]},
                    u'receipt_content': {u'agent_type': u'none_agent',
                                         u'client_email_or_phone': u'fed_or@inbox.ru',
                                         u'firm_inn': u'7736207543',
                                         u'payments': [{u'amount': u'14986.00',
                                                        u'payment_type': u'card'}],
                                         u'receipt_type': u'income',
                                         u'rows': [{u'payment_type_type': u'full_prepayment_wo_delivery',
                                                    u'price': u'1.00',
                                                    u'qty': u'14986',
                                                    u'tax_type': u'nds_20_120',
                                                    u'text': u'Турция'}],
                                         u'taxation_type': u'OSN',
                                         u'user_reply_email': u'info@check.yandex.ru'},
                    u'shift_number': 4,
                    u'recipient_email': 'test@ya.ru',
                    u'invoice_id': 1234567890,
                    u'service_id': 9876543210}

    receipt_render_response = mock.MagicMock(status_code=200, text='receipt render response')
    darkspirit_response = mock.MagicMock(status_code=200, text='darkspirit response')

    @property
    def whitespirit_api(self):
        # замокиваем вызов rest_client.API(whitespirit_url, resource_class=JSONResource).receipts.post(receipt_params)
        mock_obj = self.whitespirit_mock
        mock_obj.post.return_value = self.receipt_data
        mock_obj.receipts = mock_obj
        mock_obj.return_value = mock_obj
        return mock_obj

    @property
    def darkspirit_api(self):
        # замокиваем вызов
        # rest_client.API(config.darkspirit_url, resource_class=JSONInputResource).receipts.post(receipt_data)
        mock_obj = self.darkspirit_mock
        mock_obj.post.return_value = self.darkspirit_response
        mock_obj.receipts = mock_obj
        mock_obj.return_value = mock_obj
        return mock_obj

    @property
    def receipt_render_api(self):
        # замокиваем вызов
        # rest_client.API(config.receipt_render_url, resource_class=JSONInputResource)[''].post(receipt_data)
        mock_obj = self.render_mock
        mock_obj.post.return_value = self.receipt_render_response
        return {'': mock_obj}

    def _set_mock(self):
        # для каждого сервиса создаем отдельные замоканные api
        self.whitespirit_mock = mock.MagicMock()
        self.darkspirit_mock = mock.MagicMock()
        self.render_mock = mock.MagicMock()

    def _rest_api_new(self, *a, **kw):
        # выбораем api на основе переданного url
        config = cash_register._Config()
        api_dict = {
            config.whitespirit_url: self.whitespirit_api,
            config.darkspirit_url: self.darkspirit_api,
            config.receipt_render_url: self.receipt_render_api,
        }
        return api_dict[a[1]]

    @pytest.mark.parametrize(
        'deny_fiscal_email', [0, 1]
    )
    def test_create_new_receipt(self, session, create_tvm_client_mock, tvm_client_mock, deny_fiscal_email):
        firm = self.firm_b.build(session).obj
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)])
        invoice.receipt_email = 'test@ya.ru'
        payment = create_payment(invoice)
        export = payment.exports[self.export_type]

        firm.deny_fiscal_email = deny_fiscal_email
        session.flush()

        with mock.patch('butils.rest_client.API.__new__', self._rest_api_new):
            self._set_mock()
            output = cash_register.process_bo_payment(export)

            if deny_fiscal_email:
                message_id, response_text = re.findall(r'Receipt email id is (\w+), ds_url is (.+)', output)[0]
                assert message_id == 'None'
                assert response_text == self.darkspirit_response.text
            else:
                message_id, response_text = re.findall(r'Receipt email id is (\d+), ds_url is (.+)', output)[0]
                assert response_text == self.darkspirit_response.text

                message = session.query(mapper.EmailMessage).getone(message_id)
                subject, body, sender, attachments = pickle.loads(message.data)
                assert message.recepient_name == invoice.person.name
                assert subject == u'Электронный чек'
                assert body == self.receipt_render_response.text

            self.whitespirit_api.receipts.post.assert_called_once()
            self.receipt_render_api[''].post.assert_called_once_with(self.receipt_data)
            self.darkspirit_api.receipts.post.assert_called_once_with(self.receipt_data)

            create_tvm_client_mock.assert_called_with(cst.TVMToolAliases.YB_MEDIUM)
            tvm_client_mock.assert_has_calls([
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.WHITESPIRIT),
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.CHECK_RENDERER),
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.DARKSPIRIT)
            ])

            receipt_params_match = {
                'receipt_content': hamcrest.has_entries({'firm_inn': firm.inn, 'rows': hamcrest.has_length(1)}),
                'trace': hamcrest.has_entries({'service_id': cst.ServiceId.DIRECT})
            }
            hamcrest.assert_that(
                self.whitespirit_api.receipts.post.call_args[0][0],
                hamcrest.has_entries(receipt_params_match)
            )

            assert len(payment.fiscal_receipts) == 1
            fiscal_receipt = payment.fiscal_receipts[-1]

            assert fiscal_receipt.receipt_fd == '%010d' % int(self.receipt_data['id'])
            assert fiscal_receipt.receipt_fpd == '%010d' % int(self.receipt_data['fp'])
            assert fiscal_receipt.receipt_fn == '%016d' % int(self.receipt_data['fn']['sn'])
            assert fiscal_receipt.recipient_email == self.receipt_data['recipient_email']
            assert fiscal_receipt.invoice_id == invoice.id
            assert fiscal_receipt.service_id == export.object.service_id

            assert export.input is None

    @pytest.mark.parametrize(
        'firm_id, change_start_dt, inn_changes', [
            [cst.FirmId.MARKET, datetime.datetime.now() + relativedelta(days=1), False],
            [cst.FirmId.MARKET, datetime.datetime.now() - relativedelta(days=1), True],
            [cst.FirmId.FOOD, datetime.datetime.now() + relativedelta(days=1), False],
            [cst.FirmId.FOOD, datetime.datetime.now() - relativedelta(days=1), False],
        ],
        ids=['market-before', 'market-after', 'not-market-before', 'not-market-after']
    )
    def test_inn_change_for_market(self, session, firm_id, change_start_dt, inn_changes, create_tvm_client_mock,
                                   tvm_client_mock):
        firm = ob.Getter(mapper.Firm, firm_id).build(session).obj
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)], firm_id=firm_id)
        payment = create_payment(invoice)
        export = payment.exports[self.export_type]
        session.flush()

        with mock.patch('butils.rest_client.API.__new__', self._rest_api_new):
            self._set_mock()
            with mock.patch('balance.processors.cash_register.replace_market_inn_start_date',
                            mock.Mock(return_value=change_start_dt)):
                output = cash_register.process_bo_payment(export)

        message_id, response_text = re.findall(r'Receipt email id is (\d+), ds_url is (.+)', output)[0]
        assert response_text == self.darkspirit_response.text

        message = session.query(mapper.EmailMessage).getone(message_id)
        subject, body, sender, attachments = pickle.loads(message.data)
        assert message.recepient_name == invoice.person.name
        assert subject == u'Электронный чек'
        assert body == self.receipt_render_response.text

        receipt_params_match = {
            'receipt_content': hamcrest.has_entries(
                {'firm_inn': session.query(mapper.Firm).get(cst.FirmId.YANDEX_OOO).inn if inn_changes else firm.inn,
                 'rows': hamcrest.has_length(1)}),
            'trace': hamcrest.has_entries({'service_id': cst.ServiceId.DIRECT})
        }
        hamcrest.assert_that(
            self.whitespirit_api.receipts.post.call_args[0][0],
            hamcrest.has_entries(receipt_params_match)
        )

        assert len(payment.fiscal_receipts) == 1
        fiscal_receipt = payment.fiscal_receipts[-1]
        assert fiscal_receipt.receipt_fd == '%010d' % int(self.receipt_data['id'])
        assert fiscal_receipt.receipt_fpd == '%010d' % int(self.receipt_data['fp'])
        assert fiscal_receipt.receipt_fn == '%016d' % int(self.receipt_data['fn']['sn'])

        assert export.input is None

    def test_whitespirit_fail(self, session, create_tvm_client_mock, tvm_client_mock):
        # при падении на whitespirit
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)])
        payment = create_payment(invoice)
        export = payment.exports[self.export_type]

        with mock.patch('butils.rest_client.API.__new__', self._rest_api_new):
            self._set_mock()
            self.whitespirit_mock.post.side_effect = Exception('Boom!')
            hamcrest.assert_that(
                hamcrest.calling(cash_register.process_bo_payment).with_args(export),
                hamcrest.raises(Exception, 'Boom!'),
            )
            assert export.output is None
            assert export.input is None
            assert len(payment.fiscal_receipts) == 0

            create_tvm_client_mock.assert_called_with(cst.TVMToolAliases.YB_MEDIUM)
            tvm_client_mock.assert_has_calls([
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.WHITESPIRIT)
            ])

    def test_darkspirit_fail(self, session, create_tvm_client_mock, tvm_client_mock):
        # падение на darkspirit
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)])
        payment = create_payment(invoice)
        export = payment.exports[self.export_type]

        self.darkspirit_response = mock.MagicMock(status_code=500)

        with mock.patch('butils.rest_client.API.__new__', self._rest_api_new):
            self._set_mock()
            with pytest.raises(cash_register.FiscalAPIException):
                cash_register.process_bo_payment(export)
                assert export.output is None
                hamcrest.assert_that(export.input, hamcrest.has_entries(self.receipt_data))
                assert len(payment.fiscal_receipts) == 1

            create_tvm_client_mock.assert_called_with(cst.TVMToolAliases.YB_MEDIUM)
            tvm_client_mock.assert_has_calls([
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.WHITESPIRIT),
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.CHECK_RENDERER),
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.DARKSPIRIT)
            ])

    def test_create_receipt_fail(self, session):
        firm = self.firm_b.build(session).obj
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)])
        payment = create_payment(invoice)
        export = payment.exports[self.export_type]

        export.input = None
        fiscal_receipt = mapper.FiscalReceipt(fiscal_receipt=self.receipt_data)
        payment.fiscal_receipts.append(fiscal_receipt)
        session.flush()

        hamcrest.assert_that(
            hamcrest.calling(cash_register.create_receipt).with_args(export, firm, payment),
            hamcrest.raises(
                cash_register.FiscalReceiptExistsException,
                u'Check has been registered previously with fiscal_receipt_id=%s' % fiscal_receipt.id
            ),
        )

    def test_yamoney_unmoderate(self, session, create_tvm_client_mock, tvm_client_mock):
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)])
        invoice.invoice_orders[0].order.unmoderated = 1
        payment = ob.YandexMoneyPaymentBuilder(invoice=invoice).build(invoice.session).obj
        payment.turn_on()

        export = payment.exports[self.export_type]
        output = cash_register.process_bo_payment(export)
        assert output == 'Without payment'

        invoice.invoice_orders[0].order.unmoderated = 0
        payment.mark_paid()

        with mock.patch('butils.rest_client.API.__new__', self._rest_api_new):
            self._set_mock()
            output = cash_register.process_bo_payment(export)

            assert re.match(r'Receipt email id is (\d+), ds_url is (.+)', output) is not None

            create_tvm_client_mock.assert_called_with(cst.TVMToolAliases.YB_MEDIUM)
            tvm_client_mock.assert_has_calls([
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.WHITESPIRIT),
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.CHECK_RENDERER),
                mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.DARKSPIRIT)
            ])


    def test_ur_allowed_to_get_receipts(self, session, create_tvm_client_mock, tvm_client_mock):
        invoice = create_invoice(session, [(cst.DIRECT_PRODUCT_ID, 1)], person_type='ur', paysys_id=1033)
        payment = create_payment(invoice)
        export = payment.exports[self.export_type]
        session.flush()

        with mock.patch('butils.rest_client.API.__new__', self._rest_api_new):
            self._set_mock()
            output = cash_register.process_bo_payment(export)

        message_id, response_text = re.findall(r'Receipt email id is (\d+), ds_url is (.+)', output)[0]
        assert response_text == self.darkspirit_response.text

        message = session.query(mapper.EmailMessage).getone(message_id)
        subject, body, sender, attachments = pickle.loads(message.data)
        assert message.recepient_name == invoice.person.name
        assert subject == u'Электронный чек'
        assert body == self.receipt_render_response.text

        create_tvm_client_mock.assert_called_with(cst.TVMToolAliases.YB_MEDIUM)
        tvm_client_mock.assert_has_calls([
            mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.WHITESPIRIT),
            mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.CHECK_RENDERER),
            mock.call.get_service_ticket_for(alias=cst.TVMToolAliases.DARKSPIRIT)
        ])

    @pytest.mark.parametrize(
        'firm_id, curr_dt, inn_changes', [
            [cst.FirmId.MARKET, datetime.datetime(2021, 8, 31), False],
            [cst.FirmId.MARKET, datetime.datetime(2021, 9, 1), True],
            [cst.FirmId.FOOD, datetime.datetime(2021, 8, 31), False],
            [cst.FirmId.FOOD, datetime.datetime(2021, 9, 1), False],
        ],
        ids=['market-before', 'market-after', 'not-market-before', 'not-market-after']
    )
    def test_change_firm_inn_market(self, session, firm_id, curr_dt, inn_changes):
        yandex_ooo = session.query(mapper.Firm).get(cst.FirmId.YANDEX_OOO)
        firm = session.query(mapper.Firm).get(firm_id)

        with mock.patch('datetime.datetime', mock.Mock(now=mock.Mock(return_value=curr_dt))):
            inn = cash_register.get_firm_inn(firm)
        assert inn == yandex_ooo.inn if inn_changes else firm.inn


class TestThirdPartyPayment(TestThirdpartyTransactions):
    def test_third_party_payment(self, session):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=22, currency=840, person_type='eu_yt')
        payment = self.create_trust_payment(
            service_id=self.sid,
            thirdparty_service=thirdparty_service,
            contract=contract,
            currency='USD',
        )
        transaction = ob.ThirdPartyTransactionBuilder.construct(
            session,
            contract=contract,
            payment_id=payment.id,
            service_id=payment.billing_service_id,
            internal=0,
            amount=100,
        )
        json_rows = [{
            'id': transaction.id,
            'fiscal_title': 'spi',
            'quantity': 100,
            'fiscal_nds': 20,
        }]
        payment.payment_rows = json_rows
        payment.terminal.firm = ob.Getter(mapper.Firm, cst.FirmId.TAXI).build(session).obj
        session.flush()

        config_ = collections.namedtuple('Config', ['email'])
        output = cash_register.create_fiscal_receipt_params_bs_returns(payment,
                                                                       config_(email='no-email@yandex-team.ru'))

        hamcrest.assert_that(
            output,
            hamcrest.has_entries({
                'trace': hamcrest.has_entries({'service_id': payment.billing_service_id, 'scheme': 'bs'}),
                'receipt_content': hamcrest.has_entries({
                    'additional_user_requisite': hamcrest.has_entries(
                        {'name': 'balance_payment_id', 'value': str(payment.id)}),
                    'rows': hamcrest.contains(
                        hamcrest.has_entries({
                            'text': 'spi',
                            'price': '1.00',
                            'payment_type_type': 'full_prepayment_wo_delivery',
                            'tax_type': 20,
                            'qty': '100.000',
                        }),
                    ),
                    'agent_type': 'none_agent',
                    'payments': hamcrest.contains(
                        hamcrest.has_entries({
                            'amount': '100.00',
                            'payment_type': 'card',
                        }),
                    ),
                    'firm_url': None,
                    'firm_reply_email': 'info@check.yandex.ru',
                    'firm_inn': payment.terminal.firm.inn,
                    'taxation_type': 'OSN',
                    'client_email_or_phone': 'no-email@yandex-team.ru',
                    'receipt_type': 'return_income',
                }),
            }),
        )
