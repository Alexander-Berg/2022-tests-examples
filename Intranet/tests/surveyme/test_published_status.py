# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm
from unittest.mock import patch

from events.accounts.models import User
from events.accounts.factories import OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory
from events.surveyme.models import (
    SURVEY_STATUS_PUBLISHED,
    SURVEY_STATUS_UNPUBLISHED,
)


class TestPublishedStatus(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.base_url = '/admin/api/v2/surveys/'
        self.list_url = '/admin/api/v2/surveys/?status=%s'
        self.details_url = '/admin/api/v2/surveys/%s/'

    def test_draft(self):
        # форма должна быть в выдаче черновиков
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        # форма не должна быть в выдаче обпубликованных форм
        url = self.list_url % SURVEY_STATUS_PUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

        # форма должна быть в выдаче форм снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        # форма должна быть в дефолтной выдаче, без фильтров
        url = self.base_url
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

    def test_published(self):
        # делаем форму опубликованной
        data = {'is_published_external': True}
        url = self.details_url % self.survey.id
        response = self.client.patch(url, data)
        self.assertEqual(200, response.status_code)

        # форма не должна быть в выдаче черновиков
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

        # форма должна быть в выдаче обпубликованных форм
        url = self.list_url % SURVEY_STATUS_PUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        # форма не должна быть в выдаче форм снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

        # форма должна быть в дефолтной выдаче, без фильтров
        url = self.base_url
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

    def test_unpublished(self):
        # делаем форму опубликованной
        self.survey.is_published_external = True
        self.survey.save()

        # снимаем форму с публикации
        data = {'is_published_external': False}
        url = self.details_url % self.survey.id
        response = self.client.patch(url, data)
        self.assertEqual(200, response.status_code)

        # форма должна быть в выдаче снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        # форма не должна быть в выдаче обпубликованных форм
        url = self.list_url % SURVEY_STATUS_PUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

        # форма должна быть в выдаче форм снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        # форма должна быть в дефолтной выдаче, без фильтров
        url = self.base_url
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)


@override_settings(IS_BUSINESS_SITE=True)
class TestPublishedStatusWithOrg(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        super().setUp()
        self.o2g = OrganizationToGroupFactory()
        self.org = self.o2g.org
        self.orgs_data = [self.org.dir_id]
        with patch.object(User, '_get_organizations', return_value=self.orgs_data):
            self.profile = self.client.login_yandex()

        self.survey = SurveyFactory()
        self.survey.org = self.org
        self.survey.save()
        assign_perm('surveyme.change_survey', self.profile, self.survey)
        self.base_url = '/admin/api/v2/surveys/'
        self.list_url = '/admin/api/v2/surveys/?status=%s'
        self.details_url = '/admin/api/v2/surveys/%s/'

    def test_draft(self):
        # форма должна быть в выдаче черновиков
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.org = None
        self.survey.save()

        # формы не должно быть в выдаче черновиков
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

    def test_published(self):
        # делаем форму опубликованной
        data = {'is_published_external': True}
        url = self.details_url % self.survey.id
        with patch.object(User, 'get_karma', return_value=0):
            response = self.client.patch(url, data, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)

        # форма должна быть в выдаче опубликованных
        url = self.list_url % SURVEY_STATUS_PUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.org = None
        self.survey.save()

        # формы не должно быть в выдаче опубликованных
        url = self.list_url % SURVEY_STATUS_PUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)

    def test_unpublished(self):
        # делаем форму опубликованной
        self.survey.is_published_external = True
        self.survey.save()

        # снимаем форму с публикации
        data = {'is_published_external': False}
        url = self.details_url % self.survey.id
        response = self.client.patch(url, data, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)

        # форма должна быть в выдаче форм снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.org = None
        self.survey.save()

        # формы не должно быть в выдаче форм снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(self.survey.pk, ids)


@override_settings(IS_BUSINESS_SITE=True)
class PublishedStatusWithOrgTest__superuser(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        super().setUp()
        self.o2g = OrganizationToGroupFactory()
        self.org = self.o2g.org
        self.orgs_data = [self.org.dir_id]
        with patch.object(User, '_get_organizations', return_value=self.orgs_data):
            self.profile = self.client.login_yandex(is_superuser=True)

        self.survey = SurveyFactory()
        self.survey.org = self.org
        self.survey.save()
        self.base_url = '/admin/api/v2/surveys/'
        self.list_url = '/admin/api/v2/surveys/?status=%s'
        self.details_url = '/admin/api/v2/surveys/%s/'

    def test_unpublished(self):
        # делаем форму опубликованной
        self.survey.is_published_external = True
        self.survey.save()

        # снимаем форму с публикации
        data = {'is_published_external': False}
        url = self.details_url % self.survey.id
        response = self.client.patch(url, data, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)

        # форма должна быть в выдаче форм снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)

        self.survey.refresh_from_db()
        o2g = OrganizationToGroupFactory()
        self.survey.org = o2g.org
        self.survey.save()

        # форма должна быть в выдаче форм снятых с публикации
        url = self.list_url % SURVEY_STATUS_UNPUBLISHED
        response = self.client.get(url, HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(200, response.status_code)
        data = response.json()
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.survey.pk, ids)
