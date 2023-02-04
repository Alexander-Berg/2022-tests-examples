# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm

from balance import constants as cst, exc, mapper
from balance.utils.xml2json import xml2json_auto

from muzzle.api import order as order_api
from balance.corba_buffers import StateBuffer

from tests import object_builder as ob


@pytest.fixture(name='view_order_role')
def create_view_order_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_ORDERS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='view_invoice_role')
def create_view_invoice_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='order')
def create_order(session, client=None):
    client = client or create_client(session)
    return ob.OrderBuilder.construct(session, client=client)


@pytest.fixture(name='request_')
def create_request(session, order=None, client=None, firm_id=cst.FirmId.YANDEX_OOO):
    client = client or create_client(session)
    order = order or create_order(session, client=client)
    return ob.RequestBuilder.construct(
        session,
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=1)]
        )
    )


@pytest.fixture(name='invoice')
def create_invoice(session, request_):
    inv = ob.InvoiceBuilder.construct(session, request=request_)
    inv.manual_turn_on(inv.effective_sum)
    return inv


@pytest.fixture(name='order_w_firm')
def create_order_w_firm(session, client=None, firm_ids=None, service_id=None):
    service_id = service_id or cst.ServiceId.DIRECT
    firm_ids = firm_ids or [cst.FirmId.YANDEX_OOO]
    order = ob.OrderBuilder.construct(
        session,
        client=client or ob.ClientBuilder(),
        service_id=service_id,
    )
    for firm_id in firm_ids:
        order.add_firm(firm_id)
    session.flush()
    return order


class TestOrderLogic(object):
    def test_get_order_success_params(self, session, order):
        # правильные параметры
        for kw in [
            {'order_id': order.id},
            {'service_id': order.service_id, 'service_order_id': order.service_order_id},
        ]:
            params = order_api.get_params(**kw)
            db_order = session.query(mapper.Order).getone(**params)
            assert order.id == db_order.id

    def test_get_order_fail_params(self, order):
        # неправильный набор параметров
        for kw in [
            {'order_id': -1},
            {'service_id': order.service_id},
            {'service_id': 0},
            {'service_order_id': order.service_order_id},
            {'service_id': order.service_id, 'service_order_id': -1},
        ]:
            with pytest.raises(exc.INVALID_PARAM):
                order_api.get_params(**kw)

    def test_check_access(self, session, muzzle_logic, order):
        muzzle_res = muzzle_logic.check_access_to_order(StateBuffer(), session.passport.oper_id, order.id)
        assert muzzle_res is not None
        assert muzzle_res.tag == 'access-to-order-granted'
        assert muzzle_res.attrib['order-id'] == str(order.id)
        assert muzzle_res.attrib['passport-id'] == str(session.passport.oper_id)


class TestOrders(object):
    def test_get_orders(self, session, muzzle_logic, client):
        orders = [create_order(session, client=client) for _i in range(3)]
        request_dict = {
            'payment_status': '1',
            'client_id': client.id,
            'pn': 1,
            'ps': 20,
        }

        entry_keys = ['agency', 'agency_id', 'client', 'client_id', 'completion_qty',
                      'completion_sum', 'consume_qty', 'consume_sum', 'consumes_count',
                      'is_agency', 'order_dt', 'order_eid', 'order_id', 'passport_id',
                      'product_fullname', 'remain_qty', 'remain_sum', 'root_order_eid',
                      'root_order_service_cc', 'root_order_service_order_id', 'service_cc',
                      'service_code', 'service_id', 'service_name', 'service_order_id',
                      'service_orders_url', 'tag', 'text', 'type_rate', 'unit']



        muzzle_res = muzzle_logic._get_orders(
            session,
            request_dict,
        )
        res = xml2json_auto(muzzle_res, ['entries/entry'])

        entries = res['entry']
        request = res['request']
        total_row_count = res['total_row_count']

        assert int(total_row_count) == len(orders)

        assert int(request['client_id']) == request_dict['client_id']
        assert int(request['pn']) == request_dict['pn']
        assert int(request['ps']) == request_dict['ps']

        assert len(entries) == len(orders)
        assert sorted(entries[0]) == sorted(entry_keys)
