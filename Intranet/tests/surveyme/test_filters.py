# -*- coding: utf-8 -*-
from django.conf import settings
from django.test import TestCase
from django.test.utils import override_settings
from guardian.shortcuts import assign_perm
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.accounts.factories import OrganizationFactory, UserFactory, OrganizationToGroupFactory
from events.accounts.models import User
from events.surveyme.factories import SurveyFactory
from events.surveyme.models import (
    SURVEY_OWNERSHIP_MYFORMS,
    SURVEY_OWNERSHIP_SHARED,
)


class TestOwnership(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.base_url = '/admin/api/v2/surveys/'
        self.list_url = '/admin/api/v2/surveys/?ownership=%s'
        self.details_url = '/admin/api/v2/surveys/%s/'

    def test_myforms(self):
        self.survey.user = self.user
        self.survey.save()

        # форма должна быть в выдаче моих
        url = self.list_url % SURVEY_OWNERSHIP_MYFORMS
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        # формы не должно быть в выдаче чужих
        url = self.list_url % SURVEY_OWNERSHIP_SHARED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

    def test_shared(self):
        self.survey.user = None
        self.survey.save()

        # форма должна быть в выдаче чужих
        url = self.list_url % SURVEY_OWNERSHIP_SHARED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        # формы не должно быть в выдаче моих
        url = self.list_url % SURVEY_OWNERSHIP_MYFORMS
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

    def test_without_filter(self):
        self.survey.user = self.user
        self.survey.save()

        # форма должна быть в выдаче
        response = self.client.get(self.base_url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.user = None
        self.survey.save()

        # форма должна быть в выдаче
        response = self.client.get(self.base_url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)


@override_settings(IS_BUSINESS_SITE=True)
class TestOwnershipWithOrg(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        super().setUp()
        self.o2g = OrganizationToGroupFactory()
        self.org = self.o2g.org
        self.orgs_data = ['732', ]
        with patch.object(User, '_get_organizations', return_value=self.orgs_data):
            self.user = self.client.login_yandex()

        self.survey = SurveyFactory()
        self.survey.org = self.org
        self.survey.save()
        assign_perm('surveyme.change_survey', self.user, self.survey)
        self.base_url = '/admin/api/v2/surveys/'
        self.list_url = '/admin/api/v2/surveys/?ownership=%s'
        self.details_url = '/admin/api/v2/surveys/%s/'

    def test_myforms(self):
        self.survey.user = self.user
        self.survey.save()

        # форма должна быть в выдаче моих
        url = self.list_url % SURVEY_OWNERSHIP_MYFORMS
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.org = None
        self.survey.save()

        # форма должна быть в выдаче моих
        url = self.list_url % SURVEY_OWNERSHIP_MYFORMS
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

    def test_shared(self):
        self.survey.user = None
        self.survey.save()

        # форма должна быть в выдаче чужих
        url = self.list_url % SURVEY_OWNERSHIP_SHARED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.org = None
        self.survey.save()

        # формы не должно быть в выдаче чужих
        url = self.list_url % SURVEY_OWNERSHIP_SHARED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

    def test_without_filter(self):
        self.survey.user = self.user
        self.survey.save()

        # форма должна быть в выдаче
        response = self.client.get(self.base_url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.user = None
        self.survey.save()

        # форма должна быть в выдаче
        response = self.client.get(self.base_url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.user = self.user
        self.survey.org = None
        self.survey.save()

        # форма должна быть в выдаче
        response = self.client.get(self.base_url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.user = None
        self.survey.save()

        # формы не должно быть в выдаче
        response = self.client.get(self.base_url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)


class TestOrg(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)

        self.survey0 = SurveyFactory(user=self.user)

        self.org1 = OrganizationFactory()
        self.survey1 = SurveyFactory(org=self.org1)

        self.org2 = OrganizationFactory()
        self.survey2 = SurveyFactory(org=self.org2)

    def get_results(self, response):
        return {
            it['id']: it['org_id']
            for it in response.data['results']
        }

    def test_should_return_all_surveys(self):
        response = self.client.get('/admin/api/v2/surveys/')
        self.assertEqual(response.status_code, 200)

        results = self.get_results(response)
        self.assertEqual(len(results), 3)
        self.assertIsNone(results[self.survey0.pk])
        self.assertEqual(results[self.survey1.pk], self.survey1.org.dir_id)
        self.assertEqual(results[self.survey2.pk], self.survey2.org.dir_id)

        response = self.client.get('/admin/api/v2/surveys/?org=')
        self.assertEqual(response.status_code, 200)

        results = self.get_results(response)
        self.assertEqual(len(results), 3)
        self.assertIsNone(results[self.survey0.pk])
        self.assertEqual(results[self.survey1.pk], self.survey1.org.dir_id)
        self.assertEqual(results[self.survey2.pk], self.survey2.org.dir_id)

    def test_should_return_survey_without_org(self):
        response = self.client.get('/admin/api/v2/surveys/?org=mine')
        self.assertEqual(response.status_code, 200)

        results = self.get_results(response)
        self.assertEqual(len(results), 1)
        self.assertIsNone(results[self.survey0.pk])

    def test_should_return_survey_with_org1(self):
        response = self.client.get(f'/admin/api/v2/surveys/?org={self.org1.dir_id}')
        self.assertEqual(response.status_code, 200)

        results = self.get_results(response)
        self.assertEqual(len(results), 1)
        self.assertEqual(results[self.survey1.pk], self.survey1.org.dir_id)

    def test_should_return_survey_with_org1_and_org2(self):
        response = self.client.get(f'/admin/api/v2/surveys/?org={self.org1.dir_id},{self.org2.dir_id}')
        self.assertEqual(response.status_code, 200)

        results = self.get_results(response)
        self.assertEqual(len(results), 2)
        self.assertEqual(results[self.survey1.pk], self.survey1.org.dir_id)
        self.assertEqual(results[self.survey2.pk], self.survey2.org.dir_id)

    def test_should_return_survey_with_all_options(self):
        response = self.client.get(f'/admin/api/v2/surveys/?org=mine,{self.org1.dir_id},{self.org2.dir_id}')
        self.assertEqual(response.status_code, 200)

        results = self.get_results(response)
        self.assertEqual(len(results), 3)
        self.assertIsNone(results[self.survey0.pk])
        self.assertEqual(results[self.survey1.pk], self.survey1.org.dir_id)
        self.assertEqual(results[self.survey2.pk], self.survey2.org.dir_id)

    def test_shouldn_return_any_surveys(self):
        response = self.client.get('/admin/api/v2/surveys/?org=invalid')
        self.assertEqual(response.status_code, 200)

        results = self.get_results(response)
        self.assertEqual(len(results), 0)


class TestFilterByUserUid(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def test_superuser_should_filter_all_surveys(self):
        self.client.login_yandex(is_superuser=True)
        users = [UserFactory(), UserFactory()]
        surveys = [
            SurveyFactory(user=users[0]), SurveyFactory(user=users[0]),
            SurveyFactory(user=users[1]), SurveyFactory(user=users[1]),
        ]

        response = self.client.get(f'/admin/api/v2/surveys/?user_uid={users[0].uid}')
        data = response.data['results']
        self.assertEqual(len(data), 2)
        self.assertEqual({data[0]['id'], data[1]['id']}, {surveys[0].pk, surveys[1].pk})

        response = self.client.get(f'/admin/api/v2/surveys/?user_uid={users[1].uid}')
        data = response.data['results']
        self.assertEqual(len(data), 2)
        self.assertEqual({data[0]['id'], data[1]['id']}, {surveys[2].pk, surveys[3].pk})

    def test_user_should_filter_only_owned_surveys(self):
        user = self.client.login_yandex()
        users = [UserFactory(), UserFactory()]
        surveys = [
            SurveyFactory(user=users[0]), SurveyFactory(user=users[0]),
            SurveyFactory(user=users[1]), SurveyFactory(user=users[1]),
        ]
        assign_perm(settings.ROLE_FORM_MANAGER, user, surveys[0])
        assign_perm(settings.ROLE_FORM_MANAGER, user, surveys[3])

        response = self.client.get(f'/admin/api/v2/surveys/?user_uid={users[0].uid}')
        data = response.data['results']
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]['id'], surveys[0].pk)

        response = self.client.get(f'/admin/api/v2/surveys/?user_uid={users[1].uid}')
        data = response.data['results']
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]['id'], surveys[3].pk)
