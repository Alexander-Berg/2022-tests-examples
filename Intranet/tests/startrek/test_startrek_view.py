# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.accounts.factories import OrganizationToGroupFactory
from events.common_app.startrek.client import StartrekClient
from events.surveyme.factories import SurveyFactory


@override_settings(IS_BUSINESS_SITE=True)
class TestStartrekQueueView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=False)
        self.profile.is_staff = True
        self.profile.save(update_fields=['is_staff'])

        self.o2g = OrganizationToGroupFactory()
        self.org = self.o2g.org
        self.o2g.group.user_set.add(self.profile)

        self.b2b_survey = SurveyFactory(user=self.profile, org=self.org)
        self.b2c_survey = SurveyFactory(user=self.profile)

        self.mock_auth_kwargs = {
            'uid': self.profile.uid,
        }
        self.mock_queue = {
            'id': 1,
            'key': 'FORMS',
            'name': '',
            'description': '',
            'defaultPriority': None,
            'priorities': None,
            'defaultType': None,
            'issuetypes': None,
            'components': None,
        }
        self.mock_priorities = [{
            'id': 1,
            'key': 'normal',
            'name': '',
        }]

    def test_should_return_queue_data_without_get_param(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_queue', return_value=self.mock_queue),
            patch.object(StartrekClient, 'get_priorities', return_value=self.mock_priorities),
        ):
            url = '/admin/api/v2/startrek-queues/FORMS/'
            response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
            self.assertEqual(response.status_code, 404)

    def test_should_return_queue_data_with_get_param(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_queue', return_value=self.mock_queue),
            patch.object(StartrekClient, 'get_priorities', return_value=self.mock_priorities),
        ):
            url = '/admin/api/v2/startrek-queues/FORMS/?survey=%s' % self.b2b_survey.pk
            response = self.client.get(url, HTTP_X_ORGS='999,%s' % self.org.dir_id)
            self.assertEqual(response.status_code, 200)

    def test_shouldnt_return_queue_data_with_get_param(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_queue', return_value=self.mock_queue),
            patch.object(StartrekClient, 'get_priorities', return_value=self.mock_priorities),
        ):
            url = '/admin/api/v2/startrek-queues/FORMS/?survey=%s' % self.b2c_survey.pk
            response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
            self.assertEqual(response.status_code, 404)

    def test_shouldnt_return_queue_data_with_get_param_with_empty_orgs(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_queue', return_value=self.mock_queue),
            patch.object(StartrekClient, 'get_priorities', return_value=self.mock_priorities),
        ):
            url = '/admin/api/v2/startrek-queues/FORMS/?survey=%s' % self.b2b_survey.pk
            response = self.client.get(url, HTTP_X_ORGS='')
            self.assertEqual(response.status_code, 404)

    def test_shouldnt_return_queue_data_with_get_param_without_orgs(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_queue', return_value=self.mock_queue),
            patch.object(StartrekClient, 'get_priorities', return_value=self.mock_priorities),
        ):
            url = '/admin/api/v2/startrek-queues/FORMS/?survey=%s' % self.b2b_survey.pk
            response = self.client.get(url)
            self.assertEqual(response.status_code, 404)


@override_settings(IS_BUSINESS_SITE=True)
class TestStartrekFieldViewSet(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=False)
        self.profile.is_staff = True
        self.profile.save(update_fields=['is_staff'])

        self.o2g = OrganizationToGroupFactory()
        self.org = self.o2g.org
        self.o2g.group.user_set.add(self.profile)

        self.b2b_survey = SurveyFactory(user=self.profile, org=self.org)
        self.b2c_survey = SurveyFactory(user=self.profile)

        self.mock_auth_kwargs = {
            'uid': self.profile.uid,
        }
        self.mock_fields = [{
            'id': 'tags',
            'name': 'Tags',
            'schema': {
                'type': 'array',
                'items': 'string',
            },
        }]

    def test_should_return_field_data_without_get_param(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_fields', return_value=self.mock_fields),
        ):
            text = 'Tag'
            url = '/admin/api/v2/startrek-fields/suggest/?search=%s' % text
            response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(len(response.data), 0)

    def test_should_return_field_data_with_get_param(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_fields', return_value=self.mock_fields),
        ):
            text = 'Tag'
            url = '/admin/api/v2/startrek-fields/suggest/?search=%s&survey=%s' % (text, self.b2b_survey.pk)
            response = self.client.get(url, HTTP_X_ORGS='999,%s' % self.org.dir_id)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(len(response.data), 1)
            self.assertEqual(response.data[0]['slug'], 'tags')

    @override_settings(IS_BUSINESS_SITE=False)
    def test_should_return_local_field_data_with_get_param(self):
        local_fields = [
            {'id': '01234--cats', 'key': 'cats', 'name': 'Cats', 'schema': {'type': 'string'}},
        ]
        queue = 'FORMS'
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_fields', return_value=self.mock_fields),
            patch.object(StartrekClient, 'get_local_fields', return_value=local_fields) as mock_local_fields,
        ):
            text = 'cat'
            url = '/admin/api/v2/startrek-fields/suggest/?search=%s&survey=%s&queue=%s' % (text, self.b2b_survey.pk, queue)
            response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(len(response.data), 1)
            self.assertEqual(response.data[0]['slug'], 'cats')
            mock_local_fields.assert_called_once_with(queue)

    def test_shouldnt_return_field_data_with_get_param(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_fields', return_value=self.mock_fields),
        ):
            text = 'Tag'
            url = '/admin/api/v2/startrek-fields/suggest/?search=%s&survey=%s' % (text, self.b2c_survey.pk)
            response = self.client.get(url, HTTP_X_ORGS='999,%s' % self.org.dir_id)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.data, [])

    def test_shouldnt_return_field_data_with_empty_orgs(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_fields', return_value=self.mock_fields),
        ):
            text = 'Tag'
            url = '/admin/api/v2/startrek-fields/suggest/?search=%s&survey=%s' % (text, self.b2b_survey.pk)
            response = self.client.get(url, HTTP_X_ORGS='')
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.data, [])

    def test_shouldnt_return_field_data_without_orgs(self):
        with (
            patch('events.common_app.startrek.client.get_startrek_auth_kwargs', return_value=self.mock_auth_kwargs),
            patch.object(StartrekClient, 'get_fields', return_value=self.mock_fields),
        ):
            text = 'Tag'
            url = '/admin/api/v2/startrek-fields/suggest/?search=%s&survey=%s' % (text, self.b2b_survey.pk)
            response = self.client.get(url)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.data, [])
