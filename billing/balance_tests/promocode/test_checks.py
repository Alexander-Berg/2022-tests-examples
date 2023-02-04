# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import mock
import pytest
from hamcrest import assert_that, raises, calling

from balance import exc
from balance import mapper
from balance import constants as cst
from balance.actions import promocodes
from balance.actions.promocodes.checker import (
    PromoCodeChecker,
    check_on_client_binding,
    check_on_reservation,
)

from tests import object_builder as ob
from tests.balance_tests.promocode.common import (
    NOW,
    create_promocode,
    create_client,
    reserve_promocode,
    create_order,
    create_invoice,
)

pytestmark = [
    pytest.mark.promo_code,
]


def test_client_agency(session, promocode, agency):
    reserve_promocode(session, promocode, agency)

    with pytest.raises(exc.INVALID_PC_NON_DIRECT_CLIENT) as exc_info:
        promocodes.check_promo_code_client(promocode, agency)

    assert 'ID_PC_NON_DIRECT_CLIENT' in exc_info.value.msg


def test_active_promocode(session, promocode, client):
    promocode.group.start_dt = NOW + datetime.timedelta(days=1)
    session.flush()
    session.expire_all()

    with pytest.raises(exc.INVALID_PC_ACTIVE_PERIOD) as exc_info:
        promocodes.check_promo_code_client(promocode, client)

    assert exc_info.value.msg == 'Invalid promo code: ID_PC_INVALID_PERIOD'


def test_reserved_by_client(session, promocode, client):
    client2 = ob.ClientBuilder().build(session).obj
    reserve_promocode(session, promocode, client)

    with pytest.raises(exc.INVALID_PC_RESERVED_ON_ANOTHER_CLIENT) as exc_info:
        promocodes.check_promo_code_client(promocode, client2)

    assert 'ID_PC_RESERVED_ON_ANOTHER_CLIENT' in exc_info.value.msg


def test_is_global_unique(session, promocode, client):
    promocode.group.is_global_unique = True
    invoice = ob.InvoiceBuilder().build(session).obj
    invoice.promo_code = promocode
    session.flush()
    session.expire_all()

    with pytest.raises(exc.INVALID_PC_ALREADY_USED) as exc_info:
        promocodes.check_promo_code_client(promocode, client)

    assert 'ID_PC_USED' in exc_info.value.msg


def test_service_single(session, promocode, client, invoice):
    promocode.group.service_ids = [cst.ServiceId.GEOCON]
    session.flush()
    session.expire_all()

    with pytest.raises(exc.INVALID_PC_NO_MATCHING_ROWS) as exc_info:
        promocodes.check_promo_code_invoice(promocode, invoice)

    assert 'ID_PC_NO_MATCHING_ROWS' in exc_info.value.msg


def test_service_multiple(session, promocode, client):
    promocode.group.service_ids = [cst.ServiceId.DIRECT]

    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(
                        quantity=1,
                        order=ob.OrderBuilder(client=client, service_id=sid)
                    )
                    for sid in [cst.ServiceId.DIRECT, cst.ServiceId.MKB]
                ]
            )
        ),
    ).build(session).obj
    session.expire_all()

    promocodes.check_promo_code_invoice(promocode, invoice)


def test_service_wo_filter(session, promocode, client):
    promocode.group.service_ids = None
    order = ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, 2136),
        service_id=cst.ServiceId.TELEMED_SRV
    ).build(session).obj
    invoice = create_invoice(session, 10, order.client, [order])
    session.expire_all()

    # проверяем отсутствие дефолтной фильтрации по сервисам
    promocodes.check_promo_code_invoice(promocode, invoice)


def test_products_for_client(session, promocode, client):
    def patched_func(self):
        return {cst.DIRECT_PRODUCT_ID}

    with mock.patch('balance.mapper.promos.PromoCodeGroup.product_ids', property(patched_func)):
        promocodes.check_promo_code_client(promocode, client)


