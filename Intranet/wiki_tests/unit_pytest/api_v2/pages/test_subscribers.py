import pytest

from django.test import override_settings

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.skips import only_biz
from wiki.api_frontend.serializers.user_identity import UserIdentity
from wiki.api_v2.public.pages.subscribers.schemas import AddSubscribersSchema
from wiki.pages.models import Page
from wiki.subscriptions.models import Subscription, SubscriptionType
from wiki.users.logic.settings import set_user_setting

pytestmark = [pytest.mark.django_db]


def create_subscription(
    user, page, type_=SubscriptionType.MY, is_cluster=True, exclude: list[str] = None
) -> Subscription:
    subscr = Subscription(user=user, page=page, type=type_, is_cluster=is_cluster, exclude={'exclude': exclude or []})
    subscr.save()
    set_user_setting(user, 'new_subscriptions', True)
    return subscr


def test_get_subscribers(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    subscr = create_subscription(wiki_users.volozh, test_page)

    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers')
    subscriptions = response.json()['results']

    assert len(subscriptions) == 1
    assert subscriptions[0]['id'] == subscr.id
    assert subscriptions[0]['is_cluster'] == subscr.is_cluster
    assert subscriptions[0]['user']['username'] == subscr.user.username
    assert subscriptions[0]['user']['identity']['uid'] == subscr.user.get_uid()


def test_get_subscribers__cow(client, wiki_users):
    client.login(wiki_users.asm)

    idx, slug = -101, 'cow-users'
    copy_on_write_info = {'id': idx, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        response = client.get(f'/api/v2/public/pages/{idx}/subscribers')
        assert response.json()['results'] == []


def test_get_subscribers_invalid_page(client, wiki_users):
    client.login(wiki_users.asm)
    response = client.get(f'/api/v2/public/pages/{9999}/subscribers')
    assert response.status_code == 404


def test_get_subscribers_no_accessible_by_acl_page(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    set_access_author_only(test_page, new_authors=[wiki_users.thasonic])
    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers')
    assert response.status_code == 403


def test_get_subscribers_filter_by_page(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_a, page_b = page_cluster['root/a'], page_cluster['root/b']

    create_subscription(wiki_users.volozh, page_a)
    create_subscription(wiki_users.chapson, page_b)

    response = client.get(f'/api/v2/public/pages/{page_a.id}/subscribers')
    subscriptions = response.json()['results']

    assert len(subscriptions) == 1
    assert subscriptions[0]['user']['username'] == wiki_users.volozh.username


def test_get_subscribers_filter_by_query(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    create_subscription(wiki_users.thasonic, test_page)
    create_subscription(wiki_users.volozh, test_page)
    create_subscription(wiki_users.chapson, test_page)
    create_subscription(wiki_users.asm, test_page)

    # all
    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers')
    assert len(response.json()['results']) == 4

    # startswith username
    query = wiki_users.volozh.username[:3]
    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers?q={query}')
    subscriptions = response.json()['results']
    assert len(subscriptions) == 1
    assert subscriptions[0]['user']['username'] == wiki_users.volozh.username

    # case-insensitive first_name
    query = wiki_users.volozh.staff.first_name.upper()
    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers?q={query}')
    subscriptions = response.json()['results']
    assert len(subscriptions) == 1
    assert subscriptions[0]['user']['username'] == wiki_users.volozh.username

    # by last_name
    query = wiki_users.volozh.staff.last_name
    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers?q={query}')
    subscriptions = response.json()['results']
    assert len(subscriptions) == 1
    assert subscriptions[0]['user']['username'] == wiki_users.volozh.username

    # equal startswith first_name
    query = wiki_users.asm.staff.first_name[:2]  # == 'Ал'
    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers?q={query}')
    subscriptions = response.json()['results']
    assert {s['user']['username'] for s in subscriptions} == {wiki_users.asm.username, wiki_users.thasonic.username}

    # invalid
    query = 'invalid query'
    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers?q={query}')
    assert len(response.json()['results']) == 0


def test_get_subscribers_from_up_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_nested, page_root = page_cluster['root/a/ac/bd'], page_cluster['root']

    create_subscription(wiki_users.volozh, page_nested)

    create_subscription(wiki_users.chapson, page_root, is_cluster=True)
    create_subscription(wiki_users.asm, page_root, is_cluster=False)

    response = client.get(f'/api/v2/public/pages/{page_nested.id}/subscribers')
    subscriptions = response.json()['results']

    assert {s['user']['username'] for s in subscriptions} == {wiki_users.volozh.username, wiki_users.chapson.username}


def test_get_subscribers_pass_exclude(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_nested = page_cluster['root/a/ac/bd']

    one = create_subscription(wiki_users.volozh, page_cluster['root'], is_cluster=True)
    two = create_subscription(wiki_users.chapson, page_cluster['root'], is_cluster=True)

    response = client.get(f'/api/v2/public/pages/{page_nested.id}/subscribers')
    assert len(response.json()['results']) == 2

    one.exclude = {'exclude': ['root/a']}
    one.save()
    two.exclude = {'exclude': ['root/a/ac']}
    two.save()

    response = client.get(f'/api/v2/public/pages/{page_nested.id}/subscribers')
    assert len(response.json()['results']) == 0


@only_biz
def test_get_subscribers_page_another_org(client, wiki_users, test_page, organizations):
    client.login(wiki_users.asm)

    test_page.org = organizations.org_21  # так как wiki_users.asm из 42 организации
    test_page.save()

    response = client.get(f'/api/v2/public/pages/{test_page.id}/subscribers')
    assert response.status_code == 404


@only_biz
def test_get_subscribers_filter_by_org(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)

    page_a, page_root = page_cluster['root/a'], page_cluster['root']

    create_subscription(wiki_users.volozh, page_a, is_cluster=True)
    create_subscription(wiki_users.chapson, page_root, is_cluster=True)

    response = client.get(f'/api/v2/public/pages/{page_a.id}/subscribers')
    assert len(response.json()['results']) == 2

    page_root.org = organizations.org_21  # так как wiki_users.asm из 42 организации
    page_root.save()

    response = client.get(f'/api/v2/public/pages/{page_a.id}/subscribers')
    assert len(response.json()['results']) == 1
    assert response.json()['results'][0]['user']['username'] == wiki_users.volozh.username


def test_add_subscribers(client, wiki_users, test_page):
    set_user_setting(wiki_users.asm, 'new_subscriptions', True)
    client.login(wiki_users.asm)

    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.asm)], is_cluster=True)

    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert len(response.json()['results']) == Subscription.objects.count() == 1

    subscription = response.json()['results'][0]
    subscription_db = Subscription.objects.first()

    assert subscription['id'] == subscription_db.id
    assert subscription['is_cluster'] == subscription_db.is_cluster == data.is_cluster
    assert subscription['user']['id'] == subscription_db.user.id == wiki_users.asm.id
    assert subscription_db.type == SubscriptionType.OTHER


def test_add_subscribers__cow(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    set_user_setting(wiki_users.asm, 'new_subscriptions', True)

    idx, slug = -101, 'cow-users'
    copy_on_write_info = {'id': idx, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.asm)], is_cluster=True)
        response = client.post(f'/api/v2/public/pages/{idx}/subscribers', data=data.json())
        assert response.status_code == 200, response.json()

    assert len(response.json()['results']) == Subscription.objects.count() == 1
    assert Page.objects.filter(supertag=slug).exists()


def test_add_subscribers_many(client, wiki_users, test_page):
    set_user_setting(wiki_users.asm, 'new_subscriptions', True)
    set_user_setting(wiki_users.chapson, 'new_subscriptions', True)
    set_user_setting(wiki_users.volozh, 'new_subscriptions', True)
    client.login(wiki_users.asm)

    users = [wiki_users.asm, wiki_users.chapson, wiki_users.volozh]
    data = AddSubscribersSchema(users=[UserIdentity.from_user(user) for user in users], is_cluster=True)

    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert {s['user']['username'] for s in response.json()['results']} == {user.username for user in users}


def test_add_subscribers_invalid_page(client, wiki_users):
    client.login(wiki_users.asm)

    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.asm)], is_cluster=True)
    response = client.post(f'/api/v2/public/pages/{9999}/subscribers', data=data.json())
    assert response.status_code == 404


