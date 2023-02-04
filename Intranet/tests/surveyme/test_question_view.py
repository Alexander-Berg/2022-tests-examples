# coding: utf-8
from django.conf import settings
from django.test import TestCase, override_settings

from events.accounts.helpers import YandexClient
from events.conditions.factories import ContentTypeAttributeFactory
from events.media.api_admin.v2.serializers import ImageSerializer
from events.media.factories import ImageFactory
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import (
    AnswerType,
    SurveyQuestion,
    SurveyQuestionShowConditionNode,
    SurveyQuestionShowConditionNodeItem,
)
from events.surveyme.api_admin.v2.serializers import QUIZ_MAX_SCORES
from events.surveyme_integration.factories import (
    SurveyHookConditionFactory,
    SurveyHookConditionNodeFactory,
    SurveyHookFactory,
)


class TestBaseQuestions(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.url = self._get_url()

        answer_type = AnswerType.objects.get(slug='answer_short_text')

        self.page_to_questions = {}
        for page in range(1, 7):
            q1 = SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_type,
                page=page,
            )
            q2 = SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_type,
                page=page,
            )
            self.page_to_questions[page] = [q1, q2]

    def _get_pk_to_question(self):
        questions = SurveyQuestion.objects.filter(survey=self.survey)
        return {q.pk: q for q in questions}

    def _get_url(self):
        raise NotImplementedError


class TestMovePageQuestions(TestBaseQuestions):
    def _get_url(self):
        return '/admin/api/v2/surveys/{survey}/move-page/'.format(survey=self.survey.pk)

    def test_move_page_up(self):
        response = self.client.post(self.url, {
            'page': 5,
            'to': 3,
        })
        self.assertEqual(200, response.status_code)

        pk_to_question = self._get_pk_to_question()

        # 5 -> 3, 3 -> 4, 4 -> 5
        self.assertEqual(3, pk_to_question[self.page_to_questions[5][0].pk].page)
        self.assertEqual(3, pk_to_question[self.page_to_questions[5][1].pk].page)
        self.assertEqual(4, pk_to_question[self.page_to_questions[3][0].pk].page)
        self.assertEqual(4, pk_to_question[self.page_to_questions[3][1].pk].page)
        self.assertEqual(5, pk_to_question[self.page_to_questions[4][0].pk].page)
        self.assertEqual(5, pk_to_question[self.page_to_questions[4][1].pk].page)

    def test_move_page_down(self):
        response = self.client.post(self.url, {
            'page': 1,
            'to': 4,
        })
        self.assertEqual(200, response.status_code)

        pk_to_question = self._get_pk_to_question()

        # 1 -> 4, 2 -> 1, 3 -> 2, 4 -> 3
        self.assertEqual(4, pk_to_question[self.page_to_questions[1][0].pk].page)
        self.assertEqual(4, pk_to_question[self.page_to_questions[1][1].pk].page)
        self.assertEqual(1, pk_to_question[self.page_to_questions[2][0].pk].page)
        self.assertEqual(1, pk_to_question[self.page_to_questions[2][1].pk].page)
        self.assertEqual(2, pk_to_question[self.page_to_questions[3][0].pk].page)
        self.assertEqual(2, pk_to_question[self.page_to_questions[3][1].pk].page)
        self.assertEqual(3, pk_to_question[self.page_to_questions[4][0].pk].page)
        self.assertEqual(3, pk_to_question[self.page_to_questions[4][1].pk].page)

    def test_move_unexistent_page(self):
        response = self.client.post(self.url, {
            'page': 8,
            'to': 4,
        })
        self.assertEqual(400, response.status_code)

        response = self.client.post(self.url, {
            'page': 3,
            'to': 9,
        })
        self.assertEqual(400, response.status_code)

    def test_move_page_questions_in_condition_down(self):
        question_in_condition = self.page_to_questions[2][1]
        question_with_condition = self.page_to_questions[3][0]

        condition_node = SurveyQuestionShowConditionNode.objects.create(
            survey_question=question_with_condition
        )
        content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )
        SurveyQuestionShowConditionNodeItem.objects.create(
            survey_question_show_condition_node=condition_node,
            survey_question=question_in_condition,
            content_type_attribute_id=content_type_attribute.pk,
            operator='or', value='foo',
        )

        # Вопрос на странице 3 зависит от вопроса на странице 2,
        # нельзя переместить страницу 2 вниз на страницу 4.
        response = self.client.post(self.url, {
            'page': 2,
            'to': 4,
        })
        self.assertEqual(400, response.status_code)


