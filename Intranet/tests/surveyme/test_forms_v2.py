# -*- coding: utf-8 -*-
import responses

from collections import namedtuple
from unittest.mock import patch, ANY
from django.conf import settings
from django.test import TestCase, override_settings
from django.utils.translation import ugettext as _, override
from events.accounts.models import User
from events.accounts.factories import (
    OrganizationFactory,
    UserFactory,
)
from events.data_sources.factories import TableRowFactory
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyFactory,
    SurveyGroupFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
    SurveyQuestionShowConditionNodeFactory,
    SurveyQuestionShowConditionNodeItemFactory,
    SurveyStyleTemplateFactory,
    SurveySubmitConditionNodeFactory,
    SurveySubmitConditionNodeItemFactory,
)
from events.media.factories import ImageFactory
from events.music.factories import MusicGenreFactory
from events.surveyme_keys.factories import (
    SurveyKeysBundleFactory,
    SurveyKeyFactory,
)
from events.surveyme.models import (
    AnswerType,
    Survey,
    SurveyAgreement,
)
from events.surveyme.forms_v2 import (
    FormClass,
    get_translated,
)


class TestFormClass(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(user=self.user)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.language = 'ru'

    def test_should_raise_exception_survey_does_not_exist(self):
        with self.assertRaises(Survey.DoesNotExist):
            FormClass(99999999)

        with self.assertRaises(Survey.DoesNotExist):
            FormClass('some_slug_99999999')

    def test_should_return_form_by_its_slug(self):
        self.survey.slug = 'my-form'
        self.survey.save()

        form_class = FormClass('my-form')
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])

        # целочисленные слаги должны приводить к поиску по id
        self.survey.slug = '123454321'
        self.survey.save()

        with self.assertRaises(Survey.DoesNotExist):
            form_class = FormClass('123454321')

    def test_shouldnt_answer_for_not_published_survey(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('survey_closed', form_data['why_user_cant_answer'])

    def test_should_answer_on_preview(self):
        form_class = FormClass(self.survey.pk, is_preview=True)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

    def test_should_answer_on_active_survey_key(self):
        bundle = SurveyKeysBundleFactory(
            survey=self.survey,
            allow_access_to_unpublished_form=True,
        )
        key = SurveyKeyFactory(
            bundle=bundle,
            user=self.survey.user,
            is_active=True,
        )

        form_class = FormClass(self.survey.pk, survey_key=key.value)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

    def test_shouldnt_answer_on_inactive_survey_key(self):
        bundle = SurveyKeysBundleFactory(
            survey=self.survey,
            allow_access_to_unpublished_form=True,
        )
        key = SurveyKeyFactory(
            bundle=bundle,
            user=self.survey.user,
            is_active=False,
        )

        form_class = FormClass(self.survey.pk, survey_key=key.value)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('survey_closed', form_data['why_user_cant_answer'])

    def test_should_answer_for_logged_user(self):
        self.survey.is_published_external = True
        self.survey.need_auth_to_answer = True
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=self.user)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

    def test_shouldnt_answer_for_logged_but_unsaved_user(self):
        user = UserFactory(uid=None)
        self.survey.is_published_external = True
        self.survey.need_auth_to_answer = True
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=user)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_logged', form_data['why_user_cant_answer'])

    def test_shouldnt_answer_for_unsaved_user_without_multiple(self):
        user = UserFactory(uid=None)
        self.survey.is_published_external = True
        self.survey.need_auth_to_answer = True
        self.survey.is_allow_multiple_answers = False
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=user)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_logged', form_data['why_user_cant_answer'])

    def test_shouldnt_answer_for_not_logged_user(self):
        self.survey.is_published_external = True
        self.survey.need_auth_to_answer = True
        self.survey.save()

        self.user.uid = None
        self.user.cloud_uid = None
        self.user.save()

        form_class = FormClass(self.survey.pk, user=self.user)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_logged', form_data['why_user_cant_answer'])

        form_class = FormClass(self.survey.pk, user=None)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_logged', form_data['why_user_cant_answer'])

    def test_shouldnt_answer_for_robot_user(self):
        self.survey.is_published_external = True
        self.survey.need_auth_to_answer = True
        self.survey.save()

        robot_user = User.objects.get(pk=settings.ROBOT_USER_ID)

        form_class = FormClass(self.survey.pk, user=robot_user)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_logged', form_data['why_user_cant_answer'])

    def test_should_answer_only_once(self):
        self.survey.is_published_external = True
        self.survey.is_allow_answer_editing = False
        self.survey.is_allow_multiple_answers = False
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=self.user)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])

        ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': 'test',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=self.user)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('already_answered', form_data['why_user_cant_answer'])

        form_class = FormClass(self.survey.pk, user=None)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_answer_if_not_public_survey_in_organization_and_user_in_organization(self):
        org = OrganizationFactory()
        self.survey.org = org
        self.survey.is_published_external = True
        self.survey.is_public = False
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[org.dir_id])
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_answer_if_public_survey_in_organization_and_user_in_organization(self):
        org = OrganizationFactory()
        self.survey.org = org
        self.survey.is_published_external = True
        self.survey.is_public = True
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[org.dir_id])
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[])
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

        form_class = FormClass(self.survey.pk, user=self.user, orgs=None)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_answer_if_not_public_survey_in_organization_and_user_not_in_organization(self):
        org = OrganizationFactory()
        self.survey.org = org
        self.survey.is_published_external = True
        self.survey.is_public = False
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[])
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_public', form_data['why_user_cant_answer'])

        form_class = FormClass(self.survey.pk, user=self.user, orgs=None)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_public', form_data['why_user_cant_answer'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_answer_if_not_public_survey_in_organization_and_user_is_superuser(self):
        org = OrganizationFactory()
        self.survey.org = org
        self.survey.is_published_external = True
        self.survey.is_public = False
        self.survey.save()

        self.user.is_superuser = True
        self.user.save()

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[])
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_answer_if_public_survey_not_in_organization(self):
        org = OrganizationFactory()
        self.survey.is_published_external = True
        self.survey.is_public = True
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[org.dir_id])
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[])
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

        form_class = FormClass(self.survey.pk, user=self.user, orgs=None)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_answer_if_not_public_survey_not_in_organization(self):
        org = OrganizationFactory()
        self.survey.is_published_external = True
        self.survey.is_public = False
        self.survey.save()

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[org.dir_id])
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_public', form_data['why_user_cant_answer'])

        form_class = FormClass(self.survey.pk, user=self.user, orgs=[])
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_public', form_data['why_user_cant_answer'])

        form_class = FormClass(self.survey.pk, user=self.user, orgs=None)
        form_data = form_class.as_dict(self.language)
        self.assertFalse(form_data['is_user_can_answer'])
        self.assertIn('not_public', form_data['why_user_cant_answer'])

    def test_shouldnt_get_last_answer_for_robot_profile(self):
        self.survey.is_published_external = True
        self.survey.is_allow_answer_editing = False
        self.survey.is_allow_multiple_answers = False
        self.survey.save()

        robot_user = User.objects.get(pk=settings.ROBOT_USER_ID)

        form_class = FormClass(self.survey.pk, user=robot_user)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])

        ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=robot_user,
            data={
                'data': [{
                    'value': 'test',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=robot_user)
        form_data = form_class.as_dict(self.language)
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertIsNone(form_class.last_answer)

    def test_shouldnt_return_survey_group_info(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNone(form_data['slug'])
        self.assertEqual(form_data['metrika_counter_code'], '')
        self.assertIsNone(form_data['group'])

    def test_should_return_survey_group_info(self):
        survey_group = SurveyGroupFactory(metrika_counter_code='123')
        self.survey.group = survey_group
        self.survey.metrika_counter_code = '456'
        self.survey.slug = 'my-test'
        self.survey.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertEqual(form_data['slug'], 'my-test')
        self.assertEqual(form_data['metrika_counter_code'], '456')
        self.assertDictEqual(form_data['group'], {
            'id': survey_group.pk,
            'metrika_counter_code': '123',
        })


class TestFieldBase(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.image = ImageFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label_image=self.image,
            param_help_text='help text',
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['position'], self.question.position)
        self.assertEqual(field_data['page'], self.question.page)
        self.assertEqual(field_data['label'], self.question.label)
        self.assertEqual(field_data['name'], self.question.param_slug)
        self.assertEqual(field_data['type_slug'], self.question.answer_type.slug)
        self.assertEqual(field_data['is_hidden'], self.question.param_is_hidden)
        self.assertEqual(field_data['is_required'], self.question.param_is_required)
        self.assertEqual(field_data['help_text'], self.question.param_help_text)

        self.assertDictEqual(field_data['label_image']['links'], {
            size: '{host}get-{namespace}/{path}/{size}'.format(
                host=settings.AVATARS_HOST,
                namespace=settings.IMAGE_NAMESPACE,
                path=self.image.image,
                size=size,
            )
            for size in settings.IMAGE_SIZES_AS_STR
        })
        self.assertIsNone(field_data['group_slug'])
        other_data = field_data.get('other_data', {})
        self.assertIsNone(other_data.get('show_conditions'))

    def test_should_return_translated_fields(self):
        self.question.translations = {
            'label': {
                'ru': 'label ru',
                'en': 'label en',
            },
            'param_help_text': {
                'ru': 'help text ru',
                'en': 'help text en',
            },
        }
        self.question.save()
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict('en')
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['label'], 'label en')
        self.assertEqual(field_data['help_text'], 'help text en')

    def test_should_return_valid_json_with_show_conditions(self):
        question_with_condition = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        content_type_attribute = ContentTypeAttributeFactory()
        node1 = SurveyQuestionShowConditionNodeFactory(
            survey_question=question_with_condition,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node1,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='and',
            condition='eq',
            value='1',
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node1,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='or',
            condition='neq',
            value='2',
        )
        node2 = SurveyQuestionShowConditionNodeFactory(
            survey_question=question_with_condition,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node2,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='or',
            condition='neq',
            value='3',
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node2,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='or',
            condition='eq',
            value='4',
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 2)

        field_data = form_data['fields'][self.question.param_slug]
        other_data = field_data.get('other_data', {})
        show_conditions = other_data.get('show_conditions')
        self.assertIsNone(show_conditions)

        field_data = form_data['fields'][question_with_condition.param_slug]
        other_data = field_data.get('other_data', {})
        show_conditions = other_data['show_conditions']
        self.assertEqual(len(show_conditions), 2)
        self.assertEqual(len(show_conditions[0]), 2)
        self.assertEqual(len(show_conditions[1]), 2)
        self.assertDictEqual(show_conditions[0][0], {
            'field': self.question.param_slug,
            'field_value': '1',
            'condition': 'eq',
            'operator': 'and',
        })
        self.assertDictEqual(show_conditions[0][1], {
            'field': self.question.param_slug,
            'field_value': '2',
            'condition': 'neq',
            'operator': 'or',
        })
        self.assertDictEqual(show_conditions[1][0], {
            'field': self.question.param_slug,
            'field_value': '3',
            'condition': 'neq',
            'operator': 'or',
        })
        self.assertDictEqual(show_conditions[1][1], {
            'field': self.question.param_slug,
            'field_value': '4',
            'condition': 'eq',
            'operator': 'or',
        })


class TestGetTranslated(TestCase):
    def setUp(self):
        Value = namedtuple('Value', ['value', 'translations'])
        self.value = Value(value='default value', translations={
            'value': {
                'ru': 'ru value',
                'en': 'en value',
                'de': 'de value',
                'be': 'be value',
            },
        })

    def test_should_return_translated_value_for_ext(self):
        with patch('events.surveyme.forms_v2.TRANSLATIONS_SUPPORT', True):
            self.assertEqual(get_translated(self.value, 'value', 'ru', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'ru', '??'), 'ru value')
            self.assertEqual(get_translated(self.value, 'value', 'be', 'ru'), 'be value')
            self.assertEqual(get_translated(self.value, 'value', 'kk', 'ru'), 'ru value')
            self.assertEqual(get_translated(self.value, 'value', 'en', 'ru'), 'en value')
            self.assertEqual(get_translated(self.value, 'value', 'de', 'ru'), 'de value')
            self.assertEqual(get_translated(self.value, 'value', 'fr', 'ru'), 'en value')
            self.assertEqual(get_translated(self.value, 'value', 'tt', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'tt', '??'), 'default value')

    def test_should_return_translated_value_for_int(self):
        with patch('events.surveyme.forms_v2.TRANSLATIONS_SUPPORT', False):
            self.assertEqual(get_translated(self.value, 'value', 'ru', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'ru', '??'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'be', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'kk', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'en', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'de', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'fr', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'tt', 'ru'), 'default value')
            self.assertEqual(get_translated(self.value, 'value', 'tt', '??'), 'default value')


class TestFieldName(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_name'),
        )
        self.language = 'ru'

    def test_should_return_valid_json_for_logged_user(self):
        user = UserFactory(uid='11591176')
        user._params = {
            'fields': {'fio': 'Pupkin Vasily'}
        }
        form_class = FormClass(self.survey.pk, user=user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['value'], 'Vasily')
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_should_return_valid_json_for_not_logged_user(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertIsNone(tag['attrs']['value'])
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_shouldnt_return_initial_value_for_logged_user(self):
        self.question.initial = 'Your Name'
        self.question.save()

        user = UserFactory(uid='11591176')
        user._params = {
            'fields': {'fio': 'Pupkin Vasily'}
        }
        form_class = FormClass(self.survey.pk, user=user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'Vasily')

    def test_should_return_initial_value_for_not_logged_user(self):
        self.question.initial = 'Your Name'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'Your Name')


class TestFieldSurname(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_surname'),
        )
        self.language = 'ru'

    def test_should_return_valid_json_for_logged_user(self):
        user = UserFactory(uid='11591176')
        user._params = {
            'fields': {'fio': 'Pupkin Vasily'}
        }
        form_class = FormClass(self.survey.pk, user=user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['value'], 'Pupkin')
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_should_return_valid_json_for_not_logged_user(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertIsNone(tag['attrs']['value'])
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_shouldnt_return_initial_value_for_logged_user(self):
        self.question.initial = 'Your Surname'
        self.question.save()

        user = UserFactory(uid='11591176')
        user._params = {
            'fields': {'fio': 'Pupkin Vasily'}
        }
        form_class = FormClass(self.survey.pk, user=user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'Pupkin')

    def test_should_return_initial_value_for_not_logged_user(self):
        self.question.initial = 'Your Surname'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'Your Surname')


class TestFieldShortText(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['min'], self.question.param_min)
        self.assertEqual(tag['attrs']['max'], self.question.param_max)
        self.assertEqual(tag['attrs']['maxlength'], 255)
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_should_return_initial_value(self):
        self.question.initial = 'default text'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'default text')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': 'last text',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'last text')

    def test_shouldnt_fail_if_last_answer_data_is_null(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data=None,
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertIsNone(tag['attrs']['value'])

    def test_should_return_correct_hints_attr(self):
        self.question.param_hint_data_source = 'yt_table_source'
        self.question.param_hint_data_source_params = {
            'filters': [{
                'filter': {
                    'name': 'free_url',
                    'data_source': 'free_url',
                },
                'value': 'https://yt.yandex-team.ru/hahn/?path=//home/forms/test'
            }],
        }
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        hints = field_data.get('hints')

        self.assertIsNotNone(hints)
        self.assertTrue(hints['is_with_pagination'])
        self.assertListEqual(hints['filters'], [{
            'param_name': 'free_url',
            'value': 'https://yt.yandex-team.ru/hahn/?path=//home/forms/test'
        }])
        self.assertTrue(hints['uri'].endswith('/v1/data-source/yt-table-source/'))


class TestFieldLongText(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_long_text'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'textarea')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['min'], self.question.param_min)
        self.assertEqual(tag['attrs']['max'], self.question.param_max)
        self.assertEqual(tag['attrs']['cols'], '40')
        self.assertEqual(tag['attrs']['rows'], '10')
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_should_return_initial_value(self):
        self.question.initial = 'default\nmultiline\ntext'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['content'], 'default\nmultiline\ntext')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': 'last\nmultiline\ntext',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['content'], 'last\nmultiline\ntext')


class TestFieldBoolean(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_boolean'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'checkbox')
        self.assertEqual(tag['attrs']['checked'], None)
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

        other_data = field_data['other_data']
        self.assertEqual(other_data['widget'], 'checkbox')

    def test_should_return_initial_value(self):
        self.question.initial = True
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['checked'], 'checked')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': True,
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['checked'], 'checked')


class TestFieldNumber(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_number'),
            param_min=1,
            param_max=100,
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'number')
        self.assertEqual(tag['attrs']['max'], self.question.param_max)
        self.assertEqual(tag['attrs']['min'], self.question.param_min)
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

        other_data = field_data['other_data']
        self.assertEqual(other_data['widget'], 'number')
        self.assertEqual(field_data['widget'], 'NumberInput')

    def test_should_return_initial_value(self):
        self.question.initial = 42
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 42)

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': 13,
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 13)

    def test_should_return_valid_json_with_show_conditions(self):
        question_on_condition = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            position=1,
        )
        self.question.position = 2
        self.question.save()
        content_type_attribute = ContentTypeAttributeFactory()
        node = SurveyQuestionShowConditionNodeFactory(
            survey_question=self.question,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node,
            survey_question=question_on_condition,
            content_type_attribute=content_type_attribute,
            operator='and',
            condition='eq',
            value='42',
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 2)

        field_data = form_data['fields'][question_on_condition.param_slug]
        other_data = field_data.get('other_data', {})
        show_conditions = other_data.get('show_conditions')
        self.assertIsNone(show_conditions)

        field_data = form_data['fields'][self.question.param_slug]
        other_data = field_data.get('other_data', {})
        show_conditions = other_data['show_conditions']
        self.assertEqual(len(show_conditions), 1)
        self.assertEqual(len(show_conditions[0]), 1)
        self.assertDictEqual(show_conditions[0][0], {
            'field': question_on_condition.param_slug,
            'field_value': '42',
            'condition': 'eq',
            'operator': 'and',
        })


