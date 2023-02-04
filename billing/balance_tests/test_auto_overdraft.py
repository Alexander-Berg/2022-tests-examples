# -*- coding: utf-8 -*-
import datetime
import collections
import decimal

import pytest
import hamcrest

from balance import constants as cst
from balance import mapper
from balance import muzzle_util as ut
from balance.actions import single_account
from balance.actions.process_completions import ProcessCompletions
from balance.processors.auto_overdraft import auto_overdraft_process

import tests.object_builder as ob
from balance.constants import ExportState, OrderLogTariffState
from balance.processors.month_proc import handle_client

pytestmark = [
    pytest.mark.auto_overdraft,
]

D = decimal.Decimal
SERVICE_ID = cst.ServiceId.DIRECT
PAYMENT_METHOD_CC = 'bank'
PAYSYS_ID = 1002  # Кредитной картой
ACT_MONTH = mapper.ActMonth()
OVERDRAFT_LIMIT = 10000
FIRM_ID = cst.FirmId.YANDEX_OOO
CLIENT_OV_LIMIT = 10000
SENTINEL = object()


@pytest.fixture(autouse=True)
def patch_config(request, session):
    session.config.__dict__['TRUNCATE_AUTO_OVERDRAFT'] = getattr(request, 'param', 1)
    session.config.__dict__['USE_SERVICE_LIMIT_WO_TAX'] = getattr(request, 'param', 1)
    session.config.__dict__['AUTO_OVERDRAFT_THRESHOLDS'] = {}


def get_client(session, region_id=None, convert_type=SENTINEL, iso_currency='RUB', with_single_account=False):
    client = ob.ClientBuilder(
        name='test auto overdraft {:%d.%m.%Y}'.format(datetime.datetime.now()),
        dt=ACT_MONTH.begin_dt,
        region_id=region_id,
        with_single_account=with_single_account
    ).build(session).obj

    if convert_type is not SENTINEL:
        client.set_currency(SERVICE_ID, iso_currency, ACT_MONTH.begin_dt, convert_type)
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, OVERDRAFT_LIMIT, iso_currency)
    session.flush()

    return client


def get_person(session, client, person_type='ph'):
    return ob.PersonBuilder(client=client, type=person_type, dt=ACT_MONTH.begin_dt).build(session).obj


def get_overdraft_params(session, client, person, **overdraft_params):
    client_limit = overdraft_params.get('client_limit', CLIENT_OV_LIMIT)
    payment_method = overdraft_params.get('payment_method', PAYMENT_METHOD_CC)
    service_id = overdraft_params.get('service_id', SERVICE_ID)

    overdraft_params = ob.OverdraftParamsBuilder(
        client=client,
        person=person,
        service_id=service_id,
        payment_method_cc=payment_method,
        iso_currency=client.currency_on(),
        client_limit=client_limit,
    ).build(session).obj
    session.add(overdraft_params)
    session.flush()
    return overdraft_params


def create_orders(session, client, product_ids=None, service_id=None):
    if not product_ids:
        product_ids = [cst.DIRECT_PRODUCT_RUB_ID]
    if not service_id:
        service_id = SERVICE_ID

    main_product_id = max(product_ids)
    main_order = ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, main_product_id),
        service_id=service_id,
    ).build(session).obj

    main_order.enqueue('UA_TRANSFER')
    main_order.exports['UA_TRANSFER'].export_dt = datetime.datetime.now()

    orders = []
    for idx, product_id in enumerate(product_ids):
        product = ob.Getter(mapper.Product, product_id)
        order_dt = ACT_MONTH.end_dt - datetime.timedelta(days=7 * (idx + 1))
        orders.append(
            ob.OrderBuilder(
                client=client,
                product=product,
                service_id=service_id,
                group_order_id=main_order.id,
                dt=order_dt,
            ).build(session).obj
        )
    session.flush()
    return orders


