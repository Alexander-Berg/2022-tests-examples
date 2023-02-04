# -*- coding: utf-8 -*-
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm, remove_perm
from events.accounts.factories import OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.accounts.factories import UserFactory
from events.surveyme.factories import (
    SurveyFactory, SurveyGroupFactory, ProfileSurveyAnswerFactory,
)
from events.surveyme_integration.factories import (
    SurveyHookFactory, ServiceSurveyHookSubscriptionFactory,
)
from events.surveyme_integration.variables.fields_restrictions import fields_restrictions_list
from events.surveyme_integration.models import HookSubscriptionNotification
from events.history.models import HistoryRawEntry


class TestHookSubscriptionNotificationView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.group = SurveyGroupFactory()
        self.survey = SurveyFactory(group=self.group)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey, user=self.user)
        self.integration = self.subscription.notify(self.answer, 'create')
        self.integration.save()
        self.integration_object = self.integration

    def test_filter_headers(self):
        self.user.is_superuser = True
        self.user.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/' % self.integration_object.id

        self.integration_object.context = {'headers': {
            'Authorization': 'Oauth some_secret_token', 'SomeHeader': 'some_data',
            'X-Ya-User-Ticket': 'secret_ticket',
        }}
        self.integration_object.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(
            response.data['context']['headers'],
            {'SomeHeader': 'some_data', 'Authorization': '****', 'X-Ya-User-Ticket': '****'},
        )

    def test_datetime_filter_success(self):
        query_datetime = "2099-07-27T14:14:09.065000Z"
        url = '/admin/api/v2/hook-subscription-notifications/?date_created__gt=%s' % query_datetime

        assign_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)

        query_datetime = "2001-07-27T14:14:09.065000Z"
        url = '/admin/api/v2/hook-subscription-notifications/?date_created__gt=%s' % query_datetime
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 1)

    def test_survey_group_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/?survey_group_id=%s' % self.group.pk

        assign_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

        remove_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

    def test_survey_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/?survey_id=%s' % self.survey.pk

        assign_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

        remove_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

    def test_subscription_through_survey_group_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/?subscription_id=%s' % self.subscription.pk

        assign_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

        remove_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

    def test_subscription_through_survey_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/?subscription_id=%s' % self.subscription.pk

        assign_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

        remove_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] >= 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

    def test_integrations_through_survey_group_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/?search=%s' % self.integration

        assign_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

        remove_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

    def test_integrations_through_survey_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/?search=%s' % self.integration

        assign_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

        remove_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 1)
        self.assertIn(str(self.integration.pk), set(it['_id'] for it in response.data['results']))

    def test_integrations_through_survey_group_access_in_list(self):
        survey = SurveyFactory()
        subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=SurveyHookFactory(survey=survey),
        )
        integration = subscription.notify(
            answer=ProfileSurveyAnswerFactory(
                survey=survey,
                user=UserFactory(),
            ),
            trigger_slug='create',
        )
        integration.save()

        url = '/admin/api/v2/hook-subscription-notifications/?id__in=%s,%s' % (self.integration.pk, integration.pk)

        assign_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 1)
        ids = set(it['_id'] for it in response.data['results'])
        self.assertIn(str(self.integration.pk), ids)
        self.assertNotIn(str(integration.pk), ids)

        remove_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 2)
        ids = set(it['_id'] for it in response.data['results'])
        self.assertIn(str(self.integration.pk), ids)
        self.assertIn(str(integration.pk), ids)

    def test_integrations_through_survey_access_in_list(self):
        survey = SurveyFactory()
        subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=SurveyHookFactory(survey=survey),
        )
        integration = subscription.notify(
            answer=ProfileSurveyAnswerFactory(
                survey=survey,
                user=UserFactory(),
            ),
            trigger_slug='create',
        )
        integration.save()

        url = '/admin/api/v2/hook-subscription-notifications/?id__in=%s,%s' % (self.integration.pk, integration.pk)

        assign_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 1)
        ids = set(it['_id'] for it in response.data['results'])
        self.assertIn(str(self.integration.pk), ids)
        self.assertNotIn(str(integration.pk), ids)

        remove_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertTrue(response.data['count'] == 2)
        ids = set(it['_id'] for it in response.data['results'])
        self.assertIn(str(self.integration.pk), ids)
        self.assertIn(str(integration.pk), ids)

    def test_integration_through_survey_group_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/%s/' % self.integration.pk

        assign_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(str(self.integration.pk), response.data['_id'])

        remove_perm('surveyme.change_surveygroup', self.user, self.group)
        response = self.client.get(url)
        self.assertEqual(403, response.status_code)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(str(self.integration.pk), response.data['_id'])

    def test_integration_through_survey_access(self):
        url = '/admin/api/v2/hook-subscription-notifications/%s/' % self.integration.pk

        assign_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(str(self.integration.pk), response.data['_id'])

        remove_perm('surveyme.change_survey', self.user, self.survey)
        response = self.client.get(url)
        self.assertEqual(403, response.status_code)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(str(self.integration.pk), response.data['_id'])

    def test_survey_group_not_exists(self):
        url = '/admin/api/v2/hook-subscription-notifications/?survey_group_id=%s' % self.group.pk

        self.group.delete()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(len(response.data['results']), 0)

    def test_survey_not_exists(self):
        url = '/admin/api/v2/hook-subscription-notifications/?survey_id=%s' % self.survey.pk

        self.survey.delete()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(len(response.data['results']), 0)

    def test_subscription_not_exists(self):
        url = '/admin/api/v2/hook-subscription-notifications/?subscription_id=%s' % self.subscription.pk
        self.subscription.delete()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(len(response.data['results']), 0)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(len(response.data['results']), 0)

    def test_integration_not_exists(self):
        url = '/admin/api/v2/hook-subscription-notifications/%s/' % 33333333

        response = self.client.get(url)
        self.assertEqual(404, response.status_code)

        self.user.is_superuser = True
        self.user.save()
        response = self.client.get(url)
        self.assertEqual(404, response.status_code)

    def test_restart_fail(self):
        self.integration_object.status = 'error'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/restart/' % self.integration.pk

        response = self.client.post(url)
        self.assertEqual(403, response.status_code)

    def test_restart_success_user(self):
        self.integration_object.status = 'error'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/restart/' % self.integration.pk

        assign_perm('change_survey', self.user, self.integration.survey)
        response = self.client.post(url)
        self.assertEqual(200, response.status_code)

        self.assertEqual(str(self.integration.pk), response.data['_id'])

    def test_restart_success_superuser(self):
        self.integration_object.status = 'error'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/restart/' % self.integration.pk

        self.user.is_superuser = True
        self.user.save()
        response = self.client.post(url)
        self.assertEqual(200, response.status_code)

        self.assertEqual(str(self.integration.pk), response.data['_id'])

    def test_cancel_fail(self):
        self.integration_object.status = 'pending'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/cancel/' % self.integration.pk

        response = self.client.post(url)
        self.assertEqual(403, response.status_code)

    def test_cancel_success(self):
        self.integration_object.status = 'pending'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/cancel/' % self.integration.pk

        assign_perm('change_survey', self.user, self.integration.survey)
        response = self.client.post(url)
        self.assertEqual(200, response.status_code)

        self.assertEqual(str(self.integration.pk), response.data['_id'])

    def test_cancel_success_superuser(self):
        self.integration_object.status = 'pending'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/cancel/' % self.integration.pk

        self.user.is_superuser = True
        self.user.save()
        response = self.client.post(url)
        self.assertEqual(200, response.status_code)

        self.assertEqual(str(self.integration.pk), response.data['_id'])

    def test_shouldnt_restart_successeded_notification(self):
        self.integration_object.status = 'success'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/restart/' % self.integration.pk

        self.user.is_superuser = True
        self.user.save()
        response = self.client.post(url)
        self.assertEqual(400, response.status_code)

        self.assertEqual(
            response.data,
            {'error_detail': 'Notification with status "success" could not be restarted'},
        )

    def test_shouldnt_cancel_successeded_notification(self):
        self.integration_object.status = 'success'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/cancel/' % self.integration.pk

        self.user.is_superuser = True
        self.user.save()
        response = self.client.post(url)
        self.assertEqual(400, response.status_code)

        self.assertEqual(
            response.data,
            {'error_detail': 'Notification with status "success" could not be canceled'},
        )

    def test_history_save_success(self):
        self.integration_object.status = 'pending'
        self.integration_object.save()
        url = '/admin/api/v2/hook-subscription-notifications/%s/cancel/' % self.integration.pk

        self.user.is_superuser = True
        self.user.save()
        response = self.client.post(url)
        self.assertEqual(200, response.status_code)

        ct = ContentType.objects.get_for_model(HookSubscriptionNotification)
        history = HistoryRawEntry.objects.get(content_type=ct, object_id=self.integration.pk)
        self.assertEqual(history.path, url)


class TestFieldRestrictionView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()

    def test_not_blank_in_business(self):
        response = self.client.get('/admin/api/v2/fields-restrictions/')
        self.assertEqual(200, response.status_code)

        self.assertEqual(len(response.data), len(fields_restrictions_list))
        self.assertEqual(
            {field['field_name'] for field in response.data},
            {field.field_name for field in fields_restrictions_list}
        )


@override_settings(IS_BUSINESS_SITE=True)
class TestFieldRestrictionViewB2b(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()

        self.user = self.client.login_yandex()
        self.o2g = OrganizationToGroupFactory()
        self.org = self.o2g.org

    def test_blank_not_in_business(self):
        response = self.client.get('/admin/api/v2/fields-restrictions/')
        self.assertEqual(200, response.status_code)

        self.assertEqual(response.data, [])
