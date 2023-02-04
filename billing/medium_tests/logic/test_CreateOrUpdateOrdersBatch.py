# -*- coding: utf-8 -*-

import datetime
import mock

import pytest
import sqlalchemy as sa
import hamcrest

from balance import mapper
from balance.constants import (
    ServiceId,
    ExportState,
    UAChildType,
    DIRECT_PRODUCT_RUB_ID,
    OrderLogTariffState,
)
from tests import object_builder as ob
from tests.tutils import get_exception_code

CODE_SUCCESS = 0
CODE_ERROR = -1


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(**attrs).build(session).obj


@pytest.fixture
def agency(session, **attrs):
    return ob.ClientBuilder(is_agency=1).build(session).obj


@pytest.fixture
def product(session, **attrs):
    return ob.ProductBuilder(**attrs).build(session).obj


@pytest.fixture
def service_order_id():
    return ob.get_big_number()


@pytest.fixture
def service(session, **attrs):
    return ob.ServiceBuilder(**attrs).build(session).obj


@pytest.fixture
def order(session, **attrs):
    return ob.OrderBuilder(**attrs).build(session).obj


@pytest.fixture
def contract(session, **attrs):
    return ob.ContractBuilder(**attrs).build(session).obj


@pytest.fixture
def markup(session, **attrs):
    return ob.MarkupBuilder(**attrs).build(session).obj


@pytest.fixture
def manager(session, **attrs):
    return ob.SingleManagerBuilder(passport_id=ob.get_big_number()).build(session).obj


def mk_currency(client):
    client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)


def test_order_create_xmlrpc(session, xmlrpcserver, client, product, service_order_id, service):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(
        sa.and_(mapper.Order.service_order_id == service_order_id, mapper.Order.service_id == service.id)).all()
    assert len(orders) == 1
    assert orders[0].client == client
    assert orders[0].product == product
    assert orders[0].markups == set([])
    assert orders[0].agency is None
    assert orders[0].contract is None
    assert orders[0].is_ua_optimize == 0
    assert orders[0].is_main_order is None
    assert orders[0].unmoderated_flag is None


def test_order_create_xmlrpc_order_params_set(session, xmlrpcserver, client, product, service_order_id, service):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'ServiceOrderURL': 'https://www.yandex.ru',
                                                       'r': 'r'}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(
        sa.and_(mapper.Order.service_order_id == service_order_id, mapper.Order.service_id == service.id)).all()
    assert len(orders) == 1
    assert orders[0].client == client
    assert orders[0].product == product
    assert orders[0].markups == set([])
    assert orders[0].agency is None
    assert orders[0].contract is None
    assert orders[0].is_ua_optimize == 0
    assert orders[0].is_main_order is None
    assert orders[0].unmoderated is None


def test_order_create_xmlrpc_with_agency(session, xmlrpcserver, client, product, service_order_id, service, agency):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'AgencyID': agency.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(
        sa.and_(mapper.Order.service_order_id == service_order_id, mapper.Order.service_id == service.id)).all()
    assert len(orders) == 1
    assert orders[0].client == client
    assert orders[0].agency == agency


def test_order_create_xmlrpc_with_agency_as_client_and_agency(session, xmlrpcserver, client, product, service_order_id,
                                                              service, agency):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': agency.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'AgencyID': agency.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(
        sa.and_(mapper.Order.service_order_id == service_order_id, mapper.Order.service_id == service.id)).all()
    assert len(orders) == 1
    assert orders[0].client == agency
    assert orders[0].agency == agency


def test_order_create_xmlrpc_with_contract(session, xmlrpcserver, client, product, service_order_id, service, contract):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'ContractID': contract.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(
        sa.and_(mapper.Order.service_order_id == service_order_id, mapper.Order.service_id == service.id)).all()
    assert len(orders) == 1
    assert orders[0].contract == contract


def test_order_create_xmlrpc_with_clid(session, xmlrpcserver, client, product, service_order_id, service):
    clid_value = ob.get_big_number()
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'clid': clid_value}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(
        sa.and_(mapper.Order.service_order_id == service_order_id, mapper.Order.service_id == service.id)).all()
    assert len(orders) == 1
    assert orders[0].clid == clid_value


@pytest.mark.parametrize('extra_order_params', [{'clid': ob.get_big_number()},
                                                {},
                                                {'clid': None}])