def create_invoice(session, on_dt, person, orders, overdraft=0, paysys_id=PAYSYS_ID, invoice_type=None,
                   single_account_number=None):
    invoice = ob.InvoiceBuilder(
        dt=on_dt,
        person=person,
        paysys=ob.Getter(mapper.Paysys, paysys_id),
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=person.client,
                rows=[
                    ob.BasketItemBuilder(order=order, quantity=qty)
                    for order, qty in orders
                ]
            )
        ),
        overdraft=overdraft,
        type=invoice_type,
        single_account_number=single_account_number
    ).build(session).obj
    if single_account_number:
        invoice.charge_invoice.turn_on_rows(ref_invoice=invoice, cut_agava=True)
        invoice.charge_invoice.turn_on_dt = on_dt
        session.flush()
    else:
        invoice.turn_on_rows(cut_agava=True)
        invoice.turn_on_dt = on_dt
        session.flush()
    return invoice


def create_consumed_orders(session, client, person, orders_info, invoice_params=None, need_act=False, service_id=None):
    product_ids = [order_param[0] for order_param in orders_info]
    cur_qtys = [order_param[1] for order_param in orders_info]
    comp_qtys = [order_param[2] for order_param in orders_info]

    orders = create_orders(session, client, product_ids, service_id)

    if any(cur_qtys):
        on_dt = ACT_MONTH.document_dt - datetime.timedelta(1)
        invoice = create_invoice(session, on_dt, person, zip(orders, cur_qtys), **(invoice_params or {}))
        for o, comp_qty in zip(orders, comp_qtys):
            o.calculate_consumption(ACT_MONTH.document_dt, {o.shipment_type: comp_qty})
        if need_act:
            invoice.generate_act(force=1, backdate=on_dt)
    else:
        for o, comp_qty in zip(orders, comp_qtys):
            o.calculate_consumption(ACT_MONTH.document_dt, {o.shipment_type: comp_qty})

    session.flush()

    return orders


@pytest.mark.parametrize(
    'params',
    [
        dict(description='full_money',  # перекручиваем меньше лимита
             convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 45)],
             invoice_params=None,
             req_qtys=[10, 20, 30],
             req_amount=60),
        dict(description='up_to_limit_money',  # перекручиваем до лимита
             convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=D('34.76'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, D('21.49')),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, D('28.27'))],
             invoice_params=None,
             req_qtys=[D('10'), D('11.49'), D('13.27')],
             req_amount=D('34.76')),
        dict(description='trunc_money',  # перекручиваем сверх лимита
             convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=D('23.45'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 45)],
             invoice_params=dict(overdraft=0),
             req_qtys=[D('3.91'), D('7.82'), D('11.72')],
             req_amount=D('23.45')),
        dict(description='trunc_fish',  # перекручиваем сверх лимита при наличии фишечного заказа
             convert_type=cst.CONVERT_TYPE_MODIFY,
             overdraft_limit=D('666'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 100, 200),
                     (cst.DIRECT_PRODUCT_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_RUB_ID, 150, 300)],
             invoice_params=None,
             req_qtys=[D('78.35'), D('15.670667'), D('117.53')],
             req_amount=D('666')),
        dict(description='trunc_w_debt',  # перекручиваем меньше лимита, но больше, чем лимит минус долг
             convert_type=cst.CONVERT_TYPE_MODIFY,
             overdraft_limit=D('1000'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 100, 900),
                     (cst.DIRECT_PRODUCT_ID, 6, 11)],
             invoice_params=dict(overdraft=1),
             req_qtys=[D('606.32'), D('3.789333')],
             req_amount=D('720')),
    ],
    ids=lambda x: x['description']
)
def test_process(params, session):
    client = get_client(session, convert_type=params['convert_type'])
    person = get_person(session, client)
    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])
    orders = create_consumed_orders(session, client, person, params['orders'], params['invoice_params'])

    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert orders == [co.order for co in invoice.consumes]
    assert [co.act_qty for co in invoice.consumes] == params['req_qtys']
    assert [co.completion_qty for co in invoice.consumes] == params['req_qtys']
    assert 'is_truncated' in operation.memo


@pytest.mark.parametrize(
    'params',
    [
        dict(convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 45)],
             invoice_params=dict(invoice_type='charge_note'),
             req_qtys=[10, 20, 30],
             req_amount=60)
    ]
)
def test_single_account_process(params, session):
    client = get_client(session, convert_type=params['convert_type'], with_single_account=True)
    person = get_person(session, client)
    single_account.prepare.process_client(client)
    params['invoice_params']['single_account_number'] = client.single_account_number

    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])
    orders = create_consumed_orders(session, client, person, params['orders'], params['invoice_params'])

    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert orders == [co.order for co in invoice.consumes]
    assert [co.act_qty for co in invoice.consumes] == params['req_qtys']
    assert [co.completion_qty for co in invoice.consumes] == params['req_qtys']
    assert 'is_truncated' in operation.memo


