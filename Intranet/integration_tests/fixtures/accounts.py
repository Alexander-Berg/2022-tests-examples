import pytest

from django.contrib.auth import get_user_model
from kelvin.common.mixer_tools import django_mixer

User = get_user_model()


@pytest.fixture(scope='session')
def django_user_model():
    """
    Фикстура возвращает класс для модели пользователя в Django
    """
    return User


@pytest.fixture
def some_owner():
    """
    Модель некоторого пользователя-ученика, используется в качестве владельца
    объектов
    """
    return User.objects.create(
        username='some_username',
        email='some_email@test.com',
        yauid=1,
    )


@pytest.fixture
def student():
    """Модель ученика"""
    return User.objects.create(
        username=u'Петя Иванов',
        email='ivanofff@yandex.ru',
        yauid=10,
    )


@pytest.fixture
def student2():
    """Модель второго ученика"""
    return User.objects.create(
        username=u'Иван Петров',
        email='petrof@yandex.ru',
        yauid=20,
    )


@pytest.fixture
def student3():
    """Модель третьего ученика"""
    return User.objects.create(
        username=u'Михаил Потапов',
        email='potap@yandex.ru',
        yauid=30,
    )


@pytest.fixture
def students(student, student2, student3):
    """Модель трех учеников для сокращения парметров"""
    return student, student2, student3


@pytest.fixture
def teacher():
    """Модель учителя"""
    user = User.objects.create(
        username=u'Иван Иваныч',
        email='ivan.ivanovich@yandex.ru',
        is_teacher=True,
        yauid=100,
    )
    user.teacher_profile.region = u'Москва'
    user.teacher_profile.school = u'№1'
    user.teacher_profile.save()
    return user


@pytest.fixture
def second_teacher():
    """Вторая модель учителя"""
    return User.objects.create(
        username=u'Иван Иваныч #2',
        email='ivan.ivanovich_number_two@yandex.ru',
        is_teacher=True,
        yauid=101,
    )


@pytest.fixture
def parent():
    """Модель родителя"""
    return User.objects.create(
        username=u'Родитель',
        email='brat_ivana@yandex.ru',
        is_parent=True,
        yauid=200,
    )


@pytest.fixture
def parent_for_student2(django_user_model, student2):
    """
    Родитель студента номер 2
    """
    parent = django_mixer.blend(
        django_user_model,
        username='parent',
        is_parent=True,
        yauid=201,
    )
    parent.parent_profile.children.add(student2)
    return parent


@pytest.fixture
def content_manager():
    """Модель контент-менеджера"""
    return User.objects.create(
        username=u'Контент-менеджер',
        email='ivanova@yandex.ru',
        is_content_manager=True,
        yauid=300,
    )


@pytest.fixture
def user_with_roles(request):
    """
    Генерирует пользователя и позволяет параметризировать его роли.
    Например, чтобы выполнить тест 2 раза сначала сгенерив учителя, а
    потом учителя с правами контент менеджера, параметризируйте тест
    следующим образом:

    @pytest.mark.parametrize(
        'user_with_roles',
        [
            ('is_teacher', ),
            ('is_teacher', 'is_content_manager'),
        ],
        indirect=('user_with_roles',)
    )
    def test_something(user_with_roles, some_fixture, other_fixture):
        assert user_with_roles.is_teacher
    """
    roles = {
        'is_staff': False,
        'is_teacher': False,
        'is_parent': False,
        'is_content_manager': False,
    }

    for role in request.param:
        roles[role] = True

    return User.objects.create(
        username=u'Константин Константинопольский',
        email='kk@yandex.ru',
        yauid=500,
        **roles
    )
