# -*- coding: utf-8 -*-
import responses

from unittest.mock import patch, ANY
from django.test import TestCase, override_settings
from django.utils.translation import (
    override as override_language,
    ugettext_lazy as _,
)

from events.accounts.utils import (
    get_next_username,
    get_external_user_login,
    get_external_user_uid_by_login,
    get_external_uid,
    PersonalData,
    PassportPersonalData,
    DirPersonalData,
    GENDER_MALE,
    GENDER_FEMALE,
)


class TestGetNextUsername(TestCase):  # {{{
    def test_length(self):
        response = get_next_username()
        self.assertTrue(len(response) < 30, msg='размер генерируемого username должно быть меньше 30 символов')

    def test_should_be_random(self):
        names = [get_next_username() for i in range(10)]

        for name in names:
            self.assertEqual(names.count(name), 1, msg='get_next_username должен создавать случайные имена')
# }}}


class TestPassportPersonalData(TestCase):  # {{{
    personal_data_class = PassportPersonalData

    @patch('events.surveyme.models.PassportPersonalData._get_user_info')
    def test_should_return_full_bunch_for_personal_data(self, mock_get_user_info):
        user_info = {
            'users': [{
                'id': '25871573',
                'uid': {
                    'value': '25871573',
                    'lite': False,
                    'hosted': False,
                },
                'login': 'kdunaev',
                'have_password': True,
                'have_hint': True,
                'karma': {'value': 0},
                'karma_status': {'value': 0},
                'dbfields': {
                    'account_info.birth_date.uid': '1976-09-06',
                    'account_info.city.uid': 'Москва',
                    'account_info.email.uid': '',
                    'account_info.fio.uid': 'Дунаев Кирилл',
                    'account_info.nickname.uid': '',
                    'account_info.sex.uid': '1',
                    'accounts.login.uid': 'kdunaev',
                    'userinfo.lang.uid': 'ru',
                },
                'address-list': [{
                    'address': 'kdunaev@yandex.ru',
                    'validated': True,
                    'default': True,
                    'rpop': False,
                    'silent': False,
                    'unsafe': False,
                    'native': True,
                    'born-date': '2007-06-03 11:35:41',
                }],
                'public_id': '0e6q0tbnvpjby3k3t0j428cc28',
            }],
        }
        mock_get_user_info.return_value = user_info

        personal_data = self.personal_data_class(25871573)

        self.assertEqual(personal_data.name, 'Кирилл')
        self.assertEqual(personal_data.surname, 'Дунаев')
        self.assertIsNone(personal_data.patronymic)
        self.assertEqual(personal_data.gender, _('Мужской'))
        self.assertEqual(personal_data.birth_date, '1976-09-06')
        self.assertEqual(personal_data.email, 'kdunaev@yandex.ru')
        self.assertIsNone(personal_data.phone)
        self.assertEqual(personal_data.login, 'kdunaev')
        self.assertEqual(personal_data.karma, 0)
        self.assertEqual(personal_data.karma_status, 0)
        self.assertIsNone(personal_data.position)
        self.assertIsNone(personal_data.job_place)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.groups)
        self.assertEqual(personal_data.public_id, '0e6q0tbnvpjby3k3t0j428cc28')

        mock_get_user_info.assert_called_once()

    @patch('events.surveyme.models.PassportPersonalData._get_user_info')
    def test_shouldnt_return_personal_data_for_fake_uid(self, mock_get_user_info):
        user_info = {
            'users': [{
                'id': '1129876543239962',
                'uid': {},
                'karma': {'value': 0},
                'karma_status': {'value': 0},
            }],
        }
        mock_get_user_info.return_value = user_info

        personal_data = self.personal_data_class(1129876543239962)

        self.assertIsNone(personal_data.name)
        self.assertIsNone(personal_data.surname)
        self.assertIsNone(personal_data.patronymic)
        self.assertIsNone(personal_data.gender)
        self.assertIsNone(personal_data.birth_date)
        self.assertIsNone(personal_data.email)
        self.assertIsNone(personal_data.phone)
        self.assertIsNone(personal_data.login)
        self.assertEqual(personal_data.karma, 0)
        self.assertEqual(personal_data.karma_status, 0)
        self.assertIsNone(personal_data.position)
        self.assertIsNone(personal_data.job_place)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.groups)

        mock_get_user_info.assert_called_once()

    @patch('events.surveyme.models.PassportPersonalData._get_user_info')
    def test_shouldnt_return_personal_data_without_uid(self, mock_get_user_info):
        mock_get_user_info.return_value = {}

        personal_data = self.personal_data_class(None)

        self.assertIsNone(personal_data.name)
        self.assertIsNone(personal_data.surname)
        self.assertIsNone(personal_data.patronymic)
        self.assertIsNone(personal_data.gender)
        self.assertIsNone(personal_data.birth_date)
        self.assertIsNone(personal_data.email)
        self.assertIsNone(personal_data.phone)
        self.assertIsNone(personal_data.login)
        self.assertIsNone(personal_data.karma)
        self.assertIsNone(personal_data.karma_status)
        self.assertIsNone(personal_data.position)
        self.assertIsNone(personal_data.job_place)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.groups)

        mock_get_user_info.assert_not_called()
