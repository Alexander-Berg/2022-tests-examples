from builtins import str
import datetime
import pytest
from uuid import uuid4
from django.contrib.auth import get_user_model
from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink, ProgressIndicator
from kelvin.lessons.models import (
    LessonScenario, Lesson, LessonProblemLink,
)
from kelvin.problem_meta.models import ProblemMeta
from kelvin.problems.models import Problem
from kelvin.resources.models import Resource
from kelvin.subjects.models import Theme
from django.conf import settings

User = get_user_model()


@pytest.fixture
def problem_models(some_owner, subject_model):
    """Модели вопросов"""
    problem1 = Problem.objects.create(
        markup={
            'public_solution': u'Решение для учеников',
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
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
                }
            ],
            'checks': {},
            'answers': {
                '1': 4
            },
            'solution': u'Решение для учителей'
        },
        owner=some_owner,
        subject=subject_model,
    )
    problem2 = Problem.objects.create(
        markup={
            'public_solution': u'Решение для учеников',
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'choice',
                        'id': 1,
                        'options': {
                            'type_content': 'number',
                            'choices': [
                                u'Брежнев',
                                u'Горбачев',
                                u'Ленин'
                            ]
                        }
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': [
                    1,
                    2
                ]
            },
            'solution': u'Решение для учителей'
        },
        owner=some_owner,
        subject=subject_model,
    )
    return problem1, problem2


@pytest.fixture
def clesson_models_no_indicator(problem_models, subject_model):
    """Модель связи курса и занятия и связанные с ней"""
    problem1, problem2 = problem_models
    theme = Theme.objects.create(id=1, name='theme', code='thm',
                                 subject=subject_model)
    teacher = User.objects.create(email='1@1.w', is_teacher=True,
                                  username='1')
    course = Course.objects.create(
        name=u'Новый спец курс',
        subject=subject_model,
        owner=teacher,
        id=1,
    )
    lesson1 = Lesson.objects.create(
        owner=teacher,
        theme=theme,
        name=u'Занятие 1',
    )
    lesson2 = Lesson.objects.create(
        owner=teacher,
        theme=theme,
        name=u'Занятие 2',
    )
    now = timezone.now()
    clesson1 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson1,
        order=1,
        accessible_to_teacher=now,
    )
    clesson2 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson2,
        order=2,
        accessible_to_teacher=now,
        date_assignment=datetime.datetime(
            year=2010, month=10, day=29, hour=10, minute=29, second=2,
            tzinfo=timezone.utc,
        ),
        evaluation_date=datetime.datetime(
            year=2019, month=10, day=29, hour=10, minute=29, second=2,
            tzinfo=timezone.utc,
        ),
        visual_mode=LessonScenario.VisualModes.BLOCKS,
    )
    clesson3 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson1, 
        order=3,
        accessible_to_teacher=None,
    )

    lesson1_problem1 = LessonProblemLink.objects.create(
        lesson=lesson1,
        problem=problem1,
        order=1,
        type=1,
        options={'max_attempts': 5, 'show_tips': True},
    )
    lesson1_problem2 = LessonProblemLink.objects.create(
        lesson=lesson1,
        problem=problem2,
        order=2,
        type=1,
        options={'max_attempts': 5, 'show_tips': True},
    )
    course.refresh_from_db()
    lesson1.refresh_from_db()
    clesson1.refresh_from_db()
    return (course, [clesson1, clesson2, clesson3], lesson1_problem1,
            lesson1_problem2, teacher, subject_model, theme)


