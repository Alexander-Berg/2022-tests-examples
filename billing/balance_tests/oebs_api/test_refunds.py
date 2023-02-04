# -*- coding: utf-8 -*-

import datetime
import decimal
import json
import uuid

import hamcrest
import mock
import pytest

from balance import mapper
from balance.constants import *
from balance.queue_processor import QueueProcessor
from tests import object_builder as ob
from tests.balance_tests.oebs_api.conftest import (create_refund,
                                                   mock_post)

pytestmark = [
    pytest.mark.invoice_refunds,
]


class TestFlow(object):
    def test_ok(self, session, bank_cpf, service_ticket_mock):
        refund, export_obj = create_refund(bank_cpf)

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer):
            QueueProcessor('OEBS_API').process_one(export_obj)
        session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=1,
                rate=0,
                next_export=None,
                input=None,
                output='Successfully exported refund, request_id=%s' % answer['request_id']
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.exported,
                system_uid=str(answer['request_id']),
            ),
        )

    def test_already_exported(self, session, bank_cpf, service_ticket_mock):
        refund, export_obj = create_refund(bank_cpf)
        request_id = uuid.uuid4().int

        answer = {
            "result": "ERROR",
            "errors": [u"Возврат с идентификатором %s уже зарегистрирован в системе(%s)" % (refund.id, request_id)]
        }
        with mock_post(answer):
            QueueProcessor('OEBS_API').process_one(export_obj)
        session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=1,
                rate=0,
                next_export=None,
                input=None,
                output='Refund was already processed with request_id=%s' % request_id
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.exported,
                system_uid=str(request_id),
            ),
        )

    def test_fail(self, session, bank_cpf, service_ticket_mock):
        refund, export_obj = create_refund(bank_cpf)

        answer = {
            "result": "ERROR",
            "errors": ["абыр-абыр"],
        }
        on_dt = datetime.datetime.now().replace(microsecond=0)
        with mock_post(answer):
            QueueProcessor('OEBS_API').process_one(export_obj)
        session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=1,
                next_export=hamcrest.greater_than(on_dt),
                error=u'Error for export initialization: абыр-абыр'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.not_exported,
                system_uid=None,
            ),
        )

    def test_fail_final(self, session, bank_cpf, service_ticket_mock):
        refund, export_obj = create_refund(bank_cpf)
        export_obj.rate = 9
        session.flush()

        answer = {
            "result": "ERROR",
            "errors": ["абыр-абыр"],
        }
        with mock_post(answer):
            QueueProcessor('OEBS_API').process_one(export_obj)
        session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=2,
                rate=10,
                next_export=None,
                error=u'Error for export initialization: абыр-абыр'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.export_failed,
                system_uid=None,
            ),
        )

    def test_fail_api(self, session, bank_cpf, service_ticket_mock):
        refund, export_obj = create_refund(bank_cpf)

        answer = {
            'status': "ERROR",
            'message': 'А вы зачем эту ручку дёргаете? Дёргайте другую!',
        }
        on_dt = datetime.datetime.now().replace(microsecond=0)
        with mock_post(answer):
            QueueProcessor('OEBS_API').process_one(export_obj)
        session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=1,
                next_export=hamcrest.greater_than(on_dt),
                error=u'Error while calling api: А вы зачем эту ручку дёргаете? Дёргайте другую!'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.not_exported,
                system_uid=None,
            ),
        )


