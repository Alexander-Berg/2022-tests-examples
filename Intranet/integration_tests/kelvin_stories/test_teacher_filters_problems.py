from builtins import object
import pytest

from django.core.urlresolvers import reverse
from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink
from kelvin.group_levels.models import GroupLevel
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problem_meta.models import ProblemMeta
from kelvin.problems.models import Problem
from kelvin.subjects.models import Subject


@pytest.fixture
def problems_in_courses(teacher, second_teacher, some_owner):
    """
    Модели задач в курсах
    """
    # FIXME где создается этот предмет?
    subject, __ = Subject.objects.get_or_create(
        name=u'Математика',
        slug='mathematics',
    )
    group_level1 = GroupLevel.objects.create(
        name=u'1 класс',
        baselevel=1,
        slug='slug1'
    )
    group_level2 = GroupLevel.objects.create(
        name=u'2 класс',
        baselevel=1,
        slug='slug2'
    )

    # курс учителя
    # own_course
    #   - lesson1
    #     - problem1
    #     - problem2
    #   - lesson2
    #     - problem2
    #     - problem3
    problem1 = Problem.objects.create(
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'У капитана Флинта есть только '
                                u'изображенные ниже гири. Помоги '
                                u'ему уравновесить чаши весов. Используй '
                                u'наименьшее возможное число гирь.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                }
            ],
            'checks': {},
            'answers': {}
        },
        owner=some_owner,
        meta=ProblemMeta.objects.create(
            difficulty=1,
        ),
        subject=subject,
    )
    problem1.meta.group_levels = [group_level1]
    problem2 = Problem.objects.create(
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'У капитана Джека есть только изображенные '
                                u'ниже гири. Помоги ему уравновесить чаши '
                                u'весов. Используй наименьшее '
                                u'возможное число гирь.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                }
            ],
            'checks': {},
            'answers': {}
        },
        owner=teacher,
        meta=ProblemMeta.objects.create(
            difficulty=2,
        ),
        subject=subject,
    )
    problem2.meta.group_levels = [group_level1, group_level2]
    problem3 = Problem.objects.create(
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'У капитана Джека есть только изображенные '
                                u'ниже гири. Помоги ему уравновесить чаши '
                                u'весов. Используй наименьшее '
                                u'возможное число гирь.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                }
            ],
            'checks': {},
            'answers': {}
        },
        owner=some_owner,
        meta=ProblemMeta.objects.create(
            difficulty=3,
        ),
        subject=subject,
    )
    problem3.meta.group_levels = [group_level2]
    own_course = Course.objects.create(
        owner=teacher,
        name=u'Курс учителя',
        subject=subject,
    )
    lesson1 = Lesson.objects.create(
        owner=teacher,
    )
    lesson2 = Lesson.objects.create(
        owner=teacher,
    )
    now = timezone.now()
    CourseLessonLink.objects.create(
        course=own_course, lesson=lesson1, order=1, accessible_to_teacher=now)
    CourseLessonLink.objects.create(
        course=own_course, lesson=lesson2, order=2, accessible_to_teacher=now)
    LessonProblemLink.objects.create(
        lesson=lesson1, problem=problem1, order=1)
    LessonProblemLink.objects.create(
        lesson=lesson1, problem=problem2, order=2)
    LessonProblemLink.objects.create(
        lesson=lesson2, problem=problem2, order=1)
    LessonProblemLink.objects.create(
        lesson=lesson2, problem=problem3, order=2)

    # курс другого учителя
    # other_course
    #   - lesson3
    #     - problem4
    problem4 = Problem.objects.create(
        markup={},
        owner=second_teacher,
        meta=ProblemMeta.objects.create(
            difficulty=1,
        ),
        subject=subject,
    )
    problem4.meta.group_levels = [group_level1, group_level2]
    lesson3 = Lesson.objects.create(
        owner=second_teacher,
    )
    other_course = Course.objects.create(
        owner=second_teacher,
        name=u'Курс другого учителя',
        subject=subject,
    )
    CourseLessonLink.objects.create(
        course=other_course, lesson=lesson3, order=1,
        accessible_to_teacher=now,
    )
    LessonProblemLink.objects.create(
        lesson=lesson3, problem=problem4, order=1)

    # бесплатный курс
    # free_course
    #   - lesson4
    #     - problem5
    problem5 = Problem.objects.create(
        markup={},
        owner=some_owner,
        meta=ProblemMeta.objects.create(
            difficulty=1,
        ),
        subject=subject,
    )
    lesson4 = Lesson.objects.create(
        owner=some_owner,
    )
    free_course = Course.objects.create(
        owner=some_owner,
        name=u'Бесплатный курс',
        subject=subject,
        free=True,
    )
    CourseLessonLink.objects.create(
        course=free_course, lesson=lesson4, order=1, accessible_to_teacher=now)
    LessonProblemLink.objects.create(
        lesson=lesson4, problem=problem5, order=1)

    # просто задача вне курсов
    problem6 = Problem.objects.create(
        markup={},
        owner=teacher,
        meta=ProblemMeta.objects.create(
            difficulty=1,
        ),
        subject=subject,
    )

    # TODO Тесты доступности для скопированных курсов

    return (
        [problem1, problem2, problem3, problem4, problem5, problem6],
        [group_level1, group_level2],
        [own_course, free_course, other_course],
    )


