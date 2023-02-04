# -*- coding: utf-8 -*-
import datetime
import pytest

from balance.mapper import Product
from balance.actions.unified_account import UnifiedAccountRelations
from balance.constants import (
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
    UAChildType,
    ServiceId,
    ServiceUAType,
    OrderLogTariffState,
)
from tests import object_builder as ob
from balance import exc

NOW = datetime.datetime.now()
HOUR_AFTER = NOW + datetime.timedelta(hours=1)
HOUR_BEFORE = NOW - datetime.timedelta(hours=1)
CODE_SUCCESS = 0


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(**attrs).build(session).obj


def order(session, client, parent_group_order=None, **attrs):
    return ob.OrderBuilder(client=client, group_order_id=parent_group_order and parent_group_order.id, **attrs).build(
        session).obj


@pytest.fixture
def product(session, **attrs):
    return ob.ProductBuilder(**attrs).build(session).obj


@pytest.fixture
def service_order_id():
    return ob.get_big_number()


def calculate_consumption(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


def generate_act(session, invoice):
    invoice.generate_act(force=True)
    session.flush()
    session.expire_all()


def create_invoice(session, order, qty):
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=order.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty)])
        )
    ).build(session).obj
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    return invoice


def test_check_can_link(session, client):
    # простая проверка возможности объединить заказы
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    assert UnifiedAccountRelations().can_link(parent_order=parent_order, orders=[child_order])


def test_link_success(session, client):
    # простое объединение заказов
    child_order = order(session, client=client)
    parent_order = order(session, client=client)
    UnifiedAccountRelations().link(parent_order=parent_order, orders=[child_order])
    assert child_order.parent_group_order == parent_order
    assert child_order.root_group_order == parent_order
    assert child_order.group_order_id == parent_order.id


@pytest.mark.parametrize('product_ids', [(DIRECT_PRODUCT_ID, DIRECT_PRODUCT_RUB_ID),
                                         (DIRECT_PRODUCT_RUB_ID, DIRECT_PRODUCT_ID)])
def test_check_child_ua_type_optimize_direct_rub_products(session, client, product_ids):
    # заказы под главным оптимизированным заказом с рублевым и фишечным продуктами, работают как оптимизированные
    child_order = order(session, client=client)
    child_order.product = ob.Getter(Product, product_ids[0]).build(session).obj
    parent_order = order(session, client=client)
    parent_order.is_ua_optimize = 1
    parent_order.service_code = product_ids[1]
    UnifiedAccountRelations().link(parent_order=parent_order, orders=[child_order])
    assert child_order.child_ua_type == UAChildType.OPTIMIZED


def test_check_child_ua_type_non_optimize_equal_products(session, client, product):
    # заказы под главным неоптимизированным заказом с одинаковыми продуктами, работают как оптимизированные
    child_order = order(session, client=client, product=product)
    parent_order = order(session, client=client, product=product)
    parent_order.is_ua_optimize = 0
    UnifiedAccountRelations().link(parent_order=parent_order, orders=[child_order])
    assert child_order.child_ua_type == UAChildType.TRANSFERS


def test_check_child_ua_type_optimize_equal_products(session, client, product):
    # заказы под главным оптимизированным заказом с одинаковыми продуктами, работают как оптимизированные
    child_order = order(session, client=client, product=product)
    parent_order = order(session, client=client, product=product)
    parent_order.is_ua_optimize = 1
    UnifiedAccountRelations().link(parent_order=parent_order, orders=[child_order])
    assert child_order.child_ua_type == UAChildType.OPTIMIZED


def test_check_child_ua_type_optimize_non_equal_products(session, client):
    # заказы под главным оптимизированным заказом с разными продуктами, работают как обычный ЕС
    child_order = order(session, client=client)
    parent_order = order(session, client=client)
    parent_order.is_ua_optimize = 1
    UnifiedAccountRelations().link(parent_order=parent_order, orders=[child_order])
    assert child_order.child_ua_type == UAChildType.TRANSFERS


def test_unlink_check_child_ua_type(session, client):
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    UnifiedAccountRelations().unlink([child_order])
    assert child_order.child_ua_type is None


