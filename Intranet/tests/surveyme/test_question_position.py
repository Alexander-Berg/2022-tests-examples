# -*- coding: utf-8 -*-
from django.test import TestCase
from rest_framework.exceptions import ValidationError as RestValidationError

from events.accounts.helpers import YandexClient
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionShowConditionNodeFactory,
    SurveyQuestionShowConditionNodeItemFactory,
)
from events.surveyme.models import (
    AnswerType,
    SurveyQuestion,
)


class Param:
    def __init__(self, page=None, position=None):
        self.page, self.position = page, position


class TestSurveyQuestionPosition(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(type='simple_form')
        self.questions = [
            SurveyQuestionFactory(survey=self.survey, param_slug='1', page=1, position=1),
            SurveyQuestionFactory(survey=self.survey, param_slug='2', page=1, position=2),
            SurveyQuestionFactory(survey=self.survey, param_slug='3', page=1, position=3),
            SurveyQuestionFactory(survey=self.survey, param_slug='4', page=2, position=1),
            SurveyQuestionFactory(survey=self.survey, param_slug='5', page=2, position=2),
            SurveyQuestionFactory(survey=self.survey, param_slug='6', page=2, position=3),
        ]

        self.condition_node = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[4])
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )
        self.condition = SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.condition_node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[3],
            value='1',
        )

        self.create_url = '/admin/api/v2/survey-questions/'
        self.update_url = '/admin/api/v2/survey-questions/%s/'
        self.delete_url = '/admin/api/v2/survey-questions/%s/'
        self.move_to_position_url = '/admin/api/v2/survey-questions/%s/move-to-position/'

    def check_positions(self):
        old_page, old_position = 0, 0
        queryset = (
            self.survey.surveyquestion_set.all()
            .values_list('param_slug', 'page', 'position')
        )
        for param_slug, page, position in queryset:
            if (
                (page <= 0) or
                (position <= 0) or
                (old_page + 1 < page) or
                (old_page == page and old_position == position) or
                (old_page == page and old_position + 1 < position)
            ):
                self.fail('invalid ``%s``: (%s, %s)' % (param_slug, page, position))
            old_page, old_position = page, position

    def check_create_question_should_success(self, source, expected):
        data = {
            'label': 'new question',
            'survey_id': self.survey.pk,
            'answer_type_id': 1,
            'param_slug': '7',
        }
        if source.page is not None:
            data['page'] = source.page
        if source.position is not None:
            data['position'] = source.position

        response = self.client.post(self.create_url, data=data)
        self.assertEqual(201, response.status_code)

        data = response.data
        self.assertEqual(expected.page, data['page'])
        self.assertEqual(expected.position, data['position'])
        self.check_positions()

    def test_create_question_should_success_1(self):
        # вопрос без указания страницы/позиции должен создаться в конце первой страницы
        self.check_create_question_should_success(Param(None, None), Param(1, 4))

    def test_create_question_should_success_2(self):
        self.check_create_question_should_success(Param(2, None), Param(2, 4))

    def test_create_question_should_success_3(self):
        # вопрос должен создаться на указанном месте
        self.check_create_question_should_success(Param(2, 2), Param(2, 2))

    def test_create_question_should_success_4(self):
        self.check_create_question_should_success(Param(2, 4), Param(2, 4))

    def test_create_question_should_success_5(self):
        # далее идут тесты с уакзанием несуществующих страниц и позиций
        # если страница не существует - добавляем новую, позиции вычисляются по-порядку
        self.check_create_question_should_success(Param(3, 1), Param(3, 1))

    def test_create_question_should_success_6(self):
        self.check_create_question_should_success(Param(3, 3), Param(3, 1))

    def test_create_question_should_success_7(self):
        self.check_create_question_should_success(Param(4, 1), Param(3, 1))

    def test_create_question_should_success_8(self):
        self.check_create_question_should_success(Param(4, 3), Param(3, 1))

    def check_update_question_should_success(self, question_idx, source, expected):
        question = self.questions[question_idx]
        data = {}
        if source.page is not None:
            data['page'] = source.page
        if source.position is not None:
            data['position'] = source.position

        response = self.client.patch(self.update_url % question.pk, data=data, format='json')
        self.assertEqual(200, response.status_code)

        data = response.data
        self.assertEqual(expected.page, data['page'])
        self.assertEqual(expected.position, data['position'])
        self.check_positions()

    def test_update_question_should_success_1(self):
        # без указания номера страницы перемещение происходит в пределах текущей странцы вопроса
        self.check_update_question_should_success(0, Param(None, 2), Param(1, 2))

    def test_update_question_should_success_2(self):
        self.check_update_question_should_success(0, Param(None, 5), Param(1, 3))

    def test_update_question_should_success_3(self):
        self.check_update_question_should_success(1, Param(None, 1), Param(1, 1))

    def test_update_question_should_success_4(self):
        self.check_update_question_should_success(1, Param(None, 5), Param(1, 3))

    def test_update_question_should_success_5(self):
        self.check_update_question_should_success(4, Param(None, 3), Param(2, 3))

    def test_update_question_should_success_6(self):
        self.check_update_question_should_success(4, Param(None, 5), Param(2, 3))

    def test_update_question_should_success_7(self):
        # без указания позиции перемещение происходит в текущую позицию вопроса на новой странице
        self.check_update_question_should_success(0, Param(2, None), Param(2, 1))

    def test_update_question_should_success_8(self):
        self.check_update_question_should_success(0, Param(3, None), Param(3, 1))

    def test_update_question_should_success_9(self):
        self.check_update_question_should_success(0, Param(4, None), Param(3, 1))

    def test_update_question_should_success_10(self):
        # если передана страница и позиция перемещение должно произойти в указанное место
        self.check_update_question_should_success(3, Param(1, 1), Param(1, 1))

    def test_update_question_should_success_11(self):
        # далее идут тесты с уакзанием несуществующих страниц и позиций
        # если страница не существует - добавляем новую, позиции вычисляются по-порядку
        self.check_update_question_should_success(0, Param(3, 1), Param(3, 1))

    def test_update_question_should_success_12(self):
        self.check_update_question_should_success(0, Param(3, 3), Param(3, 1))

    def test_update_question_should_success_13(self):
        self.check_update_question_should_success(0, Param(4, 1), Param(3, 1))

    def test_update_question_should_success_14(self):
        self.check_update_question_should_success(0, Param(4, 3), Param(3, 1))

    def check_update_question_should_fail(self, question_idx, source):
        question = self.questions[question_idx]
        data = {}
        if source.page is not None:
            data['page'] = source.page
        if source.position is not None:
            data['position'] = source.position

        response = self.client.patch(self.update_url % question.pk, data=data, format='json')
        self.assertEqual(400, response.status_code)
        self.check_positions()

    def test_update_question_should_fail_1(self):
        # проверка перемещения вопросов с логикой
        self.check_update_question_should_fail(3, Param(None, 3))  # question_in_condition

    def test_update_question_should_fail_2(self):
        self.check_update_question_should_fail(4, Param(None, 1))  # question_with_condition

    def test_update_question_should_fail_3(self):
        # если передаем позицию равной 0 - должно быть выброшено исключение
        self.check_update_question_should_fail(0, Param(None, 0))  # invalid position

    def check_delete_question_should_success(self, question_idx):
        question = self.questions[question_idx]
        response = self.client.delete(self.delete_url % question.pk)
        self.assertEqual(204, response.status_code)
        self.check_positions()

    def test_delete_question_should_success_1(self):
        self.check_delete_question_should_success(0)

    def test_delete_question_should_success_2(self):
        self.check_delete_question_should_success(2)

    def test_delete_question_should_success_3(self):
        self.check_delete_question_should_success(3)

    def test_delete_question_should_success_4(self):
        self.check_delete_question_should_success(4)

    def check_move_to_position_should_success(self, question_idx, source):
        question = self.questions[question_idx]
        data = {
            'position': source.position,
        }
        if source.page is not None:
            data['page'] = source.page

        response = self.client.post(self.move_to_position_url % question.pk, data=data)
        self.assertEqual(200, response.status_code)
        self.check_positions()

    def test_move_to_position_should_success_1(self):
        # без указания номера страницы перемещение происходит в пределах текущей странцы вопроса
        self.check_move_to_position_should_success(0, Param(None, 2))

    def test_move_to_position_should_success_2(self):
        self.check_move_to_position_should_success(0, Param(None, 5))

    def test_move_to_position_should_success_3(self):
        self.check_move_to_position_should_success(1, Param(None, 1))

    def test_move_to_position_should_success_4(self):
        self.check_move_to_position_should_success(1, Param(None, 5))

    def test_move_to_position_should_success_5(self):
        self.check_move_to_position_should_success(4, Param(None, 3))

    def test_move_to_position_should_success_6(self):
        self.check_move_to_position_should_success(4, Param(None, 5))

    def test_move_to_position_should_success_7(self):
        # если передана страница и позиция перемещение должно произойти в указанное место
        self.check_move_to_position_should_success(0, Param(2, 1))

    def test_move_to_position_should_success_8(self):
        self.check_move_to_position_should_success(3, Param(1, 1))

    def test_move_to_position_should_success_9(self):
        # далее идут тесты с уакзанием несуществующих страниц и позиций
        # если страница не существует - добавляем новую, позиции вычисляются по-порядку
        self.check_move_to_position_should_success(0, Param(3, 1))

    def test_move_to_position_should_success_10(self):
        self.check_move_to_position_should_success(0, Param(3, 3))

    def test_move_to_position_should_success_11(self):
        self.check_move_to_position_should_success(0, Param(4, 1))

    def test_move_to_position_should_success_12(self):
        self.check_move_to_position_should_success(0, Param(4, 3))

    def check_move_to_position_should_fail(self, question_idx, source):
        question = self.questions[question_idx]
        data = {
            'position': source.position,
        }
        if source.page is not None:
            data['page'] = source.page

        response = self.client.post(self.move_to_position_url % question.pk, data=data)
        self.assertEqual(400, response.status_code)
        self.check_positions()

    def test_move_to_position_should_fail_1(self):
        # проверка перемещения вопросов с логикой
        self.check_move_to_position_should_fail(3, Param(None, 3))  # question_in_condition

    def test_move_to_position_should_fail_2(self):
        self.check_move_to_position_should_fail(4, Param(None, 1))  # question_with_condition

    def test_move_to_position_should_fail_3(self):
        # если передаем позицию равной 0 - должно быть выброшено исключение
        self.check_move_to_position_should_fail(0, Param(None, 0))  # invalid position


