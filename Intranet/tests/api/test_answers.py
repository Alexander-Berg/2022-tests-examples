import pytest
from django.urls import reverse

from intranet.search.core.models import SuggestedAnswer

pytestmark = pytest.mark.django_db(transaction=False)


def test_get_not_allowed(api_client):
    url = reverse('suggest-answer')
    r = api_client.get(url)
    assert r.status_code == 405


@pytest.mark.parametrize('data', [
    {
        'url': 'https://example.com',
        'scope': 'search',
        'search_text': 'аркадия',
        'request_id': 'hello-request-id',
    },
    {
        'url': 'https://ya.ru/mail/',
        'scope': 'atsearch',
        'request_id': '123456',
        'search_text': 'привет'
    }
])
def test_post_answer_success(api_client, data):
    url = reverse('suggest-answer')
    response = api_client.post(url, json=data)
    assert response.status_code == 200


def test_schemaless_url(api_client):
    data = {
        'url': 'ya.ru/hello',
        'scope': 'search',
        'search_text': 'hello',
        'request_id': 'hello-id',
    }
    url = reverse('suggest-answer')
    response = api_client.post(url, json=data)
    assert response.status_code == 200
    answer = SuggestedAnswer.objects.get()
    assert answer.url == 'ya.ru/hello'


@pytest.mark.parametrize('data', [
    {
        'url': 'https://example.com',
        'scope': 'search',
        'search_text': '',
        'request_id': 'hello-request-id',
        'errors': {
            'search_text': ['This field is required'],
            '__all__': [],
        }
    },
    {
        'url': 'https://example.com',
        'scope': 'unknown',
        'search_text': 'текст',
        'request_id': 'hello-request-id',
        'errors': {
            'scope': ['Not a valid choice'],
            '__all__': [],
        }
    },
    {
        'url': 'meow',
        'scope': 'search',
        'search_text': 'аркадия',
        'request_id': 'hello-request-id',
        'errors': {
            'url': ['Does not look like a link. Check if the url is correct'],
            '__all__': [],
        }
    },
    # TODO: fix when a.yaml will be ready
    # {
    #     'kwargs': {
    #         'HTTP_ACCEPT_LANGUAGE': 'ru-ru'
    #     },
    #     'url': 'meow',
    #     'scope': 'search',
    #     'search_text': 'аркадия',
    #     'request_id': 'hello-request-id',
    #     'errors': {
    #         'url': ['Не похоже на ссылку. Проверьте корректность url'],
    #         '__all__': [],
    #     }
    # },
])
def test_post_answer_error(api_client, data):
    url = reverse('suggest-answer')
    errors = data.pop('errors')
    kwargs = data.pop('kwargs', {})
    response = api_client.post(url, json=data, **kwargs)
    assert response.status_code == 400
    expected = {
        'errors': errors
    }
    assert response.json() == expected


def test_options(api_client):
    url = reverse('suggest-answer')
    response = api_client.options(url, HTTP_ORIGIN='https://hello.yandex-team.ru/')
    assert response.status_code == 200
    assert response['Access-Control-Allow-Origin'] == 'https://hello.yandex-team.ru/'
    assert response['Access-Control-Allow-Methods'] == 'GET, POST, OPTIONS'
    assert response['Access-Control-Allow-Headers'] == 'Content-Type, Accept-Language'
