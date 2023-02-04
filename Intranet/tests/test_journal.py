import datetime
from builtins import object

import pytest
from mock.mock import MagicMock, Mock, call

from django.utils import timezone

from kelvin.accounts.models import User
from kelvin.courses.journal import CourseLessonResult, LessonJournal, StudentJournal
from kelvin.courses.models import CourseLessonLink
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import LessonProblemLink
from kelvin.problems.answers import Answer
from kelvin.result_stats.models import CourseLessonStat
from kelvin.results.models import AbstractLessonResult


class TestStudentJournal(object):
    """
    Тесты класса статистики результатов ученика по курсу
    """
    success_percent_data = (
        (
            [],
            None,
        ),
        (
            # так может быть, когда уже есть назначение, но нет результата
            [
                {'points': 0, 'max_points': 0},
            ],
            None,
        ),
        (
            [
                {'points': 0, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
            ],
            84,
        ),
        (
            [
                {'points': 0, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
            ],
            86,
        ),
        (
            [
                {'points': 0, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
            ],
            100,
        ),
        (
            [
                {'max_points': 0},
            ],
            None,
        ),
        (
            [
                {'points': 0, 'max_points': 1},
                {'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'points': 1, 'max_points': 1},
            ],
            86,
        ),
        (
            [
                {'points': 0, 'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'max_points': 1},
                {'points': 1, 'max_points': 1},
                {'max_points': 1},
                {'points': 1, 'max_points': 1},
            ],
            75,
        ),
    )

    @pytest.mark.django_db
    @pytest.mark.xfail(reason='Test depends on implementation due to mocks.')
    def test_data(self, mocker):
        """
        Тест формирования журнала
        """
        course = MagicMock()
        date1 = timezone.now()
        date2 = timezone.now() - datetime.timedelta(days=1)
        date3 = timezone.now()
        mocked_get_summarizable_lessonproblemlinks = mocker.patch.object(
            AbstractLessonResult, 'get_summarizable_lessonproblemlinks')
        (mocked_get_summarizable_lessonproblemlinks.return_value.select_related
         .return_value) = [
            Mock(problem_id=10, options={'max_points': 10}),
            Mock(problem_id=11, options={}, problem=Mock(max_points=20)),
        ]
        lesson1 = MagicMock()
        lesson1.name = '101'
        lesson2 = MagicMock()
        lesson2.name = '102'
        lesson4 = MagicMock()
        lesson5 = MagicMock()
        lesson5.name = '105'
        course.courselessonlink_set.filter.return_value \
            .order_by.return_value = [
                MagicMock(
                    id=2, date_assignment=date2, lesson=lesson2,
                    lesson_id=102, mode=CourseLessonLink.TRAINING_MODE
                ),
                MagicMock(
                    id=1, date_assignment=date1, lesson=lesson1,
                    lesson_id=101, mode=CourseLessonLink.TRAINING_MODE
                ),
                MagicMock(
                    id=4, date_assignment=date1, lesson=lesson4,
                    mode=CourseLessonLink.TRAINING_MODE
                ),
                MagicMock(
                    id=5, date_assignment=date3, lesson=lesson5,
                    lesson_id=105,
                    evaluation_date=(
                        timezone.now() + datetime.timedelta(days=1)
                    ),
                    mode=CourseLessonLink.CONTROL_WORK_MODE
                ),
            ]
        mocked_results = mocker.patch.object(CourseLessonResult, 'objects')
        (mocked_results.filter.return_value.select_related.return_value
         .order_by.return_value) = [
            MagicMock(summary=MagicMock(clesson_id=1), points=1, max_points=2),
        ]
        mocked_assigns = mocker.patch.object(LessonAssignment, 'objects')
        mocked_assigns.filter.return_value = [
            MagicMock(clesson_id=2),
            # TODO проверить, что `10` участвует в фильтрации задач
            MagicMock(clesson_id=3, problems=[10]),
            MagicMock(clesson_id=4, problems=[]),
        ]
        mocked_stats = mocker.patch.object(CourseLessonStat, 'objects')
        mocked_stats.filter.return_value.only.return_value = [
            MagicMock(clesson_id=2, average_points=16, average_max_points=21),
        ]
        mocked_summary = mocker.patch.object(Answer, 'get_summary')
        mocked_summary.side_effect = [
            {
                'summary': 1,
            },
            {
                'summary': 2,
            },
            {
                'summary': 3,
            },
            {
                'summary': 4,
            },
        ]

        journal = StudentJournal('111', course)
        expected = [
            {
                'date': date2.strftime('%Y-%m-%dT%H:%M:%SZ'),
                'id': 2,
                'name': '102',
                'clesson_points': 16,
                'clesson_max_points': 21,
                'mode': 1,
                'max_points': 0,
                'points': 0,
                'completed': False,
                'problems': [],
            },
            {
                'date': date1.strftime('%Y-%m-%dT%H:%M:%SZ'),
                'id': 1,
                'name': '101',
                'clesson_points': 0,
                'clesson_max_points': 0,
                'mode': 1,
                'max_points': 2,
                'points': 1,
                'completed': True,
                'problems': [
                    {
                        'summary': 1,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 10,
                    },
                    {
                        'summary': 2,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                ],
            },
            {
                'date': date3.strftime('%Y-%m-%dT%H:%M:%SZ'),
                'id': 5,
                'name': '105',
                'clesson_max_points': 0,
                'mode': 2,
                'max_points': 0,
                'completed': False,
                'problems': [
                    {
                        'summary': 3,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 10,
                    },
                    {
                        'summary': 4,
                        'type': LessonProblemLink.TYPE_COMMON,
                        'max_points': 20,
                    },
                ],
            },
        ]
        assert journal.data == expected
        assert mocked_assigns.mock_calls == [
            call.filter(clesson__in=[2, 1, 4, 5], student='111'),
        ]
        assert mocked_results.mock_calls == [
            call.filter(
                date_created__gte=(
                    mocked_results.mock_calls[0][2]['date_created__gte']),
                summary__clesson__in=[2, 1, 4, 5],
                summary__student='111',
                work_out=False,
            ),
            call.filter().select_related('summary'),
            call.filter().select_related().order_by('date_updated'),
        ]
        assert mocked_stats.mock_calls == [
            call.filter(clesson__in=[2, 1, 4, 5]),
            call.filter().only('clesson_id', 'average_points',
                               'average_max_points'),
        ]
        assert mocked_get_summarizable_lessonproblemlinks.mock_calls == [
            call(102),
            call().select_related('problem'),
            call(101),
            call().select_related('problem'),
            call(105),
            call().select_related('problem'),
        ]

        # если уже посчитали, не считаем
        journal._data = '1'
        assert journal.data == '1'


class TestLessonJournal(object):
    """
    Тесты журнала занятия
    """
    @pytest.mark.django_db
    def test_data(self, mocker):
        """
        Тест `LessonJournal.data`
        """
        clesson = MagicMock(course_id=1)

        # уже есть данные
        journal = LessonJournal(clesson)

        mocked_students = mocker.patch.object(User, 'objects')
        mocked_students.filter.return_value = [
            MagicMock(id=10),
            MagicMock(id=11),
            MagicMock(id=12),
        ]

        journal.students = {x.id: x for x in mocked_students}

        journal._data = 'data'
        assert journal.data == 'data'

        clesson.lesson_id = 1234
        link1 = MagicMock(id=1)
        link2 = MagicMock(id=2)
        link3 = MagicMock(id=3)
        mocked_get_summarizable_lessonproblemlinks = mocker.patch.object(
            AbstractLessonResult, 'get_summarizable_lessonproblemlinks')
        mocked_get_summarizable_lessonproblemlinks.return_value = [
            link1, link2, link3]
        mocked_result_objects = mocker.patch.object(CourseLessonResult,
                                                    'objects')
        result10 = MagicMock(summary=MagicMock(student_id=10))
        result11 = MagicMock(summary=MagicMock(student_id=11))
        (mocked_result_objects.filter.return_value.select_related.return_value
         .order_by.return_value) = [result10, result11]
        mocked_assignment_objects = mocker.patch.object(LessonAssignment,
                                                        'objects')
        assignment11 = MagicMock(student_id=11, problems=[1, 2])
        assignment12 = MagicMock(student_id=12, problems=[])
        mocked_assignment_objects.filter.return_value = [
            assignment11, assignment12,
        ]
        mocked_get_summary = mocker.patch.object(AbstractLessonResult,
                                                 'get_summary')
        mocked_get_summary.return_value = 1000

        expected = {10: 1000, 11: 1000, 12: 1000}
        journal = LessonJournal(clesson)
        assert journal.data == expected
        assert mocked_assignment_objects.mock_calls == [
            call.filter(clesson=clesson)], (
            u'Должны получить назначения по занятию')
        assert mocked_get_summarizable_lessonproblemlinks.mock_calls == [
            call(1234),
        ]
        assert mocked_result_objects.mock_calls == [
            call.filter(summary__clesson=clesson,
                        summary__student__isnull=False,
                        date_created__gte=clesson.date_assignment,
                        work_out=False),
            call.filter().select_related('summary'),
            call.filter().select_related('summary').order_by(
                'date_updated', 'id'),
        ], (u'Должны получить только попытки "во время урока", '
            u'которые отсортированы по времени изменения')
        assert mocked_get_summary.mock_calls == [
            call([link1, link2, link3], result10, lesson_scenario=clesson),
            call([link1, link2], result11, lesson_scenario=clesson),
            call([], None, lesson_scenario=clesson),
        ], u'Неправильные вызовы сводки по ученику'