class TestFieldEmail(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_non_profile_email'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'email')
        self.assertEqual(tag['attrs']['maxlength'], 255)
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_should_return_initial_value(self):
        self.question.initial = 'user@company.com'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'user@company.com')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': 'admin@company.com',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'admin@company.com')


class TestFieldUrl(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_url'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'url')
        self.assertEqual(tag['attrs']['maxlength'], 1024)
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_should_return_initial_value(self):
        self.question.initial = 'http://my.company.com'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'http://my.company.com')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': 'http://your.company.com',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'http://your.company.com')


class TestFieldPhone(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_phone'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

    def test_should_return_initial_value(self):
        self.question.initial = '+7 800 600-00-01'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], '+7 800 600-00-01')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': '+7 800 600-00-02',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], '+7 800 600-00-02')


class TestFieldFiles(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
            param_max_files_count=5,
            param_max_file_size=12,
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'file')
        self.assertTrue(tag['attrs']['multiple'])
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

        other_data = field_data['other_data']
        self.assertEqual(other_data['widget'], 'multifile')
        self.assertEqual(other_data['max_files_count'], self.question.param_max_files_count)
        self.assertEqual(other_data['max_file_size'], self.question.param_max_file_size)


class TestFieldDate(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
            param_date_field_type='date',
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['widget'], 'DateInput')
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

        other_data = field_data['other_data']
        self.assertEqual(other_data['widget'], 'date')
        self.assertDictEqual(other_data['allowed_range'], {})

    def test_should_return_valid_json_with_allowed_range(self):
        self.question.param_date_field_min = '2020-01-01'
        self.question.param_date_field_max = '2020-12-31'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        other_data = field_data['other_data']
        self.assertDictEqual(other_data['allowed_range'], {
            'from': '2020-01-01',
            'to': '2020-12-31',
        })

    def test_should_return_initial_value(self):
        self.question.initial = '2020-07-20'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], '2020-07-20')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': '2020-07-20',
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], '2020-07-20')