def test_move_to_optimized(session, client):
    # переход под оптимизированный ЕС
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    new_parent_order = order(session, client=client)
    new_parent_order.is_ua_optimize = 1
    UnifiedAccountRelations().link(parent_order=new_parent_order, orders=[child_order])
    assert child_order.main_order is False
    assert parent_order.main_order is False
    assert new_parent_order.main_order is True
    assert child_order.parent_group_order == new_parent_order
    assert child_order.root_group_order == new_parent_order
    assert child_order.group_order_id == new_parent_order.id


def test_check_child_ua_type(session, client):
    single_order = order(session, client=client)
    assert single_order.child_ua_type is None


def test_link_success_main_orders_value_check(session, client):
    # объединяем два заказа, проверям, что верно проставились main_order
    child_order = order(session, client=client)
    parent_order = order(session, client=client)
    UnifiedAccountRelations().link(parent_order=parent_order, orders=[child_order])
    assert child_order.main_order is False
    assert parent_order.main_order is True


def test_link_success_order_tree_set_new_parent(session, client):
    # к главному заказу добавляем главный заказ, проверяем, что он перестал считать главным
    parent_order = order(session, client=client)
    order(session, client=client, parent_group_order=parent_order)
    parent_parent_order = order(session, client=client)
    UnifiedAccountRelations().link(parent_order=parent_parent_order, orders=[parent_order])
    assert parent_order.main_order is False


def test_link_success_main_order_when_parent_has_parent(session, client):
    # к дочернему заказу добавляем дочерний заказ, проверяем, что он не стал считаться главным
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    child_child_order = order(session, client=client)
    UnifiedAccountRelations().link(parent_order=child_order, orders=[child_child_order])
    assert child_order.main_order is False


def test_check_can_link_group_order_is_optimized_link_to_same(session, client):
    # прилинковать уже прилинкованный оптимизированный заказ можно
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    parent_order.is_ua_optimize = 1
    assert UnifiedAccountRelations().can_link(parent_order=parent_order,
                                              orders=[child_order])


def test_check_can_link_group_order_is_optimized_change_level(session, client):
    # заказам под оптимизированным ЕС можно менять уровень вложенности
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    child_child_order = order(session, client=client, parent_group_order=child_order)
    parent_order.is_ua_optimize = 1
    assert UnifiedAccountRelations().can_link(parent_order=parent_order,
                                              orders=[child_child_order])


def test_check_can_link_old_group_order_is_optimized(session, client):
    # если прежний главный заказ был оптимизированным, назначить новый главный заказ нельзя
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    parent_order.is_ua_optimize = 1
    new_parent_order = order(session, client=client)
    with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
        UnifiedAccountRelations().can_link(parent_order=new_parent_order, orders=[child_order])
    error_msg = 'Unified account link failed: cannot unlink order from optimized ua,' \
                ' p_order: {parent_order}, orders: {child_order}'.format(parent_order=new_parent_order,
                                                                         child_order=[child_order])
    assert exc_info.value.msg == error_msg


def test_check_can_link_old_group_group_order_is_optimized(session, client):
    # если главный заказ прежнего главного заказа был оптимизированным, назначить новый главный заказ нельзя
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    child_child_order = order(session, client=client, parent_group_order=child_order)
    parent_order.is_ua_optimize = 1
    new_parent_order = order(session, client=client)
    with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
        UnifiedAccountRelations().can_link(parent_order=new_parent_order, orders=[child_child_order])
    error_msg = 'Unified account link failed: cannot unlink order from optimized ua,' \
                ' p_order: {parent_order}, orders: {child_order}'.format(parent_order=new_parent_order,
                                                                         child_order=[child_child_order])
    assert exc_info.value.msg == error_msg


def test_check_can_link_3_lvl_tree(session, client):
    # только два уровня дочерних заказов разрешены
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    child_child_order = order(session, client=client, parent_group_order=child_order)
    new_child_3_lvl_order = order(session, client=client)
    with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
        UnifiedAccountRelations().can_link(parent_order=child_child_order, orders=[new_child_3_lvl_order])
    error_msg = 'Unified account link failed: p_order.group_order_id more 2 lvl,' \
                ' p_order: {parent_order}, orders: {child_order}'.format(parent_order=child_child_order,
                                                                         child_order=[new_child_3_lvl_order])
    assert exc_info.value.msg == error_msg


