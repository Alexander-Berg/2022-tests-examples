import pytest
from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only


@pytest.mark.django_db
def test_page_edit__title(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(f'/api/v2/public/pages/{page.id}?fields=breadcrumbs,authors', {'title': 'newTitle'})

    assert response.status_code == 200, response.json()
    assert response.json()['title'] == 'newTitle'


@pytest.mark.django_db
def test_page_edit__title__empty(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(f'/api/v2/public/pages/{page.id}?fields=breadcrumbs,authors', {'title': '   '})

    assert response.status_code == 400, response.json()


@pytest.mark.django_db
def test_page_edit__403(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    set_access_author_only(page_cluster['root'], [wiki_users.thasonic])  # наследование должно запретить
    client.login(wiki_users.asm)

    response = client.post(f'/api/v2/public/pages/{page.id}?fields=breadcrumbs,authors', {'title': 'newTitle'})

    assert response.status_code == 403, response.json()
