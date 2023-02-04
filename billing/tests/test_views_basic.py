from django.urls import reverse

from bcl.banks.registry import Sber
from bcl.core.models import RemoteClient, RemoteCredentials
from bcl.core.views.basic import csrf_failure


def test_csrf_failure(rf, init_user):

    request = rf.get('/some')
    request.user = init_user()
    result = csrf_failure(request)
    assert 'Сработала защита от CSRF' in result.content.decode()


def test_login(client):
    response = client.get('/login')
    assert response.status_code == 301


def test_ping(client):
    response = client.get('/ping')
    assert response.status_code == 200
    assert response.content == b'SUCCESS'


def test_remote_auth(client, init_user, check_client_response, response_mock, django_assert_num_queries):

    associate = Sber

    url = reverse('rauth', kwargs={'associate_id': associate.id})

    response = client.get(url)
    assert response.status_code == 302  # нужна авторизация

    assert not RemoteClient.objects.exists()  # данных о клиентах пока нет в БД

    user = init_user()  # авторизуемся

    msg_intro = 'Чтобы получить реквизиты '

    # страница с кнопкой авторизации
    with django_assert_num_queries(5) as _:
        check_client_response(url, check_content=msg_intro)

    # клиент создан автоматически
    remote_client: RemoteClient = RemoteClient.objects.first()
    assert f'{remote_client}' == '111332'
    assert remote_client.secret == '*****'

    with django_assert_num_queries(3) as _:  # тот же запос уже без sql для создания клиента
        check_client_response(url)

    assert not RemoteCredentials.objects.exists()  # реквизитов пока нет в БД
    assert remote_client.credentials_get_for_user(user) is None

    # пользователь нажал кнопку авторизации
    with django_assert_num_queries(4) as _:
        response = client.post(url, {'doauth': 'start'})
        assert response.status_code == 302  # перенаправляем во внешнюю систему

    assert 'redirect_uri=' in response.url

    # создано намерение на получение реквизитов
    credentials: RemoteCredentials = RemoteCredentials.objects.first()
    assert credentials.client_id == remote_client.id
    assert credentials.dt_revised is None
    assert credentials.dt_revise is None
    assert not credentials.hidden
    cred_nonce = credentials.nonce
    cred_state = credentials.state
    assert cred_nonce
    assert cred_state
    assert not credentials.token_access
    assert not credentials.token_refresh
    assert credentials.user_id == user.id

    # внешняя система вернула ошибку авторизации
    with django_assert_num_queries(3) as _:
        check_client_response(
            f'{url}?error=invalid_credentials&error_description=somethingwrong',
            check_content=[
                'не смогла вас авторизовать',
                'invalid_credentials: somethingwrong<'
            ]
        )

    # пришли неподдерживаемые данные
    with django_assert_num_queries(3) as _:
        check_client_response(f'{url}?some=thing', check_content=msg_intro)

    # пришли нужные данные, но мы не отправляли запрос о них (nonce не подошёл)
    with django_assert_num_queries(4) as _:
        check_client_response(f'{url}?code=123&state={cred_state}&nonce=unknown', check_content=msg_intro)

    request_valid = f'{url}?code=123&state={cred_state}&nonce={cred_nonce}'

    # пришли нужные данные, но сервер ответил ошибкой на запрос токена
    url_token = 'https://edupirfintech.sberbank.ru:9443/ic/sso/api/v2/oauth/token'
    with django_assert_num_queries(5) as _:
        with response_mock(
            f'POST {url_token} -> 400:'
            '{"error": "invalid_grant", "error_description": "Failed to extract shoulder ID from 123"}'
        ):
            check_client_response(
                request_valid,
                check_content=[
                    'не смогла вас авторизовать',
                    'invalid_grant: Failed to extract shoulder ID from 123<'
                ]
            )

    # пришли нужные данные, всё хорошо
    with django_assert_num_queries(6) as _:
        with response_mock(
            f'POST {url_token} -> 200:'
            '{"scope": "openid", "access_token": "456-789", '
            '"refresh_token": "324-545", "id_token": "zzz", "expires_in": 3600}'
        ):
            check_client_response(request_valid, check_content='получены успешно')

    credentials.refresh_from_db()
    assert credentials.dt_revised
    assert credentials.dt_revise
    assert not credentials.hidden
    assert credentials.token_access == '456-789'
    assert credentials.token_refresh == '324-545'

    assert remote_client.credentials_get_for_user(user) == credentials

    # запрос нового токена
    with response_mock(
        f'POST {url_token} -> 200:'
        '{"scope": "openid", "access_token": "77777", '
        '"refresh_token": "88888", "id_token": "rrrr", "expires_in": 3600}'
    ):
        client.post(url, {'doauth': 'start'})
        credentials_new = RemoteCredentials.objects.order_by('id').last()
        assert credentials_new.id != credentials.id
        check_client_response(f'{url}?code=456&state={credentials_new.state}&nonce={credentials_new.nonce}')

    credentials.refresh_from_db()
    assert credentials.hidden  # новый токен выдан, старый скрыт (инвалидирован)

    credentials_new.refresh_from_db()
    assert credentials_new.token_access == '77777'
    assert remote_client.credentials_get_for_user(user) == credentials_new
