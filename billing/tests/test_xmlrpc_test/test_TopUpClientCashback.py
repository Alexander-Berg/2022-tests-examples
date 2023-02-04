# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm
from decimal import Decimal as D

from balance import (
    constants as cst,
    mapper,
)

from tests import object_builder as ob


def get_notifications(session, client_cashback_id):
    notifications = (
        session
        .execute(
            'select * from bo.t_object_notification where opcode = :opcode and object_id in (:id_1)',
            {
                'opcode': cst.NOTIFY_CLIENT_CASHBACK_OPCODE,
                'id_1': client_cashback_id,
            },
        )
        .fetchall()
    )
    return notifications


@pytest.fixture(autouse=True)
def notification_config(session):
    session.config.__dict__['ALLOWED_CASHBACK_NOTIFICATIONS'] = [cst.ServiceId.DIRECT]


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


def test_new_client_cashback(session, client, test_xmlrpc_srv):
    params = {
        'client_id': client.id,
        'service_id': cst.ServiceId.DIRECT,
        'iso_currency': 'RUB',
        'bonus': D('666.6666'),
    }
    res = test_xmlrpc_srv.TopUpClientCashback(params)

    client_cashback = session.query(mapper.ClientCashback).getone(res)
    hm.assert_that(
        client_cashback,
        hm.has_properties(**params),
    )

    assert len(get_notifications(session, res)) == 1


def test_top_up_client_cashbcak(session, client, test_xmlrpc_srv):
    base_params = {
        'client_id': client.id,
        'service_id': cst.ServiceId.DIRECT,
        'iso_currency': 'RUB',
    }
    client_cashback = ob.ClientCashbackBuilder.construct(
        session,
        bonus=D('123.56784'),
        **base_params
    )
    session.flush()

    params = base_params.copy()
    params['bonus'] = '666.6666'
    res = test_xmlrpc_srv.TopUpClientCashback(params)

    assert client_cashback.bonus == D('790.23444')
    assert len(get_notifications(session, res)) == 1