class TestFieldDateRange(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
            param_date_field_type='daterange',
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['widget'], 'DateRangeFieldInput')
        self.assertEqual(len(field_data['tags']), 2)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['name'], '%s_0' % self.question.param_slug)

        tag = field_data['tags'][1]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['name'], '%s_1' % self.question.param_slug)

        other_data = field_data['other_data']
        self.assertEqual(other_data['widget'], 'daterange')
        self.assertDictEqual(other_data['allowed_range'], {})

    def test_should_return_valid_json_with_allowed_range(self):
        self.question.param_date_field_min = '2020-01-01'
        self.question.param_date_field_max = '2020-12-31'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        other_data = field_data['other_data']
        self.assertDictEqual(other_data['allowed_range'], {
            'from': '2020-01-01',
            'to': '2020-12-31',
        })

    def test_should_return_initial_value(self):
        self.question.initial = {
            'begin': '2020-07-20',
            'end': '2020-07-24',
        }
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 2)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], '2020-07-20')

        tag = field_data['tags'][1]
        self.assertEqual(tag['attrs']['value'], '2020-07-24')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': {
                        'begin': '2020-07-21',
                        'end': '2020-07-25',
                    },
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 2)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], '2020-07-21')

        tag = field_data['tags'][1]
        self.assertEqual(tag['attrs']['value'], '2020-07-25')