def test_products_for_request(session, promocode, invoice, client):
    def patched_func(self):
        return {cst.DIRECT_PRODUCT_ID}

    with mock.patch('balance.mapper.promos.PromoCodeGroup.product_ids', property(patched_func)):
        with pytest.raises(exc.INVALID_PC_NO_MATCHING_ROWS) as exc_info:
            promocodes.check_promo_code_request(promocode, invoice.request)

        assert 'ID_PC_NO_MATCHING_ROWS' in exc_info.value.msg


def test_products_for_invoice(session, promocode, invoice, client):
    def patched_func(self):
        return {cst.DIRECT_PRODUCT_ID}

    with mock.patch('balance.mapper.promos.PromoCodeGroup.product_ids', property(patched_func)):
        with pytest.raises(exc.INVALID_PC_NO_MATCHING_ROWS) as exc_info:
            promocodes.check_promo_code_invoice(promocode, invoice)

        assert 'ID_PC_NO_MATCHING_ROWS' in exc_info.value.msg


def test_products_multiple_products(session, promocode, client):
    order1 = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)
    order2 = create_order(session, client, cst.DIRECT_PRODUCT_ID)
    basket = ob.BasketBuilder(
        client=client,
        rows=[
            ob.BasketItemBuilder(quantity=1, order=order1),
            ob.BasketItemBuilder(quantity=1, order=order2),
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj

    def patched_func(self):
        return {cst.DIRECT_PRODUCT_ID}

    with mock.patch('balance.mapper.promos.PromoCodeGroup.product_ids', property(patched_func)):
        promocodes.check_promo_code_request(promocode, request)


def test_service_product(session, promocode, client):
    order1 = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)
    order2 = create_order(session, client, cst.DIRECT_PRODUCT_ID)

    order1.service_id = cst.ServiceId.GEOCON
    promocode.group.service_ids = [cst.ServiceId.GEOCON]

    basket = ob.BasketBuilder(
        client=client,
        rows=[
            ob.BasketItemBuilder(quantity=1, order=order1),
            ob.BasketItemBuilder(quantity=1, order=order2),
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj

    def patched_func(self):
        return {cst.DIRECT_PRODUCT_ID}

    with mock.patch('balance.mapper.promos.PromoCodeGroup.product_ids', property(patched_func)):
        with pytest.raises(exc.INVALID_PC_NO_MATCHING_ROWS) as exc_info:
            promocodes.check_promo_code_request(promocode, request)

        assert 'ID_PC_NO_MATCHING_ROWS' in exc_info.value.msg


def test_new_clients_only(session, promocode, client, order):
    promocode.group.new_clients_only = True

    invoice = create_invoice(session, 1000, client, order, person=ob.PersonBuilder(client=client, type='ph'))
    invoice.turn_on_rows()
    invoice.close_invoice(NOW)
    session.flush()
    session.expire_all()

    with pytest.raises(exc.INVALID_PC_NOT_NEW_CLIENT) as exc_info:
        promocodes.check_promo_code_client(promocode, client)

    assert 'ID_PC_NOT_NEW_CLIENT' in exc_info.value.msg


def test_need_unique_urls_client(session, promocode, invoice):
    promocode.group.need_unique_urls = True
    session.flush()
    session.expire_all()

    invoice.promo_code = promocode
    checked_client = create_client(session)

    with mock.patch('balance.mapper.clients.Client._q_client_ids_with_same_urls', return_value=[invoice.client_id]):
        with pytest.raises(exc.INVALID_PC_NOT_UNIQUE_URLS) as exc_info:
            promocodes.check_promo_code_client(promocode, checked_client)

        assert 'ID_PC_NOT_UNIQUE_URLS' in exc_info.value.msg


def test_need_unique_urls_request(session, promocode, client):
    promocode.group.need_unique_urls = True
    request = ob.RequestBuilder().build(session).obj
    request.deny_promocode = True
    session.flush()
    session.expire_all()

    with mock.patch('balance.mapper.invoices.Request.is_promo_deniable', True):
        with pytest.raises(exc.INVALID_PC_NOT_UNIQUE_URLS) as exc_info:
            promocodes.check_promo_code_request(promocode, request)

        assert 'ID_PC_NOT_UNIQUE_URLS' in exc_info.value.msg


def test_on_reservation(session, client):
    promocode1 = create_promocode(session)
    promocode2 = create_promocode(session)
    reserve_promocode(session, promocode1, client)

    with pytest.raises(exc.CANT_RESERVE_PROMOCODE):
        check_on_reservation(promocode2, client)


def test_on_client_binding(session, pc_params):
    target_client = create_client(session)
    bound_client = create_client(session)
    pc_params['promocode_info_list'] = [{'code': 'ABCDEF', 'client_id': bound_client.id}]
    promocode = create_promocode(session, params=pc_params)

    with pytest.raises(exc.PROMOCODE_WRONG_CLIENT):
        check_on_client_binding(promocode, target_client)


def test_promo_code_firm(session, promocode, invoice, client):
    promocode.group.firm_id = 111
    session.flush()
    session.expire_all()

    with pytest.raises(exc.INVALID_PC_FIRM_MISMATCH) as exc_info:
        promocodes.check_promo_code_invoice(promocode, invoice)

    assert 'ID_PC_FIRM_MISMATCH' in exc_info.value.msg


def test_single_account_paysys(session, promocode, client, order):
    paysys = (
        session.query(mapper.Paysys)
        .filter_by(firm_id=1,
                   iso_currency='RUB',
                   category='ph',
                   payment_method_id=cst.PaymentMethodIDs.single_account)
        .first()
    )
    invoice = create_invoice(session, 666, client, order, paysys)

    with pytest.raises(exc.INVALID_PC_SINGLE_ACCOUNT) as exc_info:
        promocodes.check_promo_code_invoice(promocode, invoice)

    assert 'ID_PC_FOR_SINGLE_ACCOUNT' in exc_info.value.msg


@pytest.mark.parametrize('promocode_info, person_attr, resp_field, person_type', [
    ({'code': 'ALD0-1234', 'client_id': None}, 'inn',   'inn', 'ur'),
    ({'code': 'DT',        'client_id': None}, 'inn',   'inn', 'ur'),
    ({'code': 'ALKZ-1234', 'client_id': None}, 'kz_in', 'iin', 'kzu'),
    ({'code': 'TCS3-1234', 'client_id': None}, 'inn',   'inn', 'ur'),
], ids=['alfabank', 'tochka', 'alfabank-kz', 'tinkoff'])
def test_bank(session, promocode_info, person_attr, resp_field, person_type):
    promocode = create_promocode(session=session, params={'promocode_info_list': [promocode_info]})

    paysys = ob.Getter(mapper.Paysys, 1063)  # Банк для юридических лиц, рубли (нерезиденты, Швейцария)
    person = ob.PersonBuilder(type=person_type, **{person_attr: '6666'})
    invoice = ob.InvoiceBuilder(person=person, paysys=paysys, promocode=promocode).build(session).obj
    invoice.turn_on_rows()
    session.flush()

    with mock.patch('requests.get') as mock_get:
        mock_file = mock.Mock()
        mock_file.text = None
        mock_file.json = lambda: {
            'code': promocode_info['code'],
            resp_field: '666',
        }
        mock_get.return_value = mock_file

        with pytest.raises(exc.INVALID_PROMO_CODE_INN):
            promocodes.check_promo_code_invoice(promocode, invoice, True)


def test_alfa_instant_paysys(session):
    """
    Нельзя применять промокод альфабанка, если счёт оплачен
    моментальным способом оплаты.
    """
    pc = create_promocode(session=session, params={'promocode_info_list': [{'code': 'ALD9123', 'client_id': None}]})

    paysys = ob.Getter(mapper.Paysys, 1801002)  # Кредитной картой
    person = ob.PersonBuilder(type='ur')
    invoice = ob.InvoiceBuilder(person=person, paysys=paysys).build(session).obj

    with pytest.raises(exc.INVALID_PC_INSTANT_PAYSYS) as exc_info:
        promocodes.check_promo_code_invoice(pc, invoice, True)

    assert 'INSTANT_PAYSYS' in exc_info.value.msg


@pytest.mark.parametrize(
    'order, invoice, min_currency, min_amount, is_fail',
    [
        (cst.DIRECT_PRODUCT_RUB_ID, {'paysys_cc': 'ur', 'qty': 3000}, 'RUB', D('2500'), False),
        (cst.DIRECT_PRODUCT_RUB_ID, {'paysys_cc': 'ur', 'qty': D('2999.99')}, 'RUB', D('2500'), True),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'ur', 'qty': 100}, 'FISH', 100, False),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'ur', 'qty': D('99.999')}, 'FISH', 100, True),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'ur', 'qty': D('1000')}, 'RUB', D('2542.37'), False),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'ur', 'qty': D('99.999')}, 'RUB', D('2542.37'), True),
        (cst.DIRECT_PRODUCT_RUB_ID, {'paysys_cc': 'rur_wo_nds', 'qty': D('2542.37')}, 'RUB', D('2542.37'), False),
        (cst.DIRECT_PRODUCT_RUB_ID, {'paysys_cc': 'rur_wo_nds', 'qty': D('2542.36')}, 'RUB', D('2542.37'), True),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'rur_wo_nds', 'qty': 100}, 'FISH', 100, False),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'rur_wo_nds', 'qty': D('99.999')}, 'FISH', 100, True),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'rur_wo_nds', 'qty': 100}, 'RUB', D('2500'), False),
        (cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'rur_wo_nds', 'qty': D('99.990')}, 'RUB', D('2500'), True),
    ],
    ['order', 'invoice']
)
def test_minimal_amounts(session, promocode, invoice, min_currency, min_amount, is_fail):
    pc_group = promocode.group
    pc_group.minimal_amounts = {min_currency: min_amount}
    session.flush()

    if is_fail:
        assert_that(
            calling(promocodes.check_promo_code_invoice).with_args(promocode, invoice),
            raises(exc.PROMOCODE_INVALID_MINIMAL_QTY)
        )
    else:
        promocodes.check_promo_code_invoice(promocode, invoice)


