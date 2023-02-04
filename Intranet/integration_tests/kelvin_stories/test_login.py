import pytest

from django.conf import settings
from django.contrib.auth import get_user_model


User = get_user_model()


@pytest.mark.django_db
def test_login_simple(jclient, student):
    url = '/auth_ping/'
    # response = apiclient.get(url)
    # assert response.status_code == 401, f"Response: {response.content}"

    response = jclient.get(url)
    assert response.status_code == 401, f"Response: {response.content}"

    jclient.login(student)
    response = jclient.get(url)
    assert response.status_code == 200, f"Response: {response.content}"


@pytest.mark.skip()
@pytest.mark.django_db
def test_login(jclient, student):
    """
    При первом запросе рердиректим на создание пользователя, если заходит
    Яндексовый пользователь
    """
    auth_ping_url = '/auth_ping/'

    # запрос неавторизованным пользователем
    response = jclient.get(auth_ping_url)
    assert response.status_code == 401

    # запрос аутентифицированным пользователем
    settings.YAUTH_TEST_USER = {
        'login': 'pupkin',
        'uid': 1234567890,
    }
    response = jclient.get(auth_ping_url)
    assert response.status_code == 302
    assert response.get('Location') == (
        '/api/v2/create-user/?next=%2Fauth_ping%2F')

    # запрос существующим пользователем
    jclient.login(student)
    response = jclient.get(auth_ping_url)
    assert response.status_code == 200


CREATE_USER_DATA = (
    (
        {
            'login': 'v.pupkin',
            'default_email': 'pupkin@yandex.ru',
            'sex': 1,
            'last_name': 'Pupkin',
            'first_name': 'Vasiliy',
            'uid': 1234567890,
        },
        {
            'username': 'v.pupkin',
            'email': 'pupkin@yandex.ru',
            'gender': 1,
            'last_name': 'Pupkin',
            'first_name': 'Vasiliy',
            'yauid': 1234567890,
        },
    ),
    (
        {
            'login': 'any.user',
            'default_email': 'any@yandex.ru',
            'sex': 0,
            'last_name': 'User',
            'first_name': 'Some',
            'uid': 1234567891,
        },
        {
            'username': 'any.user',
            'email': 'any@yandex.ru',
            'gender': 3,
            'last_name': 'User',
            'first_name': 'Some',
            'yauid': 1234567891,
        },
    ),
)


@pytest.mark.skip()
@pytest.mark.django_db
@pytest.mark.parametrize('user_data,expected_data', CREATE_USER_DATA)
def test_create_user(jclient, user_data, expected_data):
    """
    При обращении несуществующего пользователя создает его в базе, проставляя
    полученные данные из блэкбокса
    """
    create_user_url = '/api/v2/create-user/?next=/'

    settings.YAUTH_TEST_USER = user_data

    assert User.objects.all().count() == 2
    response = jclient.post(create_user_url)
    assert response.status_code == 302
    assert User.objects.all().count() == 3
    user = User.objects.get(username=user_data['login'])

    assert {
        'username': user.username,
        'email': user.email,
        'gender': user.gender,
        'last_name': user.last_name,
        'first_name': user.first_name,
        'yauid': user.yauid,
    } == expected_data