def test_check_can_link_wrong_service(session, client):
    # заказы не разрешенных сервисов объединять нельзя
    parent_direct_order = order(session, client=client)
    wrong_service_order = order(session, client=client, service_id=ServiceId.MARKET_PARTNERS)
    with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
        UnifiedAccountRelations().can_link(parent_order=parent_direct_order, orders=[wrong_service_order])
    error_msg = 'Unified account link failed: bad service_id for unified account,' \
                ' p_order: {parent_order}, orders: {child_order}'.format(parent_order=parent_direct_order,
                                                                         child_order=[wrong_service_order])
    assert exc_info.value.msg == error_msg


@pytest.mark.parametrize('unified_account_type', [None, ServiceUAType.enqueue, ServiceUAType.manual])
def test_check_can_link_unified_account_type(session, client, unified_account_type):
    # заказы не разрешенного сервиса объединять нельзя
    service = ob.ServiceBuilder().build(session).obj
    service.balance_service.unified_account_type = unified_account_type

    parent_order = order(session, client=client, service=service)
    child_order = order(session, client=client, service=service)

    if unified_account_type:
        UnifiedAccountRelations().link(parent_order=parent_order, orders=[child_order])
        assert child_order.group_order_id == parent_order.id
    else:
        with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
            UnifiedAccountRelations().can_link(parent_order=parent_order, orders=[child_order])
        error_msg = 'Unified account link failed: bad service_id for unified account,' \
                    ' p_order: {parent_order}, orders: {child_order}'.format(parent_order=parent_order,
                                                                             child_order=[child_order])
        assert exc_info.value.msg == error_msg
        assert child_order.group_order_id is None


def test_check_can_link_several_clients(session):
    # заказы разных клиентов объединять нельзя
    client_ = client(session)
    another_client = client(session)
    parent_order = order(session, client=client_)
    child_order_another_client = order(session, client=another_client)
    with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
        UnifiedAccountRelations().can_link(parent_order=parent_order, orders=[child_order_another_client])
    error_msg = 'Unified account link failed: several clients in orders,' \
                ' p_order: {parent_order}, orders: {child_order}'.format(parent_order=parent_order,
                                                                         child_order=[child_order_another_client])
    assert exc_info.value.msg == error_msg


def test_check_can_link_several_root_client(session):
    # прямые заказы клиентов и заказы под агенством объединять нельзя
    client_ = client(session, is_agency=0)
    child_order_another_client = order(session, client=client_)
    agency = client(session, is_agency=1)
    parent_order = order(session, client=client_, agency=agency)
    with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
        UnifiedAccountRelations().can_link(parent_order=parent_order, orders=[child_order_another_client])
    error_msg = 'Unified account link failed: several root_class in orders,' \
                ' p_order: {parent_order}, orders: {child_order}'.format(parent_order=parent_order,
                                                                         child_order=[child_order_another_client])
    assert exc_info.value.msg == error_msg


def test_check_root_group_order(session, client):
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    child_child_order = order(session, client=client, parent_group_order=child_order)
    assert child_child_order.root_group_order == parent_order


def test_unlink(session, client):
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    UnifiedAccountRelations().unlink([child_order])
    assert child_order.group_order_id is None
    assert child_order.parent_group_order is None
    assert child_order.root_group_order is None

    assert parent_order.main_order is False
    assert child_order.main_order is False


def test_unlink_partial(session, client):
    parent_order = order(session, client=client)
    child_order_1 = order(session, client=client, parent_group_order=parent_order)
    child_order_2 = order(session, client=client, parent_group_order=parent_order)
    UnifiedAccountRelations().unlink([child_order_1])
    assert child_order_1.group_order_id is None
    assert child_order_1.parent_group_order is None
    assert child_order_1.root_group_order is None

    assert child_order_2.group_order_id == parent_order.id
    assert child_order_2.parent_group_order == parent_order
    assert child_order_2.root_group_order == parent_order

    assert parent_order.main_order == 1


