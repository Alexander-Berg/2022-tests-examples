# -*- coding: utf-8 -*-
import json
import responses

from django.contrib.contenttypes.models import ContentType
from django.db.models import FieldDoesNotExist
from django.db.models.fields.related import ManyToManyField
from django.test import TestCase
from django.test.utils import override_settings
from django.utils.encoding import force_str
from rest_framework import status
from unittest.mock import patch

from events.accounts.models import User
from events.accounts.factories import OrganizationToGroupFactory
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.helpers import JsonFixtureMixin
from events.surveyme.models import (
    Survey,
    SurveyQuestion,
    SurveyText,
    SurveySubmitConditionNode,
    SurveySubmitConditionNodeItem,
    SurveyQuestionShowConditionNode,
    SurveyQuestionChoice,
    SurveyTemplate,
)
from events.surveyme.factories import (
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyGroupFactory,
    SurveyTemplateFactory,
)
from events.accounts.helpers import YandexClient
from events.surveyme_integration.models import StartrekSubscriptionData


def dumps(instance):
    return json.dumps(instance, indent=2, ensure_ascii=False, sort_keys=True)


def remove_keys(instance, removing_keys):
    if isinstance(instance, list):
        for i, _ in enumerate(instance):
            remove_keys(instance[i], removing_keys)
    elif isinstance(instance, dict):
        for k in list(instance.keys()):
            if k in removing_keys:
                instance.pop(k)
            else:
                remove_keys(instance[k], removing_keys)
    return instance


class CopyAssertionsMixin(object):
    def assertSurveyCopied(self, survey, survey_data, is_published_external=False):
        survey_data['is_published_external'] = is_published_external
        self.assertInstanceCopied(survey, survey_data)
        self.assertEqual(self.profile, survey.user)

    def assertInstanceCopied(self, instance, data):
        field_names = ['id', 'slug'] if isinstance(instance, Survey) else ['id']
        for field, value in data.items():
            if isinstance(value, str):
                value = force_str(value)
            if field in field_names:
                new_value = getattr(instance, field, None)
                if value is not None:
                    self.assertNotEqual(new_value, value)
                else:
                    self.assertIsNone(new_value)
                continue
            msg = '{3} был скопирован неправильно. Поле {0} ({1} != {2})'.format(
                force_str(field),
                force_str(getattr(instance, field, None)),
                force_str(value),
                type(instance),
            ).encode('utf-8')
            instance_value = getattr(instance, field, None)
            try:
                model_field = instance._meta.get_field(field)
            except FieldDoesNotExist:
                model_field = None
            if model_field and isinstance(model_field, ManyToManyField):
                self.assertEqual(set(instance_value.values_list('pk', flat=True)), set(value), msg=msg)
            else:
                fields_to_get_pk = (
                    'content_type_attribute',
                    'survey_question',
                    'survey_question_choice',
                    'survey_question_show_condition_node',
                )
                if field in fields_to_get_pk and instance_value:
                    instance_value = instance_value.pk
                self.assertEqual(instance_value, value, msg=msg)

    def assertQuestionCopied(self, question, question_data, survey_data):
        self.assertEqual(question.survey_id, survey_data['id'])
        question_data['survey_id'] = survey_data['id']
        if 'answer_type' in question_data:
            del question_data['answer_type']
        for i, choice in enumerate(question_data['choices']):
            if 'id' in choice:
                del choice['id']
            if 'slug' in choice:
                del choice['slug']
            choice['survey_question_id'] = question.pk
            self.assertInstanceCopied(question.surveyquestionchoice_set.all()[i], choice)
        del question_data['choices']
        self.assertInstanceCopied(question, question_data)


