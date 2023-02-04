# coding: utf-8

import collections
import random

import pytest
from mock import patch

from uhura import models
from uhura.celery_app import app
from utils import _staff_get_person_data


@pytest.fixture(scope='module')
def celery_app(request):
    app.conf.update(CELERY_ALWAYS_EAGER=True)
    return app


def ids_pagination_mock():
    person = _staff_get_person_data()
    person['id'] = 0
    person['phones'] = [{'number': '+79999999999'}]
    pages_list = []
    pages_list.append([person])
    for i in range(3):
        page = []
        for j in range(2):
            new_person = person.copy()
            coef = 1000 * (i + 1) + j
            new_person['id'] = coef
            new_person['uid'] = coef
            new_person['login'] += 'a' * random.randint(1, 10)
            new_person['work_email'] += 'b' * random.randint(1, 10)
            page.append(new_person)
        pages_list.append(page)
    return pages_list


def _check_dont_send_stat(mock_obj, value):
    dont_send_stat = collections.Counter([t[0][3] for t in mock_obj.call_args_list])
    assert dont_send_stat[False] == value


class PageApiMock(object):
    def __init__(self, first_page):
        self.first_page = first_page


def test_creating_notifications(uid, celery_app):
    from uhura.tasks import emergency
    with patch('uhura.tasks.emergency.sms_notify.delay') as sms_mock, \
            patch('uhura.tasks.emergency.telegram_notify.delay') as tg_mock, \
            patch('uhura.tasks.emergency.mail_notify.delay') as mail_mock, \
            patch('uhura.tasks.emergency.q_notify.delay') as q_mock, \
            patch('uhura.external.staff.persons.getiter') as iter_mock, \
            patch('uhura.external.staff.persons.get_one') as get_one_mock:
        iter_mock.side_effect = [PageApiMock(x) for x in ids_pagination_mock()]
        get_one_mock.return_value = {'id': 30000}
        sms_mock.return_value = tg_mock.return_value = mail_mock.return_value = q_mock.return_value = None

        models.Office.objects.create(
            name='Москва, БЦ Морозов',
            country_code=1,
            city_code=1,
            office_code=1,
            emergency_emails=['mosoffice-announces@yandex-team.ru'],
            floor_codes=[1, 2, 3, 4, 5, 6, 7, 24, 25, 43]
        )
        answer_id = 1234

        user = models.User.objects.create(uid='1120000000052246', yamb_id='test-q-id', username='username1')
        models.TelegramUsername.objects.create(
            telegram_id=12345,
            user=user
        )
        user = models.User.objects.create(uid='1001', yamb_id='', username='username2')
        models.TelegramUsername.objects.create(
            telegram_id=123456,
            user=user
        )
        models.EmergencyTestingList.objects.create(login='robot-uhura')
        models.EmergencyNotification.objects.create(
            answer_id=answer_id,
            author='author',
            subject='subject',
            short_template='short_template',
            long_template='long_template'
        )

        emergency.create_emergency_notifications.delay([], [], ['1'], [], answer_id, True)

        assert mail_mock.call_count == 8
        _check_dont_send_stat(mail_mock, 2)
        assert sms_mock.call_count == 7
        _check_dont_send_stat(sms_mock, 1)
        assert tg_mock.call_count == 2
        _check_dont_send_stat(tg_mock, 1)
        assert q_mock.call_count == 1
        _check_dont_send_stat(q_mock, 1)


def test_floors(uid, celery_app):
    from uhura.tasks import emergency
    with patch('uhura.tasks.emergency.sms_notify.delay') as sms_mock, \
            patch('uhura.tasks.emergency.telegram_notify.delay') as tg_mock, \
            patch('uhura.tasks.emergency.mail_notify.delay') as mail_mock, \
            patch('uhura.external.staff.persons.getiter') as iter_mock, \
            patch('uhura.external.staff.persons.get_one') as get_one_mock:
        iter_mock.side_effect = [PageApiMock(x) for x in ids_pagination_mock()]
        get_one_mock.return_value = {'id': 30000}
        sms_mock.return_value = tg_mock.return_value = mail_mock.return_value = None

        models.Office.objects.create(
            name='Москва, БЦ Аврора',
            country_code=1,
            city_code=1,
            office_code=1,
            emergency_emails=['mosoffice-announces@yandex-team.ru'],
            floor_codes=['96', '98', '99', '103', '114', '102', '104', '105']
        )
        answer_id = 1234

        user = models.User.objects.create(uid='1120000000052246', username='username1')
        models.TelegramUsername.objects.create(
            user=user,
            telegram_id=12345
        )
        user = models.User.objects.create(uid='1001', yamb_id=1234, username='username2')
        models.TelegramUsername.objects.create(
            user=user,
            telegram_id=123456
        )
        models.EmergencyTestingList.objects.create(login='robot-uhura')
        models.EmergencyNotification.objects.create(
            answer_id=answer_id,
            author='author',
            subject='subject',
            short_template='short_template',
            long_template='long_template'
        )

        emergency.create_emergency_notifications.delay([], [], [], ['96', '98', '99', '103', '114'], answer_id, True)

        assert mail_mock.call_count == 8
        _check_dont_send_stat(mail_mock, 2)