def test_order_update_xmlrpc_with_new_clid(session, xmlrpcserver, order, extra_order_params):
    clid_value = ob.get_big_number()
    order.clid = clid_value
    order_params = {'ClientID': order.client.id, 'ProductID': order.product.id,
                    'ServiceOrderID': order.service_order_id,
                    'ServiceID': order.service.id}
    order_params.update(extra_order_params)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [order_params])
    assert len(res) == 1
    assert res[0][0] == CODE_ERROR
    error_msg = 'could not change clid of this order'
    assert get_exception_code(res[0][1], tag_name='msg') == error_msg
    assert order.clid == str(clid_value)


def test_order_update_xmlrpc_with_same_clid(session, xmlrpcserver, order):
    clid_value = ob.get_big_number()
    order.clid = clid_value
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': order.client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id,
                                                       'clid': clid_value}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    assert order.clid == clid_value


def test_order_create_xmlrpc_delete_contract(session, xmlrpcserver, contract, order):
    order.contract = contract
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': order.client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    assert order.contract is None


def test_order_create_xmlrpc_client_instead_of_agency(session, xmlrpcserver, product, service_order_id, service):
    client_ = client(session)
    another_client = client(session)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client_.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'AgencyID': another_client.id}])
    assert len(res) == 1
    assert res[0][0] == CODE_ERROR
    error_msg = 'Invalid parameter for function: agency {client_id} parameter is not agency, but CLIENT'.format(
        client_id=another_client.id)
    assert get_exception_code(res[0][1], tag_name='msg') == error_msg
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == service.id)).all()
    assert len(orders) == 0


def test_order_update_xmlrpc(xmlrpcserver, order):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(None, [{'ClientID': order.client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']


def test_order_create_xmlrpc_invalid_price_factor(xmlrpcserver, client, product, service_order_id, service):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(None, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'price_factor': '0.99'}])
    assert len(res) == 1
    assert res[0][0] == CODE_ERROR
    error_msg = 'Invalid parameter for function: price_factor must be equal or more than 1'
    assert get_exception_code(res[0][1], tag_name='msg') == error_msg


def test_order_create_xmlrpc_non_force_product_change(xmlrpcserver, product, order):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(None, [{'ClientID': order.client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id}])
    assert len(res) == 1
    assert res[0][0] == CODE_ERROR
    error_msg = 'could not change product of this order from {old} to {new}'.format(old=order.product.id,
                                                                                    new=product.id)
    assert get_exception_code(res[0][1], tag_name='msg') == error_msg


def test_update_client_wo_consumes(session, xmlrpcserver, order, client):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    assert order.client == client


def test_delete_agency_wo_consumes(xmlrpcserver, client, agency, order):
    order.agency = agency
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(None, [{'ClientID': client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id, 'AgencyID': None}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    assert order.agency is None


def test_update_agency_wo_consumes(session, xmlrpcserver, client, order):
    agency_ = agency(session)
    another_agency = agency(session)
    order.agency = agency_
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': order.client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id, 'AgencyID': another_agency.id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    assert order.agency == another_agency


def test_order_create_xmlrpc_with_is_main_order(session, xmlrpcserver, client, product, service_order_id):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': ServiceId.MARKET_PARTNERS,
                                                       'is_main_order': True}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == ServiceId.MARKET_PARTNERS)).all()
    assert len(orders) == 1
    assert orders[0].is_main_order is True


def test_order_create_xmlrpc_with_two_main_orders_for_client_in_service(session, xmlrpcserver, order, service_order_id):
    order.service = ob.Getter(mapper.Service, ServiceId.MARKET_PARTNERS).build(session).obj
    order.is_main_order = True
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': order.client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': ServiceId.MARKET_PARTNERS,
                                                       'is_main_order': True}])
    assert len(res) == 1
    assert res[0][0] == CODE_ERROR
    error_msg = 'Invalid parameter for function: Main order already exists for' \
                ' ClientID={client_id} and ServiceID = {service_id}'.format(client_id=order.client.id,
                                                                            service_id=order.service.id)
    assert get_exception_code(res[0][1], tag_name='msg') == error_msg