# }}}


class TestDirPersonalData(TestCase):  # {{{
    personal_data_class = DirPersonalData

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_user_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    def test_should_return_full_bunch_for_personal_data(
        self,
        mock_get_org_info,
        mock_get_user_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        org_info = {
            'organization_type': 'common',
        }
        user_info = {
            'gender': 'male',
            'nickname': 'kdun4ev-test',
            'name': {'last': 'Dun4ev', 'first': 'Kirll', 'middle': 'A'},
            'birthday': '2001-09-01',
            'contacts': [
                {'synthetic': True, 'main': False, 'type': 'phone', 'value': '9117813', 'alias': False},
                {'synthetic': True, 'main': True, 'type': 'email', 'value': 'kdun4ev-test@yandex.ru', 'alias': False},
            ],
            'id': 4018245342,
            'position': 'Employee',
            'department': {'name': 'Отдел согласований', 'id': 2},
            'groups': [{'id': 2}],
        }
        department_info = {
            'head': {
                'name': {'last': {'ru': 'Демидов'}, 'first': {'ru': 'Иван'}, 'middle': {'ru': ''}},
            },
            'id': 2,
        }
        group_info = [{
            'name': {'en': 'Organization administrator', 'ru': 'Администратор организации'},
            'id': 2,
        }]
        mock_get_org_info.return_value = org_info
        mock_get_user_info.return_value = user_info
        mock_get_department_info.return_value = department_info
        mock_get_group_info.return_value = group_info

        personal_data = self.personal_data_class(4018245342, None, 47)

        self.assertEqual(personal_data.name, 'Kirll')
        self.assertEqual(personal_data.surname, 'Dun4ev')
        self.assertEqual(personal_data.patronymic, 'A')
        self.assertEqual(personal_data.birth_date, '2001-09-01')
        self.assertEqual(personal_data.gender, _('Мужской'))
        self.assertEqual(personal_data.email, 'kdun4ev-test@yandex.ru')
        self.assertEqual(personal_data.phone, '9117813')
        self.assertEqual(personal_data.login, 'kdun4ev-test')
        self.assertIsNone(personal_data.karma)
        self.assertIsNone(personal_data.karma_status)
        self.assertEqual(personal_data.position, 'Employee')
        self.assertEqual(personal_data.job_place, 'Отдел согласований')
        self.assertEqual(personal_data.manager, 'Иван Демидов')
        self.assertEqual(personal_data.manager, 'Иван Демидов')
        self.assertEqual(personal_data.groups, 'Администратор организации')
        self.assertEqual(personal_data.groups, 'Администратор организации')

        mock_get_org_info.assert_called_once()
        mock_get_user_info.assert_called_once()
        mock_get_department_info.assert_called_once()
        mock_get_group_info.assert_called_once()

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_user_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    def test_shouldnt_corrupt_on_incomplete_personal_data(
        self,
        mock_get_org_info,
        mock_get_user_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        org_info = {
            'organization_type': 'common',
        }
        user_info = {
            'gender': None,
            'nickname': None,
            'name': None,
            'birthday': None,
            'contacts': None,
            'id': 4018245342,
            'position': None,
            'department': None,
            'groups': None,
        }
        mock_get_org_info.return_value = org_info
        mock_get_user_info.return_value = user_info
        mock_get_department_info.return_value = {}
        mock_get_group_info.return_value = {}

        personal_data = self.personal_data_class(4018245342, None, 47)

        self.assertIsNone(personal_data.name)
        self.assertIsNone(personal_data.surname)
        self.assertIsNone(personal_data.patronymic)
        self.assertIsNone(personal_data.gender)
        self.assertIsNone(personal_data.birth_date)
        self.assertIsNone(personal_data.email)
        self.assertIsNone(personal_data.phone)
        self.assertIsNone(personal_data.login)
        self.assertIsNone(personal_data.karma)
        self.assertIsNone(personal_data.karma_status)
        self.assertIsNone(personal_data.position)
        self.assertIsNone(personal_data.job_place)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.groups)
        self.assertIsNone(personal_data.groups)

        mock_get_org_info.assert_called_once()
        mock_get_user_info.assert_called_once()
        mock_get_department_info.assert_not_called()
        mock_get_group_info.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_user_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    def test_shouldnt_return_personal_data_for_portal(
        self,
        mock_get_org_info,
        mock_get_user_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        org_info = {
            'organization_type': 'portal',
        }
        mock_get_org_info.return_value = org_info
        mock_get_user_info.return_value = {}
        mock_get_department_info.return_value = {}
        mock_get_group_info.return_value = {}

        personal_data = self.personal_data_class(4018245342, None, 47)

        self.assertIsNone(personal_data.name)
        self.assertIsNone(personal_data.surname)
        self.assertIsNone(personal_data.patronymic)
        self.assertIsNone(personal_data.gender)
        self.assertIsNone(personal_data.birth_date)
        self.assertIsNone(personal_data.email)
        self.assertIsNone(personal_data.phone)
        self.assertIsNone(personal_data.login)
        self.assertIsNone(personal_data.karma)
        self.assertIsNone(personal_data.karma_status)
        self.assertIsNone(personal_data.position)
        self.assertIsNone(personal_data.job_place)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.groups)

        mock_get_org_info.assert_called_once()
        mock_get_user_info.assert_not_called()
        mock_get_department_info.assert_not_called()
        mock_get_group_info.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_user_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    def test_shouldnt_return_personal_data_without_dir_id(
        self,
        mock_get_org_info,
        mock_get_user_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        mock_get_org_info.return_value = {}
        mock_get_user_info.return_value = {}
        mock_get_department_info.return_value = {}
        mock_get_group_info.return_value = {}

        personal_data = self.personal_data_class(4018245342, None, None)

        self.assertIsNone(personal_data.name)
        self.assertIsNone(personal_data.surname)
        self.assertIsNone(personal_data.patronymic)
        self.assertIsNone(personal_data.gender)
        self.assertIsNone(personal_data.birth_date)
        self.assertIsNone(personal_data.email)
        self.assertIsNone(personal_data.phone)
        self.assertIsNone(personal_data.login)
        self.assertIsNone(personal_data.karma)
        self.assertIsNone(personal_data.karma_status)
        self.assertIsNone(personal_data.position)
        self.assertIsNone(personal_data.job_place)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.groups)

        mock_get_org_info.assert_not_called()
        mock_get_user_info.assert_not_called()
        mock_get_department_info.assert_not_called()
        mock_get_group_info.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_user_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    def test_shouldnt_return_personal_data_without_uid(
        self,
        mock_get_org_info,
        mock_get_user_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        mock_get_org_info.return_value = {}
        mock_get_user_info.return_value = {}
        mock_get_department_info.return_value = {}
        mock_get_group_info.return_value = {}

        personal_data = self.personal_data_class(None, None, 47)

        self.assertIsNone(personal_data.name)
        self.assertIsNone(personal_data.surname)
        self.assertIsNone(personal_data.patronymic)
        self.assertIsNone(personal_data.gender)
        self.assertIsNone(personal_data.birth_date)
        self.assertIsNone(personal_data.email)
        self.assertIsNone(personal_data.phone)
        self.assertIsNone(personal_data.login)
        self.assertIsNone(personal_data.karma)
        self.assertIsNone(personal_data.karma_status)
        self.assertIsNone(personal_data.position)
        self.assertIsNone(personal_data.job_place)
        self.assertIsNone(personal_data.manager)
        self.assertIsNone(personal_data.groups)

        mock_get_org_info.assert_not_called()
        mock_get_user_info.assert_not_called()
        mock_get_department_info.assert_not_called()
        mock_get_group_info.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    @patch('events.common_app.directory.DirectoryClient.get_user')
    def test_should_invoke_cloud_userinfo(
        self,
        mock_get_user,
        mock_get_org_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        mock_get_user.return_value = {
            'id': 123,
            'cloud_uid': 'abcd',
            'name': 'test',
            'nickname': 'test',
        }
        self.personal_data_class(None, 'abcd', '47')
        mock_get_user.assert_called_once_with('47', None, cloud_uid='abcd', fields=ANY)

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    @patch('events.common_app.directory.DirectoryClient.get_user')
    def test_should_invoke_single_userinfo(
        self,
        mock_get_user,
        mock_get_org_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        mock_get_user.return_value = {
            'id': 123,
            'cloud_uid': 'abcd',
            'name': {'first': 'name', 'last': 'surname'},
            'nickname': 'test',
        }
        self.personal_data_class('123', None, '47')
        mock_get_user.assert_called_once_with('47', '123', cloud_uid=None, fields=ANY)

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    @patch('events.common_app.directory.DirectoryClient.get_user')
    def test_should_invoke_single_userinfo_even_with_cloud_uid(
        self,
        mock_get_user,
        mock_get_org_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        mock_get_user.return_value = {
            'id': 123,
            'cloud_uid': 'abcd',
            'name': {'ru': {'first': 'name', 'last': 'surname'}},
            'nickname': 'test',
        }
        self.personal_data_class('123', 'abcd', '47')
        mock_get_user.assert_called_once_with('47', '123', cloud_uid='abcd', fields=ANY)

    @override_settings(IS_BUSINESS_SITE=True)
    @patch('events.surveyme.models.DirPersonalData._get_group_info')
    @patch('events.surveyme.models.DirPersonalData._get_department_info')
    @patch('events.surveyme.models.DirPersonalData._get_org_info')
    @patch('events.common_app.directory.DirectoryClient.get_user')
    def test_shouldnt_invoke_single_or_cloud_userinfo(
        self,
        mock_get_user,
        mock_get_org_info,
        mock_get_department_info,
        mock_get_group_info,
    ):
        mock_get_user.return_value = None
        self.personal_data_class(None, None, '47')
        mock_get_user.assert_not_called()
# }}}


class TestGenderProperty(TestCase):  # {{{
    def test_male(self):
        personal_data = PersonalData()
        personal_data.personal_data = {
            'gender': GENDER_MALE,
        }

        self.assertEqual(personal_data.gender, _('Мужской'))

    def test_female(self):
        personal_data = PersonalData()
        personal_data.personal_data = {
            'gender': GENDER_FEMALE,
        }

        self.assertEqual(personal_data.gender, _('Женский'))

    def test_invalid(self):
        personal_data = PersonalData()
        personal_data.personal_data = {
            'gender': '?',
        }

        self.assertIsNone(personal_data.gender)
# }}}


class TestTranslatedProperty(TestCase):  # {{{
    def setUp(self):
        self.personal_data = PersonalData()

    def test_translate_text(self):
        data = 'text'

        with override_language('ru'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

        with override_language('be'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

        with override_language('en'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

        with override_language('de'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

    def test_translate_dict_ru_en(self):
        data = {
            'ru': 'текст',
            'en': 'text',
        }

        with override_language('ru'):
            self.assertEqual(self.personal_data._get_translated(data), 'текст')

        with override_language('be'):
            self.assertEqual(self.personal_data._get_translated(data), 'текст')

        with override_language('en'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

        with override_language('de'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

    def test_translate_dict_ru(self):
        data = {
            'ru': 'текст',
        }

        with override_language('ru'):
            self.assertEqual(self.personal_data._get_translated(data), 'текст')

        with override_language('be'):
            self.assertEqual(self.personal_data._get_translated(data), 'текст')

        with override_language('en'):
            self.assertIsNone(self.personal_data._get_translated(data))

        with override_language('de'):
            self.assertIsNone(self.personal_data._get_translated(data))

    def test_translate_dict_en(self):
        data = {
            'en': 'text',
        }

        with override_language('ru'):
            self.assertIsNone(self.personal_data._get_translated(data))

        with override_language('be'):
            self.assertIsNone(self.personal_data._get_translated(data))

        with override_language('en'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

        with override_language('de'):
            self.assertEqual(self.personal_data._get_translated(data), 'text')

    def test_translate_none(self):
        data = None

        with override_language('ru'):
            self.assertIsNone(self.personal_data._get_translated(data))

        with override_language('be'):
            self.assertIsNone(self.personal_data._get_translated(data))

        with override_language('en'):
            self.assertIsNone(self.personal_data._get_translated(data))

        with override_language('de'):
            self.assertIsNone(self.personal_data._get_translated(data))
# }}}


class TestGetExternalUid(TestCase):
    def register_uri(self, url, data=None, status=200):
        data = data or {}
        responses.add(responses.GET, url, json=data, status=status)

    @responses.activate
    def test_should_return_external_user_login(self):
        self.register_uri(
            'https://staff-api.test.yandex-team.ru/v3/persons',
            data={'yandex': {'login': 'kdunaev'}},
        )
        login = get_external_user_login('1120000000039962')
        self.assertEqual(login, 'kdunaev')

    @responses.activate
    def test_shouldnt_return_external_user_login(self):
        self.register_uri(
            'https://staff-api.test.yandex-team.ru/v3/persons',
            status=404,
        )
        login = get_external_user_login('not-existing-user-uid')
        self.assertIsNone(login)

    @responses.activate
    def test_should_return_external_user_uid(self):
        self.register_uri(
            'http://blackbox-mimino.yandex.net/blackbox',
            data={'users': [{'id': '25871573', 'login': 'kdunaev'}]},
        )
        user_uid = get_external_user_uid_by_login('kdunaev')
        self.assertEqual(user_uid, '25871573')

    @responses.activate
    def test_shouldnt_return_external_user_uid(self):
        self.register_uri(
            'http://blackbox-mimino.yandex.net/blackbox',
            data={'users': [{'id': '', 'login': ''}]},
        )
        user_uid = get_external_user_uid_by_login('not-existing-user-login')
        self.assertIsNone(user_uid)

    @responses.activate
    def test_should_return_uid(self):
        self.register_uri(
            'https://staff-api.test.yandex-team.ru/v3/persons',
            data={'yandex': {'login': 'kdunaev'}},
        )
        self.register_uri(
            'http://blackbox-mimino.yandex.net/blackbox',
            data={'users': [{'id': '25871573', 'login': 'kdunaev'}]},
        )
        user_uid = get_external_uid('1120000000039962')
        self.assertEqual(user_uid, '25871573')

    @responses.activate
    def test_shouldnt_return_uid(self):
        self.register_uri(
            'https://staff-api.test.yandex-team.ru/v3/persons',
            status=404,
        )
        user_uid = get_external_uid('not-existing-user-uid')
        self.assertIsNone(user_uid)
