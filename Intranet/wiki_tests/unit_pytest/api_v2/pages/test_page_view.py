from pprint import pprint

import pytest

from django.test import override_settings
from django.conf import settings

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only, set_access_inherited
from intranet.wiki.tests.wiki_tests.common.factories.page import PageFactory
from intranet.wiki.tests.wiki_tests.common.skips import only_biz
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from wiki.api_v2.public.pages.exceptions import RedirectChain
from wiki.api_v2.public.pages.consts import SubscriptionStatus
from wiki.api_v2.public.pages.page.details_serializer import PageCompositeSerializer
from wiki.api_v2.public.pages.schemas import UserPermission
from wiki.favorites.models import Bookmark
from wiki.pages.models import Page
from wiki.pages.models.consts import PageType, AclType, ActualityStatus
from wiki.subscriptions.models import Subscription, SubscriptionType

pytestmark = [pytest.mark.django_db]


def test_page_details_view(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages?slug={page.slug}&fields=breadcrumbs,authors')
    response2 = client.get(f'/api/v2/public/pages/{page.id}?fields=breadcrumbs,authors')

    assert response.status_code == 200
    assert response2.status_code == 200
    assert response.json() == response2.json()


def test_page_details__user_permissions(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages?slug={page.slug}&fields=breadcrumbs,authors,user_permissions')

    assert response.status_code == 200

    pprint(response.json())


def test_page_details_view__grid(client, wiki_users, page_cluster, organizations, grid_with_content):
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages?slug={grid_with_content.slug}&fields=content')

    assert response.status_code == 200
    pprint(response.json())


def test_page_details_view__cloud(client, wiki_users, organizations, cloud_page_cluster):
    page = cloud_page_cluster['root/a']
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages?slug={page.slug}&fields=content')

    assert response.status_code == 200
    pprint(response.json())


def test_page_details_view__bookmark(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages?slug={test_page.slug}&fields=bookmark')
    assert response.json()['bookmark'] is None

    b = Bookmark(user=client.user, page=test_page)
    b.save()
    response = client.get(f'/api/v2/public/pages?slug={test_page.slug}&fields=bookmark')
    assert response.json()['bookmark']['id'] == b.id


def test_page_details_view__subscription(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages?slug={test_page.slug}&fields=subscription')
    assert response.json()['subscription'] is None

    subscr = Subscription(user=client.user, page=test_page, type=SubscriptionType.MY, is_cluster=True)
    subscr.save()

    response = client.get(f'/api/v2/public/pages?slug={test_page.slug}&fields=subscription')
    assert response.json()['subscription']['status'] == SubscriptionStatus.CLUSTER

    subscr.is_cluster = False
    subscr.save()
    response = client.get(f'/api/v2/public/pages?slug={test_page.slug}&fields=subscription')
    assert response.json()['subscription']['status'] == SubscriptionStatus.PAGE


def test_page_details_view__background(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    backgrounds = {
        99: {'id': 99, 'color': 'rgba(73, 160, 246, 0.3)', 'type': 'color'},
        1: {'id': 1, 'url': 'https://yandex.net/1.jpg', 'preview': 'https://yandex.net/1_small.jpg', 'type': 'image'},
    }

    test_page.background_id = 99
    test_page.save()

    with override_settings(PAGE_BACKGROUNDS_IDS=backgrounds):
        # as color
        response = client.get(f'/api/v2/public/pages/{test_page.id}?fields=background')

        assert response.status_code == 200
        assert response.json()['background'] == backgrounds[99]

        # as image
        test_page.background_id = 1
        test_page.save()

        response = client.get(f'/api/v2/public/pages/{test_page.id}?fields=background')

        assert response.status_code == 200
        assert response.json()['background'] == backgrounds[1]


def test_page_details_view__background__not_found(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    test_page.background_id = 323
    test_page.save()

    with override_settings(PAGE_BACKGROUNDS_IDS={}):
        response = client.get(f'/api/v2/public/pages/{test_page.id}?fields=background')

    assert response.status_code == 200
    assert response.json()['background'] is None


def test_page_details_view__background__default(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    assert test_page.background_id is None

    response = client.get(f'/api/v2/public/pages/{test_page.id}?fields=background')
    assert response.status_code == 200
    assert response.json()['background'] is None


def test_page_expand_home(client, wiki_users, page_cluster, organizations, groups, test_org_id):
    p1 = PageFactory(
        supertag=f'users/{wiki_users.thasonic.username}',
        last_author=wiki_users.thasonic,
        org_id=test_org_id,
    )
    PageFactory(
        supertag=f'users/{wiki_users.thasonic.username}/notes',
        last_author=wiki_users.thasonic,
        org_id=test_org_id,
    )
    p2 = PageFactory(
        supertag=f'users/{wiki_users.thasonic.username}/subpath/123',
        last_author=wiki_users.thasonic,
        org_id=test_org_id,
    )

    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages?slug=~')
    assert response.status_code == 200
    assert response.json()['id'] == p1.id

    response = client.get('/api/v2/public/pages?slug=~/subpath/123')
    assert response.status_code == 200
    assert response.json()['id'] == p2.id


def test_no_access(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a']
    client.login(wiki_users.asm)
    set_access_author_only(page_cluster['root'], [wiki_users.thasonic])
    set_access_inherited(page)

    response = client.get(f'/api/v2/public/pages/{page.id}?fields=breadcrumbs,authors')
    assert response.status_code == 403, response.json()


@only_biz
def test_no_cross_org(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root']
    client.login(wiki_users.thasonic, organizations.org_21)
    response = client.get(f'/api/v2/public/pages/{page.id}?fields=breadcrumbs,authors')
    assert response.status_code == 404


def test_page_redirects(client, wiki_users, page_cluster, organizations, groups):
    # root -> root/a -> root/a/aa
    page_cluster['root'].redirects_to = page_cluster['root/a']
    page_cluster['root'].save()

    page_cluster['root/a'].redirects_to = page_cluster['root/a/aa']
    page_cluster['root/a'].save()

    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages', {'slug': page_cluster['root'].slug, 'fields': 'redirect'})

    assert response.status_code == 200, response.json()

    redirects = response.json()['redirect']
    assert redirects['page_id'] == page_cluster['root/a'].id
    assert redirects['redirect_target']['id'] == page_cluster['root/a/aa'].id


def test_page_raise_on_redirects(client, wiki_users, page_cluster, organizations, groups):
    # root -> root/a -> root/a/aa
    page_cluster['root'].redirects_to = page_cluster['root/a']
    page_cluster['root'].save()

    page_cluster['root/a'].redirects_to = page_cluster['root/a/aa']
    page_cluster['root/a'].save()

    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/pages', {'slug': page_cluster['root'].slug, 'raise_on_redirect': True})
    assert response.status_code == 400
    assert_json(
        response.json(),
        {
            'error_code': RedirectChain.error_code,
            'details': {
                'source_page': {'id': page_cluster['root'].id},
                'target_page': {'id': page_cluster['root/a/aa'].id},
            },
        },
    )

    root_id = page_cluster['root'].id

    response = client.get(f'/api/v2/public/pages/{root_id}', {'raise_on_redirect': True})
    assert response.status_code == 400

    assert_json(
        response.json(),
        {
            'error_code': RedirectChain.error_code,
            'details': {
                'source_page': {'id': page_cluster['root'].id},
                'target_page': {'id': page_cluster['root/a/aa'].id},
            },
        },
    )


def test_page_details_view__breadcrumbs__copy_on_write(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    Page.objects.filter(supertag='users').delete()
    copy_on_write_info = {'id': -101, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    test_page.supertag = 'users/one/two'
    test_page.save()

    with override_settings(COPY_ON_WRITE_TAGS={'users': copy_on_write_info}):
        response = client.get(f'/api/v2/public/pages?slug={test_page.slug}&fields=breadcrumbs')
        assert response.status_code == 200

    breadcrumbs = response.json()['breadcrumbs']
    assert breadcrumbs[0]['slug'] == 'users'
    assert breadcrumbs[0]['title'] == copy_on_write_info['ru']['title']
    assert breadcrumbs[0]['id'] == -101


def test_page_details_view__cow(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    idx, slug, title = -101, 'cow-users', 'Личные разделы пользователей'
    copy_on_write_info = {'id': idx, 'ru': {'title': title, 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        fields = ','.join(PageCompositeSerializer.FIELDS)
        response = client.get(f'/api/v2/public/pages/{idx}?fields={fields}')
        assert response.status_code == 200, response.json()

    expected_cluster_slug = slug if settings.NAVIGATION_TREE_CLUSTER_LEVEL > 0 else ''  # b2b == 0

    assert_json(
        response.json(),
        {
            'id': idx,
            'slug': slug,
            'title': title,
            'page_type': PageType.REGULAR_PAGE,
            'cluster': {'slug': expected_cluster_slug},
            'redirect': None,
            'authors': {
                'owner': {'username': wiki_users.robot_wiki.username},
                'last_author': {'username': wiki_users.robot_wiki.username},
                'all': [{'username': wiki_users.robot_wiki.username}],
            },
            'access': {'type': AclType.DEFAULT, 'is_readonly': False, 'inherits_from': None},
            'acl': {
                'acl_type': AclType.DEFAULT,
                'inherits_from': None,
                'break_inheritance': False,
                'is_readonly': False,
                'users': [],
                'groups': [],
            },
            'breadcrumbs': [{'id': idx, 'title': title, 'slug': slug, 'page_exists': True}],
            'attributes': {
                # 'created_at': '2022-07-12T13:15:48.813Z',
                # 'modified_at': '2022-07-12T13:15:48.813Z',
                'lang': '',
                'is_readonly': False,
                'comments_count': 0,
                'comments_enabled': True,
                'keywords': [],
            },
            # 'content': '',
            'officiality': {'is_official': False},
            'actuality': {'status': ActualityStatus.ACTUAL},
            'bookmark': None,
            'subscription': None,
            'user_permissions': [UserPermission.CAN_EDIT, UserPermission.CAN_COMMENT, UserPermission.CAN_VIEW],
            'background': None,
            'last_revision_id': None,
            'active_revision': None,
        },
    )


def test_page_details_view__cow__by_slug(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    idx, slug, title = -101, 'cow-users', 'Личные разделы пользователей'
    copy_on_write_info = {'id': idx, 'ru': {'title': title, 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}):
        fields = ','.join(PageCompositeSerializer.FIELDS)
        response = client.get(f'/api/v2/public/pages?slug={slug}&fields={fields}')
        assert response.status_code == 200, response.json()

    assert_json(response.json(), {'id': idx, 'slug': slug, 'title': title})


def test_page_details_view__cow__already_exists(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    idx, slug, title = -101, test_page.slug, test_page.title
    copy_on_write_info = {'id': idx, 'ru': {'title': title, 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        fields = ','.join(PageCompositeSerializer.FIELDS)
        response = client.get(f'/api/v2/public/pages/{idx}?fields={fields}')
        assert response.status_code == 200, response.json()

    assert_json(response.json(), {'id': test_page.id, 'slug': slug, 'title': title})
