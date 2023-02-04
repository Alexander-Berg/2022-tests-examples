# -*- coding: utf-8 -*-

import datetime
import hamcrest
import requests
import pytest
import mock
import httpretty
from urllib3.exceptions import MaxRetryError
import json
from balance.application import getApplication
from balance.queue_processor import QueueProcessor
from tests import object_builder as ob
from tests.balance_tests.oebs_api.conftest import (mock_post, check_export_obj,
                                                   assert_call_check,
                                                   assert_call_start,
                                                   create_firm)

on_dt = datetime.datetime.now().replace(microsecond=0)


def test_oebs_api_is_not_allowed(use_oebs_api, person, firm, session, service_ticket_mock):
    export_obj = person.exports['OEBS_API']
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = []
    assert not person.exports.get('OEBS')
    export_obj.input = None
    session.clear_cache()
    answer = {"result": "SUCCESS",
              "request_id": ob.get_big_number()}

    with mock_post(answer):
        QueueProcessor('OEBS_API').process_one(export_obj)
        person.session.flush()
    session.expire_all()
    check_export_obj(export_obj,
                     state=1,
                     output='Person {} will be exported with OEBS instead'.format(person.id),
                     input=None,
                     next_export=None,
                     error=None,
                     )
    check_export_obj(person.exports.get('OEBS'),
                     state=0,
                     output=None,
                     input=None,
                     next_export=None,
                     error=None,
                     )


@pytest.mark.parametrize(
    'input_',
    [None, {}],
    ids=['None', 'empty dict']
)
def test_handle_export_start_success(session, use_oebs_api, person, firm, input_, service_ticket_mock):
    """
    Плательщик с закешированной фирмой выгружается в ОЕБС, остается в state 0, экспорт откладывается.
    В input сохраняется request_id.
    """
    export_obj = person.exports['OEBS_API']
    export_obj.input = input_
    person.firms = [firm]
    person.session.flush()
    answer = {"result": "SUCCESS",
              "request_id": ob.get_big_number()}

    with mock_post(answer) as mock_obj:
        QueueProcessor('OEBS_API').process_one(export_obj)
        person.session.flush()
    assert_call_start(mock_obj)
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output='Successfully initialized export',
                     input={'request_id': answer['request_id'],
                            'firms': [firm.id]},
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error='Retrying OEBS_API processing',
                     )
    assert person.person_firms[0].oebs_export_dt is None


def test_handle_export_start_fail(session, use_oebs_api, person, firm, service_ticket_mock):
    """
    Экспорт плательщика упал с ошибкой
    """
    export_obj = person.exports['OEBS_API']
    person.firms = [firm]
    person.session.flush()
    answer = {"result": "ERROR",
              "errors": ['error_message']}
    with mock_post(answer) as mock_obj:
        QueueProcessor('OEBS_API').process_one(export_obj)
        person.session.flush()
    assert_call_start(mock_obj)
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output=None,
                     input=None,
                     rate=1,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=u'Error for export initialization: error_message')


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_handle_export_connection_timeout(session, use_oebs_api, person, firm, service_ticket_mock):
    """
    Экспорт завершился с ConnectionError, откладываем выгрузку без повышения рейта
    """
    export_obj = person.exports['OEBS_API']
    export_obj.rate = 9
    person.firms = [firm]
    person.session.flush()

    def f_w_exc(request, uri, response_headers):
        raise requests.ConnectionError(MaxRetryError(mock.MagicMock(), uri, '666'), request=request)
    key = 'CloudUrl' if session.config.get('USE_CLOUD_OEBS', False) else 'Url'
    uri = getApplication().get_component_cfg('oebs_api')[key] + 'billingImport'
    httpretty.register_uri(
        httpretty.POST,
        uri,
        body=f_w_exc)
    qp = QueueProcessor('OEBS_API')
    qp.max_rate = 10
    qp.process_one(export_obj)
    person.session.flush()
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output=u'OEBS ConnectionError on {} was thrown, defer export'.format(uri),
                     input=None,
                     rate=9,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=u'Retrying OEBS_API processing')


