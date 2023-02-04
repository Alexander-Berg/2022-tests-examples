# -*- coding: utf-8 -*-
from collections import namedtuple
from django.conf import settings
from django.contrib.auth.models import Permission, Group
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm, remove_perm
from pydantic import ValidationError
from unittest.mock import patch, call

from events.accounts.helpers import YandexClient
from events.accounts.factories import UserFactory, GroupFactory, OrganizationToGroupFactory
from events.accounts.models import User
from events.common_app.helpers import override_cache_settings
from events.common_app.startrek.client import StartrekClient
from events.common_storages.factories import ProxyStorageModelFactory
from events.common_storages.utils import get_mds_url
from events.conditions.factories import ContentTypeAttributeFactory
from events.staff.factories import StaffGroupFactory
from events.surveyme.factories import SurveyFactory, SurveyQuestionFactory
from events.surveyme.models import AnswerType, Survey
from events.surveyme_integration.factories import (
    HookSubscriptionNotificationFactory,
    IntegrationFileTemplateFactory,
    JSONRPCSubscriptionDataFactory,
    JSONRPCSubscriptionParamFactory,
    ServiceSurveyHookSubscriptionFactory,
    StartrekSubscriptionDataFactory,
    SubscriptionAttachmentFactory,
    SubscriptionHeaderFactory,
    SurveyHookConditionFactory,
    SurveyHookConditionNodeFactory,
    SurveyHookFactory,
    SurveyVariableFactory,
    WikiSubscriptionDataFactory,
)
from events.surveyme_integration.variables import variables_list
from events.v3.errors import ValidationError as ApiValidationError
from events.v3.helpers import has_perm
from events.v3.views.surveys import get_survey_out
from events.v3.schemas import ConditionItemIn, QuestionsIn, VariableIn, SurveyOut, TrackerFieldIn
from events.v3.types import ActionType, VariableId
from events.v3.views.subscriptions import (
    TrackerValidator,
    get_http_out, get_put_out, get_post_out, get_jsonrpc_out, get_wiki_out,
    get_email_out, get_tracker_out, get_wiki_extras_out, get_tracker_extras_out,
    get_jsonrpc_params_out, get_jsonrpc_extras_out, get_headers_out,
    get_static_attachments_out, get_template_attachments_out, get_questions_out,
    get_attachments_out, get_variables_out, get_question_slugs_with_type, get_variable_obj,
)
from events.v3.views.hooks import get_hooks_out
from events.v3.views.conditions import get_condition_items_obj, get_cta, get_conditions_out


def make_row(**kwargs):
    Row = namedtuple('Row', kwargs.keys())
    return Row(**kwargs)


class TestGetSurveyView_check_access(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_unauthorized(self):
        self.client.remove_cookie()
        survey = SurveyFactory()
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 401)

    def test_should_return_not_found(self):
        response = self.client.get('/v3/surveys/999999/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_not_permitted(self):
        survey = SurveyFactory()
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 403)

    def test_should_return_success(self):
        survey = SurveyFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 200)

        self.user.is_superuser = True
        self.user.save()
        remove_perm(settings.ROLE_FORM_MANAGER, self.user, survey)
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 200)


