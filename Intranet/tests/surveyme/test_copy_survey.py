# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings
from django.utils import timezone
from events.accounts.models import Organization
from events.accounts.factories import UserFactory
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.copiers import SurveyQuestionCopier, SurveyCopier
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyStyleTemplateFactory,
)
from events.surveyme.models import (
    AnswerType,
    Survey,
    SurveySubmitConditionNode,
    SurveySubmitConditionNodeItem,
    SurveyQuestion,
    SurveyQuestionChoice,
    SurveyQuestionMatrixTitle,
    SurveyText,
    ValidatorType,
)
from events.surveyme_integration.models import (
    IntegrationFileTemplate,
    JSONRPCSubscriptionData,
    JSONRPCSubscriptionParam,
    ServiceSurveyHookSubscription,
    StartrekSubscriptionData,
    SubscriptionAttachment,
    SubscriptionHeader,
    SurveyHook,
    SurveyVariable,
    WikiSubscriptionData,
)
from events.surveyme_integration.factories import (
    SurveyHookConditionFactory,
    SurveyHookConditionNodeFactory,
    SurveyHookFactory,
    ServiceSurveyHookSubscriptionFactory,
    SurveyVariableFactory,
    StartrekSubscriptionDataFactory,
)
from events.surveyme.helpers import JsonFixtureMixin
from events.surveyme.survey_importer import SurveyImporter
from events.accounts.helpers import YandexClient


