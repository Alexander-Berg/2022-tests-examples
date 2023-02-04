# coding: utf-8

import mock

from billing.dcs.dcs.monitorings.common import SUBJECT_PREFIX, send_mail

from billing.dcs.tests.utils import BaseTestCase


@mock.patch('billing.dcs.dcs.monitorings.common.utils_send_mail')
class SendMailTestCase(BaseTestCase):
    def test_delimiter_inserted(self, utils_send_mail_mock):
        subject = 'My subject'
        body = 'body'
        send_mail(subject, body, recipient_addresses=[])
        self.check_mock_calls(utils_send_mail_mock, [mock.call(
            SUBJECT_PREFIX + ' ' + subject, body, recipient_addresses=[]
        )])

    def test_delimiter_not_inserted(self, utils_send_mail_mock):
        subject = '[tag] My subject'
        body = 'body'
        send_mail(subject, body, recipient_addresses=[])
        self.check_mock_calls(utils_send_mail_mock, [mock.call(
            SUBJECT_PREFIX + subject, body, recipient_addresses=[]
        )])
