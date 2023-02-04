# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import unicode_literals

from collections import namedtuple

import pytest

from django.conf import settings
from django.core import mail

from app.utils.otrs import OTRSReportMessage
from core.models import Reporter


YaUser = namedtuple('YaUser', 'uid default_email login')


@pytest.fixture
def reporter():
    return Reporter.objects.create(uid=1)


@pytest.fixture
def yauser():
    return YaUser(uid=1, default_email='user@server', login='user')


@pytest.fixture
def message_context(reporter):
    return {
        'subject': 'subject',
        'type': 'type',
        'steps': 'steps',
        'result': 'result',
        'recommendations': 'recommendations',
        'reporter_profile_url': reporter.get_absolute_url(),
    }


@pytest.fixture()
def send_report_message(yauser, message_context):
    report_message = OTRSReportMessage(yauser, message_context, 'ru')
    report_message.send()


@pytest.mark.django_db
def test_report_message_is_sent(send_report_message):
    assert len(mail.outbox) == 1


@pytest.mark.django_db
def test_report_message_content(reporter, send_report_message):
    message = mail.outbox[0]
    expected = '----\nhttps://bugbounty.yandex-team.ru/user-profile/edit/%s/' % reporter.pk
    assert expected in message.body


@pytest.mark.django_db
@pytest.mark.parametrize(
    'header,value', [('X-OTRS-uid', 1), ('X-OTRS-fromfeedback', 'security')])
def test_report_message_extra_headers(send_report_message, header, value):
    email_message = mail.outbox[0]
    assert email_message.extra_headers[header] == value


@pytest.mark.django_db
@pytest.mark.parametrize('language', ['en', 'ru'])
def test_report_message_to(yauser, message_context, language):
    report_message = OTRSReportMessage(yauser, message_context, language)
    report_message.send()
    email_message = mail.outbox[0]
    assert email_message.to == [settings.OTRS_EMAILS[language]]