@override_settings(IS_BUSINESS_SITE=True)
class TestGetSurveyView_check_access_b2b(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_unauthorized(self):
        self.client.remove_cookie()
        survey = SurveyFactory()
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 401)

    def test_should_return_not_found(self):
        response = self.client.get('/v3/surveys/999999/')
        self.assertEqual(response.status_code, 404)

        survey = SurveyFactory()
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 404)

        o2g = OrganizationToGroupFactory()
        self.user.groups.add(o2g.group)
        response = self.client.get(f'/v3/surveys/{survey.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 404)

        self.user.groups.remove(o2g.group)
        survey.org = o2g.org
        survey.save()
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_not_permitted(self):
        o2g = OrganizationToGroupFactory()
        survey = SurveyFactory(org=o2g.org)
        self.user.groups.add(o2g.group)
        response = self.client.get(f'/v3/surveys/{survey.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 403)

    def test_should_return_success(self):
        survey = SurveyFactory(user=self.user)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/v3/surveys/{survey.pk}/', HTTP_X_ORGS='999')
        self.assertEqual(response.status_code, 200)

        o2g = OrganizationToGroupFactory()
        survey = SurveyFactory(org=o2g.org)
        self.user.groups.add(o2g.group)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)
        response = self.client.get(f'/v3/surveys/{survey.pk}/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)

        self.user.is_superuser = True
        self.user.save()
        self.user.groups.remove(o2g.group)
        remove_perm(settings.ROLE_FORM_MANAGER, self.user, survey)
        response = self.client.get(f'/v3/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 200)


class TestGetSurveyView_filters(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)

    def test_should_split_fields(self):
        survey = SurveyFactory()
        with patch('events.v3.views.surveys.get_survey_out') as mock_out:
            mock_out.return_value = SurveyOut(id=survey.pk)
            self.client.get(f'/v3/surveys/{survey.pk}/')
        mock_out.assert_called_once_with(survey, fields=None)

        with patch('events.v3.views.surveys.get_survey_out') as mock_out:
            mock_out.return_value = SurveyOut(id=survey.pk)
            self.client.get(f'/v3/surveys/{survey.pk}/', {'fields': 'name,hooks'})
        mock_out.assert_called_once_with(survey, fields=['name', 'hooks'])


class TestGetSurveyView_variables(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        self.template = IntegrationFileTemplateFactory(survey=self.survey)

    def test_get_variables_out(self):
        questions = [
            SurveyQuestionFactory(survey=self.survey),
            SurveyQuestionFactory(survey=self.survey),
            SurveyQuestionFactory(survey=self.survey),
        ]
        var1 = SurveyVariableFactory(
            hook_subscription=self.subscription,
            var='form.questions_answers',
            format_name='txt',
            filters=['sanitize', 'json'],
            arguments={
                'is_all_questions': False,
                'only_with_value': True,
                'show_filenames': True,
                'questions': [questions[0].pk, questions[1].pk],
            },
        )
        var2 = SurveyVariableFactory(
            integration_file_template=self.template,
            var='form.question_answer',
            arguments={
                'show_filenames': False,
                'question': questions[2].pk,
            },
        )
        var3 = SurveyVariableFactory(
            hook_subscription=self.subscription,
            var='request.header',
            arguments={
                'name': 'X-Foo',
            },
        )
        SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory(), var='form.id')

        variables = get_variables_out(self.survey)

        self.assertEqual(len(variables.subscriptions), 1)
        self.assertEqual(len(variables.get_subscription(self.subscription.pk)), 2)

        var = variables.get_subscription(self.subscription.pk)[0]
        self.assertEqual(var.id, str(var1.variable_id))
        self.assertEqual(var.type, var1.var)
        self.assertEqual(var.renderer, var1.format_name)
        self.assertEqual(var.questions, {
            'all': False, 'items': [questions[0].param_slug, questions[1].param_slug],
        })
        self.assertEqual(var.question, None)
        self.assertEqual(var.only_with_value, True)
        self.assertEqual(var.show_filenames, True)
        self.assertEqual(var.filters, var1.filters)
        self.assertEqual(var.name, None)

        var = variables.get_subscription(self.subscription.pk)[1]
        self.assertEqual(var.id, str(var3.variable_id))
        self.assertEqual(var.type, var3.var)
        self.assertEqual(var.renderer, var3.format_name)
        self.assertEqual(var.questions, None)
        self.assertEqual(var.question, None)
        self.assertEqual(var.only_with_value, None)
        self.assertEqual(var.show_filenames, None)
        self.assertEqual(var.filters, None)
        self.assertEqual(var.name, var3.arguments['name'])

        self.assertEqual(len(variables.templates), 1)
        self.assertEqual(len(variables.get_template(self.template.pk)), 1)

        var = variables.get_template(self.template.pk)[0]
        self.assertEqual(var.id, str(var2.variable_id))
        self.assertEqual(var.type, var2.var)
        self.assertEqual(var.renderer, None)
        self.assertEqual(var.questions, None)
        self.assertEqual(var.question, questions[2].param_slug)
        self.assertEqual(var.only_with_value, None)
        self.assertEqual(var.show_filenames, False)
        self.assertEqual(var.filters, None)
        self.assertEqual(var.name, None)


class TestGetSurveyView_attachments(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook, is_all_questions=False,
        )
        self.template = IntegrationFileTemplateFactory(
            survey=self.survey, type='pdf', name='name1', template='body1',
        )
        self.var = SurveyVariableFactory(integration_file_template=self.template, var='form.id')
        answer_type=AnswerType.objects.get(slug='answer_files')
        self.questions = [
            SurveyQuestionFactory(survey=self.survey, answer_type=answer_type),
            SurveyQuestionFactory(survey=self.survey, answer_type=answer_type),
        ]
        self.meta = ProxyStorageModelFactory(path='/123/2345_file.txt', original_name='file.txt')

    def test_get_attachments_out(self):
        self.subscription.attachment_templates.add(self.template)
        self.subscription.questions.add(self.questions[0])
        SubscriptionAttachmentFactory(subscription=self.subscription, file=self.meta.path)

        subscription = make_row(
            pk=self.subscription.pk,
            is_all_questions=self.subscription.is_all_questions,
        )
        questions = get_questions_out(self.survey)
        static_attachments = get_static_attachments_out(self.survey)
        variables = get_variables_out(self.survey)
        template_attachments = get_template_attachments_out(self.survey, variables)

        attachments = get_attachments_out(
            subscription, questions, static_attachments, template_attachments,
        )

        self.assertEqual(attachments.question.all, False)
        self.assertEqual(attachments.question.items, [self.questions[0].param_slug])

        self.assertEqual(len(attachments.static), 1)
        attach = attachments.static[0]
        self.assertEqual(attach.name, self.meta.original_name)
        self.assertEqual(attach.links, {'orig': get_mds_url(self.meta.path)})

        self.assertEqual(len(attachments.template), 1)
        attach = attachments.template[0]
        self.assertEqual(attach.name, self.template.name)
        self.assertEqual(attach.body, self.template.template)
        self.assertEqual(attach.mime, 'application/pdf')
        self.assertEqual(len(attach.variables), 1)
        var = attach.variables[0]
        self.assertEqual(var.id, str(self.var.variable_id))

    def test_is_all_questions(self):
        self.subscription.questions.add(self.questions[0])
        self.subscription.questions.add(self.questions[1])
        self.subscription.is_all_questions = True
        self.subscription.save()

        subscription = make_row(
            pk=self.subscription.pk,
            is_all_questions=self.subscription.is_all_questions,
        )
        questions = get_questions_out(self.survey)

        attachments = get_attachments_out(subscription, questions)

        self.assertEqual(attachments.question.all, True)
        self.assertEqual(attachments.question.items, None)


class TestGetSurveyView_conditions(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        cta = ContentTypeAttributeFactory(attr='answer_short_text')
        self.nodes = [
            SurveyHookConditionNodeFactory(hook=self.hook),
            SurveyHookConditionNodeFactory(hook=self.hook),
        ]
        self.question = SurveyQuestionFactory(survey=self.survey)
        self.items = [
            SurveyHookConditionFactory(
                condition_node=self.nodes[0], content_type_attribute=cta,
                value='1', operator='and', condition='eq',
            ),
            SurveyHookConditionFactory(
                condition_node=self.nodes[0], content_type_attribute=cta,
                value='2', operator='or', condition='neq', survey_question=self.question,
            ),
            SurveyHookConditionFactory(
                condition_node=self.nodes[1], content_type_attribute=cta,
                value='3', operator='and', condition='eq',
            ),
        ]

    def test_get_hook_condition_nodes_out(self):
        conditions = get_conditions_out(self.survey)

        self.assertEqual(len(conditions), 1)
        condition_nodes = conditions[self.subscription.pk]
        self.assertEqual(len(condition_nodes), 2)

        condition_node = condition_nodes[0]
        self.assertEqual(condition_node.id, self.nodes[0].pk)
        self.assertEqual(condition_node.operator, 'or')
        self.assertEqual(len(condition_node.items), 2)

        condition_item = condition_node.items[0]
        self.assertEqual(condition_item.operator, 'and')
        self.assertEqual(condition_item.condition, 'eq')
        self.assertEqual(condition_item.value, '1')
        self.assertEqual(condition_item.question, None)

        condition_item = condition_node.items[1]
        self.assertEqual(condition_item.operator, 'or')
        self.assertEqual(condition_item.condition, 'neq')
        self.assertEqual(condition_item.value, '2')
        self.assertEqual(condition_item.question, self.question.param_slug)

        condition_node = condition_nodes[1]
        self.assertEqual(condition_node.id, self.nodes[1].pk)
        self.assertEqual(condition_node.operator, 'or')
        self.assertEqual(len(condition_node.items), 1)

        condition_item = condition_node.items[0]
        self.assertEqual(condition_item.operator, 'and')
        self.assertEqual(condition_item.condition, 'eq')
        self.assertEqual(condition_item.value, '3')
        self.assertEqual(condition_item.question, None)


class TestGetSurveyView_headers(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        self.headers = [
            SubscriptionHeaderFactory(
                subscription=self.subscription, name='header1', value='1',
            ),
            SubscriptionHeaderFactory(
                subscription=self.subscription, name='header2', value='2', add_only_with_value=True,
            ),
        ]

    def test_get_headers_out(self):
        headers_out = get_headers_out(self.survey)

        self.assertEqual(len(headers_out), 1)
        headers = headers_out.get(self.subscription.pk)
        self.assertEqual(len(headers), 2)

        header = headers[0]
        self.assertEqual(header.name, 'header1')
        self.assertEqual(header.value, '1')
        self.assertEqual(header.only_with_value, False)

        header = headers[1]
        self.assertEqual(header.name, 'header2')
        self.assertEqual(header.value, '2')
        self.assertEqual(header.only_with_value, True)


class TestGetSurveyView_email(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            follow_result=True,
            context_language='en',
            email_from_address='robot@yandex.ru',
            email_from_title='robot1',
            email_to_address='user@yandex.ru',
            email_spam_check=True,
            title='title1',
            body='body1',
            is_all_questions=False,
        )
        answer_type=AnswerType.objects.get(slug='answer_files')
        self.question = SurveyQuestionFactory(survey=self.survey, answer_type=answer_type)
        self.subscription.questions.add(self.question)
        self.var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        self.header = SubscriptionHeaderFactory(subscription=self.subscription, name='header1')

    def test_email_out(self):
        subscription = make_row(
            pk=self.subscription.pk,
            is_active=self.subscription.is_active,
            follow_result=self.subscription.follow_result,
            context_language=self.subscription.context_language,
            email_from_address=self.subscription.email_from_address,
            email_from_title=self.subscription.email_from_title,
            email_to_address=self.subscription.email_to_address,
            email_spam_check=self.subscription.email_spam_check,
            title=self.subscription.title,
            body=self.subscription.body,
            is_all_questions=self.subscription.is_all_questions,
            service_type_action__slug=self.subscription.service_type_action.slug,
            service_type_action__service_type__slug=self.subscription.service_type_action.service_type.slug,
        )
        variables = get_variables_out(self.survey)
        headers = get_headers_out(self.survey)
        questions = get_questions_out(self.survey)
        static_attachments = get_static_attachments_out(self.survey)
        template_attachments = get_template_attachments_out(self.survey, variables)

        out = get_email_out(
            subscription, variables, headers, questions, static_attachments, template_attachments,
        )

        self.assertEqual(out.id, self.subscription.pk)
        self.assertEqual(out.active, self.subscription.is_active)
        self.assertEqual(out.type, 'email')
        self.assertEqual(out.follow, self.subscription.follow_result)
        self.assertEqual(out.language, self.subscription.context_language)
        self.assertEqual(out.subject, self.subscription.title)
        self.assertEqual(out.body, self.subscription.body)
        self.assertEqual(out.email_to_address, self.subscription.email_to_address)
        self.assertEqual(out.email_from_address, self.subscription.email_from_address)
        self.assertEqual(out.email_from_title, self.subscription.email_from_title)
        self.assertEqual(out.email_spam_check, self.subscription.email_spam_check)
        self.assertEqual(len(out.variables), 1)
        self.assertEqual(out.variables[0].id, str(self.var.variable_id))
        self.assertEqual(out.attachments.question.all, False)
        self.assertEqual(len(out.attachments.question.items), 1)
        self.assertEqual(out.attachments.question.items[0], self.question.param_slug)
        self.assertEqual(out.attachments.static, None)
        self.assertEqual(out.attachments.template, None)
        self.assertEqual(len(out.headers), 1)
        self.assertEqual(out.headers[0].name, 'header1')


class TestGetSurveyView_tracker(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=7,  # startrek_create_ticket
            survey_hook=self.hook,
            follow_result=True,
            context_language='en',
            title='title1',
            body='body1',
            is_all_questions=False,
        )
        self.tracker_extra = StartrekSubscriptionDataFactory(
            subscription=self.subscription,
            queue='TEST', parent='TEST-1',
            author='user1', assignee='user2',
            type=1, priority=2,
            fields=[
                {
                    'key': {'name': 'Tags', 'slug': 'tags', 'type': 'array/string'},
                    'value': 'tag1',
                    'add_only_with_value': True,
                },
                {
                    'key': {'id': '123--cats', 'name': 'Cats', 'slug': 'tags', 'type': 'string'},
                    'value': 'fluffy',
                    'add_only_with_value': True,
                },
            ],
        )
        answer_type=AnswerType.objects.get(slug='answer_files')
        self.question = SurveyQuestionFactory(survey=self.survey, answer_type=answer_type)
        self.subscription.questions.add(self.question)
        self.var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')

    def test_tracker_out(self):
        subscription = make_row(
            pk=self.subscription.pk,
            is_active=self.subscription.is_active,
            follow_result=self.subscription.follow_result,
            context_language=self.subscription.context_language,
            title=self.subscription.title,
            body=self.subscription.body,
            is_all_questions=self.subscription.is_all_questions,
            service_type_action__slug=self.subscription.service_type_action.slug,
            service_type_action__service_type__slug=self.subscription.service_type_action.service_type.slug,
        )
        variables = get_variables_out(self.survey)
        questions = get_questions_out(self.survey)
        static_attachments = get_static_attachments_out(self.survey)
        tracker_extras = get_tracker_extras_out(self.survey)

        out = get_tracker_out(subscription, variables, tracker_extras, questions, static_attachments)

        self.assertEqual(out.id, self.subscription.pk)
        self.assertEqual(out.active, self.subscription.is_active)
        self.assertEqual(out.type, 'tracker')
        self.assertEqual(out.follow, self.subscription.follow_result)
        self.assertEqual(out.language, self.subscription.context_language)
        self.assertEqual(out.subject, self.subscription.title)
        self.assertEqual(out.body, self.subscription.body)
        self.assertEqual(out.queue, self.tracker_extra.queue)
        self.assertEqual(out.parent, self.tracker_extra.parent)
        self.assertEqual(out.author, self.tracker_extra.author)
        self.assertEqual(out.assignee, self.tracker_extra.assignee)
        self.assertEqual(out.issue_type, self.tracker_extra.type)
        self.assertEqual(out.priority, self.tracker_extra.priority)
        self.assertEqual(len(out.fields), 2)
        field = out.fields[0]
        self.assertEqual(field.key.name, 'Tags')
        self.assertEqual(field.key.slug, 'tags')
        self.assertEqual(field.key.type, 'array/string')
        self.assertEqual(field.value, 'tag1')
        self.assertEqual(field.only_with_value, True)
        field = out.fields[1]
        self.assertEqual(field.key.name, 'Cats')
        self.assertEqual(field.key.slug, '123--cats')
        self.assertEqual(field.key.type, 'string')
        self.assertEqual(field.value, 'fluffy')
        self.assertEqual(field.only_with_value, True)
        self.assertEqual(len(out.variables), 1)
        self.assertEqual(out.variables[0].id, str(self.var.variable_id))
        self.assertEqual(out.attachments.question.all, False)
        self.assertEqual(len(out.attachments.question.items), 1)
        self.assertEqual(out.attachments.question.items[0], self.question.param_slug)


class TestGetSurveyView_wiki(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=9,  # wiki_append_to_wiki_page
            survey_hook=self.hook,
            follow_result=True,
            context_language='en',
            title='title1',
            body='body1',
        )
        self.wiki_extra = WikiSubscriptionDataFactory(
            subscription=self.subscription,
            supertag='supertag1/#first',
            text='body2',
        )
        self.var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')

    def test_wiki_out(self):
        subscription = make_row(
            pk=self.subscription.pk,
            is_active=self.subscription.is_active,
            follow_result=self.subscription.follow_result,
            context_language=self.subscription.context_language,
            service_type_action__slug=self.subscription.service_type_action.slug,
            service_type_action__service_type__slug=self.subscription.service_type_action.service_type.slug,
        )
        variables = get_variables_out(self.survey)
        wiki_extras = get_wiki_extras_out(self.survey)

        out = get_wiki_out(subscription, variables, wiki_extras)

        self.assertEqual(out.id, self.subscription.pk)
        self.assertEqual(out.active, self.subscription.is_active)
        self.assertEqual(out.type, 'wiki')
        self.assertEqual(out.follow, self.subscription.follow_result)
        self.assertEqual(out.language, self.subscription.context_language)
        self.assertEqual(out.body, self.wiki_extra.text)
        self.assertEqual(out.supertag, self.wiki_extra.supertag)
        self.assertEqual(len(out.variables), 1)
        self.assertEqual(out.variables[0].id, str(self.var.variable_id))


class TestGetSurveyView_jsonrpc(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=6,  # json_rpc_post
            survey_hook=self.hook,
            follow_result=True,
            context_language='en',
            http_url='https://yandex.ru',
            tvm2_client_id='123',
        )
        self.jsonrpc_extra = JSONRPCSubscriptionDataFactory(
            subscription=self.subscription,
            method='method1',
        )
        self.jsonrpc_param = JSONRPCSubscriptionParamFactory(
            subscription=self.jsonrpc_extra,
            name='param1', value='1', add_only_with_value=True,
        )
        self.header = SubscriptionHeaderFactory(subscription=self.subscription, name='header1')
        self.var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')

    def test_jsonrpc_out(self):
        subscription = make_row(
            pk=self.subscription.pk,
            is_active=self.subscription.is_active,
            follow_result=self.subscription.follow_result,
            context_language=self.subscription.context_language,
            http_url=self.subscription.http_url,
            tvm2_client_id=self.subscription.tvm2_client_id,
            service_type_action__slug=self.subscription.service_type_action.slug,
            service_type_action__service_type__slug=self.subscription.service_type_action.service_type.slug,
        )
        variables = get_variables_out(self.survey)
        jsonrpc_extras = get_jsonrpc_extras_out(self.survey)
        jsonrpc_params = get_jsonrpc_params_out(self.survey)
        headers = get_headers_out(self.survey)

        out = get_jsonrpc_out(subscription, variables, jsonrpc_extras, jsonrpc_params, headers)

        self.assertEqual(out.id, self.subscription.pk)
        self.assertEqual(out.active, self.subscription.is_active)
        self.assertEqual(out.type, 'jsonrpc')
        self.assertEqual(out.follow, self.subscription.follow_result)
        self.assertEqual(out.language, self.subscription.context_language)
        self.assertEqual(out.method, self.jsonrpc_extra.method)
        self.assertEqual(out.url, self.subscription.http_url)
        self.assertEqual(out.tvm_client, self.subscription.tvm2_client_id)
        self.assertEqual(len(out.params), 1)
        self.assertEqual(out.params[0].name, 'param1')
        self.assertEqual(out.params[0].value, '1')
        self.assertEqual(out.params[0].only_with_value, True)
        self.assertEqual(len(out.variables), 1)
        self.assertEqual(out.variables[0].id, str(self.var.variable_id))
        self.assertEqual(len(out.headers), 1)
        self.assertEqual(out.headers[0].name, 'header1')


class TestGetSurveyView_postput(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=4,  # http_post
            survey_hook=self.hook,
            follow_result=True,
            context_language='en',
            http_url='https://yandex.ru',
            http_format_name='json',
            tvm2_client_id='123',
            is_all_questions=False,
        )
        self.question = SurveyQuestionFactory(survey=self.survey)
        self.header = SubscriptionHeaderFactory(subscription=self.subscription, name='header1')
        self.var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')

    def test_post_out(self):
        self.subscription.questions.add(self.question)
        subscription = make_row(
            pk=self.subscription.pk,
            is_active=self.subscription.is_active,
            follow_result=self.subscription.follow_result,
            context_language=self.subscription.context_language,
            http_url=self.subscription.http_url,
            http_format_name=self.subscription.http_format_name,
            is_all_questions=self.subscription.is_all_questions,
            tvm2_client_id=self.subscription.tvm2_client_id,
            service_type_action__slug=self.subscription.service_type_action.slug,
            service_type_action__service_type__slug=self.subscription.service_type_action.service_type.slug,
        )
        variables = get_variables_out(self.survey)
        headers = get_headers_out(self.survey)
        questions = get_questions_out(self.survey)

        out = get_post_out(subscription, variables, headers, questions)

        self.assertEqual(out.id, self.subscription.pk)
        self.assertEqual(out.active, self.subscription.is_active)
        self.assertEqual(out.type, 'post')
        self.assertEqual(out.follow, self.subscription.follow_result)
        self.assertEqual(out.language, self.subscription.context_language)
        self.assertEqual(out.url, self.subscription.http_url)
        self.assertEqual(out.format, self.subscription.http_format_name)
        self.assertEqual(out.tvm_client, self.subscription.tvm2_client_id)
        self.assertEqual(out.questions.all, False)
        self.assertEqual(out.questions.items, [self.question.param_slug])
        self.assertEqual(len(out.variables), 1)
        self.assertEqual(out.variables[0].id, str(self.var.variable_id))
        self.assertEqual(len(out.headers), 1)
        self.assertEqual(out.headers[0].name, 'header1')

    def test_put_out(self):
        self.subscription.service_type_action_id = 10  # http_put
        self.subscription.http_format_name = 'xml'
        self.subscription.is_all_questions = True
        self.subscription.save()
        self.subscription.questions.add(self.question)
        subscription = make_row(
            pk=self.subscription.pk,
            is_active=self.subscription.is_active,
            follow_result=self.subscription.follow_result,
            context_language=self.subscription.context_language,
            http_url=self.subscription.http_url,
            http_format_name=self.subscription.http_format_name,
            is_all_questions=self.subscription.is_all_questions,
            tvm2_client_id=self.subscription.tvm2_client_id,
            service_type_action__slug=self.subscription.service_type_action.slug,
            service_type_action__service_type__slug=self.subscription.service_type_action.service_type.slug,
        )
        variables = get_variables_out(self.survey)
        headers = get_headers_out(self.survey)
        questions = get_questions_out(self.survey)

        out = get_put_out(subscription, variables, headers, questions)

        self.assertEqual(out.id, self.subscription.pk)
        self.assertEqual(out.active, self.subscription.is_active)
        self.assertEqual(out.type, 'put')
        self.assertEqual(out.follow, self.subscription.follow_result)
        self.assertEqual(out.language, self.subscription.context_language)
        self.assertEqual(out.url, self.subscription.http_url)
        self.assertEqual(out.format, self.subscription.http_format_name)
        self.assertEqual(out.tvm_client, self.subscription.tvm2_client_id)
        self.assertEqual(out.questions.all, True)
        self.assertEqual(out.questions.items, None)
        self.assertEqual(len(out.variables), 1)
        self.assertEqual(out.variables[0].id, str(self.var.variable_id))
        self.assertEqual(len(out.headers), 1)
        self.assertEqual(out.headers[0].name, 'header1')


class TestGetSurveyView_http(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=10,  # http_arbitrary
            survey_hook=self.hook,
            follow_result=True,
            context_language='en',
            http_url='https://yandex.ru',
            http_method='patch',
            body='{}',
            tvm2_client_id='123',
        )
        self.header = SubscriptionHeaderFactory(subscription=self.subscription, name='header1')
        self.var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')

    def test_http_out(self):
        subscription = make_row(
            pk=self.subscription.pk,
            is_active=self.subscription.is_active,
            follow_result=self.subscription.follow_result,
            context_language=self.subscription.context_language,
            http_url=self.subscription.http_url,
            http_method=self.subscription.http_method,
            body=self.subscription.body,
            tvm2_client_id=self.subscription.tvm2_client_id,
            service_type_action__slug=self.subscription.service_type_action.slug,
            service_type_action__service_type__slug=self.subscription.service_type_action.service_type.slug,
        )
        variables = get_variables_out(self.survey)
        headers = get_headers_out(self.survey)

        out = get_http_out(subscription, variables, headers)

        self.assertEqual(out.id, self.subscription.pk)
        self.assertEqual(out.active, self.subscription.is_active)
        self.assertEqual(out.type, 'http')
        self.assertEqual(out.follow, self.subscription.follow_result)
        self.assertEqual(out.language, self.subscription.context_language)
        self.assertEqual(out.url, self.subscription.http_url)
        self.assertEqual(out.method, self.subscription.http_method)
        self.assertEqual(out.body, self.subscription.body)
        self.assertEqual(out.tvm_client, self.subscription.tvm2_client_id)
        self.assertEqual(len(out.variables), 1)
        self.assertEqual(out.variables[0].id, str(self.var.variable_id))
        self.assertEqual(len(out.headers), 1)
        self.assertEqual(out.headers[0].name, 'header1')


class TestGetSurveyView_hook(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey, name='testit')
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        self.condition_node = SurveyHookConditionNodeFactory(hook=self.hook)

    def test_hooks_out(self):
        out = get_hooks_out(self.survey)

        self.assertEqual(len(out), 1)
        hook = out[0]
        self.assertEqual(hook.id, self.hook.pk)
        self.assertEqual(hook.active, self.hook.is_active)
        self.assertEqual(hook.name, 'testit')
        self.assertEqual(len(hook.conditions), 1)
        self.assertEqual(hook.conditions[0].id, self.condition_node.pk)
        self.assertTrue(len(hook.subscriptions), 1)
        email_out = hook.subscriptions[0].__root__
        self.assertTrue(email_out.id, self.subscription.pk)


class TestGetSurveyView_survey(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)

    def test_survey_out(self):
        out = get_survey_out(self.survey)

        self.assertEqual(out.id, self.survey.pk)
        self.assertEqual(out.name, self.survey.get_name())
        self.assertEqual(out.language, self.survey.language)
        self.assertEqual(out.is_published, self.survey.is_published_external)
        self.assertEqual(out.is_public, self.survey.is_public)
        self.assertEqual(out.dir_id, self.survey.org.dir_id)
        self.assertEqual(len(out.hooks), 1)
        self.assertEqual(out.hooks[0].id, self.hook.pk)

        out = get_survey_out(self.survey, fields=['name', 'is_published'])

        self.assertEqual(out.id, self.survey.pk)
        self.assertEqual(out.name, self.survey.get_name())
        self.assertTrue(out.language is None)
        self.assertEqual(out.is_published, self.survey.is_published_external)
        self.assertTrue(out.is_public is None)
        self.assertTrue(out.dir_id is None)
        self.assertTrue(out.hooks is None)

        out = get_survey_out(self.survey, fields=['hooks'])

        self.assertEqual(out.id, self.survey.pk)
        self.assertTrue(out.name is None)
        self.assertTrue(out.language is None)
        self.assertTrue(out.is_published is None)
        self.assertTrue(out.is_public is None)
        self.assertTrue(out.dir_id is None)
        self.assertEqual(len(out.hooks), 1)
        self.assertEqual(out.hooks[0].id, self.hook.pk)


class TestGetSurveyView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)

    def test_survey_request(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['id'], self.survey.pk)
        self.assertEqual(out['name'], self.survey.get_name())
        self.assertEqual(out['language'], self.survey.language)
        self.assertEqual(out['is_published'], self.survey.is_published_external)
        self.assertEqual(out['is_public'], self.survey.is_public)
        self.assertEqual(out['dir_id'], self.survey.org.dir_id)
        self.assertEqual(len(out['hooks']), 1)
        self.assertEqual(out['hooks'][0]['id'], self.hook.pk)

        response = self.client.get(f'/v3/surveys/{self.survey.pk}/', {'fields': 'name,is_published'})
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['id'], self.survey.pk)
        self.assertEqual(out['name'], self.survey.get_name())
        self.assertTrue('language' not in out)
        self.assertEqual(out['is_published'], self.survey.is_published_external)
        self.assertTrue('is_public' not in out)
        self.assertTrue('dir_id' not in out)
        self.assertTrue('hooks' not in out)

        response = self.client.get(f'/v3/surveys/{self.survey.pk}/', {'fields': 'hooks'})
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['id'], self.survey.pk)
        self.assertTrue('name' not in out)
        self.assertTrue('language' not in out)
        self.assertTrue('is_published' not in out)
        self.assertTrue('is_public' not in out)
        self.assertTrue('dir_id' not in out)
        self.assertEqual(len(out['hooks']), 1)
        self.assertEqual(out['hooks'][0]['id'], self.hook.pk)


class TestGetHooksView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hooks = [
            SurveyHookFactory(survey=self.survey),
            SurveyHookFactory(survey=self.survey),
        ]

    def test_should_return_list_of_objects(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, list))
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0]['id'], self.hooks[0].pk)
        self.assertEqual(out[1]['id'], self.hooks[1].pk)

    def test_should_return_empty_list_without_hooks(self):
        survey = SurveyFactory(org=self.o2g.org)
        response = self.client.get(f'/v3/surveys/{survey.pk}/hooks/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, list))
        self.assertEqual(len(out), 0)


class TestGetHookView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hooks = [
            SurveyHookFactory(survey=self.survey),
            SurveyHookFactory(survey=self.survey),
        ]

    def test_should_return_object(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{self.hooks[1].pk}/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, dict))
        self.assertEqual(out['id'], self.hooks[1].pk)

    def test_should_return_404_when_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/')
        self.assertEqual(response.status_code, 404)