class TestFieldPayment(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_payment'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['name'], self.question.param_slug)

        other_data = field_data['other_data']
        self.assertIsNone(other_data['widget'])
        self.assertEqual(other_data['min'], 2)
        self.assertEqual(other_data['max'], 15000)
        self.assertFalse(other_data['is_fixed'])

    def test_should_return_initial_value(self):
        self.question.initial = 150
        self.question.param_payment = {
            'is_fixed': True,
        }
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 150)

    def test_shouldnt_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': {
                        'amount': 12,
                        'payment_method': 'AC',
                    },
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], '0')

    def test_should_return_initial_value_instead_of_last_answer(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        self.question.initial = 150
        self.question.param_payment = {
            'is_fixed': True,
        }
        self.question.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': {
                        'amount': 12,
                        'payment_method': 'AC',
                    },
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 150)


class TestFieldChoices(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.image = ImageFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        self.choices = [
            SurveyQuestionChoiceFactory(
                survey_question=self.question,
                label='one',
                label_image=self.image,
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.question,
                label='two',
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.question,
                label='three',
            ),
        ]
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['type'], 'choices')
        self.assertEqual(field_data['is_allow_multiple_choice'], self.question.param_is_allow_multiple_choice)
        self.assertEqual(field_data['is_disabled_init_item'], self.question.param_is_disabled_init_item)
        self.assertEqual(field_data['suggest_choices'], self.question.param_suggest_choices)
        self.assertEqual(field_data['widget'], self.question.param_widget)
        self.assertEqual(len(field_data['data_source']['items']), 3)

        items = field_data['data_source']['items']
        self.assertDictEqual(items[0], {
            'id': str(self.choices[0].pk),
            'slug': str(self.choices[0].pk),
            'text': self.choices[0].label,
            'label_image': {
                'links': {
                    size: '{host}get-{namespace}/{path}/{size}'.format(
                        host=settings.AVATARS_HOST,
                        namespace=settings.IMAGE_NAMESPACE,
                        path=self.image.image,
                        size=size,
                    )
                    for size in settings.IMAGE_SIZES_AS_STR
                },
            },
        })
        self.assertDictEqual(items[1], {
            'id': str(self.choices[1].pk),
            'slug': str(self.choices[1].pk),
            'text': self.choices[1].label,
            'label_image': None
        })
        self.assertDictEqual(items[2], {
            'id': str(self.choices[2].pk),
            'slug': str(self.choices[2].pk),
            'text': self.choices[2].label,
            'label_image': None
        })

    def test_should_return_sorted_items(self):
        self.question.param_modify_choices = 'sort'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['data_source']['items']), 3)

        items = field_data['data_source']['items']
        self.assertEqual(items[0]['text'], 'one')
        self.assertEqual(items[1]['text'], 'three')
        self.assertEqual(items[2]['text'], 'two')

    def test_should_return_shuffled_items(self):
        self.question.param_modify_choices = 'shuffle'
        self.question.save()

        form_class = FormClass(self.survey.pk)
        with patch('random.shuffle') as mock_shuffle:
            form_data = form_class.as_dict(self.language)

        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(len(field_data['data_source']['items']), 3)

        mock_shuffle.assert_called_once_with(ANY)
        (items, ) = mock_shuffle.call_args_list[0][0]
        self.assertEqual(len(items), 3)
        result = {
            item.get('id'): item.get('text')
            for item in items
        }
        expected = {
            str(choice.pk): choice.label
            for choice in self.choices
        }
        self.assertDictEqual(result, expected)

    def test_should_return_initial_value(self):
        self.question.initial = [{
            'key': str(self.choices[1].pk),
            'text': self.choices[1].label,
        }]
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': str(self.choices[1].pk),
            'text': self.choices[1].label,
        }])

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': [
                        {
                            'key': str(self.choices[1].pk),
                            'text': self.choices[1].label,
                        },
                    ],
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': str(self.choices[1].pk),
            'text': self.choices[1].label,
        }])

    def test_should_return_correct_show_condition_attr(self):
        question_on_logic = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            position=1,
        )
        self.question.position=2
        self.question.save()

        content_type_attribute = ContentTypeAttributeFactory()
        node = SurveyQuestionShowConditionNodeFactory(
            survey_question=self.question,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node,
            survey_question=question_on_logic,
            content_type_attribute=content_type_attribute,
            operator='and',
            condition='eq',
            value='1',
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 2)

        field_data = form_data['fields'][self.question.param_slug]
        show_conditions = field_data['show_conditions']

        self.assertEqual(len(show_conditions), 1)
        self.assertEqual(len(show_conditions[0]), 1)
        self.assertDictEqual(show_conditions[0][0], {
            'field': question_on_logic.param_slug,
            'field_value': '1',
            'condition': 'eq',
            'operator': 'and',
        })

    def test_should_return_null_show_condition_attr_for_deleted_question(self):
        question_on_logic = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            position=1,
            is_deleted=True,
        )
        self.question.position=2
        self.question.save()

        content_type_attribute = ContentTypeAttributeFactory()
        node = SurveyQuestionShowConditionNodeFactory(
            survey_question=self.question,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node,
            survey_question=question_on_logic,
            content_type_attribute=content_type_attribute,
            operator='and',
            condition='eq',
            value='1',
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertIsNone(field_data['show_conditions'])


class TestFieldMatrix(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_matrix_choice',
        )
        self.rows = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.question,
                type='row',
                label='first row',
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.question,
                type='row',
                label='second row',
            ),
        ]
        self.cols = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.question,
                type='column',
                label='1',
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.question,
                type='column',
                label='2',
            ),
        ]
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['type'], 'choices')
        self.assertEqual(field_data['is_allow_multiple_choice'], self.question.param_is_allow_multiple_choice)
        self.assertEqual(field_data['is_disabled_init_item'], self.question.param_is_disabled_init_item)
        self.assertEqual(field_data['suggest_choices'], self.question.param_suggest_choices)
        self.assertEqual(field_data['widget'], self.question.param_widget)
        self.assertEqual(len(field_data['data_source']['items']), 4)

        items = field_data['data_source']['items']
        self.assertDictEqual(items[0], {
            'id': str(self.rows[0].pk),
            'text': self.rows[0].label,
            'position': self.rows[0].position,
            'type': self.rows[0].type,
        })
        self.assertDictEqual(items[1], {
            'id': str(self.rows[1].pk),
            'text': self.rows[1].label,
            'position': self.rows[1].position,
            'type': self.rows[1].type,
        })
        self.assertDictEqual(items[2], {
            'id': str(self.cols[0].pk),
            'text': self.cols[0].label,
            'position': self.cols[0].position,
            'type': self.cols[0].type,
        })
        self.assertDictEqual(items[3], {
            'id': str(self.cols[1].pk),
            'text': self.cols[1].label,
            'position': self.cols[1].position,
            'type': self.cols[1].type,
        })

    def test_should_return_initial_value(self):
        self.question.initial = [{
            'row': {
                'key': str(self.rows[0].pk),
                'text': self.rows[0].label,
            },
            'col': {
                'key': str(self.cols[1].pk),
                'text': self.cols[1].label,
            },
        }]
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': '%s_%s' % (self.rows[0].pk, self.cols[1].pk),
            'text': '"%s": %s' % (self.rows[0].label, self.cols[1].label),
        }])

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': [{
                        'row': {
                            'key': str(self.rows[0].pk),
                            'text': self.rows[0].label,
                        },
                        'col': {
                            'key': str(self.cols[1].pk),
                            'text': self.cols[1].label,
                        },
                    }],
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': '%s_%s' % (self.rows[0].pk, self.cols[1].pk),
            'text': '"%s": %s' % (self.rows[0].label, self.cols[1].label),
        }])


