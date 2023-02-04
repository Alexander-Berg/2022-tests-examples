# -*- coding: utf-8 -*-
import pytest
from xmlrpclib import Fault

from balance import constants as cst, exc, mapper
from medium.medium_logic import Logic
from tests import object_builder as ob
from tests.tutils import get_exception_code

CODE_SUCCESS = 0


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(**attrs).build(session).obj


@pytest.fixture
def agency(session, **attrs):
    return ob.ClientBuilder(is_agency=1).build(session).obj


@pytest.fixture(name='order')
def create_order(session, **attrs):
    return ob.OrderBuilder(**attrs).build(session).obj


@pytest.fixture(name='promocode')
def create_promocode(session, **attrs):
    return ob.PromoCodeGroupBuilder.construct(session, **attrs).promocodes[0]


@pytest.fixture
def logic():
    return Logic()


def create_manager(session, **attrs):
    return ob.SingleManagerBuilder(**attrs).build(session).obj


def test_create_request_xmlrpc(xmlrpcserver, order):
    res = xmlrpcserver.CreateRequest(None, order.client.id,
                                     [{'ServiceID': order.service.id,
                                       'ServiceOrderID': order.service_order_id,
                                       'Qty': 1}], )
    assert (res[0], res[1]) == (CODE_SUCCESS, 'SUCCESS')


def test_create_request_2_xmlrpc(session, xmlrpcserver, order):
    res = xmlrpcserver.CreateRequest2(session.oper_id, order.client.id,
                                      [{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1}], )
    request = session.query(mapper.Request).getone(res['RequestID'])
    assert request.seq == 1
    assert request.extern == 1
    assert request.dt is not None


def test_request_create_xmlrpc_unrelated_client(xmlrpcserver, client, order):
    # клиент, указанный при создании реквеста, должен быть указан в заказе
    order.agency = None
    with pytest.raises(Fault) as exc_info:
        xmlrpcserver.CreateRequest(None, client.id,
                                   [{'ServiceID': order.service.id,
                                     'ServiceOrderID': order.service_order_id,
                                     'Qty': 1}], )
    error_msg = 'Invalid parameter for function: Attempt to create request for client {client_id}' \
                ' with order for unrelated client {order_client_id} ({service_id}-{service_order_id})'
    assert get_exception_code(exc_info.value, 'msg') == error_msg.format(client_id=client.id,
                                                                         order_client_id=order.client.id,
                                                                         service_id=order.service.id,
                                                                         service_order_id=order.service_order_id)


def test_request_create_xmlrpc_with_another_agency(session, xmlrpcserver, order):
    # агенство, указанное при создании реквеста, должно совпадать с агенством, указанным в заказе
    order.agency = agency(session)
    order.client = order.agency
    another_agency = agency(session)
    with pytest.raises(Fault) as exc_info:
        res = xmlrpcserver.CreateRequest2(session.oper_id, another_agency.id,
                                          [{'ServiceID': order.service.id,
                                            'ServiceOrderID': order.service_order_id,
                                            'Qty': 1}], )
    error_msg = 'Invalid parameter for function: Attempt to create request for client {client_id}' \
                ' with order for unrelated client {order_client_id} ({service_id}-{service_order_id})'
    assert get_exception_code(exc_info.value, 'msg') == error_msg.format(client_id=another_agency.id,
                                                                         order_client_id=order.client.id,
                                                                         service_id=order.service.id,
                                                                         service_order_id=order.service_order_id)


def test_request_create_xmlrpc_with_unrelated_agency(session, xmlrpcserver, order):
    # агенство, указанное при создании реквеста, должно быть указано в заказе
    order.agency = agency(session)
    another_agency = agency(session)
    with pytest.raises(Fault) as exc_info:
        xmlrpcserver.CreateRequest2(session.oper_id, another_agency.id,
                                    [{'ServiceID': order.service.id,
                                      'ServiceOrderID': order.service_order_id,
                                      'Qty': 1}], )
    error_msg = 'Invalid parameter for function: Attempt to create request for agency {order_agency_id}' \
                ' with client {order_client_id} of another agency {another_agency_id} ({service_id}-{service_order_id})'
    assert get_exception_code(exc_info.value, 'msg') == error_msg.format(another_agency_id=order.agency.id,
                                                                         order_client_id=order.client.id,
                                                                         order_agency_id=another_agency.id,
                                                                         service_id=order.service.id,
                                                                         service_order_id=order.service_order_id)


