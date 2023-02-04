# -*- coding: utf-8 -*-
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase, override_settings
from unittest.mock import patch

from events.accounts.factories import UserFactory, OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.history.models import HistoryRawEntry
from events.surveyme.factories import SurveyFactory
from events.surveyme.models import Survey


class TestSupportViewSet(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def _run_change_owner_success_test(self, user):
        self.client.set_cookie(user.uid)
        survey = SurveyFactory(user=UserFactory())
        data = {
            'survey_id': survey.pk,
            'uid': user.uid,
        }
        response = self.client.post('/support/change-owner/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        expected = {'status': 'success'}
        self.assertEqual(response.data, expected)

        survey.refresh_from_db()
        self.assertEqual(survey.user, user)

        ct = ContentType.objects.get_for_model(Survey)
        history_qs = HistoryRawEntry.objects.filter(content_type=ct, object_id=str(survey.pk))
        self.assertTrue(history_qs.exists())

    def _run_change_organization_success_test(self, user):
        self.client.set_cookie(user.uid)
        survey = SurveyFactory(user=UserFactory())
        o2g = OrganizationToGroupFactory()
        survey.user.groups.add(o2g.group)
        data = {
            'survey_id': survey.pk,
            'dir_id': o2g.org.dir_id,
        }
        response = self.client.post('/support/change-organization/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        expected = {'status': 'success'}
        self.assertEqual(response.data, expected)

        survey.refresh_from_db()
        self.assertEqual(survey.org, o2g.org)

        ct = ContentType.objects.get_for_model(Survey)
        history_qs = HistoryRawEntry.objects.filter(content_type=ct, object_id=str(survey.pk))
        self.assertTrue(history_qs.exists())

    def test_change_owner_1(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)
        data = {
            'survey_id': 123,
            'uid': user.uid,
        }
        response = self.client.post('/support/change-owner/', data=data, format='json')
        self.assertEqual(response.status_code, 403)

    def test_change_owner_2(self):
        user = UserFactory(is_superuser=True)
        self._run_change_owner_success_test(user)

    def test_change_owner_3(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)
        survey = SurveyFactory(user=UserFactory())
        data = {
            'survey_id': survey.pk,
            'uid': 'abcd',
        }
        response = self.client.post('/support/change-owner/', data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_change_owner_4(self):
        user = UserFactory(is_staff=True)
        self._run_change_owner_success_test(user)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_change_owner_4_business_site(self):
        user = UserFactory(is_staff=True)
        self._run_change_owner_success_test(user)

    def test_change_organization_1(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)
        data = {
            'survey_id': 123,
            'dir_id': None,
        }
        response = self.client.post('/support/change-organization/', data=data, format='json')
        self.assertEqual(response.status_code, 403)

    def test_change_organization_2(self):
        user = UserFactory(is_superuser=True)
        self._run_change_organization_success_test(user)

    def test_change_organization_3(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)
        o2g = OrganizationToGroupFactory()
        survey = SurveyFactory(user=UserFactory(), org=o2g.org)
        survey.user.groups.add(o2g.group)
        data = {
            'survey_id': survey.pk,
            'dir_id': None,
        }
        response = self.client.post('/support/change-organization/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        expected = {'status': 'success'}
        self.assertEqual(response.data, expected)

        survey.refresh_from_db()
        self.assertIsNone(survey.org)

        ct = ContentType.objects.get_for_model(Survey)
        history_qs = HistoryRawEntry.objects.filter(content_type=ct, object_id=str(survey.pk))
        self.assertTrue(history_qs.exists())

    def test_change_organization_4(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)
        survey = SurveyFactory(user=UserFactory())
        data = {
            'survey_id': survey.pk,
            'dir_id': 'abcd',
        }
        response = self.client.post('/support/change-organization/', data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_change_organization_5(self):
        user = UserFactory(is_staff=True)
        self._run_change_organization_success_test(user)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_change_organization_5_business_site(self):
        user = UserFactory(is_staff=True)
        self._run_change_organization_success_test(user)

    def test_tasks_1(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)
        task_id = '1234567890-abcdef-ABCDEF'
        with patch('events.support.utils.get_task_info') as mock_task_info:
            mock_task_info.return_value = {'task_id': task_id, 'ready': True, 'failed': False}
            response = self.client.get(f'/support/tasks/{task_id}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['task_id'], task_id)
        mock_task_info.assert_called_once_with(task_id)

    def test_tasks_2(self):
        task_id = 'not-a-task-id'
        response = self.client.get(f'/support/tasks/{task_id}/')
        self.assertEqual(response.status_code, 404)