class TestData(object):
    @staticmethod
    def assert_start_call(mock_obj, req_data):
        assert mock_obj.call_count == 1
        (args, kwargs), = mock_obj.call_args_list
        data = json.loads(kwargs['data'], parse_float=decimal.Decimal)
        assert data == req_data

    def test_bank(self, session, bank_cpf, service_ticket_mock):
        refund, export_obj = create_refund(bank_cpf)

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)

        self.assert_start_call(
            mock_obj,
            {
                "entity_type": "RETURN",
                "entity_id": '%s' % refund.id,
                "payment_type": "BANK",
                "org_code": "YARU",
                "receipt_number": bank_cpf.cash_receipt_number,
                "operation_type": bank_cpf.operation_type,
                "source_id": bank_cpf.source_id,
                "trx_number": bank_cpf.receipt_number,
                "summa": 100,
                "bik": bank_cpf.bik,
                "account": bank_cpf.account_name,
                "inn": bank_cpf.inn,
                "customer_name": bank_cpf.customer_name,
                "return_date": datetime.datetime.now().strftime('%d.%m.%Y')
            }
        )

    def test_ym_builtin_reqs(self, session, ym_cpf, service_ticket_mock):
        refund, export_obj = create_refund(ym_cpf)
        payment = session.query(mapper.Payment).get(ym_cpf.orig_id)

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)

        self.assert_start_call(
            mock_obj,
            {
                "entity_type": "RETURN",
                "entity_id": '%s' % refund.id,
                "payment_type": "YMONEY",
                "org_code": "YARU",
                "receipt_number": ym_cpf.cash_receipt_number,
                "operation_type": ym_cpf.operation_type,
                "source_id": ym_cpf.source_id,
                "trx_number": ym_cpf.receipt_number,
                "transaction_num": payment.transaction_id,
                "wallet_num": payment.user_account,
                "summa": 100,
                "return_date": datetime.datetime.now().strftime('%d.%m.%Y')
            }
        )

    def test_ym_payload_reqs(self, session, ym_cpf, service_ticket_mock):
        payment = session.query(mapper.Payment).get(ym_cpf.orig_id)
        payment.transaction_id = None
        payment.user_account = None
        session.flush()
        refund, export_obj = create_refund(
            ym_cpf,
            decimal.Decimal('66.66'),
            {'transaction_num': 'transaction_id', 'wallet_num': 'user_account'}
        )

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)

        self.assert_start_call(
            mock_obj,
            {
                "entity_type": "RETURN",
                "entity_id": '%s' % refund.id,
                "payment_type": "YMONEY",
                "org_code": "YARU",
                "receipt_number": ym_cpf.cash_receipt_number,
                "operation_type": ym_cpf.operation_type,
                "source_id": ym_cpf.source_id,
                "trx_number": ym_cpf.receipt_number,
                "transaction_num": "transaction_id",
                "wallet_num": "user_account",
                "summa": decimal.Decimal('66.66'),
                "return_date": datetime.datetime.now().strftime('%d.%m.%Y')
            }
        )

    def test_wm_builtin_reqs(self, session, wm_cpf, service_ticket_mock):
        refund, export_obj = create_refund(wm_cpf)
        payment = session.query(mapper.Payment).get(wm_cpf.orig_id)

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)

        self.assert_start_call(
            mock_obj,
            {
                "entity_type": "RETURN",
                "entity_id": '%s' % refund.id,
                "payment_type": "WMONEY",
                "org_code": "YARU",
                "receipt_number": wm_cpf.cash_receipt_number,
                "operation_type": wm_cpf.operation_type,
                "source_id": wm_cpf.source_id,
                "trx_number": wm_cpf.receipt_number,
                "transaction_num": payment.transaction_id,
                "summa": 100,
                "return_date": datetime.datetime.now().strftime('%d.%m.%Y')
            }
        )

    def test_wm_payload_reqs(self, session, wm_cpf, service_ticket_mock):
        payment = session.query(mapper.Payment).get(wm_cpf.orig_id)
        payment.transaction_id = None
        session.flush()
        refund, export_obj = create_refund(
            wm_cpf,
            decimal.Decimal('66.66'),
            {'transaction_num': 'transaction_id'}
        )

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)

        self.assert_start_call(
            mock_obj,
            {
                "entity_type": "RETURN",
                "entity_id": '%s' % refund.id,
                "payment_type": "WMONEY",
                "org_code": "YARU",
                "receipt_number": wm_cpf.cash_receipt_number,
                "operation_type": wm_cpf.operation_type,
                "source_id": wm_cpf.source_id,
                "trx_number": wm_cpf.receipt_number,
                "transaction_num": "transaction_id",
                "summa": decimal.Decimal('66.66'),
                "return_date": datetime.datetime.now().strftime('%d.%m.%Y')
            }
        )

    def test_fail_reqs(self, session, bank_cpf, service_ticket_mock):
        refund, export_obj = create_refund(bank_cpf)
        bank_cpf.source_id = None
        session.flush()

        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)

        assert mock_obj.call_count == 0
        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=2,
                rate=1,
                input=None,
                error='Invalid requisites for refund with handler OEBSBankPaymentHandler: missing source_id'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.export_failed,
                system_uid=None,
            ),
        )
