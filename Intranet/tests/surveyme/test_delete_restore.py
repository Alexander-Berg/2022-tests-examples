# -*- coding: utf-8 -*-
import json

from bson import ObjectId
from django.test import TestCase
from guardian.shortcuts import assign_perm
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.models import (
    AnswerType,
    SurveyQuestion,
)
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionShowConditionNodeFactory,
    SurveyQuestionShowConditionNodeItemFactory,
)


class TestDeleteRestoreSurveySuperUser(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        assign_perm('change_survey', self.survey.user, self.survey)

    def test_should_delete_survey(self):
        response = self.client.delete(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 204)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.is_deleted, True)
        self.assertEqual(self.survey.user.has_perm('change_survey', self.survey), False)

    def test_shouldnt_delete_survey(self):
        self.user.is_superuser = False
        self.user.save()

        response = self.client.delete(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(403, response.status_code)

    def test_should_restore_survey_without_slug(self):
        response = self.client.delete(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 204)

        response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/restore/')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.slug, None)
        self.assertEqual(self.survey.is_deleted, False)
        self.assertEqual(self.survey.user.has_perm('change_survey', self.survey), True)

    def test_should_restore_survey_with_slug(self):
        survey_slug = str(ObjectId())
        self.survey.slug = survey_slug
        self.survey.save()

        response = self.client.delete(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 204)

        response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/restore/')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.slug, survey_slug)
        self.assertEqual(self.survey.is_deleted, False)
        self.assertEqual(self.survey.user.has_perm('change_survey', self.survey), True)

    def test_should_restore_survey_without_restroe_user_rights(self):
        self.survey.is_deleted = True
        self.survey.save()

        with patch('events.surveyme.logic.access.request_roles') as mock_request_roles:
            response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/restore/')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.is_deleted, False)
        self.assertEqual(self.survey.user.has_perm('change_survey', self.survey), True)
        mock_request_roles.assert_not_called()


class TestRestoreSurveySupport(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_staff=True)
        self.survey = SurveyFactory()

    def test_can_restore_survey(self):
        self.survey.is_deleted = True
        self.survey.save()
        response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/restore/')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.slug, None)
        self.assertEqual(self.survey.is_deleted, False)
        self.assertEqual(self.survey.user.has_perm('change_survey', self.survey), True)


class TestRestoreSurveyUserWithPermissions(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.survey = SurveyFactory()
        assign_perm('change_survey', self.user, self.survey)

    def test_can_restore_survey(self):
        self.survey.is_deleted = True
        self.survey.save()
        response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/restore/')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.slug, None)
        self.assertEqual(self.survey.is_deleted, False)
        self.assertEqual(self.survey.user.has_perm('change_survey', self.survey), True)


class TestRestoreSurveyAuthor(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.survey = SurveyFactory()
        self.survey.user = self.user
        self.survey.save()

    def test_can_restore_survey(self):
        self.survey.is_deleted = True
        self.survey.save()
        response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/restore/')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.slug, None)
        self.assertEqual(self.survey.is_deleted, False)
        self.assertEqual(self.survey.user.has_perm('change_survey', self.survey), True)


class TestRestoreNoSuperUserNoSupport(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.survey = SurveyFactory()

    def test_cant_restore_survey(self):
        self.survey.is_deleted = True
        self.survey.save()
        response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/restore/')
        self.assertEqual(response.status_code, 403)


class TestDeleteRestoreQuestion(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(is_published_external=True)
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.questions = [
            SurveyQuestionFactory(survey=self.survey, answer_type=answer_short_text),
            SurveyQuestionFactory(survey=self.survey, answer_type=answer_short_text, param_slug='last'),
        ]
        self.list_url = '/admin/api/v2/survey-questions/?survey=%s' % self.survey.pk
        self.details_url = '/admin/api/v2/survey-questions/%s/' % self.questions[-1].pk
        self.restore_url = '/admin/api/v2/survey-questions/%s/restore/' % self.questions[-1].pk

    def get_expected_slug(self, idx):
        question = self.questions[idx]
        return '%s_%s' % (question.answer_type.slug, question.pk)

    def test_delete_profile_question(self):
        answer_param_name = AnswerType.objects.get(slug='param_name')
        param_question = SurveyQuestionFactory(survey=self.survey, answer_type=answer_param_name)
        response = self.client.delete('/admin/api/v2/survey-questions/%s/' % param_question.pk)
        self.assertEqual(204, response.status_code)

        response = self.client.get(self.list_url)
        data = json.loads(response.content.decode(response.charset))
        ids = set(it['id'] for it in data['results'])
        self.assertNotIn(param_question.pk, ids)
        resultset = SurveyQuestion.with_deleted_objects.filter(pk=param_question.pk)
        self.assertEqual([], list(resultset))

    def test_delete(self):
        response = self.client.get(self.list_url)
        data = json.loads(response.content.decode(response.charset))
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.questions[0].pk, ids)
        self.assertIn(self.questions[1].pk, ids)

        response = self.client.delete(self.details_url)
        self.assertEqual(204, response.status_code)

        response = self.client.get(self.list_url)
        data = json.loads(response.content.decode(response.charset))
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.questions[0].pk, ids)
        self.assertNotIn(self.questions[1].pk, ids)
        deleted_question = SurveyQuestion.with_deleted_objects.get(pk=self.questions[1].pk)
        self.assertEqual(self.questions[1].pk, deleted_question.pk)
        expected_slug = self.get_expected_slug(-1)
        self.assertEqual(expected_slug, deleted_question.param_slug)

    def test_restore(self):
        response = self.client.delete(self.details_url)
        self.assertEqual(204, response.status_code)

        response = self.client.post(self.restore_url)
        self.assertEqual(200, response.status_code)

        response = self.client.get(self.list_url)
        data = json.loads(response.content.decode(response.charset))
        ids = set(it['id'] for it in data['results'])
        self.assertIn(self.questions[0].pk, ids)
        self.assertIn(self.questions[1].pk, ids)
        expected_slug = self.get_expected_slug(-1)
        last_question = data['results'][-1]
        self.assertEqual(expected_slug, last_question['param_slug'])

    def test_delete_question_in_condition(self):
        question_in_condition = self.questions[0]
        question_with_condition = self.questions[1]

        condition_node = SurveyQuestionShowConditionNodeFactory(
            survey_question=question_with_condition
        )
        content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )
        SurveyQuestionShowConditionNodeItemFactory(
            survey_question_show_condition_node=condition_node,
            survey_question=question_in_condition,
            content_type_attribute=content_type_attribute,
            operator='or', value='foo',
        )
        response = self.client.delete('/admin/api/v2/survey-questions/%s/' % question_in_condition.pk)
        self.assertEqual(204, response.status_code)
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        self.assertEqual(200, response.status_code)
        self.assertIn('fields', response.data)
        self.assertEqual([question_with_condition.param_slug], list(response.data['fields'].keys()))


class TestRestoreQuestion(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        ast = AnswerType.objects.get(slug='answer_short_text')
        self.questions = [
            SurveyQuestionFactory(param_slug='q1', survey=self.survey, answer_type=ast, page=1, position=1),
            SurveyQuestionFactory(param_slug='q2', survey=self.survey, answer_type=ast, page=2, position=1),
            SurveyQuestionFactory(param_slug='q3', survey=self.survey, answer_type=ast, page=2, position=2),
            SurveyQuestionFactory(param_slug='q4', survey=self.survey, answer_type=ast, page=3, position=1),
        ]

    def get_expected_slug(self, idx):
        question = self.questions[idx]
        return '%s_%s' % (question.answer_type.slug, question.pk)

    def delete_question(self, idx):
        return self.client.delete('/admin/api/v2/survey-questions/%s/' % self.questions[idx].pk)

    def restore_question(self, idx, data=None, new_page=False):
        url = f"/admin/api/v2/survey-questions/{self.questions[idx].pk}/restore/"
        if new_page:
            url = '{}?{}'.format(url, 'new_page=true')
        return self.client.post(url, data)

    def get_questions(self):
        return list(self.survey.surveyquestion_set.values_list('param_slug', 'page', 'position'))

    def test_restore_first_question_without_params(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        response = self.client.post('/admin/api/v2/survey-questions/%s/restore/' % self.questions[0].pk)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [(expected_slug, 1, 1), ('q2', 1, 2), ('q3', 1, 3), ('q4', 2, 1)])

    def test_restore_first_question_on_new_page(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(0, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [(expected_slug, 1, 1), ('q2', 2, 1), ('q3', 2, 2), ('q4', 3, 1)])

    def test_restore_first_question_on_new_page_more_complex(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(0, data=data, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [('q2', 1, 1), ('q3', 1, 2), (expected_slug, 2, 1), ('q4', 3, 1)])

    def test_restore_first_question_on_second_page(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2}
        response = self.restore_question(0, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [('q2', 1, 1), ('q3', 1, 2), (expected_slug, 2, 1), ('q4', 2, 2)])

    def test_restore_first_question_on_second_page_on_last_position(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(0, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [('q2', 1, 1), ('q3', 1, 2), ('q4', 2, 1), (expected_slug, 2, 2)])

    def test_restore_second_question_without_params(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(1)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q3', 2, 2), ('q4', 3, 1)])

    def test_restore_second_question_on_new_page(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(1, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q3', 3, 1), ('q4', 4, 1)])

    def test_restore_second_question_on_new_page_more_complex(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(1, data=data, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q3', 3, 1), ('q4', 4, 1)])

    def test_restore_second_question_on_second_page_on_last_position(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(1, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q3', 2, 1), (expected_slug, 2, 2), ('q4', 3, 1)])

    def test_restore_fourth_question_without_params(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(3)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q2', 2, 1), ('q3', 2, 2), (expected_slug, 3, 1)])

    def test_restore_fourth_question_on_new_page(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(3, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q2', 2, 1), ('q3', 2, 2), (expected_slug, 3, 1)])

    def test_restore_fourth_question_on_new_page_more_complex(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(3, data=data, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q2', 3, 1), ('q3', 3, 2)])

    def test_restore_fourth_question_on_second_page_second_position(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(3, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q2', 2, 1), (expected_slug, 2, 2), ('q3', 2, 3)])


class TestRestoreQuestionSupport(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_staff=True)
        self.survey = SurveyFactory()
        ast = AnswerType.objects.get(slug='answer_short_text')
        self.questions = [
            SurveyQuestionFactory(param_slug='q1', survey=self.survey, answer_type=ast, page=1, position=1),
            SurveyQuestionFactory(param_slug='q2', survey=self.survey, answer_type=ast, page=2, position=1),
            SurveyQuestionFactory(param_slug='q3', survey=self.survey, answer_type=ast, page=2, position=2),
            SurveyQuestionFactory(param_slug='q4', survey=self.survey, answer_type=ast, page=3, position=1),
        ]

    def get_expected_slug(self, idx):
        question = self.questions[idx]
        return '%s_%s' % (question.answer_type.slug, question.pk)

    def delete_question(self, idx):
        self.client.login_yandex(is_superuser=True)
        response = self.client.delete('/admin/api/v2/survey-questions/%s/' % self.questions[idx].pk)
        self.client.login_yandex(is_staff=True)
        return response

    def restore_question(self, idx, data=None, new_page=False):
        url = f"/admin/api/v2/survey-questions/{self.questions[idx].pk}/restore/"
        if new_page:
            url = '{}?{}'.format(url, 'new_page=true')
        return self.client.post(url, data)

    def get_questions(self):
        return list(self.survey.surveyquestion_set.values_list('param_slug', 'page', 'position'))

    def test_restore_first_question_without_params(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        response = self.client.post('/admin/api/v2/survey-questions/%s/restore/' % self.questions[0].pk)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [(expected_slug, 1, 1), ('q2', 1, 2), ('q3', 1, 3), ('q4', 2, 1)])

    def test_restore_first_question_on_new_page(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(0, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [(expected_slug, 1, 1), ('q2', 2, 1), ('q3', 2, 2), ('q4', 3, 1)])

    def test_restore_first_question_on_new_page_more_complex(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(0, data=data, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [('q2', 1, 1), ('q3', 1, 2), (expected_slug, 2, 1), ('q4', 3, 1)])

    def test_restore_first_question_on_second_page(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2}
        response = self.restore_question(0, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [('q2', 1, 1), ('q3', 1, 2), (expected_slug, 2, 1), ('q4', 2, 2)])

    def test_restore_first_question_on_second_page_on_last_position(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(0, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(0)
        self.assertEqual(self.get_questions(), [('q2', 1, 1), ('q3', 1, 2), ('q4', 2, 1), (expected_slug, 2, 2)])

    def test_restore_second_question_without_params(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(1)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q3', 2, 2), ('q4', 3, 1)])

    def test_restore_second_question_on_new_page(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(1, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q3', 3, 1), ('q4', 4, 1)])

    def test_restore_second_question_on_new_page_more_complex(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(1, data=data, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q3', 3, 1), ('q4', 4, 1)])

    def test_restore_second_question_on_second_page_on_last_position(self):
        response = self.delete_question(1)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(1, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(1)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q3', 2, 1), (expected_slug, 2, 2), ('q4', 3, 1)])

    def test_restore_fourth_question_without_params(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(3)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q2', 2, 1), ('q3', 2, 2), (expected_slug, 3, 1)])

    def test_restore_fourth_question_on_new_page(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        response = self.restore_question(3, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q2', 2, 1), ('q3', 2, 2), (expected_slug, 3, 1)])

    def test_restore_fourth_question_on_new_page_more_complex(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(3, data=data, new_page=True)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), (expected_slug, 2, 1), ('q2', 3, 1), ('q3', 3, 2)])

    def test_restore_fourth_question_on_second_page_second_position(self):
        response = self.delete_question(3)
        self.assertEqual(response.status_code, 204)

        data = {'page': 2, 'position': 2}
        response = self.restore_question(3, data=data)
        self.assertEqual(response.status_code, 200)

        expected_slug = self.get_expected_slug(3)
        self.assertEqual(self.get_questions(), [('q1', 1, 1), ('q2', 2, 1), (expected_slug, 2, 2), ('q3', 2, 3)])


class TestRestoreQuestionNoSuperUserNoSupport(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex()
        self.survey = SurveyFactory()
        ast = AnswerType.objects.get(slug='answer_short_text')
        self.questions = [
            SurveyQuestionFactory(param_slug='q1', survey=self.survey, answer_type=ast, page=1, position=1),
            SurveyQuestionFactory(param_slug='q2', survey=self.survey, answer_type=ast, page=2, position=1),
            SurveyQuestionFactory(param_slug='q3', survey=self.survey, answer_type=ast, page=2, position=2),
            SurveyQuestionFactory(param_slug='q4', survey=self.survey, answer_type=ast, page=3, position=1),
        ]

    def delete_question(self, idx):
        self.client.login_yandex(is_superuser=True)
        response = self.client.delete('/admin/api/v2/survey-questions/%s/' % self.questions[idx].pk)
        self.client.login_yandex()
        return response

    def restore_question(self, idx):
        return self.client.post(f"/admin/api/v2/survey-questions/{self.questions[idx].pk}/restore/")

    def test_restore_first_question_without_params(self):
        response = self.delete_question(0)
        self.assertEqual(response.status_code, 204)

        response = self.client.post('/admin/api/v2/survey-questions/%s/restore/' % self.questions[0].pk)
        self.assertEqual(response.status_code, 403)
