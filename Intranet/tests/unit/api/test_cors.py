from django.core.urlresolvers import reverse


def test_valid_cors(client):
    response = client.get(
        reverse('ping'),
        HTTP_ORIGIN='https://staff.yandex-team.ru'
    )

    assert response['Access-Control-Allow-Origin'] == 'https://staff.yandex-team.ru'


def test_invalid_cors(client):
    response = client.get(
        reverse('ping'),
        HTTP_ORIGIN='https://google.com'
    )

    assert 'Access-Control-Allow-Origin' not in response

    response = client.get(
        reverse('ping'),
        HTTP_ORIGIN='http://staff.yandex-team.ru'
    )

    assert 'Access-Control-Allow-Origin' not in response
