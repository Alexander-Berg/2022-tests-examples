# -*- coding: utf-8 -*-
import json

from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm
from unittest.mock import patch

from events.accounts.factories import UserFactory, OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.common_app.startrek.client import StartrekClient
from events.surveyme.factories import SurveyFactory, ProfileSurveyAnswerFactory
from events.surveyme_integration.factories import (
    HookSubscriptionNotificationFactory,
    SurveyHookFactory,
    ServiceSurveyHookSubscriptionFactory,
    StartrekSubscriptionDataFactory,
)
from events.surveyme_integration.services.email.action_processors import EmailActionProcessor
from events.v3.types import NotificationType, NotificationFieldType, SubscriptionType
from events.v3.views.notifications import (
    ContextAttachments, EmailContextBody, EmailContextFromAddress,
    EmailContextHeaders, EmailContextReplyTo, EmailContextSubject,
    EmailContextToAddress, EmailNotificationFields, EmailResponseMessageId,
    EmailResponseTaskId, ErrorClassName, ErrorMessage, ErrorTraceBack,
    HttpContextBody, HttpContextHeaders, HttpContextMethod,
    HttpContextTvm2ClientId, HttpContextUrl, HttpNotificationFields,
    HttpResponseContent, HttpResponseMethod, HttpResponseStatusCode,
    HttpResponseUrl, JsonrpcContextMethod, JsonrpcContextParams,
    JsonrpcNotificationFields, NotificationAcl, NotificationField,
    NotificationFieldOut, NotificationFields, PostPutContextFields,
    PostPutContextMethod, PostPutNotificationFields, ResponseContent,
    ResponseMethod, ResponseStatusCode, ResponseUrl,
    TrackerContextAssignee, TrackerContextAuthor, TrackerContextDescription,
    TrackerContextFields, TrackerContextIssueType, TrackerContextParent,
    TrackerContextPriority, TrackerContextQueue, TrackerContextSummary,
    TrackerNotificationFields, TrackerResponseIssue, WikiContextSupertag,
    WikiContextText, WikiNotificationFields, get_notification_fields,
    notification_code_field, notification_json_field, notification_text_field,
    notification_textarea_field, notification_url_field, notification_xml_field,
    get_detailed_notification_out, DetailedNotificationOut,
)


class TestNotificationFunctions(TestCase):
    def test_should_return_text_field(self):
        field = notification_text_field('what', 'that')
        self.assertEqual(isinstance(field, NotificationFieldOut), True)
        self.assertEqual(field.name, 'what')
        self.assertEqual(field.value, 'that')
        self.assertEqual(field.type, NotificationFieldType.text)

    def test_should_return_textarea_field(self):
        field = notification_textarea_field('what', 'that')
        self.assertEqual(isinstance(field, NotificationFieldOut), True)
        self.assertEqual(field.name, 'what')
        self.assertEqual(field.value, 'that')
        self.assertEqual(field.type, NotificationFieldType.textarea)

    def test_should_return_code_field(self):
        field = notification_code_field('what', 'that')
        self.assertEqual(isinstance(field, NotificationFieldOut), True)
        self.assertEqual(field.name, 'what')
        self.assertEqual(field.value, 'that')
        self.assertEqual(field.type, NotificationFieldType.code)

    def test_should_return_json_field(self):
        field = notification_json_field('what', 'that')
        self.assertEqual(isinstance(field, NotificationFieldOut), True)
        self.assertEqual(field.name, 'what')
        self.assertEqual(field.value, 'that')
        self.assertEqual(field.type, NotificationFieldType.json)

    def test_should_return_xml_field(self):
        field = notification_xml_field('what', 'that')
        self.assertEqual(isinstance(field, NotificationFieldOut), True)
        self.assertEqual(field.name, 'what')
        self.assertEqual(field.value, 'that')
        self.assertEqual(field.type, NotificationFieldType.xml)

    def test_should_return_url_field(self):
        field = notification_url_field('what', 'that')
        self.assertEqual(isinstance(field, NotificationFieldOut), True)
        self.assertEqual(field.name, 'what')
        self.assertEqual(field.value, 'that')
        self.assertEqual(field.type, NotificationFieldType.url)


class TestField(NotificationField):
    def _get(self):
        return notification_text_field('what', 'that')


