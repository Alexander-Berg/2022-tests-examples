from unittest import skipIf

from django.conf import settings
from mock import patch
from pretend import stub
from ujson import loads

from wiki.api_core.errors.bad_request import InvalidDataSentError
from wiki.api_frontend.serializers.autocomplete import AuthAutocompleteDeserializer
from wiki.api_frontend.views import AuthAutocompleteView
from wiki.intranet.models import Staff
from wiki.pages.access.groups import Group
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import CallRecorder, unexpected_call


class AuthAutocompleteDeseriazerTest(BaseApiTestCase):
    def _test_valid(self, data, q, filter=None):
        deserializer = AuthAutocompleteDeserializer(data=data)
        self.assertTrue(deserializer.is_valid())
        data = deserializer.validated_data
        self.assertEqual(data['q'], q)
        self.assertEqual(data['filter'], filter)

    def test_valid_no_filter(self):
        self._test_valid(data={'q': '  й  '}, q='й')

    def test_valid_with_filter(self):
        self._test_valid(data={'q': 'й', 'filter': 'user'}, q='й', filter='user')
        self._test_valid(data={'q': 'й', 'filter': 'group'}, q='й', filter='group')

    def _test_invalid(self, data, error_key):
        deserializer = AuthAutocompleteDeserializer(data=data)
        self.assertFalse(deserializer.is_valid())
        self.assertIn(error_key, deserializer.errors)

    def test_invalid_empty(self):
        self._test_invalid(data={'q': ''}, error_key='q')

    def test_invalid_filter(self):
        self._test_invalid(data={'q': 'z', 'filter': 'zzz'}, error_key='filter')


class AuthAutocompleteViewTest(BaseApiTestCase):
    @classmethod
    def setUpClass(cls):
        super(AuthAutocompleteViewTest, cls).setUpClass()
        cls.view = AuthAutocompleteView()

    def test_invalid(self):
        try:
            request = stub(GET={})
            self.view.get(request)
            self.fail('Exception should have been raised')
        except InvalidDataSentError as e:
            self.assertIn('q', e.errors)

    def test_filter_users(self):
        users = [
            {'id': 22, 'cloud_uid': None, 'login': 'asm', 'last_name': 'Мазуров', 'uid': 4, 'first_name': 'Алексей'},
            {
                'id': 1156,
                'cloud_uid': None,
                'login': 'thasonic',
                'last_name': 'Покатилов',
                'uid': 1,
                'first_name': 'Александр',
            },
        ]
        p = CallRecorder(lambda *args, **kwargs: users)
        request = stub(GET={'q': 'a23', 'filter': 'user'})

        @patch('wiki.api_frontend.views.autocomplete.find_staff_models', p.get_func())
        @patch('wiki.api_frontend.views.autocomplete.find_wiki_group_models', unexpected_call)
        def f():
            return self.view.get(request)

        response = f()
        self.assertEqual(response.status_code, 200)
        data = response.data
        self.assertEqual(data['groups'], None)
        self.assertEqual(data['users'], users)

        self.assertEqual(p.times, 1)
        args = p.calls[0].args
        self.assertEqual(args[0], 'a23')

    def test_filter_groups(self):
        groups = [
            {'id': 1, 'name': 'admins'},
            {'id': 3, 'name': 'employees'},
        ]
        p = CallRecorder(lambda *args, **kwargs: groups)
        request = stub(GET={'q': 'a23', 'filter': 'group'})

        @patch('wiki.api_frontend.views.autocomplete.find_wiki_group_models', p.get_func())
        @patch('wiki.api_frontend.views.autocomplete.find_staff_models', unexpected_call)
        def f():
            return self.view.get(request)

        response = f()
        self.assertEqual(response.status_code, 200)
        data = response.data
        self.assertEqual(data['users'], None)
        self.assertEqual(data['groups'], groups)

        self.assertEqual(p.times, 1)
        args = p.calls[0].args
        self.assertEqual(args[0], 'a23')


class AuthAutocompleteApiIntegrationTest(BaseApiTestCase):
    def setUp(self):
        super(AuthAutocompleteApiIntegrationTest, self).setUp()
        self.setUsers()
        if settings.IS_BUSINESS:
            self.set_groups_business()
        else:
            self.set_groups_extranet()
        self.client.login('thasonic')

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test(self):
        for staff in Staff.objects.all():
            # иначе uid непредсказуем, он растёт при выполнении других тестов
            staff.uid = staff.id * 100
            staff.save()

        url = '{api_url}/.autocomplete/auth?q='.format(api_url=self.api_url)
        response = self.client.get(url + 's')
        self.assertEqual(response.status_code, 200)

        data = loads(response.content)['data']
        users = data['users']
        groups = data['groups']

        self.assertEqual(
            users,
            [
                {'id': 22, 'login': 'asm', 'last_name': 'Мазуров', 'uid': 2200, 'first_name': 'Алексей'},
                {'id': 1156, 'login': 'thasonic', 'last_name': 'Покатилов', 'uid': 115600, 'first_name': 'Александр'},
                {'id': 3496, 'login': 'chapson', 'last_name': 'Чапоргин', 'uid': 349600, 'first_name': 'Антон'},
            ],
        )

        def normalize_order(groups):
            return sorted(groups, key=lambda obj: obj['id'])

        target = [
            {'id': Group.objects.get(name='admins').id, 'name': 'admins'},
            {'id': Group.objects.get(name='employees').id, 'name': 'employees'},
            {'id': Group.objects.get(name='school').id, 'name': 'school'},
            {'id': Group.objects.get(name='school РУС').id, 'name': 'school РУС'},
        ]

        self.assertEqual(normalize_order(groups), normalize_order(target))
