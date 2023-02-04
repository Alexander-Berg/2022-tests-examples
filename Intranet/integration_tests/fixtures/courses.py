from builtins import range
import pytest
from datetime import timedelta

from django.utils import timezone
from django.utils.timezone import now
from django.contrib.contenttypes.models import ContentType
from kelvin.common.mixer_tools import django_mixer
from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.models import Problem
from kelvin.courses.models import AssignmentRule
from kelvin.accounts.models import User
from kelvin.tags.models import Tag, TaggedObject, TagTypeStaffGroup
from integration_tests.fixtures.projects import add_user_to_default_project, add_course_to_default_project


def add_students_to_course(course, students):
    """
    Функция для добавления студентов (массив) в курс
    :param course:      Курс, в который добавляем
    :param students:    Массив студентов
    :return:            Массив CourseStudent`ов
    """
    course_students = []
    for student in students:
        course_students.append(
            CourseStudent.objects.create(
                course=course,
                student=student,
            )
        )
    return course_students


def make_course_available_for_student(course, student):
    assignment_rule, _ = AssignmentRule.objects.get_or_create(
        course=course,
        formula=[
            [
                {
                    "operations": ["==", "!="],
                    "operation": "==",
                    "semantic_type": "StaffGroup",
                    "value": u"Яндекс"
                }
            ]
        ],
        title=u"Доступно всему Яндексу",
    )

    tag, _ = Tag.objects.get_or_create(
        type=TagTypeStaffGroup.get_db_type(),
        value=u"Яндекс"
    )

    tagged_object = TaggedObject.objects.create(
        object_id=student.id,
        tag_id=tag.id,
        content_type=ContentType.objects.get_for_model(User),
    )

    add_user_to_default_project(student)
    add_course_to_default_project(course)


@pytest.fixture
def course_with_lesson_variations(teacher, subject_model):
    """
    Курс, содержащий занятия с вариациями
    """
    problems = [
        Problem.objects.create(
            name=u'Задача {}'.format(i),
            owner=teacher,
            subject=subject_model,
            markup={},
        ) for i in range(1, 9)
    ]
    lesson1 = Lesson.objects.create(owner=teacher)
    lesson2 = Lesson.objects.create(owner=teacher)
    lesson3 = Lesson.objects.create(owner=teacher)
    course = Course.objects.create(
        owner=teacher,
        subject=subject_model,
        code='ABRACADA',
    )

    # делаем в первом занятии 2 группы и 2 задачи без группы
    # первая группа: 0, 3, 6
    # вторая группа: 1, 4, 7
    # без группы: 2, 5
    lesson1_problem_links = [
        LessonProblemLink.objects.create(
            lesson=lesson1,
            problem=problem,
            order=i,
            group=(i % 3 if i % 3 != 2 else None),
        ) for i, problem in enumerate(problems)
    ]
    clesson1 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson1,
    )

    # во втором занятии делаем 1 группу из 3 задач
    lesson2_problem_links = [
        LessonProblemLink.objects.create(
            lesson=lesson2,
            problem=Problem.objects.create(
                name=u'Задача {}'.format(i),
                owner=teacher,
                subject=subject_model,
                markup={},
            ),
            order=i,
            group=1,
        ) for i in range(10, 13)
    ]
    # второе курсозанятие уже выдано
    clesson2 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson2,
        date_assignment=now() - timedelta(days=1),
    )

    # в третьем занятии 3 задачи и нет вариаций
    lesson3_problem_links = [
        LessonProblemLink.objects.create(
            lesson=lesson3,
            problem=Problem.objects.create(
                name=u'Задача {}'.format(i),
                owner=teacher,
                subject=subject_model,
                markup={},
            ),
            order=i,
            group=None,
        ) for i in range(20, 23)
    ]
    # третье курсозанятие еще не выдано
    clesson3 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson3,
        date_assignment=now() + timedelta(days=1),
    )

    return {
        'course': course,
        'clessons': [clesson1, clesson2, clesson3],
        'problem_links': {
            lesson1.id: lesson1_problem_links,
            lesson2.id: lesson2_problem_links,
            lesson3.id: lesson3_problem_links,
        },
    }