class TestDeletePageQuestions(TestBaseQuestions):
    def _get_url(self):
        return '/admin/api/v2/surveys/{survey}/page/'.format(survey=self.survey.pk)

    def test_delete_page(self):
        response = self.client.delete(self.url, {
            'page': 4,
        })
        self.assertEqual(200, response.status_code)

        pk_to_question = self._get_pk_to_question()

        # 4 -> *, 5 -> 4, 6 -> 5
        self.assertEqual(4, pk_to_question[self.page_to_questions[5][0].pk].page)
        self.assertEqual(4, pk_to_question[self.page_to_questions[5][1].pk].page)
        self.assertEqual(5, pk_to_question[self.page_to_questions[6][0].pk].page)
        self.assertEqual(5, pk_to_question[self.page_to_questions[6][1].pk].page)

    def test_delete_unexistent_page(self):
        response = self.client.delete(self.url, {
            'page': 7,
        })
        self.assertEqual(400, response.status_code)


@override_settings(IMAGE_SIZES=[(350, 70), (30, None)])
class TestSurveyQuestionsViewSet__image(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.image = ImageFactory()

    def test_show_image_correctly(self):
        url = '/admin/api/v2/survey-questions/{question_id}/'.format(question_id=self.question.pk)
        self.question.label_image = self.image
        self.question.save()

        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.data['label_image'],
            ImageSerializer(self.image).data,
        )

    def test_drop_connection_correctly(self):
        url = '/admin/api/v2/survey-questions/{question_id}/'.format(question_id=self.question.pk)
        self.question.label_image = self.image
        self.question.save()

        response = self.client.patch(
            url, {"label_image": None},
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['label_image'])
        self.question.refresh_from_db()
        self.assertIsNone(self.question.label_image_id)

    def test_create_connection_correctly(self):
        url = '/admin/api/v2/survey-questions/{question_id}/'.format(question_id=self.question.pk)

        response = self.client.patch(
            url, {"label_image": self.image.id},
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.data['label_image'],
            ImageSerializer(self.image).data,
        )
        self.question.refresh_from_db()
        self.assertEqual(self.question.label_image_id, self.image.id)

    def test_create_connection_fail_wrong_pk(self):
        url = '/admin/api/v2/survey-questions/{question_id}/'.format(question_id=self.question.pk)

        response = self.client.patch(
            url, {"label_image": self.image.id+1},
            format='json',
        )
        self.assertEqual(response.status_code, 400)
        self.assertDictEqual(
            response.data,
            {'label_image': ['Объекта с таким pk не существует']},
        )
        self.question.refresh_from_db()
        self.assertIsNone(self.question.label_image_id)