def test_add_subscribers_no_accessible_by_acl_page(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    set_access_author_only(test_page, new_authors=[wiki_users.thasonic])

    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.asm)], is_cluster=True)
    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert response.status_code == 403


def test_add_subscribers_invalid_user(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    valid_user, invalid_user = UserIdentity.from_user(wiki_users.asm), UserIdentity()
    data = AddSubscribersSchema(users=[valid_user, invalid_user], is_cluster=True)

    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert response.status_code == 400

    data.users.remove(invalid_user)
    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert response.status_code == 200


def test_add_subscribers_user_has_not_access(client, wiki_users, test_page):
    set_user_setting(wiki_users.asm, 'new_subscriptions', True)
    set_user_setting(wiki_users.volozh, 'new_subscriptions', True)
    client.login(wiki_users.asm)

    set_access_author_only(test_page, new_authors=[wiki_users.thasonic, wiki_users.asm])

    users = [wiki_users.thasonic, wiki_users.volozh]  # wiki_users.volozh has no access
    data = AddSubscribersSchema(users=[UserIdentity.from_user(user) for user in users], is_cluster=True)

    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert response.status_code == 403

    users = [wiki_users.thasonic]  # without wiki_users.volozh
    data = AddSubscribersSchema(users=[UserIdentity.from_user(user) for user in users], is_cluster=True)

    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert response.status_code == 200


def test_add_subscribers_user_has_subscription_on_cluster_above(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    root, nested = page_cluster['root'], page_cluster['root/a/aa']
    root_subscr = create_subscription(user=wiki_users.asm, page=root, is_cluster=True)
    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.asm)], is_cluster=True)

    # have subscription to cluster above
    response = client.post(f'/api/v2/public/pages/{nested.id}/subscribers', data=data.json())
    assert response.status_code == 400

    root_subscr.exclude = {'exclude': ['root/a']}
    root_subscr.save()

    # have subscription to cluster above, but nested page in exclude
    response = client.post(f'/api/v2/public/pages/{nested.id}/subscribers', data=data.json())
    assert response.status_code == 200


