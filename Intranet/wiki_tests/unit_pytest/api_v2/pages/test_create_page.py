import pytest

from django.conf import settings

from wiki.api_frontend.serializers.user_identity import UserIdentity
from wiki.api_v2.public.pages.exceptions import SlugReserved
from wiki.pages.logic.subscription import create_watch
from wiki.pages.models import Page, PageWatch
from wiki.pages.models.consts import PageType, AclType
from wiki.subscriptions.models import Subscription, SubscriptionType
from wiki.users.logic.settings import set_user_setting
from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json, in_any_order
from intranet.wiki.tests.wiki_tests.common.skips import only_biz

pytestmark = [
    pytest.mark.django_db,
]


def create_subscription(user, page, type_=SubscriptionType.MY, is_cluster=True) -> Subscription:
    subscr = Subscription(user=user, page=page, type=type_, is_cluster=is_cluster)
    subscr.save()
    return subscr


def test_create_page(client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)
    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'root/ненормализованный_слаг',
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )

    assert response.status_code == 200

    assert_json(response.json(), {'slug': 'root/nenormalizovannyjjslag', 'page_type': 'page', 'content': 'FooBar'})

    p = Page.objects.get(pk=response.json()['id'])
    assert p.org_id == test_org_id

    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'neroot',
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )

    assert response.status_code == 200


def test_create_page_403(client, wiki_users, organizations, page_cluster):
    client.login(wiki_users.thasonic)
    set_access_author_only(page_cluster['root'], [wiki_users.asm])
    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'root/ненормализованный_слаг',
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )

    assert response.status_code == 403


def test_create_page_slug_occupied(client, wiki_users, organizations, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'root',
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )

    assert response.status_code == 400


def test_create_page_bad_slug(client, wiki_users, organizations, page_cluster):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': '!!!',
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )

    assert response.status_code == 400

    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'x' * 999,
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )

    assert response.status_code == 400


def test_create_page__reserved_slug(client, wiki_users, organizations, page_cluster):
    # 3. Нельзя создавать зарезервированную страницу

    client.login(wiki_users.thasonic)

    reserved_slugs = [
        'users/testuser',
        'ping',
        'hi-there',
    ]
    if settings.IS_BUSINESS:
        reserved_slugs.extend(
            [
                'captcha',
                'xcaptcha',
                'showcaptcha',
                'checkcaptcha',
                'captchaandanyotherword',
            ]
        )
    for reserved_slug in reserved_slugs:
        response = client.post(
            '/api/v2/public/pages?fields=content,access',
            data={
                'page_type': PageType.REGULAR_PAGE.value,
                'title': 'Test',
                'slug': reserved_slug,
                'content': 'FooBar',
                'subscribe_me': True,
            },
        )

        assert response.status_code == 400
        assert response.json()['error_code'] == SlugReserved.error_code


def test_create_grid(client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)
    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.GRID.value,
            'title': 'Test',
            'slug': 'root/ненормализованный_слаг',
            'subscribe_me': True,
        },
    )

    assert response.status_code == 200, response.json()

    assert_json(
        response.json(),
        {
            'slug': 'root/nenormalizovannyjjslag',
            'page_type': 'grid',
        },
    )


@pytest.mark.parametrize('acl_type', [AclType.DEFAULT, AclType.ONLY_AUTHORS])
def test_create_page_acl__only_authors(acl_type: AclType, client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)
    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'root/1',
            'content': 'FooBar',
            'subscribe_me': True,
            'acl': {
                'break_inheritance': True,
                'acl_type': acl_type.value,
                'users': [],
                'groups': [],
            },
        },
    )

    assert response.status_code == 200
    assert_json(response.json(), {'access': {'inherits_from': None, 'type': acl_type.value}})