class TestSurveyQuestionCopier(TestCase, JsonFixtureMixin):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.org, _ = Organization.objects.get_or_create(dir_id=123)
        json_string = self.get_json_from_file('test_creating_images.json')
        importer = SurveyImporter.from_string(json_string)
        self.survey = importer.import_survey(self.user.pk, dir_id='123')
        self.old_question = SurveyQuestion.objects.filter(survey=self.survey)[0]

    def test_copy_single_question(self):
        self.assertEqual(SurveyQuestion.objects.filter(survey=self.survey).count(), 2)
        with self.assertNumQueries(8):
            new_question = SurveyQuestionCopier().copy(self.old_question)[0]
        self.assertEqual(SurveyQuestion.objects.filter(survey=self.survey).count(), 3)
        self.assertTrue(new_question in SurveyQuestion.objects.filter(survey=self.survey))

    def test_copy_question_params(self):
        params = (
            'answer_type_id',
            'validator_type_id',
            'page',
            'initial',
            'param_is_required',
            'param_is_allow_multiple_choice',
            'param_is_allow_other',
            'param_max',
            'param_min',
            'param_is_section_header',
            'param_max_file_size',
            'param_price',
            'param_variables',
            'param_widget',
            'param_is_hidden',
            'param_hint_type_id',
            'param_data_source',
            'param_data_source_params',
            'param_hint_data_source',
            'param_hint_data_source_params',
            'param_is_random_choices_position',
            'param_modify_choices',
            'param_max_files_count',
            'param_is_disabled_init_item',
            'param_date_field_type',
            'param_date_field_min',
            'param_date_field_max',
            'param_suggest_choices',
            'param_help_text',
        )
        new_question = SurveyQuestionCopier().copy(self.old_question)[0]
        old_question_params = SurveyQuestion.objects.filter(survey=self.survey).values(*params)[0]
        new_question_params = SurveyQuestion.objects.filter(survey=self.survey).values(*params)[2]
        self.assertEqual(old_question_params, new_question_params)
        self.assertTrue(new_question.param_slug == '%s_%s' % (new_question.answer_type.slug, new_question.pk))

    def test_copy_question_choices(self):
        self.assertEqual(SurveyQuestionChoice.objects.filter(survey_question__survey=self.survey).count(), 3)
        SurveyQuestionCopier().copy(SurveyQuestion.objects.filter(survey=self.survey)[1])
        self.assertEqual(SurveyQuestionChoice.objects.filter(survey_question__survey=self.survey).count(), 6)

    def test_copy_question_matrix_titles(self):
        json_string = self.get_json_from_file('test_creating_matrixes.json')
        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')
        self.assertEqual(SurveyQuestionMatrixTitle.objects.filter(survey_question__survey=survey).count(), 20)

        questions = SurveyQuestion.objects.filter(survey=survey)
        with self.assertNumQueries(12):
            SurveyQuestionCopier().copy(*questions)
        self.assertEqual(SurveyQuestionMatrixTitle.objects.filter(survey_question__survey=survey).count(), 40)

    def test_copy_question_with_new_default_label(self):
        old_question = SurveyQuestion.objects.get(pk=self.old_question.pk)
        new_question = SurveyQuestionCopier().copy(self.old_question)[0]
        self.assertNotEqual(old_question.pk, new_question.pk)
        self.assertEqual(u'Копия "%s"' % old_question.label, new_question.label)

    def test_copy_question_with_new_custom_label(self):
        old_question = SurveyQuestion.objects.get(pk=self.old_question.pk)
        new_question = SurveyQuestionCopier(label='Test label').copy(self.old_question)[0]
        self.assertNotEqual(old_question.pk, new_question.pk)
        self.assertEqual(new_question.label, 'Test label')

    def test_copy_question_without_label_changes(self):
        old_question = SurveyQuestion.objects.get(pk=self.old_question.pk)
        new_question = SurveyQuestionCopier(label=old_question.label).copy(self.old_question)[0]
        self.assertNotEqual(old_question.pk, new_question.pk)
        self.assertEqual(old_question.label, new_question.label)

    def test_copy_group_with_subquestions(self):
        json_string = self.get_json_from_file('test_creating_questions.json')
        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')
        self.assertEqual(SurveyQuestion.objects.filter(survey=survey).count(), 17)

        old_question = SurveyQuestion.objects.get(param_slug='answer_group_64367')
        with self.assertNumQueries(8):
            SurveyQuestionCopier().copy(old_question)
        self.assertEqual(SurveyQuestion.objects.filter(survey=survey).count(), 20)

    def test_copy_questions_group_without_subquestions_labels_change(self):
        json_string = self.get_json_from_file('test_creating_questions.json')
        importer = SurveyImporter.from_string(json_string)
        importer.import_survey(self.user.pk, dir_id='123')
        old_question = SurveyQuestion.objects.get(param_slug='answer_group_64367')
        old_group_labels = set(SurveyQuestion.objects.filter(group_id=old_question).values_list('label'))
        with self.assertNumQueries(8):
            new_question = SurveyQuestionCopier().copy(old_question)[0]
        new_group_labels = set(SurveyQuestion.objects.filter(group_id=new_question).values_list('label'))
        self.assertEqual(old_group_labels, new_group_labels)

    def test_copy_questions_list(self):
        json_string = self.get_json_from_file('test_creating_questions.json')
        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')
        self.assertEqual(SurveyQuestion.objects.filter(survey=survey).count(), 17)

        survey = Survey.objects.get(pk=survey.pk)
        old_questions = list(survey.surveyquestion_set.select_related('answer_type'))
        # old_questions = SurveyQuestion.objects.filter(survey=survey).select_related('answer_type', 'survey')
        with self.assertNumQueries(8):
            new_questions = SurveyQuestionCopier().copy(*old_questions)
        self.assertEqual(SurveyQuestion.objects.filter(survey=survey).count(), 34)
        self.assertEqual(len(new_questions), 17)

    def test_should_copy_question_with_param_is_required(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_is_required=True,
        )
        old_question_pk = question.pk
        (new_question, ) = SurveyQuestionCopier().copy(question)
        self.assertNotEqual(old_question_pk, new_question.pk)
        self.assertTrue(new_question.param_is_required)

    def test_should_copy_group_question_with_param_is_required(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            param_is_required=True,
        )
        old_question_pk = question.pk
        (new_question, ) = SurveyQuestionCopier().copy(question)
        self.assertNotEqual(old_question_pk, new_question.pk)
        self.assertFalse(new_question.param_is_required)

    def test_should_copy_grouped_question_with_param_is_required(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group_id=group_question.pk,
            param_is_required=True,
        )
        old_question_pk = question.pk
        (new_question, ) = SurveyQuestionCopier().copy(question)
        self.assertNotEqual(old_question_pk, new_question.pk)
        self.assertTrue(new_question.param_is_required)


