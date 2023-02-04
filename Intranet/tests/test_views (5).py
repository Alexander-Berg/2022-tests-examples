from builtins import object, range, zip
from datetime import datetime

import pytest
from mock import MagicMock, Mock, call

from django.contrib.auth import get_user_model
from django.contrib.auth.models import AnonymousUser
from django.db import transaction
from django.http.response import Http404
from django.utils import timezone
from django.utils.timezone import utc

from rest_framework.exceptions import NotFound, PermissionDenied, ValidationError
from rest_framework.response import Response
from rest_framework.viewsets import ModelViewSet

from kelvin import settings
from kelvin.common.utils_for_tests import MockedQOr
from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.courses.serializers import CourseLessonLinkSerializer, CourseSerializer, CourseShortSerializer
from kelvin.courses.views import CLessonViewSet, CourseViewSet
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import LessonProblemLink
from kelvin.result_stats.models import CourseLessonStat, StudentCourseStat
from kelvin.results.models import CourseLessonResult, CourseLessonSummary
from kelvin.results.serializers import CourseLessonResultSerializer

User = get_user_model()


@pytest.mark.skip('IN PLATO')
class TestCourseViewSet(object):
    """
    Тест для класса `CourseViewSet`
    """
    def test_perform_create(self, mocker):
        """
        Проверка, что метод `perform_create` вызывается и проставляет
        владельца курса
        """
        serializer_save = mocker.patch.object(CourseSerializer, 'save')
        serializer_save.return_value = True
        serializer = CourseSerializer()

        request = MagicMock()
        user = User(id=1)
        request.user = user

        view = CourseViewSet()
        view.request = request
        view.perform_create(serializer)
        assert serializer_save.mock_calls == [
            call(owner=user)], u'Save was called with wrong parameters'

    def test_get_serializer_context(self):
        """
        Проверяем, что в контекст добавляется параметры из запроса
        """
        query_params = {
            'expand_lessons': 'True',
            'with_students': 'True',
            'clessons_ordering': 'raw',
        }
        request = MagicMock()
        request.query_params.get = Mock(
            side_effect=lambda param: query_params[param])
        request.data = {}
        request.user = User()
        viewset = CourseViewSet()
        viewset.request = request
        viewset.format_kwarg = None
        context = viewset.get_serializer_context()
        assert set(context.keys()) == {'expand_lessons', 'with_students',
                                       'view', 'request', 'format',
                                       'for_student', 'clessons_ordering'}, (
            u'Неправильные ключи в контексте сериализатора')
        assert context['clessons_ordering'] == 'raw', u'Неправильный параметр'
        assert context['expand_lessons'] == 'True', u'Неправильный параметр'
        assert context['with_students'] == 'True', u'Неправильный параметр'
        assert request.mock_calls == [
            call.query_params.get('clessons_ordering'),
            call.query_params.get('expand_lessons'),
            call.query_params.get('with_students')], (
            u'Параметр должен браться из запроса')

    get_queryset_data = [
        (
            'list',
            False,
            False,
            [
                call.all(),
                call.all().filter(MockedQOr(owner=1, free=True)),
                call.all().filter().select_related(
                    'cover',
                    'subject',
                    'mailing_list',
                    'progress_indicator',
                ),
                call.all().filter().select_related().prefetch_related(
                    'courselessonlink_set',
                    'courselessonlink_set__lesson',
                    'courselessonlink_set__progress_indicator'
                ),
                call.all().filter().select_related().prefetch_related()
                    .annotate(lessons_count='mocked_count'),
            ],
            [
                call('courselessonlink'),
            ]
        ),
        (
            'list',
            True,
            False,
            [
                call.all(),
                call.all().filter(MockedQOr(owner=1, free=True)),
                call.all().filter().select_related(
                    'cover',
                    'subject',
                    'mailing_list',
                    'progress_indicator',
                ),
                call.all().filter().select_related().prefetch_related(
                    'courselessonlink_set',
                    'courselessonlink_set__lesson',
                    'courselessonlink_set__progress_indicator',
                ),
                call.all().filter().select_related().prefetch_related()
                    .prefetch_related('students'),
                call.all().filter().select_related().prefetch_related()
                    .prefetch_related().annotate(lessons_count='mocked_count'),
            ],
            [
                call('courselessonlink'),
            ]
        ),
        (
            'retrieve',
            False,
            False,
            [
                call.all(),
                call.all().select_related(
                    'cover',
                    'subject',
                    'mailing_list',
                    'progress_indicator'
                ),
                call.all().select_related().prefetch_related(
                    'courselessonlink_set',
                    'courselessonlink_set__lesson',
                    'courselessonlink_set__progress_indicator'
                ),
            ],
            [],
        ),
        (
            'retrieve',
            True,
            True,
            [
                call.all(),
                call.all()
                    .select_related(
                    'cover',
                    'subject',
                    'mailing_list',
                    'progress_indicator'
                ),
                call.all()
                    .select_related()
                    .prefetch_related(
                        'courselessonlink_set',
                        'courselessonlink_set__lesson',
                        'courselessonlink_set__progress_indicator',
                ),
                call.all()
                    .select_related()
                    .prefetch_related()
                    .prefetch_related(
                        'courselessonlink_set__lesson__lessonproblemlink_set',
                        'courselessonlink_set__lesson__theme'
                ),
                call.all()
                    .select_related()
                    .prefetch_related()
                    .prefetch_related()
                    .prefetch_related('students'),
            ],
            [],
        ),
        (
            'stats',
            False,
            False,
            [
                call.all(),
                call.all().select_related(
                    'cover',
                    'subject',
                    'mailing_list',
                    'progress_indicator'
                ),
                call.all().select_related().prefetch_related(
                    'courselessonlink_set',
                    'courselessonlink_set__lesson',
                    'courselessonlink_set__progress_indicator',
                ),
                call.all()
                    .select_related()
                    .prefetch_related()
                    .prefetch_related(
                        'courselessonlink_set__courselessonstat',
                    ),                                            # noqa
            ],
            [],
        ),
        (
            'student_stats',
            False,
            False,
            [
                call.all(),
                call.all().select_related(
                    'cover',
                    'subject',
                    'mailing_list',
                    'progress_indicator'
                ),
                call.all().select_related().prefetch_related(
                    'courselessonlink_set',
                    'courselessonlink_set__lesson',
                    'courselessonlink_set__progress_indicator',
                ),
                (call.all().select_related().prefetch_related()
                 .prefetch_related(
                    'courselessonlink_set__lesson__lessonproblemlink_set',
                    'courselessonlink_set__lesson__theme',
                )),
                (call.all().select_related().prefetch_related()
                 .prefetch_related().select_related('owner')),
            ],
            [],
        ),
        (
            'journal',
            False,
            False,
            [
                call.all(),
                call.all().select_related(
                    'cover', 'subject', 'mailing_list', 'progress_indicator'
                ),
                call.all().select_related().prefetch_related(
                    'courselessonlink_set',
                    'courselessonlink_set__lesson',
                    'courselessonlink_set__progress_indicator',
                ),
            ],
            [],
        ),
        (
            'find_problems',
            False,
            False,
            [
                call.all(),
                call.all().select_related(
                    'cover', 'subject', 'mailing_list', 'progress_indicator'
                ),
                call.all().select_related().prefetch_related(
                    'courselessonlink_set',
                    'courselessonlink_set__lesson',
                    'courselessonlink_set__progress_indicator',
                ),
                (call.all().select_related().prefetch_related()
                    .prefetch_related(
                    'courselessonlink_set__lesson__lessonproblemlink_set',
                )),
            ],
            [],
        ),
        (
            'diagnostics',
            False,
            False,
            [
                call.all(),
            ],
            [],
        ),
    ]

    @pytest.mark.parametrize(
        'action,with_students,expand_lessons,calls,count_calls',
        get_queryset_data,
    )
    def test_get_queryset(self, action, with_students, expand_lessons, calls,
                          count_calls, mocker):
        """
        Тест метода `get_queryset`
        """
        mocked_objects = mocker.patch.object(Course, 'objects')
        mocked_count = mocker.patch('kelvin.courses.views.Count')
        mocked_count.return_value = 'mocked_count'
        request = MagicMock()
        request.query_params = {}
        request.user = 1
        if with_students:
            request.query_params['with_students'] = True
        if expand_lessons:
            request.query_params['expand_lessons'] = True

        view = CourseViewSet()
        view.request = request
        view.action = action
        view.get_queryset()
        assert mocked_objects.mock_calls == calls
        assert mocked_count.mock_calls == count_calls

    @pytest.mark.parametrize('action,serializer_cls', [
        ('list', CourseShortSerializer),
        ('assigned', CourseShortSerializer),
        ('retrieve', CourseSerializer),
    ])
    def test_get_serializer_class(self, action, serializer_cls):
        """
        Тест метода `get_serializer_class`
        """
        viewset = CourseViewSet()
        viewset.action = action
        assert viewset.get_serializer_class() == serializer_cls, (
            u'Использован неправильный сериализатор')

    def test_retrieve(self, mocker):
        """
        Тест получения курса
        """
        viewset = CourseViewSet()
        request = MagicMock()
        request.user = MagicMock()
        viewset.request = request

        mocked_get_object = mocker.patch.object(CourseViewSet, 'get_object')
        course = MagicMock()
        mocked_get_object.return_value = course
        mocked_get_serializer = mocker.patch.object(CourseViewSet,
                                                    'get_serializer')
        serializer = MagicMock()
        serializer.data = {
            'id': 1,
            'lessons': [
                {'id': 2},
                {'id': 3},
                {'id': 5},
            ],
        }
        mocked_get_serializer.return_value = serializer
        mocked_assignments = mocker.patch.object(LessonAssignment, 'objects')
        mocked_assignments.filter.return_value.values_list.return_value = [3]
        mocked_course_student = mocker.patch.object(CourseStudent, 'objects')
        mocked_course_student.filter.return_value.exists.return_value = True

        response = viewset.retrieve(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'lessons': [
                {'id': 2},
                {'id': 5},
            ],
        }
        assert mocked_get_object.mock_calls == [
            call(),
            call().courselessonlink_set.all(),
        ]
        assert mocked_get_serializer.mock_calls == [call(course)]
        assert mocked_assignments.mock_calls == [
            call.filter(clesson__in=course.courselessonlink_set.all(),
                        problems__startswith=[],
                        student=request.user),
            call.filter().values_list('clesson_id', flat=True),
        ]
        assert mocked_course_student.mock_calls == [
            call.filter(course=course, student=request.user),
            call.filter().exists(),
        ]

    def test_web(self, mocker):
        """
        Тест получения курса вебом
        """
        viewset = CourseViewSet()
        request = MagicMock()
        request.user = MagicMock()
        viewset.request = request

        class MockedNow(str):
            def strftime(self, format):
                return self

        class MockedTimedelta(object):
            def __init__(self, value):
                self.value = value

            def __radd__(self, other):
                return self

            def strftime(self, format):
                return self.value

        mocked_now = mocker.patch('django.utils.timezone.now')
        mocked_now.return_value = MockedNow('<now>')
        mocked_timedelta = mocker.patch('kelvin.courses.views.timedelta')
        # время после `<now>`
        mocked_timedelta.return_value = MockedTimedelta('<x>')

        mocked_get_object = mocker.patch.object(CourseViewSet, 'get_object')
        course = MagicMock(id=7)
        links = [
            MagicMock(id=2, mode=2, start_date=None, lesson_id=2),
            MagicMock(id=3, mode=2, start_date=None, lesson_id=3),
            MagicMock(id=5, mode=1, start_date=None, lesson_id=5),
            # `<now>` между `<a>` и `<x>`
            MagicMock(id=6, mode=2, start_date='<a>', lesson_id=6),
        ]
        course.courselessonlink_set.all.return_value = links
        mocked_get_object.return_value = course
        mocked_get_serializer = mocker.patch.object(CourseViewSet,
                                                    'get_serializer')
        serializer = MagicMock()
        serializer.data = {
            'id': 1,
            'lessons': [
                {
                    'id': 2,
                    'date_assignment': 'after now',
                    'accessible_to_teacher': None,
                    'start_date': None,
                    'mode': 1,
                },
                {
                    'id': 3,
                    'date_assignment': 'after now',
                    'accessible_to_teacher': 'after now',
                    'start_date': None,
                    'mode': 1,
                },
                {
                    'id': 5,
                    'date_assignment': ' before now',
                    'accessible_to_teacher': ' before now',
                    'start_date': None,
                    'mode': 1,
                },
                {
                    'id': 6,
                    'date_assignment': None,
                    'accessible_to_teacher': ' before now',
                    'start_date': '<a>',
                    'duration': 10,
                    'mode': 1,
                },
            ],
        }
        mocked_get_serializer.return_value = serializer
        mocked_assignments = mocker.patch.object(LessonAssignment, 'objects')
        mocked_assignments.filter.return_value = [
            MagicMock(clesson_id=2, problems=[2]),
            MagicMock(clesson_id=3, problems=[]),
            MagicMock(clesson_id=5, problems=[5]),
            MagicMock(clesson_id=6, problems=[6]),
        ]
        mocked_course_student = mocker.patch.object(CourseStudent, 'objects')
        mocked_course_student.filter.return_value.exists.return_value = True

        mocked_clesson_serializer = mocker.patch(
            'kelvin.courses.views.CourseLessonLinkSerializer')
        mocked_clesson_serializer.control_work_data = (
            lambda clesson, result, now: {'control_work': True} if clesson
            else {}
        )

        mocked_result_manager = mocker.patch.object(CourseLessonResult,
                                                    'objects')
        (mocked_result_manager.filter.return_value.order_by.return_value
         .distinct.return_value.select_related.return_value) = (
            MagicMock(
                summary=MagicMock(clesson_id=3),
                get_incorrect_count=lambda *args, **kwargs: 0,
                get_correct_count=lambda *args, **kwargs: 0,
            ),
            MagicMock(
                summary=MagicMock(clesson_id=6),
                get_incorrect_count=lambda *args, **kwargs: 0,
                get_correct_count=lambda *args, **kwargs: 0,
            ),
        )

        mocked_lesson_problem_links = mocker.patch.object(LessonProblemLink,
                                                          'objects')
        (mocked_lesson_problem_links.filter.return_value
         .order_by.return_value) = ()

        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'lessons': [
                {
                    'id': 2,
                    'control_work': True,
                    'date_assignment': 'after now',
                    'date_assignment_passed': False,
                    'accessible_to_teacher': False,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
                {
                    'id': 5,
                    'date_assignment': ' before now',
                    'date_assignment_passed': True,
                    'accessible_to_teacher': True,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
                {
                    'id': 6,
                    'control_work': True,
                    'date_assignment': None,
                    'date_assignment_passed': False,
                    'accessible_to_teacher': True,
                    'start_date': '<a>',
                    'duration': 10,
                    'on_air': True,
                    'mode': 1,
                    'results': {
                        'wrong': 0,
                        'right': 0,
                        'all': 0,
                    },
                },
            ],
        }, u'Неправильные данные ответа'
        assert mocked_get_object.mock_calls == [
            call(),
            call().courselessonlink_set.all(),
        ], u'Нужно получить все курсозанятия'
        assert mocked_get_serializer.mock_calls == [call(course)], (
            u'Нужно сериализовать курс')
        assert mocked_assignments.mock_calls == [
            call.filter(clesson__in=course.courselessonlink_set.all(),
                        student=request.user),
        ], u'Нужно получить назначения занятий'
        assert mocked_course_student.mock_calls == [
            call.filter(course=7, student=request.user),
            call.filter().exists(),
        ]
        assert mocked_result_manager.mock_calls == [
            call.filter(
                summary__clesson__in=links,
                summary__student=request.user,
                work_out=False
            ),
            call.filter().order_by('summary', '-date_updated'),
            call.filter().order_by().distinct('summary'),
            call.filter().order_by().distinct().select_related('summary'),
        ], u'Нужно получить результаты по курсозанятиям-контрольным'
        # Last call is iteration which could not be checked easy.
        assert (mocked_lesson_problem_links.mock_calls[:-1] == [
            call.filter(lesson_id__in=[2, 3, 5, 6], problem__isnull=False),
            call.filter().prefetch_related('problem'),
            call.filter().prefetch_related().order_by('lesson'),
        ]), u'Нужно получить список всех задач'

        # Случай, когда пользователь не в курсе
        mocked_get_object.reset_mock()
        mocked_get_serializer.reset_mock()
        mocked_assignments.reset_mock()
        mocked_course_student.reset_mock()
        mocked_result_manager.reset_mock()

        mocked_course_student.filter.return_value.exists.return_value = False
        serializer.data = {
            'id': 1,
            'lessons': [
                {
                    'id': 2,
                    'date_assignment': 'after now',
                    'accessible_to_teacher': None,
                    'start_date': None,
                    'mode': 1,
                },
                {
                    'id': 3,
                    'date_assignment': 'after now',
                    'accessible_to_teacher': 'after now',
                    'start_date': None,
                    'mode': 1,
                },
                {
                    'id': 5,
                    'date_assignment': ' before now',
                    'accessible_to_teacher': ' before now',
                    'start_date': None,
                    'mode': 1,
                },
                {
                    'id': 6,
                    'date_assignment': None,
                    'accessible_to_teacher': ' before now',
                    'start_date': None,
                    'mode': 1,
                },
            ],
        }
        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'lessons': [
                {
                    'id': 2,
                    'control_work': True,
                    'date_assignment': 'after now',
                    'date_assignment_passed': False,
                    'accessible_to_teacher': False,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
                {
                    'id': 3,
                    'control_work': True,
                    'date_assignment': 'after now',
                    'date_assignment_passed': False,
                    'accessible_to_teacher': False,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': {
                        'wrong': 0,
                        'right': 0,
                        'all': 0,
                    },
                },
                {
                    'id': 5,
                    'date_assignment': ' before now',
                    'date_assignment_passed': True,
                    'accessible_to_teacher': True,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
                {
                    'id': 6,
                    'control_work': True,
                    'date_assignment': None,
                    'date_assignment_passed': False,
                    'accessible_to_teacher': True,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': {
                        'wrong': 0,
                        'right': 0,
                        'all': 0,
                    },
                },
            ],
        }, u'Неправильные данные ответа'
        assert mocked_get_object.mock_calls == [
            call(),
            call().courselessonlink_set.all(),
        ], u'Нужно получить все курсозанятия'
        assert mocked_get_serializer.mock_calls == [call(course)], (
            u'Нужно сериализовать курс')
        assert mocked_assignments.mock_calls == [
            call.filter(clesson__in=course.courselessonlink_set.all(),
                        student=request.user),
        ], u'Нужно получать назначения всех занятий'
        assert mocked_course_student.mock_calls == [
            call.filter(course=7, student=request.user),
            call.filter().exists(),
        ]
        assert mocked_result_manager.mock_calls == [
            call.filter(
                summary__clesson__in=links,
                summary__student=request.user,
                work_out=False
            ),
            call.filter().order_by('summary', '-date_updated'),
            call.filter().order_by().distinct('summary'),
            call.filter().order_by().distinct().select_related('summary'),
        ], u'Нужно получить результаты по курсозанятиям-контрольным'

        # неавторизованный пользователь
        mocked_get_object.reset_mock()
        mocked_get_serializer.reset_mock()
        mocked_assignments.reset_mock()
        mocked_course_student.reset_mock()
        mocked_result_manager.reset_mock()
        viewset.request.user.is_authenticated = False
        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'lessons': [
                {
                    'id': 2,
                    'control_work': True,
                    'date_assignment': 'after now',
                    'date_assignment_passed': False,
                    'accessible_to_teacher': False,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
                {
                    'id': 3,
                    'control_work': True,
                    'date_assignment': 'after now',
                    'date_assignment_passed': False,
                    'accessible_to_teacher': False,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
                {
                    'id': 5,
                    'date_assignment': ' before now',
                    'date_assignment_passed': True,
                    'accessible_to_teacher': True,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
                {
                    'id': 6,
                    'control_work': True,
                    'date_assignment': None,
                    'date_assignment_passed': False,
                    'accessible_to_teacher': True,
                    'start_date': None,
                    'on_air': None,
                    'mode': 1,
                    'results': None,
                },
            ],
        }, u'Неправильные данные ответа'
        assert mocked_get_object.mock_calls == [
            call(),
            call().courselessonlink_set.all(),
        ], u'Нужно получить все курсозанятия'
        assert mocked_get_serializer.mock_calls == [call(course)], (
            u'Нужно сериализовать курс')
        assert mocked_assignments.mock_calls == [], (
            u'Не нужно получать назначения, если ученик не состоит в курсе')
        assert mocked_course_student.mock_calls == []
        assert mocked_result_manager.mock_calls == []

    def test_assigned_courses_for(self, mocker):
        """
        Тест `assigned_courses_for`
        """
        viewset = CourseViewSet()
        user = User(id=1)
        mocked_course = mocker.patch.object(Course, 'objects')
        mocked_course_student = mocker.patch.object(CourseStudent, 'objects')
        mocked_course_student.filter.return_value.values_list.return_value = (
            2, 3)
        mocked_get_serializer = mocker.patch.object(viewset, 'get_serializer')
        mocked_get_serializer.return_value.data = 'courses'

        assert viewset.assigned_courses_for(user) == 'courses'
        assert mocked_course.mock_calls == [
            call.filter(students=user),
            call.filter().select_related('cover'),
        ]
        assert mocked_get_serializer.mock_calls == [
            call(
                mocked_course.filter().select_related(),
                many=True,
            )
        ], u'Должны быть сериализованы запрошенные данные'

    def test_assigned(self, mocker):
        """
        Тест метода `assigned`
        """
        user = User(id=1)
        request = MagicMock()
        request.query_params = {}
        request.user = user

        viewset = CourseViewSet()
        viewset.request = request
        viewset.format_kwarg = None

        mocked_assigned_for = mocker.patch.object(viewset,
                                                  'assigned_courses_for')
        mocked_assigned_for.return_value = 'data'

        # запрос курсов только для андроида (результат как для всех)
        viewset.request.META = {'client_application': 'Android'}
        response = viewset.assigned(request)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == 'data'
        assert mocked_assigned_for.mock_calls == [call(user)]

        # запрос курсов только для веба
        mocked_assigned_for.reset_mock()
        viewset.request.META = {'client_application': 'Web'}
        response = viewset.assigned(request)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == 'data'
        assert mocked_assigned_for.mock_calls == [call(user)]

        # запрос курсов родителем
        mocked_assigned_for.reset_mock()
        request.query_params = {'child': 15}
        mocked_user_objects = mocker.patch.object(User, 'objects')
        user.id = 20
        mocked_user_objects.get.return_value = user
        response = viewset.assigned(request)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == 'data'
        assert mocked_assigned_for.mock_calls == [call(user)]
        assert mocked_user_objects.mock_calls == [
            call.get(id=15, parents=20),
        ]

        # неправильные запросы родителем
        mocked_assigned_for.reset_mock()
        mocked_user_objects.reset_mock()
        request.query_params = {'child': 'qwerty'}
        with pytest.raises(ValidationError) as excinfo:
            viewset.assigned(request)
        assert excinfo.value.detail == ['wrong child parameter'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_assigned_for.mock_calls == []

        mocked_user_objects.reset_mock()
        mocked_assigned_for.reset_mock()
        request.query_params = {'child': 15}
        mocked_user_objects.get.side_effect = User.DoesNotExist
        with pytest.raises(Http404):
            viewset.assigned(request)
        assert mocked_user_objects.mock_calls == [
            call.get(id=15, parents=20),
        ]
        assert mocked_assigned_for.mock_calls == []

    def test_student_stats(self, mocker):
        """
        Тест статистики ученика по курсу
        """
        user = User(id=1)
        request = MagicMock()
        request.query_params = {}
        request.user = user

        viewset = CourseViewSet()
        viewset.request = request
        viewset.format_kwarg = None

        mocked_users = mocker.patch.object(User, 'objects')
        mocked_users.filter.return_value.exists.return_value = True
        mocked_journal = mocker.patch('kelvin.courses.views.StudentJournal')
        mocked_journal.return_value.data = 'data'
        mocked_journal.return_value.get_success_percent.return_value = 43
        mocked_get_object = mocker.patch.object(viewset, 'get_object')
        mocked_course_student = mocker.patch.object(CourseStudent, 'objects')
        mocked_course_student.filter.return_value.exists.return_value = True
        course = MagicMock()
        course.owner = User(id=1, first_name=u'Имя', last_name=u'Фамилия')
        mocked_get_object.return_value = course

        mocked_owner_serializer = mocker.patch(
            'kelvin.courses.views.BaseUserSerializer')
        mocked_owner_serializer.return_value.data = {'data': True}

        # в запросе нет параметра ученика
        with pytest.raises(ValidationError) as excinfo:
            viewset.student_stats(request, 4)
        assert excinfo.value.detail == ['wrong `student` parameter'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_users.mock_calls == []
        assert mocked_course_student.mock_calls == []
        assert mocked_journal.mock_calls == []
        assert mocked_get_object.mock_calls == []
        assert mocked_owner_serializer.mock_calls == []

        # нормальный случай
        user.is_teacher = True
        request.query_params = {'student': 2}
        expected = {
            'success_percent': 43,
            'journal': 'data',
            'course_owner': {'data': True},
        }

        response = viewset.student_stats(request, 4)
        assert response.status_code == 200
        assert response.data == expected
        assert mocked_users.mock_calls == [
            call.filter(id=1, parent_profile__children__id=2)
        ]
        assert mocked_course_student.mock_calls == [
            call.filter(course__owner=request.user, student=2),
            call.filter().exists(),
        ]
        assert mocked_journal.mock_calls == [
            call(2, course),
            call().get_success_percent(),
        ]
        assert mocked_get_object.mock_calls == [call()]
        assert mocked_owner_serializer.mock_calls == [
            call(instance=course.owner)]

        # пользователь не родитель ученика
        mocked_course_student.reset_mock()
        mocked_get_object.reset_mock()
        mocked_journal.reset_mock()
        mocked_users.reset_mock()
        user.is_parent = True
        user.is_teacher = False
        mocked_users.filter.return_value.exists.return_value = False

        with pytest.raises(Http404):
            viewset.student_stats(request, 4)
        assert mocked_users.mock_calls == [
            call.filter(id=1, parent_profile__children__id=2),
            call.filter().exists(),
        ]
        assert mocked_course_student.mock_calls == [
            call.filter(course__owner=request.user, student=2),
        ]
        assert mocked_journal.mock_calls == []
        assert mocked_get_object.mock_calls == []

    def test_add_clesson(self, mocker):
        """
        Тест добавления занятия в курс
        """
        user = User(id=1)
        request = MagicMock()
        request.query_params = {}
        request.user = user
        request.data = {}

        viewset = CourseViewSet()
        viewset.request = request
        viewset.format_kwarg = None

        mocked_get_clesson = mocker.patch(
            'kelvin.courses.views.get_object_or_404')
        mocked_get_object = mocker.patch.object(viewset, 'get_object')

        # в данных нет идентификатора
        with pytest.raises(ValidationError) as excinfo:
            viewset.add_clesson(request, 10)
        assert excinfo.value.detail == ['`id` field required']
        assert mocked_get_clesson.mock_calls == []
        assert mocked_get_object.mock_calls == []

        # неправильный идентификатор
        mocked_get_clesson.side_effect = ValueError
        request.data = {'id': 3}
        with pytest.raises(ValidationError) as excinfo:
            viewset.add_clesson(request, 10)
        assert excinfo.value.detail == ['wrong `id` value']
        assert mocked_get_clesson.mock_calls == [call(CourseLessonLink, id=3)]
        assert mocked_get_object.mock_calls == []

        # несуществующая связь курс-занятие
        mocked_get_clesson.reset_mock()
        mocked_get_clesson.side_effect = Http404
        with pytest.raises(Http404):
            viewset.add_clesson(request, 10)
        assert mocked_get_clesson.mock_calls == [call(CourseLessonLink, id=3)]
        assert mocked_get_object.mock_calls == []

        # недоступное учителю занятие
        mocked_get_clesson.reset_mock()
        clesson = CourseLessonLink(accessible_to_teacher=False)
        mocked_get_clesson.side_effect = [clesson]
        with pytest.raises(PermissionDenied) as excinfo:
            viewset.add_clesson(request, 10)
        assert mocked_get_clesson.mock_calls == [call(CourseLessonLink, id=3)]
        assert mocked_get_object.mock_calls == []

        # нормальное поведение
        mocked_get_clesson.reset_mock()
        clesson = CourseLessonLink(accessible_to_teacher=True)
        mocked_get_clesson.side_effect = [clesson]
        course = Course()
        mocked_get_object.return_value = course
        mocked_add_clesson = mocker.patch.object(course, 'add_clesson')
        response = viewset.add_clesson(request, 10)
        assert response.status_code == 200
        assert mocked_get_clesson.mock_calls == [call(CourseLessonLink, id=3)]
        assert mocked_get_object.mock_calls == [call()]
        assert mocked_add_clesson.mock_calls == [call(clesson)]

    stats_cases = (
        (
            [],
            [],
            [],
            {'lessons': {}, 'students': {}},
        ),
        (
            [
                CourseLessonLink(
                    id=1,
                    lesson_id=3,
                    url='""',
                ),
            ],
            [
                CourseLessonStat(
                    id=2,
                    percent_complete=70,
                    percent_fail=10,
                    results_count=20,
                    max_results_count=30,
                ),
            ],
            [
                StudentCourseStat(
                    student_id=1,
                    total_efficiency=99,
                    clesson_data={},
                ),
            ],
            {
                'lessons': {
                    1: {
                        'percent_complete': 70,
                        'percent_fail': 10,
                        'results_count': 20,
                        'max_results_count': 30,
                    }
                },
                'students': {
                    1: {
                        'total_efficiency': 99,
                        'lessons': [],
                    }
                },
            },
        ),
        (
            [
                CourseLessonLink(
                    id=1,
                    lesson_id=3,
                    url='""',
                ),
                CourseLessonLink(
                    id=4,
                    lesson_id=6,
                    url='""',
                )
            ],
            [
                CourseLessonStat(id=2, percent_complete=70),
                CourseLessonStat(id=5, percent_complete=21),
            ],
            [
                StudentCourseStat(
                    student_id=1,
                    total_efficiency=34,
                    clesson_data={
                        '1': {
                            'efficiency': 55,
                        }
                    },
                ),
            ],
            {
                'lessons': {
                    1: {
                        'percent_complete': 70,
                        'percent_fail': 0,
                        'results_count': 0,
                        'max_results_count': 0,
                    },
                    4: {
                        'percent_complete': 21,
                        'percent_fail': 0,
                        'results_count': 0,
                        'max_results_count': 0,
                    }
                },
                'students': {
                    1: {
                        'total_efficiency': 34,
                        'lessons': [55],
                    }
                },
            },
        ),
        (
            [
                CourseLessonLink(
                    id=1,
                    lesson_id=3,
                    url='""',
                ),
                CourseLessonLink(
                    id=4,
                    lesson_id=6,
                    url='""',
                ),
                CourseLessonLink(
                    id=7,
                    lesson_id=9,
                    url='""',
                ),
            ],
            [
                CourseLessonStat(id=2, percent_complete=70),
                None,
                CourseLessonStat(id=5, percent_complete=42),
            ],
            [],
            {
                'lessons': {
                    1: {
                        'percent_complete': 70,
                        'percent_fail': 0,
                        'results_count': 0,
                        'max_results_count': 0,
                    },
                    7: {
                        'percent_complete': 42,
                        'percent_fail': 0,
                        'results_count': 0,
                        'max_results_count': 0,
                    }
                },
                'students': {},
            },
        ),
    )

    @pytest.mark.django_db
    @pytest.mark.parametrize(
        "courselessonlinks,clesson_stats,student_stats,expected_data",
        stats_cases
    )
    def test_stats(self, mocker, courselessonlinks,
                   clesson_stats, student_stats, expected_data):
        """
        Тестирование статистики группы по курсу
        """
        mocked_course = MagicMock()
        for link, stat in zip(courselessonlinks, clesson_stats):
            if stat:
                link.courselessonstat = stat
        mocked_course.studentcoursestat_set.all.return_value = student_stats
        mocked_course.courselessonlink_set.all.return_value = courselessonlinks

        viewset = CourseViewSet()
        mocked_get_object = mocker.patch.object(viewset, 'get_object')
        mocked_get_object.return_value = mocked_course

        response = viewset.stats(MagicMock(), 1)
        assert response.data == expected_data, u'Неправильная статистика'
        assert mocked_get_object.called, u'Должен быть вызыван `get_object`'

    @pytest.mark.parametrize(
        'count_students',
        [
            max(1, settings.MIN_LESSON_CSV_STUDENTS - 1),
            max(1, settings.MIN_LESSON_CSV_STUDENTS),
            max(1, settings.MIN_LESSON_CSV_STUDENTS + 1),
            max(1, settings.MAX_LESSON_JOURNAL_STUDENTS - 1),
            max(1, settings.MAX_LESSON_JOURNAL_STUDENTS),
            max(1, settings.MAX_LESSON_JOURNAL_STUDENTS + 1),
        ],
    )
    def test_journal(self, mocker, count_students):
        """
        Тест курсового журнала
        объединен с test_journal_too_long
        в рамках EDUCATION-2576
        """
        viewset = CourseViewSet()
        mocked_course = MagicMock()
        mocked_get_object = mocker.patch.object(viewset, 'get_object')
        mocked_get_object.return_value = mocked_course
        mocked_journal = mocker.patch(
            'kelvin.courses.views.CourseGroupJournal')
        students_data = 'data'
        mocked_journal.return_value.data = {'some': students_data}
        mocked_course_students = mocker.patch.object(CourseStudent, 'objects')
        mocked_course_students.filter.return_value.count.return_value = (
            count_students)
        mocked_course_journal = mocker.patch.object(Course, 'objects')
        mocked_cj = MagicMock()
        url_of_csv = "BLAH"
        mocked_cj.url = url_of_csv
        mocked_course_journal.get.return_value.journal = mocked_cj
        response = viewset.journal(MagicMock(), 1)
        data_result = None
        url_result = None
        if count_students <= settings.MAX_LESSON_JOURNAL_STUDENTS:
            data_result = {'some': students_data}
        if count_students >= settings.MIN_LESSON_CSV_STUDENTS:
            url_result = url_of_csv
        assert response.data == {'data': data_result, 'csv_url': url_result}, (
            u'Неправильные данные')
        if count_students <= settings.MAX_LESSON_JOURNAL_STUDENTS:
            assert mocked_get_object.mock_calls == [call()], (
                u'Нужно получить курс'
            )
            assert mocked_journal.mock_calls == [
                call(mocked_course)
            ], u'Нужно инициализировать журнал полученным курсом'
        else:
            assert mocked_get_object.mock_calls == [], u'Ненужно получать курс'
            assert mocked_journal.mock_calls == [
            ], u'Ненужно инициализировать журнал полученным курсом'

    def test_join(self, mocker):
        """
        Тест добавления ученика в группу
        """
        mocked_atomic = mocker.patch.object(transaction, 'atomic')
        request = MagicMock()
        request.data = {'codes': ['AAA', 'BBB']}
        viewset = CourseViewSet()
        viewset.request = request
        courses = [MagicMock(), MagicMock()]

        mocked_course = mocker.patch.object(Course, 'objects')
        mocked_course.filter.return_value = courses
        mocked_assigned = mocker.patch.object(viewset, 'assigned_courses_for')
        mocked_assigned.return_value = 'courses'
        mocked_course_student = mocker.patch.object(CourseStudent, 'objects')
        mocked_ensure_assignments = mocker.patch(
            'kelvin.courses.views.ensure_student_assignments')

        # успешное добавление
        response = viewset.join(request)
        assert response.data == 'courses'
        assert response.status_code == 200
        assert mocked_course.mock_calls == [
            call.filter(code__in=['AAA', 'BBB']),
        ]
        assert mocked_assigned.mock_calls == [call(request.user)]
        assert mocked_course_student.mock_calls == [
            call.get_or_create(course=courses[0], student=request.user),
            call.get_or_create(course=courses[1], student=request.user),
        ]
        assert mocked_ensure_assignments.mock_calls == [
            call(courses[0], request.user),
            call(courses[1], request.user),
        ]

        # неправильный запрос, ненайденный курс
        mocked_assigned.reset_mock()
        mocked_course.reset_mock()
        mocked_course_student.reset_mock()
        mocked_ensure_assignments.reset_mock()
        request.data = {'codes': ['BBB', 'DDD', 'EEE']}

        with pytest.raises(NotFound) as excinfo:
            viewset.join(request)

        assert excinfo.value.status_code == 404
        assert excinfo.value.detail == 'Course with provided code(s) not found'
        assert mocked_assigned.mock_calls == []
        assert mocked_course.mock_calls == [
            call.filter(code__in=['BBB', 'DDD', 'EEE']),
        ]
        assert mocked_course_student.mock_calls == []
        assert mocked_ensure_assignments.mock_calls == []

        # запрос родителя на добавление ребенка
        mocked_assigned.reset_mock()
        mocked_course.reset_mock()
        mocked_course_student.reset_mock()
        mocked_ensure_assignments.reset_mock()
        mocked_course.filter.return_value = courses[:1]
        mocked_user_objects = mocker.patch.object(User, 'objects')
        child = User(id=15)
        mocked_user_objects.get.return_value = child
        request.data = {'codes': ['BBB'], 'child': 15}
        request.user.id = 20
        response = viewset.join(request)
        assert response.data == 'courses'
        assert response.status_code == 200
        assert mocked_course.mock_calls == [
            call.filter(code__in=['BBB']),
        ]
        assert mocked_assigned.mock_calls == [call(child)]
        assert mocked_user_objects.mock_calls == [
            call.get(id=15, parents=20),
        ]
        assert mocked_course_student.mock_calls == [
            call.get_or_create(course=courses[0], student=child),
        ]
        assert mocked_ensure_assignments.mock_calls == [
            call(courses[0], child),
        ]

        # неправильный параметр кодов
        mocked_ensure_assignments.reset_mock()
        request.data = {'code': ['BBB']}
        with pytest.raises(ValidationError) as excinfo:
            viewset.join(request)
        assert excinfo.value.detail == ['Codes not provided'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_ensure_assignments.mock_calls == []

        # неправильные коды курса
        mocked_course.filter.side_effect = TypeError
        with pytest.raises(ValidationError) as excinfo:
            viewset.join(request)
        assert excinfo.value.detail == ['Codes not provided'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_ensure_assignments.mock_calls == []

        # неправильные запросы родителя на добавление ребенка
        request.data = {'code': 'BBB', 'child': 'qwerty'}
        with pytest.raises(ValidationError) as excinfo:
            viewset.join(request)
        assert excinfo.value.detail == ['Wrong "child" parameter'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_ensure_assignments.mock_calls == []

        request.data = {'code': 'BBB', 'child': 15}
        mocked_user_objects.get.side_effect = User.DoesNotExist
        with pytest.raises(PermissionDenied):
            viewset.join(request)
        assert mocked_ensure_assignments.mock_calls == []

    def test_find_problems(self, mocker):
        """
        Тест нахождения задач в курсе
        """
        request = MagicMock()
        request.query_params = {'problems': '1,qwerty,2'}
        viewset = CourseViewSet()
        viewset.request = request

        # неправильный набор параметров
        response = viewset.find_problems(MagicMock(), 100)
        assert response.status_code == 400
        assert response.data == 'wrong problem ids'

        # правильный запрос
        request.query_params = {'problems': '10,11,12,13'}
        course = MagicMock()
        lesson1 = MagicMock()
        lesson1.name = u'Занятие 1'
        lesson1.lessonproblemlink_set.all.return_value = [
            MagicMock(problem_id=10),
            MagicMock(problem_id=11),
        ]
        lesson2 = MagicMock()
        lesson2.name = u'Занятие 2'
        lesson2.lessonproblemlink_set.all.return_value = [
            MagicMock(problem_id=11),
            MagicMock(problem_id=12),
        ]
        course.courselessonlink_set.all.return_value = [
            MagicMock(id=1, lesson=lesson1),
            MagicMock(id=2, lesson=lesson2),
        ]
        mocked_get_object = mocker.patch.object(CourseViewSet, 'get_object')
        mocked_get_object.return_value = course
        expected = {
            10: [
                {
                    'id': 1,
                    'name': u'Занятие 1',
                },
            ],
            11: [
                {
                    'id': 1,
                    'name': u'Занятие 1',
                },
                {
                    'id': 2,
                    'name': u'Занятие 2',
                },
            ],
            12: [
                {
                    'id': 2,
                    'name': u'Занятие 2',
                },
            ],
        }

        response = viewset.find_problems(MagicMock(), 100)
        assert response.status_code == 200
        assert response.data == expected


class TestCLessonViewSet(object):
    """
    Тесты вьюсета курсозанятия
    """
    def test_init(self):
        """Тест инициализации"""
        assert CLessonViewSet()._clesson is None

    get_queryset_data = [
        (
            'GET',
            'retrieve',
            False,
            False,
            False,
            False,
            False,
            [
                call.all(),
                call.all().select_related('course', 'lesson',
                                          'lesson__subject',
                                          'progress_indicator'),
                call.all().select_related().prefetch_related(
                    'lesson__lessonproblemlink_set',
                    'lesson__lessonproblemlink_set__problem',
                    'lesson__lessonproblemlink_set__theory',
                ),
            ],
        ),
        (
            'GET',
            'retrieve',
            False,
            False,
            True,
            False,
            False,
            [
                call.all(),
                call.all().select_related('course', 'lesson',
                                          'lesson__subject',
                                          'progress_indicator'),
                call.all().select_related().prefetch_related(
                    'lesson__lessonproblemlink_set',
                    'lesson__lessonproblemlink_set__problem',
                    'lesson__lessonproblemlink_set__theory',
                ),
            ],
        ),
        (
            'GET',
            'retrieve',
            False,
            False,
            True,
            True,
            False,
            [
                call.all(),
                call.all().filter(
                    MockedQOr(
                        course__in='<source_courses_queryset>',
                        accessible_to_teacher__lt='now'
                    )
                ),
                call.all().filter().select_related(
                    'course',
                    'lesson',
                    'lesson__subject',
                    'progress_indicator',
                ),
                (call.all().filter().select_related()
                    .prefetch_related(
                    'lesson__methodology',
                )),
                (
                    call.all().filter().select_related()
                    .prefetch_related().prefetch_related(
                        'lesson__lessonproblemlink_set',
                        'lesson__lessonproblemlink_set__problem',
                        'lesson__lessonproblemlink_set__theory',
                    )
                ),
            ]
        ),
        (
            'GET',
            'retrieve',
            False,
            False,
            True,
            True,
            True,
            [
                call.all(),
                call.all().select_related('course', 'lesson',
                                          'lesson__subject',
                                          'progress_indicator'),
                (
                    call.all().select_related()
                    .prefetch_related(
                        'lesson__methodology',
                    )
                ),
                (
                    call.all().select_related().prefetch_related()
                    .prefetch_related(
                        'lesson__lessonproblemlink_set',
                        'lesson__lessonproblemlink_set__problem',
                        'lesson__lessonproblemlink_set__theory',
                    )
                ),
            ],
        ),
        (
            'GET',
            'retrieve',
            True,
            False,
            True,
            False,
            False,
            [
                call.all(),
                call.all().select_related('course', 'lesson',
                                          'lesson__subject',
                                          'progress_indicator'),
                call.all().select_related().prefetch_related(
                    'lesson__lessonproblemlink_set',
                    'lesson__lessonproblemlink_set__problem',
                    'lesson__lessonproblemlink_set__theory',
                ),
                call.all().select_related().prefetch_related()
                    .prefetch_related(
                        'lesson__lessonproblemlink_set__problem__resources',
                        'lesson__lessonproblemlink_set__theory__resources',
                        'lesson__lessonproblemlink_set__theory'
                        '__content_type_object',
                        'lesson__lessonproblemlink_set__theory'
                        '__content_type_object__resource',
                ),
            ]
        ),
        (
            'GET',
            'retrieve',
            True,
            False,
            True,
            True,
            False,
            [
                call.all(),
                call.all().filter(
                    MockedQOr(
                        course__in='<source_courses_queryset>',
                        accessible_to_teacher__lt='now'
                    )
                ),
                call.all().filter().select_related(
                    'course',
                    'lesson',
                    'lesson__subject',
                    'progress_indicator',
                ),
                (
                    call.all().filter().select_related()
                    .prefetch_related(
                        'lesson__methodology',
                    )
                ),
                (
                    call.all().filter().select_related().prefetch_related()
                    .prefetch_related(
                        'lesson__lessonproblemlink_set',
                        'lesson__lessonproblemlink_set__problem',
                        'lesson__lessonproblemlink_set__theory',
                    )
                ),
                (call.all().filter().select_related().prefetch_related()
                 .prefetch_related().prefetch_related(
                    'lesson__lessonproblemlink_set__problem__resources',
                    'lesson__lessonproblemlink_set__theory__resources',
                    'lesson__lessonproblemlink_set__theory'
                    '__content_type_object',
                    'lesson__lessonproblemlink_set__theory'
                    '__content_type_object__resource',
                )),
                (call.all().filter().select_related().prefetch_related()
                 .prefetch_related().prefetch_related().prefetch_related(
                    'lesson__lessonproblemlink_set__problem__subject',
                    'lesson__lessonproblemlink_set__problem__meta',
                )),
            ]
        ),
        (
            'GET',
            'retrieve',
            True,
            True,
            True,
            True,
            False,
            [
                call.all(),
                call.all().filter(
                    MockedQOr(
                        course__in='<source_courses_queryset>',
                        accessible_to_teacher__lt='now'
                    )
                ),
                call.all().filter().select_related(
                    'course',
                    'lesson',
                    'lesson__subject',
                    'progress_indicator',
                ),
                (
                    call.all().filter().select_related()
                        .prefetch_related(
                        'lesson__methodology',
                    )
                ),
                (
                    call.all().filter().select_related()
                    .prefetch_related().prefetch_related(
                        'lesson__lessonproblemlink_set',
                        'lesson__lessonproblemlink_set__problem',
                        'lesson__lessonproblemlink_set__theory',
                    )
                ),
                (call.all().filter().select_related().prefetch_related()
                    .prefetch_related().prefetch_related(
                    'lesson__lessonproblemlink_set__problem__resources',
                    'lesson__lessonproblemlink_set__theory__resources',
                    'lesson__lessonproblemlink_set__theory'
                    '__content_type_object',
                    'lesson__lessonproblemlink_set__theory'
                    '__content_type_object__resource',
                )),
                (call.all().filter().select_related().prefetch_related()
                    .prefetch_related().prefetch_related().prefetch_related(
                    'lesson__lessonproblemlink_set__problem__subject',
                    'lesson__lessonproblemlink_set__problem__meta',
                )),
                (call.all().filter().select_related().prefetch_related()
                 .prefetch_related().prefetch_related().prefetch_related()
                 .prefetch_related(
                    'lesson__lessonproblemlink_set__problem__meta__main_theme',
                    'lesson__lessonproblemlink_set__problem__meta'
                    '__additional_themes',
                    'lesson__lessonproblemlink_set'
                    '__problem__meta__group_levels',
                    'lesson__lessonproblemlink_set__problem__meta__skills',
                    'lesson__lessonproblemlink_set__problem__meta'
                    '__problemmetaexam_set',
                    'lesson__lessonproblemlink_set__problem__meta'
                    '__problemmetaexam_set__exam',
                )),
            ]
        ),
        (
            'POST',
            'create',
            False,
            False,
            True,
            False,
            False,
            [
                call.all(),
                call.all().select_related('course', 'lesson',
                                          'lesson__subject',
                                          'progress_indicator'),
            ]
        ),
        (
            'POST',
            'create',
            False,
            False,
            True,
            True,
            False,
            [
                call.all(),
                call.all().filter(
                    MockedQOr(
                        course__in='<source_courses_queryset>',
                        accessible_to_teacher__lt='now'
                    )
                ),
                call.all().filter().select_related(
                    'course',
                    'lesson',
                    'lesson__subject',
                    'progress_indicator',
                ),
            ]
        ),
        (
            'POST',
            'create',
            True,
            False,
            True,
            False,
            False,
            [
                call.all(),
                call.all().select_related('course', 'lesson',
                                          'lesson__subject',
                                          'progress_indicator'),
            ]
        ),
    ]

    @pytest.mark.parametrize(
        'method,action,expand_problems,expand_meta,is_authenticated,'
        'is_teacher,is_content_manager,calls', get_queryset_data,
    )
    def test_get_queryset(self, method, action, expand_problems, expand_meta,
                          is_authenticated, is_teacher, is_content_manager,
                          calls, mocker):
        """
        Тест метода `get_queryset`
        """
        mocked_timezone = mocker.patch.object(timezone, 'now')
        mocked_timezone.return_value = 'now'
        mocked_objects = mocker.patch.object(CourseLessonLink, 'objects')
        request = MagicMock()
        request.method = method
        request.query_params = {}
        request.user = User(
            is_teacher=is_teacher,
            is_content_manager=is_content_manager,
            is_parent=False,
        ) if is_authenticated else AnonymousUser()
        teacher_courses_objects = mocker.patch.object(Course, 'objects')
        teacher_courses_objects.filter = MagicMock()
        source_courses_queryset = teacher_courses_objects.filter
        source_courses_queryset.return_value = '<source_courses_queryset>'

        if expand_problems:
            request.query_params['expand_problems'] = True
        if expand_meta:
            request.query_params['expand_meta'] = True
        view = CLessonViewSet()
        view.request = request
        view.action = action
        view.get_queryset()

        if is_teacher and is_authenticated and not is_content_manager:
            source_courses_queryset.assert_called_once_with(
                course__owner=request.user
            )

        assert mocked_objects.mock_calls == calls

    get_object_data_with_copy = (
        (
            'PUT',
            MagicMock(lesson_editable=False),
        ),
        (
            'POST',
            MagicMock(lesson_editable=False),
        ),
        (
            'PATCH',
            MagicMock(lesson_editable=False),
        ),
    )

    @pytest.mark.parametrize('method,clesson', get_object_data_with_copy)
    def test_get_object_with_copy(self, mocker, method, clesson):
        """
        Проверяем, что при изменении копируем занятие
        """
        mocked_atomic = mocker.patch.object(transaction, 'atomic')
        mocked_get_object = mocker.patch.object(ModelViewSet, 'get_object')
        mocked_get_object.return_value = clesson
        copy_return_data = {
            'new_lesson_id': 101,
        }
        mocked_lesson_copy = mocker.patch(
            'kelvin.courses.views.course_lesson.clesson.copy_lesson'
        )
        mocked_lesson_copy.return_value = copy_return_data

        request = MagicMock()
        request.user = User()
        request.method = method
        viewset = CLessonViewSet()
        viewset.request = request
        mocked_change_data_ids = mocker.patch.object(
            viewset, '_change_data_ids',
        )

        returned_object = viewset.get_object()

        print(("mocked_get_object=", mocked_get_object.mock_calls))
        print(("mocked_lesson_copy=", mocked_lesson_copy.mock_calls))
        print(("clesson=", clesson.mock_calls))

        assert mocked_get_object.mock_calls == [
            call(),
        ]
        assert mocked_lesson_copy.mock_calls == [
            call(mocked_get_object().lesson, owner=request.user),
        ]
        assert clesson.mock_calls == [
            call.__bool__(),
            # call.lesson.copy(owner=request.user),
            call.save()
        ]
        assert clesson.lesson_id == 101
        assert clesson.lesson_editable is True
        assert mocked_change_data_ids.mock_calls == [call(copy_return_data)]
        assert returned_object is clesson, (
            u'Должен быть объект, возвращенный родительским методом')
        assert clesson.lesson_editable is True, (
            u'Занятие должно быть редактируемым')

    get_object_data_no_copy = (
        (
            'GET',
            CourseLessonLink(lesson_editable=False),
            [
                call(),
            ],
        ),
        (
            'PUT',
            CourseLessonLink(lesson_editable=True),
            [
                call(),
            ],
        ),
        (
            'PUT',
            CourseLessonLink(lesson_editable=True),
            [
                call(),
            ],
        ),
    )

    @pytest.mark.parametrize('method,clesson,calls', get_object_data_no_copy)
    def test_get_object_no_copy(self, mocker, method, clesson, calls):
        """
        Проверяем, что при изменении копируем занятие
        """
        mocked_get_object = mocker.patch.object(ModelViewSet, 'get_object')
        mocked_get_object.return_value = clesson

        request = MagicMock()
        request.user = User()
        request.method = method
        viewset = CLessonViewSet()
        viewset.request = request

        returned_object = viewset.get_object()

        assert mocked_get_object.mock_calls == calls
        assert returned_object is clesson, (
            u'Должен быть объект, возвращенный родительским методом')

        # повторный вызов не приводит к новым запросам
        mocked_get_object.reset_mock()
        assert viewset.get_object() is clesson, (
            u'Должен быть объект, возвращенный родительским методом')
        assert mocked_get_object.mock_calls == []

    def test_change_data_ids(self):
        """
        Проверяем изменение идентификаторов в данных запроса
        """
        request = MagicMock()
        request.data = {
            'id': 3,
            'problems': [
                {
                    'id': '10',
                    'attempts': 5,
                    'problem': {
                        'id': '100',
                    },
                },
                {
                    'id': 'asd',
                    'attempts': 5,
                    'problem': {
                        'id': 101,
                    },
                },
                {
                    'id': 12,
                    'attempts': 5,
                    'problem': {
                        'id': 'qwe',
                    },
                },
            ],
            'clesson': {
                'id': 21,
            }
        }
        viewset = CLessonViewSet()
        viewset.request = request

        old_to_new = {
            'new_lesson_id': 2003,
            'problems': {
                10: 1000,
                11: 1001,
                12: 1002,
            }
        }
        expected = {
            'id': 2003,
            'problems': [
                {
                    'id': 1000,
                    'attempts': 5,
                    'problem': {
                        'id': '100',
                    },
                },
                {
                    'id': 'asd',
                    'attempts': 5,
                    'problem': {
                        'id': 101,
                    },
                },
                {
                    'id': 1002,
                    'attempts': 5,
                    'problem': {
                        'id': 'qwe',
                    },
                },
            ],
            'clesson': {
                'id': 21,
            }
        }

        assert viewset._change_data_ids(old_to_new) is None
        assert request.data == expected, u'Должны измениться данные в запросе'

    def test_latest_result(self, mocker):
        """
        Тест метода получения последнего результата
        """
        mocked_context = mocker.patch.object(CLessonViewSet,
                                             'get_serializer_context')
        mocked_context.return_value = 'ctx'
        request = MagicMock()
        viewset = CLessonViewSet()
        viewset.request = request
        mocked_results = mocker.patch.object(CourseLessonResult, 'objects')
        (mocked_results.filter.return_value.select_related.return_value
         .latest.return_value) = 'object'
        mocked_clesson_serializer = mocker.patch.object(
            CourseLessonResultSerializer, 'to_representation')
        mocked_clesson_serializer.return_value = 'serialized object'

        response = viewset.latest_result(request, 25)
        assert response.status_code == 200
        assert response.data == 'serialized object'
        assert mocked_clesson_serializer.mock_calls == [
            call('object'),
        ]
        assert mocked_results.mock_calls == [
            call.filter(summary__clesson=25, summary__student=request.user),
            call.filter().select_related('summary'),
            call.filter().select_related().latest('date_created'),
        ]

        # отсутствие результата
        mocked_clesson_serializer.reset_mock()
        mocked_results.reset_mock()
        (mocked_results.filter.return_value.select_related.return_value
         .latest.side_effect) = CourseLessonResult.DoesNotExist
        with pytest.raises(Http404):
            viewset.latest_result(request, 25)
        assert mocked_clesson_serializer.mock_calls == []
        assert mocked_results.mock_calls == [
            call.filter(summary__clesson=25, summary__student=request.user),
            call.filter().select_related('summary'),
            call.filter().select_related().latest('date_created'),
        ]

    @pytest.mark.skip
    def test_complete(self, mocker):
        """
        Тест завершения занятия
        """
        request = MagicMock()
        viewset = CLessonViewSet()
        viewset.request = request
        viewset.format_kwarg = None

        # нет даты назначения
        mocked_get_object = mocker.patch.object(CLessonViewSet, 'get_object')
        clesson = MagicMock(date_assignment=None)
        mocked_get_object.return_value = clesson
        with pytest.raises(ValidationError) as excinfo:
            viewset.complete('request', 'pk')
        assert excinfo.value.detail == ['CLesson must be assigned']
        assert mocked_get_object.mock_calls == [call()]

        # уже есть дата назначения
        mocked_get_object.reset_mock()
        clesson = MagicMock(date_assignment=True, date_completed=True)
        mocked_get_object.return_value = clesson
        with pytest.raises(ValidationError) as excinfo:
            viewset.complete('request', 'pk')
        assert excinfo.value.detail == ['CLesson is completed']
        assert mocked_get_object.mock_calls == [call()]

        # есть дата назначения
        mocked_get_object.reset_mock()
        clesson = MagicMock(date_assignment=True, date_completed=None)
        mocked_get_object.return_value = clesson
        mocked_datetime = mocker.patch('kelvin.courses.views.timezone')
        mocked_datetime.now.return_value = 'some datetime'
        mocked_get_serializer = mocker.patch.object(CLessonViewSet,
                                                    'get_serializer')
        mocked_get_serializer.return_value.to_representation.return_value = (
            'some data')

        response = viewset.complete('request', 'pk')
        assert isinstance(response, Response)
        assert response.data == 'some data'
        assert mocked_get_object.mock_calls == [call(), call().save()]
        assert mocked_datetime.mock_calls == [call.now()]
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(clesson),
        ]

    def test_finish(self, mocker):
        """
        Тест завершения занятия учеником
        """
        request = MagicMock()
        request.user = MagicMock()
        viewset = CLessonViewSet()
        viewset.request = request
        viewset.format_kwarg = None
        mocked_get_object = mocker.patch.object(CLessonViewSet, 'get_object')
        clesson = MagicMock()
        mocked_get_object.return_value = clesson

        # создание сводки результатов
        mocked_summary = mocker.patch.object(CourseLessonSummary, 'objects')
        summary = MagicMock()
        mocked_summary.get_or_create.return_value = summary, True
        response = viewset.finish(request, 'pk')
        assert isinstance(summary.lesson_finished, MagicMock)
        assert response.status_code == 201
        assert response.data is None
        assert mocked_summary.mock_calls == [
            call.get_or_create(clesson=clesson, student=request.user,
                               defaults={'lesson_finished': True}),
        ]
        assert mocked_get_object.mock_calls == [call()]
        assert summary.mock_calls == []

        # изменение сводки результатов
        mocked_get_object.reset_mock()
        mocked_summary.reset_mock()
        summary = MagicMock()
        summary.lesson_finished = False
        mocked_summary.get_or_create.return_value = summary, False
        response = viewset.finish(request, 'pk')
        assert summary.lesson_finished is True
        assert response.status_code == 200
        assert response.data is None
        assert mocked_summary.mock_calls == [
            call.get_or_create(clesson=clesson, student=request.user,
                               defaults={'lesson_finished': True}),
        ]
        assert mocked_get_object.mock_calls == [call()]
        assert summary.mock_calls == [call.save()]

    def test_web(self, mocker):
        """
        Тесты получения курсозанятия вебом
        """
        DEFAULT_CLESSON_PARAMS = {
            'available': True
        }

        class MockedCache(dict):
            data = []

            def set(self, key, value, timeout=None):
                self.data.append((key, value, timeout))

        mocker.patch(
            'kelvin.courses.views.course_lesson.web_view.cache',
            new=MockedCache(),
        )
        mocked_now = mocker.patch.object(timezone, 'now')
        mocked_now.return_value = '<now>'
        mocked_get_object = mocker.patch.object(CLessonViewSet, 'get_object')
        mocked_result_manager = mocker.patch.object(
            CourseLessonResult,
            'objects',
        )
        mocked_get_serializer = mocker.patch.object(
            CLessonViewSet,
            'get_serializer',
        )
        mocked_control_work_data = mocker.patch.object(
            CLessonViewSet,
            'get_control_work_data',
        )
        mocked_control_work_data.return_value = {'control_work': True}
        serializer = MagicMock()
        serializer.data = {
            'id': 1,
            'clesson': {'some': 'data'},
        }
        mocked_get_serializer.return_value = serializer

        viewset = CLessonViewSet()
        request = MagicMock()
        request.user = MagicMock()
        viewset.request = request

        # Случай, когда курсозанятие не контрольная
        clesson = MagicMock(mode=1, date_assignment=None,
                            accessible_to_teacher=None, available=True)
        mocked_get_object.return_value = clesson

        response = viewset.web(request, pk=100)

        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'clesson': dict({
                'some': 'data',
                'date_assignment_passed': False,
                'accessible_to_teacher': False,
            }, **DEFAULT_CLESSON_PARAMS),
            'group_levels': [],
        }, u'Для не-контрольных должны вернуть обычные данные'
        assert mocked_get_object.called, u'Нужно получить курсозанятие'
        assert clesson.course.group_levels.__iter__.called, (
            u'Нужно получить классы курса')
        assert [call(clesson)] in mocked_get_serializer.mock_calls, (
            u'Нужно сериализовать курсозанятие')
        assert mocked_result_manager.mock_calls == [], (
            u'Не должно быть обращений к результатам')
        assert mocked_control_work_data.mock_calls == [], (
            u'Не должно быть получения данных контрольной')

        # Случай контрольной
        mocked_get_object.reset_mock()
        mocked_get_serializer.reset_mock()

        clesson = MagicMock(mode=2, date_assignment=None,
                            accessible_to_teacher=' before now')
        mocked_get_object.return_value = clesson
        (mocked_result_manager.filter.return_value.order_by.return_value
         .first.return_value) = '<result>'

        response = viewset.web(request, pk=100)

        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'clesson': dict({
                'some': 'data',
                'control_work': True,
                'accessible_to_teacher': True,
                'date_assignment_passed': False,
            }, **DEFAULT_CLESSON_PARAMS),
            'group_levels': [],
        }, u'Для контрольной нужно вернуть дополнительные данные'
        assert mocked_get_object.called, u'Нужно получить курсозанятие'
        assert clesson.course.group_levels.__iter__.called, (
            u'Нужно получить классы курса')
        assert [call(clesson)] in mocked_get_serializer.mock_calls, (
            u'Нужно сериализовать курсозанятие')
        assert mocked_result_manager.mock_calls == [
            call.filter(
                summary__clesson=clesson,
                summary__student=request.user,
                work_out=False,
            ),
            call.filter().order_by('-date_updated'),
            call.filter().order_by().first()
        ], u'Нужно получить последний результат ученика'
        assert mocked_control_work_data.mock_calls == [
            call(clesson, '<result>', '<now>')
        ], u'Нужно сериализовать данные контрольной'

        # проверяем корректность выставления флага `date_assignment_passed`
        serializer.data = {
            'id': 1,
            'clesson': {},
            'group_levels': [],
        }

        # 1. не выданное
        clesson = MagicMock(mode=1, date_assignment=None,
                            accessible_to_teacher=' before now')
        mocked_get_object.return_value = clesson

        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'clesson': dict({
                'date_assignment_passed': False,
                'accessible_to_teacher': True,
            }, **DEFAULT_CLESSON_PARAMS),
            'group_levels': [],
        }

        # 2. выданное в прошлом
        clesson = MagicMock(mode=1, date_assignment=' before now',
                            accessible_to_teacher=' before now')
        mocked_get_object.return_value = clesson

        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'clesson': dict({
                'date_assignment_passed': True,
                'accessible_to_teacher': True,
            }, **DEFAULT_CLESSON_PARAMS),
            'group_levels': [],
        }

        # 3. выданное в будущем
        clesson = MagicMock(mode=1, date_assignment='after now',
                            accessible_to_teacher=' before now')
        mocked_get_object.return_value = clesson

        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {
            'id': 1,
            'clesson': dict({
                'date_assignment_passed': False,
                'accessible_to_teacher': True,
            }, **DEFAULT_CLESSON_PARAMS),
            'group_levels': [],
        }

        # неавторизованный пользователь
        mocked_get_object.reset_mock()
        mocked_get_serializer.reset_mock()
        request.user.is_authenticated = False
        clesson = MagicMock(
            id=99,
            mode=1,
            date_assignment=None,
            accessible_to_teacher=None,
            date_updated=datetime(2017, 3, 1, minute=1, tzinfo=utc),
            **DEFAULT_CLESSON_PARAMS
        )
        mocked_get_object.return_value = clesson

        # данных нет в кэше
        mocked_clesson_objects = mocker.patch.object(CourseLessonLink,
                                                     'objects')
        mocked_clesson_objects.values_list.return_value.get.return_value = (
            datetime(2017, 3, 1, minute=1, tzinfo=utc))
        request.query_params = {
            'expand_problems': 'True',
            'hide_answers': '',
        }
        expected_data = {
            'id': 1,
            'clesson': dict({
                'date_assignment_passed': False,
                'accessible_to_teacher': False,
            }, **DEFAULT_CLESSON_PARAMS),
            'group_levels': [],
        }
        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == expected_data
        assert MockedCache.data == [
            ('clesson_99_1488326460000000_11', expected_data, 600),
        ]

        # данные в кэше
        mocker.patch(
            'kelvin.courses.views.course_lesson.web_view.cache',
            new=MockedCache(
                {'clesson_100_1488326460000000_11': {'some': 'data'}}),
        )
        response = viewset.web(request, pk=100)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert response.data == {'some': 'data'}

        # несуществующее занятие
        mocked_clesson_objects.values_list.return_value.get.side_effect = (
            TypeError, ValueError, CourseLessonLink.DoesNotExist)
        for _ in range(3):
            with pytest.raises(Http404):
                viewset.web(request, pk=100)