@pytest.mark.parametrize(
    'params',
    [
        dict(description='trunc_byn',
             iso_currency='BYN',
             person_type='byp',
             orders=[(cst.DIRECT_PRODUCT_QUASI_BYN_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_QUASI_BYN_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_QUASI_BYN_ID, 15, 45)],
             invoice_params=dict(paysys_id=1102),
             req_amount=D('48')),
        dict(description='trunc_kzp',
             iso_currency='KZT',
             person_type='kzp',
             orders=[(cst.DIRECT_PRODUCT_QUASI_KZT_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_QUASI_KZT_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_QUASI_KZT_ID, 15, 45)],
             invoice_params=dict(paysys_id=1121),
             req_amount=D('44.80')),
    ],
    ids=lambda x: x['description']
)
def test_quasi_currency(params, session):
    overdraft_limit = D('40')
    req_qtys = [D('6.666667'), D('13.333333'), D('20.00')]

    client = get_client(session, convert_type=cst.CONVERT_TYPE_COPY, iso_currency=params['iso_currency'])
    person = get_person(session, client, params['person_type'])
    overdraft_params = get_overdraft_params(session, client, person, client_limit=overdraft_limit)
    orders = create_consumed_orders(session, client, person, params['orders'], params['invoice_params'])

    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert [co.order for co in invoice.consumes] == orders
    assert [co.act_qty for co in invoice.consumes] == req_qtys
    assert [co.completion_qty for co in invoice.consumes] == req_qtys
    assert [co.act_qty for co in invoice.consumes] == req_qtys
    assert 'is_truncated' in operation.memo
    assert 'limit_wo_tax' in operation.memo


@pytest.mark.parametrize(
    'params',
    [
        dict(description='undership_no_thresholds',
             thresholds={},
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 10),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 15)],
             req_amount=None),
        dict(description='undership_zero_threshold',
             thresholds={'RUB': 0},
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 10),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 15)],
             req_amount=None
             ),
        dict(description='undership_nonzero_threshold',
             thresholds={'RUB': 1},
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 10),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 15)],
             req_amount=None),
        dict(description='undership_other_threshold',
             thresholds={'KZT': 10},
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 10),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 15)],
             req_amount=None),
        dict(description='overship_other_threshold_up_to_limit_money',
             thresholds={'KZT': 2},
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 6),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 12),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 18)],
             req_amount=6),
        dict(description='overship_other_threshold_trunc_money',
             thresholds={'KZT': 2},
             overdraft_limit=4,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 6),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 12),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 18)],
             req_amount=4),
        dict(description='overship_below_nonzero_threshold_up_to_limit_money',
             thresholds={'RUB': 1},
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5.3),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 10.3),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 15.3)],
             req_amount=None),
        dict(description='overship_below_nonzero_threshold_trunc_money',
             thresholds={'RUB': 1},
             overdraft_limit=0.5,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5.3),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 10.3),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 15.3)],
             req_amount=None),
        dict(description='overship_above_nonzero_threshold_up_to_limit_money',
             thresholds={'RUB': 1},
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5.3),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 10.3),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 15.4)],
             req_amount=1),
        dict(description='overship_above_nonzero_threshold_trunc_money',
             thresholds={'RUB': 2},
             overdraft_limit=5,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 6),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 12),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 18)],
             req_amount=5)
    ],
    ids=lambda x: x['description']
)
def test_process_with_thresholds(params, session):
    session.config.__dict__['AUTO_OVERDRAFT_THRESHOLDS'] = params['thresholds']

    client = get_client(session, convert_type=cst.CONVERT_TYPE_COPY)
    person = get_person(session, client)
    create_consumed_orders(session, client, person, params['orders'])
    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])

    if params['req_amount']:
        invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
        assert invoice.type == 'overdraft'
        assert invoice.is_auto_overdraft is True
        assert invoice.total_sum == params['req_amount']
        assert invoice.total_act_sum == params['req_amount']
        assert len(invoice.acts) == 1
    else:
        assert auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt) is None