def test_order_update_xmlrpc_with_two_main_orders_for_client_in_service(session, xmlrpcserver):
    order_ = order(session, service_id=ServiceId.MARKET_PARTNERS)
    order_.is_main_order = True
    another_order = order(session, service_id=ServiceId.MARKET_PARTNERS)
    another_order.is_main_order = False
    another_order.client = order_.client
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': another_order.client.id,
                                                       'ProductID': another_order.product.id,
                                                       'ServiceOrderID': another_order.service_order_id,
                                                       'ServiceID': another_order.service.id,
                                                       'is_main_order': True}])
    assert len(res) == 1
    assert res[0][0] == CODE_ERROR
    error_msg = 'Invalid parameter for function: Main order already exists for' \
                ' ClientID={client_id} and ServiceID = {service_id}'.format(client_id=order_.client.id,
                                                                            service_id=order_.service.id)
    assert get_exception_code(res[0][1], tag_name='msg') == error_msg


def test_order_create_xmlrpc_is_main_order_error(session, xmlrpcserver, client, product, service_order_id):
    service = ob.Getter(mapper.Service, ServiceId.DIRECT).build(session).obj
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id, 'ServiceID': service.id,
                                                       'is_main_order': 1}])
    assert len(res) == 1
    assert res[0][0] == CODE_ERROR
    error_msg = 'Invalid parameter for function: IsMainOrder is for MarketPartners only'
    assert get_exception_code(res[0][1], tag_name='msg') == error_msg


def test_order_create_xmlrpc_set_markups(session, xmlrpcserver, client, product, service_order_id, markup):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': ServiceId.MARKET_PARTNERS,
                                                       'markups': [markup.id]}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == ServiceId.MARKET_PARTNERS)).all()
    assert len(orders) == 1
    assert orders[0].markups == {markup.id}


def test_order_create_xmlrpc_set_manager(session, xmlrpcserver, client, product, service_order_id, manager):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': ServiceId.MARKET_PARTNERS,
                                                       'ManagerUID': manager.passport_id}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == ServiceId.MARKET_PARTNERS)).all()
    assert len(orders) == 1
    assert orders[0].manager == manager


def test_order_create_xmlrpc_delete_manager(xmlrpcserver, order, manager):
    order.manager = manager
    assert order.manager == manager
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(None, [{'ClientID': order.client.id, 'ProductID': order.product.id,
                                                       'ServiceOrderID': order.service_order_id,
                                                       'ServiceID': order.service.id,
                                                       'ManagerUID': None}])
    assert len(res) == 1
    assert res[0] == [CODE_SUCCESS, 'Success']
    assert order.manager is None


def test_order_create_xmlrpc_set_order_text(session, xmlrpcserver, client, product, service_order_id, service, order,
                                            manager):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': service.id,
                                                       'text': 'order_test'}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == service.id)).all()
    assert orders[0].text == 'order_test'


def test_order_create_xmlrpc_product_name_is_order_text(session, xmlrpcserver, client, product, service_order_id,
                                                        service):
    product.name = 'product_name'
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': service.id}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == service.id)).all()
    assert orders[0].text == 'product_name'


def test_order_create_xmlrpc_unmoderated(session, xmlrpcserver, client, product, service_order_id, service):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': service.id, 'unmoderated': 1}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == service.id)).all()
    assert orders[0].unmoderated == 1


def test_order_create_xmlrpc_region(session, xmlrpcserver, client, product, service_order_id, service):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [{'ClientID': client.id, 'ProductID': product.id,
                                                       'ServiceOrderID': service_order_id,
                                                       'ServiceID': service.id, 'RegionID': 225}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == service.id)).all()
    assert orders[0].region_id == 225


@pytest.mark.parametrize('service_id', [ServiceId.DIRECT,
                                        ServiceId.MARKET,
                                        ServiceId.OFD,
                                        ServiceId.MEDIA_BANNERS,
                                        ServiceId.NAVIGATOR_ADV,
                                        ServiceId.TAXI_CASH,
                                        ServiceId.TAXI_CARD])
def test_order_xmlrpc_simple_group(session, xmlrpcserver, client, product, service_order_id, service_id):
    """простое объединение в группу для заказов разрешенных сервисов"""
    parent_order = order(session, client=client, service_id=service_id)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': client.id,
                                                   'ProductID': product.id,
                                                   'ServiceOrderID': service_order_id,
                                                   'ServiceID': parent_order.service.id,
                                                   'GroupServiceOrderID': parent_order.service_order_id}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == service_order_id,
                                                 mapper.Order.service_id == parent_order.service.id)).all()
    assert orders[0].group_order_id == parent_order.id
    assert orders[0].parent_group_order == parent_order