class TestNotificationFields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()

    def test_check_acl_for_intranet_user(self):
        field_class = TestField
        field_class.acl = NotificationAcl.intranet_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), True)
        self.assertEqual(isinstance(field.get(), NotificationFieldOut), True)

        field_class.acl = NotificationAcl.business_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

        field_class.acl = NotificationAcl.support
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

        field_class.acl = NotificationAcl.superuser
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_check_acl_for_business_user(self):
        field_class = TestField
        field_class.acl = NotificationAcl.business_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), True)
        self.assertEqual(isinstance(field.get(), NotificationFieldOut), True)

        field_class.acl = NotificationAcl.intranet_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

        field_class.acl = NotificationAcl.support
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

        field_class.acl = NotificationAcl.superuser
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

    def test_check_acl_for_support(self):
        field_class = TestField
        field_class.acl = NotificationAcl.support
        self.user.is_staff = True
        self.user.save()
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), True)
        self.assertEqual(isinstance(field.get(), NotificationFieldOut), True)

        field_class.acl = NotificationAcl.business_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

        field_class.acl = NotificationAcl.intranet_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), True)
        self.assertEqual(isinstance(field.get(), NotificationFieldOut), True)

        field_class.acl = NotificationAcl.superuser
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

    def test_check_acl_for_superuser(self):
        field_class = TestField
        field_class.acl = NotificationAcl.superuser
        self.user.is_superuser = True
        self.user.save()
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), True)
        self.assertEqual(isinstance(field.get(), NotificationFieldOut), True)

        field_class.acl = NotificationAcl.business_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

        field_class.acl = NotificationAcl.intranet_user
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), True)
        self.assertEqual(isinstance(field.get(), NotificationFieldOut), True)

        field_class.acl = NotificationAcl.support
        field = field_class(self.user, None)
        self.assertEqual(field.has_perm(), False)
        self.assertEqual(field.get(), None)

    def test_should_return_error_class_name(self):
        field_class = ErrorClassName
        notification = HookSubscriptionNotificationFactory(error={'classname': 'my.class.name.TestClass'})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.business_user + NotificationAcl.intranet_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, 'TestClass')
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(error=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_error_message(self):
        field_class = ErrorMessage
        message = 'testit'
        notification = HookSubscriptionNotificationFactory(error={'message': message})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.business_user + NotificationAcl.intranet_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, message)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(error=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_error_trace_back(self):
        field_class = ErrorTraceBack
        self.user.is_superuser = True
        self.user.save()

        traceback = 'testit'
        notification = HookSubscriptionNotificationFactory(error={'traceback': traceback})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, traceback)
        self.assertEqual(out.type, NotificationFieldType.code)

        notification = HookSubscriptionNotificationFactory(error=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_response_status_code(self):
        field_class = ResponseStatusCode
        notification = HookSubscriptionNotificationFactory(response={'status_code': 400})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.support + NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, '400')
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_response_method(self):
        field_class = ResponseMethod
        method = 'post'
        notification = HookSubscriptionNotificationFactory(response={'method': method})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.support + NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, method.upper())
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_response_url(self):
        field_class = ResponseUrl
        url = 'http://yandex.ru/test'
        notification = HookSubscriptionNotificationFactory(response={'url': url})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.support + NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, url)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_response_content(self):
        field_class = ResponseContent
        content = '{"what": "that"}'
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.support + NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, json.dumps({"what": "that"}, indent=2))
        self.assertEqual(out.type, NotificationFieldType.json)

        content = 'testit'
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.support + NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, content)
        self.assertEqual(out.type, NotificationFieldType.code)

        content = 'test' * ResponseContent.max_content_length
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.support + NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value.startswith('test'), True)
        self.assertEqual(out.value.endswith('test'), True)
        self.assertEqual(len(out.value) < ResponseContent.max_content_length, True)
        self.assertEqual(out.type, NotificationFieldType.code)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_context_attachments(self):
        field_class = ContextAttachments
        notification = HookSubscriptionNotificationFactory(context={
            'attachments': [{'frontend_url': 'file:///test1.txt'}],
            'static_attachments': [{'frontend_url': 'file:///test2.txt'}],
        })
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0].name.endswith(' 1'), True)
        self.assertEqual(out[0].value, 'file:///test1.txt')
        self.assertEqual(out[0].type, NotificationFieldType.url)
        self.assertEqual(out[1].name.endswith(' 2'), True)
        self.assertEqual(out[1].value, 'file:///test2.txt')
        self.assertEqual(out[1].type, NotificationFieldType.url)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_response_issue(self):
        field_class = TrackerResponseIssue
        notification = HookSubscriptionNotificationFactory(response={'issue': {'key': 'FORMS-123'}})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value.startswith('https://'), True)
        self.assertEqual(out.value.endswith('FORMS-123'), True)
        self.assertEqual(out.type, NotificationFieldType.url)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_summary(self):
        field_class = TrackerContextSummary
        summary = 'test it'
        notification = HookSubscriptionNotificationFactory(context={'summary': summary})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, summary)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_description(self):
        field_class = TrackerContextDescription
        description = 'test it'
        notification = HookSubscriptionNotificationFactory(context={'description': description})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, description)
        self.assertEqual(out.type, NotificationFieldType.textarea)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_queue(self):
        field_class = TrackerContextQueue
        queue = 'FORMS'
        notification = HookSubscriptionNotificationFactory(context={'queue': queue})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, queue)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_parent(self):
        field_class = TrackerContextParent
        parent = 'FORMS-123'
        notification = HookSubscriptionNotificationFactory(context={'parent': parent})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value.startswith('https://'), True)
        self.assertEqual(out.value.endswith(parent), True)
        self.assertEqual(out.type, NotificationFieldType.url)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_issue_type(self):
        field_class = TrackerContextIssueType
        issue_type = 2
        notification = HookSubscriptionNotificationFactory(context={'type': issue_type})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        with patch.object(StartrekClient, 'get_issuetype') as mock_issuetype:
            mock_issuetype.return_value = {'name': 'Task'}
            out = field.get()

        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, 'Task')
        self.assertEqual(out.type, NotificationFieldType.text)
        mock_issuetype.assert_called_once_with(issue_type)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_priority(self):
        field_class = TrackerContextPriority
        priority = 2
        notification = HookSubscriptionNotificationFactory(context={'priority': priority})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        with patch.object(StartrekClient, 'get_priority') as mock_priority:
            mock_priority.return_value = {'name': 'Normal'}
            out = field.get()

        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, 'Normal')
        self.assertEqual(out.type, NotificationFieldType.text)
        mock_priority.assert_called_once_with(priority)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_author(self):
        field_class = TrackerContextAuthor
        author = 'user1'
        notification = HookSubscriptionNotificationFactory(context={'author': author})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, author)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_assignee(self):
        field_class = TrackerContextAssignee
        assignee = 'user1'
        notification = HookSubscriptionNotificationFactory(context={'assignee': assignee})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, assignee)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_tracker_context_fields(self):
        field_class = TrackerContextFields
        st_fields = [
            {'key': {'id': '123--name1', 'name': 'Name1'}},
            {'key': {'id': 'name2', 'name': 'Name2'}},
            {'key': {'slug': 'name3', 'name': 'Name3'}},
        ]
        context_fields = {
            '123--name1': 'value1',
            'name3': [['value3.1'], ['value3.2']],
            'name4': 'value4',
        }
        notification = HookSubscriptionNotificationFactory(context={'fields': context_fields})
        StartrekSubscriptionDataFactory(fields=st_fields, subscription=notification.subscription)
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 3)
        self.assertEqual(out[0].name, 'Name1')
        self.assertEqual(out[0].value, 'value1')
        self.assertEqual(out[0].type, NotificationFieldType.text)
        self.assertEqual(out[1].name, 'Name3')
        self.assertEqual(out[1].value, '["value3.1", "value3.2"]')
        self.assertEqual(out[1].type, NotificationFieldType.json)
        self.assertEqual(out[2].name, 'name4')
        self.assertEqual(out[2].value, 'value4')
        self.assertEqual(out[2].type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_response_message_id(self):
        field_class = EmailResponseMessageId
        message_id = '<123@host>'
        notification = HookSubscriptionNotificationFactory(response={'message_id': message_id})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, message_id)
        self.assertEqual(out.type, NotificationFieldType.text)

        content = '{"result": {"message_id": "%s"}}' % message_id
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, message_id)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_response_task_id(self):
        field_class = EmailResponseTaskId
        task_id = '123-456'
        notification = HookSubscriptionNotificationFactory(response={'task_id': task_id})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, task_id)
        self.assertEqual(out.type, NotificationFieldType.text)

        content = '{"result": {"task_id": "%s"}}' % task_id
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, task_id)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_context_to_address(self):
        field_class = EmailContextToAddress
        to_address = 'user@yandex.ru'
        notification = HookSubscriptionNotificationFactory(context={'to_address': to_address})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, to_address)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_context_from_address(self):
        field_class = EmailContextFromAddress
        from_address = 'robot@yandex.ru'
        notification = HookSubscriptionNotificationFactory(context={'from_address': from_address})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, from_address)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_context_subject(self):
        field_class = EmailContextSubject
        subject = 'test it'
        notification = HookSubscriptionNotificationFactory(context={'subject': subject})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, subject)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_context_body(self):
        field_class = EmailContextBody
        body = 'test it'
        notification = HookSubscriptionNotificationFactory(context={'body': body})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, body)
        self.assertEqual(out.type, NotificationFieldType.textarea)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_context_reply_to(self):
        field_class = EmailContextReplyTo
        headers = {'reply-to': 'user@yandex.ru'}
        notification = HookSubscriptionNotificationFactory(context={'headers': headers})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, 'user@yandex.ru')
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_email_context_headers(self):
        field_class = EmailContextHeaders
        headers = {
            'x-one': '1',
            'reply-to': 'user@yandex.ru',
            'authorization': 'oauth mytoken',
        }
        notification = HookSubscriptionNotificationFactory(context={'headers': headers})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.support + NotificationAcl.superuser)

        out = field.get()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0].name.endswith('x-one'), True)
        self.assertEqual(out[0].value, '1')
        self.assertEqual(out[0].type, NotificationFieldType.text)
        self.assertEqual(out[1].name.endswith('authorization'), True)
        self.assertEqual(out[1].value, 'oauth mytoken')
        self.assertEqual(out[1].type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_wiki_context_supertag(self):
        field_class = WikiContextSupertag
        supertag = '/my/wiki/page/'
        notification = HookSubscriptionNotificationFactory(context={'supertag': supertag})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value.startswith('https://'), True)
        self.assertEqual(out.value.endswith(supertag), True)
        self.assertEqual(out.type, NotificationFieldType.url)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_wiki_context_text(self):
        field_class = WikiContextText
        text = 'test it'
        notification = HookSubscriptionNotificationFactory(context={'text': text})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, text)
        self.assertEqual(out.type, NotificationFieldType.textarea)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_jsonrpc_context_method(self):
        field_class = JsonrpcContextMethod
        method = 'runit'
        notification = HookSubscriptionNotificationFactory(context={'body_data': {'method': method}})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, method)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_jsonrpc_context_params(self):
        field_class = JsonrpcContextParams
        params = {'one': 'value1', 'two': 'value2'}
        notification = HookSubscriptionNotificationFactory(context={'body_data': {'params': params}})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0].name.endswith(' one'), True)
        self.assertEqual(out[0].value, 'value1')
        self.assertEqual(out[0].type, NotificationFieldType.text)
        self.assertEqual(out[1].name.endswith(' two'), True)
        self.assertEqual(out[1].value, 'value2')
        self.assertEqual(out[1].type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_context_url(self):
        field_class = HttpContextUrl
        url = 'http://yandex.ru/test'
        notification = HookSubscriptionNotificationFactory(context={'url': url})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, url)
        self.assertEqual(out.type, NotificationFieldType.url)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_context_method(self):
        field_class = HttpContextMethod
        method = 'put'
        notification = HookSubscriptionNotificationFactory(context={'method': method})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, method.upper())
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_context_body(self):
        field_class = HttpContextBody
        body_data = '{"one": "1", "two": "2"}'
        notification = HookSubscriptionNotificationFactory(context={'body_data': body_data})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, json.dumps({'one': '1', 'two': '2'}, indent=2))
        self.assertEqual(out.type, NotificationFieldType.json)

        body_data = 'dict(one=1, two=2)'
        notification = HookSubscriptionNotificationFactory(context={'body_data': body_data})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, body_data)
        self.assertEqual(out.type, NotificationFieldType.code)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_context_tvm2_client_id(self):
        field_class = HttpContextTvm2ClientId
        client_id = '100123'
        notification = HookSubscriptionNotificationFactory(context={'tvm2_client_id': client_id})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, client_id)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_response_status_code(self):
        field_class = HttpResponseStatusCode
        notification = HookSubscriptionNotificationFactory(response={'status_code': 400})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, '400')
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_response_method(self):
        field_class = HttpResponseMethod
        notification = HookSubscriptionNotificationFactory(response={'method': 'post'})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, 'POST')
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_response_url(self):
        field_class = HttpResponseUrl
        url = 'http://yandex.ru/test'
        notification = HookSubscriptionNotificationFactory(response={'url': url})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, url)
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_response_content(self):
        field_class = HttpResponseContent
        content = '{"what": "that"}'
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, json.dumps({"what": "that"}, indent=2))
        self.assertEqual(out.type, NotificationFieldType.json)

        content = 'testit'
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, content)
        self.assertEqual(out.type, NotificationFieldType.code)

        content = 'test' * ResponseContent.max_content_length
        notification = HookSubscriptionNotificationFactory(response={'content': content})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value.startswith('test'), True)
        self.assertEqual(out.value.endswith('test'), True)
        self.assertEqual(len(out.value) < ResponseContent.max_content_length, True)
        self.assertEqual(out.type, NotificationFieldType.code)

        notification = HookSubscriptionNotificationFactory(response=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_http_context_headers(self):
        field_class = HttpContextHeaders
        headers = {
            'x-one': '1',
            'reply-to': 'user@yandex.ru',
            'authorization': 'oauth mytoken',
        }
        notification = HookSubscriptionNotificationFactory(context={'headers': headers})
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 3)
        self.assertEqual(out[0].name.endswith('x-one'), True)
        self.assertEqual(out[0].value, '1')
        self.assertEqual(out[0].type, NotificationFieldType.text)
        self.assertEqual(out[1].name.endswith('reply-to'), True)
        self.assertEqual(out[1].value, 'user@yandex.ru')
        self.assertEqual(out[1].type, NotificationFieldType.text)
        self.assertEqual(out[2].name.endswith('authorization'), True)
        self.assertEqual(out[2].value, 'oauth xxxxx')
        self.assertEqual(out[2].type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory(context=1)
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_post_put_context_method(self):
        field_class = PostPutContextMethod
        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=4)  # post
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, 'POST')
        self.assertEqual(out.type, NotificationFieldType.text)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=10)  # put
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, NotificationFieldOut), True)
        self.assertEqual(out.value, 'PUT')
        self.assertEqual(out.type, NotificationFieldType.text)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_should_return_post_put_context_fields(self):
        field_class = PostPutContextFields
        context_fields = {
            'field_1': '<value>1</value>',
            'field_2': '<value>2</value>',
        }
        subscription = ServiceSurveyHookSubscriptionFactory(http_format_name='xml')
        notification = HookSubscriptionNotificationFactory(
            context={'body_data': context_fields},
            subscription=subscription,
        )
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0].name.endswith(' 1'), True)
        self.assertEqual(out[0].value, '<value>1</value>')
        self.assertEqual(out[0].type, NotificationFieldType.xml)
        self.assertEqual(out[1].name.endswith(' 2'), True)
        self.assertEqual(out[1].value, '<value>2</value>')
        self.assertEqual(out[1].type, NotificationFieldType.xml)

        context_fields = {
            'field_1': '{"value": 1}',
            'field_2': '{"value": 2}',
        }
        subscription = ServiceSurveyHookSubscriptionFactory(http_format_name='json')
        notification = HookSubscriptionNotificationFactory(
            context={'body_data': context_fields},
            subscription=subscription,
        )
        field = field_class(self.user, notification)
        self.assertEqual(field.acl, NotificationAcl.intranet_user + NotificationAcl.business_user)

        out = field.get()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 2)
        self.assertEqual(out[0].name.endswith(' 1'), True)
        self.assertEqual(out[0].value, '{"value": 1}')
        self.assertEqual(out[0].type, NotificationFieldType.json)
        self.assertEqual(out[1].name.endswith(' 2'), True)
        self.assertEqual(out[1].value, '{"value": 2}')
        self.assertEqual(out[1].type, NotificationFieldType.json)

        notification = HookSubscriptionNotificationFactory()
        field = field_class(self.user, notification)
        self.assertEqual(field.get(), None)

    def test_tracker_notification_fields(self):
        fields_class = TrackerNotificationFields
        self.assertEqual(fields_class.context, [
            TrackerContextSummary, TrackerContextDescription,
            TrackerContextQueue, TrackerContextParent,
            TrackerContextIssueType, TrackerContextPriority,
            TrackerContextAuthor, TrackerContextAssignee,
            TrackerContextFields, ContextAttachments,
        ])
        self.assertEqual(fields_class.response, [TrackerResponseIssue])
        self.assertEqual(fields_class.error_response, [
            ResponseStatusCode, ResponseMethod,
            ResponseUrl, ResponseContent,
        ])
        self.assertEqual(fields_class.error, [ErrorClassName, ErrorMessage, ErrorTraceBack])

    def test_email_notification_fields(self):
        fields_class = EmailNotificationFields
        self.assertEqual(fields_class.context, [
            EmailContextToAddress, EmailContextFromAddress, EmailContextReplyTo,
            EmailContextSubject, EmailContextBody, ContextAttachments,
            EmailContextHeaders,
        ])
        self.assertEqual(fields_class.response, [EmailResponseMessageId, EmailResponseTaskId])
        self.assertEqual(fields_class.error_response, [
            ResponseStatusCode, ResponseMethod,
            ResponseUrl, ResponseContent,
        ])
        self.assertEqual(fields_class.error, [ErrorClassName, ErrorMessage, ErrorTraceBack])

    def test_wiki_notification_fields(self):
        fields_class = WikiNotificationFields
        self.assertEqual(fields_class.context, [WikiContextSupertag, WikiContextText])
        self.assertEqual(fields_class.response, [])
        self.assertEqual(fields_class.error_response, [
            ResponseStatusCode, ResponseMethod,
            ResponseUrl, ResponseContent,
        ])
        self.assertEqual(fields_class.error, [ErrorClassName, ErrorMessage, ErrorTraceBack])

    def test_jsonrpc_notification_fields(self):
        fields_class = JsonrpcNotificationFields
        self.assertEqual(fields_class.context, [
            HttpContextUrl, JsonrpcContextMethod, HttpContextTvm2ClientId,
            JsonrpcContextParams, HttpContextHeaders,
        ])
        self.assertEqual(fields_class.response, [
            HttpResponseStatusCode, HttpResponseMethod, HttpResponseUrl,
            HttpResponseContent,
        ])
        self.assertEqual(fields_class.error_response, [
            ResponseStatusCode, ResponseMethod,
            ResponseUrl, ResponseContent,
        ])
        self.assertEqual(fields_class.error, [ErrorClassName, ErrorMessage, ErrorTraceBack])

    def test_http_notification_fields(self):
        fields_class = HttpNotificationFields
        self.assertEqual(fields_class.context, [
            HttpContextUrl, HttpContextMethod, HttpContextTvm2ClientId,
            HttpContextBody, HttpContextHeaders,
        ])
        self.assertEqual(fields_class.response, [
            HttpResponseStatusCode, HttpResponseMethod, HttpResponseUrl,
            HttpResponseContent,
        ])
        self.assertEqual(fields_class.error_response, [
            ResponseStatusCode, ResponseMethod,
            ResponseUrl, ResponseContent,
        ])
        self.assertEqual(fields_class.error, [ErrorClassName, ErrorMessage, ErrorTraceBack])

    def test_post_put_notification_fields(self):
        fields_class = PostPutNotificationFields
        self.assertEqual(fields_class.context, [
            HttpContextUrl, PostPutContextMethod, HttpContextTvm2ClientId,
            PostPutContextFields, HttpContextHeaders,
        ])
        self.assertEqual(fields_class.response, [
            HttpResponseStatusCode, HttpResponseMethod, HttpResponseUrl,
            HttpResponseContent,
        ])
        self.assertEqual(fields_class.error_response, [
            ResponseStatusCode, ResponseMethod,
            ResponseUrl, ResponseContent,
        ])
        self.assertEqual(fields_class.error, [ErrorClassName, ErrorMessage, ErrorTraceBack])

    def test_notification_fields(self):
        user = UserFactory()
        notifications = [
            HookSubscriptionNotificationFactory(status='success'),
            HookSubscriptionNotificationFactory(status='error'),
        ]
        dir_id = '123'

        with patch.object(NotificationFields, '_get_category', return_value=[]) as mock_category:
            fields = NotificationFields(user, notifications[0])
            out = fields.get_context(dir_id=dir_id)
        self.assertEqual(isinstance(out, list), True)
        mock_category.assert_called_once_with(fields.context, dir_id=dir_id)

        with patch.object(NotificationFields, '_get_category', return_value=[]) as mock_category:
            fields = NotificationFields(user, notifications[1])
            out = fields.get_context(dir_id=dir_id)
        self.assertEqual(isinstance(out, list), True)
        mock_category.assert_called_once_with(fields.context, dir_id=dir_id)

        with patch.object(NotificationFields, '_get_category', return_value=[]) as mock_category:
            fields = NotificationFields(user, notifications[0])
            out = fields.get_response(dir_id=dir_id)
        self.assertEqual(isinstance(out, list), True)
        mock_category.assert_called_once_with(fields.response, dir_id=dir_id)

        with patch.object(NotificationFields, '_get_category', return_value=[]) as mock_category:
            fields = NotificationFields(user, notifications[1])
            out = fields.get_response(dir_id=dir_id)
        self.assertEqual(isinstance(out, list), True)
        mock_category.assert_called_once_with(fields.error_response, dir_id=dir_id)

        with patch.object(NotificationFields, '_get_category', return_value=[]) as mock_category:
            fields = NotificationFields(user, notifications[0])
            out = fields.get_error(dir_id=dir_id)
        self.assertEqual(out, None)
        mock_category.assert_not_called()

        with patch.object(NotificationFields, '_get_category', return_value=[]) as mock_category:
            fields = NotificationFields(user, notifications[1])
            out = fields.get_error(dir_id=dir_id)
        self.assertEqual(isinstance(out, list), True)
        mock_category.assert_called_once_with(fields.error, dir_id=dir_id)

    def test_get_notification_fields(self):
        user = UserFactory()
        notification = HookSubscriptionNotificationFactory(subscription=None)
        self.assertEqual(get_notification_fields(user, notification), None)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=3)  # email
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, EmailNotificationFields), True)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=7)  # tracker
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, TrackerNotificationFields), True)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=8)  # tracker
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, TrackerNotificationFields), True)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=9)  # wiki
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, WikiNotificationFields), True)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=6)  # jsonrpc
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, JsonrpcNotificationFields), True)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=11)  # http
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, HttpNotificationFields), True)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=4)  # post
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, PostPutNotificationFields), True)

        subscription = ServiceSurveyHookSubscriptionFactory(service_type_action_id=10)  # put
        notification = HookSubscriptionNotificationFactory(subscription=subscription)
        out = get_notification_fields(user, notification)
        self.assertEqual(isinstance(out, PostPutNotificationFields), True)

    def test_get_detailed_notification_out_short_version(self):
        user = UserFactory()
        answer = ProfileSurveyAnswerFactory()
        notification = HookSubscriptionNotificationFactory(
            user=answer.user, survey=answer.survey, answer=answer, subscription=None,
        )
        notification.comment = None

        out = get_detailed_notification_out(user, notification)
        self.assertEqual(isinstance(out, DetailedNotificationOut), True)
        self.assertEqual(out.id, notification.pk)
        self.assertEqual(out.status, notification.status)
        self.assertEqual(out.created, notification.date_created)
        self.assertEqual(out.finished, notification.date_finished)
        self.assertEqual(out.answer_id, notification.answer_id)
        self.assertEqual(out.user_id, notification.user_id)
        self.assertEqual(out.error is None, True)
        self.assertEqual(out.context is None, True)
        self.assertEqual(out.response is None, True)
        self.assertEqual(out.survey_id, notification.survey_id)
        self.assertEqual(out.survey_name, answer.survey.name)
        self.assertEqual(out.subscription_id is None, True)
        self.assertEqual(out.type is None, True)
        self.assertEqual(out.hook_id is None, True)
        self.assertEqual(out.hook_name is None, True)

    def test_get_detailed_notification_out_full_version(self):
        user = UserFactory()
        answer = ProfileSurveyAnswerFactory()
        hook = SurveyHookFactory(survey=answer.survey, name='hook1')
        subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=hook)
        notification = HookSubscriptionNotificationFactory(
            user=answer.user, survey=answer.survey, answer=answer,
            subscription=subscription, status='error',
            context={'to_address': 'user@yandex.ru'},
            response={'url': 'http://yandex.ru/test'},
            error={'message': 'internal error'},
        )
        notification.comment = None

        out = get_detailed_notification_out(user, notification)
        self.assertEqual(isinstance(out, DetailedNotificationOut), True)
        self.assertEqual(out.id, notification.pk)
        self.assertEqual(out.status, notification.status)
        self.assertEqual(out.created, notification.date_created)
        self.assertEqual(out.finished, notification.date_finished)
        self.assertEqual(out.answer_id, notification.answer_id)
        self.assertEqual(out.user_id, notification.user_id)
        self.assertEqual(out.error is not None, True)
        self.assertEqual(out.context is not None, True)
        self.assertEqual(out.response is not None, True)
        self.assertEqual(out.survey_id, notification.survey_id)
        self.assertEqual(out.survey_name, answer.survey.name)
        self.assertEqual(out.subscription_id, notification.subscription_id)
        self.assertEqual(out.type, SubscriptionType.email)
        self.assertEqual(out.hook_id, notification.subscription.survey_hook_id)
        self.assertEqual(out.hook_name, notification.subscription.survey_hook.name)