def test_no_overshipment(session):
    orders_params = [(cst.DIRECT_PRODUCT_RUB_ID, 100, 100), ]

    client = get_client(session, convert_type=None)
    person = get_person(session, client)
    overdraft_params = get_overdraft_params(session, client, person)
    order, = create_consumed_orders(session, client, person, orders_params)

    # перекрут в далеком будущем
    order.calculate_consumption(order.shipment.dt + datetime.timedelta(100), {order.shipment_type: 666})
    session.flush()

    assert auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt) is None


def test_partial_overshipment(session):
    orders_params = [(cst.DIRECT_PRODUCT_RUB_ID, 100, 100),
                     (cst.DIRECT_PRODUCT_RUB_ID, 100, 120), ]

    client = get_client(session, convert_type=None)
    person = get_person(session, client)
    overdraft_params = get_overdraft_params(session, client, person)
    order1, order2 = create_consumed_orders(session, client, person, orders_params)

    # перекрут в далеком будущем
    order1.calculate_consumption(order1.shipment.dt + datetime.timedelta(100), {order1.shipment_type: 666})
    order2.calculate_consumption(order2.shipment.dt + datetime.timedelta(100), {order2.shipment_type: 666})
    session.flush()

    res_invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    res_consumes = [(q.order, q.current_qty, q.completion_qty, q.act_qty) for q in res_invoice.consumes]
    res_invoice_orders = [(io.order, io.quantity, io.amount) for io in res_invoice.invoice_orders]
    assert res_invoice.total_sum == 20
    assert res_consumes == [(order2, 20, 20, 20)]
    assert res_invoice_orders == [(order2, 20, 20)]


@pytest.mark.parametrize(
    'params',
    [
        dict(description='bank',
             invoice_params=dict(overdraft=0),
             overdraft_params=dict(client_limit=666666, payment_method='bank'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 0, 666666)],
             req_payment_method='bank'),
        dict(description='card',
             overdraft_params=dict(client_limit=666666, payment_method='card'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 0, 666)],
             req_payment_method='card'),
        dict(description='unavailable_card',
             overdraft_params=dict(client_limit=666666, payment_method='card'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 0, 666666)],
             req_payment_method='bank'),
        dict(description='unavailable_card',
             overdraft_params=dict(client_limit=666666, payment_method='yamoney_wallet'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 0, 666666)],
             req_payment_method='bank')
    ],
    ids=lambda x: x['description']
)
def test_payment_methods(params, session):
    client = get_client(session, convert_type=cst.CONVERT_TYPE_COPY)
    person = get_person(session, client)
    overdraft_params = get_overdraft_params(session, client, person, **params['overdraft_params'])
    create_consumed_orders(session, client, person, params['orders'])

    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    assert invoice.payment_method.cc == params['req_payment_method']
    assert invoice.iso_currency == 'RUB'


