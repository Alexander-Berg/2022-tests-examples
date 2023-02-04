import pytest
from django.contrib.auth import get_user_model

from integration_tests.kelvin.sapi.utils import obj_factory
from kelvin.courses.models import Course, CourseStudent
from kelvin.lessons.models import Lesson
from kelvin.subjects.models import Subject

User = get_user_model()


@pytest.fixture
def sirius_teacher_user():
    """
    Пользователь с ролью учителя
    """
    return obj_factory(User, dict(
        username='teacher_user',
        email='teacher@mail.test',
        is_teacher=True,
    ))


@pytest.fixture
def sirius_staff_user():
    """
    Пользователь с ролью администратора
    """
    return obj_factory(User, dict(
        username='staff_user',
        email='staff@mail.test',
        is_staff=True,
    ))


@pytest.fixture
def sirius_student_user():
    """
    Пользователь с ролью ученика
    """
    return obj_factory(User, dict(
        username='student_user',
        email='student@mail.test',
    ))


@pytest.fixture
def sirius_users(sirius_teacher_user, sirius_student_user):
    """
    Фикстура пользователей с разными ролями
    """
    return (
        sirius_teacher_user, sirius_student_user
    )


@pytest.fixture
def sirius_subjects():
    """
    Объекты предмета
    """
    subject1 = Subject.objects.create(
        name=u'Тестовый предмет',
        slug=u'test-subj',
        public=True,
    )

    subject2 = Subject.objects.create(
        name=u'Ядерная физика',
        slug=u'atomics',
        public=True,
    )

    subject3 = Subject.objects.create(
        name=u'Русский язык',
        slug=u'russian-lang',
        public=False,
    )

    return (
        subject1, subject2, subject3
    )


@pytest.fixture
def course1(sirius_users, sirius_subjects):
    """
    Тестовый курс 1
    """
    teacher_user, student_user = sirius_users
    subject1, subject2, subject3 = sirius_subjects

    return Course.objects.create(
        name=u'Тестовый курс 1',
        description=u'Описание курса',
        color='#015093',
        free=True,
        code='AABBCCDD',
        mode=Course.USUAL_COURSE,
        owner=teacher_user,
        subject=subject1,
    )


@pytest.fixture
def course2(sirius_users, sirius_subjects):
    """
    Тестовый курс 2
    """
    teacher_user, student_user = sirius_users
    subject1, subject2, subject3 = sirius_subjects

    return Course.objects.create(
        name=u'Тестовый курс 2',
        description=u'Описание курса',
        color='#016095',
        free=True,
        code='DDDDEEEE',
        mode=Course.USUAL_COURSE,
        owner=teacher_user,
        subject=subject1,
    )


@pytest.fixture
def course3_not_free(sirius_users, sirius_subjects):
    """
    Тестовый курс 3
    """
    teacher_user, student_user = sirius_users
    subject1, subject2, subject3 = sirius_subjects

    return Course.objects.create(
        name=u'Тестовый курс 3. За деньги',
        description=u'Описание курса',
        color='#016095',
        free=False,
        code='DDDDFFFF',
        mode=Course.USUAL_COURSE,
        owner=teacher_user,
        subject=subject2,
    )


@pytest.fixture
def course_student1(sirius_users, course1):
    """
    Связь пользователя с курсом
    """
    teacher_user, student_user = sirius_users

    return CourseStudent.objects.create(
        course=course1,
        student=student_user
    )


@pytest.fixture
def sirius_lesson1(sirius_teacher_user):
    """
    Объект занятия
    """
    return obj_factory(Lesson, dict(
        owner=sirius_teacher_user,
        name=u'Урок 1',
    ))


@pytest.fixture
def sirius_lesson2(sirius_teacher_user):
    """
    Объект занятия
    """
    return obj_factory(Lesson, dict(
        owner=sirius_teacher_user,
        name=u'Урок 2',
    ))
