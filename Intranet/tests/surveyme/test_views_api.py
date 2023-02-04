# -*- coding: utf-8 -*-
from django.test import TestCase

from events.accounts.factories import OrganizationFactory
from events.surveyme.factories import SurveyFactory


class TestSurveyStatusView(TestCase):
    fixtures = ['initial_data.json']

    def test_should_return_data_for_published(self):
        survey = SurveyFactory(is_published_external=True, is_ban_detected=False)
        response = self.client.get(f'/v1/surveys/{survey.pk}/status/')
        self.assertEqual(response.status_code, 200)
        self.assertDictEqual(response.json(), {
            'id': survey.pk,
            'is_public': survey.is_public,
            'org_id': None,
        })

    def test_should_return_data_for_public(self):
        org = OrganizationFactory()
        survey = SurveyFactory(is_published_external=True, is_public=True, org=org)
        response = self.client.get(f'/v1/surveys/{survey.pk}/status/')
        self.assertEqual(response.status_code, 200)
        self.assertDictEqual(response.json(), {
            'id': survey.pk,
            'is_public': survey.is_public,
            'org_id': org.dir_id,
        })

    def test_shouldnt_return_data_for_not_published(self):
        survey = SurveyFactory(is_published_external=False)
        response = self.client.get(f'/v1/surveys/{survey.pk}/status/')
        self.assertEqual(response.status_code, 404)

    def test_shouldnt_return_data_for_banned(self):
        survey = SurveyFactory(is_published_external=True, is_ban_detected=True)
        response = self.client.get(f'/v1/surveys/{survey.pk}/status/')
        self.assertEqual(response.status_code, 404)

    def test_shouldnt_return_data_for_not_existed(self):
        response = self.client.get('/v1/surveys/1000000/status/')
        self.assertEqual(response.status_code, 404)