class TestQuestionWithSurveyHookCondition(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_group'),
            ),
        ]
        self.hook = SurveyHookFactory(survey=self.survey)
        self.condition_node = SurveyHookConditionNodeFactory(hook=self.hook)
        self.condition = SurveyHookConditionFactory(
            condition_node=self.condition_node,
            survey_question=self.questions[1],
        )

    def test_should_move_first_question_to_group(self):
        url = '/admin/api/v2/survey-questions/%s/' % self.questions[0].pk
        data = {
            'group_id': self.questions[2].pk,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.questions[0].refresh_from_db()
        self.assertEqual(self.questions[0].group_id, self.questions[2].pk)

    def test_should_move_first_question_to_group_with_newpage_param(self):
        url = '/admin/api/v2/survey-questions/%s/?newpage=true' % self.questions[0].pk
        data = {
            'group_id': self.questions[2].pk,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.questions[0].refresh_from_db()
        self.assertEqual(self.questions[0].group_id, self.questions[2].pk)

    def test_shouldnt_move_second_question_to_group(self):
        url = '/admin/api/v2/survey-questions/%s/' % self.questions[1].pk
        data = {
            'group_id': self.questions[2].pk,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.questions[1].refresh_from_db()
        self.assertIsNone(self.questions[1].group_id)

    def test_shouldnt_move_second_question_to_group_with_newpage_param(self):
        url = '/admin/api/v2/survey-questions/%s/?newpage=true' % self.questions[1].pk
        data = {
            'group_id': self.questions[2].pk,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.questions[1].refresh_from_db()
        self.assertIsNone(self.questions[1].group_id)


class TestPaymentQuestion(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.answer_payment = AnswerType.objects.get(slug='answer_payment')

    def test_should_create_first_payment_question(self):
        survey = SurveyFactory()
        data = {
            'survey_id': survey.pk,
            'answer_type_id': self.answer_payment.pk,
            'label': 'Payment',
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

        data = response.data
        self.assertIn('param_payment', data)

        param_payment = data['param_payment']
        self.assertIn('account_id', param_payment)
        self.assertIn('is_fixed', param_payment)

        self.assertIsNone(param_payment['account_id'])
        self.assertTrue(param_payment['account_id'] != '')
        self.assertFalse(param_payment['is_fixed'])

    def test_shouldnt_create_second_payment_question(self):
        survey = SurveyFactory()
        SurveyQuestionFactory(
            survey=survey,
            answer_type=self.answer_payment,
            label='Payment',
        )
        data = {
            'survey_id': survey.pk,
            'answer_type_id': self.answer_payment.pk,
            'label': 'Another Payment',
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_shouldnt_create_payment_question_in_group(self):
        survey = SurveyFactory()
        group_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            label='Group',
        )
        data = {
            'survey_id': survey.pk,
            'answer_type_id': self.answer_payment.pk,
            'group_id': group_question.pk,
            'label': 'Payment',
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_shouldnt_copy_payment_question(self):
        survey = SurveyFactory()
        payment_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=self.answer_payment,
            label='Payment',
        )
        response = self.client.post('/admin/api/v2/survey-questions/%s/copy/' % payment_question.pk)
        self.assertEqual(response.status_code, 400)

    def test_shouldnt_move_payment_question_into_group(self):
        survey = SurveyFactory()
        payment_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=self.answer_payment,
            label='Payment',
        )
        group_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            label='Group',
        )
        data = {
            'group_id': group_question.pk,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % payment_question.pk,
                                     data=data, format='json')
        self.assertEqual(response.status_code, 400)


class TestParamQuiz(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

    def test_should_create_param_quiz(self):
        from events.surveyme.dataclasses import ParamQuiz
        data = {
            'param_quiz': {
                'enabled': True,
                'answers': [
                    {
                        'correct': True,
                        'scores': 2,
                        'value': 'two',
                    },
                    {
                        'correct': True,
                        'scores': 3,
                        'value': 'three',
                    },
                    {
                        'correct': False,
                        'scores': 0,
                        'value': 'zero',
                    },
                ],
            },
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.question.refresh_from_db()
        param_quiz = ParamQuiz(self.question.param_quiz)
        self.assertTrue(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 3)

        self.assertTrue(param_quiz.answers[0].correct)
        self.assertEqual(param_quiz.answers[0].scores, 2)
        self.assertEqual(param_quiz.answers[0].value, 'two')

        self.assertTrue(param_quiz.answers[1].correct)
        self.assertEqual(param_quiz.answers[1].scores, 3)
        self.assertEqual(param_quiz.answers[1].value, 'three')

        self.assertFalse(param_quiz.answers[2].correct)
        self.assertEqual(param_quiz.answers[2].scores, 0)
        self.assertEqual(param_quiz.answers[2].value, 'zero')

    def test_should_modify_param_quiz(self):
        from events.surveyme.dataclasses import ParamQuiz
        self.question.param_quiz = {
            'param_quiz': {
                'enabled': True,
                'answers': [
                    {
                        'correct': True,
                        'scores': 2,
                        'value': 'two',
                    },
                ],
            },
        }
        self.question.save()

        data = {
            'param_quiz': {
                'enabled': True,
                'answers': [
                    {
                        'correct': True,
                        'scores': 1,
                        'value': 'one',
                    },
                    {
                        'correct': True,
                        'scores': 3,
                        'value': 'three',
                    },
                ],
            },
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.question.refresh_from_db()
        param_quiz = ParamQuiz(self.question.param_quiz)
        self.assertTrue(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 2)

        self.assertTrue(param_quiz.answers[0].correct)
        self.assertEqual(param_quiz.answers[0].scores, 1)
        self.assertEqual(param_quiz.answers[0].value, 'one')

        self.assertTrue(param_quiz.answers[1].correct)
        self.assertEqual(param_quiz.answers[1].scores, 3)
        self.assertEqual(param_quiz.answers[1].value, 'three')

    def test_should_create_empty_param_quiz(self):
        from events.surveyme.dataclasses import ParamQuiz
        data = {}
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.question.refresh_from_db()
        param_quiz = ParamQuiz(self.question.param_quiz)
        self.assertFalse(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 0)

    def test_should_throw_validation_error(self):
        data = {
            'param_quiz': {
                'enabled': True,
                'answers': [
                    {
                        'correct': '1True',
                        'scores': 1,
                        'value': 'one',
                    },
                    {
                        'correct': True,
                        'scores': 'three3',
                        'value': 'three',
                    },
                    {
                        'correct': True,
                        'scores': 4,
                        'value': 'X'*61,
                    },
                    {
                        'correct': True,
                        'scores': QUIZ_MAX_SCORES + 1,
                        'value': 'four',
                    },
                    {
                        'correct': True,
                        'scores': -QUIZ_MAX_SCORES - 1,
                        'value': 'five',
                    },
                ],
            },
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

        errors = response.data
        self.assertIn('param_quiz', errors)
        self.assertIn('answers', errors['param_quiz'])
        self.assertEqual(len(errors['param_quiz']['answers']), 5)
        self.assertIn('correct', errors['param_quiz']['answers'][0])
        self.assertIn('scores', errors['param_quiz']['answers'][1])
        self.assertIn('scores', errors['param_quiz']['answers'][3])
        self.assertIn('scores', errors['param_quiz']['answers'][4])

    def test_should_throw_validation_error_on_ommited_value(self):
        data = {
            'param_quiz': {
                'answers': [
                    {
                        'correct': True,
                        'scores': 4,
                    },
                ],
            },
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

        errors = response.data
        self.assertIn('param_quiz', errors)
        self.assertIn('answers', errors['param_quiz'])
        self.assertEqual(len(errors['param_quiz']['answers']), 1)
        self.assertIn('value', errors['param_quiz']['answers'][0])

    def test_should_throw_validation_error_on_nulled_value(self):
        data = {
            'param_quiz': {
                'answers': [
                    {
                        'correct': True,
                        'scores': 4,
                        'value': None,
                    },
                ],
            },
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

        errors = response.data
        self.assertIn('param_quiz', errors)
        self.assertIn('answers', errors['param_quiz'])
        self.assertEqual(len(errors['param_quiz']['answers']), 1)
        self.assertIn('value', errors['param_quiz']['answers'][0])

    def test_should_throw_validation_error_on_empty_value(self):
        data = {
            'param_quiz': {
                'answers': [
                    {
                        'correct': True,
                        'scores': 4,
                        'value': '',
                    },
                ],
            },
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

        errors = response.data
        self.assertIn('param_quiz', errors)
        self.assertIn('answers', errors['param_quiz'])
        self.assertEqual(len(errors['param_quiz']['answers']), 1)
        self.assertIn('value', errors['param_quiz']['answers'][0])

    def test_create_question_with_param_quiz(self):
        from events.surveyme.dataclasses import ParamQuiz
        survey = SurveyFactory()
        data = {
            'survey_id': survey.pk,
            'answer_type_id': AnswerType.objects.get(slug='answer_short_text').pk,
            'label': 'Stationery',
            'param_quiz': {
                'enabled': True,
                'answers': [
                    {
                        'scores': 2.0,
                        'correct': True,
                        'value': 'pen'
                    },
                    {
                        'scores': 3.0,
                        'correct': True,
                        'value': 'pensil'
                    },
                ],
            }
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

        question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertIsNotNone(question.param_quiz)

        param_quiz = ParamQuiz(question.param_quiz)
        self.assertTrue(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 2)
        self.assertEqual(param_quiz.answers[0].scores, 2.0)
        self.assertEqual(param_quiz.answers[0].value, 'pen')
        self.assertTrue(param_quiz.answers[0].correct)
        self.assertEqual(param_quiz.answers[1].scores, 3.0)
        self.assertEqual(param_quiz.answers[1].value, 'pensil')
        self.assertTrue(param_quiz.answers[1].correct)


class TestSurveyQuestionFieldsRestrictionsCase(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.answer_type = AnswerType.objects.get(slug='answer_choices')
        self.survey = SurveyFactory()
        self.question_for_patch = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_type,
            page=1,
        )

    def test_post_answer_id_conditional_field_length_limitation_positive(self):
        correct_data = {
            'survey_id': self.survey.id,
            'answer_type_id': self.answer_type.id,
            'label': 'short_label',
            'param_help_text': 'short_help_text'
        }
        response = self.client.post(
            '/admin/api/v2/survey-questions/',
            data=correct_data,
            format='json'
        )
        self.assertEqual(201, response.status_code)

    def test_post_answer_id_conditional_label_limitation_negative(self):
        data_w_bad_label = {
            'survey_id': self.survey.id,
            'answer_type_id': self.answer_type.id,
            'label': ('1' * 5001),
            'param_help_text': 'short_help_text'
        }
        response = self.client.post(
            '/admin/api/v2/survey-questions/',
            data=data_w_bad_label,
            format='json'
        )
        self.assertEqual(400, response.status_code)

    def test_post_answer_id_conditional_param_help_text_limitation_negative(self):
        data_w_bad_param_help_text = {
            'survey_id': self.survey.id,
            'answer_type_id': self.answer_type.id,
            'label': 'short_label',
            'param_help_text': ('1' * 5001)
        }
        response = self.client.post(
            '/admin/api/v2/survey-questions/',
            data=data_w_bad_param_help_text,
            format='json'
        )
        self.assertEqual(400, response.status_code)

    def test_patch_answer_id_conditional_field_length_limitation_positive(self):
        correct_data = {
            'label': 'short_label',
            'param_help_text': 'short_help_text'
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/{question_id}/'.format(
                question_id=self.question_for_patch.id
            ),
            data=correct_data,
            format='json'
        )
        self.assertEqual(200, response.status_code)

    def test_patch_answer_id_conditional_label_limitation_negative(self):
        data_w_bad_label = {
            'label': ('1' * 5001),
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/{question_id}/'.format(
                question_id=self.question_for_patch.id
            ),
            data=data_w_bad_label,
            format='json'
        )
        self.assertEqual(400, response.status_code)

    def test_patch_answer_id_conditional_param_help_text_limitation_negative(self):
        data_w_bad_param_help_text = {
            'param_help_text': ('1' * 5001)
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/{question_id}/'.format(
                question_id=self.question_for_patch.id
            ),
            data=data_w_bad_param_help_text,
            format='json'
        )
        self.assertEqual(400, response.status_code)


class TestQuestionParams(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

    def test_should_save_question_with_big_param_max(self):
        data = {
            'param_max': settings.MAX_POSTGRESQL_INT_VALUE - 1,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_save_question_with_enormous_param_max(self):
        data = {
            'param_max': settings.MAX_POSTGRESQL_INT_VALUE * 10,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_should_save_question_with_small_param_max(self):
        data = {
            'param_max': -settings.MAX_POSTGRESQL_INT_VALUE + 1,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_save_question_with_tiny_param_max(self):
        data = {
            'param_max': -settings.MAX_POSTGRESQL_INT_VALUE * 10,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_should_save_question_with_big_param_min(self):
        data = {
            'param_min': settings.MAX_POSTGRESQL_INT_VALUE - 1,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_save_question_with_enormous_param_min(self):
        data = {
            'param_min': settings.MAX_POSTGRESQL_INT_VALUE * 10,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_should_save_question_with_small_param_min(self):
        data = {
            'param_min': -settings.MAX_POSTGRESQL_INT_VALUE + 1,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_save_question_with_tiny_param_min(self):
        data = {
            'param_min': -settings.MAX_POSTGRESQL_INT_VALUE * 10,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_should_save_question_with_param_min_less_then_param_max(self):
        data = {
            'param_min': -100,
            'param_max': 100,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_save_question_with_param_min_greater_then_param_max(self):
        data = {
            'param_min': 100,
            'param_max': -100,
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % self.question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)


class TestQuestionFieldValidation_param_slug(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.answer_type = AnswerType.objects.get(slug='answer_short_text')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_type,
            label='Text',
            param_slug='text',
        )

    def test_should_modify_question_without_param_slug_changed(self):
        data = {
            'label': 'New Text',
            'param_slug': 'text',
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{self.question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_should_create_question_with_unique_param_slug(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_type.pk,
            'label': 'New Text',
            'param_slug': 'new-text',
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

    def test_shouldnt_create_question_with_existing_param_slug(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_type.pk,
            'label': 'New Text',
            'param_slug': 'text',
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_should_modify_question_with_unique_param_slug(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_type,
            label='New Text',
        )
        data = {
            'param_slug': 'new-text',
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_modify_question_with_existing_param_slug(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_type,
            label='New Text',
        )
        data = {
            'param_slug': 'text',
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 400)


class TestQuestionFieldValidation__param_is_required(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.text_answer_type = AnswerType.objects.get(slug='answer_short_text')
        self.group_answer_type = AnswerType.objects.get(slug='answer_group')

    def test_should_create_group_question(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.group_answer_type.pk,
            'label': '.',
            'param_is_required': True,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['survey_id'], self.survey.pk)
        self.assertEqual(response.data['answer_type_id'], self.group_answer_type.pk)
        self.assertEqual(response.data['label'], '.')
        self.assertFalse(response.data['param_is_required'])

    def test_should_create_text_question(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.text_answer_type.pk,
            'label': '.',
            'param_is_required': True,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['survey_id'], self.survey.pk)
        self.assertEqual(response.data['answer_type_id'], self.text_answer_type.pk)
        self.assertEqual(response.data['label'], '.')
        self.assertTrue(response.data['param_is_required'])

    def test_should_create_grouped_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.group_answer_type,
        )
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.text_answer_type.pk,
            'group_id': question.pk,
            'label': '.',
            'param_is_required': True,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, format='json')
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['survey_id'], self.survey.pk)
        self.assertEqual(response.data['answer_type_id'], self.text_answer_type.pk)
        self.assertEqual(response.data['group_id'], question.pk)
        self.assertEqual(response.data['label'], '.')
        self.assertTrue(response.data['param_is_required'])

    def test_should_update_group_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.group_answer_type,
            param_is_required=False,
        )
        data = {
            'param_is_required': True,
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['id'], question.pk)
        self.assertFalse(response.data['param_is_required'])

    def test_should_update_text_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.text_answer_type,
            param_is_required=False,
        )
        data = {
            'param_is_required': True,
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['id'], question.pk)
        self.assertTrue(response.data['param_is_required'])

    def test_should_update_grouped_question(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.group_answer_type,
            param_is_required=False,
        )
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.text_answer_type,
            group_id=group_question.pk,
            param_is_required=False,
        )
        data = {
            'param_is_required': True,
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['id'], question.pk)
        self.assertTrue(response.data['param_is_required'])