def test_request_create_xmlrpc_with_agency(session, xmlrpcserver, agency, order):
    # агенство, указанное при создании реквеста, должно быть указано в заказе
    order.agency = agency
    res = xmlrpcserver.CreateRequest2(session.oper_id, agency.id,
                                      [{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1}], )
    request = session.query(mapper.Request).getone(res['RequestID'])
    assert request


def test_request_create_xmlrpc_without_qty(xmlrpcserver, order):
    with pytest.raises(Fault) as exc_info:
        xmlrpcserver.CreateRequest(None, order.client.id,
                                   [{'ServiceID': order.service.id,
                                     'ServiceOrderID': order.service_order_id}], )
    error_msg = 'Invalid parameter for function: qty or quantity must be more than 0'
    assert get_exception_code(exc_info.value, 'msg') == error_msg


def test_create_request_2_w_promo_code(session, logic, order, promocode):
    """Помокод привязывается к каждому новому реквесту,
    резервация создается только одна и не мешает привязывать промокод к
    новым реквестам того же клиента
    """
    requests = []
    for _i in range(2):
        res = logic.CreateRequest2(
            session.oper_id,
            order.client.id,
            [{'ServiceID': order.service.id, 'ServiceOrderID': order.service_order_id,  'Qty': 1}],
            {'PromoCode': promocode.code},
        )
        requests.append(session.query(mapper.Request).getone(res['RequestID']))

    for request in requests:
        assert request.promo_code is promocode

    reservation_count = (
        session
        .query(mapper.PromoCodeReservation)
        .filter(
            mapper.PromoCodeReservation.client == order.client,
            mapper.PromoCodeReservation.promocode == promocode
        )
        .count()
    )
    assert reservation_count == 1


def test_create_request_2_failed_to_other_client(session, logic, promocode):
    """Нельзя привязать промокод к другому клиенту
    """
    order1 = create_order(session)
    order2 = create_order(session)

    res1 = logic.CreateRequest2(
        session.oper_id,
        order1.client.id,
        [{'ServiceID': order1.service.id, 'ServiceOrderID': order1.service_order_id, 'Qty': 1}],
        {'PromoCode': promocode.code},
    )
    with pytest.raises(exc.INVALID_PC_RESERVED_ON_ANOTHER_CLIENT) as exc_info:
        logic.CreateRequest2(
            session.oper_id,
            order2.client.id,
            [{'ServiceID': order2.service.id, 'ServiceOrderID': order2.service_order_id, 'Qty': 1}],
            {'PromoCode': promocode.code},
        )
    assert exc_info.value.msg == 'Invalid promo code: ID_PC_RESERVED_ON_ANOTHER_CLIENT'


def test_create_request_2_w_wrong_promo_code(session, logic, order):
    promocode = create_promocode(session, service_ids=[cst.ServiceId.DRIVE])
    with pytest.raises(exc.INVALID_PC_NO_MATCHING_ROWS) as exc_info:
        logic.CreateRequest2(
            session.oper_id,
            order.client.id,
            [{'ServiceID': order.service.id, 'ServiceOrderID': order.service_order_id,  'Qty': 1}],
            {'PromoCode': promocode.code},
        )
    assert exc_info.value.msg == 'Invalid promo code: ID_PC_NO_MATCHING_ROWS'


def test_request_unicode_promocode(session, logic, order):
    with pytest.raises(exc.PROMOCODE_NOT_FOUND) as exc_info:
        logic.CreateRequest2(
            session.oper_id,
            order.client.id,
            [{'ServiceID': order.service.id, 'ServiceOrderID': order.service_order_id, 'Qty': 1}],
            {'PromoCode': u'\u042f\u043d\u0434\u0435\u043a\u0441'},
        )
    assert exc_info.value.msg == 'Invalid promo code: ID_PC_UNKNOWN'
