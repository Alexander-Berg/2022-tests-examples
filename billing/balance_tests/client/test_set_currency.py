# -*- coding: utf-8 -*-

import pytest
import datetime
from decimal import Decimal
import re

from balance.constants import ServiceId, RegionId
from balance import exc
from balance.muzzle_util import trunc_date
from balance.constants import CONVERT_TYPE_COPY, CONVERT_TYPE_MODIFY, NOTIFY_CLIENT_OPCODE
from balance.mapper.clients import ClientServiceData
from balance.actions.invoice_turnon import InvoiceTurnOn

from tests import object_builder as ob

NOW = datetime.datetime.now()
CODE_SUCCESS = 0


def create_invoice(session):
    return ob.InvoiceBuilder().build(session).obj


def create_client(session):
    return ob.ClientBuilder().build(session).obj


def create_client_service_data(migrate_to_currency_dt, currency='RUB', currency_convert_type=CONVERT_TYPE_COPY):
    client_service_data = ClientServiceData(ServiceId.DIRECT)
    client_service_data.iso_currency = currency
    client_service_data.migrate_to_currency = migrate_to_currency_dt
    client_service_data.convert_type = currency_convert_type
    return client_service_data


@pytest.mark.parametrize('service_id, currency, convert_type',
                         [(ServiceId.DIRECT, 'USD', 'MODIFY')])
def test_modify_with_rub_only(session, service_id, currency, convert_type):
    client = create_client(session)
    with pytest.raises(exc.INVALID_PARAM) as excinfo:
        client.set_currency(service_id, currency, NOW, convert_type)
    assert excinfo.type == exc.INVALID_PARAM
    assert excinfo.value.msg == u'Invalid parameter for function: Wrong currency \'{0}\' for convert type \'{1}\''.format(
        currency,
        convert_type)


@pytest.mark.parametrize('currency_convert_type', [CONVERT_TYPE_COPY, CONVERT_TYPE_MODIFY])
def test_client_is_in_migrate_queue(session, currency_convert_type):
    client = create_client(session)
    client.set_currency(ServiceId.DIRECT, 'RUB', NOW, currency_convert_type)
    assert client.exports['MIGRATE_TO_CURRENCY'].object_id == client.id
    assert client.exports['MIGRATE_TO_CURRENCY'].classname == 'Client'
    assert client.exports['MIGRATE_TO_CURRENCY'].input['service_id'] == ServiceId.DIRECT
    assert client.exports['MIGRATE_TO_CURRENCY'].input['convert_type'] == currency_convert_type
    assert client.exports['MIGRATE_TO_CURRENCY'].input['for_dt'] == trunc_date(NOW)


def test_client_migrate_update(session, xmlrpcserver):
    client = create_client(session)
    migrate_to_currency_dt = NOW + datetime.timedelta(hours=1)
    client.service_data[ServiceId.DIRECT] = create_client_service_data(currency='RUB',
                                                                       migrate_to_currency_dt=migrate_to_currency_dt,
                                                                       currency_convert_type=CONVERT_TYPE_COPY)
    result = xmlrpcserver.CreateClient(0, {'CLIENT_ID': client.id,
                                           'IS_AGENCY': False,
                                           'NAME': 'Test Client',
                                           'CITY': 'Moscow',
                                           'REGION_ID': RegionId.RUSSIA,
                                           'CURRENCY': 'RUB',
                                           'MIGRATE_TO_CURRENCY': migrate_to_currency_dt,
                                           'SERVICE_ID': ServiceId.DIRECT})
    assert len(result) == 3
    assert (result[0], result[1]) == (CODE_SUCCESS, 'SUCCESS')


@pytest.mark.parametrize('descr, params',
                         [
                             # миграция уже состоялась
                             ('already migrated', {'old_migrate_dt': NOW - datetime.timedelta(hours=1)}),
                             # миграция не может быть в прошлом
                             ('in the past', {'new_migrate_dt': NOW - datetime.timedelta(hours=1)}),
                         ])
def test_client_migrate_update_errors(session, descr, params):
    invoice = create_invoice(session)
    InvoiceTurnOn(invoice, manual=True).do()
    invoice.client.service_data[ServiceId.DIRECT] = create_client_service_data(currency='RUB',
                                                                               migrate_to_currency_dt=params.get(
                                                                                   'old_migrate_dt',
                                                                                   NOW + datetime.timedelta(hours=1)),
                                                                               currency_convert_type=CONVERT_TYPE_COPY)
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        invoice.client.set_currency(ServiceId.DIRECT, params.get('currency', 'RUB'),
                                    migrate_to_currency=params.get('new_migrate_dt', NOW + datetime.timedelta(hours=1)),
                                    convert_type=CONVERT_TYPE_COPY)
    if descr == 'already migrated':
        assert re.match(u'Invalid parameter for function: Too late to change migrate_to_currency .*',
                        exc_info.value.msg)
        assert exc_info.value.error_booster_msg == u'Invalid parameter for function: Too late to change migrate_to_currency %s'
    else:
        assert re.match(u'Invalid parameter for function: New migrate_to_currency must be not less than .*',
                        exc_info.value.msg)
        assert exc_info.value.error_booster_msg == u'Invalid parameter for function: New migrate_to_currency must be not less than %s'


def test_client_in_notify_queue(session):
    client = create_client(session)
    res = session.execute("select * from bo.ton t where t.user_data.order_id = :oid",
                          {'oid': client.id + NOTIFY_CLIENT_OPCODE / Decimal(1000)})
    assert len(res.fetchall()) == 0
    client.set_currency(ServiceId.DIRECT, 'RUB', NOW, CONVERT_TYPE_COPY)
    res = session.execute("select * from bo.ton t where t.user_data.order_id = :oid",
                          {'oid': client.id + NOTIFY_CLIENT_OPCODE / Decimal(1000)})
    assert len(res.fetchall())