class TestSurveyViewSet__import_from_json_behaviour(TestCase, JsonFixtureMixin, CopyAssertionsMixin):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.post_url = '/admin/api/v2/surveys/import/'
        self.base_survey_data = {
            'name': 'Test survey',
            'slug': 'test-survey',
            'need_auth_to_answer': True,
            'is_published_external': True,
            'is_allow_answer_editing': True,
            'metrika_counter_code': '123',
            'captcha_display_mode': 'auto',
        }
        self.base_question_data = {
            'answer_type_id': 1,
            'choices': [],
            'id': 13797,
            'label': 'Тестовый вопрос',
            'param_action_id': None,
            'param_data_source': 'survey_question_choice',
            'param_data_source_params': None,
            'param_help_text': '',
            'param_hint_data_source': None,
            'param_hint_data_source_params': None,
            'param_hint_type_id': None,
            'param_is_allow_multiple_choice': False,
            'param_is_allow_other': False,
            'param_is_hidden': False,
            'param_is_random_choices_position': False,
            'param_modify_choices': '',
            'param_is_required': True,
            'param_is_section_header': False,
            'param_max': 10,
            'param_max_file_size': 10,
            'param_max_files_count': 2,
            'param_min': 10,
            'param_price': None,
            'param_slug': 'answer_short_text_13797',
            'param_variables': None,
            'param_widget': 'list',
            'position': 1,
            'survey_id': 1667,
        }
        self.base_choices = [
            {
                'id': 3717,
                'survey_question_id': 13797,
                'position': 3717,
                'slug': '3717',
                'label': 'Не было необходимости',
            },
            {
                'id': 3718,
                'survey_question_id': 13797,
                'position': 3718,
                'slug': '3718',
                'label': 'Не было возможности',
            }
        ]

    def test_should_return_400_if_wrong_survey_data_provided(self):
        experiments = [
            {},
            {'survey': {}},
            {'survey': None},
        ]
        for data in experiments:
            response = self.client.post(self.post_url, data=data, format='json')
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_should_create_copy_of_survey_from_json(self):
        Survey.objects.all().delete()
        self.assertEqual(Survey.objects.count(), 0)
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': []}
        response = self.client.post(self.post_url, data=json.dumps(data), content_type='application/json')
        self.assertEqual(Survey.objects.count(), 1)
        msg = '%s' % response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=msg)
        self.assertSurveyCopied(Survey.objects.get(pk=response.data['result']['survey']['id']), self.base_survey_data)

    def test_should_create_copy_of_survey_from_template(self):
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': []}
        template = SurveyTemplate.objects.create(
            name='name',
            position=1,
            slug='test',
            data=data,
        )
        data = {'import_from_template': template.id}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEquals(response.status_code, 200)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertSurveyCopied(survey, self.base_survey_data)
        self.assertEqual(survey.users_count, 0)
        self.assertEqual(survey.groups_count, 0)

    def test_should_create_copy_of_survey_from_template_string_id(self):
        self.assertEquals(Survey.objects.count(), 0)
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': []}
        template = SurveyTemplateFactory(data=data)

        data = {'import_from_template': str(template.id)}
        response = self.client.post(self.post_url, data=data, format='json')
        msg = '%s' % response.data
        self.assertEquals(response.status_code, status.HTTP_200_OK, msg=msg)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertSurveyCopied(survey, self.base_survey_data)
        self.assertEqual(survey.users_count, 0)
        self.assertEqual(survey.groups_count, 0)

    def test_should_create_copy_of_survey_from_template_slug(self):
        self.assertEquals(Survey.objects.count(), 0)
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': []}
        template = SurveyTemplateFactory(data=data)

        data = {'import_from_template': template.slug}
        response = self.client.post(self.post_url, data=data, format='json')
        msg = '%s' % response.data
        self.assertEquals(response.status_code, status.HTTP_200_OK, msg=msg)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertSurveyCopied(survey, self.base_survey_data)
        self.assertEqual(survey.users_count, 0)
        self.assertEqual(survey.groups_count, 0)

    def test_should_create_copy_linked_to_org_if_passed(self):
        self.assertEquals(Survey.objects.count(), 0)
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': []}
        template = SurveyTemplate.objects.create(
            name='name',
            position=1,
            slug='test',
            data=data,
        )
        data = {'import_from_template': template.id, 'org_id': '123'}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEquals(Survey.objects.count(), 1)
        survey = Survey.objects.first()
        self.assertEqual(survey.org.dir_id, '123')
        self.assertEqual(survey.users_count, 0)
        self.assertEqual(survey.groups_count, 0)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_copy_of_survey_with_org_from_request(self):
        ContentTypeAttributeFactory(pk=4, lookup_field='answer_boolean')
        data = json.loads(self.get_json_from_file('test_creating_full_survey.json'))
        data['org_id'] = '222'

        with patch.object(User, 'get_karma', return_value=0):
            response = self.client.post(self.post_url, data=data, format='json', HTTP_X_ORGS=str(123))
        self.assertEquals(response.status_code, status.HTTP_200_OK)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertEqual(survey.org.dir_id, '222')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_copy_of_survey_without_org_from_request(self):
        ContentTypeAttributeFactory(pk=4, lookup_field='answer_boolean')
        data = json.loads(self.get_json_from_file('test_creating_full_survey.json'))
        data['org_id'] = None

        with patch.object(User, 'get_karma', return_value=0):
            response = self.client.post(self.post_url, data=data, format='json', HTTP_X_ORGS=str(123))
        self.assertEquals(response.status_code, status.HTTP_200_OK)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertIsNone(survey.org_id)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_copy_of_survey_with_org_from_json(self):
        ContentTypeAttributeFactory(pk=4, lookup_field='answer_boolean')
        data = json.loads(self.get_json_from_file('test_creating_full_survey.json'))

        with patch.object(User, 'get_karma', return_value=0):
            response = self.client.post(self.post_url, data=data, format='json', HTTP_X_ORGS='123')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertEqual(self.profile, survey.user)
        self.assertTrue(self.profile.has_perm('surveyme.change_survey', survey))
        self.assertIsNone(survey.org)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_copy_of_survey_from_template_biz(self):
        ContentTypeAttributeFactory(pk=4, lookup_field='answer_boolean')
        data = json.loads(self.get_json_from_file('test_creating_full_survey.json'))
        template = SurveyTemplate.objects.create(
            name='name',
            position=1,
            slug='test',
            data=data,
        )
        data = {'import_from_template': template.id, 'org_id': '123'}

        with patch.object(User, 'get_karma', return_value=0):
            response = self.client.post(self.post_url, data=data, format='json', HTTP_X_ORGS='321,123')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertEqual(self.profile, survey.user)
        self.assertTrue(self.profile.has_perm('surveyme.change_survey', survey))
        self.assertEqual(survey.org.dir_id, '123')

    def test_should_success_on_current_personal(self):
        Survey.objects.all().delete()
        self.assertEqual(Survey.objects.count(), 0)
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': []}
        template = SurveyTemplate.objects.create(
            name='name',
            slug='test',
            position=1,
            data=data,
            is_personal=True,
            user=self.profile,
        )
        data = {'import_from_template': template.id}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(Survey.objects.count(), 1)
        msg = '%s' % response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=msg)
        self.assertSurveyCopied(Survey.objects.get(pk=response.data['result']['survey']['id']), self.base_survey_data)

    def test_should_fail_on_bad_data(self):
        data = {'test': 'some'}
        template = SurveyTemplate.objects.create(
            name='name',
            slug='test',
            position=1,
            data=data,
            is_personal=True,
        )
        data = {'import_from_template': template.id}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEquals(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.content, b'{"errors":["Invalid data format"]}')

    def test_should_fail_on_non_existing(self):
        data = {'import_from_template': 10}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.content, b'"No template with such pk exists"')

    def test_should_create_copy_of_survey_from_json_unpublished(self):
        Survey.objects.all().delete()
        self.assertEqual(Survey.objects.count(), 0)
        self.base_survey_data['is_published_external'] = True
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': []}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(Survey.objects.count(), 1)
        msg = '%s' % response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=msg)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertSurveyCopied(survey, self.base_survey_data)
        self.assertFalse(survey.is_published_external)

    def test_should_copy_questions(self):
        questions = [self.base_question_data]
        data = {
            'survey': self.base_survey_data,
            'questions': questions,
        }
        SurveyQuestion.objects.all().delete()
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEquals(response.status_code, status.HTTP_200_OK)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        questions_list = SurveyQuestion.objects.filter(survey=survey)
        self.assertEqual(questions_list.count(), 1)
        self.assertQuestionCopied(
            SurveyQuestion.objects.get(pk=survey.surveyquestion_set.all()[0].pk),
            questions[0],
            response.data['result']['survey'],
        )

    def test_should_copy_questions_with_answer_type(self):
        self.base_question_data['answer_type'] = {'id': 1, 'slug': 'answer_short_text'},
        questions = [self.base_question_data]
        data = {
            'survey': self.base_survey_data,
            'questions': questions,
        }
        SurveyQuestion.objects.all().delete()
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEquals(response.status_code, status.HTTP_200_OK)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        questions_list = SurveyQuestion.objects.filter(survey=survey)
        self.assertEqual(questions_list.count(), 1)
        self.assertQuestionCopied(
            SurveyQuestion.objects.get(pk=survey.surveyquestion_set.all()[0].pk),
            questions[0],
            response.data['result']['survey'],
        )

    def test_should_copy_questions_with_choices(self):
        SurveyQuestion.objects.all().delete()
        survey_question = SurveyQuestionFactory()
        survey_question.save()
        choice = SurveyQuestionChoiceFactory(survey_question=survey_question)
        choice.slug = '3717'
        choice.save()
        self.base_question_data['choices'] = self.base_choices
        questions = [self.base_question_data]
        data = {
            'survey': self.base_survey_data,
            'questions': questions,
        }
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEquals(response.status_code, status.HTTP_200_OK)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        questions_list = SurveyQuestion.objects.filter(survey=survey)
        self.assertEqual(questions_list.count(), 1)
        self.assertQuestionCopied(
            SurveyQuestion.objects.get(pk=survey.surveyquestion_set.all()[0].pk),
            questions[0],
            response.data['result']['survey'],
        )

    def test_should_copy_survey_with_texts(self):
        survey_text_data = {
            'id': 28728,
            'max_length': 30,
            'null': False,
            'slug': 'invitation_to_change',
            'survey': 520,
            'value': 'Изменить ответ на опрос',
        }
        data = {'survey': self.base_survey_data, 'questions': [], 'texts': [survey_text_data]}
        SurveyText.objects.all().delete()
        self.assertEqual(SurveyText.objects.count(), 0)
        response = self.client.post(self.post_url, data=data, format='json')
        msg = '%s' % response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=msg)
        survey_text_data['survey'] = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertInstanceCopied(SurveyText.objects.get(slug='invitation_to_change'), survey_text_data)

    def test_should_copy_with_survey_submit_conditions(self):
        cta = ContentTypeAttributeFactory(content_type=ContentType.objects.get_for_model(SurveyQuestion))
        node_condition = {
            'id': 171,
            'survey_id': 1778,
            'items': [
                {
                    'id': 287,
                    'operator': 'and',
                    'content_type_attribute': cta.pk,
                    'condition': 'neq',
                    'value': 'False',
                    'position': 1,
                    'survey_submit_condition_node': 171,
                    'survey_question': 13797,
                    'survey_question_choice': None
                }
            ]
        }
        data = {'survey': self.base_survey_data, 'questions': [self.base_question_data], 'texts': [], 'nodes': [node_condition]}
        SurveyText.objects.all().delete()
        SurveySubmitConditionNodeItem.objects.all().delete()
        SurveySubmitConditionNode.objects.all().delete()
        self.assertEqual(SurveyText.objects.count(), 0)

        response = self.client.post(self.post_url, data=data, format='json')
        msg = (' '.join(response.data)).encode('utf-8')
        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=msg)

        self.assertEqual(SurveySubmitConditionNode.objects.count(), 1)
        node = SurveySubmitConditionNode.objects.all()[0]
        msg = 'Условия показа кнопки сабмита должны привязаываться к импортируемой форме'
        self.assertEqual(node.survey_id, response.data['result']['survey']['id'], msg=msg)

        new_question = SurveyQuestion.objects.all()[0]
        node_condition['items'][0]['survey_question'] = new_question.pk
        self.assertEqual(SurveySubmitConditionNodeItem.objects.count(), 1)
        node_item = SurveySubmitConditionNodeItem.objects.all()[0]
        self.assertEqual(node_item.content_type_attribute.pk, node_condition['items'][0]['content_type_attribute'])
        del node_condition['items'][0]['content_type_attribute']
        self.assertEqual(node.pk, node_item.survey_submit_condition_node.pk)
        del node_condition['items'][0]['survey_submit_condition_node']
        self.assertInstanceCopied(node_item, node_condition['items'][0])

    def get_survey_data(self, service_type_action_id=3):  # email
        cta = ContentTypeAttributeFactory(content_type=ContentType.objects.get_for_model(SurveyQuestion))
        hooks = [{
            'is_active': True,
            'condition_nodes': [
                {
                    'id': 176,
                    'items': [
                        {
                            'id': 1,
                            'condition': 'eq',
                            'content_type_attribute': cta.pk,
                            'operator': 'and',
                            'position': 1,
                            'survey_question': None,
                            'value': '3717',
                        },
                        {
                            'id': 2,
                            'condition': 'neq',
                            'content_type_attribute': cta.pk,
                            'operator': 'or',
                            'position': 2,
                            'survey_question': 13797,
                            'value': '3718'
                        }
                    ],
                },
            ],
            'subscriptions': [{
                'id': 1,
                'body': 'text',
                'context_language': 'ru',
                'email': '',
                'email_from_address': 'from-address@yandex-team.ru',
                'email_to_address': 'to-address@yandex-team.ru',
                'email_spam_check': True,
                'headers': [{
                    'add_only_with_value': True,
                    'name': 'one',
                    'value': 'two',
                }],
                'attachments': [],
                'http_format_name': 'json',
                'http_url': '',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': False,
                'json_rpc': {
                    'method': 'foo',
                    'params': [{
                        'name': 'param1',
                        'value': 'bar',
                        'add_only_with_value': True,
                    }],
                },
                'phone': '',
                'questions': [13797],
                'service_type_action': service_type_action_id,
                'startrek': {
                    'assignee': '',
                    'author': '',
                    'components': None,
                    'followers': ['akhmetov'],
                    'parent': '',
                    'priority': 4,
                    'project': None,
                    'queue': 'FORMS',
                    'subscription': 1298,
                    'tags': ['tag'],
                    'type': 2,
                    'fields': [{
                        'key': {
                            'slug': 'abcService',
                            'name': 'ABC Service',
                        },
                        'value': 'Forms',
                        'add_only_with_value': True,
                    }],
                },
                'title': 'my survey',
                'variables': {
                    '55ad0c6bbb2ffe23d6000000': {
                        '_id': '55ad0c6bbb2ffe23d6000000',
                        'arguments': {'questions': [13797, 13797]},
                        'filters': [],
                        'var': 'user.uid'
                    },
                    '55ad0c6ebb2ffe23d6000001': {
                        '_id': '55ad0c6ebb2ffe23d6000001',
                        'arguments': {'question': 13797},
                        'filters': [],
                        'var': 'form.question_answer'
                    }
                },
                'attachment_templates': [{
                    'id': 17,
                    'name': 'my_template',
                    'template': '{5a7ad8f389bbcb56c5000001}',
                    'type': 'pdf',
                    'slug': 'template_17',
                    'variables': {
                        '5a7ad8f389bbcb56c5000001': {
                            'integration_file_template_id': 17,
                            'var': 'form.question_answer',
                            '_id': '5a7ad8f389bbcb56c5000001',
                            'arguments': {
                                'show_filenames': False,
                                'question': 13797,
                            },
                            'filters': []
                        }
                    }
                }],
                'wiki': {'supertag': 'test/some', 'text': '', 'id': 1, 'subscription': 1298},
            }],
            'triggers': [1, 2],
        }]
        integration_file_templates = [{
            'id': 17,
            'name': 'my_template',
            'template': '{5a7ad8f389bbcb56c5000001}',
            'type': 'pdf',
            'slug': 'template_17',
            'variables': {
                '5a7ad8f389bbcb56c5000001': {
                    'integration_file_template_id': 17,
                    'var': 'form.question_answer',
                    '_id': '5a7ad8f389bbcb56c5000001',
                    'arguments': {
                        'show_filenames': False,
                        'question': 13797,
                    },
                    'filters': []
                }
            }
        }]
        self.base_survey_data['hooks'] = hooks
        self.base_survey_data['integration_file_templates'] = integration_file_templates

        return {'survey': self.base_survey_data, 'questions': [self.base_question_data], 'texts': []}

    @responses.activate
    def test_should_copy_with_integration_hooks__email(self):
        data = self.get_survey_data(3)  # email
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)

        survey_data = data['survey']
        survey_data.pop('is_published_external', None)
        survey_data.pop('slug', None)
        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.slug)
        self.assert_fields_equal(survey, survey_data)

        self.assertEqual(self.profile, survey.user)
        self.assertFalse(survey.texts.exists())

        question = survey.surveyquestion_set.first()
        self.assertIsNotNone(question)
        question_data = data['questions'][0]
        self.assert_fields_equal(question, question_data)

        integration_file_template = survey.integration_file_templates.first()
        self.assertIsNotNone(integration_file_template)
        self.assertNotEqual(integration_file_template.pk, 17)
        self.assertNotEqual(integration_file_template.template, '{5a7ad8f389bbcb56c5000001}')
        self.assertEqual(integration_file_template.name, 'my_template')
        self.assertEqual(integration_file_template.slug, 'template_17')
        variables = integration_file_template.surveyvariable_set.all()
        self.assertTrue(variables.count(), 1)
        self.assertEqual(variables.filter(variable_id='5a7ad8f389bbcb56c5000001').count(), 0)
        self.assertEqual('{%s}' % variables.first().variable_id, integration_file_template.template)
        self.assertEqual(variables.first().arguments['question'], question.pk)

        hooks_data = survey_data['hooks']
        self.assertEqual(len(hooks_data), survey.hooks.count())
        hook = survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assert_fields_equal(hook, hooks_data[0])
        self.assertEqual(list(hook.triggers.values_list('pk', flat=True)), hooks_data[0]['triggers'])

        condition_nodes_data = hooks_data[0]['condition_nodes']
        self.assertEqual(len(condition_nodes_data), hook.condition_nodes.count())
        condition_node = hook.condition_nodes.first()
        self.assertIsNotNone(condition_node)
        self.assertEqual(len(condition_nodes_data[0]['items']), condition_node.items.count())
        for i, condition_node_item in enumerate(condition_node.items.all()):
            condition_node_data = condition_nodes_data[0]['items'][i]
            if condition_node_item.survey_question:
                self.assertEqual(condition_node_item.survey_question, question)
            self.assert_fields_equal(condition_node_item, condition_node_data)

        subscriptions_data = hooks_data[0]['subscriptions']
        self.assertEqual(len(subscriptions_data), hook.subscriptions.count())
        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assert_fields_equal(subscription, subscriptions_data[0])

        self.assertEqual(len(subscriptions_data[0]['headers']), subscription.headers.count())
        for i, header in enumerate(subscription.headers.all()):
            header_data = subscriptions_data[0]['headers'][i]
            self.assert_fields_equal(header, header_data)

        self.assertEqual(len(subscriptions_data[0]['attachments']), subscription.attachments.count())
        for i, attachment in enumerate(subscription.attachments.all()):
            attachment_data = subscriptions_data[0]['attachments'][i]
            self.assert_fields_equal(attachment, attachment_data)

        variables_data = subscriptions_data[0]['variables']
        variables = subscription.surveyvariable_set.all()
        self.assertEqual(len(variables_data), variables.count())
        variables_data_by_type = {
            variable_data['var']: variable_data
            for variable_data in list(variables_data.values())
        }
        for variable in variables:
            variable_key = variable.variable_id
            self.assertNotIn(variable_key, list(variables_data.keys()))
            variable_data = variables_data_by_type.get(variable.var)
            self.assertIsNotNone(variable_data)
            self.assertNotEqual(variable.variable_id, variable_data['_id'])
            self.assertEqual(variable.filters, variable_data['filters'])
            self.assertEqual(len(variable_data['arguments']), len(variable.arguments))
            if 'question' in variable.arguments:
                self.assertIn('question', variable_data['arguments'])
                self.assertEqual(variable.arguments['question'], question.pk)
            if 'questions' in variable.arguments:
                self.assertIn('questions', variable.arguments)
                self.assertEqual(len(variable_data['arguments']['questions']), len(variable.arguments['questions']))
                for question_pk in variable.arguments['questions']:
                    self.assertEqual(question_pk, question.pk)

    @responses.activate
    def test_should_copy_with_integration_hooks__http(self):
        data = self.get_survey_data(4)  # http
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)

        survey_data = data['survey']
        survey_data.pop('is_published_external', None)
        survey_data.pop('slug', None)
        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.slug)
        self.assert_fields_equal(survey, survey_data)

        self.assertEqual(self.profile, survey.user)
        self.assertFalse(survey.texts.exists())

        question = survey.surveyquestion_set.first()
        self.assertIsNotNone(question)
        question_data = data['questions'][0]
        self.assert_fields_equal(question, question_data)

        integration_file_template = survey.integration_file_templates.first()
        self.assertIsNotNone(integration_file_template)
        self.assertNotEqual(integration_file_template.pk, 17)
        self.assertNotEqual(integration_file_template.template, '{5a7ad8f389bbcb56c5000001}')
        self.assertEqual(integration_file_template.name, 'my_template')
        self.assertEqual(integration_file_template.slug, 'template_17')
        variables = integration_file_template.surveyvariable_set.all()
        self.assertEqual(variables.count(), 1)
        self.assertNotEqual(variables.first().variable_id, '5a7ad8f389bbcb56c5000001')
        self.assertEqual('{%s}' % variables.first().variable_id, integration_file_template.template)
        self.assertEqual(variables.first().arguments['question'], question.pk)

        hooks_data = survey_data['hooks']
        self.assertEqual(len(hooks_data), survey.hooks.count())
        hook = survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assert_fields_equal(hook, hooks_data[0])
        self.assertEqual(list(hook.triggers.values_list('pk', flat=True)), hooks_data[0]['triggers'])

        condition_nodes_data = hooks_data[0]['condition_nodes']
        self.assertEqual(len(condition_nodes_data), hook.condition_nodes.count())
        condition_node = hook.condition_nodes.first()
        self.assertIsNotNone(condition_node)
        self.assertEqual(len(condition_nodes_data[0]['items']), condition_node.items.count())
        for i, condition_node_item in enumerate(condition_node.items.all()):
            condition_node_data = condition_nodes_data[0]['items'][i]
            if condition_node_item.survey_question:
                self.assertEqual(condition_node_item.survey_question, question)
            self.assert_fields_equal(condition_node_item, condition_node_data)

        subscriptions_data = hooks_data[0]['subscriptions']
        self.assertEqual(len(subscriptions_data), hook.subscriptions.count())
        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assert_fields_equal(subscription, subscriptions_data[0])

        self.assertEqual(len(subscriptions_data[0]['headers']), subscription.headers.count())
        for i, header in enumerate(subscription.headers.all()):
            header_data = subscriptions_data[0]['headers'][i]
            self.assert_fields_equal(header, header_data)

        self.assertEqual(len(subscriptions_data[0]['attachments']), subscription.attachments.count())
        for i, attachment in enumerate(subscription.attachments.all()):
            attachment_data = subscriptions_data[0]['attachments'][i]
            self.assert_fields_equal(attachment, attachment_data)

        variables_data = subscriptions_data[0]['variables']
        variables = subscription.surveyvariable_set.all()
        self.assertEqual(len(variables_data), variables.count())
        variables_data_by_type = {
            variable_data['var']: variable_data
            for variable_data in list(variables_data.values())
        }
        for variable in variables:
            variable_key = variable.variable_id
            self.assertNotIn(variable_key, list(variables_data.keys()))
            variable_data = variables_data_by_type.get(variable.var)
            self.assertIsNotNone(variable_data)
            self.assertNotEqual(variable.variable_id, variable_data['_id'])
            self.assertEqual(variable.filters, variable_data['filters'])
            self.assertEqual(len(variable_data['arguments']), len(variable.arguments))
            if 'question' in variable.arguments:
                self.assertIn('question', variable_data['arguments'])
                self.assertEqual(variable.arguments['question'], question.pk)
            if 'questions' in variable.arguments:
                self.assertIn('questions', variable.arguments)
                self.assertEqual(len(variable_data['arguments']['questions']), len(variable.arguments['questions']))
                for question_pk in variable.arguments['questions']:
                    self.assertEqual(question_pk, question.pk)

    @responses.activate
    def test_should_copy_with_integration_hooks__json_rpc(self):
        data = self.get_survey_data(6)  # json_rpc
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)

        survey_data = data['survey']
        survey_data.pop('is_published_external', None)
        survey_data.pop('slug', None)
        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.slug)
        self.assert_fields_equal(survey, survey_data)

        self.assertEqual(self.profile, survey.user)
        self.assertFalse(survey.texts.exists())

        question = survey.surveyquestion_set.first()
        self.assertIsNotNone(question)
        question_data = data['questions'][0]
        self.assert_fields_equal(question, question_data)

        integration_file_template = survey.integration_file_templates.first()
        self.assertIsNotNone(integration_file_template)
        self.assertNotEqual(integration_file_template.pk, 17)
        self.assertNotEqual(integration_file_template.template, '{5a7ad8f389bbcb56c5000001}')
        self.assertEqual(integration_file_template.name, 'my_template')
        self.assertEqual(integration_file_template.slug, 'template_17')
        variables = integration_file_template.surveyvariable_set.all()
        self.assertEqual(variables.count(), 1)
        self.assertNotEqual(variables.first().variable_id, '5a7ad8f389bbcb56c5000001')
        self.assertEqual('{%s}' % variables.first().variable_id, integration_file_template.template)
        self.assertEqual(variables.first().arguments['question'], question.pk)

        hooks_data = survey_data['hooks']
        self.assertEqual(len(hooks_data), survey.hooks.count())
        hook = survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assert_fields_equal(hook, hooks_data[0])
        self.assertEqual(list(hook.triggers.values_list('pk', flat=True)), hooks_data[0]['triggers'])

        condition_nodes_data = hooks_data[0]['condition_nodes']
        self.assertEqual(len(condition_nodes_data), hook.condition_nodes.count())
        condition_node = hook.condition_nodes.first()
        self.assertIsNotNone(condition_node)
        self.assertEqual(len(condition_nodes_data[0]['items']), condition_node.items.count())
        for i, condition_node_item in enumerate(condition_node.items.all()):
            condition_node_data = condition_nodes_data[0]['items'][i]
            if condition_node_item.survey_question:
                self.assertEqual(condition_node_item.survey_question, question)
            self.assert_fields_equal(condition_node_item, condition_node_data)

        subscriptions_data = hooks_data[0]['subscriptions']
        self.assertEqual(len(subscriptions_data), hook.subscriptions.count())
        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assert_fields_equal(subscription, subscriptions_data[0])

        self.assertEqual(len(subscriptions_data[0]['headers']), subscription.headers.count())
        for i, header in enumerate(subscription.headers.all()):
            header_data = subscriptions_data[0]['headers'][i]
            self.assert_fields_equal(header, header_data)

        self.assertEqual(len(subscriptions_data[0]['attachments']), subscription.attachments.count())
        for i, attachment in enumerate(subscription.attachments.all()):
            attachment_data = subscriptions_data[0]['attachments'][i]
            self.assert_fields_equal(attachment, attachment_data)

        json_rpc_data = subscriptions_data[0]['json_rpc']
        self.assert_fields_equal(subscription.json_rpc, json_rpc_data)
        self.assertEqual(len(json_rpc_data['params']), subscription.json_rpc.params.count())
        for i, param in enumerate(subscription.json_rpc.params.all()):
            param_data = json_rpc_data['params'][i]
            self.assert_fields_equal(param, param_data)

        variables_data = subscriptions_data[0]['variables']
        variables = subscription.surveyvariable_set.all()
        self.assertEqual(len(variables_data), variables.count())
        variables_data_by_type = {
            variable_data['var']: variable_data
            for variable_data in list(variables_data.values())
        }
        for variable in variables:
            variable_key = variable.variable_id
            self.assertNotIn(variable_key, list(variables_data.keys()))
            variable_data = variables_data_by_type.get(variable.var)
            self.assertIsNotNone(variable_data)
            self.assertNotEqual(variable.variable_id, variable_data['_id'])
            self.assertEqual(variable.filters, variable_data['filters'])
            self.assertEqual(len(variable_data['arguments']), len(variable.arguments))
            if 'question' in variable.arguments:
                self.assertIn('question', variable_data['arguments'])
                self.assertEqual(variable.arguments['question'], question.pk)
            if 'questions' in variable.arguments:
                self.assertIn('questions', variable.arguments)
                self.assertEqual(len(variable_data['arguments']['questions']), len(variable.arguments['questions']))
                for question_pk in variable.arguments['questions']:
                    self.assertEqual(question_pk, question.pk)

    @responses.activate
    def test_should_copy_with_integration_hooks__startrek(self):
        data = self.get_survey_data(7)  # startrek
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)

        survey_data = data['survey']
        survey_data.pop('is_published_external', None)
        survey_data.pop('slug', None)
        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.slug)
        self.assert_fields_equal(survey, survey_data)

        self.assertEqual(self.profile, survey.user)
        self.assertFalse(survey.texts.exists())

        question = survey.surveyquestion_set.first()
        self.assertIsNotNone(question)
        question_data = data['questions'][0]
        self.assert_fields_equal(question, question_data)

        integration_file_template = survey.integration_file_templates.first()
        self.assertIsNotNone(integration_file_template)
        self.assertNotEqual(integration_file_template.pk, 17)
        self.assertNotEqual(integration_file_template.template, '{5a7ad8f389bbcb56c5000001}')
        self.assertEqual(integration_file_template.name, 'my_template')
        self.assertEqual(integration_file_template.slug, 'template_17')
        variables = integration_file_template.surveyvariable_set.all()
        self.assertEqual(variables.count(), 1)
        self.assertNotEqual(variables.first().variable_id, '5a7ad8f389bbcb56c5000001')
        self.assertEqual('{%s}' % variables.first().variable_id, integration_file_template.template)
        self.assertEqual(variables.first().arguments['question'], question.pk)

        hooks_data = survey_data['hooks']
        self.assertEqual(len(hooks_data), survey.hooks.count())
        hook = survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assert_fields_equal(hook, hooks_data[0])
        self.assertEqual(list(hook.triggers.values_list('pk', flat=True)), hooks_data[0]['triggers'])

        condition_nodes_data = hooks_data[0]['condition_nodes']
        self.assertEqual(len(condition_nodes_data), hook.condition_nodes.count())
        condition_node = hook.condition_nodes.first()
        self.assertIsNotNone(condition_node)
        self.assertEqual(len(condition_nodes_data[0]['items']), condition_node.items.count())
        for i, condition_node_item in enumerate(condition_node.items.all()):
            condition_node_data = condition_nodes_data[0]['items'][i]
            if condition_node_item.survey_question:
                self.assertEqual(condition_node_item.survey_question, question)
            self.assert_fields_equal(condition_node_item, condition_node_data)

        subscriptions_data = hooks_data[0]['subscriptions']
        self.assertEqual(len(subscriptions_data), hook.subscriptions.count())
        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assert_fields_equal(subscription, subscriptions_data[0])

        self.assertEqual(len(subscriptions_data[0]['headers']), subscription.headers.count())
        for i, header in enumerate(subscription.headers.all()):
            header_data = subscriptions_data[0]['headers'][i]
            self.assert_fields_equal(header, header_data)

        self.assertEqual(len(subscriptions_data[0]['attachments']), subscription.attachments.count())
        for i, attachment in enumerate(subscription.attachments.all()):
            attachment_data = subscriptions_data[0]['attachments'][i]
            self.assert_fields_equal(attachment, attachment_data)

        startrek_data = subscriptions_data[0]['startrek']
        self.assert_fields_equal(subscription.startrek, startrek_data)
        self.assertEqual(len(startrek_data['fields']), len(subscription.startrek.fields))
        for i, field in enumerate(subscription.startrek.fields):
            field_data = startrek_data['fields'][i]
            self.assertDictEqual(field, field_data)

        variables_data = subscriptions_data[0]['variables']
        variables = subscription.surveyvariable_set.all()
        self.assertEqual(len(variables_data), variables.count())
        variables_data_by_type = {
            variable_data['var']: variable_data
            for variable_data in list(variables_data.values())
        }
        for variable in variables:
            variable_key = variable.variable_id
            self.assertNotIn(variable_key, list(variables_data.keys()))
            variable_data = variables_data_by_type.get(variable.var)
            self.assertIsNotNone(variable_data)
            self.assertNotEqual(variable.variable_id, variable_data['_id'])
            self.assertEqual(variable.filters, variable_data['filters'])
            self.assertEqual(len(variable_data['arguments']), len(variable.arguments))
            if 'question' in variable.arguments:
                self.assertIn('question', variable_data['arguments'])
                self.assertEqual(variable.arguments['question'], question.pk)
            if 'questions' in variable.arguments:
                self.assertIn('questions', variable.arguments)
                self.assertEqual(len(variable_data['arguments']['questions']), len(variable.arguments['questions']))
                for question_pk in variable.arguments['questions']:
                    self.assertEqual(question_pk, question.pk)

    @responses.activate
    def test_should_copy_with_integration_hooks__wiki(self):
        data = self.get_survey_data(9)  # wiki
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        survey_id = response.data['result']['survey']['id']
        survey = Survey.objects.get(pk=survey_id)

        survey_data = data['survey']
        survey_data.pop('is_published_external', None)
        survey_data.pop('slug', None)
        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.slug)
        self.assert_fields_equal(survey, survey_data)

        self.assertEqual(self.profile, survey.user)
        self.assertFalse(survey.texts.exists())

        question = survey.surveyquestion_set.first()
        self.assertIsNotNone(question)
        question_data = data['questions'][0]
        self.assert_fields_equal(question, question_data)

        integration_file_template = survey.integration_file_templates.first()
        self.assertIsNotNone(integration_file_template)
        self.assertNotEqual(integration_file_template.pk, 17)
        self.assertNotEqual(integration_file_template.template, '{5a7ad8f389bbcb56c5000001}')
        self.assertEqual(integration_file_template.name, 'my_template')
        self.assertEqual(integration_file_template.slug, 'template_17')
        variables = integration_file_template.surveyvariable_set.all()
        self.assertEqual(variables.count(), 1)
        self.assertNotEqual(variables.first().variable_id, '5a7ad8f389bbcb56c5000001')
        self.assertEqual('{%s}' % variables.first().variable_id, integration_file_template.template)
        self.assertEqual(variables.first().arguments['question'], question.pk)

        hooks_data = survey_data['hooks']
        self.assertEqual(len(hooks_data), survey.hooks.count())
        hook = survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assert_fields_equal(hook, hooks_data[0])
        self.assertEqual(list(hook.triggers.values_list('pk', flat=True)), hooks_data[0]['triggers'])

        condition_nodes_data = hooks_data[0]['condition_nodes']
        self.assertEqual(len(condition_nodes_data), hook.condition_nodes.count())
        condition_node = hook.condition_nodes.first()
        self.assertIsNotNone(condition_node)
        self.assertEqual(len(condition_nodes_data[0]['items']), condition_node.items.count())
        for i, condition_node_item in enumerate(condition_node.items.all()):
            condition_node_data = condition_nodes_data[0]['items'][i]
            if condition_node_item.survey_question:
                self.assertEqual(condition_node_item.survey_question, question)
            self.assert_fields_equal(condition_node_item, condition_node_data)

        subscriptions_data = hooks_data[0]['subscriptions']
        self.assertEqual(len(subscriptions_data), hook.subscriptions.count())
        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assert_fields_equal(subscription, subscriptions_data[0])

        self.assertEqual(len(subscriptions_data[0]['headers']), subscription.headers.count())
        for i, header in enumerate(subscription.headers.all()):
            header_data = subscriptions_data[0]['headers'][i]
            self.assert_fields_equal(header, header_data)

        self.assertEqual(len(subscriptions_data[0]['attachments']), subscription.attachments.count())
        for i, attachment in enumerate(subscription.attachments.all()):
            attachment_data = subscriptions_data[0]['attachments'][i]
            self.assert_fields_equal(attachment, attachment_data)

        wiki_data = subscriptions_data[0]['wiki']
        self.assert_fields_equal(subscription.wiki, wiki_data)

        variables_data = subscriptions_data[0]['variables']
        variables = subscription.surveyvariable_set.all()
        self.assertEqual(len(variables_data), variables.count())
        variables_data_by_type = {
            variable_data['var']: variable_data
            for variable_data in list(variables_data.values())
        }
        for variable in variables:
            variable_key = variable.variable_id
            self.assertNotIn(variable_key, list(variables_data.keys()))
            variable_data = variables_data_by_type.get(variable.var)
            self.assertIsNotNone(variable_data)
            self.assertNotEqual(variable.variable_id, variable_data['_id'])
            self.assertEqual(variable.filters, variable_data['filters'])
            self.assertEqual(len(variable_data['arguments']), len(variable.arguments))
            if 'question' in variable.arguments:
                self.assertIn('question', variable_data['arguments'])
                self.assertEqual(variable.arguments['question'], question.pk)
            if 'questions' in variable.arguments:
                self.assertIn('questions', variable.arguments)
                self.assertEqual(len(variable_data['arguments']['questions']), len(variable.arguments['questions']))
                for question_pk in variable.arguments['questions']:
                    self.assertEqual(question_pk, question.pk)

    def assert_fields_equal(self, instance, instance_data):
        from django.db.models import AutoField
        for field in instance._meta.fields:
            if not field.is_relation and not isinstance(field, AutoField):
                if field.name in instance_data:
                    msg = 'field `%s` error' % field.name
                    self.assertEqual(getattr(instance, field.name), instance_data[field.name], msg=msg)

    def test_should_copy_question_show_conditions(self):
        ContentTypeAttributeFactory(title='test', pk=2, content_type=ContentType.objects.get_for_model(SurveyQuestion))
        questions_show_conditions = [{
            'id': 377,
            'items': [{
                'condition': 'eq',
                'content_type_attribute': 2,
                'id': 392,
                'operator': 'and',
                'position': 1,
                'survey_question': 13797,
                'survey_question_show_condition_node': 377,
                'value': '3717'
            }],
            'survey_question': 13797
        }]
        data = {
            'survey': self.base_survey_data,
            'questions': [self.base_question_data],
            'texts': [],
            'questions_show_conditions': questions_show_conditions,
        }
        self.base_question_data['choices'] = self.base_choices
        self.assertEqual(SurveyQuestionShowConditionNode.objects.count(), 0)

        response = self.client.post(self.post_url, data=data, format='json')
        msg = '%s' % response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=msg)

        msg = 'Должны были быть импортированы условия показа вопросов'
        self.assertEqual(SurveyQuestionShowConditionNode.objects.count(), 1, msg=msg)
        node = SurveyQuestionShowConditionNode.objects.all()[0]
        self.assertEqual(node.items.count(), 1, msg=msg)

        exc_question = SurveyQuestion.objects.all()[0]
        msg = 'Должны были замениться id вопросов на вновь созданные'
        self.assertEqual(node.survey_question.pk, exc_question.pk, msg=msg)

        exc_choice = SurveyQuestionChoice.objects.all()[0]
        exc_questions_show_conditions = questions_show_conditions[0]
        exc_questions_show_condition_item = exc_questions_show_conditions['items'][0]
        exc_questions_show_condition_item['survey_question'] = exc_question.pk
        exc_questions_show_condition_item['value'] = str(exc_choice.pk)
        exc_questions_show_condition_item['survey_question_show_condition_node'] = node.pk
        self.assertInstanceCopied(node.items.all()[0], exc_questions_show_condition_item)

    def test_should_copy_survey_without_group_if_it_is_not_exists(self):
        survey_group = SurveyGroupFactory(pk=1)
        survey_group.save()
        self.base_survey_data['group_id'] = 10
        self.base_survey_data['group'] = {'id': 10, 'name': survey_group.name}
        data = {'survey': self.base_survey_data}

        response = self.client.post(self.post_url, data=data, format='json')
        msg = '%s' % response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=msg)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        msg = 'Если группы нет, то форма должна быть скопирована без нее'
        self.assertEqual(survey.group, None, msg=msg)

    def test_must_replace_param_data_source_params_question_id_to_new_question(self):
        self.base_question_data['param_data_source_params'] = {
            'filters': [
                {
                    'filter': {'name': 'question'},
                    'type': 'specified_value',
                    'value': self.base_question_data['id'],
                }
            ]
        }

        questions = [self.base_question_data]
        data = {
            'survey': self.base_survey_data,
            'questions': questions,
        }
        SurveyQuestion.objects.all().delete()
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEquals(response.status_code, status.HTTP_200_OK)
        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        questions = SurveyQuestion.objects.filter(survey=survey)
        self.assertEqual(questions.count(), 1)
        fresh_question = SurveyQuestion.objects.get(pk=survey.surveyquestion_set.all()[0].pk)
        self.assertEquals(fresh_question.param_data_source_params['filters'][0]['value'], fresh_question.pk)


