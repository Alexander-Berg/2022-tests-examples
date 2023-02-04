# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.contrib.auth.models import Group
from django.test import TestCase, override_settings
from unittest.mock import patch

from events.accounts.models import User
from events.accounts.factories import UserFactory
from events.accounts.helpers import YandexClient


class TestCreateUser(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def register_bb(self, uid, login, email, name='', host=None):
        host = host or 'blackbox.yandex-team.ru'
        url = f'http://{host}/blackbox'
        body = f'''<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="0">{uid}</uid>
                <login>{login}</login>
                <have_password>1</have_password>
                <have_hint>0</have_hint>
                <aliases>
                    <alias type="1">{login}</alias>
                </aliases>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <regname>{login}</regname>
                <display_name>
                    <name>{login}</name>
                    <avatar>
                        <default>0/0-0</default>
                        <empty>1</empty>
                    </avatar>
                </display_name>
                <dbfield id="account_info.email.uid"></dbfield>
                <dbfield id="account_info.fio.uid">{name}</dbfield>
                <dbfield id="accounts.login.uid">{login}</dbfield>
                <address-list>
                    <address validated="1" default="1" unsafe="0" native="1">{email}</address>
                </address-list>
            </doc>
        '''
        responses.add(responses.GET, url, body=body, content_type='text/xml')

    def register_staff(self, affiliation):
        url = 'https://staff-api.test.yandex-team.ru/v3/persons'
        body = {
            'result': [{'official': {'affiliation': affiliation}}],
            'links': {},
            'page': 1,
            'total': 1,
            'pages': 1,
            'limit': 50,
        }
        responses.add(responses.GET, url, json=body)

    def register_cloud(self, cloud_uid, login, email, name=''):
        url = f'https://api-integration-qa.directory.ws.yandex.net/v6/users/cloud/{cloud_uid}/'
        parts = name.split(' ')
        body = {
            'email': email,
            'nickname': login,
            'name': {
                'first': parts[0],
                'last': parts[-1],
            },
        }
        responses.add(responses.GET, url, json=body)

    @responses.activate
    def test_create_by_uid(self):
        uid = '1120000000016772'
        self.register_bb(uid, 'smosker', 'smosker@yandex-team.ru')
        self.register_staff('yandex')
        user = User.objects.get_or_create_user(uid, None, None)
        self.assertEqual(user.email, 'smosker@yandex-team.ru')
        self.assertEqual(user.username, 'smosker')
        self.assertEqual(user.uid, uid)
        group_ids = list(user.groups.values_list('pk', flat=True))
        self.assertIn(settings.GROUP_ALLSTAFF_PK, group_ids)

    @responses.activate
    def test_create_by_uid_outstaff(self):
        uid = '1120000000052023'
        self.register_bb(uid, 'robot-forms', 'robot-forms@yandex-team.ru')
        self.register_staff('external')
        user = User.objects.get_or_create_user(uid, None, None)
        self.assertEqual(user.email, 'robot-forms@yandex-team.ru')
        self.assertEqual(user.username, 'robot-forms')
        self.assertEqual(user.uid, uid)
        group_ids = list(user.groups.values_list('pk', flat=True))
        self.assertNotIn(settings.GROUP_ALLSTAFF_PK, group_ids)

    @responses.activate
    @override_settings(IS_INTERNAL_SITE=False)
    def test_create_by_uid_extrenal(self):
        uid = '283315747'
        self.register_bb(uid, 'smoskerYan', 'smoskerYan@yandex.ru', host='blackbox-mimino.yandex.net')
        user = User.objects.get_or_create_user(uid, None, None)
        self.assertEqual(user.email, 'smoskerYan@yandex.ru')
        self.assertTrue(user.username.startswith('gu'))
        self.assertEqual(user.uid, uid)
        group_ids = list(user.groups.values_list('pk', flat=True))
        self.assertNotIn(settings.GROUP_ALLSTAFF_PK, group_ids)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_link_orgs(self):
        uid = '283315747'
        dir_id = '732'
        self.register_bb(uid, 'smoskerYan', 'smoskerYan@yandex.ru', host='blackbox-mimino.yandex.net')
        with patch.object(User, '_get_organizations', return_value=[dir_id]):
            user = User.objects.get_or_create_user(uid, None, None)
            group_ids = list(user.groups.values_list('pk', flat=True))
            self.assertIn(Group.objects.get(o2g__org__dir_id=dir_id).pk, group_ids)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_cloud_uid_get_data(self):
        dir_id = '1101'
        cloud_uid = 'bfb0imi4jkldenpjpcsr'
        self.register_cloud(cloud_uid, 'masloval', 'masloval@yandex-team.ru', 'Liubov Maslova')
        user = User.objects.create_forms_user(
            uid=None,
            cloud_uid=cloud_uid,
            email='masloval@yandex-team.ru',
            username='not_important',
        )
        params = User.objects.get_params(user.uid, user.cloud_uid, dir_id)
        expected = {
            'default_email': 'masloval@yandex-team.ru',
            'fields': {
                'fio': 'Liubov Maslova',
                'login': 'masloval',
            },
        }
        self.assertDictEqual(params, expected)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_update_cloud_uid_field(self):
        uid = '123'
        user = UserFactory(uid=uid, username='user', email='user@yandex.ru')

        dir_id = '1101'
        cloud_uid = 'abcd'
        self.register_cloud(cloud_uid, 'new_user', email='new_user@yandex.ru')
        new_user = User.objects.get_or_create_user(uid, cloud_uid, dir_id)

        self.assertEqual(new_user.uid, uid)
        self.assertEqual(new_user.cloud_uid, cloud_uid)
        self.assertEqual(user.pk, new_user.pk)
        self.assertEqual(user.username, new_user.username)
        self.assertEqual(user.email, new_user.email)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_update_uid_field(self):
        cloud_uid = 'abcd'
        user = UserFactory(uid=None, cloud_uid=cloud_uid, username='user', email='user@yandex.ru')

        dir_id = '1101'
        uid = '123'
        new_user = User.objects.get_or_create_user(uid, cloud_uid, dir_id)

        self.assertEqual(new_user.uid, uid)
        self.assertEqual(new_user.cloud_uid, cloud_uid)
        self.assertEqual(user.pk, new_user.pk)
        self.assertEqual(user.username, new_user.username)
        self.assertEqual(user.email, new_user.email)
