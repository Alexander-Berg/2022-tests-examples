# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.test import TestCase

from events.accounts.models import User
from events.accounts.utils import is_yandex_user


class TestOutstaffUsers(TestCase):
    fixtures = ['initial_data.json']

    def register_uri(self, affiliation):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons',
            json={
                'page': 1, 'pages': 1, 'total': 1, 'limit': 50,
                'result': [{
                    'official': {
                        'affiliation': affiliation,
                    },
                }],
            },
        )

    @responses.activate
    def test_is_user_external_by_uid(self):
        self.register_uri('external')
        is_yandex = is_yandex_user('1120000000037913')
        self.assertFalse(is_yandex)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_is_user_yandex_by_uid(self):
        self.register_uri('yandex')
        is_yandex = is_yandex_user('1120000000106806')
        self.assertTrue(is_yandex)
        self.assertEqual(len(responses.calls), 1)


class TestIsYandexUid(TestCase):
    fixtures = ['initial_data.json']

    def register_uri(self, affiliation, user_uid, user_name):
        responses.add(
            responses.GET,
            'http://blackbox.yandex-team.ru/blackbox',
            content_type='text/xml',
            body=f'''<?xml version="1.0" encoding="UTF-8"?>
<doc>
    <uid hosted="0">{user_uid}</uid>
    <dbfield id="accounts.login.uid">{user_name}</dbfield>
</doc>''')
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons',
            json={
                'page': 1, 'pages': 1, 'total': 1, 'limit': 50,
                'result': [{
                    'official': {
                        'affiliation': affiliation,
                    },
                }],
            },
        )

    @responses.activate
    def test_is_new_user_external(self):
        self.register_uri('external', '1120000000052023', 'robot-forms')
        profile = User.objects.get_or_create_user('1120000000052023', None, None)
        group_ids = list(profile.groups.values_list('pk', flat=True))
        self.assertNotIn(settings.GROUP_ALLSTAFF_PK, group_ids)
        self.assertEqual(len(responses.calls), 2)

    @responses.activate
    def test_is_new_user_yandexl(self):
        self.register_uri('yandex', '1120000000106806', 'login')
        profile = User.objects.get_or_create_user('1120000000106806', None, None)
        group_ids = list(profile.groups.values_list('pk', flat=True))
        self.assertIn(settings.GROUP_ALLSTAFF_PK, group_ids)
        self.assertEqual(len(responses.calls), 2)