class TestSurveyImportTracker(TestCase, JsonFixtureMixin, CopyAssertionsMixin):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.post_url = '/admin/api/v2/surveys/import-tracker/'
        self.template_data = json.loads(self.get_json_from_file('test_import_tracker.json'))
        self.template = SurveyTemplate.objects.create(
            name='name',
            position=1,
            slug='test',
            data=self.template_data,
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_tracker_import_required_parameters_business(self):
        data = {}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('import_from_template' in response.data)

        data = {'import_from_template': 1}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('org_id' in response.data)

    def test_tracker_import_required_parameters_intranet(self):
        data = {}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('import_from_template' in response.data)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_tracker_import_template_not_found_business(self):
        o2g = OrganizationToGroupFactory()
        data = {'import_from_template': 'not_found', 'org_id': o2g.org.dir_id}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('import_from_template' in response.data)

    def test_tracker_import_template_not_found_intranet(self):
        data = {'import_from_template': 'not_found'}
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('import_from_template' in response.data)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_tracker_import_fails_if_user_not_org_member(self):
        o2g = OrganizationToGroupFactory()
        data = {
            'import_from_template': self.template.id,
            'queue': 'TEST_QUEUE_NAME',
            'org_id': o2g.org.dir_id,
        }
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 401)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_tracker_import_creates_survey_with_integration_business(self):
        o2g = OrganizationToGroupFactory()
        data = {
            'import_from_template': self.template.id,
            'queue': 'TEST_QUEUE_NAME',
            'name': 'New Form',
            'org_id': o2g.org.dir_id,
        }
        response = self.client.post(self.post_url, data=data, format='json', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertEqual(survey.name, data['name'])
        self.assertEqual(survey.translations['name']['ru'], data['name'])
        self.assertEqual(survey.org, o2g.org)
        self.assertEqual(survey.is_published_external, True)

        st_subscription = StartrekSubscriptionData.objects.get(
            subscription__survey_hook__survey__pk=survey.pk
        )
        self.assertEqual(st_subscription.queue, 'TEST_QUEUE_NAME')

    def test_tracker_import_creates_survey_with_integration_intranet(self):
        data = {
            'import_from_template': self.template.id,
            'queue': 'TEST_QUEUE_NAME',
            'name': 'New Form',
        }
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertEqual(survey.name, data['name'])
        self.assertEqual(survey.translations['name']['ru'], data['name'])
        self.assertEqual(survey.org, None)
        self.assertEqual(survey.is_published_external, True)

        st_subscription = StartrekSubscriptionData.objects.get(
            subscription__survey_hook__survey__pk=survey.pk
        )
        self.assertEqual(st_subscription.queue, 'TEST_QUEUE_NAME')

    def test_tracker_import_creates_survey_with_integration_intranet_not_published(self):
        data = {
            'import_from_template': self.template.id,
            'is_published_external': False,
        }
        response = self.client.post(self.post_url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        survey = Survey.objects.get(pk=response.data['result']['survey']['id'])
        self.assertEqual(survey.is_published_external, False)