@pytest.mark.parametrize(
    'params',
    [
        dict(description='different',
             overdraft_params=dict(client_limit=666666),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 0, 10),
                     (cst.DIRECT_MEDIA_PRODUCT_RUB_ID, 0, 15), ],
             req_commission_type=cst.DIRECT_COMMISSION_TYPE,
             req_discount_type=cst.DIRECT_DISCOUNT_TYPE),
        dict(description='only_media',
             overdraft_params=dict(client_limit=666666),
             orders=[(cst.DIRECT_MEDIA_PRODUCT_RUB_ID, 0, 10),
                     (cst.DIRECT_MEDIA_PRODUCT_RUB_ID, 0, 15), ],
             req_commission_type=cst.DIRECT_MEDIA_COMMISSION_TYPE,
             req_discount_type=cst.DIRECT_DISCOUNT_TYPE),
        dict(description='discount_types',
             overdraft_params=dict(client_limit=666666),
             orders=[(cst.DIRECT_MEDIA_PRODUCT_RUB_ID, 0, 10),
                     (cst.DIRECT_PRIVATE_MARKETPLACE_PRODUCT_RUB_ID, 0, 15), ],
             req_commission_type=cst.DIRECT_MEDIA_COMMISSION_TYPE,
             req_discount_type=cst.DIRECT_DISCOUNT_TYPE),
        dict(description='discount_types_card',
             overdraft_params=dict(client_limit=666666, payment_method='card'),
             orders=[(cst.DIRECT_MEDIA_PRODUCT_RUB_ID, 0, 10),
                     (cst.DIRECT_PRIVATE_MARKETPLACE_PRODUCT_RUB_ID, 0, 15), ],
             req_commission_type=cst.DIRECT_MEDIA_COMMISSION_TYPE,
             req_discount_type=cst.DIRECT_DISCOUNT_TYPE),
        dict(description='different',
             overdraft_params=dict(client_limit=666666, payment_method='card'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 0, 10),
                     (cst.DIRECT_PRODUCT_RUB_ID, 0, 15),
                     (cst.DIRECT_MEDIA_PRODUCT_RUB_ID, 0, 20),
                     (cst.DIRECT_PRIVATE_MARKETPLACE_PRODUCT_RUB_ID, 0, 25), ],
             req_commission_type=cst.DIRECT_COMMISSION_TYPE,
             req_discount_type=cst.DIRECT_DISCOUNT_TYPE),
    ],
    ids=lambda x: x['description']
)
def test_commission_discount_types(params, session):
    client = get_client(session, convert_type=cst.CONVERT_TYPE_COPY)
    person = get_person(session, client)
    overdraft_params = get_overdraft_params(session, client, person, **params['overdraft_params'])
    create_consumed_orders(session, client, person, params['orders'])
    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.commission_type == params['req_commission_type']
    assert invoice.discount_type == params['req_discount_type']


@pytest.mark.parametrize(
    'params',
    [
        dict(description='full_money',  # перекручиваем до лимита
             convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=50,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 35)],
             invoice_params=None,
             req_qtys=[10, 20, 20],
             req_amount=50),
    ],
    ids=lambda x: x['description']
)
def test_process_after_act(params, session):
    client = get_client(session, convert_type=params['convert_type'])
    person = get_person(session, client)
    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])
    orders = create_consumed_orders(session, client, person, params['orders'], params['invoice_params'], need_act=True)

    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert [co.order for co in invoice.consumes] == orders
    assert [co.act_qty for co in invoice.consumes] == params['req_qtys']
    assert [co.completion_qty for co in invoice.consumes] == params['req_qtys']
    assert 'is_truncated' in operation.memo


@pytest.mark.parametrize(
    'params',
    [
        dict(description='full_money',  # перекручиваем до лимита
             convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=50,
             orders_direct=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                            (cst.DIRECT_PRODUCT_RUB_ID, 10, 30),
                            (cst.DIRECT_PRODUCT_RUB_ID, 15, 35)],
             orders_market=[(cst.MARKET_FISH_PRODUCT_ID, 5, 6),
                            (cst.MARKET_FISH_PRODUCT_ID, 10, 11),
                            (cst.MARKET_FISH_PRODUCT_ID, 15, 16)],
             req_qtys=[10, 20, 20],
             req_amount=50),
    ],
    ids=lambda x: x['description']
)
def test_another_service_overshipment(params, session):
    client = get_client(session, convert_type=params['convert_type'])
    person = get_person(session, client)
    orders_direct = create_consumed_orders(session, client, person, params['orders_direct'])

    create_consumed_orders(session, client, person, params['orders_market'], service_id=cst.ServiceId.MARKET)

    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])
    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert [co.order for co in invoice.consumes] == orders_direct
    assert [co.act_qty for co in invoice.consumes] == params['req_qtys']
    assert [co.completion_qty for co in invoice.consumes] == params['req_qtys']
    assert 'is_truncated' in operation.memo


