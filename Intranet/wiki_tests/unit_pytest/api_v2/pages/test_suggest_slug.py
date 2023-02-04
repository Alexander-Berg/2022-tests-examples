from uuid import uuid4

import pytest

from wiki.api_v2.public.pages.exceptions import SlugTooLong, SlugEmpty
from wiki.api_v2.public.pages.suggest_slug_view import find_occupied_slugs
from wiki.pages.models import Page
from wiki.sync.connect.intranet_org import IntranetOrganization
from intranet.wiki.tests.wiki_tests.common.factories.page import PageFactory

pytestmark = [pytest.mark.django_db]


def test_occupied__normalize(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/suggest_slug?slug=/root')
    data = response.json()
    assert response.status_code == 200
    assert data['occupied']
    assert data['slug'] == 'root'


def test_occupied(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)
    page = Page.objects.first()
    slug: str = page.supertag
    response = client.get(f'/api/v2/public/pages/suggest_slug?slug={slug}')
    data = response.json()
    assert response.status_code == 200
    assert data['occupied']
    assert data['slug'] == slug

    org = page.org or IntranetOrganization()
    assert not find_occupied_slugs(org, set(data['suggest']))

    new_slug = str(uuid4().hex)
    response = client.get(f'/api/v2/public/pages/suggest_slug?slug={new_slug}')
    data = response.json()
    assert not data['occupied']
    assert data['slug'] == new_slug
    assert len(data['suggest']) == 0


def test_title(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)

    org_id = None

    if organizations:
        org_id = organizations.org_42.id

    page = PageFactory(
        supertag='foo/bar/test',
        last_author=wiki_users.thasonic,
        org_id=org_id,
    )

    response = client.get('/api/v2/public/pages/suggest_slug?current_slug=/foo/bar/&title=тест')
    data = response.json()
    assert data['occupied']
    assert data['slug'] == 'foo/bar/test'

    org = page.org or IntranetOrganization()
    assert not find_occupied_slugs(org, set(data['suggest']))

    title = 'ПриВЕт_как-дела'
    response = client.get(f'/api/v2/public/pages/suggest_slug?current_slug=/foo/bar/&title={title}')
    data = response.json()
    assert not data['occupied']
    assert data['slug'] == 'foo/bar/privetkak-dela'
    assert len(data['suggest']) == 0


def test_title__long(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    title = '1.Улучшение пользовательского опыта a.\t(Электронный договор) Договор\\распечатка b.\tОнлайн\\оплата 0% из'
    response = client.get(f'/api/v2/public/pages/suggest_slug?current_slug={test_page.slug}&title={title}')

    assert response.status_code == 200
    assert response.json()['slug'] == f'{test_page.slug}/1.uluchsheniepolzovatelskogoopytaa.jelektronnyjjdo'


def test_slug__long(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    test_page.supertag = 'a' * 250
    test_page.save()

    title = 'Заголовок, с которым итоговый slug будет больше 255'
    response = client.get(f'/api/v2/public/pages/suggest_slug?current_slug={test_page.slug}&title={title}')
    assert response.status_code == 400

    data = response.json()
    assert data['error_code'] == SlugTooLong.error_code
    assert len(data['details']['slug']) > 255


def test_suggest__long(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    long_slug = 'a' * 255

    test_page.supertag = long_slug
    test_page.save()

    response = client.get(f'/api/v2/public/pages/suggest_slug?slug={long_slug}')
    assert response.status_code == 200

    data = response.json()
    assert data['occupied'] is True
    assert data['slug'] == long_slug
    assert len(data['suggest']) == 0


def test_slug__empty(client, wiki_users):
    client.login(wiki_users.thasonic)

    # empty title
    for title in ['//', '[]', ';;', '!', '\\\\']:
        current_slug = ''
        response = client.get(f'/api/v2/public/pages/suggest_slug?current_slug={current_slug}&title={title}')
        assert response.status_code == 400, response.json()
        assert response.json()['error_code'] == SlugEmpty.error_code, response.json()

    # empty slug
    for slug in ['//', '[]', ';;', '!', '\\\\']:
        response = client.get(f'/api/v2/public/pages/suggest_slug?slug={slug}')
        assert response.status_code == 400, response.json()
        assert response.json()['error_code'] == SlugEmpty.error_code, response.json()


def test_slug__user_cluster_alias(client, wiki_users):
    client.login(wiki_users.thasonic)

    # via title
    current_slug, title = '~', 'any'
    response = client.get(f'/api/v2/public/pages/suggest_slug?current_slug={current_slug}&title={title}')
    assert response.status_code == 200, response.json()
    assert response.json()['slug'] == f'users/{client.user.username}/{title}', response.json()

    # via slug
    slug = '~'
    response = client.get(f'/api/v2/public/pages/suggest_slug?slug={slug}')
    assert response.status_code == 200, response.json()
    assert response.json()['slug'] == f'users/{client.user.username}', response.json()

    # via slug + page
    slug = '~/some-page'
    response = client.get(f'/api/v2/public/pages/suggest_slug?slug={slug}')
    assert response.status_code == 200, response.json()
    assert response.json()['slug'] == f'users/{client.user.username}/some-page', response.json()


def test_params(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages/suggest_slug')
    assert response.status_code == 400
