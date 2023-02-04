# coding: utf-8
"""
Тестирование выгрузки платажей в логброкер.
"""

import mock
import pytest
import simplejson

import tests.object_builder as ob
from balance import constants, mapper
from balance.mapper.exportable_ng import ExportNg
from balance.processors.logbroker_proc import process_logbroker_payments
from balance.son_schema import payments


def find_export_ng_task(session, payment):
    """Ищет платеж в очереди LOGBROKER-PAYMENTS"""

    return session \
        .query(ExportNg) \
        .filter((ExportNg.type == 'LOGBROKER-PAYMENTS')
                & (ExportNg.object_id == payment.id)
                & (ExportNg.state == constants.ExportState.enqueued)
                & (ExportNg.in_progress.is_(None))) \
        .one_or_none()


def enable_payments_export(session, value=1):
    """Включает/выключает feature-флаг выгрузки платежей в логброкер"""
    session.config.__dict__['USE_PAYMENTS_EXPORT_NG'] = value


class TestPaymentEnqueue:
    """Тестирует, что при создании и изменении платежей изменяется version_id и платеж проставляется в export_ng"""

    @pytest.mark.parametrize('use_payment_export_ng', [0, 1])
    def test_create_payment_without_invoice(self, session, use_payment_export_ng):
        """Тестирует, что при создании платежа без инвойса платеж не попадает в выгрузку.
        Но после установки инвойса этот платеж выгружается.
        Так создает платежи траст - сначала без инвойса, потом проставляет invoice_id"""

        enable_payments_export(session, use_payment_export_ng)

        payment = mapper.TrustPayment(None)
        session.add(payment)

        expected_object_version_id = 0
        expected_export_version_id = None
        self._check_exported_with_version_id(session, payment, expected_object_version_id, expected_export_version_id)

        invoice = ob.InvoiceBuilder.construct(session)
        payment.invoice_id = invoice.id

        expected_object_version_id += 1
        expected_export_version_id = 0 if use_payment_export_ng else None
        self._check_exported_with_version_id(session, payment, expected_object_version_id, expected_export_version_id)

    @pytest.mark.parametrize('use_payment_export_ng', [0, 1])
    def test_change_payment_fields(self, session, use_payment_export_ng):
        """Тестирует, что при создании нового платежа или изменении его полей, платеж выгружается"""

        enable_payments_export(session, use_payment_export_ng)

        # При создании платежа с инвойсом должна появиться задача в export_ng на выгрузку в логброкер

        invoice = ob.InvoiceBuilder.construct(session)
        payment = ob.TrustPaymentBuilder.construct(session, invoice=invoice)
        session.add(payment)

        expected_object_version_id = 0
        expected_export_version_id = 0 if use_payment_export_ng else None
        self._check_exported_with_version_id(session, payment, expected_object_version_id, expected_export_version_id)

        # После установки другого инвойса, платеж должен обновиться и выгрузиться

        invoice2 = ob.InvoiceBuilder().build(session).obj
        payment.invoice = invoice2

        expected_object_version_id += 1
        expected_export_version_id = (expected_export_version_id + 1) if use_payment_export_ng else None
        self._check_exported_with_version_id(session, payment, expected_object_version_id, expected_export_version_id)

        # После обнуления инвойса, платеж должен обновиться, но НЕ должен выгрузиться

        payment.invoice = None

        expected_object_version_id += 1
        expected_export_version_id = (expected_export_version_id + 0) if use_payment_export_ng else None
        self._check_exported_with_version_id(session, payment, expected_object_version_id, expected_export_version_id)

        # При установке invoice_id, платеж должен обновиться и выгрузиться

        payment.invoice_id = invoice2.id

        expected_object_version_id += 1
        expected_export_version_id = (expected_export_version_id + 1) if use_payment_export_ng else None
        self._check_exported_with_version_id(session, payment, expected_object_version_id, expected_export_version_id)

        # При изменении других свойств платежа, платеж не должен обновляться и не должен выгрузиться

        payment.amount = 555

        expected_object_version_id += 0
        expected_export_version_id = (expected_export_version_id + 0) if use_payment_export_ng else None
        self._check_exported_with_version_id(session, payment, expected_object_version_id, expected_export_version_id)

    def _check_exported_with_version_id(self, session, payment, payment_version_id, export_version_id=None):
        """Проверяет, что у платежа установлен заданный version_id и платеж выгружен в очередь с заданным version_id"""

        # выключаем оптимизации в целях тестирования - чтобы каждое изменение платежа попадало в БД
        payment.exportable_ng_use_export_store = False
        payment.versionable_use_export_store = False

        # flush и refresh, чтобы обновились version_id у объектов
        session.flush()
        session.refresh(payment)
        assert payment.version_id == payment_version_id

        export_obj = find_export_ng_task(session, payment)
        if export_version_id is not None:
            assert export_obj is not None
            session.refresh(export_obj)
            assert export_obj.version_id == export_version_id
        else:
            assert export_obj is None


class TestLogbroker:
    """Тестирует отправку платежа в логброкер"""

    def test_send(self, session):
        enable_payments_export(session, 1)
        payment = ob.TrustPaymentBuilder.construct(session)
        json_data = simplejson.dumps({
            'obj': payments.PaymentSchema().dump(payment).data,
            'classname': 'Payment',
            'version': payment.version_id or 0,
        }, ensure_ascii=False, use_decimal=True).encode("UTF-8")
        with mock.patch('balance.processors.logbroker_proc._write_batch') as _write_mock:
            export_ng_object = find_export_ng_task(session, payment)
            process_logbroker_payments([export_ng_object])
            _write_mock.assert_called_once_with('lbkx', 'payments', [json_data])
