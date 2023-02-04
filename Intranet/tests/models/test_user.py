# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest
import six
from mock import patch, call

from core.models import NewPaymentInfo
from core.tests.factory import NewPaymentInfoFactory, ReporterFactory


pytestmark = pytest.mark.django_db


def test_account_holder_cyrillic(payment_info):
    assert payment_info.account_holder_cyrillic == 'Йохн Смитх\n(John Smith)'


@pytest.mark.parametrize('is_russian_resident', [True, False])
def test_reporter_is_russian_resident(reporter, payment_info, is_russian_resident):
    payment_info.is_russian_resident = is_russian_resident
    payment_info.reporter = reporter
    payment_info.save()
    assert reporter.is_russian_resident == payment_info.is_russian_resident


def test_reporter_is_russian_resident_is_none(reporter):
    assert reporter.is_russian_resident is None


class MockedResponse(object):
    def __init__(self, status_code=200, json=None):
        self.status_code = status_code
        self._json = json

    def json(self):
        return self._json


def fill_info(
        payment_info,
        email='test@yandex.ru', phone='+7(800)5553535', fname='Vasya', lname='Pupkin',
        **kwargs
):
    payment_info.datasync_values.update({
        'email': email,
        'phone': phone,
        'fname': fname,
        'lname': lname,
    })
    for k, v in kwargs.items():
        if isinstance(k, six.binary_type):
            k = k.decode('utf-8')
        payment_info.datasync_values[k] = v


def test_save_new_payment_info():
    payment_info = NewPaymentInfo(reporter=ReporterFactory(uid=1337))

    headers = {
        'X-Ya-Service-Ticket': 'TICKET',
        'X-Uid': '1337',
    }

    with patch('core.utils.datasync.requests.get') as get,\
            patch('core.utils.datasync.requests.put') as put,\
            patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
        get_tvm_ticket.return_value = 'TICKET'
        get.return_value = MockedResponse()
        put.return_value = MockedResponse()
        fill_info(payment_info, some_invalid_param='123')
        payment_info.save()
        url = 'https://datasync.yandex.net:8443/v1/personality/profile/bugbounty/payment_info/{}'.format(
            payment_info.pk
        )
        body = {
            'email': 'test@yandex.ru',
            'phone': '+7(800)5553535',
            'fname': 'Vasya',
            'lname': 'Pupkin',
            'id': unicode(payment_info.pk),
        }
        assert get.call_args is None
        assert put.call_args == call(url, headers=headers, json=body)

    with patch('core.utils.datasync.requests.get') as get, \
            patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
        get_tvm_ticket.return_value = 'TICKET'
        get.return_value = MockedResponse(json=body)
        same_payment_info = NewPaymentInfo.objects.get(pk=payment_info.pk)
        assert same_payment_info.datasync_values['email'] == 'test@yandex.ru'
        assert same_payment_info.datasync_values['phone'] == '+7(800)5553535'
        assert same_payment_info.datasync_values['fname'] == 'Vasya'
        assert same_payment_info.datasync_values['lname'] == 'Pupkin'
        for field in set(NewPaymentInfo.DATASYNC_FIELDS) - {'email', 'phone', 'fname', 'lname'}:
            assert same_payment_info.datasync_values.get(field) is None
        assert get.call_args == call(url, headers=headers)



