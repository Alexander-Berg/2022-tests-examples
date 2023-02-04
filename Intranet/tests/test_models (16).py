from builtins import object

import pytest
from mock import MagicMock, call

from django.contrib.auth import get_user_model

from kelvin.courses.models import CourseLessonLink, CourseStudent
from kelvin.lesson_assignments.models import LessonAssignment  # ensure_clesson_assignments_on_save,
from kelvin.lesson_assignments.services import ensure_clesson_assignments

User = get_user_model()


class TestLessonAssignmentModel(object):
    """
    Тесты методов работы с моделью назначений занятия
    """
    def test_get_student_problems(self, mocker):
        """
        Тест получения задач одного ученика
        """
        mocked_objects = mocker.patch.object(LessonAssignment, 'objects')

        assignment1 = LessonAssignment(
            student_id=1,
            clesson_id=1,
            problems=[1, 2, 3],
        )
        assignment1.student_id = 1
        assignment2 = LessonAssignment(problems=[2, 3, 5])
        assignment2.student_id = 2

        user = User(id=1)
        clesson = MagicMock()

        LessonAssignment.get_student_problems(user, clesson)

        assert mocked_objects.mock_calls == [
            call.get(student=user, clesson=clesson)
        ]

    @pytest.mark.parametrize(
        'assigned_problems,problem_links_params,is_valid,result',
        (
            (
                [],
                [
                    {'id': 1, 'group': 1},
                    {'id': 2, 'group': 1},
                    {'id': 3, 'group': 2},
                    {'id': 4, 'group': 2},
                    {'id': 5, 'group': 2},
                ],
                False,
                [
                    [1, 2],
                    [3, 4, 5],
                ],
            ),
            (
                [1, 2],
                [
                    {'id': 1, 'group': 1},
                    {'id': 2, 'group': 1},
                    {'id': 3, 'group': 2},
                    {'id': 4, 'group': 2},
                    {'id': 5, 'group': 2},
                ],
                False,
                [
                    [1, 2],
                    [3, 4, 5],
                ],
            ),
            (
                [2, 5],
                [
                    {'id': 1, 'group': 1},
                    {'id': 2, 'group': 1},
                    {'id': 3, 'group': 2},
                    {'id': 4, 'group': 2},
                    {'id': 5, 'group': 2},
                ],
                True,
                [
                    [1, 2],
                    [3, 4, 5],
                ],
            ),
            (
                [2, 5],
                [
                    {'id': 1, 'group': 1},
                    {'id': 2, 'group': 1},
                    {'id': 3, 'group': 2},
                    {'id': 4, 'group': 2},
                    {'id': 5, 'group': 2},
                    {'id': 6, 'group': None},
                ],
                False,
                [
                    [6],  # особенность алгоритма - первый элемент без вариации
                    [1, 2],
                    [3, 4, 5],
                ],
            ),
            (
                [2, 5, 6],
                [
                    {'id': 1, 'group': 1},
                    {'id': 2, 'group': 1},
                    {'id': 3, 'group': 2},
                    {'id': 4, 'group': 2},
                    {'id': 5, 'group': 2},
                    {'id': 6, 'group': None},
                ],
                True,
                [
                    [1, 2],
                    [3, 4, 5],
                    [6],
                ],
            ),
        ),
    )
    def test_ensure_variations(self, assigned_problems, problem_links_params,
                               is_valid, result):
        """
        Тест проверки назначений согласно вариациям
        """
        if assigned_problems is not None:
            assignment = LessonAssignment(
                student=User(username='user'),
                clesson=CourseLessonLink(),
                problems=assigned_problems,
            )
        problem_links = [
            MagicMock(**params) for params in problem_links_params
        ]

        assert assignment.ensure_variations(problem_links) is is_valid
        assert len(assignment.problems) == len(result)
        for i, problem_link_id in enumerate(assignment.problems):
            assert problem_link_id in result[i]