class TestFieldDataSourceGender(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='gender',
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        with override('ru'):
            form_class = FormClass(self.survey.pk)
            form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['type'], 'choices')
        self.assertEqual(field_data['is_allow_multiple_choice'], self.question.param_is_allow_multiple_choice)
        self.assertEqual(field_data['is_disabled_init_item'], self.question.param_is_disabled_init_item)
        self.assertEqual(field_data['suggest_choices'], self.question.param_suggest_choices)
        self.assertEqual(field_data['widget'], self.question.param_widget)

        data_source = field_data['data_source']
        self.assertTrue('is_with_pagination' not in data_source)
        self.assertIn('items', data_source)
        expected = [
            {'id': '1', 'text': _('Мужской')},
            {'id': '2', 'text': _('Женский')},
        ]
        self.assertListEqual(data_source['items'], expected)

    def test_should_return_initial_value(self):
        self.question.initial = [{
            'key': '2',
            'text': _('Женский'),
        }]
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': '2',
            'text': _('Женский'),
        }])

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': [{
                        'key': '2',
                        'text': _('Женский'),
                    }],
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': '2',
            'text': _('Женский'),
        }])


class TestFieldDataSourceMusicGenre(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='music_genre',
        )
        self.music_genre = MusicGenreFactory(
            music_id='jazz',
            title='Jazz',
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['type'], 'choices')
        self.assertEqual(field_data['is_allow_multiple_choice'], self.question.param_is_allow_multiple_choice)
        self.assertEqual(field_data['is_disabled_init_item'], self.question.param_is_disabled_init_item)
        self.assertEqual(field_data['suggest_choices'], self.question.param_suggest_choices)
        self.assertEqual(field_data['widget'], self.question.param_widget)

        data_source = field_data['data_source']
        self.assertTrue(data_source['is_with_pagination'])
        self.assertListEqual(data_source['filters'], [])
        self.assertTrue(data_source['uri'].endswith('/v1/data-source/music-genre/'))

    def test_should_return_initial_value(self):
        self.question.initial = [{
            'key': self.music_genre.music_id,
            'text': self.music_genre.title,
        }]
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': self.music_genre.music_id,
            'text': self.music_genre.title,
        }])

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': [{
                        'key': self.music_genre.music_id,
                        'text': self.music_genre.title,
                    }],
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': self.music_genre.music_id,
            'text': self.music_genre.title,
        }])