def test_check_can_unlink_old_group_order_is_optimized(session, client):
    # если главный заказ оптимизированный, оторвать заказ нельзя
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    parent_order.is_ua_optimize = 1
    with pytest.raises(exc.UA_UNLINK_IMPOSSIBLE) as exc_info:
        UnifiedAccountRelations().can_unlink(orders=[child_order], strict=True)
    error_msg = 'Unified account unlink failed: cannot unlink order from optimized ua,' \
                ' orders: {child_order}'.format(child_order=[child_order])
    assert exc_info.value.msg == error_msg


@pytest.mark.log_tariff
@pytest.mark.parametrize(
    'log_tariff_state, child_log_tariff_state, child_ua_type, type_, msg_key',
    [
        pytest.param(OrderLogTariffState.OFF, OrderLogTariffState.MIGRATED, None, None, None, id='off'),
        pytest.param(OrderLogTariffState.OFF, OrderLogTariffState.MIGRATED, None, 'is_completed', None, id='off_overcompleted'),
        pytest.param(OrderLogTariffState.INIT, OrderLogTariffState.MIGRATED, None, None, 'migrating', id='init'),
        pytest.param(OrderLogTariffState.INIT, OrderLogTariffState.MIGRATED, None, 'is_completed', 'migrating', id='init_overcompleted'),
        pytest.param(OrderLogTariffState.MIGRATED, OrderLogTariffState.MIGRATED, None, None, None, id='migrated'),
        pytest.param(OrderLogTariffState.MIGRATED, OrderLogTariffState.OFF, None, 'is_completed', "overcompletion", id='migrated_overcompleted'),
        pytest.param(OrderLogTariffState.MIGRATED, OrderLogTariffState.OFF, None, 'active_consumes', "wrong_child", id='old_transport_child'),
        pytest.param(OrderLogTariffState.MIGRATED, OrderLogTariffState.OFF, UAChildType.OPTIMIZED, 'active_consumes', "wrong_child", id='old_transport_child_2'),
        pytest.param(OrderLogTariffState.MIGRATED, OrderLogTariffState.OFF, UAChildType.LOG_TARIFF, None, None, id='off_w_child'),
        pytest.param(OrderLogTariffState.MIGRATED, OrderLogTariffState.OFF, None, 'archived', None, id='archived_consumes'),
        pytest.param(OrderLogTariffState.MIGRATED, OrderLogTariffState.OFF, None, 'active_consumes', "wrong_child", id='active_consumes'),
    ]
)
def test_check_can_link_log_tariff(session, client, log_tariff_state, child_log_tariff_state, child_ua_type, type_, msg_key):
    msgs = {
        'overcompletion': "linking order with overcompletion to main order with new tariffication",
        'migrating': 'main order is migrating to new tariffication',
        'wrong_child': 'linking old transport order to main order with new tariffication',
    }

    main_order = order(session, client, _is_log_tariff=log_tariff_state)
    child_order = order(session, client, _is_log_tariff=child_log_tariff_state, child_ua_type=child_ua_type)

    if type_:
        if type_ == 'is_completed':
            child_order.consume_qty = 10
            child_order.completion_qty = 11
            session.flush()
        elif type_ == 'archived':
            inv = create_invoice(session, child_order, 10)
            calculate_consumption(child_order, 10)
            generate_act(session, inv)
        elif type_ == 'active_consumes':
            inv = create_invoice(session, child_order, 10)
            calculate_consumption(child_order, 10)

    def _check():
        return UnifiedAccountRelations().can_link(main_order, [child_order], strict=True)

    if msg_key:
        with pytest.raises(exc.UA_LINK_IMPOSSIBLE) as exc_info:
            _check()

        assert msgs[msg_key] in exc_info.value.msg
    else:
        assert _check() is True


def test_link_inside_tariffication(session, client):
    main_order = order(session, client, _is_log_tariff=OrderLogTariffState.MIGRATED, is_ua_optimize=1)
    child_order = order(session, client, _is_log_tariff=OrderLogTariffState.MIGRATED, is_ua_optimize=1, deny_shipment=1)

    UnifiedAccountRelations().link(parent_order=main_order, orders=[child_order])
    assert child_order.root_group_order == main_order
    assert child_order._is_log_tariff == OrderLogTariffState.OFF
    assert child_order.is_ua_optimize == 0
    assert child_order.deny_shipment == 1
