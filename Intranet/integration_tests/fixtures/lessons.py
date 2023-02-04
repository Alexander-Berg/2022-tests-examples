import pytest

from kelvin.lessons.models import Lesson, LessonProblemLink


@pytest.fixture
def empty_lesson(some_owner):
    """
    Занятие без задач
    """
    return Lesson.objects.create(name=u'Занятие без задач', owner=some_owner)


@pytest.fixture
def lesson_models(some_owner, problem_models, theme_model):
    """Модель урока и сопутствующие модели"""
    problem1, problem2 = problem_models
    lesson = Lesson.objects.create(owner=some_owner, theme=theme_model,
                                   name=u'Урок')
    link1 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem1, order=1, type=1,
        options={'max_attempts': 5, 'show_tips': True},
    )
    link2 = LessonProblemLink.objects.create(
        block_id=1,
        lesson=lesson, problem=problem2, order=2, type=1,
        options={'max_attempts': 5, 'show_tips': True},
    )
    return lesson, problem1, problem2, link1, link2


@pytest.fixture
def lesson_with_problem_with_union(some_owner, problem_with_input_union,
                                   theme_model):
    """
    Модель урока в котором есть задача с группировкой инпутов
    """
    lesson = Lesson.objects.create(owner=some_owner, theme=theme_model,
                                   name=u'Урок')
    link1 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem_with_input_union, order=1, type=1,
        options={'max_attempts': 5, 'show_tips': True},
    )
    return lesson, problem_with_input_union, link1


@pytest.fixture
def lesson_with_theory_models(some_owner, lesson_models, theory_model,
                              theme_model):
    """
    Модель урока с двумя задачами и одной теорией
    """
    lesson, problem1, problem2, link1, link3 = lesson_models
    link3.order = 3
    link3.save()
    link2 = LessonProblemLink.objects.create(
        lesson=lesson, theory=theory_model, order=2, type=3,
        options={},
    )
    lesson.refresh_from_db()
    return lesson, problem1, theory_model, problem2, link1, link2, link3
