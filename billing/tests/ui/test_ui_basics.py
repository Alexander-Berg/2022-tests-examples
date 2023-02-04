from django.middleware.csrf import _compare_masked_tokens as compare_masked_tokens

from mdh.core.models import ReaderRole


def test_csrf(init_user, drf_client):
    user = init_user()

    client = drf_client(user=user)
    response = client.get('/uiapi/csrf/')

    from_cookie = response.cookies['csrftoken'].value
    from_json = response.json()['token']

    assert from_cookie != from_json  # засолены по-разному
    assert compare_masked_tokens(from_cookie, from_json)  # секрет один


def test_pagination(init_domain, init_user, drf_client):

    user = init_user(roles=[ReaderRole])

    init_domain('dom1', user=user)
    init_domain('dom2', user=user)

    client = drf_client(user=user)

    response = client.get('/uiapi/domains/')
    headers = response.serialize_headers().decode()
    assert 'MDH-Show-Settings: 0' in headers

    response= response.json()
    assert response['count'] == 2
    assert len(response['results']) == 2

    response = client.get('/uiapi/domains/?page_size=1&page=2').json()
    assert response['count'] ==2
    assert len(response['results']) == 1


def test_noauth(drf_client, init_user):

    client = drf_client()
    response = client.get('/uiapi/domains/').json()
    assert response['error']['type'] == 'NotAuthenticated'

    # Попытка зайти неактивным пользователем.
    user = init_user(is_active=False)
    client = drf_client(user=user)
    response = client.get('/uiapi/domains/').json()
    assert response['error']['type'] == 'PermissionDenied'