def test_order_xmlrpc_simple_optimize_group_export_check(
    session, xmlrpcserver, client, product, service_order_id,
):
    """простое объединение в группу для заказов разрешенных сервисов"""
    parent_order = order(session, client=client)
    parent_order.is_ua_optimize = 1
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': client.id,
                                                   'ProductID': product.id,
                                                   'ServiceOrderID': service_order_id,
                                                   'ServiceID': parent_order.service.id,
                                                   'GroupServiceOrderID': parent_order.service_order_id}])
    assert res == [[CODE_SUCCESS, 'Success']]
    assert parent_order.exports['UA_TRANSFER'].object_id == parent_order.id
    assert parent_order.exports['UA_TRANSFER'].classname == 'Order'
    assert parent_order.exports['UA_TRANSFER'].input['use_completion_history'] is True
    assert parent_order.exports['UA_TRANSFER'].input['for_dt'] is not None
    assert parent_order.exports['UA_TRANSFER'].state == ExportState.enqueued


def test_order_xmlrpc_simple_non_optimize_group_export_check(
    session, xmlrpcserver, client, product, service_order_id,
):
    """простое объединение в группу для заказов разрешенных сервисов"""
    parent_order = order(session, client=client)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': client.id,
                                                   'ProductID': product.id,
                                                   'ServiceOrderID': service_order_id,
                                                   'ServiceID': parent_order.service.id,
                                                   'GroupServiceOrderID': parent_order.service_order_id}])
    assert res == [[CODE_SUCCESS, 'Success']]
    assert parent_order.exports['UA_TRANSFER'].object_id == parent_order.id
    assert parent_order.exports['UA_TRANSFER'].classname == 'Order'
    assert parent_order.exports['UA_TRANSFER'].input is None
    assert parent_order.exports['UA_TRANSFER'].state == ExportState.enqueued


def test_order_xmlrpc_simple_non_optimize_group_wo_export(
    session, xmlrpcserver, client, product, service_order_id,
):
    """простое объединение в группу для заказов разрешенных сервисов"""
    parent_order = order(session, client=client)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': client.id,
                                                   'ProductID': product.id,
                                                   'ServiceOrderID': service_order_id,
                                                   'ServiceID': parent_order.service.id,
                                                   'GroupServiceOrderID': parent_order.service_order_id,
                                                   'GroupWithoutTransfer': True}])
    assert res == [[CODE_SUCCESS, 'Success']]
    assert 'UA_TRANSFER' not in parent_order.exports


def test_order_xmlrpc_simple_unlink(session, xmlrpcserver, client):
    """апдейтим дочерний заказ без указания родительского, проверяем, что заказ больше не в группе"""
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': child_order.client.id,
                                                   'ProductID': child_order.product.id,
                                                   'ServiceOrderID': child_order.service_order_id,
                                                   'ServiceID': child_order.service.id}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == child_order.service_order_id,
                                                 mapper.Order.service_id == child_order.service.id)).all()
    assert orders[0].group_order_id is None
    assert orders[0].parent_group_order is None


@pytest.mark.parametrize('group_service_order_id_value', [0, -1])
def test_order_xmlrpc_simple_unlink_with_special_param(session, xmlrpcserver, client, group_service_order_id_value):
    """апдейтим дочерний заказ, проверяем, что заказ больше не в группе"""
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': child_order.client.id,
                                                   'ProductID': child_order.product.id,
                                                   'ServiceOrderID': child_order.service_order_id,
                                                   'ServiceID': child_order.service.id,
                                                   'GroupServiceOrderID': group_service_order_id_value}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(mapper.Order).filter(sa.and_(mapper.Order.service_order_id == child_order.service_order_id,
                                                 mapper.Order.service_id == child_order.service.id)).all()
    assert orders[0].group_order_id is None
    assert orders[0].parent_group_order is None


@pytest.mark.parametrize(
    'test_env, env_type, is_ok',
    [
        (0, 'prod', True),
        (0, 'test', True),
        (1, 'prod', False),
        (1, 'test', True),
        (1, 'dev', True),
    ]
)
def test_service_test_env(session, app, xmlrpcserver, client, product, service_order_id, test_env, env_type, is_ok):
    service = ob.ServiceBuilder.construct(session)
    service.balance_service.test_env = test_env
    session.flush()

    with mock.patch.object(app, 'get_current_env_type', return_value=env_type):
        res, = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [{
                'ClientID': client.id,
                'ProductID': product.id,
                'ServiceOrderID': service_order_id,
                'ServiceID': service.id
            }]
        )
    order = (
        session.query(mapper.Order)
            .filter_by(service_id=service.id,
                       service_order_id=service_order_id)
            .first()
    )

    if is_ok:
        assert res[0] == CODE_SUCCESS
        assert res[1] == 'Success'
        assert order.service_id == service.id
    else:
        assert res[0] == CODE_ERROR
        assert "Can't create order with test service" in res[1]
        assert order is None