class TestGetNotificationView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_notification_for_superuser(self):
        survey = SurveyFactory(name='Survey1')
        hook = SurveyHookFactory(survey=survey, name='Hook1')
        subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=hook)
        answer = ProfileSurveyAnswerFactory(survey=survey)
        notification = HookSubscriptionNotificationFactory(
            status=NotificationType.success,
            subscription=subscription,
            survey=survey,
            answer=answer,
        )

        self.user.is_superuser = True
        self.user.save()

        response = self.client.get(f'/v3/notifications/{notification.pk}/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['id'], notification.pk)
        self.assertEqual(data['status'], notification.status)
        self.assertEqual(data['survey_id'], survey.pk)
        self.assertEqual(data['survey_name'], survey.name)
        self.assertEqual(data['hook_id'], hook.pk)
        self.assertEqual(data['hook_name'], hook.name)
        self.assertEqual(data['subscription_id'], notification.subscription_id)
        self.assertEqual(data['user_id'], notification.user_id)
        self.assertEqual(data['answer_id'], notification.answer_id)
        self.assertEqual(data['type'], 'email')

    def test_should_return_notification_for_user(self):
        subscription = ServiceSurveyHookSubscriptionFactory()
        answer = ProfileSurveyAnswerFactory(survey=subscription.survey_hook.survey)
        notification = HookSubscriptionNotificationFactory(
            status=NotificationType.success,
            subscription=subscription,
            survey=subscription.survey_hook.survey,
            answer=answer,
        )

        assign_perm('change_survey', self.user, notification.survey)

        response = self.client.get(f'/v3/notifications/{notification.pk}/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['id'], notification.pk)
        self.assertEqual(data['status'], notification.status)
        self.assertEqual(data['survey_id'], notification.survey_id)
        self.assertEqual(data['subscription_id'], notification.subscription_id)
        self.assertEqual(data['user_id'], notification.user_id)
        self.assertEqual(data['answer_id'], notification.answer_id)
        self.assertEqual(data['type'], 'email')

    def test_should_return_not_permitted(self):
        subscription = ServiceSurveyHookSubscriptionFactory()
        notification = HookSubscriptionNotificationFactory(
            status=NotificationType.success,
            subscription=subscription,
            survey=subscription.survey_hook.survey,
        )

        response = self.client.get(f'/v3/notifications/{notification.pk}/')
        self.assertEqual(response.status_code, 403)


class TestGetNotificationsView_request(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_notification_for_superuser(self):
        surveys = [
            SurveyFactory(name='Survey1'),
            SurveyFactory(name='Survey2'),
        ]
        hooks = [
            SurveyHookFactory(survey=surveys[0], name='Hook1'),
            SurveyHookFactory(survey=surveys[1], name='Hook2'),
        ]
        subscriptions = [
            ServiceSurveyHookSubscriptionFactory(survey_hook=hooks[0]),
            ServiceSurveyHookSubscriptionFactory(survey_hook=hooks[1], service_type_action_id=7),
        ]
        answers = [
            ProfileSurveyAnswerFactory(survey=surveys[0]),
            ProfileSurveyAnswerFactory(survey=surveys[1]),
            ProfileSurveyAnswerFactory(survey=surveys[0]),
        ]
        notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=subscriptions[0],
                survey=surveys[0],
                answer=answers[0],
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=subscriptions[1],
                survey=surveys[1],
                answer=answers[1],
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=subscriptions[0],
                survey=surveys[0],
                answer=answers[2],
            ),
        ]

        self.user.is_superuser = True
        self.user.save()

        response = self.client.get('/v3/notifications/', {'survey_id': surveys[0].pk})
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 2)
        self.assertEqual(data['result'][0]['id'], notifications[0].pk)
        self.assertEqual(data['result'][0]['type'], 'email')
        self.assertEqual(data['result'][0]['survey_id'], surveys[0].pk)
        self.assertEqual(data['result'][0]['survey_name'], surveys[0].name)
        self.assertEqual(data['result'][0]['hook_id'], hooks[0].pk)
        self.assertEqual(data['result'][0]['hook_name'], hooks[0].name)
        self.assertEqual(data['result'][1]['id'], notifications[2].pk)
        self.assertEqual(data['result'][1]['type'], 'email')
        self.assertEqual(data['result'][1]['survey_id'], surveys[0].pk)
        self.assertEqual(data['result'][1]['survey_name'], surveys[0].name)
        self.assertEqual(data['result'][1]['hook_id'], hooks[0].pk)
        self.assertEqual(data['result'][1]['hook_name'], hooks[0].name)
        self.assertEqual(data['links'], {})

        params = {'survey_id': surveys[0].pk, 'page_size': 1}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[0].pk)
        self.assertEqual(data['result'][0]['type'], 'email')
        self.assertEqual(
            data['links']['next'],
            f'/v3/notifications/?survey_id={surveys[0].pk}&page_size=1&id={notifications[0].pk}',
        )

        params = {'survey_id': surveys[1].pk, 'status': 'error'}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)
        self.assertEqual(data['result'][0]['type'], 'tracker')
        self.assertEqual(data['result'][0]['survey_id'], surveys[1].pk)
        self.assertEqual(data['result'][0]['survey_name'], surveys[1].name)
        self.assertEqual(data['result'][0]['hook_id'], hooks[1].pk)
        self.assertEqual(data['result'][0]['hook_name'], hooks[1].name)

        params = {'survey_id': surveys[1].pk}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)
        self.assertEqual(data['result'][0]['type'], 'tracker')
        self.assertEqual(data['result'][0]['survey_id'], surveys[1].pk)
        self.assertEqual(data['result'][0]['survey_name'], surveys[1].name)
        self.assertEqual(data['result'][0]['hook_id'], hooks[1].pk)
        self.assertEqual(data['result'][0]['hook_name'], hooks[1].name)

        params = {'hook_id': hooks[0].pk, 'created_lt': notifications[1].date_created}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[0].pk)
        self.assertEqual(data['result'][0]['type'], 'email')
        self.assertEqual(data['result'][0]['survey_id'], surveys[0].pk)
        self.assertEqual(data['result'][0]['survey_name'], surveys[0].name)
        self.assertEqual(data['result'][0]['hook_id'], hooks[0].pk)
        self.assertEqual(data['result'][0]['hook_name'], hooks[0].name)

        params = {'subscription_id': subscriptions[1].pk}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)
        self.assertEqual(data['result'][0]['type'], 'tracker')
        self.assertEqual(data['result'][0]['survey_id'], surveys[1].pk)
        self.assertEqual(data['result'][0]['survey_name'], surveys[1].name)
        self.assertEqual(data['result'][0]['hook_id'], hooks[1].pk)
        self.assertEqual(data['result'][0]['hook_name'], hooks[1].name)
        self.assertEqual(data['result'][0]['subscription_id'], subscriptions[1].pk)

        params = {'answer_id': answers[1].pk, 'created_gt': notifications[0].date_created}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)
        self.assertEqual(data['result'][0]['type'], 'tracker')
        self.assertEqual(data['result'][0]['survey_id'], surveys[1].pk)
        self.assertEqual(data['result'][0]['survey_name'], surveys[1].name)
        self.assertEqual(data['result'][0]['hook_id'], hooks[1].pk)
        self.assertEqual(data['result'][0]['hook_name'], hooks[1].name)
        self.assertEqual(data['result'][0]['answer_id'], answers[1].pk)

        response = self.client.get('/v3/notifications/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 0)

    def test_should_return_notification_for_user(self):
        survey = SurveyFactory()
        hook = SurveyHookFactory(survey=survey)
        subscriptions = [
            ServiceSurveyHookSubscriptionFactory(),
            ServiceSurveyHookSubscriptionFactory(survey_hook=hook),
        ]
        answer = ProfileSurveyAnswerFactory(survey=survey)
        notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=subscriptions[0],
                survey=subscriptions[0].survey_hook.survey,
                is_visible=False,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=subscriptions[1],
                survey=survey,
                answer=answer,
                is_visible=True,
            ),
        ]

        assign_perm('change_survey', self.user, notifications[1].survey)

        params = {'survey_id': notifications[1].survey_id}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)
        self.assertEqual(data['links'], {})

        params = {'survey_id': notifications[1].survey_id, 'status': 'error'}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'survey_id': notifications[1].survey_id}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'subscription_id': notifications[1].subscription_id}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'answer_id': notifications[1].answer_id}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'survey_id': notifications[1].survey_id, 'created_lt': notifications[1].date_created}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 0)

        params = {'survey_id': notifications[1].survey_id, 'created_gt': notifications[0].date_created}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'survey_id': notifications[1].survey_id, 'visible': 1}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        response = self.client.get('/v3/notifications/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 0)

    def test_should_return_filter_notifications_by_list_of_statuses(self):
        survey = SurveyFactory()
        hook = SurveyHookFactory(survey=survey)
        subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=hook)
        notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=subscription,
                survey=survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=subscription,
                survey=survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.pending,
                subscription=subscription,
                survey=survey,
            ),
        ]
        assign_perm('change_survey', self.user, survey)

        params = {'survey_id': survey.pk, 'status': ['success', 'pending']}
        response = self.client.get('/v3/notifications/', params)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 2)
        self.assertEqual(data['result'][0]['id'], notifications[0].pk)
        self.assertEqual(data['result'][1]['id'], notifications[2].pk)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_notification_for_user_with_org(self):
        survey = SurveyFactory()
        hook = SurveyHookFactory(survey=survey)
        answer = ProfileSurveyAnswerFactory(survey=survey)
        subscriptions = [
            ServiceSurveyHookSubscriptionFactory(),
            ServiceSurveyHookSubscriptionFactory(survey_hook=hook),
        ]
        notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=subscriptions[0],
                survey=subscriptions[0].survey_hook.survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=subscriptions[1],
                survey=survey,
                answer=answer,
            ),
        ]

        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(self.user)
        notifications[1].survey.org = o2g.org
        notifications[1].survey.save()
        assign_perm('change_survey', self.user, notifications[1].survey)

        response = self.client.get('/v3/notifications/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 0)

        params = {'survey_id': notifications[1].survey_id}
        response = self.client.get('/v3/notifications/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)
        self.assertEqual(data['links'], {})

        params = {'survey_id': notifications[1].survey_id, 'status': 'error'}
        response = self.client.get('/v3/notifications/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'hook_id': subscriptions[1].survey_hook_id}
        response = self.client.get('/v3/notifications/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'subscription_id': notifications[1].subscription_id}
        response = self.client.get('/v3/notifications/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'answer_id': notifications[1].answer_id}
        response = self.client.get('/v3/notifications/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        params = {'survey_id': notifications[1].survey_id, 'created_lt': notifications[1].date_created}
        response = self.client.get('/v3/notifications/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 0)

        params = {'survey_id': notifications[1].survey_id, 'created_gt': notifications[0].date_created}
        response = self.client.get('/v3/notifications/', params, HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        response = self.client.get('/v3/notifications/', HTTP_X_ORGS=o2g.org.dir_id)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 0)

    def test_filter_notifications_by_slug(self):
        survey = SurveyFactory()
        hook = SurveyHookFactory(survey=survey)
        subscriptions = [
            ServiceSurveyHookSubscriptionFactory(survey_hook=hook),
            ServiceSurveyHookSubscriptionFactory(survey_hook=hook, service_type_action_id=7),
        ]
        notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=subscriptions[0],
                survey=subscriptions[0].survey_hook.survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=subscriptions[1],
                survey=subscriptions[1].survey_hook.survey,
            ),
        ]

        self.user.is_superuser = True
        self.user.save()

        response = self.client.get('/v3/notifications/', {'survey_id': survey.pk, 'type': 'tracker'})
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['result']), 1)
        self.assertEqual(data['result'][0]['id'], notifications[1].pk)

        response = self.client.get('/v3/notifications/', {'type': 'tracke'})
        self.assertEqual(response.status_code, 400)


class TestPostHideNotificationErrorsView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscriptions = [
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
        ]
        self.notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=self.subscriptions[0],
                survey=self.survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscriptions[1],
                survey=self.survey,
                is_visible=True,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscriptions[1],
                survey=self.survey,
                is_visible=False,
            ),
        ]
        self.subscriptions[1].hooksubscriptionnotificationcounter.errors_count = 1
        self.subscriptions[1].hooksubscriptionnotificationcounter.save()

    def test_should_hide_errors_by_subscription(self):
        assign_perm('change_survey', self.user, self.survey)

        response = self.client.post(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscriptions[1].pk}/hide-errors/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {'status': 'ok'})

        self.notifications[2].refresh_from_db()
        self.assertEqual(self.notifications[2].is_visible, False)

        self.subscriptions[1].hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscriptions[1].hooksubscriptionnotificationcounter.errors_count, 0)

    def test_should_hide_errors_by_survey(self):
        assign_perm('change_survey', self.user, self.survey)

        response = self.client.post(f'/v3/surveys/{self.survey.pk}/hide-errors/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {'status': 'ok'})

        self.notifications[2].refresh_from_db()
        self.assertEqual(self.notifications[2].is_visible, False)

        self.subscriptions[1].hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscriptions[1].hooksubscriptionnotificationcounter.errors_count, 0)

    def test_shouldnt_hide_errors(self):
        response = self.client.post(f'/v3/surveys/{self.survey.pk}/hooks/{self.hook.pk}/subscriptions/{self.subscriptions[1].pk}/hide-errors/')
        self.assertEqual(response.status_code, 403)