@pytest.mark.parametrize('status_code', [403, 502])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_handle_export_start_fail_500(session, use_oebs_api, person, firm, status_code, service_ticket_mock):
    """
    Экспорт завершился с http ошибкой 4XX-5XX, откладываем выгрузку без повышения рейта
    """
    export_obj = person.exports['OEBS_API']
    export_obj.rate = 9
    person.firms = [firm]
    person.session.flush()
    for key in ('Url', 'CloudUrl'):
        httpretty.register_uri(
            httpretty.POST,
            getApplication().get_component_cfg('oebs_api')[key] + 'billingImport',
            json.dumps([{"request_id": "3499129"}]),
            status=status_code)
    qp = QueueProcessor('OEBS_API')
    qp.max_rate = 10
    qp.process_one(export_obj)
    person.session.flush()
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output=u'HTTP response status code {} was given, defer export'.format(status_code),
                     input=None,
                     rate=9,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=u'Retrying OEBS_API processing')


@pytest.mark.parametrize('message', [u'java.sql.SQLException: ORA-12541: TNS:нет прослушивателя',
                                     u'ORA-12514: TNS:прослушиватель в данный момент не имеет данных о службе'])
def test_handle_export_start_fail_db_error(session, use_oebs_api, person, firm, message, service_ticket_mock):
    """
    Экспорт завершился с ошибкой доступа к базе данных ОЕБС, откладываем выгрузку без повышения рейта
    """
    export_obj = person.exports['OEBS_API']
    export_obj.rate = 9
    person.firms = [firm]
    person.session.flush()

    with mock_post({"status": "ERROR",
                    "message": message}) as mock_obj:
        qp = QueueProcessor('OEBS_API')
        qp.max_rate = 10
        qp.process_one(export_obj)
        person.session.flush()
    assert_call_start(mock_obj)
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output=u'OEBS DatabaseError \'{}\' was thrown, defer export'.format(message),
                     input=None,
                     rate=9,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=u'Retrying OEBS_API processing')


def test_handle_export_start_fail_final_technical_mistake(use_oebs_api, person, firm, service_ticket_mock):
    """
    Экспорт плательщика упал с ошибкой, переход в state=2
    """
    export_obj = person.exports['OEBS_API']
    export_obj.rate = 9
    person.firms = [firm]
    person.session.flush()
    with mock_post({"status": "ERROR",
                    "message": "error_message"}) as mock_obj:
        qp = QueueProcessor('OEBS_API')
        qp.max_rate = 10
        qp.process_one(export_obj)
        person.session.flush()
    assert_call_start(mock_obj)
    check_export_obj(export_obj,
                     state=2,
                     output=None,
                     input=None,
                     rate=10,
                     next_export=None,
                     error=u'Error while calling api: error_message')


def test_handle_export_start_fail_final_logical_mistake(use_oebs_api, person, firm, service_ticket_mock):
    """
    Экспорт плательщика упал с ошибкой, переход в state=2
    """
    export_obj = person.exports['OEBS_API']
    export_obj.rate = 9
    person.firms = [firm]
    person.session.flush()
    with mock_post({"result": "ERROR",
                    "errors": ['error_message']}) as mock_obj:
        qp = QueueProcessor('OEBS_API')
        qp.max_rate = 10
        qp.process_one(export_obj)
        person.session.flush()
    assert_call_start(mock_obj)
    check_export_obj(export_obj,
                     state=2,
                     output=None,
                     input=None,
                     rate=10,
                     next_export=None,
                     error=u'Error for export initialization: error_message')