def test_create_with_optimize(session, xmlrpcserver, client, product, service, service_order_id):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(
        session.oper_id,
        [{'ClientID': client.id,
          'ProductID': product.id,
          'ServiceOrderID': service_order_id,
          'ServiceID': service.id,
          'IsUAOptimize': 1,
          }]
    )
    assert res == [[CODE_SUCCESS, 'Success']]

    order = (
        session.query(mapper.Order)
            .filter(mapper.Order.service_order_id == service_order_id,
                    mapper.Order.service_id == service.id)
            .one()
    )
    hamcrest.assert_that(
        order,
        hamcrest.has_properties(
            is_ua_optimize=1,
            main_order=1,
        )
    )


def test_create_with_optimize_direct_unconverted(session, xmlrpcserver, client, product, service_order_id):
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(
        session.oper_id,
        [{'ClientID': client.id,
          'ProductID': product.id,
          'ServiceOrderID': service_order_id,
          'ServiceID': ServiceId.DIRECT,
          'IsUAOptimize': 1,
          }]
    )
    hamcrest.assert_that(
        res,
        hamcrest.contains(
            hamcrest.contains(
                CODE_ERROR,
                hamcrest.contains_string('ORDER_UA_OPTIMIZE_TURN_ON_ERROR')
            )
        )
    )


def test_set_ua_optimize_for_main_order(session, xmlrpcserver, service):
    main_order = ob.OrderBuilder.construct(session, service_id=service.id, main_order=1)
    child_order = ob.OrderBuilder.construct(
        session,
        client=main_order.client,
        parent_group_order=main_order,
        product=main_order.product,
    )

    res = xmlrpcserver.CreateOrUpdateOrdersBatch(
        session.oper_id,
        [{'ClientID': main_order.client_id,
          'ProductID': main_order.service_code,
          'ServiceOrderID': main_order.service_order_id,
          'ServiceID': main_order.service_id,
          'IsUAOptimize': 1,
          }]
    )
    assert res == [[CODE_SUCCESS, 'Success']]

    hamcrest.assert_that(
        main_order,
        hamcrest.has_properties(
            is_ua_optimize=1,
            main_order=1,
        )
    )
    hamcrest.assert_that(
        child_order,
        hamcrest.has_properties(
            child_ua_type=UAChildType.OPTIMIZED,
        )
    )


