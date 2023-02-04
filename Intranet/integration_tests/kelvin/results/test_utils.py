from builtins import str
import pytest
from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import LessonProblemLink
from kelvin.problems.models import Problem
from kelvin.results.models import (
    CourseLessonResult, CourseLessonSummary, LessonResult, LessonSummary,
)
from kelvin.results.utils import (
    recalculate_lesson_result, recalculate_clesson_result,
)


@pytest.mark.django_db
def test_recalculate_lesson_result(lesson_result_models):
    """Тест пересчета результатов"""
    lesson_result, lesson, problem_link1, problem_link2 = lesson_result_models

    # изменяем число баллов за вопрос и правильный ответ
    problem_link1.problem.max_points = 1
    problem_link1.problem.save()
    problem_link2.problem.markup['answers']['1'] = [0, 1]
    problem_link2.problem.save()

    # Добавляем неправильную попытку после правильной для первой задачи
    lesson_result.answers[str(problem_link1.id)].append({
        'markers': {
            '1': {
                'answer_status': {'1': True},
                'user_answer': {'1': '6.0'},
                'mistakes': 1,
                'max_mistakes': 1,
            },
        },
        'theory': None,
        'mistakes': 1,
        'max_mistakes': 1,
        'completed': False,
        'spent_time': None,
        'points': 0,
        'checked_points': None,
        'comment': '',
        'answered': False,
    })

    # пересчитываем результаты
    recalculate_lesson_result(lesson_result)

    expected_answers = {
        str(problem_link1.id): [
            {
                'markers': {
                    '1': {
                        'answer_status': {'1': True},
                        'user_answer': {'1': '4.0'},
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 0,
                'max_mistakes': 1,
                'completed': False,
                'spent_time': None,
                'points': 1,
                'checked_points': None,
                'comment': '',
                'answered': False,
            },
        ],
        str(problem_link2.id): [
            {
                'markers': {
                    '1': {
                        'answer_status': [1, 1],
                        'user_answer': [0, 1],
                        'mistakes': 0,
                        'max_mistakes': 3,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 0,
                'max_mistakes': 3,
                'completed': True,
                'spent_time': None,
                'points': 1,
                'checked_points': None,
                'comment': '',
                'answered': False,
            },
        ],
    }
    lesson_result.refresh_from_db()
    assert lesson_result.answers == expected_answers, (
        u'Неправильно  проверены ответы')
    assert lesson_result.points == 2, u'Неправильно посчитаны баллы'
    assert lesson_result.max_points == 2, u'Неправильно посчитан максимум'


@pytest.mark.django_db
def test_recalculate_lesson_result_with_android_problem(lesson_result_models):
    """Тест пересчета результатов с андроидной непроверяемой задачей"""
    lesson_result, lesson, problem_link1, problem_link2 = lesson_result_models

    # изменяем число баллов за вопрос и правильный ответ
    problem_link1.problem.max_points = 1
    problem_link1.problem.save()
    problem_link2.problem.markup['answers']['1'] = [0, 1]

    # делаем маркер во второй задаче непроверяемым андроидным
    problem_link2.problem.markup['layout'][1]['content']['type'] = 'coreformbuilder'
    problem_link2.problem.save()

    # пересчитываем результаты
    recalculate_lesson_result(lesson_result)

    expected_answers = {
        str(problem_link1.id): [
            {
                'markers': {
                    '1': {
                        'answer_status': {'1': True},
                        'user_answer': {'1': '4.0'},
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 0,
                'max_mistakes': 1,
                'completed': False,
                'spent_time': None,
                'points': 1,
                'checked_points': None,
                'comment': '',
                'answered': False,
            },
        ],
        str(problem_link2.id): [
            {
                'markers': {
                    '1': {
                        'answer_status': [0, 1],
                        'user_answer': [0, 1],
                        'mistakes': 1,
                        'max_mistakes': 3,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 1,
                'max_mistakes': 3,
                'completed': True,
                'spent_time': None,
                'points': 0,
                'checked_points': None,
                'comment': '',
                'answered': False,
            },
        ],
    }
    lesson_result.refresh_from_db()
    assert lesson_result.answers == expected_answers, (
        u'Неправильно  проверены ответы')
    assert lesson_result.points == 1, u'Неправильно посчитаны баллы'
    assert lesson_result.max_points == 2, u'Неправильно посчитан максимум'


@pytest.mark.django_db
def test_recalculate_lesson_result_with_textandfile(lesson_models, student,
                                                    some_owner, subject_model):
    """Тест пересчета результатов, в которых есть маркер файла"""
    lesson, problem1, problem2, problem_link1, problem_link2 = lesson_models

    # добавляем в занятие задачу с непроверяемым маркером
    problem3 = Problem.objects.create(
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'Вычислите значение **выражения** $-0{,}6:  5  : ( -0{,}3 )$.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
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
                        'type': 'textandfile',
                        'id': 1,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': None
            }
        },
        owner=some_owner,
        subject=subject_model,
    )
    problem_link3 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem3, order=3)

    # создаем результат по занятию
    summary = LessonSummary.objects.create(
        student=student,
        lesson=lesson,
    )
    result = LessonResult.objects.create(
        summary=summary,
        max_points=60,
        points=None,
        answers={
            str(problem_link1.id): [
                {
                    'completed': True,
                    'spent_time': None,
                    'markers': {
                        '1': {
                            'user_answer': {'1': '3'},
                            'answer_status': {'1': False},
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'points': 0,
                    'answered': False,
                },
                {
                    'completed': True,
                    'spent_time': 17,
                    'markers': {
                        '1': {
                            'user_answer': {'1': '4'},
                            'answer_status': {'1': True},
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'points': 1,
                    'answered': False,
                },
            ],
            str(problem_link2.id): [
                {
                    'completed': True,
                    'spent_time': 43,
                    'markers': {
                        '1': {
                            'user_answer': [0],
                            'answer_status': [1],
                            'mistakes': 1,
                            'max_mistakes': 3,
                        },
                    },
                    'theory': None,
                    'mistakes': 1,
                    'max_mistakes': 3,
                    'points': 0,
                    'checked_points': 1,
                    'comment': u'Балл за попытку',
                    'answered': False,
                },
            ],
            str(problem_link3.id): [
                {
                    'completed': True,
                    'spent_time': 130,
                    'markers': {
                        '1': {
                            'user_answer': {
                                'text': u'Ответ',
                                'files': ['link1', 'link2'],
                            },
                            'answer_status': 1,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'points': 1,
                    'checked_points': 11,
                    'comment': u'Арифметическая ошибка',
                    'answered': False,
                },
            ],
        },
    )

    # изменяем правильные ответы
    problem_link1.problem.markup['answers']['1'] = '0'
    problem_link1.problem.markup['checks']['1']['1']['sources'][1]['source'] = '0'
    problem_link1.problem.save()
    problem_link2.problem.markup['answers']['1'] = [1]
    problem_link2.problem.save()

    # пересчитываем результаты
    recalculate_lesson_result(result)
    expected_answers = {
        str(problem_link1.id): [
            {
                'completed': True,
                'spent_time': None,
                'markers': {
                    '1': {
                        'user_answer': {'1': '3'},
                        'answer_status': {'1': False},
                        'mistakes': 1,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 1,
                'max_mistakes': 1,
                'points': None,  # баллы проставляются в последней попытке
                'checked_points': None,
                'comment': '',
                'answered': False,
            },
            {
                'completed': True,
                'spent_time': 17,
                'markers': {
                    '1': {
                        'user_answer': {'1': '4'},
                        'answer_status': {'1': False},
                        'mistakes': 1,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 1,
                'max_mistakes': 1,
                'points': 0,
                'checked_points': None,
                'comment': '',
                'answered': False,
            },
        ],
        str(problem_link2.id): [
            {
                'completed': True,
                'spent_time': 43,
                'markers': {
                    '1': {
                        'user_answer': [0],
                        'answer_status': [0],
                        'mistakes': 2,
                        'max_mistakes': 3,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 2,
                'max_mistakes': 3,
                'points': 0,
                'checked_points': 1,
                'comment': u'Балл за попытку',
                'answered': False,
            },
        ],
        str(problem_link3.id): [
            {
                'completed': True,
                'spent_time': 130,
                'markers': {
                    '1': {
                        'user_answer': {
                            'text': u'Ответ',
                            'files': ['link1', 'link2'],
                        },
                        'answer_status': 1,
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 0,
                'max_mistakes': 1,
                'points': 1,
                'checked_points': 11,
                'comment': u'Арифметическая ошибка',
                'answered': False,
            },
        ],
    }

    result.refresh_from_db()
    assert result.answers == expected_answers, u'Неправильно  проверены ответы'
    assert result.points == 12, u'Неправильно посчитаны баллы'
    assert result.max_points == 3, u'Неправильно посчитан максимум'


@pytest.mark.django_db
def test_recalculate_lesson_result_with_mixed_markers(
        lesson_models, student, some_owner):
    """
    Тест пересчета результатов, в которых есть маркер файла и маркер ввода в
    одном вопросе
    """
    lesson, problem1, problem2, problem_link1, problem_link2 = lesson_models

    # добавляем в первый вопрос маркер вопроса и меняем ответ на маркер ввода
    problem1.markup['layout'].append({
        'kind': 'marker',
        'content': {
            'id': 2,
            'type': 'textandfile',
            'options': {},
        },
    })
    problem1.markup['answers']['1'] = {'1': '3'}
    problem1.markup['answers']['2'] = None
    problem1.markup['checks']['1']['1']['sources'][1]['source'] = '3'
    problem1.save()

    # создаем результат ученика
    summary = LessonSummary.objects.create(
        student=student,
        lesson=lesson,
    )
    result = LessonResult.objects.create(
        summary=summary,
        max_points=60,
        points=None,
        answers={
            str(problem_link1.id): [
                {
                    'completed': True,
                    'spent_time': 17,
                    'markers': {
                        '1': {
                            'user_answer': {'1': '3'},
                            'answer_status': {'1': False},
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                        '2': {
                            'user_answer': {
                                'text': u'Ответ',
                                'files': ['link1', 'link2'],
                            },
                            'answer_status': 1,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'mistakes': 1,
                    'max_mistakes': 2,
                    'points': 0,
                    'checked_points': 11,
                    'comment': u'Арифметическая ошибка',
                    'answered': True,
                },
            ],
            str(problem_link2.id): [
                {
                    'completed': True,
                    'spent_time': 43,
                    'markers': {
                        '1': {
                            # неправильные статусы, будут пересчитаны
                            'user_answer': [0],
                            'answer_status': [1],
                            'mistakes': 1,
                            'max_mistakes': 3,
                        },
                    },
                    'theory': None,
                    'mistakes': 1,
                    'max_mistakes': 3,
                    'points': 0,
                    'checked_points': 1,
                    'comment': u'Балл за попытку',
                    'answered': False,
                },
            ],
        },
    )

    # пересчитываем результаты
    recalculate_lesson_result(result)
    expected_answers = {
        str(problem_link1.id): [
            {
                'completed': True,
                'spent_time': 17,
                'markers': {
                    '1': {
                        'user_answer': {'1': '3'},
                        'answer_status': {'1': True},
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                    '2': {
                        'user_answer': {
                            'text': u'Ответ',
                            'files': ['link1', 'link2'],
                        },
                        'answer_status': 1,
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 0,
                'max_mistakes': 2,
                'points': 1,
                'checked_points': 11,
                'comment': u'Арифметическая ошибка',
                'answered': True,
            },
        ],
        str(problem_link2.id): [
            {
                'completed': True,
                'spent_time': 43,
                'markers': {
                    '1': {
                        'user_answer': [0],
                        'answer_status': [0],
                        'mistakes': 3,
                        'max_mistakes': 3,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 3,
                'max_mistakes': 3,
                'points': 0,
                'checked_points': 1,
                'comment': u'Балл за попытку',
                'answered': False,
            },
        ],
    }

    result.refresh_from_db()
    assert result.answers == expected_answers, u'Неправильно  проверены ответы'
    assert result.points == 12, u'Неправильно посчитаны баллы'
    assert result.max_points == 2, u'Неправильно посчитан максимум'


@pytest.mark.django_db
def test_recalculate_clesson_result_with_mixed_markers(
        lesson_models, student, subject_model, teacher):
    """
    Тест пересчета результатов по курсозанятию, в которых есть маркер файла и
    маркер ввода в одном вопросе
    """
    lesson, problem1, problem2, problem_link1, problem_link2 = lesson_models

    course = Course.objects.create(
        name=u'Тестовый курс',
        subject=subject_model,
        owner=teacher,
        color='#abcabc',
    )
    clesson = CourseLessonLink.objects.create(
        lesson=lesson,
        course=course,
        order=1,
        accessible_to_teacher=timezone.now(),
    )

    # добавляем в первый вопрос маркер вопроса и меняем ответ на маркер ввода
    problem1.markup['layout'].append({
        'kind': 'marker',
        'content': {
            'id': 2,
            'type': 'textandfile',
            'options': {},
        }
    })
    problem1.markup['answers']['1'] = {'1': '3'}
    problem1.markup['answers']['2'] = None
    problem1.markup['checks']['1']['1']['sources'][1]['source'] = '3'
    problem1.save()

    # создаем результат ученика
    summary = CourseLessonSummary.objects.create(
        student=student,
        clesson=clesson,
    )
    result = CourseLessonResult.objects.create(
        summary=summary,
        max_points=60,
        points=None,
        answers={
            str(problem_link1.id): [
                {
                    'completed': True,
                    'spent_time': 17,
                    'markers': {
                        '1': {
                            'user_answer': {'1': '3'},
                            'answer_status': {'1': False},
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                        '2': {
                            'user_answer': {
                                'text': u'Ответ',
                                'files': ['link1', 'link2'],
                            },
                            'answer_status': 1,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'mistakes': 1,
                    'max_mistakes': 2,
                    'points': 0,
                    'checked_points': 11,
                    'comment': u'Арифметическая ошибка',
                    'answered': True,
                },
            ],
            str(problem_link2.id): [
                {
                    'completed': True,
                    'spent_time': 43,
                    'markers': {
                        '1': {
                            # неправильные статусы, будут пересчитаны
                            'user_answer': [0],
                            'answer_status': [1],
                            'mistakes': 1,
                            'max_mistakes': 3,
                        },
                    },
                    'theory': None,
                    'mistakes': 1,
                    'max_mistakes': 3,
                    'points': 0,
                    'checked_points': 1,
                    'comment': u'Балл за попытку',
                    'answered': False,
                },
            ],
        },
    )

    # пересчитываем результаты
    recalculate_clesson_result(result)
    expected_answers = {
        str(problem_link1.id): [
            {
                'completed': True,
                'spent_time': 17,
                'markers': {
                    '1': {
                        'user_answer': {'1': '3'},
                        'answer_status': {'1': True},
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                    '2': {
                        'user_answer': {
                            'text': u'Ответ',
                            'files': ['link1', 'link2'],
                        },
                        'answer_status': 1,
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 0,
                'max_mistakes': 2,
                'points': 1,
                'checked_points': 11,
                'comment': u'Арифметическая ошибка',
                'answered': True,
            },
        ],
        str(problem_link2.id): [
            {
                'completed': True,
                'spent_time': 43,
                'markers': {
                    '1': {
                        'user_answer': [0],
                        'answer_status': [0],
                        'mistakes': 3,
                        'max_mistakes': 3,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'mistakes': 3,
                'max_mistakes': 3,
                'points': 0,
                'checked_points': 1,
                'comment': u'Балл за попытку',
                'answered': False,
            },
        ],
    }

    result.refresh_from_db()
    assert result.answers == expected_answers, u'Неправильно  проверены ответы'
    assert result.points == 12, u'Неправильно посчитаны баллы'
    assert result.max_points == 2, u'Неправильно посчитан максимум'

    # Создаем назначение и выставляем в нем только первую задачу
    assignment = LessonAssignment.objects.create(
        student=student, clesson=clesson, problems=[problem_link1.id])

    # Изменяем ответ на вторую задачу - результат при этом не должен измениться
    problem2.markup['answers']['1'] = [0]
    problem2.save()

    # пересчитываем результаты
    recalculate_clesson_result(result)
    result.refresh_from_db()

    assert result.answers == expected_answers, u'Неправильно  проверены ответы'
    assert result.points == 11, u'Неправильно посчитаны баллы'
    assert result.max_points == 1, u'Неправильно посчитан максимум'
