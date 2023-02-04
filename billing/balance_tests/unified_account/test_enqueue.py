# -*- coding: utf-8 -*-

import pytest

from balance import mapper
from balance.actions import unified_account as a_ua
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    ServiceUAType,
)

from tests import object_builder as ob


def create_client(session, is_agency=False, agency=None):
    return ob.ClientBuilder(
        is_agency=is_agency,
        agency=agency
    ).build(session).obj


def create_service(session, ua_type=None):
    service = ob.ServiceBuilder().build(session).obj
    service.balance_service.unified_account_type = ua_type
    session.flush()
    return service


def create_order(client, service, group_order=None, agency=None):
    return ob.OrderBuilder(
        client=client,
        agency=agency,
        parent_group_order=group_order,
        product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID),
        service=service
    ).build(client.session).obj


def test_service_selection(session):
    service_enqueue = create_service(session, ServiceUAType.enqueue)
    client_enqueue = create_client(session)
    order_enqueue = create_order(client_enqueue, service_enqueue)
    create_order(client_enqueue, service_enqueue, order_enqueue)

    service_manual = create_service(session, ServiceUAType.manual)
    client_manual = create_client(session)
    order_manual = create_order(client_manual, service_manual)
    create_order(client_manual, service_manual, order_manual)

    service_nope = create_service(session, None)
    client_nope = create_client(session)
    order_nope = create_order(client_nope, service_nope)
    create_order(client_nope, service_nope, order_nope)

    a_ua.UnifiedAccountEnqueuer.enqueue(session, client_ids=[client_enqueue.id, client_manual.id, client_nope.id])
    session.expire_all()

    assert client_enqueue.exports['UA_TRANSFER'].state == 0
    assert 'UA_TRANSFER' not in client_manual.exports
    assert 'UA_TRANSFER' not in client_nope.exports


def test_agency(session):
    service = create_service(session, ServiceUAType.enqueue)
    agency = create_client(session, is_agency=True)
    client = create_client(session, agency=agency)
    order = create_order(client, service, agency=agency)
    create_order(client, service, order, agency=agency)

    a_ua.UnifiedAccountEnqueuer.enqueue(session, client_ids=[client.id, agency.id])
    session.expire_all()

    assert agency.exports['UA_TRANSFER'].state == 0
    assert 'UA_TRANSFER' not in client.exports


def test_only_new(session):
    service = create_service(session, ServiceUAType.enqueue)

    session.config.__dict__['UA_ENQUEUE_UPDATE_DT_DAYS'] = [(0, 666)]
    session.config.__dict__['UA_ENQUEUE_UPDATE_DT_DELTA'] = 10

    client_old = create_client(session)
    order_old = create_order(client_old, service)
    child_order_old = create_order(client_old, service, order_old)
    session.execute(
        '''update bo.t_order set update_dt=sysdate-666, mru_date=sysdate-666, shipment_update_dt=sysdate-666
        where id in (:id1, :id2)''',
        {'id1': order_old.id, 'id2': child_order_old.id}
    )

    client_mru = create_client(session)
    order_mru = create_order(client_mru, service)
    child_order_mru = create_order(client_mru, service, order_mru)
    session.execute(
        '''update bo.t_order set update_dt=sysdate-666, mru_date=sysdate-9, shipment_update_dt=sysdate-666
        where id in (:id1, :id2)''',
        {'id1': order_mru.id, 'id2': child_order_mru.id}
    )

    client_shipm = create_client(session)
    order_shipm = create_order(client_shipm, service)
    child_order_shipm = create_order(client_shipm, service, order_shipm)
    session.execute(
        '''update bo.t_order set update_dt=sysdate-666, mru_date=sysdate-666, shipment_update_dt=sysdate-9
        where id in (:id1, :id2)''',
        {'id1': order_shipm.id, 'id2': child_order_shipm.id}
    )

    client_upd = create_client(session)
    order_upd = create_order(client_upd, service)
    child_order_upd = create_order(client_upd, service, order_upd)
    session.execute(
        '''update bo.t_order set update_dt=sysdate-9, mru_date=sysdate-666, shipment_update_dt=sysdate-666
        where id in (:id1, :id2)''',
        {'id1': order_upd.id, 'id2': child_order_upd.id}
    )

    a_ua.UnifiedAccountEnqueuer.enqueue(
        session,
        client_ids=[client_old.id, client_mru.id, client_shipm.id, client_upd.id]
    )
    session.expire_all()

    assert client_mru.exports['UA_TRANSFER'].state == 0
    assert client_shipm.exports['UA_TRANSFER'].state == 0
    assert client_upd.exports['UA_TRANSFER'].state == 0
    assert 'UA_TRANSFER' not in client_old.exports
