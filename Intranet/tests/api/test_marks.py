import json

import pytest
from django.urls import reverse_lazy

pytestmark = pytest.mark.django_db(transaction=False)


def test_get_not_allowed(api_client):
    url = reverse_lazy('marks')
    response = api_client.get(url)
    assert response.status_code == 405


@pytest.mark.parametrize('data', [
    {
        'url': 'https://example.com',
        'score': 'vital',
        'result': 400,
        'text': 'test',
        'position': 1,
        'layer': 'wiki',
        'href': 'https://st.yandex-team.ru/ISEARCH-7105',
    },
    {
        'url': 'https://ya.ru/mail/',
        'request_id': '123456',
        'result': 400,

        'text': 'test',
        'position': 1,
        'layer': 'wiki',
        'href': 'https://st.yandex-team.ru/ISEARCH-7105',
    },
    {
        'url': 'https://example.com/profiles/admin.cgi',
        'request_id': '123456',
        'score': 'relevant',
        'result': 400,
    },
    {
        'url': 'https://example.com/profiles/admin.cgi',
        'score': 'relevant',
        'request_id': '12345',
        'result': 200,

        'text': 'test',
        'position': 1,
        'layer': 'doc-doc',
        'scope': 'wikisearch',
        'mode': 'search',
        'href': 'https://st.yandex-team.ru/ISEARCH-7105',
    },
    {
        'url': 'https://example.com/profiles/admin.cgi',
        'score': 'vital',
        'result': 200,

        'text': 'test',
        'position': 1,
        'layer': 'wiki',
        'scope': 'wiki',
        'mode': 'suggest',
        'href': 'https://st.yandex-team.ru/ISEARCH-7105',
    },
    {
        'url': 'https://example.com/profiles/admin.cgi',
        'score': 'relevant',
        'result': 400,

        'text': 'test',
        'position': 1,
        'layer': 'wiki',
        'scope': 'wiki',
        'mode': 'suggest',
        'href': 'https://st.yandex-team.ru/ISEARCH-7105',
    },
    {
        'url': 'https://example.com/profiles/admin.cgi',
        'score': 'irrelevant',
        'result': 200,

        'text': 'test',
        'position': 1,
        'layer': 'wiki',
        'scope': 'wiki',
        'mode': 'suggest',
        'href': 'https://st.yandex-team.ru/ISEARCH-7105',
    },
    {
        'url': 'https://example.com/profiles/admin.cgi',
        'score': 'irrelevant',
        'result': 200,
        'request_id': '123456',
        'text': 'test',
        'position': 0,
        'layer': 'wiki-wiki',
        'scope': 'wikisearch',
        'mode': 'search',
    },
    {
        'url': 'https://example.com/profiles/admin.cgi',
        'score': 'relevant',
        'result': 200,
        'request_id': '123456',
        'text': 'test',
        'position': 0,
        'layer': 'doc-readme',
        'scope': 'search',
        'mode': 'search',
        'wizard_id': '12345',
    }
])
def test_position_mark(api_client, data):
    url = reverse_lazy('marks')
    result = data.pop('result')
    response = api_client.post(url, json=data)
    assert response.status_code == result


def test_position_plaintext(api_client):
    url = reverse_lazy('marks')
    data = {
        'url': 'https://example.com/profiles/admin.cgi',
        'score': 'irrelevant',
        'result': 200,
        'text': 'test',
        'position': 1,
        'layer': 'wiki',
        'scope': 'wiki',
        'mode': 'suggest',
        'href': 'https://st.yandex-team.ru/ISEARCH-7105',
    }
    response = api_client.post(url, content_type='text/plain;charset=UTF-8', data=json.dumps(data))
    assert response.status_code == 200
