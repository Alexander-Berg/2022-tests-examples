# -*- coding: utf-8 -*-
import logging

import cx_Oracle
import mock
import pytest
import requests
import sentry_sdk
import sqlalchemy as sa
from errorboosterclient.sentry import ErrorBoosterTransport
from requests.adapters import HTTPAdapter
from sentry_sdk.integrations.logging import LoggingIntegration

from balance import exc
from balance.constants import ExportState
from cluster_tools.yt_import_client_cashback import ImportClientCashback
from balance.queue_processor import QueueProcessor, process_object
from balance.queue_processor_deco import processors
from butils.error_booster import CustomEventConverter, CustomEventHandler
from medium.medium_logic import batch_error_serialize
from tests.object_builder import ClientBuilder


def run_error_booster():
    sent = []

    def send_me(event):
        sent.append(event)

    transport = ErrorBoosterTransport(
        project='myproject',
        sender=send_me,
        bgworker=False,
    )

    transport.converter_cls = CustomEventConverter
    sentry_logging = LoggingIntegration(
        level=None,
        event_level=logging.ERROR
    )

    if sentry_logging._handler:
        sentry_logging._handler = CustomEventHandler(level=logging.ERROR)

    sentry_sdk.init(
        transport=transport,
        debug=True,
        integrations=[sentry_logging]
    )
    return sent


@pytest.mark.parametrize('error_msg, error_args',
                         [
                             ('No payment options available', tuple()),
                             ('No payment options %s %s available', ('123', '34'))
                         ])
def test_batch_error_serialize(error_msg, error_args):
    sent = run_error_booster()

    def f_w_exc():
        if error_args:
            raise exc.INVALID_PARAM(error_msg, *error_args)
        else:
            raise exc.INVALID_PARAM(error_msg)

    _, exc_str = batch_error_serialize(f_w_exc)()
    assert exc_str == '<error>' \
                      '<msg>Invalid parameter for function: %s</msg>' \
                      '<wo-rollback>0</wo-rollback>' \
                      '<method>f_w_exc</method>' \
                      '<code>INVALID_PARAM</code>' \
                      '<parent-codes>' \
                      '<code>EXCEPTION</code>' \
                      '</parent-codes>' \
                      '<contents>Invalid parameter for function: %s</contents>' \
                      '</error>' % (error_msg % error_args, error_msg % error_args)
    assert sent[0]['message'] == 'Invalid parameter for function: {}'.format(error_msg)


def test_xmlprc_exc_with_format():
    sent = run_error_booster()

    def f_w_exc():
        e = exc.EXCEPTION('exc_text')
        e._format = 'non formatted exc text'
        raise e

    _, exc_str = batch_error_serialize(f_w_exc)()
    assert sent[0]['message'] == 'non formatted exc text'


def test_exc_with_format():
    sent = run_error_booster()

    def f_w_exc():
        raise sa.exc.DatabaseError(None, None, cx_Oracle.DatabaseError(44, 'df'))

    _, exc_str = batch_error_serialize(f_w_exc)()
    assert sent[0]['message'] == "Error: DatabaseError Description: (cx_Oracle.DatabaseError) (44, 'df')"


def test_connection_error():
    sent = run_error_booster()

    def f_w_exc():
        s = requests.Session()
        s.mount('http://tes3434534t777.com', HTTPAdapter(max_retries=0))
        requests.get("http://tes3434534t777.com/3443/443", timeout=100)

    _, exc_str = batch_error_serialize(f_w_exc)()
    assert sent[0][
               'message'] == "Error: ConnectionError Description: HTTPConnectionPool(host='tes3434534t777.com', port=80):" \
                             " Max retries exceeded with url: /3443/443"


def test_trust_api():
    sent = run_error_booster()

    def f_w_exc():
        raise exc.TRUST_API_EXCEPTION("Can't bind more than 5 cards to one user", 'too_many_cards')

    _, exc_str = batch_error_serialize(f_w_exc)()
    assert sent[0]['message'] == "Error in trust api call: too_many_cards"


def test_flush_error(session):
    sent = run_error_booster()

    def f_w_exc():
        raise sa.orm.exc.FlushError()

    _, exc_str = batch_error_serialize(f_w_exc)()
    assert sent[0]['message'] == 'Error: FlushError Description: FlushError'


def test_invalid_person_type():
    sent = run_error_booster()

    def f_w_exc():
        raise exc.INVALID_PERSON_TYPE('ur', verbose=True, cpc='ut, ph')

    _, exc_str = batch_error_serialize(f_w_exc)()
    assert sent[0]['message'] == u'Cannot create person with type = %(person_type)s, available types: %(cpc)s'


def test_method_from_tag(session, medium_xmlrpc):
    sent = run_error_booster()
    try:
        medium_xmlrpc.CreateClient()
    except Exception as e:
        assert sent[0]['method'] == 'CreateClient'


def test_event_wo_vars(session, medium_xmlrpc):
    sent = run_error_booster()
    try:
        medium_xmlrpc.CreateClient()
    except Exception as e:
        for var in sent[0]['additional']['vars']:
            assert var['vars'] == {}


@pytest.fixture(scope='session')
def export_queue():
    return "SMS_NOTIFY"


@pytest.fixture()
def client_obj(session):
    return ClientBuilder().build(session).obj


def fail(*args, **kwargs):
    raise RuntimeError("foobar")


def test_queue_from_tag_queue_processor(session, export_queue, client_obj):
    sent = run_error_booster()

    client_obj.enqueue(export_queue)
    export_obj = client_obj.exports[export_queue]
    assert export_obj.state == ExportState.enqueued

    qp = QueueProcessor(export_queue, max_rate=1)
    with mock.patch.dict(processors[export_queue], {'Client': fail}):
        qp.process_one(export_obj)

    assert export_obj.state == ExportState.failed
    assert sent[0]['sourceMethod'] == export_queue


def test_queue_from_tag_process_object(session, export_queue, client_obj):
    sent = run_error_booster()

    classname = 'Client'
    with mock.patch.dict(processors[export_queue], {classname: fail}):
        process_object(
            session,
            export_queue,
            classname,
            client_obj.id
        )

    assert sent[0]['sourceMethod'] == export_queue


def test_yt_import_client_cashback(session):
    from butils import logger
    log = logger.get_logger('test_logger')

    sent = run_error_booster()

    class TestImportClientCashback(ImportClientCashback):
        def _get_tables(self):
            # логирование ошибок вынесено в YtImportClientCashbackApp.main, поэтому логируем ошибку прям в тесте
            log.error('Failed to import client cashback')
            raise Exception

    try:
        with mock.patch('balance.utils.yt_helpers.get_token'):
            TestImportClientCashback(session).import_from_yt()
    except Exception as e:
        assert sent[0]['sourceMethod'] == 'yt_import_client_cashback'