@pytest.fixture
def clesson_models(problem_models, subject_model, progress_indicator):
    """Модель связи курса и занятия и связанные с ней"""
    problem1, problem2 = problem_models
    theme = Theme.objects.create(id=1, name='theme', code='thm',
                                 subject=subject_model)
    teacher = User.objects.create(email='1@1.w', is_teacher=True,
                                  username='1')
    course = Course.objects.create(
        name=u'Новый спец курс',
        subject=subject_model,
        owner=teacher,
        id=1,
        progress_indicator=progress_indicator,
    )
    lesson1 = Lesson.objects.create(
        owner=teacher,
        theme=theme,
        name=u'Занятие 1',
    )
    lesson2 = Lesson.objects.create(
        owner=teacher,
        theme=theme,
        name=u'Занятие 2',
    )
    now = timezone.now()
    clesson1 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson1,
        order=1,
        accessible_to_teacher=now,
        progress_indicator=progress_indicator,
    )
    clesson2 = CourseLessonLink.objects.create(
        course=course, lesson=lesson2, order=2, accessible_to_teacher=now,
        date_assignment=datetime.datetime(year=2010, month=10, day=29, hour=10,
                                          minute=29, second=2,
                                          tzinfo=timezone.utc,),
        evaluation_date=datetime.datetime(year=2019, month=10, day=29, hour=10,
                                          minute=29, second=2,
                                          tzinfo=timezone.utc,),
        visual_mode=LessonScenario.VisualModes.BLOCKS,
    )
    clesson3 = CourseLessonLink.objects.create(
        course=course, lesson=lesson1, order=3, accessible_to_teacher=None)

    lesson1_problem1 = LessonProblemLink.objects.create(
        lesson=lesson1, problem=problem1, order=1, type=1, options={
            'max_attempts': 5, 'show_tips': True},
    )
    lesson1_problem2 = LessonProblemLink.objects.create(
        lesson=lesson1, problem=problem2, order=2, type=1, options={
            'max_attempts': 5, 'show_tips': True},
    )
    course.refresh_from_db()
    lesson1.refresh_from_db()
    clesson1.refresh_from_db()
    return (course, [clesson1, clesson2, clesson3], lesson1_problem1,
            lesson1_problem2, teacher, subject_model, theme)


@pytest.fixture
def clesson_with_theory_models(clesson_models, theory_model):
    """Модель связи курса и занятия и связанные с ней"""
    (course, clessons, lesson1_link1,
     lesson1_link3, teacher, subject_model, theme) = clesson_models
    lesson1_link2 = LessonProblemLink.objects.create(
        lesson_id=clessons[0].lesson_id, theory=theory_model, order=2, type=3, options={},
    )
    lesson1_link3.order = 3
    lesson1_link3.save()

    # после добавления задачи в занятие, обновляются занятия и курс
    course.refresh_from_db()
    clessons[0].refresh_from_db()
    clessons[0].lesson.refresh_from_db()
    return (course, clessons, [lesson1_link1,
            lesson1_link2, lesson1_link3], teacher, subject_model, theme)


@pytest.fixture
def progress_indicator():
    palette = """[
            {
                "progress": 0,
                "color": "#000000"
            },
            {
                "progress": 100,
                "color": "#FFFFFF"
            }
        ]
        """
    return ProgressIndicator.objects.create(
        slug='awesome gradient',
        palette=palette
    )


@pytest.fixture
def source_course(subject_model, content_manager):
    """Модель курса-источника-книги с обложкой"""
    resource = Resource.objects.create(
        file='./image.png',
        name=u'изображение',
        shortened_file_url=settings.SWITCHMAN_URL + '/' + str(uuid4()),
    )
    return Course.objects.create(
        name=u'Новая книга',
        cover=resource,
        subject=subject_model,
        owner=content_manager,
        author=u'Автор книги',
        mode=Course.BOOK_COURSE,
    )


@pytest.fixture
def meta_models(problem_models, clesson_models_no_indicator):
    """
    Модель связи курса и занятия и связанные с ней, включая meta problem
    """
    (
        course,
        [clesson1, clesson2, clesson3],
        lesson1_problem1,
        lesson1_problem2,
        teacher,
        subject_model,
        theme,
    ) = clesson_models_no_indicator

    meta = ProblemMeta.objects.create(
        difficulty=ProblemMeta.DIFFICULTY_LIGHT,
        main_theme=theme,
    )

    for problem in problem_models:
        problem.meta = meta
        problem.save()

    return {
        'lesson1_problem1': lesson1_problem1,
        'lesson1_problem2': lesson1_problem2,
        'course': course,
        'clesson1': clesson1,
        'clesson2': clesson2,
        'clesson3': clesson3,
        'theme': theme,
        'meta': meta,
        'problems': problem_models,
        'subject': subject_model,
        'teacher': teacher,
    }