class TestFieldDataSourceYtTableSource(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.master_question = SurveyQuestionFactory(survey=self.survey)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='yt_table_source',
            param_data_source_params={
                'filters': [{
                    'filter': {
                        'name': 'free_url',
                        'data_source': 'free_url',
                    },
                    'type': 'specified_value',
                    'value': 'https://yt.yandex-team.ru/hahn/?path=//home/forms/test'
                }, {
                    'filter': {
                        'name': 'yt_table_source',
                        'data_source': 'yt_table_source',
                    },
                    'type': 'field_value',
                    'field': str(self.master_question.pk),
                }],
            },
        )
        self.table_row = TableRowFactory(
            table_identifier='//home/forms/test_hahn',
            source_id='id-0001',
            text='first row',
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 2)

        field_data = form_data['fields'][self.question.param_slug]
        self.assertEqual(field_data['type'], 'choices')
        self.assertEqual(field_data['is_allow_multiple_choice'], self.question.param_is_allow_multiple_choice)
        self.assertEqual(field_data['is_disabled_init_item'], self.question.param_is_disabled_init_item)
        self.assertEqual(field_data['suggest_choices'], self.question.param_suggest_choices)
        self.assertEqual(field_data['widget'], self.question.param_widget)

        data_source = field_data['data_source']
        self.assertTrue(data_source['is_with_pagination'])
        self.assertEqual(len(data_source['filters']), 2)
        self.assertDictEqual(data_source['filters'][0], {
            'param_name': 'free_url',
            'type': 'specified_value',
            'value': 'https://yt.yandex-team.ru/hahn/?path=//home/forms/test'
        })
        self.assertDictEqual(data_source['filters'][1], {
            'param_name': 'yt_table_source',
            'type': 'field_value',
            'field': self.master_question.param_slug
        })
        self.assertTrue(data_source['uri'].endswith('/v1/data-source/yt-table-source/'))

    def test_should_return_initial_value(self):
        self.question.initial = [{
            'key': self.table_row.source_id,
            'text': self.table_row.text,
        }]
        self.question.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 2)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': self.table_row.source_id,
            'text': self.table_row.text,
        }])

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': [{
                        'key': self.table_row.source_id,
                        'text': self.table_row.text,
                    }],
                    'question': self.question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 2)

        field_data = form_data['fields'][self.question.param_slug]

        self.assertEqual(field_data['value'], [{
            'id': self.table_row.source_id,
            'text': self.table_row.text,
        }])


class TestFieldStatement(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_statement'),
            param_is_section_header=True,
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields'][self.question.param_slug]
        other_data = field_data['other_data']
        self.assertTrue(other_data['is_section_header'])
        self.assertEqual(other_data['widget'], 'statement')


class TestFieldAgreement(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
        )
        self.agreement = SurveyAgreement.objects.get(slug='events')
        self.survey.agreements.add(self.agreement)
        self.language = 'ru'

    def test_should_return_valid_json_for_empty_form(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_name = 'is_agree_with_%s' % self.agreement.slug
        field_data = form_data['fields'][field_name]
        self.assertEqual(field_data['name'], field_name)
        self.assertEqual(field_data['type_slug'], 'agreement')

        self.assertEqual(field_data['label'], self.agreement.text)
        self.assertEqual(field_data['is_required'], self.agreement.is_required)
        self.assertEqual(field_data['page'], 1)
        self.assertEqual(field_data['position'], 1)
        self.assertFalse(field_data['is_hidden'])
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'checkbox')
        self.assertEqual(tag['attrs']['name'], field_name)

        other_data = field_data['other_data']
        self.assertEqual(other_data['widget'], 'agreement')

    def test_should_return_valid_json_for_not_empty_form(self):
        SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            page=2,
            position=1,
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 3)

        field_name = 'is_agree_with_%s' % self.agreement.slug
        field_data = form_data['fields'][field_name]
        self.assertEqual(field_data['page'], 2)
        self.assertEqual(field_data['position'], 2)


