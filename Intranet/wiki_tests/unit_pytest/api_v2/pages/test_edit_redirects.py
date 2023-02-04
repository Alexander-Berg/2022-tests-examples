from pprint import pprint

import pytest

from wiki.api_v2.public.pages.page.set_redirect import RedirectLoop


@pytest.mark.django_db
def test_page_edit__redirect(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=redirect', {'redirect': {'page': {'id': page_cluster['root/b'].id}}}
    )

    assert response.status_code == 200, response.json()
    assert response.json()['redirect']['redirect_target']['slug'] == 'root/b'

    response = client.post(f'/api/v2/public/pages/{page.id}?fields=redirect', {'redirect': {'page': None}})

    pprint(response.json())

    assert response.status_code == 200
    assert response.json()['redirect'] is None


@pytest.mark.django_db
def test_page_edit__redirect_via_slug(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=redirect',
        {'redirect': {'page': {'slug': page_cluster['root/b'].slug}}},
    )

    assert response.status_code == 200
    assert response.json()['redirect']['redirect_target']['slug'] == 'root/b'


@pytest.mark.django_db
def test_page_edit__redirect__non_existent(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(f'/api/v2/public/pages/{page.id}?fields=redirect', {'redirect': {'page': {'id': 999}}})

    assert response.status_code == 400


@pytest.mark.django_db
def test_page_edit__redirect__loop(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)
    page_a = page_cluster['root/a'].id
    page_b = page_cluster['root/b'].id
    response = client.post(f'/api/v2/public/pages/{page_a}?fields=redirect', {'redirect': {'page': {'id': page_b}}})
    assert response.status_code == 200
    response = client.post(f'/api/v2/public/pages/{page_b}?fields=redirect', {'redirect': {'page': {'id': page_a}}})
    assert response.status_code == 400
    assert response.json()['error_code'] == RedirectLoop.error_code
