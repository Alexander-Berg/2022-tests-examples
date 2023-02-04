# -*- coding: utf-8 -*-

import datetime
import decimal
import json
import hamcrest
import httpretty

import pytest

from balance.application import getApplication
from balance.constants import *
from balance.queue_processor import QueueProcessor
from tests import object_builder as ob
from tests.balance_tests.oebs_api.conftest import (
    assert_call_check,
    check_export_obj,
    create_invoice_transfer,
    mock_post,
)
from tests.balance_tests.invoice_transfer.test_actions import src_invoice, dst_invoice

pytestmark = [
    pytest.mark.invoice_transfer,
]


@pytest.fixture(autouse=True)
def patch_config(session):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'InvoiceTransfer': 1}


class TestData(object):
    @staticmethod
    def assert_start_call(mock_obj, req_data):
        assert mock_obj.call_count == 1
        (args, kwargs), = mock_obj.call_args_list
        data = json.loads(kwargs['data'], parse_float=decimal.Decimal)
        assert data == req_data

    def test_data(self, session, src_invoice, dst_invoice, service_ticket_mock):
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)

        self.assert_start_call(
            mock_obj,
            {
                "entity_type": "TRANSFER-CASH",
                "entity_id": '%s' % invoice_transfer.id,
                "bill_from": src_invoice.external_id,
                "bill_to": dst_invoice.external_id,
                "amount": 100
            }
        )


class TestStatus(object):
    def test_success(self, session, src_invoice, dst_invoice, service_ticket_mock):
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        request_id = ob.get_big_number()
        export_obj.input = {'request_id': request_id}
        session.flush()

        answer = [{
            "status": "OK",
            "request_id": request_id
        }]

        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)
            session.flush()
        assert_call_check(mock_obj, request_id=request_id)
        check_export_obj(export_obj,
                         state=1,
                         output="Successfully exported InvoiceTransfer, oebs_entities='{}'",
                         input={'request_id': request_id},
                         rate=0,
                         next_export=None,
                         error=None)
        assert invoice_transfer.status_code == InvoiceTransferStatus.exported
        assert invoice_transfer.status_descr is None

    def test_start_final_error(self, session, src_invoice, dst_invoice, service_ticket_mock):
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        export_obj.rate = 9
        session.flush()

        with mock_post({"result": "ERROR",
                        "errors": ['error_message_1', 'error_message_2']}) as mock_obj:
            qp = QueueProcessor('OEBS_API')
            qp.max_rate = 10
            qp.process_one(export_obj)
            session.flush()

        check_export_obj(export_obj,
                         state=2,
                         output=None,
                         input=None,
                         rate=10,
                         next_export=None,
                         error=u'Error for export initialization: error_message_1\nerror_message_2')
        assert invoice_transfer.status_code == InvoiceTransferStatus.export_failed
        assert invoice_transfer.status_descr == 'error_message_1\nerror_message_2'

    def test_final_get_status_fail_technical_mistake(self, session, src_invoice, dst_invoice, service_ticket_mock):
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        request_id = ob.get_big_number()
        export_obj.input = {'request_id': request_id}
        export_obj.rate = 9
        session.flush()

        with mock_post({"status": "ERROR", "message": "error_message"}) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)
            session.flush()
        assert_call_check(mock_obj, request_id=request_id)
        check_export_obj(export_obj,
                         state=2,
                         output=None,
                         input={'request_id': request_id},
                         rate=10,
                         next_export=None,
                         error=u"Error while calling api: error_message")
        assert invoice_transfer.status_code == InvoiceTransferStatus.export_failed
        assert invoice_transfer.status_descr == 'Error while calling api: error_message'

    def test_final_error(self, session, src_invoice, dst_invoice, service_ticket_mock):
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        request_id = ob.get_big_number()
        export_obj.input = {'request_id': request_id}
        export_obj.rate = 9
        session.flush()

        with mock_post([{"result": "ERROR",
                        "errors": ['error_message_1', 'error_message_2']}]):
            qp = QueueProcessor('OEBS_API')
            qp.max_rate = 10
            qp.process_one(export_obj)
            session.flush()

        check_export_obj(export_obj,
                         state=2,
                         output=None,
                         input=None,
                         rate=10,
                         next_export=None,
                         error=u"Error for export check: error_message_1\nerror_message_2")
        assert invoice_transfer.status_code == InvoiceTransferStatus.export_failed
        assert invoice_transfer.status_descr == 'error_message_1\nerror_message_2'