@pytest.mark.django_db
@pytest.mark.parametrize('assignments,result', (
    # первом занятии 2 группы и 2 задачи без группы
    #   первая группа: 0, 3, 6
    #   вторая группа: 1, 4, 7
    #   без группы: 2, 5
    # во втором занятии делаем 1 группу из 3 задач
    # в третьем занятии 3 задачи и нет вариаций
    (
        [],
        2,
    ),
    (
        [
            [0, 2, 5, 7],
        ],
        1,
    ),
    (
        [
            [0, 2, 5, 7],
            [2],
        ],
        0,
    ),
    (
        [
            [0, 2, 5, 7],
            [2],
            [0, 1],
        ],
        0,
    ),
    (
        [
            [0, 1, 2, 3, 5, 7],
            [1, 2],
            [0, 1],
        ],
        2,
    ),
))
def test_ensure_student_assignments(student, student2, assignments, result,
                                    course_with_lesson_variations):
    """
    Проверяет правильность выставления назначений для ученика и курса в
    занятиях с вариативностью
    """
    data = course_with_lesson_variations
    course = data['course']
    clessons = data['clessons']
    all_problem_links = data['problem_links']

    # назначения первого ученика
    for i, assigned_problems in enumerate(assignments):
        if assigned_problems is None:
            continue
        clesson = clessons[i]
        problem_links = all_problem_links[clesson.lesson.id]
        LessonAssignment.objects.create(
            student=student,
            clesson=clesson,
            problems=[
                problem_links[j].id for j in assigned_problems
            ],
        )

    # назначение второго ученика для проверки, что оно не пересчитывается
    student2_assignment = LessonAssignment.objects.create(
        student=student2,
        clesson=clessons[0],
        problems=[1],
    )

    assert LessonAssignment.ensure_student_assignments(
        course, student,
    ) == result

    # проверка назначения на первое занятие
    assignment = LessonAssignment.objects.get(
        student=student, clesson=clessons[0])
    problem_links = all_problem_links[clessons[0].lesson.id]
    assert len(assignment.problems) == 4
    problems_set = set(assignment.problems)
    assert problem_links[2].id in problems_set
    assert problem_links[5].id in problems_set
    assert (
        problem_links[0].id in problems_set or
        problem_links[3].id in problems_set or
        problem_links[6].id in problems_set
    )
    assert (
        problem_links[1].id in problems_set or
        problem_links[4].id in problems_set or
        problem_links[7].id in problems_set
    )

    # проверка назначения во втором занятии
    assignment = LessonAssignment.objects.get(
        student=student, clesson=clessons[1])
    problem_links = all_problem_links[clessons[1].lesson.id]
    assert len(assignment.problems) == 1
    assigned_problem = assignment.problems[0]
    assert (
        problem_links[0].id == assigned_problem or
        problem_links[1].id == assigned_problem or
        problem_links[2].id == assigned_problem
    )

    # проверка назначения на третье занятие
    if len(assignments) >= 3:
        assignment = LessonAssignment.objects.get(
            student=student, clesson=clessons[2])
        problem_links = all_problem_links[clessons[2].lesson.id]
        assert assignment.problems == [
            problem_links[i].id for i in assignments[2]
        ]
    else:
        assert LessonAssignment.objects.filter(
            student=student, clesson=clessons[2]).count() == 0

    # проверка неизменноти назначения второго ученика
    student2_assignment.refresh_from_db()
    assert student2_assignment.problems == [1]


@pytest.mark.django_db
def test_ensure_clesson_assignments(student, student2,
                                    course_with_lesson_variations):
    """
    Проверяет правильность выставления назначений для учеников в курсозанятии
    с вариативностью
    """
    data = course_with_lesson_variations
    course = data['course']
    clessons = data['clessons']
    all_problem_links = data['problem_links']

    CourseStudent.objects.create(
        course=course,
        student=student,
    )
    CourseStudent.objects.create(
        course=course,
        student=student2,
    )
    problem_links = all_problem_links[clessons[1].lesson.id]
    LessonAssignment.objects.create(
        student=student,
        clesson=clessons[1],
        problems=[problem_links[0].id, problem_links[1].id]
    )

    ensure_clesson_assignments(clessons[1])

    assignment = LessonAssignment.objects.get(student=student,
                                              clesson=clessons[1])
    assert len(assignment.problems) == 1
    assert assignment.problems[0] in [
        problem_links[0].id,
        problem_links[1].id,
        problem_links[2].id,
    ]
    assignment = LessonAssignment.objects.get(student=student2,
                                              clesson=clessons[1])
    assert len(assignment.problems) == 1
    assert assignment.problems[0] in [
        problem_links[0].id,
        problem_links[1].id,
        problem_links[2].id,
    ]