@pytest.mark.parametrize(
    'params',
    [
        dict(description='full_money',  # перекручиваем до лимита
             convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=50,
             orders_overshipment=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                                  (cst.DIRECT_PRODUCT_RUB_ID, 10, 30),
                                  (cst.DIRECT_PRODUCT_RUB_ID, 15, 35)],
             orders_not_overshipment=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 5),
                                      (cst.DIRECT_PRODUCT_RUB_ID, 10, 9),
                                      (cst.DIRECT_PRODUCT_RUB_ID, 15, 0)],
             req_qtys=[10, 20, 20],
             req_amount=50),
    ],
    ids=lambda x: x['description']
)
def test_unused_qty(params, session):
    u'''
    Проверяем, что перекрут по заказу не покрыт свободными средствами с другого заказа
    '''
    client = get_client(session, convert_type=params['convert_type'])
    person = get_person(session, client)

    orders_direct = create_consumed_orders(session, client, person, params['orders_overshipment'])

    create_consumed_orders(session, client, person, params['orders_not_overshipment'])

    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])
    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert [co.order for co in invoice.consumes] == orders_direct
    assert [co.act_qty for co in invoice.consumes] == params['req_qtys']
    assert [co.completion_qty for co in invoice.consumes] == params['req_qtys']
    assert 'is_truncated' in operation.memo


@pytest.mark.parametrize(
    'params',
    [
        dict(description='full_money',  # перекручиваем меньше лимита
             convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=100,
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 15),
                     (cst.DIRECT_PRODUCT_RUB_ID, 10, 30),
                     (cst.DIRECT_PRODUCT_RUB_ID, 15, 45)],
             invoice_params=None,
             req_qtys=[10, 20, 30],
             req_amount=60),
        dict(description='trunc_w_debt',  # перекручиваем меньше лимита, но больше, чем лимит минус долг
             convert_type=cst.CONVERT_TYPE_MODIFY,
             overdraft_limit=D('1000'),
             orders=[(cst.DIRECT_PRODUCT_RUB_ID, 100, 900),
                     (cst.DIRECT_PRODUCT_ID, 6, 11)],
             invoice_params=dict(overdraft=1),
             req_qtys=[D('606.32'), D('3.789333')],
             req_amount=D('720')),
    ],
    ids=lambda x: x['description']
)
def test_process_with_ban(params, session):
    client = get_client(session, convert_type=params['convert_type'])
    person = get_person(session, client)
    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])
    orders = create_consumed_orders(session, client, person, params['orders'], params['invoice_params'])

    client.overdraft_ban = 1
    session.flush()

    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert [co.order for co in invoice.consumes] == orders
    assert [co.act_qty for co in invoice.consumes] == params['req_qtys']
    assert [co.completion_qty for co in invoice.consumes] == params['req_qtys']
    assert 'is_truncated' in operation.memo


@pytest.mark.parametrize(
    'params',
    [
        dict(convert_type=cst.CONVERT_TYPE_COPY,
             overdraft_limit=50,
             orders_direct=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 10),
                            (cst.DIRECT_PRODUCT_RUB_ID, 10, 25),
                            (cst.DIRECT_PRODUCT_RUB_ID, 15, 30)],
             orders_direct_log_tariff=[(cst.DIRECT_PRODUCT_RUB_ID, 5, 10),
                                       (cst.DIRECT_PRODUCT_RUB_ID, 10, 15),
                                       (cst.DIRECT_PRODUCT_RUB_ID, 15, 20)],
             req_qtys=[5, 15, 15],
             req_amount=35),
    ]
)
def test_log_tariff_orders(params, session):
    client = get_client(session, convert_type=params['convert_type'])
    person = get_person(session, client)
    orders_direct = create_consumed_orders(session, client, person, params['orders_direct'])

    orders_direct_log_tariff = create_consumed_orders(session, client, person, params['orders_direct_log_tariff'])

    for order in orders_direct_log_tariff:
        order._is_log_tariff = OrderLogTariffState.MIGRATED
    session.flush()

    overdraft_params = get_overdraft_params(session, client, person, client_limit=params['overdraft_limit'])
    invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
    operation, = invoice.operations

    assert invoice.type == 'overdraft'
    assert invoice.is_auto_overdraft is True
    assert invoice.total_sum == params['req_amount']
    assert invoice.total_act_sum == params['req_amount']
    assert len(invoice.acts) == 1
    assert [co.order for co in invoice.consumes] == orders_direct
    assert [co.act_qty for co in invoice.consumes] == params['req_qtys']
    assert [co.completion_qty for co in invoice.consumes] == params['req_qtys']
    assert 'is_truncated' in operation.memo