class TestSurveyQuestion_move_to_position(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        # создадим опрос
        self.survey = SurveyFactory()

    def test_experiments(self):
        SurveyQuestion.objects.bulk_create([
            SurveyQuestionFactory.build(survey=self.survey, id=1),
            SurveyQuestionFactory.build(survey=self.survey, id=2),
            SurveyQuestionFactory.build(survey=self.survey, id=3),
            SurveyQuestionFactory.build(survey=self.survey, id=4),
            SurveyQuestionFactory.build(survey=self.survey, id=5),
            SurveyQuestionFactory.build(survey=self.survey, id=6),
            SurveyQuestionFactory.build(survey=self.survey, id=7),
        ])

        # нужно помнить, что каждый следующий exp зависит от предыдущего
        experiments = [
            {
                'set': {
                    'id': 3,
                    'position': 2
                },
                'expected': [
                    {'position': 1, 'id': 1},
                    {'position': 2, 'id': 3},
                    {'position': 3, 'id': 2},
                    {'position': 4, 'id': 4},
                    {'position': 5, 'id': 5},
                    {'position': 6, 'id': 6},
                    {'position': 7, 'id': 7},
                ]
            },
            {
                'set': {
                    'id': 1,
                    'position': 7
                },
                'expected': [
                    {'position': 1, 'id': 3},
                    {'position': 2, 'id': 2},
                    {'position': 3, 'id': 4},
                    {'position': 4, 'id': 5},
                    {'position': 5, 'id': 6},
                    {'position': 6, 'id': 7},
                    {'position': 7, 'id': 1},
                ]
            },
            {
                'set': {
                    'id': 6,
                    'position': 2
                },
                'expected': [
                    {'position': 1, 'id': 3},
                    {'position': 2, 'id': 6},
                    {'position': 3, 'id': 2},
                    {'position': 4, 'id': 4},
                    {'position': 5, 'id': 5},
                    {'position': 6, 'id': 7},
                    {'position': 7, 'id': 1},
                ]
            },
            {
                'set': {
                    'id': 3,
                    'position': 7
                },
                'expected': [
                    {'position': 1, 'id': 6},
                    {'position': 2, 'id': 2},
                    {'position': 3, 'id': 4},
                    {'position': 4, 'id': 5},
                    {'position': 5, 'id': 7},
                    {'position': 6, 'id': 1},
                    {'position': 7, 'id': 3},
                ]
            },
        ]
        for exp in experiments:
            question = SurveyQuestion.objects.get(pk=exp['set']['id'])
            question.change_position(exp['set']['position'])
            expected = exp['expected']
            response = list(SurveyQuestion.objects.all().order_by('position').values('id', 'position'))
            msg = ('элемент с id={id} должен был переместиться на позицию {position}, а остальные элементы '
                   'сместиться относительно него, уважая при этом дефолтную сортировку'.format(**exp['set']))
            try:
                self.assertEqual(response, expected, msg=msg)
            except Exception:
                raise


class TestSurveyQuestionBehavior_move_to_position_with_questions_depends_on_another_in_condition(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        # создадим опрос
        self.survey = SurveyFactory()

        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                position=1,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                position=2,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                position=3,
            ),
        ]
        # create question choices for first question
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='1')
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='2')

        # create question choices for second question
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='3')
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='4')

        # create question choices for third question
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='5')
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='6')

        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[1])
        self.node_2 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[2])

        answer_choices_content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_choices',
        )

        # create conditions for second question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            survey_question=self.questions[0],
            operator='and',
            condition='eq',
            value=self.questions[0].surveyquestionchoice_set.all()[0].id,
            content_type_attribute=answer_choices_content_type_attribute,
        )

        # create conditions for third question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_2,
            survey_question=self.questions[1],
            operator='and',
            condition='eq',
            value=self.questions[1].surveyquestionchoice_set.all()[0],
            content_type_attribute=answer_choices_content_type_attribute,
        )

    def test_move_item_on_which_depends_another_to_bad_position(self):
        for position in [2, 3]:
            try:
                self.questions[0].change_position(position)
            except RestValidationError:
                pass
            else:
                msg = ('перемещение первого вопроса на позицию {0} должно быть запрещено, т.к. второй вопрос зависит '
                       'от первого на уровне show condition'.format(position))
                self.fail(msg)

    def test_move_item_on_which_depends_another_to_good_position(self):
        self.node_1.delete()
        for position in [2, 3]:
            try:
                self.questions[0].change_position(position)
            except RestValidationError:
                msg = ('перемещение первого вопроса на позицию {0} должно быть разрешено, т.к. '
                       'никто от него не зависит '.format(position))
                self.fail(msg)

    def test_move_item_which_itself_depends_another_to_bad_position(self):
        try:
            self.questions[1].change_position(1)
        except RestValidationError:
            pass
        else:
            msg = ('перемещение второго вопроса на позицию 1 должно быть запрещено, т.к. второй вопрос зависит '
                   'от первого на уровне show condition')
            self.fail(msg)

    def test_move_item_which_itself_depends_another_to_good_position(self):
        self.node_2.delete()
        try:
            self.questions[1].change_position(3)
        except RestValidationError:
            msg = ('перемещение второго вопроса на позицию 3 должно быть разрешено, т.к. '
                   'третий вопрос не зависит от второго')
            self.fail(msg)

    def test_move_item_to_another_page_and_position_success(self):
        self.questions[2].page = 2
        self.questions[2].position = 2
        self.questions[2].save()
        self.questions[1].page = 2
        self.questions[1].position = 1
        self.questions[1].save()

        try:
            self.questions[1].change_position(2, 1)
        except RestValidationError:
            msg = ('перемещение второго вопроса на вторую позицию на первой странице '
                   'должно быть разрешено, т.к. эта страница находится раньше второй '
                   'где находится зависимый вопрос')
            self.fail(msg)

        self.questions[1].refresh_from_db()
        self.assertEqual(self.questions[1].position, 2)
        self.assertEqual(self.questions[1].page, 1)