class TestFieldCaptcha(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            is_public=True,
            user=UserFactory(),
        )
        self.language = 'ru'

    def register_uri(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/generate',
            body='''<?xml version="1.0"?>
<number url='https://ext.captcha.yandex.net/image?key=12345'>12345</number>
            ''',
            content_type='text/xml',
        )

    @responses.activate
    def test_should_return_valid_json_for_empty_form(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'always'
        self.survey.save()

        with override(self.language):
            form_class = FormClass(self.survey.pk)
            form_data = form_class.as_dict(self.language)
        self.assertEqual(len(responses.calls), 1)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        field_data = form_data['fields']['captcha']
        self.assertEqual(field_data['name'], 'captcha')
        self.assertEqual(field_data['type_slug'], 'captcha')
        self.assertEqual(field_data['label'], 'Введите капчу')
        self.assertTrue(field_data['is_required'])
        self.assertFalse(field_data['is_hidden'])
        self.assertEqual(field_data['page'], 1)
        self.assertEqual(field_data['position'], 1)
        self.assertEqual(len(field_data['tags']), 2)

        tag = field_data['tags'][0]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'hidden')
        self.assertEqual(tag['attrs']['captcha_type'], self.survey.captcha_type)
        self.assertEqual(tag['attrs']['data-captcha-image-url'], 'https://ext.captcha.yandex.net/image?key=12345')
        self.assertEqual(tag['attrs']['value'], '12345')
        self.assertEqual(tag['attrs']['name'], 'captcha_0')

        tag = field_data['tags'][1]
        self.assertEqual(tag['tag'], 'input')
        self.assertEqual(tag['attrs']['type'], 'text')
        self.assertEqual(tag['attrs']['captcha_type'], self.survey.captcha_type)
        self.assertEqual(tag['attrs']['data-captcha-image-url'], 'https://ext.captcha.yandex.net/image?key=12345')
        self.assertEqual(tag['attrs']['name'], 'captcha_1')

        other_data = field_data['other_data']
        self.assertEqual(other_data['widget'], 'captcha')
        self.assertEqual(other_data['captcha_type'], self.survey.captcha_type)

    @responses.activate
    def test_should_return_valid_json_for_not_empty_form(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'always'
        self.survey.save()

        SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            page=2,
            position=1,
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertEqual(len(responses.calls), 1)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 3)

        field_data = form_data['fields']['captcha']
        self.assertEqual(field_data['page'], 2)
        self.assertEqual(field_data['position'], 2)

    @responses.activate
    def test_should_return_valid_json_for_empty_form_with_spam_intranet(self):
        self.register_uri()
        Survey.set_spam_detected(self.survey.pk, True)

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertEqual(len(responses.calls), 0)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 0)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_valid_json_for_empty_form_with_spam_business(self):
        self.register_uri()
        Survey.set_spam_detected(self.survey.pk, True)

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertEqual(len(responses.calls), 1)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 1)

        self.assertIn('captcha', form_data['fields'])