class TestTurnOnLogTariff(object):
    @pytest.mark.parametrize(
        'with_agency, for_client, for_agency',
        [
            pytest.param(False, True, False, id='client'),
            pytest.param(True, False, True, id='agency'),
            pytest.param(True, True, False, id='subclient'),
        ]
    )
    def test_flag(self, session, xmlrpcserver, client, agency, service_order_id, with_agency, for_client, for_agency):
        if for_client:
            client.should_turn_on_log_tariff = 1
        if for_agency:
            agency.should_turn_on_log_tariff = 1
        mk_currency(client)
        session.flush()

        args = {
            'ClientID': client.id,
            'ProductID': DIRECT_PRODUCT_RUB_ID,
            'ServiceOrderID': service_order_id,
            'ServiceID': ServiceId.DIRECT,
        }
        if with_agency:
            args['AgencyID'] = agency.id
        res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [args])
        assert res == [[CODE_SUCCESS, 'Success']]

        order = (
            session.query(mapper.Order)
                .filter_by(service_order_id=service_order_id,
                           service_id=ServiceId.DIRECT)
                .one()
        )
        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                is_ua_optimize=False,
                _is_log_tariff=OrderLogTariffState.MIGRATED,
                child_ua_type=None,
                shipment=hamcrest.has_properties(
                    deny_shipment=1,
                )
            )
        )

    @pytest.mark.parametrize(
        'switch_default_turn_on',
        [True, False],
    )
    @pytest.mark.parametrize(
        'with_agency, for_client, for_agency, is_turned_on',
        [
            pytest.param(False, False, False, True, id='off'),
            pytest.param(False, True, False, False, id='client'),
            pytest.param(True, True, False, False, id='subclient'),
            pytest.param(True, False, True, False, id='agency'),
        ]
    )
    def test_inverse_flag(self, session, xmlrpcserver, client, agency, service_order_id,
                          with_agency, for_client, for_agency, is_turned_on, switch_default_turn_on):
        session.config.__dict__['DEFAULT_TURN_ON_LOG_TARIFF'] = switch_default_turn_on
        if for_client:
            client.dont_turn_on_log_tariff = 1
        if for_agency:
            agency.dont_turn_on_log_tariff = 1
        mk_currency(client)
        session.flush()

        args = {
            'ClientID': client.id,
            'ProductID': DIRECT_PRODUCT_RUB_ID,
            'ServiceOrderID': service_order_id,
            'ServiceID': ServiceId.DIRECT,
        }
        if with_agency:
            args['AgencyID'] = agency.id
        res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [args])
        assert res == [[CODE_SUCCESS, 'Success']]

        order = (
            session.query(mapper.Order)
                .filter_by(service_order_id=service_order_id,
                           service_id=ServiceId.DIRECT)
                .one()
        )
        assert order.is_log_tariff == (switch_default_turn_on and is_turned_on)

    @pytest.mark.parametrize(
        'agency_positive_flag, for_client, for_agency, is_turned_on',
        [
            pytest.param(False, False, False, True, id='off'),
            pytest.param(False, True, False, False, id='client'),
            pytest.param(True, True, False, False, id='subclient'),
            pytest.param(True, False, True, False, id='agency'),
        ]
    )
    def test_inverse_flag_priority(self, session, xmlrpcserver, client, agency, service_order_id,
                                   agency_positive_flag, for_client, for_agency, is_turned_on):
        session.config.__dict__['DEFAULT_TURN_ON_LOG_TARIFF'] = 1
        if agency_positive_flag:
            agency.should_turn_on_log_tariff = 1
        else:
            client.should_turn_on_log_tariff = 1
        if for_client:
            client.dont_turn_on_log_tariff = 1
        if for_agency:
            agency.dont_turn_on_log_tariff = 1
        mk_currency(client)
        session.flush()

        args = {
            'ClientID': client.id,
            'ProductID': DIRECT_PRODUCT_RUB_ID,
            'ServiceOrderID': service_order_id,
            'ServiceID': ServiceId.DIRECT,
            'AgencyID': agency.id,
        }
        res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id, [args])
        assert res == [[CODE_SUCCESS, 'Success']]

        order = (
            session.query(mapper.Order)
                .filter_by(service_order_id=service_order_id,
                           service_id=ServiceId.DIRECT)
                .one()
        )
        assert order.is_log_tariff == is_turned_on

    def test_existing(self, session, xmlrpcserver):
        order = ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID)

        order.client.should_turn_on_log_tariff = 1
        mk_currency(order.client)
        session.flush()

        res = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [{'ClientID': order.client_id,
              'ProductID': DIRECT_PRODUCT_RUB_ID,
              'ServiceOrderID': order.service_order_id,
              'ServiceID': ServiceId.DIRECT,
              }]
        )
        assert res == [[CODE_SUCCESS, 'Success']]

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=None,
                shipment=hamcrest.has_properties(
                    deny_shipment=None,
                )
            )
        )

    @pytest.mark.parametrize('parent_turned_on', [False, True])
    def test_child(self, session, xmlrpcserver, service_order_id, parent_turned_on):
        order = ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID)

        order.client.should_turn_on_log_tariff = 1
        mk_currency(order.client)
        if parent_turned_on:
            order.force_log_tariff()
        session.flush()

        res = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [{'ClientID': order.client_id,
              'ProductID': DIRECT_PRODUCT_RUB_ID,
              'ServiceOrderID': service_order_id,
              'GroupServiceOrderID': order.service_order_id,
              'ServiceID': ServiceId.DIRECT,
              }]
        )
        assert res == [[CODE_SUCCESS, 'Success']]

        child_order = (
            session.query(mapper.Order)
                .filter_by(service_order_id=service_order_id,
                           service_id=ServiceId.DIRECT)
                .one()
        )

        hamcrest.assert_that(
            child_order,
            hamcrest.has_properties(
                group_order_id=order.id,
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=UAChildType.LOG_TARIFF if parent_turned_on else UAChildType.TRANSFERS,
                shipment=hamcrest.has_properties(
                    deny_shipment=None,
                )
            )
        )

    def test_move_to_child(self, session, xmlrpcserver, client):
        client.should_turn_on_log_tariff = 1
        mk_currency(client)

        sid1 = ob.get_big_number()
        sid2 = ob.get_big_number()

        res = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [{'ClientID': client.id,
              'ProductID': DIRECT_PRODUCT_RUB_ID,
              'ServiceOrderID': sid1,
              'ServiceID': ServiceId.DIRECT,
              }]
        )
        assert res == [[CODE_SUCCESS, 'Success']]

        res = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [{'ClientID': client.id,
              'ProductID': DIRECT_PRODUCT_RUB_ID,
              'ServiceOrderID': sid2,
              'ServiceID': ServiceId.DIRECT,
              }]
        )
        assert res == [[CODE_SUCCESS, 'Success']]

        res = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [{'ClientID': client.id,
              'ProductID': DIRECT_PRODUCT_RUB_ID,
              'ServiceOrderID': sid2,
              'GroupServiceOrderID': sid1,
              'ServiceID': ServiceId.DIRECT,
              }]
        )
        assert res == [[CODE_SUCCESS, 'Success']]

        order1 = (
            session.query(mapper.Order)
                .filter_by(service_order_id=sid1, service_id=ServiceId.DIRECT)
                .one()
        )
        order2 = (
            session.query(mapper.Order)
                .filter_by(service_order_id=sid2, service_id=ServiceId.DIRECT)
                .one()
        )

        hamcrest.assert_that(
            order1,
            hamcrest.has_properties(
                is_ua_optimize=1,
                _is_log_tariff=OrderLogTariffState.MIGRATED,
            )
        )
        hamcrest.assert_that(
            order2,
            hamcrest.has_properties(
                group_order_id=order1.id,
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=UAChildType.LOG_TARIFF,
                shipment=hamcrest.has_properties(
                    deny_shipment=1,
                )
            )
        )

    def test_optimized_not_available(self, session, xmlrpcserver, client, service_order_id):
        client.should_turn_on_log_tariff = 1
        session.flush()

        res = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [{
                'ClientID': client.id,
                'ProductID': DIRECT_PRODUCT_RUB_ID,
                'ServiceOrderID': service_order_id,
                'ServiceID': ServiceId.DIRECT,
            }]
        )
        assert res == [[CODE_SUCCESS, 'Success']]

        order = (
            session.query(mapper.Order)
                .filter_by(service_order_id=service_order_id,
                           service_id=ServiceId.DIRECT)
                .one()
        )
        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=None,
                shipment=hamcrest.has_properties(
                    deny_shipment=None,
                )
            )
        )

    def test_child_w_parent(self, session, xmlrpcserver, client):
        client.should_turn_on_log_tariff = 1
        mk_currency(client)

        sid1 = ob.get_big_number()
        sid2 = ob.get_big_number()

        res = xmlrpcserver.CreateOrUpdateOrdersBatch(
            session.oper_id,
            [
                {
                    'ClientID': client.id,
                    'ProductID': DIRECT_PRODUCT_RUB_ID,
                    'ServiceOrderID': sid1,
                    'ServiceID': ServiceId.DIRECT,
                },
                {
                    'ClientID': client.id,
                    'ProductID': DIRECT_PRODUCT_RUB_ID,
                    'ServiceOrderID': sid2,
                    'GroupServiceOrderID': sid1,
                    'ServiceID': ServiceId.DIRECT,
                }
            ]
        )
        assert res == [[CODE_SUCCESS, 'Success'], [CODE_SUCCESS, 'Success']]

        order1 = (
            session.query(mapper.Order)
                .filter_by(service_order_id=sid1, service_id=ServiceId.DIRECT)
                .one()
        )
        order2 = (
            session.query(mapper.Order)
                .filter_by(service_order_id=sid2, service_id=ServiceId.DIRECT)
                .one()
        )

        hamcrest.assert_that(
            order1,
            hamcrest.has_properties(
                is_ua_optimize=1,
                _is_log_tariff=OrderLogTariffState.MIGRATED,
            )
        )
        hamcrest.assert_that(
            order2,
            hamcrest.has_properties(
                group_order_id=order1.id,
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=UAChildType.LOG_TARIFF,
                shipment=hamcrest.has_properties(
                    deny_shipment=None,
                )
            )
        )
