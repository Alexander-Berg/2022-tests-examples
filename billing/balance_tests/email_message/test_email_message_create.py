# -*- coding: utf-8 -*-
import datetime
import pickle
import pytest

from balance import muzzle_util as ut, overdraft
from balance.actions import invoice_turnon
from balance.constants import (GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE,
                               INVOICE_FORM_CREATOR_MESSAGE_OPCODE,
                               HTML_MESSAGE_CREATOR_MESSAGE_OPCODE,
                               GENERIC_MAKO_CREATOR_MESSAGE_OPCODE)
from balance.mapper import EmailMessage
from balance.person import update_by_request
from balance.processors import cash_register
from mailer import balance_mailer
from tests import object_builder as ob

NOW = datetime.datetime.now()


def create_manager(session):
    return ob.SingleManagerBuilder().build(session).obj


def create_invoice(session):
    return ob.InvoiceBuilder().build(session).obj


def create_person(session):
    return ob.PersonBuilder().build(session).obj


@pytest.mark.email
class TestCreateMessage(object):
    def _check_message(self, message, opcode, data=None, object_id=0, recepient_name=None, recepient_address=None,
                       memo_c=None,
                       sent_dt=None):
        if data is None:
            assert message.data is None
        else:
            assert pickle.loads(message.data) == data
        assert message.object_id == object_id
        assert message.recepient_name == recepient_name
        assert message.recepient_address == recepient_address
        assert message.sent == 0
        assert message.memo_c == memo_c
        assert message.opcode == opcode
        if sent_dt:
            assert message.send_dt is not None
        else:
            assert message.send_dt is None

    def _check_message_data(self, message_data, attach_list=None, bcc=None, body='', multipart='', rcpt=None,
                            rcpt_name=None,
                            reply_to_email=None, reply_to_name=None, sender=None, sender_name=None, subject=''):
        assert message_data.attach_list == attach_list
        assert message_data.bcc == bcc
        assert message_data.body == body
        assert message_data.multipart == multipart
        assert message_data.rcpt == rcpt
        assert message_data.rcpt_name == rcpt_name
        assert message_data.reply_to_email == reply_to_email
        assert message_data.reply_to_name == reply_to_name
        assert message_data.sender == sender
        assert message_data.sender_name == sender_name
        assert message_data.subject == subject

    def test_reset_overdraft_message(self, session, app):
        manager = create_manager(session)
        invoice = create_invoice(session)
        g_invoices = [(invoice.client, [invoice])]
        overdraft.Overdraft(session)._mail_reset_overdraft_manager(on_now=NOW, manager=manager, g_invoices=g_invoices)
        messages = [object for object in session.new if isinstance(object, EmailMessage)]
        body = app.mako_renderer.render('reset_overdraft/mail.mako',
                                        manager=manager, g_invoices=g_invoices, on_now=NOW)
        subject = u'Приостановка оказания услуг из-за неоплаты овердрафтных счетов. ' + NOW.strftime('%d.%m.%Y')
        assert len(messages) == 1
        self._check_message(messages[0], data=(subject, body), recepient_name=manager.name,
                            recepient_address=manager.safe_email,
                            opcode=GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE)

    def test_invoice_success_message(self, session, muzzle_logic):
        invoice = create_invoice(session)
        muzzle_logic.send_invoice_ex(session,
                                     invoice_id=invoice.id,
                                     recepient='test@test.ru',
                                     mail_memo='test_mail_memo',
                                     manager_cc=0,
                                     msg_fmt=INVOICE_FORM_CREATOR_MESSAGE_OPCODE)

        message = session.query(EmailMessage).getone(object_id=invoice.id, opcode=INVOICE_FORM_CREATOR_MESSAGE_OPCODE)
        self._check_message(message,
                            data=None,
                            recepient_name=None,
                            recepient_address='test@test.ru',
                            memo_c='test_mail_memo',
                            opcode=INVOICE_FORM_CREATOR_MESSAGE_OPCODE,
                            object_id=invoice.id)

    @pytest.mark.parametrize('person_type, expected_title', [
        ('yt_kzp', u'Papers for person "person_name" have been verified'),
        ('yt_kzu', u'Papers for person "person_name" have been verified'),
        ('ytph', u'Papers for person "person_name" have been verified'),
        ('by_ytph', u'Papers for person "person_name" have been verified'),
        ('sw_ur', u'Papers for person "person_name" have been verified'),
        ('sw_ph', u'Papers for person "person_name" have been verified'),
        ('sw_yt', u'Papers for person "person_name" have been verified'),
        ('de_ur', u'Papers for person "person_name" have been verified'),
        ('de_yt', u'Papers for person "person_name" have been verified'),
        ('de_ph', u'Papers for person "person_name" have been verified'),
        ('sw_ytph', u'Papers for person "person_name" have been verified'),
        ('de_ytph', u'Papers for person "person_name" have been verified'),
        ('unexist', '')
    ])
    def test_person_docs_verified_message(self, session, app, person_type, expected_title):
        person = create_person(session)
        person.verified_docs = 0
        person.name = 'person_name'
        person.type = person_type
        if person_type == 'yt_kzu':
            person.iik = 'iik'
            person.bik = 'bik'
        update_by_request(person, {'verified-docs': 1})
        messages = [_object for _object in session.new if isinstance(_object, EmailMessage)]
        if person_type == 'unexist':
            assert len(messages) == 0
        else:
            assert len(messages) == 1
            data = (expected_title,
                    ('info-noreply@support.yandex.com', u'Яндекс.Баланс'),
                    [],
                    'verified_docs/response.mako',
                    {})
            self._check_message(messages[0], data=data, recepient_name=person.sensible_name, object_id=person.id,
                                recepient_address=person.email, opcode=GENERIC_MAKO_CREATOR_MESSAGE_OPCODE)

    def test_overdraft_cancellation_message(self, session):
        paid_amount = 3034
        invoice = create_invoice(session)
        manager = ob.SingleManagerBuilder().build(session).obj
        invoice.manager = manager
        turn_on = invoice_turnon.InvoiceTurnOn(invoice, sum=paid_amount)
        turn_on._overdraft_cancellation_message(NOW)
        messages = [object for object in session.new if isinstance(object, EmailMessage)]
        assert len(messages) == 1
        fmt_dt = lambda on_dt: on_dt.strftime('%d.%m.%Y')
        params = {'invoice_id': invoice.id,
                  'invoice_external_id': invoice.external_id,
                  'invoice_dt': fmt_dt(invoice.dt),
                  'paid_amount': ut.round00(paid_amount),
                  'invoice_currency': invoice.currency,
                  'person_name': invoice.person and invoice.person.name,
                  'paysys_name': invoice.paysys and invoice.paysys.name,
                  'person_id': invoice.person and invoice.person.id,
                  'turnon_dt': fmt_dt(NOW),
                  'comment': (u'Комментарий: ' + invoice.client.manual_suspect_comment.decode('utf-8')
                              if invoice.client and invoice.client.manual_suspect_comment else u'')}
        subj = u'Получена предоплата от плательщика %(person_name)s (id %(person_id)s) с просроченным овердрафтом' % params
        body = u'''Оплата по счету %(invoice_external_id)s (https://admin.balance.yandex.ru/invoice.xml?invoice_id=%(invoice_id)s) от %(invoice_dt)s.
Сумма оплаты %(paid_amount)s %(invoice_currency)s.
Способ оплаты %(paysys_name)s.
Плательщик %(person_name)s (id %(person_id)s).
%(comment)s
--
%(turnon_dt)s
''' % params
        self._check_message(messages[0], data=(subj, body), recepient_name=invoice.manager.name,
                            recepient_address=invoice.manager.safe_email,
                            opcode=GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE)

    def test_cash_message(self, session):
        receipt_html = 'recept_html'
        reply_to_email = 'reply_to_email'
        invoice = create_invoice(session)
        person = invoice.person
        message = cash_register.enqueue_email(invoice, payment_id=-1, receipt_html=receipt_html,
                                              reply_to_email=reply_to_email)
        subject = u'Электронный чек'
        body = receipt_html
        attachments = []
        self._check_message(message, object_id=-1, data=(subject, body, (reply_to_email, u'Yandex'), attachments),
                            recepient_name=person.name,
                            recepient_address=person.email,
                            opcode=HTML_MESSAGE_CREATOR_MESSAGE_OPCODE)

    def test_check_message_creator_generic_message(self, session):
        suitable_message_creator = balance_mailer.ACTION_MAP[GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE]
        mail_processor = balance_mailer.MailProcessor()
        data = pickle.dumps(['subject, body'])
        message_creator = suitable_message_creator(mail_processor.cfg, 0, data, session=session)
        bcc = message_creator.get_bcc()
        m_sender_p = message_creator.get_sender()
        reply_to = message_creator.get_reply_to()
        assert bcc is None
        assert m_sender_p is None
        assert reply_to is None

    def test_check_message_creator_html_message(self, session):
        suitable_message_creator = balance_mailer.ACTION_MAP[HTML_MESSAGE_CREATOR_MESSAGE_OPCODE]
        mail_processor = balance_mailer.MailProcessor()
        receipt_html = 'recept_html'
        reply_to_email = 'reply_to_email'
        subject = u'Электронный чек'
        body = receipt_html
        attachments = []
        data = pickle.dumps([subject, body, (reply_to_email, u'Yandex'), attachments])
        message_creator = suitable_message_creator(mail_processor.cfg, 0, data, session=session)
        bcc = message_creator.get_bcc()
        m_sender_p = message_creator.get_sender()
        reply_to = message_creator.get_reply_to()
        assert bcc is None
        assert m_sender_p == (reply_to_email, 'Yandex')
        assert reply_to is None