class TestSurveyQuestionBehavior_move_group_question(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()

        self.answer_short_type = AnswerType.objects.get(slug='answer_short_text')
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            position=1,
        )
        self.question_in_group = SurveyQuestionFactory(
            survey=self.survey,
            group=self.group_question,
            answer_type=self.answer_short_type,
            position=1,
        )
        self.another_question_in_group = SurveyQuestionFactory(
            survey=self.survey,
            group=self.group_question,
            answer_type=self.answer_short_type,
            position=2,
        )
        self.not_group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_short_type,
            position=2,
        )
        self.another_not_group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_short_type,
            position=3,
        )

    def test_move_into_group_correct(self):
        self.not_group_question.change_position(2, group_id=self.group_question.pk)

        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()
        self.not_group_question.refresh_from_db()
        self.another_not_group_question.refresh_from_db()

        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.not_group_question.position, 2)
        self.assertEqual(self.another_question_in_group.position, 3)
        self.assertEqual(self.another_not_group_question.position, 2)

    def test_move_from_one_group_to_another_group_correct(self):
        self.another_group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            position=4,
        )
        self.second_group_question_in_group = SurveyQuestionFactory(
            survey=self.survey,
            group=self.another_group_question,
            answer_type=self.answer_short_type,
            position=1,
        )
        self.second_group_another_question_in_group = SurveyQuestionFactory(
            survey=self.survey,
            group=self.another_group_question,
            answer_type=self.answer_short_type,
            position=2,
        )
        self.second_group_one_another_question_in_group = SurveyQuestionFactory(
            survey=self.survey,
            group=self.another_group_question,
            answer_type=self.answer_short_type,
            position=3,
        )
        self.second_group_question_in_group.change_position(2, group_id=self.group_question.pk)

        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()

        self.not_group_question.refresh_from_db()

        self.another_not_group_question.refresh_from_db()
        self.second_group_one_another_question_in_group.refresh_from_db()
        self.second_group_another_question_in_group.refresh_from_db()
        self.second_group_question_in_group.refresh_from_db()

        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.second_group_question_in_group.position, 2)
        self.assertEqual(self.another_question_in_group.position, 3)

        self.assertEqual(self.second_group_another_question_in_group.position, 1)
        self.assertEqual(self.second_group_one_another_question_in_group.position, 2)

    def test_move_group_to_not_existed_page_correct(self):
        self.group_question.change_position(1, page=10)
        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()

        self.assertEqual(self.group_question.page, 2)
        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.page, 2)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.another_question_in_group.page, 2)
        self.assertEqual(self.another_question_in_group.position, 2)

    def test_add_question_to_group_correct(self):
        self.group_question.change_position(1, page=2)
        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()

        self.assertEqual(self.group_question.page, 2)
        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.page, 2)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.another_question_in_group.page, 2)
        self.assertEqual(self.another_question_in_group.position, 2)

        self.another_not_group_question.change_position(3, page=self.group_question.page, group_id=self.group_question.pk)

        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()
        self.another_not_group_question.refresh_from_db()

        self.assertEqual(self.group_question.page, 2)
        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.page, 2)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.another_question_in_group.page, 2)
        self.assertEqual(self.another_question_in_group.position, 2)
        self.assertEqual(self.another_not_group_question.page, 2)
        self.assertEqual(self.another_not_group_question.position, 3)

    def test_move_group_to_another_page_correct(self):
        self.group_question.change_position(1, page=2)
        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()

        self.assertEqual(self.group_question.page, 2)
        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.page, 2)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.another_question_in_group.page, 2)
        self.assertEqual(self.another_question_in_group.position, 2)

    def test_should_move_questions_inside_group_correct_when_page_change(self):
        self.another_group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            position=1,
            page=2,
        )
        self.second_question_in_group = SurveyQuestionFactory(
            survey=self.survey,
            group=self.another_group_question,
            answer_type=self.answer_short_type,
            position=1,
            page=2,
        )

        self.another_not_group_question.delete()
        self.group_question.change_position(1, page=2)
        self.not_group_question.change_position(3, page=2)

        self.another_group_question.refresh_from_db()
        self.not_group_question.refresh_from_db()
        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()
        self.second_question_in_group.refresh_from_db()

        self.assertEqual(self.group_question.page, 1)
        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.page, 1)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.another_question_in_group.page, 1)
        self.assertEqual(self.another_question_in_group.position, 2)

        self.assertEqual(self.not_group_question.page, 1)
        self.assertEqual(self.not_group_question.position, 3)

        self.assertEqual(self.another_group_question.page, 1)
        self.assertEqual(self.another_group_question.position, 2)
        self.assertEqual(self.second_question_in_group.page, 1)
        self.assertEqual(self.second_question_in_group.position, 1)

    def test_move_group_to_another_position_correct(self):
        self.group_question.change_position(2)
        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()
        self.not_group_question.refresh_from_db()

        self.assertEqual(self.group_question.position, 2)
        self.assertEqual(self.question_in_group.position, 1)
        self.assertEqual(self.another_question_in_group.position, 2)
        self.assertEqual(self.not_group_question.position, 1)

    def test_move_question_inside_group_correct(self):
        self.question_in_group.change_position(2, group_id=self.question_in_group.group_id)
        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()

        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.position, 2)
        self.assertEqual(self.another_question_in_group.position, 1)

    def test_should_move_question_in_group_with_conditions_correct(self):
        self.node = SurveyQuestionShowConditionNodeFactory(survey_question=self.another_question_in_group)
        answer_short_text_content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_short_text',
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node,
            survey_question=self.question_in_group,
            operator='and',
            condition='eq',
            value='test',
            content_type_attribute=answer_short_text_content_type_attribute,
        )
        with self.assertRaises(RestValidationError):
            self.question_in_group.change_position(2, group_id=self.question_in_group.group_id)

    def test_should_move_group_question_with_conditions_correct(self):
        self.node = SurveyQuestionShowConditionNodeFactory(survey_question=self.not_group_question)
        answer_group_content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_group',
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node,
            survey_question=self.group_question,
            operator='and',
            condition='eq',
            value='test',
            content_type_attribute=answer_group_content_type_attribute,
        )

        self.question_in_group.change_position(2, group_id=self.question_in_group.group_id)
        self.group_question.refresh_from_db()
        self.question_in_group.refresh_from_db()
        self.another_question_in_group.refresh_from_db()
        self.not_group_question.refresh_from_db()

        self.assertEqual(self.group_question.position, 1)
        self.assertEqual(self.question_in_group.position, 2)
        self.assertEqual(self.another_question_in_group.position, 1)
        self.assertEqual(self.not_group_question.position, 2)

        with self.assertRaises(RestValidationError):
            self.group_question.change_position(2)

    def test_move_group_with_question_with_condition_to_outside_group_fail(self):
        self.node = SurveyQuestionShowConditionNodeFactory(survey_question=self.not_group_question)
        answer_group_content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_group',
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node,
            survey_question=self.question_in_group,
            operator='and',
            condition='eq',
            value='test',
            content_type_attribute=answer_group_content_type_attribute,
        )
        with self.assertRaises(RestValidationError):
            self.group_question.change_position(1, page=2)


