import pytest

from intranet.wiki.tests.wiki_tests.common.factories.page import PageFactory
from intranet.wiki.tests.wiki_tests.common.skips import only_biz


@pytest.fixture
def create_page(organizations):
    _org_id = organizations and organizations.org_42.id  # default or None

    def _create_page(supertag: str, user, org_id: int = _org_id, only_author: bool = False):
        page = PageFactory(supertag=supertag, last_author=user, org_id=org_id)
        page.authors.add(user)
        if only_author:
            page.access_set.create(is_owner=False)
        return page

    return _create_page


@pytest.mark.django_db
def test_common_case(client, wiki_users, page_cluster, api_url):
    client.login(wiki_users.volozh)

    page = page_cluster['root']
    response = client.get(f'{api_url}/{page.supertag}/.children')
    assert response.status_code == 200

    subpages = [page for page in page_cluster.values() if page.slug.startswith('root')]
    assert len(response.data['data']['results']) == len(subpages)


@pytest.mark.django_db
def test_received_page_info(client, wiki_users, page_cluster, api_url):
    client.login(wiki_users.volozh)
    page = page_cluster['root/c']
    response = client.get(f'{api_url}/{page.supertag}/.children')
    assert len(response.data['data']['results']) == 1
    received_page_info = response.data['data']['results'][0]
    assert len(received_page_info) == 2
    assert received_page_info['supertag'] == page.supertag
    assert received_page_info['id'] == page.id


@pytest.mark.django_db
def test_non_existent_supertag(client, wiki_users, api_url):
    client.login(wiki_users.volozh)
    supertag = 'invalid-supertag'
    response = client.get(f'{api_url}/{supertag}/.children')
    assert response.status_code == 404


@pytest.mark.django_db
def test_filter_accessible_page(client, wiki_users, api_url, create_page):
    supertag = 'test'
    create_page(supertag, user=wiki_users.volozh, only_author=True)

    client.login(wiki_users.volozh)
    response = client.get(f'{api_url}/{supertag}/.children')
    assert len(response.data['data']['results']) == 1

    client.login(wiki_users.asm)
    response = client.get(f'{api_url}/{supertag}/.children')
    assert len(response.data['data']['results']) == 0


@only_biz
@pytest.mark.django_db
def test_access_by_organization(client, wiki_users, api_url, create_page, organizations):
    supertag = 'test'
    create_page(supertag, user=wiki_users.volozh, org_id=organizations.org_21.id)
    create_page(supertag, user=wiki_users.volozh, org_id=organizations.org_42.id)
    create_page(f'{supertag}/some', user=wiki_users.volozh, org_id=organizations.org_42.id)

    client.login(wiki_users.asm, organization=organizations.org_42)
    response = client.get(f'{api_url}/{supertag}/.children')
    assert len(response.data['data']['results']) == 2

    client.login(wiki_users.asm, organization=organizations.org_21)
    response = client.get(f'{api_url}/{supertag}/.children')
    assert len(response.data['data']['results']) == 1


@pytest.mark.django_db
def test_get_empty_response_because_no_access(client, wiki_users, api_url, create_page):
    supertag = 'root'
    create_page(f'{supertag}', user=wiki_users.volozh, only_author=True)
    create_page(f'{supertag}/a', user=wiki_users.volozh, only_author=True)
    create_page(f'{supertag}/b', user=wiki_users.volozh, only_author=True)

    client.login(wiki_users.asm)
    response = client.get(f'{api_url}/{supertag}/.children')

    assert response.status_code == 200
    assert len(response.data['data']['results']) == 0


@pytest.mark.django_db
def test_pagination(client, wiki_users, api_url, page_cluster):
    client.login(wiki_users.volozh)
    page = page_cluster['root']
    subpages = [page for page in page_cluster.values() if page.slug.startswith('root')]

    response = client.get(f'{api_url}/{page.supertag}/.children?page={1}&page_size={100}')
    assert len(response.data['data']['results']) == len(subpages)

    response = client.get(f'{api_url}/{page.supertag}/.children?page={1}&page_size={3}')
    assert len(response.data['data']['results']) == 3

    response = client.get(f'{api_url}/{page.supertag}/.children?page={2}&page_size={6}')
    assert len(response.data['data']['results']) == len(subpages) - 6


@pytest.mark.django_db
def test_pagination_get_last_nonexistent_page(client, wiki_users, page_cluster, api_url):
    client.login(wiki_users.volozh)
    page = page_cluster['root']
    response = client.get(f'{api_url}/{page.supertag}/.children?page={99999}')
    assert response.status_code == 404