@pytest.fixture
def student_in_course_with_lesson_variations(course_with_lesson_variations,
                                             student):
    """
    Ученик в курсе с вариативными занятиями
    """
    data = course_with_lesson_variations
    CourseStudent.objects.create(course=data['course'], student=student)
    data['student'] = student
    make_course_available_for_student(data['course'], student)
    return data


@pytest.fixture
def course_with_teacher_and_users(teacher, subject_model, student2, student3):
    """
    Курс с учителем и двумя учениками в нем
    """
    course = Course.objects.create(
        name=u'Новый спец курс',
        subject=subject_model,
        owner=teacher,
        id=1,
    )
    CourseStudent.objects.create(course=course, student=student2)
    CourseStudent.objects.create(course=course, student=student3)
    return {
        "course": course,
        "teacher": teacher,
        "students": [student2, student3],
    }


@pytest.fixture(params=[
    CourseLessonLink.TRAINING_MODE,
    CourseLessonLink.CONTROL_WORK_MODE,
])
def student_in_course_with_lesson(request, teacher, subject_model,
                                  lesson_models, student):
    """
    Курс с одним занятием и 2 задачами, один ученик в курсе.
    """
    course = Course.objects.create(
        name=u'Суперкурс',
        subject=subject_model,
        owner=teacher,
    )
    lesson, __, __, link1, link2 = lesson_models
    now_moment = now()
    clesson_kwargs = {
        'course': course,
        'lesson': lesson,
        'date_assignment': now_moment,
        'accessible_to_teacher': now_moment,
    }
    if request.param == CourseLessonLink.CONTROL_WORK_MODE:
        tomorrow = now_moment + timedelta(days=1)
        clesson_kwargs['mode'] = CourseLessonLink.CONTROL_WORK_MODE
        clesson_kwargs['finish_date'] = tomorrow
        clesson_kwargs['evaluation_date'] = tomorrow
        clesson_kwargs['duration'] = 45
    clesson = CourseLessonLink.objects.create(**clesson_kwargs)
    CourseStudent.objects.create(course=course, student=student)
    return {
        'course': course,
        'clesson': clesson,
        'lesson': lesson,
        'problem_links': [link1, link2],
        'student': student,
    }


@pytest.fixture
def student_in_scorm_course(request, teacher, subject_model, lesson_models, student, some_owner, theme_model,
                            scorm_problem):
    """
    SCORM-курс, один ученик в курсе.
    """
    course = Course.objects.create(
        name=u'SCORM курс',
        subject=subject_model,
        owner=teacher,
    )

    lesson = Lesson.objects.create(
        owner=some_owner,
        theme=theme_model,
        name=u'Урок',
    )
    link = LessonProblemLink.objects.create(
        lesson=lesson, problem=scorm_problem, order=1, type=1,
    )

    now_moment = now()
    clesson = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson,
        date_assignment=now_moment,
        accessible_to_teacher=now_moment,
    )
    CourseStudent.objects.create(course=course, student=student)
    return {
        'course': course,
        'clesson': clesson,
        'lesson': lesson,
        'problem_link': link,
        'student': student,
    }


@pytest.fixture
def course(subject_model, teacher):
    """
    Создает пустой курс, который назначен первому учителю
    """
    return Course.objects.create(
        name=u"Курс 1",
        subject=subject_model,
        owner=teacher,
    )


@pytest.fixture
def course2(subject_model, teacher):
    """
    Создает пустой курс (2), который назначен первому учителю
    """
    return Course.objects.create(
        name=u"Курс 2",
        subject=subject_model,
        owner=teacher,
    )


@pytest.fixture
def courses_with_students(subject_model, teacher, student, student2):
    """
    Создает несколько курсов и добавляет в них несколько студентов
    """
    course1, course2, course3 = django_mixer.cycle(3).blend(
        Course,
        name=django_mixer.sequence(u'Курс {0}'),
        subject=subject_model,
        owner=teacher,
        code=django_mixer.sequence('course{0}'),
        supports_web=django_mixer.sequence(False, False, True),
    )

    django_mixer.cycle(4).blend(
        CourseStudent,
        course=django_mixer.sequence(
            course1, course2, course2, course3,
        ),
        student=django_mixer.sequence(
            student, student, student2, student,
        )
    )

    return course1, course2, course3
