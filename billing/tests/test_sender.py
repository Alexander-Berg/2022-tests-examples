# -*- coding: utf-8 -*-
import datetime
import pytest
import mock
import copy
from email.header import Header

from butils import sender, str_util


ENC = 'utf-8'

MSG_TEMPLATE = u"""Subject: {subject}
From: {sender_name} <{sender_email}>
To: {rcpt_name} {rcpt_email}
Bcc: {bcc}
Reply-To: {reply_to_email}
Return-Path: {return_path}
MIME-Version: 1.0
Content-Type: text/plain; charset="{enc}"
Content-Transfer-Encoding: base64

{body}"""


def enc_str(s):
    h = Header(s, ENC)
    return str(h)


def _get_msg(email_dict):
    template_dict = copy.deepcopy(email_dict)
    template_dict['subject'] = enc_str(template_dict['subject'])
    template_dict['sender_name'] = enc_str(template_dict['sender_name'])
    template_dict['rcpt_name'] = enc_str(template_dict['rcpt_name'])

    template_dict['rcpt_email'][0] = '<%s>' % template_dict['rcpt_email'][0]
    if len(template_dict['rcpt_email']) > 1:
        template_dict['rcpt_email'] = ', '.join(template_dict['rcpt_email'])
    else:
        template_dict['rcpt_email'] = template_dict['rcpt_email'][0]

    template_dict['body'] = template_dict['body'].encode(ENC).encode('base64')
    template_dict['return_path'] = template_dict['reply_to_email'] or template_dict['sender_email']
    template_dict['enc'] = ENC
    template_dict['dt'] = datetime.datetime.now().strftime('%a %b %d %H:%M:%S %Y')

    return str_util.utf8(MSG_TEMPLATE.format(**template_dict))


class MockSmtp(object):
    sender_email = None
    rcpt_email = None
    msg = None  # базовые значения для сравнения

    host = None
    post = None

    def __init__(self, host='', port=None, local_hostname=None, *a, **kw):
        assert host == self.host
        assert port == self.port
        assert local_hostname == 'balance.yandex.ru'

    def sendmail(self, sender_email, rcpt_email, msg):
        assert sender_email == self.sender_email
        assert rcpt_email == self.rcpt_email
        assert msg == self.msg

    def quit(self):
        pass


class TestSender(object):
    email_dict = {
        'sender_email': 'granger@hogwarts.com',
        'sender_name': 'Hermione Granger',
        'rcpt_name': 'Harry Potter',
        'subject': 'Pottermore',
        'body': u'See latest news on www.pottermore.com. И кириллицу добавим',
        'attach_list': None,
        'reply_to_email': 'hogwarts@team.com',
        'multipart': None,
        'bcc': 'weasley@hogwarts.yandex-team.com',
    }

    def test_sender_really_send_smtplib(self):
        def tst(rcpt_emails, gateway):
            host, port = gateway
            email_dict = copy.deepcopy(self.email_dict)
            email_dict['rcpt_email'] = rcpt_emails

            s = sender.Sender(None)
            with mock.patch('smtplib.SMTP', MockSmtp):
                MockSmtp.sender_email = email_dict['sender_email']
                MockSmtp.rcpt_email = email_dict['rcpt_email']
                MockSmtp.msg = _get_msg(email_dict)

                MockSmtp.host = host
                MockSmtp.port = port

                s.really_send(**email_dict)

        for rcpt_emails, gateway in (
            (['harry@yandex-team.ru'], sender.EMAIL_INTERNAL_GATEWAY),
            (['potter@mail.com'], sender.EMAIL_EXTERNAL_GATEWAY),
            (['harry@yandex-team.ru', 'potter@mail.com'],
             sender.EMAIL_EXTERNAL_GATEWAY),
        ):
            tst(rcpt_emails, gateway)

    def test_sender_really_send_sendmail(self):
        email_dict = copy.deepcopy(self.email_dict)
        email_dict['rcpt_email'] = 'potter@mail.com'
        email_dict['use_smtplib'] = False
        mailer = 'sendmail.ssh'

        s = sender.Sender(None, mailer)
        with mock.patch('os.popen') as os_mock:
            s.really_send(**email_dict)
            os_mock.assert_called_once_with("%s -f %s -t" % (mailer, email_dict['sender_email']), "w")

    def test_encode_domain(self):
        email, encoded_email = (u'test@test.com', u'test@test.com')
        assert sender.encode_domain(email) == encoded_email
        email, encoded_email = (u'test@яндекс.рф',
                                u'test@xn--d1acpjx3f.xn--p1ai')
        assert sender.encode_domain(email) == encoded_email


# vim:ts=4:sts=4:sw=4:tw=88:et:
