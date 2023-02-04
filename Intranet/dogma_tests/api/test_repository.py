# coding: utf-8
import pytest

from json import loads

from django.core.urlresolvers import reverse


pytestmark = pytest.mark.django_db(transaction=True)


def test_get_repository(client, users, repo):
    client.login(username='vasya')

    response = loads(client.get(reverse('api:repository', args=[repo.source.code, repo.owner, repo.name])).content)

    assert response['description'] == repo.description
    assert response['full_name'] == repo.full_name
    assert response['owner']['login'] == repo.source.code + ':' + repo.owner
    assert response['name'] == repo.name


def test_list_repositories(client, users, repos):
    client.login(username='vasya')

    response = loads(client.get(reverse('api:all_repositories')).content)
    assert len(response) == len(repos)

    raw_response = client.get(reverse('api:all_repositories'), {'per_page': 2})
    response = loads(raw_response.content)
    assert len(response) == 2
    ids = set(r['id'] for r in response)

    assert raw_response['Link'] == (
        '<http://testserver/api/v3/repositories?per_page=2>; rel="first",'
        ' <http://testserver/api/v3/repositories?since=%s&per_page=2>; rel="next"' % max(ids)
    )

    response = loads(client.get(reverse('api:all_repositories'), {'per_page': 2, 'since': max(ids)}).content)
    assert len(response) == 1

    ids.add(response[0]['id'])
    assert ids == set(r.id for r in repos)

    # Проверка паджинации по page

    raw_response = client.get(reverse('api:all_repositories'), {'per_page': 2, 'page': 2})
    response = loads(raw_response.content)
    assert len(response) == 1

    assert raw_response['Link'] == (
        '<http://testserver/api/v3/repositories?per_page=2>; rel="first", '
        '<http://testserver/api/v3/repositories?page=2&per_page=2>; rel="last"'
    )


def test_user_repositories(client, users, repos):
    client.login(username='vasya')

    response = loads(client.get(reverse('api:user_repositories', args=['users', 'github', 'cardinalis'])).content)

    assert len(response) == sum(1 for r in repos if r.owner == 'cardinalis')
    assert all(r['owner']['login'] == 'github:cardinalis' for r in response)