class TestFieldGroup(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=self.user,
        )
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
                group=self.group_question,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_boolean'),
                group=self.group_question,
            ),
        ]
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 3)

        field_data = form_data['fields'][self.group_question.param_slug]
        self.assertIsNone(field_data['group_slug'])
        self.assertEqual(field_data['widget'], 'GroupWidget')

        question_name = '%s__0' % self.questions[0].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertIsNone(tag['attrs']['value'])

        question_name = '%s__0' % self.questions[1].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertIsNone(tag['attrs']['checked'])

    def test_should_return_initial_value(self):
        self.questions[0].initial = 'default text'
        self.questions[0].save()

        self.questions[1].initial = True
        self.questions[1].save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 3)

        field_data = form_data['fields'][self.group_question.param_slug]
        self.assertIsNone(field_data['group_slug'])
        self.assertEqual(field_data['widget'], 'GroupWidget')

        question_name = '%s__0' % self.questions[0].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'default text')

        question_name = '%s__0' % self.questions[1].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['checked'], 'checked')

    def test_should_return_last_answer_value(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
            data={
                'data': [{
                    'value': [
                        [
                            {
                                'value': 'text 1',
                                'question': self.questions[0].get_answer_info(),
                            },
                            {
                                'value': True,
                                'question': self.questions[1].get_answer_info(),
                            },
                        ],
                        [
                            {
                                'value': 'text 2',
                                'question': self.questions[0].get_answer_info(),
                            },
                            {
                                'value': False,
                                'question': self.questions[1].get_answer_info(),
                            },
                        ],
                    ],
                    'question': self.group_question.get_answer_info(),
                }],
            },
        )

        form_class = FormClass(self.survey.pk, user=answer.user)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 5)

        field_data = form_data['fields'][self.group_question.param_slug]
        self.assertIsNone(field_data['group_slug'])
        self.assertEqual(field_data['widget'], 'GroupWidget')

        # --- first fieldset ---
        question_name = '%s__0' % self.questions[0].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'text 1')

        question_name = '%s__0' % self.questions[1].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['checked'], 'checked')

        # --- second fieldset ---
        question_name = '%s__1' % self.questions[0].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertEqual(tag['attrs']['value'], 'text 2')

        question_name = '%s__1' % self.questions[1].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        self.assertEqual(len(field_data['tags']), 1)

        tag = field_data['tags'][0]
        self.assertIsNone(tag['attrs']['checked'])

    def test_should_return_correct_name_for_question_in_condition(self):
        content_type_attribute = ContentTypeAttributeFactory()
        node = SurveyQuestionShowConditionNodeFactory(
            survey_question=self.questions[1],
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=node,
            survey_question=self.questions[0],
            content_type_attribute=content_type_attribute,
            operator='and',
            condition='eq',
            value='1',
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertEqual(len(form_data['fields']), 3)

        question_name = '%s__0' % self.questions[1].param_slug
        field_data = form_data['fields'][question_name]
        self.assertEqual(field_data['group_slug'], self.group_question.param_slug)
        other_data = field_data['other_data']
        show_conditions = other_data['show_conditions']

        self.assertEqual(len(show_conditions), 1)
        self.assertEqual(len(show_conditions[0]), 1)
        self.assertDictEqual(show_conditions[0][0], {
            'field': '%s__0' % self.questions[0].param_slug,
            'field_value': '1',
            'condition': 'eq',
            'operator': 'and',
        })


class TestSurveyAttributes(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(
            is_published_external=True,
            user=UserFactory(),
            name='Test survey',
            metrika_counter_code='my-metrika-counter',
            is_only_for_iframe=True,
            extra={
                'footer': {
                    'enabled': False,
                },
                'redirect': {
                    'enabled': False,
                },
                'stats': {
                    'enabled': False,
                },
                'teaser': {
                    'enabled': False,
                },
            },
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.language = 'ru'

    def test_should_return_valid_json(self):
        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertEqual(form_data['id'], self.survey.pk)
        self.assertEqual(form_data['name'], self.survey.name)
        self.assertEqual(form_data['metrika_counter_code'], self.survey.metrika_counter_code)
        self.assertDictEqual(form_data['texts'], {
            text.slug: text.value
            for text in self.survey.texts.all()
        })
        self.assertIsNone(form_data['allow_post_conditions'])
        self.assertDictEqual(form_data['footer'], {'enabled': False})
        self.assertDictEqual(form_data['redirect'], {'enabled': False})
        self.assertDictEqual(form_data['stats'], {'enabled': False})
        self.assertDictEqual(form_data['teaser'], {'enabled': False})
        self.assertIsNone(form_data['org_id'])
        self.assertTrue(form_data['is_user_can_answer'])
        self.assertDictEqual(form_data['why_user_cant_answer'], {})
        self.assertFalse(form_data['is_user_already_answered'])
        self.assertFalse(form_data['is_answer_could_be_edited'])
        self.assertTrue(form_data['is_only_for_iframe'])

    def test_should_return_valid_json_with_submit_conditions(self):
        content_type_attribute = ContentTypeAttributeFactory()
        node1 = SurveySubmitConditionNodeFactory(survey=self.survey)
        SurveySubmitConditionNodeItemFactory(
            survey_submit_condition_node=node1,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='and',
            condition='eq',
            value='1',
        )
        SurveySubmitConditionNodeItemFactory(
            survey_submit_condition_node=node1,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='or',
            condition='neq',
            value='2',
        )
        node2 = SurveySubmitConditionNodeFactory(survey=self.survey)
        SurveySubmitConditionNodeItemFactory(
            survey_submit_condition_node=node2,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='or',
            condition='neq',
            value='3',
        )
        SurveySubmitConditionNodeItemFactory(
            survey_submit_condition_node=node2,
            survey_question=self.question,
            content_type_attribute=content_type_attribute,
            operator='or',
            condition='eq',
            value='4',
        )

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertIn('allow_post_conditions', form_data)
        submit_conditions = form_data['allow_post_conditions']

        self.assertEqual(len(submit_conditions), 2)
        self.assertEqual(len(submit_conditions[0]), 2)
        self.assertEqual(len(submit_conditions[1]), 2)
        self.assertDictEqual(submit_conditions[0][0], {
            'field': self.question.param_slug,
            'field_value': '1',
            'condition': 'eq',
            'operator': 'and',
        })
        self.assertDictEqual(submit_conditions[0][1], {
            'field': self.question.param_slug,
            'field_value': '2',
            'condition': 'neq',
            'operator': 'or',
        })
        self.assertDictEqual(submit_conditions[1][0], {
            'field': self.question.param_slug,
            'field_value': '3',
            'condition': 'neq',
            'operator': 'or',
        })
        self.assertDictEqual(submit_conditions[1][1], {
            'field': self.question.param_slug,
            'field_value': '4',
            'condition': 'eq',
            'operator': 'or',
        })

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_valid_org_id(self):
        org = OrganizationFactory()
        self.survey.org = org
        self.survey.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertEqual(form_data['org_id'], org.dir_id)

    def test_should_return_vaild_json_with_styles(self):
        image_page = ImageFactory()
        image_form = ImageFactory()
        self.survey.styles_template = SurveyStyleTemplateFactory(
            styles={
                'display': 'always',
            },
            image_page=image_page,
            image_form=image_form,
        )
        self.survey.save()

        form_class = FormClass(self.survey.pk)
        form_data = form_class.as_dict(self.language)
        self.assertIsNotNone(form_data)
        self.assertIn('styles_template', form_data)
        styles_template = form_data['styles_template']

        self.assertDictEqual(styles_template['styles'], self.survey.styles_template.styles)
        self.assertDictEqual(styles_template['image_page']['links'], {
            size: '{host}get-{namespace}/{path}/{size}'.format(
                host=settings.AVATARS_HOST,
                namespace=settings.IMAGE_NAMESPACE,
                path=image_page.image,
                size=size,
            )
            for size in settings.IMAGE_SIZES_AS_STR
        })
        self.assertDictEqual(styles_template['image_form']['links'], {
            size: '{host}get-{namespace}/{path}/{size}'.format(
                host=settings.AVATARS_HOST,
                namespace=settings.IMAGE_NAMESPACE,
                path=image_form.image,
                size=size,
            )
            for size in settings.IMAGE_SIZES_AS_STR
        })