def test_ensure_clesson_assignments_mocked(mocker):
    """
    Тест проверки назначений в занятии у учеников курса
    """
    clesson = MagicMock(course_id=1)
    (clesson.lesson.lessonproblemlink_set.filter.return_value
     .exists.return_value) = True
    (clesson.lesson.lessonproblemlink_set.all.return_value
     .order_by.return_value) = 'problem_links'
    mocked_transaction = mocker.patch('kelvin.lesson_assignments.services.transaction')
    mocked_course_student_objects = mocker.patch.object(CourseStudent, 'objects')
    (mocked_course_student_objects.filter.return_value
     .values_list.return_value) = [10, 20, 30]
    mocked_assignments = mocker.patch(
        'kelvin.lesson_assignments.services.LessonAssignment')
    assignment1 = MagicMock(id=10, student_id=10)
    assignment1.ensure_variations.return_value = True
    assignment2 = MagicMock(id=20, student_id=20)
    assignment2.ensure_variations.return_value = False
    assignment3 = MagicMock(id=None, student_id=None)
    assignment3.ensure_variations.return_value = False

    assignments_queryset = MagicMock()
    mocked_assignments.objects.filter.side_effect = (
        [assignment1, assignment2],
        assignments_queryset,
    )
    mocked_assignments.return_value = assignment3

    assert ensure_clesson_assignments(clesson) == 2

    assert clesson.mock_calls == [
        call.lesson.lessonproblemlink_set.filter(group__isnull=False),
        call.lesson.lessonproblemlink_set.filter().exists(),
        call.lesson.lessonproblemlink_set.all(),
        call.lesson.lessonproblemlink_set.all().order_by('group'),
    ]
    assert mocked_transaction.method_calls == [call.atomic()]
    assert mocked_course_student_objects.mock_calls == [
        call.filter(course=1),
        call.filter().values_list('student_id', flat=True),
    ]
    assert assignments_queryset.mock_calls == [call.delete()]
    assert mocked_assignments.mock_calls == [
        call.objects.filter(clesson=clesson),
        call(clesson=clesson, problems=[], student_id=30),
        call.objects.filter(id__in=[20]),
        call.objects.bulk_create([assignment3, assignment2]),
    ]
    assert assignment3.method_calls == [call.ensure_variations('problem_links')]

    # случай, когда в занятии нет вариаций
    clesson.reset_mock()
    mocked_transaction.reset_mock()
    mocked_course_student_objects.reset_mock()
    assignments_queryset.reset_mock()
    mocked_assignments.reset_mock()
    (clesson.lesson.lessonproblemlink_set.filter.return_value
     .exists.return_value) = False
    assert ensure_clesson_assignments(clesson) is None
    assert clesson.mock_calls == [
        call.lesson.lessonproblemlink_set.filter(group__isnull=False),
        call.lesson.lessonproblemlink_set.filter().exists(),
    ]
    assert assignments_queryset.mock_calls == []
    assert mocked_transaction.mock_calls == []
    assert mocked_assignments.mock_calls == []


# def test_ensure_clesson_assignments_on_save(mocker):
#     """
#     Тест вызова проверки назначений
#     """
#     mocked_ensure_function = mocker.patch(
#         'kelvin.lesson_assignments.models.ensure_clesson_assignments')
#     ensure_clesson_assignments_on_save(sender=MagicMock(), instance='clesson')
#     assert mocked_ensure_function.mock_calls == [call('clesson')]
