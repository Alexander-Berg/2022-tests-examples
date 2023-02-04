# -*- coding: utf-8 -*-
import datetime
import pytest
from decimal import Decimal as D
import mock

from butils.decimal_unit import DecimalUnit as DU

from balance import exc, mapper
from balance import constants as cst
from balance.actions.cart import Cart

from tests import object_builder as ob

pytestmark = [pytest.mark.cart]

NOW = datetime.datetime.now()
QTY = D('12345.67')
UNIT = 'QTY'
SERVICE_ID = cst.ServiceId.DIRECT  # BALANCE-30931: пока корзина только для одного сервиса


def create_client(session, passport, els=True):
    params = {'service': SERVICE_ID}
    if els:
        params['single_account_number'] = ob.get_big_number()
    client = ob.ClientBuilder.construct(session, **params)
    passport.link_to_client(client)
    return client


def create_orders(session, client, count=1, agency=None):
    return [
        ob.OrderBuilder(
            client=client,
            service=ob.Getter(mapper.Service, SERVICE_ID),
            product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_ID),
            agency=agency
        ).build(session).obj
        for _i in range(count)
    ]


@pytest.fixture
def passport(session):
    return ob.PassportBuilder().build(session).obj


@pytest.fixture
def client(session, passport):
    return create_client(session, passport)


@pytest.fixture
def order(session, client):
    return create_orders(session, client)[0]


@pytest.fixture
def cart_item(session, client, order):
    return ob.CartItemBuilder(
        client=client,
        order=order,
    ).build(session).obj


@pytest.fixture(name='cart')
def create_cart(session, client):
    return Cart(client)


class TestCartItemMapper(object):

    @pytest.mark.parametrize(
        'use_object',
        [True, False],
        ids=['by order', 'by serice_id and service_order_id'],
    )
    def test_create_cart_item(self, session, client, order, use_object):
        cart_item_params = {
            'client_id': client.id,
            'quantity': QTY,
        }
        if use_object:
            cart_item_params['order'] = order
        else:
            cart_item_params['service_id'] = order.service_id
            cart_item_params['service_order_id'] = order.service_order_id

        cart_item = mapper.CartItem(**cart_item_params)
        session.add(cart_item)
        session.flush()

        assert cart_item.client == client
        assert cart_item.service == order.service
        assert cart_item.order == order
        assert cart_item.quantity == DU(QTY, UNIT)


