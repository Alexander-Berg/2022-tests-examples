import pytest
from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from wiki.api_v2.public.pages.exceptions import PageReserved


@pytest.mark.django_db
def test_page_delete(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root']
    set_access_author_only(page, [wiki_users.thasonic])
    client.login(wiki_users.thasonic)

    response = client.delete(f'/api/v2/public/pages/{page.id}')

    assert response.status_code == 204, response.json()
    page.refresh_from_db()
    assert page.status == 0


@pytest.mark.django_db
def test_page_delete__no_access(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root']
    set_access_author_only(page, [wiki_users.thasonic])
    client.login(wiki_users.asm)

    response = client.delete(f'/api/v2/public/pages/{page.id}')

    assert response.status_code == 403, response.json()
    page.refresh_from_db()
    assert page.status == 1


@pytest.mark.django_db
def test_page_delete__reserved(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root']
    page.supertag = 'users/testuser'
    page.save()

    set_access_author_only(page, [wiki_users.thasonic])
    client.login(wiki_users.thasonic)

    response = client.delete(f'/api/v2/public/pages/{page.id}')

    assert response.status_code == 400, response.json()
    assert response.json()['error_code'] == PageReserved.error_code
