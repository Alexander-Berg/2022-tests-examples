# -*- coding: utf-8 -*-
import os

from django.conf import settings
from django.test import TestCase, override_settings
from django.contrib.contenttypes.models import ContentType
from json import loads as json_loads

from events.accounts.helpers import YandexClient
from events.conditions.factories import ContentTypeAttributeFactory
from events.history.actions import get_action_info
from events.history.models import HistoryRawEntry
from events.history.factories import HistoryRawEntryFactory
from events.surveyme.models import (
    AnswerType,
    Survey,
    SurveyGroup,
    SurveyQuestion,
    SurveyText,
)
from events.surveyme.factories import (
    SurveyFactory,
    SurveyGroupFactory,
    SurveyQuestionFactory,
    SurveyTemplateFactory,
)
from events.surveyme_integration.factories import HookSubscriptionNotificationFactory
from events.arc_compat import read_asset


class TestCreateHistoryEntry_for_survey(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def get_history_qs(self, model, pk):
        return (
            HistoryRawEntry.objects.all()
            .filter(
                content_type=ContentType.objects.get_for_model(model),
                object_id=pk,
            )
        )

    def get_from_fixture(self, fixture_name):
        file_path = os.path.join(settings.FIXTURES_DIR, 'history', fixture_name)
        buf = read_asset(file_path)
        return json_loads(buf)

    def test_should_create_history_entry_on_create(self):
        data = {
            'name': 'test survey',
        }
        response = self.client.post('/admin/api/v2/surveys/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

        survey_id = response.data['id']
        history_qs = self.get_history_qs(Survey, survey_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_copy(self):
        survey = SurveyFactory()
        response = self.client.post('/admin/api/v2/surveys/%s/copy/' % survey.pk)
        self.assertEqual(response.status_code, 200)

        survey_id = response.data['survey_id']
        history_qs = self.get_history_qs(Survey, survey_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_import_from_file(self):
        data = self.get_from_fixture('test_should_create_history_entry_on_import_from_file.json')
        response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        survey_id = response.data['result']['survey']['id']
        history_qs = self.get_history_qs(Survey, survey_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_import_from_template_as_slug(self):
        template_data = self.get_from_fixture('test_should_create_history_entry_on_import_from_template.json')
        SurveyTemplateFactory(slug='simple', data=template_data)
        data = {
            'import_from_template': 'simple',
        }
        response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        survey_id = response.data['result']['survey']['id']
        history_qs = self.get_history_qs(Survey, survey_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_import_from_template_as_pk(self):
        template_data = self.get_from_fixture('test_should_create_history_entry_on_import_from_template.json')
        template = SurveyTemplateFactory(slug='simple', data=template_data)
        data = {
            'import_from_template': str(template.pk),
        }
        response = self.client.post('/admin/api/v2/surveys/import/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        survey_id = response.data['result']['survey']['id']
        history_qs = self.get_history_qs(Survey, survey_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_change(self):
        survey = SurveyFactory()
        data = {
            'is_published_external': True,
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        survey_id = response.data['id']
        history_qs = self.get_history_qs(Survey, survey_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_delete(self):
        survey = SurveyFactory()
        response = self.client.delete('/admin/api/v2/surveys/%s/' % survey.pk)
        self.assertEqual(response.status_code, 204)

        history_qs = self.get_history_qs(Survey, survey.pk)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_restore(self):
        survey = SurveyFactory(is_deleted=True)
        response = self.client.post('/admin/api/v2/surveys/%s/restore/' % survey.pk)
        self.assertEqual(response.status_code, 200)

        history_qs = self.get_history_qs(Survey, survey.pk)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_change_access(self):
        survey = SurveyFactory(user=self.profile)
        data = {
            'type': 'common',
            'users': [],
        }
        response = self.client.post('/admin/api/v2/surveys/%s/access/' % survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        history_qs = self.get_history_qs(Survey, survey.pk)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_submit_logic(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(survey=survey)
        cta = ContentTypeAttributeFactory(
            title='test',
            content_type=ContentType.objects.get_for_model(SurveyQuestion),
        )
        data = {
            'survey_id': survey.pk,
            'nodes': [{
                'survey_id': survey.pk,
                'items': [{
                    'condition': 'eq',
                    'operator': 'and',
                    'position': 1,
                    'survey_question': question.pk,
                    'value': 'test value',
                    'content_type_attribute': cta.pk,
                }],
            }],
        }
        response = self.client.post('/admin/api/v2/survey-submit-condition-nodes/save-with-items/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        history_qs = self.get_history_qs(Survey, survey.pk)
        self.assertEqual(history_qs.count(), 1)


class TestCreateHistoryEntry_for_survey_group(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def get_history_qs(self, model, pk):
        return (
            HistoryRawEntry.objects.all()
            .filter(
                content_type=ContentType.objects.get_for_model(model),
                object_id=pk,
            )
        )

    def test_should_create_history_entry_on_create(self):
        data = {
            'name': 'test group',
        }
        response = self.client.post('/admin/api/v2/survey-groups/', data, format='json')
        self.assertEqual(response.status_code, 201)

        group_id = response.data['id']
        history_qs = self.get_history_qs(SurveyGroup, group_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_change(self):
        group = SurveyGroupFactory()
        data = {
            'name': 'test group',
        }
        response = self.client.patch('/admin/api/v2/survey-groups/%s/' % group.pk, data, format='json')
        self.assertEqual(response.status_code, 200)

        group_id = response.data['id']
        history_qs = self.get_history_qs(SurveyGroup, group_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_delete(self):
        group = SurveyGroupFactory()
        response = self.client.delete('/admin/api/v2/survey-groups/%s/' % group.pk)
        self.assertEqual(response.status_code, 204)

        history_qs = self.get_history_qs(SurveyGroup, group.pk)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_change_access(self):
        group = SurveyGroupFactory(user=self.profile)
        data = {
            'type': 'common',
            'users': [],
        }
        response = self.client.post('/admin/api/v2/survey-groups/%s/access/' % group.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        history_qs = self.get_history_qs(SurveyGroup, group.pk)
        self.assertEqual(history_qs.count(), 1)


class TestCreateHistoryEntry_for_survey_question(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    def get_history_qs(self, model, pk):
        return (
            HistoryRawEntry.objects.all()
            .filter(
                content_type=ContentType.objects.get_for_model(model),
                object_id=pk,
            )
        )

    def test_should_create_history_entry_on_create(self):
        survey = SurveyFactory()
        answer_type = AnswerType.objects.get(slug='answer_short_text')
        data = {
            'survey_id': survey.pk,
            'label': 'test question',
            'answer_type_id': answer_type.pk,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data, format='json')
        self.assertEqual(response.status_code, 201)

        question_id = response.data['id']
        history_qs = self.get_history_qs(SurveyQuestion, question_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_change(self):
        question = SurveyQuestionFactory()
        data = {
            'label': 'test question',
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data, format='json')
        self.assertEqual(response.status_code, 200)

        question_id = response.data['id']
        history_qs = self.get_history_qs(SurveyQuestion, question_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_delete(self):
        question = SurveyQuestionFactory()
        response = self.client.delete('/admin/api/v2/survey-questions/%s/' % question.pk)
        self.assertEqual(response.status_code, 204)

        history_qs = self.get_history_qs(SurveyQuestion, question.pk)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_restore(self):
        question = SurveyQuestionFactory(is_deleted=True)
        response = self.client.post('/admin/api/v2/survey-questions/%s/restore/' % question.pk)
        self.assertEqual(response.status_code, 200)

        history_qs = self.get_history_qs(SurveyQuestion, question.pk)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_copy(self):
        question = SurveyQuestionFactory()
        response = self.client.post('/admin/api/v2/survey-questions/%s/copy/' % question.pk)
        self.assertEqual(response.status_code, 201)

        question_id = response.data['id']
        history_qs = self.get_history_qs(SurveyQuestion, question_id)
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_entry_on_view_logic(self):
        survey = SurveyFactory()
        question = SurveyQuestionFactory(survey=survey)
        question_with_logic = SurveyQuestionFactory(survey=survey)
        cta = ContentTypeAttributeFactory(
            title='test',
            content_type=ContentType.objects.get_for_model(SurveyQuestion),
        )
        data = {
            'survey_question': question_with_logic.pk,
            'nodes': [{
                'survey_question': question_with_logic.pk,
                'items': [{
                    'condition': 'eq',
                    'operator': 'and',
                    'position': 1,
                    'survey_question': question.pk,
                    'value': 'test value',
                    'content_type_attribute': cta.pk,
                }],
            }],
        }
        response = self.client.post('/admin/api/v2/survey-question-show-condition-nodes/save-with-items/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        history_qs = self.get_history_qs(SurveyQuestion, question_with_logic.pk)
        self.assertEqual(history_qs.count(), 1)


class TestCreateHistoryEntry_for_survey_text(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    def get_history_qs(self, model, pk):
        return (
            HistoryRawEntry.objects.all()
            .filter(
                content_type=ContentType.objects.get_for_model(model),
                object_id=pk,
            )
        )

    def test_should_create_history_entry_on_change(self):
        survey = SurveyFactory()
        text = survey.texts.get(slug='submit_button')
        data = {
            'value': 'send',
        }
        response = self.client.patch('/admin/api/v2/survey-texts/%s/' % text.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        text_id = response.data['id']
        history_qs = self.get_history_qs(SurveyText, text_id)
        self.assertEqual(history_qs.count(), 1)


class TestGetHistoryEntries(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(survey=self.survey)
        self.text = self.survey.texts.get(slug='submit_button')

    def test_should_return_created_history_entries_for_intranet(self):
        content_types = ContentType.objects.get_for_models(Survey, SurveyQuestion, SurveyText)
        HistoryRawEntryFactory(
            endpoint='admin_api_v2:survey-list',
            content_type=content_types[Survey],
            object_id=self.survey.pk,
            user=self.profile,
        )
        HistoryRawEntryFactory(
            endpoint='admin_api_v2:survey-question-list',
            content_type=content_types[SurveyQuestion],
            object_id=self.question.pk,
            user=self.profile,
        )
        HistoryRawEntryFactory(
            endpoint='admin_api_v2:survey-text-detail',
            content_type=content_types[SurveyText],
            object_id=self.text.pk,
            user=self.profile,
        )
        response = self.client.get('/admin/api/v2/history-entries/?object_id=%s' % self.survey.pk)
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 3)
        results = response.data['results']

        self.assertEqual(results[0]['content_type'], content_types[SurveyText].pk)
        self.assertEqual(results[0]['object_id'], self.text.pk)
        self.assertIsNotNone(results[0]['info']['template'])

        self.assertEqual(results[1]['content_type'], content_types[SurveyQuestion].pk)
        self.assertEqual(results[1]['object_id'], self.question.pk)
        self.assertIsNotNone(results[1]['info']['template'])

        self.assertEqual(results[2]['content_type'], content_types[Survey].pk)
        self.assertEqual(results[2]['object_id'], self.survey.pk)
        self.assertIsNotNone(results[2]['info']['template'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_created_history_entries_for_biz(self):
        content_types = ContentType.objects.get_for_models(Survey, SurveyQuestion, SurveyText)
        HistoryRawEntryFactory(
            endpoint='admin_api_v2:survey-list',
            content_type=content_types[Survey],
            object_id=self.survey.pk,
            user=self.profile,
        )
        HistoryRawEntryFactory(
            endpoint='admin_api_v2:survey-question-list',
            content_type=content_types[SurveyQuestion],
            object_id=self.question.pk,
            user=self.profile,
        )
        HistoryRawEntryFactory(
            endpoint='admin_api_v2:survey-text-detail',
            content_type=content_types[SurveyText],
            object_id=self.text.pk,
            user=self.profile,
        )
        response = self.client.get('/admin/api/v2/history-entries/?object_id=%s' % self.survey.pk)
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 3)
        results = response.data['results']

        self.assertEqual(results[0]['content_type'], content_types[SurveyText].pk)
        self.assertEqual(results[0]['object_id'], self.text.pk)
        self.assertIsNotNone(results[0]['info']['template'])

        self.assertEqual(results[1]['content_type'], content_types[SurveyQuestion].pk)
        self.assertEqual(results[1]['object_id'], self.question.pk)
        self.assertIsNotNone(results[1]['info']['template'])

        self.assertEqual(results[2]['content_type'], content_types[Survey].pk)
        self.assertEqual(results[2]['object_id'], self.survey.pk)
        self.assertIsNotNone(results[2]['info']['template'])


class TestActionInfo(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(survey=self.survey)
        self.text = self.survey.texts.get(slug='submit_button')

    def create_entry(self, obj, endpoint, method='POST', path=None):
        return HistoryRawEntryFactory(
            endpoint=endpoint,
            method=method,
            path=path,
            content_type=ContentType.objects.get_for_model(obj._meta.model),
            object_id=obj.pk,
            user_id=settings.ROBOT_USER_ID,
        )

    def assertString(self, value):
        self.assertTrue(isinstance(value, str))

    def assertValuableString(self, value):
        self.assertString(value)
        self.assertTrue(len(value.strip()) > 0)

    def test_action_survey_list(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_ban(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-ban')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_copy(self):
        survey = SurveyFactory()
        entry = self.create_entry(
            self.survey,
            'admin_api_v2:survey-copy',
            path='/admin/api/v2/surveys/%s/copy/' % survey.pk,
        )
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_update(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-detail', method='PATCH')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_delete(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-detail', method='DELETE')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_restore(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-restore', method='POST')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_import(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-import-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_import_tracker(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-import-tracker-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_question_copy(self):
        question = SurveyQuestionFactory(survey=self.survey)
        entry = self.create_entry(
            self.question,
            'admin_api_v2:survey-question-copy',
            path='/admin/api/v2/survey-questions/%s/copy/' % question.pk,
        )
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_question_update_patch(self):
        entry = self.create_entry(self.question, 'admin_api_v2:survey-question-detail', method='PATCH')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_question_update_delete(self):
        entry = self.create_entry(self.question, 'admin_api_v2:survey-question-detail', method='DELETE')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_question_create(self):
        entry = self.create_entry(self.question, 'admin_api_v2:survey-question-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_question_restore(self):
        entry = self.create_entry(self.question, 'admin_api_v2:survey-question-restore')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_quiz(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-quiz')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_access(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-access')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_question_logic(self):
        entry = self.create_entry(self.question, 'admin_api_v2:survey-question-show-condition-node-save-with-items-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_logic(self):
        entry = self.create_entry(self.survey, 'admin_api_v2:survey-submit-condition-node-save-with-items-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_survey_text_update(self):
        entry = self.create_entry(self.text, 'admin_api_v2:survey-text-detail')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_notification_list_restart(self):
        notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            user_id=settings.ROBOT_USER_ID,
        )
        entry = self.create_entry(notification, 'admin_api_v2:hook-subscription-notification-restart-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_notification_list_cancel(self):
        notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            user_id=settings.ROBOT_USER_ID,
        )
        entry = self.create_entry(notification, 'admin_api_v2:hook-subscription-notification-cancel-list')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_notification_restart(self):
        notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            user_id=settings.ROBOT_USER_ID,
        )
        entry = self.create_entry(notification, 'admin_api_v2:hook-subscription-notification-restart')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)

    def test_action_notification_cancel(self):
        notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            user_id=settings.ROBOT_USER_ID,
        )
        entry = self.create_entry(notification, 'admin_api_v2:hook-subscription-notification-cancel')
        action_info = str(get_action_info(entry))
        self.assertValuableString(action_info)
        self.assertFalse(entry.endpoint in action_info)
