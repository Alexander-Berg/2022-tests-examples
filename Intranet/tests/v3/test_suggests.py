from django.conf import settings
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm

from events.accounts.helpers import YandexClient
from events.accounts.factories import UserFactory, OrganizationToGroupFactory
from events.surveyme.factories import SurveyFactory
from events.surveyme_integration.factories import SurveyHookFactory, ServiceSurveyHookSubscriptionFactory
from events.v3.types import LayerType, SubscriptionType


class TestSurveySuggest(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_suggest_survey(self):
        survey = SurveyFactory(name='This is test')
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)
        params = {
            'layer': LayerType.survey,
            'text': 'test',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {'layer': LayerType.survey, 'id': survey.pk, 'name': survey.name},
        ])

        params = {
            'layer': LayerType.survey,
            'text': str(survey.pk),
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {'layer': LayerType.survey, 'id': survey.pk, 'name': survey.name},
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_suggest_survey_b2b(self):
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(self.user)
        survey = SurveyFactory(name='This is test', org=o2g.org)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)
        params = {
            'layer': LayerType.survey,
            'text': 'test',
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {'layer': LayerType.survey, 'id': survey.pk, 'name': survey.name},
        ])

        params = {
            'layer': LayerType.survey,
            'text': str(survey.pk),
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {'layer': LayerType.survey, 'id': survey.pk, 'name': survey.name},
        ])

    def test_shouldnt_suggest_survey(self):
        params = {
            'layer': LayerType.survey,
            'text': 'not exists',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.survey,
            'text': '999',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.survey,
            'text': '',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.survey,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])


class TestHookSuggest(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_suggest_survey_hook(self):
        hook = SurveyHookFactory(name='This is test')
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, hook.survey)
        params = {
            'layer': LayerType.hook,
            'text': 'test',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.hook, 'id': hook.pk, 'name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.hook,
            'text': str(hook.pk),
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.hook, 'id': hook.pk, 'name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.hook,
            'survey_id': hook.survey.pk,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.hook, 'id': hook.pk, 'name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_suggest_survey_hook_b2b(self):
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(self.user)
        survey = SurveyFactory(org=o2g.org)
        hook = SurveyHookFactory(name='This is test', survey=survey)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, hook.survey)
        params = {
            'layer': LayerType.hook,
            'text': 'test',
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.hook, 'id': hook.pk, 'name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.hook,
            'text': str(hook.pk),
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.hook, 'id': hook.pk, 'name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.hook,
            'survey_id': hook.survey.pk,
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.hook, 'id': hook.pk, 'name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

    def test_shouldnt_suggest_survey_hook(self):
        params = {
            'layer': LayerType.hook,
            'text': 'not exists',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.hook,
            'text': '999',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.hook,
            'survey_id': 999,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.hook,
            'text': '',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.hook,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])


class TestSubscriptionSuggest(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_suggest_subscription(self):
        hook = SurveyHookFactory(name='This is test')
        subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=hook)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, hook.survey)
        params = {
            'layer': LayerType.subscription,
            'text': str(subscription.pk),
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.subscription, 'id': subscription.pk, 'type': SubscriptionType.email,
                'hook_id': hook.pk, 'hook_name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.subscription,
            'survey_id': hook.survey.pk,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.subscription, 'id': subscription.pk, 'type': SubscriptionType.email,
                'hook_id': hook.pk, 'hook_name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.subscription,
            'hook_id': hook.pk,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.subscription, 'id': subscription.pk, 'type': SubscriptionType.email,
                'hook_id': hook.pk, 'hook_name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_suggest_subscription_b2b(self):
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(self.user)
        survey = SurveyFactory(org=o2g.org)
        hook = SurveyHookFactory(name='This is test', survey=survey)
        subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=hook, service_type_action_id=7)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, hook.survey)

        params = {
            'layer': LayerType.subscription,
            'text': str(subscription.pk),
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.subscription, 'id': subscription.pk, 'type': SubscriptionType.tracker,
                'hook_id': hook.pk, 'hook_name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.subscription,
            'survey_id': hook.survey.pk,
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.subscription, 'id': subscription.pk, 'type': SubscriptionType.tracker,
                'hook_id': hook.pk, 'hook_name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

        params = {
            'layer': LayerType.subscription,
            'hook_id': hook.pk,
        }
        response = self.client.get('/v3/suggests/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [
            {
                'layer': LayerType.subscription, 'id': subscription.pk, 'type': SubscriptionType.tracker,
                'hook_id': hook.pk, 'hook_name': hook.name,
                'survey_id': hook.survey.pk, 'survey_name': hook.survey.name,
            },
        ])

    def test_shouldnt_suggest_subscription(self):
        params = {
            'layer': LayerType.subscription,
            'text': '999',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.subscription,
            'survey_id': 999,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.subscription,
            'hook_id': 999,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.subscription,
            'text': '',
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

        params = {
            'layer': LayerType.subscription,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

    def test_shouldnt_suggest_subscription_if_not_exists(self):
        hook = SurveyHookFactory(name='This is test')
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, hook.survey)
        params = {
            'layer': LayerType.subscription,
            'text': '',
            'survey_id': hook.survey.pk,
        }
        response = self.client.get('/v3/suggests/', params)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])