class TestGetHideNotificationErrorsView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscriptions = [
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
            ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook),
        ]
        self.notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=self.subscriptions[0],
                survey=self.survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscriptions[1],
                survey=self.survey,
                is_visible=True,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscriptions[1],
                survey=self.survey,
                is_visible=False,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscriptions[1],
                survey=self.survey,
                is_visible=True,
            ),
        ]
        self.subscriptions[1].hooksubscriptionnotificationcounter.errors_count = 2
        self.subscriptions[1].hooksubscriptionnotificationcounter.save()

    def test_should_show_errors_by_survey(self):
        assign_perm('change_survey', self.user, self.survey)

        response = self.client.get(f'/v3/surveys/{self.survey.pk}/show-errors/')
        self.assertEqual(response.status_code, 200)
        result = response.json()
        self.assertEqual(isinstance(result, list), True)
        self.assertEqual(len(result), 1)
        self.assertEqual(result, [self.subscriptions[1].pk])

    def test_shouldnt_show_errors(self):
        response = self.client.get(f'/v3/surveys/{self.survey.pk}/show-errors/')
        self.assertEqual(response.status_code, 403)


class TestPostRestartNotificationView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        self.notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=self.subscription,
                survey=self.survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscription,
                survey=self.survey,
                is_visible=True,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscription,
                survey=self.survey,
                is_visible=False,
            ),
        ]

    def test_should_restart_notification(self):
        assign_perm('change_survey', self.user, self.survey)
        notification = self.notifications[1]
        with patch.object(EmailActionProcessor, 'do_action') as mock_action:
            mock_action.return_value = {
                'status': 'success',
                'response': {},
            }
            response = self.client.post(f'/v3/notifications/{notification.pk}/restart/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['id'], notification.pk)
        self.assertEqual(out['survey_id'], notification.survey_id)
        self.assertEqual(out['subscription_id'], notification.subscription_id)
        self.assertEqual(out['result']['status'], 'operation')
        self.assertNotEqual(out['result']['operation_id'], '')

        notification.refresh_from_db()
        self.assertEqual(notification.status, 'success')

    def test_should_skip_notification(self):
        assign_perm('change_survey', self.user, self.survey)
        notification = self.notifications[0]
        with patch.object(EmailActionProcessor, 'do_action') as mock_action:
            mock_action.return_value = {
                'status': 'success',
                'response': {},
            }
            response = self.client.post(f'/v3/notifications/{notification.pk}/restart/')
        self.assertEqual(response.status_code, 200)

        out = response.json()
        self.assertEqual(out['id'], notification.pk)
        self.assertEqual(out['survey_id'], notification.survey_id)
        self.assertEqual(out['subscription_id'], notification.subscription_id)
        self.assertEqual(out['result']['status'], 'skip')

        notification.refresh_from_db()
        self.assertEqual(notification.status, 'success')

    def test_shouldnt_restart_notification_without_permissions(self):
        response = self.client.post(f'/v3/notifications/{self.notifications[1].pk}/restart/')
        self.assertEqual(response.status_code, 403)


class TestPostRestartNotificationsView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)
        self.survey = SurveyFactory()
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        self.notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success,
                subscription=self.subscription,
                survey=self.survey,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscription,
                survey=self.survey,
                is_visible=True,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
                subscription=self.subscription,
                survey=self.survey,
                is_visible=False,
            ),
        ]

    def test_should_restart_notifications(self):
        assign_perm('change_survey', self.user, self.survey)
        notification = self.notifications[1]
        data = {
            'ids': [notification.pk],
        }
        with patch.object(EmailActionProcessor, 'do_action') as mock_action:
            mock_action.return_value = {
                'status': 'success',
                'response': {},
            }
            response = self.client.post('/v3/notifications/restart/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        out = response.json()
        self.assertEqual(len(out), 1)
        self.assertEqual(out[0]['id'], notification.pk)
        self.assertEqual(out[0]['survey_id'], notification.survey_id)
        self.assertEqual(out[0]['subscription_id'], notification.subscription_id)
        self.assertEqual(out[0]['result']['status'], 'operation')
        self.assertNotEqual(out[0]['result']['operation_id'], '')

        notification.refresh_from_db()
        self.assertEqual(notification.status, 'success')

    def test_should_skip_notifications(self):
        assign_perm('change_survey', self.user, self.survey)
        notification = self.notifications[0]
        data = {
            'ids': [notification.pk],
        }
        with patch.object(EmailActionProcessor, 'do_action') as mock_action:
            mock_action.return_value = {
                'status': 'success',
                'response': {},
            }
            response = self.client.post('/v3/notifications/restart/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        out = response.json()
        self.assertEqual(len(out), 1)
        self.assertEqual(out[0]['id'], notification.pk)
        self.assertEqual(out[0]['survey_id'], notification.survey_id)
        self.assertEqual(out[0]['subscription_id'], notification.subscription_id)
        self.assertEqual(out[0]['result']['status'], 'skip')

        notification.refresh_from_db()
        self.assertEqual(notification.status, 'success')

    def test_shouldnt_restart_notifications_without_permissions(self):
        data = {
            'ids': [self.notifications[1].pk],
        }
        response = self.client.post('/v3/notifications/restart/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])


class TestGetStatusNotificationView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_notification_status(self):
        notification = HookSubscriptionNotificationFactory(status=NotificationType.success)
        assign_perm('change_survey', self.user, notification.survey)

        response = self.client.get(f'/v3/notifications/{notification.pk}/status/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {'id': notification.pk, 'status': 'success'})

    def test_should_return_permission_denied(self):
        notification = HookSubscriptionNotificationFactory(status=NotificationType.success)

        response = self.client.get(f'/v3/notifications/{notification.pk}/status/')
        self.assertEqual(response.status_code, 403)

    def test_should_return_object_not_found(self):
        notification = HookSubscriptionNotificationFactory(status=NotificationType.success)
        notification_id = notification.pk
        notification.delete()

        response = self.client.get(f'/v3/notifications/{notification_id}/status/')
        self.assertEqual(response.status_code, 404)


class TestPostStatusNotificationsView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_should_return_notifications_status(self):
        hook = SurveyHookFactory()
        assign_perm('change_survey', self.user, hook.survey)
        subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=hook)
        notifications = [
            HookSubscriptionNotificationFactory(
                status=NotificationType.success, survey=hook.survey, subscription=subscription,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error,
            ),
            HookSubscriptionNotificationFactory(
                status=NotificationType.error, survey=hook.survey, subscription=subscription,
            ),
        ]

        data = {'ids': [it.pk for it in notifications]}
        response = self.client.post('/v3/notifications/status/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        out = response.json()
        self.assertEqual(isinstance(out, list), True)
        self.assertEqual(len(out), 2)
        self.assertEqual(out, [
            {'id': notifications[0].pk, 'status': 'success'},
            {'id': notifications[2].pk, 'status': 'error'},
        ])

    def test_shouldnt_return_notifications_status(self):
        notification = HookSubscriptionNotificationFactory()

        data = {'ids': [notification.pk]}
        response = self.client.post('/v3/notifications/status/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])
