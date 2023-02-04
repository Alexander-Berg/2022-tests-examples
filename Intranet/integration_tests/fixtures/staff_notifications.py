import pytest

from kelvin.staff_notifications.models import ActivationCode, NewTeacherAction


@pytest.fixture
def action1():
    """
    Пустое действие 1
    """
    return NewTeacherAction.objects.create(name='Action 1')


@pytest.fixture
def action2():
    """
    Пустое действие 2
    """
    return NewTeacherAction.objects.create(name='Action 2')


@pytest.fixture
def action_with_subject(subject_model):
    """
    Действие, которое проставит предмет
    """
    return NewTeacherAction.objects.create(
        name='Action 3',
        subject=subject_model,
    )


@pytest.fixture
def action_copy_course(course, subject_model):
    """
    Создает действие (NewTeacherAction) копирования курса из другой фикстуры
    """
    return NewTeacherAction.objects.create(
        name=u'Копирование курса',
        subject=subject_model,
        course_to_own=course,
    )


@pytest.fixture
def action_copy_course_2(course2, subject_model):
    """
    Создает действие (NewTeacherAction) копирования второго курса
    """
    return NewTeacherAction.objects.create(
        name=u'Копирование воторого курса',
        subject=subject_model,
        course_to_own=course2,
    )


@pytest.fixture
def action_course_to_add(subject_model, course):
    """
    Действие, которое добавит учителя как ученика в курс
    """
    return NewTeacherAction.objects.create(
        name=u'Добавление в качестве ученика',
        subject=subject_model,
        course_to_add=course,
    )


@pytest.fixture
def empty_activation_code():
    """
    Пустой код активации
    """
    return ActivationCode.objects.create(code="1234567890")


@pytest.fixture
def code_copy_one_course(action_copy_course):
    """
    Создает код, который скопирует учителю один курс
    """
    activation_code = ActivationCode.objects.create(
        code="0987654321",
    )
    activation_code.actions = [action_copy_course]
    activation_code.save()
    return activation_code


@pytest.fixture
def code_with_subject(action_with_subject):
    """
    Создает код, который установит учителю предмет
    """
    activation_code = ActivationCode.objects.create(
        code="5677654321",
    )
    activation_code.actions = [action_with_subject]
    activation_code.save()
    return activation_code


@pytest.fixture
def code_copy_two_courses(action_copy_course, action_copy_course_2):
    """
    Создает код, который скопирует учителю два курса
    """
    activation_code = ActivationCode.objects.create(
        code="1234567890",
    )
    activation_code.actions = [
        action_copy_course, action_copy_course_2,
    ]
    activation_code.save()
    return activation_code


@pytest.fixture
def code_course_to_add(action_course_to_add):
    """
    Код, который добавит учителя как ученика в курс
    """
    activation_code = ActivationCode.objects.create(
        code="0987192321",
    )
    activation_code.actions = [action_course_to_add]
    activation_code.save()
    return activation_code