def test_create_page_acl__custom(client, wiki_users, test_group, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)
    response = client.post(
        '/api/v2/public/pages?fields=content,access,acl',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'root/1',
            'content': 'FooBar',
            'subscribe_me': True,
            'acl': {
                'break_inheritance': True,
                'acl_type': AclType.CUSTOM.value,
                'users': [UserIdentity.from_user(q).dict() for q in [wiki_users.asm, wiki_users.volozh]],
                'groups': [test_group.get_public_group_id()],
            },
        },
    )

    data = response.json()
    assert response.status_code == 200, data

    assert_json(
        data,
        {
            'access': {
                'inherits_from': None,
                'type': AclType.CUSTOM.value,
            },
            'acl': {
                'users': in_any_order(
                    [
                        {'username': 'asm'},
                        {'username': 'volozh'},
                    ]
                ),
                'groups': [{'id': test_group.get_public_group_id()}],
            },
        },
    )


def test_create_page__subscription(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    # legacy
    watch_users = [wiki_users.asm, wiki_users.volozh, wiki_users.thasonic]
    for user in watch_users:
        set_user_setting(user, 'new_subscriptions', False)
        create_watch(test_page, user, True)

    # new
    subscribers_users = [wiki_users.kolomeetz, wiki_users.chapson]
    for user in subscribers_users:
        set_user_setting(user, 'new_subscriptions', True)
        create_subscription(user, page=test_page, is_cluster=True)

    slug = f'{test_page.slug}/other'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': slug,
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )
    assert response.status_code == 200

    usernames = [user.username for user in watch_users]
    assert PageWatch.objects.filter(page__supertag=slug, user__in=usernames).count() == len(usernames)
    assert Subscription.objects.filter(page__supertag=slug).exists() is False


def test_create_page__subscription__no_inherit__new(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    user = wiki_users.kolomeetz
    create_watch(test_page, user, True)
    create_subscription(user, page=test_page, is_cluster=True)
    set_user_setting(user, 'new_subscriptions', True)

    slug = f'{test_page.slug}/other'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': slug,
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )
    assert response.status_code == 200

    assert PageWatch.objects.filter(page__supertag=slug, user=user.username).exists() is False
    assert Subscription.objects.filter(page__supertag=slug, user=user).exists() is False


def test_create_page__subscription__author(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    set_user_setting(client.user, 'new_subscriptions', True)

    slug = f'{test_page.slug}/other'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': slug,
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )
    assert response.status_code == 200
    subscr = Subscription.objects.filter(page__supertag=slug, user=client.user, is_cluster=False).first()
    assert subscr

    subscr.is_cluster = True
    subscr.save()

    # if already exist - no create
    nested = f'{slug}/nested'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': nested,
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )
    assert response.status_code == 200
    assert Subscription.objects.filter(page__supertag=nested, user=client.user).exists() is False


def test_create_page__subscription__author__legacy(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    set_user_setting(client.user, 'new_subscriptions', False)
    username = client.user.username

    slug = f'{test_page.slug}/other'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': slug,
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )
    assert response.status_code == 200
    assert PageWatch.objects.filter(page__supertag=slug, user=username, is_cluster=False).exists() is True

    # if already exist - also create subscription
    nested = f'{slug}/nested'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': nested,
            'content': 'FooBar',
            'subscribe_me': True,
        },
    )
    assert response.status_code == 200
    assert PageWatch.objects.filter(page__supertag=nested, user=username, is_cluster=False).exists() is True

    # if no subscribe me - no create
    nested_2 = f'{nested}/nested2'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': nested_2,
            'content': 'FooBar',
            'subscribe_me': False,
        },
    )
    assert response.status_code == 200
    assert PageWatch.objects.filter(page__supertag=nested_2, user=username).exists() is False


@only_biz
def test_create_page__subscription__no_inherit__biz(client, wiki_users, test_page, organizations):
    client.login(wiki_users.thasonic)

    other_watchers = [wiki_users.asm, wiki_users.volozh]

    for user in other_watchers:
        set_user_setting(user, 'new_subscriptions', False)
        create_watch(test_page, user, True)
        user.orgs.set([organizations.org_21])

    slug = f'{test_page.slug}/other'
    response = client.post(
        '/api/v2/public/pages',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': slug,
            'content': 'FooBar',
            'subscribe_me': False,
        },
    )
    assert response.status_code == 200
    assert PageWatch.objects.filter(page__supertag=slug).exists() is False
