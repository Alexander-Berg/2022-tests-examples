from builtins import object, str
from collections import OrderedDict
from datetime import datetime
from types import MethodType

import pytest
from mock import MagicMock, call

from django.contrib.auth import get_user_model
from django.db import models

from kelvin import settings
from kelvin.lessons.models import Lesson, LessonProblemLink, LessonScenario
from kelvin.lessons.services import copy_lesson
from kelvin.problems.answers import Answer
from kelvin.problems.models import Problem

User = get_user_model()


class TestLesson(object):
    """
    Тесты модели занятия
    """
    def test_get_points(self, mocker):
        """
        Проверка подсчета баллов по проверенным ответам
        """
        mocked_get_points = mocker.patch.object(Answer, 'get_points')
        mocked_get_points.side_effect = [10, 20, None]

        problem_links = OrderedDict()
        problem_links['1'] = LessonProblemLink(
            options={'max_attempts': 5},
            problem=Problem(max_points=100),
        )
        problem_links['2'] = LessonProblemLink(
            options={'max_attempts': 5, 'max_points': 200},
            problem=Problem(max_points=100),
        )
        problem_links['3'] = LessonProblemLink(
            options={'max_attempts': 5, 'max_points': 300,
                     'count_type': 2},
            problem=Problem(max_points=100),
        )

        mocker.patch.object(Lesson, 'problem_links_dict', new=problem_links)
        lesson = Lesson(id=1)
        assert lesson.get_points({}) is 30, (
            u'Неправильно посчитано количество баллов')
        assert mocked_get_points.mock_calls == [
            call(None, 5, 100, 1),
            call(None, 5, 200, 1),
            call(None, 5, 300, 2),
        ]

    def test_get_max_points(self):
        """
        Проверка подсчета максимального балла за задание
        """
        lesson = Lesson(id=1)
        lesson.problem_links_dict = {
            '1': LessonProblemLink(
                id=1, problem=Problem(id=3, markup={}, max_points=100),
                options={},
            ),
            '2': LessonProblemLink(
                id=2, problem=Problem(id=4, markup={}, max_points=200),
                options={},
            ),
            '4': LessonProblemLink(
                id=4, problem=Problem(id=5, markup={}, max_points=200),
                options={'max_points': 50},
            ),
        }

        # без назначения
        assert lesson.get_max_points() == 350

        # с назначением
        assert lesson.get_max_points([2, 3]) == 200

    def test_problem_links_dict(self, mocker):
        """
        Тест получения словаря вопросов
        """
        mocked_links = mocker.patch.object(Lesson, 'lessonproblemlink_set')
        link1 = LessonProblemLink(id=1, problem_id=2)
        link2 = LessonProblemLink(id=3, problem_id=4)
        mocked_links.exclude.return_value.select_related.return_value = [
            link1, link2]
        lesson = Lesson(id=1)
        assert lesson.problem_links_dict == {
            '1': link1,
            '3': link2,
        }

    def test_check_answers(self, mocker):
        """
        Тест проверки ответов на занятие
        """
        problem1 = MagicMock()
        problem1.id = 1
        answer1 = MagicMock()
        problem2 = MagicMock()
        problem2.id = 2
        answer2 = MagicMock()
        mocked_check_answer = mocker.patch(
            'kelvin.lessons.models.check_answer'
        )

        checked_answers = Lesson.check_answers(
            {
                str(problem1.id): problem1,
                str(problem2.id): problem2,
            },
            {
                '1': [answer1],
                '2': [answer2],
            },
        )
        assert isinstance(checked_answers, dict), (
            u'После проверки должен вернуться список')
        assert len(checked_answers) == 2, (
            u'Неправильное число проверенных ответов')
        assert mocked_check_answer.mock_calls == [
            call(problem1, answer1), call(problem2, answer2)], (
            u'Должно быть 2 вызова проверки вопроса')

    unicode_data = (
        (
            Lesson(id=1),
            'Lesson 1',
        ),
        (
            Lesson(id=2, owner=User(id=3)),
            'Lesson 2',
        ),
    )

    def test_primary_scenario(self, mocker):
        """
        Тест получения основного сценария
        """
        # когда найден сценарий
        mocked_objects = mocker.patch.object(LessonScenario, 'objects')
        mocked_objects.filter.return_value = ['first', 'second']
        lesson = Lesson()
        assert lesson.primary_scenario == 'first', (
            u'Должен взяться первый сценарий')
        assert mocked_objects.mock_calls == [call.filter(lesson=lesson,
                                                         primary=True)], (
            u'Должны были отфильтровать по занятию и `primary`')

        # значение закэшировано
        mocked_objects.reset_mock()
        assert lesson.primary_scenario == 'first', (
            u'Должен взяться первый сценарий')
        assert mocked_objects.mock_calls == [], u'Не должно быть вызовов'

        # когда не найден сценарий
        mocked_objects.reset_mock()
        mocked_objects.filter.side_effect = IndexError
        lesson = Lesson()
        assert lesson.primary_scenario is None, u'Нет сценария'
        assert mocked_objects.mock_calls == [call.filter(lesson=lesson,
                                                         primary=True)], (
            u'Должны были отфильтровать по занятию и `primary`')

        # значение закэшировано
        mocked_objects.reset_mock()
        assert lesson.primary_scenario is None, u'Нет сценария'
        assert mocked_objects.mock_calls == [], u'Не должно быть вызовов'

    def test_set_primary_scenario(self):
        """Проверка, что задаем свойство главного сценария"""
        lesson = Lesson()
        lesson.set_primary_scenario('scenario')
        assert lesson._primary_scenario == 'scenario', (
            u'Должно быть заданное значение')

    def test_copy(self, mocker):
        """
        Проверяем копирование занятия
        """
        lesson = Lesson()
        mocked_methodology = mocker.patch.object(Lesson, 'methodology')
        mocked_methodology.all.return_value = [u'Метода']
        mocked_link_set = mocker.patch.object(Lesson, 'lessonproblemlink_set')
        mocked_link_set.all.return_value = [
            LessonProblemLink(id=1), LessonProblemLink(id=2)]

        def new_save(obj):
            obj.id = 5
        lesson.save = MethodType(new_save, lesson)

        mocked_objects = mocker.patch.object(LessonProblemLink, 'objects')
        # mocked_objects.bulk_create.return_value = [
        #     LessonProblemLink(id=10), LessonProblemLink(id=20)]
        (mocked_objects.filter.return_value.values_list.return_value
         .order_by.return_value) = [10, 20]
        owner = User(id=21)

        expected = {
            'new_lesson_id': 5,
            'problems': {
                1: 10,
                2: 20,
            },
        }

        # копирование без ограничения по вопросам
        assert copy_lesson(
            lesson, owner=owner, name='new name') == expected, u'Неправильное возвращаемое значение'
        assert mocked_link_set.mock_calls == [call.all()]
        assert mocked_objects.mock_calls == [
            call.bulk_create(mocked_link_set.all.return_value),
            call.filter(lesson_id=5),
            call.filter().values_list('id', flat=True),
            call.filter().values_list().order_by('id'),
        ], u'Должны создаваться связи из существующих'
        assert mocked_methodology.mock_calls == [call.all()]
        assert lesson.methodology == [u'Метода']
        assert mocked_link_set.all.return_value[0].id is None, (
            u'Идентификатор связи должен быть проставлен в `None`')
        assert mocked_link_set.all.return_value[1].id is None, (
            u'Идентификатор связи должен быть проставлен в `None`')
        assert lesson.owner is owner, (
            u'У нового занятия должен быть новый владелец')
        assert lesson.name == 'new name', u'Должно быть задано переданное имя'

        # копирование с ограничением по вопросам
        mocked_link_set.reset_mock()
        mocked_methodology.reset_mock()
        mocked_objects.reset_mock()
        mocked_link_set.filter.return_value = [
            LessonProblemLink(id=1), LessonProblemLink(id=2)]
        lesson = Lesson(name='old name')
        lesson.save = MethodType(new_save, lesson)

        assert copy_lesson(
            lesson, owner=owner, include_problem_link_ids=[6]) == expected, u'Неправильное возвращаемое значение'

        assert mocked_link_set.mock_calls == [call.filter(id__in=[6])]
        assert mocked_objects.mock_calls == [
            call.bulk_create(mocked_link_set.filter.return_value),
            call.filter(lesson_id=5),
            call.filter().values_list('id', flat=True),
            call.filter().values_list().order_by('id'),
        ], u'Должны создаваться связи из существующих'
        assert mocked_methodology.mock_calls == [call.all()]
        assert lesson.methodology == [u'Метода']
        assert mocked_link_set.all.return_value[0].id is None, (
            u'Идентификатор связи должен быть проставлен в `None`')
        assert mocked_link_set.all.return_value[1].id is None, (
            u'Идентификатор связи должен быть проставлен в `None`')
        assert lesson.owner is owner, (
            u'У нового занятия должен быть новый владелец')
        assert lesson.name == 'old name', u'Должно быть старое название'

    def test_get_homework_name(self, mocker):
        """
        Тест генерации названия домашнего задания
        """
        mocked_datetime = mocker.patch(
            'kelvin.lessons.models.timezone'
        )
        mocked_datetime.now.return_value = datetime(year=1998, month=7, day=8)
        assert Lesson.get_homework_name() == u'Домашняя работа от 08.07.1998'

    def test_journal_const(self):
        assert settings.MIN_LESSON_CSV_STUDENTS <= (
            settings.MAX_LESSON_JOURNAL_STUDENTS
        ), (
            '''
            Минимальное значение, при котором начинаем формировать CSV
            не должно быть больше максимального значения,
            при котором просто показываем статистику
            (избегаем ситуации, когда нет ни CSV, ни статистики)
            '''
        )


class TestLessonProblemLink(object):
    """
    Тесты связи занятие-вопрос
    """
    save_data = (
        (
            LessonProblemLink(type=LessonProblemLink.TYPE_COMMON,
                              problem=Problem(max_points=5),
                              options=None),
            {
                'max_attempts': LessonProblemLink.DEFAULT_MAX_ATTEMPTS,
                'max_points': 5,
                'show_tips': LessonProblemLink.DEFAULT_SHOW_TIPS,
            },
        ),
        (
            LessonProblemLink(type=LessonProblemLink.TYPE_COMMON,
                              options=None),
            {
                'max_attempts': LessonProblemLink.DEFAULT_MAX_ATTEMPTS,
                'max_points': None,
                'show_tips': LessonProblemLink.DEFAULT_SHOW_TIPS,
            },
        ),
        (
            LessonProblemLink(type=LessonProblemLink.TYPE_COMMON, options={}),
            {},
        ),
    )

    @pytest.mark.parametrize('link,expected_options', save_data)
    def test_save(self, mocker, link, expected_options):
        mocked_save = mocker.patch.object(models.Model, 'save')
        link.save()
        assert link.options == expected_options
        assert mocked_save.mock_calls == [call()]