@pytest.mark.django_db
class TestTeacherFiltersProblems(object):
    """
    Тесты списка задач для учителя с возможностью фильтрации
    """
    @pytest.mark.parametrize('version', ['v2'])
    def test_no_filter(self, jclient, teacher, problems_in_courses, version):
        """
        Без фильтрации
        """
        problems, __, __ = problems_in_courses
        list_url = reverse('{0}:problem-list'.format(version))
        jclient.login(user=teacher)
        response = jclient.get(list_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 5
        assert [problem['id'] for problem in answer['results']] == [
            problems[0].id, problems[1].id, problems[2].id, problems[4].id,
            problems[5].id,
        ]

    @pytest.mark.parametrize('version', ['v2'])
    def test_filter_difficulty(self, jclient, teacher, problems_in_courses,
                               version):
        """
        Фильтрация по сложности
        """
        problems, __, __ = problems_in_courses
        list_url = reverse('{0}:problem-list'.format(version))
        jclient.login(user=teacher)
        response = jclient.get(list_url, data={'difficulty': 1})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 3
        assert [problem['id'] for problem in answer['results']] == [
            problems[0].id, problems[4].id, problems[5].id,
        ]

    @pytest.mark.parametrize('version', ['v2'])
    def test_filter_group_levels(self, jclient, teacher, problems_in_courses,
                                 version):
        """
        Фильтрация по классам
        """
        problems, group_levels, __ = problems_in_courses
        list_url = reverse('{0}:problem-list'.format(version))
        jclient.login(user=teacher)

        response = jclient.get(list_url + '?group_levels={0}'.format(
            group_levels[0].id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 2
        assert [problem['id'] for problem in answer['results']] == [
            problems[0].id, problems[1].id,
        ]

        response = jclient.get(
            list_url + '?group_levels={0}&group_levels={1}'.format(
                group_levels[0].id, group_levels[1].id)
        )
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 3
        assert [problem['id'] for problem in answer['results']] == [
            problems[0].id, problems[1].id, problems[2].id,
        ]

    @pytest.mark.parametrize('version', ['v2'])
    def test_filter_markup(self, jclient, teacher, problems_in_courses,
                           version):
        """
        Фильтрация по разметке
        """
        problems, group_levels, __ = problems_in_courses
        list_url = reverse('{0}:problem-list'.format(version))
        jclient.login(user=teacher)

        response = jclient.get(list_url, data={'markup': u'джек'})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 2
        assert [problem['id'] for problem in answer['results']] == [
            problems[1].id, problems[2].id,
        ]

    @pytest.mark.parametrize('version', ['v2'])
    def test_filter_course(self, jclient, teacher, problems_in_courses,
                           version):
        """
        Фильтрация по курсу
        """
        problems, group_levels, courses = problems_in_courses
        list_url = reverse('{0}:problem-list'.format(version))
        jclient.login(user=teacher)

        # запрос задач из курса
        response = jclient.get(list_url, data={'course': courses[0].id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 3
        assert [problem['id'] for problem in answer['results']] == [
            problems[0].id, problems[1].id, problems[2].id,
        ]

        # запрос из недоступного курса
        response = jclient.get(list_url, data={'course': courses[2].id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 0
        assert [problem['id'] for problem in answer['results']] == []

    @pytest.mark.parametrize('version', ['v2'])
    def test_filter_lesson(self, jclient, teacher, problems_in_courses,
                           version):
        """
        Фильтрация по занятию
        """
        problems, group_levels, courses = problems_in_courses
        list_url = reverse('{0}:problem-list'.format(version))
        jclient.login(user=teacher)

        # запрос задач из доступного занятия
        lesson2 = (
            courses[0].courselessonlink_set.all().order_by('order')[1].lesson)
        response = jclient.get(list_url, data={'lesson': lesson2.id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 2
        assert [problem['id'] for problem in answer['results']] == [
            problems[1].id, problems[2].id,
        ]

        # запрос задач из недоступного занятия
        lesson3 = courses[2].lessons.all()[0]  # в курсе одно занятие
        response = jclient.get(list_url, data={'lesson': lesson3.id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 0
        assert [problem['id'] for problem in answer['results']] == []

    @pytest.mark.parametrize('version', ['v2'])
    def test_filter_mine(self, jclient, teacher, problems_in_courses, version):
        """
        Фильтрация своих задач (с учетом доступности для учителя)
        """
        problems, group_levels, courses = problems_in_courses
        list_url = reverse('{0}:problem-list'.format(version))
        jclient.login(user=teacher)
        response = jclient.get(list_url, data={'mine': 1})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['count'] == 2
        assert [problem['id'] for problem in answer['results']] == [
            problems[1].id, problems[5].id]