class TestGetSubscriptionsView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscriptions = [
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
        ]

    def test_should_return_list_of_objects(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, list))
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0]['id'], self.subscriptions[0].pk)
        self.assertEqual(out[1]['id'], self.subscriptions[1].pk)

    def test_should_return_empty_list_when_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/subscriptions/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, list))
        self.assertEqual(len(out), 0)


class TestGetSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscriptions = [
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
        ]

    def test_should_return_object(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscriptions[1].pk}/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, dict))
        self.assertEqual(out['id'], self.subscriptions[1].pk)

    def test_should_return_404_when_subscription_from_another_survey(self):
        subscription = ServiceSurveyHookSubscriptionFactory()  # an object from another survey
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{subscription.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_when_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/subscriptions/{self.subscriptions[1].pk}/')
        self.assertEqual(response.status_code, 404)


class TestGetConditionsView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.conditions = [
            SurveyHookConditionNodeFactory(hook=self.hook),
            SurveyHookConditionNodeFactory(hook=self.hook),
        ]

    def test_should_return_list_of_objects(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, list))
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0]['id'], self.conditions[0].pk)
        self.assertEqual(out[1]['id'], self.conditions[1].pk)

    def test_should_return_empty_list_when_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/conditions/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, list))
        self.assertEqual(len(out), 0)


class TestGetConditionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.conditions = [
            SurveyHookConditionNodeFactory(hook=self.hook),
            SurveyHookConditionNodeFactory(hook=self.hook),
        ]

    def test_should_return_list_of_objects(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.conditions[1].pk}/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(isinstance(out, dict))
        self.assertEqual(out['id'], self.conditions[1].pk)

    def test_should_return_404_when_condition_from_another_survey(self):
        condition = SurveyHookConditionNodeFactory()  # an object from another survey
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{condition.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_when_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/conditions/{self.conditions[1].pk}/')
        self.assertEqual(response.status_code, 404)


class TestPostHookView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)

    def test_should_create_new_hook(self):
        data = {
            'name': 'testit',
            'active': True,
        }
        response = self.client.post(f'/v3/surveys/{self.survey.pk}/hooks/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['name'], 'testit')
        self.assertEqual(out['active'], True)

    def test_should_create_new_hook_not_active(self):
        data = {
            'name': 'testit',
        }
        response = self.client.post(f'/v3/surveys/{self.survey.pk}/hooks/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['name'], 'testit')
        self.assertEqual(out['active'], True)


class TestPatchHookView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey, name='', is_active=False)

    def test_should_modify_existing_hook(self):
        data = {
            'name': 'testit',
            'active': True,
        }
        response = self.client.patch(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['name'], 'testit')
        self.assertEqual(out['active'], True)

    def test_should_modify_existing_hook_change_name(self):
        data = {
            'name': 'testit',
        }
        response = self.client.patch(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['name'], 'testit')
        self.assertEqual(out['active'], False)

    def test_should_modify_existing_hook_change_active(self):
        data = {
            'active': True,
        }
        response = self.client.patch(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['name'], '')
        self.assertEqual(out['active'], True)

    def test_should_return_404_for_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        data = {
            'name': 'testit',
            'active': True,
        }
        response = self.client.patch(f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 404)


class TestDeleteHookView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey, name='', is_active=False)

    def test_should_delete_hook(self):
        response = self.client.delete(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.hooks.count(), 0)

        response = self.client.delete(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_for_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.delete(f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/')
        self.assertEqual(response.status_code, 404)


class TestPostConditionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.cta = [
            ContentTypeAttributeFactory(attr='answer_short_text'),
            ContentTypeAttributeFactory(attr='answer_date.date_start'),
            ContentTypeAttributeFactory(attr='answer_date.date_end'),
            ContentTypeAttributeFactory(attr='source_request', lookup_field='accept-language'),
            ContentTypeAttributeFactory(attr='source_request', lookup_field='parent_origin'),
            ContentTypeAttributeFactory(attr='user.groups'),
        ]

    def test_get_cta(self):
        cta = get_cta()
        self.assertEqual(cta['answer_short_text'], self.cta[0].pk)
        self.assertEqual(cta['answer_date.date_start'], self.cta[1].pk)
        self.assertEqual(cta['answer_date.date_end'], self.cta[2].pk)
        self.assertEqual(cta['language'], self.cta[3].pk)
        self.assertEqual(cta['origin'], self.cta[4].pk)

    def test_get_condition_items_obj_success(self):
        questions = [
            SurveyQuestionFactory(
                survey=self.survey, answer_type=AnswerType.objects.get(slug='answer_short_text'),
            ),
            SurveyQuestionFactory(
                survey=self.survey, answer_type=AnswerType.objects.get(slug='answer_date'),
            ),
        ]
        items = [
            ConditionItemIn.parse_obj({
                'type': 'question', 'question': questions[0].param_slug,
                'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }),
            ConditionItemIn.parse_obj({
                'type': 'question', 'question': questions[1].param_slug,
                'operator': 'and', 'condition': 'eq', 'value': '2021-12-31',
            }),
            ConditionItemIn.parse_obj({
                'type': 'language', 'operator': 'or', 'condition': 'neq', 'value': 'en',
            }),
            ConditionItemIn.parse_obj({
                'type': 'origin', 'operator': 'and', 'condition': 'neq', 'value': 'yandex.ru',
            }),
        ]
        condition_items = get_condition_items_obj(self.survey, items=items)
        self.assertEqual(len(condition_items), 4)

        self.assertEqual(condition_items[0].survey_question_id, questions[0].pk)
        self.assertEqual(condition_items[0].content_type_attribute_id, self.cta[0].pk)
        self.assertEqual(condition_items[0].operator, 'or')
        self.assertEqual(condition_items[0].condition, 'eq')
        self.assertEqual(condition_items[0].value, 'testit')
        self.assertEqual(condition_items[0].condition_node_id, None)
        self.assertEqual(condition_items[0].position, 1)

        self.assertEqual(condition_items[1].survey_question_id, questions[1].pk)
        self.assertEqual(condition_items[1].content_type_attribute_id, self.cta[1].pk)
        self.assertEqual(condition_items[1].operator, 'and')
        self.assertEqual(condition_items[1].condition, 'eq')
        self.assertEqual(condition_items[1].value, '2021-12-31')
        self.assertEqual(condition_items[1].condition_node_id, None)
        self.assertEqual(condition_items[1].position, 2)

        self.assertEqual(condition_items[2].survey_question_id, None)
        self.assertEqual(condition_items[2].content_type_attribute_id, self.cta[3].pk)
        self.assertEqual(condition_items[2].operator, 'or')
        self.assertEqual(condition_items[2].condition, 'neq')
        self.assertEqual(condition_items[2].value, 'en')
        self.assertEqual(condition_items[2].condition_node_id, None)
        self.assertEqual(condition_items[2].position, 3)

        self.assertEqual(condition_items[3].survey_question_id, None)
        self.assertEqual(condition_items[3].content_type_attribute_id, self.cta[4].pk)
        self.assertEqual(condition_items[3].operator, 'and')
        self.assertEqual(condition_items[3].condition, 'neq')
        self.assertEqual(condition_items[3].value, 'yandex.ru')
        self.assertEqual(condition_items[3].condition_node_id, None)
        self.assertEqual(condition_items[3].position, 4)

    def test_get_condition_items_obj_success_with_condition_id(self):
        condition = SurveyHookConditionNodeFactory(hook=self.hook)
        items = [
            ConditionItemIn.parse_obj({
                'type': 'language', 'operator': 'or', 'condition': 'eq', 'value': 'en',
            }),
        ]
        condition_items = get_condition_items_obj(self.survey, condition_id=condition.pk, items=items)
        self.assertEqual(len(condition_items), 1)

        self.assertEqual(condition_items[0].survey_question_id, None)
        self.assertEqual(condition_items[0].content_type_attribute_id, self.cta[3].pk)
        self.assertEqual(condition_items[0].operator, 'or')
        self.assertEqual(condition_items[0].condition, 'eq')
        self.assertEqual(condition_items[0].value, 'en')
        self.assertEqual(condition_items[0].condition_node_id, condition.pk)
        self.assertEqual(condition_items[0].position, 1)

    def test_get_condition_items_obj_error_question_slug_required(self):
        items = [
            ConditionItemIn.parse_obj({
                'type': 'question', 'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }),
        ]
        with self.assertRaises(ApiValidationError):
            get_condition_items_obj(self.survey, items=items)

    def test_get_condition_items_obj_error_unknown_question_slug(self):
        question = SurveyQuestionFactory()  # an object from another survey
        items = [
            ConditionItemIn.parse_obj({
                'type': 'question', 'question': question.param_slug,
                'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }),
        ]
        with self.assertRaises(ApiValidationError):
            get_condition_items_obj(self.survey, items=items)

    def test_get_condition_items_obj_error_unsupported_question_type(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_url'),
        )
        items = [
            ConditionItemIn.parse_obj({
                'type': 'question', 'question': question.param_slug,
                'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }),
        ]
        with self.assertRaises(ApiValidationError):
            get_condition_items_obj(self.survey, items=items)

    def test_get_condition_items_obj_error_unknown_condition_type(self):
        with self.assertRaises(ValidationError):
            ConditionItemIn.parse_obj({
                'type': 'group', 'operator': 'or', 'condition': 'eq', 'value': 'manager',
            })

    def test_get_condition_items_obj_error_unknown_operator_type(self):
        with self.assertRaises(ValidationError):
            ConditionItemIn.parse_obj({
                'type': 'language', 'operator': 'xor', 'condition': 'eq', 'value': 'en',
            })

    def test_should_create_new_condition(self):
        questions = [
            SurveyQuestionFactory(
                survey=self.survey, answer_type=AnswerType.objects.get(slug='answer_short_text'),
            ),
            SurveyQuestionFactory(
                survey=self.survey, answer_type=AnswerType.objects.get(slug='answer_date'),
            ),
        ]
        data = {
            'items': [
                {
                    'type': 'question', 'question': questions[0].param_slug,
                    'operator': 'or', 'condition': 'eq', 'value': 'testit',
                },
                {
                    'type': 'question', 'question': questions[1].param_slug,
                    'operator': 'and', 'condition': 'eq', 'value': '2021-12-31',
                },
                {
                    'type': 'language', 'operator': 'or', 'condition': 'neq', 'value': 'en',
                },
                {
                    'type': 'origin', 'operator': 'and', 'condition': 'neq', 'value': 'yandex.ru',
                },
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(out['id'] is not None)
        condition_items = out['items']
        self.assertEqual(len(condition_items), 4)

        self.assertEqual(condition_items[0]['question'], questions[0].param_slug)
        self.assertEqual(condition_items[0]['type'], 'question')
        self.assertEqual(condition_items[0]['operator'], 'or')
        self.assertEqual(condition_items[0]['condition'], 'eq')
        self.assertEqual(condition_items[0]['value'], 'testit')

        self.assertEqual(condition_items[1]['question'], questions[1].param_slug)
        self.assertEqual(condition_items[1]['type'], 'question')
        self.assertEqual(condition_items[1]['operator'], 'and')
        self.assertEqual(condition_items[1]['condition'], 'eq')
        self.assertEqual(condition_items[1]['value'], '2021-12-31')

        self.assertTrue('question' not in condition_items[2])
        self.assertEqual(condition_items[2]['type'], 'language')
        self.assertEqual(condition_items[2]['operator'], 'or')
        self.assertEqual(condition_items[2]['condition'], 'neq')
        self.assertEqual(condition_items[2]['value'], 'en')

        self.assertTrue('question' not in condition_items[3])
        self.assertEqual(condition_items[3]['type'], 'origin')
        self.assertEqual(condition_items[3]['operator'], 'and')
        self.assertEqual(condition_items[3]['condition'], 'neq')
        self.assertEqual(condition_items[3]['value'], 'yandex.ru')

    def test_should_return_error_question_slug_required(self):
        data = {
            'items': [{
                'type': 'question', 'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['items', 'question'])

    def test_should_return_error_unknown_question_slug(self):
        question = SurveyQuestionFactory()  # an object from another survey
        data = {
            'items': [{
                'type': 'question', 'question': question.param_slug,
                'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['items', 'question'])

    def test_should_return_error_unsupported_question_type(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_url'),
        )
        data = {
            'items': [{
                'type': 'question', 'question': question.param_slug,
                'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['items', 'type'])

    def test_should_return_error_unknown_condition_type(self):
        data = {
            'items': [{
                'type': 'group', 'operator': 'or', 'condition': 'eq', 'value': 'manager',
            }],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'type_error.enum')
        self.assertEqual(result[0]['loc'], ['items', 0, 'type'])

    def test_should_return_error_unknown_operator_type(self):
        data = {
            'items': [{
                'type': 'language', 'operator': 'xor', 'condition': 'eq', 'value': 'en',
            }],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'type_error.enum')
        self.assertEqual(result[0]['loc'], ['items', 0, 'operator'])

    def test_should_return_404_for_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        data = {}
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/conditions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 404)


class TestPatchConditionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.condition = SurveyHookConditionNodeFactory(hook=self.hook)
        self.cta = [
            ContentTypeAttributeFactory(attr='answer_short_text'),
            ContentTypeAttributeFactory(attr='answer_date.date_start'),
            ContentTypeAttributeFactory(attr='answer_date.date_end'),
            ContentTypeAttributeFactory(attr='source_request', lookup_field='accept-language'),
            ContentTypeAttributeFactory(attr='source_request', lookup_field='parent_origin'),
            ContentTypeAttributeFactory(attr='user.groups'),
        ]

    def test_should_create_new_condition(self):
        questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_date'),
            ),
        ]
        existing_items = [
            SurveyHookConditionFactory(
                condition_node=self.condition, content_type_attribute=self.cta[0],
                survey_question_id=questions[0].pk,
                operator='or', condition='eq', value='testit', position=1,
            ),
            SurveyHookConditionFactory(
                condition_node=self.condition, content_type_attribute=self.cta[1],
                survey_question_id=questions[1].pk,
                operator='and', condition='eq', value='2021-12-31', position=2,
            ),
            SurveyHookConditionFactory(
                condition_node=self.condition, content_type_attribute=self.cta[3],
                operator='or', condition='neq', value='en', position=3,
            ),
            SurveyHookConditionFactory(
                condition_node=self.condition, content_type_attribute=self.cta[4],
                operator='and', condition='neq', value='yandex.ru', position=4,
            ),
        ]

        data = {
            'items': [
                {
                    'type': 'question', 'question': questions[1].param_slug,
                    'operator': 'or', 'condition': 'eq', 'value': '2022-01-01',
                },
                {
                    'type': 'question', 'question': questions[0].param_slug,
                    'operator': 'and', 'condition': 'eq', 'value': 'testthat',
                },
                {
                    'type': 'origin', 'operator': 'or', 'condition': 'neq', 'value': 'yandex.com',
                },
                {
                    'type': 'language', 'operator': 'and', 'condition': 'neq', 'value': 'ru',
                },
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertTrue(out['id'] is not None)
        condition_items = out['items']
        self.assertEqual(len(condition_items), 4)

        self.assertEqual(condition_items[0]['question'], questions[1].param_slug)
        self.assertEqual(condition_items[0]['type'], 'question')
        self.assertEqual(condition_items[0]['operator'], 'or')
        self.assertEqual(condition_items[0]['condition'], 'eq')
        self.assertEqual(condition_items[0]['value'], '2022-01-01')

        self.assertEqual(condition_items[1]['question'], questions[0].param_slug)
        self.assertEqual(condition_items[1]['type'], 'question')
        self.assertEqual(condition_items[1]['operator'], 'and')
        self.assertEqual(condition_items[1]['condition'], 'eq')
        self.assertEqual(condition_items[1]['value'], 'testthat')

        self.assertTrue('question' not in condition_items[2])
        self.assertEqual(condition_items[2]['type'], 'origin')
        self.assertEqual(condition_items[2]['operator'], 'or')
        self.assertEqual(condition_items[2]['condition'], 'neq')
        self.assertEqual(condition_items[2]['value'], 'yandex.com')

        self.assertTrue('question' not in condition_items[3])
        self.assertEqual(condition_items[3]['type'], 'language')
        self.assertEqual(condition_items[3]['operator'], 'and')
        self.assertEqual(condition_items[3]['condition'], 'neq')
        self.assertEqual(condition_items[3]['value'], 'ru')

        self.condition.refresh_from_db()
        new_pks = set(self.condition.items.values_list('pk', flat=True))
        existing_pks = set(it.pk for it in existing_items)
        self.assertEqual(len(new_pks & existing_pks), 0)

    def test_should_return_error_question_slug_required(self):
        data = {
            'items': [{
                'type': 'question', 'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['items', 'question'])

    def test_should_return_error_unknown_question_slug(self):
        question = SurveyQuestionFactory()  # an object from another survey
        data = {
            'items': [{
                'type': 'question', 'question': question.param_slug,
                'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['items', 'question'])

    def test_should_return_error_unsupported_question_type(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_url'),
        )
        data = {
            'items': [{
                'type': 'question', 'question': question.param_slug,
                'operator': 'or', 'condition': 'eq', 'value': 'testit',
            }],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['items', 'type'])

    def test_should_return_error_unknown_condition_type(self):
        data = {
            'items': [{
                'type': 'group', 'operator': 'or', 'condition': 'eq', 'value': 'manager',
            }],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'type_error.enum')
        self.assertEqual(result[0]['loc'], ['items', 0, 'type'])

    def test_should_return_error_unknown_operator_type(self):
        data = {
            'items': [{
                'type': 'language', 'operator': 'xor', 'condition': 'eq', 'value': 'en',
            }],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'type_error.enum')
        self.assertEqual(result[0]['loc'], ['items', 0, 'operator'])

    def test_should_return_404_for_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        data = {}
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/conditions/{self.condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_for_condition_from_another_survey(self):
        condition = SurveyHookConditionNodeFactory()  # an object from another survey
        data = {}
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{condition.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 404)


class TestDeleteConditionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey, name='', is_active=False)
        self.condition = SurveyHookConditionNodeFactory(hook=self.hook)

    def test_should_delete_condition(self):
        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
        )
        self.assertEqual(response.status_code, 200)

        self.hook.refresh_from_db()
        self.assertEqual(self.hook.condition_nodes.count(), 0)

        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{self.condition.pk}/',
        )
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_for_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/conditions/{self.condition.pk}/',
        )
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_for_condition_from_another_survey(self):
        condition = SurveyHookConditionNodeFactory()  # an object from another survey
        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/conditions/{condition.pk}/',
        )
        self.assertEqual(response.status_code, 404)


class TestDeleteSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey, name='', is_active=False)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)

    def test_should_delete_subscription(self):
        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
        )
        self.assertEqual(response.status_code, 200)

        self.hook.refresh_from_db()
        self.assertEqual(self.hook.subscriptions.count(), 0)

        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
        )
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_for_hook_from_another_survey(self):
        hook = SurveyHookFactory()  # an object from another survey
        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{hook.pk}/subscriptions/{self.subscription.pk}/',
        )
        self.assertEqual(response.status_code, 404)

    def test_should_return_404_for_subscription_from_another_survey(self):
        subscription = ServiceSurveyHookSubscriptionFactory()  # an object from another survey
        response = self.client.delete(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{subscription.pk}/',
        )
        self.assertEqual(response.status_code, 404)


class TestPostEmailSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)

    def test_should_create_new_subscription(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'email_from_address': 'user2@example.com',
            'email_from_title': 'title1',
            'email_spam_check': True,
            'subject': 'subject1',
            'body': 'body1',
            'active': False,
            'follow': True,
            'language': 'en',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['email_to_address'], data['email_to_address'])
        self.assertEqual(out['email_from_address'], data['email_from_address'])
        self.assertEqual(out['email_from_title'], data['email_from_title'])
        self.assertEqual(out['email_spam_check'], data['email_spam_check'])
        self.assertEqual(out['subject'], data['subject'])
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])

    def test_should_create_new_subscription_only_required_fields(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['email_to_address'], data['email_to_address'])
        self.assertEqual(out['email_from_address'], f'{self.survey.pk}@forms.yandex.ru')
        self.assertTrue('email_from_title' not in out)
        self.assertEqual(out['email_spam_check'], False)
        self.assertEqual(out['subject'], '')
        self.assertEqual(out['body'], '')
        self.assertEqual(out['active'], True)
        self.assertEqual(out['follow'], False)
        self.assertEqual(out['language'], 'from_request')

    def test_should_create_new_subscription_with_headers_intranet(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'headers': [
                {'name': 'header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['headers'], data['headers'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_new_subscription_with_headers_business(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'headers': [
                {'name': 'reply-to', 'value': 'user@yandex.ru', 'only_with_value': True},
                {'name': 'x-header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['headers'], data['headers'])

    def test_should_create_new_subscription_with_variables(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
                {'id': VariableId.create(), 'type': 'form.questions_answers',
                    'renderer': 'txt', 'questions': {'all': True}},
                {'id': VariableId.create(), 'type': 'request.query_param', 'name': 'survey'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['variables'], data['variables'])

    def test_should_create_new_subscription_with_attachments(self):
        meta = ProxyStorageModelFactory(path='/1/testit.txt')
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'attachments': {
                'question': {'items': [question.param_slug]},
                'static': [{'path': meta.path}],
            },
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['attachments']['question']['all'], False)
        self.assertEqual(out['attachments']['question']['items'], data['attachments']['question']['items'])
        self.assertEqual(len(out['attachments']['static']), 1)
        self.assertEqual(out['attachments']['static'][0]['links']['orig'], get_mds_url(meta.path))

    def test_should_create_new_subscription_with_all_question_attachments(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'attachments': {
                'question': {'all': True, 'items': [question.param_slug]},
            },
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['attachments']['question']['all'], True)
        self.assertTrue('items' not in out['attachments']['question'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_subscription_with_b2b_defaults(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'email_from_address': 'user2@example.com',
            'email_spam_check': False,
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['email_to_address'], data['email_to_address'])
        self.assertTrue('email_from_address' not in out)
        self.assertTrue('email_spam_check' not in out)

        self.hook.refresh_from_db()
        subscription = self.hook.subscriptions.all()[0]
        self.assertEqual(subscription.email_from_address, f'{self.survey.pk}@forms-mailer.yaconnect.com')
        self.assertEqual(subscription.email_spam_check, True)

    def test_should_return_400_without_email_to_address(self):
        data = {
            'type': 'email',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['email_to_address'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_question_not_found_1(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'variables': [
                {
                    'id': VariableId.create(), 'type': 'form.question_answer',
                    'question': question.param_slug,
                },
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'question'])

    def test_should_return_400_if_question_not_found_2(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'variables': [
                {
                    'id': VariableId.create(), 'type': 'form.questions_answers',
                    'questions': {'items': [question.param_slug]},
                },
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'questions', 'items', 0])

    def test_should_return_400_if_renderer_from_another_variable(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'variables': [
                {
                    'id': VariableId.create(), 'type': 'form.name', 'renderer': 'txt',
                },
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'renderer'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_400_with_not_valid_headers(self):
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'headers': [
                {'name': 'reply-to', 'value': 'user@yandex.ru', 'only_with_value': True},
                {'name': 'header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['headers', 1, 'name'])

    def test_should_return_400_if_attachment_question_does_not_exist(self):
        question = SurveyQuestionFactory(answer_type=AnswerType.objects.get(slug='answer_files'))
        data = {
            'type': 'email',
            'email_to_address': 'user1@example.com',
            'attachments': {
                'question': {'items': [question.param_slug]},
            },
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['attachments', 'question', 'items', 0])


class TestPatchEmailSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=ActionType.email,
            email_to_address='user1@example.com',
            email_from_address='user2@example.com',
            email_from_title='title1',
            email_spam_check=True,
            title='subject1',
            body='body1',
            is_active=False,
            follow_result=True,
            context_language='en',
            is_all_questions=False,
        )

    def test_should_modify_existing_subscription(self):
        data = {
            'type': 'email',
            'email_to_address': 'user12@example.com',
            'email_from_address': 'user22@example.com',
            'email_from_title': 'title12',
            'email_spam_check': False,
            'subject': 'subject12',
            'body': 'body12',
            'active': True,
            'follow': False,
            'language': 'de',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['email_to_address'], data['email_to_address'])
        self.assertEqual(out['email_from_address'], data['email_from_address'])
        self.assertEqual(out['email_from_title'], data['email_from_title'])
        self.assertEqual(out['email_spam_check'], data['email_spam_check'])
        self.assertEqual(out['subject'], data['subject'])
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        self.assertTrue('attachments' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_shouldnt_modify_existing_subscription(self):
        data = {
            'type': 'email',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['email_to_address'], self.subscription.email_to_address)
        self.assertEqual(out['email_from_address'], self.subscription.email_from_address)
        self.assertEqual(out['email_from_title'], self.subscription.email_from_title)
        self.assertEqual(out['email_spam_check'], self.subscription.email_spam_check)
        self.assertEqual(out['subject'], self.subscription.title)
        self.assertEqual(out['body'], self.subscription.body)
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        self.assertTrue('attachments' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

    def test_should_modify_subscription_with_headers_intranet(self):
        header = SubscriptionHeaderFactory(
            subscription=self.subscription,
            name='header1', value='value1', add_only_with_value=False,
        )
        data = {
            'type': 'email',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['headers'], [
            {'name': header.name, 'value': header.value, 'only_with_value': header.add_only_with_value},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'email',
            'headers': [
                {'name': 'header12', 'value': 'value12', 'only_with_value': True},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['headers'], data['headers'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'email',
            'headers': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertTrue('headers' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_modify_subscription_with_headers_business(self):
        header = SubscriptionHeaderFactory(
            subscription=self.subscription,
            name='x-header1', value='value1', add_only_with_value=False,
        )
        data = {
            'type': 'email',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['headers'], [
            {'name': header.name, 'value': header.value, 'only_with_value': header.add_only_with_value},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'email',
            'headers': [
                {'name': 'x-header12', 'value': 'value12', 'only_with_value': True},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['headers'], data['headers'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'email',
            'headers': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertTrue('headers' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_variables(self):
        var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        data = {
            'type': 'email',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['variables'], [
            {'id': str(var.variable_id), 'type': var.var},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'email',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.name'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'email',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'email',
            'variables': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertTrue('variables' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_attachments(self):
        meta = ProxyStorageModelFactory(path='/1/testit.txt')
        SubscriptionAttachmentFactory(subscription=self.subscription, file=meta.path)
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        self.subscription.questions.add(question)
        data = {
            'type': 'email',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['attachments']['question']['all'], False)
        self.assertEqual(out['attachments']['question']['items'], [question.param_slug])
        self.assertEqual(len(out['attachments']['static']), 1)
        self.assertEqual(out['attachments']['static'][0]['links']['orig'], get_mds_url(meta.path))

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        another_meta = ProxyStorageModelFactory(path='/2/testthat.txt')
        another_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        data = {
            'type': 'email',
            'attachments': {
                'question': {'items': [another_question.param_slug]},
                'static': [{'path': another_meta.path}],
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['attachments']['question']['all'], False)
        self.assertEqual(out['attachments']['question']['items'], data['attachments']['question']['items'])
        self.assertEqual(len(out['attachments']['static']), 1)
        self.assertEqual(out['attachments']['static'][0]['links']['orig'], get_mds_url(another_meta.path))

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'email',
            'attachments': {
                'question': {'all': True},
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertEqual(out['attachments']['question']['all'], True)
        self.assertTrue('items' not in out['attachments']['question'])
        self.assertTrue('static' in out['attachments'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'email',
            'attachments': {
                'question': {},
                'static': [],
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'email')
        self.assertTrue('attachments' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_return_400_with_empty_email_to_address(self):
        data = {
            'type': 'email',
            'email_to_address': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['email_to_address'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'email',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_question_not_found_1(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'email',
            'variables': [
                {
                    'id': VariableId.create(), 'type': 'form.question_answer',
                    'question': question.param_slug,
                },
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'question'])

    def test_should_return_400_if_question_not_found_2(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'email',
            'variables': [
                {
                    'id': VariableId.create(), 'type': 'form.questions_answers',
                    'questions': {'items': [question.param_slug]},
                },
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'questions', 'items', 0])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_400_with_not_valid_headers(self):
        data = {
            'type': 'email',
            'headers': [
                {'name': 'reply-to', 'value': 'user@yandex.ru', 'only_with_value': True},
                {'name': 'header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['headers', 1, 'name'])

    def test_should_return_400_if_type_was_changed(self):
        data = {
            'type': 'http',
            'headers': [
                {'name': 'from', 'value': 'admin@yandex.ru'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['type'])

    def test_should_return_400_if_attachment_question_does_not_exist(self):
        question = SurveyQuestionFactory(answer_type=AnswerType.objects.get(slug='answer_files'))
        data = {
            'type': 'email',
            'attachments': {
                'question': {'items': [question.param_slug]},
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['attachments', 'question', 'items', 0])


class TestPostWikiSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)

    def test_should_create_new_subscription(self):
        data = {
            'type': 'wiki',
            'supertag': 'https://wiki.yandex-team.ru/user/testit/#first',
            'body': 'body1',
            'active': False,
            'follow': True,
            'language': 'en',
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            mock_check_supertag.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['supertag'], 'user/testit#first')
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        mock_check_supertag.assert_called_once_with(out['supertag'], self.survey.org.dir_id)

    def test_should_create_new_subscription_only_required_fields(self):
        data = {
            'type': 'wiki',
            'supertag': '/user/testit/#first',
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            mock_check_supertag.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['supertag'], 'user/testit#first')
        self.assertEqual(out['body'], '')
        self.assertEqual(out['active'], True)
        self.assertEqual(out['follow'], False)
        self.assertEqual(out['language'], 'from_request')
        mock_check_supertag.assert_called_once_with(out['supertag'], self.survey.org.dir_id)

    def test_should_create_new_subscription_with_variables(self):
        data = {
            'type': 'wiki',
            'supertag': '/user/testit/',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            mock_check_supertag.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['supertag'], 'user/testit')
        self.assertEqual(out['variables'], data['variables'])
        mock_check_supertag.assert_called_once_with(out['supertag'], self.survey.org.dir_id)

    def test_should_return_400_without_supertag(self):
        data = {
            'type': 'wiki',
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_check_supertag.assert_not_called()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['supertag'])

    def test_should_return_400_when_supertag_does_not_exist(self):
        data = {
            'type': 'wiki',
            'supertag': '/user/testit/',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            mock_check_supertag.return_value = False
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_check_supertag.assert_called_once_with('user/testit', self.survey.org.dir_id)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['supertag'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'wiki',
            'supertag': '/user/testit/',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            mock_check_supertag.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_check_supertag.assert_called_once_with('user/testit', self.survey.org.dir_id)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])


class TestPatchWikiSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=ActionType.wiki,
            is_active=False,
            follow_result=True,
            context_language='en',
            is_all_questions=False,
        )
        self.wiki = WikiSubscriptionDataFactory(
            subscription=self.subscription,
            supertag='user/testit#first',
            text='body1',
        )

    def test_should_modify_existing_subscription(self):
        data = {
            'type': 'wiki',
            'supertag': 'https://wiki.yandex-team.ru/user/testit/#second',
            'body': 'body12',
            'active': True,
            'follow': False,
            'language': 'de',
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            mock_check_supertag.return_value = True
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['supertag'], 'user/testit#second')
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertTrue('variables' not in out)
        mock_check_supertag.assert_called_once_with('user/testit#second', self.survey.org.dir_id)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_shouldnt_modify_existing_subscription(self):
        data = {
            'type': 'wiki',
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['supertag'], self.wiki.supertag)
        self.assertEqual(out['body'], self.wiki.text)
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertTrue('variables' not in out)
        mock_check_supertag.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

    def test_should_modify_subscription_with_variables(self):
        var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        data = {
            'type': 'wiki',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['variables'], [
            {'id': str(var.variable_id), 'type': var.var},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'wiki',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.name'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'wiki',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'wiki')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'wiki',
            'variables': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'wiki')
        self.assertTrue('variables' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_return_400_with_empty_supertag(self):
        data = {
            'type': 'wiki',
            'supertag': '',
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_check_supertag.assert_not_called()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['supertag'])

    def test_should_return_400_when_supertag_does_not_exist(self):
        data = {
            'type': 'wiki',
            'supertag': '/user/testit/',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            mock_check_supertag.return_value = False
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_check_supertag.assert_called_once_with('user/testit', self.survey.org.dir_id)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['supertag'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'wiki',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        with patch('events.v3.views.subscriptions.check_if_supertag_exist') as mock_check_supertag:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_check_supertag.assert_not_called()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])


class TestPostTrackerSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)

    def test_should_create_new_subscription(self):
        field_data = {
            'key': {'slug': 'tags', 'name': 'Tags', 'type': 'array/string'},
            'value': 'one', 'add_only_with_value': True,
        }
        data = {
            'type': 'tracker',
            'subject': 'subject1',
            'body': 'body1',
            'active': False,
            'follow': True,
            'language': 'en',
            'issue_type': 2,
            'priority': 2,
            'queue': 'FORMS',
            'parent': 'TEST-12',
            'author': 'user1',
            'assignee': 'user2',
            'fields': [{
                'key': field_data['key']['slug'],
                'value': field_data['value'],
                'only_with_value': field_data['add_only_with_value']
            }],
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_parent') as mock_validate_parent,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
            patch.object(TrackerValidator, 'validate_fields') as mock_validate_fields,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_parent.return_value = data['parent']
            mock_validate_issue_type.return_value = data['issue_type']
            mock_validate_priority.return_value = data['priority']
            mock_validate_fields.return_value = [field_data]
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['subject'], data['subject'])
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['issue_type'], data['issue_type'])
        self.assertEqual(out['priority'], data['priority'])
        self.assertEqual(out['queue'], data['queue'])
        self.assertEqual(out['parent'], data['parent'])
        self.assertEqual(out['author'], data['author'])
        self.assertEqual(out['assignee'], data['assignee'])
        self.assertEqual(out['fields'], [{
            'key': field_data['key'],
            'value': field_data['value'],
            'only_with_value': field_data['add_only_with_value'],
        }])
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_parent.assert_called_once_with(data['parent'])
        mock_validate_issue_type.assert_called_once_with(data['issue_type'])
        mock_validate_priority.assert_called_once_with(data['priority'])
        mock_validate_fields.assert_called_once_with([TrackerFieldIn.parse_obj(data['fields'][0])])

    def test_should_create_new_subscription_fields_key_object(self):
        field_data = {
            'key': {'slug': 'tags', 'name': 'Tags', 'type': 'array/string'},
            'value': 'one', 'add_only_with_value': True,
        }
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'issue_type': 2,
            'priority': 2,
            'fields': [{
                'key': field_data['key'],
                'value': field_data['value'],
                'only_with_value': field_data['add_only_with_value']
            }],
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_parent') as mock_validate_parent,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
            patch.object(TrackerValidator, 'validate_fields') as mock_validate_fields,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_parent.return_value = None
            mock_validate_issue_type.return_value = data['issue_type']
            mock_validate_priority.return_value = data['priority']
            mock_validate_fields.return_value = [field_data]
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['issue_type'], data['issue_type'])
        self.assertEqual(out['priority'], data['priority'])
        self.assertEqual(out['queue'], data['queue'])
        self.assertEqual(out['fields'], [{
            'key': field_data['key'],
            'value': field_data['value'],
            'only_with_value': field_data['add_only_with_value'],
        }])
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_parent.assert_not_called()
        mock_validate_issue_type.assert_called_once_with(data['issue_type'])
        mock_validate_priority.assert_called_once_with(data['priority'])
        mock_validate_fields.assert_called_once_with([TrackerFieldIn.parse_obj(data['fields'][0])])

    def test_should_create_new_subscription_only_required_fields(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_parent') as mock_validate_parent,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
            patch.object(TrackerValidator, 'validate_fields') as mock_validate_fields,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_issue_type.return_value = 2
            mock_validate_priority.return_value = 2
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['subject'], 'testit')
        self.assertEqual(out['body'], '')
        self.assertEqual(out['active'], True)
        self.assertEqual(out['follow'], False)
        self.assertEqual(out['language'], 'from_request')
        self.assertEqual(out['issue_type'], 2)
        self.assertEqual(out['priority'], 2)
        self.assertEqual(out['queue'], data['queue'])
        self.assertTrue('parent' not in out)
        self.assertTrue('author' not in out)
        self.assertTrue('assignee' not in out)
        self.assertTrue('fields' not in out)
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_parent.assert_not_called()
        mock_validate_issue_type.assert_called_once_with(0)
        mock_validate_priority.assert_called_once_with(0)
        mock_validate_fields.assert_not_called()

    def test_should_create_new_subscription_with_variables(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_issue_type.return_value = 2
            mock_validate_priority.return_value = 2
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['variables'], data['variables'])
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_issue_type.assert_called_once_with(0)
        mock_validate_priority.assert_called_once_with(0)

    def test_should_create_new_subscription_with_attachments(self):
        meta = ProxyStorageModelFactory(path='/1/testit.txt')
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'attachments': {
                'question': {'items': [question.param_slug]},
                'static': [{'path': meta.path}],
            },
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_issue_type.return_value = 2
            mock_validate_priority.return_value = 2
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['attachments']['question']['all'], False)
        self.assertEqual(out['attachments']['question']['items'], data['attachments']['question']['items'])
        self.assertEqual(len(out['attachments']['static']), 1)
        self.assertEqual(out['attachments']['static'][0]['links']['orig'], get_mds_url(meta.path))
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_issue_type.assert_called_once_with(0)
        mock_validate_priority.assert_called_once_with(0)

    def test_should_create_new_subscription_with_all_question_attachments(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'attachments': {
                'question': {'all': True, 'items': [question.param_slug]},
            },
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_issue_type.return_value = 2
            mock_validate_priority.return_value = 2
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['attachments']['question']['all'], True)
        self.assertTrue('items' not in out['attachments']['question'])
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_issue_type.assert_called_once_with(0)
        mock_validate_priority.assert_called_once_with(0)

    def test_should_return_400_without_subject(self):
        data = {
            'type': 'tracker',
            'subject': '',
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
        ):
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_validate_queue.assert_not_called()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['subject'])

    def test_should_return_400_without_queue_and_parent(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
        ):
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_validate_queue.assert_not_called()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['queue'])
        self.assertEqual(result[1]['error_code'], 'value_error.missing')
        self.assertEqual(result[1]['loc'], ['parent'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_issue_type.return_value = 2
            mock_validate_priority.return_value = 2
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)

        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_issue_type.assert_called_once_with(0)
        mock_validate_priority.assert_called_once_with(0)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    @override_cache_settings()
    def test_should_return_400_if_queue_does_not_exist(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMZ',
        }
        with patch.object(StartrekClient, 'get_queue') as mock_get_queue:
            mock_get_queue.return_value = None
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_queue.assert_called_once_with(data['queue'])
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['queue'])

    @override_cache_settings()
    def test_should_return_400_if_parent_does_not_exist(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'parent': 'FORMS-999',
        }
        with patch.object(StartrekClient, 'get_issue') as mock_get_issue:
            mock_get_issue.return_value = None
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_issue.assert_called_once_with(data['parent'])
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['parent'])

    @override_cache_settings()
    def test_should_return_400_if_issue_type_does_not_exist(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'issue_type': 3,
        }
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
        ):
            mock_get_queue.return_value = {'key': 'FORMS'}
            mock_get_issuetypes.return_value = [{'id': 1}, {'id': 2}]
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_queue.assert_called_once_with(data['queue'])
        mock_get_issuetypes.assert_called_once_with(data['queue'])
        mock_get_priorities.assert_not_called()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['issue_type'])

    @override_cache_settings()
    def test_should_return_400_if_priority_does_not_exist(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'issue_type': 2,
            'priority': 3,
        }
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
        ):
            mock_get_queue.return_value = {'key': 'FORMS'}
            mock_get_issuetypes.return_value = [{'id': 1}, {'id': 2}]
            mock_get_priorities.return_value = [{'id': 1}, {'id': 2}]
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_queue.assert_called_once_with(data['queue'])
        mock_get_issuetypes.assert_called_once_with(data['queue'])
        mock_get_priorities.assert_called_once_with()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['priority'])

    @override_cache_settings()
    def test_should_return_400_if_field_does_not_exist(self):
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'issue_type': 2,
            'priority': 2,
            'fields': [{'key': 'tag', 'value': '1'}],
        }
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
            patch.object(StartrekClient, 'get_field') as mock_get_field,
        ):
            mock_get_queue.return_value = {'key': 'FORMS'}
            mock_get_issuetypes.return_value = [{'id': 1}, {'id': 2}]
            mock_get_priorities.return_value = [{'id': 1}, {'id': 2}]
            mock_get_field.return_value = None
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_queue.assert_called_once_with(data['queue'])
        mock_get_issuetypes.assert_called_once_with(data['queue'])
        mock_get_priorities.assert_called_once_with()
        mock_get_field.assert_called_once_with(data['fields'][0]['key'])
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['fields', 0, 'key'])

    @override_cache_settings()
    def test_should_return_400_if_attachment_question_does_not_exist(self):
        question = SurveyQuestionFactory(answer_type=AnswerType.objects.get(slug='answer_files'))
        data = {
            'type': 'tracker',
            'subject': 'testit',
            'queue': 'FORMS',
            'attachments': {
                'question': {'items': [question.param_slug]},
            },
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_issue_type.return_value = 2
            mock_validate_priority.return_value = 2
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_issue_type.assert_called_once_with(0)
        mock_validate_priority.assert_called_once_with(0)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['attachments', 'question', 'items', 0])


class TestPatchTrackerSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=ActionType.tracker,
            title='subject1',
            body='body1',
            is_active=False,
            follow_result=True,
            context_language='en',
            is_all_questions=False,
        )
        self.tracker = StartrekSubscriptionDataFactory(
            subscription=self.subscription,
            type=2, priority=2,
            queue='FORMS', parent='TEST-12',
            author='user1', assignee='user2',
            fields=[
                {
                    'key': {'slug': 'tags', 'name': 'Tags', 'type': 'array/string'},
                    'value': 'one', 'add_only_with_value': True,
                },
            ],
        )

    def test_should_modify_existing_subscription(self):
        field_data = {
            'key': {'slug': 'components', 'name': 'Components', 'type': 'array/string'},
            'value': 'dev', 'add_only_with_value': False,
        }
        data = {
            'type': 'tracker',
            'subject': 'subject12',
            'body': 'body12',
            'active': True,
            'follow': False,
            'language': 'de',
            'issue_type': 1,
            'priority': 1,
            'queue': 'TEST',
            'parent': 'FORMS-12',
            'author': 'user12',
            'assignee': 'user22',
            'fields': [{
                'key': field_data['key']['slug'],
                'value': field_data['value'],
                'only_with_value': field_data['add_only_with_value']
            }],
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_parent') as mock_validate_parent,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
            patch.object(TrackerValidator, 'validate_fields') as mock_validate_fields,
        ):
            mock_validate_queue.return_value = data['queue']
            mock_validate_parent.return_value = data['parent']
            mock_validate_issue_type.return_value = data['issue_type']
            mock_validate_priority.return_value = data['priority']
            mock_validate_fields.return_value = [field_data]
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['subject'], data['subject'])
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['issue_type'], data['issue_type'])
        self.assertEqual(out['priority'], data['priority'])
        self.assertEqual(out['queue'], data['queue'])
        self.assertEqual(out['parent'], data['parent'])
        self.assertEqual(out['author'], data['author'])
        self.assertEqual(out['assignee'], data['assignee'])
        self.assertEqual(out['fields'], [{
            'key': field_data['key'],
            'value': field_data['value'],
            'only_with_value': field_data['add_only_with_value'],
        }])
        self.assertTrue('variables' not in out)
        self.assertTrue('attachments' not in out)
        mock_validate_queue.assert_called_once_with(data['queue'])
        mock_validate_parent.assert_called_once_with(data['parent'])
        mock_validate_issue_type.assert_called_once_with(data['issue_type'])
        mock_validate_priority.assert_called_once_with(data['priority'])
        mock_validate_fields.assert_called_once_with([TrackerFieldIn.parse_obj(data['fields'][0])])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_shouldnt_modify_existing_subscription(self):
        data = {
            'type': 'tracker',
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_parent') as mock_validate_parent,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
            patch.object(TrackerValidator, 'validate_fields') as mock_validate_fields,
        ):
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['subject'], self.subscription.title)
        self.assertEqual(out['body'], self.subscription.body)
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertEqual(out['issue_type'], self.tracker.type)
        self.assertEqual(out['priority'], self.tracker.priority)
        self.assertEqual(out['queue'], self.tracker.queue)
        self.assertEqual(out['parent'], self.tracker.parent)
        self.assertEqual(out['author'], self.tracker.author)
        self.assertEqual(out['assignee'], self.tracker.assignee)
        self.assertEqual(out['fields'], [
            {
                'key': {'slug': 'tags', 'name': 'Tags', 'type': 'array/string'},
                'value': 'one', 'only_with_value': True,
            },
        ])
        self.assertTrue('variables' not in out)
        self.assertTrue('attachments' not in out)
        mock_validate_queue.assert_not_called()
        mock_validate_parent.assert_not_called()
        mock_validate_issue_type.assert_not_called()
        mock_validate_priority.assert_not_called()
        mock_validate_fields.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

    def test_should_reset_parent(self):
        data = {
            'type': 'tracker',
            'parent': '',
        }
        with (
            patch.object(TrackerValidator, 'validate_queue') as mock_validate_queue,
            patch.object(TrackerValidator, 'validate_issue_type') as mock_validate_issue_type,
            patch.object(TrackerValidator, 'validate_parent') as mock_validate_parent,
            patch.object(TrackerValidator, 'validate_priority') as mock_validate_priority,
            patch.object(TrackerValidator, 'validate_fields') as mock_validate_fields,
        ):
            mock_validate_parent.return_value = ''
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertTrue('parent' not in out)
        mock_validate_queue.assert_not_called()
        mock_validate_parent.assert_called_once_with('')
        mock_validate_issue_type.assert_not_called()
        mock_validate_priority.assert_not_called()
        mock_validate_fields.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_variables(self):
        var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        data = {
            'type': 'tracker',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['variables'], [
            {'id': str(var.variable_id), 'type': var.var},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'tracker',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.name'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'tracker',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'tracker',
            'variables': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertTrue('variables' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_attachments(self):
        meta = ProxyStorageModelFactory(path='/1/testit.txt')
        SubscriptionAttachmentFactory(subscription=self.subscription, file=meta.path)
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        self.subscription.questions.add(question)
        data = {
            'type': 'tracker',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['attachments']['question']['all'], False)
        self.assertEqual(out['attachments']['question']['items'], [question.param_slug])
        self.assertEqual(len(out['attachments']['static']), 1)
        self.assertEqual(out['attachments']['static'][0]['links']['orig'], get_mds_url(meta.path))

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        another_meta = ProxyStorageModelFactory(path='/2/testthat.txt')
        another_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        data = {
            'type': 'tracker',
            'attachments': {
                'question': {'items': [another_question.param_slug]},
                'static': [{'path': another_meta.path}],
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['attachments']['question']['all'], False)
        self.assertEqual(out['attachments']['question']['items'], data['attachments']['question']['items'])
        self.assertEqual(len(out['attachments']['static']), 1)
        self.assertEqual(out['attachments']['static'][0]['links']['orig'], get_mds_url(another_meta.path))

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'tracker',
            'attachments': {
                'question': {'all': True},
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertEqual(out['attachments']['question']['all'], True)
        self.assertTrue('items' not in out['attachments']['question'])
        self.assertTrue('static' in out['attachments'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'tracker',
            'attachments': {
                'question': {},
                'static': [],
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'tracker')
        self.assertTrue('attachments' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_return_400_with_empty_subject(self):
        data = {
            'type': 'tracker',
            'subject': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['subject'])

    def test_should_return_400_with_empty_queue(self):
        data = {
            'type': 'tracker',
            'queue': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error')
        self.assertEqual(result[0]['loc'], ['queue'])

    def test_should_return_400_with_empty_issue_type(self):
        data = {
            'type': 'tracker',
            'issue_type': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'type_error.integer')
        self.assertEqual(result[0]['loc'], ['issue_type'])

    def test_should_return_400_with_empty_priority(self):
        data = {
            'type': 'tracker',
            'priority': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'type_error.integer')
        self.assertEqual(result[0]['loc'], ['priority'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'tracker',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    @override_cache_settings()
    def test_should_return_400_if_queue_does_not_exist(self):
        data = {
            'type': 'tracker',
            'queue': 'FORMZ',
        }
        with patch.object(StartrekClient, 'get_queue') as mock_get_queue:
            mock_get_queue.return_value = None
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_queue.assert_called_once_with(data['queue'])
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['queue'])

    @override_cache_settings()
    def test_should_return_400_if_parent_does_not_exist(self):
        data = {
            'type': 'tracker',
            'parent': 'FORMS-999',
        }
        with patch.object(StartrekClient, 'get_issue') as mock_get_issue:
            mock_get_issue.return_value = None
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_issue.assert_called_once_with(data['parent'])
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['parent'])

    @override_cache_settings()
    def test_should_return_400_if_issue_type_does_not_exist(self):
        data = {
            'type': 'tracker',
            'queue': 'FORMS',
            'issue_type': 3,
        }
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
        ):
            mock_get_queue.return_value = {'key': 'FORMS'}
            mock_get_issuetypes.return_value = [{'id': 1}, {'id': 2}]
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_queue.assert_called_once_with(data['queue'])
        mock_get_issuetypes.assert_called_once_with(data['queue'])
        mock_get_priorities.assert_not_called()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['issue_type'])

    @override_cache_settings()
    def test_should_return_400_if_priority_does_not_exist(self):
        data = {
            'type': 'tracker',
            'queue': 'FORMS',
            'issue_type': 2,
            'priority': 3,
        }
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
        ):
            mock_get_queue.return_value = {'key': 'FORMS'}
            mock_get_issuetypes.return_value = [{'id': 1}, {'id': 2}]
            mock_get_priorities.return_value = [{'id': 1}, {'id': 2}]
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_queue.assert_called_once_with(data['queue'])
        mock_get_issuetypes.assert_called_once_with(data['queue'])
        mock_get_priorities.assert_called_once_with()
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['priority'])

    @override_cache_settings()
    def test_should_return_400_if_field_does_not_exist(self):
        data = {
            'type': 'tracker',
            'fields': [{'key': 'tag', 'value': '1'}],
        }
        with patch.object(StartrekClient, 'get_field') as mock_get_field:
            mock_get_field.return_value = None
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_get_field.assert_called_once_with(data['fields'][0]['key'])
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['fields', 0, 'key'])

    def test_should_return_400_if_attachment_question_does_not_exist(self):
        question = SurveyQuestionFactory(answer_type=AnswerType.objects.get(slug='answer_files'))
        data = {
            'type': 'tracker',
            'attachments': {
                'question': {'items': [question.param_slug]},
            },
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['attachments', 'question', 'items', 0])


class TestPostJsonRpcSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)

    def test_should_create_new_subscription(self):
        data = {
            'type': 'jsonrpc',
            'active': False,
            'follow': True,
            'language': 'en',
            'url': 'http://yandex.ru',
            'tvm_client': '123',
            'method': 'foobar',
            'params': [
                {'name': 'param1', 'value': 'one', 'only_with_value': True},
            ],
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['method'], data['method'])
        self.assertEqual(out['params'], data['params'])
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

    def test_should_create_new_subscription_only_required_fields(self):
        data = {
            'type': 'jsonrpc',
            'url': 'http://yandex.ru',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['active'], True)
        self.assertEqual(out['follow'], False)
        self.assertEqual(out['language'], 'from_request')
        self.assertEqual(out['method'], '')
        self.assertTrue('tvm_client' not in out)
        self.assertTrue('params' not in out)
        mock_has_role.assert_not_called()

    def test_should_create_new_subscription_with_headers(self):
        data = {
            'type': 'jsonrpc',
            'url': 'http://yandex.ru',
            'headers': [
                {'name': 'header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['headers'], data['headers'])

    def test_should_create_new_subscription_with_variables(self):
        data = {
            'type': 'jsonrpc',
            'url': 'http://yandex.ru',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['variables'], data['variables'])

    def test_should_return_400_without_url(self):
        data = {
            'type': 'jsonrpc',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'jsonrpc',
            'url': 'http://yandex.ru',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'jsonrpc',
            'url': 'http://yandex.ru',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])


class TestPatchJsonRpcSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=ActionType.jsonrpc,
            is_active=False,
            follow_result=True,
            context_language='en',
            is_all_questions=False,
            http_url='http://yandex.ru',
            tvm2_client_id='123',
        )
        self.jsonrpc = JSONRPCSubscriptionDataFactory(
            subscription=self.subscription,
            method='foobar',
        )
        self.jsonrpc_param = JSONRPCSubscriptionParamFactory(
            subscription=self.jsonrpc,
            name='param1', value='one', add_only_with_value=True,
        )

    def test_should_modify_existing_subscription(self):
        data = {
            'type': 'jsonrpc',
            'active': True,
            'follow': False,
            'language': 'de',
            'url': 'http://yandex.com',
            'tvm_client': '234',
            'method': 'barbaz',
            'params': [
                {'name': 'param12', 'value': 'two', 'only_with_value': False},
            ],
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['method'], data['method'])
        self.assertEqual(out['params'], data['params'])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_shouldnt_modify_existing_subscription(self):
        data = {
            'type': 'jsonrpc',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertEqual(out['url'], self.subscription.http_url)
        self.assertEqual(out['tvm_client'], self.subscription.tvm2_client_id)
        self.assertEqual(out['method'], self.jsonrpc.method)
        self.assertEqual(out['params'], [
            {'name': 'param1', 'value': 'one', 'only_with_value': True},
        ])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

    def test_should_reset_tvm_client(self):
        data = {
            'type': 'jsonrpc',
            'tvm_client': '',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertTrue('tvm_client' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_headers(self):
        header = SubscriptionHeaderFactory(
            subscription=self.subscription,
            name='header1', value='value1', add_only_with_value=False,
        )
        data = {
            'type': 'jsonrpc',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['headers'], [
            {'name': header.name, 'value': header.value, 'only_with_value': header.add_only_with_value},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'jsonrpc',
            'headers': [
                {'name': 'header12', 'value': 'value12', 'only_with_value': True},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['headers'], data['headers'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'jsonrpc',
            'headers': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertTrue('headers' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_variables(self):
        var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        data = {
            'type': 'jsonrpc',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['variables'], [
            {'id': str(var.variable_id), 'type': var.var},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'jsonrpc',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.name'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'jsonrpc',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'jsonrpc',
            'variables': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'jsonrpc')
        self.assertTrue('variables' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_return_400_with_empty_url(self):
        data = {
            'type': 'jsonrpc',
            'url': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.any_str.min_length')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'jsonrpc',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'jsonrpc',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])


class TestPostHttpSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)

    def test_should_create_new_subscription(self):
        data = {
            'type': 'http',
            'active': False,
            'follow': True,
            'language': 'en',
            'url': 'http://yandex.ru',
            'method': 'post',
            'body': '{}',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['method'], data['method'])
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

    def test_should_create_new_subscription_only_required_fields(self):
        data = {
            'type': 'http',
            'url': 'http://yandex.ru',
            'method': 'post',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['method'], data['method'])
        self.assertEqual(out['active'], True)
        self.assertEqual(out['follow'], False)
        self.assertEqual(out['language'], 'from_request')
        self.assertEqual(out['body'], '')
        self.assertTrue('tvm_client' not in out)
        mock_has_role.assert_not_called()

    def test_should_create_new_subscription_with_headers(self):
        data = {
            'type': 'http',
            'url': 'http://yandex.ru',
            'method': 'post',
            'headers': [
                {'name': 'header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['headers'], data['headers'])

    def test_should_create_new_subscription_with_variables(self):
        data = {
            'type': 'http',
            'url': 'http://yandex.ru',
            'method': 'post',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['variables'], data['variables'])

    def test_should_return_400_without_url(self):
        data = {
            'type': 'http',
            'method': 'post',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_without_method(self):
        data = {
            'type': 'http',
            'url': 'http://yandex.ru',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['method'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'http',
            'url': 'http://yandex.ru',
            'method': 'post',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'http',
            'url': 'http://yandex.ru',
            'method': 'post',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])


class TestPatchHttpSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=ActionType.http,
            is_active=False,
            follow_result=True,
            context_language='en',
            is_all_questions=False,
            http_url='http://yandex.ru',
            http_method='post',
            body='{}',
            tvm2_client_id='123',
        )

    def test_should_modify_existing_subscription(self):
        data = {
            'type': 'http',
            'active': True,
            'follow': False,
            'language': 'de',
            'url': 'http://yandex.com',
            'method': 'put',
            'body': '[]',
            'tvm_client': '234',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['method'], data['method'])
        self.assertEqual(out['body'], data['body'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_shouldnt_modify_existing_subscription(self):
        data = {
            'type': 'http',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertEqual(out['url'], self.subscription.http_url)
        self.assertEqual(out['method'], self.subscription.http_method)
        self.assertEqual(out['body'], self.subscription.body)
        self.assertEqual(out['tvm_client'], self.subscription.tvm2_client_id)
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

    def test_should_reset_tvm_client(self):
        data = {
            'type': 'http',
            'tvm_client': '',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertTrue('tvm_client' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_headers(self):
        header = SubscriptionHeaderFactory(
            subscription=self.subscription,
            name='header1', value='value1', add_only_with_value=False,
        )
        data = {
            'type': 'http',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['headers'], [
            {'name': header.name, 'value': header.value, 'only_with_value': header.add_only_with_value},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'http',
            'headers': [
                {'name': 'header12', 'value': 'value12', 'only_with_value': True},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['headers'], data['headers'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'http',
            'headers': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertTrue('headers' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_variables(self):
        var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        data = {
            'type': 'http',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['variables'], [
            {'id': str(var.variable_id), 'type': var.var},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'http',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.name'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'http',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'http',
            'variables': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'http')
        self.assertTrue('variables' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_return_400_with_empty_url(self):
        data = {
            'type': 'http',
            'url': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.any_str.min_length')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_with_empty_method(self):
        data = {
            'type': 'http',
            'method': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'type_error.enum')
        self.assertEqual(result[0]['loc'], ['method'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'http',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'http',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])

    def test_should_return_400_with_not_valid_url(self):
        data = {
            'type': 'http',
            'url': 'file:///readme.txt',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.url.scheme')
        self.assertEqual(result[0]['loc'], ['url'])


class TestPostPostSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.question = SurveyQuestionFactory(survey=self.survey)

    def test_should_create_new_subscription(self):
        data = {
            'type': 'post',
            'active': False,
            'follow': True,
            'language': 'en',
            'url': 'http://yandex.ru',
            'format': 'json',
            'tvm_client': '123',
            'questions': {'items': [self.question.param_slug]},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], data['format'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['questions']['all'], False)
        self.assertEqual(out['questions']['items'], data['questions']['items'])
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

    def test_should_create_new_subscription_all_questions(self):
        data = {
            'type': 'post',
            'active': False,
            'follow': True,
            'language': 'en',
            'url': 'http://yandex.ru',
            'format': 'json',
            'tvm_client': '123',
            'questions': {'all': True},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], data['format'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['questions']['all'], True)
        self.assertTrue('items' not in out['questions'])
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

    def test_should_create_new_subscription_only_required_fields(self):
        data = {
            'type': 'post',
            'url': 'http://yandex.ru',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['active'], True)
        self.assertEqual(out['follow'], False)
        self.assertEqual(out['language'], 'from_request')
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], 'json')
        self.assertTrue('tvm_client' not in out)
        self.assertTrue('questions' not in out)
        mock_has_role.assert_not_called()

    def test_should_create_new_subscription_with_headers(self):
        data = {
            'type': 'post',
            'url': 'http://yandex.ru',
            'headers': [
                {'name': 'header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['headers'], data['headers'])

    def test_should_create_new_subscription_with_variables(self):
        data = {
            'type': 'post',
            'url': 'http://yandex.ru',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['variables'], data['variables'])

    def test_should_return_400_without_url(self):
        data = {
            'type': 'post',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'post',
            'url': 'http://yandex.ru',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'post',
            'url': 'http://yandex.ru',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])

    def test_should_return_400_if_question_not_found(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'post',
            'url': 'http://yandex.ru',
            'questions': {'items': [question.param_slug]},
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['questions', 'items', 0])


class TestPatchPostSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.question = SurveyQuestionFactory(survey=self.survey)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=ActionType.post,
            is_active=False,
            follow_result=True,
            context_language='en',
            is_all_questions=False,
            http_url='http://yandex.ru',
            http_method='post',
            http_format_name='json',
            tvm2_client_id='123',
        )
        self.subscription.questions.add(self.question)

    def test_should_modify_existing_subscription(self):
        question = SurveyQuestionFactory(survey=self.survey)
        data = {
            'type': 'post',
            'active': True,
            'follow': False,
            'language': 'de',
            'url': 'http://yandex.com',
            'format': 'xml',
            'tvm_client': '234',
            'questions': {'items': [question.param_slug]},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], data['format'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['questions']['all'], False)
        self.assertEqual(out['questions']['items'], data['questions']['items'])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_existing_subscription_all_questions(self):
        data = {
            'type': 'post',
            'questions': {'all': True},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertEqual(out['url'], self.subscription.http_url)
        self.assertEqual(out['format'], self.subscription.http_format_name)
        self.assertEqual(out['tvm_client'], self.subscription.tvm2_client_id)
        self.assertEqual(out['questions']['all'], True)
        self.assertTrue('items' not in out['questions'])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_shouldnt_modify_existing_subscription(self):
        data = {
            'type': 'post',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertEqual(out['url'], self.subscription.http_url)
        self.assertEqual(out['format'], self.subscription.http_format_name)
        self.assertEqual(out['tvm_client'], self.subscription.tvm2_client_id)
        self.assertEqual(out['questions']['all'], False)
        self.assertEqual(out['questions']['items'], [self.question.param_slug])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

    def test_should_reset_tvm_client(self):
        data = {
            'type': 'post',
            'tvm_client': '',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertTrue('tvm_client' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_headers(self):
        header = SubscriptionHeaderFactory(
            subscription=self.subscription,
            name='header1', value='value1', add_only_with_value=False,
        )
        data = {
            'type': 'post',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['headers'], [
            {'name': header.name, 'value': header.value, 'only_with_value': header.add_only_with_value},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'post',
            'headers': [
                {'name': 'header12', 'value': 'value12', 'only_with_value': True},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['headers'], data['headers'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'post',
            'headers': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertTrue('headers' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_variables(self):
        var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        data = {
            'type': 'post',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['variables'], [
            {'id': str(var.variable_id), 'type': var.var},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'post',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.name'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'post',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'post',
            'variables': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'post')
        self.assertTrue('variables' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_return_400_with_empty_url(self):
        data = {
            'type': 'post',
            'url': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.any_str.min_length')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'post',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'post',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])

    def test_should_return_400_if_question_not_found(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'post',
            'questions': {'items': [question.param_slug]},
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['questions', 'items', 0])


class TestPostPutSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.question = SurveyQuestionFactory(survey=self.survey)

    def test_should_create_new_subscription(self):
        data = {
            'type': 'put',
            'active': False,
            'follow': True,
            'language': 'en',
            'url': 'http://yandex.ru',
            'format': 'json',
            'tvm_client': '123',
            'questions': {'items': [self.question.param_slug]},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], data['format'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['questions']['all'], False)
        self.assertEqual(out['questions']['items'], data['questions']['items'])
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

    def test_should_create_new_subscription_all_questions(self):
        data = {
            'type': 'put',
            'active': False,
            'follow': True,
            'language': 'en',
            'url': 'http://yandex.ru',
            'format': 'json',
            'tvm_client': '123',
            'questions': {'all': True},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], data['format'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['questions']['all'], True)
        self.assertTrue('items' not in out['questions'])
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

    def test_should_create_new_subscription_only_required_fields(self):
        data = {
            'type': 'put',
            'url': 'http://yandex.ru',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.hook.refresh_from_db()
        self.assertTrue(out['id'] in self.hook.subscriptions.values_list('pk', flat=True))
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['active'], True)
        self.assertEqual(out['follow'], False)
        self.assertEqual(out['language'], 'from_request')
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], 'json')
        self.assertTrue('tvm_client' not in out)
        self.assertTrue('questions' not in out)
        mock_has_role.assert_not_called()

    def test_should_create_new_subscription_with_headers(self):
        data = {
            'type': 'put',
            'url': 'http://yandex.ru',
            'headers': [
                {'name': 'header1', 'value': 'value1', 'only_with_value': True},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['headers'], data['headers'])

    def test_should_create_new_subscription_with_variables(self):
        data = {
            'type': 'put',
            'url': 'http://yandex.ru',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['variables'], data['variables'])

    def test_should_return_400_without_url(self):
        data = {
            'type': 'put',
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.missing')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'put',
            'url': 'http://yandex.ru',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'put',
            'url': 'http://yandex.ru',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.post(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])

    def test_should_return_400_if_question_not_found(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'put',
            'url': 'http://yandex.ru',
            'questions': {'items': [question.param_slug]},
        }
        response = self.client.post(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['questions', 'items', 0])


class TestPatchPutSubscriptionView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        self.o2g = OrganizationToGroupFactory()
        self.survey = SurveyFactory(org=self.o2g.org)
        self.question = SurveyQuestionFactory(survey=self.survey)
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=ActionType.put,
            is_active=False,
            follow_result=True,
            context_language='en',
            is_all_questions=False,
            http_url='http://yandex.ru',
            http_method='put',
            http_format_name='json',
            tvm2_client_id='123',
        )
        self.subscription.questions.add(self.question)

    def test_should_modify_existing_subscription(self):
        question = SurveyQuestionFactory(survey=self.survey)
        data = {
            'type': 'put',
            'active': True,
            'follow': False,
            'language': 'de',
            'url': 'http://yandex.com',
            'format': 'xml',
            'tvm_client': '234',
            'questions': {'items': [question.param_slug]},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = True
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['active'], data['active'])
        self.assertEqual(out['follow'], data['follow'])
        self.assertEqual(out['language'], data['language'])
        self.assertEqual(out['url'], data['url'])
        self.assertEqual(out['format'], data['format'])
        self.assertEqual(out['tvm_client'], data['tvm_client'])
        self.assertEqual(out['questions']['all'], False)
        self.assertEqual(out['questions']['items'], data['questions']['items'])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_existing_subscription_all_questions(self):
        data = {
            'type': 'put',
            'questions': {'all': True},
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertEqual(out['url'], self.subscription.http_url)
        self.assertEqual(out['format'], self.subscription.http_format_name)
        self.assertEqual(out['tvm_client'], self.subscription.tvm2_client_id)
        self.assertEqual(out['questions']['all'], True)
        self.assertTrue('items' not in out['questions'])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_shouldnt_modify_existing_subscription(self):
        data = {
            'type': 'put',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['active'], self.subscription.is_active)
        self.assertEqual(out['follow'], self.subscription.follow_result)
        self.assertEqual(out['language'], self.subscription.context_language)
        self.assertEqual(out['url'], self.subscription.http_url)
        self.assertEqual(out['format'], self.subscription.http_format_name)
        self.assertEqual(out['tvm_client'], self.subscription.tvm2_client_id)
        self.assertEqual(out['questions']['all'], False)
        self.assertEqual(out['questions']['items'], [self.question.param_slug])
        self.assertTrue('variables' not in out)
        self.assertTrue('headers' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

    def test_should_reset_tvm_client(self):
        data = {
            'type': 'put',
            'tvm_client': '',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertTrue('tvm_client' not in out)
        mock_has_role.assert_not_called()

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_headers(self):
        header = SubscriptionHeaderFactory(
            subscription=self.subscription,
            name='header1', value='value1', add_only_with_value=False,
        )
        data = {
            'type': 'put',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['headers'], [
            {'name': header.name, 'value': header.value, 'only_with_value': header.add_only_with_value},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'put',
            'headers': [
                {'name': 'header12', 'value': 'value12', 'only_with_value': True},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['headers'], data['headers'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'put',
            'headers': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertTrue('headers' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_modify_subscription_with_variables(self):
        var = SurveyVariableFactory(hook_subscription=self.subscription, var='form.id')
        data = {
            'type': 'put',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['variables'], [
            {'id': str(var.variable_id), 'type': var.var},
        ])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated == self.subscription.date_updated)

        data = {
            'type': 'put',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.name'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'put',
            'variables': [
                {'id': VariableId.create(), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertEqual(out['variables'], data['variables'])

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

        data = {
            'type': 'put',
            'variables': [],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['type'], 'put')
        self.assertTrue('variables' not in out)

        old_date_updated = self.subscription.date_updated
        self.subscription.refresh_from_db()
        self.assertTrue(old_date_updated < self.subscription.date_updated)

    def test_should_return_400_with_empty_url(self):
        data = {
            'type': 'put',
            'url': '',
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.any_str.min_length')
        self.assertEqual(result[0]['loc'], ['url'])

    def test_should_return_400_with_not_unique_variable(self):
        var = SurveyVariableFactory(hook_subscription=ServiceSurveyHookSubscriptionFactory())
        data = {
            'type': 'put',
            'variables': [
                {'id': str(var.variable_id), 'type': 'form.id'},
            ],
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.duplicated')
        self.assertEqual(result[0]['loc'], ['variables', 0, 'id'])

    def test_should_return_400_if_user_does_not_have_role_tvm_manager(self):
        data = {
            'type': 'put',
            'tvm_client': '123',
        }
        with patch('events.v3.views.subscriptions.has_role_tvm_or_form_manager') as mock_has_role:
            mock_has_role.return_value = False
            response = self.client.patch(
                f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
                data=data, format='json',
            )
        self.assertEqual(response.status_code, 400)
        mock_has_role.assert_called_once_with(data['tvm_client'], self.user.uid)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_permitted')
        self.assertEqual(result[0]['loc'], ['tvm_client'])

    def test_should_return_400_if_question_not_found(self):
        question = SurveyQuestionFactory()
        data = {
            'type': 'put',
            'questions': {'items': [question.param_slug]},
        }
        response = self.client.patch(
            f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscription.pk}/',
            data=data, format='json',
        )
        self.assertEqual(response.status_code, 400)
        result = response.json()
        self.assertEqual(result[0]['error_code'], 'value_error.not_found')
        self.assertEqual(result[0]['loc'], ['questions', 'items', 0])


class TestGetVariableObj(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_files'),
            ),
        ]

    def test_should_return_questions_with_slugs(self):
        questions = get_question_slugs_with_type(self.survey)
        self.assertEqual(len(questions), 2)
        self.assertEqual(
            questions[self.questions[0].param_slug],
            (self.questions[0].pk, self.questions[0].answer_type.slug),
        )
        self.assertEqual(
            questions[self.questions[1].param_slug],
            (self.questions[1].pk, self.questions[1].answer_type.slug),
        )

    def test_should_return_variable_forms_id(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(id=VariableId.create(), type='form.id')
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {})
        self.assertEqual(variable.filters, [])
        self.assertEqual(variable.format_name, None)

    def test_should_return_variable_question_answer_short_text(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.question_answer',
            filters=['json'], question=self.questions[0].param_slug,
            only_with_value=False, show_filenames=True,
        )
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {
            'question': self.questions[0].pk, 'only_with_value': False,
        })
        self.assertEqual(variable.filters, ['json'])
        self.assertEqual(variable.format_name, None)

    def test_should_return_variable_question_answer_files(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.question_answer',
            filters=['json'], question=self.questions[1].param_slug,
            only_with_value=False, show_filenames=True,
        )
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {
            'question': self.questions[1].pk, 'only_with_value': False, 'show_filenames': True,
        })
        self.assertEqual(variable.filters, ['json'])
        self.assertEqual(variable.format_name, None)

    def test_should_return_variable_questions_answers_by_items_0(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.questions_answers',
            filters=['sanitize', 'json'], renderer='startrek',
            questions=QuestionsIn(items=[self.questions[0].param_slug]),
            only_with_value=True, show_filenames=True,
        )
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {
            'questions': [self.questions[0].pk],
            'is_all_questions': False, 'only_with_value': True,
        })
        self.assertEqual(variable.filters, ['sanitize', 'json'])
        self.assertEqual(variable.format_name, 'startrek')

    def test_should_return_variable_questions_answers_by_items_1(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.questions_answers',
            filters=['sanitize', 'json'], renderer='startrek',
            questions=QuestionsIn(items=[self.questions[1].param_slug]),
            only_with_value=True, show_filenames=True,
        )
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {
            'questions': [self.questions[1].pk],
            'is_all_questions': False, 'only_with_value': True, 'show_filenames': True,
        })
        self.assertEqual(variable.filters, ['sanitize', 'json'])
        self.assertEqual(variable.format_name, 'startrek')

    def test_should_return_variable_questions_answers_all_by_items(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.questions_answers',
            filters=['sanitize', 'json'], renderer='startrek',
            questions=QuestionsIn(items=[self.questions[0].param_slug, self.questions[1].param_slug]),
            only_with_value=True, show_filenames=True,
        )
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {
            'questions': [self.questions[0].pk, self.questions[1].pk],
            'is_all_questions': False, 'only_with_value': True, 'show_filenames': True,
        })
        self.assertEqual(variable.filters, ['sanitize', 'json'])
        self.assertEqual(variable.format_name, 'startrek')

    def test_should_return_variable_questions_answers_all_by_flag(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.questions_answers',
            filters=['sanitize', 'json'], renderer='startrek',
            questions=QuestionsIn(all=True),
            only_with_value=True, show_filenames=True,
        )
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {
            'is_all_questions': True, 'only_with_value': True, 'show_filenames': True,
        })
        self.assertEqual(variable.filters, ['sanitize', 'json'])
        self.assertEqual(variable.format_name, 'startrek')

    def test_should_return_variable_form_name(self):
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.name',
            filters=['sanitize', 'json'],
            question=self.questions[0].param_slug,
            questions=QuestionsIn(items=[self.questions[0].param_slug, self.questions[1].param_slug]),
            only_with_value=True, show_filenames=True,
        )
        variable = get_variable_obj(questions, 0, data)

        self.assertEqual(variable.variable_id, data.id)
        self.assertEqual(variable.var, data.type)
        self.assertEqual(variable.arguments, {'only_with_value': True})
        self.assertEqual(variable.filters, ['sanitize', 'json'])
        self.assertEqual(variable.format_name, None)

    def test_should_raise_error_on_question_does_not_exist(self):
        types = (
            'form.question_answer', 'directory.vip', 'dir_staff.meta_question',
            'quiz.question_scores', 'staff.meta_question', 'staff.external_login',
        )
        for type_ in types:
            with self.assertRaises(ValidationError):
                VariableIn.parse_obj({'id': VariableId.create(), 'type': type_})

    def test_should_raise_error_on_name_does_not_exist(self):
        types = ('request.header', 'request.query_param', 'request.cookie')
        for type_ in types:
            with self.assertRaises(ValidationError):
                VariableIn.parse_obj({'id': VariableId.create(), 'type': type_})

    def test_should_raise_error_on_question_does_not_found(self):
        another_question = SurveyQuestionFactory()  # object from another survey
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.question_answer',
            question=another_question.param_slug,
        )
        with self.assertRaises(ApiValidationError):
            get_variable_obj(questions, 0, data)

    def test_should_raise_error_on_questions_does_not_exist(self):
        with self.assertRaises(ValidationError):
            VariableIn.parse_obj({'id': VariableId.create(), 'type': 'form.questions_answers'})

    def test_should_raise_error_on_questions_does_not_found(self):
        another_question = SurveyQuestionFactory()  # object from another survey
        questions = get_question_slugs_with_type(self.survey)
        data = VariableIn(
            id=VariableId.create(), type='form.questions_answers',
            questions=QuestionsIn(items=[self.questions[0].param_slug, another_question.param_slug]),
        )
        with self.assertRaises(ApiValidationError):
            get_variable_obj(questions, 0, data)


class TestTrackerValidator(TestCase):
    def test_should_validate_queue(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with (
            patch('events.v3.views.subscriptions.get_startrek_client') as mock_client,
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
        ):
            mock_client.return_value = StartrekClient('120001', dir_id=dir_id)
            mock_get_queue.return_value = {'key': queue_name}
            self.assertEqual(validator.validate_queue(queue_name), queue_name)
        mock_client.assert_called_once_with(dir_id=dir_id)
        mock_get_queue.assert_called_once_with(queue_name)

    def test_shouldnt_validate_queue(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with patch.object(StartrekClient, 'get_queue') as mock_get_queue:
            mock_get_queue.return_value = None
            with self.assertRaises(ApiValidationError) as exc:
                validator.validate_queue(queue_name)
        mock_get_queue.assert_called_once_with(queue_name)
        self.assertEqual(len(exc.exception.errors), 1)
        self.assertEqual(exc.exception.errors[0].loc, ['queue'])

    def test_should_validate_parent(self):
        queue_name = 'FORMS'
        dir_id = '123'
        parent = 'FORMS-12'
        validator = TrackerValidator(queue_name, dir_id)
        with patch.object(StartrekClient, 'get_issue') as mock_get_issue:
            mock_get_issue.return_value = {'key': parent}
            self.assertEqual(validator.validate_parent(parent), parent)
        mock_get_issue.assert_called_once_with(parent)

    def test_shouldnt_validate_parent(self):
        queue_name = 'FORMS'
        dir_id = '123'
        parent = 'FORMS-12'
        validator = TrackerValidator(queue_name, dir_id)
        with patch.object(StartrekClient, 'get_issue') as mock_get_issue:
            mock_get_issue.return_value = None
            with self.assertRaises(ApiValidationError) as exc:
                validator.validate_parent(parent)
        mock_get_issue.assert_called_once_with(parent)
        self.assertEqual(len(exc.exception.errors), 1)
        self.assertEqual(exc.exception.errors[0].loc, ['parent'])

    def test_should_validate_issue_type(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
        ):
            mock_get_issuetypes.return_value = [{'id': 1}, {'id': 2}]
            self.assertEqual(validator.validate_issue_type(2), 2)
        mock_get_queue.assert_not_called()
        mock_get_issuetypes.assert_called_once_with(queue_name)

    def test_should_validate_issue_type_and_return_default(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
        ):
            mock_get_queue.return_value = {'key': queue_name, 'defaultType': {'id': 2}}
            self.assertEqual(validator.validate_issue_type(0), 2)
        mock_get_queue.assert_called_once_with(queue_name)
        mock_get_issuetypes.assert_not_called()

    def test_shouldnt_validate_issue_type(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_issuetypes') as mock_get_issuetypes,
        ):
            mock_get_issuetypes.return_value = [{'id': 1}, {'id': 2}]
            with self.assertRaises(ApiValidationError) as exc:
                validator.validate_issue_type(3)
        mock_get_queue.assert_not_called()
        mock_get_issuetypes.assert_called_once_with(queue_name)
        self.assertEqual(len(exc.exception.errors), 1)
        self.assertEqual(exc.exception.errors[0].loc, ['issue_type'])

    def test_should_validate_priority(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
        ):
            mock_get_priorities.return_value = [{'id': 1}, {'id': 2}]
            self.assertEqual(validator.validate_priority(2), 2)
        mock_get_queue.assert_not_called()
        mock_get_priorities.assert_called_once_with()

    def test_should_validate_priority_and_return_default(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
        ):
            mock_get_queue.return_value = {'key': queue_name, 'defaultPriority': {'id': 2}}
            self.assertEqual(validator.validate_priority(0), 2)
        mock_get_queue.assert_called_once_with(queue_name)
        mock_get_priorities.assert_not_called()

    def test_shouldnt_validate_priority(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_priorities') as mock_get_priorities,
        ):
            mock_get_priorities.return_value = [{'id': 1}, {'id': 2}]
            with self.assertRaises(ApiValidationError) as exc:
                validator.validate_priority(3)
        mock_get_queue.assert_not_called()
        mock_get_priorities.assert_called_once_with()
        self.assertEqual(len(exc.exception.errors), 1)
        self.assertEqual(exc.exception.errors[0].loc, ['priority'])

    def test_should_validate_fields(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        fields = [
            TrackerFieldIn.parse_obj({'key': 'tag', 'value': '1', 'only_with_value': True}),
            TrackerFieldIn.parse_obj({'key': '0123--local', 'value': '2'}),
        ]
        response = [
            {'key': 'tag', 'name': 'Tag', 'schema': {'type': 'array', 'items': 'string'}},
            {'id': '0123--local', 'key': 'local', 'name': 'Local', 'schema': {'type': 'string'}},
        ]
        expected = [
            {
                'key': {'slug': 'tag', 'name': 'Tag', 'type': 'array/string'},
                'value': '1', 'add_only_with_value': True,
            },
            {
                'key': {'slug': '0123--local', 'name': 'Local', 'type': 'string'},
                'value': '2', 'add_only_with_value': False,
            },
        ]
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_field') as mock_get_field,
        ):
            mock_get_field.side_effect = response
            self.assertEqual(validator.validate_fields(fields), expected)
        mock_get_queue.assert_not_called()
        mock_get_field.assert_has_calls([call('tag'), call('0123--local')])

    def test_should_validate_fields_as_object(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        fields = [
            TrackerFieldIn.parse_obj({'key': {'slug': 'tag'}, 'value': '1', 'only_with_value': True}),
            TrackerFieldIn.parse_obj({'key': {'slug': '0123--local'}, 'value': '2'}),
        ]
        response = [
            {'key': 'tag', 'name': 'Tag', 'schema': {'type': 'array', 'items': 'string'}},
            {'id': '0123--local', 'key': 'local', 'name': 'Local', 'schema': {'type': 'string'}},
        ]
        expected = [
            {
                'key': {'slug': 'tag', 'name': 'Tag', 'type': 'array/string'},
                'value': '1', 'add_only_with_value': True,
            },
            {
                'key': {'slug': '0123--local', 'name': 'Local', 'type': 'string'},
                'value': '2', 'add_only_with_value': False,
            },
        ]
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_field') as mock_get_field,
        ):
            mock_get_field.side_effect = response
            self.assertEqual(validator.validate_fields(fields), expected)
        mock_get_queue.assert_not_called()
        mock_get_field.assert_has_calls([call('tag'), call('0123--local')])

    def test_shouldnt_validate_fields(self):
        queue_name = 'FORMS'
        dir_id = '123'
        validator = TrackerValidator(queue_name, dir_id)
        fields = [
            TrackerFieldIn.parse_obj({'key': 'tag', 'value': '1', 'only_with_value': True}),
            TrackerFieldIn.parse_obj({'key': '0123--local', 'value': '2'}),
        ]
        with (
            patch.object(StartrekClient, 'get_queue') as mock_get_queue,
            patch.object(StartrekClient, 'get_field') as mock_get_field,
        ):
            mock_get_field.side_effect = [{}, {}]
            with self.assertRaises(ApiValidationError) as exc:
                validator.validate_fields(fields)
        mock_get_queue.assert_not_called()
        mock_get_field.assert_has_calls([call('tag'), call('0123--local')])
        self.assertEqual(len(exc.exception.errors), 2)
        self.assertEqual(exc.exception.errors[0].loc, ['fields', 0, 'key'])
        self.assertEqual(exc.exception.errors[1].loc, ['fields', 1, 'key'])


class TestGetShowErrors(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_subscription_ids(self):
        hook = SurveyHookFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, hook.survey)
        subscriptions = [
            ServiceSurveyHookSubscriptionFactory(survey_hook=hook),
            ServiceSurveyHookSubscriptionFactory(survey_hook=hook),
        ]
        notifications = [
            HookSubscriptionNotificationFactory(
                status='error',
                is_visible=True,
                subscription=subscriptions[0],
                survey=hook.survey,
            ),
            HookSubscriptionNotificationFactory(
                status='error',
                is_visible=True,
                subscription=subscriptions[1],
                survey=hook.survey,
            ),
            HookSubscriptionNotificationFactory(
                status='error',
                is_visible=False,
                subscription=subscriptions[1],
                survey=hook.survey,
            ),
        ]
        response = self.client.get(f'/v3/surveys/{hook.survey.pk}/show-errors/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [notifications[0].subscription_id, notifications[1].subscription_id])

    def test_shouldnt_return_subscription_ids(self):
        hook = SurveyHookFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, hook.survey)
        subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=hook)
        notifications = [
            HookSubscriptionNotificationFactory(
                status='error',
                is_visible=False,
                subscription=subscription,
                survey=hook.survey,
            ),
            HookSubscriptionNotificationFactory(
                status='error',
                is_visible=True,
                subscription=None,
                survey=hook.survey,
            ),
        ]
        self.assertEqual(notifications[1].subscription_id, None)

        response = self.client.get(f'/v3/surveys/{hook.survey.pk}/show-errors/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])


class TestGetSurveyVariables(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_variable_list_for_intranet(self):
        survey = SurveyFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)

        response = self.client.get(f'/v3/surveys/{survey.pk}/variables/')
        self.assertEqual(response.status_code, 200)

        data = response.json()
        self.assertEqual(len(data), len(variables_list))

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_variable_list_for_b2c(self):
        survey = SurveyFactory(user=self.user)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)

        response = self.client.get(f'/v3/surveys/{survey.pk}/variables/')
        self.assertEqual(response.status_code, 200)

        data = response.json()
        self.assertTrue(len(data) > 0)
        self.assertTrue(len(data) < len(variables_list))
        self.assertTrue('connect_only' in data[0])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_variable_list_for_b2b(self):
        o2g = OrganizationToGroupFactory()
        self.user.groups.add(o2g.group)
        survey = SurveyFactory(org=o2g.org)
        assign_perm(settings.ROLE_FORM_MANAGER, self.user, survey)

        response = self.client.get(f'/v3/surveys/{survey.pk}/variables/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)

        data = response.json()
        self.assertEqual(len(data), len(variables_list))
        self.assertTrue('connect_only' in data[0])


class TestGetSurveyAccessView_response(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        Permission.objects.get_or_create(
            codename='viewfile_survey',
            content_type=ContentType.objects.get_for_model(Survey),
        )
        self.common_group = Group.objects.get(pk=settings.GROUP_ALLSTAFF_PK)

    def test_should_return_owner_access(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        assign_perm('change_survey', user, survey)
        assign_perm('viewfile_survey', user, survey)

        response = self.client.get(f'/v3/surveys/{survey.pk}/access/')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'owner',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'owner',
            'action': 'viewfile',
        })

    def test_should_return_common_access(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        assign_perm('change_survey', self.common_group, survey)
        assign_perm('viewfile_survey', self.common_group, survey)

        response = self.client.get(f'/v3/surveys/{survey.pk}/access/')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'common',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'common',
            'action': 'viewfile',
        })

    def test_should_return_restricted_access(self):
        survey = SurveyFactory(user=UserFactory())
        user = UserFactory()
        staff_group = StaffGroupFactory(name='Test group', url='svc_test')
        group = GroupFactory(name=f'group:{staff_group.pk}')
        assign_perm('change_survey', user, survey)
        assign_perm('change_survey', group, survey)
        assign_perm('viewfile_survey', user, survey)
        assign_perm('viewfile_survey', group, survey)

        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            response = self.client.get(f'/v3/surveys/{survey.pk}/access/')
            self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(result[1], {
            'access': 'restricted',
            'action': 'viewfile',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(mock_get_name.call_count, 2)


class TestPostSurveyAccessView_response(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory(is_superuser=True)
        self.client.set_cookie(self.user.uid)
        Permission.objects.get_or_create(
            codename='viewfile_survey',
            content_type=ContentType.objects.get_for_model(Survey),
        )
        self.common_group = Group.objects.get(pk=settings.GROUP_ALLSTAFF_PK)

    def test_should_make_owner_access(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        data = [
            {'access': 'owner', 'action': 'change'},
            {'access': 'owner', 'action': 'viewfile'},
        ]
        response = self.client.post(f'/v3/surveys/{survey.pk}/access/', data, format='json')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'owner',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'owner',
            'action': 'viewfile',
        })

        self.assertTrue(has_perm(user, 'change_survey', survey))
        self.assertTrue(has_perm(user, 'viewfile_survey', survey))

    def test_should_make_common_access(self):
        survey = SurveyFactory()
        data = [
            {'access': 'common', 'action': 'change'},
            {'access': 'common', 'action': 'viewfile'},
        ]
        response = self.client.post(f'/v3/surveys/{survey.pk}/access/', data, format='json')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'common',
            'action': 'change',
        })
        self.assertEqual(result[1], {
            'access': 'common',
            'action': 'viewfile',
        })

        self.assertTrue(has_perm(self.common_group, 'change_survey', survey))
        self.assertTrue(has_perm(self.common_group, 'viewfile_survey', survey))

    def test_should_make_restricted_access(self):
        user = UserFactory()
        survey = SurveyFactory()
        staff_group = StaffGroupFactory(name='Test group', url='svc_test')
        group = GroupFactory(name=f'group:{staff_group.pk}')
        data = [
            {
                'access': 'restricted', 'action': 'change',
                'users': [user.uid], 'groups': [staff_group.pk],
            },
            {
                'access': 'restricted', 'action': 'viewfile',
                'users': [{'uid': user.uid}], 'groups': [staff_group.pk],
            },
        ]
        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            response = self.client.post(f'/v3/surveys/{survey.pk}/access/', data, format='json')
            self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], {
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(result[1], {
            'access': 'restricted',
            'action': 'viewfile',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        })
        self.assertEqual(mock_get_name.call_count, 2)

        self.assertTrue(has_perm(user, 'change_survey', survey))
        self.assertTrue(has_perm(user, 'viewfile_survey', survey))
        self.assertTrue(has_perm(group, 'change_survey', survey))
        self.assertTrue(has_perm(group, 'viewfile_survey', survey))