@pytest.mark.parametrize(
    'minimal_amount, invoice_qty, service_ids, is_fail',
    [
        pytest.param(100, 60, [cst.ServiceId.DIRECT, cst.ServiceId.DIRECT], False, id='same'),
        pytest.param(100, 60, [cst.ServiceId.DIRECT, cst.ServiceId.MKB], True, id='different_not_enough'),
        pytest.param(100, 120, [cst.ServiceId.DIRECT, cst.ServiceId.MKB], False, id='different_enough'),
    ]
)
def test_service_minimal_amounts(session, client, invoice_qty, service_ids, minimal_amount, is_fail):
    promocode = create_promocode(
        session,
        dict(
            service_ids=[cst.ServiceId.DIRECT],
            minimal_amounts={'RUB': minimal_amount}
        )
    )
    orders = [
        ob.OrderBuilder.construct(
            session,
            client=client,
            product_id=cst.DIRECT_PRODUCT_RUB_ID,
            service_id=service_id
        )
        for service_id in service_ids
    ]
    invoice = create_invoice(session, invoice_qty, client, orders)

    if is_fail:
        assert_that(
            calling(promocodes.check_promo_code_invoice).with_args(promocode, invoice),
            raises(exc.PROMOCODE_INVALID_MINIMAL_QTY)
        )
    else:
        promocodes.check_promo_code_invoice(promocode, invoice)
