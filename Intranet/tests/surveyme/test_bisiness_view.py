# -*- coding: utf-8 -*-
from django.test import override_settings
from guardian.shortcuts import assign_perm, remove_perm

from events.accounts.factories import OrganizationToGroupFactory, UserFactory
from events.surveyme.factories import SurveyFactory, SurveyQuestionFactory
from events.surveyme.models import AnswerType
from events.yauth_contrib.helpers import CookieAuthTestCase


@override_settings(IS_BUSINESS_SITE=True)
class TestBusinessAdminApiPermissions(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        o2g = OrganizationToGroupFactory()
        self.org = o2g.org
        self.group = o2g.group
        self.user = UserFactory()
        self.group.user_set.add(self.user)
        self.survey = SurveyFactory(user=self.user, org=self.org)
        assign_perm('surveyme.change_survey', self.user, self.survey)
        self.client.set_cookie(self.user.uid)

    def test_user_should_create_survey(self):
        self.client.set_cookie(self.user.uid)
        data = {
            'name': 'survey1',
            'org_id': self.org.dir_id,
        }
        response = self.client.post(
            '/admin/api/v2/surveys/',
            data=data, format='json', HTTP_X_ORGS=str(self.org.dir_id),
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['name'], 'survey1')
        self.assertEqual(response.data['org_id'], self.org.dir_id)

    def test_user_should_get_surveys(self):
        data = {
            'name': 'survey1',
        }
        response = self.client.post(
            '/admin/api/v2/surveys/', data=data, format='json',
            HTTP_X_ORGS='732',
        )
        self.assertEqual(response.status_code, 201)

        response = self.client.get('/admin/api/v2/surveys/', HTTP_X_ORGS='732')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['results']), 1)

    def test_user_from_another_org_shouldnt_access_to_survey(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/', HTTP_X_ORGS='9999')
        self.assertEqual(response.status_code, 404)

    def test_user_from_another_org_should_receive_404_for_not_extisting_survey(self):
        response = self.client.get('/admin/api/v2/surveys/5432/', HTTP_X_ORGS='9999')
        self.assertEqual(response.status_code, 404)

    def test_superuser_from_another_org_should_access_to_survey(self):
        self.user.is_superuser = True
        self.user.save()

        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/', HTTP_X_ORGS='9999')
        self.assertEqual(response.status_code, 200)

    def test_superuser_from_another_org_should_receive_404_for_not_extisting_survey(self):
        self.user.is_superuser = True
        self.user.save()

        response = self.client.get('/admin/api/v2/surveys/5432/', HTTP_X_ORGS='9999')
        self.assertEqual(response.status_code, 404)

    def test_user_without_access_rights(self):
        self.client.remove_cookie()

        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/', HTTP_X_ORGS='9999')
        self.assertEqual(response.status_code, 401)

    def test_not_a_connect_user(self):
        self.client.remove_cookie()

        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/', HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 401)

    def test_user_with_permissions_from_the_same_org_should_access_to_survey(self):
        response = self.client.get(
            f'/admin/api/v2/surveys/{self.survey.pk}/',
            HTTP_X_ORGS=str(self.survey.org.dir_id),
        )
        self.assertEqual(response.status_code, 200)

    def test_superuser_from_the_same_org_should_access_to_survey(self):
        self.user.is_superuser = True
        self.user.save()
        remove_perm('surveyme.change_survey', self.user, self.survey)

        response = self.client.get(
            f'/admin/api/v2/surveys/{self.survey.pk}/',
            HTTP_X_ORGS=str(self.survey.org.dir_id),
        )
        self.assertEqual(response.status_code, 200)

    def test_user_without_permissions_from_the_same_org_shouldnt_access_to_survey(self):
        remove_perm('surveyme.change_survey', self.user, self.survey)

        response = self.client.get(
            f'/admin/api/v2/surveys/{self.survey.pk}/',
            HTTP_X_ORGS=self.survey.org.dir_id,
        )
        self.assertEqual(response.status_code, 403)
        self.assertTrue('detail' in response.data)

    def test_user_with_permission_from_the_same_org_should_create_field(self):
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': answer_short_text.pk,
            'label': 'label1',
        }
        response = self.client.post(
            '/admin/api/v2/survey-questions/', data=data, format='json',
            HTTP_X_ORGS=self.survey.org.dir_id,
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['label'], 'label1')

    def test_user_without_permission_from_the_same_org_shouldnt_create_field(self):
        remove_perm('surveyme.change_survey', self.user, self.survey)

        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': answer_short_text.pk,
            'label': 'label1',
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 404)

    def test_user_with_permission_from_the_same_org_should_change_field(self):
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_short_text,
            label='label1',
        )
        data = {
            'label': 'label2',
        }
        response = self.client.patch(
            f'/admin/api/v2/survey-questions/{question.pk}/', data=data, format='json',
            HTTP_X_ORGS=self.survey.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], 'label2')

    def test_user_without_permission_from_the_same_org_shouldnt_change_field(self):
        remove_perm('surveyme.change_survey', self.user, self.survey)

        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_short_text,
            label='label1',
        )
        data = {
            'label': 'label2',
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 404)