class TestHandler(object):
    def mock_error_status(self, error_msg):
        for key in ('Url', 'CloudUrl'):
            httpretty.register_uri(
                httpretty.POST,
                getApplication().get_component_cfg('oebs_api')[key] + 'getStatusBilling',
                json.dumps([{"request_id": "3499129",
                             "entity_type": "TRANSFER-CASH",
                             "entity_id": "118811909",
                             "status": "ERROR",
                             "errors": [error_msg]}]))

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_lost_request_id(self, session, src_invoice, dst_invoice, service_ticket_mock):
        """
        Если мы потеряли request_id переноса (например, упал разборщик до коммита) при повторной попытке
        выгрузки оебс вернет соот-вую ошибку и потерянный request_id
        """
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        initial_request_id = ob.generate_int(10)
        request_id = ob.generate_int(10)

        export_obj.input = {'request_id': request_id}
        error_msg = u"Перенос с идентификатором {} уже зарегистрирован в системе с request_id={}.".format(
            invoice_transfer.id, initial_request_id
        )
        self.mock_error_status(error_msg)
        QueueProcessor('OEBS_API').process_one(export_obj)

        session.flush()
        delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
        check_export_obj(export_obj,
                         state=0,
                         output='Still waiting on export',
                         error='Retrying OEBS_API processing',
                         input={'orig_request_id': request_id,
                                'request_id': initial_request_id},
                         next_export=hamcrest.greater_than_or_equal_to(delay))
        # проверяем, что следующий запрос будет с новым request_id
        for key in ('Url', 'CloudUrl'):
            httpretty.register_uri(
                httpretty.POST,
                getApplication().get_component_cfg('oebs_api')[key] + 'getStatusBilling',
                json.dumps([{"status": "OK"}]))
        QueueProcessor('OEBS_API').process_one(export_obj)
        assert httpretty.last_request().body == json.dumps([{"request_id": initial_request_id}])

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_lost_request_id_is_same(self, session, src_invoice, dst_invoice, service_ticket_mock):
        """
        Если мы потеряли request_id переноса (например, упал разборщик до коммита) и при повторной попытке
        выгрузки оебс вернет нам тот же request_id, падаем с обычной ошибкой
        """
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        request_id = ob.generate_int(10)
        export_obj.input = {'request_id': request_id}
        error_msg = u"Перенос с идентификатором {} уже зарегистрирован в системе с request_id={}.".format(
            invoice_transfer.id, request_id
        )
        self.mock_error_status(error_msg)
        QueueProcessor('OEBS_API').process_one(export_obj)
        session.flush()
        delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
        check_export_obj(export_obj,
                         state=0,
                         rate=1,
                         output=None,
                         error=u'Error for export check: {}'.format(error_msg),
                         input=None,
                         next_export=hamcrest.greater_than_or_equal_to(delay))

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_lost_request_id_is_unknown(self, session, src_invoice, dst_invoice, service_ticket_mock):
        invoice_transfer, export_obj = create_invoice_transfer(src_invoice, dst_invoice)

        export_obj.input = {'request_id': ob.generate_int(10)}
        error_msg = u"Перенос с идентификатором {} уже зарегистрирован в системе с request_id=.".format(
            invoice_transfer.id
        )
        self.mock_error_status(error_msg)
        QueueProcessor('OEBS_API').process_one(export_obj)
        session.flush()
        delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
        check_export_obj(export_obj,
                         state=0,
                         rate=1,
                         output=None,
                         error=u'Error for export check: {}'.format(error_msg),
                         input=None,
                         next_export=hamcrest.greater_than_or_equal_to(delay))