def test_add_subscriptions_recreate_existing_subscription(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    first = create_subscription(user=wiki_users.volozh, page=test_page, is_cluster=True)
    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.volozh)], is_cluster=False)
    assert Subscription.objects.count() == 1

    client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())

    assert Subscription.objects.count() == 1
    assert Subscription.objects.filter(id=first.id).exists() is False
    assert Subscription.objects.filter(page=test_page).exists()
    assert Subscription.objects.first().is_cluster is False


def test_add_subscriptions_remove_nested_subscriptions(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    root, middle, nested = page_cluster['root'], page_cluster['root/a'], page_cluster['root/a/aa']
    create_subscription(user=wiki_users.volozh, page=nested)
    other = create_subscription(user=wiki_users.asm, page=nested)  # check no delete other people's subscriptions
    assert Subscription.objects.count() == 2

    # no delete when is_cluster=False
    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.volozh)], is_cluster=False)
    client.post(f'/api/v2/public/pages/{middle.id}/subscribers', data=data.json())
    assert {s.page for s in Subscription.objects.filter(user=wiki_users.volozh)} == {middle, nested}
    assert Subscription.objects.filter(id=other.id).exists()

    data.is_cluster = True
    client.post(f'/api/v2/public/pages/{root.id}/subscribers', data=data.json())
    assert {s.page for s in Subscription.objects.filter(user=wiki_users.volozh)} == {root}
    assert Subscription.objects.filter(id=other.id).exists()


@only_biz
def test_add_subscribers_filter_by_org_subscription_above(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)

    root, nested = page_cluster['root'], page_cluster['root/a/aa']
    create_subscription(user=wiki_users.asm, page=root, is_cluster=True)
    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.asm)], is_cluster=True)

    # have subscription to cluster above
    response = client.post(f'/api/v2/public/pages/{nested.id}/subscribers', data=data.json())
    assert response.status_code == 400

    root.org = organizations.org_21
    root.save()

    # subscription to cluster above in another org
    response = client.post(f'/api/v2/public/pages/{nested.id}/subscribers', data=data.json())
    assert response.status_code == 200


@only_biz
def test_add_subscriptions_filter_by_org_nested_subscriptions(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)

    root, nested = page_cluster['root'], page_cluster['root/a/aa']

    nested.org = organizations.org_21
    nested.save()

    create_subscription(user=wiki_users.volozh, page=nested)
    assert Subscription.objects.count() == 1

    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.volozh)], is_cluster=True)
    client.post(f'/api/v2/public/pages/{root.id}/subscribers', data=data.json())
    assert {s.page for s in Subscription.objects.all()} == {root, nested}


def test_delete_subscriber(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    subscr = create_subscription(user=wiki_users.volozh, page=test_page)
    other = create_subscription(user=wiki_users.chapson, page=test_page)
    assert Subscription.objects.count() == 2

    response = client.delete(f'/api/v2/public/pages/{test_page.id}/subscribers/{subscr.id}')
    assert response.status_code == 204
    assert Subscription.objects.count() == 1
    assert Subscription.objects.filter(id=subscr.id).exists() is False
    assert Subscription.objects.filter(id=other.id).exists() is True


def test_delete_subscriber_invalid_page(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    subscr = create_subscription(user=wiki_users.volozh, page=test_page)
    response = client.delete(f'/api/v2/public/pages/{9999}/subscribers/{subscr.id}')
    assert response.status_code == 404


def test_delete_subscriber_invalid_subscr(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    response = client.delete(f'/api/v2/public/pages/{test_page.id}/subscribers/{999}')
    assert response.status_code == 404


def test_delete_subscriber_no_accessible_by_acl_page(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    set_access_author_only(test_page, new_authors=[wiki_users.thasonic])

    subscr = create_subscription(user=wiki_users.volozh, page=test_page)
    response = client.delete(f'/api/v2/public/pages/{test_page.id}/subscribers/{subscr.id}')
    assert response.status_code == 403


def test_delete_subscriber_other_subscription(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    current, other = page_cluster['root/a'], page_cluster['root/b']
    subscr = create_subscription(user=wiki_users.volozh, page=other)

    response = client.delete(f'/api/v2/public/pages/{current.id}/subscribers/{subscr.id}')
    assert response.status_code == 404