class TestTranslatedSurveyQuestionCopier(TestCase, JsonFixtureMixin):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)

    def test_copy_translated_question_ru(self):
        survey = SurveyFactory(language='ru')
        question = SurveyQuestionFactory(
            survey=survey,
            label='Вопрос',
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            translations={
                'label': {
                    'ru': 'Вопрос',
                },
            },
        )
        response = self.client.post('/admin/api/v2/survey-questions/%s/copy/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)
        new_question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(new_question.label, 'Копия "Вопрос"')
        self.assertEqual(new_question.translations['label']['ru'], 'Копия "Вопрос"')

    def test_copy_translated_question_ru_accept_en(self):
        survey = SurveyFactory(language='ru')
        question = SurveyQuestionFactory(
            survey=survey,
            label='Вопрос',
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            translations={
                'label': {
                    'ru': 'Вопрос',
                    'en': 'Answer',
                },
            },
        )
        response = self.client.post('/admin/api/v2/survey-questions/%s/copy/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)
        new_question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(new_question.label, 'Вопрос')
        self.assertEqual(new_question.translations['label']['ru'], 'Вопрос')
        self.assertEqual(new_question.translations['label']['en'], 'Copy "Answer"')

    def test_copy_translated_question_en(self):
        survey = SurveyFactory(language='en')
        question = SurveyQuestionFactory(
            survey=survey,
            label='Answer',
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            translations={
                'label': {
                    'en': 'Answer',
                },
            },
        )
        response = self.client.post('/admin/api/v2/survey-questions/%s/copy/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)
        new_question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(new_question.label, 'Copy "Answer"')
        self.assertEqual(new_question.translations['label']['en'], 'Copy "Answer"')

    def test_copy_translated_question_en_accept_ru(self):
        survey = SurveyFactory(language='en')
        question = SurveyQuestionFactory(
            survey=survey,
            label='Answer',
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            translations={
                'label': {
                    'ru': 'Вопрос',
                    'en': 'Answer',
                },
            },
        )
        response = self.client.post('/admin/api/v2/survey-questions/%s/copy/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)
        new_question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(new_question.label, 'Answer')
        self.assertEqual(new_question.translations['label']['en'], 'Answer')
        self.assertEqual(new_question.translations['label']['ru'], 'Копия "Вопрос"')


class TestSurveyCopier(TestCase, JsonFixtureMixin):
    fixtures = ['initial_data.json']

    def setUp(self):
        ContentTypeAttributeFactory(pk=2, lookup_field='answer_choices')
        ContentTypeAttributeFactory(pk=12, lookup_field='answer_long_text')
        self.user = UserFactory()
        self.org, _ = Organization.objects.get_or_create(dir_id=123)
        json_string = self.get_json_from_file('test_creating_images.json')
        importer = SurveyImporter.from_string(json_string)
        self.survey = importer.import_survey(self.user.pk, dir_id='123')

    def test_copy_survey(self):
        old_survey_name = self.survey.name
        self.assertEqual(Survey.objects.count(), 1)
        self.survey.is_published_external = True
        self.survey.save()

        with self.assertNumQueries(16):
            new_survey = SurveyCopier(self.survey).copy(self.user.pk)
        new_survey_name = new_survey.name
        self.assertEqual(Survey.objects.count(), 2)
        self.assertEqual(u'Копия "{}"'.format(old_survey_name), new_survey_name)
        self.assertIsNone(new_survey.date_unpublished)
        self.assertFalse(new_survey.is_published_external)

    def test_copy_empty_survey(self):
        survey = SurveyFactory()
        with self.assertNumQueries(10):
            new_survey = SurveyCopier(survey).copy(self.user.pk)
        self.assertEqual(
            len(SurveyQuestion.objects.filter(survey_id=survey.pk)),
            len(SurveyQuestion.objects.filter(survey_id=new_survey.pk))
        )
        self.assertEqual(len(SurveyQuestion.objects.filter(survey_id=survey.pk)), 0)

    def test_copy_survey_params(self):
        params = (
            'group_id',
            'user_id',
            'org_id',
            'type',
            'need_auth_to_answer',
            'is_published_external',
            'is_public',
            'is_only_for_iframe',
            'is_allow_answer_editing',
            'is_allow_multiple_answers',
            'is_allow_answer_versioning',
            'is_remove_answer_after_integration',
            'is_send_user_notification_email_message',
            'user_notification_email_message_subject',
            'user_notification_email_message_text',
            'user_notification_email_frontend_name',
            'agreements',
            'content_type_id',
            'object_id',
            'cached_unicode_value',
            'is_recent_data_is_sended_to_localize',
            'metrika_counter_code',
            'captcha_display_mode',
            'captcha_type',
            'slug',
            'maximum_answers_count',
            'auto_control_publication_status',
            'validator_url',
            'is_deleted',
            'styles_template_id',
            'users_count',
            'groups_count',
        )
        old_survey_pk = self.survey.pk
        new_survey_pk = SurveyCopier(self.survey).copy(self.user.pk).pk
        old_survey_params = list(Survey.objects.filter(pk=old_survey_pk).values(*params))
        new_survey_params = list(Survey.objects.filter(pk=new_survey_pk).values(*params))
        self.assertEqual(old_survey_params, new_survey_params)

    def test_copy_survey_with_questions(self):
        old_survey_pk = self.survey.pk
        new_survey_pk = SurveyCopier(self.survey).copy(self.user.pk).pk
        self.assertEqual(
            SurveyQuestion.objects.filter(survey_id=old_survey_pk).count(),
            SurveyQuestion.objects.filter(survey_id=new_survey_pk).count(),
        )
        params = (
            'label',
            'position',
            'is_hidden',
            'label_image',
        )
        self.assertEqual(
            {frozenset(obj.items()) for obj in SurveyQuestionChoice.objects.filter(survey_question__survey_id=old_survey_pk).values(*params)},
            {frozenset(obj.items()) for obj in SurveyQuestionChoice.objects.filter(survey_question__survey_id=new_survey_pk).values(*params)},
        )
        params = (
            'label',
            'type',
            'position',
        )
        self.assertEqual(
            list(SurveyQuestionMatrixTitle.objects.filter(survey_question__survey_id=old_survey_pk).values(*params)).sort(),
            list(SurveyQuestionMatrixTitle.objects.filter(survey_question__survey_id=new_survey_pk).values(*params)).sort(),
        )

    def test_copy_survey_with_submit_conditions(self):
        json_string = self.get_json_from_file('test_creating_submit_conditions_nodes.json')
        importer = SurveyImporter.from_string(json_string)
        old_survey_pk = importer.import_survey(self.user.pk, dir_id='123').pk
        survey = Survey.objects.get(pk=old_survey_pk)
        with self.assertNumQueries(18):
            new_survey_pk = SurveyCopier(survey).copy(self.user.pk).pk

        params = (
            'position',
        )
        self.assertEqual(
            list(SurveySubmitConditionNode.objects.filter(survey_id=old_survey_pk).values(*params)),
            list(SurveySubmitConditionNode.objects.filter(survey_id=new_survey_pk).values(*params)),
        )
        params = (
            'operator',
            'content_type_attribute_id',
            'condition',
            'position',
        )
        self.assertEqual(
            {frozenset(obj.items()) for obj in SurveySubmitConditionNodeItem.objects.filter(survey_submit_condition_node__survey_id=old_survey_pk).values(*params)},
            {frozenset(obj.items()) for obj in SurveySubmitConditionNodeItem.objects.filter(survey_submit_condition_node__survey_id=new_survey_pk).values(*params)},
        )

    def test_copy_survey_with_texts(self):
        old_survey_pk = self.survey.pk
        new_survey_pk = SurveyCopier(self.survey).copy(self.user.pk).pk

        params = (
            'slug',
            'max_length',
            'null',
            'value',
        )
        self.assertEqual(
            {frozenset(obj.items()) for obj in SurveyText.objects.filter(survey_id=old_survey_pk).values(*params)},
            {frozenset(obj.items()) for obj in SurveyText.objects.filter(survey_id=new_survey_pk).values(*params)},
        )

    def test_copy_survey_with_integrations(self):
        json_string = self.get_json_from_file('test_creating_integrations_params.json')
        importer = SurveyImporter.from_string(json_string)
        old_survey_pk = importer.import_survey(self.user.pk, dir_id='123').pk
        survey = Survey.objects.get(pk=old_survey_pk)
        with self.assertNumQueries(36):
            new_survey_pk = SurveyCopier(survey).copy(self.user.pk).pk
        params = (
            'name',
            'template',
            'type',
            'slug',
        )
        self.assertEqual(
            list(IntegrationFileTemplate.objects.filter(survey_id=old_survey_pk).values(*params)).sort(),
            list(IntegrationFileTemplate.objects.filter(survey_id=new_survey_pk).values(*params)).sort(),
        )
        params = (
            'method',
        )
        self.assertEqual(
            list(JSONRPCSubscriptionData.objects.filter(subscription__survey_hook__survey_id=old_survey_pk).values(*params)).sort(),
            list(JSONRPCSubscriptionData.objects.filter(subscription__survey_hook__survey_id=new_survey_pk).values(*params)).sort(),
        )
        params = (
            'name',
            'value',
            'add_only_with_value',
        )
        self.assertEqual(
            {frozenset(obj.items()) for obj in JSONRPCSubscriptionParam.objects.filter(subscription__subscription__survey_hook__survey_id=old_survey_pk).values(*params)},
            {frozenset(obj.items()) for obj in JSONRPCSubscriptionParam.objects.filter(subscription__subscription__survey_hook__survey_id=new_survey_pk).values(*params)}
        )
        params = (
            'service_type_action_id',
            'is_synchronous',
            'is_active',
            'context_language',
            'title',
            'body',
            'email',
            'phone',
            'is_all_questions',
            'attachment_templates',
            'email_to_address',
            'email_from_address',
            'email_from_title',
            'email_spam_check',
            'http_url',
            'http_method',
            'http_format_name',
        )
        self.assertEqual(
            list(ServiceSurveyHookSubscription.objects.filter(survey_hook__survey_id=old_survey_pk).values(*params)).sort(),
            list(ServiceSurveyHookSubscription.objects.filter(survey_hook__survey_id=new_survey_pk).values(*params)).sort(),
        )

        params = (
            'queue',
            'parent',
            'author',
            'assignee',
            'followers',
            'tags',
            'type',
            'components',
            'project',
            'priority',
            'fields',
        )
        self.assertEqual(
            list(StartrekSubscriptionData.objects.filter(subscription__survey_hook__survey_id=old_survey_pk).values(*params)).sort(),
            list(StartrekSubscriptionData.objects.filter(subscription__survey_hook__survey_id=new_survey_pk).values(*params)).sort(),
        )
        params = (
            'file',
        )
        self.assertEqual(
            list(SubscriptionAttachment.objects.filter(subscription__survey_hook__survey_id=old_survey_pk).values(*params)).sort(),
            list(SubscriptionAttachment.objects.filter(subscription__survey_hook__survey_id=new_survey_pk).values(*params)).sort(),
        )
        params = (
            'name',
            'value',
            'add_only_with_value',
        )
        self.assertEqual(
            list(SubscriptionHeader.objects.filter(subscription__survey_hook__survey_id=old_survey_pk).values(*params)).sort(),
            list(SubscriptionHeader.objects.filter(subscription__survey_hook__survey_id=new_survey_pk).values(*params)).sort(),
        )
        params = (
            'triggers',
            'is_active',
        )
        self.assertEqual(
            {frozenset(obj.items()) for obj in SurveyHook.objects.filter(survey_id=old_survey_pk).values(*params)},
            {frozenset(obj.items()) for obj in SurveyHook.objects.filter(survey_id=new_survey_pk).values(*params)},
        )
        params = (
            'integration_file_template',
            'var',
            'format_name',
            'filters',
        )
        old_variables = SurveyVariable.objects.filter(hook_subscription__survey_hook__survey_id=old_survey_pk)
        new_variables = SurveyVariable.objects.filter(hook_subscription__survey_hook__survey_id=new_survey_pk)
        self.assertListEqual(
            list(old_variables.values_list(*params)),
            list(new_variables.values_list(*params)),
        )
        params = (
            'supertag',
            'text',
        )
        self.assertEqual(
            list(WikiSubscriptionData.objects.filter(subscription__survey_hook__survey_id=old_survey_pk).values(*params)).sort(),
            list(WikiSubscriptionData.objects.filter(subscription__survey_hook__survey_id=new_survey_pk).values(*params)).sort(),
        )

    def test_should_correct_import_varables(self):
        variables_qs = lambda survey_pk: (
            SurveyVariable.objects.all()
            .filter(hook_subscription__survey_hook__survey_id=survey_pk)
        )
        json_string = self.get_json_from_file('test_should_correct_import_varables.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)
        question = SurveyQuestion.objects.get(survey_id=survey.pk, label='field1')

        for variable in variables_qs(survey.pk):
            if variable.var == 'form.questions_answers':
                variable.arguments['questions'] = [question.pk, 1234567]
            elif variable.var == 'form.question_answer':
                if variable.arguments['question'] is None:
                    variable.arguments['question'] = 1234567
                else:
                    variable.arguments['question'] = question.pk
            variable.save()

        new_survey = SurveyCopier(survey).copy(self.user.pk)

        variables = list(variables_qs(new_survey.pk))
        self.assertEqual(len(variables), 4)

        new_question = SurveyQuestion.objects.get(survey_id=new_survey.pk, label='field1')

        var_all_questions, var_not_all_questions, var_valid_question, var_invalid_question = None, None, None, None
        for variable in variables:
            if variable.var == 'form.questions_answers':
                if variable.arguments['is_all_questions']:
                    var_all_questions = variable
                else:
                    var_not_all_questions = variable
            elif variable.var == 'form.question_answer':
                if variable.arguments['question'] is not None:
                    var_valid_question = variable
                else:
                    var_invalid_question = variable

        self.assertIsNotNone(var_all_questions)
        self.assertEqual(var_all_questions.arguments['questions'], [])

        self.assertIsNotNone(var_not_all_questions)
        self.assertEqual(var_not_all_questions.arguments['questions'], [new_question.pk])

        self.assertIsNotNone(var_valid_question)
        self.assertEqual(var_valid_question.arguments['question'], new_question.pk)

        self.assertIsNotNone(var_invalid_question)
        self.assertIsNone(var_invalid_question.arguments['question'])

    def test_should_create_questions_in_original_order(self):
        json_string = self.get_json_from_file('test_should_create_questions_in_original_order.json')
        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)
        expected = ['field1', 'field2', 'field3']

        labels = list(
            SurveyQuestion.objects.all()
            .filter(survey_id=survey.pk)
            .order_by('pk')
            .values_list('label', flat=True)
        )
        self.assertEqual(labels, expected)

        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        labels = list(
            SurveyQuestion.objects.all()
            .filter(survey_id=copied_survey.pk)
            .order_by('pk')
            .values_list('label', flat=True)
        )
        self.assertEqual(labels, expected)

    def test_should_drop_survey_slug(self):
        survey = SurveyFactory(slug='test_should_drop_survey_slug')
        self.assertEqual(survey.slug, 'test_should_drop_survey_slug')

        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        self.assertIsNone(copied_survey.slug)

    def test_should_link_default_styles_template(self):
        styles_template = SurveyStyleTemplateFactory(
            type='default',
            styles={
                'display': 'none',
            },
        )
        survey = SurveyFactory(styles_template=styles_template)

        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        self.assertEqual(copied_survey.styles_template.pk, styles_template.pk)
        self.assertEqual(copied_survey.styles_template.name, styles_template.name)
        self.assertEqual(copied_survey.styles_template.type, styles_template.type)
        self.assertEqual(copied_survey.styles_template.styles, styles_template.styles)

    def test_should_create_custom_styles_template(self):
        styles_template = SurveyStyleTemplateFactory(
            type='custom',
            styles={
                'display': 'none',
            },
        )
        survey = SurveyFactory(styles_template=styles_template)

        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        self.assertNotEqual(copied_survey.styles_template.pk, styles_template.pk)
        self.assertEqual(copied_survey.styles_template.name, styles_template.name)
        self.assertEqual(copied_survey.styles_template.type, styles_template.type)
        self.assertEqual(copied_survey.styles_template.styles, styles_template.styles)

    def test_should_copy_with_correct_subscription_questions(self):
        json_string = self.get_json_from_file('test_should_copy_with_correct_subscription_questions.json')
        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        questions = list(survey.surveyquestion_set.values_list('pk', flat=True))
        self.assertEqual(len(questions), 2)

        subscription_questions = list(
            question_pk
            for subscription in ServiceSurveyHookSubscription.objects.filter(
                survey_hook__survey_id=survey.pk,
            )
            for question_pk in subscription.questions.values_list('pk', flat=True)
        )
        self.assertEqual(len(subscription_questions), 1)
        self.assertTrue(set(subscription_questions).issubset(set(questions)))

        copied_survey = SurveyCopier(survey).copy(self.user.pk)

        copied_questions = list(copied_survey.surveyquestion_set.values_list('pk', flat=True))
        self.assertEqual(len(copied_questions), 2)

        copied_subscription_questions = list(
            question_pk
            for subscription in ServiceSurveyHookSubscription.objects.filter(
                survey_hook__survey_id=copied_survey.pk,
            )
            for question_pk in subscription.questions.values_list('pk', flat=True)
        )
        self.assertEqual(len(copied_subscription_questions), 1)
        self.assertTrue(set(copied_subscription_questions).issubset(set(copied_questions)))

        self.assertNotEqual(set(questions), set(copied_questions))
        self.assertNotEqual(set(subscription_questions), set(copied_subscription_questions))

    @override_settings(IS_BUSINESS_SITE=True)
    def test_copy_in_b2b_should_be_published(self):
        survey = SurveyFactory(name='Test form', is_published_external=False)
        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.date_published)

        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        self.assertTrue(copied_survey.is_published_external)
        self.assertIsNotNone(copied_survey.date_published)

    @override_settings(IS_BUSINESS_SITE=False)
    def test_copy_in_int_shouldnt_be_published(self):
        survey = SurveyFactory(name='Test form', is_published_external=True)
        self.assertTrue(survey.is_published_external)
        self.assertIsNotNone(survey.date_published)

        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        self.assertFalse(copied_survey.is_published_external)
        self.assertIsNone(copied_survey.date_published)

    def test_should_create_counters(self):
        survey = SurveyFactory()
        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        new_survey = Survey.objects.get(pk=copied_survey.pk)
        self.assertIsNotNone(new_survey.answercount)

    def test_copy_forms_with_wrong_hook_conditions(self):
        survey = SurveyFactory()
        SurveyQuestionFactory(survey=survey)
        fake_question = SurveyQuestionFactory()
        hook = SurveyHookFactory(survey=survey)
        condition_node = SurveyHookConditionNodeFactory(hook=hook)
        SurveyHookConditionFactory(
            condition_node=condition_node,
            survey_question=fake_question,
        )
        survey_pk = survey.pk
        copied_survey = SurveyCopier(survey).copy(self.user.pk)
        self.assertNotEqual(survey_pk, copied_survey.pk)

    def test_copy_form_with_email_from_title(self):
        json_string = self.get_json_from_file('test_import_email_from_title.json')

        importer = SurveyImporter.from_string(json_string)
        old_survey = importer.import_survey(self.user.pk, dir_id=None)

        old_subscription = old_survey.hooks.first().subscriptions.first()
        old_variable = old_subscription.email_from_title.strip('{}')
        old_ref_question = old_subscription.variables_map[old_variable].arguments['question']

        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_subscription = new_survey.hooks.first().subscriptions.first()
        new_variable = new_subscription.email_from_title.strip('{}')
        new_ref_question = new_subscription.variables_map[new_variable].arguments['question']

        self.assertNotEqual(new_variable, old_variable)
        self.assertNotEqual(new_ref_question, old_ref_question)

    @override_settings(APP_TYPE='forms_ext')
    def test_should_set_email_spam_check_for_ext(self):
        json_string = self.get_json_from_file('test_should_set_email_spam_check.json')

        importer = SurveyImporter.from_string(json_string)
        old_survey = importer.import_survey(self.user.pk, dir_id=None)
        old_subscription = old_survey.hooks.first().subscriptions.first()
        self.assertTrue(old_subscription.email_spam_check)

        old_subscription.email_spam_check = False
        old_subscription.save()

        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_subscription = new_survey.hooks.first().subscriptions.first()
        self.assertTrue(new_subscription.email_spam_check)

    @override_settings(APP_TYPE='forms_int')
    def test_shouldnt_set_email_spam_check_for_int(self):
        json_string = self.get_json_from_file('test_should_set_email_spam_check.json')

        importer = SurveyImporter.from_string(json_string)
        old_survey = importer.import_survey(self.user.pk, dir_id=None)
        old_subscription = old_survey.hooks.first().subscriptions.first()
        self.assertFalse(old_subscription.email_spam_check)

        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_subscription = new_survey.hooks.first().subscriptions.first()
        self.assertFalse(new_subscription.email_spam_check)

    def test_should_convert_variables_in_json_rpc(self):
        json_string = self.get_json_from_file('test_should_convert_variables_in_json_rpc.json')

        importer = SurveyImporter.from_string(json_string)
        old_survey = importer.import_survey(self.user.pk, dir_id=None)

        old_hook = old_survey.hooks.first()
        old_subscription = old_hook.subscriptions.first()

        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_hook = new_survey.hooks.first()
        new_subscription = new_hook.subscriptions.first()
        new_variables = new_subscription.variables_map

        # сравниваем с исходными данными
        self.assertNotEqual(new_subscription.http_url, old_subscription.http_url)
        self.assertNotEqual(new_subscription.json_rpc.method, old_subscription.json_rpc.method)

        # сравниваем реальные данные
        self.assertIn(new_subscription.http_url[1:-1], new_variables)
        self.assertIn(new_subscription.json_rpc.method[1:-1], new_variables)

    def test_copy_survey_with_number_of_variables_in_one_field(self):
        subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=3,
            email_to_address='user@company.com',
        )
        variables = [
            SurveyVariableFactory(
                hook_subscription=subscription,
                var='form.id',
            ),
            SurveyVariableFactory(
                hook_subscription=subscription,
                var='form.name',
            ),
        ]
        variables_set = {v.variable_id for v in variables}
        subscription.body = '{%s} {%s}' % (variables[0].variable_id, variables[1].variable_id)
        subscription.save()

        old_survey = subscription.survey_hook.survey
        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_hook = new_survey.hooks.first()
        self.assertIsNotNone(new_hook)

        new_subscription = new_hook.subscriptions.first()
        self.assertIsNotNone(new_subscription)
        self.assertNotEqual(new_subscription.body, subscription.body)

        new_variables = list(new_subscription.surveyvariable_set.all())
        new_variables_set = {v.variable_id for v in new_variables}
        self.assertEqual(len(new_variables), 2)
        self.assertIn(new_variables[0].variable_id, new_subscription.body)
        self.assertIn(new_variables[1].variable_id, new_subscription.body)
        self.assertNotEqual(new_variables_set, variables_set)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_copy_survey_with_patched_email_from_address_biz(self):
        subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=3,
            email_to_address='user@company.com',
        )
        old_survey = subscription.survey_hook.survey
        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_hook = new_survey.hooks.first()
        self.assertIsNotNone(new_hook)

        new_subscription = new_hook.subscriptions.first()
        self.assertIsNotNone(new_subscription)
        self.assertEqual(new_subscription.email_from_address, f'{new_survey.pk}@forms-mailer.yaconnect.com')

    def test_shouldnt_copy_date_archived(self):
        self.survey.date_archived = timezone.now().date()
        self.survey.date_exported = timezone.now()
        self.survey.save()

        new_survey = SurveyCopier(self.survey).copy(self.user.pk)
        self.assertIsNone(new_survey.date_archived)
        self.assertIsNone(new_survey.date_exported)

    def test_should_copy_variables_in_tracker_fields(self):
        startrek = StartrekSubscriptionDataFactory()
        subscription = startrek.subscription
        variables = [
            SurveyVariableFactory(
                hook_subscription=subscription,
                var='form.id',
            ),
            SurveyVariableFactory(
                hook_subscription=subscription,
                var='form.name',
            ),
        ]
        variables_set = {v.variable_id for v in variables}
        startrek.fields = [
            {'key': 'form.id', 'value': f'{{{variables[0].variable_id}}}'},
            {'key': 'form.name', 'value': f'{{{variables[1].variable_id}}}'},
        ]
        startrek.save()

        old_survey = subscription.survey_hook.survey
        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_hook = new_survey.hooks.first()
        self.assertIsNotNone(new_hook)

        new_subscription = new_hook.subscriptions.first()
        new_startrek = new_subscription.startrek
        self.assertIsNotNone(new_startrek)
        self.assertEqual(len(new_startrek.fields), len(startrek.fields))

        old_fields = {f['key']: f['value'] for f in startrek.fields}
        new_fields = {f['key']: f['value'] for f in new_startrek.fields}
        self.assertNotEqual(old_fields['form.id'], new_fields['form.id'])
        self.assertNotEqual(old_fields['form.name'], new_fields['form.name'])

        new_variables = list(new_subscription.surveyvariable_set.all())
        new_variables_set = {v.variable_id for v in new_variables}
        self.assertEqual(len(new_variables), 2)
        self.assertIn(new_variables[0].variable_id, new_fields['form.id'])
        self.assertIn(new_variables[1].variable_id, new_fields['form.name'])
        self.assertNotEqual(new_variables_set, variables_set)

    def test_copy_survey_with_variable_with_null_in_arguments(self):
        subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=3,
            email_to_address='user@company.com',
        )
        variable = SurveyVariableFactory(
            hook_subscription=subscription,
            var='form.id',
            arguments=None,
        )
        subscription.body = f'{{{variable.variable_id}}}'
        subscription.save()

        old_survey = subscription.survey_hook.survey
        new_survey = SurveyCopier(old_survey).copy(self.user.pk)

        new_hook = new_survey.hooks.first()
        self.assertIsNotNone(new_hook)

        new_subscription = new_hook.subscriptions.first()
        self.assertIsNotNone(new_subscription)
        self.assertNotEqual(new_subscription.body, subscription.body)

        new_variable = new_subscription.surveyvariable_set.first()
        self.assertIsNotNone(new_variable)
        self.assertIn(new_variable.variable_id, new_subscription.body)
        self.assertNotEqual(new_variable.variable_id, variable.variable_id)
        self.assertEqual(new_variable.arguments, {})
        self.assertIsNone(variable.arguments)


class TestSurveyQuestionCopySerializer(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.question = SurveyQuestionFactory()

    def test_validation_options(self):
        self.question.validator_type = ValidatorType.objects.get(slug='regexp')
        self.question.validator_options = {'regexp': r'\w+'}
        self.question.save()

        response = self.client.post('/admin/api/v2/survey-questions/%s/copy/' % self.question.pk)
        self.assertEqual(response.status_code, 201)

        self.assertNotEqual(response.data['id'], self.question.pk)
        self.assertEqual(response.data['validator_type_id'], self.question.validator_type_id)
        self.assertDictEqual(response.data['validator_options'], self.question.validator_options)