def test_get_status_ok(use_oebs_api, person, firm, session, service_ticket_mock):
    """
    Получение статуса по плательщику вернуло OK, экспорт успешно завершен
    """
    export_obj = person.exports['OEBS_API']
    request_id = ob.get_big_number()
    firm2 = create_firm(session)
    export_obj.input = {'request_id': request_id,
                        'firms': [firm.id]}
    person.firms = [firm, firm2]
    person.session.flush()
    answer = [{"request_id": "25916", "entity_type": "CUSTOMER",
               "entity_id": "P71614921", "status": "OK",
               "oebs_entities": {"cust_number": "P" + str(ob.get_big_number())}}]
    with mock_post(answer) as mock_obj:
        QueueProcessor('OEBS_API').process_one(export_obj)
        person.session.flush()
    assert_call_check(mock_obj, request_id=request_id)
    check_export_obj(export_obj,
                     state=1,
                     output="Successfully exported Person, oebs_entities='{}'".format(
                         answer[0]['oebs_entities']),
                     input={'request_id': request_id,
                            'firms': [firm.id]},
                     rate=0,
                     next_export=None,
                     error=None)
    hamcrest.assert_that(
        person.person_firms,
        hamcrest.contains_inanyorder(
            hamcrest.has_properties(
                person=person,
                firm=firm2,
                oebs_export_dt=None
            ),
            hamcrest.has_properties(
                person=person,
                firm=firm,
                oebs_export_dt=hamcrest.is_not(None)
            )
        )
    )


@pytest.mark.parametrize(
    'current_rate', [0, 9],
    ids=['new', 'was_failed']
)
def test_get_status_wait(session, use_oebs_api, person, firm, current_rate, service_ticket_mock):
    """
    Получение статуса по плательщику вернуло WAIT, не повышаем rate
    """
    export_obj = person.exports['OEBS_API']
    request_id = ob.get_big_number()
    export_obj.input = {'request_id': request_id}
    export_obj.rate = current_rate
    person.firms = [firm]
    person.session.flush()
    answer = [{"request_id": "25916", "entity_type": "CUSTOMER",
               "entity_id": "P71614921", "status": "WAIT"}]
    with mock_post(answer) as mock_obj:
        QueueProcessor('OEBS_API').process_one(export_obj)
        person.session.flush()
    assert_call_check(mock_obj, request_id=request_id)
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output='Still waiting on export',
                     input={'request_id': request_id},
                     rate=current_rate,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=u'Retrying OEBS_API processing')


@pytest.mark.parametrize('answer', [
    {"result": "ERROR",
     "errors": ["Ошибка получения статуса"]},

    {"result": "ERROR",
     "errors": [None]},

    {"status": "ERROR",
     "message": "error_message"}
]
                         )
def test_get_status_fail_technical_mistake(session, use_oebs_api, person, firm, answer, service_ticket_mock):
    """
    Получение статуса по плательщику закончилось ошибкой
    """
    export_obj = person.exports['OEBS_API']
    request_id = ob.get_big_number()
    export_obj.input = {'request_id': request_id}
    person.firms = [firm]
    person.session.flush()
    with mock_post(answer) as mock_obj:
        QueueProcessor('OEBS_API').process_one(export_obj)
        person.session.flush()
    assert_call_check(mock_obj, request_id=request_id)
    if answer.get('status'):
        msg = u'Error while calling api: error_message'
    else:
        msg = u'Error while calling api: {}'.format(answer)
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output=None,
                     input={'request_id': request_id},
                     rate=1,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=msg)


@pytest.mark.parametrize('status_code', [403, 502, 200])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_handle_export_check_fail_500(session, use_oebs_api, person, firm, status_code, service_ticket_mock):
    """
    Экспорт завершился с http ошибкой 4XX-5XX, откладываем выгрузку без повышения рейта
    """
    export_obj = person.exports['OEBS_API']
    export_obj.rate = 9

    person.firms = [firm]
    export_obj.input = {'request_id': 124,
                        'firms': [firm.id]}
    person.session.flush()

    with mock_post([{"request_id": "-666666", 'status': 'OK'}], status_code) as mock_obj:
        qp = QueueProcessor('OEBS_API')
        qp.max_rate = 10
        qp.process_one(export_obj)
    person.session.flush()
    if status_code in [403, 502]:
        delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
        check_export_obj(export_obj,
                         state=0,
                         output=u'HTTP response status code {} was given, defer export'.format(status_code),
                         input=export_obj.input,
                         rate=9,
                         next_export=hamcrest.greater_than_or_equal_to(delay),
                         error=u'Retrying OEBS_API processing')
    else:
        check_export_obj(export_obj,
                         state=1,
                         output='Successfully exported Person, oebs_entities=\'{}\'',
                         input=export_obj.input,
                         rate=9,
                         next_export=None,
                         error=None)


