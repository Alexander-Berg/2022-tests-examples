# -*- coding: utf-8 -*-

from unittest.mock import patch
from django.test import TestCase, override_settings

from events.abc.factories import AbcServiceFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.factories import StartrekSubscriptionDataFactory, SurveyVariableFactory
from events.surveyme_integration.services.base.context_processors.base import ServiceContextInstance
from events.surveyme_integration.services.startrek.context_processors import StartrekContextProcessor


@override_settings(STARTREK_UNIQUE_IN_TESTS=False)
class TestStartrekContextProcessor(TestCase):
    fixtures = ['initial_data.json']
    context_processor_class = StartrekContextProcessor

    def setUp(self):
        super().setUp()
        self.startrek = StartrekSubscriptionDataFactory()
        self.subscription = self.startrek.subscription
        self.survey = self.subscription.survey_hook.survey
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey)
        self.trigger_data = {}
        self.notification_unique_id = '123'
        self.service_context_instance = ServiceContextInstance(
            subscription=self.subscription,
            answer=self.answer,
            trigger_data=self.trigger_data,
            notification_unique_id=self.notification_unique_id,
        )

    def test_default_fields(self):
        response = self.context_processor_class(self.service_context_instance).data
        self.assertEqual(response['notification_unique_id'], self.notification_unique_id)
        self.assertEqual(response['survey_id'], self.answer.survey.id)
        self.assertEqual(response['answer_id'], self.answer.id)
        self.assertEqual(response['subscription_id'], self.subscription.id)

    @override_settings(APP_TYPE='forms_int')
    def test_should_return_valid_unique_for_forms_int(self):
        response = self.context_processor_class(self.service_context_instance).data
        self.assertEqual(response['unique'], 'FORMS/1/123')

    @override_settings(APP_TYPE='forms_ext')
    def test_should_return_valid_unique_for_forms_ext(self):
        response = self.context_processor_class(self.service_context_instance).data
        self.assertEqual(response['unique'], 'FORMS/2/123')

    @override_settings(APP_TYPE='forms_ext_admin')
    def test_should_return_valid_unique_for_forms_ext_admin(self):
        response = self.context_processor_class(self.service_context_instance).data
        self.assertEqual(response['unique'], 'FORMS/2/123')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_valid_unique_for_forms_biz(self):
        response = self.context_processor_class(self.service_context_instance).data
        self.assertEqual(response['unique'], 'FORMS/0/123')

    def test_startrek_fields__abc_services_with_string_value(self):
        name = 'Конструктор форм'
        service = AbcServiceFactory(name=name, translations={'name': {'ru': name}})
        field = dict(
            key={
                'type': 'array/service',
                'slug': 'abcService',
                'name': 'ABC',
            },
            value=name,
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'abcService': [{'id': service.abc_id}],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__abc_services_with_variable(self):
        name = 'Конструктор форм'
        service = AbcServiceFactory(name=name, translations={'name': {'ru': name}})
        self.survey.name = name
        self.survey.save()

        var = SurveyVariableFactory(
            hook_subscription=self.subscription,
            var='form.name',
            filters=[],
            arguments=[],
        )
        var.save()

        field = dict(
            key={
                'type': 'array/service',
                'slug': 'abcService',
                'name': 'ABC',
            },
            value=name,
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'abcService': [{'id': service.abc_id}],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__boards(self):
        field = dict(
            key={
                'type': 'array/board',
                'slug': 'boards',
                'name': 'Boards',
            },
            value='2531, 2532',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'boards': [2531, 2532],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_user_fields(self):
        self.startrek.author = 'George Washington (Washington)'
        self.startrek.assignee = 'adams'
        self.startrek.followers = ['Thomas Jefferson (TommyJ)', 'Medison']

        managers = dict(
            key={
                'type': 'array/user',
                'slug': 'managers',
                'name': 'Managers',
            },
            value='James Monroe (Monroe), Quincy',
        )

        superviser = dict(
            key={
                'type': 'user',
                'slug': 'superviser',
                'name': 'Superviser',
            },
            value='Andrew Jackson (Jackson)',
        )

        self.startrek.fields = [managers, superviser]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        self.assertEqual(response['author'], 'washington')
        self.assertEqual(response['assignee'], 'adams')
        self.assertListEqual(response['followers'], ['tommyj', 'medison'])
        self.assertListEqual(response['fields']['managers'], ['monroe', 'quincy'])
        self.assertEqual(response['fields']['superviser'], 'jackson')

    def test_startrek_fields__tags(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'tags',
                'name': 'Tags',
            },
            value='one, twoo, threee',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'tags': ['one', 'twoo', 'threee'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__components(self):
        field = dict(
            key={
                'type': 'array/component',
                'slug': 'components',
                'name': 'Components',
            },
            value='one, twoo, threee',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'components': ['one', 'twoo', 'threee'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__yandexService(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'yandexService',
                'name': 'Сервисы Яндекса',
            },
            value='forms, wiki, tracker',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'yandexService': ['forms', 'wiki', 'tracker'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__yandexApplications(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'yandexApplications',
                'name': 'Яндекс приложения',
            },
            value='forms, wiki, tracker',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'yandexApplications': ['forms', 'wiki', 'tracker'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__employment(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'employment',
                'name': 'Занятость',
            },
            value='полная, частичная, учеба',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'employment': ['полная', 'частичная', 'учеба'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__emailProvider(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'emailProvider',
                'name': 'Провайдер почты',
            },
            value='gmail, hotmail, mailru',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'emailProvider': ['gmail', 'hotmail', 'mailru'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__smartphoneExperience(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'smartphoneExperience',
                'name': 'Опыт на смартфоне',
            },
            value='фильмы, сериалы, музыка',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'smartphoneExperience': ['фильмы', 'сериалы', 'музыка'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__webSearchOnDesktop(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'webSearchOnDesktop',
                'name': 'Поиск на компьютере',
            },
            value='yandex, google, rambler',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'webSearchOnDesktop': ['yandex', 'google', 'rambler'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__webSearchOnSmartphone(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'webSearchOnSmartphone',
                'name': 'Поиск на смартфоне',
            },
            value='yandex, google, rambler',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'webSearchOnSmartphone': ['yandex', 'google', 'rambler'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__defaultBrowser(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'defaultBrowser',
                'name': 'Браузер на компьютере',
            },
            value='yandex.browser, google chrome, mozilla firefox',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'defaultBrowser': ['yandex.browser', 'google chrome', 'mozilla firefox'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__browserOnSmartphone(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'browserOnSmartphone',
                'name': 'Браузер на смартфоне',
            },
            value='yandex.browser, google chrome, mozilla firefox',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'browserOnSmartphone': ['yandex.browser', 'google chrome', 'mozilla firefox'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__transportApplications(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'transportApplications',
                'name': 'Используемый транспорт',
            },
            value='автомобиль, автобус, такси',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'transportApplications': ['автомобиль', 'автобус', 'такси'],
        }
        self.assertEqual(response['fields'], expected)

    def test_startrek_fields__stationNew(self):
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'stationNew',
                'name': 'Умная колонка',
            },
            value='Яндекс.Станция, Ирбис, JBL',
        )
        self.startrek.fields = [field]
        self.startrek.save()

        response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'stationNew': ['Яндекс.Станция', 'Ирбис', 'JBL'],
        }
        self.assertEqual(response['fields'], expected)

    def test_should_split_rendered_variable(self):
        var = SurveyVariableFactory(
            var='staff.meta_user',
            format_name='staff.hr_partner',
            hook_subscription=self.subscription,
        )
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'hrPartners',
                'name': 'HrPartners',
            },
            value='{%s}' % var.variable_id,
        )
        self.startrek.fields = [field]
        self.startrek.save()

        with patch('events.surveyme_integration.services.base.context_processors.fields.render_variables',
                   return_value='one,twoo,threee'):
            response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'hrPartners': ['one', 'twoo', 'threee'],
        }
        self.assertEqual(response['fields'], expected)

    def test_shouldnt_split_rendered_variable(self):
        var = SurveyVariableFactory(
            var='staff.meta_user',
            format_name='staff.department',
            hook_subscription=self.subscription,
        )
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'departments',
                'name': 'Departments',
            },
            value='{%s}' % var.variable_id,
        )
        self.startrek.fields = [field]
        self.startrek.save()

        with patch('events.surveyme_integration.services.base.context_processors.fields.render_variables',
                   return_value='one,twoo,threee'):
            response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'departments': ['one,twoo,threee'],
        }
        self.assertEqual(response['fields'], expected)

    def test_should_split_rendered_variable_exclude_blanks(self):
        var = SurveyVariableFactory(
            var='staff.meta_user',
            format_name='staff.hr_partner',
            hook_subscription=self.subscription,
        )
        field = dict(
            key={
                'type': 'array/string',
                'slug': 'hrPartners',
                'name': 'HrPartners',
            },
            value='{%s}' % var.variable_id,
        )
        self.startrek.fields = [field]
        self.startrek.save()

        with patch('events.surveyme_integration.services.base.context_processors.fields.render_variables',
                   return_value=',twoo,threee'):
            response = self.context_processor_class(self.service_context_instance).data
        expected = {
            'hrPartners': ['twoo', 'threee'],
        }
        self.assertEqual(response['fields'], expected)
