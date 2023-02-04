# coding: utf-8
import pickle
import textwrap
import datetime
import pytest
import mock

from butils.sender import Sender

from balance import exc
from balance.mapper import Paysys, EmailMessage
from balance.mapper.exportable import SkipExport
from balance.mailing.fraud import notify_balance_support
from balance.constants import *
from balance.processors.email_message import run_email_process
from balance.queue_processor import QueueProcessor

import mailer

from tests.object_builder import (
    Getter,
    InvoiceBuilder,
)


YA_MONEY_PAYSYS_ID = 1000
PH_BANK_PAYSYS_ID = 1001


class TestFraud(object):
    def build_invoice(self, session, paysys, manager=None):
        invoice = InvoiceBuilder(
            paysys=paysys,
            manager=manager,
            receipt_dt=session.now() - datetime.timedelta(days=10),
            receipt_sum_1c=0,
            receipt_sum=1,
            consume_sum=1,
        ).build(session).obj
        invoice.postpay = 1

        session.flush()

        return invoice

    def test_notify_balance_support(self, session):
        paysys = Getter(Paysys, YA_MONEY_PAYSYS_ID)

        self.build_invoice(session, paysys)

        notify_balance_support(session)
        emails = [obj for obj in session.new if isinstance(obj, EmailMessage)]
        assert len(emails) == 1

        email = emails[0]
        subject, body, _, attachments = pickle.loads(email.data)

        assert subject == u"Возможный фрод"
        assert body == textwrap.dedent(
            u"""\
            Добрый день, Администрация Баланса!

            Счета, приложенные к письму, включены и до сих пор не оплачены.

            Внимание! Убедитесь, что денег на счёте на текущий момент всё ещё нет (сделать это можно, перейдя по ссылке на номер счёта в Баланс - информация о приходе денег находится в разделе "Платежи").

            С уважением,
            администрация Баланса"""
        )


@pytest.mark.email
class TestEmail(object):
    export_type = 'EMAIL_MESSAGE'
    subject = u'Test Mail'
    recepient_email = 'test@test.ru'
    recepient_name = 'Pupkin pup'
    memo = 'Memo momo'
    object_id = 0
    opcode = GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE
    data = pickle.dumps((subject, (recepient_email, recepient_name)), protocol=pickle.HIGHEST_PROTOCOL)
    invalid_email = '..invalid+email@yandex.net'

    @pytest.fixture(params=[0, 1], ids=['wo_delete', 'w_delete'])
    def cfg_delete_data(self, request, session):
        session.config.__dict__['DELETE_T_MESSAGE_DATA'] = request.param
        return request.param

    def _get_message(self, session, **kwargs):
        email_params = {'opcode': self.opcode, 'object_id': self.object_id, 'recepient_name': self.recepient_name,
                        'recepient_address': self.recepient_email, 'memo': self.memo, 'data': self.data}
        email_params.update(kwargs)
        message = EmailMessage(**email_params)
        session.add(message)
        session.flush()
        return message

    def _success_send(self, *args):
        return True

    def _error_send(self, *args):
        raise exc.MESSAGE_ERROR('rcpt_email', 'rcpt_name')

    def test_new_export(self, session):
        message = self._get_message(session)
        exports = message.exports
        assert self.export_type in message.get_export_types()
        assert self.export_type in exports
        export = exports[self.export_type]
        assert export.rate == 0
        assert export.state == 0

    @mock.patch('butils.sender.Sender._do_send_smtplib', _error_send)
    @pytest.mark.usefixtures('cfg_delete_data')
    def test_send_error(self, session):
        message = self._get_message(session)
        with pytest.raises(exc.CRITICAL_ERROR):
            run_email_process(message)  # вызывается ошибка при отправке сообщения
        assert message.sent == 2
        assert message.data == self.data

    @mock.patch('butils.sender.Sender._do_send_smtplib', _success_send)
    def test_queue_send(self, session, cfg_delete_data):
        message = self._get_message(session)
        export = message.exports[self.export_type]
        qp = QueueProcessor(self.export_type)
        qp.process_one(export)
        assert message.sent == 1
        assert message.data == (None if cfg_delete_data else self.data)
        assert export.state == ExportState.exported
        assert export.output =='Success sent email id="%s" and opcode="%s" for %s' % (
            message.id, message.opcode, message.recepient_address
        )

    @mock.patch('butils.sender.Sender._do_send_smtplib', _error_send)
    @pytest.mark.usefixtures('cfg_delete_data')
    def test_queue_send_error(self, session):
        message = self._get_message(session)
        export = message.exports[self.export_type]
        qp = QueueProcessor(self.export_type)
        qp.process_one(export)  # Ошибка обрабатывается в очереди
        assert message.sent == 2
        assert message.data == self.data
        assert export.state == ExportState.failed
        assert export.rate == 1

    @mock.patch('butils.sender.Sender._do_send_smtplib', _success_send)
    def test_message_was_sent_previously(self, session):
        message = self._get_message(session)
        message.sent = 1
        session.flush()
        with pytest.raises(SkipExport):
            run_email_process(message)  # вызывается ошибка при отправке сообщения

    @mock.patch('butils.sender.Sender._do_send_smtplib', _success_send)
    def test_queue_message_was_sent_previously(self, session):
        message = self._get_message(session)
        message.sent = 1
        export = message.exports[self.export_type]
        qp = QueueProcessor(self.export_type)
        qp.process_one(export)  # Ошибка обрабатывается в очереди
        assert message.sent == 1
        assert export.state == ExportState.exported
        assert export.rate == 0

    @mock.patch('butils.sender.Sender._do_send_smtplib', _success_send)
    @mock.patch('mailer.balance_mailer.MessageData._get_recp_address', return_value={invalid_email})
    def test_raise_invalid_email(self, mock_fun, session):
        message = self._get_message(session, recepient_address=self.invalid_email)
        with pytest.raises(exc.INVALID_EMAIL):
            run_email_process(message)

    @mock.patch('butils.sender.Sender._do_send_smtplib', _success_send)
    def test_raise_empty_email(self, session):
        empty_data = pickle.dumps((u'', u'', (self.recepient_email, u''), None), protocol=pickle.HIGHEST_PROTOCOL)
        message = self._get_message(session, memo=None, data=empty_data)
        with pytest.raises(exc.EMPTY_EMAIL):
            run_email_process(message)

    @mock.patch('butils.sender.Sender._do_send_smtplib', _success_send)
    def test_raise_disabled_email_opcode(self, session):
        message = self._get_message(session)
        session.execute(
            "update bo.t_config set value_json='%s' where item='DISABLED_EMAIL_OPCODES'" % [self.opcode]
        )
        session.flush()
        with pytest.raises(exc.DISABLED_EMAIL_OPCODE):
            run_email_process(message)