@override_settings(IS_BUSINESS_SITE=True)
class TestBusinessApiPermissions(CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        o2g = OrganizationToGroupFactory()
        self.org = o2g.org
        self.group = o2g.group
        self.user = UserFactory()
        self.group.user_set.add(self.user)

        self.survey = SurveyFactory(user=self.user, org=self.org)
        assign_perm('surveyme.change_survey', self.user, self.survey)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.client.set_cookie(self.user.uid)

    def test_user_from_another_org_shouldnt_access_to_survey(self):
        o2g = OrganizationToGroupFactory()
        user = UserFactory()
        o2g.group.user_set.add(user)
        self.client.set_cookie(user.uid)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 404)

    def test_user_should_receive_404_for_not_existing_survey(self):
        response = self.client.get('/v1/surveys/5432/', HTTP_X_ORGS=self.org.dir_id)
        self.assertEqual(response.status_code, 404)

    def test_superuser_should_access_to_survey(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)

    def test_superuser_should_receive_404_for_not_existing_survey(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)

        response = self.client.get('/v1/surveys/5432/')
        self.assertEqual(response.status_code, 404)

    def test_not_logged_user_should_receive_404(self):
        self.client.remove_cookie()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_b2c_user_should_receive_404(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_user_without_xorgs_should_receive_404(self):
        user = UserFactory()
        self.group.user_set.add(user)
        assign_perm('surveyme.change_survey', user, self.survey)
        self.client.set_cookie(user.uid)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_user_from_the_same_org_should_access_to_survey(self):
        user = UserFactory()
        self.group.user_set.add(user)
        self.client.set_cookie(user.uid)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/', HTTP_X_ORGS=self.survey.org.dir_id)
        self.assertEqual(response.status_code, 200)

    def test_superuser_from_the_same_org_should_access_to_survey(self):
        user = UserFactory(is_superuser=True)
        self.group.user_set.add(user)
        self.client.set_cookie(user.uid)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/', HTTP_X_ORGS=self.survey.org.dir_id)
        self.assertEqual(response.status_code, 200)

    def test_b2c_user_should_access_to_public_survey(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)

        self.survey.is_published_external = True
        self.survey.is_public = True
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)

    def test_not_logged_user_should_access_to_public_survey(self):
        self.client.remove_cookie()

        self.survey.is_published_external = True
        self.survey.is_public = True
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)

    def test_user_from_another_org_should_access_to_public_survey(self):
        o2g = OrganizationToGroupFactory()
        user = UserFactory()
        o2g.group.user_set.add(user)

        self.client.set_cookie(user.uid)

        self.survey.is_published_external = True
        self.survey.is_public = True
        self.survey.save()

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/form/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