class TestReconsume(object):
    def test_partial(self, session):
        orders_params = [(cst.DIRECT_PRODUCT_RUB_ID, 0, 100),
                         (cst.DIRECT_PRODUCT_RUB_ID, 0, 120), ]

        client = get_client(session, convert_type=None)
        person = get_person(session, client)
        overdraft_params = get_overdraft_params(session, client, person)

        order1, order2 = create_consumed_orders(session, client, person, orders_params)

        invoice1 = create_invoice(session, datetime.datetime.now(), person, [(order1, 30), (order2, 36)])
        invoice2 = create_invoice(session, datetime.datetime.now(), person, [(order1, 66)])
        ProcessCompletions(order1).process_completions()  # Разбор откруток на конзюмы
        ProcessCompletions(order2).process_completions()
        session.flush()

        res_invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)

        hamcrest.assert_that(
            res_invoice,
            hamcrest.has_properties(
                consume_sum=220,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order1,
                        current_qty=100,
                        completion_qty=100
                    ),
                    hamcrest.has_properties(
                        order=order2,
                        current_qty=120,
                        completion_qty=120
                    )
                ),
            )
        )
        hamcrest.assert_that(
            invoice1,
            hamcrest.has_properties(
                consume_sum=66,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order1,
                        current_qty=0,
                        completion_qty=0
                    ),
                    hamcrest.has_properties(
                        order=order2,
                        current_qty=0,
                        completion_qty=0
                    ),
                    hamcrest.has_properties(
                        order=order1,
                        current_qty=30,
                        completion_qty=0
                    ),
                    hamcrest.has_properties(
                        order=order2,
                        current_qty=36,
                        completion_qty=0
                    )
                ),
                reverses=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order1,
                        reverse_qty=30
                    ),
                    hamcrest.has_properties(
                        order=order2,
                        reverse_qty=36
                    ),
                )
            )
        )
        hamcrest.assert_that(
            invoice2,
            hamcrest.has_properties(
                consume_sum=66,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order1,
                        current_qty=0,
                        completion_qty=0
                    ),
                    hamcrest.has_properties(
                        order=order1,
                        current_qty=66,
                        completion_qty=0
                    ),
                ),
                reverses=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order1,
                        reverse_qty=66
                    ),
                )
            )
        )

    def test_full(self, session):
        orders_params = [(cst.DIRECT_PRODUCT_RUB_ID, 0, 666), ]

        client = get_client(session, convert_type=None)
        person = get_person(session, client)
        overdraft_params = get_overdraft_params(session, client, person)

        order, = create_consumed_orders(session, client, person, orders_params)

        invoice = create_invoice(session, datetime.datetime.now(), person, [(order, 700)])
        ProcessCompletions(order).process_completions()
        session.flush()

        res_invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)

        hamcrest.assert_that(
            res_invoice,
            hamcrest.has_properties(
                consume_sum=666,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order,
                        current_qty=666,
                        completion_qty=666
                    ),
                ),
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                consume_sum=700,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order,
                        current_qty=0,
                        completion_qty=0
                    ),
                    hamcrest.has_properties(
                        order=order,
                        current_qty=700,
                        completion_qty=0
                    ),
                ),
                reverses=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order,
                        reverse_qty=700
                    ),
                )
            )
        )

    def test_services(self, session):
        orders_params = [(cst.DIRECT_PRODUCT_RUB_ID, 0, 36), ]

        client = get_client(session, convert_type=None)
        person = get_person(session, client)
        overdraft_params = get_overdraft_params(session, client, person)

        order_ok, = create_consumed_orders(session, client, person, orders_params)

        order_other = ob.OrderBuilder(
            client=client,
            product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID),
            service_id=cst.ServiceId.MEDIA_SELLING,
            dt=order_ok.dt,
        ).build(session).obj
        order_other.calculate_consumption(order_ok.shipment_dt, {order_other.shipment_type: 30})

        invoice = create_invoice(session, datetime.datetime.now(), person, [(order_ok, 100), (order_other, 100)])
        ProcessCompletions(order_ok).process_completions()
        ProcessCompletions(order_other).process_completions()
        session.flush()

        res_invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)

        hamcrest.assert_that(
            res_invoice,
            hamcrest.has_properties(
                consume_sum=36,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order_ok,
                        current_qty=36,
                        completion_qty=36
                    ),
                ),
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                consume_sum=200,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order_ok,
                        current_qty=0,
                        completion_qty=0
                    ),
                    hamcrest.has_properties(
                        order=order_ok,
                        current_qty=100,
                        completion_qty=0
                    ),
                    hamcrest.has_properties(
                        order=order_other,
                        current_qty=100,
                        completion_qty=30
                    ),
                ),
                reverses=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order_ok,
                        reverse_qty=100
                    ),
                )
            )
        )

    def test_no_completions(self, session):
        client = get_client(session, convert_type=None)
        person = get_person(session, client)
        overdraft_params = get_overdraft_params(session, client, person)

        order, = create_orders(session, client, [cst.DIRECT_PRODUCT_RUB_ID])
        invoice = create_invoice(session, datetime.datetime.now(), person, [(order, 700)])

        res_invoice = auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)

        assert res_invoice is None
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                consume_sum=700,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order,
                        current_qty=700,
                        completion_qty=0
                    ),
                ),
                reverses=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=order,
                        reverse_qty=700
                    ),
                    hamcrest.has_properties(
                        order=order,
                        reverse_qty=-700
                    ),
                )
            )
        )

    def test_acted(self, session):
        orders_params = [(cst.DIRECT_PRODUCT_RUB_ID, 0, 666), ]

        client = get_client(session, convert_type=None)
        person = get_person(session, client)
        overdraft_params = get_overdraft_params(session, client, person)

        order, = create_consumed_orders(session, client, person, orders_params)

        invoice = create_invoice(session, datetime.datetime.now(), person, [(order, 700)])
        ProcessCompletions(order).process_completions()
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        session.flush()

        with pytest.raises(Exception) as exc_info:
            auto_overdraft_process(overdraft_params, ACT_MONTH.document_dt)
        expected_msg = "Invalid parameter for function: Invoice {} that is to be withdrawn has acts".format(
            invoice.id)
        assert exc_info.value.msg == expected_msg
        assert exc_info.typename == "INVALID_PARAM"

    def test_export_failed_before_act(self, session):
        orders_params = [(cst.DIRECT_PRODUCT_RUB_ID, 0, 666), ]

        client = get_client(session, convert_type=None)
        person = get_person(session, client)
        overdraft_params = get_overdraft_params(session, client, person)

        order, = create_consumed_orders(session, client, person, orders_params)

        create_invoice(session, datetime.datetime.now(), person, [(order, 700)])
        ProcessCompletions(order).process_completions()
        session.flush()

        overdraft_params.enqueue('AUTO_OVERDRAFT')
        overdraft_params.exports.get('AUTO_OVERDRAFT').state = ExportState.failed
        session.flush()

        client.enqueue('MONTH_PROC')
        client.exports['MONTH_PROC'].input = {}

        with pytest.raises(Exception) as exc_info:
            handle_client(client, {})
        expected_msg = "Auto overdraft is not processed for params id {}".format(overdraft_params.id)
        assert exc_info.value.msg == expected_msg
        assert exc_info.typename == "AUTO_OVERDRAFT_NOT_PROCESSED"


@pytest.mark.taxes_update
class TestTaxUpdate(object):

    def test_on_change_dt(self, session):
        past = datetime.datetime(2000, 1, 1)
        present = ut.trunc_date(datetime.datetime.now())

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[
                (past, 18),
                (present, 20)
            ]
        ).build(session).obj

        tpp1, tpp2 = tax_policy.taxes

        product = ob.ProductBuilder(
            taxes=tax_policy,
            prices=[
                (past, 'RUR', 30, tpp1),
                (present, 'RUR', 30, tpp2),
            ]
        ).build(session).obj

        client = get_client(session, convert_type=None)

        person = get_person(session, client)
        overdraft_params = get_overdraft_params(session, client, person)
        orders = create_orders(session, client, [product.id] * 3)
        for order in orders:
            order.calculate_consumption(order.dt, {order.shipment_type: 6})
            session.flush()

        invoice = auto_overdraft_process(overdraft_params, datetime.datetime.now() - datetime.timedelta(1))
        assert invoice.consume_sum == 540
        assert invoice.tax_policy_pct == tpp1
        assert {co.tax_policy_pct for co in invoice.consumes} == {tpp1}
