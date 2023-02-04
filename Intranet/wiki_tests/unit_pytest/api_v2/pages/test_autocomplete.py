import pytest

from wiki import access as wiki_access

pytestmark = [
    pytest.mark.django_db
]


def get_slugs_from_page_schemas(page_schemas):
    return sorted([page_schema['slug'] for page_schema in page_schemas])


def test_param(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete')

    assert response.status_code == 400


def test_empty_param(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete?slug')

    assert response.status_code == 400


def test_usual_case(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete?slug=root/')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == sorted(['root/a', 'root/b', 'root/c'])

    response = client.get('/api/v2/public/pages/autocomplete?slug=root/a/')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == sorted(['root/a/aa', 'root/a/ad'])


def test_without_slash(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete?slug=root')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == ['root']

    response = client.get('/api/v2/public/pages/autocomplete?slug=root/a')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == ['root/a']


def test_start_with_slash(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete?slug=/root/')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == sorted(['root/a', 'root/b', 'root/c'])


def test_incomplete(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete?slug=ro')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == ['root']

    response = client.get('/api/v2/public/pages/autocomplete?slug=root/a/a')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == sorted(['root/a/aa', 'root/a/ad'])


def test_non_exists(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete?slug=asassasasa')
    response_data = response.json()

    assert response.status_code == 200
    assert len(response_data['results']) == 0


def test_access(client, wiki_users, page_cluster):
    page_cluster['root/a/ad'].authors.remove(wiki_users.thasonic)
    wiki_access.set_access(page_cluster['root/a/ad'], wiki_access.TYPES.OWNER, wiki_users.chapson)
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/autocomplete?slug=root/a/')
    response_data = response.json()

    assert response.status_code == 200
    assert get_slugs_from_page_schemas(response_data['results']) == ['root/a/aa']
