# -*- coding: utf-8 -*-
from datetime import timedelta
from django.conf import settings
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm
from unittest.mock import patch

from events.accounts.factories import UserFactory
from events.accounts.helpers import YandexClient
from events.accounts.managers import UserManager
from events.accounts.models import User, UserKarmaError, UserKarmaWarn
from events.surveyme.factories import SurveyFactory, SurveyTemplateFactory
from events.surveyme.models import Survey


class TestUserKarma_get_karma(TestCase):
    fixtures = ['initial_data.json']

    def test_get_karma_intranet(self):
        user = UserFactory()
        with patch.object(UserManager, 'get_params_from_passport') as mock_params:
            karma = user.get_karma()
        self.assertEqual(karma, 0)
        mock_params.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_get_karma_business(self):
        user = UserFactory()
        with patch.object(UserManager, 'get_params_from_passport') as mock_params:
            mock_params.return_value = {'karma': '80'}
            karma = user.get_karma()
        self.assertEqual(karma, 80)
        mock_params.assert_called_once_with(user.uid)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_get_karma_anonymous(self):
        user = UserFactory(uid=None)
        with patch.object(UserManager, 'get_params_from_passport') as mock_params:
            karma = user.get_karma()
        self.assertEqual(karma, 0)
        mock_params.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_get_karma_mock_profile(self):
        user = User.objects.get(pk=settings.MOCK_PROFILE_ID)
        with patch.object(UserManager, 'get_params_from_passport') as mock_params:
            karma = user.get_karma()
        self.assertEqual(karma, 0)
        mock_params.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_get_karma_business_cached(self):
        user = UserFactory()
        with patch.object(UserManager, '_get_params_from_passport') as mock_params:
            mock_params.return_value = {'karma': '80'}
            karma = user.get_karma()
        self.assertEqual(karma, 80)
        mock_params.assert_called_once_with(user.uid)

        with patch.object(UserManager, '_get_params_from_passport') as mock_params:
            karma = user.get_karma()
        self.assertEqual(karma, 80)
        mock_params.assert_not_called()


class TestUserKarma_check_karma(TestCase):
    fixtures = ['initial_data.json']

    def test_check_karma_over_85(self):
        user = UserFactory()
        with patch.object(User, 'get_karma', return_value=100) as mock_karma:
            with self.assertRaises(UserKarmaError):
                user.check_karma()
        mock_karma.assert_called_once()

    def test_check_karma_over_75(self):
        user = UserFactory()
        with patch.object(User, 'get_karma', return_value=80) as mock_karma:
            with self.assertRaises(UserKarmaWarn):
                user.check_karma()
        mock_karma.assert_called_once()

    def test_check_karma_over_75_with_new_survey(self):
        user = UserFactory()
        SurveyFactory(user=user)
        with patch.object(User, 'get_karma', return_value=80) as mock_karma:
            with self.assertRaises(UserKarmaError):
                user.check_karma()
        mock_karma.assert_called_once()

    def test_check_karma_over_75_with_old_survey(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        survey.date_created -= timedelta(hours=1)
        survey.save()
        with patch.object(User, 'get_karma', return_value=80) as mock_karma:
            with self.assertRaises(UserKarmaWarn):
                user.check_karma()
        mock_karma.assert_called_once()

    def test_check_karma_below_75(self):
        user = UserFactory()
        SurveyFactory(user=user)
        with patch.object(User, 'get_karma', return_value=0) as mock_karma:
            user.check_karma()
        mock_karma.assert_called_once()


class TestUserKarma_import_survey(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_with_karma_over_85(self):
        karma = 100
        data = {'survey': {'name': 'New imported'}}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data['code'], 'bad-karma-85')
        mock_karma.assert_called_once()

    def test_with_karma_over_75_with_new_survey(self):
        karma = 80
        SurveyFactory(user=self.user)
        data = {'survey': {'name': 'New imported'}}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data['code'], 'bad-karma-75')
        mock_karma.assert_called_once()

    def test_with_karma_over_75_with_old_survey(self):
        karma = 80
        survey = SurveyFactory(user=self.user)
        survey.date_created -= timedelta(hours=1)
        survey.save()
        data = {'survey': {'name': 'New imported'}}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_called_once()

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)
        self.assertEqual(survey.name, 'New imported')
        self.assertEqual(survey.user, self.user)
        self.assertEqual(survey.is_published_external, False)

    def test_with_karma_below_75_json_intranet(self):
        karma = 0
        data = {'survey': {'name': 'New imported'}}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_called_once()

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)
        self.assertEqual(survey.name, 'New imported')
        self.assertEqual(survey.user, self.user)
        self.assertEqual(survey.is_published_external, False)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_with_karma_below_75_json(self):
        karma = 0
        data = {'survey': {'name': 'New imported'}}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_called_once()

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)
        self.assertEqual(survey.name, 'New imported')
        self.assertEqual(survey.user, self.user)
        self.assertEqual(survey.is_published_external, True)

    def test_with_karma_below_75_template_intranet(self):
        template_data = {'survey': {'name': 'New imported'}}
        template = SurveyTemplateFactory(data=template_data)
        karma = 0
        data = {'import_from_template': template.pk}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_called_once()

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)
        self.assertEqual(survey.name, 'New imported')
        self.assertEqual(survey.user, self.user)
        self.assertEqual(survey.is_published_external, False)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_with_karma_below_75_template(self):
        template_data = {'survey': {'name': 'New imported'}}
        template = SurveyTemplateFactory(data=template_data)
        karma = 0
        data = {'import_from_template': template.pk}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_called_once()

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)
        self.assertEqual(survey.name, 'New imported')
        self.assertEqual(survey.user, self.user)
        self.assertEqual(survey.is_published_external, False)


class TestUserKarma_patch_survey(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(user=self.user)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, self.survey)
        self.client.set_cookie(self.user.uid)

    def test_with_karma_over_85(self):
        karma = 100
        data = {'is_published_external': True}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data['code'], 'bad-karma-85')
        mock_karma.assert_called_once()

        self.survey.is_published_external = True
        self.survey.save()
        data = {'is_published_external': False}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_not_called()

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.is_published_external, False)

    def test_with_karma_over_75_with_new_survey(self):
        karma = 80
        SurveyFactory(user=self.user)
        data = {'is_published_external': True}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data['code'], 'bad-karma-75')
        mock_karma.assert_called_once()

        self.survey.is_published_external = True
        self.survey.save()
        data = {'is_published_external': False}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_not_called()

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.is_published_external, False)

    def test_with_karma_over_75_with_old_survey(self):
        karma = 80
        survey = SurveyFactory(user=self.user)
        survey.date_created -= timedelta(hours=1)
        survey.save()
        data = {'is_published_external': True}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data['code'], 'bad-karma-75')
        mock_karma.assert_called_once()

        self.survey.is_published_external = True
        self.survey.save()
        data = {'is_published_external': False}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_not_called()

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.is_published_external, False)

    def test_with_karma_below_75(self):
        karma = 0
        data = {'is_published_external': True}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_called_once()

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.is_published_external, True)

        self.survey.is_published_external = True
        self.survey.save()
        data = {'is_published_external': False}

        with patch.object(User, 'get_karma', return_value=karma) as mock_karma:
            response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        mock_karma.assert_not_called()

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.is_published_external, False)