class TestSurveyQuestionsViewSet__validate(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(type='simple_form')

        text_answer_type = AnswerType.objects.get(slug='answer_short_text')
        group_answer_type = AnswerType.objects.get(slug='answer_group')

        self.text_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=text_answer_type,
            label='text1',
        )
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=group_answer_type,
            label='group1',
        )
        self.another_group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=group_answer_type,
            label='group2',
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=text_answer_type,
            group=self.group_question,
            position=1,
            label='text1 in group1',
        )
        self.another_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=text_answer_type,
            group=self.group_question,
            position=2,
            label='text2 in group1',
        )
        self.survey.sync_questions()

        self.node = SurveyQuestionShowConditionNodeFactory(survey_question=self.another_question)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

    def test_validate_correctly(self):
        condition = SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.question,
            value='test1',
        )

        url = f'/admin/api/v2/survey-questions/{self.another_question.pk}/'
        data = {
            'group_id': self.another_group_question.pk,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 400)

        self.question.group_id = self.another_group_question.pk
        self.question.save()

        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        condition.delete()
        data = {
            'group_id': self.another_group_question.pk,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.another_question.refresh_from_db()
        self.assertEqual(self.another_question.group_id, self.another_group_question.pk)

    def test_validate_correctly_with_old_params(self):
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.question,
            value='test1',
        )

        url = f'/admin/api/v2/survey-questions/{self.question.pk}/'
        data = {
            'page': self.question.page,
            'position': self.question.position,
            'group_id': self.question.group_id,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        data = {
            'label': 'testit',
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_should_move_question_into_group(self):
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.text_question,
            value='test1',
        )

        url = f'/admin/api/v2/survey-questions/{self.text_question.pk}/'
        data = {
            'group_id': self.group_question.pk,
        }
        response = self.client.patch(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.text_question.refresh_from_db()
        self.assertEqual(self.text_question.position, 1)
        self.assertEqual(self.text_question.group_id, self.group_question.pk)
