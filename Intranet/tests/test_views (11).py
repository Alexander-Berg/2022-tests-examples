from builtins import object
from datetime import datetime

from mock import MagicMock, call

from django.contrib.auth import get_user_model

from kelvin.courses.models import CourseLessonLink
from kelvin.courses.tests.test_views import MockedQOr
from kelvin.lessons.models import LessonProblemLink
from kelvin.problems.answers import Answer
from kelvin.problems.markers import Marker
from kelvin.problems.models import Problem
from kelvin.problems.serializers import (
    ProblemSerializer, ProblemWithExpandedOwnerSerializer, TextResourceSerializer,
    TextResourceWithExpandedOwnerSerializer,
)
from kelvin.problems.views import ProblemViewSet, TextResourceViewSet

User = get_user_model()


class TestProblemViewSet(object):
    """
    Тесты API задач
    """

    def test_get_serializer_class(self):
        """Тест определения класса сериализатора"""
        request = MagicMock()
        viewset = ProblemViewSet()
        viewset.request = request

        # Клиент-андроид
        viewset.action = 'list'
        request.META = {'client_application': 'Android'}
        assert viewset.get_serializer_class() == ProblemSerializer, (
            u'Для андроида возвращается обычная разметка')

        # Любой другой клиент запрашивает список
        request.META = {}
        assert viewset.get_serializer_class() == ProblemSerializer, (
            u'Нужно вернуть обычный сериализатор задачи')

        # Любой другой клиент запрашивает задачу
        viewset.action = 'retrieve'
        request.META = {}
        assert (viewset.get_serializer_class() ==
                ProblemWithExpandedOwnerSerializer), (
            u'Нужно вернуть обычный сериализатор задачи')

    def test_screenshot(self, mocker):
        """
        Тест ручки обновления скриншота
        """
        markup = {
            'containers': [
                {
                    'content': '{marker:1} {marker:2}',
                    'style': 'normal',
                },
            ],
            'markers': {
                '1': {
                    'type': 'field',
                    'options': {
                        'type_content': 'number',
                    },
                    'answer': '3',
                },
                '2': {
                    'type': 'field',
                    'answer': 'qwerty',
                }
            }
        }
        owner = User(id=11, email='any@1.com')
        problem = Problem(markup=markup, id=10, owner=owner,
                          date_updated=datetime.now())

        problem.screenshot = MagicMock()
        mocked_get_object = mocker.patch.object(ProblemViewSet, 'get_object')
        mocked_get_object.return_value = problem

        viewset = ProblemViewSet()
        request = MagicMock()
        request.query_params = {}
        viewset.request = request
        viewset.format_kwarg = None
        viewset.serializer_class = MagicMock()
        viewset.action = 'list'

        response = viewset.screenshot(request)
        # запрос без файла
        assert response.status_code == 400

        file_mock = MagicMock()
        file_mock.name = 'test_file_name'
        request.data = {'file': file_mock}

        # запрос с файлом
        response = viewset.screenshot(request)
        assert response.status_code == 200
        assert problem.screenshot.delete.called_once_with(save=False)
        assert problem.screenshot.save.called_once_with(file_mock.name,
                                                        file_mock)
        assert viewset.serializer_class.called_once_with(problem)

    def test_answer(self, mocker):
        """
        Тест ручки ответа на задачу
        """
        markup = {
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1} {marker:2}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 1,
                        'options': {
                            'type_content': 'number'
                        }
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 2,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': '3',
                '2': 'qwerty'
            }
        }
        owner = User(id=11, email='any@1.com')
        problem = Problem(markup=markup, id=10, owner=owner,
                          date_updated=datetime.now())
        mocked_problem_resources = mocker.patch.object(Problem, 'resources')
        mocked_problem_resources.return_value = []
        mocked_get_object = mocker.patch.object(ProblemViewSet, 'get_object')
        mocked_get_object.return_value = problem

        attempt = {
            'markers': {
                '1': {
                    'status': Marker.INCORRECT,
                    'answer_status': 0,
                    'user_answer': '2',
                    'mistakes': 1,
                    'max_mistakes': 1,
                },
                '2': {
                    'status': Marker.INCORRECT,
                    'answer_status': -1,
                    'mistakes': 1,
                    'max_mistakes': 1,
                },
            },
            'theory': None,
            'custom_answer': None,
            'status': Answer.INCORRECT,
            'completed': True,
            'spent_time': None,
            'points': None,  # EDU-274
            'comment': '',
            'answered': False,
        }

        viewset = ProblemViewSet()
        request = MagicMock()
        request.query_params = {}
        request.data = {'1': {'user_answer': '2'}}
        viewset.request = request
        viewset.format_kwarg = None
        viewset.action = 'list'
        response = viewset.answer(request)

        assert response.status_code == 200, u'Неправильный код ответа'
        answer = response.data
        assert 'markup' in answer, u'Должен быть вопрос в ответе'
        assert 'attempt' in answer, u'Должна быть попытка в ответе'
        assert answer['attempt'] == attempt, (
            u'Неправильно сериализована попытка')
        assert mocked_get_object.mock_calls == [call()], (
            u'Должны вызвать метод для получения объекта задачи')

    def test_get_queryset(self, mocker):
        """
        Тест `get_queryset`
        """
        mocked_timezone = mocker.patch('kelvin.problems.views.timezone')
        mocked_timezone.now.return_value = 'timezone now'
        # анонимный пользователь запрашивает одну задачу
        request = MagicMock()
        request.user.is_authenticated = False
        request.method = 'GET'
        request.query_params = {}
        viewset = ProblemViewSet()
        viewset.request = request
        viewset.action = 'retrieve'
        mocked_problems = mocker.patch.object(Problem, 'objects')
        mocked_problems.filter.return_value.select_related.return_value = 'qs2'
        assert viewset.get_queryset() == 'qs2'
        assert mocked_problems.mock_calls == [
            call.filter(visibility=Problem.VISIBILITY_PUBLIC),
            call.filter().select_related('owner'),
        ]

        # анонимный пользователь запрашивает список
        mocked_problems.reset_mock()
        viewset.action = 'list'
        mocked_problems.filter.return_value = 'qs'
        assert viewset.get_queryset() == 'qs'
        assert mocked_problems.mock_calls == [
            call.filter(visibility=Problem.VISIBILITY_PUBLIC),
        ]

        # пользователь не учитель запрашивает одну задачу
        request.user.is_authenticated = True
        request.user.is_teacher = False
        request.user.is_content_manager = False
        viewset.action = 'retrieve'
        mocked_problems.reset_mock()
        (mocked_problems.all.return_value.select_related.return_value
         .filter.return_value.select_related.return_value.order_by.return_value
         .prefetch_related.return_value) = 'qs'

        mocked_clessons = mocker.patch.object(CourseLessonLink, 'objects')
        mocked_lproblems = mocker.patch.object(LessonProblemLink, 'objects')
        mocked_lproblems.filter.return_value.values_list.return_value = (
            'problems')

        assert viewset.get_queryset() == 'qs'
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().select_related('owner'),
            call.all().select_related().filter(
                MockedQOr(
                    MockedQOr(id__in='problems'),
                    MockedQOr(owner=request.user),
                    MockedQOr(visibility=Problem.VISIBILITY_PUBLIC),
                )
            ),
            call.all().select_related().filter().select_related('meta'),
            call.all().select_related().filter().select_related()
                .order_by('id'),
            call.all().select_related().filter().select_related().order_by()
                .prefetch_related('resources'),
        ]

        # пользователь не учитель запрашивает список задач
        request.user.is_authenticated = True
        request.user.is_teacher = False
        request.user.is_content_manager = False
        viewset.action = 'list'
        mocked_clessons.reset_mock()
        mocked_lproblems.reset_mock()
        mocked_problems.reset_mock()
        (mocked_problems.all.return_value.filter.return_value
         .select_related.return_value.order_by.return_value
         .prefetch_related.return_value) = 'qs'

        assert viewset.get_queryset() == 'qs'
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(
                MockedQOr(
                    MockedQOr(id__in='problems'),
                    MockedQOr(owner=request.user),
                    MockedQOr(visibility=Problem.VISIBILITY_PUBLIC),
                )
            ),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
            call.all().filter().select_related().order_by()
                .prefetch_related('resources'),
        ]

        # пользователь контент-менеджер
        mocked_problems.reset_mock()
        (mocked_problems.all.return_value.filter.return_value
         .select_related.return_value.order_by.return_value
         .prefetch_related.return_value) = 'qs'
        request.user.is_content_manager = True
        assert viewset.get_queryset() == 'qs'
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(owner__is_content_manager=True),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
            call.all().filter().select_related().order_by().prefetch_related(
                'resources'),
        ]
        # фильтрация своих задач
        mocked_problems.reset_mock()
        request.query_params = {'mine': 1}
        (mocked_problems.all.return_value.filter.return_value
         .select_related.return_value.order_by.return_value
         .prefetch_related.return_value) = 'my problems'
        request.user.is_content_manager = True
        assert viewset.get_queryset() == 'my problems'
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(owner=request.user),
            call.all().filter(owner=request.user).select_related('meta'),
            call.all().filter(owner=request.user).select_related()
                .order_by('id'),
            call.all().filter(owner=request.user).select_related().order_by()
                .prefetch_related('resources'),
        ]

        # запрос задач с метаинформацией
        # снова мокаем, чтобы задать правильно возвращаемое значение,
        # иначе `'str' object has no attribute 'select_related'`
        mocked_problems = mocker.patch.object(Problem, 'objects')
        request.query_params = {'expand_meta': 1}
        (mocked_problems.all.return_value.filter.return_value.select_related.
         return_value.order_by.return_value.prefetch_related.return_value
         .select_related.return_value.prefetch_related
         .return_value) = 'qs with meta'
        request.user.is_content_manager = True
        assert viewset.get_queryset() == 'qs with meta'
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(owner__is_content_manager=True),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
            call.all().filter().select_related().order_by().prefetch_related(
                'resources'),
            call.all().filter().select_related().order_by().prefetch_related()
                .select_related('meta__main_theme'),
            call.all().filter().select_related().order_by().prefetch_related()
                .select_related().prefetch_related(
                    'meta__group_levels',
                    'meta__problemmetaexam_set',
                    'meta__problemmetaexam_set__exam',
                    'meta__skills',
                    'meta__additional_themes',
            ),
        ]

        # пользователь учитель
        mocked_problems.reset_mock()
        request.query_params = {}
        request.user.is_content_manager = False
        request.user.is_teacher = True

        mocked_clessons.reset_mock()
        mocked_clessons.filter.return_value.values_list.return_value = (
            'lessons')
        mocked_lproblems.reset_mock()
        (mocked_problems.all.return_value.filter.return_value.
         select_related.return_value.order_by.return_value.
         prefetch_related.return_value) = 'teacher qs'
        teacher_q = MockedQOr(
            MockedQOr(
                course__owner=request.user,
                accessible_to_teacher__lt='timezone now',
            ),
            MockedQOr(
                copies__course__owner=request.user,
                copies__accessible_to_teacher__lt='timezone now',
            ),
            MockedQOr(
                course__free=True,
                accessible_to_teacher__lt='timezone now',
            ),
        )

        assert viewset.get_queryset() == 'teacher qs'
        assert mocked_clessons.mock_calls == [
            call.filter(teacher_q),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in='lessons'),
            call.filter().values_list('problem_id'),
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(
                MockedQOr(
                    MockedQOr(id__in='problems'),
                    MockedQOr(owner=request.user),
                    MockedQOr(visibility=Problem.VISIBILITY_PUBLIC),
                )
            ),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
            call.all().filter().select_related().order_by()
                .prefetch_related('resources'),
        ]

        # запрос с фильтрацией по курсу
        mocked_clessons.reset_mock()
        mocked_lproblems.reset_mock()
        mocked_problems.reset_mock()
        request.query_params = {'course': '5'}
        (mocked_clessons.filter.return_value.values_list
         .return_value) = 'course lessons'
        assert viewset.get_queryset() == 'teacher qs'
        assert mocked_clessons.mock_calls == [
            call.filter(
                teacher_q,
                MockedQOr(course_id='5'),
            ),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in='course lessons'),
            call.filter().values_list('problem_id')
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(MockedQOr(id__in='problems')),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
            call.all().filter().select_related().order_by()
                .prefetch_related('resources'),
        ]

        # запрос с фильтрацией по занятию
        mocked_clessons.reset_mock()
        mocked_lproblems.reset_mock()
        mocked_problems.reset_mock()
        request.query_params = {'lesson': '7'}
        mocked_clessons.filter.return_value.values_list.return_value = [7]
        mocked_lproblems.filter.return_value.values_list.return_value = (
            'lesson problems')
        assert viewset.get_queryset() == 'teacher qs'
        assert mocked_clessons.mock_calls == [
            call.filter(
                teacher_q,
                MockedQOr(lesson_id='7'),
            ),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in=[7]),
            call.filter().values_list('problem_id')
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(MockedQOr(id__in='lesson problems')),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
            call.all().filter().select_related().order_by()
                .prefetch_related('resources')
        ]

        # исключение занятия из выдачи
        mocked_clessons.reset_mock()
        mocked_lproblems.reset_mock()
        mocked_problems.reset_mock()
        request.query_params = {'exclude_lesson': '7'}
        mocked_clessons.filter.return_value.values_list.return_value = [81]
        mocked_lproblems.filter.return_value.values_list.return_value = (
            'lesson problems')
        (mocked_problems.all.return_value.filter.return_value.exclude
         .return_value.select_related.return_value.order_by.return_value.
         prefetch_related.return_value) = 'teacher exclude qs'
        assert viewset.get_queryset() == 'teacher exclude qs'
        assert mocked_clessons.mock_calls == [
            call.filter(teacher_q),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in=[81]),
            call.filter().values_list('problem_id'),
            call.filter(lesson_id='7', problem_id__isnull=False),
            call.filter().values_list('problem_id')
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(
                MockedQOr(
                    MockedQOr(id__in='lesson problems'),
                    MockedQOr(owner=request.user),
                    MockedQOr(visibility=Problem.VISIBILITY_PUBLIC),
                ),
            ),
            call.all().filter().exclude(id__in='lesson problems'),
            call.all().filter().exclude().select_related('meta'),
            call.all().filter().exclude().select_related().order_by('id'),
            call.all().filter().exclude().select_related().order_by()
                .prefetch_related('resources')
        ]

        # проверяем с POST
        request.user.is_teacher = False
        request.user.is_content_manager = False
        request.method = 'POST'
        request.query_params = {}
        mocked_problems.reset_mock()

        (mocked_problems.all.return_value.filter.return_value
         .select_related.return_value.order_by.return_value) = 'qs'

        assert viewset.get_queryset() == 'qs'
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(
                MockedQOr(
                    MockedQOr(id__in='lesson problems'),
                    MockedQOr(owner=request.user),
                    MockedQOr(visibility=Problem.VISIBILITY_PUBLIC),
                ),
            ),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
        ]

        # пользователь контент-менеджер
        mocked_problems.reset_mock()
        (mocked_problems.all.return_value.filter.return_value
         .select_related.return_value.order_by.return_value) = 'qs'
        request.user.is_content_manager = True
        assert viewset.get_queryset() == 'qs'
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(owner__is_content_manager=True),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
        ]

        # пользователь учитель
        mocked_problems.reset_mock()
        request.user.is_content_manager = False
        request.user.is_teacher = True

        mocked_clessons = mocker.patch.object(CourseLessonLink, 'objects')
        mocked_clessons.filter.return_value.values_list.return_value = (
            'lessons')
        mocked_lproblems = mocker.patch.object(LessonProblemLink, 'objects')
        mocked_lproblems.filter.return_value.values_list.return_value = (
            'problems')
        (mocked_problems.all.return_value.filter.return_value.
         select_related.return_value.order_by.return_value) = 'teacher qs'
        teacher_q = MockedQOr(
            MockedQOr(
                course__owner=request.user,
                accessible_to_teacher__lt='timezone now',
            ),
            MockedQOr(
                copies__course__owner=request.user,
                copies__accessible_to_teacher__lt='timezone now',
            ),
            MockedQOr(
                course__free=True,
                accessible_to_teacher__lt='timezone now',
            ),
        )

        assert viewset.get_queryset() == 'teacher qs'
        assert mocked_clessons.mock_calls == [
            call.filter(teacher_q),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in='lessons'),
            call.filter().values_list('problem_id'),
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(
                MockedQOr(
                    MockedQOr(id__in='problems'),
                    MockedQOr(owner=request.user),
                    MockedQOr(visibility=Problem.VISIBILITY_PUBLIC),
                ),
            ),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
        ]

        # запрос с фильтрацией по курсу
        mocked_clessons.reset_mock()
        mocked_lproblems.reset_mock()
        mocked_problems.reset_mock()
        request.query_params = {'course': '5'}
        (mocked_clessons.filter.return_value.values_list
         .return_value) = 'course lessons'
        assert viewset.get_queryset() == 'teacher qs'
        assert mocked_clessons.mock_calls == [
            call.filter(
                teacher_q,
                MockedQOr(course_id='5'),
            ),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in='course lessons'),
            call.filter().values_list('problem_id')
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(MockedQOr(id__in='problems')),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
        ]

        # запрос с фильтрацией по занятию
        mocked_clessons.reset_mock()
        mocked_lproblems.reset_mock()
        mocked_problems.reset_mock()
        request.query_params = {'lesson': '7'}
        mocked_clessons.filter.return_value.values_list.return_value = [7]
        mocked_lproblems.filter.return_value.values_list.return_value = (
            'lesson problems')
        assert viewset.get_queryset() == 'teacher qs'
        assert mocked_clessons.mock_calls == [
            call.filter(
                teacher_q,
                MockedQOr(lesson_id='7'),
            ),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in=[7]),
            call.filter().values_list('problem_id')
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(MockedQOr(id__in='lesson problems')),
            call.all().filter().select_related('meta'),
            call.all().filter().select_related().order_by('id'),
        ]

        # исключение занятия из выдачи
        mocked_clessons.reset_mock()
        mocked_lproblems.reset_mock()
        mocked_problems.reset_mock()
        request.query_params = {'exclude_lesson': '7'}
        mocked_clessons.filter.return_value.values_list.return_value = [81]
        mocked_lproblems.filter.return_value.values_list.return_value = (
            'lesson problems')
        (mocked_problems.all.return_value.filter.return_value.exclude.
         return_value.select_related.return_value.
         order_by.return_value) = 'teacher exclude qs'
        assert viewset.get_queryset() == 'teacher exclude qs'
        assert mocked_clessons.mock_calls == [
            call.filter(teacher_q),
            call.filter().values_list('lesson_id'),
        ]
        assert mocked_lproblems.mock_calls == [
            call.filter(lesson_id__in=[81]),
            call.filter().values_list('problem_id'),
            call.filter(lesson_id='7', problem_id__isnull=False),
            call.filter().values_list('problem_id')
        ]
        assert mocked_problems.mock_calls == [
            call.all(),
            call.all().filter(
                MockedQOr(
                    MockedQOr(id__in='lesson problems'),
                    MockedQOr(owner=request.user),
                    MockedQOr(visibility=Problem.VISIBILITY_PUBLIC),
                ),
            ),
            call.all().filter().exclude(id__in='lesson problems'),
            call.all().filter().exclude().select_related('meta'),
            call.all().filter().exclude().select_related().order_by('id'),
        ]


class TestTextResourceViewSet(object):
    """
    Тесты API текстовых ресурсов
    """

    def test_get_serializer_class(self):
        """Тест определения класса сериализатора"""
        request = MagicMock()
        viewset = TextResourceViewSet()
        viewset.request = request

        # Запрос списка
        viewset.action = 'list'
        assert viewset.get_serializer_class() == TextResourceSerializer, (
            u'Нужно вернуть обычный сериализатор текстового ресурса')

        # Любой другой клиент запрашивает задачу
        viewset.action = 'retrieve'
        assert (viewset.get_serializer_class() ==
                TextResourceWithExpandedOwnerSerializer), (
            u'Нужно вернуть развёрнутый сериализатор текстового ресурса')
