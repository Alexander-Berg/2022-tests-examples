# -*- coding: utf-8 -*-
import json

import datetime
import hamcrest
import httpretty
import mock
import pytest

from balance.application import getApplication
from balance import constants as cst
from balance.queue_processor import QueueProcessor
from conftest import check_export_obj
from tests import object_builder as ob

on_dt = datetime.datetime.now().replace(microsecond=0)


def mock_error_status(error_msg):
    for key in ('Url', 'CloudUrl'):
        httpretty.register_uri(
            httpretty.POST,
            getApplication().get_component_cfg('oebs_api')[key] + 'getStatusBilling',
            json.dumps([{"request_id": "3499129",
                         "entity_type": "AKT",
                         "entity_id": "118811909",
                         "status": "ERROR",
                         "errors": [error_msg]}]))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('act_external_id', ['147685538', 'YB-1244'])
def test_lost_request_id(session, act, act_external_id, service_ticket_mock):
    """
    Если мы потеряли request_id акта (например, упал разборщик до коммита) при повторной попытке
    выгрузки оебс вернет соот-вую ошибку и потерянный request_id
    """
    old_request_id = ob.generate_int(10)
    act.exports['OEBS_API'].input = {'request_id': old_request_id}
    new_request_id = ob.generate_int(10)
    error_msg = "Акт с указанным номером (\"{}\")  уже существует, correct_request_id={}".format(act_external_id,
                                                                                                 new_request_id)
    mock_error_status(error_msg)
    QueueProcessor('OEBS_API').process_one(act.exports['OEBS_API'])
    session.flush()
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(act.exports['OEBS_API'],
                     state=0,
                     output='Still waiting on export',
                     error='Retrying OEBS_API processing',
                     input={'orig_request_id': old_request_id,
                            'request_id': new_request_id},
                     next_export=hamcrest.greater_than_or_equal_to(delay))
    # проверяем, что следующий запрос будет с новым request_id
    for key in ('Url', 'CloudUrl'):
        httpretty.register_uri(
            httpretty.POST,
            getApplication().get_component_cfg('oebs_api')[key] + 'getStatusBilling',
            json.dumps([{"status": "OK"}]))
    QueueProcessor('OEBS_API').process_one(act.exports['OEBS_API'])
    assert httpretty.last_request().body == json.dumps([{"request_id": new_request_id}])


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('act_external_id', ['147685538', 'YB-1244'])
def test_lost_request_id_is_same(session, act, act_external_id, service_ticket_mock):
    """
    Если мы потеряли request_id акта (например, упал разборщик до коммита) и при повторной попытке
    выгрузки оебс вернет нам тот же request_id, падаем с обычной ошибкой
    """
    old_request_id = ob.generate_int(10)
    act.exports['OEBS_API'].input = {'request_id': old_request_id}
    error_msg = u"Акт с указанным номером (\"{}\")  уже существует, correct_request_id={}".format(act_external_id,
                                                                                                  old_request_id)
    mock_error_status(error_msg)
    QueueProcessor('OEBS_API').process_one(act.exports['OEBS_API'])
    session.flush()
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(act.exports['OEBS_API'],
                     state=0,
                     rate=1,
                     output=None,
                     error=u'Error for export check: {}'.format(error_msg),
                     input=None,
                     next_export=hamcrest.greater_than_or_equal_to(delay))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_lost_request_id_is_unknown(session, act, service_ticket_mock):
    act.exports['OEBS_API'].input = {'request_id': ob.generate_int(10)}
    error_msg = u"Акт с указанным номером (\"147685538\")  уже существует, correct_request_id="
    mock_error_status(error_msg)
    QueueProcessor('OEBS_API').process_one(act.exports['OEBS_API'])
    session.flush()
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(act.exports['OEBS_API'],
                     state=0,
                     rate=1,
                     output=None,
                     error=u'Error for export check: {}'.format(error_msg),
                     input=None,
                     next_export=hamcrest.greater_than_or_equal_to(delay))


@pytest.mark.parametrize('invoice_export_state', [cst.ExportState.exported,
                                                  cst.ExportState.enqueued])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_invoice(session, act, invoice_export_state, service_ticket_mock):
    act.exports['OEBS_API'].input = {'request_id': ob.generate_int(10)}
    error_msg = u'Счет с указанным GUID ("{}")  не найден.'.format(act.invoice.external_id)
    act.invoice.exports['OEBS_API'].state = invoice_export_state
    act.invoice.exports['OEBS_API'].export_dt = on_dt
    mock_error_status(error_msg)
    QueueProcessor('OEBS_API').process_one(act.exports['OEBS_API'])
    session.flush()
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    if invoice_export_state == cst.ExportState.enqueued:
        check_export_obj(act.exports['OEBS_API'],
                         state=0,
                         rate=0,
                         output=None,
                         error=u'Export has been deferred because related Invoice [{}] is not in OEBS.'.format(
                             act.invoice.id),
                         input=act.exports['OEBS_API'].input,
                         next_export=hamcrest.greater_than_or_equal_to(delay))
    else:
        check_export_obj(act.exports['OEBS_API'],
                         state=0,
                         rate=1,
                         output=None,
                         error=u'Error for export check: {}'.format(error_msg),
                         input=None,
                         next_export=hamcrest.greater_than_or_equal_to(delay))