@pytest.mark.parametrize('message', [u'java.sql.SQLException: ORA-12541: TNS:нет прослушивателя',
                                     u'ORA-12514: TNS:прослушиватель в данный момент не имеет данных о службе'])
def test_handle_export_check_fail_db_error(session, use_oebs_api, person, firm, message, service_ticket_mock):
    """
    Экспорт завершился с ошибкой доступа к базе данных ОЕБС, откладываем выгрузку без повышения рейта
    """
    export_obj = person.exports['OEBS_API']
    export_obj.rate = 9
    export_obj.input = {'request_id': 2323}
    person.firms = [firm]
    person.session.flush()

    with mock_post({"status": "ERROR",
                    "message": message}) as mock_obj:
        qp = QueueProcessor('OEBS_API')
        qp.max_rate = 10
        qp.process_one(export_obj)
        person.session.flush()
    assert_call_check(mock_obj, export_obj.input['request_id'])
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output=u'OEBS DatabaseError \'{}\' was thrown, defer export'.format(message),
                     input=export_obj.input,
                     rate=9,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=u'Retrying OEBS_API processing')


@pytest.mark.parametrize('errors', [[u"Ошибка получения статуса", u"Ошибка2"], [None]])
def test_get_status_fail_final_logical_mistake(use_oebs_api, person, firm, errors, service_ticket_mock):
    """
    Получение статуса по плательщику закончилось ошибкой
    """
    export_obj = person.exports['OEBS_API']
    request_id = ob.get_big_number()
    export_obj.input = {'request_id': request_id}
    export_obj.rate = 9
    person.firms = [firm]
    person.session.flush()
    answer = [{"result": "ERROR",
               "errors": errors}]
    with mock_post(answer) as mock_obj:
        qp = QueueProcessor('OEBS_API')
        qp.max_rate = 10
        qp.process_one(export_obj)
        person.session.flush()
    assert_call_check(mock_obj, request_id=request_id)
    error_msg = u'Error for export check: {}'.format(u'\n'.join(errors) if errors != [None] else u'Unknown OEBS error')
    check_export_obj(export_obj,
                     state=2,
                     output=None,
                     input=None,
                     rate=10,
                     next_export=None,
                     error=error_msg)


def test_get_status_error(session, use_oebs_api, person, firm, service_ticket_mock):
    """
    Пришел статус 'Ошибка'
    """
    export_obj = person.exports['OEBS_API']
    request_id = ob.get_big_number()
    export_obj.input = {'request_id': request_id}
    person.firms = [firm]
    person.session.flush()
    answer = [{"request_id": "25929",
               "entity_type": "CUSTOMER",
               "entity_id": "P71620472",
               "status": "ERROR",
               "errors": ["Статус 'Ошибка'"]}]
    with mock_post(answer) as mock_obj:
        QueueProcessor('OEBS_API').process_one(export_obj)
        person.session.flush()
    assert_call_check(mock_obj, request_id=request_id)
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    check_export_obj(export_obj,
                     state=0,
                     output=None,
                     input=None,
                     rate=1,
                     next_export=hamcrest.greater_than_or_equal_to(delay),
                     error=u"Error for export check: Статус 'Ошибка'")


def test_get_status_error_final(use_oebs_api, person, firm, service_ticket_mock):
    """
    Пришел статус 'Ошибка'
    """
    export_obj = person.exports['OEBS_API']
    request_id = ob.get_big_number()
    export_obj.input = {'request_id': request_id}
    export_obj.rate = 9
    person.firms = [firm]
    person.session.flush()
    answer = [{"request_id": "25929",
               "entity_type": "CUSTOMER",
               "entity_id": "P71620472",
               "status": "ERROR",
               "errors": ["Статус 'Ошибка'"]}]
    with mock_post(answer) as mock_obj:
        qp = QueueProcessor('OEBS_API')
        qp.max_rate = 10
        qp.process_one(export_obj)
        person.session.flush()
    assert_call_check(mock_obj, request_id=request_id)
    check_export_obj(export_obj,
                     state=2,
                     output=None,
                     input=None,
                     rate=10,
                     next_export=None,
                     error=u"Error for export check: Статус 'Ошибка'")
