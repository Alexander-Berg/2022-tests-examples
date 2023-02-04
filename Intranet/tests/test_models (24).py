from builtins import object
from datetime import datetime, timedelta

import pytest
from mock.mock import MagicMock, call

from django.db import connection
from django.utils import timezone

import swissknife.assertions
from kelvin.accounts.models import User
from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.answers import Answer
from kelvin.results.models import AbstractLessonResult, CourseLessonResult, CourseLessonSummary, backup_clesson_results
from kelvin.subjects.models import Subject


class TestAbstractLessonResult(object):
    """
    Тесты модели результатов на занятие
    """
    get_summarizable_lessonproblemlinks_data = (
        (
            (1,),
            [
                call.filter(lesson_id=1),
                call.filter().exclude(type=LessonProblemLink.TYPE_THEORY),
            ],
            False,
        ),
        (
            (2, None),
            [
                call.filter(lesson_id=2),
                call.filter().exclude(type=LessonProblemLink.TYPE_THEORY),
            ],
            False,
        ),
        (
            (3, []),
            [
                call.filter(lesson_id=3),
                call.filter().exclude(type=LessonProblemLink.TYPE_THEORY),
                call.filter().exclude().filter(id__in=[]),
            ],
            True,
        ),
        (
            (4, [11, 22]),
            [
                call.filter(lesson_id=4),
                call.filter().exclude(type=LessonProblemLink.TYPE_THEORY),
                call.filter().exclude().filter(id__in=[11, 22]),
            ],
            True,
        ),
    )

    @pytest.mark.parametrize('args,calls,filter_on_assignments',
                             get_summarizable_lessonproblemlinks_data)
    def test_get_summarizable_lessonproblemlinks(self, mocker, args, calls,
                                                 filter_on_assignments):
        """
        Тесты получения списка связей занятие-задача, по которым делается
        сводка
        """
        mocked_lessonproblemlink_manager = mocker.patch.object(
            LessonProblemLink, 'objects')
        result = AbstractLessonResult.get_summarizable_lessonproblemlinks(
            *args)
        assert mocked_lessonproblemlink_manager.mock_calls == calls, (
            u'Неправильный запрос')
        if filter_on_assignments:
            assert result == (
                mocked_lessonproblemlink_manager.filter.return_value.exclude
                .return_value.filter.return_value), (
                u'Нужно получить все связи, которые не являются теорией '
                u'и входят в назначения')
        else:
            assert result == (
                mocked_lessonproblemlink_manager.filter.return_value.exclude
                .return_value), (
                u'Нужно получить все связи, которые не являются теорией')

    get_summary_data = (
        (
            [
                LessonProblemLink(
                    id=1,
                    options={
                        'max_attempts': 3,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
                LessonProblemLink(
                    id=2,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
                LessonProblemLink(
                    id=3,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
            ],
            None,
            False,
            None,
            {
                'points': 0,
                'max_points': 0,
                'completed': False,
                'mode': None,
                'problems': {
                    1: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    2: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    3: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                },
            },
        ),
        (
            [
                LessonProblemLink(
                    id=1,
                    options={
                        'max_attempts': 3,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
                LessonProblemLink(
                    id=2,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20
                    },
                ),
                LessonProblemLink(
                    id=3,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
            ],
            None,
            True,
            None,
            {
                'points': 0,
                'max_points': 0,
                'completed': False,
                'mode': None,
                'problems': [
                    {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                ],
            },
        ),
        (
            [
                LessonProblemLink(
                    id=1,
                    options={
                        'max_attempts': 3,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
                LessonProblemLink(
                    id=2,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20
                    },
                ),
                LessonProblemLink(
                    id=3,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
            ],
            None,
            False,
            CourseLessonLink(
                mode=CourseLessonLink.CONTROL_WORK_MODE,
                evaluation_date=timezone.now() - timedelta(days=1),

            ),
            {
                'points': 0,
                'max_points': 0,
                'completed': False,
                'mode': CourseLessonLink.CONTROL_WORK_MODE,
                'problems': {
                    1: {
                        'answered': True,
                        'status': Answer.SUMMARY_INCORRECT,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    2: {
                        'answered': True,
                        'status': Answer.SUMMARY_INCORRECT,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    3: {
                        'answered': True,
                        'status': Answer.SUMMARY_INCORRECT,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                },
            },
        ),
        (
            [
                LessonProblemLink(
                    id=1,
                    options={
                        'max_attempts': 3,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
                LessonProblemLink(
                    id=2,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20
                    },
                ),
                LessonProblemLink(
                    id=3,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
            ],
            None,
            False,
            CourseLessonLink(
                mode=CourseLessonLink.DIAGNOSTICS_MODE,
                evaluation_date=timezone.now() - timedelta(days=1),

            ),
            {
                'points': 0,
                'max_points': 0,
                'completed': False,
                'mode': CourseLessonLink.DIAGNOSTICS_MODE,
                'problems': {
                    1: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    2: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    3: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                },
            },
        ),
        (
            [
                LessonProblemLink(
                    id=1,
                    options={
                        'max_attempts': 3,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
                LessonProblemLink(
                    id=2,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20
                    },
                ),
                LessonProblemLink(
                    id=3,
                    options={
                        'max_attempts': 1,
                        'show_tips': True,
                        'max_points': 20,
                    },
                ),
            ],
            None,
            False,
            CourseLessonLink(
                mode=CourseLessonLink.DIAGNOSTICS_MODE,
                evaluation_date=timezone.now() + timedelta(days=1),
            ),
            {
                'points': 0,
                'max_points': 0,
                'completed': False,
                'mode': CourseLessonLink.DIAGNOSTICS_MODE,
                'problems': {
                    1: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    2: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                    3: {
                        'answered': False,
                        'status': None,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                },
            },
        ),
    )

    @pytest.mark.parametrize(
        'assigned_problem_links,result,to_list,scenario,expected',
        get_summary_data,
    )
    def test_get_summary(self, assigned_problem_links, result, to_list,
                         scenario, expected):
        """
        Тест сводки результатов
        """
        assert AbstractLessonResult.get_summary(
            assigned_problem_links, result, format_as_list=to_list,
            lesson_scenario=scenario
        ) == expected

    get_correct_count_data = (
        (
            {
                'problems': {
                    '1': {
                        'status': None,
                        'answered': False,
                    },
                    '2': {
                        'status': Answer.SUMMARY_CORRECT,
                        'answered': True,
                    },
                    '3': {
                        'status': Answer.SUMMARY_CORRECT,
                        'answered': True,
                    },
                    '4': {
                        'status': Answer.SUMMARY_CORRECT,
                        'answered': True,
                    },
                    '5': {
                        'status': Answer.SUMMARY_INCORRECT,
                        'answered': True,
                    },
                    '6': {
                        'status': None,
                        'answered': False,
                    },
                    '7': {
                        'status': Answer.SUMMARY_CORRECT,
                        'answered': True,
                    }
                }
            },
            {'correct': 4, 'incorrect': 1, 'unanswered': 2},
        ),
    )

    @pytest.mark.parametrize('summary,expected',
                             get_correct_count_data)
    def test_get_count_methods(self, mocker, summary, expected):
        """
        Тест подсчета числа правильных, неправильных, пропущенных ответов
        """
        result = AbstractLessonResult()
        mocked_get_summary = mocker.patch.object(result, 'get_summary')
        mocked_get_summary.return_value = summary
        assert result.get_correct_count(123) == expected['correct'], (
            u'Неправильно посчитано число правильно решенных задач')
        assert result.get_incorrect_count(123) == expected['incorrect'], (
            u'Неправильно посчитано число неверно решенных задач')
        assert result.get_unanswered_count(123) == expected['unanswered'], (
            u'Неправильно посчитано число задач без ответа')


class TestCourseLessonResult(object):
    """
    Тесты модели результатов на занятие в курсе
    """

    quiz_time_limit_data = (
        (
            datetime(year=1970, month=1, day=1, hour=1, minute=1, second=1),
            MagicMock(
                duration=45,
                finish_date=datetime(year=1970, month=1, day=1, hour=1,
                                     minute=46, second=2)
            ),
            datetime(year=1970, month=1, day=1, hour=1, minute=46, second=1),
        ),
        (
            datetime(year=1970, month=1, day=1, hour=1, minute=1, second=1),
            MagicMock(
                duration=45,
                finish_date=datetime(year=1970, month=1, day=1, hour=1,
                                     minute=45, second=2)
            ),
            datetime(year=1970, month=1, day=1, hour=1, minute=45, second=2),
        ),
    )

    @pytest.mark.parametrize('date_created,scenario,expected',
                             quiz_time_limit_data)
    def test_quiz_time_limit(self, date_created, scenario, expected):
        """
        Тест подсчета лимита времени
        """
        assert CourseLessonResult(
            date_created=date_created).quiz_time_limit(scenario) == expected, (
            u'Неправильно подсчитано время завершения контрольной')

    @pytest.mark.django_db
    def test_get_problem_stats(self):
        """
        Проверяем возврат метода модели get_problem_stats
        :return: должны вернуть мапу из problem_id в словарь,
        в котором есть поле "result" : "ok|fail"
        "ok" - если не было ошибок (self.answers[<problem_id>]['mistakes']) в ответах
        "failed" - если была хотя бы одна ошибка
        """
        student = User.objects.create(username="blablabla")
        clr = CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                student=student,
                clesson=CourseLessonLink.objects.create(
                    course=Course.objects.create(
                        name="blablabla",
                        owner=student,
                        subject=Subject.objects.create(
                            name="blablabla",
                        )
                    ),
                    lesson=Lesson.objects.create(
                        name="blablabla",
                        owner=student,
                    ),
                )
            ),
            max_points=1,
            answers={
                '1': [{
                    'completed': True,
                    'mistakes': 0
                }],
                '2': [
                    {
                        'completed': False,
                        'mistakes': 0,

                    },
                    {
                        'completed': True,
                        'mistakes': 1,
                    }
                ],
            }
        )

        assert(clr.get_problem_stats() == {
            1: {
                "result": "ok",
                "id": 1,
                "order": 0,
            },
            2: {
                "result": "fail",
                "id": 2,
                "order": 0,
            }
        }), "Illegal problems stats"

    @pytest.mark.django_db
    def test_backup_clesson_results(self):
        owner = User.objects.create(username='owner')
        subject = Subject.objects.create(name='subject')
        course = Course.objects.create(name='course', owner=owner, subject=subject)
        lesson = Lesson.objects.create(name='lesson', owner=owner)
        clesson = CourseLessonLink.objects.create(course=course, lesson=lesson)

        student = User.objects.create(username='student-one')
        summary = CourseLessonSummary.objects.create(clesson=clesson, student=student)

        clr_one = CourseLessonResult.objects.create(max_points=5, answers={}, summary=summary)
        clr_two = CourseLessonResult.objects.create(max_points=5, answers={}, summary=summary)
        results_ids = [clr_one.id, clr_two.id]

        results = CourseLessonResult.objects.filter(id__in=results_ids)
        assert results.count() == 2

        backup_clesson_results(results_ids)

        results = CourseLessonResult.objects.filter(id__in=results_ids)
        assert results.count() == 0

        with connection.cursor() as cursor:
            cursor.execute("SELECT count(*) from results_courselessonresult_backup;")
            backup_results_count = cursor.fetchone()[0]
            assert backup_results_count == 2