class TestCartAction(object):

    def test_add_new(self, session, cart, order):
        assert cart.items_query.count() == 0
        cart_item = cart.add(order, QTY)
        session.flush()

        assert cart.items_query.all() == [cart_item]
        assert cart_item.order == order
        assert cart_item.quantity == DU(QTY, UNIT)

    def test_add_existing_cart_item(self, session, cart, order, cart_item):
        assert cart.items_query.all() == [cart_item]
        old_qty = cart_item.quantity
        new_qty = D('1.98')
        cart_item = cart.add(order, new_qty, old_qty)
        session.flush()

        assert cart.items_query.all() == [cart_item]
        assert cart_item.order == order
        assert cart_item.quantity == old_qty + new_qty

    def test_add_existing_cart_item_fail(self, client, cart, order, cart_item):
        assert cart.items_query.all() == [cart_item]
        with pytest.raises(exc.STALE_CART_STATE) as e:
            cart.add(order, QTY)

        assert e.value.client_id == client.id
        assert e.value.filter_params == {
            'client_id': client.id,
            'quantity': QTY,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'payload': None,
        }

    @pytest.mark.parametrize('qty', [D('0'), D('-12.23')])
    def test_add_invalid_qty(self, cart, order, cart_item, qty):
        assert cart.items_query.all() == [cart_item]
        with pytest.raises(exc.INVALID_PARAM) as e:
            cart.add(order, qty)
        assert e.value.msg == 'Invalid parameter for function: Allowed only cart_item.quantity > 0 in cart'

    def test_add_order_denied_wrong_client(self, session, cart):
        another_client = ob.ClientBuilder().build(session).obj
        another_order = create_orders(session, another_client)[0]

        with pytest.raises(exc.CART_ORDER_PERMISSION_DENIED):
            cart.add(another_order, QTY)

    def test_add_agency_order(self, session):
        agency = ob.ClientBuilder(is_agency=1).build(session).obj
        cart = create_cart(session, agency)
        another_client = ob.ClientBuilder().build(session).obj
        order = create_orders(session, another_client, agency=agency)[0]
        cart.add(order, QTY)

    def test_items(self, session, client, cart):
        order_count = 3
        orders = create_orders(session, client, order_count)

        cart_items = []
        for i, order in enumerate(orders):
            cart_items.append(cart.add(order, QTY * (i + 1)))
        session.flush()

        assert sorted(cart.items_query.all()) == sorted(cart_items)

    def test_update(self, session, cart, cart_item):
        assert cart.items_query.all() == [cart_item]
        old_qty = cart_item.quantity
        new_qty = D('1.98')
        cart_item = cart.update(cart_item.id, new_qty, old_qty)
        session.flush()
        assert cart_item.quantity == new_qty

    def test_update_fail(self, cart, cart_item):
        assert cart.items_query.all() == [cart_item]
        wrong_old_qty = cart_item.quantity + D('1')
        new_qty = D('1.98')
        with pytest.raises(exc.STALE_CART_STATE):
            cart.update(cart_item.id, new_qty, wrong_old_qty)

    def test_delete_cart_item(self, cart, cart_item):
        cart.delete([cart_item.id])
        assert cart.items_query.all() == []

    def test_delete_wrong_cart_item(self, session, cart, cart_item):
        wrong_client = ob.ClientBuilder().build(session).obj
        wrong_cart_item = ob.CartItemBuilder(client=wrong_client).build(session).obj
        cart.delete([wrong_cart_item.id])

        assert cart.items_query.all() == [cart_item]  # корзина осталась цена
        assert wrong_client.cart_items_query.all() == [wrong_cart_item]  # чужой заказ тоже не удален

    def test_create_empty_request(self, session, passport, client, cart):
        with pytest.raises(exc.EMPTY_CART):
            cart.create_request(passport.passport_id)

    @pytest.mark.parametrize('count_items', [2, 5])
    @mock.patch('balance.constants.ORACLE_MAX_IN_CONDITION_ENTRIES', 3)
    def test_create_request_orders(self, session, passport, client, cart, count_items):
        count = 5
        orders = [ob.OrderBuilder(client=client) for _i in range(count)]
        cart_items = [
            ob.CartItemBuilder(client=client, order=o).build(session).obj
            for o in orders
        ]
        cart_order_ids = [i.order.id for i in cart_items]

        cart_item_ids = [ci.id for ci in cart_items][:count_items] if count_items else None
        request = cart.create_request(passport.passport_id, cart_item_ids)
        session.flush()

        assert request.client_id == client.id
        assert len(request.basket().rows) == count_items or count
        assert cart.items_query.count() == count - (count_items or count)

        basket_order_ids = [row.order.id for row in request.basket().rows]
        assert sorted(basket_order_ids) == sorted(cart_order_ids[:count_items or count])

    @pytest.mark.parametrize('count_items', [None, 2])
    @mock.patch('balance.constants.ORACLE_MAX_IN_CONDITION_ENTRIES', 3)
    def test_create_request_invoices(self, session, passport, client, cart, count_items):
        count = 5
        orders = [ob.OrderBuilder(client=client) for _i in range(count)]
        cart_items = [
            ob.CartItemBuilder(client=client, order=o).build(session).obj
            for o in orders
        ]
        cart_order_ids = [i.order.id for i in cart_items]
        invoice = ob.InvoiceBuilder.construct(
            session,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=client
                )
            )
        )

        cart_item_ids = [ci.id for ci in cart_items][:count_items] if count_items else None
        request = cart.create_request(passport.passport_id, cart_item_ids, [invoice.id])
        session.flush()

        assert request.client_id == client.id
        assert len(request.rows) == (count_items or 0)
        assert {r.ref_invoice for r in request.register_rows} == {invoice}
        assert cart.items_query.count() == count - (count_items or 0)

        basket_order_ids = {row.order.id for row in request.rows}
        assert basket_order_ids == (set(cart_order_ids[:count_items]) if count_items else set())

    @pytest.mark.parametrize('deny_promocode', [None, False, True])
    @pytest.mark.parametrize('service_promocode', [None, False, True])
    def test_create_request_promocode_flags(self, session, passport, client, cart, deny_promocode, service_promocode):
        count = 5
        orders = [ob.OrderBuilder(client=client) for _i in range(count)]
        payload = {}
        if deny_promocode is not None:
            payload['deny_promocode'] = deny_promocode
        if service_promocode is not None:
            payload['service_promocode'] = service_promocode
        cart_items = [
            ob.CartItemBuilder(
                client=client,
                order=o,
                payload=payload or None
            ).build(session).obj
            for o in orders
        ]

        request = cart.create_request(passport.passport_id, [ci.id for ci in cart_items])
        session.flush()

        assert request.deny_promocode is bool(deny_promocode)
        assert request.service_promocode is bool(service_promocode)

    def test_filter_by_service(self, session):
        # BALANCE-30931: только один сервис
        client = create_client(session, session.passport, False)
        services = [SERVICE_ID, cst.ServiceId.MARKET]
        orders = [
            ob.OrderBuilder(
                client=client,
                service=ob.Getter(mapper.Service, service),
            ).build(session).obj
            for service in services
        ]
        items = [
            ob.CartItemBuilder(
                client=client,
                order=order,
            ).build(session).obj
            for order in orders
        ]
        session.flush()

        cart = Cart(client, SERVICE_ID)
        assert cart.items_query.all() == [item for item in items if item.service_id == SERVICE_ID]

    @pytest.mark.single_account
    @pytest.mark.parametrize(
        'is_ok',
        [True, False],
        ids=['is_ok', 'is_not_ok'],
    )
    def test_check_request(self, session, passport, client, cart, is_ok):
        service1 = ob.ServiceBuilder().build(session).obj
        service2 = ob.ServiceBuilder().build(session).obj

        pay_policy_id_1 = ob.create_pay_policy_service(
            session, service1.id, cst.FirmId.YANDEX_OOO,
            paymethods_params=[('RUB', cst.PaymentMethodIDs.bank)]
        )
        ob.create_pay_policy_region(session, pay_policy_id_1, cst.RegionId.RUSSIA)
        pay_policy_id_2 = ob.create_pay_policy_service(
            session, service2.id, cst.FirmId.YANDEX_OOO,
            paymethods_params=[('RUB', cst.PaymentMethodIDs.bank)]
        )
        ob.create_pay_policy_region(session, pay_policy_id_2, cst.RegionId.RUSSIA)

        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT
        client.region_id = cst.RegionId.RUSSIA
        if is_ok:
            session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = {service1.id, service2.id}
        else:
            session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = {service1.id}

        orders = [
            ob.OrderBuilder(client=client, service_id=sid).build(session).obj
            for sid in [service1.id, service2.id]
        ]
        cart_items = [
            ob.CartItemBuilder(client=client, order=o).build(session).obj
            for o in orders
        ]
        cart_item_ids = [ci.id for ci in cart_items]

        assert cart.check_request(passport.passport_id, cart_item_ids) is is_ok

    def test_check_request_amount(self, session, passport, client, cart):
        order = ob.OrderBuilder(client=client).build(session).obj
        item = ob.CartItemBuilder.construct(
            session,
            client=client,
            order=order,
            quantity=6666666666
        )

        patch_amount_check = mock.patch('balance.mapper.invoices.Invoice.is_amount_too_big', return_value=True)
        patch_limit = mock.patch('balance.mapper.invoices.Paysys.payment_limit', return_value=666)
        patch_flag = mock.patch('balance.mapper.common.Paysys.limit_amount_at_paystep', True)
        with patch_amount_check, patch_limit, patch_flag:
            assert cart.check_request(passport.passport_id, [item.id]) is True
